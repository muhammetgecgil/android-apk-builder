package com.mg.fixturecockpitsim.sim;

/**
 * AVM-27 fighter-like aerodynamic core.
 *
 * This is a deterministic simulation model, not certified aircraft data.  It replaces
 * the old target-speed/lift-support approximation with coefficient-table aerodynamics,
 * ISA-like atmosphere, dynamic pressure, mass/CG/inertia scheduling, ground effect,
 * stall/spin cues and control-generated moments.
 */
public final class AdvancedFlightPhysicsModel {
    public static final double GRAVITY_MPS2 = 9.80665;
    public static final double WING_AREA_M2 = 28.5;
    public static final double WING_SPAN_M = 9.45;
    public static final double MEAN_CHORD_M = 3.40;
    public static final double DRY_MASS_KG = 8500.0;
    public static final double INITIAL_FUEL_KG = 3200.0;
    public static final double EMPTY_CG_MAC = 0.300;
    public static final double FULL_FUEL_CG_MAC = 0.342;
    public static final double NEUTRAL_POINT_MAC = 0.390;

    // Generic fighter-like wind-tunnel schedule: post-stall CL rolls off while CD rises.
    private static final double[] AOA_DEG =
            {-20,-15,-10,-5,0,5,10,15,18,22,26,30,35,40};
    private static final double[] CL_TABLE =
            {-0.95,-0.75,-0.48,-0.20,0.10,0.62,1.10,1.42,1.55,1.45,1.22,0.95,0.70,0.52};
    private static final double[] CD0_TABLE =
            {0.25,0.15,0.080,0.035,0.022,0.025,0.038,0.065,0.100,0.180,0.300,0.440,0.650,0.880};

    private final SupersonicFlightModel supersonic = new SupersonicFlightModel();

    public static final class Output {
        public double densityKgM3;
        public double speedOfSoundMps;
        public double mach;
        public double dynamicPressurePa;
        public double flightPathDeg;
        public double aoaDeg;
        public double betaDeg;
        public double cl;
        public double cd;
        public double cy;
        public double liftN;
        public double dragN;
        public double sideForceN;
        public double thrustN;
        public double rollMomentNm;
        public double pitchMomentNm;
        public double yawMomentNm;
        public double adverseYawMomentNm;
        public double stall01;
        public double spin01;
        public double groundEffect01;
        public double waveDrag01;
        public double transonicBuffet01;
        public double shockStrength01;
        public double afterburner01;
        public double fuelFlowKgSec;
        public double loadFactor;
    }

