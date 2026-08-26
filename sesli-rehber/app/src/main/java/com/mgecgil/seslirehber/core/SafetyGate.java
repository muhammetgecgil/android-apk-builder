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

    public CorridorAssessment assessCorridor(ObjectObservation observation, float deviceStability) {
        return collisionCorridor.assess(observation, deviceStability);
    }

    private static GuidanceDecision unstable(float stability) {
        return new GuidanceDecision(
                Risk.STOP,
                Direction.UNKNOWN,
                "Görüntü kararsız. Dur ve bastonla doğrula.",
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
