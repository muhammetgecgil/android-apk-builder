package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public final class SafetyGate {
    public GuidanceDecision evaluate(MotionObservation observation, float deviceStability) {
        if (deviceStability < 0.35f) return new GuidanceDecision(Risk.STOP, Direction.UNKNOWN, "Görüntü kararsız. Dur ve bastonla doğrula.", 1f - deviceStability);
        float fused = clamp(observation.visionConfidence() * (0.55f + 0.45f * deviceStability));
        if (observation.changedAreaRatio() < 0.025f) return new GuidanceDecision(Risk.INFO, Direction.UNKNOWN, "", fused);
        if (fused < 0.58f) return new GuidanceDecision(Risk.CAUTION, observation.direction(), "Hareket algısı belirsiz. Bastonla doğrula.", fused);
        String side = switch (observation.direction()) { case LEFT -> "solda"; case RIGHT -> "sağda"; case CENTER -> "önde"; default -> "çevrede"; };
        return new GuidanceDecision(Risk.CAUTION, observation.direction(), side + " hareket var.", fused);
    }
    private static float clamp(float x) { return Math.max(0f, Math.min(1f, x)); }
}
