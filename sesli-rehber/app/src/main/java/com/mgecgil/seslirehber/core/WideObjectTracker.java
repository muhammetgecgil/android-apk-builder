package com.mgecgil.seslirehber.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/**
 * Spatial + temporal identity gate for the broad named-object detector.
 * A repeated label only accumulates confidence when it comes from the same physical image region.
 */
public final class WideObjectTracker {
    public record Result(WideObjectObservation observation, boolean announce) {}

    private static final float STRONG = 0.82f;
    private static final float REPEATED_DEFINITE = 0.68f;
    private static final float CANDIDATE = 0.56f;
    private static final float IMPORTANT_STRONG = 0.88f;
    private static final float IMPORTANT_REPEAT = 0.74f;
    private static final float IMPORTANT_CANDIDATE = 0.62f;
    private static final long STREAK_WINDOW_MS = 1800L;
    private static final long TRACK_STALE_MS = 3400L;
    private static final long SPEECH_COOLDOWN_MS = 9000L;
    private static final long LABEL_CORRECTION_GAP_MS = 2800L;
    private static final float BOX_ALPHA = 0.46f;
    private static final float CONF_ALPHA = 0.55f;
    private static final float VOTE_DECAY = 0.76f;
    private static final int MAX_TRACKS = 24;

    private final Map<Integer, Track> tracks = new HashMap<>();
    private int nextTrackId = 1;

    public synchronized Result observe(
            String label,
            float confidence,
            float left,
            float top,
            float right,
            float bottom,
            long nowMs) {
        prune(nowMs);
        if (label == null || label.isBlank() || right <= left || bottom <= top) return null;
        String clean = label.trim();
        boolean important = WideObjectPolicy.important(clean);
        float candidateFloor = important ? IMPORTANT_CANDIDATE : CANDIDATE;
        if (confidence < candidateFloor) return null;

        Track track = findTrack(clean, left, top, right, bottom, nowMs);
        if (track == null) {
            track = new Track(nextTrackId++, left, top, right, bottom, nowMs);
            tracks.put(track.id, track);
            trimTracks();
        } else {
            track.left = ema(track.left, left, BOX_ALPHA);
            track.top = ema(track.top, top, BOX_ALPHA);
            track.right = ema(track.right, right, BOX_ALPHA);
            track.bottom = ema(track.bottom, bottom, BOX_ALPHA);
        }

        long gap = Math.max(0L, nowMs - track.lastSeenMs);
        if (clean.equals(track.lastObservedLabel) && gap <= STREAK_WINDOW_MS) {
            track.consecutive = Math.min(20, track.consecutive + 1);
        } else {
            track.consecutive = 1;
        }
        track.lastObservedLabel = clean;
        track.lastSeenMs = nowMs;
        track.lastFrameMs = nowMs;

        for (Vote vote : track.votes.values()) vote.weight *= VOTE_DECAY;
        Vote vote = track.votes.get(clean);
        if (vote == null) {
            vote = new Vote(confidence, confidence, 1, nowMs);
            track.votes.put(clean, vote);
        } else {
            vote.emaConfidence = ema(vote.emaConfidence, confidence, CONF_ALPHA);
            vote.weight += confidence;
            vote.hits = Math.min(30, vote.hits + 1);
            vote.lastSeenMs = nowMs;
        }
        track.votes.entrySet().removeIf(e -> nowMs - e.getValue().lastSeenMs > TRACK_STALE_MS);

        Winner winner = winner(track.votes);
        if (winner == null || !clean.equals(winner.label)) return null;

        float strongFloor = important ? IMPORTANT_STRONG : STRONG;
        float repeatedFloor = important ? IMPORTANT_REPEAT : REPEATED_DEFINITE;
        float margin = winner.score - winner.secondScore;
        boolean strongSingle = winner.vote.hits == 1 && confidence >= strongFloor;
        boolean repeatedDefinite = track.consecutive >= 2
                && winner.vote.emaConfidence >= repeatedFloor
                && margin >= 0.08f;
        boolean definite = strongSingle || repeatedDefinite;
        boolean usable = definite || (track.consecutive >= 2
                && winner.vote.emaConfidence >= candidateFloor
                && margin >= 0.04f);
        if (!usable) return null;

        float cx = (track.left + track.right) * 0.5f;
        Direction direction = cx < 0.38f ? Direction.LEFT : cx > 0.62f ? Direction.RIGHT : Direction.CENTER;
        WideObjectObservation observation = new WideObjectObservation(
                clean,
                winner.vote.emaConfidence,
                clamp(track.left),
                clamp(track.top),
                clamp(track.right),
                clamp(track.bottom),
                direction,
                definite,
                important,
                nowMs);

        boolean correction = track.lastSpokenLabel != null
                && !track.lastSpokenLabel.equals(clean)
                && nowMs - track.lastSpokenMs >= LABEL_CORRECTION_GAP_MS;
        boolean announce = track.lastSpokenMs == 0L
                || correction
                || nowMs - track.lastSpokenMs >= SPEECH_COOLDOWN_MS;
        if (announce) {
            track.lastSpokenMs = nowMs;
            track.lastSpokenLabel = clean;
        }
        return new Result(observation, announce);
    }

