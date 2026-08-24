package com.mg.quakewatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.AlphaAnimation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PremiumResearchActivity extends Activity {
    private static final int BG=Color.rgb(4,8,15), PANEL=Color.rgb(11,18,30), CARD=Color.rgb(15,24,39);
    private static final int TEXT=Color.rgb(242,247,255), MUTED=Color.rgb(137,160,190), CYAN=Color.rgb(76,211,255), GOLD=Color.rgb(255,204,99), RED=Color.rgb(255,74,100), GREEN=Color.rgb(69,228,160), PURPLE=Color.rgb(174,128,255);
    private static final String PREF="research_console_settings";
    private FrameLayout stage;
    private LinearLayout body, nav;
    private TextView riskMetric, updateMetric, statusMetric, title;
    private TurkeyAnalyzer.Report current;
    private String active="HARİTA";
    private WebView map;
    private int hours=168;
    private double minMag=-1.0,maxDepth=700;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.rgb(6,12,21));
        build();showPage("HARİTA");showSplash();analyze();
    }

    private void build(){
        stage=new FrameLayout(this);stage.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=20){root.setOnApplyWindowInsetsListener((v,in)->{int top=0,bottom=0;if(Build.VERSION.SDK_INT>=30){top=in.getInsets(WindowInsets.Type.statusBars()).top;bottom=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{top=in.getSystemWindowInsetTop();bottom=in.getSystemWindowInsetBottom();}v.setPadding(0,top,0,bottom+dp(4));return in;});root.requestApplyInsets();}

        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(16),dp(12),dp(16),dp(12));
        head.setBackground(gradient(new int[]{Color.rgb(12,24,40),Color.rgb(7,13,24)},GradientDrawable.Orientation.TL_BR));
        LinearLayout brand=row();TextView logo=pill("QW",CYAN,Color.rgb(6,24,35));logo.setTypeface(Typeface.DEFAULT_BOLD);logo.setGravity(Gravity.CENTER);brand.addView(logo,new LinearLayout.LayoutParams(dp(52),dp(36)));
        LinearLayout bn=new LinearLayout(this);bn.setOrientation(LinearLayout.VERTICAL);bn.setPadding(dp(10),0,0,0);TextView name=t("QUAKE WATCH",23,TEXT);name.setTypeface(Typeface.DEFAULT_BOLD);bn.addView(name);bn.addView(t("PREDICTIVE EARTH SYSTEM • RESEARCH MODE",9,MUTED));brand.addView(bn,new LinearLayout.LayoutParams(0,-2,1));
        TextView live=pill("● LIVE",GREEN,Color.rgb(8,38,30));live.setGravity(Gravity.CENTER);brand.addView(live,new LinearLayout.LayoutParams(dp(78),dp(30)));head.addView(brand);
        LinearLayout meters=row();meters.setPadding(0,dp(10),0,0);riskMetric=metric("QIE TÜRKİYE","--",GOLD);updateMetric=metric("SON VERİ","--",CYAN);statusMetric=metric("MOTOR","HAZIR",GREEN);meters.addView(riskMetric,one());meters.addView(gap(6));meters.addView(updateMetric,one());meters.addView(gap(6));meters.addView(statusMetric,one());head.addView(meters);root.addView(head);

        LinearLayout pageBar=row();pageBar.setPadding(dp(16),dp(10),dp(16),dp(8));title=t("HARİTA",17,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);pageBar.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView badge=pill("PRO LAB",CYAN,Color.rgb(11,34,49));badge.setGravity(Gravity.CENTER);pageBar.addView(badge,new LinearLayout.LayoutParams(dp(88),dp(30)));root.addView(pageBar);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),0,dp(12),dp(16));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(8),dp(7),dp(8),dp(10));nav.setBackground(gradient(new int[]{Color.rgb(14,23,38),Color.rgb(7,13,23)},GradientDrawable.Orientation.TOP_BOTTOM));
        String[] tx={"⌖\nHarita","◆\nTahmin","◫\nAnaliz","✓\nDenetim","⚙\nAyarlar"};String[] id={"HARİTA","TAHMİN","ANALİZ","DENETİM","AYARLAR"};for(int i=0;i<id.length;i++){final String p=id[i];Button n=navButton(tx[i]);n.setTag(p);n.setOnClickListener(v->showPage(p));nav.addView(n,new LinearLayout.LayoutParams(0,dp(62),1));}root.addView(nav);
        stage.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(stage);updateNav();
    }

    private void showSplash(){
        final LinearLayout s=new LinearLayout(this);s.setOrientation(LinearLayout.VERTICAL);s.setGravity(Gravity.CENTER);s.setPadding(dp(28),dp(28),dp(28),dp(28));s.setBackground(gradient(new int[]{Color.rgb(2,8,18),Color.rgb(8,27,42),Color.rgb(3,9,17)},GradientDrawable.Orientation.TL_BR));
        TextView orb=t("◉",74,CYAN);orb.setGravity(Gravity.CENTER);s.addView(orb);TextView q=t("QUAKE WATCH",31,TEXT);q.setTypeface(Typeface.DEFAULT_BOLD);q.setGravity(Gravity.CENTER);s.addView(q);TextView r=t("EARTH INTELLIGENCE SYSTEM",12,GOLD);r.setGravity(Gravity.CENTER);r.setPadding(0,dp(8),0,dp(18));s.addView(r);TextView scan=t("Sismik ağlar • fay zekâsı • QIE • Space Watch",11,MUTED);scan.setGravity(Gravity.CENTER);s.addView(scan);TextView boot=pill("INITIALIZING RESEARCH CORE",GREEN,Color.rgb(8,33,29));boot.setGravity(Gravity.CENTER);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(240),dp(38));bp.setMargins(0,dp(28),0,0);s.addView(boot,bp);
        stage.addView(s,new FrameLayout.LayoutParams(-1,-1));new Handler().postDelayed(()->{AlphaAnimation a=new AlphaAnimation(1f,0f);a.setDuration(420);a.setFillAfter(true);s.startAnimation(a);new Handler().postDelayed(()->stage.removeView(s),430);},1250);
    }

    private void showPage(String p){active=p;if(title!=null)title.setText(p);if(body==null)return;body.removeAllViews();updateNav();if("HARİTA".equals(p))mapPage();else if("TAHMİN".equals(p))forecastPage();else if("ANALİZ".equals(p))analysisPage();else if("DENETİM".equals(p))auditPage();else settingsPage();}
    private void updateNav(){if(nav==null)return;for(int i=0;i<nav.getChildCount();i++){View v=nav.getChildAt(i);if(v instanceof Button){Button b=(Button)v;boolean on=active.equals(b.getTag());b.setTextColor(on?CYAN:MUTED);b.setBackground(round(on?Color.rgb(17,46,66):Color.TRANSPARENT,16,on?Color.rgb(44,118,151):Color.TRANSPARENT));}}}

    private void mapPage(){
        LinearLayout hero=hero();hero.addView(label("LIVE EARTH RADAR"));hero.addView(t("Sismik aktiviteyi tek elle yönetilen araştırma haritasında izle",17,TEXT));hero.addView(t("Animasyonlu risk halkaları • akan fay izi • sürüklenebilir bilgi paneli",11,MUTED));body.addView(hero);
        LinearLayout filters=card();filters.addView(label("AKILLI FİLTRELER"));TextView ht=value("Zaman","Son "+hours+" saat");SeekBar hs=seek(hours-1,167);filters.addView(ht);filters.addView(hs);TextView mt=value("Büyüklük",String.format(Locale.US,"M ≥ %.1f",minMag));SeekBar ms=seek((int)((minMag+1)*10),70);filters.addView(mt);filters.addView(ms);TextView dt=value("Derinlik",(int)maxDepth+" km ve sığ");SeekBar ds=seek((int)maxDepth,700);filters.addView(dt);filters.addView(ds);body.addView(filters);
        FrameLayout frame=new FrameLayout(this);frame.setBackground(round(Color.rgb(7,13,23),20,Color.rgb(35,65,88)));map=new WebView(this);WebSettings w=map.getSettings();w.setJavaScriptEnabled(true);w.setDomStorageEnabled(true);map.setBackgroundColor(Color.rgb(5,10,18));frame.addView(map,new FrameLayout.LayoutParams(-1,dp(650)));body.addView(frame,new LinearLayout.LayoutParams(-1,dp(650)));
        if(current!=null)renderMap(current);else map.loadData("<html><body style='background:#050a12;color:white;font-family:sans-serif'>Araştırma haritası yükleniyor...</body></html>","text/html","UTF-8");
        hs.setOnSeekBarChangeListener(listener(p->{hours=p+1;ht.setText("Zaman                                      Son "+hours+" saat");js("setTime("+hours+")");}));ms.setOnSeekBarChangeListener(listener(p->{minMag=-1+p/10.0;mt.setText(String.format(Locale.US,"Büyüklük                                      M ≥ %.1f",minMag));js("setMag("+minMag+")");}));ds.setOnSeekBarChangeListener(listener(p->{maxDepth=Math.max(5,p);dt.setText("Derinlik                                      "+(int)maxDepth+" km ve sığ");js("setDepth("+maxDepth+")");}));
        body.addView(note("Harita jestleri: tek parmak sürükle • iki parmak yakınlaştır • çift dokun büyüt. Sağdaki hızlı araçlar tek elle kullanıma göre yerleştirildi."));
    }

    private void forecastPage(){
        LinearLayout h=hero();h.addView(label("QIE FORECAST"));h.addView(t("Modelin şimdi en dikkat çekici gördüğü bölgeler",18,TEXT));h.addView(t("Göreli aktivite endeksi • açıklanabilir gerekçe • güven",11,MUTED));body.addView(h);
        LinearLayout c=card();c.addView(label("TOP 10 RİSK BÖLGESİ"));c.addView(t(current==null?"Analiz bekleniyor...":top10(current.hotspotsJson),14,TEXT));body.addView(c);LinearLayout why=card();why.addView(label("NEDEN RİSKLİ?"));why.addView(t(current==null?"Veri bekleniyor...":explain(current.hotspotsJson),13,TEXT));body.addView(why);Button r=accent("↻ ŞİMDİ YENİLE",CYAN);r.setOnClickListener(v->analyze());body.addView(r,new LinearLayout.LayoutParams(-1,dp(56)));body.addView(note("Bu ekran kesin deprem yeri/saati söylemez; göreli kısa dönem sismik anomaliyi sıralar."));
    }

    private void analysisPage(){
        LinearLayout h=hero();h.addView(label("PROFESSOR CONSOLE"));h.addView(t("Sismoloji + istatistik + Space Watch araştırma araçları",17,TEXT));body.addView(h);LinearLayout tools=row();Button b=accent("BACKTEST",GREEN),s=accent("SPACE",PURPLE),d=accent("DERİNLİK",CYAN);tools.addView(b,one());tools.addView(gap(6));tools.addView(s,one());tools.addView(gap(6));tools.addView(d,one());body.addView(tools);TextView out=t(current==null?"Analiz bekleniyor...":current.text,13,TEXT);LinearLayout c=card();c.addView(out);body.addView(c);b.setOnClickListener(v->{out.setText("Backtest çalışıyor...");new Thread(()->{try{BacktestEngine.Result x=BacktestEngine.run();runOnUiThread(()->out.setText(x.text));}catch(Exception e){runOnUiThread(()->out.setText(e.getMessage()));}}).start();});s.setOnClickListener(v->{out.setText("Space Watch verisi alınıyor...");new Thread(()->{try{SatelliteResearchEngine.Report x=SatelliteResearchEngine.fetch();runOnUiThread(()->out.setText(x.text));}catch(Exception e){runOnUiThread(()->out.setText(e.getMessage()));}}).start();});d.setOnClickListener(v->{showPage("HARİTA");if(map!=null)map.postDelayed(()->js("openDepth()"),500);});
    }

    private void auditPage(){LinearLayout h=hero();h.addView(label("FORECAST AUDIT"));h.addView(t("Model ne dedi, sonra ne oldu?",18,TEXT));h.addView(t("İsabet • yanlış alarm • açık tahmin • Brier",11,MUTED));body.addView(h);TextView r=t(PredictionAudit.report(this),13,TEXT);LinearLayout c=card();c.addView(r);body.addView(c);LinearLayout a=row();Button v=accent("KATALOGLA DOĞRULA",GREEN),x=accent("RAPORU YENİLE",CYAN);a.addView(v,one());a.addView(gap(8));a.addView(x,one());body.addView(a);v.setOnClickListener(z->{if(current!=null){PredictionAudit.verifyAgainstCatalog(this,current.eventsJson);r.setText(PredictionAudit.report(this));}});x.setOnClickListener(z->r.setText(PredictionAudit.report(this)));}

    private void settingsPage(){SharedPreferences p=getSharedPreferences(PREF,Context.MODE_PRIVATE);LinearLayout c=card();c.addView(label("UYARI MERKEZİ"));TextView tv=value("Risk eşiği",p.getInt("threshold",65)+" / 100");SeekBar sk=seek(p.getInt("threshold",65)-50,40);c.addView(tv);c.addView(sk);Switch micro=new Switch(this);micro.setText("Mikrodepremleri göster");micro.setTextColor(TEXT);micro.setChecked(p.getBoolean("micro",true));c.addView(micro);body.addView(c);LinearLayout row=row();Button on=accent("UYARILARI AÇ",GREEN),off=accent("DURDUR",RED),google=accent("GOOGLE",CYAN);row.addView(on,one());row.addView(gap(5));row.addView(off,one());row.addView(gap(5));row.addView(google,one());body.addView(row);sk.setOnSeekBarChangeListener(listener(v->{int q=50+v;tv.setText("Risk eşiği                                      "+q+" / 100");p.edit().putInt("threshold",q).apply();}));micro.setOnCheckedChangeListener((b,v)->p.edit().putBoolean("micro",v).apply());on.setOnClickListener(v->{Intent i=new Intent(this,QuakeMonitorService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);});off.setOnClickListener(v->stopService(new Intent(this,QuakeMonitorService.class)));google.setOnClickListener(v->{try{startActivity(new Intent("android.settings.SAFETY_CENTER"));}catch(Exception e){startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}});body.addView(note("Alt navigasyon ve tüm kritik kontroller Android sistem gezinme alanından güvenli boşlukla ayrılır."));}

    private void analyze(){statusMetric.setText("MOTOR\nHESAPLIYOR");new Thread(()->{try{TurkeyAnalyzer.Report r=TurkeyAnalyzer.fetchAndAnalyze();PredictionAudit.verifyAgainstCatalog(this,r.eventsJson);PredictionAudit.recordTurkeyForecast(this,r);current=r;runOnUiThread(()->{riskMetric.setText(String.format(Locale.US,"QIE TÜRKİYE\n%.1f/100",r.maxScore));updateMetric.setText("SON VERİ\n"+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));statusMetric.setText("MOTOR\nAKTİF");if("HARİTA".equals(active)&&map!=null)renderMap(r);});}catch(Exception e){runOnUiThread(()->statusMetric.setText("MOTOR\nHATA"));}}).start();}

    private void renderMap(TurkeyAnalyzer.Report r){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#m{height:100%;margin:0;background:#050a12;font-family:Arial;color:#fff}.leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#101b2b;color:#fff}.rail{position:absolute;z-index:1200;right:10px;top:88px;display:flex;flex-direction:column;gap:8px}.rail button{width:50px;height:50px;border-radius:16px;border:1px solid #31506c;background:#0b1726eF;color:#eaf6ff;font-weight:bold;box-shadow:0 7px 18px #0008}.rail button:active{transform:scale(.96);background:#16344c}.sheet{position:absolute;z-index:1250;left:10px;right:10px;bottom:10px;min-height:78px;max-height:250px;border-radius:20px;background:linear-gradient(145deg,#0d1929f5,#07101df5);border:1px solid #2c4d68;box-shadow:0 12px 35px #000a;padding:10px 14px;overflow:auto;touch-action:none}.grab{width:48px;height:5px;border-radius:5px;background:#45647c;margin:0 auto 9px}.riskPulse{animation:pulse 1.8s ease-out infinite}@keyframes pulse{0%{stroke-opacity:.95;fill-opacity:.22}70%{stroke-opacity:.25;fill-opacity:.06}100%{stroke-opacity:.05;fill-opacity:.02}}.faultFlow{stroke-dasharray:10 10;animation:flow 2.1s linear infinite}@keyframes flow{to{stroke-dashoffset:-40}}.legend{position:absolute;z-index:1100;left:10px;top:10px;background:#0a1524e8;border:1px solid #29435a;border-radius:14px;padding:8px 10px;font-size:11px}</style></head><body><div id='m'></div><div class='legend'>QIE RADAR • risk halkaları canlı</div><div class='rail'><button onclick='fitTR()'>TR</button><button onclick='fitMar()'>MAR</button><button onclick='toggleRisk()'>RISK</button><button onclick='toggleFault()'>FAY</button><button onclick='play()'>▶</button></div><div id='sheet' class='sheet'><div class='grab'></div><b id='st'>Bölge seç</b><div id='sd' style='color:#9cb4cf;font-size:12px;margin-top:6px'>Bir risk halkasına, faya veya depreme dokun. Paneli yukarı-aşağı sürükleyebilirsin.</div></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>const E="+r.eventsJson+",H="+r.hotspotsJson+",F="+r.faultsJson+";const m=L.map('m',{dragging:true,touchZoom:true,doubleClickZoom:true,inertia:true,preferCanvas:false}).setView([39,35],5);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:14,attribution:'© OpenStreetMap'}).addTo(m);let hrs=168,mm=-1,md=700,rOn=true,fOn=true,timer=null;const q=L.layerGroup().addTo(m),hg=L.layerGroup().addTo(m),fg=L.layerGroup().addTo(m);function hc(s){return s>=80?'#ff3654':s>=60?'#ff981c':s>=40?'#ffd33c':'#42dc98'}function info(t,d){document.getElementById('st').innerHTML=t;document.getElementById('sd').innerHTML=d}F.forEach(x=>{let p=L.polyline(x.pts,{color:'#ff5972',weight:3,opacity:.88,className:'faultFlow'}).addTo(fg);p.on('click',()=>info(x.name,x.system+' • '+x.type))});H.forEach(x=>{let p=L.circle([x.lat,x.lon],{radius:8500+x.score*260,color:hc(x.score),fillColor:hc(x.score),fillOpacity:.14,weight:3,className:'riskPulse'}).addTo(hg);p.on('click',()=>info('QIE risk '+x.score.toFixed(1)+'/100','24s '+(x.q24||0).toFixed(1)+' • 7g '+(x.q7||0).toFixed(1)+' • güven '+(x.confidence||0).toFixed(0)+'%<br>oran '+x.rate.toFixed(2)+'x • b≈'+x.b.toFixed(2)+' • ETAS '+x.etas.toFixed(2)))});function qc(e){return e.mag>=4?'#ff3957':e.mag>=2?'#ffb331':'#43d99a'}function draw(){q.clearLayers();let cut=Date.now()-hrs*3600000;E.filter(e=>e.time>=cut&&e.mag>=mm&&e.depth<=md).forEach(e=>{let p=L.circleMarker([e.lat,e.lon],{radius:Math.max(3,3+Math.max(0,e.mag)*1.45),color:qc(e),fillColor:qc(e),fillOpacity:.86,weight:1}).addTo(q);p.on('click',()=>info('M'+e.mag.toFixed(2)+' • '+e.place,e.depth.toFixed(1)+' km derinlik<br>Yakın fay: '+e.fault+' ~'+e.faultKm.toFixed(0)+' km'))})}draw();window.setTime=x=>{hrs=x;draw()};window.setMag=x=>{mm=x;draw()};window.setDepth=x=>{md=x;draw()};window.fitTR=()=>m.fitBounds([[35.5,25.5],[42.3,45.5]]);window.fitMar=()=>m.fitBounds([[39.7,26.2],[41.4,31.1]]);window.toggleRisk=()=>{rOn?m.removeLayer(hg):m.addLayer(hg);rOn=!rOn};window.toggleFault=()=>{fOn?m.removeLayer(fg):m.addLayer(fg);fOn=!fOn};window.play=()=>{if(timer){clearInterval(timer);timer=null;hrs=168;draw();return}let h=6;timer=setInterval(()=>{hrs=h;draw();h+=6;if(h>168){clearInterval(timer);timer=null;hrs=168;draw()}},420)};window.openDepth=()=>info('Derinlik analizi','Bir deprem noktasına dokunarak derinlik ve yakın fay bağlamını incele.');let sh=document.getElementById('sheet'),sy=0,sb=10;sh.addEventListener('touchstart',e=>{sy=e.touches[0].clientY;sb=parseInt(getComputedStyle(sh).bottom)||10},{passive:true});sh.addEventListener('touchmove',e=>{let dy=sy-e.touches[0].clientY;sh.style.bottom=Math.max(10,Math.min(innerHeight-120,sb+dy))+'px'},{passive:true});fitTR();</script></body></html>";
        map.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);
    }

    private String top10(String j){try{JSONArray a=new JSONArray(j);StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(10,a.length());i++){JSONObject o=a.getJSONObject(i);FaultModel.Nearest f=FaultModel.nearest(o.getDouble("lat"),o.getDouble("lon"));s.append(i+1).append(") ").append(f.name).append("\n   birleşik ").append(String.format(Locale.US,"%.1f",o.getDouble("score"))).append("/100 • 24s ").append(String.format(Locale.US,"%.1f",o.optDouble("q24"))).append(" • güven ").append(String.format(Locale.US,"%.0f%%\n\n",o.optDouble("confidence")));}return s.toString();}catch(Exception e){return "Risk listesi okunamadı.";}}
    private String explain(String j){try{JSONArray a=new JSONArray(j);if(a.length()==0)return "Risk bölgesi yok.";JSONObject o=a.getJSONObject(0);FaultModel.Nearest f=FaultModel.nearest(o.getDouble("lat"),o.getDouble("lon"));return String.format(Locale.US,"%s\nBirleşik %.1f/100 • QIE24 %.1f • 7g %.1f • 30g %.1f\nAktivite %.2fx • b≈%.2f • ETAS %.2f • göç %.0f%% • güven %.0f%%\n\nSkor kısa dönem kümelenme, ETAS-benzeri tetiklenme, b-değeri, mikrodeprem göçü ve fay bağlamının birleşimidir.",f.name,o.getDouble("score"),o.optDouble("q24"),o.optDouble("q7"),o.optDouble("q30"),o.optDouble("rate"),o.optDouble("b"),o.optDouble("etas"),100*o.optDouble("migration"),o.optDouble("confidence"));}catch(Exception e){return "Açıklama hazırlanamadı.";}}
    private void js(String x){if(map!=null)map.evaluateJavascript("window."+x,null);}

    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout hero(){LinearLayout v=card();v.setBackground(gradient(new int[]{Color.rgb(17,34,52),Color.rgb(12,20,34)},GradientDrawable.Orientation.TL_BR));return v;}
    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(15),dp(14),dp(15),dp(14));v.setBackground(round(CARD,18,Color.rgb(37,58,78)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));v.setLayoutParams(p);return v;}
    private TextView metric(String a,String b,int c){TextView v=t(a+"\n"+b,11,c);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT_BOLD);v.setBackground(round(Color.rgb(10,20,32),13,Color.rgb(30,53,72)));v.setPadding(dp(5),dp(7),dp(5),dp(7));return v;}
    private TextView pill(String s,int tc,int bg){TextView v=t(s,11,tc);v.setPadding(dp(10),dp(5),dp(10),dp(5));v.setBackground(round(bg,14,Color.TRANSPARENT));return v;}
    private TextView label(String s){TextView v=t(s,11,CYAN);v.setTypeface(Typeface.DEFAULT_BOLD);v.setPadding(0,0,0,dp(7));return v;}
    private TextView value(String a,String b){TextView v=t(a+"                                      "+b,12,TEXT);v.setPadding(0,dp(8),0,0);return v;}
    private TextView note(String s){TextView v=t(s,12,Color.rgb(255,193,118));v.setPadding(dp(4),dp(8),dp(4),dp(8));return v;}
    private Button navButton(String s){Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setPadding(0,0,0,0);return b;}
    private Button accent(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(11);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT_BOLD);b.setBackground(round(Color.argb(55,Color.red(c),Color.green(c),Color.blue(c)),15,c));b.setMinHeight(dp(52));return b;}
    private SeekBar seek(int p,int max){SeekBar s=new SeekBar(this);s.setMax(max);s.setProgress(Math.max(0,Math.min(max,p)));s.setMinHeight(dp(38));return s;}
    private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setLineSpacing(dp(2),1f);return v;}
    private View gap(int w){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(dp(w),1));return v;}
    private LinearLayout.LayoutParams one(){return new LinearLayout.LayoutParams(0,-2,1);}
    private GradientDrawable round(int color,int r,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));if(stroke!=Color.TRANSPARENT)g.setStroke(dp(1),stroke);return g;}
    private GradientDrawable gradient(int[] colors,GradientDrawable.Orientation o){GradientDrawable g=new GradientDrawable(o,colors);g.setCornerRadius(0);return g;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    private interface Prog{void set(int p);}private SeekBar.OnSeekBarChangeListener listener(Prog p){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean f){p.set(v);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
}
