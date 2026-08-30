package com.mgecgil.seslirehber.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Visual-only cache for the Tesla-style camera HUD. It has no SafetyGate authority and is not used
 * for user guidance decisions. It simply mirrors already-produced evidence for display.
 */
public final class HudPerceptionContext {
    public record Snapshot(
            List<ObjectObservation> objects,
            GroundObservation ground,
            DepthObservation depth,
            WalkableCorridorObservation walkable,
            SceneHealthObservation sceneHealth,
            float sourceAspect,
            long timestampMs) {}

    private static final long OBJECT_FRESH_MS = 2200L;
    private static final long OTHER_FRESH_MS = 2200L;
    private static final int MAX_OBJECTS = 8;
    private static final Map<Integer, ObjectObservation> TRACKS = new LinkedHashMap<>();
    private static int anonymousId = -1;
    private static GroundObservation ground;
    private static DepthObservation depth;
    private static WalkableCorridorObservation walkable;
    private static SceneHealthObservation sceneHealth;
    private static float sourceAspect = 9f / 16f;

    private HudPerceptionContext() {}

    public static synchronized void noteSourceAspect(float value) {
        if (value > 0.2f && value < 5f) sourceAspect = value;
    }

    public static synchronized void noteObject(ObjectObservation value) {
        if (value == null) return;
        int key = value.trackingId() >= 0 ? value.trackingId() : anonymousId--;
        TRACKS.put(key, value);
        while (TRACKS.size() > MAX_OBJECTS) {
            Integer first = TRACKS.keySet().iterator().next();
            TRACKS.remove(first);
        }
    }

    public static synchronized void noteGround(GroundObservation value) { if (value != null) ground = value; }
    public static synchronized void noteDepth(DepthObservation value) { if (value != null) depth = value; }
    public static synchronized void noteWalkable(WalkableCorridorObservation value) { if (value != null) walkable = value; }
    public static synchronized void noteSceneHealth(SceneHealthObservation value) { if (value != null) sceneHealth = value; }

    public static synchronized Snapshot snapshot(long nowMs) {
        TRACKS.entrySet().removeIf(e -> !fresh(e.getValue().timestampMs(), nowMs, OBJECT_FRESH_MS));
        ObjectSemanticContext.prune(nowMs);
        List<ObjectObservation> objects = new ArrayList<>(TRACKS.values());
        return new Snapshot(
                List.copyOf(objects),
                fresh(ground == null ? 0L : ground.timestampMs(), nowMs, OTHER_FRESH_MS) ? ground : null,
                fresh(depth == null ? 0L : depth.timestampMs(), nowMs, OTHER_FRESH_MS) ? depth : null,
                fresh(walkable == null ? 0L : walkable.timestampMs(), nowMs, OTHER_FRESH_MS) ? walkable : null,
                fresh(sceneHealth == null ? 0L : sceneHealth.timestampMs(), nowMs, OTHER_FRESH_MS) ? sceneHealth : null,
                sourceAspect,
                nowMs);
    }

    public static synchronized void reset() {
        TRACKS.clear();
        anonymousId = -1;
        ground = null;
        depth = null;
        walkable = null;
        sceneHealth = null;
        sourceAspect = 9f / 16f;
        ObjectSemanticContext.reset();
    }

    private static boolean fresh(long timestampMs, long nowMs, long ttlMs) {
        return timestampMs > 0L && nowMs >= timestampMs && nowMs - timestampMs <= ttlMs;
    }
}
