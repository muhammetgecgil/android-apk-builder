from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
AIRFIELD=PKG/'AirfieldWorldView.java'
MISSION=SIM/'AutonomousFlightMission.java'
JET=PKG/'Jet3DView.java'
TURN=SIM/'AutonomousTurnSmoother.java'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/sim'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v105 patch anchor missing: {label}')
    return text.replace(old,new,1)

# 1) Start the autonomous demo already lined up on the main runway.
m=MISSION.read_text()
m=rep(m,
'        phase=Phase.TAXI_OUT;phaseTime=orbitTime=0;turnSmoother.reset();\n',
'        phase=Phase.RUNWAY_HOLD;phaseTime=orbitTime=0;turnSmoother.reset();\n',
'runway start phase')
# After landing, stop on the runway instead of entering a taxi-in sequence.
m=rep(m,
'                if(s.trueAirspeedMps<2&&phaseTime>1.5)next(Phase.TAXI_IN);break;\n',
'                if(s.trueAirspeedMps<2&&phaseTime>1.5)next(Phase.COMPLETE);break;\n',
'rollout stop on runway')
MISSION.write_text(m)

# 2) Remove the taxiway from the visual sequence. The method is intentionally
# kept in source for compatibility, but it is never drawn in v105.
a=AIRFIELD.read_text()
a=rep(a,
'        if(taxiPhase())drawTaxiway(c,w,h,hz);\n',
'        // v105: taxiway intentionally omitted; the aircraft starts and finishes on the main runway.\n',
'remove taxiway draw')
AIRFIELD.write_text(a)

# 3) Strengthen autonomous turn conditioning. This affects DEMO AUTO only;
# manual IMU / Bluetooth commands do not pass through this conditioner.
TURN.write_text(r'''package com.mg.fixturecockpitsim.sim;

/**
 * v105 autonomous turn envelope.
 * Prevents snap reversals, excessive bank build-up and yaw/roll double chasing.
 */
public final class AutonomousTurnSmoother {
    private double rollOut, yawOut;

    public void reset(){rollOut=0;yawOut=0;}

    public void apply(FlightState s,FlightControls c,double dt){
        dt=clamp(dt,.001,.08);
        if(s.onGround){rollOut=0;yawOut=clamp(c.yaw,-.22,.22);c.roll=0;c.yaw=yawOut;return;}

        // Conservative visual/flight envelope for the autonomous cinematic run.
        double desiredRoll=clamp(c.roll*.78,-.28,.28);
        double desiredYaw=clamp(c.yaw*.32-clamp(s.yawRateDegSec/120.0,-.08,.08),-.13,.13);
        if(Math.abs(desiredRoll)<.015)desiredRoll=0;
        if(Math.abs(desiredYaw)<.010)desiredYaw=0;

        // Never keep feeding the same bank direction once the aircraft is already
        // steeply banked. Gently unload instead of allowing a roll-over/tumble look.
        if(Math.abs(s.rollDeg)>32.0 && Math.signum(desiredRoll)==Math.signum(s.rollDeg))desiredRoll=0;
        if(Math.abs(s.rollDeg)>38.0)desiredRoll=-Math.copySign(.10,s.rollDeg);

        // Direction changes must pass through near-level command first.
        if(rollOut*desiredRoll<0&&Math.abs(rollOut)>.025)desiredRoll=0;
        if(yawOut*desiredYaw<0&&Math.abs(yawOut)>.020)desiredYaw=0;
        if(desiredYaw*s.yawRateDegSec<0&&Math.abs(s.yawRateDegSec)>6.0)desiredYaw=0;

        // Lower slew rates than v103 remove one-frame cinematic snaps.
        rollOut=approach(rollOut,desiredRoll,.42*dt);
        yawOut=approach(yawOut,desiredYaw,.50*dt);
        c.roll=rollOut;c.yaw=yawOut;
    }

    public double getRollOut(){return rollOut;}
    public double getYawOut(){return yawOut;}
    private static double approach(double v,double t,double d){if(v<t)return Math.min(t,v+d);return Math.max(t,v-d);}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

# 4) The old renderer yawed the aircraft model against runway heading. At the
# +/-180 wrap this could visually jump from one side to the other, looking like
# an abrupt right-hand spin around the aircraft. Chase view should stay attached
# to the aircraft, so remove this absolute-heading model rotation; bank/pitch
# still show the actual turn naturally.
j=JET.read_text()
old='Matrix.rotateM(md,0,-runwayRelativeYaw*.12f,0,1,0);'
if old not in j:
    raise SystemExit('v105 patch anchor missing: absolute-heading visual yaw')
j=j.replace(old,'/* v105: no absolute-heading airframe yaw; prevents wrap-around spin */',1)
JET.write_text(j)

TEST.mkdir(parents=True,exist_ok=True)
(TEST/'AutonomousRunwayStartV105Test.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutonomousRunwayStartV105Test {
    @Test public void resetStartsOnMainRunwayHold(){
        AutonomousFlightMission m=new AutonomousFlightMission();FlightState s=new FlightState();m.reset(s);
        assertEquals(AutonomousFlightMission.Phase.RUNWAY_HOLD,m.getPhase());
        assertTrue(s.onGround);assertEquals(0.0,s.trueAirspeedMps,0.0001);
        assertEquals(AutonomousFlightMission.RUNWAY_HEADING_DEG,s.headingDeg,0.0001);
    }

    @Test public void autoTurnEnvelopeCannotCommandTumble(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;s.rollDeg=0;s.yawRateDegSec=0;
        FlightControls c=new FlightControls();c.roll=1;c.yaw=1;
        for(int i=0;i<100;i++){c.roll=1;c.yaw=1;sm.apply(s,c,.05);}
        assertTrue(Math.abs(sm.getRollOut())<=.281);
        assertTrue(Math.abs(sm.getYawOut())<=.131);
    }

    @Test public void excessiveBankIsActivelyUnloaded(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;s.rollDeg=44;s.yawRateDegSec=0;
        FlightControls c=new FlightControls();c.roll=1;c.yaw=0;
        for(int i=0;i<20;i++){c.roll=1;sm.apply(s,c,.05);}
        assertTrue(sm.getRollOut()<0);
    }
}
''')

# 5) Version bump after v104.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s*(?:=)?\s*\d+','versionCode 105',g,count=1)
g=re.sub(r'''versionName\s*(?:=)?\s*['\"][^'\"]+['\"]''',"versionName '26.23-avm31.2-runway-start-smooth-auto'",g,count=1)
GRADLE.write_text(g)

print('v105 applied: main-runway start, taxiway removed, autonomous spin/tumble suppression')
