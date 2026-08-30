package com.mg.fixturecockpitsim.sim;

/** Deterministic mobile-friendly flight model with a lightweight atmosphere and flight envelope. */
public final class FlightDynamicsEngine {
    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final double SEA_LEVEL_DENSITY = 1.225;
    private static final double GEAR_RATE_PER_SEC = 0.55;
    private static final double GROUND_HEIGHT_M = 0.0;
    private static final double CLEAN_STALL_IAS_MPS = 58.0;

    public void step(FlightState s, FlightControls in, double dtSec) {
        if (dtSec <= 0) return;
        dtSec = Math.min(dtSec, 0.05);
        in.clamp();

        double gearTarget = in.gearDown ? 1.0 : 0.0;
        s.gearPosition = approach(s.gearPosition, gearTarget, GEAR_RATE_PER_SEC * dtSec);
        s.brake01 += (in.brake - s.brake01) * Math.min(1.0, dtSec * 7.0);

        updateAtmosphere(s);

        // Normal commands stay in a conventional bank envelope. Near-full stick enters the
        // aerobatic region so the existing full-roll / inverted-flight mode remains available.
        double rollInput=clamp(in.roll,-1.0,1.0);
        double absRoll=Math.abs(rollInput);
        double targetRoll;
        if(absRoll<=0.78) targetRoll=rollInput*96.15;
        else {
            double t=(absRoll-0.78)/0.22;
            targetRoll=Math.copySign(75.0+t*110.0,rollInput);
        }
        double targetPitch = in.pitch * 30.0;
        double rollRate = absRoll>0.78 ? 4.2 : 3.2;
        s.rollDeg += (targetRoll - s.rollDeg) * Math.min(1.0, dtSec * rollRate);
        s.pitchDeg += (targetPitch - s.pitchDeg) * Math.min(1.0, dtSec * 2.4);

        // Small deterministic gust component: enough to make the aircraft feel alive without
        // making demo/autopilot operation random or device dependent.
        double gustYaw = s.onGround ? 0.0 : (s.windEastMps-s.windNorthMps)*0.035*s.turbulence01;
        s.headingDeg = wrap360(s.headingDeg + (Math.sin(Math.toRadians(s.rollDeg))*28.0 + in.yaw*18.0 + gustYaw) * dtSec);

        s.throttle += (in.throttle - s.throttle) * Math.min(1.0, dtSec * 2.0);

        // AoA and load are deliberately bounded for a stable mobile model.
        double flightPathDeg=Math.toDegrees(Math.atan2(s.verticalSpeedMps,Math.max(25.0,s.trueAirspeedMps)));
        s.angleOfAttackDeg=clamp(s.pitchDeg-flightPathDeg+in.pitch*3.2,-28.0,28.0);
        double bankCos=Math.max(0.18,Math.abs(Math.cos(Math.toRadians(s.rollDeg))));
        double geometricLoad=1.0/bankCos;
        s.loadFactor=clamp(geometricLoad,0.1,9.0);

        double stallIas=CLEAN_STALL_IAS_MPS*Math.sqrt(Math.max(1.0,Math.min(4.0,s.loadFactor)));
        // Extended gear approximates approach configuration and slightly lowers reference stall speed.
        stallIas*=1.0-0.08*s.gearPosition;
        s.stallMargin01=clamp((s.indicatedAirspeedMps-stallIas)/38.0,0.0,1.0);
        double alphaSeverity=clamp((Math.abs(s.angleOfAttackDeg)-15.0)/9.0,0.0,1.0);
        double lowSpeedSeverity=1.0-s.stallMargin01;
        double stallSeverity=Math.max(alphaSeverity,lowSpeedSeverity);
        s.stallWarning=!s.onGround && stallSeverity>0.58;

        // Preserve the established throttle-to-speed feel while adding configuration and AoA drag.
        double baseTargetSpeed=s.onGround?s.throttle*125.0:55.0+s.throttle*250.0;
        double gearDrag=s.onGround?0.0:s.gearPosition*24.0;
        double alphaDrag=s.onGround?0.0:Math.max(0.0,Math.abs(s.angleOfAttackDeg)-4.0)*0.85;
        double inducedDrag=s.onGround?0.0:Math.max(0.0,s.loadFactor-1.0)*5.0;
        double targetSpeed=Math.max(s.onGround?0.0:38.0,baseTargetSpeed-gearDrag-alphaDrag-inducedDrag);
        double speedResponse=s.onGround?0.72:0.52;
        s.trueAirspeedMps += (targetSpeed-s.trueAirspeedMps)*Math.min(1.0,dtSec*speedResponse);
        if(s.onGround && s.brake01>0.01) s.trueAirspeedMps=Math.max(0.0,s.trueAirspeedMps-(0.5+8.8*s.brake01)*dtSec);

        updateAtmosphere(s); // IAS/Mach/q after speed response.
        stallIas=CLEAN_STALL_IAS_MPS*Math.sqrt(Math.max(1.0,Math.min(4.0,s.loadFactor)))*(1.0-0.08*s.gearPosition);
        s.stallMargin01=clamp((s.indicatedAirspeedMps-stallIas)/38.0,0.0,1.0);
        lowSpeedSeverity=1.0-s.stallMargin01;
        stallSeverity=Math.max(alphaSeverity,lowSpeedSeverity);
        s.stallWarning=!s.onGround && stallSeverity>0.58;
        s.overspeedWarning=!s.onGround && (s.indicatedAirspeedMps>292.0 || s.mach>0.92);
        s.gearWarning=!s.onGround && s.altitudeM<180.0 && s.verticalSpeedMps<-1.5 && s.gearPosition<0.82;

        // Stall progressively removes vertical authority and adds sink instead of switching to an
        // abrupt scripted fall. The absolute bank cosine keeps intentional inverted flight usable.
        double bankLift=Math.max(0.22,Math.abs(Math.cos(Math.toRadians(s.rollDeg))));
        double liftRetention=1.0-0.72*stallSeverity;
        double airborneVs=s.trueAirspeedMps*Math.sin(Math.toRadians(s.pitchDeg))*bankLift*liftRetention;
        airborneVs-=stallSeverity*16.0;
        double proposedAltitude=s.altitudeM+airborneVs*dtSec;
        boolean gearUsable=s.gearPosition>0.82;
        boolean groundCandidate=proposedAltitude<=GROUND_HEIGHT_M+0.12 && airborneVs<=1.0;

        if (groundCandidate && gearUsable) {
            if (!s.onGround) s.touchdownSinkMps=Math.max(0.0,-airborneVs);
            s.onGround=true;
            s.altitudeM=GROUND_HEIGHT_M;
            s.verticalSpeedMps=0.0;
            double touchdownLoad=clamp01(s.touchdownSinkMps/4.5);
            double speedLoad=clamp01(s.trueAirspeedMps/95.0)*0.18;
            double targetMainCompression=clamp01(0.16+touchdownLoad*0.72+speedLoad);
            double targetNoseCompression=clamp01(0.10+Math.max(0.0,-s.pitchDeg)/12.0*0.48);
            s.mainStrutCompression01+=(targetMainCompression-s.mainStrutCompression01)*Math.min(1.0,dtSec*7.5);
            s.noseStrutCompression01+=(targetNoseCompression-s.noseStrutCompression01)*Math.min(1.0,dtSec*6.0);
            double rollingDecel=0.55+9.0*s.brake01;
            s.trueAirspeedMps=Math.max(0.0,s.trueAirspeedMps-rollingDecel*dtSec);
            double steerAuthority=20.0*clamp01(1.0-s.trueAirspeedMps/85.0);
            s.headingDeg=wrap360(s.headingDeg+in.yaw*steerAuthority*dtSec);
            s.rollDeg+=(0.0-s.rollDeg)*Math.min(1.0,dtSec*2.8);
            s.pitchDeg+=(0.0-s.pitchDeg)*Math.min(1.0,dtSec*1.4);
        } else {
            s.onGround=false;
            s.verticalSpeedMps=airborneVs;
            s.altitudeM=Math.max(0.0,proposedAltitude);
            s.mainStrutCompression01+=(0.0-s.mainStrutCompression01)*Math.min(1.0,dtSec*5.0);
            s.noseStrutCompression01+=(0.0-s.noseStrutCompression01)*Math.min(1.0,dtSec*5.0);
            if(s.altitudeM>1.0)s.touchdownSinkMps=0.0;
        }

        // Wind affects ground track, not true airspeed.
        double groundSpeed=s.trueAirspeedMps*Math.cos(Math.toRadians(s.pitchDeg));
        double hdg=Math.toRadians(s.headingDeg);
        double north=Math.cos(hdg)*groundSpeed + (s.onGround?0.0:s.windNorthMps);
        double east=Math.sin(hdg)*groundSpeed + (s.onGround?0.0:s.windEastMps);
        s.latitudeDeg+=Math.toDegrees(north*dtSec/EARTH_RADIUS_M);
        double cosLat=Math.max(0.15,Math.cos(Math.toRadians(s.latitudeDeg)));
        s.longitudeDeg+=Math.toDegrees(east*dtSec/(EARTH_RADIUS_M*cosLat));
        s.timeSec+=dtSec;
    }

