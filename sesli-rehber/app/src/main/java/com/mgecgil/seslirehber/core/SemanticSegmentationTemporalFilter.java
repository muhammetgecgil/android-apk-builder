package com.mgecgil.seslirehber.core;

/** EMA + persistence filter so a single segmentation frame cannot dominate the world model. */
public final class SemanticSegmentationTemporalFilter {
    private static final float ALPHA = 0.42f;
    private static final long STALE_MS = 1800L;
    private SemanticSegmentationMaskAnalyzer.Raw ema;
    private long lastTimestampMs;
    private int consecutiveFrames;

    public synchronized SemanticSegmentationObservation update(
            SemanticSegmentationMaskAnalyzer.Raw raw,
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
        return new SemanticSegmentationObservation(
                ema.personRatio(), ema.vehicleRatio(), ema.twoWheelerRatio(), ema.animalRatio(),
                ema.furnitureRatio(), ema.smallObstacleRatio(), ema.foregroundRatio(),
                ema.leftOccupancy(), ema.centerOccupancy(), ema.rightOccupancy(),
                ema.farOccupancy(), ema.midOccupancy(), ema.nearOccupancy(), ema.lowerCenterOccupancy(),
                persistence, Math.max(0L, inferenceMs), timestampMs);
    }

    public synchronized void reset() {
        ema = null;
        lastTimestampMs = 0L;
        consecutiveFrames = 0;
    }

    private static SemanticSegmentationMaskAnalyzer.Raw blend(
            SemanticSegmentationMaskAnalyzer.Raw a,
            SemanticSegmentationMaskAnalyzer.Raw b,
            float alpha) {
        if (a == null) return b;
        return new SemanticSegmentationMaskAnalyzer.Raw(
                mix(a.personRatio(), b.personRatio(), alpha),
                mix(a.vehicleRatio(), b.vehicleRatio(), alpha),
                mix(a.twoWheelerRatio(), b.twoWheelerRatio(), alpha),
                mix(a.animalRatio(), b.animalRatio(), alpha),
                mix(a.furnitureRatio(), b.furnitureRatio(), alpha),
                mix(a.smallObstacleRatio(), b.smallObstacleRatio(), alpha),
                mix(a.foregroundRatio(), b.foregroundRatio(), alpha),
                mix(a.leftOccupancy(), b.leftOccupancy(), alpha),
                mix(a.centerOccupancy(), b.centerOccupancy(), alpha),
                mix(a.rightOccupancy(), b.rightOccupancy(), alpha),
                mix(a.farOccupancy(), b.farOccupancy(), alpha),
                mix(a.midOccupancy(), b.midOccupancy(), alpha),
                mix(a.nearOccupancy(), b.nearOccupancy(), alpha),
                mix(a.lowerCenterOccupancy(), b.lowerCenterOccupancy(), alpha));
    }

    private static float mix(float a, float b, float alpha) { return a * (1f - alpha) + b * alpha; }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
