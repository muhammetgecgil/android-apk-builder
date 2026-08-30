package com.mgecgil.seslirehber.core;

/**
 * Advisory Cityscapes-style urban semantic evidence from PIDNet-S.
 * Ratios are image-mask area fractions; they are not metric geometry or a safe-path decision.
 */
public record UrbanSegmentationObservation(
        float roadRatio,
        float sidewalkRatio,
        float buildingWallRatio,
        float fencePoleRatio,
        float trafficControlRatio,
        float vegetationRatio,
        float terrainRatio,
        float personRiderRatio,
        float vehicleRatio,
        float twoWheelerRatio,
        float skyRatio,
        float leftObstacleOccupancy,
        float centerObstacleOccupancy,
        float rightObstacleOccupancy,
        float lowerCenterRoadRatio,
        float lowerCenterSidewalkRatio,
        float lowerCenterObstacleRatio,
        float temporalStability,
        long inferenceMs,
        long timestampMs) {

    public boolean mature() {
        return temporalStability >= 0.48f && classifiedRatio() >= 0.08f;
    }

    public float classifiedRatio() {
        return clamp(roadRatio + sidewalkRatio + buildingWallRatio + fencePoleRatio
                + trafficControlRatio + vegetationRatio + terrainRatio + personRiderRatio
                + vehicleRatio + twoWheelerRatio + skyRatio);
    }

    public float dynamicRelevantRatio() {
        return clamp(personRiderRatio + vehicleRatio + twoWheelerRatio);
    }

    public boolean hasLowerCenterSurfaceEvidence() {
        return lowerCenterRoadRatio >= 0.16f || lowerCenterSidewalkRatio >= 0.12f;
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
