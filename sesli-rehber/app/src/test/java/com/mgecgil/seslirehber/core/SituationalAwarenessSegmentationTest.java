package com.mgecgil.seslirehber.core;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SituationalAwarenessSegmentationTest {
    @After public void clean() { SituationalAwarenessContext.reset(); }

    @Test public void freshMatureSegmentationAppearsWithoutObjectTrackerEvidence() {
        long now = 10_000L;
        SituationalAwarenessContext.noteSegmentation(new SemanticSegmentationObservation(
                0.08f, 0.04f, 0f, 0f, 0f, 0f, 0.14f,
                0.02f, 0.30f, 0.02f,
                0.02f, 0.06f, 0.30f, 0.35f,
                0.80f, 95L, now));
        SceneSummaryState scene = new SceneSummaryState();
        String text = scene.summarize(now).toLowerCase();
        assertTrue(text.contains("segmentasyon"));
        assertTrue(text.contains("insan") || text.contains("araç"));
        assertTrue(text.contains("güvenli yol onayı değildir"));
    }

    @Test public void staleSegmentationIsNotNarratedAsCurrent() {
        long now = 20_000L;
        SituationalAwarenessContext.noteSegmentation(new SemanticSegmentationObservation(
                0.10f, 0f, 0f, 0f, 0f, 0f, 0.10f,
                0.3f, 0.3f, 0.3f,
                0.2f, 0.2f, 0.2f, 0.2f,
                0.90f, 90L, now - 3000L));
        SceneSummaryState scene = new SceneSummaryState();
        String text = scene.summarize(now).toLowerCase();
        assertFalse(text.contains("segmentasyonda insan"));
    }

    @Test public void segmentationSummaryNeverInventsRoadCurbHoleOrSafeCrossing() {
        long now = 30_000L;
        SituationalAwarenessContext.noteSegmentation(new SemanticSegmentationObservation(
                0.03f, 0.06f, 0.02f, 0f, 0.04f, 0f, 0.15f,
                0.25f, 0.40f, 0.12f,
                0.08f, 0.20f, 0.35f, 0.40f,
                0.85f, 100L, now));
        String text = SituationalAwarenessContext.summarize(now).toLowerCase();
        assertFalse(text.contains("kaldırım var"));
        assertFalse(text.contains("çukur var"));
        assertFalse(text.contains("merdiven var"));
        assertFalse(text.contains("karşıya geçmek güvenli"));
        assertFalse(text.contains("yol güvenli"));
    }
}
