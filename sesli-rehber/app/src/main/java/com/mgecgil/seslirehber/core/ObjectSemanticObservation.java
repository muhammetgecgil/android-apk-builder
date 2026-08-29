package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/** Advisory semantic identity attached to an already-tracked geometric object. */
public record ObjectSemanticObservation(
        int trackingId,
        String label,
        float confidence,
        boolean definite,
        Direction direction,
        int persistenceCount,
        long timestampMs) {

    public boolean usable() {
        return trackingId >= 0
                && label != null
                && !label.isBlank()
                && confidence >= 0.58f
                && direction != null;
    }
}
