package com.mg.fixturecockpitsim.visual;

import static org.junit.Assert.*;
import org.junit.Test;

public class AircraftControlSurfacesTest {
    @Test public void pitchMovesStabilatorsTogether() {
        AircraftControlSurfaces s = new AircraftControlSurfaces();
        s.update(0.7f, 0f, 0f, 0.8f, 0f, 150f, false);
        assertTrue(s.leftStabilatorDeg < -5f);
        assertEquals(s.leftStabilatorDeg, s.rightStabilatorDeg, 0.001f);
    }

    @Test public void rollMovesFlaperonsAndStabilatorsDifferentially() {
        AircraftControlSurfaces s = new AircraftControlSurfaces();
        s.update(0f, 0.8f, 0f, 0.8f, 0f, 150f, false);
        assertTrue(s.leftFlaperonDeg > 0f);
        assertTrue(s.rightFlaperonDeg < 0f);
        assertTrue(s.leftStabilatorDeg > 0f);
        assertTrue(s.rightStabilatorDeg < 0f);
    }

    @Test public void yawMovesTwinRuddersTogether() {
        AircraftControlSurfaces s = new AircraftControlSurfaces();
        s.update(0f, 0f, 0.75f, 0.8f, 0f, 150f, false);
        assertTrue(s.leftRudderDeg > 0f);
        assertEquals(s.leftRudderDeg, s.rightRudderDeg, 0.001f);
    }

    @Test public void gearDownLowSpeedAddsFlaperonDroop() {
        AircraftControlSurfaces s = new AircraftControlSurfaces();
        s.update(0f, 0f, 0f, 0.5f, 1f, 55f, false);
        assertTrue(s.flapDroopDeg > 8f);
        assertTrue(s.leftFlaperonDeg > 8f);
        assertTrue(s.rightFlaperonDeg > 8f);
    }

    @Test public void highSpeedReducesExtremeDeflection() {
        AircraftControlSurfaces slow = new AircraftControlSurfaces();
        AircraftControlSurfaces fast = new AircraftControlSurfaces();
        slow.update(0f, 1f, 0f, 1f, 0f, 100f, false);
        fast.update(0f, 1f, 0f, 1f, 0f, 330f, false);
        assertTrue(Math.abs(fast.leftFlaperonDeg) < Math.abs(slow.leftFlaperonDeg));
        assertTrue(Math.abs(fast.leftStabilatorDeg) < Math.abs(slow.leftStabilatorDeg));
    }
}
