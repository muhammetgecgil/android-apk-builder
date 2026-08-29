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
        WideObjectTracker.Result r = t.observe("bardak", 0.70f, 0.45f, 0.3f, 0.60f, 0.70f, 1450L);
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
}
