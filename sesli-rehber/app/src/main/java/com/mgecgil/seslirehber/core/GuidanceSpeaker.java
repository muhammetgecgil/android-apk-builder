package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import com.mgecgil.seslirehber.navigation.NavigationCoordinator;
import java.util.Locale;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public final class GuidanceSpeaker implements TextToSpeech.OnInitListener {
    private static final long STOP_NAV_HOLD_MS = 3000L;
    private static final long CAUTION_NAV_HOLD_MS = 1800L;
    private static final String DESTINATION_PREFIX = "Hedef algılandı: ";
    private static final String OLD_ROUTE_SUFFIX = ". Rota motoru henüz bağlı değil; yönlendirme başlatılmadı.";

    private final TextToSpeech tts;
    private final Vibrator vibrator;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable navigationRetry = this::deliverPendingNavigation;
    private final NavigationCoordinator navigation;
    private volatile boolean ready;
    private String lastSpeech = "";
    private long navigationBlockedUntilMs;
    private String pendingNavigationText = "";

    public GuidanceSpeaker(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this);
        tts.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        vibrator = context.getSystemService(Vibrator.class);
        navigation = new NavigationCoordinator(context, new NavigationCoordinator.Output() {
            @Override public void speakSystem(String text) { GuidanceSpeaker.this.speakNavigation(text); }
            @Override public void speakNavigation(String text) { GuidanceSpeaker.this.speakNavigation(text); }
        });
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("tr", "TR"));
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
            tts.setSpeechRate(1.08f);
        }
    }

    public void announce(GuidanceDecision decision) {
        if (decision.speech() == null || decision.speech().trim().isEmpty()) return;
        long now = System.currentTimeMillis();
        if (decision.risk() == Risk.STOP) {
            navigationBlockedUntilMs = Math.max(navigationBlockedUntilMs, now + STOP_NAV_HOLD_MS);
        } else if (decision.risk() == Risk.CAUTION) {
            navigationBlockedUntilMs = Math.max(navigationBlockedUntilMs, now + CAUTION_NAV_HOLD_MS);
        }
        lastSpeech = decision.speech();
        if (ready) tts.speak(decision.speech(), TextToSpeech.QUEUE_FLUSH, null, "guidance");
        vibrate(decision.direction(), decision.risk());
    }

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String clean = text.trim();
        if (clean.startsWith(DESTINATION_PREFIX) && clean.endsWith(OLD_ROUTE_SUFFIX)) {
            String target = clean.substring(
                    DESTINATION_PREFIX.length(),
                    clean.length() - OLD_ROUTE_SUFFIX.length()).trim();
            navigation.requestDestination(target);
            return;
        }
        if (clean.startsWith("Sesli Rehber sürüm sıfır nokta dokuz.")) {
            clean = clean.replaceFirst(
                    "Sesli Rehber sürüm sıfır nokta dokuz\\.",
                    "Sesli Rehber sürüm sıfır nokta on dört. Çoklu nesne izleme, uzak görüş ve sol orta sağ yakın orta uzak durumsal çevre modeli aktif.");
        }
        speakRaw(clean, "speech");
    }

    private void speakNavigation(String text) {
        if (text == null || text.trim().isEmpty()) return;
        pendingNavigationText = text.trim();
        mainHandler.removeCallbacks(navigationRetry);
        deliverPendingNavigation();
    }

    private void deliverPendingNavigation() {
        if (pendingNavigationText.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now < navigationBlockedUntilMs) {
            long delay = Math.max(120L, navigationBlockedUntilMs - now + 80L);
            mainHandler.removeCallbacks(navigationRetry);
            mainHandler.postDelayed(navigationRetry, delay);
            return;
        }
        mainHandler.removeCallbacks(navigationRetry);
        String text = pendingNavigationText;
        pendingNavigationText = "";
        speakRaw(text, "navigation");
    }

    private void speakRaw(String text, String utteranceId) {
        if (text == null || text.trim().isEmpty()) return;
        lastSpeech = text.trim();
        if (ready) tts.speak(lastSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    public boolean isSpeaking() {
        try { return ready && tts.isSpeaking(); }
        catch (Throwable ignored) { return false; }
    }

    public void repeat() { speakRaw(lastSpeech, "repeat"); }

    public void shutdown() {
        mainHandler.removeCallbacksAndMessages(null);
        navigation.close();
        tts.stop();
        tts.shutdown();
    }

    private void vibrate(Direction direction, Risk risk) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern;
        if (risk == Risk.STOP) pattern = new long[]{0, 180, 80, 180, 80, 260};
        else if (direction == Direction.LEFT) pattern = new long[]{0, 70, 50, 70};
        else if (direction == Direction.RIGHT) pattern = new long[]{0, 140};
        else pattern = new long[]{0, 90, 50, 90, 50, 90};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
    }
}
