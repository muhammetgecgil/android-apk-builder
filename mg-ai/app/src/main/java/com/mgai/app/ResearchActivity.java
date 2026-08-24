package com.mgai.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class ResearchActivity extends Activity {
    private static final String PREFS = "mg_ai_v02";
    private SharedPreferences prefs;
    private EditText query;
    private TextView output;
    private Button run;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(244,246,248));

        TextView title = new TextView(this);
        title.setText("Internet Research Engine");
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("v0.4 entegrasyon arayüzü • kaynak, güven, çelişki ve provenance sonuçlarını gösterir");
        note.setTextSize(13);
        note.setTextColor(Color.DKGRAY);
        note.setPadding(0, dp(4), 0, dp(14));
        root.addView(note);

        query = new EditText(this);
        query.setHint("Araştırılacak soruyu yaz...");
        query.setMinLines(2);
        query.setMaxLines(5);
        root.addView(query, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        run = new Button(this);
        run.setText("Araştır");
        run.setAllCaps(false);
        run.setOnClickListener(v -> research());
        root.addView(run);

        ScrollView sv = new ScrollView(this);
        output = new TextView(this);
        output.setText("Research server endpoint'i hazır olduğunda sonuçlar burada görünecek.");
        output.setTextSize(15);
        output.setPadding(0, dp(16), 0, dp(16));
        sv.addView(output);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void research() {
        String q = query.getText().toString().trim();
        if (TextUtils.isEmpty(q)) return;
        String endpoint = prefs.getString("research_endpoint", "").trim();
        if (endpoint.isEmpty()) {
            output.setText("Research endpoint henüz ayarlanmadı. Adım 4 backend sözleşmesi hazır; gerçek arama sağlayıcısı bağlanınca bu ekran canlı sonuç gösterecek.\n\nSorgu: " + q);
            return;
        }
        run.setEnabled(false);
        output.setText("Araştırılıyor…");
        ResearchClient.research(endpoint, q, new ResearchClient.Callback() {
            @Override public void onSuccess(JSONObject packet) {
                runOnUiThread(() -> {
                    output.setText(formatPacket(packet));
                    run.setEnabled(true);
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    output.setText("Araştırma hatası: " + message);
                    run.setEnabled(true);
                });
            }
        });
    }

    private String formatPacket(JSONObject p) {
        StringBuilder b = new StringBuilder();
        b.append("Sorgu: ").append(p.optString("query")).append("\n\n");
        JSONArray sources = p.optJSONArray("sources");
        if (sources != null) {
            for (int i=0;i<sources.length();i++) {
                JSONObject s = sources.optJSONObject(i);
                if (s == null) continue;
                b.append(i+1).append(") ").append(s.optString("title")).append("\n");
                b.append(s.optString("url")).append("\n");
                b.append("Güven skoru: ").append(s.optDouble("score",0)).append("\n");
                b.append(s.optString("snippet")).append("\n\n");
            }
        }
        JSONArray c = p.optJSONArray("contradictions");
        b.append("Çelişki sinyali: ").append(c != null && c.length()>0 ? "VAR" : "YOK").append("\n");
        JSONObject prov = p.optJSONObject("provenance");
        if (prov != null) b.append("Bağımsız domain: ").append(prov.optInt("independent_domains",0));
        return b.toString();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
