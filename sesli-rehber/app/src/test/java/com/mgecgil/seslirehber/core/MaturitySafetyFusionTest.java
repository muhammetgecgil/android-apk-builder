package com.mgecgil.seslirehber.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class MaturitySafetyFusionTest {
    private final SafetyGate gate = new SafetyGate();

    @Before public void before() { PerceptionContext.resetForTest(); }
    @After public void after() { PerceptionContext.resetForTest(); }

    @Test
    public void freshPersistentDarkCameraPreemptsOtherwiseQuietMotion() {
        long ts = 10_000L;
        PerceptionContext.noteSceneHealth(new SceneHealthObservation(
                5f, 0.01f, 0.99f, 0f, 0.02f, 0.95f, 1f, ts));
        GuidanceDecision decision = gate.evaluate(
                new MotionObservation(0f, -1f, -1f, 0f, ts), 0.90f);
        assertEquals(Risk.STOP, decision.risk());
        assertTrue(decision.speech().contains("Kamera"));
    }

    @Test
    public void staleBadCameraCannotPoisonNewDecision() {
        PerceptionContext.noteSceneHealth(new SceneHealthObservation(
                5f, 0.01f, 0.99f, 0f, 0.02f, 0.95f, 1f, 1_000L));
        GuidanceDecision decision = gate.evaluate(
                new MotionObservation(0f, -1f, -1f, 0f, 2_000L), 0.90f);
        assertEquals(Risk.INFO, decision.risk());
    }

    @Test
    public void freshPersistentWalkableCandidateCanGiveCautionButNeverSafeClaim() {
        long ts = 20_000L;
        PerceptionContext.noteWalkable(new WalkableCorridorObservation(
                0.88f, 0.24f, 0.38f, 0.76f,
                Direction.LEFT, 0.86f, 0.92f, ts));
        GuidanceDecision decision = gate.evaluateDepth(
                new DepthObservation(0.82f, 2100f, 1800f, 2800f,
                        220f, 0.08f, 0.82f, ts),
                0.90f);
        assertEquals(Risk.CAUTION, decision.risk());
        assertEquals(Direction.LEFT, decision.direction());
        assertTrue(decision.speech().contains("daha açık"));
        assertFalse(decision.speech().toLowerCase().contains("güvenli yol"));
        assertFalse(decision.speech().toLowerCase().contains("sola dön"));
    }

    @Test
    public void staleWalkableCandidateIsIgnored() {
        PerceptionContext.noteWalkable(new WalkableCorridorObservation(
                0.88f, 0.24f, 0.38f, 0.76f,
                Direction.LEFT, 0.86f, 0.92f, 10_000L));
        GuidanceDecision decision = gate.evaluateDepth(
                new DepthObservation(0.82f, 2100f, 1800f, 2800f,
                        220f, 0.08f, 0.82f, 11_000L),
                0.90f);
        assertEquals(Risk.INFO, decision.risk());
    }
}
