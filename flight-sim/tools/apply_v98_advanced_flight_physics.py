from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
STATE=SIM/'FlightState.java'
DYN=SIM/'FlightDynamicsEngine.java'
FCS=SIM/'FighterFlightControlSystem.java'
MISSION=SIM/'AutonomousFlightMission.java'
RUNTIME=PKG/'FlightRuntimeActivity.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v98 advanced-physics patch anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# State: one shared source of truth for aerodynamic/rigid-body/mass properties.
# ---------------------------------------------------------------------------
STATE.write_text(r'''package com.mg.fixturecockpitsim.sim;

/** Mutable simulation state in SI units where practical. */
public final class FlightState {
    public double timeSec;
    public double latitudeDeg = 40.0;
    public double longitudeDeg = 29.0;
    public double altitudeM = 1500.0;
    public double trueAirspeedMps = 120.0;
    public double verticalSpeedMps;
    public double headingDeg;
    public double pitchDeg;
    public double rollDeg;
    public double throttle = 0.65;

    // AVM-27 aerodynamic state.
    public double angleOfAttackDeg;
    public double sideslipDeg;
    public double dynamicPressurePa;
    public double liftCoefficient;
    public double dragCoefficient;
    public double liftN;
    public double dragN;
    public double thrustN;
    public double loadFactor = 1.0;
    public double stall01;
    public double spin01;
    public double groundEffect01;
    public double adverseYawMomentNm;

    // AVM-27 rigid-body rates / mass properties.
    public double rollRateDegSec;
    public double pitchRateDegSec;
    public double yawRateDegSec;
    public double fuelKg = AdvancedFlightPhysicsModel.INITIAL_FUEL_KG;
    public double fuelFraction01 = 1.0;
    public double massKg = AdvancedFlightPhysicsModel.DRY_MASS_KG + AdvancedFlightPhysicsModel.INITIAL_FUEL_KG;
    public double cgMac = AdvancedFlightPhysicsModel.FULL_FUEL_CG_MAC;
    public double inertiaRollKgM2 = 15500.0;
    public double inertiaPitchKgM2 = 50500.0;
    public double inertiaYawKgM2 = 62500.0;

    // Compressibility / supersonic state retained for visual and audio systems.
    public double speedOfSoundMps = 340.3;
    public double mach;
    public double waveDrag01;
    public double transonicBuffet01;
    public double shockStrength01;
    public double sonicBoomPulse;

    // Fighter flight-control actuator state.
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
    public double controlAuthority01 = 1.0;

    // Ground / landing system state.
    public double gearPosition = 1.0;
    public boolean onGround;
    public double mainStrutCompression01;
    public double noseStrutCompression01;
    public double brake01;
    public double touchdownSinkMps;

    public FlightState copy() {
        FlightState c = new FlightState();
        c.timeSec=timeSec;c.latitudeDeg=latitudeDeg;c.longitudeDeg=longitudeDeg;
        c.altitudeM=altitudeM;c.trueAirspeedMps=trueAirspeedMps;c.verticalSpeedMps=verticalSpeedMps;
        c.headingDeg=headingDeg;c.pitchDeg=pitchDeg;c.rollDeg=rollDeg;c.throttle=throttle;
        c.angleOfAttackDeg=angleOfAttackDeg;c.sideslipDeg=sideslipDeg;c.dynamicPressurePa=dynamicPressurePa;
        c.liftCoefficient=liftCoefficient;c.dragCoefficient=dragCoefficient;c.liftN=liftN;c.dragN=dragN;c.thrustN=thrustN;
        c.loadFactor=loadFactor;c.stall01=stall01;c.spin01=spin01;c.groundEffect01=groundEffect01;c.adverseYawMomentNm=adverseYawMomentNm;
        c.rollRateDegSec=rollRateDegSec;c.pitchRateDegSec=pitchRateDegSec;c.yawRateDegSec=yawRateDegSec;
        c.fuelKg=fuelKg;c.fuelFraction01=fuelFraction01;c.massKg=massKg;c.cgMac=cgMac;
        c.inertiaRollKgM2=inertiaRollKgM2;c.inertiaPitchKgM2=inertiaPitchKgM2;c.inertiaYawKgM2=inertiaYawKgM2;
        c.speedOfSoundMps=speedOfSoundMps;c.mach=mach;c.waveDrag01=waveDrag01;
        c.transonicBuffet01=transonicBuffet01;c.shockStrength01=shockStrength01;c.sonicBoomPulse=sonicBoomPulse;
        c.leftStabilatorDeg=leftStabilatorDeg;c.rightStabilatorDeg=rightStabilatorDeg;
        c.leftFlaperonDeg=leftFlaperonDeg;c.rightFlaperonDeg=rightFlaperonDeg;
        c.leftRudderDeg=leftRudderDeg;c.rightRudderDeg=rightRudderDeg;
        c.leftLeadingEdgeFlapDeg=leftLeadingEdgeFlapDeg;c.rightLeadingEdgeFlapDeg=rightLeadingEdgeFlapDeg;
        c.speedBrake01=speedBrake01;c.speedBrakeDeg=speedBrakeDeg;c.autoTrim=autoTrim;
        c.yawDamper=yawDamper;c.controlAuthority01=controlAuthority01;
        c.gearPosition=gearPosition;c.onGround=onGround;c.mainStrutCompression01=mainStrutCompression01;
        c.noseStrutCompression01=noseStrutCompression01;c.brake01=brake01;c.touchdownSinkMps=touchdownSinkMps;
        return c;
    }
}
''')

