package com.muhammetgecgil.turkradyo;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.view.ViewGroup;
import java.io.InputStream;
import java.net.URLConnection;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String A="https://appassets.androidplatform.net/assets/";
    private static final int REQ_NOTIF=42, REQ_NOTA_AUDIO=43;
    private int pendingNotaSeconds=0;
    private String pendingNotaStation="", pendingNotaTitle="";
    private boolean pendingNotaText=false;
    private boolean rendererRecoveryPending=false;
    private boolean activityDestroyed=false;
    private android.window.OnBackInvokedCallback backCallback;
    private final Handler uiHandler=new Handler(Looper.getMainLooper());
    private long recoveryWindowStart=0;
    private int recoveryCount=0;

    private void disposeWebView(WebView view){
        if(view==null)return;
        if(view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);
        view.removeJavascriptInterface("RadioNative");
        view.destroy();
    }

    @Override public void onCreate(Bundle b){super.onCreate(b);ProductGuard.install(this,"MainActivity");ProductGuard.recordLaunch(this,"MainActivity");buildWebView();
        if(Build.VERSION.SDK_INT>=33){backCallback=this::handleBack;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback);}
        handleSearchIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleSearchIntent(intent);}
    private void handleSearchIntent(Intent intent){
        if(intent!=null&&android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction()))
            startFg(new Intent(this,RadioService.class).setAction(RadioService.ACTION_SEARCH).putExtra("query",intent.getStringExtra(android.app.SearchManager.QUERY)));
    }

    private void buildWebView(){
        if(activityDestroyed||isFinishing())return;
        rendererRecoveryPending=false;
        webView=new WebView(this);setContentView(webView);
        if(Build.VERSION.SDK_INT>=26)webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT,false);
        if(Build.VERSION.SDK_INT>=29){
            webView.setWebViewRenderProcessClient(new WebViewRenderProcessClient(){
                @Override public void onRenderProcessUnresponsive(WebView view,WebViewRenderProcess renderer){
                    if(activityDestroyed||view!=webView||rendererRecoveryPending)return;
                    rendererRecoveryPending=true;
                    try{if(renderer==null||!renderer.terminate())rendererRecoveryPending=false;}catch(Exception e){rendererRecoveryPending=false;}
                }
                @Override public void onRenderProcessResponsive(WebView view,WebViewRenderProcess renderer){rendererRecoveryPending=false;}
            });
        }
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setLoadsImagesAutomatically(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.addJavascriptInterface(new Bridge(),"RadioNative");webView.setWebViewClient(new C());
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIF);
        webView.loadUrl(A+"index.html?v=2873");
    }

    public final class Bridge {
        @JavascriptInterface public void command(String command){
            if(command==null||!command.startsWith("radioapp://"))return;
            runOnUiThread(()->{if(!activityDestroyed)try{nativeCall(Uri.parse(command));}catch(Exception e){android.widget.Toast.makeText(MainActivity.this,"İşlem uygulanamadı",android.widget.Toast.LENGTH_SHORT).show();}});
        }
        @JavascriptInterface public String getRecentTracks(){
            try{
                org.json.JSONArray src=new org.json.JSONArray(getSharedPreferences("radio",MODE_PRIVATE).getString("tracks","[]"));
                org.json.JSONArray out=new org.json.JSONArray();
                for(int i=0;i<src.length()&&out.length()<50;i++){org.json.JSONObject x=src.optJSONObject(i);if(x!=null)out.put(x);}
                return out.toString();
            }catch(Exception e){return "[]";}
        }
        @JavascriptInterface public String getTelemetry(){
            try{
                SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);
                org.json.JSONObject state=new org.json.JSONObject(p.getString("telemetry","{}"));
                org.json.JSONArray eq=new org.json.JSONArray();for(int b=0;b<5;b++)eq.put(p.getInt("eq_"+b,0));
                state.put("eq",eq);state.put("volume",p.getFloat("volume",1f));state.put("gainMb",p.getInt("gainMb",0));
                state.put("normalize",p.getBoolean("normalize",false));state.put("smooth",p.getBoolean("smooth",true));
                if(!RadioService.isRunning){state.put("serviceActive",false);state.put("isPlaying",false);state.put("buffering",false);state.put("playWhenReady",false);}
                return state.toString();
            }catch(Exception e){return "{}";}
        }
        @JavascriptInterface public String getSleepTimer(){
            try{
                SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);
                org.json.JSONObject o=new org.json.JSONObject();
                o.put("when",p.getLong("sleepDeadline",0));o.put("fade",p.getBoolean("sleepFade",false));
                o.put("exactAllowed",Build.VERSION.SDK_INT<31||((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms());
                return o.toString();
            }catch(Exception e){return "{}";}
        }
        @JavascriptInterface public String getNowTitle(){return getSharedPreferences("radio",MODE_PRIVATE).getString("nowTitle","");}
        @JavascriptInterface public String getStreamHealth(String name){return StreamFallbackManager.getHealthJson(MainActivity.this,name==null?"Türk Radyo":name);}
        @JavascriptInterface public String getProductGuard(){return ProductGuard.statusJson(MainActivity.this);}
        @JavascriptInterface public String getCatalogHealth(){return CatalogHealthManager.report(MainActivity.this);}
        @JavascriptInterface public void scanCatalog(){String q=getSharedPreferences("radio",MODE_PRIVATE).getString("queue","[]");CatalogHealthManager.scanAsync(MainActivity.this,q);}
        @JavascriptInterface public void scanCatalogFull(){String q=getSharedPreferences("radio",MODE_PRIVATE).getString("queue","[]");CatalogHealthManager.fullSweepAsync(MainActivity.this,q);}
        @JavascriptInterface public void setQueue(String json,int idx){String q=json==null?"[]":json;getSharedPreferences("radio",MODE_PRIVATE).edit().putString("queue",q).putInt("queueIndex",Math.max(0,idx)).apply();CatalogHealthManager.scanAsync(MainActivity.this,q);CatalogHealthManager.autoSweepIfDue(MainActivity.this,q);}
        @JavascriptInterface public String startNotaAnalysis(int seconds,String station,String title,boolean textMode){
            int sec=Math.max(15,Math.min(180,seconds));
            if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                pendingNotaSeconds=sec;pendingNotaStation=station==null?"":station;pendingNotaTitle=title==null?"":title;pendingNotaText=textMode;
                runOnUiThread(()->requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_NOTA_AUDIO));
                return "{\"state\":\"PERMISSION_REQUIRED\",\"running\":false,\"progress\":0,\"error\":\"Ses analizi izni bekleniyor\"}";
            }
            return NotaTranscriber.start(MainActivity.this,sec,station,title,textMode);
        }
        @JavascriptInterface public String getNotaAnalysis(){return NotaTranscriber.getStatusJson(MainActivity.this);}
        @JavascriptInterface public String stopNotaAnalysis(){return NotaTranscriber.stop(MainActivity.this);}
        @JavascriptInterface public String getNotaTextArchive(){return NotaTranscriber.getTextArchive(MainActivity.this);}
        @JavascriptInterface public void clearNotaTextArchive(){NotaTranscriber.clearTextArchive(MainActivity.this);}
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQ_NOTA_AUDIO){
            boolean ok=grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED;
            if(ok&&pendingNotaSeconds>0){
                int sec=pendingNotaSeconds;String st=pendingNotaStation,ti=pendingNotaTitle;boolean tx=pendingNotaText;
                pendingNotaSeconds=0;pendingNotaStation="";pendingNotaTitle="";pendingNotaText=false;
                NotaTranscriber.start(this,sec,st,ti,tx);
            }else{
                pendingNotaSeconds=0;pendingNotaStation="";pendingNotaTitle="";pendingNotaText=false;
                NotaTranscriber.permissionDenied(this);
            }
        }
    }

    final class C extends WebViewClient {
        @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){String u=r.getUrl().toString();if(u.startsWith(A)){try{String p=u.substring(A.length());int q=p.indexOf('?');if(q>=0)p=p.substring(0,q);InputStream in=getAssets().open(p);String m=URLConnection.guessContentTypeFromName(p);if(m==null)m=p.endsWith(".css")?"text/css":p.endsWith(".js")?"application/javascript":p.endsWith(".html")?"text/html":"application/octet-stream";return new WebResourceResponse(m,"UTF-8",in);}catch(Exception ignored){}}return super.shouldInterceptRequest(v,r);}
        @Override public void onPageFinished(WebView v,String u){if(v==webView&&!activityDestroyed&&u.startsWith(A+"index.html"))v.evaluateJavascript("(function(){function c(i,n){if(document.getElementById(i))return;var x=document.createElement('link');x.id=i;x.rel='stylesheet';x.href='"+A+"'+n+'?v=2873';document.head.appendChild(x)}function j(i,n){if(document.getElementById(i))return;var x=document.createElement('script');x.async=false;x.id=i;x.src='"+A+"'+n+'?v=2873';document.body.appendChild(x)}c('profile1NatureCss','profile1-nature-themes.css');c('profile1ArtV2Css','profile1-art-v2.css');c('profile1ArtV3Css','profile1-art-v3.css');c('profile1ArtV4Css','profile1-art-v4.css');c('profile1RadioV5Css','profile1-radio-engine-v5.css');c('profile1RadioV6Css','profile1-radio-ui-v6.css');c('profile1RadioV7Css','profile1-radio-ui-v7.css');c('profile1CardV8Css','profile1-card-system-v8.css');c('profile1RadioV9Css','profile1-radio-ui-v9.css');c('profile2BalancedCss','profile2-balanced.css');c('profile2DiscoveryV12Css','profile2-discovery-v12.css');c('profile2DesignV13Css','profile2-design-v13.css');c('profile2CompactV14Css','profile2-compact-v14.css');c('profile2ArtV15Css','profile2-art-v15.css');c('profile2ArtV16Css','profile2-art-v16.css');c('profile2UnifiedV17Css','profile2-unified-v17.css');c('profile2FixesV18Css','profile2-fixes-v18.css');c('profile2DialFitV183Css','profile2-dial-fit-v18-3.css');c('profile2HealthTimerV184Css','profile2-health-timer-v18-4.css');c('premiumArtV2873Css','premium-art-v2873.css');j('premiumArtV2873Js','premium-art-v2873.js');j('profile1NatureJs','profile1-nature-themes.js');j('profile1ArtV3Js','profile1-art-v3.js');j('profile1RadioV6Js','profile1-radio-ui-v6.js');j('profile1RadioV9Js','profile1-radio-ui-v9.js');j('profile2FixesV18Js','profile2-fixes-v18.js');j('profile2BalancedV11Js','profile2-balanced-v11.js');j('profile2DiscoveryV12Js','profile2-discovery-v12.js');j('profile2DesignV13Js','profile2-design-v13.js');j('profile2ArtV16Js','profile2-art-v16.js');j('profile2UnifiedV17Js','profile2-unified-v17.js');j('profile2DialFitV183Js','profile2-dial-fit-v18-3.js');j('profile2HealthTimerV184Js','profile2-health-timer-v18-4.js');j('profileMain80V186Js','profile-main80-v18-6.js');j('lastRadioV187Js','last-radio-v18-7.js');j('catalogManagerV1Js','radio-catalog-manager-v1.js');j('genreModeV1Js','genre-mode-v1.js');j('smartListenerV1Js','smart-listener-v1.js');j('smartListenerSettingsV1Js','smart-listener-settings-v1.js');j('aboutProfile3V1Js','about-profile3-v1.js');j('notaAiV1Js','nota-ai-v1.js');j('productHealthV1Js','product-health-v1.js');j('productHardeningV3Js','product-hardening-v3.js');j('productHomeV1Js','product-home-v1.js');j('reliabilityGateV1Js','reliability-gate-v1.js');j('catalogHealthUiV1Js','catalog-health-ui-v1.js');j('themeSyncV1Js','theme-sync-v1.js');j('refinementV2872Js','refinement-v2872.js')})();",null);}
        @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if("radioapp".equalsIgnoreCase(u.getScheme())){nativeCall(u);return true;}if(("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme()))&&u.toString().startsWith(A))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;}
        @Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail){
            rendererRecoveryPending=false;
            boolean current=view==webView;
            if(current)webView=null;
            disposeWebView(view);
            if(!current||activityDestroyed||isFinishing())return true;
            long now=SystemClock.elapsedRealtime();
            if(now-recoveryWindowStart>60_000){recoveryWindowStart=now;recoveryCount=0;}
            if(++recoveryCount<=3)uiHandler.postDelayed(MainActivity.this::buildWebView,250);
            else {
                android.widget.Button retry=new android.widget.Button(MainActivity.this);
                retry.setText("Radyo ekranını yeniden aç");
                retry.setOnClickListener(v->{recoveryCount=0;buildWebView();});
                setContentView(retry);
            }
            return true;
        }
    }

    private void nativeCall(Uri u){String h=u.getHost()==null?"":u.getHost();Intent i;switch(h){case"play":String url=u.getQueryParameter("url"),n=u.getQueryParameter("name");if(url==null)return;i=new Intent(this,RadioService.class).setAction(RadioService.ACTION_PLAY).putExtra("url",url).putExtra("name",n);startFg(i);break;case"pause":startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_PAUSE));break;case"resume":startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_RESUME));break;case"stop":startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_STOP));break;case"vol":startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_VOLUME).putExtra("volume",f(u.getQueryParameter("v"),1)));break;case"gain":startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_GAIN).putExtra("gain",q(u.getQueryParameter("mb"),0)));break;case"eq":startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_EQ).putExtra("band",q(u.getQueryParameter("band"),0)).putExtra("level",q(u.getQueryParameter("level"),0)));break;case"normalize":startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_NORMALIZE).putExtra("on","1".equals(u.getQueryParameter("on"))));break;case"smooth":startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_SMOOTH).putExtra("on","1".equals(u.getQueryParameter("on"))));break;case"shazam":try{Intent z=getPackageManager().getLaunchIntentForPackage("com.shazam.android");startActivity(z!=null?z:new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.shazam.com/")));}catch(Exception ignored){}break;case"alarmsettings":if(Build.VERSION.SDK_INT>=31)try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}break;case"alarm":alarm(u,false);break;case"sleep":alarm(u,true);break;case"alarmclear":case"sleepclear":cancel(u);break;}}
    private void alarm(Uri u,boolean sleep){
        long when=l(u.getQueryParameter("when"),0);
        int id=q(u.getQueryParameter("id"),sleep?7999:7400);
        if(when<=System.currentTimeMillis())return;
        Intent intent=new Intent(this,AlarmReceiver.class).putExtra("sleep",sleep);
        if(!sleep)intent.putExtra("url",u.getQueryParameter("url")).putExtra("name",u.getQueryParameter("name"));
        PendingIntent pending=PendingIntent.getBroadcast(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms=(AlarmManager)getSystemService(ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=31&&!alarms.canScheduleExactAlarms())alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);
        else alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending);
        if(sleep){
            getSharedPreferences("radio",MODE_PRIVATE).edit().putLong("sleepDeadline",when).putBoolean("sleepFade","1".equals(u.getQueryParameter("fade"))).apply();
            if(RadioService.isRunning)startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_SLEEP_TIMER));
        }
    }
    private void cancel(Uri u){
        int id=q(u.getQueryParameter("id"),7400);
        PendingIntent p=PendingIntent.getBroadcast(this,id,new Intent(this,AlarmReceiver.class),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
        if(p!=null)((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(p);
        if("sleepclear".equals(u.getHost())){
            getSharedPreferences("radio",MODE_PRIVATE).edit().remove("sleepDeadline").remove("sleepFade").apply();
            if(RadioService.isRunning)startService(new Intent(this,RadioService.class).setAction(RadioService.ACTION_SLEEP_TIMER));
        }
    }
    private void startFg(Intent i){if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}static int q(String s,int d){try{return Integer.parseInt(s);}catch(Exception e){return d;}}static long l(String s,long d){try{return Long.parseLong(s);}catch(Exception e){return d;}}static float f(String s,float d){try{return Float.parseFloat(s);}catch(Exception e){return d;}}
    @Override protected void onResume(){super.onResume();if(webView!=null)webView.onResume();}
    @Override protected void onPause(){if(webView!=null)webView.onPause();super.onPause();}
    @Override protected void onDestroy(){
        activityDestroyed=true;uiHandler.removeCallbacksAndMessages(null);
        if(Build.VERSION.SDK_INT>=33&&backCallback!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        WebView old=webView;webView=null;disposeWebView(old);super.onDestroy();
    }
    private void handleBack(){
        WebView view=webView;
        if(view==null){finish();return;}
        view.evaluateJavascript("(function(){if(typeof window.trCloseTopOverlay==='function'&&window.trCloseTopOverlay())return true;var s=document.getElementById('sheet');if(s&&s.classList.contains('show')){s.classList.remove('show');return true}return false})()",closed->{
            if(activityDestroyed||view!=webView||"true".equals(closed))return;
            if(view.canGoBack())view.goBack();else finish();
        });
    }
    // Android 13+ uses the dispatcher registered above; this is the API 26-32 fallback.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){handleBack();}
}
