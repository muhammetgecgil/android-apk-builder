package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class UrbanGateWizardTest {
    @Test public void doesNotAdvanceBeforeMinimumDwell() {
        UrbanGateWizard w = new UrbanGateWizard();
        w.start(1_000L);
        UrbanGateWizard.Decision d = w.tick(
                9_999L,
                UrbanValidationTelemetry.Scenario.SIDEWALK,
                20L,
                20L);
        assertEquals(UrbanGateWizard.Action.NONE, d.action());
    }

    @Test public void advancesWhenDwellAndEvidenceAreEnough() {
        UrbanGateWizard w = new UrbanGateWizard();
        w.start(0L);
        UrbanGateWizard.Decision d = w.tick(
                10_001L,
                UrbanValidationTelemetry.Scenario.SIDEWALK,
                7L,
                3L);
        assertEquals(UrbanGateWizard.Action.ADVANCE, d.action());
        assertFalse(w.isComplete());
    }

    @Test public void requestsOneRetryWhenEvidenceIsWeak() {
        UrbanGateWizard w = new UrbanGateWizard();
        w.start(0L);
        UrbanGateWizard.Decision first = w.tick(
                14_001L,
                UrbanValidationTelemetry.Scenario.TRAFFIC_CONTROL,
                8L,
                0L);
        UrbanGateWizard.Decision second = w.tick(
                15_000L,
                UrbanValidationTelemetry.Scenario.TRAFFIC_CONTROL,
                8L,
                0L);
        assertEquals(UrbanGateWizard.Action.RETRY_PROMPT, first.action());
        assertEquals(UrbanGateWizard.Action.NONE, second.action());
    }

    @Test public void weakScenarioCannotStallForever() {
        UrbanGateWizard w = new UrbanGateWizard();
        w.start(0L);
        w.tick(14_100L, UrbanValidationTelemetry.Scenario.POLE_FENCE, 2L, 0L);
        UrbanGateWizard.Decision d = w.tick(
                22_001L,
                UrbanValidationTelemetry.Scenario.POLE_FENCE,
                3L,
                0L);
        assertEquals(UrbanGateWizard.Action.ADVANCE, d.action());
    }

    @Test public void lowLightCompletesAfterEnoughDwellAndFrames() {
        UrbanGateWizard w = new UrbanGateWizard();
        w.start(500L);
        UrbanGateWizard.Decision d = w.tick(
                10_501L,
                UrbanValidationTelemetry.Scenario.LOW_LIGHT,
                6L,
                0L);
        assertEquals(UrbanGateWizard.Action.COMPLETE, d.action());
        assertTrue(w.isComplete());
        assertEquals(UrbanGateWizard.Action.NONE, w.tick(
                20_000L,
                UrbanValidationTelemetry.Scenario.LOW_LIGHT,
                20L,
                20L).action());
    }

    @Test public void newScenarioResetsDwellClock() {
        UrbanGateWizard w = new UrbanGateWizard();
        w.start(0L);
        assertEquals(UrbanGateWizard.Action.ADVANCE, w.tick(
                10_100L,
                UrbanValidationTelemetry.Scenario.ROAD_EDGE,
                7L,
                3L).action());
        w.beginScenario(10_100L);
        assertEquals(UrbanGateWizard.Action.NONE, w.tick(
                15_000L,
                UrbanValidationTelemetry.Scenario.BUILDING_WALL,
                20L,
                20L).action());
    }
}
