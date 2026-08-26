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
     * Generic on-device object observation. This deliberately carries geometry and motion only.
     * A semantic class name must not be spoken until a separately validated custom model exists.
     */
    public record ObjectObservation(
            float centerX,
            float centerY,
            float bottomY,
            float areaRatio,
            float growthPerSecond,
            float centerVelocityX,
            int trackingId,
            float visionConfidence,
            long timestampMs) {
        public Direction direction() { return directionForX(centerX); }
        public boolean isApproaching() { return growthPerSecond > 0.055f; }
        public boolean isMovingTowardCenter() {
            if (centerX < 0f || centerX > 1f) return false;
            float towardCenter = Math.signum(0.5f - centerX) * centerVelocityX;
            return towardCenter > 0.035f;
        }
    }

    public record CorridorAssessment(
            float corridorHalfWidth,
            float pathOverlap,
            float sizeScore,
            float approachScore,
            float crossingScore,
            float verticalRelevance,
            float hazardScore,
            boolean inCorridor,
            boolean crossingIntoCorridor) {}

    public record GuidanceDecision(Risk risk, Direction direction, String speech, float confidence) {}

    private static Direction directionForX(float x) {
        if (x < 0f || x > 1f) return Direction.UNKNOWN;
        if (x < 0.38f) return Direction.LEFT;
        if (x > 0.62f) return Direction.RIGHT;
        return Direction.CENTER;
    }
}
