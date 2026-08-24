package com.mgai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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
    private TextView endpointStatus;
    private Button run;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        updateEndpointStatus();
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
        note.setText("v0.4 • gerçek web arama • kaynak puanı • provenance • kanıta dayalı sentez");
        note.setTextSize(13);
        note.setTextColor(Color.DKGRAY);
        note.setPadding(0, dp(4), 0, dp(10));
        root.addView(note);

        endpointStatus = new TextView(this);
        endpointStatus.setTextSize(12);
        endpointStatus.setTextColor(Color.DKGRAY);
        root.addView(endpointStatus);

        Button settings = new Button(this);
        settings.setText("Research Endpoint Ayarla");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> showEndpointDialog());
        root.addView(settings);

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
        output.setText("Sorgu sonucunda MG-Core sentezi, kaynak listesi, güven puanları ve çelişki sinyalleri burada görünecek.");
        output.setTextSize(15);
        output.setPadding(0, dp(16), 0, dp(16));
        sv.addView(output);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void showEndpointDialog() {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint("http://sunucu:8090/research");
        e.setText(prefs.getString("research_endpoint", ""));
        new AlertDialog.Builder(this)
                .setTitle("Research Engine endpoint")
                .setView(e)
                .setPositiveButton("Kaydet", (d,w) -> {
                    prefs.edit().putString("research_endpoint", e.getText().toString().trim()).apply();
                    updateEndpointStatus();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void updateEndpointStatus() {
        String endpoint = prefs.getString("research_endpoint", "").trim();
        endpointStatus.setText(endpoint.isEmpty() ? "● Research endpoint ayarlanmadı" : "● " + endpoint);
        endpointStatus.setTextColor(endpoint.isEmpty() ? Color.rgb(180,95,35) : Color.rgb(30,130,80));
    }

    private void research() {
        String q = query.getText().toString().trim();
        if (TextUtils.isEmpty(q)) return;
        String endpoint = prefs.getString("research_endpoint", "").trim();
        if (endpoint.isEmpty()) {
            output.setText("Önce Research Endpoint Ayarla bölümünden MG Research server adresini gir.");
            showEndpointDialog();
            return;
        }
        run.setEnabled(false);
        output.setText("Web taranıyor, kaynaklar puanlanıyor ve kanıtlar sentezleniyor…");
        ResearchClient.research(endpoint, q, new ResearchClient.Callback() {
            @Override public void onSuccess(JSONObject packet) {
                runOnUiThread(() -> { output.setText(formatPacket(packet)); run.setEnabled(true); });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> { output.setText("Araştırma hatası: " + message); run.setEnabled(true); });
            }
        });
    }

    private String formatPacket(JSONObject p) {
        StringBuilder b = new StringBuilder();
        b.append("Sorgu: ").append(p.optString("query")).append("\n");
        b.append("Sağlayıcı: ").append(p.optString("provider", "bilinmiyor")).append("\n\n");
        String answer = p.optString("answer", "").trim();
        if (!answer.isEmpty()) b.append("MG-AI SENTEZİ\n").append(answer).append("\n\n");
        if (p.has("synthesis_error")) b.append("Sentez uyarısı: ").append(p.optString("synthesis_error")).append("\n\n");
        JSONArray sources = p.optJSONArray("sources");
        if (sources != null) {
            b.append("KAYNAKLAR\n");
            for (int i=0;i<sources.length();i++) {
                JSONObject s = sources.optJSONObject(i);
                if (s == null) continue;
                b.append(i+1).append(") ").append(s.optString("title")).append("\n");
                b.append(s.optString("url")).append("\n");
                b.append("Güven skoru: ").append(s.optDouble("score",0)).append("\n");
                String snippet = s.optString("snippet");
                if (snippet.length() > 700) snippet = snippet.substring(0,700) + "…";
                b.append(snippet).append("\n\n");
            }
        }
        JSONArray c = p.optJSONArray("contradictions");
        b.append("Çelişki sinyali: ").append(c != null && c.length()>0 ? "VAR — verifier gerekli" : "YOK").append("\n");
        JSONObject prov = p.optJSONObject("provenance");
        if (prov != null) {
            b.append("Kaynak: ").append(prov.optInt("source_count",0));
            b.append(" • Bağımsız domain: ").append(prov.optInt("independent_domains",0));
        }
        return b.toString();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
