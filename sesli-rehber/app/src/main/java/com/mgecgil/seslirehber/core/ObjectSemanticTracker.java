package com.mgecgil.seslirehber.core;

import java.util.HashMap;
import java.util.Map;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/** Temporal confirmation for crop-level object labels. */
public final class ObjectSemanticTracker {
    public record Result(ObjectSemanticObservation observation, boolean announce) {}

    static final float DEFINITE_REPEAT_CONFIDENCE = 0.72f;
    static final float DEFINITE_SINGLE_CONFIDENCE = 0.86f;
    static final float CANDIDATE_CONFIDENCE = 0.58f;
    private static final long TRACK_STALE_MS = 4200L;
    private static final long ANNOUNCE_COOLDOWN_MS = 14_000L;
    private static final float EMA_ALPHA = 0.42f;

    private final Map<Integer, State> states = new HashMap<>();
    private final Map<String, Announcement> announcements = new HashMap<>();

    public synchronized Result observe(
            int trackingId,
            String label,
            float confidence,
            Direction direction,
            long nowMs) {
        prune(nowMs);
        if (trackingId < 0 || label == null || label.isBlank() || confidence < CANDIDATE_CONFIDENCE) {
            return null;
        }
        String clean = label.trim();
        State previous = states.get(trackingId);
        int streak = 1;
        float smoothed = confidence;
        if (previous != null && previous.label.equals(clean) && nowMs - previous.timeMs <= TRACK_STALE_MS) {
            streak = Math.min(12, previous.streak + 1);
            smoothed = previous.confidence * (1f - EMA_ALPHA) + confidence * EMA_ALPHA;
        }
        states.put(trackingId, new State(clean, smoothed, streak, nowMs));

        boolean definite = confidence >= DEFINITE_SINGLE_CONFIDENCE
                || (streak >= 2 && smoothed >= DEFINITE_REPEAT_CONFIDENCE);
        boolean candidate = definite || (streak >= 2 && smoothed >= CANDIDATE_CONFIDENCE);
        if (!candidate) return null;

        ObjectSemanticObservation observation = new ObjectSemanticObservation(
                trackingId, clean, smoothed, definite,
                direction == null ? Direction.UNKNOWN : direction,
                streak, nowMs);
        boolean announce = shouldAnnounce(observation, nowMs);
        return new Result(observation, announce);
    }

    public synchronized void reset() {
        states.clear();
        announcements.clear();
    }

    private boolean shouldAnnounce(ObjectSemanticObservation o, long nowMs) {
        String key = o.label() + "|" + o.direction();
        Announcement previous = announcements.get(key);
        boolean allow = previous == null
                || (o.definite() && !previous.definite)
                || nowMs - previous.timeMs >= ANNOUNCE_COOLDOWN_MS;
        if (allow) announcements.put(key, new Announcement(o.definite(), nowMs));
        return allow;
    }

    private void prune(long nowMs) {
        states.entrySet().removeIf(e -> nowMs - e.getValue().timeMs > TRACK_STALE_MS);
        announcements.entrySet().removeIf(e -> nowMs - e.getValue().timeMs > ANNOUNCE_COOLDOWN_MS * 2L);
    }

    private record State(String label, float confidence, int streak, long timeMs) {}
    private record Announcement(boolean definite, long timeMs) {}
}
