package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SceneSummaryStateTest {
    @Test public void freshPersistentWalkableCandidateIsAdvisoryOnly() {
        long now = 10_000L;
        SceneSummaryState state = new SceneSummaryState();
        state.update(new WalkableCorridorObservation(
                0.82f, 0.30f, 0.40f, 0.68f,
                Direction.LEFT, 0.86f, 0.90f, now - 100L));
        String summary = state.summarize(now).toLowerCase();
        assertTrue(summary.contains("sol taraf"));
        assertTrue(summary.contains("güvenli yol onayı değildir"));
        assertFalse(summary.contains("sola dön"));
    }

    @Test public void staleHazardsAreNotNarratedAsCurrent() {
        long now = 20_000L;
        SceneSummaryState state = new SceneSummaryState();
        state.update(new GroundObservation(
                0.95f, 0.90f, 0.8f, 0.7f, 0.95f, 0.9f, 0.7f, now - 5000L));
        String summary = state.summarize(now).toLowerCase();
        assertFalse(summary.contains("zeminde süreklilik"));
    }

    @Test public void darkCameraIsExplainedWithoutSceneGuessing() {
        long now = 30_000L;
        SceneSummaryState state = new SceneSummaryState();
        state.update(new SceneHealthObservation(
                7f, 0.02f, 0.94f, 0.0f, 0.05f, 0.92f, 0.88f, now));
        String summary = state.summarize(now).toLowerCase();
        assertTrue(summary.contains("karanlık") || summary.contains("kapalı"));
        assertFalse(summary.contains("güvenli"));
    }
}
