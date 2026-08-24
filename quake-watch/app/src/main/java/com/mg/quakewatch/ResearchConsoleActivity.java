package com.mg.quakewatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
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
    private LinearLayout body;
    private TextView status, topRisk, lastUpdate, pageTitle;
    private WebView map;
    private TurkeyAnalyzer.Report current;
    private boolean professor=true, mapLarge=false;
    private int timeHours=168;
    private double minMag=-1.0, maxDepth=700;
    private String city="Türkiye";

    @Override public void onCreate(Bundle b){super.onCreate(b);buildShell();showPage("HARİTA");analyzeTurkey();}

    private void buildShell(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(6,11,19));
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(14),dp(14),dp(14),dp(10));head.setBackgroundColor(Color.rgb(11,18,30));
        TextView title=t("QUAKE WATCH • RESEARCH LAB",25,Color.WHITE);title.setTypeface(null,1);head.addView(title);
        TextView sub=t("Tahmin • harita • profesör analizi • Space Watch • denetim • backtest",12,Color.rgb(150,174,205));head.addView(sub);
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);stats.setPadding(0,dp(8),0,0);
        topRisk=t("Türkiye risk: --",12,Color.rgb(255,203,102));lastUpdate=t("Güncelleme: --",12,Color.rgb(148,170,198));status=t("● Hazır",12,Color.rgb(90,232,170));
        stats.addView(topRisk,new LinearLayout.LayoutParams(0,-2,1));stats.addView(lastUpdate,new LinearLayout.LayoutParams(0,-2,1));head.addView(stats);head.addView(status);root.addView(head);

        pageTitle=t("HARİTA",15,Color.WHITE);pageTitle.setTypeface(null,1);pageTitle.setPadding(dp(14),dp(8),dp(14),dp(6));root.addView(pageTitle);
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),0,dp(12),dp(12));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(6),dp(6),dp(6),dp(8));nav.setBackgroundColor(Color.rgb(11,18,30));
        String[] ns={"Harita","Tahmin","Analiz","Denetim","Ayarlar"};String[] ids={"HARİTA","TAHMİN","ANALİZ","DENETİM","AYARLAR"};
        for(int i=0;i<ns.length;i++){final String id=ids[i];Button b=button(ns[i],Color.rgb(40,57,82));b.setOnClickListener(v->showPage(id));nav.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}
        root.addView(nav);setContentView(root);
    }

    private void showPage(String id){pageTitle.setText(id);body.removeAllViews();if("HARİTA".equals(id))buildMapPage();else if("TAHMİN".equals(id))buildForecastPage();else if("ANALİZ".equals(id))buildAnalysisPage();else if("DENETİM".equals(id))buildAuditPage();else buildSettingsPage();}

    private void buildMapPage(){
        LinearLayout quick=row();Button tr=button("TÜRKİYE",Color.rgb(35,118,255));Button mar=button("MARMARA",Color.rgb(184,62,77));Button space=button("SPACE WATCH",Color.rgb(106,79,186));Button three=button("3B KABUK",Color.rgb(65,86,112));
        quick.addView(tr,lp1());quick.addView(mar,lp1());quick.addView(space,lp1());quick.addView(three,lp1());body.addView(quick);
        LinearLayout ctl=card();ctl.addView(t("ZAMAN / BÜYÜKLÜK / DERİNLİK FİLTRESİ",12,Color.rgb(123,158,205)));
        TextView tv=t("Son "+timeHours+" saat",12,Color.WHITE);SeekBar time=new SeekBar(this);time.setMax(167);time.setProgress(timeHours-1);ctl.addView(tv);ctl.addView(time);
        TextView mv=t(String.format(Locale.US,"M≥%.1f",minMag),12,Color.WHITE);SeekBar mag=new SeekBar(this);mag.setMax(70);mag.setProgress((int)Math.round((minMag+1)*10));ctl.addView(mv);ctl.addView(mag);
        TextView dv=t("Derinlik ≤ "+(int)maxDepth+" km",12,Color.WHITE);SeekBar dep=new SeekBar(this);dep.setMax(700);dep.setProgress((int)maxDepth);ctl.addView(dv);ctl.addView(dep);body.addView(ctl);
        LinearLayout actions=row();Button anim=button("▶ ZAMAN ANİMASYONU",Color.rgb(0,145,105));Button reset=button("HARİTAYI SIFIRLA",Color.rgb(71,85,110));Button expand=button("BÜYÜT",Color.rgb(49,65,93));actions.addView(anim,lp1());actions.addView(reset,lp1());actions.addView(expand,lp1());body.addView(actions);
        map=new WebView(this);WebSettings ws=map.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setBuiltInZoomControls(false);ws.setDisplayZoomControls(false);map.setBackgroundColor(Color.rgb(7,12,20));body.addView(map,new LinearLayout.LayoutParams(-1,dp(mapLarge?820:560)));
        if(current!=null)renderMap(current);else showLoadingMap();
        time.setOnSeekBarChangeListener(sb((p)->{timeHours=p+1;tv.setText("Son "+timeHours+" saat");js("setTime("+timeHours+")");}));
        mag.setOnSeekBarChangeListener(sb((p)->{minMag=-1.0+p/10.0;mv.setText(String.format(Locale.US,"M≥%.1f",minMag));js("setMag("+minMag+")");}));
        dep.setOnSeekBarChangeListener(sb((p)->{maxDepth=Math.max(5,p);dv.setText("Derinlik ≤ "+(int)maxDepth+" km");js("setDepth("+maxDepth+")");}));
        tr.setOnClickListener(v->js("fitTR()"));mar.setOnClickListener(v->js("fitMarmara()"));three.setOnClickListener(v->js("toggle3d()"));anim.setOnClickListener(v->js("toggleAnim()"));reset.setOnClickListener(v->js("resetAll()"));
        expand.setOnClickListener(v->{mapLarge=!mapLarge;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)map.getLayoutParams();p.height=dp(mapLarge?820:560);map.setLayoutParams(p);expand.setText(mapLarge?"KÜÇÜLT":"BÜYÜT");});
        space.setOnClickListener(v->showSpaceDialog());
    }

    private void buildForecastPage(){
        LinearLayout c=card();c.addView(t("TOP 10 RİSK BÖLGESİ",14,Color.rgb(255,204,105)));TextView list=t(current==null?"Analiz bekleniyor...":top10(current.hotspotsJson),14,Color.WHITE);c.addView(list);body.addView(c);
        LinearLayout why=card();why.addView(t("NEDEN RİSKLİ?",14,Color.rgb(126,193,255)));why.addView(t(current==null?"Henüz veri yok.":explainTop(current.hotspotsJson),13,Color.WHITE));body.addView(why);
        Button refresh=button("TAHMİNİ ŞİMDİ YENİLE",Color.rgb(35,118,255));refresh.setOnClickListener(v->analyzeTurkey());body.addView(refresh,new LinearLayout.LayoutParams(-1,dp(50)));
        TextView note=t("24 saat / 7 gün / 30 gün değerleri göreli QIE tahmin endeksidir; mutlak deprem olasılığı değildir.",12,Color.rgb(255,190,112));note.setPadding(0,dp(12),0,0);body.addView(note);
    }

    private void buildAnalysisPage(){
        LinearLayout m=card();m.addView(t("PROFESÖR / BASİT MOD",14,Color.WHITE));Switch sw=new Switch(this);sw.setText(professor?"Profesör modu açık":"Basit mod açık");sw.setTextColor(Color.WHITE);sw.setChecked(professor);m.addView(sw);body.addView(m);
        LinearLayout p=card();p.addView(t("JEOLOJİ PROFESÖRÜ YORUMU",14,Color.rgb(126,193,255)));p.addView(t(current==null?"Analiz bekleniyor...":(professor?current.text:basicSummary(current)),13,Color.WHITE));body.addView(p);
        LinearLayout tools=row();Button bt=button("BACKTEST",Color.rgb(0,145,105));Button sp=button("SPACE WATCH",Color.rgb(105,78,183));Button depth=button("DERİNLİK KESİTİ",Color.rgb(76,92,120));tools.addView(bt,lp1());tools.addView(sp,lp1());tools.addView(depth,lp1());body.addView(tools);
        TextView res=t("Araştırma araçları burada sonuçlanır.",13,Color.WHITE);LinearLayout out=card();out.addView(res);body.addView(out);
        bt.setOnClickListener(v->{res.setText("Backtest çalışıyor...");new Thread(()->{try{BacktestEngine.Result r=BacktestEngine.run();runOnUiThread(()->res.setText(r.text));}catch(Exception e){runOnUiThread(()->res.setText("Backtest hatası: "+e.getMessage()));}}).start();});
        sp.setOnClickListener(v->{res.setText("Space Watch verisi alınıyor...");new Thread(()->{try{SatelliteResearchEngine.Report r=SatelliteResearchEngine.fetch();runOnUiThread(()->res.setText(r.text));}catch(Exception e){runOnUiThread(()->res.setText("Space Watch hatası: "+e.getMessage()));}}).start();});
        depth.setOnClickListener(v->{showPage("HARİTA");if(map!=null)map.postDelayed(()->js("startSection()"),500);});
        sw.setOnCheckedChangeListener((b,on)->{professor=on;sw.setText(on?"Profesör modu açık":"Basit mod açık");showPage("ANALİZ");});
    }

    private void buildAuditPage(){
        LinearLayout c=card();c.addView(t("TAHMİN GÜNLÜĞÜ + MODEL PERFORMANSI",14,Color.rgb(255,204,105)));TextView r=t(PredictionAudit.report(this),13,Color.WHITE);c.addView(r);body.addView(c);
        LinearLayout row=row();Button refresh=button("RAPORU YENİLE",Color.rgb(35,118,255));Button verify=button("KATALOGLA DOĞRULA",Color.rgb(0,145,105));row.addView(refresh,lp1());row.addView(verify,lp1());body.addView(row);
        refresh.setOnClickListener(v->r.setText(PredictionAudit.report(this)));verify.setOnClickListener(v->{if(current!=null){PredictionAudit.verifyAgainstCatalog(this,current.eventsJson);r.setText(PredictionAudit.report(this));}});
        TextView note=t("İsabet/kaçırma kayıtları önceden saklanmış tahminlerle karşılaştırılır; sonradan geçmiş tahmin değiştirilmez.",12,Color.rgb(255,190,112));body.addView(note);
    }

    private void buildSettingsPage(){
        SharedPreferences p=getSharedPreferences(PREF,Context.MODE_PRIVATE);LinearLayout c=card();c.addView(t("UYARI MERKEZİ",14,Color.WHITE));
        TextView th=t("Uyarı eşiği: "+p.getInt("threshold",65)+"/100",13,Color.WHITE);SeekBar threshold=new SeekBar(this);threshold.setMax(40);threshold.setProgress(p.getInt("threshold",65)-50);c.addView(th);c.addView(threshold);
        Switch micro=new Switch(this);micro.setText("Mikrodepremleri göster");micro.setTextColor(Color.WHITE);micro.setChecked(p.getBoolean("micro",true));c.addView(micro);body.addView(c);
        LinearLayout cityCard=card();cityCard.addView(t("ŞEHİR TAKİBİ",14,Color.WHITE));Spinner sp=new Spinner(this);String[] cities={"Türkiye","İstanbul","Bursa","İzmir","Ankara","Hatay","Erzincan","Bingöl","Muğla","Manisa"};sp.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cities));cityCard.addView(sp);body.addView(cityCard);
        LinearLayout svc=row();Button on=button("UYARILARI AÇ",Color.rgb(0,145,105));Button off=button("DURDUR",Color.rgb(168,65,79));Button google=button("GOOGLE UYARILARI",Color.rgb(62,79,108));svc.addView(on,lp1());svc.addView(off,lp1());svc.addView(google,lp1());body.addView(svc);
        threshold.setOnSeekBarChangeListener(sb((x)->{int v=50+x;th.setText("Uyarı eşiği: "+v+"/100");p.edit().putInt("threshold",v).apply();}));micro.setOnCheckedChangeListener((b,v)->p.edit().putBoolean("micro",v).apply());
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> a,View v,int pos,long id){city=cities[pos];p.edit().putString("city",city).apply();}public void onNothingSelected(android.widget.AdapterView<?> a){}});
        on.setOnClickListener(v->{Intent i=new Intent(this,QuakeMonitorService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("● Uyarılar açık");});off.setOnClickListener(v->{stopService(new Intent(this,QuakeMonitorService.class));status.setText("● Uyarılar kapalı");});google.setOnClickListener(v->{try{startActivity(new Intent("android.settings.SAFETY_CENTER"));}catch(Exception e){startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}});
        TextView n=t("Deneysel Space Watch termal/TEC kanalları ana sismik skora doğrudan karıştırılmaz. Gerçek InSAR/GNSS akışı bağlı değilse uygulama yapay deformasyon değeri üretmez.",12,Color.rgb(255,190,112));n.setPadding(0,dp(12),0,0);body.addView(n);
    }

    private void analyzeTurkey(){status.setText("● Türkiye QIE analizi çalışıyor...");new Thread(()->{try{TurkeyAnalyzer.Report r=TurkeyAnalyzer.fetchAndAnalyze();PredictionAudit.verifyAgainstCatalog(this,r.eventsJson);PredictionAudit.recordTurkeyForecast(this,r);current=r;runOnUiThread(()->{topRisk.setText(String.format(Locale.US,"Türkiye risk: %.1f/100",r.maxScore));lastUpdate.setText("Güncelleme: "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));status.setText("● "+r.eventCount+" olay • veri güncel");if("HARİTA".contentEquals(pageTitle.getText()))renderMap(r);});}catch(Exception e){runOnUiThread(()->status.setText("● Veri hatası: "+e.getMessage()));}}).start();}

    private String top10(String json){try{JSONArray a=new JSONArray(json);StringBuilder s=new StringBuilder();int n=Math.min(10,a.length());for(int i=0;i<n;i++){JSONObject o=a.getJSONObject(i);FaultModel.Nearest f=FaultModel.nearest(o.getDouble("lat"),o.getDouble("lon"));s.append(i+1).append(") ").append(f.name).append("\n   ").append(String.format(Locale.US,"%.3f, %.3f • birleşik %.1f/100 • QIE24 %.1f • 7g %.1f • güven %.0f%%\n\n",o.getDouble("lat"),o.getDouble("lon"),o.getDouble("score"),o.optDouble("q24"),o.optDouble("q7"),o.optDouble("confidence")));}return s.toString();}catch(Exception e){return "Risk listesi okunamadı.";}}
    private String explainTop(String json){try{JSONArray a=new JSONArray(json);if(a.length()==0)return "Risk bölgesi yok.";JSONObject o=a.getJSONObject(0);FaultModel.Nearest f=FaultModel.nearest(o.getDouble("lat"),o.getDouble("lon"));return String.format(Locale.US,"En yüksek bölge: %s\nBirleşik skor %.1f/100\nQIE 24s %.1f • 7g %.1f • 30g %.1f\nAktivite oranı %.2fx\nb-değeri ≈ %.2f\nETAS %.2f\nGöç %.0f%%\nModel güveni %.0f%%\n\nYorum: skorun yükselmesinde kısa dönem kümelenme, ETAS-benzeri tetiklenme, b-değeri, mikrodeprem göçü ve fay yakınlığı birlikte rol oynar. Tek başına büyük deprem garantisi değildir.",f.name,o.getDouble("score"),o.optDouble("q24"),o.optDouble("q7"),o.optDouble("q30"),o.optDouble("rate"),o.optDouble("b"),o.optDouble("etas"),100*o.optDouble("migration"),o.optDouble("confidence"));}catch(Exception e){return "Açıklama hazırlanamadı.";}}
    private String basicSummary(TurkeyAnalyzer.Report r){return String.format(Locale.US,"Türkiye'de son katalog verisine göre en yüksek göreli aktivite %.1f/100. Haritada kırmızı/turuncu alanlar diğer bölgelere göre daha yüksek kısa dönem anomaliyi gösterir. Kesin deprem tahmini değildir.",r.maxScore);}

    private void showSpaceDialog(){showPage("ANALİZ");}
    private void showLoadingMap(){map.loadData("<html><body style='background:#07101b;color:white;font-family:sans-serif'>Harita verisi yükleniyor...</body></html>","text/html","UTF-8");}
    private void js(String x){if(map!=null)map.evaluateJavascript("window."+x,null);}

    private void renderMap(TurkeyAnalyzer.Report r){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>"+
        "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#m{height:100%;margin:0;background:#07101b;font-family:Arial}.leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#101b2b;color:#fff}.legend{position:absolute;z-index:1000;bottom:8px;left:8px;background:#0b1524e8;color:white;padding:7px;border-radius:9px;font-size:11px}.canvas{display:none;position:absolute;z-index:1300;left:3%;right:3%;top:8%;bottom:8%;background:#07101bee;border:1px solid #536982;border-radius:12px}.canvas canvas{width:100%;height:100%}</style></head><body><div id='m'></div><div class='legend'>Yeşil: düşük • Sarı: orta • Turuncu: yüksek • Kırmızı: çok yüksek</div><div id='three' class='canvas'><canvas id='cv' width='900' height='600'></canvas></div>"+
        "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>const events="+r.eventsJson+",hot="+r.hotspotsJson+",faults="+r.faultsJson+";const m=L.map('m',{dragging:true,touchZoom:true,doubleClickZoom:true,inertia:true,preferCanvas:true}).setView([39,35],5);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:14,attribution:'© OpenStreetMap'}).addTo(m);let hours=168,minMag=-1,maxDepth=700,anim=false,timer=null;const q=L.layerGroup().addTo(m),f=L.layerGroup().addTo(m),h=L.layerGroup().addTo(m);function hc(s){return s>=80?'#ff354f':s>=60?'#ff961a':s>=40?'#ffd53a':'#35d07d'};faults.forEach(x=>L.polyline(x.pts,{color:'#ff6670',weight:3,opacity:.85}).addTo(f).bindPopup('<b>'+x.name+'</b><br>'+x.system+'<br>'+x.type));hot.forEach(x=>L.circle([x.lat,x.lon],{radius:7000+x.score*240,color:hc(x.score),fillColor:hc(x.score),fillOpacity:.16,weight:2}).addTo(h).bindPopup('<b>Göreli risk '+x.score.toFixed(1)+'/100</b><br>QIE24 '+(x.q24||0).toFixed(1)+' • 7g '+(x.q7||0).toFixed(1)+'<br>güven '+(x.confidence||0).toFixed(0)+'%<br>oran '+x.rate.toFixed(2)+'x • b≈'+x.b.toFixed(2)+' • ETAS '+x.etas.toFixed(2)));function color(e){return e.mag>=4?'#ff3b50':e.mag>=2?'#ffb02e':'#47d58d'}function draw(){q.clearLayers();let cut=Date.now()-hours*3600000;events.filter(e=>e.time>=cut&&e.mag>=minMag&&e.depth<=maxDepth).forEach(e=>L.circleMarker([e.lat,e.lon],{radius:Math.max(2.5,3+Math.max(0,e.mag)*1.35),color:color(e),fillColor:color(e),fillOpacity:.8,weight:1}).addTo(q).bindPopup('<b>M'+e.mag.toFixed(2)+'</b> • '+e.depth.toFixed(1)+' km<br>'+e.place+'<br>Yakın fay: '+e.fault+' ~'+e.faultKm.toFixed(0)+' km'));}draw();window.setTime=x=>{hours=x;draw()};window.setMag=x=>{minMag=x;draw()};window.setDepth=x=>{maxDepth=x;draw()};window.fitTR=()=>m.fitBounds([[35.5,25.5],[42.3,45.5]]);window.fitMarmara=()=>m.fitBounds([[39.7,26.2],[41.4,31.1]]);window.resetAll=()=>{hours=168;minMag=-1;maxDepth=700;draw();window.fitTR()};window.toggleAnim=()=>{if(anim){clearInterval(timer);anim=false;return}anim=true;let hh=6;timer=setInterval(()=>{hours=hh;draw();hh+=6;if(hh>168){clearInterval(timer);anim=false;hours=168;draw()}},450)};window.toggle3d=()=>{let d=document.getElementById('three');if(d.style.display==='block'){d.style.display='none';return}d.style.display='block';let c=document.getElementById('cv'),x=c.getContext('2d');x.fillStyle='#07101b';x.fillRect(0,0,c.width,c.height);x.strokeStyle='#52657c';for(let i=0;i<6;i++){let yy=50+i*90;x.beginPath();x.moveTo(45,yy);x.lineTo(860,yy);x.stroke()}events.filter(e=>e.mag>=minMag&&e.depth<=Math.min(150,maxDepth)).forEach(e=>{let px=55+(e.lon-25)/21*790,py=50+Math.min(150,e.depth)/150*450;x.beginPath();x.fillStyle=color(e);x.arc(px,py,Math.max(2,2+e.mag),0,Math.PI*2);x.fill()});x.fillStyle='white';x.font='18px Arial';x.fillText('3B-benzeri kabuk projeksiyonu: boylam × derinlik',50,25)};window.startSection=()=>{alert('Kesit seçimi için ayrıntılı iki-nokta aracı Türkiye Jeoloji Lab ekranında kullanılabilir.');};window.fitTR();</script></body></html>";
        map.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);
    }

    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(14),dp(14),dp(14),dp(14));GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(16,24,38));g.setCornerRadius(dp(16));g.setStroke(dp(1),Color.rgb(38,53,75));v.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));v.setLayoutParams(p);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));v.setLayoutParams(p);return v;}
    private LinearLayout.LayoutParams lp1(){return new LinearLayout.LayoutParams(0,dp(46),1);}
    private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(10);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(12));b.setBackground(g);return b;}
    private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
    private interface P{void set(int p);} private SeekBar.OnSeekBarChangeListener sb(P p){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int x,boolean f){p.set(x);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
}
