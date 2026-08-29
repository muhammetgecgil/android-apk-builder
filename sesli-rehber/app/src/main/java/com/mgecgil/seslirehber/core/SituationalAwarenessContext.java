package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/** Process-local bridge so all camera/depth/segmentation producers feed one temporal world model. */
public final class SituationalAwarenessContext {
    private static final SituationalAwarenessEngine ENGINE = new SituationalAwarenessEngine();
    private static final long SEGMENTATION_FRESH_MS = 1800L;
    private static SemanticSegmentationObservation segmentation;

    private SituationalAwarenessContext() {}

    public static void noteMotion(MotionObservation value) { ENGINE.noteMotion(value); }
    public static void noteObject(ObjectObservation value) { ENGINE.noteObject(value); }
    public static void noteDistant(DistantObjectObservation value) { ENGINE.noteDistant(value); }
    public static void noteGround(GroundObservation value) { ENGINE.noteGround(value); }
    public static void noteDepth(DepthObservation value) { ENGINE.noteDepth(value); }
    public static void noteLevelChange(LevelChangeObservation value) { ENGINE.noteLevelChange(value); }
    public static void noteWalkable(WalkableCorridorObservation value) { ENGINE.noteWalkable(value); }
    public static void noteSceneHealth(SceneHealthObservation value) { ENGINE.noteSceneHealth(value); }
    public static synchronized void noteSegmentation(SemanticSegmentationObservation value) {
        if (value != null) segmentation = value;
    }

    public static SituationalAwarenessEngine.Snapshot snapshot(long nowMs) { return ENGINE.snapshot(nowMs); }

    public static synchronized boolean hasFreshSegmentation(long nowMs) {
        SemanticSegmentationObservation s = segmentation;
        return s != null && fresh(s.timestampMs(), nowMs, SEGMENTATION_FRESH_MS) && s.mature();
    }

    public static synchronized String summarize(long nowMs) {
        String base = ENGINE.summarize(nowMs);
        SemanticSegmentationObservation s = segmentation;
        if (s == null || !fresh(s.timestampMs(), nowMs, SEGMENTATION_FRESH_MS) || !s.mature()) return base;

        StringBuilder extra = new StringBuilder();
        if (s.personRatio() >= 0.012f) extra.append(" Segmentasyonda insan alanı belirgin.");
        if (s.vehicleRatio() >= 0.012f) extra.append(" Araç bölgesi adayı segmentasyonda görülüyor.");
        if (s.twoWheelerRatio() >= 0.008f) extra.append(" Bisiklet veya motosiklet bölgesi adayı görülüyor.");
        if (s.animalRatio() >= 0.010f) extra.append(" Hayvan bölgesi adayı görülüyor.");
        if (s.furnitureRatio() >= 0.018f) extra.append(" Mobilya benzeri engel alanı segmentasyonda belirgin.");

        String dominant = dominantSector(s);
        if (!dominant.isEmpty()) extra.append(" Tanınmış nesne alanı ").append(dominant).append(" daha yoğun.");
        if (s.lowerCenterOccupancy() >= 0.22f) {
            extra.append(" Ön alt merkezde tanınmış nesne alanı yüksek; bu güvenli yol onayı değildir.");
        }
        return base + extra;
    }

    public static synchronized SemanticSegmentationObservation latestSegmentation(long nowMs) {
        SemanticSegmentationObservation s = segmentation;
        return s != null && fresh(s.timestampMs(), nowMs, SEGMENTATION_FRESH_MS) ? s : null;
    }

    public static synchronized void reset() {
        ENGINE.reset();
        segmentation = null;
    }

    private static String dominantSector(SemanticSegmentationObservation s) {
        float l = s.leftOccupancy(), c = s.centerOccupancy(), r = s.rightOccupancy();
        float max = Math.max(l, Math.max(c, r));
        if (max < 0.10f) return "";
        if (c >= l && c >= r) return "ön orta bölgede";
        if (l >= r) return "sol bölgede";
        return "sağ bölgede";
    }

    private static boolean fresh(long ts, long now, long ttl) {
        return ts > 0L && now >= ts && now - ts <= ttl;
    }
}
