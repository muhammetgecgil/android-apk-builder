package com.mg.fixturecockpitsim.sim;

/**
 * Deterministic mobile-friendly first flight model.
 * It is intentionally separated from rendering/Bluetooth so a higher fidelity 6-DoF model can replace it later.
 */
public final class FlightDynamicsEngine {
    private static final double G = 9.80665;
    private static final double EARTH_RADIUS_M = 6371000.0;

    public void step(FlightState s, FlightControls in, double dtSec) {
        if (dtSec <= 0) return;
        dtSec = Math.min(dtSec, 0.05);
        in.clamp();

        double targetRoll = in.roll * 75.0;
        double targetPitch = in.pitch * 30.0;
        s.rollDeg += (targetRoll - s.rollDeg) * Math.min(1.0, dtSec * 3.2);
        s.pitchDeg += (targetPitch - s.pitchDeg) * Math.min(1.0, dtSec * 2.4);
        s.headingDeg = wrap360(s.headingDeg + Math.sin(Math.toRadians(s.rollDeg)) * 28.0 * dtSec + in.yaw * 18.0 * dtSec);

        s.throttle += (in.throttle - s.throttle) * Math.min(1.0, dtSec * 2.0);
        double targetSpeed = 55.0 + s.throttle * 250.0;
        s.trueAirspeedMps += (targetSpeed - s.trueAirspeedMps) * Math.min(1.0, dtSec * 0.55);

        s.verticalSpeedMps = s.trueAirspeedMps * Math.sin(Math.toRadians(s.pitchDeg));
        s.altitudeM = Math.max(0.0, s.altitudeM + s.verticalSpeedMps * dtSec);
        s.angleOfAttackDeg = in.pitch * 10.0 - s.pitchDeg * 0.08;
        s.loadFactor = Math.max(0.1, 1.0 / Math.max(0.18, Math.cos(Math.toRadians(s.rollDeg))));

        double groundSpeed = s.trueAirspeedMps * Math.cos(Math.toRadians(s.pitchDeg));
        double distance = groundSpeed * dtSec;
        double hdg = Math.toRadians(s.headingDeg);
        double north = Math.cos(hdg) * distance;
        double east = Math.sin(hdg) * distance;
        s.latitudeDeg += Math.toDegrees(north / EARTH_RADIUS_M);
        double cosLat = Math.max(0.15, Math.cos(Math.toRadians(s.latitudeDeg)));
        s.longitudeDeg += Math.toDegrees(east / (EARTH_RADIUS_M * cosLat));
        s.timeSec += dtSec;
    }

    private static double wrap360(double d){ d%=360.0; return d<0?d+360.0:d; }
}
