package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class AnnouncementGateTest {
    @Test public void stopPreemptsRecentCaution() {
        AnnouncementGate gate = new AnnouncementGate();
        GuidanceDecision caution = new GuidanceDecision(Risk.CAUTION, Direction.LEFT, "Solda engel.", 0.7f);
        GuidanceDecision stop = new GuidanceDecision(Risk.STOP, Direction.CENTER, "Dur. Önünde yaklaşan engel.", 0.8f);
        assertTrue(gate.shouldAnnounce(caution, 1000L));
        assertTrue(gate.shouldAnnounce(stop, 1100L));
    }

    @Test public void cautionCannotOverwriteFreshStop() {
        AnnouncementGate gate = new AnnouncementGate();
        GuidanceDecision stop = new GuidanceDecision(Risk.STOP, Direction.CENTER, "Dur. Önünde yaklaşan engel.", 0.8f);
        GuidanceDecision caution = new GuidanceDecision(Risk.CAUTION, Direction.RIGHT, "Sağda engel.", 0.7f);
        assertTrue(gate.shouldAnnounce(stop, 1000L));
        assertFalse(gate.shouldAnnounce(caution, 1500L));
    }
}
