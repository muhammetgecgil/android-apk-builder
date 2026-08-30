package com.mgecgil.seslirehber.core;

/**
 * Advisory pixel-level semantic evidence. Ratios are normalized mask area fractions.
 * The bundled DeepLab-v3 model is Pascal-style; road/sidewalk/curb are intentionally NOT claimed.
 */
public record SemanticSegmentationObservation(
        float personRatio,
        float vehicleRatio,
        float twoWheelerRatio,
        float animalRatio,
        float furnitureRatio,
        float smallObstacleRatio,
        float foregroundRatio,
        float leftOccupancy,
        float centerOccupancy,
        float rightOccupancy,
        float farOccupancy,
        float midOccupancy,
        float nearOccupancy,
        float lowerCenterOccupancy,
        float temporalStability,
        long inferenceMs,
        long timestampMs) {

    public boolean mature() {
        return temporalStability >= 0.48f && foregroundRatio >= 0.008f;
    }

    public float dynamicRelevantRatio() {
        return clamp(personRatio + vehicleRatio + twoWheelerRatio);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