# ---------------------------------------------------------------------------
# Dynamics: force/moment integration rather than target-speed/lift-support logic.
# ---------------------------------------------------------------------------
DYN.write_text(r'''package com.mg.fixturecockpitsim.sim;

import com.mg.fixturecockpitsim.CinematicEnvironmentView;
import com.mg.fixturecockpitsim.WeatherEffectsView;

/** AVM-27 coefficient-table aerodynamic + rigid-body fighter simulation. */
public final class FlightDynamicsEngine {
    private static final double EARTH_RADIUS_M=6371000.0;
    private static final double GEAR_RATE_PER_SEC=.55;
    private static final double GROUND_HEIGHT_M=0.0;
    private static final double G=AdvancedFlightPhysicsModel.GRAVITY_MPS2;

    private final FighterFlightControlSystem fighterFcs=new FighterFlightControlSystem();
    private final AdvancedFlightPhysicsModel physics=new AdvancedFlightPhysicsModel();

    public void step(FlightState s,FlightControls in,double dtSec){
        if(dtSec<=0)return;dtSec=Math.min(.05,dtSec);in.clamp();
        final double previousMach=s.mach;

        final boolean windy=WeatherEffectsView.isSharedWindy();
        final double windStrength=windy?WeatherEffectsView.getSharedWindStrength():0.0;
        final int windSign=WeatherEffectsView.getSharedWindSign();
        final double airWindFactor=s.onGround?.12:1.0;
        final double gustA=Math.sin(s.timeSec*1.73+.35)*.62+Math.sin(s.timeSec*4.91+1.14)*.25+Math.sin(s.timeSec*9.2)*.13;
        final double gustB=Math.sin(s.timeSec*2.31+1.7)*.72+Math.sin(s.timeSec*6.4+.4)*.28;
        final double gustRollAccel=windy?windSign*windStrength*airWindFactor*(6.0+18.0*gustA):0.0;
        final double gustYawAccel=windy?windSign*windStrength*airWindFactor*(3.0+10.0*gustB):0.0;
        final double gustVerticalMps=windy&&!s.onGround?windStrength*(1.25*gustB+.55*gustA):0.0;

        double gearTarget=in.gearDown?1.0:0.0;
        s.gearPosition=approach(s.gearPosition,gearTarget,GEAR_RATE_PER_SEC*dtSec);
        s.brake01+=(in.brake-s.brake01)*Math.min(1.0,dtSec*7.0);
        double throttleResponse=s.onGround?.82:1.75;
        s.throttle+=(in.throttle-s.throttle)*Math.min(1.0,dtSec*throttleResponse);

        AdvancedFlightPhysicsModel.updateMassAndFuel(s,dtSec);
        FighterFlightControlSystem.Output fcs=fighterFcs.update(s,in,dtSec);
        AdvancedFlightPhysicsModel.Output pre=physics.evaluate(s,in,fcs);
        publishAero(s,pre);

        if(s.onGround){
            // Nose-wheel steering dominates yaw on the runway. Aerodynamic pitch authority
            // becomes available during rotation; roll remains constrained by the gear.
            s.rollRateDegSec=approach(s.rollRateDegSec,0,220*dtSec);
            s.rollDeg+=(-s.rollDeg)*Math.min(1.0,dtSec*4.0);
            double pitchAccel=Math.toDegrees(pre.pitchMomentNm/Math.max(1000.0,s.inertiaPitchKgM2));
            if(s.trueAirspeedMps>62&&fcs.effectivePitch>.01){
                s.pitchRateDegSec+=pitchAccel*dtSec;
                s.pitchRateDegSec*=Math.max(0.0,1.0-dtSec*1.4);
            }else{
                s.pitchRateDegSec=approach(s.pitchRateDegSec,0,35*dtSec);
                if(s.trueAirspeedMps<55)s.pitchDeg+=(-s.pitchDeg)*Math.min(1.0,dtSec*2.4);
            }
            s.pitchRateDegSec=clamp(s.pitchRateDegSec,-18,28);
            s.pitchDeg=clamp(s.pitchDeg+s.pitchRateDegSec*dtSec,-2.0,14.0);
            double steerAuthority=26.0*clamp01(1.0-s.trueAirspeedMps/95.0)+5.0;
            s.yawRateDegSec+=(in.yaw*steerAuthority-s.yawRateDegSec)*Math.min(1.0,dtSec*5.0);
            s.headingDeg=wrap360(s.headingDeg+s.yawRateDegSec*dtSec);
            s.sideslipDeg=approach(s.sideslipDeg,0,18*dtSec);
        }else{
            double rollAccel=Math.toDegrees(pre.rollMomentNm/Math.max(1000.0,s.inertiaRollKgM2))+gustRollAccel;
            double pitchAccel=Math.toDegrees(pre.pitchMomentNm/Math.max(1000.0,s.inertiaPitchKgM2));
            double yawAccel=Math.toDegrees(pre.yawMomentNm/Math.max(1000.0,s.inertiaYawKgM2))+gustYawAccel;

            // Deep stall plus beta/yaw can develop into autorotation instead of a cosmetic warning.
            double spinSign=Math.abs(s.sideslipDeg)>1.0?Math.signum(s.sideslipDeg):
                    (Math.abs(s.yawRateDegSec)>.5?Math.signum(s.yawRateDegSec):Math.signum(s.rollRateDegSec+.01));
            pitchAccel-=pre.stall01*(12.0+34.0*pre.stall01);
            rollAccel+=spinSign*92.0*pre.spin01;
            yawAccel+=spinSign*126.0*pre.spin01;

            // Moving transonic shocks produce a real attitude-rate disturbance.
            double b=pre.transonicBuffet01;
            rollAccel+=(Math.sin(s.timeSec*19.1)+.45*Math.sin(s.timeSec*31.7))*b*18.0;
            pitchAccel+=(Math.sin(s.timeSec*23.4+.8)+.35*Math.sin(s.timeSec*37.2))*b*12.0;

            s.rollRateDegSec=clamp(s.rollRateDegSec+rollAccel*dtSec,-220,220);
            s.pitchRateDegSec=clamp(s.pitchRateDegSec+pitchAccel*dtSec,-120,120);
            s.yawRateDegSec=clamp(s.yawRateDegSec+yawAccel*dtSec,-160,160);

            s.rollDeg=wrap180(s.rollDeg+s.rollRateDegSec*dtSec);
            s.pitchDeg=clamp(s.pitchDeg+s.pitchRateDegSec*dtSec,-78,86);
            double coordRate=Math.toDegrees(G*Math.tan(Math.toRadians(clamp(s.rollDeg,-82,82)))/Math.max(35.0,s.trueAirspeedMps));
            s.headingDeg=wrap360(s.headingDeg+(coordRate+.34*s.yawRateDegSec)*dtSec);

            double sideAccel=pre.sideForceN/Math.max(AdvancedFlightPhysicsModel.DRY_MASS_KG,s.massKg);
            double betaDot=-.78*s.sideslipDeg+.24*s.yawRateDegSec-.08*coordRate+
                    Math.toDegrees(sideAccel/Math.max(35.0,s.trueAirspeedMps));
            if(windy)betaDot+=windSign*windStrength*(2.0+2.2*gustB);
            s.sideslipDeg=clamp(s.sideslipDeg+betaDot*dtSec,-30,30);
        }

        // Re-evaluate after the attitude/rate update so AoA, beta, q and moments are coherent.
        AdvancedFlightPhysicsModel.Output aero=physics.evaluate(s,in,fcs);
        publishAero(s,aero);
        double mass=Math.max(AdvancedFlightPhysicsModel.DRY_MASS_KG,s.massKg);
        double alphaRad=Math.toRadians(aero.aoaDeg);

        if(s.onGround){
            double normal=Math.max(0.0,mass*G-Math.max(0.0,aero.liftN));
            double rollingMu=.018+.30*clamp01(s.brake01);
            double rollingResistance=normal*rollingMu;
            double forwardThrust=aero.thrustN*Math.cos(alphaRad);
            double accel=(forwardThrust-aero.dragN-rollingResistance)/mass;
            if(s.trueAirspeedMps<.25&&accel<0)accel=0;
            s.trueAirspeedMps=Math.max(0.0,s.trueAirspeedMps+accel*dtSec);

            double verticalForce=aero.liftN+aero.thrustN*Math.sin(alphaRad)-mass*G;
            double verticalAccel=Math.max(0.0,verticalForce/mass);
            s.verticalSpeedMps=Math.max(0.0,s.verticalSpeedMps+verticalAccel*dtSec);
        }else{
            double gammaRad=Math.toRadians(aero.flightPathDeg);
            double longitudinal=(aero.thrustN*Math.cos(alphaRad)-aero.dragN-mass*G*Math.sin(gammaRad))/mass;
            s.trueAirspeedMps=Math.max(24.0,s.trueAirspeedMps+longitudinal*dtSec);

            double verticalForce=aero.liftN*Math.cos(Math.toRadians(s.rollDeg))+aero.thrustN*Math.sin(alphaRad)-mass*G;
            double verticalAccel=verticalForce/mass;
            s.verticalSpeedMps+=verticalAccel*dtSec+gustVerticalMps*dtSec*1.7;
            s.verticalSpeedMps=clamp(s.verticalSpeedMps,-155,145);
        }

        // Final aerodynamic state at the new velocity drives HUD/FCS/audio on the next frame.
        AdvancedFlightPhysicsModel.Output fin=physics.evaluate(s,in,fcs);
        publishAero(s,fin);
        s.sonicBoomPulse=Math.max(0.0,s.sonicBoomPulse-dtSec*1.30);
        if(!s.onGround&&s.timeSec>.5&&SupersonicFlightModel.crossedMachOne(previousMach,s.mach))s.sonicBoomPulse=1.0;

        double proposedAltitude=s.altitudeM+s.verticalSpeedMps*dtSec;
        boolean gearUsable=s.gearPosition>.82;
        boolean groundCandidate=proposedAltitude<=GROUND_HEIGHT_M+.12&&s.verticalSpeedMps<=1.5;
        if(groundCandidate&&gearUsable){
            if(!s.onGround)s.touchdownSinkMps=Math.max(0.0,-s.verticalSpeedMps);
            s.onGround=true;s.altitudeM=GROUND_HEIGHT_M;s.verticalSpeedMps=0;
            double touchdownLoad=clamp01(s.touchdownSinkMps/4.5);
            double speedLoad=clamp01(s.trueAirspeedMps/95.0)*.18;
            double targetMain=clamp01(.16+touchdownLoad*.72+speedLoad);
            double targetNose=clamp01(.10+Math.max(0.0,-s.pitchDeg)/12.0*.48);
            s.mainStrutCompression01+=(targetMain-s.mainStrutCompression01)*Math.min(1.0,dtSec*7.5);
            s.noseStrutCompression01+=(targetNose-s.noseStrutCompression01)*Math.min(1.0,dtSec*6.0);
            if(s.trueAirspeedMps<55){s.pitchRateDegSec=approach(s.pitchRateDegSec,0,40*dtSec);s.pitchDeg+=(-s.pitchDeg)*Math.min(1.0,dtSec*1.5);}
            s.rollRateDegSec=approach(s.rollRateDegSec,0,180*dtSec);
        }else if(proposedAltitude>GROUND_HEIGHT_M+.12){
            s.onGround=false;s.altitudeM=Math.max(0.0,proposedAltitude);
            s.mainStrutCompression01+=(0-s.mainStrutCompression01)*Math.min(1.0,dtSec*5.0);
            s.noseStrutCompression01+=(0-s.noseStrutCompression01)*Math.min(1.0,dtSec*5.0);
            if(s.altitudeM>1.0)s.touchdownSinkMps=0;
        }else{
            // Gear-up surface contact is left at zero altitude for the runtime crash gate.
            s.altitudeM=0;s.verticalSpeedMps=Math.max(0,s.verticalSpeedMps);
        }

        double horizontal=Math.sqrt(Math.max(0.0,s.trueAirspeedMps*s.trueAirspeedMps-s.verticalSpeedMps*s.verticalSpeedMps));
        double distance=horizontal*dtSec;
        double hdg=Math.toRadians(s.headingDeg);
        double north=Math.cos(hdg)*distance,east=Math.sin(hdg)*distance;
        if(windy&&!s.onGround){
            double crosswind=windSign*(5.0+15.0*windStrength),crossHdg=hdg+Math.PI*.5;
            north+=Math.cos(crossHdg)*crosswind*dtSec;east+=Math.sin(crossHdg)*crosswind*dtSec;
        }
        s.latitudeDeg+=Math.toDegrees(north/EARTH_RADIUS_M);
        double cosLat=Math.max(.15,Math.cos(Math.toRadians(s.latitudeDeg)));
        s.longitudeDeg+=Math.toDegrees(east/(EARTH_RADIUS_M*cosLat));
        s.timeSec+=dtSec;
        CinematicEnvironmentView.setLiveFlightState(s.altitudeM,s.trueAirspeedMps,s.pitchDeg,s.rollDeg,s.headingDeg,s.onGround);
    }

    private static void publishAero(FlightState s,AdvancedFlightPhysicsModel.Output a){
        s.angleOfAttackDeg=a.aoaDeg;s.dynamicPressurePa=a.dynamicPressurePa;s.liftCoefficient=a.cl;s.dragCoefficient=a.cd;
        s.liftN=a.liftN;s.dragN=a.dragN;s.thrustN=a.thrustN;s.loadFactor=clamp(a.loadFactor,-4.5,12.0);
        s.stall01=a.stall01;s.spin01=a.spin01;s.groundEffect01=a.groundEffect01;s.adverseYawMomentNm=a.adverseYawMomentNm;
        s.speedOfSoundMps=a.speedOfSoundMps;s.mach=a.mach;s.waveDrag01=a.waveDrag01;s.transonicBuffet01=a.transonicBuffet01;s.shockStrength01=a.shockStrength01;
    }
    private static double approach(double v,double t,double d){if(v<t)return Math.min(t,v+d);return Math.max(t,v-d);}
    private static double clamp01(double v){return clamp(v,0,1);}
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    private static double wrap360(double d){d%=360;return d<0?d+360:d;}
    private static double wrap180(double d){d%=360;if(d>180)d-=360;if(d<-180)d+=360;return d;}
}
''')

