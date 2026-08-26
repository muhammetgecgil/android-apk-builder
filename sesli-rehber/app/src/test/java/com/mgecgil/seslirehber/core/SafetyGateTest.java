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
        ObjectObservation o = new ObjectObservation(0.50f, 0.50f, 0.16f, 0.22f, 7, 0.82f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateObject(o, 0.90f).risk());
    }

    @Test public void smallSideObjectDoesNotForceStop() {
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(0.12f, 0.50f, 0.04f, 0.00f, 8, 0.82f, 1000L);
        assertEquals(Risk.INFO, gate.evaluateObject(o, 0.90f).risk());
    }

    @Test public void unstablePhoneAlsoBlocksObjectConfidence() {
        SafetyGate gate = new SafetyGate();
        ObjectObservation o = new ObjectObservation(0.50f, 0.50f, 0.30f, 0.30f, 9, 0.90f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateObject(o, 0.20f).risk());
    }
}
