package com.mg.fixturecockpitsim.sim;

/**
 * Demo mission that uses the same FlightControls path as a cockpit/pilot input.
 * Sequence: runway hold -> takeoff -> climb -> 5 minute orbit -> approach -> flare -> rollout.
 */
public final class AutonomousFlightMission {
    public enum Phase { RUNWAY_HOLD, TAKEOFF_ROLL, ROTATE_CLIMB, ORBIT, APPROACH, FLARE, ROLLOUT, COMPLETE }

    private static final double ORBIT_DURATION_SEC = 300.0;
    private static final double CRUISE_ALTITUDE_M = 900.0;
    private static final double RUNWAY_HEADING_DEG = 0.0;

    private Phase phase = Phase.RUNWAY_HOLD;
    private double phaseTime;
    private double orbitTime;

    public void reset(FlightState s) {
        phase = Phase.RUNWAY_HOLD;
        phaseTime = 0.0;
        orbitTime = 0.0;
        s.timeSec = 0.0;
        s.altitudeM = 0.0;
        s.trueAirspeedMps = 0.0;
        s.verticalSpeedMps = 0.0;
        s.headingDeg = RUNWAY_HEADING_DEG;
        s.pitchDeg = 0.0;
        s.rollDeg = 0.0;
        s.throttle = 0.0;
        s.gearPosition = 1.0;
        s.brake01 = 1.0;
        s.onGround = true;
    }

    public Phase getPhase() { return phase; }
    public double getOrbitTimeSec() { return orbitTime; }

    public void update(FlightState s, FlightControls c, double dtSec) {
        phaseTime += dtSec;
        c.pitch = 0.0; c.roll = 0.0; c.yaw = 0.0; c.brake = 0.0;

        switch (phase) {
            case RUNWAY_HOLD:
                c.throttle = 0.10;
                c.brake = 1.0;
                c.gearDown = true;
                if (phaseTime >= 2.5) next(Phase.TAKEOFF_ROLL);
                break;

            case TAKEOFF_ROLL:
                c.throttle = 1.0;
                c.brake = 0.0;
                c.gearDown = true;
                c.yaw = headingError(s.headingDeg, RUNWAY_HEADING_DEG) * 0.035;
                if (s.trueAirspeedMps >= 82.0 || phaseTime >= 14.0) next(Phase.ROTATE_CLIMB);
                break;

            case ROTATE_CLIMB:
                c.throttle = 0.94;
                c.pitch = altitudePitch(s.altitudeM, CRUISE_ALTITUDE_M, 0.42);
                c.roll = headingRoll(s.headingDeg, RUNWAY_HEADING_DEG);
                c.gearDown = s.altitudeM < 35.0;
                if (s.altitudeM >= CRUISE_ALTITUDE_M - 35.0) next(Phase.ORBIT);
                break;

            case ORBIT:
                orbitTime += dtSec;
                c.throttle = 0.70;
                c.pitch = altitudePitch(s.altitudeM, CRUISE_ALTITUDE_M, 0.14);
                c.roll = 0.28; // gentle continuous orbit using normal cockpit control path
                c.gearDown = false;
                if (orbitTime >= ORBIT_DURATION_SEC) next(Phase.APPROACH);
                break;

            case APPROACH:
                c.gearDown = true;
                c.throttle = s.altitudeM > 260.0 ? 0.46 : 0.36;
                c.roll = headingRoll(s.headingDeg, RUNWAY_HEADING_DEG);
                c.pitch = s.altitudeM > 160.0 ? -0.16 : -0.10;
                if (s.altitudeM <= 18.0) next(Phase.FLARE);
                break;

            case FLARE:
                c.gearDown = true;
                c.throttle = 0.18;
                c.pitch = s.altitudeM > 3.0 ? -0.035 : 0.10;
                c.roll = headingRoll(s.headingDeg, RUNWAY_HEADING_DEG) * 0.6;
                if (s.onGround || s.altitudeM <= 0.05) next(Phase.ROLLOUT);
                break;

            case ROLLOUT:
                c.gearDown = true;
                c.throttle = 0.0;
                c.brake = 0.88;
                c.yaw = headingError(s.headingDeg, RUNWAY_HEADING_DEG) * 0.04;
                if (s.trueAirspeedMps < 2.0 && phaseTime > 2.0) next(Phase.COMPLETE);
                break;

            case COMPLETE:
                c.gearDown = true;
                c.throttle = 0.0;
                c.brake = 1.0;
                break;
        }
        c.clamp();
    }

    private void next(Phase p) { phase = p; phaseTime = 0.0; }

    private static double altitudePitch(double altitude, double target, double maxPitchCmd) {
        return clamp((target - altitude) / 700.0, -0.14, maxPitchCmd);
    }

    private static double headingRoll(double heading, double target) {
        return clamp(headingError(heading, target) / 55.0, -0.45, 0.45);
    }

    private static double headingError(double heading, double target) {
        double d = target - heading;
        while (d > 180.0) d -= 360.0;
        while (d < -180.0) d += 360.0;
        return d;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
