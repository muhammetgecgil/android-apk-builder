package com.mgecgil.seslirehber.core;

import java.util.Arrays;

/**
 * Process-local, bounded validation telemetry for the advisory urban segmenter.
 * No network upload and no SafetyGate authority.
 */
public final class UrbanValidationTelemetry {
    public enum Backend { INITIALIZING, GPU, CPU, UNAVAILABLE }

    public enum Scenario {
        SIDEWALK("Kaldırım"),
        ROAD_EDGE("Yol kenarı"),
        BUILDING_WALL("Bina veya duvar"),
        POLE_FENCE("Direk veya çit"),
        TRAFFIC_CONTROL("Trafik ışığı veya tabela"),
        PERSON_VEHICLE("İnsan veya araç"),
        LOW_LIGHT("Düşük ışık");

        private final String label;
        Scenario(String label) { this.label = label; }
        public String label() { return label; }

        public Scenario next() {
            Scenario[] all = values();
            return all[(ordinal() + 1) % all.length];
        }

        public boolean evidenceMatches(UrbanSegmentationObservation o) {
            if (o == null) return false;
            return switch (this) {
                case SIDEWALK -> o.sidewalkRatio() >= 0.05f || o.lowerCenterSidewalkRatio() >= 0.10f;
                case ROAD_EDGE -> o.roadRatio() >= 0.08f || o.lowerCenterRoadRatio() >= 0.14f;
                case BUILDING_WALL -> o.buildingWallRatio() >= 0.08f;
                case POLE_FENCE -> o.fencePoleRatio() >= 0.012f;
                case TRAFFIC_CONTROL -> o.trafficControlRatio() >= 0.004f;
                case PERSON_VEHICLE -> o.personRiderRatio() >= 0.008f || o.vehicleRatio() >= 0.012f;
                case LOW_LIGHT -> o.classifiedRatio() >= 0.05f;
            };
        }
    }

    public record Snapshot(
            Backend backend,
            Scenario scenario,
            long successfulInferences,
            long failedInferences,
            long scenarioFrames,
            long scenarioEvidenceFrames,
            long lastInferenceMs,
            long p95InferenceMs,
            long lastTimestampMs,
            UrbanSegmentationObservation lastObservation) {
        public float scenarioEvidenceRate() {
            return scenarioFrames <= 0L ? 0f : scenarioEvidenceFrames / (float) scenarioFrames;
        }
    }

    private static final int LATENCY_CAP = 256;
    private static final long[] LATENCIES = new long[LATENCY_CAP];
    private static Backend backend = Backend.INITIALIZING;
    private static Scenario scenario = Scenario.SIDEWALK;
    private static long successes;
    private static long failures;
    private static long scenarioFrames;
    private static long scenarioEvidenceFrames;
    private static long lastInferenceMs;
    private static long lastTimestampMs;
    private static UrbanSegmentationObservation lastObservation;
    private static int latencyCount;
    private static int latencyIndex;

    private UrbanValidationTelemetry() {}

    public static synchronized void noteBackend(Backend value) {
        if (value != null) backend = value;
    }

    public static synchronized Scenario scenario() { return scenario; }

    public static synchronized Scenario cycleScenario() {
        scenario = scenario.next();
        scenarioFrames = 0L;
        scenarioEvidenceFrames = 0L;
        return scenario;
    }

    public static synchronized void setScenario(Scenario value) {
        if (value == null || value == scenario) return;
        scenario = value;
        scenarioFrames = 0L;
        scenarioEvidenceFrames = 0L;
    }

    public static synchronized void noteSuccess(UrbanSegmentationObservation observation) {
        if (observation == null) return;
        successes++;
        scenarioFrames++;
        if (scenario.evidenceMatches(observation)) scenarioEvidenceFrames++;
        lastObservation = observation;
        lastInferenceMs = Math.max(0L, observation.inferenceMs());
        lastTimestampMs = observation.timestampMs();
        LATENCIES[latencyIndex] = lastInferenceMs;
        latencyIndex = (latencyIndex + 1) % LATENCY_CAP;
        latencyCount = Math.min(LATENCY_CAP, latencyCount + 1);
    }

    public static synchronized void noteFailure() {
        failures++;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(
                backend,
                scenario,
                successes,
                failures,
                scenarioFrames,
                scenarioEvidenceFrames,
                lastInferenceMs,
                p95(),
                lastTimestampMs,
                lastObservation);
    }

    public static synchronized void resetSessionCounters() {
        successes = 0L;
        failures = 0L;
        scenarioFrames = 0L;
        scenarioEvidenceFrames = 0L;
        lastInferenceMs = 0L;
        lastTimestampMs = 0L;
        lastObservation = null;
        latencyCount = 0;
        latencyIndex = 0;
        Arrays.fill(LATENCIES, 0L);
    }

    private static long p95() {
        if (latencyCount <= 0) return 0L;
        long[] copy = Arrays.copyOf(LATENCIES, latencyCount);
        Arrays.sort(copy);
        int index = Math.max(0, (int) Math.ceil(copy.length * 0.95) - 1);
        return copy[Math.min(copy.length - 1, index)];
    }
}
