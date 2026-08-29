package com.mgecgil.seslirehber.core;

import java.util.Arrays;

/**
 * Process-local, bounded validation telemetry for the advisory urban segmenter.
 * No network upload and no SafetyGate authority.
 */
public final class UrbanValidationTelemetry {
    public enum Backend { INITIALIZING, GPU, CPU, UNAVAILABLE }

    public record Snapshot(
            Backend backend,
            long successfulInferences,
            long failedInferences,
            long lastInferenceMs,
            long p95InferenceMs,
            long lastTimestampMs,
            UrbanSegmentationObservation lastObservation) {}

    private static final int LATENCY_CAP = 256;
    private static final long[] LATENCIES = new long[LATENCY_CAP];
    private static Backend backend = Backend.INITIALIZING;
    private static long successes;
    private static long failures;
    private static long lastInferenceMs;
    private static long lastTimestampMs;
    private static UrbanSegmentationObservation lastObservation;
    private static int latencyCount;
    private static int latencyIndex;

    private UrbanValidationTelemetry() {}

    public static synchronized void noteBackend(Backend value) {
        if (value != null) backend = value;
    }

    public static synchronized void noteSuccess(UrbanSegmentationObservation observation) {
        if (observation == null) return;
        successes++;
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
                successes,
                failures,
                lastInferenceMs,
                p95(),
                lastTimestampMs,
                lastObservation);
    }

    public static synchronized void resetSessionCounters() {
        successes = 0L;
        failures = 0L;
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
