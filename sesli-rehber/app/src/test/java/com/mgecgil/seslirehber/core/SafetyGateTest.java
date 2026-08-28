package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class SafetyGateTest {
    @Test public void unstablePhoneForcesStop() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        MotionObservation o = new MotionObservation(0.2f, 0.5f, 0.5f, 0.9f, 1L);
        assertEquals(Risk.STOP, gate.evaluate(o, 0.2f).risk());
    }

    @Test public void fastApproachingCenterObjectForcesStop() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.50f, 0.50f, 0.82f, 0.16f, 0.22f, 0.00f, 7, 0.82f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateObject(o, 0.90f).risk());
    }

    @Test public void veryLargeStaticCenterObjectForcesStop() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.50f, 0.50f, 0.90f, 0.30f, 0.00f, 0.00f, 10, 0.86f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateObject(o, 0.92f).risk());
    }

    @Test public void smallSideObjectDoesNotForceStop() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.12f, 0.50f, 0.70f, 0.04f, 0.00f, 0.00f, 8, 0.82f, 1000L);
        assertEquals(Risk.INFO, gate.evaluateObject(o, 0.90f).risk());
    }

    @Test public void sideObjectCrossingTowardPathProducesCaution() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.25f, 0.52f, 0.78f, 0.09f, 0.03f, 0.18f, 11, 0.84f, 1000L);
        assertEquals(Risk.CAUTION, gate.evaluateObject(o, 0.90f).risk());
    }

    @Test public void unstablePhoneAlsoBlocksObjectConfidence() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.50f, 0.50f, 0.90f, 0.30f, 0.30f, 0.00f, 9, 0.90f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateObject(o, 0.20f).risk());
    }

    @Test public void persistentGroundDiscontinuityProducesCautionNotStop() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        GroundObservation o = new GroundObservation(
                0.72f, 0.71f, 0.55f, 0.18f, 0.84f, 0.82f, 0.73f, 1000L);
        assertEquals(Risk.CAUTION, gate.evaluateGround(o, 0.90f).risk());
    }

    @Test public void weakOrUncertainGroundEvidenceStaysQuiet() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        GroundObservation o = new GroundObservation(
                0.66f, 0.62f, 0.50f, 0.10f, 0.88f, 0.20f, 0.70f, 1000L);
        assertEquals(Risk.INFO, gate.evaluateGround(o, 0.90f).risk());
    }

    @Test public void persistentLevelCandidateAloneIsAdvisoryOnly() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        LevelChangeObservation level = new LevelChangeObservation(
                LevelChangeKind.DOWNWARD_CANDIDATE,
                0.78f, 0.76f, 0.72f, 0.10f, 0.86f, 0.82f, 0.68f, 5000L);
        GuidanceDecision decision = gate.evaluateLevelChange(level, 0.90f);
        assertEquals(Risk.CAUTION, decision.risk());
        String lower = decision.speech().toLowerCase();
        assertTrue(lower.contains("aşağı yönlü seviye değişimi olabilir"));
        assertFalse(lower.contains("çukur"));
        assertFalse(lower.contains("kaldırım"));
        assertFalse(lower.contains("merdiven"));
    }

    @Test public void freshIndependentGroundEvidenceCanRaisePersistentLevelCandidateToStop() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        GroundObservation ground = new GroundObservation(
                0.73f, 0.70f, 0.56f, 0.18f, 0.78f, 0.84f, 0.69f, 6000L);
        PerceptionContext.noteGround(ground);
        LevelChangeObservation level = new LevelChangeObservation(
                LevelChangeKind.UPWARD_CANDIDATE,
                0.80f, 0.77f, 0.74f, 0.08f, 0.82f, 0.84f, 0.66f, 6100L);
        GuidanceDecision decision = gate.evaluateLevelChange(level, 0.92f);
        assertEquals(Risk.STOP, decision.risk());
        assertTrue(decision.speech().contains("yukarı yönlü seviye değişimi olabilir"));
        assertFalse(decision.speech().toLowerCase().contains("merdiven"));
    }

    @Test public void staleGroundEvidenceCannotRaiseLevelCandidateToStop() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        PerceptionContext.noteGround(new GroundObservation(
                0.80f, 0.75f, 0.60f, 0.20f, 0.90f, 0.90f, 0.68f, 1000L));
        LevelChangeObservation level = new LevelChangeObservation(
                LevelChangeKind.DOWNWARD_CANDIDATE,
                0.82f, 0.80f, 0.77f, 0.10f, 0.90f, 0.86f, 0.68f, 5000L);
        assertEquals(Risk.CAUTION, gate.evaluateLevelChange(level, 0.92f).risk());
    }

    @Test public void oneFrameLevelCandidateStaysQuiet() {
        PerceptionContext.resetForTest();
        SafetyGate gate = new SafetyGate();
        LevelChangeObservation level = new LevelChangeObservation(
                LevelChangeKind.DOWNWARD_CANDIDATE,
                0.82f, 0.80f, 0.77f, 0.10f, 0.0f, 0.86f, 0.68f, 5000L);
        assertEquals(Risk.INFO, gate.evaluateLevelChange(level, 0.92f).risk());
    }
}
