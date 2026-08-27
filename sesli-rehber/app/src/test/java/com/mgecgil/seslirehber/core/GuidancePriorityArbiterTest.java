package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class GuidancePriorityArbiterTest {
    @Test
    public void stopSuppressesNavigationForHoldWindow() {
        GuidancePriorityArbiter arbiter = new GuidancePriorityArbiter();
        GuidanceDecision stop = new GuidanceDecision(Risk.STOP, Direction.CENTER,
                "Dur. Önünde engel.", 0.9f);
        GuidanceDecision nav = new GuidanceDecision(Risk.INFO, Direction.RIGHT,
                "Sağa dön.", 0.9f);

        assertTrue(arbiter.shouldDeliver(GuidancePriorityArbiter.Channel.SAFETY, stop, 1000L));
        assertFalse(arbiter.shouldDeliver(GuidancePriorityArbiter.Channel.NAVIGATION, nav, 2500L));
        assertTrue(arbiter.shouldDeliver(GuidancePriorityArbiter.Channel.NAVIGATION, nav, 4100L));
    }

    @Test
    public void cautionAlsoTemporarilySuppressesNavigation() {
        GuidancePriorityArbiter arbiter = new GuidancePriorityArbiter();
        GuidanceDecision caution = new GuidanceDecision(Risk.CAUTION, Direction.LEFT,
                "Solda engel.", 0.8f);
        GuidanceDecision nav = new GuidanceDecision(Risk.INFO, Direction.RIGHT,
                "Sağa dön.", 0.9f);

        assertTrue(arbiter.shouldDeliver(GuidancePriorityArbiter.Channel.SAFETY, caution, 5000L));
        assertFalse(arbiter.shouldDeliver(GuidancePriorityArbiter.Channel.NAVIGATION, nav, 6200L));
        assertTrue(arbiter.shouldDeliver(GuidancePriorityArbiter.Channel.NAVIGATION, nav, 6900L));
    }

    @Test
    public void blankSpeechIsNeverDelivered() {
        GuidancePriorityArbiter arbiter = new GuidancePriorityArbiter();
        GuidanceDecision quiet = new GuidanceDecision(Risk.INFO, Direction.UNKNOWN, "", 0.4f);
        assertFalse(arbiter.shouldDeliver(GuidancePriorityArbiter.Channel.SAFETY, quiet, 100L));
    }
}
