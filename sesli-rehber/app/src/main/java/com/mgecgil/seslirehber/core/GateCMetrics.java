package com.mgecgil.seslirehber.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small in-memory accumulator for Gate-C device validation summaries. */
public final class GateCMetrics {
    public record Snapshot(
            long durationMs,
            int visionFrames,
            int depthFrames,
            int groundFrames,
            int objectFrames,
            int cautionDecisions,
            int stopDecisions,
            int fallbacks,
            float meanDepthValidRatio,
            long p95DecisionLatencyMs,
            float maxBatteryC,
            int maxThermalStatus) {}

    private long startedElapsedMs;
    private int visionFrames;
    private int depthFrames;
    private int groundFrames;
    private int objectFrames;
    private int cautionDecisions;
    private int stopDecisions;
    private int fallbacks;
    private float depthValidSum;
    private float maxBatteryC = Float.NaN;
    private int maxThermalStatus;
    private final List<Long> decisionLatencies = new ArrayList<>();

    public synchronized void reset(long nowElapsedMs) {
        startedElapsedMs = nowElapsedMs;
        visionFrames = 0;
        depthFrames = 0;
        groundFrames = 0;
        objectFrames = 0;
        cautionDecisions = 0;
        stopDecisions = 0;
        fallbacks = 0;
        depthValidSum = 0f;
        maxBatteryC = Float.NaN;
        maxThermalStatus = 0;
        decisionLatencies.clear();
    }

    public synchronized void noteVision() { visionFrames++; }
    public synchronized void noteGround() { groundFrames++; }
    public synchronized void noteObject() { objectFrames++; }
    public synchronized void noteDepth(float validRatio) {
        depthFrames++;
        depthValidSum += Math.max(0f, Math.min(1f, validRatio));
    }
    public synchronized void noteFallback() { fallbacks++; }

    public synchronized void noteDecision(String risk, long latencyMs) {
        if ("STOP".equals(risk)) stopDecisions++;
        else if ("CAUTION".equals(risk)) cautionDecisions++;
        if (latencyMs >= 0L) {
            if (decisionLatencies.size() >= 2048) decisionLatencies.remove(0);
            decisionLatencies.add(latencyMs);
        }
    }

    public synchronized void noteThermal(float batteryC, int thermalStatus) {
        if (!Float.isNaN(batteryC)) {
            if (Float.isNaN(maxBatteryC) || batteryC > maxBatteryC) maxBatteryC = batteryC;
        }
        if (thermalStatus > maxThermalStatus) maxThermalStatus = thermalStatus;
    }

    public synchronized Snapshot snapshot(long nowElapsedMs) {
        long p95 = percentile95(decisionLatencies);
        return new Snapshot(
                Math.max(0L, nowElapsedMs - startedElapsedMs),
                visionFrames,
                depthFrames,
                groundFrames,
                objectFrames,
                cautionDecisions,
                stopDecisions,
                fallbacks,
                depthFrames == 0 ? 0f : depthValidSum / depthFrames,
                p95,
                maxBatteryC,
                maxThermalStatus);
    }

    static long percentile95(List<Long> values) {
        if (values == null || values.isEmpty()) return -1L;
        List<Long> copy = new ArrayList<>(values);
        Collections.sort(copy);
        int index = (int) Math.ceil(copy.size() * 0.95d) - 1;
        index = Math.max(0, Math.min(copy.size() - 1, index));
        return copy.get(index);
    }
}