# ---------------------------------------------------------------------------
# FCS authority now schedules from dynamic pressure rather than raw speed alone.
# ---------------------------------------------------------------------------
f=FCS.read_text()
f=rep(f,
'''        final double speed = Math.max(0.0, s.trueAirspeedMps);\n        final double aoa = s.angleOfAttackDeg;\n        final double highSpeed = clamp01((speed - 150.0) / 165.0);\n        final double lowSpeed = 1.0 - clamp01((speed - 65.0) / 115.0);\n''',
'''        final double speed = Math.max(0.0, s.trueAirspeedMps);\n        final double aoa = s.angleOfAttackDeg;\n        final double qPa = s.dynamicPressurePa>1.0?s.dynamicPressurePa:AdvancedFlightPhysicsModel.dynamicPressurePa(s.altitudeM,s.trueAirspeedMps);\n        final double highSpeed = clamp01((qPa - 12000.0) / 42000.0);\n        final double lowSpeed = 1.0 - clamp01((qPa - 3000.0) / 13000.0);\n''',
'dynamic pressure FCS schedule')
FCS.write_text(f)

# Demo/reset refuels the aircraft and clears rigid-body transient states.
m=MISSION.read_text()
m=rep(m,
'        s.gearPosition=1;s.brake01=1;s.onGround=true;\n',
'        s.gearPosition=1;s.brake01=1;s.onGround=true;\n        s.fuelKg=AdvancedFlightPhysicsModel.INITIAL_FUEL_KG;s.fuelFraction01=1;s.massKg=AdvancedFlightPhysicsModel.DRY_MASS_KG+s.fuelKg;s.cgMac=AdvancedFlightPhysicsModel.FULL_FUEL_CG_MAC;\n        s.rollRateDegSec=s.pitchRateDegSec=s.yawRateDegSec=s.sideslipDeg=0;s.stall01=s.spin01=0;AdvancedFlightPhysicsModel.updateMassAndFuel(s,0);\n',
'mission reset mass properties')
MISSION.write_text(m)

