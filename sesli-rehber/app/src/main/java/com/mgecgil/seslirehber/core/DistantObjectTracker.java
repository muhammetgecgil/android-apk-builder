package com.mgecgil.seslirehber.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/** Multi-frame confirmation and speech cooldown for advisory long-range semantic candidates. */
public final class DistantObjectTracker {
    private static final long TRACK_GAP_MS = 2600L;
    private static final long TRACK_STALE_MS = 9000L;
    private static final long SPEECH_COOLDOWN_MS = 9000L;
    private final Map<String, State> states = new HashMap<>();

    public synchronized DistantObjectObservation observe(
            String label,
            Direction direction,
            float confidence,
            float zoomFactor,
            float cropContrast,
            long nowMs) {
        if (label == null || label.trim().isEmpty() || direction == null
                || direction == Direction.UNKNOWN || confidence < 0.60f || cropContrast < 0.10f) {
            prune(nowMs);
            return null;
        }
        String clean = label.trim().toLowerCase(java.util.Locale.forLanguageTag("tr-TR"));
        String key = clean + "|" + direction.name();
        State old = states.get(key);
        State next;
        if (old != null && nowMs >= old.lastSeenMs && nowMs - old.lastSeenMs <= TRACK_GAP_MS) {
            float ema = old.emaConfidence * 0.58f + confidence * 0.42f;
            next = new State(old.firstSeenMs, nowMs, Math.min(12, old.count + 1), ema, old.lastSpokenMs);
        } else {
            next = new State(nowMs, nowMs, 1, confidence, old == null ? 0L : old.lastSpokenMs);
        }
        states.put(key, next);
        prune(nowMs);

        long age = Math.max(0L, nowMs - next.firstSeenMs);
        float countScore = clamp01((next.count - 1f) / 2f);
        float timeScore = clamp01(age / 1300f);
        float persistence = Math.min(countScore, timeScore);
        boolean highConfidenceEarly = next.count >= 2 && next.emaConfidence >= 0.84f && age >= 650L;
        boolean normalMature = next.count >= 3 && next.emaConfidence >= 0.68f && age >= 1200L;
        if (!highConfidenceEarly && !normalMature) return null;
        if (next.lastSpokenMs > 0L && nowMs - next.lastSpokenMs < SPEECH_COOLDOWN_MS) return null;

        next.lastSpokenMs = nowMs;
        float fused = clamp01(next.emaConfidence * (0.78f + 0.22f * clamp01(cropContrast)));
        return new DistantObjectObservation(
                clean,
                direction,
                fused,
                Math.max(0.50f, persistence),
                Math.max(1f, zoomFactor),
                clamp01(cropContrast),
                nowMs);
    }

    public synchronized void reset() {
        states.clear();
    }

    private void prune(long nowMs) {
        Iterator<Map.Entry<String, State>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            State state = iterator.next().getValue();
            if (nowMs - state.lastSeenMs > TRACK_STALE_MS) iterator.remove();
        }
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static final class State {
        final long firstSeenMs;
        final long lastSeenMs;
        final int count;
        final float emaConfidence;
        long lastSpokenMs;

        State(long firstSeenMs, long lastSeenMs, int count, float emaConfidence, long lastSpokenMs) {
            this.firstSeenMs = firstSeenMs;
            this.lastSeenMs = lastSeenMs;
            this.count = count;
            this.emaConfidence = emaConfidence;
            this.lastSpokenMs = lastSpokenMs;
        }
    }
}
