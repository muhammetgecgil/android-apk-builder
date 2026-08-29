package com.mgecgil.seslirehber.core;

import java.util.HashMap;
import java.util.Map;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/** Confidence/persistence gate for broad named-object detections. */
public final class WideObjectTracker {
    public record Result(WideObjectObservation observation, boolean announce) {}

    private static final float STRONG = 0.78f;
    private static final float REPEATED_DEFINITE = 0.66f;
    private static final float CANDIDATE = 0.54f;
    private static final long STREAK_WINDOW_MS = 1700L;
    private static final long SPEECH_COOLDOWN_MS = 9000L;
    private final Map<String, State> states = new HashMap<>();

    public Result observe(String label, float confidence, float left, float top, float right, float bottom, long nowMs) {
        if (label == null || label.isBlank() || confidence < CANDIDATE) return null;
        float cx = (left + right) * 0.5f;
        Direction direction = cx < 0.38f ? Direction.LEFT : cx > 0.62f ? Direction.RIGHT : Direction.CENTER;
        String key = label + ":" + direction;
        State old = states.get(key);
        int streak = old != null && nowMs - old.lastSeenMs <= STREAK_WINDOW_MS ? old.streak + 1 : 1;
        float smoothed = old == null ? confidence : old.smoothed * 0.42f + confidence * 0.58f;
        boolean important = WideObjectPolicy.important(label);
        boolean definite = confidence >= STRONG || (streak >= 2 && smoothed >= REPEATED_DEFINITE);
        if (important && streak >= 2 && smoothed >= 0.62f) definite = true;
        boolean usable = definite || (streak >= 2 && smoothed >= CANDIDATE);
        if (!usable) {
            states.put(key, new State(streak, smoothed, nowMs, old == null ? 0L : old.lastSpokenMs));
            return null;
        }
        long lastSpoken = old == null ? 0L : old.lastSpokenMs;
        boolean announce = lastSpoken == 0L || nowMs - lastSpoken >= SPEECH_COOLDOWN_MS;
        if (announce) lastSpoken = nowMs;
        states.put(key, new State(streak, smoothed, nowMs, lastSpoken));
        WideObjectObservation observation = new WideObjectObservation(
                label, smoothed, left, top, right, bottom, direction, definite, important, nowMs);
        return new Result(observation, announce);
    }

    public void reset() { states.clear(); }

    private record State(int streak, float smoothed, long lastSeenMs, long lastSpokenMs) {}
}
