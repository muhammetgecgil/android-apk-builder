package com.mg.quakewatch;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private TextView results;

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
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(Color.rgb(16, 20, 28));
        scroll.addView(root);

        TextView title = t("QUAKE WATCH • Dünya Sismik Analiz", 26, Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        TextView sub = t("USGS FDSN verileri + oran anomalisi + Gutenberg–Richter b-değeri + ETAS-benzeri tetiklenme skoru", 15, Color.LTGRAY);
        sub.setPadding(0, 8, 0, 20);
        root.addView(sub);

        status = t("Durum: hazır", 16, Color.rgb(120,220,160));
        root.addView(status);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button analyze = new Button(this); analyze.setText("ŞİMDİ ANALİZ ET");
        Button start = new Button(this); start.setText("SÜREKLİ İZLE");
        Button stop = new Button(this); stop.setText("DURDUR");
        row.addView(analyze, new LinearLayout.LayoutParams(0,-2,1));
        row.addView(start, new LinearLayout.LayoutParams(0,-2,1));
        row.addView(stop, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(row);

        results = t("Henüz analiz yapılmadı.", 14, Color.WHITE);
        results.setPadding(0, 20, 0, 20);
        root.addView(results);

        TextView note = t("ÖNEMLİ: Bu uygulama depremi kesin olarak tahmin etmez. 'Sıradaki deprem burada olacak' iddiasında bulunmaz. Yalnızca katalogdaki istatistiksel değişimleri ve artçı/tetiklenmiş aktiviteyi puanlar. Uyarılar resmi erken uyarı değildir; AFAD/USGS/yerel makam talimatları önceliklidir.", 13, Color.rgb(255,190,120));
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
        status.setText("Durum: dünya kataloğu indiriliyor...");
        results.setText("Analiz sürüyor...");
        new Thread(() -> {
            try {
                QuakeAnalyzer.Report r = QuakeAnalyzer.fetchAndAnalyze();
                runOnUiThread(() -> {
                    status.setText("Durum: tamamlandı • " + r.eventCount + " olay");
                    results.setText(r.text);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Durum: hata");
                    results.setText(e.getClass().getSimpleName() + ": " + e.getMessage());
                });
            }
        }).start();
    }

    private TextView t(String s, int sp, int c) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(c); return v;
    }
}
