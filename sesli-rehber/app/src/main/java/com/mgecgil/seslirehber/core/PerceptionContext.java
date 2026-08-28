package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Small process-local snapshot bridge between camera/depth producers and the safety gate.
 * Every read is freshness-checked; stale evidence is never fused into a current decision.
 */
public final class PerceptionContext {
    private static volatile SceneHealthObservation sceneHealth;
    private static volatile GroundObservation ground;
    private static volatile WalkableCorridorObservation walkable;
    private static volatile LevelChangeObservation levelChange;

    private PerceptionContext() {}

    public static void noteSceneHealth(SceneHealthObservation observation) {
        if (observation != null) sceneHealth = observation;
    }

    public static void noteGround(GroundObservation observation) {
        if (observation != null) ground = observation;
    }

    public static void noteWalkable(WalkableCorridorObservation observation) {
        if (observation != null) walkable = observation;
    }

    public static void noteLevelChange(LevelChangeObservation observation) {
        if (observation != null) levelChange = observation;
    }

    public static SceneHealthObservation sceneHealthNear(long timestampMs, long maxSkewMs) {
        SceneHealthObservation value = sceneHealth;
        if (value == null || timestampMs <= 0L) return null;
        return Math.abs(value.timestampMs() - timestampMs) <= maxSkewMs ? value : null;
    }

    public static GroundObservation groundNear(long timestampMs, long maxSkewMs) {
        GroundObservation value = ground;
        if (value == null || timestampMs <= 0L) return null;
        return Math.abs(value.timestampMs() - timestampMs) <= maxSkewMs ? value : null;
    }

    public static WalkableCorridorObservation walkableNear(long timestampMs, long maxSkewMs) {
        WalkableCorridorObservation value = walkable;
        if (value == null || timestampMs <= 0L) return null;
        return Math.abs(value.timestampMs() - timestampMs) <= maxSkewMs ? value : null;
    }

    public static LevelChangeObservation levelChangeNear(long timestampMs, long maxSkewMs) {
        LevelChangeObservation value = levelChange;
        if (value == null || timestampMs <= 0L) return null;
        return Math.abs(value.timestampMs() - timestampMs) <= maxSkewMs ? value : null;
    }

    static void resetForTest() {
        sceneHealth = null;
        ground = null;
        walkable = null;
        levelChange = null;
    }
}
