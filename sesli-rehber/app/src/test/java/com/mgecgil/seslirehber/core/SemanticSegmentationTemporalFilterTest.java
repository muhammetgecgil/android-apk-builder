package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SemanticSegmentationTemporalFilterTest {
    private static SemanticSegmentationMaskAnalyzer.Raw raw(float foreground) {
        return new SemanticSegmentationMaskAnalyzer.Raw(
                foreground, 0f, 0f, 0f, 0f, 0f, foreground,
                foreground, foreground, 0f,
                foreground, 0f, 0f, 0f);
    }

    @Test public void oneFrameCannotBecomeMature() {
        SemanticSegmentationTemporalFilter f = new SemanticSegmentationTemporalFilter();
        SemanticSegmentationObservation s = f.update(raw(0.15f), 90L, 1000L);
        assertFalse(s.mature());
    }

    @Test public void threeConsistentFramesBecomeMature() {
        SemanticSegmentationTemporalFilter f = new SemanticSegmentationTemporalFilter();
        f.update(raw(0.15f), 80L, 1000L);
        f.update(raw(0.15f), 82L, 1300L);
        SemanticSegmentationObservation s = f.update(raw(0.15f), 79L, 1600L);
        assertTrue(s.temporalStability() >= 0.48f);
        assertTrue(s.mature());
    }

    @Test public void staleGapResetsPersistence() {
        SemanticSegmentationTemporalFilter f = new SemanticSegmentationTemporalFilter();
        f.update(raw(0.15f), 80L, 1000L);
        f.update(raw(0.15f), 80L, 1300L);
        f.update(raw(0.15f), 80L, 1600L);
        SemanticSegmentationObservation reset = f.update(raw(0.15f), 80L, 4000L);
        assertFalse(reset.mature());
        assertTrue(reset.temporalStability() < 0.1f);
    }
}
