package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.mgecgil.seslirehber.core.GuidanceModels.DepthObservation;

public class DepthGeometryEstimatorTest {
    @Test public void smoothPerspectiveGroundStaysLowRisk() {
        int w = 72, h = 72;
        short[] depth = new short[w * h];
        for (int y = 0; y < h; y++) {
            int mm = 1700 + (h - 1 - y) * 18;
            for (int x = 0; x < w; x++) depth[y * w + x] = (short) mm;
        }
        DepthObservation o = new DepthGeometryEstimator().analyze(depth, w, h, 1000L);
        assertTrue(o.validRatio() > 0.80f);
        assertTrue(o.discontinuityScore() < 0.25f);
        assertFalse(o.strongDiscontinuity());
    }

    @Test public void abruptCentralDepthBreakCreatesStrongEvidence() {
        int w = 72, h = 72;
        short[] depth = new short[w * h];
        for (int y = 0; y < h; y++) {
            int mm = y < 46 ? 5200 : 1700;
            for (int x = 0; x < w; x++) depth[y * w + x] = (short) mm;
        }
        DepthObservation o = new DepthGeometryEstimator().analyze(depth, w, h, 1000L);
        assertTrue(o.validRatio() > 0.80f);
        assertTrue(o.maxBandJumpMm() > 2000f);
        assertTrue(o.discontinuityScore() > 0.62f);
        assertTrue(o.strongDiscontinuity());
    }

    @Test public void sparseDepthCannotBecomeStrongEvidence() {
        int w = 72, h = 72;
        short[] depth = new short[w * h];
        for (int y = 0; y < h; y += 8) {
            for (int x = 0; x < w; x += 8) depth[y * w + x] = (short) 2500;
        }
        DepthObservation o = new DepthGeometryEstimator().analyze(depth, w, h, 1000L);
        assertTrue(o.validRatio() < 0.20f);
        assertFalse(o.strongDiscontinuity());
    }
}
