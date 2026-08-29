package com.mgecgil.seslirehber.core;

import java.util.HashMap;
import java.util.Map;

/** Visual/advisory semantic cache keyed by the detector tracking id. No SafetyGate authority. */
public final class ObjectSemanticContext {
    private static final long FRESH_MS = 4200L;
    private static final Map<Integer, ObjectSemanticObservation> VALUES = new HashMap<>();

    private ObjectSemanticContext() {}

    public static synchronized void note(ObjectSemanticObservation observation) {
        if (observation == null || !observation.usable()) return;
        VALUES.put(observation.trackingId(), observation);
    }

    public static synchronized ObjectSemanticObservation forTrackingId(int trackingId, long nowMs) {
        ObjectSemanticObservation value = VALUES.get(trackingId);
        if (value == null) return null;
        if (!fresh(value.timestampMs(), nowMs)) {
            VALUES.remove(trackingId);
            return null;
        }
        return value;
    }

    public static synchronized void prune(long nowMs) {
        VALUES.entrySet().removeIf(e -> !fresh(e.getValue().timestampMs(), nowMs));
    }

    public static synchronized void reset() { VALUES.clear(); }

    private static boolean fresh(long timestampMs, long nowMs) {
        return timestampMs > 0L && nowMs >= timestampMs && nowMs - timestampMs <= FRESH_MS;
    }
}
