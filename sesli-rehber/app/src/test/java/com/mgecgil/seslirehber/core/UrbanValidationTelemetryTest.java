package com.mgecgil.seslirehber.core;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public final class UrbanValidationTelemetryTest {
    @Before public void reset() {
        UrbanValidationTelemetry.setScenario(UrbanValidationTelemetry.Scenario.SIDEWALK);
        UrbanValidationTelemetry.resetSessionCounters();
    }

    @Test public void sidewalkScenarioCountsOnlyMatchingEvidence() {
        UrbanValidationTelemetry.noteSuccess(obs(120, 0.12f, 0.01f, 0.01f, 0.01f, 0.01f, 1000L));
        UrbanValidationTelemetry.noteSuccess(obs(130, 0.02f, 0.20f, 0.01f, 0.01f, 0.01f, 1100L));
        UrbanValidationTelemetry.Snapshot s = UrbanValidationTelemetry.snapshot();
        assertEquals(2L, s.scenarioFrames());
        assertEquals(1L, s.scenarioEvidenceFrames());
        assertEquals(0.5f, s.scenarioEvidenceRate(), 0.0001f);
    }

    @Test public void scenarioChangeResetsScenarioWindowButNotSessionCount() {
        UrbanValidationTelemetry.noteSuccess(obs(100, 0.20f, 0f, 0f, 0f, 0f, 1000L));
        UrbanValidationTelemetry.setScenario(UrbanValidationTelemetry.Scenario.BUILDING_WALL);
        UrbanValidationTelemetry.noteSuccess(obs(110, 0f, 0f, 0.25f, 0f, 0f, 1100L));
        UrbanValidationTelemetry.Snapshot s = UrbanValidationTelemetry.snapshot();
        assertEquals(2L, s.successfulInferences());
        assertEquals(1L, s.scenarioFrames());
        assertEquals(1L, s.scenarioEvidenceFrames());
        assertEquals(UrbanValidationTelemetry.Scenario.BUILDING_WALL, s.scenario());
    }

    @Test public void p95CapturesUpperTail() {
        for (int i = 1; i <= 20; i++) {
            UrbanValidationTelemetry.noteSuccess(obs(i * 10L, 0.1f, 0f, 0f, 0f, 0f, 1000L + i));
        }
        UrbanValidationTelemetry.Snapshot s = UrbanValidationTelemetry.snapshot();
        assertEquals(190L, s.p95InferenceMs());
        assertEquals(200L, s.lastInferenceMs());
    }

    @Test public void trafficControlNeedsSmallButRealPixelEvidence() {
        UrbanValidationTelemetry.setScenario(UrbanValidationTelemetry.Scenario.TRAFFIC_CONTROL);
        assertFalse(UrbanValidationTelemetry.Scenario.TRAFFIC_CONTROL.evidenceMatches(
                obs(100, 0f, 0f, 0f, 0f, 0.003f, 1000L)));
        assertTrue(UrbanValidationTelemetry.Scenario.TRAFFIC_CONTROL.evidenceMatches(
                obs(100, 0f, 0f, 0f, 0f, 0.006f, 1001L)));
    }

    private static UrbanSegmentationObservation obs(
            long inferenceMs,
            float sidewalk,
            float road,
            float buildingWall,
            float fencePole,
            float traffic,
            long timestampMs) {
        return new UrbanSegmentationObservation(
                road, sidewalk, buildingWall, fencePole, traffic,
                0.05f, 0.02f, 0.01f, 0.01f, 0.01f, 0.20f,
                0.01f, 0.02f, 0.03f,
                road, sidewalk, 0.01f,
                0.75f, inferenceMs, timestampMs);
    }
}
