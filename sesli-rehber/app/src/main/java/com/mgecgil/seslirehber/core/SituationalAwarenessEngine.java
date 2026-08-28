package com.mgecgil.seslirehber.core;

import java.util.HashMap;
import java.util.Map;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Lightweight temporal world model. It fuses recent multi-object, distant-semantic, motion,
 * ground/depth and walkable evidence into a 3x3 direction x relative-range awareness grid.
 * Range bands are image-relative proxies, never metric distance. This model does not issue STOP;
 * SafetyGate remains the authority for safety actions.
 */
public final class SituationalAwarenessEngine {
    public enum RangeBand { NEAR, MID, FAR, UNKNOWN }

    public record SectorSnapshot(
            Direction direction,
            float nearOccupancy,
            float midOccupancy,
            float farOccupancy,
            float dynamicScore,
            float approachingScore,
            int activeTracks,
            String farSemanticLabel,
            float farSemanticConfidence) {
        public float occupancyScore() {
            return clamp(Math.max(nearOccupancy, Math.max(midOccupancy * 0.82f, farOccupancy * 0.58f)));
        }
    }

    public record Snapshot(
            SectorSnapshot left,
            SectorSnapshot center,
            SectorSnapshot right,
            Direction attentionDirection,
            float sceneQuality,
            boolean groundDiscontinuity,
            boolean depthDiscontinuity,
            LevelChangeKind levelChangeKind,
            Direction moreOpenDirection,
            float environmentComplexity,
            float awarenessConfidence,
            long timestampMs) {}

    private static final long OBJECT_TTL_MS = 2600L;
    private static final long TRACK_TTL_MS = 2800L;
    private static final long MOTION_TTL_MS = 1800L;
    private static final long DISTANT_TTL_MS = 7000L;
    private static final long GROUND_TTL_MS = 2200L;
    private static final long DEPTH_TTL_MS = 1800L;
    private static final long WALKABLE_TTL_MS = 1800L;
    private static final long SCENE_TTL_MS = 2800L;

    private final SectorMemory left = new SectorMemory(Direction.LEFT);
    private final SectorMemory center = new SectorMemory(Direction.CENTER);
    private final SectorMemory right = new SectorMemory(Direction.RIGHT);
    private final Map<Integer, TrackMemory> tracks = new HashMap<>();

    private GroundObservation ground;
    private DepthObservation depth;
    private LevelChangeObservation levelChange;
    private WalkableCorridorObservation walkable;
    private SceneHealthObservation sceneHealth;

    public synchronized void noteObject(ObjectObservation observation) {
        if (observation == null || observation.direction() == Direction.UNKNOWN) return;
        SectorMemory sector = sector(observation.direction());
        RangeBand band = relativeRange(observation.areaRatio());
        float occupancy = clamp(0.24f + (float) Math.sqrt(Math.max(0f, observation.areaRatio()) / 0.22f) * 0.64f);
        sector.noteOccupancy(band, occupancy, observation.timestampMs());

        float dynamic = clamp(Math.abs(observation.centerVelocityX()) / 0.20f
                + Math.max(0f, observation.growthPerSecond()) / 0.24f);
        float approaching = clamp(Math.max(0f, observation.growthPerSecond()) / 0.18f);
        sector.noteDynamic(dynamic, approaching, observation.timestampMs());

        if (observation.trackingId() >= 0) {
            tracks.put(observation.trackingId(), new TrackMemory(
                    observation.direction(), band, observation.timestampMs()));
        }
        pruneTracks(observation.timestampMs());
    }

    public synchronized void noteDistant(DistantObjectObservation observation) {
        if (observation == null || !observation.mature() || observation.direction() == Direction.UNKNOWN) return;
        SectorMemory sector = sector(observation.direction());
        sector.noteOccupancy(
                RangeBand.FAR,
                clamp(0.32f + observation.labelConfidence() * 0.38f),
                observation.timestampMs());
        sector.farSemanticLabel = observation.label().trim();
        sector.farSemanticConfidence = observation.labelConfidence();
        sector.farSemanticMs = observation.timestampMs();
    }

