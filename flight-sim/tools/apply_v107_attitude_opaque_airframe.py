from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/sim'
JET=PKG/'Jet3DView.java'
RUNTIME=PKG/'FlightRuntimeActivity.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v107 anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Autonomous state guard: even if a gust, stall transient or FCS overshoot tries
# to build a rapid axial roll, the demo/autonomous presentation remains a
# coordinated non-aerobatic flight. Manual/BT pilot control is intentionally
# left outside this guard.
# ---------------------------------------------------------------------------
(SIM/'AutonomousAttitudeGuard.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** Final post-dynamics envelope guard for autonomous/demo flight only. */
public final class AutonomousAttitudeGuard {
    private AutonomousAttitudeGuard(){}

    public static void apply(FlightState s,double dt){
        dt=clamp(dt,.001,.08);
        if(s.onGround){
            s.rollRateDegSec=approach(s.rollRateDegSec,0,120.0*dt);
            s.yawRateDegSec=approach(s.yawRateDegSec,0,90.0*dt);
            s.rollDeg=approach(s.rollDeg,0,42.0*dt);
            return;
        }

        final double bankSoft=31.0;
        final double bankHard=40.0;
        final double maxRollRate=30.0;
        final double maxPitchRate=26.0;
        final double maxYawRate=24.0;

        double a=Math.abs(s.rollDeg),sign=Math.signum(s.rollDeg==0?1:s.rollDeg);
        if(a>bankSoft){
            double excess=clamp((a-bankSoft)/(bankHard-bankSoft),0,1);
            double recovery=-sign*(7.0+15.0*excess);
            if(s.rollRateDegSec*sign>0)
                s.rollRateDegSec=approach(s.rollRateDegSec,recovery,(85.0+90.0*excess)*dt);
        }

        s.rollRateDegSec=clamp(s.rollRateDegSec,-maxRollRate,maxRollRate);
        s.pitchRateDegSec=clamp(s.pitchRateDegSec,-maxPitchRate,maxPitchRate);
        s.yawRateDegSec=clamp(s.yawRateDegSec,-maxYawRate,maxYawRate);

        if(s.rollDeg>bankHard){s.rollDeg=bankHard;if(s.rollRateDegSec>0)s.rollRateDegSec=-8.0;}
        else if(s.rollDeg<-bankHard){s.rollDeg=-bankHard;if(s.rollRateDegSec<0)s.rollRateDegSec=8.0;}

        if(s.pitchDeg>24.0){s.pitchDeg=24.0;if(s.pitchRateDegSec>0)s.pitchRateDegSec=-5.0;}
        else if(s.pitchDeg<-21.0){s.pitchDeg=-21.0;if(s.pitchRateDegSec<0)s.pitchRateDegSec=5.0;}

        if(s.spin01>.08){
            double k=clamp((s.spin01-.08)/.42,0,1);
            s.rollRateDegSec=approach(s.rollRateDegSec,0,(55.0+75.0*k)*dt);
            s.yawRateDegSec=approach(s.yawRateDegSec,0,(45.0+65.0*k)*dt);
        }
    }

    private static double approach(double v,double t,double d){if(v<t)return Math.min(t,v+d);return Math.max(t,v-d);}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

TEST.mkdir(parents=True,exist_ok=True)
(TEST/'AutonomousAttitudeGuardTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutonomousAttitudeGuardTest {
    @Test public void axialRollRateIsCapped(){
        FlightState s=new FlightState();s.onGround=false;s.rollDeg=18;s.rollRateDegSec=180;s.pitchRateDegSec=90;s.yawRateDegSec=110;
        AutonomousAttitudeGuard.apply(s,.02);
        assertTrue(Math.abs(s.rollRateDegSec)<=30.0001);
        assertTrue(Math.abs(s.pitchRateDegSec)<=26.0001);
        assertTrue(Math.abs(s.yawRateDegSec)<=24.0001);
    }
    @Test public void excessiveBankIsContainedAndSentBackInward(){
        FlightState s=new FlightState();s.onGround=false;s.rollDeg=67;s.rollRateDegSec=120;
        AutonomousAttitudeGuard.apply(s,.02);
        assertEquals(40.0,s.rollDeg,.001);
        assertTrue(s.rollRateDegSec<0);
    }
    @Test public void negativeBankContainmentIsSymmetric(){
        FlightState s=new FlightState();s.onGround=false;s.rollDeg=-68;s.rollRateDegSec=-120;
        AutonomousAttitudeGuard.apply(s,.02);
        assertEquals(-40.0,s.rollDeg,.001);
        assertTrue(s.rollRateDegSec>0);
    }
    @Test public void normalCoordinatedTurnIsNotFlattened(){
        FlightState s=new FlightState();s.onGround=false;s.rollDeg=20;s.rollRateDegSec=12;s.yawRateDegSec=8;
        AutonomousAttitudeGuard.apply(s,.02);
        assertEquals(20.0,s.rollDeg,.001);
        assertEquals(12.0,s.rollRateDegSec,.001);
    }
}
''')

# Apply the final guard only to AUTO/demo modes, after the physics integration.
r=RUNTIME.read_text()
r=rep(r,
'        dynamics.step(state,controls,dt);\n        if(freeNavSeeded)updateRunwayPosition(dt);',
'        dynamics.step(state,controls,dt);\n        if(demoMode&&!localManual&&!btPilot&&!linkArmed)com.mg.fixturecockpitsim.sim.AutonomousAttitudeGuard.apply(state,dt);\n        if(freeNavSeeded)updateRunwayPosition(dt);',
'autonomous post-dynamics attitude guard')
RUNTIME.write_text(r)

# ---------------------------------------------------------------------------
# Renderer: use shortest angular travel with an explicit visual rate limit. This
# also removes the apparent 358-degree spin when simulation roll wraps across
# +180/-180. Make external canopy fully opaque and depth-writing so internal
# mesh/detail cannot show through the aircraft from external cameras.
# ---------------------------------------------------------------------------
j=JET.read_text()
j=rep(j,
'float k=1-(float)Math.exp(-dt*8),kg=1-(float)Math.exp(-dt*2.2),ks=1-(float)Math.exp(-dt*11);roll+=(tr-roll)*k;pitch+=(tp-pitch)*k;yaw+=shortest(ty-yaw)*k*.65f;',
'float k=1-(float)Math.exp(-dt*8),kg=1-(float)Math.exp(-dt*2.2),ks=1-(float)Math.exp(-dt*11);float rd=shortest(tr-roll);roll+=cl(rd,-65f*dt,65f*dt);pitch+=cl(tp-pitch,-42f*dt,42f*dt);yaw+=cl(shortest(ty-yaw),-46f*dt,46f*dt);',
'visual angular rate limiter')

# Previous renderer patches can insert extra solid/effect draws, so avoid a
# brittle full-line match. Remove the existing transparent canopy draw, then
# insert it immediately before the first transparent depth-mask pass.
canopy_call='bindAndDraw(vbCanopy,canopyCount);'
if canopy_call not in j:
    raise SystemExit('v107 anchor missing: canopy draw')
j=j.replace(canopy_call,'',1)
j=rep(j,'bindAndDraw(vbOpaque,opaqueCount);','GLES20.glDisable(GLES20.GL_BLEND);bindAndDraw(vbOpaque,opaqueCount);','opaque pass blend off')
j=rep(j,'GLES20.glDepthMask(false);',canopy_call+'GLES20.glEnable(GLES20.GL_BLEND);GLES20.glDepthMask(false);','opaque canopy before transparent pass')

# Replace canopy fragment branch by boundaries rather than exact old material,
# because v96 material-realism can legitimately alter the glass parameters.
start=j.find('else if(vP>.5&&vP<1.5){')
if start<0:
    raise SystemExit('v107 anchor missing: canopy shader start')
end=j.find('return;}',start)
if end<0:
    raise SystemExit('v107 anchor missing: canopy shader end')
end+=len('return;}')
opaque_canopy='else if(vP>.5&&vP<1.5){vec3 R=reflect(-V,N);float fr=.08+.92*pow(1.-ndv,4.2);vec3 glass=mix(vec3(.012,.024,.031),envc(R),.46+.42*fr);float sun=pow(ndh,120.);glass+=vec3(.80,.88,.90)*sun*.72;glass+=vec3(.10,.070,.032)*pow(1.-ndv,2.2)*.16;gl_FragColor=vec4(glass,1.0);return;}'
j=j[:start]+opaque_canopy+j[end:]
JET.write_text(j)

# Version.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 107',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.25-avm34.0-attitude-opaque-airframe'",g,count=1)
GRADLE.write_text(g)
print('v107 applied: autonomous anti-drill-roll guard + shortest-path renderer + opaque depth-writing canopy')
