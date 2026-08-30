package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;
import static org.junit.Assert.*;

public class ObjectSemanticTrackerTest {
    @Test public void veryHighConfidenceSingleScanCanBeDefinite() {
        ObjectSemanticTracker tracker = new ObjectSemanticTracker();
        ObjectSemanticTracker.Result result = tracker.observe(7, "koltuk", 0.91f, Direction.CENTER, 1000L);
        assertNotNull(result);
        assertTrue(result.observation().definite());
        assertTrue(result.announce());
    }

    @Test public void repeatedHighConfidenceBecomesDefinite() {
        ObjectSemanticTracker tracker = new ObjectSemanticTracker();
        assertNull(tracker.observe(7, "koltuk", 0.76f, Direction.CENTER, 1000L));
        ObjectSemanticTracker.Result result = tracker.observe(7, "koltuk", 0.78f, Direction.CENTER, 1800L);
        assertNotNull(result);
        assertTrue(result.observation().definite());
    }

    @Test public void repeatedModerateConfidenceRemainsCandidate() {
        ObjectSemanticTracker tracker = new ObjectSemanticTracker();
        assertNull(tracker.observe(3, "sandalye", 0.63f, Direction.LEFT, 1000L));
        ObjectSemanticTracker.Result result = tracker.observe(3, "sandalye", 0.65f, Direction.LEFT, 1800L);
        assertNotNull(result);
        assertFalse(result.observation().definite());
        assertEquals("sandalye", result.observation().label());
    }

    @Test public void oneWeakScanDoesNotCreateIdentity() {
        ObjectSemanticTracker tracker = new ObjectSemanticTracker();
        assertNull(tracker.observe(2, "masa", 0.60f, Direction.RIGHT, 1000L));
    }

    @Test public void repeatedAnnouncementIsRateLimitedButDefiniteUpgradeCanSpeak() {
        ObjectSemanticTracker tracker = new ObjectSemanticTracker();
        assertNull(tracker.observe(5, "koltuk", 0.62f, Direction.CENTER, 1000L));
        ObjectSemanticTracker.Result candidate = tracker.observe(5, "koltuk", 0.64f, Direction.CENTER, 1800L);
        assertNotNull(candidate);
        assertTrue(candidate.announce());
        ObjectSemanticTracker.Result definite = tracker.observe(5, "koltuk", 0.92f, Direction.CENTER, 2500L);
        assertNotNull(definite);
        assertTrue(definite.observation().definite());
        assertTrue(definite.announce());
        ObjectSemanticTracker.Result repeated = tracker.observe(5, "koltuk", 0.93f, Direction.CENTER, 3200L);
        assertNotNull(repeated);
        assertFalse(repeated.announce());
    }
}
