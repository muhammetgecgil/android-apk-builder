package com.mg.quakewatch;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TurkeyLabActivity extends Activity {
    private TextView status, report;
    private WebView map;

    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();analyze();}

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(12),dp(12),dp(16));root.setBackgroundColor(Color.rgb(7,12,20));
        TextView title=t("TÜRKİYE JEOLOJİ LAB",26,Color.WHITE);title.setTypeface(null,1);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=t("Mikrodeprem görünümü • sürükle/yakınlaştır • anomali katmanı • profesör yorumu",13,Color.rgb(150,172,202));sub.setGravity(Gravity.CENTER);sub.setPadding(0,dp(4),0,dp(8));root.addView(sub);

        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh=button("YENİLE",Color.rgb(35,118,255));Button fit=button("TÜRKİYE'YE DÖN",Color.rgb(64,79,105));Button micro=button("MİKRO OLAYLAR",Color.rgb(0,155,105));
        bar.addView(refresh,new LinearLayout.LayoutParams(0,dp(46),1));bar.addView(fit,new LinearLayout.LayoutParams(0,dp(46),1));bar.addView(micro,new LinearLayout.LayoutParams(0,dp(46),1));root.addView(bar);
        status=t("● Hazır",14,Color.rgb(88,230,170));status.setPadding(0,dp(8),0,dp(8));root.addView(status);

        map=new WebView(this); WebSettings ws=map.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setBuiltInZoomControls(false);ws.setDisplayZoomControls(false);map.setBackgroundColor(Color.rgb(8,13,22));
        root.addView(map,new LinearLayout.LayoutParams(-1,dp(560)));

        ScrollView s=new ScrollView(this);report=t("Analiz hazırlanıyor...",14,Color.WHITE);report.setPadding(dp(4),dp(10),dp(4),dp(18));s.addView(report);root.addView(s,new LinearLayout.LayoutParams(-1,0,1));
        refresh.setOnClickListener(v->analyze());fit.setOnClickListener(v->map.evaluateJavascript("window.fitTR&&fitTR()",null));micro.setOnClickListener(v->map.evaluateJavascript("window.toggleMicro&&toggleMicro()",null));
        setContentView(root);
    }

    private void analyze(){status.setText("● Türkiye kataloğu indiriliyor...");report.setText("Analiz sürüyor...");new Thread(()->{try{TurkeyAnalyzer.Report r=TurkeyAnalyzer.fetchAndAnalyze();runOnUiThread(()->{status.setText("● Güncel • "+r.eventCount+" katalog olayı • tepe "+String.format(java.util.Locale.US,"%.1f",r.maxScore)+"/100");report.setText(r.text);showMap(r.hotspotsJson,r.eventsJson);});}catch(Exception e){runOnUiThread(()->{status.setText("● Veri hatası");report.setText(e.getClass().getSimpleName()+": "+e.getMessage());});}}).start();}

    private void showMap(String hot,String events){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"+
        "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#m{height:100%;margin:0;background:#09111d}.leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#101b2a;color:#fff}.leaflet-control-layers{font:12px sans-serif}.prof{position:absolute;z-index:9999;left:8px;right:8px;bottom:8px;background:rgba(10,18,30,.94);color:#fff;border:1px solid #31445f;border-radius:14px;padding:10px;font:12px sans-serif;max-height:120px;overflow:auto;touch-action:none}</style></head><body><div id='m'></div><div id='prof' class='prof'>Haritayı parmağınla sürükle; iki parmakla yakınlaştır. Bir olaya dokununca jeolojik yorum burada görünür.</div>"+
        "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"+
        "const m=L.map('m',{zoomControl:true,inertia:true,inertiaDeceleration:2200,worldCopyJump:false,preferCanvas:true,tap:true}).setView([39,35],6);"+
        "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:14,attribution:'© OpenStreetMap'}).addTo(m);"+
        "const hot="+hot+", ev="+events+";const anomaly=L.layerGroup().addTo(m),micro=L.layerGroup().addTo(m);let microOn=true;"+
        "function col(s){return s>=80?'#ff334f':s>=60?'#ff9918':s>=40?'#ffd63d':'#35d17d'}"+
        "hot.forEach(p=>{L.circle([p.lat,p.lon],{radius:9000+450*p.score,color:col(p.score),fillColor:col(p.score),fillOpacity:.18,weight:2}).addTo(anomaly).bindPopup('<b>Anomali '+p.score.toFixed(1)+'/100</b><br>Olay '+p.count+'<br>Oran '+p.rate.toFixed(2)+'x<br>b≈'+p.b.toFixed(2)+'<br>ETAS '+p.etas.toFixed(2)+'<br>Göç '+(p.migration*100).toFixed(0)+'%')});"+
        "function explain(e){let d=e.depth,mx=e.mag,s='M'+mx.toFixed(2)+' • '+e.place+' • '+d.toFixed(1)+' km derinlik. ';if(d<15)s+='Sığ kabuk depremi; yüzeyde hissedilme potansiyeli aynı büyüklükteki derin olaya göre daha yüksektir. ';else if(d<40)s+='Üst-orta kabuk derinliği. ';else s+='Görece derin odak. ';if(mx<2)s+='Mikro/küçük olaydır; tek başına büyük deprem işareti değildir.';else if(mx<4)s+='Küçük-orta olay; kümelenme ve fay geometrisiyle birlikte değerlendirilir.';else s+='Bölgesel olarak dikkat çeken olay; artçı dağılımı ve yakın fay segmentleri izlenmelidir.';document.getElementById('prof').innerHTML=s;}"+
        "ev.forEach(e=>{let rr=e.mag<1?2.2:e.mag<2?3:e.mag<3?4.5:6;let cc=e.mag<1?'#8ab4f8':e.mag<2?'#66d9ef':e.mag<3?'#ffd166':'#ff6b6b';let q=L.circleMarker([e.lat,e.lon],{renderer:L.canvas(),radius:rr,color:cc,fillColor:cc,fillOpacity:.82,weight:1}).addTo(micro);q.on('click',()=>explain(e));q.bindTooltip('M'+e.mag.toFixed(2)+' • '+e.depth.toFixed(1)+' km',{direction:'top'})});"+
        "L.control.layers(null,{'Mikro depremler':micro,'Anomali alanları':anomaly},{collapsed:false}).addTo(m);"+
        "window.fitTR=()=>m.fitBounds([[35.5,25.5],[42.2,45.5]],{padding:[10,10]});window.toggleMicro=()=>{if(microOn){m.removeLayer(micro);microOn=false}else{m.addLayer(micro);microOn=true}};"+
        "let box=document.getElementById('prof'),sy=0,oy=0;box.addEventListener('touchstart',e=>{sy=e.touches[0].clientY;oy=box.offsetTop},{passive:true});box.addEventListener('touchmove',e=>{let dy=e.touches[0].clientY-sy;box.style.bottom='auto';box.style.top=Math.max(5,oy+dy)+'px'},{passive:true});"+
        "</script></body></html>";
        map.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);
    }

    private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(10);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(12));b.setBackground(g);return b;}
    private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
}
