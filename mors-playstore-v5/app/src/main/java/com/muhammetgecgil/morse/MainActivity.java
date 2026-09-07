package com.muhammetgecgil.morse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int CAMERA_REQUEST = 314;
    private static final String PREFS = "morse_prefs_v5";
    private static final String KEY_HISTORY = "history";

    private static final int BG = Color.rgb(3, 10, 18);
    private static final int SURFACE = Color.rgb(7, 21, 34);
    private static final int SURFACE_ALT = Color.rgb(10, 28, 43);
    private static final int CYAN = Color.rgb(32, 223, 244);
    private static final int GREEN = Color.rgb(130, 244, 91);
    private static final int TEXT = Color.rgb(244, 251, 255);
    private static final int TEXT_2 = Color.rgb(168, 189, 203);
    private static final int STROKE = Color.rgb(28, 94, 120);

    private SharedPreferences prefs;
    private SignalEngine signalEngine;

    private LinearLayout root;
    private EditText input;
    private TextView output;
    private TextView inputLabel;
    private TextView outputLabel;
    private TextView modeTextToMorse;
    private TextView modeMorseToText;
    private TextView statusText;
    private LinearLayout historyList;

    private boolean textToMorse = true;
    private boolean suppressWatcher = false;
    private boolean pendingFlashAfterPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        signalEngine = new SignalEngine(this);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        setContentView(buildUi());
        applyInsets();
        loadInitialState();
    }

    private View buildUi() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        addHeader();
        addTitle();
        addModeSelector();
        addInputCard();
        addOutputCard();
        addStatusStrip();
        addMorseLinkLauncher();
        addActionGrid();
        addHistoryCard();
        addFooter();
        return frame;
    }

    private void addHeader() {
        MorseHeaderView header = new MorseHeaderView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(210));
        lp.bottomMargin = dp(14);
        root.addView(header, lp);
    }

    private void addTitle() {
        TextView title = label("Mors Kod Çevirici", 30, TEXT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, matchWrap(0, 2));

        TextView subtitle = label("Çevrimdışı • Ses • Titreşim • Flaş • Geçmiş", 13, TEXT_2, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = matchWrap(0, 14);
        root.addView(subtitle, lp);
    }

    private void addModeSelector() {
        LinearLayout segment = new LinearLayout(this);
        segment.setOrientation(LinearLayout.HORIZONTAL);
        segment.setPadding(dp(4), dp(4), dp(4), dp(4));
        segment.setBackground(rounded(SURFACE, STROKE, 18, 1));

        modeTextToMorse = modeButton("A→  Metin → Mors", true);
        modeMorseToText = modeButton("•••→A  Mors → Metin", false);
        segment.addView(modeTextToMorse, new LinearLayout.LayoutParams(0, dp(56), 1));
        segment.addView(modeMorseToText, new LinearLayout.LayoutParams(0, dp(56), 1));

        LinearLayout.LayoutParams lp = matchWrap(0, 14);
        root.addView(segment, lp);
    }

    private TextView modeButton(String text, boolean selected) {
        TextView v = label(text, 15, selected ? CYAN : TEXT_2, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(48));
        styleModeButton(v, selected);
        v.setOnClickListener(view -> {
            haptic(view);
            setMode(v == modeTextToMorse);
        });
        return v;
    }

    private void styleModeButton(TextView v, boolean selected) {
        int fill = selected ? Color.rgb(7, 42, 60) : Color.TRANSPARENT;
        int stroke = selected ? CYAN : Color.TRANSPARENT;
        v.setBackground(ripple(rounded(fill, stroke, 14, selected ? 1 : 0)));
        v.setTextColor(selected ? CYAN : TEXT_2);
    }

    private void addInputCard() {
        LinearLayout card = card();
        inputLabel = label("Giriş Metni", 16, CYAN, Typeface.BOLD);
        card.addView(inputLabel, matchWrap(0, 8));

        input = new EditText(this);
        input.setTextColor(TEXT);
        input.setHintTextColor(Color.rgb(100, 127, 143));
        input.setTextSize(18);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(dp(14), dp(14), dp(14), dp(14));
        input.setMinHeight(dp(126));
        input.setBackground(rounded(BG, STROKE, 14, 1));
        input.setSingleLine(false);
        input.setMaxLines(8);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
        input.setHint("Örn. Merhaba dünya");
        input.setContentDescription("Mors dönüşümü için giriş alanı");
        card.addView(input, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        quickRow.addView(miniAction("Temizle", v -> {
            input.setText("");
            output.setText("");
            signalEngine.cancelAll();
        }));
        quickRow.addView(miniAction("Klavyeyi Kapat", v -> hideKeyboard()));
        LinearLayout.LayoutParams qlp = matchWrap(0, 0);
        qlp.topMargin = dp(8);
        card.addView(quickRow, qlp);

        root.addView(card, matchWrap(0, 12));

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!suppressWatcher && prefs.getBoolean("auto", true)) convert(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void addOutputCard() {
        LinearLayout card = card();
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        outputLabel = label("Mors Çıktısı", 16, CYAN, Typeface.BOLD);
        heading.addView(outputLabel, new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(miniAction("Kopyala", v -> copyOutput()));
        card.addView(heading, matchWrap(0, 8));

        output = label("", 18, Color.rgb(104, 245, 226), Typeface.BOLD);
        output.setPadding(dp(14), dp(14), dp(14), dp(14));
        output.setMinHeight(dp(104));
        output.setTextIsSelectable(true);
        output.setGravity(Gravity.TOP | Gravity.START);
        output.setBackground(rounded(BG, STROKE, 14, 1));
        output.setContentDescription("Dönüşüm sonucu");
        card.addView(output, new LinearLayout.LayoutParams(-1, -2));

        root.addView(card, matchWrap(0, 10));
    }

    private void addStatusStrip() {
        statusText = label("", 12, TEXT_2, Typeface.NORMAL);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setPadding(dp(8), dp(8), dp(8), dp(8));
        statusText.setBackground(rounded(Color.rgb(5, 17, 27), Color.rgb(14, 63, 80), 12, 1));
        root.addView(statusText, matchWrap(0, 12));
        refreshStatus();
    }

    private void addMorseLinkLauncher() {
        TextView link = label("⌁  MORS LINK\nTelefon → telefon ışık haberleşmesi", 16, GREEN, Typeface.BOLD);
        link.setGravity(Gravity.CENTER);
        link.setPadding(dp(14), dp(10), dp(14), dp(10));
        link.setMinHeight(dp(72));
        link.setBackground(ripple(rounded(Color.rgb(8, 38, 36), GREEN, 16, 1)));
        link.setContentDescription("Mors Link iki telefon arasında ışıkla Mors gönderme");
        link.setOnClickListener(v -> {
            haptic(v);
            signalEngine.cancelAll();
            startActivity(new Intent(this, MorseLinkActivity.class));
        });
        root.addView(link, matchWrap(0, 12));
    }

    private void addActionGrid() {
        String[][] labels = {
                {"↻", "Çevir"}, {"◖)))", "Sesli Çal"}, {"▯≋", "Titreşim"},
                {"ϟ", "Flaş"}, {"▣", "Kopyala"}, {"⌯", "Paylaş"}
        };
        View.OnClickListener[] actions = new View.OnClickListener[]{
                v -> { haptic(v); convert(true); },
                v -> { haptic(v); playAudio(); },
                v -> { haptic(v); playVibration(); },
                v -> { haptic(v); playFlash(); },
                v -> { haptic(v); copyOutput(); },
                v -> { haptic(v); shareOutput(); }
        };

        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 3; col++) {
                int i = row * 3 + col;
                TextView b = actionButton(labels[i][0], labels[i][1]);
                b.setOnClickListener(actions[i]);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(78), 1);
                if (col > 0) bp.leftMargin = dp(8);
                line.addView(b, bp);
            }
            LinearLayout.LayoutParams lp = matchWrap(0, row == 0 ? 8 : 14);
            root.addView(line, lp);
        }
    }

    private TextView actionButton(String icon, String text) {
        TextView b = label(icon + "\n" + text, 15, CYAN, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setLineSpacing(0, 0.95f);
        b.setMinWidth(dp(48));
        b.setMinHeight(dp(48));
        b.setBackground(ripple(rounded(SURFACE_ALT, CYAN, 14, 1)));
        b.setContentDescription(text);
        return b;
    }

    private void addHistoryCard() {
        LinearLayout card = card();
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView h = label("Geçmiş", 16, CYAN, Typeface.BOLD);
        heading.addView(h, new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(miniAction("Temizle", v -> clearHistory()));
        card.addView(heading, matchWrap(0, 8));

        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        card.addView(historyList, new LinearLayout.LayoutParams(-1, -2));
        root.addView(card, matchWrap(0, 14));
    }

    private void addFooter() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        TextView settings = footerButton("⚙  Ayarlar");
        settings.setOnClickListener(v -> showSettings());
        TextView about = footerButton("ⓘ  Hakkında");
        about.setOnClickListener(v -> showAbout());
        row.addView(settings, new LinearLayout.LayoutParams(0, dp(54), 1));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, dp(54), 1);
        ap.leftMargin = dp(10);
        row.addView(about, ap);
        root.addView(row, matchWrap(0, 4));

        TextView privacy = label("Veriler cihazda kalır • İnternet izni yok • v5.1", 11, Color.rgb(96, 123, 139), Typeface.NORMAL);
        privacy.setGravity(Gravity.CENTER);
        root.addView(privacy, matchWrap(0, 0));
    }

    private TextView footerButton(String text) {
        TextView v = label(text, 15, TEXT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setBackground(ripple(rounded(SURFACE, STROKE, 14, 1)));
        return v;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(rounded(SURFACE, STROKE, 18, 1));
        return card;
    }

    private TextView miniAction(String text, View.OnClickListener listener) {
        TextView v = label(text, 12, CYAN, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(10), dp(8), dp(10), dp(8));
        v.setMinHeight(dp(40));
        v.setBackground(ripple(rounded(Color.rgb(5, 29, 42), Color.rgb(25, 104, 128), 10, 1)));
        v.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(8);
        v.setLayoutParams(lp);
        return v;
    }

    private void loadInitialState() {
        textToMorse = prefs.getBoolean("mode_text_to_morse", true);
        setMode(textToMorse);
        input.setText(prefs.getString("draft", ""));
        input.setSelection(input.length());
        convert(false);
        renderHistory();
    }

    private void setMode(boolean toMorse) {
        if (textToMorse == toMorse && inputLabel != null) {
            styleModeButton(modeTextToMorse, toMorse);
            styleModeButton(modeMorseToText, !toMorse);
        }
        textToMorse = toMorse;
        prefs.edit().putBoolean("mode_text_to_morse", toMorse).apply();
        styleModeButton(modeTextToMorse, toMorse);
        styleModeButton(modeMorseToText, !toMorse);
        if (input != null) {
            inputLabel.setText(toMorse ? "Giriş Metni" : "Mors Girişi");
            outputLabel.setText(toMorse ? "Mors Çıktısı" : "Metin Çıktısı");
            input.setHint(toMorse ? "Örn. Merhaba dünya" : "Örn. ... --- ...");
            convert(false);
        }
    }

    private void convert(boolean save) {
        if (input == null || output == null) return;
        String source = input.getText().toString();
        String result = textToMorse ? MorseCodec.toMorse(source) : MorseCodec.fromMorse(source);
        output.setText(result);
        prefs.edit().putString("draft", source).apply();
        if (save && !source.trim().isEmpty() && !result.trim().isEmpty()) {
            addHistory(source, result, textToMorse);
            renderHistory();
            Toast.makeText(this, "Dönüştürüldü", Toast.LENGTH_SHORT).show();
        }
    }

    private String currentMorse() {
        String candidate = textToMorse ? output.getText().toString() : input.getText().toString();
        if (!MorseCodec.looksLikeMorse(candidate)) candidate = MorseCodec.toMorse(candidate);
        return candidate;
    }

    private void playAudio() {
        convert(false);
        String morse = currentMorse();
        if (morse.isEmpty()) { toast("Önce bir metin veya Mors kodu gir."); return; }
        signalEngine.playAudio(morse, wpm(), frequency(), () -> toast("Sesli oynatma tamamlandı"));
        toast("Mors sesi çalıyor");
    }

    private void playVibration() {
        convert(false);
        String morse = currentMorse();
        if (morse.isEmpty()) { toast("Önce bir metin veya Mors kodu gir."); return; }
        if (!signalEngine.hasVibrator()) { toast("Bu cihazda titreşim motoru kullanılamıyor."); return; }
        boolean ok = signalEngine.vibrate(morse, wpm(), vibrationAmplitude());
        toast(ok ? "Mors titreşimi başladı" : "Titreşim başlatılamadı");
    }

    private void playFlash() {
        convert(false);
        String morse = currentMorse();
        if (morse.isEmpty()) { toast("Önce bir metin veya Mors kodu gir."); return; }
        if (!signalEngine.hasTorch()) { toast("Uygun kamera flaşı bulunamadı."); return; }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingFlashAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            return;
        }
        signalEngine.playTorch(morse, wpm(), () -> toast("Flaş iletim tamamlandı"));
        toast("Flaş ile Mors iletiliyor");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && pendingFlashAfterPermission) {
                pendingFlashAfterPermission = false;
                playFlash();
            } else {
                pendingFlashAfterPermission = false;
                toast("Flaş özelliği için kamera izni gerekir. İzin yalnızca el fenerini kontrol etmek için kullanılır.");
            }
        }
    }

    private void copyOutput() {
        String text = output.getText().toString();
        if (text.isEmpty()) { toast("Kopyalanacak sonuç yok."); return; }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Mors sonucu", text));
        toast("Sonuç panoya kopyalandı");
    }

    private void shareOutput() {
        String text = output.getText().toString();
        if (text.isEmpty()) { toast("Paylaşılacak sonuç yok."); return; }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Mors sonucunu paylaş"));
    }

    private void addHistory(String source, String result, boolean mode) {
        JSONArray arr = historyArray();
        JSONArray updated = new JSONArray();
        JSONObject entry = new JSONObject();
        try {
            entry.put("source", source);
            entry.put("result", result);
            entry.put("mode", mode ? "text" : "morse");
            entry.put("time", System.currentTimeMillis());
            updated.put(entry);
            for (int i = 0; i < arr.length() && updated.length() < 10; i++) {
                JSONObject old = arr.optJSONObject(i);
                if (old == null) continue;
                if (source.equals(old.optString("source")) && result.equals(old.optString("result"))) continue;
                updated.put(old);
            }
        } catch (JSONException ignored) {}
        prefs.edit().putString(KEY_HISTORY, updated.toString()).apply();
    }

    private JSONArray historyArray() {
        try { return new JSONArray(prefs.getString(KEY_HISTORY, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    private void renderHistory() {
        if (historyList == null) return;
        historyList.removeAllViews();
        JSONArray arr = historyArray();
        if (arr.length() == 0) {
            TextView empty = label("Henüz kayıt yok. Çevir düğmesiyle yaptığın dönüşümler burada tutulur.", 13, TEXT_2, Typeface.NORMAL);
            empty.setPadding(0, dp(6), 0, dp(6));
            historyList.addView(empty);
            return;
        }
        int count = Math.min(5, arr.length());
        for (int i = 0; i < count; i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            String source = item.optString("source");
            String result = item.optString("result");
            long time = item.optLong("time", 0L);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(10), dp(9), dp(10), dp(9));
            row.setBackground(rounded(BG, Color.rgb(16, 58, 75), 12, 1));
            TextView first = label(ellipsize(source, 54), 14, TEXT, Typeface.BOLD);
            TextView second = label(ellipsize(result, 80), 12, TEXT_2, Typeface.NORMAL);
            TextView timeView = label(time > 0 ? DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(new Date(time)) : "", 11, GREEN, Typeface.NORMAL);
            row.addView(first);
            row.addView(second);
            row.addView(timeView);
            row.setContentDescription("Geçmiş kaydı: " + source);
            row.setOnClickListener(v -> {
                suppressWatcher = true;
                input.setText(source);
                input.setSelection(input.length());
                suppressWatcher = false;
                boolean mode = "text".equals(item.optString("mode", "text"));
                setMode(mode);
                convert(false);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            if (i > 0) lp.topMargin = dp(7);
            historyList.addView(row, lp);
        }
    }

    private void clearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Geçmiş temizlensin mi?")
                .setMessage("Kaydedilmiş dönüşümler cihazdan silinecek.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Temizle", (d, w) -> {
                    prefs.edit().remove(KEY_HISTORY).apply();
                    renderHistory();
                }).show();
    }

    private void showSettings() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(6), dp(18), 0);

        TextView wpmLabel = label("Hız: " + wpm() + " WPM", 15, Color.BLACK, Typeface.BOLD);
        SeekBar wpmBar = new SeekBar(this);
        wpmBar.setMax(35);
        wpmBar.setProgress(wpm() - 5);
        panel.addView(wpmLabel);
        panel.addView(wpmBar);

        TextView freqLabel = label("Ses frekansı: " + frequency() + " Hz", 15, Color.BLACK, Typeface.BOLD);
        SeekBar freqBar = new SeekBar(this);
        freqBar.setMax(800);
        freqBar.setProgress(frequency() - 400);
        panel.addView(freqLabel);
        panel.addView(freqBar);

        TextView vibLabel = label("Titreşim gücü: " + vibrationAmplitude(), 15, Color.BLACK, Typeface.BOLD);
        SeekBar vibBar = new SeekBar(this);
        vibBar.setMax(205);
        vibBar.setProgress(vibrationAmplitude() - 50);
        panel.addView(vibLabel);
        panel.addView(vibBar);

        Switch auto = new Switch(this);
        auto.setText("Yazarken otomatik çevir");
        auto.setTextSize(15);
        auto.setChecked(prefs.getBoolean("auto", true));
        auto.setPadding(0, dp(10), 0, 0);
        panel.addView(auto);

        wpmBar.setOnSeekBarChangeListener(seekListener(v -> wpmLabel.setText("Hız: " + (v + 5) + " WPM")));
        freqBar.setOnSeekBarChangeListener(seekListener(v -> freqLabel.setText("Ses frekansı: " + (v + 400) + " Hz")));
        vibBar.setOnSeekBarChangeListener(seekListener(v -> vibLabel.setText("Titreşim gücü: " + (v + 50))));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ayarlar")
                .setView(panel)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            prefs.edit()
                    .putInt("wpm", wpmBar.getProgress() + 5)
                    .putInt("frequency", freqBar.getProgress() + 400)
                    .putInt("vib", vibBar.getProgress() + 50)
                    .putBoolean("auto", auto.isChecked())
                    .apply();
            refreshStatus();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private SeekBar.OnSeekBarChangeListener seekListener(java.util.function.IntConsumer consumer) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { consumer.accept(progress); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Mors Kod Çevirici 5.1")
                .setMessage("Modern ve çevrimdışı Mors yardımcı aracı.\n\n" +
                        "• Metin ↔ Mors dönüşümü\n" +
                        "• Türkçe genişletilmiş karakterler\n" +
                        "• WPM tabanlı doğru zamanlama\n" +
                        "• Ses, S24 Ultra uyumlu titreşim ve flaş\n" +
                        "• Yerel geçmiş ve paylaşım\n" +
                        "• MORS LINK: flaş ve ekran ışığıyla telefon → telefon gönderim\n\n" +
                        "Gizlilik: Uygulama internet izni istemez ve dönüşüm verilerini sunucuya göndermez. Kamera izni yalnızca kullanıcı Flaş özelliğini seçtiğinde el fenerini kontrol etmek için istenir.\n\n" +
                        "MUHAMMET tasarımı uygulama içi görsel kimliğin bir parçasıdır.")
                .setPositiveButton("Tamam", null)
                .show();
    }

    private int wpm() { return Math.max(5, Math.min(40, prefs.getInt("wpm", 15))); }
    private int frequency() { return Math.max(400, Math.min(1200, prefs.getInt("frequency", 700))); }
    private int vibrationAmplitude() { return Math.max(50, Math.min(255, prefs.getInt("vib", 180))); }

    private void refreshStatus() {
        if (statusText != null) {
            statusText.setText(wpm() + " WPM  •  " + frequency() + " Hz  •  Titreşim " + vibrationAmplitude() + "  •  API 36 hazır");
        }
    }

    private void applyInsets() {
        View decor = getWindow().getDecorView();
        decor.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            root.setPadding(dp(16), top + dp(10), dp(16), bottom + dp(22));
            return insets;
        });
        decor.requestApplyInsets();
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            focus.clearFocus();
        }
    }

    private void haptic(View v) {
        if (v != null) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }

    private String ellipsize(String s, int max) {
        if (s == null) return "";
        String oneLine = s.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max - 1) + "…";
    }

    private TextView label(String text, int sp, int color, int style) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.create("sans", style));
        v.setIncludeFontPadding(false);
        return v;
    }

    private GradientDrawable rounded(int fill, int strokeColor, int radiusDp, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0 && strokeColor != Color.TRANSPARENT) g.setStroke(dp(strokeDp), strokeColor);
        return g;
    }

    private RippleDrawable ripple(GradientDrawable base) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(90, 32, 223, 244)), base, null);
    }

    private LinearLayout.LayoutParams matchWrap(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(top);
        lp.bottomMargin = dp(bottom);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        signalEngine.cancelAll();
        if (input != null) prefs.edit().putString("draft", input.getText().toString()).apply();
    }

    @Override
    protected void onDestroy() {
        signalEngine.shutdown();
        super.onDestroy();
    }
}