    public synchronized void noteMotion(MotionObservation observation) {
        if (observation == null || observation.direction() == Direction.UNKNOWN
                || observation.changedAreaRatio() < 0.018f) return;
        float dynamic = clamp(observation.changedAreaRatio() * 3.4f
                * (0.45f + 0.55f * observation.visionConfidence()));
        sector(observation.direction()).noteDynamic(dynamic, 0f, observation.timestampMs());
    }

    public synchronized void noteGround(GroundObservation value) { if (value != null) ground = value; }
    public synchronized void noteDepth(DepthObservation value) { if (value != null) depth = value; }
    public synchronized void noteLevelChange(LevelChangeObservation value) { if (value != null) levelChange = value; }
    public synchronized void noteWalkable(WalkableCorridorObservation value) { if (value != null) walkable = value; }
    public synchronized void noteSceneHealth(SceneHealthObservation value) { if (value != null) sceneHealth = value; }

    public synchronized Snapshot snapshot(long nowMs) {
        pruneTracks(nowMs);
        SectorSnapshot l = left.snapshot(nowMs, activeTracks(Direction.LEFT, nowMs));
        SectorSnapshot c = center.snapshot(nowMs, activeTracks(Direction.CENTER, nowMs));
        SectorSnapshot r = right.snapshot(nowMs, activeTracks(Direction.RIGHT, nowMs));

        float lAttention = attentionScore(l, false);
        float cAttention = attentionScore(c, true);
        float rAttention = attentionScore(r, false);
        Direction attention = Direction.UNKNOWN;
        float maxAttention = Math.max(lAttention, Math.max(cAttention, rAttention));
        if (maxAttention >= 0.20f) {
            attention = cAttention >= lAttention && cAttention >= rAttention
                    ? Direction.CENTER
                    : (lAttention >= rAttention ? Direction.LEFT : Direction.RIGHT);
        }

        boolean groundChange = fresh(ground == null ? 0L : ground.timestampMs(), nowMs, GROUND_TTL_MS)
                && ground.persistentAnomaly();
        boolean depthChange = fresh(depth == null ? 0L : depth.timestampMs(), nowMs, DEPTH_TTL_MS)
                && depth.strongDiscontinuity();
        LevelChangeKind levelKind = fresh(levelChange == null ? 0L : levelChange.timestampMs(), nowMs, DEPTH_TTL_MS)
                && levelChange.persistentCandidate() ? levelChange.kind() : LevelChangeKind.UNKNOWN;
        Direction openDirection = fresh(walkable == null ? 0L : walkable.timestampMs(), nowMs, WALKABLE_TTL_MS)
                && walkable.hasPersistentCandidate() ? walkable.moreOpenDirection() : Direction.UNKNOWN;

        float sceneQuality = fresh(sceneHealth == null ? 0L : sceneHealth.timestampMs(), nowMs, SCENE_TTL_MS)
                ? sceneHealth.qualityScore() : 0.34f;
        int occupied = (l.occupancyScore() >= 0.36f ? 1 : 0)
                + (c.occupancyScore() >= 0.36f ? 1 : 0)
                + (r.occupancyScore() >= 0.36f ? 1 : 0);
        float complexity = clamp(occupied / 3f * 0.58f
                + Math.max(l.dynamicScore(), Math.max(c.dynamicScore(), r.dynamicScore())) * 0.27f
                + (groundChange || depthChange || levelKind != LevelChangeKind.UNKNOWN ? 0.18f : 0f));

        int evidenceChannels = 0;
        if (maxAttention > 0.10f) evidenceChannels++;
        if (groundChange) evidenceChannels++;
        if (depthChange) evidenceChannels++;
        if (levelKind != LevelChangeKind.UNKNOWN) evidenceChannels++;
        if (openDirection != Direction.UNKNOWN) evidenceChannels++;
        float awarenessConfidence = clamp(sceneQuality * 0.55f
                + Math.min(1f, evidenceChannels / 3f) * 0.45f);

        return new Snapshot(l, c, r, attention, sceneQuality, groundChange, depthChange,
                levelKind, openDirection, complexity, awarenessConfidence, nowMs);
    }

