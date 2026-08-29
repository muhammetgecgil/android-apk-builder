package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/** Process-local bridge so all camera/depth/segmentation producers feed one temporal world model. */
public final class SituationalAwarenessContext {
    private static final SituationalAwarenessEngine ENGINE = new SituationalAwarenessEngine();
    private static final long SEGMENTATION_FRESH_MS = 1800L;
    private static final long URBAN_SEGMENTATION_FRESH_MS = 2800L;
    private static SemanticSegmentationObservation segmentation;
    private static UrbanSegmentationObservation urbanSegmentation;

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

    public static synchronized void noteUrbanSegmentation(UrbanSegmentationObservation value) {
        if (value != null) urbanSegmentation = value;
    }

    public static SituationalAwarenessEngine.Snapshot snapshot(long nowMs) { return ENGINE.snapshot(nowMs); }

    public static synchronized boolean hasFreshSegmentation(long nowMs) {
        SemanticSegmentationObservation s = segmentation;
        return s != null && fresh(s.timestampMs(), nowMs, SEGMENTATION_FRESH_MS) && s.mature();
    }

    public static synchronized boolean hasFreshUrbanSegmentation(long nowMs) {
        UrbanSegmentationObservation s = urbanSegmentation;
        return s != null && fresh(s.timestampMs(), nowMs, URBAN_SEGMENTATION_FRESH_MS) && s.mature();
    }

    public static synchronized String summarize(long nowMs) {
        String base = ENGINE.summarize(nowMs);
        StringBuilder extra = new StringBuilder();

        SemanticSegmentationObservation s = segmentation;
        if (s != null && fresh(s.timestampMs(), nowMs, SEGMENTATION_FRESH_MS) && s.mature()) {
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
        }

        UrbanSegmentationObservation u = urbanSegmentation;
        if (u != null && fresh(u.timestampMs(), nowMs, URBAN_SEGMENTATION_FRESH_MS) && u.mature()) {
            appendUrbanSummary(extra, u);
        }
        return base + extra;
    }

    public static synchronized SemanticSegmentationObservation latestSegmentation(long nowMs) {
        SemanticSegmentationObservation s = segmentation;
        return s != null && fresh(s.timestampMs(), nowMs, SEGMENTATION_FRESH_MS) ? s : null;
    }

    public static synchronized UrbanSegmentationObservation latestUrbanSegmentation(long nowMs) {
        UrbanSegmentationObservation s = urbanSegmentation;
        return s != null && fresh(s.timestampMs(), nowMs, URBAN_SEGMENTATION_FRESH_MS) ? s : null;
    }

    public static synchronized void reset() {
        ENGINE.reset();
        segmentation = null;
        urbanSegmentation = null;
    }

    private static void appendUrbanSummary(StringBuilder out, UrbanSegmentationObservation u) {
        if (u.sidewalkRatio() >= 0.08f) {
            out.append(" Şehir segmentasyonunda kaldırım sınıfı belirgin.");
        }
        if (u.roadRatio() >= 0.16f) {
            out.append(" Yol yüzeyi sınıfı görüntüde belirgin.");
        }
        if (u.buildingWallRatio() >= 0.12f) {
            out.append(" Bina veya duvar alanı belirgin.");
        }
        if (u.fencePoleRatio() >= 0.025f) {
            out.append(" Çit veya direk sınıfı adayı görülüyor.");
        }
        if (u.trafficControlRatio() >= 0.0035f) {
            out.append(" Trafik ışığı veya trafik işareti sınıfı adayı görülüyor.");
        }
        if (u.vegetationRatio() >= 0.14f) {
            out.append(" Bitki alanı görüntüde belirgin.");
        }
        if (u.personRiderRatio() >= 0.010f) {
            out.append(" İnsan veya sürücü sınıfı alanı görülüyor.");
        }
        if (u.vehicleRatio() >= 0.010f) {
            out.append(" Araç sınıfı alanı görülüyor.");
        }
        if (u.twoWheelerRatio() >= 0.006f) {
            out.append(" Bisiklet veya motosiklet sınıfı alanı görülüyor.");
        }

        String obstacleSector = dominantUrbanObstacleSector(u);
        if (!obstacleSector.isEmpty()) {
            out.append(" Şehir sınıflarındaki engel benzeri alan ").append(obstacleSector).append(" daha yoğun.");
        }
        if (u.lowerCenterSidewalkRatio() >= 0.28f) {
            out.append(" Ön alt merkezde kaldırım sınıfı baskın; bu yürünebilirlik veya güvenli yol onayı değildir.");
        } else if (u.lowerCenterRoadRatio() >= 0.34f) {
            out.append(" Ön alt merkezde yol yüzeyi sınıfı baskın; bu karşıya geçiş güvenliği onayı değildir.");
        }
        if (u.lowerCenterObstacleRatio() >= 0.22f) {
            out.append(" Ön alt merkezde şehir sınıflarına göre engel benzeri alan yüksek.");
        }
    }

    private static String dominantSector(SemanticSegmentationObservation s) {
        float l = s.leftOccupancy(), c = s.centerOccupancy(), r = s.rightOccupancy();
        float max = Math.max(l, Math.max(c, r));
        if (max < 0.10f) return "";
        if (c >= l && c >= r) return "ön orta bölgede";
        if (l >= r) return "sol bölgede";
        return "sağ bölgede";
    }

    private static String dominantUrbanObstacleSector(UrbanSegmentationObservation s) {
        float l = s.leftObstacleOccupancy();
        float c = s.centerObstacleOccupancy();
        float r = s.rightObstacleOccupancy();
        float max = Math.max(l, Math.max(c, r));
        if (max < 0.16f) return "";
        if (c >= l && c >= r) return "ön orta bölgede";
        if (l >= r) return "sol bölgede";
        return "sağ bölgede";
    }

    private static boolean fresh(long ts, long now, long ttl) {
        return ts > 0L && now >= ts && now - ts <= ttl;
    }
}
