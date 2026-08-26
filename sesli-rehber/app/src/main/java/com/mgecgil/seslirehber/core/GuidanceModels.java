package com.mgecgil.seslirehber.core;

public final class GuidanceModels {
    private GuidanceModels() {}

    public enum Direction { LEFT, CENTER, RIGHT, UNKNOWN }
    public enum Risk { INFO, CAUTION, STOP }

    public record MotionObservation(
            float changedAreaRatio,
            float centroidX,
            float centroidY,
            float visionConfidence,
            long timestampMs) {
        public Direction direction() { return directionForX(centroidX); }
    }

    /**
     * Generic on-device object observation. The detector is intentionally not treated as a
     * semantic navigation oracle: label is omitted until a validated custom model is bundled.
     */
    public record ObjectObservation(
            float centerX,
            float centerY,
            float areaRatio,
            float growthPerSecond,
            int trackingId,
            float visionConfidence,
            long timestampMs) {
        public Direction direction() { return directionForX(centerX); }
        public boolean isApproaching() { return growthPerSecond > 0.06f; }
    }

    public record GuidanceDecision(Risk risk, Direction direction, String speech, float confidence) {}

    private static Direction directionForX(float x) {
        if (x < 0f || x > 1f) return Direction.UNKNOWN;
        if (x < 0.38f) return Direction.LEFT;
        if (x > 0.62f) return Direction.RIGHT;
        return Direction.CENTER;
    }
}
