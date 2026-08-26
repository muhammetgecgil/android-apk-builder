package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public final class SafetyGate {
    public GuidanceDecision evaluate(MotionObservation observation, float deviceStability) {
        if (deviceStability < 0.35f) return unstable(deviceStability);
        float fused = clamp(observation.visionConfidence() * (0.55f + 0.45f * deviceStability));
        if (observation.changedAreaRatio() < 0.025f) return quiet(fused);
        if (fused < 0.58f) return new GuidanceDecision(Risk.CAUTION, observation.direction(), "Hareket algısı belirsiz. Bastonla doğrula.", fused);
        return new GuidanceDecision(Risk.CAUTION, observation.direction(), side(observation.direction()) + " hareket var.", fused);
    }

    public GuidanceDecision evaluateObject(ObjectObservation observation, float deviceStability) {
        if (deviceStability < 0.35f) return unstable(deviceStability);

        float fused = clamp(observation.visionConfidence() * (0.60f + 0.40f * deviceStability));
        if (observation.areaRatio() < 0.025f || fused < 0.48f) return quiet(fused);

        boolean center = observation.direction() == Direction.CENTER;
        boolean large = observation.areaRatio() >= 0.18f;
        boolean veryLarge = observation.areaRatio() >= 0.28f;
        boolean approachingFast = observation.growthPerSecond() >= 0.18f;

        if (center && fused >= 0.58f && (veryLarge || approachingFast)) {
            return new GuidanceDecision(Risk.STOP, Direction.CENTER, "Dur. Önünde yaklaşan engel.", fused);
        }

        String where = side(observation.direction());
        if (center && (large || observation.isApproaching())) {
            return new GuidanceDecision(Risk.CAUTION, Direction.CENTER, "Önde engel. Yavaşla ve bastonla doğrula.", fused);
        }
        if (observation.areaRatio() >= 0.08f || observation.isApproaching()) {
            String approach = observation.isApproaching() ? " yaklaşıyor" : " engel";
            return new GuidanceDecision(Risk.CAUTION, observation.direction(), where + approach + ".", fused);
        }
        return quiet(fused);
    }

    private static GuidanceDecision unstable(float stability) {
        return new GuidanceDecision(Risk.STOP, Direction.UNKNOWN, "Görüntü kararsız. Dur ve bastonla doğrula.", clamp(1f - stability));
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

    private static float clamp(float x) { return Math.max(0f, Math.min(1f, x)); }
}
