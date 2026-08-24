package com.mgai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String PREFS = "mg_ai_v02";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_MODEL = "model";

    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private SharedPreferences prefs;
    private TextView status;
    private Button send;
    private String sessionApiKey = "";
    private boolean waiting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        restoreHistory();
        updateStatus();
        if (messages.getChildCount() == 0) {
            addMessage("MG-AI", "MG-AI v0.2 model adaptörü hazır. Sağ üstteki Ayarlar bölümünden MG-Core endpoint ve model adını gir. Kendi OpenAI-compatible sunucumuza, vLLM/Ollama/LM Studio uyumlu geçide veya test için başka uyumlu bir servise bağlanabilirsin.", false, false);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(14));
        root.setBackgroundColor(Color.rgb(244, 246, 248));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleArea = new LinearLayout(this);
        titleArea.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleAreaLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

        TextView title = new TextView(this);
        title.setText("MG-AI");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(20, 24, 32));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleArea.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("v0.2 • MG-Core adapter • Local memory");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.rgb(90, 97, 110));
        subtitle.setPadding(0, dp(2), 0, 0);
        titleArea.addView(subtitle);
        header.addView(titleArea, titleAreaLp);

        Button settings = new Button(this);
        settings.setText("Ayarlar");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> showSettings());
        header.addView(settings);
        root.addView(header);

        status = new TextView(this);
        status.setTextSize(12);
        status.setTextColor(Color.rgb(95, 102, 116));
        status.setPadding(0, dp(4), 0, dp(10));
        root.addView(status);

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

        send = new Button(this);
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
        if (waiting) return;
        String text = input.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String endpoint = prefs.getString(KEY_ENDPOINT, "").trim();
        String model = prefs.getString(KEY_MODEL, "").trim();
        if (endpoint.isEmpty() || model.isEmpty()) {
            addMessage("MG-AI", "Önce Ayarlar bölümünden MG-Core endpoint ve model adını gir.", false, false);
            showSettings();
            return;
        }

        input.setText("");
        addMessage("Sen", text, true, true);
        hideKeyboard();
        setWaiting(true);

        JSONArray context = buildRecentConversation(16);
        ModelClient.chat(endpoint, model, sessionApiKey, context, new ModelClient.Callback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    addMessage("MG-AI", result, false, true);
                    setWaiting(false);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    addMessage("Bağlantı hatası", message, false, false);
                    setWaiting(false);
                });
            }
        });
    }

    private JSONArray buildRecentConversation(int maxItems) {
        JSONArray out = new JSONArray();
        try {
            JSONArray saved = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            int start = Math.max(0, saved.length() - maxItems);
            for (int i = start; i < saved.length(); i++) {
                JSONObject source = saved.getJSONObject(i);
                String text = source.optString("text", "").trim();
                if (text.isEmpty()) continue;
                JSONObject m = new JSONObject();
                m.put("role", source.optBoolean("user", false) ? "user" : "assistant");
                m.put("content", text);
                out.put(m);
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), 0);

        EditText endpoint = new EditText(this);
        endpoint.setHint("https://sunucu/v1/chat/completions");
        endpoint.setText(prefs.getString(KEY_ENDPOINT, ""));
        endpoint.setSingleLine(true);
        box.addView(label("MG-Core endpoint"));
        box.addView(endpoint);

        EditText model = new EditText(this);
        model.setHint("ör. mg-core-7b");
        model.setText(prefs.getString(KEY_MODEL, ""));
        model.setSingleLine(true);
        box.addView(label("Model adı"));
        box.addView(model);

        EditText key = new EditText(this);
        key.setHint("Opsiyonel • sadece bu oturumda tutulur");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setText(sessionApiKey);
        key.setSingleLine(true);
        box.addView(label("API anahtarı"));
        box.addView(key);

        TextView note = new TextView(this);
        note.setText("API anahtarı kalıcı hafızaya yazılmaz. HTTP yerel ağ endpointleri geliştirme için desteklenir; üretimde HTTPS kullanacağız.");
        note.setTextSize(12);
        note.setTextColor(Color.DKGRAY);
        note.setPadding(0, dp(10), 0, 0);
        box.addView(note);

        new AlertDialog.Builder(this)
                .setTitle("MG-Core bağlantısı")
                .setView(box)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    prefs.edit()
                            .putString(KEY_ENDPOINT, endpoint.getText().toString().trim())
                            .putString(KEY_MODEL, model.getText().toString().trim())
                            .apply();
                    sessionApiKey = key.getText().toString();
                    updateStatus();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(Color.rgb(70, 76, 88));
        t.setPadding(0, dp(10), 0, 0);
        return t;
    }

    private void updateStatus() {
        String endpoint = prefs.getString(KEY_ENDPOINT, "").trim();
        String model = prefs.getString(KEY_MODEL, "").trim();
        if (endpoint.isEmpty() || model.isEmpty()) {
            status.setText("● MG-Core ayarlanmadı");
            status.setTextColor(Color.rgb(180, 95, 35));
        } else {
            status.setText("● Hazır • " + model + " • " + endpoint);
            status.setTextColor(Color.rgb(30, 130, 80));
        }
    }

    private void setWaiting(boolean value) {
        waiting = value;
        send.setEnabled(!value);
        input.setEnabled(!value);
        if (value) {
            status.setText("● MG-Core yanıtı bekleniyor…");
            status.setTextColor(Color.rgb(70, 90, 180));
        } else {
            updateStatus();
        }
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
}
