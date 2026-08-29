package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Process-local shared world model bridge. Producers can update it without coupling MainActivity
 * to each perception implementation. SafetyGate remains separate and authoritative for STOP.
 */
public final class SituationalAwarenessHub {
    private static final SituationalAwarenessEngine ENGINE = new SituationalAwarenessEngine();
    private static final long SEGMENTATION_FRESH_MS = 1800L;
    private static SemanticSegmentationObservation segmentation;

    private SituationalAwarenessHub() {}

    public static synchronized void note(MotionObservation v) { if (v != null) ENGINE.noteMotion(v); }
    public static synchronized void note(ObjectObservation v) { if (v != null) ENGINE.noteObject(v); }
    public static synchronized void note(GroundObservation v) { if (v != null) ENGINE.noteGround(v); }
    public static synchronized void note(DepthObservation v) { if (v != null) ENGINE.noteDepth(v); }
    public static synchronized void note(LevelChangeObservation v) { if (v != null) ENGINE.noteLevelChange(v); }
    public static synchronized void note(WalkableCorridorObservation v) { if (v != null) ENGINE.noteWalkable(v); }
    public static synchronized void note(SceneHealthObservation v) { if (v != null) ENGINE.noteSceneHealth(v); }
    public static synchronized void note(DistantObjectObservation v) { if (v != null) ENGINE.noteDistant(v); }

    public static synchronized void note(SemanticSegmentationObservation v) {
        if (v != null) segmentation = v;
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
        if (!dominant.isEmpty()) extra.append(" Tanınmış nesne alanı göreli olarak ").append(dominant).append(" yoğun.");
        if (s.lowerCenterOccupancy() >= 0.22f) {
            extra.append(" Ön alt merkezde tanınmış nesne alanı yüksek; bu yürünebilir yol onayı değildir.");
        }
        if (extra.length() == 0) return base;
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
        if (c >= l && c >= r) return "ön orta bölgede daha";
        if (l >= r) return "sol bölgede daha";
        return "sağ bölgede daha";
    }

    private static boolean fresh(long ts, long now, long ttl) {
        return ts > 0L && now >= ts && now - ts <= ttl;
    }
}
