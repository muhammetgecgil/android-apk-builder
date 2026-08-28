package com.mgecgil.seslirehber.core;

public final class GuidanceModels {
    private GuidanceModels() {}

    public enum Direction { LEFT, CENTER, RIGHT, UNKNOWN }
    public enum Risk { INFO, CAUTION, STOP }
    public enum LevelChangeKind { UPWARD_CANDIDATE, DOWNWARD_CANDIDATE, MULTI_LEVEL_CANDIDATE, UNKNOWN }

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

    /**
     * Geometry/appearance-only evidence from the lower walking corridor.
     * This is NOT a hole, curb or step classification and must never be spoken as one.
     */
    public record GroundObservation(
            float anomalyScore,
            float broadBoundaryScore,
            float textureChangeScore,
            float temporalChangeScore,
            float persistenceScore,
            float viewConfidence,
            float boundaryY,
            long timestampMs) {
        public boolean persistentAnomaly() {
            return anomalyScore >= 0.58f && persistenceScore >= 0.58f;
        }
    }

    /**
     * Image usability evidence. It is intentionally about sensing quality, not scene semantics.
     * Persistent darkness, saturation or near-flat texture can make camera guidance unreliable.
     */
    public record SceneHealthObservation(
            float meanLuma,
            float contrastScore,
            float darkRatio,
            float brightRatio,
            float qualityScore,
            float unusableScore,
            float persistenceScore,
            long timestampMs) {
        public boolean persistentlyUnusable() {
            return unusableScore >= 0.72f && persistenceScore >= 0.58f;
        }
    }

    /**
     * Internal metric evidence derived from a depth image. Millimetres are kept internally only;
     * user-facing metric distance is forbidden until device calibration and field validation pass.
     */
    public record DepthObservation(
            float validRatio,
            float centerMedianMm,
            float nearBandMedianMm,
            float farBandMedianMm,
            float maxBandJumpMm,
            float discontinuityScore,
            float depthConfidence,
            long timestampMs) {
        public boolean strongDiscontinuity() {
            return validRatio >= 0.42f
                    && discontinuityScore >= 0.62f
                    && depthConfidence >= 0.58f;
        }
    }

    /**
     * Conservative relative level-change evidence from an upright, camera-aligned depth profile.
     * Kind names are candidates only: they do NOT certify a curb, hole, stair or safe step.
     */
    public record LevelChangeObservation(
            LevelChangeKind kind,
            float candidateScore,
            float boundaryScore,
            float trendResidualScore,
            float multiLevelScore,
            float persistenceScore,
            float depthConfidence,
            float boundaryY,
            long timestampMs) {
        public boolean persistentCandidate() {
            return kind != LevelChangeKind.UNKNOWN
                    && candidateScore >= 0.58f
                    && persistenceScore >= 0.56f
                    && depthConfidence >= 0.50f;
        }
    }

    /**
     * Relative openness of three forward depth corridors. This does NOT certify a safe path.
     * A direction is only a persistent "more open" candidate and must be paired with cane/other
     * evidence until controlled field validation is complete.
     */
    public record WalkableCorridorObservation(
            float leftOpenScore,
            float centerOpenScore,
            float rightOpenScore,
            float centerBlockedScore,
            Direction moreOpenDirection,
            float confidence,
            float persistenceScore,
            long timestampMs) {
        public float score(Direction direction) {
            return switch (direction) {
                case LEFT -> leftOpenScore;
                case CENTER -> centerOpenScore;
                case RIGHT -> rightOpenScore;
                default -> 0f;
            };
        }
        public boolean hasPersistentCandidate() {
            return moreOpenDirection != Direction.UNKNOWN
                    && confidence >= 0.54f
                    && persistenceScore >= 0.56f;
        }
    }

    /** Ground and depth observations are only fused when their timestamps are close enough. */
    public record GroundDepthEvidence(
            GroundObservation ground,
            DepthObservation depth,
            long timestampSkewMs) {}

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