    public synchronized void reset() {
        tracks.clear();
        nextTrackId = 1;
    }

    private Track findTrack(
            String label,
            float left,
            float top,
            float right,
            float bottom,
            long nowMs) {
        Track best = null;
        float bestScore = -Float.MAX_VALUE;
        for (Track t : tracks.values()) {
            if (nowMs - t.lastSeenMs > TRACK_STALE_MS) continue;
            // A track can consume at most one detector result from the same frame.
            if (t.lastFrameMs == nowMs) continue;
            String previous = t.lastObservedLabel == null ? "" : t.lastObservedLabel;
            boolean sameLabel = label.equals(previous);
            if (!SpatialIdentityPolicy.samePhysicalRegion(
                    sameLabel,
                    left, top, right, bottom,
                    t.left, t.top, t.right, t.bottom)) continue;
            float overlap = SpatialIdentityPolicy.iou(
                    left, top, right, bottom, t.left, t.top, t.right, t.bottom);
            float distance = SpatialIdentityPolicy.centerDistance(
                    left, top, right, bottom, t.left, t.top, t.right, t.bottom);
            float score = overlap * 2.1f - distance + (sameLabel ? 0.28f : 0f);
            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }
        return best;
    }

    private static Winner winner(Map<String, Vote> votes) {
        String bestLabel = null;
        Vote bestVote = null;
        float best = -1f;
        float second = -1f;
        for (Map.Entry<String, Vote> e : votes.entrySet()) {
            Vote v = e.getValue();
            float score = v.weight + Math.min(0.24f, v.hits * 0.04f) + v.emaConfidence * 0.24f;
            if (score > best) {
                second = best;
                best = score;
                bestLabel = e.getKey();
                bestVote = v;
            } else if (score > second) {
                second = score;
            }
        }
        if (bestVote == null) return null;
        return new Winner(bestLabel, bestVote, best, Math.max(0f, second));
    }

    private void prune(long nowMs) {
        Iterator<Map.Entry<Integer, Track>> it = tracks.entrySet().iterator();
        while (it.hasNext()) {
            Track t = it.next().getValue();
            if (nowMs - t.lastSeenMs > TRACK_STALE_MS) it.remove();
        }
    }

    private void trimTracks() {
        while (tracks.size() > MAX_TRACKS) {
            Integer oldestId = null;
            long oldest = Long.MAX_VALUE;
            for (Map.Entry<Integer, Track> e : tracks.entrySet()) {
                if (e.getValue().lastSeenMs < oldest) {
                    oldest = e.getValue().lastSeenMs;
                    oldestId = e.getKey();
                }
            }
            if (oldestId == null) return;
            tracks.remove(oldestId);
        }
    }

    private static float ema(float oldValue, float current, float alpha) {
        return oldValue * (1f - alpha) + current * alpha;
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static final class Track {
        final int id;
        float left, top, right, bottom;
        long lastSeenMs;
        long lastFrameMs = Long.MIN_VALUE;
        String lastObservedLabel;
        int consecutive;
        long lastSpokenMs;
        String lastSpokenLabel;
        final Map<String, Vote> votes = new HashMap<>();

        Track(int id, float left, float top, float right, float bottom, long nowMs) {
            this.id = id;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.lastSeenMs = nowMs;
        }
    }

    private static final class Vote {
        float emaConfidence;
        float weight;
        int hits;
        long lastSeenMs;

        Vote(float emaConfidence, float weight, int hits, long lastSeenMs) {
            this.emaConfidence = emaConfidence;
            this.weight = weight;
            this.hits = hits;
            this.lastSeenMs = lastSeenMs;
        }
    }

    private record Winner(String label, Vote vote, float score, float secondScore) {}
}
