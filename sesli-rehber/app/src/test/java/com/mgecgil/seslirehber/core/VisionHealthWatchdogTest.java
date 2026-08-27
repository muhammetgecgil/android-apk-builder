package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class VisionHealthWatchdogTest {
    @Test public void startupGraceDoesNotFalseStop() {
        VisionHealthWatchdog watchdog = new VisionHealthWatchdog();
        watchdog.beginMode(1000L, true);
        assertEquals(VisionHealthWatchdog.Health.STARTING, watchdog.snapshot(3500L).health());
    }

    @Test public void staleVisionRequestsFailover() {
        VisionHealthWatchdog watchdog = new VisionHealthWatchdog();
        watchdog.beginMode(0L, true);
        watchdog.noteVision(4000L);
        VisionHealthWatchdog.Snapshot snapshot = watchdog.snapshot(5901L);
        assertEquals(VisionHealthWatchdog.Health.VISION_STALE, snapshot.health());
        assertTrue(snapshot.failoverRecommended());
    }

    @Test public void depthStaleWarnsBeforeFailover() {
        VisionHealthWatchdog watchdog = new VisionHealthWatchdog();
        watchdog.beginMode(0L, true);
        watchdog.noteVision(5000L);
        watchdog.noteDepth(500L);
        VisionHealthWatchdog.Snapshot snapshot = watchdog.snapshot(5200L);
        assertEquals(VisionHealthWatchdog.Health.DEPTH_STALE, snapshot.health());
        assertFalse(snapshot.failoverRecommended());
    }

    @Test public void prolongedDepthLossRequestsFailover() {
        VisionHealthWatchdog watchdog = new VisionHealthWatchdog();
        watchdog.beginMode(0L, true);
        watchdog.noteVision(9000L);
        watchdog.noteDepth(500L);
        VisionHealthWatchdog.Snapshot snapshot = watchdog.snapshot(9100L);
        assertEquals(VisionHealthWatchdog.Health.DEPTH_STALE, snapshot.health());
        assertTrue(snapshot.failoverRecommended());
    }

    @Test public void healthyVisionAndDepthRemainHealthy() {
        VisionHealthWatchdog watchdog = new VisionHealthWatchdog();
        watchdog.beginMode(0L, true);
        watchdog.noteVision(10000L);
        watchdog.noteDepth(9900L);
        assertEquals(VisionHealthWatchdog.Health.HEALTHY, watchdog.snapshot(10100L).health());
    }
}
