package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;
import static org.junit.Assert.*;

public class SituationalAwarenessEngineTest {
    @Test public void threeDirectionsCanStayPresentAtTheSameTime() {
        long t = 10_000L;
        SituationalAwarenessEngine engine = new SituationalAwarenessEngine();
        engine.noteObject(object(0.20f, 0.060f, 0.00f, 0.00f, 1, t));
        engine.noteObject(object(0.50f, 0.200f, 0.00f, 0.00f, 2, t));
        engine.noteObject(object(0.82f, 0.020f, 0.00f, 0.00f, 3, t));

        SituationalAwarenessEngine.Snapshot s = engine.snapshot(t + 100L);
        assertTrue(s.left().midOccupancy() > 0.30f);
        assertTrue(s.center().nearOccupancy() > 0.40f);
        assertTrue(s.right().farOccupancy() > 0.25f);
        assertEquals(1, s.left().activeTracks());
        assertEquals(1, s.center().activeTracks());
        assertEquals(1, s.right().activeTracks());
        assertEquals(Direction.CENTER, s.attentionDirection());
    }

    @Test public void approachingCenterTrackRaisesAttention() {
        long t = 20_000L;
        SituationalAwarenessEngine engine = new SituationalAwarenessEngine();
        engine.noteObject(object(0.50f, 0.075f, 0.20f, 0.01f, 7, t));
        SituationalAwarenessEngine.Snapshot s = engine.snapshot(t + 80L);
        assertEquals(Direction.CENTER, s.attentionDirection());
        assertTrue(s.center().approachingScore() >= 0.70f);
        assertTrue(s.center().dynamicScore() >= 0.70f);
    }

    @Test public void matureFarSemanticEnrichesOnlyItsSector() {
        long t = 30_000L;
        SituationalAwarenessEngine engine = new SituationalAwarenessEngine();
        engine.noteDistant(new DistantObjectObservation(
                "araç", Direction.RIGHT, 0.84f, 0.78f, 3.2f, 0.40f, t));
        SituationalAwarenessEngine.Snapshot s = engine.snapshot(t + 200L);
        assertEquals("araç", s.right().farSemanticLabel());
        assertTrue(s.right().farOccupancy() > 0.30f);
        assertTrue(s.left().farSemanticLabel().isEmpty());
        assertTrue(s.center().farSemanticLabel().isEmpty());
    }

    @Test public void oldTracksAndFarEvidenceDecayAway() {
        long t = 40_000L;
        SituationalAwarenessEngine engine = new SituationalAwarenessEngine();
        engine.noteObject(object(0.50f, 0.18f, 0f, 0f, 9, t));
        engine.noteDistant(new DistantObjectObservation(
                "bisiklet", Direction.LEFT, 0.82f, 0.80f, 3f, 0.4f, t));
        SituationalAwarenessEngine.Snapshot stale = engine.snapshot(t + 8_500L);
        assertEquals(0, stale.center().activeTracks());
        assertEquals(0f, stale.center().nearOccupancy(), 0.001f);
        assertEquals(0f, stale.left().farOccupancy(), 0.001f);
        assertTrue(stale.left().farSemanticLabel().isEmpty());
    }

    @Test public void levelAndDistantEvidenceAppearInConservativeSummary() {
        long t = 50_000L;
        SituationalAwarenessEngine engine = new SituationalAwarenessEngine();
        engine.noteDistant(new DistantObjectObservation(
                "otobüs", Direction.LEFT, 0.86f, 0.82f, 3.0f, 0.42f, t));
        engine.noteLevelChange(new LevelChangeObservation(
                LevelChangeKind.DOWNWARD_CANDIDATE,
                0.82f, 0.76f, 0.70f, 0.20f, 0.84f, 0.80f, 0.68f, t));
        String text = engine.summarize(t + 100L).toLowerCase();
        assertTrue(text.contains("otobüs"));
        assertTrue(text.contains("aşağı yönlü"));
        assertTrue(text.contains("adayı"));
        assertFalse(text.contains("metre"));
        assertFalse(text.contains("çukur var"));
        assertFalse(text.contains("merdiven var"));
    }

    @Test public void relativeRangeIsImageRelativeNotMetric() {
        assertEquals(SituationalAwarenessEngine.RangeBand.NEAR,
                SituationalAwarenessEngine.relativeRange(0.18f));
        assertEquals(SituationalAwarenessEngine.RangeBand.MID,
                SituationalAwarenessEngine.relativeRange(0.06f));
        assertEquals(SituationalAwarenessEngine.RangeBand.FAR,
                SituationalAwarenessEngine.relativeRange(0.01f));
    }

    private static ObjectObservation object(
            float x, float area, float growth, float velocityX, int id, long t) {
        return new ObjectObservation(
                x, 0.55f, 0.78f, area, growth, velocityX, id, 0.86f, t);
    }
}
