package com.mg.quakewatch;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {
    private TextView status,results,fusion,magnetic;
    private WebView map;
    private SensorManager sensors; private Sensor magSensor; private volatile double magneticUt=0;
    private boolean mapLarge=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
        sensors=(SensorManager)getSystemService(SENSOR_SERVICE);magSensor=sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);buildUi();
    }
    @Override protected void onResume(){super.onResume();if(magSensor!=null)sensors.registerListener(this,magSensor,SensorManager.SENSOR_DELAY_NORMAL);}
    @Override protected void onPause(){sensors.unregisterListener(this);super.onPause();}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(16),dp(12),dp(28));root.setBackgroundColor(Color.rgb(7,12,20));scroll.addView(root);
        TextView title=t("QUAKE WATCH • GEO LAB",27,Color.WHITE);title.setGravity(Gravity.CENTER_HORIZONTAL);title.setTypeface(null,1);root.addView(title);
        TextView sub=t("Türkiye jeoloji laboratuvarı • mikrodeprem • fay segmenti • zaman • derinlik kesiti",13,Color.rgb(155,176,205));sub.setGravity(Gravity.CENTER_HORIZONTAL);sub.setPadding(0,dp(4),0,dp(14));root.addView(sub);

        LinearLayout sc=card();status=t("● Sistem hazır",16,Color.rgb(91,235,171));sc.addView(status);magnetic=t("Manyetik sensör: bekleniyor",12,Color.rgb(156,177,205));magnetic.setPadding(0,dp(5),0,0);sc.addView(magnetic);root.addView(sc);

        LinearLayout controls=card();TextView ct=t("ANALİZ",12,Color.rgb(122,157,204));ct.setTypeface(null,1);controls.addView(ct);
        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);r1.setPadding(0,dp(8),0,0);Button world=button("DÜNYA",Color.rgb(41,121,255));Button tr=button("TÜRKİYE PROFESÖR",Color.rgb(196,55,76));r1.addView(world,new LinearLayout.LayoutParams(0,dp(52),1));r1.addView(tr,new LinearLayout.LayoutParams(0,dp(52),1));controls.addView(r1);
        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);r2.setPadding(0,dp(8),0,0);Button start=button("UYARILARI AÇ",Color.rgb(0,168,107));Button stop=button("DURDUR",Color.rgb(92,102,122));r2.addView(start,new LinearLayout.LayoutParams(0,dp(48),1));r2.addView(stop,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(r2);root.addView(controls);

        Button google=button("ANDROID / GOOGLE DEPREM UYARILARI",Color.rgb(67,82,110));google.setOnClickListener(v->openGoogleEarthquakeSettings());root.addView(google,new LinearLayout.LayoutParams(-1,dp(50)));

        LinearLayout mc=card();TextView mt=t("ETKİLEŞİMLİ HARİTA",13,Color.WHITE);mt.setTypeface(null,1);mc.addView(mt);
        TextView hint=t("Tek parmak sürükle • iki parmak pinch-zoom • çift dokun büyüt • katmanları aç/kapat • zaman sürgüsü • iki nokta ile derinlik kesiti",12,Color.rgb(156,177,205));hint.setPadding(0,dp(5),0,dp(7));mc.addView(hint);
        Button expand=button("HARİTAYI BÜYÜT / KÜÇÜLT",Color.rgb(50,70,105));mc.addView(expand,new LinearLayout.LayoutParams(-1,dp(44)));
        map=new WebView(this);WebSettings ws=map.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);ws.setBuiltInZoomControls(false);ws.setDisplayZoomControls(false);map.setBackgroundColor(Color.rgb(11,17,28));map.setNestedScrollingEnabled(true);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(540));mp.setMargins(0,dp(8),0,0);mc.addView(map,mp);showWorldMap("[]");root.addView(mc);
        expand.setOnClickListener(v->{mapLarge=!mapLarge;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)map.getLayoutParams();p.height=dp(mapLarge?820:540);map.setLayoutParams(p);map.requestLayout();});

        LinearLayout fc=card();TextView ft=t("ÇEVRESEL + ENDÜSTRİYEL FÜZYON",13,Color.rgb(255,210,115));ft.setTypeface(null,1);fc.addView(ft);fusion=t("Dünya analizinden sonra NOAA Kp, gelgit ve manyetometre bağlamı hesaplanır.",14,Color.WHITE);fusion.setPadding(0,dp(8),0,0);fc.addView(fusion);root.addView(fc);
        LinearLayout rc=card();TextView rt=t("JEOLOJİ PROFESÖRÜ ÇIKTISI",13,Color.rgb(126,193,255));rt.setTypeface(null,1);rc.addView(rt);results=t("Türkiye Profesör'e basınca mikrodeprem kataloğu, ana fay bağlamı ve istatistiksel model çalışır.",14,Color.WHITE);results.setPadding(0,dp(8),0,0);rc.addView(results);root.addView(rc);
        TextView note=t("Bilimsel sınır: Fay çizgileri uygulama içindeki sadeleştirilmiş eğitim/analiz geometrisidir; resmi MTA mühendislik fay verisinin yerine geçmez. Harita kaynak katalogda bulunan en küçük depremleri gösterir; ağın algılamadığı olayları üretemez. Kesin deprem yeri, saati veya büyüklüğü tahmini değildir.",12,Color.rgb(255,191,112));note.setPadding(dp(8),dp(8),dp(8),0);root.addView(note);

        world.setOnClickListener(v->analyzeWorld());tr.setOnClickListener(v->analyzeTurkey());
        start.setOnClickListener(v->{Intent i=new Intent(this,QuakeMonitorService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("● Sürekli izleme ve uyarılar açık");});
        stop.setOnClickListener(v->{stopService(new Intent(this,QuakeMonitorService.class));status.setText("● Sürekli izleme kapalı");});setContentView(scroll);
    }

    private void analyzeWorld(){status.setText("● Dünya kataloğu hesaplanıyor...");results.setText("Dünya analizi sürüyor...");new Thread(()->{try{QuakeAnalyzer.Report r=QuakeAnalyzer.fetchAndAnalyze();FusionEngine.Context fx;try{fx=FusionEngine.fetch(r.maxScore,magneticUt);}catch(Exception e){fx=null;}FusionEngine.Context ffx=fx;runOnUiThread(()->{status.setText("● Dünya güncel • "+r.eventCount+" olay • tepe "+String.format(java.util.Locale.US,"%.1f",r.maxScore));results.setText(r.text);showWorldMap(r.hotspotsJson);fusion.setText(ffx!=null?ffx.text:"NOAA/çevresel veri alınamadı.");});}catch(Exception e){runOnUiThread(()->{status.setText("● Veri hatası");results.setText(e.getClass().getSimpleName()+": "+e.getMessage());});}}).start();}

    private void analyzeTurkey(){status.setText("● Türkiye mikrodeprem + fay + göç modeli hesaplanıyor...");results.setText("Türkiye Profesör analizi sürüyor...");new Thread(()->{try{TurkeyAnalyzer.Report r=TurkeyAnalyzer.fetchAndAnalyze();runOnUiThread(()->{status.setText("● Türkiye güncel • "+r.eventCount+" katalog olayı • tepe "+String.format(java.util.Locale.US,"%.1f",r.maxScore));results.setText(r.text);showTurkeyLab(r.hotspotsJson,r.eventsJson,r.faultsJson);fusion.setText("Türkiye modunda sismik puan, jeomanyetizma/gelgit gibi çevresel göstergelerden bağımsız tutulur. Haritada olay → en yakın sadeleştirilmiş ana fay bağlantısı açıklanır.");});}catch(Exception e){runOnUiThread(()->{status.setText("● Türkiye veri hatası");results.setText(e.getClass().getSimpleName()+": "+e.getMessage());});}}).start();}

    private void showWorldMap(String data){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=5,user-scalable=yes'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#m{height:100%;margin:0;background:#0b111c}.leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#121b2b;color:#fff}</style></head><body><div id='m'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>const m=L.map('m',{dragging:true,touchZoom:true,doubleClickZoom:true,scrollWheelZoom:true,zoomControl:true}).setView([18,10],1);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:12,attribution:'© OpenStreetMap'}).addTo(m);const pts="+data+";function col(s){return s>=80?'#ff3b4d':s>=60?'#ff9417':s>=40?'#ffd43b':'#32d47b'}pts.forEach(p=>L.circleMarker([p.lat,p.lon],{radius:5+p.score*.12,color:col(p.score),fillColor:col(p.score),fillOpacity:.55,weight:2}).addTo(m).bindPopup('<b>Anomali '+p.score.toFixed(1)+'/100</b><br>Olay: '+p.count+'<br>Aktivite: '+p.rate.toFixed(2)+'x<br>b≈'+p.b.toFixed(2)+'<br>ETAS: '+p.etas.toFixed(2)));</script></body></html>";
        map.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);
    }

    private void showTurkeyLab(String hotJson,String eventJson,String faultJson){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=5,user-scalable=yes'>"+
        "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#m{height:100%;margin:0;background:#0b111c;font-family:Arial;color:#fff}.leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#111b2c;color:#fff}.ctl{position:absolute;z-index:1000;background:rgba(9,15,25,.94);border:1px solid #33435d;border-radius:12px;padding:7px;box-shadow:0 5px 18px #0008}.top{top:8px;left:8px;right:8px;display:flex;gap:5px;flex-wrap:wrap}.top button{background:#253550;color:#fff;border:0;border-radius:8px;padding:7px 9px;font-weight:700}.top button.on{background:#c53b51}.range{top:58px;left:8px;right:8px;padding:6px 10px}.range input{width:70%}.legend{bottom:8px;left:8px;font-size:11px}.prof{display:none;position:absolute;z-index:1200;left:4%;right:4%;bottom:5%;height:230px;background:#0e1726;border:1px solid #55708f;border-radius:14px;padding:10px}.prof canvas{width:100%;height:165px;background:#09101b;border-radius:8px}.prof button{float:right;background:#a33a4b;color:white;border:0;border-radius:7px;padding:5px 9px}.faultLabel{background:#17243a;color:#ffd27c;border:1px solid #5d7088;border-radius:5px;padding:1px 4px;font-size:10px;white-space:nowrap}</style></head><body><div id='m'></div>"+
        "<div class='ctl top'><button id='bQ' class='on'>Depremler</button><button id='bF' class='on'>Faylar</button><button id='bH'>Anomali</button><button id='bL'>Fay Adları</button><button id='bX'>KESİT SEÇ</button><button id='bR'>Türkiye'ye dön</button></div>"+
        "<div class='ctl range'><span id='th'>Son 168 saat</span> <input id='time' type='range' min='1' max='168' value='168'> <span id='mh'>M≥-1.0</span> <input id='mag' type='range' min='-10' max='60' value='-10'></div>"+
        "<div class='ctl legend'>● mikro M&lt;2 &nbsp; ● M2–4 &nbsp; ● M4+</div><div id='prof' class='prof'><button id='close'>Kapat</button><b id='pt'>Derinlik Kesiti</b><canvas id='cv' width='900' height='330'></canvas></div>"+
        "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"+
        "const events="+eventJson+",hot="+hotJson+",faults="+faultJson+";const m=L.map('m',{dragging:true,touchZoom:true,doubleClickZoom:true,boxZoom:true,keyboard:true,zoomControl:true,inertia:true}).setView([39,35],5);L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:14,attribution:'© OpenStreetMap'}).addTo(m);"+
        "const qg=L.layerGroup().addTo(m),fg=L.layerGroup().addTo(m),hg=L.layerGroup(),lg=L.layerGroup();let hours=168,minMag=-1;function qcol(x){return x>=4?'#ff3951':x>=2?'#ffb029':'#48d890'}function hcol(s){return s>=80?'#ff3b4d':s>=60?'#ff9417':s>=40?'#ffd43b':'#32d47b'}"+
        "faults.forEach(f=>{L.polyline(f.pts,{color:'#ff6b72',weight:3,opacity:.85}).addTo(fg).bindPopup('<b>'+f.name+'</b><br>'+f.system+'<br>'+f.type+'<br><small>Sadeleştirilmiş analiz geometrisi</small>');let p=f.pts[Math.floor(f.pts.length/2)];L.marker(p,{icon:L.divIcon({className:'faultLabel',html:f.name,iconSize:null})}).addTo(lg)});"+
        "hot.forEach(p=>L.circle([p.lat,p.lon],{radius:9000+p.score*240,color:hcol(p.score),fillColor:hcol(p.score),fillOpacity:.18,weight:2}).addTo(hg).bindPopup('<b>Anomali '+p.score.toFixed(1)+'/100</b><br>Olay: '+p.count+'<br>oran '+p.rate.toFixed(2)+'x • b≈'+p.b.toFixed(2)+' • ETAS '+p.etas.toFixed(2)+'<br>Göç '+(p.migration*100).toFixed(0)+'%'));"+
        "function drawQ(){qg.clearLayers();let cut=Date.now()-hours*3600000;events.filter(e=>e.time>=cut&&e.mag>=minMag).forEach(e=>{let r=Math.max(3,3+Math.max(0,e.mag)*1.5);L.circleMarker([e.lat,e.lon],{radius:r,color:qcol(e.mag),fillColor:qcol(e.mag),fillOpacity:.76,weight:1}).addTo(qg).bindPopup('<b>M'+e.mag.toFixed(2)+'</b> • '+e.place+'<br>Derinlik '+e.depth.toFixed(1)+' km<br>En yakın ana segment: <b>'+e.fault+'</b><br>Yaklaşık uzaklık '+e.faultKm.toFixed(1)+' km<br>'+e.faultType+'<br><small>'+new Date(e.time).toLocaleString()+'</small>')})}drawQ();"+
        "function tog(id,layer,onDefault){let b=document.getElementById(id);b.onclick=()=>{if(m.hasLayer(layer)){m.removeLayer(layer);b.classList.remove('on')}else{layer.addTo(m);b.classList.add('on')}}}tog('bQ',qg);tog('bF',fg);tog('bH',hg);tog('bL',lg);document.getElementById('bR').onclick=()=>m.setView([39,35],5);"+
        "document.getElementById('time').oninput=e=>{hours=+e.target.value;document.getElementById('th').innerText='Son '+hours+' saat';drawQ()};document.getElementById('mag').oninput=e=>{minMag=+e.target.value/10;document.getElementById('mh').innerText='M≥'+minMag.toFixed(1);drawQ()};"+
        "let cross=false,a=null,b=null,line=null;document.getElementById('bX').onclick=()=>{cross=true;a=null;b=null;if(line)m.removeLayer(line);document.getElementById('bX').innerText='1. noktayı seç'};m.on('click',e=>{if(!cross)return;if(!a){a=e.latlng;document.getElementById('bX').innerText='2. noktayı seç'}else{b=e.latlng;cross=false;document.getElementById('bX').innerText='KESİT SEÇ';line=L.polyline([a,b],{color:'#5ad7ff',weight:4,dashArray:'7,6'}).addTo(m);profile(a,b)}});"+
        "function hav(x,y){let R=6371,d1=(y.lat-x.lat)*Math.PI/180,d2=(y.lng-x.lng)*Math.PI/180,A=Math.sin(d1/2)**2+Math.cos(x.lat*Math.PI/180)*Math.cos(y.lat*Math.PI/180)*Math.sin(d2/2)**2;return 2*R*Math.asin(Math.sqrt(A))}function proj(p,a,b){let cl=Math.cos(((a.lat+b.lat)/2)*Math.PI/180),ax=a.lng*cl*111,ay=a.lat*111,bx=b.lng*cl*111,by=b.lat*111,px=p.lon*cl*111,py=p.lat*111,dx=bx-ax,dy=by-ay,L=Math.sqrt(dx*dx+dy*dy);if(L<1)return null;let u=((px-ax)*dx+(py-ay)*dy)/(L*L),perp=Math.abs((px-ax)*dy-(py-ay)*dx)/L;return{u:u,x:u*L,perp:perp,L:L}}"+
        "function profile(a,b){let cut=Date.now()-hours*3600000,pts=[];events.forEach(e=>{if(e.time<cut||e.mag<minMag)return;let p=proj(e,a,b);if(p&&p.u>=0&&p.u<=1&&p.perp<=35)pts.push({x:p.x,d:e.depth,m:e.mag});});let P=document.getElementById('prof');P.style.display='block';document.getElementById('pt').innerText='Derinlik Kesiti • ±35 km koridor • '+pts.length+' olay';let c=document.getElementById('cv'),g=c.getContext('2d'),W=c.width,H=c.height;g.fillStyle='#09101b';g.fillRect(0,0,W,H);let maxD=Math.max(30,...pts.map(p=>p.d)),maxX=Math.max(1,hav(a,b));g.strokeStyle='#516987';g.lineWidth=2;g.beginPath();g.moveTo(55,20);g.lineTo(55,H-35);g.lineTo(W-20,H-35);g.stroke();g.fillStyle='#a9bfd8';g.font='20px Arial';g.fillText('0 km',8,27);g.fillText(maxD.toFixed(0)+' km',3,H-40);g.fillText('0',50,H-8);g.fillText(maxX.toFixed(0)+' km',W-105,H-8);pts.forEach(p=>{let x=55+p.x/maxX*(W-85),y=20+p.d/maxD*(H-55);g.beginPath();g.arc(x,y,Math.max(4,4+Math.max(0,p.m)*1.5),0,Math.PI*2);g.fillStyle=qcol(p.m);g.fill()})}document.getElementById('close').onclick=()=>document.getElementById('prof').style.display='none';"+
        "</script></body></html>";
        map.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);
    }

    private void openGoogleEarthquakeSettings(){try{startActivity(new Intent("android.settings.SAFETY_CENTER"));}catch(Exception e){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}}}
    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(14),dp(14),dp(14),dp(14));GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(17,25,39));g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.rgb(38,53,75));v.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(12));v.setLayoutParams(p);return v;}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(11);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(14));b.setBackground(g);return b;}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}
    @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){double x=e.values[0],y=e.values[1],z=e.values[2];magneticUt=Math.sqrt(x*x+y*y+z*z);if(magnetic!=null)magnetic.setText(String.format(java.util.Locale.US,"Manyetik sensör: %.1f µT • yerel metal/mıknatıs etkisine duyarlı",magneticUt));}}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
}
