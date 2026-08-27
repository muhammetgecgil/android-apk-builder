package com.mgecgil.seslirehber.core;

/**
 * Runtime fail-safe for camera/depth freshness. It is deliberately independent from Android so the
 * timing policy can be unit tested. A stale vision stream is never interpreted as a safe scene.
 */
public final class VisionHealthWatchdog {
    public enum Health { STARTING, HEALTHY, DEPTH_STALE, VISION_STALE }

    public record Snapshot(
            Health health,
            long visionAgeMs,
            long depthAgeMs,
            boolean depthExpected,
            boolean failoverRecommended) {}

    static final long STARTUP_GRACE_MS = 3200L;
    static final long VISION_STALE_MS = 1800L;
    static final long DEPTH_WARN_MS = 4500L;
    static final long DEPTH_FAILOVER_MS = 8000L;

    private long modeStartedMs;
    private long lastVisionMs;
    private long lastDepthMs;
    private boolean depthExpected;

    public synchronized void beginMode(long nowMs, boolean expectsDepth) {
        modeStartedMs = nowMs;
        lastVisionMs = 0L;
        lastDepthMs = 0L;
        depthExpected = expectsDepth;
    }

    public synchronized void noteVision(long nowMs) {
        lastVisionMs = nowMs;
    }

    public synchronized void noteDepth(long nowMs) {
        lastDepthMs = nowMs;
    }

    public synchronized Snapshot snapshot(long nowMs) {
        long sinceStart = Math.max(0L, nowMs - modeStartedMs);
        long visionAge = lastVisionMs > 0L ? Math.max(0L, nowMs - lastVisionMs) : sinceStart;
        long depthAge = lastDepthMs > 0L ? Math.max(0L, nowMs - lastDepthMs) : sinceStart;

        if (sinceStart <= STARTUP_GRACE_MS && lastVisionMs == 0L) {
            return new Snapshot(Health.STARTING, visionAge, depthAge, depthExpected, false);
        }
        if (lastVisionMs == 0L || visionAge > VISION_STALE_MS) {
            return new Snapshot(Health.VISION_STALE, visionAge, depthAge, depthExpected, true);
        }
        if (depthExpected && depthAge > DEPTH_WARN_MS) {
            return new Snapshot(
                    Health.DEPTH_STALE,
                    visionAge,
                    depthAge,
                    true,
                    depthAge > DEPTH_FAILOVER_MS);
        }
        return new Snapshot(Health.HEALTHY, visionAge, depthAge, depthExpected, false);
    }
}
