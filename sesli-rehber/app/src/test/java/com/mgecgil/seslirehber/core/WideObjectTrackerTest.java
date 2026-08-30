package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class WideObjectTrackerTest {
    @Test public void strongSingleDetectionCanBeDefinite() {
        WideObjectTracker t = new WideObjectTracker();
        WideObjectTracker.Result r = t.observe("televizyon", 0.86f, 0.2f, 0.2f, 0.7f, 0.8f, 1000L);
        assertNotNull(r);
        assertTrue(r.observation().definite());
        assertTrue(r.announce());
        assertEquals("Önde televizyon var.", WideObjectDetectorEngine.speech(r.observation()));
    }

    @Test public void repeatedMediumDetectionBecomesDefinite() {
        WideObjectTracker t = new WideObjectTracker();
        assertNull(t.observe("bardak", 0.68f, 0.45f, 0.3f, 0.60f, 0.70f, 1000L));
        WideObjectTracker.Result r = t.observe("bardak", 0.70f, 0.46f, 0.30f, 0.61f, 0.70f, 1450L);
        assertNotNull(r);
        assertTrue(r.observation().definite());
    }

    @Test public void weakSingleDetectionStaysQuiet() {
        WideObjectTracker t = new WideObjectTracker();
        assertNull(t.observe("kalem", 0.57f, 0.1f, 0.2f, 0.2f, 0.4f, 1000L));
    }

    @Test public void candidateLanguageNeverClaimsCertainty() {
        WideObjectTracker t = new WideObjectTracker();
        assertNull(t.observe("saksı", 0.57f, 0.70f, 0.2f, 0.90f, 0.7f, 1000L));
        WideObjectTracker.Result r = t.observe("saksı", 0.58f, 0.70f, 0.2f, 0.90f, 0.7f, 1400L);
        assertNotNull(r);
        assertFalse(r.observation().definite());
        String speech = WideObjectDetectorEngine.speech(r.observation());
        assertTrue(speech.contains("olabilir"));
        assertFalse(speech.contains(" var."));
    }

    @Test public void sameLabelInDifferentImageRegionDoesNotAccumulate() {
        WideObjectTracker t = new WideObjectTracker();
        assertNull(t.observe("araç", 0.70f, 0.02f, 0.20f, 0.16f, 0.48f, 1000L));
        assertNull(t.observe("araç", 0.71f, 0.22f, 0.20f, 0.36f, 0.48f, 1400L));
    }

    @Test public void oneContradictoryHighConfidenceLabelCannotInstantlyTakeOverTrack() {
        WideObjectTracker t = new WideObjectTracker();
        assertNull(t.observe("koltuk", 0.74f, 0.18f, 0.40f, 0.82f, 0.82f, 1000L));
        WideObjectTracker.Result couch = t.observe("koltuk", 0.76f, 0.19f, 0.40f, 0.83f, 0.82f, 1400L);
        assertNotNull(couch);
        assertTrue(couch.observation().definite());
        assertNull(t.observe("sandalye", 0.94f, 0.19f, 0.40f, 0.83f, 0.82f, 1800L));
    }

    @Test public void importantTrafficObjectNeedsStrongerEvidence() {
        WideObjectTracker t = new WideObjectTracker();
        assertNull(t.observe("araç", 0.80f, 0.35f, 0.30f, 0.65f, 0.72f, 1000L));
        WideObjectTracker.Result candidate = t.observe(
                "araç", 0.66f, 0.36f, 0.30f, 0.66f, 0.72f, 1400L);
        assertNotNull(candidate);
        assertFalse(candidate.observation().definite());
        assertTrue(WideObjectDetectorEngine.speech(candidate.observation()).contains("olabilir"));

        WideObjectTracker.Result r = t.observe("araç", 0.80f, 0.36f, 0.30f, 0.66f, 0.72f, 1750L);
        assertNotNull(r);
        assertTrue(r.observation().definite());
        assertTrue(WideObjectDetectorEngine.speech(r.observation()).contains(" var."));
    }
}
