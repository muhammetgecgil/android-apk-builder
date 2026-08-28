package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/** Process-local bridge so all camera/depth producers feed one temporal world model. */
public final class SituationalAwarenessContext {
    private static final SituationalAwarenessEngine ENGINE = new SituationalAwarenessEngine();
    private SituationalAwarenessContext() {}

    public static void noteMotion(MotionObservation value) { ENGINE.noteMotion(value); }
    public static void noteObject(ObjectObservation value) { ENGINE.noteObject(value); }
    public static void noteDistant(DistantObjectObservation value) { ENGINE.noteDistant(value); }
    public static void noteGround(GroundObservation value) { ENGINE.noteGround(value); }
    public static void noteDepth(DepthObservation value) { ENGINE.noteDepth(value); }
    public static void noteLevelChange(LevelChangeObservation value) { ENGINE.noteLevelChange(value); }
    public static void noteWalkable(WalkableCorridorObservation value) { ENGINE.noteWalkable(value); }
    public static void noteSceneHealth(SceneHealthObservation value) { ENGINE.noteSceneHealth(value); }

    public static SituationalAwarenessEngine.Snapshot snapshot(long nowMs) { return ENGINE.snapshot(nowMs); }
    public static String summarize(long nowMs) { return ENGINE.summarize(nowMs); }
    public static void reset() { ENGINE.reset(); }
}
