package com.mgai.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CoreHealthActivity extends Activity {
    private static final String PREFS = "mg_ai_v02";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_MODEL = "model";

    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(244, 246, 248));

        TextView title = new TextView(this);
        title.setText("MG-Core Durum Testi");
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("Bu test /v1/models üzerinden sunucu erişimini ve seçili modelin gerçekten sunulup sunulmadığını doğrular.");
        note.setTextSize(14);
        note.setPadding(0, dp(8), 0, dp(18));
        root.addView(note);

        result = new TextView(this);
        result.setText("Henüz test edilmedi.");
        result.setTextSize(16);
        result.setPadding(dp(14), dp(14), dp(14), dp(14));
        result.setBackgroundColor(Color.WHITE);
        root.addView(result);

        Button test = new Button(this);
        test.setText("Bağlantıyı Test Et");
        test.setAllCaps(false);
        test.setOnClickListener(v -> runTest(test));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(16), 0, 0);
        root.addView(test, lp);

        setContentView(root);
    }

    private void runTest(Button button) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String endpoint = prefs.getString(KEY_ENDPOINT, "").trim();
        String model = prefs.getString(KEY_MODEL, "").trim();
        if (endpoint.isEmpty() || model.isEmpty()) {
            result.setText("Önce Sohbet > Ayarlar bölümünde MG-Core endpoint ve model adını gir.");
            result.setTextColor(Color.rgb(180, 95, 35));
            return;
        }
        result.setText("Test ediliyor…\n" + endpoint + "\n" + model);
        result.setTextColor(Color.rgb(70, 90, 180));
        button.setEnabled(false);

        // API key is deliberately not persisted by the chat UI. For authenticated remote endpoints,
        // this health screen will test unauthenticated connectivity; session-key testing remains in chat.
        ModelClient.testConnection(endpoint, model, "", new ModelClient.Callback() {
            @Override public void onSuccess(String text) {
                runOnUiThread(() -> {
                    result.setText("BAŞARILI\n" + text);
                    result.setTextColor(Color.rgb(30, 130, 80));
                    button.setEnabled(true);
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    result.setText("BAŞARISIZ\n" + message);
                    result.setTextColor(Color.rgb(185, 55, 55));
                    button.setEnabled(true);
                });
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
