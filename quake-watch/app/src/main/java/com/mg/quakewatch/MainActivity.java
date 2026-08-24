package com.mg.quakewatch;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private TextView results;
    private WebView map;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.rgb(16, 20, 28));
        scroll.addView(root);

        TextView title = t("QUAKE WATCH • Dünya Tahmin Haritası", 25, Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        TextView sub = t("Canlı deprem kataloğu + ETAS-benzeri tetiklenme + Gutenberg–Richter b-değeri + kısa dönem oran anomalisi", 14, Color.LTGRAY);
        sub.setPadding(0, 8, 0, 14);
        root.addView(sub);

        status = t("Durum: hazır", 16, Color.rgb(120,220,160));
        root.addView(status);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button analyze = new Button(this); analyze.setText("HARİTAYI HESAPLA");
        Button start = new Button(this); start.setText("SÜREKLİ İZLE");
        Button stop = new Button(this); stop.setText("DURDUR");
        row.addView(analyze, new LinearLayout.LayoutParams(0,-2,1));
        row.addView(start, new LinearLayout.LayoutParams(0,-2,1));
        row.addView(stop, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(row);

        map = new WebView(this);
        WebSettings ws = map.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        map.setBackgroundColor(Color.rgb(16,20,28));
        root.addView(map, new LinearLayout.LayoutParams(-1, dp(420)));
        showEmptyMap();

        TextView legend = t("Harita: yeşil=düşük göreli aktivite • sarı=orta • turuncu=yüksek • kırmızı=çok yüksek. Daire büyüklüğü puanla artar.", 13, Color.LTGRAY);
        legend.setPadding(0, 10, 0, 10);
        root.addView(legend);

        results = t("Haritayı hesapla düğmesine basınca dünya üzerindeki göreli sismik sıcak noktalar sıralanır.", 14, Color.WHITE);
        results.setPadding(0, 14, 0, 20);
        root.addView(results);

        TextView note = t("ÖNEMLİ: 'Tahmin' burada olasılıksal sıcak nokta/anomali anlamındadır. Uygulama deprem için kesin yer-saat-büyüklük vermez ve resmi erken uyarı sistemi değildir. AFAD/USGS/yerel makam uyarıları önceliklidir.", 13, Color.rgb(255,190,120));
        root.addView(note);

        analyze.setOnClickListener(v -> analyzeNow());
        start.setOnClickListener(v -> {
            Intent i = new Intent(this, QuakeMonitorService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            status.setText("Durum: sürekli izleme açık (15 dk aralık)");
        });
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, QuakeMonitorService.class));
            status.setText("Durum: sürekli izleme kapalı");
        });
        setContentView(scroll);
    }

    private void analyzeNow() {
        status.setText("Durum: dünya kataloğu indiriliyor ve hücreler hesaplanıyor...");
        results.setText("Analiz sürüyor...");
        new Thread(() -> {
            try {
                QuakeAnalyzer.Report r = QuakeAnalyzer.fetchAndAnalyze();
                runOnUiThread(() -> {
                    status.setText("Durum: tamamlandı • " + r.eventCount + " olay • tepe puan " + String.format(java.util.Locale.US,"%.1f",r.maxScore));
                    results.setText(r.text);
                    showMap(r.hotspotsJson);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Durum: hata");
                    results.setText(e.getClass().getSimpleName() + ": " + e.getMessage());
                });
            }
        }).start();
    }

    private void showEmptyMap(){ showMap("[]"); }

    private void showMap(String data){
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>"+
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"+
                "<style>html,body,#m{height:100%;margin:0;background:#10141c}.leaflet-control-attribution{font-size:9px}</style></head><body><div id='m'></div>"+
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"+
                "const m=L.map('m',{worldCopyJump:true,minZoom:1}).setView([18,10],1);"+
                "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:8,attribution:'© OpenStreetMap'}).addTo(m);"+
                "const pts="+data+";"+
                "function col(s){return s>=80?'#e53935':s>=60?'#fb8c00':s>=40?'#fdd835':'#43a047'}"+
                "pts.forEach(p=>{let r=5+Math.max(0,p.score)*0.14;L.circleMarker([p.lat,p.lon],{radius:r,color:col(p.score),fillColor:col(p.score),fillOpacity:.48,weight:2}).addTo(m).bindPopup('<b>Göreli tahmin puanı: '+p.score.toFixed(1)+'/100</b><br>24s olay: '+p.count+'<br>Aktivite oranı: '+p.rate.toFixed(2)+'x<br>b≈'+p.b.toFixed(2)+'<br>ETAS: '+p.etas.toFixed(2)+'<br><small>Kesin deprem tahmini değildir.</small>')});"+
                "</script></body></html>";
        map.loadDataWithBaseURL("https://localhost/",html,"text/html","UTF-8",null);
    }

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    private TextView t(String s, int sp, int c) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(c); return v;
    }
}
