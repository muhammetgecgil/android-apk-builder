package com.mgai.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DashboardActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(244, 246, 248));

        TextView title = new TextView(this);
        title.setText("MG-AI");
        title.setTextSize(34);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.rgb(20, 24, 32));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("v0.3 • Requirement-driven AI platform");
        sub.setTextSize(14);
        sub.setTextColor(Color.rgb(90, 97, 110));
        sub.setPadding(0, dp(4), 0, dp(22));
        root.addView(sub);

        TextView summary = new TextView(this);
        summary.setText("Gereksinim entegrasyon durumu\n" + CapabilityRegistry.summary());
        summary.setTextSize(16);
        summary.setTextColor(Color.rgb(35, 40, 50));
        summary.setPadding(dp(14), dp(14), dp(14), dp(14));
        summary.setBackgroundColor(Color.WHITE);
        root.addView(summary, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button chat = button("MG-AI Sohbet");
        chat.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        root.addView(chat);

        Button coreHealth = button("MG-Core Durum Testi");
        coreHealth.setOnClickListener(v -> startActivity(new Intent(this, CoreHealthActivity.class)));
        root.addView(coreHealth);

        Button capabilities = button("Yetenekler & Gereksinimler");
        capabilities.setOnClickListener(v -> startActivity(new Intent(this, CapabilitiesActivity.class)));
        root.addView(capabilities);

        TextView rule = new TextView(this);
        rule.setText("Temel kural: Bir özellik yalnızca kodlandı diye tamamlanmış sayılmaz. Doğrulama kaydı olmadan TESTED/ACTIVE durumuna geçemez. Robotik fiziksel eylemler LLM'den doğrudan aktüatöre gidemez.");
        rule.setTextSize(13);
        rule.setTextColor(Color.rgb(95, 102, 116));
        rule.setPadding(0, dp(22), 0, 0);
        root.addView(rule);

        setContentView(root);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(16), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
