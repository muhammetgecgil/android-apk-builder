package com.mg.fixturecockpitsim.sim;

/** Deterministic mobile-friendly flight model with stabilized normal bank and deliberate aerobatic authority. */
public final class FlightDynamicsEngine {
    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double GEAR_RATE_PER_SEC = 0.55;
    private static final double GROUND_HEIGHT_M = 0.0;
    private static final double ROTATION_SPEED_MPS = 82.0;

    public void step(FlightState s, FlightControls in, double dtSec) {
        if (dtSec <= 0) return;
        dtSec = Math.min(dtSec, 0.05);
        in.clamp();

        double gearTarget = in.gearDown ? 1.0 : 0.0;
        s.gearPosition = approach(s.gearPosition, gearTarget, GEAR_RATE_PER_SEC * dtSec);
        s.brake01 += (in.brake - s.brake01) * Math.min(1.0, dtSec * 7.0);

        // AVM-12.7: while weight-on-wheels is true the aircraft cannot visually bank.
        // Normal IMU/controller roll remains in a stable bank envelope; aerobatics require a deliberate near-full command in the air.
        double rollInput=Math.max(-1.0,Math.min(1.0,in.roll));
        double absRoll=Math.abs(rollInput);
        if(absRoll<0.025) rollInput=0.0;
        absRoll=Math.abs(rollInput);
        double targetRoll;
        if(s.onGround) {
            targetRoll=0.0;
        } else if(absRoll<=0.96) {
            targetRoll=rollInput*(65.0/0.96);
        } else {
            double t=(absRoll-0.96)/0.04;
            targetRoll=Math.copySign(65.0+t*115.0,rollInput);
        }

        // Below rotation speed the pilot cannot raise/lower the nose: ground mode is taxi/NWS only.
        // Above Vr the takeoff rotation command is allowed, which lets both demo and manual modes actually lift off.
        double pitchInput=(s.onGround && s.trueAirspeedMps<ROTATION_SPEED_MPS)?0.0:in.pitch;
        double targetPitch = pitchInput * 30.0;
        double rollRate = absRoll>0.96 && !s.onGround ? 3.25 : 2.30;
        s.rollDeg += (targetRoll - s.rollDeg) * Math.min(1.0, dtSec * rollRate);
        s.pitchDeg += (targetPitch - s.pitchDeg) * Math.min(1.0, dtSec * 2.4);
        s.headingDeg = wrap360(s.headingDeg + Math.sin(Math.toRadians(s.rollDeg)) * 28.0 * dtSec + in.yaw * 18.0 * dtSec);

        s.throttle += (in.throttle - s.throttle) * Math.min(1.0, dtSec * 2.0);
        double targetSpeed = s.onGround ? s.throttle * 125.0 : 55.0 + s.throttle * 250.0;
        s.trueAirspeedMps += (targetSpeed - s.trueAirspeedMps) * Math.min(1.0, dtSec * 0.55);

        double bankLift=Math.max(0.28,Math.abs(Math.cos(Math.toRadians(s.rollDeg))));
        double airborneVs = s.trueAirspeedMps * Math.sin(Math.toRadians(s.pitchDeg)) * bankLift;
        double proposedAltitude = s.altitudeM + airborneVs * dtSec;
        boolean gearUsable = s.gearPosition > 0.82;
        boolean groundCandidate = proposedAltitude <= GROUND_HEIGHT_M + 0.12 && airborneVs <= 1.0;

        if (groundCandidate && gearUsable) {
            if (!s.onGround) s.touchdownSinkMps = Math.max(0.0, -airborneVs);
            s.onGround = true;
            s.altitudeM = GROUND_HEIGHT_M;
            s.verticalSpeedMps = 0.0;
            double touchdownLoad = clamp01(s.touchdownSinkMps / 4.5);
            double speedLoad = clamp01(s.trueAirspeedMps / 95.0) * 0.18;
            double targetMainCompression = clamp01(0.16 + touchdownLoad * 0.72 + speedLoad);
            double targetNoseCompression = clamp01(0.10 + Math.max(0.0, -s.pitchDeg) / 12.0 * 0.48);
            s.mainStrutCompression01 += (targetMainCompression - s.mainStrutCompression01) * Math.min(1.0, dtSec * 7.5);
            s.noseStrutCompression01 += (targetNoseCompression - s.noseStrutCompression01) * Math.min(1.0, dtSec * 6.0);
            double rollingDecel = 0.55 + 9.0 * s.brake01;
            s.trueAirspeedMps = Math.max(0.0, s.trueAirspeedMps - rollingDecel * dtSec);
            double steerAuthority = 20.0 * clamp01(1.0 - s.trueAirspeedMps / 85.0);
            s.headingDeg = wrap360(s.headingDeg + in.yaw * steerAuthority * dtSec);
            s.rollDeg += (0.0 - s.rollDeg) * Math.min(1.0, dtSec * 5.2);
            s.pitchDeg += (0.0 - s.pitchDeg) * Math.min(1.0, dtSec * 2.8);
        } else {
            s.onGround = false;
            s.verticalSpeedMps = airborneVs;
            s.altitudeM = Math.max(0.0, proposedAltitude);
            s.mainStrutCompression01 += (0.0 - s.mainStrutCompression01) * Math.min(1.0, dtSec * 5.0);
            s.noseStrutCompression01 += (0.0 - s.noseStrutCompression01) * Math.min(1.0, dtSec * 5.0);
            if (s.altitudeM > 1.0) s.touchdownSinkMps = 0.0;
        }

        s.angleOfAttackDeg = pitchInput * 10.0 - s.pitchDeg * 0.08;
        s.loadFactor = Math.max(0.1, 1.0 / Math.max(0.18, Math.abs(Math.cos(Math.toRadians(s.rollDeg)))));
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

    private static double approach(double value,double target,double maxDelta){if(value<target)return Math.min(target,value+maxDelta);return Math.max(target,value-maxDelta);}
    private static double clamp01(double v){return Math.max(0.0,Math.min(1.0,v));}
    private static double wrap360(double d){d%=360.0;return d<0?d+360.0:d;}
}
