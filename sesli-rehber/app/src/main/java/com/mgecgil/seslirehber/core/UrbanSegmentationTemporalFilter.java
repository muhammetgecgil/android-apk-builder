package com.mgecgil.seslirehber.core;

/** EMA + persistence for Cityscapes masks. A single urban frame never becomes mature evidence. */
public final class UrbanSegmentationTemporalFilter {
    private static final float ALPHA = 0.38f;
    private static final long STALE_MS = 2400L;
    private UrbanSegmentationLogitAnalyzer.Raw ema;
    private long lastTimestampMs;
    private int consecutiveFrames;

    public synchronized UrbanSegmentationObservation update(
            UrbanSegmentationLogitAnalyzer.Raw raw,
            long inferenceMs,
            long timestampMs) {
        if (raw == null || timestampMs <= 0L) return null;
        if (lastTimestampMs <= 0L || timestampMs - lastTimestampMs > STALE_MS) {
            ema = raw;
            consecutiveFrames = 1;
        } else {
            ema = blend(ema, raw, ALPHA);
            consecutiveFrames = Math.min(12, consecutiveFrames + 1);
        }
        lastTimestampMs = timestampMs;
        float persistence = clamp((consecutiveFrames - 1) / 3f);
        return new UrbanSegmentationObservation(
                ema.roadRatio(), ema.sidewalkRatio(), ema.buildingWallRatio(), ema.fencePoleRatio(),
                ema.trafficControlRatio(), ema.vegetationRatio(), ema.terrainRatio(),
                ema.personRiderRatio(), ema.vehicleRatio(), ema.twoWheelerRatio(), ema.skyRatio(),
                ema.leftObstacleOccupancy(), ema.centerObstacleOccupancy(), ema.rightObstacleOccupancy(),
                ema.lowerCenterRoadRatio(), ema.lowerCenterSidewalkRatio(), ema.lowerCenterObstacleRatio(),
                persistence, Math.max(0L, inferenceMs), timestampMs);
    }

    public synchronized void reset() {
        ema = null;
        lastTimestampMs = 0L;
        consecutiveFrames = 0;
    }

    private static UrbanSegmentationLogitAnalyzer.Raw blend(
            UrbanSegmentationLogitAnalyzer.Raw a,
            UrbanSegmentationLogitAnalyzer.Raw b,
            float alpha) {
        if (a == null) return b;
        return new UrbanSegmentationLogitAnalyzer.Raw(
                mix(a.roadRatio(), b.roadRatio(), alpha),
                mix(a.sidewalkRatio(), b.sidewalkRatio(), alpha),
                mix(a.buildingWallRatio(), b.buildingWallRatio(), alpha),
                mix(a.fencePoleRatio(), b.fencePoleRatio(), alpha),
                mix(a.trafficControlRatio(), b.trafficControlRatio(), alpha),
                mix(a.vegetationRatio(), b.vegetationRatio(), alpha),
                mix(a.terrainRatio(), b.terrainRatio(), alpha),
                mix(a.personRiderRatio(), b.personRiderRatio(), alpha),
                mix(a.vehicleRatio(), b.vehicleRatio(), alpha),
                mix(a.twoWheelerRatio(), b.twoWheelerRatio(), alpha),
                mix(a.skyRatio(), b.skyRatio(), alpha),
                mix(a.leftObstacleOccupancy(), b.leftObstacleOccupancy(), alpha),
                mix(a.centerObstacleOccupancy(), b.centerObstacleOccupancy(), alpha),
                mix(a.rightObstacleOccupancy(), b.rightObstacleOccupancy(), alpha),
                mix(a.lowerCenterRoadRatio(), b.lowerCenterRoadRatio(), alpha),
                mix(a.lowerCenterSidewalkRatio(), b.lowerCenterSidewalkRatio(), alpha),
                mix(a.lowerCenterObstacleRatio(), b.lowerCenterObstacleRatio(), alpha));
    }

    private static float mix(float a, float b, float alpha) { return a * (1f - alpha) + b * alpha; }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