# Add two compact engineering telemetry lines below the existing Mach display.
r=RUNTIME.read_text()
if 'FUEL %.0f kg' not in r:
    marker='hud.append(String.format(Locale.US,"\\nMACH %.2f'
    i=r.find(marker)
    if i<0: raise SystemExit('v98 advanced-physics patch anchor missing: Mach HUD append')
    j=r.find('updateButtons();',i)
    if j<0: raise SystemExit('v98 advanced-physics patch anchor missing: HUD updateButtons')
    extra='hud.append(String.format(Locale.US,"\\nAOA %+.1f°   BETA %+.1f°   Q %.1f kPa   G %+.2f   CL %.2f   CD %.3f",state.angleOfAttackDeg,state.sideslipDeg,state.dynamicPressurePa/1000.0,state.loadFactor,state.liftCoefficient,state.dragCoefficient));hud.append(String.format(Locale.US,"\\nSTALL %.0f%%   SPIN %.0f%%   GND FX %.0f%%   FUEL %.0f kg   MASS %.0f kg   CG %.1f%% MAC",state.stall01*100,state.spin01*100,state.groundEffect01*100,state.fuelKg,state.massKg,state.cgMac*100));'
    r=r[:j]+extra+r[j:]
RUNTIME.write_text(r)

# Version bump.
g=GRADLE.read_text()
g=rep(g,'        versionCode 97\n','        versionCode 98\n','version code')
g=rep(g,"        versionName '26.15-avm26.0-jet-blast-ground-effects'\n","        versionName '26.16-avm27.0-advanced-flight-physics'\n",'version name')
GRADLE.write_text(g)

print('v98 advanced flight physics applied: coefficient tables, AoA/beta/Mach/q, G-load, stall/spin, ground effect, adverse yaw, inertia/CG and fuel mass shift')
