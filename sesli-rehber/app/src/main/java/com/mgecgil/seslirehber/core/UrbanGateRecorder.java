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
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * Dedicated real-device validation recorder for the advisory PIDNet urban segmentation channel.
 * This recorder never changes SafetyGate decisions and never uploads data automatically.
 */
public final class UrbanGateRecorder implements AutoCloseable {
    private static final long SAMPLE_INTERVAL_MS = 650L;
    private static final long THERMAL_INTERVAL_MS = 2000L;

    private static final class ScenarioStats {
        long frames;
        long evidence;
    }

    private final Context context;
    private final EnumMap<UrbanValidationTelemetry.Scenario, ScenarioStats> stats =
            new EnumMap<>(UrbanValidationTelemetry.Scenario.class);
    private final UrbanGateWizard wizard = new UrbanGateWizard();
    private BufferedWriter writer;
    private File currentFile;
    private File lastFile;
    private boolean active;
    private long lastSampleElapsedMs;
    private long lastObservationTimestampMs;
    private long lastThermalElapsedMs;
    private float maxBatteryC = Float.NaN;
    private int maxThermalStatus;
    private int rowsSinceFlush;
    private UrbanGateAcceptance.Result lastAcceptance;

    public UrbanGateRecorder(Context context) {
        this.context = context.getApplicationContext();
        for (UrbanValidationTelemetry.Scenario s : UrbanValidationTelemetry.Scenario.values()) {
            stats.put(s, new ScenarioStats());
        }
    }

    public synchronized boolean start(String version, String mode) {
        stopInternal();
        resetStats();
        UrbanValidationTelemetry.setScenario(UrbanValidationTelemetry.Scenario.SIDEWALK);
        UrbanValidationTelemetry.resetSessionCounters();
        wizard.start(SystemClock.elapsedRealtime());
        try {
            File dir = new File(context.getCacheDir(), "gate_c");
            if (!dir.exists() && !dir.mkdirs()) return false;
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            currentFile = new File(dir, "SesliRehber_UrbanGate_" + stamp + ".csv");
            writer = new BufferedWriter(new FileWriter(currentFile, false), 32 * 1024);
            writer.write("wall_ms,elapsed_ms,scenario,vision_mode,backend,successes,failures,inference_ms,p95_inference_ms,temporal_stability,evidence_match,road,sidewalk,building_wall,fence_pole,traffic_control,vegetation,terrain,person_rider,vehicle,two_wheeler,sky,left_obstacle,center_obstacle,right_obstacle,lower_center_road,lower_center_sidewalk,lower_center_obstacle,battery_c,thermal_status\n");
            active = true;
            writeMarker("SESSION_START", mode, "version=" + version + ";wizard=auto");
            return true;
        } catch (IOException error) {
            stopInternal();
            return false;
        }
    }

    public synchronized boolean isActive() { return active; }

    public synchronized UrbanValidationTelemetry.Scenario scenario() {
        return UrbanValidationTelemetry.scenario();
    }

    /** Manual skip remains available for a sighted test assistant, but normal operation is automatic. */
    public synchronized UrbanValidationTelemetry.Scenario cycleScenario(String mode) {
        if (wizard.isComplete()) return UrbanValidationTelemetry.scenario();
        UrbanValidationTelemetry.Scenario next = UrbanValidationTelemetry.cycleScenario();
        wizard.beginScenario(SystemClock.elapsedRealtime());
        if (active) writeMarker("SCENARIO_CHANGE", mode, "manual:" + next.label());
        return next;
    }

