package com.muhammetgecgil.morse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MORS LINK - Parça 1.
 * İki telefon arasında fiziksel ışık kanalıyla Mors gönderir.
 * Bu sürüm verici tarafıdır: kamera flaşı veya tam ekran ışığı.
 */
public final class MorseLinkActivity extends Activity {
    private static final int CAMERA_REQUEST = 501;
    private static final String PREFS = "morse_link_prefs";
    private static final int MIN_OPTICAL_WPM = 5;
    private static final int MAX_OPTICAL_WPM = 8;

    private static final int BG = Color.rgb(3, 10, 18);
    private static final int SURFACE = Color.rgb(7, 21, 34);
    private static final int CYAN = Color.rgb(32, 223, 244);
    private static final int GREEN = Color.rgb(130, 244, 91);
    private static final int TEXT = Color.rgb(244, 251, 255);
    private static final int TEXT_2 = Color.rgb(168, 189, 203);
    private static final int STROKE = Color.rgb(28, 94, 120);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService screenExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger screenGeneration = new AtomicInteger();

    private SharedPreferences prefs;
    private SignalEngine signalEngine;
    private EditText messageInput;
    private TextView morsePreview;
    private TextView wpmLabel;
    private TextView statusText;
    private FrameLayout screenOverlay;
    private TextView overlayTitle;
    private TextView overlayMorse;
    private boolean pendingTorch;
    private float oldBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        signalEngine = new SignalEngine(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildUi());
        refreshPreview();
    }

    private View buildUi() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView back = button("‹  Mors Kod Çevirici", CYAN, SURFACE);
        back.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        back.setOnClickListener(v -> finish());
        root.addView(back, lp(-1, dp(48), 0, 10));

        TextView title = text("MORS LINK", 30, GREEN, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp(-1, -2, 0, 4));

        TextView subtitle = text("Telefon → telefon optik Mors haberleşmesi • Parça 1", 13, TEXT_2, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, lp(-1, -2, 0, 16));

        LinearLayout info = card();
        info.addView(text("OPTİK VERİCİ", 15, CYAN, Typeface.BOLD));
        TextView infoText = text("Mesajını karşı telefona kamera flaşı veya ekran ışığıyla gönder. Kamera ile otomatik okuma bir sonraki parçada eklenecek.", 13, TEXT_2, Typeface.NORMAL);
        infoText.setPadding(0, dp(6), 0, 0);
        info.addView(infoText);
        root.addView(info, lp(-1, -2, 0, 12));

        LinearLayout inputCard = card();
        inputCard.addView(text("Gönderilecek Mesaj", 16, CYAN, Typeface.BOLD));
        messageInput = new EditText(this);
        messageInput.setTextColor(TEXT);
        messageInput.setHintTextColor(Color.rgb(100, 127, 143));
        messageInput.setTextSize(18);
        messageInput.setGravity(Gravity.TOP | Gravity.START);
        messageInput.setPadding(dp(14), dp(14), dp(14), dp(14));
        messageInput.setMinHeight(dp(110));
        messageInput.setSingleLine(false);
        messageInput.setMaxLines(6);
        messageInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        messageInput.setHint("Örn. Beni duyuyor musun?");
        messageInput.setBackground(rounded(BG, STROKE, 14, 1));
        messageInput.setText(prefs.getString("draft", ""));
        inputCard.addView(messageInput, lp(-1, -2, 8, 0));
        root.addView(inputCard, lp(-1, -2, 0, 12));

        LinearLayout previewCard = card();
        previewCard.addView(text("Mors Önizleme", 16, CYAN, Typeface.BOLD));
        morsePreview = text("", 17, Color.rgb(104, 245, 226), Typeface.BOLD);
        morsePreview.setPadding(dp(12), dp(12), dp(12), dp(12));
        morsePreview.setMinHeight(dp(88));
        morsePreview.setTextIsSelectable(true);
        morsePreview.setBackground(rounded(BG, STROKE, 12, 1));
        previewCard.addView(morsePreview, lp(-1, -2, 8, 0));
        root.addView(previewCard, lp(-1, -2, 0, 12));

        LinearLayout speedCard = card();
        wpmLabel = text("Optik hız: " + opticalWpm() + " WPM", 15, GREEN, Typeface.BOLD);
        speedCard.addView(wpmLabel);
        SeekBar speed = new SeekBar(this);
        speed.setMax(MAX_OPTICAL_WPM - MIN_OPTICAL_WPM);
        speed.setProgress(opticalWpm() - MIN_OPTICAL_WPM);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + MIN_OPTICAL_WPM;
                wpmLabel.setText("Optik hız: " + value + " WPM");
                if (fromUser) prefs.edit().putInt("optical_wpm", value).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        speedCard.addView(speed);
        TextView safety = text("⚠ Işık güvenliği: Bu sürümde ekran ve flaş 8 WPM ile sınırlandırıldı. Hızlı yanıp sönen ışığa duyarlı kişiler bu özelliği kullanmamalıdır.", 12, Color.rgb(255, 198, 90), Typeface.NORMAL);
        safety.setPadding(0, dp(6), 0, 0);
        speedCard.addView(safety);
        root.addView(speedCard, lp(-1, -2, 0, 12));

        TextView flashButton = button("⚡  FLAŞLA GÖNDER\nArka kamera flaşını Mors ritminde yak", GREEN, Color.rgb(10, 36, 31));
        flashButton.setGravity(Gravity.CENTER);
        flashButton.setOnClickListener(v -> startTorchWithWarning());
        root.addView(flashButton, lp(-1, dp(78), 0, 10));

        TextView screenButton = button("▣  EKRANLA GÖNDER\nTüm ekranı siyah/beyaz Mors sinyaline çevir", CYAN, Color.rgb(7, 30, 46));
        screenButton.setGravity(Gravity.CENTER);
        screenButton.setOnClickListener(v -> startScreenWithWarning());
        root.addView(screenButton, lp(-1, dp(78), 0, 10));

        TextView stopButton = button("■  TÜM İLETİMİ DURDUR", Color.rgb(255, 144, 132), Color.rgb(48, 21, 25));
        stopButton.setGravity(Gravity.CENTER);
        stopButton.setOnClickListener(v -> stopAll("İletim durduruldu"));
        root.addView(stopButton, lp(-1, dp(54), 0, 12));

        statusText = text("Hazır • Kanal seç", 13, TEXT_2, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(10), dp(10), dp(10), dp(10));
        statusText.setBackground(rounded(Color.rgb(5, 17, 27), Color.rgb(14, 63, 80), 12, 1));
        root.addView(statusText, lp(-1, -2, 0, 14));

        LinearLayout next = card();
        next.addView(text("SONRAKİ PARÇA", 14, Color.rgb(255, 202, 101), Typeface.BOLD));
        next.addView(text("📷 Kamera ile otomatik Mors alma\n• ışık kaynağını bulma ve kilitleme\n• nokta/çizgi sürelerini ölçme\n• gelen mesajı canlı Türkçeye çevirme", 13, TEXT_2, Typeface.NORMAL));
        root.addView(next, lp(-1, -2, 0, 6));

        TextView version = text("MORS LINK TX v1 • Çevrimdışı • Sunucu yok", 11, Color.rgb(96, 123, 139), Typeface.NORMAL);
        version.setGravity(Gravity.CENTER);
        root.addView(version, lp(-1, -2, 0, 0));

        buildScreenOverlay(frame);

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                prefs.edit().putString("draft", s.toString()).apply();
                refreshPreview();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        return frame;
    }

    private void buildScreenOverlay(FrameLayout frame) {
        screenOverlay = new FrameLayout(this);
        screenOverlay.setBackgroundColor(Color.BLACK);
        screenOverlay.setVisibility(View.GONE);
        frame.addView(screenOverlay, new FrameLayout.LayoutParams(-1, -1));

        overlayTitle = text("EKRAN MORS İLETİMİ", 18, Color.WHITE, Typeface.BOLD);
        overlayTitle.setGravity(Gravity.CENTER);
        overlayTitle.setPadding(dp(12), dp(10), dp(12), dp(10));
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        titleLp.topMargin = dp(26);
        titleLp.leftMargin = dp(28);
        titleLp.rightMargin = dp(28);
        screenOverlay.addView(overlayTitle, titleLp);

        overlayMorse = text("", 15, Color.WHITE, Typeface.BOLD);
        overlayMorse.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams morseLp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        morseLp.leftMargin = dp(24);
        morseLp.rightMargin = dp(24);
        screenOverlay.addView(overlayMorse, morseLp);

        TextView cancel = button("DURDUR", Color.WHITE, Color.argb(150, 100, 0, 0));
        cancel.setGravity(Gravity.CENTER);
        cancel.setOnClickListener(v -> stopAll("Ekran iletimi durduruldu"));
        FrameLayout.LayoutParams cancelLp = new FrameLayout.LayoutParams(dp(160), dp(54), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        cancelLp.bottomMargin = dp(36);
        screenOverlay.addView(cancel, cancelLp);
    }

    private void refreshPreview() {
        if (morsePreview == null || messageInput == null) return;
        String morse = MorseCodec.toMorse(messageInput.getText().toString());
        morsePreview.setText(morse.isEmpty() ? "Mors kodu burada görünecek" : morse);
    }

    private String currentMorse() {
        String source = messageInput.getText().toString().trim();
        return source.isEmpty() ? "" : MorseCodec.toMorse(source);
    }

    private int opticalWpm() {
        return Math.max(MIN_OPTICAL_WPM, Math.min(MAX_OPTICAL_WPM, prefs.getInt("optical_wpm", 6)));
    }

    private void startTorchWithWarning() {
        if (!prepareForSend()) return;
        showOpticalWarningIfNeeded(this::startTorchNow);
    }

    private void startScreenWithWarning() {
        if (!prepareForSend()) return;
        showOpticalWarningIfNeeded(this::startScreenNow);
    }

    private boolean prepareForSend() {
        if (currentMorse().isEmpty()) {
            toast("Önce gönderilecek mesajı yaz.");
            return false;
        }
        stopAll(null);
        return true;
    }

    private void showOpticalWarningIfNeeded(Runnable continuation) {
        if (prefs.getBoolean("optical_warning_seen", false)) {
            continuation.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Yanıp sönen ışık uyarısı")
                .setMessage("Flaş ve tam ekran Mors modu yanıp sönen ışık üretir. Fotosensitif epilepsi veya ışığa karşı hassasiyet varsa kullanmayın. Karşıdaki kişinin yüzüne yakın mesafeden flaş tutmayın.\n\nBu sürüm optik hızı güvenlik amacıyla 5-8 WPM ile sınırlar.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Anladım, devam et", (d, w) -> {
                    prefs.edit().putBoolean("optical_warning_seen", true).apply();
                    continuation.run();
                })
                .show();
    }

    private void startTorchNow() {
        if (!signalEngine.hasTorch()) {
            toast("Bu cihazda kullanılabilir kamera flaşı bulunamadı.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingTorch = true;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            return;
        }
        String morse = currentMorse();
        status("⚡ Flaş ile gönderiliyor • " + opticalWpm() + " WPM", GREEN);
        signalEngine.playTorch(morse, opticalWpm(), () -> status("✓ Flaş iletimi tamamlandı", GREEN));
    }

    private void startScreenNow() {
        final String morse = currentMorse();
        if (morse.isEmpty()) return;
        final int token = screenGeneration.incrementAndGet();
        final int unitMs = Math.max(150, 1200 / opticalWpm());
        final List<SignalEngine.Segment> segments = SignalEngine.segmentsFromMorse(morse, unitMs);
        if (segments.isEmpty()) return;

        saveAndMaximizeBrightness();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        screenOverlay.setVisibility(View.VISIBLE);
        overlayMorse.setText(morse);
        status("▣ Ekran ile gönderiliyor • " + opticalWpm() + " WPM", CYAN);

        screenExecutor.execute(() -> {
            try {
                setOverlayState(false, token);
                if (!sleepCancelable(1000, token)) return;
                for (SignalEngine.Segment s : segments) {
                    if (screenGeneration.get() != token) return;
                    setOverlayState(s.on, token);
                    if (!sleepCancelable(s.durationMs, token)) return;
                }
            } finally {
                if (screenGeneration.get() == token) {
                    main.post(() -> {
                        finishScreenTransmission();
                        status("✓ Ekran iletimi tamamlandı", GREEN);
                    });
                }
            }
        });
    }

    private boolean sleepCancelable(long durationMs, int token) {
        long end = SystemClock.elapsedRealtime() + durationMs;
        while (SystemClock.elapsedRealtime() < end) {
            if (screenGeneration.get() != token) return false;
            SystemClock.sleep(Math.min(25, Math.max(1, end - SystemClock.elapsedRealtime())));
        }
        return screenGeneration.get() == token;
    }

    private void setOverlayState(boolean on, int token) {
        main.post(() -> {
            if (screenGeneration.get() != token) return;
            screenOverlay.setBackgroundColor(on ? Color.WHITE : Color.BLACK);
            int textColor = on ? Color.BLACK : Color.WHITE;
            overlayTitle.setTextColor(textColor);
            overlayMorse.setTextColor(textColor);
        });
    }

    private void stopAll(String message) {
        signalEngine.cancelAll();
        screenGeneration.incrementAndGet();
        finishScreenTransmission();
        if (message != null) status(message, TEXT_2);
    }

    private void finishScreenTransmission() {
        if (screenOverlay != null) {
            screenOverlay.setBackgroundColor(Color.BLACK);
            screenOverlay.setVisibility(View.GONE);
        }
        restoreBrightness();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void saveAndMaximizeBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        oldBrightness = lp.screenBrightness;
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);
    }

    private void restoreBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = oldBrightness;
        getWindow().setAttributes(lp);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && pendingTorch) {
                pendingTorch = false;
                startTorchNow();
            } else {
                pendingTorch = false;
                toast("Flaş ile gönderim için kamera izni gerekir.");
            }
        }
    }

    @Override
    protected void onStop() {
        stopAll(null);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopAll(null);
        signalEngine.shutdown();
        screenExecutor.shutdownNow();
        super.onDestroy();
    }

    private void status(String value, int color) {
        if (statusText == null) return;
        statusText.setText(value);
        statusText.setTextColor(color);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(rounded(SURFACE, STROKE, 18, 1));
        return card;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, style);
        return v;
    }

    private TextView button(String value, int color, int fill) {
        TextView v = text(value, 15, color, Typeface.BOLD);
        v.setPadding(dp(14), dp(8), dp(14), dp(8));
        v.setMinHeight(dp(48));
        v.setBackground(ripple(rounded(fill, color, 14, 1)));
        return v;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private RippleDrawable ripple(GradientDrawable content) {
        return new RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(60, 255, 255, 255)), content, null);
    }

    private LinearLayout.LayoutParams lp(int w, int h, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
