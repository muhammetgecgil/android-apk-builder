package com.mg.fixturecockpitsim.sim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class FighterFlightControlSystemTest {

    private static FlightState airborne(double speedMps, double aoaDeg) {
        FlightState s = new FlightState();
        s.onGround = false;
        s.altitudeM = 1500.0;
        s.trueAirspeedMps = speedMps;
        s.angleOfAttackDeg = aoaDeg;
        s.loadFactor = 1.0;
        s.gearPosition = 0.0;
        return s;
    }

    @Test
    public void rollCommandsDifferentialStabilatorsAndFlaperons() {
        FighterFlightControlSystem fcs = new FighterFlightControlSystem();
        FlightState s = airborne(140.0, 5.0);
        FlightControls c = new FlightControls();
        c.roll = 0.75;

        FighterFlightControlSystem.Output o = fcs.update(s, c, 0.02);

        assertTrue(Math.abs(o.leftStabilatorDeg - o.rightStabilatorDeg) > 2.0);
        assertTrue(Math.abs(o.leftFlaperonDeg - o.rightFlaperonDeg) > 8.0);
        assertEquals(o.leftStabilatorDeg, s.leftStabilatorDeg, 1e-9);
        assertEquals(o.rightFlaperonDeg, s.rightFlaperonDeg, 1e-9);
    }

    @Test
    public void leadingEdgeFlapsScheduleForLowSpeedHighAoA() {
        FlightControls neutral = new FlightControls();

        FighterFlightControlSystem lowFcs = new FighterFlightControlSystem();
        FlightState low = airborne(90.0, 18.0);
        FighterFlightControlSystem.Output lowOut = lowFcs.update(low, neutral, 0.02);

        FighterFlightControlSystem fastFcs = new FighterFlightControlSystem();
        FlightState fast = airborne(300.0, 2.0);
        FighterFlightControlSystem.Output fastOut = fastFcs.update(fast, neutral, 0.02);

        assertTrue(lowOut.leftLeadingEdgeFlapDeg > 10.0);
        assertTrue(lowOut.rightLeadingEdgeFlapDeg > 10.0);
        assertTrue(lowOut.leftLeadingEdgeFlapDeg > fastOut.leftLeadingEdgeFlapDeg + 8.0);
    }

    @Test
    public void speedBrakeIsLimitedAtMaximumThrustAndHighAoA() {
        FlightControls c = new FlightControls();
        c.speedBrake = 1.0;
        c.throttle = 0.60;

        FighterFlightControlSystem normalFcs = new FighterFlightControlSystem();
        FighterFlightControlSystem.Output normal = normalFcs.update(airborne(180.0, 6.0), c, 0.02);
        assertTrue(normal.speedBrake01 > 0.95);

        c.throttle = 0.98;
        FighterFlightControlSystem thrustFcs = new FighterFlightControlSystem();
        FighterFlightControlSystem.Output thrustLimited = thrustFcs.update(airborne(180.0, 6.0), c, 0.02);
        assertTrue(thrustLimited.speedBrake01 <= 0.220001);

        c.throttle = 0.60;
        FighterFlightControlSystem aoaFcs = new FighterFlightControlSystem();
        FighterFlightControlSystem.Output aoaLimited = aoaFcs.update(airborne(180.0, 24.0), c, 0.02);
        assertTrue(aoaLimited.speedBrake01 <= 0.380001);
    }

    @Test
    public void highSpeedSchedulesDownControlAuthority() {
        FlightControls c = new FlightControls();
        c.pitch = 0.55;
        c.roll = 0.80;
        c.yaw = 0.50;

        FighterFlightControlSystem lowFcs = new FighterFlightControlSystem();
        FighterFlightControlSystem.Output low = lowFcs.update(airborne(110.0, 5.0), c, 0.02);

        FighterFlightControlSystem highFcs = new FighterFlightControlSystem();
        FighterFlightControlSystem.Output high = highFcs.update(airborne(305.0, 5.0), c, 0.02);

        assertTrue(high.authority01 < low.authority01);
        assertTrue(Math.abs(high.effectiveRoll) < Math.abs(low.effectiveRoll));
        assertTrue(Math.abs(high.effectiveYaw) < Math.abs(low.effectiveYaw));
    }

    @Test
    public void autotrimMovesStabilatorsWithoutSeparateTrimTab() {
        FighterFlightControlSystem fcs = new FighterFlightControlSystem();
        FlightState s = airborne(150.0, 7.0);
        s.pitchDeg = 14.0;
        FlightControls c = new FlightControls();

        for (int i = 0; i < 200; i++) fcs.update(s, c, 0.02);

        assertTrue(Math.abs(s.autoTrim) > 0.015);
        assertTrue(Math.abs(s.leftStabilatorDeg) > 0.20);
        assertEquals(s.leftStabilatorDeg, s.rightStabilatorDeg, 1e-9);
    }
}