    private static void updateAtmosphere(FlightState s){
        double h=Math.max(0.0,s.altitudeM);
        s.airDensityKgM3=SEA_LEVEL_DENSITY*Math.exp(-h/8500.0);
        s.indicatedAirspeedMps=Math.max(0.0,s.trueAirspeedMps*Math.sqrt(s.airDensityKgM3/SEA_LEVEL_DENSITY));
        double speedOfSound=clamp(340.3-0.0030*h,295.0,340.3);
        s.mach=s.trueAirspeedMps/speedOfSound;
        s.dynamicPressurePa=0.5*s.airDensityKgM3*s.trueAirspeedMps*s.trueAirspeedMps;
        s.windNorthMps=3.2+2.1*Math.sin(s.timeSec*0.037+h*0.0007);
        s.windEastMps=1.6+2.6*Math.sin(s.timeSec*0.051+1.3+h*0.0004);
        s.turbulence01=clamp(0.18+0.10*Math.sin(s.timeSec*0.19)+0.06*Math.sin(s.timeSec*0.47+0.8),0.04,0.42);
    }
    private static double approach(double value,double target,double maxDelta){if(value<target)return Math.min(target,value+maxDelta);return Math.max(target,value-maxDelta);}
    private static double clamp01(double v){return clamp(v,0.0,1.0);}private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    private static double wrap360(double d){d%=360.0;return d<0?d+360.0:d;}
}
