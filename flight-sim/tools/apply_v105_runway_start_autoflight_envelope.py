from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
MISSION=SIM/'AutonomousFlightMission.java'
AIRFIELD=PKG/'AirfieldWorldView.java'
GRADLE=ROOT/'app/build.gradle'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/sim'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v105 anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Autonomous flight envelope: closed-loop bank/pitch targets instead of letting
# a persistent mission command integrate into full rolls or abrupt snap motion.
# ---------------------------------------------------------------------------
(SIM/'AutonomousTurnSmoother.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/**
 * Autonomous attitude-command governor.
 * Keeps demo flight inside a non-aerobatic envelope and rate-limits reversals.
 * Manual/BT pilot inputs are not routed through this class.
 */
public final class AutonomousTurnSmoother {
    private double rollOut, pitchOut, yawOut;

    public void reset(){rollOut=pitchOut=yawOut=0;}

    public void apply(FlightState s,FlightControls c,double dt){
        dt=clamp(dt,.001,.08);
        if(s.onGround){rollOut=0;pitchOut=0;yawOut=c.yaw;c.roll=0;c.pitch=0;return;}

        // Mission commands become desired attitudes. This prevents a constant
        // roll stick command from accumulating into a 180/360 degree roll.
        double desiredBank=clamp(c.roll*92.0,-34.0,34.0);
        double desiredPitch=clamp(c.pitch*42.0,-18.0,20.0);

        // If the aircraft is already outside the normal demo envelope, force a
        // smooth recovery toward wings-level / moderate pitch before following
        // any new mission command.
        if(Math.abs(s.rollDeg)>46.0)desiredBank=0.0;
        if(s.pitchDeg>25.0)desiredPitch=8.0;
        if(s.pitchDeg<-24.0)desiredPitch=-6.0;

        double bankErr=desiredBank-s.rollDeg;
        double pitchErr=desiredPitch-s.pitchDeg;
        double desiredRoll=clamp(bankErr/30.0-s.rollRateDegSec/95.0,-.38,.38);
        double desiredPitchCmd=clamp(pitchErr/24.0-s.pitchRateDegSec/75.0,-.34,.34);

        // Rudder is only a small coordination / damping term in autonomous
        // flight. Bank remains the primary turn mechanism.
        double desiredYaw=clamp(c.yaw*.24-s.yawRateDegSec/120.0-s.sideslipDeg/48.0,-.14,.14);

        // Never snap directly from one bank direction into the other.
        if(rollOut*desiredRoll<0&&Math.abs(rollOut)>.025)desiredRoll=0;
        if(pitchOut*desiredPitchCmd<0&&Math.abs(pitchOut)>.025)desiredPitchCmd=0;
        if(yawOut*desiredYaw<0&&Math.abs(yawOut)>.020)desiredYaw=0;

        rollOut=approach(rollOut,desiredRoll,.48*dt);
        pitchOut=approach(pitchOut,desiredPitchCmd,.42*dt);
        yawOut=approach(yawOut,desiredYaw,.38*dt);

        c.roll=rollOut;c.pitch=pitchOut;c.yaw=yawOut;
    }

    public double getRollOut(){return rollOut;}
    public double getPitchOut(){return pitchOut;}
    public double getYawOut(){return yawOut;}
    private static double approach(double v,double t,double d){if(v<t)return Math.min(t,v+d);return Math.max(t,v-d);}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

TEST.mkdir(parents=True,exist_ok=True)
(TEST/'AutonomousTurnSmootherTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutonomousTurnSmootherTest {
    @Test public void reversalPassesThroughZeroInsteadOfSnapping(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;
        FlightControls c=new FlightControls();
        for(int i=0;i<50;i++){c.roll=.32;c.pitch=.08;c.yaw=.16;s.rollDeg=18;s.rollRateDegSec=4;s.pitchDeg=4;s.pitchRateDegSec=1;sm.apply(s,c,.05);}
        double before=sm.getRollOut();assertTrue(before>0);
        c.roll=-.32;c.pitch=-.08;c.yaw=-.16;s.rollRateDegSec=10;sm.apply(s,c,.05);
        assertTrue(sm.getRollOut()>=0);
    }
    @Test public void excessiveBankCommandsRecoveryNotMoreRoll(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;s.rollDeg=62;s.rollRateDegSec=18;
        FlightControls c=new FlightControls();c.roll=.8;c.pitch=.2;c.yaw=.4;
        for(int i=0;i<20;i++)sm.apply(s,c,.05);
        assertTrue(sm.getRollOut()<0);
    }
    @Test public void pitchIsRateLimitedAndNonAerobatic(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;s.pitchDeg=30;s.pitchRateDegSec=20;
        FlightControls c=new FlightControls();c.pitch=1;c.roll=0;c.yaw=0;sm.apply(s,c,.05);
        assertTrue(sm.getPitchOut()<0);
        assertTrue(Math.abs(sm.getPitchOut())<=.0211);
    }
    @Test public void oneFrameDeltasAreSmall(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;FlightControls c=new FlightControls();c.roll=.8;c.pitch=.8;c.yaw=.8;
        sm.apply(s,c,.05);assertTrue(Math.abs(sm.getRollOut())<=.025);assertTrue(Math.abs(sm.getPitchOut())<=.022);assertTrue(Math.abs(sm.getYawOut())<=.020);
    }
}
''')

# ---------------------------------------------------------------------------
# Mission starts on the main runway. Taxi-out / taxi-in are removed from the
# autonomous presentation. Orbit heading targets become continuous rather than
# abrupt step changes, so turns visibly flow into the next flight segment.
# ---------------------------------------------------------------------------
m=MISSION.read_text()
m=rep(m,
'        phase=Phase.TAXI_OUT;phaseTime=orbitTime=0;turnSmoother.reset();\n',
'        phase=Phase.RUNWAY_HOLD;phaseTime=orbitTime=0;turnSmoother.reset();\n',
'runway start phase')
old_orbit='''            case ORBIT:{
                orbitTime+=dt;c.gearDown=false;
                double t=orbitTime,ta=820,th=285,b=.08,tr=.70;
                if(t<22){ta=900;th=285;b=.08;tr=.72;}
                else if(t<44){ta=760;th=335;b=.14;tr=.69;}
                else if(t<66){ta=870;th=35;b=-.10;tr=.70;}
                else if(t<84){ta=720;th=100;b=.12;tr=.67;}
                else{ta=650;th=RUNWAY_HEADING_DEG;b=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.35;tr=.58;}
                c.throttle=tr;c.pitch=altitudePitch(s.altitudeM,ta,.20);
                c.roll=clamp(b+headingRoll(s.headingDeg,th)*.46,-.30,.30);
                c.yaw=clamp(headingError(s.headingDeg,th)*.011,-.20,.20);
                if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;
            }
'''
new_orbit='''            case ORBIT:{
                orbitTime+=dt;c.gearDown=false;
                double t=orbitTime;
                // One broad, continuous scenic path: no discrete heading jumps.
                double ta=810+105*Math.sin(t*.055)+55*Math.sin(t*.021+.8);
                double th=wrap360(286+24*Math.sin(t*.040)+10*Math.sin(t*.017+.4));
                double tr=.68+.025*Math.sin(t*.045);
                if(t>88){double blend=clamp((t-88)/17.0,0,1);th=blendHeading(th,RUNWAY_HEADING_DEG,blend);ta=ta*(1-blend)+650*blend;tr=tr*(1-blend)+.58*blend;}
                c.throttle=tr;c.pitch=altitudePitch(s.altitudeM,ta,.18);
                c.roll=clamp(headingRoll(s.headingDeg,th)*.42,-.24,.24);
                c.yaw=clamp(headingError(s.headingDeg,th)*.008,-.12,.12);
                if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;
            }
'''
m=rep(m,old_orbit,new_orbit,'continuous orbit')
m=rep(m,
'                if(s.trueAirspeedMps<2&&phaseTime>1.5)next(Phase.TAXI_IN);break;\n',
'                if(s.trueAirspeedMps<2&&phaseTime>1.5)reset(s);break;\n',
'no taxi-in after rollout')
m=rep(m,
'    private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}\n',
'    private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}\n    private static double wrap360(double d){while(d>=360)d-=360;while(d<0)d+=360;return d;}\n    private static double blendHeading(double a,double b,double t){return wrap360(a+headingError(a,b)*clamp(t,0,1));}\n',
'heading helpers')
MISSION.write_text(m)

# Taxiway is deliberately absent from the cinematic environment.
a=AIRFIELD.read_text()
a=rep(a,
'    private boolean taxiPhase(){return onGround&&(phase.contains("TAXI_OUT")||phase.contains("TAXI_IN"));}\n',
'    private boolean taxiPhase(){return false;}\n',
'taxiway removal')
AIRFIELD.write_text(a)

# Version.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 105',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.23-avm32.0-runway-start-autoflight-envelope'",g,count=1)
GRADLE.write_text(g)
print('v105 applied: main-runway start, no taxiway, non-aerobatic autonomous envelope, continuous scenic turns')
