package com.mgai.app;

import android.app.Activity;
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

public class MemoryActivity extends Activity {
    private static final String PREFS = "mg_ai_v02";
    private SharedPreferences prefs;
    private EditText endpoint;
    private EditText memoryText;
    private EditText query;
    private TextView output;

    @Override protected void onCreate(Bundle savedInstanceState) {
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
        title.setText("Hafıza & Bilgi");
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("RAG • semantik hafıza • provenance • Knowledge Graph");
        note.setTextSize(13);
        note.setTextColor(Color.DKGRAY);
        note.setPadding(0, dp(4), 0, dp(12));
        root.addView(note);

        endpoint = new EditText(this);
        endpoint.setHint("Memory API base endpoint, ör. http://sunucu:8081");
        endpoint.setText(prefs.getString("memory_endpoint", ""));
        endpoint.setSingleLine(true);
        root.addView(endpoint);

        Button save = new Button(this);
        save.setText("Endpoint'i Kaydet");
        save.setAllCaps(false);
        save.setOnClickListener(v -> {
            prefs.edit().putString("memory_endpoint", endpoint.getText().toString().trim()).apply();
            output.setText("Memory endpoint kaydedildi.");
        });
        root.addView(save);

        memoryText = new EditText(this);
        memoryText.setHint("MG-AI'ın hatırlamasını istediğin metni yaz...");
        memoryText.setMinLines(3);
        memoryText.setMaxLines(8);
        root.addView(memoryText);

        Button add = new Button(this);
        add.setText("Hafızaya Ekle");
        add.setAllCaps(false);
        add.setOnClickListener(v -> ingest());
        root.addView(add);

        query = new EditText(this);
        query.setHint("Hafızada ara...");
        root.addView(query);

        Button search = new Button(this);
        search.setText("Semantik Ara");
        search.setAllCaps(false);
        search.setOnClickListener(v -> search());
        root.addView(search);

        ScrollView sv = new ScrollView(this);
        output = new TextView(this);
        output.setText("Sonuçlar burada gösterilecek.");
        output.setTextSize(14);
        output.setPadding(0, dp(14), 0, dp(14));
        sv.addView(output);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private String base() {
        String e = endpoint.getText().toString().trim();
        if (e.isEmpty()) e = prefs.getString("memory_endpoint", "").trim();
        return e;
    }

    private void ingest() {
        String text = memoryText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        if (base().isEmpty()) { output.setText("Önce Memory API endpoint'ini ayarla."); return; }
        output.setText("Hafızaya ekleniyor…");
        String id = "android-" + System.currentTimeMillis();
        MemoryClient.ingest(base(), id, text, new MemoryClient.Callback() {
            @Override public void onSuccess(JSONObject value) { runOnUiThread(() -> output.setText(value.toString())); }
            @Override public void onError(String message) { runOnUiThread(() -> output.setText("Hata: " + message)); }
        });
    }

    private void search() {
        String q = query.getText().toString().trim();
        if (TextUtils.isEmpty(q)) return;
        if (base().isEmpty()) { output.setText("Önce Memory API endpoint'ini ayarla."); return; }
        output.setText("Hafıza taranıyor…");
        MemoryClient.query(base(), q, new MemoryClient.Callback() {
            @Override public void onSuccess(JSONObject value) { runOnUiThread(() -> output.setText(formatHits(value))); }
            @Override public void onError(String message) { runOnUiThread(() -> output.setText("Hata: " + message)); }
        });
    }

    private String formatHits(JSONObject value) {
        StringBuilder b = new StringBuilder();
        JSONArray hits = value.optJSONArray("hits");
        if (hits == null || hits.length() == 0) return "Sonuç bulunamadı.";
        for (int i=0;i<hits.length();i++) {
            JSONObject h = hits.optJSONObject(i);
            if (h == null) continue;
            b.append(i+1).append(") Skor: ").append(h.optDouble("retrieval_score",0)).append("\n");
            b.append(h.optString("content")).append("\n\n");
        }
        return b.toString();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
