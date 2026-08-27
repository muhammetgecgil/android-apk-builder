package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public final class GuidanceSpeaker implements TextToSpeech.OnInitListener {
    private final TextToSpeech tts;
    private final Vibrator vibrator;
    private volatile boolean ready;
    private String lastSpeech = "";

    public GuidanceSpeaker(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this);
        tts.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        vibrator = context.getSystemService(Vibrator.class);
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
        lastSpeech = decision.speech();
        if (ready) tts.speak(decision.speech(), TextToSpeech.QUEUE_FLUSH, null, "guidance");
        vibrate(decision.direction(), decision.risk());
    }

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        lastSpeech = text;
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speech");
    }

    public boolean isSpeaking() {
        try { return ready && tts.isSpeaking(); }
        catch (Throwable ignored) { return false; }
    }

    public void repeat() { speak(lastSpeech); }

    public void shutdown() {
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
