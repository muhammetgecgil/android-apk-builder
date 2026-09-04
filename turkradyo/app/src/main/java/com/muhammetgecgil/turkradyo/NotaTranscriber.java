package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class NotaTranscriber {
    private static final Object LOCK = new Object();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final String PREF = "radio";
    private static final String LAST = "notaAiLastResult";
    private static final String ARCHIVE = "notaAiTextArchive";
    private static final int MAX_EVENTS = 1200;
    private static final int MIC_RATE = 16000;
    private static final String[] NOTE_NAMES = {"Do","Do♯","Re","Re♯","Mi","Fa","Fa♯","Sol","Sol♯","La","La♯","Si"};

    private static Context app;
    private static boolean running;
    private static int generation;
    private static int targetSeconds;
    private static long startedMs;
    private static String state = "IDLE";
    private static String method = "";
    private static String station = "";
    private static String title = "";
    private static String error = "";
    private static String warning = "";
    private static boolean textMode;
    private static final ArrayList<NoteEvent> events = new ArrayList<>();
    private static Visualizer visualizer;
    private static AudioRecord recorder;
    private static int pendingMidi = -1;
    private static int pendingFrames = 0;
    private static int silenceFrames = 0;
    private static int activeMidi = -1;
    private static long activeStart = 0;
    private static float activeConfidence = 0f;
    private static int activeConfidenceN = 0;
    private static Runnable finishTask;
    private static Runnable guardTask;

    private NotaTranscriber() {}

    static String start(Context context, int seconds, String stationName, String songTitle, boolean saveText) {
        if (context == null) return "{\"state\":\"ERROR\",\"error\":\"Context yok\"}";
        final int sec = Math.max(15, Math.min(180, seconds));
        synchronized (LOCK) {
            stopEngineLocked();
            generation++;
            app = context.getApplicationContext();
            targetSeconds = sec;
            startedMs = System.currentTimeMillis();
            station = clean(stationName);
            if (station.isEmpty()) station = app.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("name", "Türk Radyo");
            title = resolveTitle(app, songTitle, station);
            textMode = saveText;
            state = "STARTING";
            method = "";
            error = "";
            warning = "";
            running = true;
            events.clear();
            pendingMidi = -1;
            pendingFrames = 0;
            silenceFrames = 0;
            activeMidi = -1;
            activeStart = 0;
            activeConfidence = 0f;
            activeConfidenceN = 0;
            final int token = generation;
            writeSnapshotLocked();
            MAIN.post(() -> startVisualizerOrMic(token));
            finishTask = () -> finish(token, false, "");
            MAIN.postDelayed(finishTask, sec * 1000L);
            guardTask = new Runnable() {
                @Override public void run() {
                    synchronized (LOCK) {
                        if (!running || token != generation || app == null) return;
                        SharedPreferences p = app.getSharedPreferences(PREF, Context.MODE_PRIVATE);
                        String nowStation = clean(p.getString("name", ""));
                        String nowTitle = clean(p.getString("nowTitle", ""));
                        if (!nowStation.isEmpty() && !station.isEmpty() && !nowStation.equalsIgnoreCase(station)) {
                            warning = "Radyo değiştiği için analiz erken tamamlandı.";
                            MAIN.post(() -> finish(token, false, warning));
                            return;
                        }
                        if (isUsefulTitle(title) && isUsefulTitle(nowTitle) && !sameSong(title, nowTitle) && elapsedMsLocked() > 12000) {
                            warning = "Şarkı değiştiği için analiz erken tamamlandı.";
                            MAIN.post(() -> finish(token, false, warning));
                            return;
                        }
                        MAIN.postDelayed(this, 1000L);
                    }
                }
            };
            MAIN.postDelayed(guardTask, 1500L);
        }
        return getStatusJson(context);
    }

    static void permissionDenied(Context context) {
        synchronized (LOCK) {
            stopEngineLocked();
            app = context == null ? app : context.getApplicationContext();
            running = false;
            state = "ERROR";
            error = "Ses analizi izni verilmedi";
            warning = "";
            writeSnapshotLocked();
        }
    }

    static String stop(Context context) {
        final int token;
        synchronized (LOCK) { token = generation; }
        finish(token, false, "Kullanıcı tarafından tamamlandı.");
        return getStatusJson(context);
    }

    static String getStatusJson(Context context) {
        synchronized (LOCK) {
            if (app == null && context != null) app = context.getApplicationContext();
            if (("IDLE".equals(state) || (!running && events.isEmpty())) && app != null) {
                String saved = app.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(LAST, "");
                if (!saved.isEmpty()) return saved;
            }
            return buildJsonLocked().toString();
        }
    }

    static String getTextArchive(Context context) {
        if (context == null) return "[]";
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(ARCHIVE, "[]");
    }

    static void clearTextArchive(Context context) {
        if (context != null) context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(ARCHIVE, "[]").apply();
    }

    private static void startVisualizerOrMic(int token) {
        synchronized (LOCK) {
            if (!running || token != generation) return;
        }
        try {
            Visualizer v = new Visualizer(0);
            int[] range = Visualizer.getCaptureSizeRange();
            int size = range == null || range.length < 2 ? 1024 : range[1];
            size = Math.max(128, Math.min(2048, size));
            v.setCaptureSize(size);
            int rate = Math.min(Visualizer.getMaxCaptureRate(), 20000);
            v.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {}
                @Override public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    analyzeFft(token, fft, samplingRate);
                }
            }, Math.max(5000, rate), false, true);
            v.setEnabled(true);
            synchronized (LOCK) {
                if (!running || token != generation) { try { v.release(); } catch (Exception ignored) {} return; }
                visualizer = v;
                method = "OUTPUT_FFT";
                state = "RUNNING";
                writeSnapshotLocked();
            }
        } catch (Throwable visualizerError) {
            startMic(token, visualizerError.getClass().getSimpleName());
        }
    }

    private static void startMic(int token, String visualizerError) {
        synchronized (LOCK) {
            if (!running || token != generation) return;
            method = "MIC_FALLBACK";
            warning = "Çıkış FFT kullanılamadı; mikrofon yedeği aktif" + (visualizerError == null || visualizerError.isEmpty() ? "" : " (" + visualizerError + ")");
            state = "RUNNING";
            writeSnapshotLocked();
        }
        IO.execute(() -> {
            AudioRecord r = null;
            try {
                int min = AudioRecord.getMinBufferSize(MIC_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int bufBytes = Math.max(min, 4096 * 2);
                r = new AudioRecord(MediaRecorder.AudioSource.MIC, MIC_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufBytes);
                if (r.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("AudioRecord başlatılamadı");
                synchronized (LOCK) {
                    if (!running || token != generation) { r.release(); return; }
                    recorder = r;
                }
                short[] buf = new short[2048];
                r.startRecording();
                while (true) {
                    synchronized (LOCK) { if (!running || token != generation) break; }
                    int n = r.read(buf, 0, buf.length);
                    if (n > 512) {
                        Pitch p = estimateMicPitch(buf, n, MIC_RATE);
                        acceptPitch(token, p.midi, p.confidence, System.currentTimeMillis());
                    }
                }
            } catch (Throwable e) {
                fail(token, "Ses yakalama başlatılamadı: " + e.getClass().getSimpleName());
            } finally {
                if (r != null) {
                    try { r.stop(); } catch (Exception ignored) {}
                    try { r.release(); } catch (Exception ignored) {}
                }
                synchronized (LOCK) { if (recorder == r) recorder = null; }
            }
        });
    }

    private static void analyzeFft(int token, byte[] fft, int samplingRateMilliHz) {
        if (fft == null || fft.length < 64) return;
        synchronized (LOCK) { if (!running || token != generation) return; }
        double sr = samplingRateMilliHz / 1000.0;
        if (sr < 4000) return;
        int n = fft.length;
        int bins = n / 2;
        double[] mag = new double[bins];
        int lo = Math.max(1, (int)Math.ceil(55.0 * n / sr));
        int hi = Math.min(bins - 2, (int)Math.floor(1400.0 * n / sr));
        if (hi <= lo) return;
        double mean = 0;
        int count = 0;
        for (int k = lo; k <= hi; k++) {
            int ri = 2 * k;
            int ii = ri + 1;
            if (ii >= fft.length) break;
            double re = fft[ri];
            double im = fft[ii];
            double m = Math.hypot(re, im);
            mag[k] = m;
            mean += m;
            count++;
        }
        if (count == 0) return;
        mean /= count;
        double best = 0;
        int bestBin = -1;
        for (int k = lo; k <= hi; k++) {
            double score = mag[k];
            if (2 * k < bins) score += .48 * mag[2 * k];
            if (3 * k < bins) score += .24 * mag[3 * k];
            if (score > best) { best = score; bestBin = k; }
        }
        if (bestBin < 0 || best < Math.max(8.0, mean * 2.6)) {
            acceptPitch(token, -1, 0f, System.currentTimeMillis());
            return;
        }
        double freq = bestBin * sr / n;
        int midi = frequencyToMidi(freq);
        float conf = (float)Math.max(0, Math.min(1, (best / Math.max(1.0, mean) - 2.0) / 8.0));
        acceptPitch(token, midi, conf, System.currentTimeMillis());
    }

    private static Pitch estimateMicPitch(short[] data, int n, int rate) {
        if (n < 512) return Pitch.NONE;
        double mean = 0;
        for (int i = 0; i < n; i++) mean += data[i];
        mean /= n;
        double energy = 0;
        for (int i = 0; i < n; i++) { double x = data[i] - mean; energy += x * x; }
        double rms = Math.sqrt(energy / n);
        if (rms < 280) return Pitch.NONE;
        int minLag = Math.max(8, rate / 1000);
        int maxLag = Math.min(n / 2, rate / 55);
        double best = 0;
        int bestLag = -1;
        for (int lag = minLag; lag <= maxLag; lag++) {
            double sum = 0, e1 = 0, e2 = 0;
            int limit = n - lag;
            for (int i = 0; i < limit; i += 2) {
                double a = data[i] - mean;
                double b = data[i + lag] - mean;
                sum += a * b;
                e1 += a * a;
                e2 += b * b;
            }
            double corr = sum / Math.sqrt(Math.max(1.0, e1 * e2));
            if (corr > best) { best = corr; bestLag = lag; }
        }
        if (bestLag <= 0 || best < .53) return Pitch.NONE;
        double freq = (double)rate / bestLag;
        int midi = frequencyToMidi(freq);
        return midi < 0 ? Pitch.NONE : new Pitch(midi, (float)Math.min(1, best));
    }

    private static int frequencyToMidi(double f) {
        if (f < 55 || f > 1800) return -1;
        int midi = (int)Math.round(69 + 12.0 * (Math.log(f / 440.0) / Math.log(2.0)));
        return midi < 33 || midi > 96 ? -1 : midi;
    }

    private static void acceptPitch(int token, int midi, float confidence, long nowMs) {
        synchronized (LOCK) {
            if (!running || token != generation) return;
            long at = Math.max(0, nowMs - startedMs);
            if (midi < 0) {
                silenceFrames++;
                pendingFrames = 0;
                pendingMidi = -1;
                if (silenceFrames >= 4) closeActiveLocked(at);
                return;
            }
            silenceFrames = 0;
            if (midi == pendingMidi) pendingFrames++; else { pendingMidi = midi; pendingFrames = 1; }
            if (pendingFrames < 2) return;
            if (activeMidi != midi) {
                closeActiveLocked(at);
                activeMidi = midi;
                activeStart = at;
                activeConfidence = confidence;
                activeConfidenceN = 1;
            } else {
                activeConfidence += confidence;
                activeConfidenceN++;
            }
        }
    }

    private static void closeActiveLocked(long endMs) {
        if (activeMidi < 0) return;
        long duration = Math.max(0, endMs - activeStart);
        if (duration >= 120 && events.size() < MAX_EVENTS) {
            float c = activeConfidenceN == 0 ? 0f : activeConfidence / activeConfidenceN;
            NoteEvent last = events.isEmpty() ? null : events.get(events.size() - 1);
            if (last != null && last.midi == activeMidi && activeStart - last.endMs < 320) {
                last.endMs = Math.max(last.endMs, endMs);
                last.confidence = Math.max(last.confidence, c);
            } else {
                events.add(new NoteEvent(activeMidi, activeStart, endMs, c));
            }
        }
        activeMidi = -1;
        activeStart = 0;
        activeConfidence = 0f;
        activeConfidenceN = 0;
    }

    private static void finish(int token, boolean failed, String message) {
        synchronized (LOCK) {
            if (token != generation || (!running && !"STARTING".equals(state) && !"RUNNING".equals(state))) return;
            long end = elapsedMsLocked();
            closeActiveLocked(end);
            running = false;
            stopEngineLocked();
            if (failed) {
                state = "ERROR";
                error = message == null ? "Analiz hatası" : message;
            } else {
                state = "DONE";
                if (message != null && !message.isEmpty()) warning = message;
            }
            JSONObject out = buildJsonLocked();
            if (app != null) {
                SharedPreferences p = app.getSharedPreferences(PREF, Context.MODE_PRIVATE);
                p.edit().putString(LAST, out.toString()).apply();
                if (textMode && "DONE".equals(state)) saveTextLocked(p, out);
            }
        }
    }

    private static void fail(int token, String message) { MAIN.post(() -> finish(token, true, message)); }

    private static void stopEngineLocked() {
        if (finishTask != null) MAIN.removeCallbacks(finishTask);
        if (guardTask != null) MAIN.removeCallbacks(guardTask);
        finishTask = null;
        guardTask = null;
        if (visualizer != null) {
            try { visualizer.setEnabled(false); } catch (Exception ignored) {}
            try { visualizer.release(); } catch (Exception ignored) {}
            visualizer = null;
        }
        AudioRecord r = recorder;
        recorder = null;
        if (r != null) {
            try { r.stop(); } catch (Exception ignored) {}
        }
    }

    private static long elapsedMsLocked() {
        if (startedMs <= 0) return 0;
        return Math.min(targetSeconds * 1000L, Math.max(0, System.currentTimeMillis() - startedMs));
    }

    private static JSONObject buildJsonLocked() {
        JSONObject o = new JSONObject();
        try {
            long elapsed = elapsedMsLocked();
            int progress = targetSeconds <= 0 ? 0 : (int)Math.min(100, Math.round(100.0 * elapsed / (targetSeconds * 1000.0)));
            o.put("state", state);
            o.put("running", running);
            o.put("progress", "DONE".equals(state) ? 100 : progress);
            o.put("targetSeconds", targetSeconds);
            o.put("elapsedMs", elapsed);
            o.put("station", station);
            o.put("title", title);
            o.put("method", method);
            o.put("textMode", textMode);
            o.put("error", error);
            o.put("warning", warning);
            JSONArray a = new JSONArray();
            for (NoteEvent e : events) a.put(e.toJson());
            o.put("notes", a);
            o.put("noteCount", events.size());
            o.put("sequence", sequenceLocked());
            o.put("text", textLocked(elapsed));
        } catch (Exception ignored) {}
        return o;
    }

    private static String sequenceLocked() {
        StringBuilder b = new StringBuilder();
        String last = "";
        for (NoteEvent e : events) {
            String n = noteName(e.midi);
            if (n.equals(last) && e.durationMs() < 450) continue;
            if (b.length() > 0) b.append("  –  ");
            b.append(n);
            last = n;
            if (b.length() > 5000) break;
        }
        return b.toString();
    }

    private static String textLocked(long elapsed) {
        StringBuilder b = new StringBuilder();
        b.append("Şarkı: ").append(title).append('\n');
        b.append("Radyo: ").append(station).append('\n');
        b.append("Analiz süresi: ").append(formatTime(elapsed)).append('\n');
        b.append("Kaynak: ").append("OUTPUT_FFT".equals(method) ? "Cihaz çıkış FFT" : "Mikrofon yedeği").append('\n');
        b.append("\nNotalar:\n");
        if (events.isEmpty()) b.append("Belirgin perde tespit edilemedi.");
        for (NoteEvent e : events) {
            b.append(formatTime(e.startMs)).append("  ").append(noteName(e.midi)).append("  ").append(String.format(Locale.US, "%.2f sn", e.durationMs() / 1000.0)).append('\n');
            if (b.length() > 20000) { b.append("…\n"); break; }
        }
        if (!warning.isEmpty()) b.append("\nNot: ").append(warning);
        return b.toString();
    }

    private static void saveTextLocked(SharedPreferences p, JSONObject out) {
        try {
            JSONArray old;
            try { old = new JSONArray(p.getString(ARCHIVE, "[]")); } catch (Exception e) { old = new JSONArray(); }
            JSONArray next = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("id", System.currentTimeMillis());
            item.put("time", System.currentTimeMillis());
            item.put("title", title);
            item.put("station", station);
            item.put("method", method);
            item.put("noteCount", events.size());
            item.put("text", out.optString("text", ""));
            item.put("sequence", out.optString("sequence", ""));
            next.put(item);
            for (int i = 0; i < old.length() && next.length() < 60; i++) {
                JSONObject x = old.optJSONObject(i);
                if (x != null) next.put(x);
            }
            p.edit().putString(ARCHIVE, next.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static void writeSnapshotLocked() {
        if (app == null) return;
        app.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(LAST, buildJsonLocked().toString()).apply();
    }

    private static String resolveTitle(Context c, String proposed, String stationName) {
        String x = clean(proposed);
        if (!isUsefulTitle(x)) x = clean(c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("nowTitle", ""));
        if (!isUsefulTitle(x)) x = "Şarkı adı alınamadı";
        if (!clean(stationName).isEmpty() && x.equalsIgnoreCase(stationName)) x = "Şarkı adı alınamadı";
        return x;
    }

    private static boolean isUsefulTitle(String x) {
        String s = clean(x);
        return !s.isEmpty() && !s.equalsIgnoreCase("Canlı yayın") && !s.equalsIgnoreCase("Canlı radyo") && !s.equalsIgnoreCase("Türk Radyo") && !s.equalsIgnoreCase("Şarkı adı alınamadı");
    }

    private static boolean sameSong(String a, String b) {
        return clean(a).replaceAll("\\s+", " ").equalsIgnoreCase(clean(b).replaceAll("\\s+", " "));
    }

    private static String noteName(int midi) {
        if (midi < 0) return "—";
        int pc = ((midi % 12) + 12) % 12;
        int octave = midi / 12 - 1;
        return NOTE_NAMES[pc] + octave;
    }

    private static String formatTime(long ms) {
        long total = Math.max(0, ms) / 1000;
        long min = total / 60;
        long sec = total % 60;
        long tenth = (Math.max(0, ms) % 1000) / 100;
        return String.format(Locale.US, "%02d:%02d.%d", min, sec, tenth);
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }

    private static final class Pitch {
        static final Pitch NONE = new Pitch(-1, 0f);
        final int midi;
        final float confidence;
        Pitch(int midi, float confidence) { this.midi = midi; this.confidence = confidence; }
    }

    private static final class NoteEvent {
        final int midi;
        final long startMs;
        long endMs;
        float confidence;
        NoteEvent(int midi, long startMs, long endMs, float confidence) {
            this.midi = midi;
            this.startMs = startMs;
            this.endMs = endMs;
            this.confidence = confidence;
        }
        long durationMs() { return Math.max(0, endMs - startMs); }
        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("midi", midi);
                o.put("note", noteName(midi));
                o.put("startMs", startMs);
                o.put("durationMs", durationMs());
                o.put("confidence", Math.round(confidence * 100));
            } catch (Exception ignored) {}
            return o;
        }
    }
}