    /** Detailed user-requested world-model narration. It is descriptive, never a safe-path claim. */
    public synchronized String summarize(long nowMs) {
        Snapshot s = snapshot(nowMs);
        StringBuilder out = new StringBuilder("Durumsal görünüm. ");
        int before = out.length();
        appendSector(out, s.left());
        appendSector(out, s.center());
        appendSector(out, s.right());

        if (s.levelChangeKind() != LevelChangeKind.UNKNOWN) {
            switch (s.levelChangeKind()) {
                case DOWNWARD_CANDIDATE -> out.append("Ön zeminde aşağı yönlü seviye değişimi adayı izleniyor. ");
                case UPWARD_CANDIDATE -> out.append("Ön zeminde yukarı yönlü seviye değişimi adayı izleniyor. ");
                case MULTI_LEVEL_CANDIDATE -> out.append("Ön zeminde birden fazla seviye değişimi adayı izleniyor. ");
                default -> { }
            }
        } else if (s.groundDiscontinuity() || s.depthDiscontinuity()) {
            out.append("Ön zeminde geometri veya süreklilik değişimi izleniyor. ");
        }

        if (s.moreOpenDirection() == Direction.LEFT) {
            out.append("Sol taraf göreli olarak daha açık görünüyor; bu bir yön güvenliği onayı değildir. ");
        } else if (s.moreOpenDirection() == Direction.RIGHT) {
            out.append("Sağ taraf göreli olarak daha açık görünüyor; bu bir yön güvenliği onayı değildir. ");
        }
        if (s.environmentComplexity() >= 0.58f) {
            out.append("Çevresel hareket ve işgal yoğunluğu yüksek. ");
        }
        if (out.length() == before) {
            out.append("Şu anda modelde belirgin bir unsur öne çıkmıyor; bu tehlike olmadığı anlamına gelmez. ");
        }
        out.append("Bastonla çevre doğrulamasına devam et.");
        return out.toString();
    }

    public synchronized void reset() {
        left.reset(); center.reset(); right.reset(); tracks.clear();
        ground = null; depth = null; levelChange = null; walkable = null; sceneHealth = null;
    }

    static RangeBand relativeRange(float areaRatio) {
        if (areaRatio >= 0.14f) return RangeBand.NEAR;
        if (areaRatio >= 0.040f) return RangeBand.MID;
        if (areaRatio > 0f) return RangeBand.FAR;
        return RangeBand.UNKNOWN;
    }

    private int activeTracks(Direction direction, long nowMs) {
        int count = 0;
        for (TrackMemory track : tracks.values()) {
            if (track.direction == direction && fresh(track.timestampMs, nowMs, TRACK_TTL_MS)) count++;
        }
        return count;
    }

    private void pruneTracks(long nowMs) {
        tracks.entrySet().removeIf(e -> !fresh(e.getValue().timestampMs, nowMs, TRACK_TTL_MS));
    }

    private SectorMemory sector(Direction direction) {
        return switch (direction) {
            case LEFT -> left;
            case RIGHT -> right;
            default -> center;
        };
    }

    private static float attentionScore(SectorSnapshot s, boolean center) {
        return clamp(s.nearOccupancy() * 0.62f
                + s.midOccupancy() * 0.28f
                + s.farOccupancy() * 0.10f
                + s.approachingScore() * 0.42f
                + s.dynamicScore() * 0.18f
                + (center ? 0.08f : 0f));
    }

