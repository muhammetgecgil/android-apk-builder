package com.mgecgil.seslirehber.core;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class GateCMetricsTest {
    @Test public void percentile95UsesUpperTail() {
        long p95 = GateCMetrics.percentile95(Arrays.asList(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L));
        assertEquals(100L, p95);
    }

    @Test public void snapshotAccumulatesDepthAndDecisions() {
        GateCMetrics metrics = new GateCMetrics();
        metrics.reset(1000L);
        metrics.noteVision();
        metrics.noteDepth(0.5f);
        metrics.noteDepth(0.9f);
        metrics.noteDecision("CAUTION", 40L);
        metrics.noteDecision("STOP", 120L);
        metrics.noteFallback();
        GateCMetrics.Snapshot snapshot = metrics.snapshot(5000L);
        assertEquals(4000L, snapshot.durationMs());
        assertEquals(1, snapshot.visionFrames());
        assertEquals(2, snapshot.depthFrames());
        assertEquals(1, snapshot.cautionDecisions());
        assertEquals(1, snapshot.stopDecisions());
        assertEquals(1, snapshot.fallbacks());
        assertEquals(0.7f, snapshot.meanDepthValidRatio(), 0.001f);
        assertEquals(120L, snapshot.p95DecisionLatencyMs());
    }
}
