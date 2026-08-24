package com.mgai.app;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public final class LocalAudioRecorder {
    private static final int SAMPLE_RATE = 16000;
    private volatile boolean recording;
    private AudioRecord audioRecord;
    private Thread thread;
    private ByteArrayOutputStream pcm;

    public void start() {
        if (recording) return;
        int min = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(min, SAMPLE_RATE * 2);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("audio_record_init_failed");
        }
        pcm = new ByteArrayOutputStream();
        recording = true;
        audioRecord.startRecording();
        thread = new Thread(() -> {
            byte[] buf = new byte[bufferSize];
            while (recording) {
                int n = audioRecord.read(buf, 0, buf.length);
                if (n > 0) pcm.write(buf, 0, n);
            }
        }, "mg-ai-audio-record");
        thread.start();
    }

    public File stopToWav(File out) throws Exception {
        if (!recording) throw new IllegalStateException("not_recording");
        recording = false;
        try { if (thread != null) thread.join(1500); } catch (InterruptedException ignored) {}
        try { audioRecord.stop(); } catch (Exception ignored) {}
        audioRecord.release();
        byte[] data = pcm.toByteArray();
        try (FileOutputStream fos = new FileOutputStream(out)) {
            writeWavHeader(fos, data.length, SAMPLE_RATE, 1, 16);
            fos.write(data);
        }
        return out;
    }

    public boolean isRecording() { return recording; }

    private static void writeWavHeader(FileOutputStream out, int pcmBytes, int sampleRate, int channels, int bits) throws Exception {
        int byteRate = sampleRate * channels * bits / 8;
        int blockAlign = channels * bits / 8;
        int riffSize = 36 + pcmBytes;
        out.write(new byte[]{'R','I','F','F'}); writeLE32(out, riffSize);
        out.write(new byte[]{'W','A','V','E','f','m','t',' '}); writeLE32(out, 16);
        writeLE16(out, 1); writeLE16(out, channels); writeLE32(out, sampleRate);
        writeLE32(out, byteRate); writeLE16(out, blockAlign); writeLE16(out, bits);
        out.write(new byte[]{'d','a','t','a'}); writeLE32(out, pcmBytes);
    }
    private static void writeLE16(FileOutputStream out, int v) throws Exception { out.write(v & 255); out.write((v >> 8) & 255); }
    private static void writeLE32(FileOutputStream out, int v) throws Exception { out.write(v & 255); out.write((v >> 8) & 255); out.write((v >> 16) & 255); out.write((v >> 24) & 255); }
}
