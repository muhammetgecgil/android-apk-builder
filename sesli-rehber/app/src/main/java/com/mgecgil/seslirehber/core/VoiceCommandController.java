package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import com.mgecgil.seslirehber.navigation.NavigationVoiceBridge;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Turkish voice controller with explicit one-shot and best-effort foreground hands-free cycles.
 * Navigation confirmation gets first refusal while a destination question is active.
 */
public final class VoiceCommandController implements RecognitionListener {
    public interface Listener {
        void onVoiceText(String text);
        void onVoiceError(String message);
        default void onVoiceState(String message) {}
        default void onWakeModeChanged(boolean enabled, boolean onDevice) {}
    }

    private static final long NORMAL_RESTART_MS = 420L;
    private static final long BUSY_RESTART_MS = 1200L;
    private static final long HARD_ERROR_RESTART_MS = 2200L;

    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SpeechRecognizer recognizer;
    private final boolean onDeviceRecognizer;

    private BooleanSupplier speechBusy = () -> false;
    private boolean handsFreeEnabled;
    private boolean awaitingCommand;
    private boolean manualOneShot;
    private boolean listening;
    private boolean destroyed;
    private boolean hostActive;
    private long lastSpokenErrorMs;

    public VoiceCommandController(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        boolean local = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && SpeechRecognizer.isOnDeviceRecognitionAvailable(this.context);
        SpeechRecognizer created;
        if (local) {
            try {
                created = SpeechRecognizer.createOnDeviceSpeechRecognizer(this.context);
            } catch (Throwable unsupported) {
                local = false;
                created = SpeechRecognizer.createSpeechRecognizer(this.context);
            }
        } else {
            created = SpeechRecognizer.createSpeechRecognizer(this.context);
        }
        onDeviceRecognizer = local;
        recognizer = created;
        recognizer.setRecognitionListener(this);
    }

    public void setSpeechBusySupplier(BooleanSupplier supplier) {
        speechBusy = supplier == null ? () -> false : supplier;
    }

    public void listenOnce() {
        mainHandler.post(() -> {
            if (destroyed) return;
            manualOneShot = true;
            awaitingCommand = false;
            cancelCurrent();
            startListeningWhenReady(120L);
        });
    }

    public boolean setHandsFreeEnabled(boolean enabled) {
        if (destroyed) return false;
        if (enabled && !onDeviceRecognizer) {
            handsFreeEnabled = false;
            listener.onWakeModeChanged(false, false);
            listener.onVoiceState("Cihazda yerel sürekli ses tanıma hazır değil. Sesli Komut düğmesi kullanılabilir.");
            return false;
        }
        handsFreeEnabled = enabled;
        awaitingCommand = false;
        manualOneShot = false;
        if (!enabled) mainHandler.post(this::cancelCurrent);
        else if (hostActive) mainHandler.post(() -> startListeningWhenReady(120L));
        listener.onWakeModeChanged(enabled, onDeviceRecognizer);
        return enabled;
    }

    public void onHostStart() {
        hostActive = true;
        if (handsFreeEnabled) mainHandler.post(() -> startListeningWhenReady(180L));
    }

    public void onHostStop() {
        hostActive = false;
        mainHandler.post(this::cancelCurrent);
    }

    public boolean isHandsFreeEnabled() { return handsFreeEnabled; }
    public boolean isOnDeviceRecognizer() { return onDeviceRecognizer; }

    public String modeDescription() {
        if (handsFreeEnabled) return "Hey Rehber açık, yerel tanıma";
        if (onDeviceRecognizer) return "Yerel ses tanıma hazır";
        return "Tek seferlik sistem ses tanıma";
    }

