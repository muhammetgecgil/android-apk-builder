package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

public class DistantObjectTrackerTest {
    @Test public void oneFarRecognitionNeverSpeaks() {
        DistantObjectTracker tracker = new DistantObjectTracker();
        assertNull(tracker.observe("araç", Direction.CENTER, 0.91f, 2.4f, 0.40f, 1000L));
    }

    @Test public void repeatedHighConfidenceCandidateMaturesEarly() {
        DistantObjectTracker tracker = new DistantObjectTracker();
        assertNull(tracker.observe("araç", Direction.CENTER, 0.90f, 2.4f, 0.45f, 1000L));
        DistantObjectObservation o = tracker.observe("araç", Direction.CENTER, 0.92f, 2.4f, 0.47f, 1750L);
        assertNotNull(o);
        assertTrue(o.mature());
        assertEquals("araç", o.label());
        assertEquals(Direction.CENTER, o.direction());
    }

    @Test public void mediumConfidenceNeedsThreeObservations() {
        DistantObjectTracker tracker = new DistantObjectTracker();
        assertNull(tracker.observe("insan", Direction.LEFT, 0.73f, 2.1f, 0.36f, 1000L));
        assertNull(tracker.observe("insan", Direction.LEFT, 0.74f, 2.1f, 0.38f, 1800L));
        DistantObjectObservation o = tracker.observe("insan", Direction.LEFT, 0.75f, 2.1f, 0.40f, 2600L);
        assertNotNull(o);
        assertTrue(o.mature());
    }

    @Test public void sameCandidateIsSuppressedDuringCooldown() {
        DistantObjectTracker tracker = new DistantObjectTracker();
        tracker.observe("otobüs", Direction.RIGHT, 0.90f, 2.0f, 0.42f, 1000L);
        assertNotNull(tracker.observe("otobüs", Direction.RIGHT, 0.92f, 2.0f, 0.44f, 1750L));
        assertNull(tracker.observe("otobüs", Direction.RIGHT, 0.93f, 2.0f, 0.44f, 2600L));
        assertNull(tracker.observe("otobüs", Direction.RIGHT, 0.94f, 2.0f, 0.44f, 3400L));
    }

    @Test public void lowContrastTileCannotMature() {
        DistantObjectTracker tracker = new DistantObjectTracker();
        assertNull(tracker.observe("araç", Direction.CENTER, 0.95f, 3f, 0.05f, 1000L));
        assertNull(tracker.observe("araç", Direction.CENTER, 0.95f, 3f, 0.05f, 1800L));
        assertNull(tracker.observe("araç", Direction.CENTER, 0.95f, 3f, 0.05f, 2600L));
    }
}
