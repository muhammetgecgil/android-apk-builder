package com.mg.fixturecockpitsim.sim;

/**
 * AVM-19 fighter flight-control computer.
 *
 * The simulated airframe uses all-moving differential stabilators, trailing-edge
 * flaperons, twin rudders, automatic leading-edge flaps and one dorsal speed
 * brake. There is intentionally no separate elevator, conventional flap or
 * second "air brake" surface on this configuration.
 */
public final class FighterFlightControlSystem {
    public static final double MAX_STABILATOR_DEG = 25.0;
    public static final double MAX_FLAPERON_DEG = 25.0;
    public static final double MAX_RUDDER_DEG = 25.0;
    public static final double MAX_LE_FLAP_DEG = 20.0;
    public static final double MAX_SPEED_BRAKE_DEG = 45.0;

    private double autoTrim;
    private double previousHeadingDeg;
    private boolean headingValid;

    public static final class Output {
        public double effectivePitch;
        public double effectiveRoll;
        public double effectiveYaw;
        public double leftStabilatorDeg;
        public double rightStabilatorDeg;
        public double leftFlaperonDeg;
        public double rightFlaperonDeg;
        public double leftRudderDeg;
        public double rightRudderDeg;
        public double leftLeadingEdgeFlapDeg;
        public double rightLeadingEdgeFlapDeg;
        public double speedBrake01;
        public double speedBrakeDeg;
        public double autoTrim;
        public double yawDamper;
        public double authority01;
    }

    private final Output out = new Output();

