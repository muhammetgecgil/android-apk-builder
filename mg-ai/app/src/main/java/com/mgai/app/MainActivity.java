package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String PREFS = "mg_ai_v01";
    private static final String KEY_HISTORY = "history";
    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        restoreHistory();
        if (messages.getChildCount() == 0) {
            addMessage("MG-AI", "MG-AI v0.1 çekirdeği hazır. Bu ilk sürüm sohbet arayüzü ve yerel konuşma hafızasını doğrular. Sonraki adımda gerçek model/araştırma motorunu bağlayacağız.", false, false);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(14));
        root.setBackgroundColor(Color.rgb(244, 246, 248));

        TextView title = new TextView(this);
        title.setText("MG-AI");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(20, 24, 32));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("v0.1 • Core bootstrap • Local memory");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.rgb(90, 97, 110));
        subtitle.setPadding(0, dp(2), 0, dp(12));
        root.addView(subtitle);

        scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, 0, 0, dp(10));
        scroll.addView(messages, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scroll, scrollLp);

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.BOTTOM);

        input = new EditText(this);
        input.setHint("MG-AI'a yaz...");
        input.setMinLines(1);
        input.setMaxLines(5);
        input.setTextSize(16);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        composer.addView(input, inputLp);

        Button send = new Button(this);
        send.setText("Gönder");
        send.setAllCaps(false);
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        sendLp.setMargins(dp(8), 0, 0, 0);
        composer.addView(send, sendLp);
        root.addView(composer);

        send.setOnClickListener(v -> sendMessage());
        input.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });

        setContentView(root);
    }

    private void sendMessage() {
        String text = input.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        input.setText("");
        addMessage("Sen", text, true, true);
        hideKeyboard();

        // Step 1 deliberately uses a deterministic local bootstrap engine.
        // It proves the app/UI/memory path before any external AI dependency is introduced.
        String response = LocalBootstrapEngine.reply(text);
        addMessage("MG-AI", response, false, true);
    }

    private void addMessage(String who, String text, boolean user, boolean persist) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(9), dp(12), dp(9));
        card.setBackgroundColor(user ? Color.rgb(229, 232, 255) : Color.WHITE);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(user ? dp(36) : 0, 0, user ? 0 : dp(36), dp(9));

        TextView label = new TextView(this);
        label.setText(who);
        label.setTextSize(12);
        label.setTextColor(Color.rgb(91, 92, 226));
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(label);

        TextView body = new TextView(this);
        body.setText(text);
        body.setTextSize(16);
        body.setTextColor(Color.rgb(30, 34, 42));
        body.setPadding(0, dp(3), 0, 0);
        card.addView(body);

        messages.addView(card, cardLp);
        if (persist) appendHistory(who, text, user);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void appendHistory(String who, String text, boolean user) {
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            JSONObject item = new JSONObject();
            item.put("who", who);
            item.put("text", text);
            item.put("user", user);
            item.put("ts", System.currentTimeMillis());
            array.put(item);
            while (array.length() > 100) {
                JSONArray trimmed = new JSONArray();
                for (int i = 1; i < array.length(); i++) trimmed.put(array.get(i));
                array = trimmed;
            }
            prefs.edit().putString(KEY_HISTORY, array.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    private void restoreHistory() {
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                addMessage(item.optString("who", "MG-AI"), item.optString("text", ""),
                        item.optBoolean("user", false), false);
            }
        } catch (JSONException ignored) {
            prefs.edit().remove(KEY_HISTORY).apply();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    static final class LocalBootstrapEngine {
        static String reply(String input) {
            String normalized = input.toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("merhaba") || normalized.contains("selam")) {
                return "Merhaba. MG-AI v0.1 yerel çekirdeği çalışıyor. Şu anda amaç Android iskeletini ve kalıcı konuşma yolunu doğrulamak.";
            }
            if (normalized.contains("durum") || normalized.contains("bitti")) {
                return "Adım 1 çalışıyor: uygulama açılıyor, mesaj alıyor ve konuşmayı cihazda saklıyor. Gerçek yapay zekâ modeli henüz bağlanmadı.";
            }
            return "Mesajını aldım ve cihazdaki konuşma hafızasına kaydettim. Bu v0.1 bootstrap cevabıdır; sonraki adımda MG-Core model adaptörü bağlanacak.";
        }
    }
}
