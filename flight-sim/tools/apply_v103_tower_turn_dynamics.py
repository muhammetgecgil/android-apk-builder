from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
AIRFIELD=PKG/'AirfieldWorldView.java'
MISSION=SIM/'AutonomousFlightMission.java'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v103 tower/turn patch anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Autonomous turn command conditioner. The mission still chooses the desired
# heading/bank; this layer prevents an overshoot from producing an instant
# right-to-left command reversal and reduces double-counted rudder steering.
# ---------------------------------------------------------------------------
(SIM/'AutonomousTurnSmoother.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** Rate-limited coordinated-turn command conditioner for autonomous missions. */
public final class AutonomousTurnSmoother {
    private double rollOut, yawOut;

    public void reset(){rollOut=0;yawOut=0;}

    public void apply(FlightState s,FlightControls c,double dt){
        dt=clamp(dt,.001,.08);
        if(s.onGround){rollOut=c.roll;yawOut=c.yaw;return;}

        // Bank is the primary heading-control mechanism in flight. Rudder is
        // deliberately smaller and includes yaw-rate damping so roll+yaw do
        // not both aggressively chase the same heading error.
        double desiredRoll=c.roll*.86;
        double desiredYaw=c.yaw*.46-clamp(s.yawRateDegSec/95.0,-.12,.12);
        if(Math.abs(desiredRoll)<.018)desiredRoll=0;
        if(Math.abs(desiredYaw)<.012)desiredYaw=0;

        // If the requested turn direction changes, first unload the previous
        // command toward zero. This removes the visually implausible snap from
        // a small right turn straight into a hard left turn after overshoot.
        if(rollOut*desiredRoll<0&&Math.abs(rollOut)>.035)desiredRoll=0;
        if(yawOut*desiredYaw<0&&Math.abs(yawOut)>.025)desiredYaw=0;
        if(desiredYaw*s.yawRateDegSec<0&&Math.abs(s.yawRateDegSec)>7.0)desiredYaw=0;

        rollOut=approach(rollOut,desiredRoll,.62*dt);
        yawOut=approach(yawOut,desiredYaw,.78*dt);
        c.roll=rollOut;c.yaw=yawOut;
    }

    public double getRollOut(){return rollOut;}
    public double getYawOut(){return yawOut;}
    private static double approach(double v,double t,double d){if(v<t)return Math.min(t,v+d);return Math.max(t,v-d);}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

(TEST/'sim').mkdir(parents=True,exist_ok=True)
(TEST/'sim/AutonomousTurnSmootherTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutonomousTurnSmootherTest {
    @Test public void reversalPassesThroughZeroInsteadOfSnapping(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;
        FlightControls c=new FlightControls();
        for(int i=0;i<20;i++){c.roll=.32;c.yaw=.20;s.yawRateDegSec=8;sm.apply(s,c,.05);}
        double before=sm.getRollOut();assertTrue(before>.15);
        c.roll=-.32;c.yaw=-.20;s.yawRateDegSec=18;sm.apply(s,c,.05);
        assertTrue(sm.getRollOut()>=0);assertTrue(sm.getYawOut()>-.02);
    }
    @Test public void sustainedOppositeCommandEventuallyTurnsOtherWay(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;FlightControls c=new FlightControls();
        for(int i=0;i<20;i++){c.roll=.30;c.yaw=.15;s.yawRateDegSec=6;sm.apply(s,c,.05);}
        for(int i=0;i<80;i++){c.roll=-.30;c.yaw=-.15;s.yawRateDegSec=Math.max(0,6-i*.2);sm.apply(s,c,.05);}
        assertTrue(sm.getRollOut()<-.10);assertTrue(sm.getYawOut()<0);
    }
    @Test public void oneFrameCommandDeltaIsLimited(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();FlightState s=new FlightState();s.onGround=false;FlightControls c=new FlightControls();c.roll=.8;c.yaw=.8;
        sm.apply(s,c,.05);assertTrue(Math.abs(sm.getRollOut())<=.032);assertTrue(Math.abs(sm.getYawOut())<=.040);
    }
}
''')

# Wire the conditioner into autonomous flight only. Manual/IMU controls are not touched.
m=MISSION.read_text()
m=rep(m,'    private double phaseTime,orbitTime;\n','    private double phaseTime,orbitTime;\n    private final AutonomousTurnSmoother turnSmoother=new AutonomousTurnSmoother();\n','turn smoother field')
m=rep(m,'        phase=Phase.TAXI_OUT;phaseTime=orbitTime=0;\n','        phase=Phase.TAXI_OUT;phaseTime=orbitTime=0;turnSmoother.reset();\n','turn smoother reset')
m=rep(m,'        c.clamp();\n        CinematicEnvironmentView.setFlightScene','        turnSmoother.apply(s,c,dt);\n        c.clamp();\n        CinematicEnvironmentView.setFlightScene','turn smoother integration')
MISSION.write_text(m)

# ---------------------------------------------------------------------------
# Fixed right-side tower projection. It stays on the runway's right side,
# becomes substantially larger as the aircraft approaches, and after pass-by
# moves further right/out of frame instead of sliding unrealistically left.
# ---------------------------------------------------------------------------
(SIM/'TowerPerspectiveModel.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** Screen projection schedule for the runway-side control tower. */
public final class TowerPerspectiveModel {
    public static final double TOWER_ALONG_M=330.0;
    public static final double TOWER_CROSS_M=155.0;
    public static final class Sample {
        public boolean visible;public double relM,near01,fade01,x01;
    }
    public static Sample sample(double alongTrackM,double crossTrackM,double altitudeM){
        Sample o=new Sample();o.relM=TOWER_ALONG_M-alongTrackM;
        o.visible=altitudeM<=220&&o.relM>-300&&o.relM<880;
        o.near01=clamp(1.0-Math.max(0,o.relM)/790.0,.14,1.0);
        double passed=Math.max(0,-o.relM);o.fade01=1.0-clamp(passed/270.0,0,1);
        double lateralBias=clamp((TOWER_CROSS_M-crossTrackM)/1200.0,-.035,.075);
        // Tower remains on the right; once passed it sweeps farther right.
        o.x01=clamp(.735+.075*(1-o.near01)+lateralBias+clamp(passed/720.0,0,.18),.64,.975);
        if(o.fade01<=.01)o.visible=false;
        return o;
    }
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

(TEST/'sim/TowerPerspectiveModelTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class TowerPerspectiveModelTest {
    @Test public void towerStaysOnRightSide(){
        for(double along=-200;along<520;along+=80){TowerPerspectiveModel.Sample s=TowerPerspectiveModel.sample(along,0,40);if(s.visible)assertTrue(s.x01>.63);}
    }
    @Test public void towerGrowsInPerspectiveAsAircraftApproaches(){
        TowerPerspectiveModel.Sample far=TowerPerspectiveModel.sample(-350,0,0),near=TowerPerspectiveModel.sample(300,0,0);
        assertTrue(near.near01>far.near01+.45);
    }
    @Test public void passedTowerMovesRightThenFades(){
        TowerPerspectiveModel.Sample at=TowerPerspectiveModel.sample(330,0,20),passed=TowerPerspectiveModel.sample(430,0,20);
        assertTrue(passed.x01>at.x01);assertTrue(passed.fade01<at.fade01);
    }
}
''')

a=AIRFIELD.read_text()
a=rep(a,
'''    private boolean towerVisible(){
        if(!(phase.contains("RUNWAY_HOLD")||phase.contains("TAKEOFF_ROLL")||phase.contains("ROTATE_CLIMB")))return false;
        if(altitudeM>150f)return false;
        float rel=TOWER_ALONG_M-alongTrackM;
        return rel>-250f&&rel<620f;
    }
''',
'''    private boolean towerVisible(){
        if(!(phase.contains("TAXI_OUT")||phase.contains("RUNWAY_HOLD")||phase.contains("TAKEOFF_ROLL")||phase.contains("ROTATE_CLIMB")))return false;
        return com.mg.fixturecockpitsim.sim.TowerPerspectiveModel.sample(alongTrackM,crossTrackM,altitudeM).visible;
    }
''','tower visibility')

new_tower=r'''    /** Larger right-side airport control tower with a readable cab, base and perspective depth. */
    private void drawTakeoffTower(Canvas c,int w,int h,float hz){
        com.mg.fixturecockpitsim.sim.TowerPerspectiveModel.Sample tv=com.mg.fixturecockpitsim.sim.TowerPerspectiveModel.sample(alongTrackM,crossTrackM,altitudeM);
        if(!tv.visible)return;
        float near=(float)tv.near01,fade=(float)tv.fade01,x=w*(float)tv.x01;
        int a=(int)(255f*fade);if(a<=3)return;
        float groundY=hz+(h-hz)*(.30f+.66f*near);
        float towerH=h*(.19f+.45f*near),shaftW=w*(.026f+.054f*near),depth=shaftW*.58f;
        float topY=groundY-towerH,shaftTop=topY+towerH*.30f;

        // Ground complex gives the tower scale and prevents a floating/toy look.
        float baseW=shaftW*3.20f,baseH=towerH*.12f;
        p.setColor(withAlpha(0x50000000,fade));c.drawOval(x-baseW*.75f,groundY-baseH*.12f,x+baseW*.82f,groundY+baseH*.55f,p);
        p.setColor(withAlpha(0xff9ca3a2,fade));c.drawRect(x-baseW*.52f,groundY-baseH,x+baseW*.50f,groundY,p);
        path.reset();path.moveTo(x+baseW*.50f,groundY-baseH);path.lineTo(x+baseW*.50f+depth,groundY-baseH-depth*.28f);path.lineTo(x+baseW*.50f+depth,groundY-depth*.12f);path.lineTo(x+baseW*.50f,groundY);path.close();p.setColor(withAlpha(0xff747c7d,fade));c.drawPath(path,p);
        // Service doors/windows on the base.
        p.setColor(withAlpha(0xff35434a,fade));c.drawRect(x-baseW*.34f,groundY-baseH*.70f,x-baseW*.17f,groundY-baseH*.12f,p);c.drawRect(x+baseW*.07f,groundY-baseH*.67f,x+baseW*.27f,groundY-baseH*.28f,p);

        // Broad tapered shaft and its right-side face.
        path.reset();path.moveTo(x-shaftW*.52f,shaftTop);path.lineTo(x+shaftW*.52f,shaftTop);path.lineTo(x+shaftW*.70f,groundY-baseH);path.lineTo(x-shaftW*.70f,groundY-baseH);path.close();p.setColor(withAlpha(0xffc9ccca,fade));c.drawPath(path,p);
        path.reset();path.moveTo(x+shaftW*.52f,shaftTop);path.lineTo(x+shaftW*.52f+depth,shaftTop-depth*.54f);path.lineTo(x+shaftW*.70f+depth,groundY-baseH-depth*.18f);path.lineTo(x+shaftW*.70f,groundY-baseH);path.close();p.setColor(withAlpha(0xff879092,fade));c.drawPath(path,p);
        // Vertical service slit windows.
        p.setColor(withAlpha(0xff4c6671,fade));for(int i=0;i<4;i++){float yy=lerp(shaftTop+towerH*.10f,groundY-baseH-towerH*.08f,i/3f);c.drawRect(x-shaftW*.17f,yy,x+shaftW*.17f,yy+towerH*.025f,p);}

        // Large glazed control cab with sloped front and visible right face.
        float cabW=shaftW*2.95f,cabH=towerH*.205f,cabY=topY+towerH*.075f;
        path.reset();path.moveTo(x-cabW*.54f,cabY);path.lineTo(x+cabW*.54f,cabY);path.lineTo(x+cabW*.45f,cabY+cabH);path.lineTo(x-cabW*.45f,cabY+cabH);path.close();p.setColor(withAlpha(0xff243740,fade));c.drawPath(path,p);
        path.reset();path.moveTo(x+cabW*.54f,cabY);path.lineTo(x+cabW*.54f+depth,cabY-depth*.52f);path.lineTo(x+cabW*.45f+depth,cabY+cabH-depth*.22f);path.lineTo(x+cabW*.45f,cabY+cabH);path.close();p.setColor(withAlpha(0xff152831,fade));c.drawPath(path,p);

        float winTop=cabY+cabH*.16f,winBot=cabY+cabH*.73f;
        for(int i=0;i<5;i++){float lx=lerp(x-cabW*.43f,x+cabW*.25f,i/4f),rw=lx+cabW*.135f;p.setShader(new LinearGradient(0,winTop,0,winBot,new int[]{withAlpha(0xff9dd9e7,fade),withAlpha(0xff173946,fade)},null,Shader.TileMode.CLAMP));c.drawRect(lx,winTop,rw,winBot,p);p.setShader(null);}
        // Side glazing emphasizes that the tower has volume, not a flat sprite.
        p.setColor(withAlpha(0xff376271,fade));path.reset();path.moveTo(x+cabW*.54f+depth*.10f,winTop-depth*.42f);path.lineTo(x+cabW*.54f+depth*.82f,winTop-depth*.50f);path.lineTo(x+cabW*.45f+depth*.72f,winBot-depth*.20f);path.lineTo(x+cabW*.45f+depth*.08f,winBot);path.close();c.drawPath(path,p);

        // Balcony/roof, radar housing, mast and obstruction beacon.
        float balconyY=cabY+cabH*.84f;stroke.setColor(withAlpha(0xffd8dcda,fade));stroke.setStrokeWidth(Math.max(1.5f,w*.0012f));c.drawLine(x-cabW*.62f,balconyY,x+cabW*.67f,balconyY,stroke);for(int i=0;i<7;i++){float bx=lerp(x-cabW*.60f,x+cabW*.64f,i/6f);c.drawLine(bx,balconyY,bx,balconyY+cabH*.12f,stroke);}
        float roofY=cabY-cabH*.08f;p.setColor(withAlpha(0xffd7dad7,fade));c.drawOval(x-cabW*.64f,roofY-cabH*.09f,x+cabW*.70f,roofY+cabH*.13f,p);
        p.setColor(withAlpha(0xff5f696d,fade));c.drawOval(x-cabW*.20f,roofY-cabH*.15f,x+cabW*.22f,roofY+cabH*.03f,p);
        stroke.setColor(withAlpha(0xffe4e4df,fade));stroke.setStrokeWidth(Math.max(2f,w*.0015f));c.drawLine(x+depth*.18f,roofY,x+depth*.18f,roofY-towerH*.19f,stroke);c.drawLine(x+depth*.18f,roofY-towerH*.13f,x+depth*.36f,roofY-towerH*.10f,stroke);
        p.setColor(withAlpha(0xffff3b30,fade*(.68f+.32f*(float)Math.sin(System.nanoTime()/1e8))));c.drawCircle(x+depth*.18f,roofY-towerH*.19f,Math.max(2.5f,w*.0027f),p);
    }

'''
pat=r'    /\*\* Perspective 3-D airport control tower\..*?\n    private void drawTakeoffTower\(Canvas c,int w,int h,float hz\)\{.*?\n    \}\n\n    private void drawMountains'
a2,n=re.subn(pat,lambda m:new_tower+'    private void drawMountains',a,count=1,flags=re.S)
if n!=1:
    # tolerate comment wording changes and replace from method signature instead
    pat2=r'    private void drawTakeoffTower\(Canvas c,int w,int h,float hz\)\{.*?\n    \}\n\n    private void drawMountains'
    a2,n=re.subn(pat2,lambda m:new_tower+'    private void drawMountains',a,count=1,flags=re.S)
if n!=1:raise SystemExit('v103 tower/turn patch anchor missing: tower draw method')
AIRFIELD.write_text(a2)

# Version bump.
g=GRADLE.read_text()
g=rep(g,'        versionCode 102\n','        versionCode 103\n','version code')
g=rep(g,"        versionName '26.20-avm30.0-hardpoint-external-tank-realism'\n","        versionName '26.21-avm31.0-tower-turn-dynamics'\n",'version name')
GRADLE.write_text(g)

print('v103 applied: larger right-side tower + rate-limited coordinated autonomous turns')
