package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Low-overhead device-validation recorder. Files stay in app cache and can be shared explicitly by
 * the user. No network upload is performed.
 */
public final class GateCRecorder implements AutoCloseable {
    private static final long FRAME_LOG_INTERVAL_MS = 250L;
    private static final long THERMAL_SAMPLE_INTERVAL_MS = 2000L;

    private final Context context;
    private final GateCMetrics metrics = new GateCMetrics();
    private BufferedWriter writer;
    private File currentFile;
    private File lastFile;
    private boolean active;
    private int rowsSinceFlush;
    private long lastMotionLogMs;
    private long lastGroundLogMs;
    private long lastDepthLogMs;
    private long lastThermalSampleMs;
    private float cachedBatteryC = Float.NaN;
    private int cachedThermalStatus;
    private GateCMetrics.Snapshot lastSnapshot;

    public GateCRecorder(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized boolean start(String version, String initialMode) {
        stopInternal();
        try {
            File dir = new File(context.getCacheDir(), "gate_c");
            if (!dir.exists() && !dir.mkdirs()) return false;
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            currentFile = new File(dir, "SesliRehber_GateC_" + stamp + ".csv");
            writer = new BufferedWriter(new FileWriter(currentFile, false), 32 * 1024);
            writer.write("wall_ms,elapsed_ms,event,mode,source_age_ms,stability,depth_valid,depth_confidence,ground_anomaly,ground_persistence,object_area,object_growth,risk,decision_confidence,battery_c,thermal_status,detail\n");
            metrics.reset(SystemClock.elapsedRealtime());
            active = true;
            lastSnapshot = null;
            recordRaw("SESSION_START", initialMode, -1L, Float.NaN,
                    Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                    "", Float.NaN, "version=" + version);
            return true;
        } catch (IOException error) {
            stopInternal();
            return false;
        }
    }

    public synchronized boolean isActive() { return active; }

    public synchronized String stop() {
        if (!active) return summaryText();
        recordRaw("SESSION_STOP", "", -1L, Float.NaN,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                "", Float.NaN, "user_stop");
        lastSnapshot = metrics.snapshot(SystemClock.elapsedRealtime());
        stopInternal();
        return summaryText();
    }

    public synchronized File lastReportFile() {
        return lastFile != null && lastFile.exists() ? lastFile : null;
    }

    public synchronized void recordMode(String mode, String detail) {
        if (!active) return;
        recordRaw("MODE", mode, -1L, Float.NaN,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                "", Float.NaN, detail);
    }

    public synchronized void recordFallback(String mode, String detail) {
        if (!active) return;
        metrics.noteFallback();
        recordRaw("FALLBACK", mode, -1L, Float.NaN,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                "", Float.NaN, detail);
    }

    public synchronized void recordWatchdog(String mode, String detail, long ageMs, float stability) {
        if (!active) return;
        recordRaw("WATCHDOG", mode, ageMs, stability,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                "", Float.NaN, detail);
    }

    public synchronized void recordMotion(String mode, MotionObservation observation, float stability) {
        if (!active) return;
        metrics.noteVision();
        long now = SystemClock.elapsedRealtime();
        if (now - lastMotionLogMs < FRAME_LOG_INTERVAL_MS) return;
        lastMotionLogMs = now;
        recordRaw("MOTION", mode, sourceAge(observation.timestampMs()), stability,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                observation.changedAreaRatio(), Float.NaN,
                "", observation.visionConfidence(),
                "cx=" + fmt(observation.centroidX()) + ";cy=" + fmt(observation.centroidY()));
    }

    public synchronized void recordGround(String mode, GroundObservation observation, float stability) {
        if (!active) return;
        metrics.noteGround();
        long now = SystemClock.elapsedRealtime();
        if (now - lastGroundLogMs < FRAME_LOG_INTERVAL_MS) return;
        lastGroundLogMs = now;
        recordRaw("GROUND", mode, sourceAge(observation.timestampMs()), stability,
                Float.NaN, Float.NaN,
                observation.anomalyScore(), observation.persistenceScore(),
                Float.NaN, Float.NaN, "", observation.viewConfidence(),
                "boundary=" + fmt(observation.broadBoundaryScore()) + ";y=" + fmt(observation.boundaryY()));
    }

    public synchronized void recordDepth(String mode, DepthObservation observation, float stability) {
        if (!active) return;
        metrics.noteDepth(observation.validRatio());
        long now = SystemClock.elapsedRealtime();
        if (now - lastDepthLogMs < FRAME_LOG_INTERVAL_MS) return;
        lastDepthLogMs = now;
        recordRaw("DEPTH", mode, sourceAge(observation.timestampMs()), stability,
                observation.validRatio(), observation.depthConfidence(),
                Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                "", observation.discontinuityScore(),
                "jump_mm=" + Math.round(observation.maxBandJumpMm()));
    }

    public synchronized void recordObject(String mode, ObjectObservation observation, float stability) {
        if (!active) return;
        metrics.noteObject();
        recordRaw("OBJECT", mode, sourceAge(observation.timestampMs()), stability,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                observation.areaRatio(), observation.growthPerSecond(),
                "", observation.visionConfidence(),
                "id=" + observation.trackingId() + ";x=" + fmt(observation.centerX()));
    }

    public synchronized void recordDecision(
            String source,
            String mode,
            GuidanceDecision decision,
            long sourceTimestampMs,
            float stability) {
        if (!active || decision.risk() == Risk.INFO) return;
        long latency = sourceAge(sourceTimestampMs);
        metrics.noteDecision(decision.risk().name(), latency);
        recordRaw("DECISION_" + source, mode, latency, stability,
                Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN,
                decision.risk().name(), decision.confidence(), decision.speech());
    }

    public synchronized String summaryText() {
        GateCMetrics.Snapshot s = active
                ? metrics.snapshot(SystemClock.elapsedRealtime())
                : lastSnapshot;
        if (s == null) return "Gate C raporu henüz oluşturulmadı.";
        String battery = Float.isNaN(s.maxBatteryC()) ? "n/a" : String.format(Locale.US, "%.1f C", s.maxBatteryC());
        return "Gate C: "
                + Math.round(s.durationMs() / 1000f) + " sn, "
                + "vision=" + s.visionFrames()
                + ", depth=" + s.depthFrames()
                + ", depth coverage=" + Math.round(s.meanDepthValidRatio() * 100f) + "%"
                + ", caution=" + s.cautionDecisions()
                + ", stop=" + s.stopDecisions()
                + ", fallback=" + s.fallbacks()
                + ", p95 karar=" + s.p95DecisionLatencyMs() + " ms"
                + ", max batarya=" + battery
                + ", thermal=" + s.maxThermalStatus() + ".";
    }

    private void recordRaw(
            String event,
            String mode,
            long sourceAgeMs,
            float stability,
            float depthValid,
            float depthConfidence,
            float groundAnomaly,
            float groundPersistence,
            float objectArea,
            float objectGrowth,
            String risk,
            float decisionConfidence,
            String detail) {
        if (!active || writer == null) return;
        refreshThermalIfNeeded();
        long wall = System.currentTimeMillis();
        long elapsed = SystemClock.elapsedRealtime();
        try {
            writer.write(Long.toString(wall)); writer.write(',');
            writer.write(Long.toString(elapsed)); writer.write(',');
            writer.write(csv(event)); writer.write(',');
            writer.write(csv(mode)); writer.write(',');
            writer.write(sourceAgeMs < 0 ? "" : Long.toString(sourceAgeMs)); writer.write(',');
            writer.write(num(stability)); writer.write(',');
            writer.write(num(depthValid)); writer.write(',');
            writer.write(num(depthConfidence)); writer.write(',');
            writer.write(num(groundAnomaly)); writer.write(',');
            writer.write(num(groundPersistence)); writer.write(',');
            writer.write(num(objectArea)); writer.write(',');
            writer.write(num(objectGrowth)); writer.write(',');
            writer.write(csv(risk)); writer.write(',');
            writer.write(num(decisionConfidence)); writer.write(',');
            writer.write(num(cachedBatteryC)); writer.write(',');
            writer.write(Integer.toString(cachedThermalStatus)); writer.write(',');
            writer.write(csv(detail)); writer.write('\n');
            rowsSinceFlush++;
            if (rowsSinceFlush >= 20 || (risk != null && "STOP".equals(risk))) {
                writer.flush();
                rowsSinceFlush = 0;
            }
        } catch (IOException ignored) {
            // Recording must never crash or block the safety guidance path.
        }
    }

    private long sourceAge(long sourceTimestampMs) {
        if (sourceTimestampMs <= 0L) return -1L;
        return Math.max(0L, System.currentTimeMillis() - sourceTimestampMs);
    }

    private void refreshThermalIfNeeded() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastThermalSampleMs < THERMAL_SAMPLE_INTERVAL_MS) return;
        lastThermalSampleMs = now;
        try {
            Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int tenthC = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                if (tenthC != Integer.MIN_VALUE) cachedBatteryC = tenthC / 10f;
            }
        } catch (Throwable ignored) {}
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) cachedThermalStatus = pm.getCurrentThermalStatus();
            }
        } catch (Throwable ignored) {}
        metrics.noteThermal(cachedBatteryC, cachedThermalStatus);
    }

    private void stopInternal() {
        active = false;
        if (writer != null) {
            try { writer.flush(); } catch (IOException ignored) {}
            try { writer.close(); } catch (IOException ignored) {}
        }
        writer = null;
        if (currentFile != null && currentFile.exists()) lastFile = currentFile;
        currentFile = null;
        rowsSinceFlush = 0;
    }

    private static String fmt(float value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static String num(float value) {
        return Float.isNaN(value) ? "" : fmt(value);
    }

    private static String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    @Override public synchronized void close() {
        if (active) lastSnapshot = metrics.snapshot(SystemClock.elapsedRealtime());
        stopInternal();
    }
}