    public Output update(FlightState s, FlightControls in, double dtSec) {
        dtSec = Math.max(0.001, Math.min(0.05, dtSec));
        final double speed = Math.max(0.0, s.trueAirspeedMps);
        final double aoa = s.angleOfAttackDeg;
        final double highSpeed = clamp01((speed - 150.0) / 165.0);
        final double lowSpeed = 1.0 - clamp01((speed - 65.0) / 115.0);
        final double highAoA = clamp01((aoa - 17.0) / 10.0);

        // Dynamic-pressure scheduling: preserve low-speed control power while
        // progressively reducing surface travel at fighter speeds.
        final double pitchAuthority = 1.0 - 0.30 * highSpeed;
        final double rollAuthority = (1.0 - 0.27 * highSpeed) * (1.0 - 0.18 * highAoA);
        final double yawAuthority = 1.0 - 0.42 * highSpeed;
        out.authority01 = Math.min(pitchAuthority, Math.min(rollAuthority, yawAuthority));

        // Soft AoA and positive-g protection. This does not take control away;
        // it only washes out additional nose-up demand near the configured limit.
        double pitchStick = in.pitch;
        if (pitchStick > 0.0) {
            final double aoaLimiter = 1.0 - 0.78 * clamp01((aoa - 20.0) / 8.0);
            final double gLimiter = 1.0 - 0.62 * clamp01((s.loadFactor - 8.1) / 1.2);
            pitchStick *= Math.max(0.18, Math.min(aoaLimiter, gLimiter));
        }

        // Fighter-style autotrim: the all-moving tail carries the trim function,
        // so there is no external trim-tab geometry. Manual trim remains available
        // for future cockpit/BT commands through FlightControls.pitchTrim.
        double trimTarget;
        if (s.onGround) {
            trimTarget = 0.0;
        } else if (Math.abs(in.pitch) < 0.10 && Math.abs(s.rollDeg) < 70.0) {
            trimTarget = clamp(s.pitchDeg / 32.0 * 0.11 + aoa / 24.0 * 0.045, -0.15, 0.15);
        } else {
            trimTarget = clamp(in.pitch * 0.07, -0.10, 0.10);
        }
        if (aoa > 22.0) trimTarget -= 0.10 * clamp01((aoa - 22.0) / 6.0);
        autoTrim += (trimTarget - autoTrim) * Math.min(1.0, dtSec * (s.onGround ? 2.2 : 0.55));
        autoTrim = clamp(autoTrim, -0.18, 0.18);
        out.autoTrim = autoTrim;

        out.effectivePitch = clamp(pitchStick * pitchAuthority + in.pitchTrim * 0.28 + autoTrim, -1.0, 1.0);
        out.effectiveRoll = clamp(in.roll * rollAuthority, -1.0, 1.0);

        // Residual-yaw damper. Coordinated bank turn is subtracted so the damper
        // attacks sideslip/gust motion instead of fighting the intended turn.
        double yawDamper = 0.0;
        if (!s.onGround && headingValid) {
            final double yawRate = wrap180(s.headingDeg - previousHeadingDeg) / dtSec;
            final double coordinatedRate = Math.sin(Math.toRadians(s.rollDeg)) * 28.0;
            final double residual = yawRate - coordinatedRate;
            yawDamper = clamp(-residual / 55.0, -0.24, 0.24);
        }
        previousHeadingDeg = s.headingDeg;
        headingValid = true;
        out.yawDamper = yawDamper;
        out.effectiveYaw = clamp(in.yaw * yawAuthority + yawDamper + in.roll * 0.035, -1.0, 1.0);

        // All-moving differential stabilators: primary pitch plus roll assist.
        final double stabPitchTravel = 24.0 - 5.0 * highSpeed;
        final double stabRollTravel = 8.0 - 2.0 * highSpeed;
        out.leftStabilatorDeg = clamp(-out.effectivePitch * stabPitchTravel + out.effectiveRoll * stabRollTravel,
                -MAX_STABILATOR_DEG, MAX_STABILATOR_DEG);
        out.rightStabilatorDeg = clamp(-out.effectivePitch * stabPitchTravel - out.effectiveRoll * stabRollTravel,
                -MAX_STABILATOR_DEG, MAX_STABILATOR_DEG);

        // Flaperons provide roll and automatically droop for low-speed/gear-down
        // operation. They also share a small pitch component with the stabilators.
        final double gearCamber = clamp01(s.gearPosition) * (s.onGround ? 3.0 : 4.5);
        final double autoCamber = lowSpeed * (s.onGround && speed < 25.0 ? 1.0 : 5.5);
        final double camber = Math.min(9.0, gearCamber + autoCamber);
        final double flapRollTravel = 18.5 - 3.5 * highSpeed;
        out.leftFlaperonDeg = clamp(camber - out.effectivePitch * 5.5 + out.effectiveRoll * flapRollTravel,
                -MAX_FLAPERON_DEG, MAX_FLAPERON_DEG);
        out.rightFlaperonDeg = clamp(camber - out.effectivePitch * 5.5 - out.effectiveRoll * flapRollTravel,
                -MAX_FLAPERON_DEG, MAX_FLAPERON_DEG);

        // Automatic leading-edge flap schedule. AoA is the main driver; low speed,
        // gear state and takeoff thrust add camber. A little differential motion
        // assists roll without turning the LE devices into primary ailerons.
        double leBase = 1.0 + 0.78 * Math.max(0.0, aoa - 3.0) + 5.5 * lowSpeed;
        if (s.gearPosition > 0.65 && speed > 35.0) leBase += 2.5;
        if (s.onGround && speed < 20.0 && in.throttle < 0.55) leBase = 0.0;
        if (speed > 270.0 && aoa < 7.0) leBase *= Math.max(0.0, 1.0 - (speed - 270.0) / 80.0);
        final double leDiff = out.effectiveRoll * (1.6 - 0.7 * highSpeed);
        out.leftLeadingEdgeFlapDeg = clamp(leBase - leDiff, 0.0, MAX_LE_FLAP_DEG);
        out.rightLeadingEdgeFlapDeg = clamp(leBase + leDiff, 0.0, MAX_LE_FLAP_DEG);

        // One dorsal speed-brake system. High AoA, very high speed and maximum
        // thrust automatically limit/retract it to avoid an unrealistic conflict.
        double sb = clamp01(in.speedBrake);
        if (!s.onGround && in.throttle > 0.92) sb = Math.min(sb, 0.22);
        if (!s.onGround && aoa > 21.0) sb = Math.min(sb, 0.38);
        if (!s.onGround && speed > 285.0) sb = Math.min(sb, 0.58);
        if (s.onGround && speed < 3.0) sb = 0.0;
        out.speedBrake01 = sb;
        out.speedBrakeDeg = MAX_SPEED_BRAKE_DEG * Math.sqrt(sb);

        // Publish the actual actuator commands for rendering, HUD and future data logging.
        s.leftStabilatorDeg = out.leftStabilatorDeg;
        s.rightStabilatorDeg = out.rightStabilatorDeg;
        s.leftFlaperonDeg = out.leftFlaperonDeg;
        s.rightFlaperonDeg = out.rightFlaperonDeg;
        s.leftRudderDeg = clamp(out.effectiveYaw * (23.5 - 7.0 * highSpeed), -MAX_RUDDER_DEG, MAX_RUDDER_DEG);
        s.rightRudderDeg = s.leftRudderDeg;
        out.leftRudderDeg = s.leftRudderDeg;
        out.rightRudderDeg = s.rightRudderDeg;
        s.leftLeadingEdgeFlapDeg = out.leftLeadingEdgeFlapDeg;
        s.rightLeadingEdgeFlapDeg = out.rightLeadingEdgeFlapDeg;
        s.speedBrake01 = out.speedBrake01;
        s.speedBrakeDeg = out.speedBrakeDeg;
        s.autoTrim = out.autoTrim;
        s.yawDamper = out.yawDamper;
        s.controlAuthority01 = out.authority01;
        return out;
    }

    private static double clamp01(double v) { return clamp(v, 0.0, 1.0); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double wrap180(double d) { d %= 360.0; if (d > 180.0) d -= 360.0; if (d < -180.0) d += 360.0; return d; }
}
