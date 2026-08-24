package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MultimodalActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(244,246,248));

        TextView title = new TextView(this);
        title.setText("Görsel & Ses Analizi");
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("v0.9 multimodal altyapısı: image/audio/sensor/video/document olayları, confidence, freshness, calibration ve provenance ile taşınır. Kamera/mikrofon capture ve gerçek vision/audio model bağlantıları sıradaki alt adımdadır.");
        info.setTextSize(14);
        info.setPadding(0, dp(12), 0, dp(16));
        root.addView(info);

        EditText endpoint = new EditText(this);
        endpoint.setHint("Multimodal API endpoint (örn. http://192.168.1.10:8094)");
        endpoint.setSingleLine(true);
        root.addView(endpoint);

        Button test = new Button(this);
        test.setText("Multimodal Durum Testi");
        test.setAllCaps(false);
        root.addView(test);

        TextView out = new TextView(this);
        out.setText("Henüz test edilmedi.");
        out.setPadding(0, dp(14), 0, 0);
        root.addView(out);

        test.setOnClickListener(v -> {
            String base = endpoint.getText().toString().trim();
            if (base.isEmpty()) { out.setText("Endpoint gir."); return; }
            out.setText("Bağlantı testi çalışıyor...");
            MultimodalClient.health(base, new MultimodalClient.Callback() {
                public void onSuccess(String value) { runOnUiThread(() -> out.setText(value)); }
                public void onError(String error) { runOnUiThread(() -> out.setText("Hata: " + error)); }
            });
        });

        setContentView(root);
    }
    private int dp(int v){ return Math.round(v * getResources().getDisplayMetrics().density); }
}
