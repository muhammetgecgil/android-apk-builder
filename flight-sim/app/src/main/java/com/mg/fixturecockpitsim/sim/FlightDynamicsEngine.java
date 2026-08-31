package com.mg.fixturecockpitsim.sim;

import com.mg.fixturecockpitsim.WeatherEffectsView;

/** Deterministic mobile flight model with progressive takeoff, gravity/lift balance, wind gusts and stable deep-bank control. */
public final class FlightDynamicsEngine {
    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double GEAR_RATE_PER_SEC = 0.55;
    private static final double GROUND_HEIGHT_M = 0.0;
    private static final double NORMAL_BANK_DEG = 68.0;
    private static final double DEEP_BANK_DEG = 160.0;
    private static final double DEEP_BANK_GATE = 0.90;
    private static final double GRAVITY_MPS2 = 9.80665;
    private static final double LIFT_REFERENCE_SPEED_MPS = 82.0;

    public void step(FlightState s, FlightControls in, double dtSec) {
        if (dtSec <= 0) return;
        dtSec = Math.min(dtSec, 0.05);
        in.clamp();

        final boolean windy = WeatherEffectsView.isSharedWindy();
        final double windStrength = windy ? WeatherEffectsView.getSharedWindStrength() : 0.0;
        final int windSign = WeatherEffectsView.getSharedWindSign();
        final double airWindFactor = s.onGround ? 0.12 : 1.0;
        final double gustA = Math.sin(s.timeSec * 1.73 + 0.35) * 0.62 + Math.sin(s.timeSec * 4.91 + 1.14) * 0.25 + Math.sin(s.timeSec * 9.2) * 0.13;
        final double gustB = Math.sin(s.timeSec * 2.31 + 1.7) * 0.72 + Math.sin(s.timeSec * 6.4 + .4) * 0.28;
        final double gustRollDeg = windy ? windSign * windStrength * airWindFactor * (1.6 + 3.8 * gustA) : 0.0;
        final double gustYawRate = windy ? windSign * windStrength * airWindFactor * (0.45 + 1.15 * gustB) : 0.0;
        final double gustVerticalMps = windy && !s.onGround ? windStrength * (1.25 * gustB + .55 * gustA) : 0.0;

        double gearTarget = in.gearDown ? 1.0 : 0.0;
        s.gearPosition = approach(s.gearPosition, gearTarget, GEAR_RATE_PER_SEC * dtSec);
        s.brake01 += (in.brake - s.brake01) * Math.min(1.0, dtSec * 7.0);

        // Normal travel remains calm. A deliberate near-full roll command opens the
        // slower deep-bank envelope up to 160 degrees. Wind rides on top as a small gust bias.
        double rollInput = Math.max(-1.0, Math.min(1.0, in.roll));
        double absRoll = Math.abs(rollInput);
        if (absRoll < 0.025) rollInput = 0.0;
        absRoll = Math.abs(rollInput);
        double targetRoll;
        double rollRate;
        if (absRoll <= DEEP_BANK_GATE) {
            targetRoll = rollInput * (NORMAL_BANK_DEG / DEEP_BANK_GATE);
            rollRate = Math.abs(s.rollDeg) > 75.0 ? 1.28 : 2.05;
        } else {
            targetRoll = Math.copySign(DEEP_BANK_DEG, rollInput);
            rollRate = .56;
        }
        targetRoll = Math.max(-DEEP_BANK_DEG, Math.min(DEEP_BANK_DEG, targetRoll + gustRollDeg));
        double targetPitch = in.pitch * 30.0;
        s.rollDeg += (targetRoll - s.rollDeg) * Math.min(1.0, dtSec * rollRate);
        s.rollDeg = Math.max(-DEEP_BANK_DEG, Math.min(DEEP_BANK_DEG, s.rollDeg));
        s.pitchDeg += (targetPitch - s.pitchDeg) * Math.min(1.0, dtSec * 2.4);
        s.headingDeg = wrap360(s.headingDeg + Math.sin(Math.toRadians(s.rollDeg)) * 28.0 * dtSec + in.yaw * 18.0 * dtSec + gustYawRate * dtSec);

        double throttleResponse = s.onGround ? .82 : 1.75;
        s.throttle += (in.throttle - s.throttle) * Math.min(1.0, dtSec * throttleResponse);

        if (s.onGround) {
            double targetSpeed = s.throttle * 125.0;
            double error = targetSpeed - s.trueAirspeedMps;
            if (error >= 0) {
                double accel = 1.15 + 6.0 * s.throttle;
                double aeroLoss = .00017 * s.trueAirspeedMps * s.trueAirspeedMps;
                double net = Math.max(.35, accel - aeroLoss);
                s.trueAirspeedMps = Math.min(targetSpeed, s.trueAirspeedMps + net * dtSec);
            } else {
                double coast = 1.0 + 2.0 * s.brake01;
                s.trueAirspeedMps = Math.max(targetSpeed, s.trueAirspeedMps - coast * dtSec);
            }
        } else {
            double gearDrag = 18.0 * s.gearPosition;
            double speedBrakeDrag = 70.0 * s.brake01;
            double targetSpeed = Math.max(0.0, 55.0 + s.throttle * 250.0 - gearDrag - speedBrakeDrag);
            s.trueAirspeedMps += (targetSpeed - s.trueAirspeedMps) * Math.min(1.0, dtSec * .48);
            if (s.brake01 > .05) s.trueAirspeedMps = Math.max(0.0, s.trueAirspeedMps - (1.2 + 5.2 * s.brake01) * dtSec);
        }

        // Lift/gravity balance: below flying speed the aircraft can no longer hold itself up.
        // At extreme bank angles the vertical lift component also collapses, so a stable 160°
        // attitude is possible but altitude is no longer magically maintained.
        double uprightLift = Math.max(0.0, Math.cos(Math.toRadians(s.rollDeg)));
        double speedLift = (s.trueAirspeedMps / LIFT_REFERENCE_SPEED_MPS);
        speedLift *= speedLift;
        double liftSupport = clamp01(speedLift * uprightLift);
        double pitchKinematicVs = s.trueAirspeedMps * Math.sin(Math.toRadians(s.pitchDeg)) * Math.max(.20, uprightLift);

        double airborneVs;
        if (s.onGround) {
            airborneVs = Math.max(0.0, pitchKinematicVs);
        } else {
            double follow = Math.min(1.0, dtSec * 1.55);
            s.verticalSpeedMps += (pitchKinematicVs - s.verticalSpeedMps) * follow;
            double gravityDeficit = 1.0 - liftSupport;
            s.verticalSpeedMps -= GRAVITY_MPS2 * gravityDeficit * dtSec;
            s.verticalSpeedMps += gustVerticalMps * dtSec * 2.2;
            s.verticalSpeedMps = Math.max(-78.0, Math.min(75.0, s.verticalSpeedMps));

            // Low-energy stall tendency: nose gently seeks down as energy disappears.
            double stall = clamp01((68.0 - s.trueAirspeedMps) / 68.0) * clamp01(s.altitudeM / 20.0);
            if (stall > 0.0) s.pitchDeg += (-8.0 - s.pitchDeg) * Math.min(1.0, dtSec * (.16 + .42 * stall));
            airborneVs = s.verticalSpeedMps;
        }

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
            s.rollDeg += (0.0 - s.rollDeg) * Math.min(1.0, dtSec * 2.8);
            s.pitchDeg += (0.0 - s.pitchDeg) * Math.min(1.0, dtSec * 1.4);
        } else {
            s.onGround = false;
            s.verticalSpeedMps = airborneVs;
            s.altitudeM = Math.max(0.0, proposedAltitude);
            s.mainStrutCompression01 += (0.0 - s.mainStrutCompression01) * Math.min(1.0, dtSec * 5.0);
            s.noseStrutCompression01 += (0.0 - s.noseStrutCompression01) * Math.min(1.0, dtSec * 5.0);
            if (s.altitudeM > 1.0) s.touchdownSinkMps = 0.0;
        }

