package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/**
 * Advisory semantic evidence from the throttled multi-scale far-vision path.
 * It must never be used as metric distance or as a STOP source by itself.
 */
public record DistantObjectObservation(
        String label,
        Direction direction,
        float labelConfidence,
        float persistenceScore,
        float zoomFactor,
        float cropContrast,
        long timestampMs) {
    public boolean mature() {
        return label != null
                && !label.trim().isEmpty()
                && direction != Direction.UNKNOWN
                && labelConfidence >= 0.68f
                && persistenceScore >= 0.50f;
    }
}
