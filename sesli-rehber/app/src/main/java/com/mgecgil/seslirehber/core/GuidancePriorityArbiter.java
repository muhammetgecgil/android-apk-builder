package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Cross-channel speech priority contract for the future route engine.
 * Safety speech wins over navigation/scene speech; route prompts are temporarily suppressed after
 * a hazard so a turn instruction cannot mask an obstacle warning.
 */
public final class GuidancePriorityArbiter {
    public enum Channel { SAFETY, NAVIGATION, SCENE, SYSTEM }

    private static final long STOP_NAV_HOLD_MS = 3000L;
    private static final long CAUTION_NAV_HOLD_MS = 1800L;
    private static final long SAFETY_SCENE_HOLD_MS = 2500L;

    private long lastStopMs = Long.MIN_VALUE;
    private long lastCautionMs = Long.MIN_VALUE;

    public synchronized boolean shouldDeliver(
            Channel channel,
            GuidanceDecision decision,
            long nowMs) {
        if (decision == null || decision.speech() == null || decision.speech().isBlank()) return false;

        if (channel == Channel.SAFETY) {
            if (decision.risk() == Risk.STOP) lastStopMs = nowMs;
            else if (decision.risk() == Risk.CAUTION) lastCautionMs = nowMs;
            return true;
        }

        if (channel == Channel.SYSTEM && decision.risk() == Risk.STOP) return true;

        long sinceStop = age(nowMs, lastStopMs);
        long sinceCaution = age(nowMs, lastCautionMs);
        if (channel == Channel.NAVIGATION) {
            return sinceStop >= STOP_NAV_HOLD_MS && sinceCaution >= CAUTION_NAV_HOLD_MS;
        }
        if (channel == Channel.SCENE) {
            return sinceStop >= SAFETY_SCENE_HOLD_MS && sinceCaution >= SAFETY_SCENE_HOLD_MS;
        }
        return true;
    }

    public synchronized void reset() {
        lastStopMs = Long.MIN_VALUE;
        lastCautionMs = Long.MIN_VALUE;
    }

    private static long age(long nowMs, long thenMs) {
        if (thenMs == Long.MIN_VALUE) return Long.MAX_VALUE;
        return Math.max(0L, nowMs - thenMs);
    }
}