        s.angleOfAttackDeg = in.pitch * 10.0 - s.pitchDeg * 0.08;
        s.loadFactor = Math.max(0.1, liftSupport / Math.max(0.18, Math.abs(Math.cos(Math.toRadians(s.rollDeg)))));
        double groundSpeed = s.trueAirspeedMps * Math.cos(Math.toRadians(s.pitchDeg));
        double distance = groundSpeed * dtSec;
        double hdg = Math.toRadians(s.headingDeg);
        double north = Math.cos(hdg) * distance;
        double east = Math.sin(hdg) * distance;

        if (windy && !s.onGround) {
            double crosswind = windSign * (5.0 + 15.0 * windStrength);
            double crossHdg = hdg + Math.PI * .5;
            north += Math.cos(crossHdg) * crosswind * dtSec;
            east += Math.sin(crossHdg) * crosswind * dtSec;
        }

        s.latitudeDeg += Math.toDegrees(north / EARTH_RADIUS_M);
        double cosLat = Math.max(0.15, Math.cos(Math.toRadians(s.latitudeDeg)));
        s.longitudeDeg += Math.toDegrees(east / (EARTH_RADIUS_M * cosLat));
        s.timeSec += dtSec;
    }

    private static double approach(double value,double target,double maxDelta){if(value<target)return Math.min(target,value+maxDelta);return Math.max(target,value-maxDelta);}
    private static double clamp01(double v){return Math.max(0.0,Math.min(1.0,v));}
    private static double wrap360(double d){d%=360.0;return d<0?d+360.0:d;}
}
