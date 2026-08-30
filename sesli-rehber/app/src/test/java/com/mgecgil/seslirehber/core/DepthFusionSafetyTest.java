package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class DepthFusionSafetyTest {
    @Test public void depthAloneNeverForcesStop() {
        SafetyGate gate = new SafetyGate();
        DepthObservation depth = new DepthObservation(
                0.90f, 2400f, 1700f, 5200f, 3500f, 0.92f, 0.90f, 1000L);
        assertEquals(Risk.CAUTION, gate.evaluateDepth(depth, 0.92f).risk());
    }

    @Test public void persistentGroundPlusStrongDepthCanForceStop() {
        SafetyGate gate = new SafetyGate();
        GroundObservation ground = new GroundObservation(
                0.82f, 0.76f, 0.64f, 0.62f, 0.86f, 0.88f, 0.66f, 1000L);
        DepthObservation depth = new DepthObservation(
                0.88f, 2450f, 1750f, 5100f, 3350f, 0.90f, 0.88f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateGroundWithDepth(ground, depth, 0.92f).risk());
    }

    @Test public void weakGroundEvidencePreventsDepthStop() {
        SafetyGate gate = new SafetyGate();
        GroundObservation ground = new GroundObservation(
                0.38f, 0.30f, 0.30f, 0.25f, 0.28f, 0.88f, 0.66f, 1000L);
        DepthObservation depth = new DepthObservation(
                0.88f, 2450f, 1750f, 5100f, 3350f, 0.90f, 0.88f, 1000L);
        assertEquals(Risk.CAUTION, gate.evaluateGroundWithDepth(ground, depth, 0.92f).risk());
    }

    @Test public void unstableDeviceStillUsesFailSafeStop() {
        SafetyGate gate = new SafetyGate();
        GroundObservation ground = new GroundObservation(
                0.82f, 0.76f, 0.64f, 0.62f, 0.86f, 0.88f, 0.66f, 1000L);
        DepthObservation depth = new DepthObservation(
                0.88f, 2450f, 1750f, 5100f, 3350f, 0.90f, 0.88f, 1000L);
        assertEquals(Risk.STOP, gate.evaluateGroundWithDepth(ground, depth, 0.20f).risk());
    }
}