    private Intent recognizerIntent() {
        return new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR")
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    private void startListeningWhenReady(long delayMs) {
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.postDelayed(() -> {
            if (destroyed) return;
            if (!manualOneShot && (!handsFreeEnabled || !hostActive)) return;
            if (safeSpeechBusy()) {
                startListeningWhenReady(300L);
                return;
            }
            try {
                listening = true;
                recognizer.startListening(recognizerIntent());
                if (manualOneShot) listener.onVoiceState("Dinliyorum.");
            } catch (Throwable error) {
                listening = false;
                if (handsFreeEnabled && hostActive) startListeningWhenReady(HARD_ERROR_RESTART_MS);
                else if (manualOneShot) listener.onVoiceError("Sesli komut başlatılamadı.");
            }
        }, Math.max(0L, delayMs));
    }

    private boolean safeSpeechBusy() {
        try { return speechBusy.getAsBoolean(); }
        catch (Throwable ignored) { return false; }
    }

    private void cancelCurrent() {
        try { recognizer.cancel(); } catch (Throwable ignored) {}
        listening = false;
    }

    private void dispatchRecognized(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!NavigationVoiceBridge.tryHandle(text)) listener.onVoiceText(text);
    }

    @Override public void onResults(Bundle results) {
        listening = false;
        ArrayList<String> items = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String best = items == null || items.isEmpty() ? "" : items.get(0).trim();

        if (manualOneShot) {
            manualOneShot = false;
            if (!best.isEmpty()) dispatchRecognized(best);
            else listener.onVoiceError("Ses anlaşılamadı.");
            if (handsFreeEnabled && hostActive) startListeningWhenReady(NORMAL_RESTART_MS);
            return;
        }

        if (!handsFreeEnabled || !hostActive) return;
        if (best.isEmpty()) {
            startListeningWhenReady(NORMAL_RESTART_MS);
            return;
        }

        if (awaitingCommand) {
            awaitingCommand = false;
            dispatchRecognized(best);
            startListeningWhenReady(NORMAL_RESTART_MS);
            return;
        }

        int wakeEnd = wakePhraseEnd(best);
        if (wakeEnd >= 0) {
            String remainder = best.substring(Math.min(wakeEnd, best.length())).trim();
            if (!remainder.isEmpty()) dispatchRecognized(remainder);
            else {
                awaitingCommand = true;
                listener.onVoiceState("Dinliyorum.");
            }
        }
        startListeningWhenReady(awaitingCommand ? 180L : NORMAL_RESTART_MS);
    }

    private static int wakePhraseEnd(String text) {
        if (text == null) return -1;
        String lower = text.toLowerCase(new Locale("tr", "TR"));
        String[] phrases = {"hey rehber", "hey, rehber", "hey rehberim"};
        for (String phrase : phrases) {
            int index = lower.indexOf(phrase);
            if (index >= 0) return index + phrase.length();
        }
        return -1;
    }

    @Override public void onError(int error) {
        listening = false;
        if (manualOneShot) {
            manualOneShot = false;
            listener.onVoiceError(errorMessage(error));
            if (handsFreeEnabled && hostActive) startListeningWhenReady(NORMAL_RESTART_MS);
            return;
        }
        if (!handsFreeEnabled || !hostActive) return;

        long delay = switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> NORMAL_RESTART_MS;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> BUSY_RESTART_MS;
            default -> HARD_ERROR_RESTART_MS;
        };
        long now = System.currentTimeMillis();
        if (isHardError(error) && now - lastSpokenErrorMs > 15000L) {
            lastSpokenErrorMs = now;
            listener.onVoiceState("Hey Rehber dinleme geçici olarak yeniden başlatılıyor.");
        }
        startListeningWhenReady(delay);
    }

    private static boolean isHardError(int error) {
        return error != SpeechRecognizer.ERROR_NO_MATCH
                && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
    }

    private static String errorMessage(int error) {
        return switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH -> "Ses anlaşılamadı.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Konuşma duyulmadı.";
            case SpeechRecognizer.ERROR_AUDIO -> "Mikrofon sesi alınamadı.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni gerekli.";
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                    "Türkçe ses modeli cihazda hazır değil.";
            default -> "Sesli komut alınamadı. Kod " + error;
        };
    }

    public void destroy() {
        destroyed = true;
        hostActive = false;
        handsFreeEnabled = false;
        manualOneShot = false;
        mainHandler.removeCallbacksAndMessages(null);
        try { recognizer.cancel(); } catch (Throwable ignored) {}
        try { recognizer.destroy(); } catch (Throwable ignored) {}
    }

    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() { listening = false; }
    @Override public void onPartialResults(Bundle partialResults) {}
    @Override public void onEvent(int eventType, Bundle params) {}
}
