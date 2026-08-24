package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class CapabilitiesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(20));
        root.setBackgroundColor(Color.rgb(244, 246, 248));

        TextView title = text("Yetenekler & Gereksinimler", 26, true, Color.rgb(20,24,32));
        root.addView(title);
        TextView summary = text(CapabilityRegistry.summary(), 14, false, Color.rgb(90,97,110));
        summary.setPadding(0, dp(4), 0, dp(14));
        root.addView(summary);

        for (CapabilityRegistry.Capability c : CapabilityRegistry.all()) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(10));

            card.addView(text(c.id + " • " + stateLabel(c.state), 12, true, stateColor(c.state)));
            card.addView(text(c.name, 17, true, Color.rgb(30,34,42)));
            TextView fam = text("Gereksinim ailesi: " + c.requirementFamily, 12, false, Color.rgb(90,97,110));
            fam.setPadding(0, dp(4), 0, 0);
            card.addView(fam);
            TextView ver = text("Doğrulama: " + c.verification, 12, false, Color.rgb(70,76,88));
            ver.setPadding(0, dp(4), 0, 0);
            card.addView(ver);
            root.addView(card, lp);
        }

        scroll.addView(root);
        setContentView(scroll);
    }

    private String stateLabel(CapabilityRegistry.State s) {
        switch (s) {
            case ACTIVE: return "AKTİF";
            case TESTED: return "TEST EDİLDİ";
            case INTEGRATING: return "ENTEGRASYONDA";
            default: return "PLANLI";
        }
    }

    private int stateColor(CapabilityRegistry.State s) {
        switch (s) {
            case ACTIVE: return Color.rgb(20,135,75);
            case TESTED: return Color.rgb(40,110,190);
            case INTEGRATING: return Color.rgb(190,110,25);
            default: return Color.rgb(110,115,125);
        }
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
