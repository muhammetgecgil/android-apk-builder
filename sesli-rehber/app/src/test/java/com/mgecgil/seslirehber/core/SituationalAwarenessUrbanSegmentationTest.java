package com.mgecgil.seslirehber.core;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SituationalAwarenessUrbanSegmentationTest {
    @After public void clean() { SituationalAwarenessContext.reset(); }

    @Test public void matureUrbanEvidenceAppearsWithoutObjectTracker() {
        long now = 10_000L;
        SituationalAwarenessContext.noteUrbanSegmentation(new UrbanSegmentationObservation(
                0.32f, 0.18f, 0.15f, 0.03f, 0.006f, 0.10f, 0.03f,
                0.02f, 0.04f, 0.01f, 0.17f,
                0.08f, 0.20f, 0.06f,
                0.34f, 0.31f, 0.12f,
                0.80f, 70L, now));
        SceneSummaryState scene = new SceneSummaryState();
        String text = scene.summarize(now).toLowerCase();
        assertTrue(text.contains("kaldırım"));
        assertTrue(text.contains("yol yüzeyi"));
        assertTrue(text.contains("bina") || text.contains("duvar"));
        assertTrue(text.contains("güvenli yol onayı değildir") || text.contains("karşıya geçiş güvenliği onayı değildir"));
    }

    @Test public void staleUrbanEvidenceDoesNotRemainCurrent() {
        long now = 20_000L;
        SituationalAwarenessContext.noteUrbanSegmentation(new UrbanSegmentationObservation(
                0.35f, 0.22f, 0.12f, 0.03f, 0.01f, 0.08f, 0.02f,
                0.01f, 0.03f, 0.01f, 0.12f,
                0.10f, 0.18f, 0.08f,
                0.35f, 0.30f, 0.10f,
                0.90f, 80L, now - 4000L));
        String text = new SceneSummaryState().summarize(now).toLowerCase();
        assertFalse(text.contains("şehir segmentasyonunda kaldırım"));
        assertFalse(text.contains("ön alt merkezde yol yüzeyi"));
    }

    @Test public void urbanNarrationNeverClaimsSafeCrossingOrDefiniteHazard() {
        long now = 30_000L;
        SituationalAwarenessContext.noteUrbanSegmentation(new UrbanSegmentationObservation(
                0.40f, 0.25f, 0.10f, 0.04f, 0.02f, 0.05f, 0.02f,
                0.01f, 0.05f, 0.01f, 0.05f,
                0.10f, 0.20f, 0.08f,
                0.50f, 0.35f, 0.12f,
                0.90f, 75L, now));
        String text = SituationalAwarenessContext.summarize(now).toLowerCase();
        assertFalse(text.contains("karşıya geç"));
        assertFalse(text.contains("güvenle"));
        assertFalse(text.contains("çukur var"));
        assertFalse(text.contains("merdiven var"));
        assertFalse(text.contains("kaldırım güvenli"));
    }
}
