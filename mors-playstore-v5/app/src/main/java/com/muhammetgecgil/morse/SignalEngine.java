package com.muhammetgecgil.morse;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class SignalEngine {
    public interface Completion { void onComplete(); }

    private static final int SAMPLE_RATE = 44_100;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger generation = new AtomicInteger();
    private volatile AudioTrack audioTrack;
    private volatile String activeTorchId;

    public SignalEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public void playAudio(String morse, int wpm, int frequencyHz, Completion completion) {
        cancelAll();
        final int token = generation.incrementAndGet();
        final List<Segment> segments = segmentsFromMorse(morse, unitMs(wpm));
        if (segments.isEmpty()) {
            post(completion);
            return;
        }

        executor.execute(() -> {
            int minBuffer = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int buffer = Math.max(minBuffer, SAMPLE_RATE / 4);
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(buffer)
                    .build();
            audioTrack = track;
            try {
                track.play();
                double phase = 0.0;
                for (Segment segment : segments) {
                    if (generation.get() != token) break;
                    int count = Math.max(1, (int) ((long) SAMPLE_RATE * segment.durationMs / 1000L));
                    short[] pcm = new short[count];
                    if (segment.on) {
                        double step = 2.0 * Math.PI * frequencyHz / SAMPLE_RATE;
                        int fadeSamples = Math.min(count / 4, SAMPLE_RATE / 200);
                        for (int i = 0; i < count; i++) {
                            double envelope = 1.0;
                            if (fadeSamples > 0 && i < fadeSamples) envelope = i / (double) fadeSamples;
                            else if (fadeSamples > 0 && i >= count - fadeSamples) envelope = (count - 1 - i) / (double) fadeSamples;
                            envelope = Math.max(0.0, Math.min(1.0, envelope));
                            pcm[i] = (short) (Math.sin(phase) * 10_500 * envelope);
                            phase += step;
                            if (phase > Math.PI * 2) phase -= Math.PI * 2;
                        }
                    }
                    int written = 0;
                    while (written < pcm.length && generation.get() == token) {
                        int n = track.write(pcm, written, pcm.length - written, AudioTrack.WRITE_BLOCKING);
                        if (n <= 0) break;
                        written += n;
                    }
                }
            } finally {
                try { track.pause(); } catch (Exception ignored) {}
                try { track.flush(); } catch (Exception ignored) {}
                try { track.release(); } catch (Exception ignored) {}
                if (audioTrack == track) audioTrack = null;
                if (generation.get() == token) post(completion);
            }
        });
    }

    public boolean vibrate(String morse, int wpm, int amplitude) {
        cancelAll();
        generation.incrementAndGet();
        List<Segment> segments = segmentsFromMorse(morse, unitMs(wpm));
        if (segments.isEmpty()) return false;
        Vibrator vibrator = getVibrator();
        if (vibrator == null || !vibrator.hasVibrator()) return false;

        long[] timings = new long[segments.size()];
        int[] amplitudes = new int[segments.size()];
        int safeAmplitude = Math.max(1, Math.min(255, amplitude));
        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            timings[i] = s.durationMs;
            amplitudes[i] = s.on ? safeAmplitude : 0;
        }
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
        return true;
    }

    public boolean hasVibrator() {
        Vibrator vibrator = getVibrator();
        return vibrator != null && vibrator.hasVibrator();
    }

    public boolean hasTorch() {
        return findTorchId() != null;
    }

    public void playTorch(String morse, int wpm, Completion completion) {
        cancelAll();
        final int token = generation.incrementAndGet();
        final String cameraId = findTorchId();
        if (cameraId == null) {
            post(completion);
            return;
        }
        final CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        final List<Segment> segments = segmentsFromMorse(morse, unitMs(wpm));
        activeTorchId = cameraId;
        executor.execute(() -> {
            try {
                for (Segment s : segments) {
                    if (generation.get() != token) break;
                    try {
                        manager.setTorchMode(cameraId, s.on);
                    } catch (SecurityException | CameraAccessException e) {
                        break;
                    }
                    SystemClock.sleep(s.durationMs);
                }
            } finally {
                try { manager.setTorchMode(cameraId, false); } catch (Exception ignored) {}
                activeTorchId = null;
                if (generation.get() == token) post(completion);
            }
        });
    }

    public void cancelAll() {
        generation.incrementAndGet();
        AudioTrack track = audioTrack;
        if (track != null) {
            try { track.pause(); } catch (Exception ignored) {}
            try { track.flush(); } catch (Exception ignored) {}
        }
        Vibrator vibrator = getVibrator();
        if (vibrator != null) {
            try { vibrator.cancel(); } catch (Exception ignored) {}
        }
        String torchId = activeTorchId;
        if (torchId != null) {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            try { manager.setTorchMode(torchId, false); } catch (Exception ignored) {}
            activeTorchId = null;
        }
    }

    public void shutdown() {
        cancelAll();
        executor.shutdownNow();
    }

    private Vibrator getVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return manager == null ? null : manager.getDefaultVibrator();
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private String findTorchId() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return null;
        try {
            String fallback = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics c = manager.getCameraCharacteristics(id);
                Boolean flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (!Boolean.TRUE.equals(flash)) continue;
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (fallback == null) fallback = id;
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
            }
            return fallback;
        } catch (CameraAccessException | SecurityException e) {
            return null;
        }
    }

    private static int unitMs(int wpm) {
        int safe = Math.max(5, Math.min(40, wpm));
        return Math.max(30, 1200 / safe);
    }

    static List<Segment> segmentsFromMorse(String morse, int unitMs) {
        List<Segment> out = new ArrayList<>();
        if (morse == null || morse.trim().isEmpty()) return out;
        String normalized = morse.trim().replace('•', '.').replace('·', '.').replace('–', '-').replace('—', '-');
        String[] words = normalized.split("\\s*/\\s*");
        for (int w = 0; w < words.length; w++) {
            String word = words[w].trim();
            if (!word.isEmpty()) {
                String[] letters = word.split("\\s+");
                for (int l = 0; l < letters.length; l++) {
                    String code = letters[l];
                    for (int i = 0; i < code.length(); i++) {
                        char c = code.charAt(i);
                        if (c != '.' && c != '-') continue;
                        out.add(new Segment(true, c == '.' ? unitMs : unitMs * 3));
                        if (i < code.length() - 1) out.add(new Segment(false, unitMs));
                    }
                    if (l < letters.length - 1) out.add(new Segment(false, unitMs * 3));
                }
            }
            if (w < words.length - 1) out.add(new Segment(false, unitMs * 7));
        }
        return out;
    }

    private void post(Completion completion) {
        if (completion == null) return;
        android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        main.post(completion::onComplete);
    }

    static final class Segment {
        final boolean on;
        final int durationMs;
        Segment(boolean on, int durationMs) { this.on = on; this.durationMs = durationMs; }
    }
}
