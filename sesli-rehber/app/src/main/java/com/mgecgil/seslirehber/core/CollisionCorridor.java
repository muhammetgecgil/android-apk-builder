package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.CorridorAssessment;
import static com.mgecgil.seslirehber.core.GuidanceModels.ObjectObservation;

/**
 * Geometry-only collision corridor. It does not claim physical distance.
 * The corridor widens slightly when device stability degrades, while very low stability is
 * handled fail-safe by SafetyGate before this score is trusted.
 */
public final class CollisionCorridor {
    public CorridorAssessment assess(ObjectObservation observation, float deviceStability) {
        float stability = clamp01(deviceStability);
        float halfWidth = 0.17f + (1f - stability) * 0.10f;
        float lateralDistance = Math.abs(observation.centerX() - 0.5f);
        float pathOverlap = clamp01(1f - lateralDistance / Math.max(0.08f, halfWidth));

        float sizeScore = clamp01((observation.areaRatio() - 0.025f) / 0.28f);
        float approachScore = clamp01((observation.growthPerSecond() - 0.015f) / 0.22f);

        float towardCenterVelocity = Math.signum(0.5f - observation.centerX()) * observation.centerVelocityX();
        float crossingScore = clamp01((towardCenterVelocity - 0.015f) / 0.18f);

        // Objects whose box extends into the lower/central image are more likely to intersect the
        // walking corridor, but retain a floor so head-height/hanging hazards are not discarded.
        float verticalRelevance = Math.max(0.52f, clamp01((observation.bottomY() - 0.20f) / 0.70f));

        float hazard = (
                0.30f * sizeScore
                + 0.29f * approachScore
                + 0.24f * pathOverlap
                + 0.10f * crossingScore
                + 0.07f * verticalRelevance);
        hazard *= (0.72f + 0.28f * stability);
        hazard *= (0.68f + 0.32f * clamp01(observation.visionConfidence()));
        hazard = clamp01(hazard);

        boolean inCorridor = pathOverlap >= 0.38f;
        boolean crossingIntoCorridor = !inCorridor && observation.isMovingTowardCenter() && crossingScore >= 0.18f;
        return new CorridorAssessment(
                halfWidth,
                pathOverlap,
                sizeScore,
                approachScore,
                crossingScore,
                verticalRelevance,
                hazard,
                inCorridor,
                crossingIntoCorridor);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
