package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/** Controls speech density while ensuring a new STOP warning can preempt lower-priority speech. */
public final class AnnouncementGate {
    private long lastAnyMs = Long.MIN_VALUE / 4;
    private long lastStopMs = Long.MIN_VALUE / 4;
    private Risk lastRisk = Risk.INFO;
    private String lastSpeech = "";

    public synchronized boolean shouldAnnounce(GuidanceDecision decision, long nowMs) {
        if (decision == null || decision.speech() == null || decision.speech().isBlank()) return false;

        if (decision.risk() == Risk.STOP) {
            boolean changed = lastRisk != Risk.STOP || !decision.speech().equals(lastSpeech);
            if (changed || elapsed(nowMs, lastStopMs) >= 700L) {
                mark(decision, nowMs);
                lastStopMs = nowMs;
                return true;
            }
            return false;
        }

        // A recent STOP owns the audio channel briefly; lower-priority chatter cannot immediately
        // overwrite it.
        if (elapsed(nowMs, lastStopMs) < 1000L) return false;

        long cooldown = decision.risk() == Risk.CAUTION ? 1800L : 3000L;
        if (elapsed(nowMs, lastAnyMs) < cooldown) return false;

        mark(decision, nowMs);
        return true;
    }

    public synchronized void reset() {
        lastAnyMs = Long.MIN_VALUE / 4;
        lastStopMs = Long.MIN_VALUE / 4;
        lastRisk = Risk.INFO;
        lastSpeech = "";
    }

    private void mark(GuidanceDecision decision, long nowMs) {
        lastAnyMs = nowMs;
        lastRisk = decision.risk();
        lastSpeech = decision.speech();
    }

    private static long elapsed(long nowMs, long thenMs) {
        if (thenMs < 0 && nowMs >= 0) return Long.MAX_VALUE;
        return Math.max(0L, nowMs - thenMs);
    }
}
