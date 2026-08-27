package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Prevents stale cross-sensor fusion. Ground and depth evidence must describe effectively the same
 * instant before they may enter the dual-channel safety gate.
 */
public final class GroundDepthSynchronizer {
    private static final long MAX_SKEW_MS = 280L;
    private GroundObservation latestGround;
    private DepthObservation latestDepth;
    private long lastEmittedGroundTs = Long.MIN_VALUE;
    private long lastEmittedDepthTs = Long.MIN_VALUE;

    public synchronized GroundDepthEvidence offerGround(GroundObservation ground) {
        latestGround = ground;
        return tryMatch();
    }

    public synchronized GroundDepthEvidence offerDepth(DepthObservation depth) {
        latestDepth = depth;
        return tryMatch();
    }

    public synchronized void reset() {
        latestGround = null;
        latestDepth = null;
        lastEmittedGroundTs = Long.MIN_VALUE;
        lastEmittedDepthTs = Long.MIN_VALUE;
    }

    private GroundDepthEvidence tryMatch() {
        if (latestGround == null || latestDepth == null) return null;
        long skew = Math.abs(latestGround.timestampMs() - latestDepth.timestampMs());
        if (skew > MAX_SKEW_MS) return null;
        if (latestGround.timestampMs() == lastEmittedGroundTs
                && latestDepth.timestampMs() == lastEmittedDepthTs) return null;

        lastEmittedGroundTs = latestGround.timestampMs();
        lastEmittedDepthTs = latestDepth.timestampMs();
        return new GroundDepthEvidence(latestGround, latestDepth, skew);
    }
}