    public synchronized void sample(String visionMode) {
        if (!active || writer == null) return;
        long elapsed = SystemClock.elapsedRealtime();
        if (elapsed - lastSampleElapsedMs < SAMPLE_INTERVAL_MS) return;
        lastSampleElapsedMs = elapsed;

        UrbanValidationTelemetry.Snapshot s = UrbanValidationTelemetry.snapshot();
        UrbanSegmentationObservation o = s.lastObservation();
        if (o == null || o.timestampMs() <= 0L || o.timestampMs() == lastObservationTimestampMs) return;
        lastObservationTimestampMs = o.timestampMs();
        refreshThermal();

        ScenarioStats scenarioStats = stats.get(s.scenario());
        if (scenarioStats != null) {
            scenarioStats.frames++;
            if (s.scenario().evidenceMatches(o)) scenarioStats.evidence++;
        }

        try {
            writer.write(Long.toString(System.currentTimeMillis())); writer.write(',');
            writer.write(Long.toString(elapsed)); writer.write(',');
            writer.write(csv(s.scenario().label())); writer.write(',');
            writer.write(csv(visionMode)); writer.write(',');
            writer.write(csv(s.backend().name())); writer.write(',');
            writer.write(Long.toString(s.successfulInferences())); writer.write(',');
            writer.write(Long.toString(s.failedInferences())); writer.write(',');
            writer.write(Long.toString(s.lastInferenceMs())); writer.write(',');
            writer.write(Long.toString(s.p95InferenceMs())); writer.write(',');
            writer.write(num(o.temporalStability())); writer.write(',');
            writer.write(s.scenario().evidenceMatches(o) ? "1" : "0"); writer.write(',');
            writer.write(num(o.roadRatio())); writer.write(',');
            writer.write(num(o.sidewalkRatio())); writer.write(',');
            writer.write(num(o.buildingWallRatio())); writer.write(',');
            writer.write(num(o.fencePoleRatio())); writer.write(',');
            writer.write(num(o.trafficControlRatio())); writer.write(',');
            writer.write(num(o.vegetationRatio())); writer.write(',');
            writer.write(num(o.terrainRatio())); writer.write(',');
            writer.write(num(o.personRiderRatio())); writer.write(',');
            writer.write(num(o.vehicleRatio())); writer.write(',');
            writer.write(num(o.twoWheelerRatio())); writer.write(',');
            writer.write(num(o.skyRatio())); writer.write(',');
            writer.write(num(o.leftObstacleOccupancy())); writer.write(',');
            writer.write(num(o.centerObstacleOccupancy())); writer.write(',');
            writer.write(num(o.rightObstacleOccupancy())); writer.write(',');
            writer.write(num(o.lowerCenterRoadRatio())); writer.write(',');
            writer.write(num(o.lowerCenterSidewalkRatio())); writer.write(',');
            writer.write(num(o.lowerCenterObstacleRatio())); writer.write(',');
            writer.write(num(maxBatteryC)); writer.write(',');
            writer.write(Integer.toString(maxThermalStatus)); writer.write('\n');
            rowsSinceFlush++;
            if (rowsSinceFlush >= 10) {
                writer.flush();
                rowsSinceFlush = 0;
            }
        } catch (IOException ignored) {
            // Validation recording must never affect camera/safety guidance.
        }

        handleWizard(elapsed, visionMode, s.scenario(), scenarioStats);
    }

    public synchronized String stop(String mode) {
        if (!active) return summaryText();
        refreshThermal();
        lastAcceptance = acceptanceResultInternal();
        writeMarker("ACCEPTANCE", mode, lastAcceptance.shortText());
        writeMarker("SESSION_STOP", mode, wizard.isComplete() ? "wizard_complete" : "user_stop_before_wizard_complete");
        stopInternal();
        return summaryText();
    }

    public synchronized File lastReportFile() {
        return lastFile != null && lastFile.exists() ? lastFile : null;
    }

    public synchronized UrbanGateAcceptance.Result acceptanceResult() {
        return lastAcceptance != null ? lastAcceptance : acceptanceResultInternal();
    }

    public synchronized boolean wizardComplete() { return wizard.isComplete(); }

