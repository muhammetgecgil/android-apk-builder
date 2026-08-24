package com.mgai.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReasoningActivity extends Activity {
    private static final String PREFS = "mg_ai_v02";
    private EditText task;
    private TextView output;
    private Button run;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(18));
        root.setBackgroundColor(Color.rgb(244,246,248));

        TextView title = new TextView(this);
        title.setText("Derin Muhakeme");
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("Plan → Reasoner → Critic → Revizyon → Verifier");
        note.setPadding(0,dp(4),0,dp(12));
        root.addView(note);

        task = new EditText(this);
        task.setHint("Çözülecek problemi yaz...");
        task.setMinLines(3);
        root.addView(task);

        run = new Button(this);
        run.setText("Derin Muhakeme Çalıştır");
        run.setAllCaps(false);
        run.setOnClickListener(v -> execute());
        root.addView(run);

        ScrollView sv = new ScrollView(this);
        output = new TextView(this);
        output.setText("Reasoning endpoint ayarlandığında sonuçlar burada görünecek.");
        output.setTextSize(14);
        output.setPadding(0,dp(14),0,dp(14));
        sv.addView(output);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));
        setContentView(root);
    }

    private void execute() {
        String q = task.getText().toString().trim();
        if (q.isEmpty()) return;
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String endpoint = p.getString("reasoning_endpoint", "").trim();
        if (endpoint.isEmpty()) {
            output.setText("Reasoning endpoint ayarlı değil. Beklenen yol: /v1/reason");
            return;
        }
        run.setEnabled(false);
        output.setText("Muhakeme ediliyor…");
        ReasoningClient.reason(endpoint, q, new ReasoningClient.Callback() {
            @Override public void onSuccess(JSONObject value) {
                runOnUiThread(() -> { output.setText(format(value)); run.setEnabled(true); });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> { output.setText("Hata: " + message); run.setEnabled(true); });
            }
        });
    }

    private String format(JSONObject v) {
        StringBuilder b = new StringBuilder();
        JSONArray plan = v.optJSONArray("plan");
        b.append("PLAN\n");
        if (plan != null) for (int i=0;i<plan.length();i++) b.append(i+1).append(". ").append(plan.optString(i)).append("\n");
        b.append("\nADAY ÇÖZÜM\n").append(v.optString("candidate"));
        b.append("\n\nELEŞTİRİ\n").append(v.optString("critique"));
        b.append("\n\nREVİZE SONUÇ\n").append(v.optString("revision"));
        b.append("\n\nVERIFIER\n").append(v.optString("verifier_report", v.optJSONObject("verification") == null ? "" : v.optJSONObject("verification").toString()));
        b.append("\n\nMod: ").append(v.optString("mode"));
        return b.toString();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