    private static void appendSector(StringBuilder out, SectorSnapshot s) {
        String where = switch (s.direction()) {
            case LEFT -> "Solda";
            case RIGHT -> "Sağda";
            default -> "Önde";
        };
        if (s.nearOccupancy() >= 0.44f) {
            out.append(where).append(" yakın bölgede engel izi");
        } else if (s.midOccupancy() >= 0.38f) {
            out.append(where).append(" orta bölgede engel izi");
        } else if (s.farOccupancy() >= 0.32f) {
            if (s.farSemanticLabel() != null && !s.farSemanticLabel().isBlank()) {
                out.append(where).append(" uzakta ").append(s.farSemanticLabel()).append(" adayı");
            } else {
                out.append(where).append(" uzak bölgede nesne izi");
            }
        } else {
            return;
        }
        if (s.approachingScore() >= 0.45f) out.append(", yaklaşma eğilimi var");
        else if (s.dynamicScore() >= 0.42f) out.append(", hareketli");
        if (s.activeTracks() >= 2) out.append(", birden fazla iz");
        out.append(". ");
    }

    private static boolean fresh(long timestampMs, long nowMs, long ttlMs) {
        return timestampMs > 0L && nowMs >= timestampMs && nowMs - timestampMs <= ttlMs;
    }

    private static float decay(float value, long timestampMs, long nowMs, long ttlMs) {
        if (!fresh(timestampMs, nowMs, ttlMs)) return 0f;
        float age = (nowMs - timestampMs) / (float) ttlMs;
        return clamp(value * (1f - 0.72f * age));
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }

    private static final class SectorMemory {
        final Direction direction;
        float near, mid, far, dynamic, approaching;
        long nearMs, midMs, farMs, dynamicMs;
        String farSemanticLabel = "";
        float farSemanticConfidence;
        long farSemanticMs;

        SectorMemory(Direction direction) { this.direction = direction; }

        void noteOccupancy(RangeBand band, float value, long timestampMs) {
            switch (band) {
                case NEAR -> { near = Math.max(decay(near, nearMs, timestampMs, OBJECT_TTL_MS), value); nearMs = timestampMs; }
                case MID -> { mid = Math.max(decay(mid, midMs, timestampMs, OBJECT_TTL_MS), value); midMs = timestampMs; }
                case FAR -> { far = Math.max(decay(far, farMs, timestampMs, DISTANT_TTL_MS), value); farMs = timestampMs; }
                default -> { }
            }
        }

        void noteDynamic(float dynamicValue, float approachingValue, long timestampMs) {
            dynamic = Math.max(decay(dynamic, dynamicMs, timestampMs, MOTION_TTL_MS), dynamicValue);
            approaching = Math.max(decay(approaching, dynamicMs, timestampMs, MOTION_TTL_MS), approachingValue);
            dynamicMs = timestampMs;
        }

        SectorSnapshot snapshot(long nowMs, int activeTracks) {
            String label = fresh(farSemanticMs, nowMs, DISTANT_TTL_MS) ? farSemanticLabel : "";
            float labelConfidence = fresh(farSemanticMs, nowMs, DISTANT_TTL_MS)
                    ? decay(farSemanticConfidence, farSemanticMs, nowMs, DISTANT_TTL_MS) : 0f;
            return new SectorSnapshot(direction,
                    decay(near, nearMs, nowMs, OBJECT_TTL_MS),
                    decay(mid, midMs, nowMs, OBJECT_TTL_MS),
                    decay(far, farMs, nowMs, DISTANT_TTL_MS),
                    decay(dynamic, dynamicMs, nowMs, MOTION_TTL_MS),
                    decay(approaching, dynamicMs, nowMs, MOTION_TTL_MS),
                    activeTracks, label, labelConfidence);
        }

        void reset() {
            near = mid = far = dynamic = approaching = 0f;
            nearMs = midMs = farMs = dynamicMs = farSemanticMs = 0L;
            farSemanticLabel = ""; farSemanticConfidence = 0f;
        }
    }

    private record TrackMemory(Direction direction, RangeBand band, long timestampMs) {}
}