    public synchronized String summaryText() {
        UrbanValidationTelemetry.Snapshot s = UrbanValidationTelemetry.snapshot();
        long total = s.successfulInferences() + s.failedInferences();
        int failurePct = total <= 0L ? 0 : Math.round(100f * s.failedInferences() / total);
        String battery = Float.isNaN(maxBatteryC) ? "n/a" : String.format(Locale.US, "%.1f C", maxBatteryC);
        StringBuilder out = new StringBuilder("Urban Gate: backend=")
                .append(s.backend().name())
                .append(", inference=").append(s.successfulInferences())
                .append(", hata=").append(failurePct).append("%")
                .append(", p95=").append(s.p95InferenceMs()).append(" ms")
                .append(", max batarya=").append(battery)
                .append(", thermal=").append(maxThermalStatus).append('.')
                .append(" Otomatik akış=").append(wizard.isComplete() ? "tamam" : "tamamlanmadı").append('.');
        for (UrbanValidationTelemetry.Scenario scenario : UrbanValidationTelemetry.Scenario.values()) {
            ScenarioStats st = stats.get(scenario);
            if (st == null || st.frames == 0L) continue;
            out.append(' ').append(scenario.label()).append('=')
                    .append(Math.round(100f * st.evidence / st.frames)).append("%(")
                    .append(st.frames).append(" kare).");
        }
        UrbanGateAcceptance.Result result = lastAcceptance != null
                ? lastAcceptance : acceptanceResultInternal();
        out.append(' ').append(result.shortText());
        return out.toString();
    }

    private void handleWizard(
            long elapsed,
            String visionMode,
            UrbanValidationTelemetry.Scenario currentScenario,
            ScenarioStats scenarioStats) {
        if (wizard.isComplete() || scenarioStats == null) return;
        UrbanGateWizard.Decision decision = wizard.tick(
                elapsed, currentScenario, scenarioStats.frames, scenarioStats.evidence);
        switch (decision.action()) {
            case NONE -> { }
            case RETRY_PROMPT -> {
                writeMarker("AUTO_RETRY", visionMode,
                        currentScenario.label() + ";frames=" + scenarioStats.frames + ";evidence=" + scenarioStats.evidence);
                GuidanceSpeaker.speakTestPrompt(retryInstruction(currentScenario));
            }
            case ADVANCE -> {
                UrbanValidationTelemetry.Scenario next = UrbanValidationTelemetry.cycleScenario();
                wizard.beginScenario(elapsed);
                writeMarker("AUTO_SCENARIO_CHANGE", visionMode,
                        currentScenario.label() + "->" + next.label());
                GuidanceSpeaker.speakTestPrompt(
                        "Sıradaki otomatik senaryo: " + next.label() + ". " + scenarioInstruction(next));
            }
            case COMPLETE -> {
                writeMarker("AUTO_WIZARD_COMPLETE", visionMode,
                        currentScenario.label() + ";frames=" + scenarioStats.frames + ";evidence=" + scenarioStats.evidence);
                GuidanceSpeaker.speakTestPrompt(
                        "Urban Gate'in yedi senaryosu tamamlandı. Ölçümü mühürlemek ve PASS, REVIEW veya FAIL sonucunu almak için Urban Gate Testini Durdur düğmesine bas.");
            }
        }
    }

    private static String scenarioInstruction(UrbanValidationTelemetry.Scenario scenario) {
        return switch (scenario) {
            case SIDEWALK -> "Kamerayı kaldırım yüzeyi ve kaldırım kenarı birlikte görünecek şekilde sabit tut.";
            case ROAD_EDGE -> "Güvenli ve sabit bir noktadan yol yüzeyi ile kaldırım sınırını birlikte kadraja al.";
            case BUILDING_WALL -> "Bina cephesi veya duvarı kadrajın belirgin bölümüne al.";
            case POLE_FENCE -> "Sabit bir direk veya çiti kadrajda belirgin tut.";
            case TRAFFIC_CONTROL -> "Güvenli bir noktadan trafik ışığı veya tabelayı kadrajda belirgin tut.";
            case PERSON_VEHICLE -> "Güvenli mesafeden bir insan veya aracı kadrajda belirgin tut.";
            case LOW_LIGHT -> "Yürümeden, düşük ışıklı bir sahneyi sabit tut.";
        };
    }