    public Output evaluate(FlightState s, FlightControls in, FighterFlightControlSystem.Output fcs) {
        Output o = new Output();
        final double v = Math.max(0.0, s.trueAirspeedMps);
        final double alt = Math.max(0.0, s.altitudeM);
        final double mass = s.massKg > DRY_MASS_KG ? s.massKg : DRY_MASS_KG + Math.max(0.0, s.fuelKg);
        final double fuelFraction = clamp(s.fuelKg / INITIAL_FUEL_KG, 0.0, 1.0);
        final double cg = validCg(s.cgMac) ? s.cgMac : EMPTY_CG_MAC + fuelFraction * (FULL_FUEL_CG_MAC - EMPTY_CG_MAC);
        final double le01 = clamp((s.leftLeadingEdgeFlapDeg + s.rightLeadingEdgeFlapDeg) /
                (2.0 * FighterFlightControlSystem.MAX_LE_FLAP_DEG), 0.0, 1.0);
        final double sb = clamp(s.speedBrake01, 0.0, 1.0);
        final double gear = clamp(s.gearPosition, 0.0, 1.0);

        o.densityKgM3 = airDensityKgM3(alt);
        o.speedOfSoundMps = speedOfSoundMps(alt);
        o.mach = v / Math.max(1.0, o.speedOfSoundMps);
        o.dynamicPressurePa = 0.5 * o.densityKgM3 * v * v;

        double forward = Math.sqrt(Math.max(1.0, v * v - s.verticalSpeedMps * s.verticalSpeedMps));
        o.flightPathDeg = Math.toDegrees(Math.atan2(s.verticalSpeedMps, forward));
        o.aoaDeg = clamp(wrap180(s.pitchDeg - o.flightPathDeg), -40.0, 40.0);
        o.betaDeg = clamp(s.sideslipDeg, -30.0, 30.0);

        SupersonicFlightModel.Output sup = supersonic.evaluate(alt, v, s.throttle, gear, sb, le01);
        o.waveDrag01 = sup.waveDrag01;
        o.transonicBuffet01 = sup.transonicBuffet01;
        o.shockStrength01 = sup.shockStrength01;
        o.afterburner01 = sup.afterburner01;

        final double absAlpha = Math.abs(o.aoaDeg);
        final double criticalAlpha = 18.0 + 3.0 * le01;
        o.stall01 = smooth(criticalAlpha, criticalAlpha + 13.0, absAlpha);

        double cl = interpolate(AOA_DEG, CL_TABLE, o.aoaDeg);
        // Automatic LE flap adds camber but the gain washes out after deep stall.
        cl += 0.22 * le01 * (1.0 - 0.72 * o.stall01) * Math.signum(o.aoaDeg + 0.25);
        double machLift = 1.0 + 0.10 * smooth(0.45, 0.82, o.mach) - 0.18 * smooth(0.95, 1.35, o.mach);
        cl *= machLift;

        double groundRatio = alt / WING_SPAN_M;
        o.groundEffect01 = s.onGround ? 1.0 : sq(1.0 - clamp(groundRatio, 0.0, 1.0));
        cl *= 1.0 + 0.12 * o.groundEffect01;

        double cd0 = interpolate(AOA_DEG, CD0_TABLE, o.aoaDeg);
        double inducedFactor = 1.0 - 0.55 * o.groundEffect01;
        double induced = 0.060 * cl * cl * inducedFactor;
        double betaDrag = 0.00055 * o.betaDeg * o.betaDeg;
        double waveCd = 0.115 * o.waveDrag01;
        double configCd = 0.095 * gear + 0.205 * sb + 0.016 * le01;
        o.cl = cl;
        o.cd = Math.max(0.018, cd0 + induced + betaDrag + waveCd + configCd);
        o.cy = clamp(-0.020 * o.betaDeg + 0.105 * fcs.effectiveYaw, -0.75, 0.75);

        o.liftN = o.dynamicPressurePa * WING_AREA_M2 * o.cl;
        o.dragN = o.dynamicPressurePa * WING_AREA_M2 * o.cd;
        o.sideForceN = o.dynamicPressurePa * WING_AREA_M2 * o.cy;

        double densityRatio = clamp(o.densityKgM3 / 1.225, 0.08, 1.0);
        double lapse = 0.52 + 0.48 * Math.sqrt(densityRatio);
        double dryThrust = 79000.0 * Math.pow(clamp(s.throttle, 0.0, 1.0), 1.18);
        double abExtra = 52000.0 * o.afterburner01;
        double ramLoss = 1.0 - 0.10 * smooth(0.95, 2.0, o.mach);
        o.thrustN = (s.fuelKg > 0.01 ? 1.0 : 0.0) * (dryThrust + abExtra) * lapse * ramLoss;
        o.fuelFlowKgSec = fuelFlowKgSec(s.throttle, o.afterburner01);

        double vEff = Math.max(35.0, v);
        double pHat = Math.toRadians(s.rollRateDegSec) * WING_SPAN_M / (2.0 * vEff);
        double qHat = Math.toRadians(s.pitchRateDegSec) * MEAN_CHORD_M / (2.0 * vEff);
        double rHat = Math.toRadians(s.yawRateDegSec) * WING_SPAN_M / (2.0 * vEff);
        double staticMargin = clamp(NEUTRAL_POINT_MAC - cg, 0.015, 0.12);

        double clRoll = 0.0125 * fcs.effectiveRoll - 0.18 * pHat;
        double cm = 0.038 * fcs.effectivePitch - 0.0075 * o.aoaDeg * (staticMargin / 0.075) - 0.65 * qHat;
        // Roll command creates an opposite yawing moment; this is the adverse-yaw source.
        double cnAdverse = -0.0045 * fcs.effectiveRoll;
        double cn = 0.0125 * fcs.effectiveYaw - 0.00060 * o.betaDeg + cnAdverse - 0.22 * rHat;

        o.rollMomentNm = o.dynamicPressurePa * WING_AREA_M2 * WING_SPAN_M * clRoll;
        o.pitchMomentNm = o.dynamicPressurePa * WING_AREA_M2 * MEAN_CHORD_M * cm;
        o.adverseYawMomentNm = o.dynamicPressurePa * WING_AREA_M2 * WING_SPAN_M * cnAdverse;
        o.yawMomentNm = o.dynamicPressurePa * WING_AREA_M2 * WING_SPAN_M * cn;

        double rotationalCue = Math.abs(s.yawRateDegSec) + 0.28 * Math.abs(s.rollRateDegSec);
        o.spin01 = o.stall01 * smooth(4.0, 13.0, Math.abs(o.betaDeg)) * smooth(12.0, 75.0, rotationalCue);
        o.loadFactor = o.liftN / Math.max(1.0, mass * GRAVITY_MPS2);
        return o;
    }

