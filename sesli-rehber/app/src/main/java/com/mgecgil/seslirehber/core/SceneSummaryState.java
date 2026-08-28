package com.mgecgil.seslirehber.core;

import java.util.ArrayList;
import java.util.List;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Keeps recent evidence for the user-triggered "çevremi anlat" command. v0.14 mirrors evidence into
 * a temporal 3x3 world model, while preserving conservative legacy fallbacks for sparse scenes.
 */
public final class SceneSummaryState {
    private static final long VISION_FRESH_MS = 2200L;
    private static final long DEPTH_FRESH_MS = 1600L;

    private MotionObservation motion;
    private ObjectObservation object;
    private GroundObservation ground;
    private DepthObservation depth;
    private WalkableCorridorObservation walkable;
    private SceneHealthObservation sceneHealth;

    public synchronized void update(MotionObservation value) {
        motion = value;
        SituationalAwarenessContext.noteMotion(value);
    }
    public synchronized void update(ObjectObservation value) {
        object = value;
        SituationalAwarenessContext.noteObject(value);
    }
    public synchronized void update(GroundObservation value) {
        ground = value;
        SituationalAwarenessContext.noteGround(value);
    }
    public synchronized void update(DepthObservation value) {
        depth = value;
        SituationalAwarenessContext.noteDepth(value);
    }
    public synchronized void update(WalkableCorridorObservation value) {
        walkable = value;
        SituationalAwarenessContext.noteWalkable(value);
    }
    public synchronized void update(SceneHealthObservation value) {
        sceneHealth = value;
        SituationalAwarenessContext.noteSceneHealth(value);
    }

    public synchronized String summarize(long nowMs) {
        SituationalAwarenessEngine.Snapshot awareness = SituationalAwarenessContext.snapshot(nowMs);
        boolean worldHasEvidence = awareness.left().occupancyScore() >= 0.24f
                || awareness.center().occupancyScore() >= 0.24f
                || awareness.right().occupancyScore() >= 0.24f
                || awareness.groundDiscontinuity()
                || awareness.depthDiscontinuity()
                || awareness.levelChangeKind() != LevelChangeKind.UNKNOWN
                || awareness.moreOpenDirection() != Direction.UNKNOWN;
        if (worldHasEvidence) {
            // Preserve the mature product-language contract used by existing accessibility tests.
            return SituationalAwarenessContext.summarize(nowMs)
                    .replace("bu bir yön güvenliği onayı değildir", "bu güvenli yol onayı değildir");
        }

        List<String> parts = new ArrayList<>();

        if (fresh(sceneHealth == null ? 0L : sceneHealth.timestampMs(), nowMs, VISION_FRESH_MS)) {
            if (sceneHealth.persistentlyUnusable()) {
                if (sceneHealth.darkRatio() >= 0.78f) parts.add("Kamera görüntüsü çok karanlık veya kapalı görünüyor.");
                else if (sceneHealth.brightRatio() >= 0.78f) parts.add("Kamera görüntüsü aşırı parlak görünüyor.");
                else parts.add("Kamera görüntüsü güvenilir değil.");
            }
        }

        if (fresh(object == null ? 0L : object.timestampMs(), nowMs, VISION_FRESH_MS)
                && object.visionConfidence() >= 0.48f && object.areaRatio() >= 0.055f) {
            String where = switch (object.direction()) {
                case LEFT -> "Solda";
                case RIGHT -> "Sağda";
                case CENTER -> "Önde";
                default -> "Çevrede";
            };
            if (object.isApproaching()) parts.add(where + " yaklaşan bir engel şekli izleniyor.");
            else parts.add(where + " belirgin bir engel şekli izleniyor.");
        }

        if (fresh(ground == null ? 0L : ground.timestampMs(), nowMs, VISION_FRESH_MS)
                && ground.persistentAnomaly() && ground.viewConfidence() >= 0.40f) {
            parts.add("Ön zeminde süreklilik değişimi izleniyor.");
        }

        if (fresh(depth == null ? 0L : depth.timestampMs(), nowMs, DEPTH_FRESH_MS)
                && depth.strongDiscontinuity()) {
            parts.add("Ön bölgede belirgin derinlik değişimi var.");
        }

        if (fresh(walkable == null ? 0L : walkable.timestampMs(), nowMs, DEPTH_FRESH_MS)
                && walkable.hasPersistentCandidate()
                && walkable.centerOpenScore() <= 0.56f) {
            if (walkable.moreOpenDirection() == Direction.LEFT) {
                parts.add("Sol taraf göreli olarak daha açık görünüyor; bu güvenli yol onayı değildir.");
            } else if (walkable.moreOpenDirection() == Direction.RIGHT) {
                parts.add("Sağ taraf göreli olarak daha açık görünüyor; bu güvenli yol onayı değildir.");
            }
        }

        if (parts.isEmpty()) {
            return "Şu anda belirgin bir tehlike kanıtı öne çıkmıyor. Bu, çevrenin güvenli olduğu anlamına gelmez; bastonla doğrulamaya devam et.";
        }
        return String.join(" ", parts);
    }

    public synchronized void reset() {
        motion = null;
        object = null;
        ground = null;
        depth = null;
        walkable = null;
        sceneHealth = null;
        SituationalAwarenessContext.reset();
    }

    private static boolean fresh(long timestampMs, long nowMs, long maxAgeMs) {
        return timestampMs > 0L && nowMs >= timestampMs && nowMs - timestampMs <= maxAgeMs;
    }
}