    private static String retryInstruction(UrbanValidationTelemetry.Scenario scenario) {
        return switch (scenario) {
            case SIDEWALK -> "Kaldırım kanıtı zayıf. Kamerayı biraz aşağı alıp yüzey ile kenarı birlikte göster.";
            case ROAD_EDGE -> "Yol kenarı kanıtı zayıf. Güvenli konumunu değiştirmeden yol ve kaldırım sınırını birlikte büyüt.";
            case BUILDING_WALL -> "Bina veya duvar kanıtı zayıf. Cepheyi kadrajın daha büyük bölümüne al.";
            case POLE_FENCE -> "Direk veya çit kanıtı zayıf. Nesneyi kadrajın daha büyük bölümünde sabit tut.";
            case TRAFFIC_CONTROL -> "Trafik ışığı veya tabela kanıtı zayıf. Güvenli konumdan hedefi kadrajda büyüt.";
            case PERSON_VEHICLE -> "İnsan veya araç kanıtı zayıf. Güvenli mesafeyi koruyarak hedefi daha belirgin kadraja al.";
            case LOW_LIGHT -> "Düşük ışık örneği yetersiz. Yürümeden kamerayı birkaç saniye daha sabit tut.";
        };
    }

    private UrbanGateAcceptance.Result acceptanceResultInternal() {
        UrbanValidationTelemetry.Snapshot s = UrbanValidationTelemetry.snapshot();
        List<UrbanGateAcceptance.ScenarioMetric> scenarioMetrics = new ArrayList<>();
        for (UrbanValidationTelemetry.Scenario scenario : UrbanValidationTelemetry.Scenario.values()) {
            ScenarioStats st = stats.get(scenario);
            long frames = st == null ? 0L : st.frames;
            long evidence = st == null ? 0L : st.evidence;
            scenarioMetrics.add(new UrbanGateAcceptance.ScenarioMetric(scenario, frames, evidence));
        }
        return UrbanGateAcceptance.evaluate(new UrbanGateAcceptance.Input(
                s.backend(),
                s.successfulInferences(),
                s.failedInferences(),
                s.p95InferenceMs(),
                maxBatteryC,
                maxThermalStatus,
                scenarioMetrics));
    }

    private void writeMarker(String marker, String mode, String detail) {
        if (!active || writer == null) return;
        refreshThermal();
        UrbanValidationTelemetry.Snapshot s = UrbanValidationTelemetry.snapshot();
        try {
            writer.write(Long.toString(System.currentTimeMillis())); writer.write(',');
            writer.write(Long.toString(SystemClock.elapsedRealtime())); writer.write(',');
            writer.write(csv(s.scenario().label() + ":" + marker + ":" + detail)); writer.write(',');
            writer.write(csv(mode)); writer.write(',');
            writer.write(csv(s.backend().name()));
            for (int i = 0; i < 24; i++) writer.write(',');
            writer.write(num(maxBatteryC)); writer.write(',');
            writer.write(Integer.toString(maxThermalStatus)); writer.write('\n');
            writer.flush();
        } catch (IOException ignored) {}
    }

    private void refreshThermal() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastThermalElapsedMs < THERMAL_INTERVAL_MS) return;
        lastThermalElapsedMs = now;
        try {
            Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int tenthC = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                if (tenthC != Integer.MIN_VALUE) {
                    float c = tenthC / 10f;
                    if (Float.isNaN(maxBatteryC) || c > maxBatteryC) maxBatteryC = c;
                }
            }
        } catch (Throwable ignored) {}
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) maxThermalStatus = Math.max(maxThermalStatus, pm.getCurrentThermalStatus());
            }
        } catch (Throwable ignored) {}
    }

    private void resetStats() {
        for (ScenarioStats s : stats.values()) {
            s.frames = 0L;
            s.evidence = 0L;
        }
        lastSampleElapsedMs = 0L;
        lastObservationTimestampMs = 0L;
        lastThermalElapsedMs = 0L;
        maxBatteryC = Float.NaN;
        maxThermalStatus = 0;
        rowsSinceFlush = 0;
        lastAcceptance = null;
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
    }

    private static String num(float value) {
        return Float.isNaN(value) ? "" : String.format(Locale.US, "%.5f", value);
    }

    private static String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Override public synchronized void close() {
        stopInternal();
    }
}
