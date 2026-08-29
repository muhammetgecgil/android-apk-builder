package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UrbanSegmentationTemporalFilterTest {
    private static UrbanSegmentationLogitAnalyzer.Raw raw(float sidewalk, float road, float obstacle) {
        return new UrbanSegmentationLogitAnalyzer.Raw(
                road, sidewalk, 0.10f, 0.02f, 0.005f, 0.08f, 0.02f,
                0.01f, 0.02f, 0.005f, 0.20f,
                obstacle * 0.7f, obstacle, obstacle * 0.4f,
                road, sidewalk, obstacle);
    }

    @Test public void oneFrameCannotBecomeMature() {
        UrbanSegmentationTemporalFilter f = new UrbanSegmentationTemporalFilter();
        UrbanSegmentationObservation s = f.update(raw(0.20f, 0.30f, 0.15f), 90L, 1000L);
        assertFalse(s.mature());
    }

    @Test public void threeConsistentFramesBecomeMature() {
        UrbanSegmentationTemporalFilter f = new UrbanSegmentationTemporalFilter();
        f.update(raw(0.20f, 0.30f, 0.15f), 80L, 1000L);
        f.update(raw(0.21f, 0.29f, 0.16f), 82L, 1500L);
        UrbanSegmentationObservation s = f.update(raw(0.19f, 0.31f, 0.15f), 79L, 2000L);
        assertTrue(s.temporalStability() >= 0.48f);
        assertTrue(s.mature());
        assertTrue(s.lowerCenterSidewalkRatio() > 0.15f);
    }

    @Test public void staleGapResetsPersistence() {
        UrbanSegmentationTemporalFilter f = new UrbanSegmentationTemporalFilter();
        f.update(raw(0.20f, 0.30f, 0.15f), 80L, 1000L);
        f.update(raw(0.20f, 0.30f, 0.15f), 80L, 1500L);
        f.update(raw(0.20f, 0.30f, 0.15f), 80L, 2000L);
        UrbanSegmentationObservation reset = f.update(raw(0.20f, 0.30f, 0.15f), 80L, 5000L);
        assertFalse(reset.mature());
        assertTrue(reset.temporalStability() < 0.1f);
    }
}