    public static void updateMassAndFuel(FlightState s, double dtSec) {
        double dt = clamp(dtSec, 0.0, 0.05);
        if (s.fuelKg < 0.0 || s.fuelKg > INITIAL_FUEL_KG * 1.05) s.fuelKg = INITIAL_FUEL_KG;
        double ab = smooth(0.80, 0.97, clamp(s.throttle, 0.0, 1.0));
        s.fuelKg = Math.max(0.0, s.fuelKg - fuelFlowKgSec(s.throttle, ab) * dt);
        double ff = clamp(s.fuelKg / INITIAL_FUEL_KG, 0.0, 1.0);
        s.fuelFraction01 = ff;
        s.massKg = DRY_MASS_KG + s.fuelKg;
        s.cgMac = EMPTY_CG_MAC + ff * (FULL_FUEL_CG_MAC - EMPTY_CG_MAC);
        double massRatio = s.massKg / (DRY_MASS_KG + INITIAL_FUEL_KG);
        // Fuel is concentrated near the fuselage, so pitch/yaw inertia fall faster than roll inertia.
        s.inertiaRollKgM2 = 15500.0 * (0.86 + 0.14 * massRatio);
        s.inertiaPitchKgM2 = 50500.0 * (0.76 + 0.24 * massRatio);
        s.inertiaYawKgM2 = 62500.0 * (0.77 + 0.23 * massRatio);
    }

    public static double fuelFlowKgSec(double throttle, double afterburner01) {
        double t = clamp(throttle, 0.0, 1.0);
        return 0.055 + 0.72 * Math.pow(t, 1.65) + 1.55 * clamp(afterburner01, 0.0, 1.0);
    }

    public static double airDensityKgM3(double altitudeM) {
        double h = clamp(altitudeM, 0.0, 22000.0);
        if (h <= 11000.0) {
            double t = 288.15 - 0.0065 * h;
            double p = 101325.0 * Math.pow(t / 288.15, 5.2558797);
            return p / (287.05 * t);
        }
        double t = 216.65;
        double p11 = 22632.06;
        double p = p11 * Math.exp(-GRAVITY_MPS2 * (h - 11000.0) / (287.05 * t));
        return p / (287.05 * t);
    }

    public static double speedOfSoundMps(double altitudeM) {
        double h = clamp(altitudeM, 0.0, 22000.0);
        double t = h <= 11000.0 ? 288.15 - 0.0065 * h : 216.65;
        return Math.sqrt(1.4 * 287.05 * t);
    }

    public static double dynamicPressurePa(double altitudeM, double speedMps) {
        double v = Math.max(0.0, speedMps);
        return 0.5 * airDensityKgM3(altitudeM) * v * v;
    }

    public static double liftCoefficientAtAoA(double aoaDeg) {
        return interpolate(AOA_DEG, CL_TABLE, clamp(aoaDeg, AOA_DEG[0], AOA_DEG[AOA_DEG.length-1]));
    }

    public static double baseDragCoefficientAtAoA(double aoaDeg) {
        return interpolate(AOA_DEG, CD0_TABLE, clamp(aoaDeg, AOA_DEG[0], AOA_DEG[AOA_DEG.length-1]));
    }

    private static boolean validCg(double v) { return v > 0.20 && v < 0.50; }
    private static double interpolate(double[] x, double[] y, double v) {
        if (v <= x[0]) return y[0];
        for (int i=1;i<x.length;i++) {
            if (v <= x[i]) {
                double t=(v-x[i-1])/(x[i]-x[i-1]);
                return y[i-1]+(y[i]-y[i-1])*t;
            }
        }
        return y[y.length-1];
    }
    private static double smooth(double a,double b,double x){
        if(b<=a)return x>=b?1:0;
        double t=clamp((x-a)/(b-a),0,1);return t*t*(3-2*t);
    }
    private static double sq(double v){return v*v;}
    private static double wrap180(double d){d%=360.0;if(d>180)d-=360;if(d<-180)d+=360;return d;}
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
