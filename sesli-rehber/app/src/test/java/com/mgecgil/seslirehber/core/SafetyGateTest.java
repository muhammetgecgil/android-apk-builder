package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class SafetyGateTest {
    @Test public void unstablePhoneForcesStop() {
        SafetyGate gate = new SafetyGate();
        MotionObservation o = new MotionObservation(0.2f, 0.5f, 0.5f, 0.9f, 1L);
        assertEquals(Risk.STOP, gate.evaluate(o, 0.2f).risk());
    }

    @Test public void fastApproachingCenterObjectForcesStop() {
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.50f, 0.50f, 0.82f, 0.16f, 0.22f, 0.00f, 7, 0.82f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateObject(o, 0.90f).risk());
    }

    @Test public void veryLargeStaticCenterObjectForcesStop() {
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.50f, 0.50f, 0.90f, 0.30f, 0.00f, 0.00f, 10, 0.86f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateObject(o, 0.92f).risk());
    }

    @Test public void smallSideObjectDoesNotForceStop() {
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.12f, 0.50f, 0.70f, 0.04f, 0.00f, 0.00f, 8, 0.82f, 1000L);
        assertEquals(Risk.INFO, gate.evaluateObject(o, 0.90f).risk());
    }

    @Test public void sideObjectCrossingTowardPathProducesCaution() {
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.25f, 0.52f, 0.78f, 0.09f, 0.03f, 0.18f, 11, 0.84f, 1000L);
        assertEquals(Risk.CAUTION, gate.evaluateObject(o, 0.90f).risk());
    }

    @Test public void unstablePhoneAlsoBlocksObjectConfidence() {
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(
                0.50f, 0.50f, 0.90f, 0.30f, 0.30f, 0.00f, 9, 0.90f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateObject(o, 0.20f).risk());
    }

    @Test public void persistentGroundDiscontinuityProducesCautionNotStop() {
        SafetyGate gate = new SafetyGate();
        GroundObservation o = new GroundObservation(
                0.72f, 0.71f, 0.55f, 0.18f, 0.84f, 0.82f, 0.73f, 1000L);
        assertEquals(Risk.CAUTION, gate.evaluateGround(o, 0.90f).risk());
    }

    @Test public void weakOrUncertainGroundEvidenceStaysQuiet() {
        SafetyGate gate = new SafetyGate();
        GroundObservation o = new GroundObservation(
                0.66f, 0.62f, 0.50f, 0.10f, 0.88f, 0.20f, 0.70f, 1000L);
        assertEquals(Risk.INFO, gate.evaluateGround(o, 0.90f).risk());
    }
}
