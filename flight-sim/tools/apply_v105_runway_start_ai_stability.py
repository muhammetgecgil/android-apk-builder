from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
MISSION=SIM/'AutonomousFlightMission.java'
AIRFIELD=PKG/'AirfieldWorldView.java'
SMOOTHER=SIM/'AutonomousTurnSmoother.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v105 anchor missing: {label}')
    return text.replace(old, new, 1)

# ---------------------------------------------------------------------------
# 1) Start directly on the main runway. Taxi-out / taxi-in are no longer part
#    of the autonomous presentation. The enum is retained for compatibility.
# ---------------------------------------------------------------------------
m=MISSION.read_text()
m=rep(m, '    private Phase phase=Phase.TAXI_OUT;\n', '    private Phase phase=Phase.RUNWAY_HOLD;\n', 'initial phase')
m=rep(m,
      '        phase=Phase.TAXI_OUT;phaseTime=orbitTime=0;turnSmoother.reset();\n',
      '        phase=Phase.RUNWAY_HOLD;phaseTime=orbitTime=0;turnSmoother.reset();\n',
      'reset phase')
m=rep(m,
'''            case HANGAR_START:\n                next(Phase.TAXI_OUT);break;\n\n            case TAXI_OUT:\n                c.throttle=.13;c.gearDown=true;groundLock(c,s,.20);\n                if(s.trueAirspeedMps>9)c.brake=.24;\n                if(phaseTime>=18)next(Phase.RUNWAY_HOLD);break;\n''',
'''            case HANGAR_START:\n                next(Phase.RUNWAY_HOLD);break;\n\n            case TAXI_OUT:\n                // Compatibility-only phase: the v105 presentation starts on the main runway.\n                c.throttle=.08;c.brake=1;c.gearDown=true;groundLock(c,s,.24);\n                next(Phase.RUNWAY_HOLD);break;\n''',
      'taxi-out bypass')
m=rep(m,
'''            case ROLLOUT:\n                c.gearDown=true;c.throttle=0;c.brake=.90;groundLock(c,s,.30);\n                if(s.trueAirspeedMps<2&&phaseTime>1.5)next(Phase.TAXI_IN);break;\n\n            case TAXI_IN:\n                c.gearDown=true;c.throttle=.10;groundLock(c,s,.20);\n                if(s.trueAirspeedMps>8)c.brake=.24;\n                if(phaseTime>=12)next(Phase.HANGAR_PARK);break;\n\n            case HANGAR_PARK:\n                reset(s);break;\n\n            case COMPLETE:\n                c.gearDown=true;c.throttle=0;c.brake=1;groundLock(c,s,.25);break;\n''',
'''            case ROLLOUT:\n                c.gearDown=true;c.throttle=0;c.brake=.90;groundLock(c,s,.30);\n                if(s.trueAirspeedMps<2&&phaseTime>1.5)next(Phase.COMPLETE);break;\n\n            case TAXI_IN:\n                // Kept only for old scenario compatibility; no taxi sequence is shown.\n                c.gearDown=true;c.throttle=0;c.brake=1;groundLock(c,s,.25);next(Phase.COMPLETE);break;\n\n            case HANGAR_PARK:\n                next(Phase.COMPLETE);break;\n\n            case COMPLETE:\n                c.gearDown=true;c.throttle=0;c.brake=1;groundLock(c,s,.25);\n                if(phaseTime>4.0)reset(s);break;\n''',
      'taxi-in removal')

