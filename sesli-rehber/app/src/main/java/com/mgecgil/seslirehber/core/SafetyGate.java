package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public final class SafetyGate {
    private final CollisionCorridor collisionCorridor = new CollisionCorridor();

    public GuidanceDecision evaluate(MotionObservation observation, float deviceStability) {
        if (deviceStability < 0.35f) return unstable(deviceStability);
        float fused = clamp(observation.visionConfidence() * (0.55f + 0.45f * deviceStability));
        if (observation.changedAreaRatio() < 0.025f) return quiet(fused);
        if (fused < 0.58f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    observation.direction(),
                    "Hareket algısı belirsiz. Bastonla doğrula.",
                    fused);
        }
        return new GuidanceDecision(
                Risk.CAUTION,
                observation.direction(),
                side(observation.direction()) + " hareket var.",
                fused);
    }

    public GuidanceDecision evaluateObject(ObjectObservation observation, float deviceStability) {
        if (deviceStability < 0.35f) return unstable(deviceStability);

        float fused = clamp(observation.visionConfidence() * (0.60f + 0.40f * deviceStability));
        if (observation.areaRatio() < 0.022f || fused < 0.46f) return quiet(fused);

        CorridorAssessment corridor = collisionCorridor.assess(observation, deviceStability);
        boolean centerStop = corridor.inCorridor()
                && fused >= 0.58f
                && (
                        corridor.hazardScore() >= 0.60f
                        || (corridor.approachScore() >= 0.82f && observation.areaRatio() >= 0.075f)
                        || corridor.sizeScore() >= 0.88f);

        if (centerStop) {
            String reason = corridor.approachScore() >= 0.42f
                    ? "Dur. Önünde yaklaşan engel."
                    : "Dur. Önünde büyük engel.";
            return new GuidanceDecision(Risk.STOP, Direction.CENTER, reason, fused);
        }

        if (corridor.crossingIntoCorridor() && fused >= 0.56f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    observation.direction(),
                    side(observation.direction()) + " engel önüne doğru hareket ediyor.",
                    fused);
        }

        if (corridor.inCorridor()
                && (corridor.hazardScore() >= 0.31f
                    || observation.areaRatio() >= 0.09f
                    || observation.isApproaching())) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Önde engel. Yavaşla ve bastonla doğrula.",
                    fused);
        }

        if (observation.isApproaching() && fused >= 0.54f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    observation.direction(),
                    side(observation.direction()) + " engel yaklaşıyor.",
                    fused);
        }

        if (corridor.hazardScore() >= 0.24f && observation.areaRatio() >= 0.07f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    observation.direction(),
                    side(observation.direction()) + " engel.",
                    fused);
        }
        return quiet(fused);
    }

    /**
     * Ground continuity is an appearance/geometric evidence channel. Without depth it may raise
     * CAUTION after persistence, but never STOP and never a semantic "hole/curb" claim.
     */
    public GuidanceDecision evaluateGround(GroundObservation observation, float deviceStability) {
        if (deviceStability < 0.35f) return unstable(deviceStability);

        float fused = clamp(observation.viewConfidence() * (0.52f + 0.48f * deviceStability));
        if (observation.viewConfidence() < 0.34f) return quiet(fused);

        boolean persistent = observation.persistenceScore() >= 0.58f;
        boolean strong = observation.anomalyScore() >= 0.62f
                && observation.broadBoundaryScore() >= 0.50f;
        if (persistent && strong && fused >= 0.46f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Ön zeminde süreklilik bozuluyor. Yavaşla ve bastonla doğrula.",
                    fused);
        }

        if (observation.persistenceScore() >= 0.82f
                && observation.anomalyScore() >= 0.54f
                && observation.broadBoundaryScore() >= 0.42f
                && fused >= 0.44f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Ön zemin belirsiz. Yavaşla ve bastonla doğrula.",
                    fused);
        }
        return quiet(fused);
    }

    /**
     * Depth alone remains CAUTION-only. A single depth edge may be a wall/object boundary rather
     * than a ground drop, so semantic language is deliberately avoided.
     */
    public GuidanceDecision evaluateDepth(DepthObservation depth, float deviceStability) {
        if (deviceStability < 0.35f) return unstable(deviceStability);
        float fused = clamp(depth.depthConfidence() * (0.58f + 0.42f * deviceStability));
        if (!depth.strongDiscontinuity() || fused < 0.52f) return quiet(fused);
        return new GuidanceDecision(
                Risk.CAUTION,
                Direction.CENTER,
                "Ön bölgede ani derinlik değişimi algılandı. Yavaşla ve bastonla doğrula.",
                fused);
    }

    /**
     * P0 fusion gate for the future live Depth16 stream. STOP requires two independent channels:
     * persistent ground discontinuity AND strong depth discontinuity, plus stable device evidence.
     * It still does not call the event a hole, curb or stair.
     */
    public GuidanceDecision evaluateGroundWithDepth(
            GroundObservation ground,
            DepthObservation depth,
            float deviceStability) {
        if (deviceStability < 0.35f) return unstable(deviceStability);

        float groundConfidence = clamp(ground.viewConfidence() * (0.52f + 0.48f * deviceStability));
        float depthConfidence = clamp(depth.depthConfidence() * (0.58f + 0.42f * deviceStability));
        float fused = clamp((float) Math.sqrt(Math.max(0f, groundConfidence * depthConfidence)));

        boolean groundStrong = ground.persistenceScore() >= 0.64f
                && ground.anomalyScore() >= 0.64f
                && ground.broadBoundaryScore() >= 0.52f;
        boolean depthStrong = depth.strongDiscontinuity();

        if (groundStrong && depthStrong && fused >= 0.56f) {
            return new GuidanceDecision(
                    Risk.STOP,
                    Direction.CENTER,
                    "Dur. Ön zeminde ani derinlik değişimi. Bastonla doğrula.",
                    fused);
        }

        if (depthStrong
                && ground.persistenceScore() >= 0.42f
                && ground.anomalyScore() >= 0.46f
                && fused >= 0.48f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Ön zemin ve derinlik birlikte değişiyor. Yavaşla ve bastonla doğrula.",
                    fused);
        }

        GuidanceDecision groundOnly = evaluateGround(ground, deviceStability);
        if (groundOnly.risk() != Risk.INFO) return groundOnly;
        return evaluateDepth(depth, deviceStability);
    }

    public CorridorAssessment assessCorridor(ObjectObservation observation, float deviceStability) {
        return collisionCorridor.assess(observation, deviceStability);
    }

    private static GuidanceDecision unstable(float stability) {
        return new GuidanceDecision(
                Risk.STOP,
                Direction.UNKNOWN,
                "Telefon veya görüntü kararsız. Dur ve bastonla doğrula.",
                clamp(1f - stability));
    }

    private static GuidanceDecision quiet(float confidence) {
        return new GuidanceDecision(Risk.INFO, Direction.UNKNOWN, "", confidence);
    }

    private static String side(Direction direction) {
        return switch (direction) {
            case LEFT -> "Solda";
            case RIGHT -> "Sağda";
            case CENTER -> "Önde";
            default -> "Çevrede";
        };
    }

    private static float clamp(float x) {
        return Math.max(0f, Math.min(1f, x));
    }
}
