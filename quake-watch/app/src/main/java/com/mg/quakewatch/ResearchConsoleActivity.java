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
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResearchConsoleActivity extends Activity {
    private static final String PREF="research_console_settings";
    private static final int BG=Color.rgb(5,9,16), PANEL=Color.rgb(12,19,31), CARD=Color.rgb(16,25,40);
    private static final int TEXT=Color.rgb(238,244,255), MUTED=Color.rgb(143,165,194), CYAN=Color.rgb(82,202,255), GOLD=Color.rgb(255,202,101), RED=Color.rgb(255,83,104), GREEN=Color.rgb(74,226,162);
    private LinearLayout body, nav;
    private TextView status, topRisk, lastUpdate, pageTitle, heroRisk;
    private WebView map;
    private TurkeyAnalyzer.Report current;
    private boolean professor=true, mapLarge=false;
    private int timeHours=168;
    private double minMag=-1.0, maxDepth=700;
    private String city="Türkiye", activePage="HARİTA";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(5,9,16));
        getWindow().setNavigationBarColor(Color.rgb(8,14,24));
        buildShell();showPage("HARİTA");analyzeTurkey();
    }

    private void buildShell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=20){
            root.setOnApplyWindowInsetsListener((v,in)->{
                int top=0,bottom=0;
                if(Build.VERSION.SDK_INT>=30){top=in.getInsets(WindowInsets.Type.statusBars()).top;bottom=in.getInsets(WindowInsets.Type.navigationBars()).bottom;}
                else {top=in.getSystemWindowInsetTop();bottom=in.getSystemWindowInsetBottom();}
                v.setPadding(0,top,0,bottom);return in;
            });
            root.requestApplyInsets();
        } else root.setPadding(0,0,0,dp(20));

        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(16),dp(13),dp(16),dp(12));head.setBackground(gradient(new int[]{Color.rgb(12,22,38),Color.rgb(8,14,25)},GradientDrawable.Orientation.TL_BR,0));
        LinearLayout brand=rowNoMargin();
        TextView mark=pill("QW",CYAN,Color.rgb(7,18,30));mark.setGravity(Gravity.CENTER);brand.addView(mark,new LinearLayout.LayoutParams(dp(46),dp(34)));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(10),0,0,0);
        TextView title=t("QUAKE WATCH",22,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);names.addView(title);
        names.addView(t("RESEARCH LAB • QIE",10,MUTED));brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView live=pill("● LIVE",GREEN,Color.rgb(10,35,31));live.setGravity(Gravity.CENTER);brand.addView(live,new LinearLayout.LayoutParams(dp(74),dp(30)));head.addView(brand);
        TextView sub=t("Sismoloji • fay zekâsı • tahmin denetimi • Space Watch",11,Color.rgb(132,157,189));sub.setPadding(0,dp(8),0,0);head.addView(sub);

        LinearLayout stats=rowNoMargin();stats.setPadding(0,dp(10),0,0);
        topRisk=metric("TÜRKİYE", "--", GOLD);lastUpdate=metric("GÜNCELLEME", "--", CYAN);status=metric("SİSTEM", "HAZIR", GREEN);
        stats.addView(topRisk,metricLp());stats.addView(spacer(dp(6)));stats.addView(lastUpdate,metricLp());stats.addView(spacer(dp(6)));stats.addView(status,metricLp());head.addView(stats);root.addView(head);

        LinearLayout sectionHead=new LinearLayout(this);sectionHead.setOrientation(LinearLayout.HORIZONTAL);sectionHead.setGravity(Gravity.CENTER_VERTICAL);sectionHead.setPadding(dp(16),dp(10),dp(16),dp(8));
        pageTitle=t("HARİTA",16,TEXT);pageTitle.setTypeface(Typeface.DEFAULT_BOLD);sectionHead.addView(pageTitle,new LinearLayout.LayoutParams(0,-2,1));
        heroRisk=pill("QIE --",GOLD,Color.rgb(40,31,17));heroRisk.setGravity(Gravity.CENTER);sectionHead.addView(heroRisk,new LinearLayout.LayoutParams(dp(88),dp(30)));root.addView(sectionHead);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),0,dp(12),dp(14));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(8),dp(7),dp(8),dp(9));nav.setBackground(gradient(new int[]{Color.rgb(14,23,38),Color.rgb(8,14,24)},GradientDrawable.Orientation.TOP_BOTTOM,0));
        String[] ns={"⌖\nHarita","◆\nTahmin","◫\nAnaliz","✓\nDenetim","⚙\nAyarlar"};String[] ids={"HARİTA","TAHMİN","ANALİZ","DENETİM","AYARLAR"};
        for(int i=0;i<ns.length;i++){final String id=ids[i];Button b=navButton(ns[i]);b.setTag(id);b.setOnClickListener(v->showPage(id));nav.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));}
        root.addView(nav);setContentView(root);updateNav();
    }

    private void showPage(String id){activePage=id;pageTitle.setText(id);body.removeAllViews();updateNav();if("HARİTA".equals(id))buildMapPage();else if("TAHMİN".equals(id))buildForecastPage();else if("ANALİZ".equals(id))buildAnalysisPage();else if("DENETİM".equals(id))buildAuditPage();else buildSettingsPage();}
    private void updateNav(){if(nav==null)return;for(int i=0;i<nav.getChildCount();i++){View v=nav.getChildAt(i);if(v instanceof Button){Button b=(Button)v;boolean on=activePage.equals(b.getTag());b.setTextColor(on?CYAN:MUTED);b.setBackground(round(on?Color.rgb(20,45,65):Color.TRANSPARENT,14,on?Color.rgb(45,105,140):Color.TRANSPARENT));}}}

    private void buildMapPage(){
        LinearLayout hero=heroCard();LinearLayout hr=rowNoMargin();LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.addView(t("SİSMİK DURUM HARİTASI",15,TEXT));txt.addView(t("Canlı katalog + QIE anomali + fay bağlamı",11,MUTED));hr.addView(txt,new LinearLayout.LayoutParams(0,-2,1));TextView badge=pill("PRO MODE",CYAN,Color.rgb(12,38,55));badge.setGravity(Gravity.CENTER);hr.addView(badge,new LinearLayout.LayoutParams(dp(86),dp(30)));hero.addView(hr);body.addView(hero);

        LinearLayout quick=row();Button tr=accentButton("TÜRKİYE",CYAN);Button mar=accentButton("MARMARA",RED);Button space=accentButton("SPACE",Color.rgb(171,129,255));Button three=accentButton("3B KABUK",Color.rgb(126,152,188));quick.addView(tr,lp1());quick.addView(mar,lp1());quick.addView(space,lp1());quick.addView(three,lp1());body.addView(quick);
        LinearLayout ctl=card();ctl.addView(sectionLabel("AKILLI FİLTRELER"));
        TextView tv=valueLabel("Zaman penceresi","Son "+timeHours+" saat");SeekBar time=seek(timeHours-1,167);ctl.addView(tv);ctl.addView(time);
        TextView mv=valueLabel("Minimum büyüklük",String.format(Locale.US,"M ≥ %.1f",minMag));SeekBar mag=seek((int)Math.round((minMag+1)*10),70);ctl.addView(mv);ctl.addView(mag);
        TextView dv=valueLabel("Maksimum derinlik",(int)maxDepth+" km");SeekBar dep=seek((int)maxDepth,700);ctl.addView(dv);ctl.addView(dep);body.addView(ctl);
        LinearLayout actions=row();Button anim=softButton("▶ ANİMASYON");Button reset=softButton("↺ SIFIRLA");Button expand=softButton("⛶ BÜYÜT");actions.addView(anim,lp1());actions.addView(reset,lp1());actions.addView(expand,lp1());body.addView(actions);
        LinearLayout mapFrame=frame();map=new WebView(this);WebSettings ws=map.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setBuiltInZoomControls(false);ws.setDisplayZoomControls(false);map.setBackgroundColor(Color.rgb(7,12,20));mapFrame.addView(map,new LinearLayout.LayoutParams(-1,dp(mapLarge?820:570)));body.addView(mapFrame);
        if(current!=null)renderMap(current);else showLoadingMap();
        time.setOnSeekBarChangeListener(sb((p)->{timeHours=p+1;tv.setText("Zaman penceresi                                      Son "+timeHours+" saat");js("setTime("+timeHours+")");}));
        mag.setOnSeekBarChangeListener(sb((p)->{minMag=-1.0+p/10.0;mv.setText(String.format(Locale.US,"Minimum büyüklük                                      M ≥ %.1f",minMag));js("setMag("+minMag+")");}));
        dep.setOnSeekBarChangeListener(sb((p)->{maxDepth=Math.max(5,p);dv.setText("Maksimum derinlik                                      "+(int)maxDepth+" km");js("setDepth("+maxDepth+")");}));
        tr.setOnClickListener(v->js("fitTR()"));mar.setOnClickListener(v->js("fitMarmara()"));three.setOnClickListener(v->js("toggle3d()"));anim.setOnClickListener(v->js("toggleAnim()"));reset.setOnClickListener(v->js("resetAll()"));
        expand.setOnClickListener(v->{mapLarge=!mapLarge;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)map.getLayoutParams();p.height=dp(mapLarge?820:570);map.setLayoutParams(p);expand.setText(mapLarge?"▣ KÜÇÜLT":"⛶ BÜYÜT");});space.setOnClickListener(v->showSpaceDialog());
    }

    private void buildForecastPage(){
        LinearLayout hero=heroCard();hero.addView(sectionLabel("QIE FORECAST ENGINE"));hero.addView(t("Modelin şu an en dikkat çekici gördüğü bölgeler",18,TEXT));hero.addView(t("24 saat / 7 gün / 30 gün göreli aktivite endeksleri",11,MUTED));body.addView(hero);
        LinearLayout c=card();c.addView(sectionLabel("TOP 10 RİSK BÖLGESİ"));TextView list=t(current==null?"Analiz bekleniyor...":top10(current.hotspotsJson),14,TEXT);list.setLineSpacing(dp(3),1f);c.addView(list);body.addView(c);
        LinearLayout why=card();why.addView(sectionLabel("NEDEN RİSKLİ?"));why.addView(t(current==null?"Henüz veri yok.":explainTop(current.hotspotsJson),13,TEXT));body.addView(why);
        Button refresh=accentButton("↻ TAHMİNİ ŞİMDİ YENİLE",CYAN);refresh.setOnClickListener(v->analyzeTurkey());body.addView(refresh,new LinearLayout.LayoutParams(-1,dp(54)));
        body.addView(note("QIE değerleri mutlak deprem olasılığı değildir; diğer bölgelerle karşılaştırılan deneysel operasyonel tahmin endeksleridir."));
    }

    private void buildAnalysisPage(){
        LinearLayout mode=card();mode.addView(sectionLabel("ANALİZ DERİNLİĞİ"));Switch sw=new Switch(this);sw.setText(professor?"Profesör modu • tüm parametreler":"Basit mod • özet görünüm");sw.setTextColor(TEXT);sw.setTextSize(14);sw.setChecked(professor);mode.addView(sw);body.addView(mode);
        LinearLayout p=card();p.addView(sectionLabel("JEOLOJİ PROFESÖRÜ YORUMU"));TextView pv=t(current==null?"Analiz bekleniyor...":(professor?current.text:basicSummary(current)),13,TEXT);pv.setLineSpacing(dp(3),1f);p.addView(pv);body.addView(p);
        LinearLayout tools=row();Button bt=accentButton("BACKTEST",GREEN);Button sp=accentButton("SPACE WATCH",Color.rgb(171,129,255));Button depth=accentButton("DERİNLİK",Color.rgb(126,152,188));tools.addView(bt,lp1());tools.addView(sp,lp1());tools.addView(depth,lp1());body.addView(tools);
        TextView res=t("Araştırma aracı seçildiğinde sonuç burada açılır.",13,TEXT);LinearLayout out=card();out.addView(sectionLabel("ARAŞTIRMA ÇIKTISI"));out.addView(res);body.addView(out);
        bt.setOnClickListener(v->{res.setText("Backtest çalışıyor...");new Thread(()->{try{BacktestEngine.Result r=BacktestEngine.run();runOnUiThread(()->res.setText(r.text));}catch(Exception e){runOnUiThread(()->res.setText("Backtest hatası: "+e.getMessage()));}}).start();});
        sp.setOnClickListener(v->{res.setText("Space Watch verisi alınıyor...");new Thread(()->{try{SatelliteResearchEngine.Report r=SatelliteResearchEngine.fetch();runOnUiThread(()->res.setText(r.text));}catch(Exception e){runOnUiThread(()->res.setText("Space Watch hatası: "+e.getMessage()));}}).start();});
        depth.setOnClickListener(v->{showPage("HARİTA");if(map!=null)map.postDelayed(()->js("startSection()"),500);});sw.setOnCheckedChangeListener((b,on)->{professor=on;showPage("ANALİZ");});
    }

    private void buildAuditPage(){
        LinearLayout hero=heroCard();hero.addView(sectionLabel("FORECAST AUDIT"));hero.addView(t("Model ne dedi? Sonra ne oldu?",18,TEXT));hero.addView(t("İsabet, yanlış alarm ve Brier skoru kayıt altında",11,MUTED));body.addView(hero);
        LinearLayout c=card();TextView r=t(PredictionAudit.report(this),13,TEXT);r.setLineSpacing(dp(3),1f);c.addView(r);body.addView(c);
        LinearLayout rr=row();Button refresh=accentButton("RAPORU YENİLE",CYAN);Button verify=accentButton("KATALOGLA DOĞRULA",GREEN);rr.addView(refresh,lp1());rr.addView(verify,lp1());body.addView(rr);
        refresh.setOnClickListener(v->r.setText(PredictionAudit.report(this)));verify.setOnClickListener(v->{if(current!=null){PredictionAudit.verifyAgainstCatalog(this,current.eventsJson);r.setText(PredictionAudit.report(this));}});body.addView(note("Geçmiş tahminler sonradan değiştirilmez. Performans yalnız önceden kaydedilmiş tahminlerle ölçülür."));
    }

    private void buildSettingsPage(){
        SharedPreferences p=getSharedPreferences(PREF,Context.MODE_PRIVATE);LinearLayout c=card();c.addView(sectionLabel("UYARI MERKEZİ"));
        TextView th=valueLabel("Risk uyarı eşiği",p.getInt("threshold",65)+" / 100");SeekBar threshold=seek(p.getInt("threshold",65)-50,40);c.addView(th);c.addView(threshold);
        Switch micro=new Switch(this);micro.setText("Mikrodepremleri haritada göster");micro.setTextColor(TEXT);micro.setChecked(p.getBoolean("micro",true));c.addView(micro);body.addView(c);
        LinearLayout cityCard=card();cityCard.addView(sectionLabel("ŞEHİR TAKİBİ"));Spinner sp=new Spinner(this);String[] cities={"Türkiye","İstanbul","Bursa","İzmir","Ankara","Hatay","Erzincan","Bingöl","Muğla","Manisa"};ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cities);sp.setAdapter(ad);cityCard.addView(sp);body.addView(cityCard);
        Button on=accentButton("● SÜREKLİ UYARILARI AÇ",GREEN);Button off=accentButton("■ İZLEMEYİ DURDUR",RED);Button google=softButton("ANDROID / GOOGLE DEPREM UYARILARI");body.addView(on,new LinearLayout.LayoutParams(-1,dp(54)));body.addView(spacer(dp(8)));body.addView(off,new LinearLayout.LayoutParams(-1,dp(54)));body.addView(spacer(dp(8)));body.addView(google,new LinearLayout.LayoutParams(-1,dp(52)));
        threshold.setOnSeekBarChangeListener(sb((x)->{int v=50+x;th.setText("Risk uyarı eşiği                                      "+v+" / 100");p.edit().putInt("threshold",v).apply();}));micro.setOnCheckedChangeListener((b,v)->p.edit().putBoolean("micro",v).apply());
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> a,View v,int pos,long id){city=cities[pos];p.edit().putString("city",city).apply();}public void onNothingSelected(android.widget.AdapterView<?> a){}});
        on.setOnClickListener(v->{Intent i=new Intent(this,QuakeMonitorService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("SİSTEM\nUYARILAR AÇIK");});off.setOnClickListener(v->{stopService(new Intent(this,QuakeMonitorService.class));status.setText("SİSTEM\nDURDU");});google.setOnClickListener(v->{try{startActivity(new Intent("android.settings.SAFETY_CENTER"));}catch(Exception e){startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}});body.addView(note("Space Watch termal/TEC kanalları ana sismik skora doğrudan karıştırılmaz. Gerçek InSAR/GNSS akışı yoksa yapay deformasyon değeri üretilmez."));
    }

    private void analyzeTurkey(){status.setText("SİSTEM\nANALİZ...");new Thread(()->{try{TurkeyAnalyzer.Report r=TurkeyAnalyzer.fetchAndAnalyze();PredictionAudit.verifyAgainstCatalog(this,r.eventsJson);PredictionAudit.recordTurkeyForecast(this,r);current=r;runOnUiThread(()->{topRisk.setText(String.format(Locale.US,"TÜRKİYE\n%.1f / 100",r.maxScore));lastUpdate.setText("GÜNCELLEME\n"+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));status.setText("SİSTEM\n"+r.eventCount+" OLAY");heroRisk.setText(String.format(Locale.US,"QIE %.0f",r.maxScore));if("HARİTA".equals(activePage))renderMap(r);});}catch(Exception e){runOnUiThread(()->status.setText("SİSTEM\nVERİ HATASI"));}}).start();}

    private String top10(String json){try{JSONArray a=new JSONArray(json);StringBuilder s=new StringBuilder();int n=Math.min(10,a.length());for(int i=0;i<n;i++){JSONObject o=a.getJSONObject(i);FaultModel.Nearest f=FaultModel.nearest(o.getDouble("lat"),o.getDouble("lon"));s.append(i+1).append("  ").append(f.name).append("\n    ").append(String.format(Locale.US,"QIE24 %.1f  •  7g %.1f  •  skor %.1f  •  güven %.0f%%\n\n",o.optDouble("q24"),o.optDouble("q7"),o.getDouble("score"),o.optDouble("confidence")));}return s.toString();}catch(Exception e){return "Risk listesi okunamadı.";}}
    private String explainTop(String json){try{JSONArray a=new JSONArray(json);if(a.length()==0)return "Risk bölgesi yok.";JSONObject o=a.getJSONObject(0);FaultModel.Nearest f=FaultModel.nearest(o.getDouble("lat"),o.getDouble("lon"));return String.format(Locale.US,"%s\n\nBirleşik skor   %.1f / 100\nQIE 24 saat    %.1f\nQIE 7 gün      %.1f\nQIE 30 gün     %.1f\nAktivite oranı %.2fx\nb-değeri       %.2f\nETAS           %.2f\nGöç            %.0f%%\nModel güveni   %.0f%%\n\nSkor; kısa dönem kümelenme, ETAS-benzeri tetiklenme, b-değeri, mikrodeprem göçü ve fay yakınlığının birlikte değerlendirilmesiyle yükselir.",f.name,o.getDouble("score"),o.optDouble("q24"),o.optDouble("q7"),o.optDouble("q30"),o.optDouble("rate"),o.optDouble("b"),o.optDouble("etas"),100*o.optDouble("migration"),o.optDouble("confidence"));}catch(Exception e){return "Açıklama hazırlanamadı.";}}
    private String basicSummary(TurkeyAnalyzer.Report r){return String.format(Locale.US,"Türkiye'de son katalog verisine göre en yüksek göreli aktivite %.1f/100. Renkli alanlar diğer bölgelere göre kısa dönem anomalinin yoğunluğunu gösterir. Kesin deprem tahmini değildir.",r.maxScore);}
    private void showSpaceDialog(){showPage("ANALİZ");}
    private void showLoadingMap(){map.loadData("<html><body style='background:#07101b;color:#dce9ff;font-family:sans-serif;padding:24px'>Harita verisi yükleniyor...</body></html>","text/html","UTF-8");}
    private void js(String x){if(map!=null)map.evaluateJavascript("window."+x,null);}

    private void renderMap(TurkeyAnalyzer.Report r){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#m{height:100%;margin:0;background:#07101b;font-family:Arial}.leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#101b2b;color:#fff}.legend{position:absolute;z-index:1000;bottom:12px;left:10px;background:#091524e8;color:#e9f2ff;padding:8px 10px;border:1px solid #30445e;border-radius:12px;font-size:11px;box-shadow:0 8px 24px #0008}.canvas{display:none;position:absolute;z-index:1300;left:3%;right:3%;top:6%;bottom:6%;background:#07101bee;border:1px solid #536982;border-radius:16px;overflow:hidden}.canvas canvas{width:100%;height:100%}</style></head><body><div id='m'></div><div class='legend'>● düşük &nbsp; ● orta &nbsp; ● yüksek &nbsp; ● çok yüksek</div><div id='three' class='canvas'><canvas id='cv' width='900' height='600'></canvas></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>const events="+r.eventsJson+",hot="+r.hotspotsJson+",faults="+r.faultsJson+";const m=L.map('m',{dragging:true,touchZoom:true,doubleClickZoom:true,inertia:true,preferCanvas:true,zoomControl:true}).setView([39,35],5);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:14,attribution:'© OSM'}).addTo(m);let hours=168,minMag=-1,maxDepth=700,anim=false,timer=null;const q=L.layerGroup().addTo(m),f=L.layerGroup().addTo(m),h=L.layerGroup().addTo(m);function hc(s){return s>=80?'#ff4760':s>=60?'#ff9d28':s>=40?'#ffd85e':'#46dca0'};faults.forEach(x=>L.polyline(x.pts,{color:'#ff6878',weight:3,opacity:.9}).addTo(f).bindPopup('<b>'+x.name+'</b><br>'+x.system+'<br>'+x.type));hot.forEach(x=>L.circle([x.lat,x.lon],{radius:7000+x.score*240,color:hc(x.score),fillColor:hc(x.score),fillOpacity:.18,weight:2}).addTo(h).bindPopup('<b>QIE '+x.score.toFixed(1)+'/100</b><br>24s '+(x.q24||0).toFixed(1)+' • 7g '+(x.q7||0).toFixed(1)+'<br>güven '+(x.confidence||0).toFixed(0)+'%<br>oran '+x.rate.toFixed(2)+'x • b≈'+x.b.toFixed(2)+' • ETAS '+x.etas.toFixed(2)));function color(e){return e.mag>=4?'#ff4862':e.mag>=2?'#ffba42':'#55e2aa'}function draw(){q.clearLayers();let cut=Date.now()-hours*3600000;events.filter(e=>e.time>=cut&&e.mag>=minMag&&e.depth<=maxDepth).forEach(e=>L.circleMarker([e.lat,e.lon],{radius:Math.max(2.5,3+Math.max(0,e.mag)*1.35),color:color(e),fillColor:color(e),fillOpacity:.86,weight:1}).addTo(q).bindPopup('<b>M'+e.mag.toFixed(2)+'</b> • '+e.depth.toFixed(1)+' km<br>'+e.place+'<br>Yakın fay: '+e.fault+' ~'+e.faultKm.toFixed(0)+' km'));}draw();window.setTime=x=>{hours=x;draw()};window.setMag=x=>{minMag=x;draw()};window.setDepth=x=>{maxDepth=x;draw()};window.fitTR=()=>m.fitBounds([[35.5,25.5],[42.3,45.5]]);window.fitMarmara=()=>m.fitBounds([[39.7,26.2],[41.4,31.1]]);window.resetAll=()=>{hours=168;minMag=-1;maxDepth=700;draw();window.fitTR()};window.toggleAnim=()=>{if(anim){clearInterval(timer);anim=false;return}anim=true;let hh=6;timer=setInterval(()=>{hours=hh;draw();hh+=6;if(hh>168){clearInterval(timer);anim=false;hours=168;draw()}},450)};window.toggle3d=()=>{let d=document.getElementById('three');if(d.style.display==='block'){d.style.display='none';return}d.style.display='block';let c=document.getElementById('cv'),x=c.getContext('2d');x.fillStyle='#07101b';x.fillRect(0,0,c.width,c.height);x.strokeStyle='#36516c';for(let i=0;i<6;i++){let yy=50+i*90;x.beginPath();x.moveTo(45,yy);x.lineTo(860,yy);x.stroke()}events.filter(e=>e.mag>=minMag&&e.depth<=Math.min(150,maxDepth)).forEach(e=>{let px=55+(e.lon-25)/21*790,py=50+Math.min(150,e.depth)/150*450;x.beginPath();x.fillStyle=color(e);x.arc(px,py,Math.max(2,2+e.mag),0,Math.PI*2);x.fill()});x.fillStyle='#e9f2ff';x.font='18px Arial';x.fillText('Kabuk projeksiyonu • boylam × derinlik',50,25)};window.startSection=()=>alert('İki noktalı ayrıntılı kesit Türkiye Jeoloji Lab aracında kullanılabilir.');window.fitTR();</script></body></html>";
        map.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);
    }

    private LinearLayout heroCard(){LinearLayout v=card();v.setBackground(gradient(new int[]{Color.rgb(20,37,57),Color.rgb(13,23,37)},GradientDrawable.Orientation.TL_BR,18));return v;}
    private LinearLayout frame(){LinearLayout v=new LinearLayout(this);v.setPadding(dp(2),dp(2),dp(2),dp(2));v.setBackground(round(Color.rgb(9,16,27),18,Color.rgb(42,70,98)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));v.setLayoutParams(p);return v;}
    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(15),dp(15),dp(15),dp(15));v.setBackground(round(CARD,18,Color.rgb(37,55,78)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));v.setLayoutParams(p);return v;}
    private LinearLayout row(){LinearLayout v=rowNoMargin();LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(9));v.setLayoutParams(p);return v;}
    private LinearLayout rowNoMargin(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout.LayoutParams lp1(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.setMargins(dp(2),0,dp(2),0);return p;}
    private LinearLayout.LayoutParams metricLp(){return new LinearLayout.LayoutParams(0,dp(54),1);}
    private TextView metric(String a,String b,int accent){TextView v=t(a+"\n"+b,10,accent);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT_BOLD);v.setBackground(round(Color.rgb(10,17,29),13,Color.rgb(33,52,74)));return v;}
    private TextView sectionLabel(String s){TextView v=t(s,10,CYAN);v.setTypeface(Typeface.DEFAULT_BOLD);v.setLetterSpacing(.12f);v.setPadding(0,0,0,dp(8));return v;}
    private TextView valueLabel(String a,String b){TextView v=t(a+"                                      "+b,12,TEXT);v.setPadding(0,dp(8),0,0);return v;}
    private TextView note(String s){TextView v=t(s,12,Color.rgb(255,197,121));v.setPadding(dp(4),dp(10),dp(4),dp(12));return v;}
    private TextView pill(String s,int textColor,int bg){TextView v=t(s,10,textColor);v.setTypeface(Typeface.DEFAULT_BOLD);v.setPadding(dp(10),dp(4),dp(10),dp(4));v.setBackground(round(bg,30,Color.TRANSPARENT));return v;}
    private Button accentButton(String s,int accent){Button b=buttonBase(s);b.setTextColor(Color.WHITE);b.setBackground(gradient(new int[]{accent,dim(accent,.68f)},GradientDrawable.Orientation.TL_BR,14));return b;}
    private Button softButton(String s){Button b=buttonBase(s);b.setTextColor(Color.rgb(205,220,241));b.setBackground(round(Color.rgb(28,42,61),14,Color.rgb(48,68,94)));return b;}
    private Button navButton(String s){Button b=buttonBase(s);b.setTextSize(9);b.setGravity(Gravity.CENTER);b.setPadding(0,dp(2),0,dp(2));return b;}
    private Button buttonBase(String s){Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT_BOLD);b.setPadding(dp(5),0,dp(5),0);b.setMinHeight(0);b.setMinWidth(0);return b;}
    private SeekBar seek(int p,int max){SeekBar s=new SeekBar(this);s.setMax(max);s.setProgress(p);s.setPadding(0,0,0,0);return s;}
    private GradientDrawable gradient(int[] colors,GradientDrawable.Orientation o,int radius){GradientDrawable g=new GradientDrawable(o,colors);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable round(int color,int radius,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)g.setStroke(dp(1),stroke);return g;}
    private int dim(int color,float f){return Color.rgb((int)(Color.red(color)*f),(int)(Color.green(color)*f),(int)(Color.blue(color)*f));}
    private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}
    private View spacer(int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(dp(h),dp(h)));return v;}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
    private interface P{void set(int p);} private SeekBar.OnSeekBarChangeListener sb(P p){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int x,boolean f){p.set(x);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
}