# Replace segmented heading jumps with a continuous scenic S-turn and a smooth
# blend back to runway heading before approach.
old_orbit='''            case ORBIT:{\n                orbitTime+=dt;c.gearDown=false;\n                double t=orbitTime,ta=820,th=285,b=.08,tr=.70;\n                if(t<22){ta=900;th=285;b=.08;tr=.72;}\n                else if(t<44){ta=760;th=335;b=.14;tr=.69;}\n                else if(t<66){ta=870;th=35;b=-.10;tr=.70;}\n                else if(t<84){ta=720;th=100;b=.12;tr=.67;}\n                else{ta=650;th=RUNWAY_HEADING_DEG;b=headingRoll(s.headingDeg,RUNWAY_HEADING_DEG)*.35;tr=.58;}\n                c.throttle=tr;c.pitch=altitudePitch(s.altitudeM,ta,.20);\n                c.roll=clamp(b+headingRoll(s.headingDeg,th)*.46,-.30,.30);\n                c.yaw=clamp(headingError(s.headingDeg,th)*.011,-.20,.20);\n                if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;\n            }\n'''
new_orbit='''            case ORBIT:{\n                orbitTime+=dt;c.gearDown=false;\n                double t=orbitTime;\n                double scenicHdg=285.0+34.0*Math.sin(t*.034);\n                double homeBlend=clamp((t-80.0)/25.0,0,1);\n                double th=blendHeading(scenicHdg,RUNWAY_HEADING_DEG,homeBlend);\n                double ta=(820.0+105.0*Math.sin(t*.041))*(1.0-homeBlend)+650.0*homeBlend;\n                c.throttle=.70*(1.0-homeBlend)+.58*homeBlend;\n                c.pitch=altitudePitch(s.altitudeM,ta,.17);\n                c.roll=clamp(.025*Math.sin(t*.10)+headingRoll(s.headingDeg,th)*.32,-.20,.20);\n                c.yaw=clamp(headingError(s.headingDeg,th)*.0065,-.10,.10);\n                if(orbitTime>=SCENIC_DURATION_SEC)next(Phase.APPROACH);break;\n            }\n'''
m=rep(m, old_orbit, new_orbit, 'continuous orbit')
m=rep(m,
      '    private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}\n',
      '    private static double headingError(double h,double t){double d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}\n    private static double blendHeading(double a,double b,double q){return a+headingError(a,b)*clamp(q,0,1);}\n',
      'heading blend helper')
MISSION.write_text(m)

# ---------------------------------------------------------------------------
# 2) Stronger autonomous stability conditioner. AI flight is limited to a
#    scenic coordinated-turn envelope and actively unloads excess bank.
# ---------------------------------------------------------------------------
SMOOTHER.write_text(r'''package com.mg.fixturecockpitsim.sim;

/** v105 autonomous-flight stability guard: smooth scenic turns, no snap-roll behavior. */
public final class AutonomousTurnSmoother {
    private double rollOut, yawOut;

    public void reset(){rollOut=0;yawOut=0;}

    public void apply(FlightState s,FlightControls c,double dt){
        dt=clamp(dt,.001,.08);
        if(s.onGround){rollOut=0;yawOut=c.yaw;return;}

        double desiredRoll=clamp(c.roll*.66,-.30,.30);
        double desiredYaw=clamp(c.yaw*.28-clamp(s.yawRateDegSec/125.0,-.08,.08),-.10,.10);

        // Scenic AI envelope. Above 28 deg bank, stop adding bank; above 32 deg
        // command a gentle recovery toward wings-level regardless of mission command.
        double absBank=Math.abs(s.rollDeg);
        if(absBank>32.0){desiredRoll=-Math.signum(s.rollDeg)*Math.min(.22,(absBank-28.0)/35.0);}
        else if(absBank>28.0&&Math.signum(desiredRoll)==Math.signum(s.rollDeg))desiredRoll=0;

        if(Math.abs(desiredRoll)<.015)desiredRoll=0;
        if(Math.abs(desiredYaw)<.010)desiredYaw=0;

        // Never snap directly through the opposite bank/yaw command.
        if(rollOut*desiredRoll<0&&Math.abs(rollOut)>.025)desiredRoll=0;
        if(yawOut*desiredYaw<0&&Math.abs(yawOut)>.020)desiredYaw=0;
        if(desiredYaw*s.yawRateDegSec<0&&Math.abs(s.yawRateDegSec)>6.0)desiredYaw=0;

        rollOut=approach(rollOut,desiredRoll,.30*dt);
        yawOut=approach(yawOut,desiredYaw,.42*dt);
        c.roll=rollOut;c.yaw=yawOut;
    }

    public double getRollOut(){return rollOut;}
    public double getYawOut(){return yawOut;}
    private static double approach(double v,double t,double d){if(v<t)return Math.min(t,v+d);return Math.max(t,v-d);}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

# Remove the taxiway from the rendered presentation entirely.
a=AIRFIELD.read_text()
a=rep(a,
      '        if(taxiPhase())drawTaxiway(c,w,h,hz);\n        if(runwayVisible())drawRunway(c,w,h,hz,onGround);\n',
      '        // v105: taxiway intentionally omitted; presentation begins and ends on the main runway.\n        if(runwayVisible())drawRunway(c,w,h,hz,onGround);\n',
      'taxiway render removal')
a=a.replace('phase.contains("TAXI_OUT")||phase.contains("RUNWAY_HOLD")', 'phase.contains("RUNWAY_HOLD")')
AIRFIELD.write_text(a)

# Version metadata.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 105',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]", 'versionName "26.23-avm31.2-runway-start-ai-stability"', g, count=1)
GRADLE.write_text(g)

print('v105 applied: main-runway start, taxiway removed, continuous AI turns, anti-snap stability guard')
