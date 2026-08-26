package com.mgecgil.seslirehber.core;

public final class GuidanceModels {
    private GuidanceModels() {}
    public enum Direction { LEFT, CENTER, RIGHT, UNKNOWN }
    public enum Risk { INFO, CAUTION, STOP }
    public record MotionObservation(float changedAreaRatio, float centroidX, float centroidY, float visionConfidence, long timestampMs) {
        public Direction direction() {
            if (centroidX < 0f || centroidX > 1f) return Direction.UNKNOWN;
            if (centroidX < 0.38f) return Direction.LEFT;
            if (centroidX > 0.62f) return Direction.RIGHT;
            return Direction.CENTER;
        }
    }
    public record GuidanceDecision(Risk risk, Direction direction, String speech, float confidence) {}
}
