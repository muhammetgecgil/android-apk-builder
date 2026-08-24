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
    private TextView status, results, fusion, magnetic;
    private WebView map;
    private SensorManager sensors;
    private Sensor magSensor;
    private volatile double magneticUt=0;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
        sensors=(SensorManager)getSystemService(SENSOR_SERVICE); magSensor=sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); buildUi();
    }
    @Override protected void onResume(){super.onResume();if(magSensor!=null)sensors.registerListener(this,magSensor,SensorManager.SENSOR_DELAY_NORMAL);}
    @Override protected void onPause(){sensors.unregisterListener(this);super.onPause();}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(18),dp(14),dp(28));root.setBackgroundColor(Color.rgb(8,13,22));scroll.addView(root);
        TextView title=t("QUAKE WATCH FUSION",27,Color.WHITE);title.setGravity(Gravity.CENTER_HORIZONTAL);title.setTypeface(null,1);root.addView(title);
        TextView sub=t("Dünya + Türkiye özel sismik analiz • uzay hava durumu • çevresel füzyon",13,Color.rgb(157,177,204));sub.setGravity(Gravity.CENTER_HORIZONTAL);sub.setPadding(0,dp(4),0,dp(14));root.addView(sub);

        LinearLayout sc=card();status=t("● Sistem hazır",16,Color.rgb(95,235,173));sc.addView(status);magnetic=t("Manyetik sensör: bekleniyor",12,Color.rgb(156,177,205));magnetic.setPadding(0,dp(5),0,0);sc.addView(magnetic);root.addView(sc);

        LinearLayout controls=card();TextView ct=t("ANALİZ MODLARI",12,Color.rgb(122,157,204));ct.setTypeface(null,1);controls.addView(ct);
        LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);r1.setPadding(0,dp(8),0,0);
        Button world=button("DÜNYA",Color.rgb(41,121,255));Button tr=button("TÜRKİYE LAB",Color.rgb(196,55,76));
        r1.addView(world,new LinearLayout.LayoutParams(0,dp(50),1));r1.addView(tr,new LinearLayout.LayoutParams(0,dp(50),1));controls.addView(r1);
        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);r2.setPadding(0,dp(8),0,0);Button start=button("UYARILARI AÇ",Color.rgb(0,168,107));Button stop=button("DURDUR",Color.rgb(92,102,122));r2.addView(start,new LinearLayout.LayoutParams(0,dp(48),1));r2.addView(stop,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(r2);root.addView(controls);

        Button google=button("ANDROID / GOOGLE DEPREM UYARILARI AYARLARI",Color.rgb(67,82,110));google.setOnClickListener(v->openGoogleEarthquakeSettings());root.addView(google,new LinearLayout.LayoutParams(-1,dp(50)));

        LinearLayout mapCard=card();TextView mt=t("OLASILIKSAL SICAK NOKTA HARİTASI",13,Color.WHITE);mt.setTypeface(null,1);mapCard.addView(mt);map=new WebView(this);WebSettings ws=map.getSettings();ws.setJavaScriptEnabled(true);ws.setDomStorageEnabled(true);map.setBackgroundColor(Color.rgb(11,17,28));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(430));mp.setMargins(0,dp(10),0,0);mapCard.addView(map,mp);showMap("[]",false);TextView legend=t("● Yeşil düşük   ● Sarı orta   ● Turuncu yüksek   ● Kırmızı çok yüksek",12,Color.rgb(160,178,202));legend.setPadding(0,dp(8),0,0);mapCard.addView(legend);root.addView(mapCard);

        LinearLayout fc=card();TextView ft=t("ÇEVRESEL + ENDÜSTRİYEL FÜZYON",13,Color.rgb(255,210,115));ft.setTypeface(null,1);fc.addView(ft);fusion=t("Dünya analizinden sonra NOAA Kp, gelgit geometrisi ve telefon manyetometresi bağlamı hesaplanır.",14,Color.WHITE);fusion.setPadding(0,dp(8),0,0);fc.addView(fusion);root.addView(fc);
        LinearLayout rc=card();TextView rt=t("MODEL ÇIKTISI",13,Color.rgb(126,193,255));rt.setTypeface(null,1);rc.addView(rt);results=t("Henüz analiz yapılmadı.",14,Color.WHITE);results.setPadding(0,dp(8),0,0);rc.addView(results);root.addView(rc);
        TextView note=t("Türkiye Lab: Türkiye 34–43°K / 25–46°D alanını 0.5° hücrelerde 7 günlük katalogla analiz eder. 6s/24s hızlanma, ETAS-benzeri tetiklenme, b-değeri ve aktivite göçünü birleştirir. Kesin deprem yeri, zamanı veya büyüklüğü tahmini değildir.",12,Color.rgb(255,191,112));note.setPadding(dp(8),dp(8),dp(8),0);root.addView(note);

        world.setOnClickListener(v->analyzeWorld());tr.setOnClickListener(v->analyzeTurkey());
        start.setOnClickListener(v->{Intent i=new Intent(this,QuakeMonitorService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("● Sürekli izleme ve uyarılar açık");});
        stop.setOnClickListener(v->{stopService(new Intent(this,QuakeMonitorService.class));status.setText("● Sürekli izleme kapalı");});setContentView(scroll);
    }

    private void analyzeWorld(){status.setText("● Dünya kataloğu + çevresel kaynaklar hesaplanıyor...");results.setText("Sismik analiz sürüyor...");fusion.setText("Füzyon verileri bekleniyor...");new Thread(()->{try{QuakeAnalyzer.Report r=QuakeAnalyzer.fetchAndAnalyze();FusionEngine.Context fx;try{fx=FusionEngine.fetch(r.maxScore,magneticUt);}catch(Exception e){fx=null;}FusionEngine.Context ffx=fx;runOnUiThread(()->{status.setText("● Dünya güncel • "+r.eventCount+" olay • tepe "+String.format(java.util.Locale.US,"%.1f",r.maxScore));results.setText(r.text);showMap(r.hotspotsJson,false);fusion.setText(ffx!=null?ffx.text:"NOAA/çevresel veri alınamadı.");});}catch(Exception e){runOnUiThread(()->{status.setText("● Veri hatası");results.setText(e.getClass().getSimpleName()+": "+e.getMessage());});}}).start();}

    private void analyzeTurkey(){status.setText("● Türkiye özel katalog ve göç modeli hesaplanıyor...");results.setText("Türkiye Lab analiz sürüyor...");new Thread(()->{try{TurkeyAnalyzer.Report r=TurkeyAnalyzer.fetchAndAnalyze();runOnUiThread(()->{status.setText("● Türkiye güncel • "+r.eventCount+" olay • tepe "+String.format(java.util.Locale.US,"%.1f",r.maxScore));results.setText(r.text);showMap(r.hotspotsJson,true);fusion.setText("Türkiye Lab sismik puanı çevresel göstergelerden bağımsız tutulur. Manyetizma, Kp ve gelgit deprem öncüsü olarak kabul edilmez.");});}catch(Exception e){runOnUiThread(()->{status.setText("● Türkiye veri hatası");results.setText(e.getClass().getSimpleName()+": "+e.getMessage());});}}).start();}

    private void openGoogleEarthquakeSettings(){try{startActivity(new Intent("android.settings.SAFETY_CENTER"));}catch(Exception e){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}}}
    private void showMap(String data,boolean turkey){String center=turkey?"[39.0,35.0],5":"[18,10],1";String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><style>html,body,#m{height:100%;margin:0;background:#0b111c}.leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#121b2b;color:#fff}</style></head><body><div id='m'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>const m=L.map('m',{worldCopyJump:true}).setView("+center+");L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:10,attribution:'© OpenStreetMap'}).addTo(m);const pts="+data+";function col(s){return s>=80?'#ff3b4d':s>=60?'#ff9417':s>=40?'#ffd43b':'#32d47b'}pts.forEach(p=>{let r=5+Math.max(0,p.score)*.14;let extra=p.migration!==undefined?'<br>Göç: '+(p.migration*100).toFixed(0)+'%':'';L.circleMarker([p.lat,p.lon],{radius:r,color:col(p.score),fillColor:col(p.score),fillOpacity:.55,weight:2}).addTo(m).bindPopup('<b>Anomali '+p.score.toFixed(1)+'/100</b><br>Olay: '+p.count+'<br>Aktivite: '+p.rate.toFixed(2)+'x<br>b≈'+p.b.toFixed(2)+'<br>ETAS: '+p.etas.toFixed(2)+extra+'<br><small>Kesin deprem tahmini değildir.</small>')});</script></body></html>";map.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);}

    private LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(14),dp(14),dp(14),dp(14));GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(17,25,39));g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.rgb(38,53,75));v.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(12));v.setLayoutParams(p);return v;}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(11);b.setAllCaps(false);GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(14));b.setBackground(g);return b;}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}private TextView t(String s,int sp,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);return v;}
    @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){double x=e.values[0],y=e.values[1],z=e.values[2];magneticUt=Math.sqrt(x*x+y*y+z*z);if(magnetic!=null)magnetic.setText(String.format(java.util.Locale.US,"Manyetik sensör: %.1f µT • yerel metal/mıknatıs etkisine duyarlı",magneticUt));}}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
}
