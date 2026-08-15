package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Build;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** In-memory event/time-series logger with explicit CSV export to Downloads. */
public final class EventLogger {
    private final StringBuilder csv = new StringBuilder(64 * 1024);
    private boolean recording = false;
    private long startNs = 0L;
    private long lastRowNs = 0L;
    private int rows = 0;

    public synchronized void start() {
        csv.setLength(0);
        csv.append("elapsed_s,wall_time,dbfs,sound_class,audio_level,motion,source_confidence,source_x,source_y,object_label,object_track,object_score,azimuth_deg,elevation_deg,imu_motion,ai_ready\n");
        recording = true;
        startNs = System.nanoTime();
        lastRowNs = 0L;
        rows = 0;
    }

    public synchronized void stop() { recording = false; }
    public synchronized boolean isRecording() { return recording; }
    public synchronized int getRows() { return rows; }

    public synchronized void log(AudioEngine.Snapshot a, FusionEngine.Result r,
                                 ImuEngine.Snapshot imu, AiObjectDetector.Result ai) {
        if (!recording || r == null) return;
        long now = System.nanoTime();
        if (lastRowNs != 0L && now - lastRowNs < 120_000_000L) return; // <= 8.3 Hz CSV
        lastRowNs = now;
        double elapsed = (now - startNs) / 1_000_000_000.0;
        float db = a == null ? -120f : a.dbfs;
        float level = a == null ? 0f : a.level01;
        String cls = a == null ? "" : a.soundClass;
        float imuMotion = imu == null ? 0f : imu.motion01;
        boolean aiReady = ai != null && ai.aiReady;
        int sx = r.bestCell < 0 ? -1 : (r.bestCell % FusionEngine.COLS) + 1;
        int sy = r.bestCell < 0 ? -1 : (r.bestCell / FusionEngine.COLS) + 1;
        csv.append(String.format(Locale.US,
                "%.3f,%s,%.2f,%s,%.4f,%.4f,%.4f,%d,%d,%s,%d,%.4f,%.2f,%.2f,%.4f,%s\n",
                elapsed,
                new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date()),
                db, q(cls), level, r.motion, r.confidence, sx, sy,
                q(r.bestObjectLabel), r.bestObjectTrackId, r.bestObjectScore,
                r.azimuthDeg, r.elevationDeg, imuMotion, aiReady ? "1" : "0"));
        rows++;
        if (csv.length() > 4_000_000) recording = false;
    }

    public synchronized Uri export(Context context) throws Exception {
        if (csv.length() == 0) throw new IllegalStateException("Kayıt verisi yok");
        String name = "SesGoruntu_V7_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
            cv.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SesGoruntuHaritasi");
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) throw new IllegalStateException("Downloads kaydı açılamadı");
            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                if (os == null) throw new IllegalStateException("Dosya açılamadı");
                os.write(csv.toString().getBytes(StandardCharsets.UTF_8)); os.flush();
            }
            return uri;
        } else {
            File base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (base == null) base = context.getFilesDir();
            File dir = new File(base, "SesGoruntuHaritasi"); if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, name);
            try (FileOutputStream os = new FileOutputStream(file)) { os.write(csv.toString().getBytes(StandardCharsets.UTF_8)); os.flush(); }
            return Uri.fromFile(file);
        }
    }

    private static String q(String s) {
        if (s == null) s = "";
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
