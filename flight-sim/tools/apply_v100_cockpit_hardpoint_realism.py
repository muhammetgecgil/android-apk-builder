from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
VIS=PKG/'visual/VisualOrdnanceMesh.java'
JET=PKG/'Jet3DView.java'
RUNTIME=PKG/'FlightRuntimeActivity.java'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v100 cockpit/hardpoint patch anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Pure cockpit warning/symbology scheduling.
# ---------------------------------------------------------------------------
(SIM/'FighterCockpitSystemsModel.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** Deterministic cockpit warning/symbology model fed only by FlightState. */
public final class FighterCockpitSystemsModel {
    public static final int PAGE_PFD=0, PAGE_ENGINE=1, PAGE_RADAR=2, PAGE_FUEL=3;

    public static final class Snapshot {
        public boolean stall,spin,lowFuel,gearUnsafe,highG,masterWarning;
        public double flightPathDeg,aoaBracket01;
        public String primaryWarning="";
    }

    public Snapshot evaluate(FlightState s){
        Snapshot o=new Snapshot();
        double v=Math.max(1.0,s.trueAirspeedMps);
        o.stall=s.stall01>.52;
        o.spin=s.spin01>.42;
        o.lowFuel=s.fuelKg<520.0;
        o.gearUnsafe=!s.onGround&&s.altitudeM<180.0&&s.verticalSpeedMps<-1.2&&s.gearPosition<.78;
        o.highG=Math.abs(s.loadFactor)>8.6;
        o.masterWarning=o.spin||o.stall||o.gearUnsafe||o.highG||o.lowFuel;
        o.flightPathDeg=Math.toDegrees(Math.atan2(s.verticalSpeedMps,v));
        o.aoaBracket01=clamp((s.angleOfAttackDeg-7.0)/13.0,0,1);
        if(o.spin)o.primaryWarning="SPIN";
        else if(o.stall)o.primaryWarning="STALL";
        else if(o.gearUnsafe)o.primaryWarning="GEAR";
        else if(o.highG)o.primaryWarning="G LIMIT";
        else if(o.lowFuel)o.primaryWarning="FUEL";
        return o;
    }

    public static int nextPage(int page){return (page+1)%4;}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

TEST.mkdir(parents=True,exist_ok=True)
(TEST/'sim/FighterCockpitSystemsModelTest.java').parent.mkdir(parents=True,exist_ok=True)
(TEST/'sim/FighterCockpitSystemsModelTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class FighterCockpitSystemsModelTest {
    @Test public void lowApproachWithoutGearRaisesWarning(){
        FlightState s=new FlightState();s.onGround=false;s.altitudeM=90;s.verticalSpeedMps=-8;s.gearPosition=.1;s.trueAirspeedMps=92;
        FighterCockpitSystemsModel.Snapshot x=new FighterCockpitSystemsModel().evaluate(s);
        assertTrue(x.gearUnsafe);assertTrue(x.masterWarning);assertEquals("GEAR",x.primaryWarning);
    }
    @Test public void stallAndSpinHavePriority(){
        FlightState s=new FlightState();s.stall01=.8;s.spin01=.7;
        FighterCockpitSystemsModel.Snapshot x=new FighterCockpitSystemsModel().evaluate(s);
        assertTrue(x.stall);assertTrue(x.spin);assertEquals("SPIN",x.primaryWarning);
    }
    @Test public void pagesCycle(){assertEquals(0,FighterCockpitSystemsModel.nextPage(3));}
}
''')

# ---------------------------------------------------------------------------
# Full-screen first-person cockpit overlay. World/weather remain visible through
# the HUD; the external aircraft mesh is hidden while cockpit mode is active.
# ---------------------------------------------------------------------------
(PKG/'FighterCockpitView.java').write_text(r'''package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import com.mg.fixturecockpitsim.sim.FighterCockpitSystemsModel;
import com.mg.fixturecockpitsim.sim.FlightControls;
import com.mg.fixturecockpitsim.sim.FlightState;

import java.util.Locale;

/** AVM-28 first-person fighter cockpit: HUD, MFD pages, warnings and working switches. */
public final class FighterCockpitView extends View {
    public interface Listener { void onGearToggle(); void onSpeedBrakeToggle(); void onExitCockpit(); }
    private final Paint p=new Paint(3),stroke=new Paint(3),text=new Paint(3);
    private final Path path=new Path();
    private final FighterCockpitSystemsModel systems=new FighterCockpitSystemsModel();
    private FighterCockpitSystemsModel.Snapshot snap=new FighterCockpitSystemsModel.Snapshot();
    private Listener listener;
    private int page=FighterCockpitSystemsModel.PAGE_PFD;
    private float hudBrightness=1f;
    private boolean masterAck;
    private double alt,vs,spd,hdg,roll,pitch,aoa,beta,g,mach,q,fuel,fuelFrac,cg,gear,stall,spin,throttle,thrust,speedBrake;
    private boolean onGround;

    public FighterCockpitView(Context c){super(c);setBackgroundColor(Color.TRANSPARENT);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);text.setTypeface(android.graphics.Typeface.MONOSPACE);}
    public void setListener(Listener l){listener=l;}
    public void update(FlightState s,FlightControls c,double localSpeedBrake){
        if(s==null)return;alt=s.altitudeM;vs=s.verticalSpeedMps;spd=s.trueAirspeedMps;hdg=s.headingDeg;roll=s.rollDeg;pitch=s.pitchDeg;aoa=s.angleOfAttackDeg;beta=s.sideslipDeg;g=s.loadFactor;mach=s.mach;q=s.dynamicPressurePa;fuel=s.fuelKg;fuelFrac=s.fuelFraction01;cg=s.cgMac;gear=s.gearPosition;stall=s.stall01;spin=s.spin01;throttle=s.throttle;thrust=s.thrustN;speedBrake=localSpeedBrake;onGround=s.onGround;snap=systems.evaluate(s);if(!snap.masterWarning)masterAck=false;invalidate();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);int w=getWidth(),h=getHeight();if(w<20||h<20)return;
        drawCanopy(c,w,h);drawHud(c,w,h);drawPanel(c,w,h);drawMfd(c,w,h);drawWarnings(c,w,h);postInvalidateOnAnimation();
    }

    private void drawCanopy(Canvas c,int w,int h){
        p.setStyle(Paint.Style.FILL);p.setColor(0xd9181c1f);c.drawRect(0,h*.64f,w,h,p);
        stroke.setColor(0xee15191d);stroke.setStrokeWidth(Math.max(8f,w*.014f));
        path.reset();path.moveTo(w*.08f,h*.64f);path.cubicTo(w*.14f,h*.14f,w*.30f,h*.035f,w*.50f,h*.025f);path.cubicTo(w*.70f,h*.035f,w*.86f,h*.14f,w*.92f,h*.64f);c.drawPath(path,stroke);
        stroke.setStrokeWidth(Math.max(5f,w*.007f));c.drawLine(w*.50f,h*.025f,w*.50f,h*.105f,stroke);c.drawLine(w*.10f,h*.58f,w*.22f,h*.69f,stroke);c.drawLine(w*.90f,h*.58f,w*.78f,h*.69f,stroke);
        p.setColor(0x551d252b);c.drawRect(w*.04f,h*.61f,w*.96f,h*.68f,p);
    }

    private void drawHud(Canvas c,int w,int h){
        float cx=w*.5f,top=h*.09f,bottom=h*.57f,left=w*.31f,right=w*.69f;
        stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(Math.max(1.5f,w*.0016f));stroke.setColor(argb((int)(150*hudBrightness),0x69ff9c));c.drawRoundRect(left,top,right,bottom,w*.012f,w*.012f,stroke);
        int hc=argb((int)(245*hudBrightness),0x55ff82);text.setColor(hc);text.setTextSize(w*.0175f);text.setTextAlign(Paint.Align.LEFT);
        c.drawText(String.format(Locale.US,"SPD %03.0f",spd),left+w*.012f,top+h*.035f,text);c.drawText(String.format(Locale.US,"M %.2f",mach),left+w*.012f,top+h*.062f,text);
        text.setTextAlign(Paint.Align.RIGHT);c.drawText(String.format(Locale.US,"ALT %.0f",alt),right-w*.012f,top+h*.035f,text);c.drawText(String.format(Locale.US,"VS %+.0f",vs),right-w*.012f,top+h*.062f,text);
        text.setTextAlign(Paint.Align.CENTER);c.drawText(String.format(Locale.US,"%03.0f",hdg),cx,top+h*.030f,text);

        c.save();c.rotate((float)-roll,cx,h*.34f);float py=h*.34f+(float)pitch*h*.0060f;stroke.setColor(hc);stroke.setStrokeWidth(Math.max(1f,w*.0013f));for(int d=-30;d<=30;d+=5){float y=py-d*h*.0060f,half=(d%10==0?w*.055f:w*.030f);c.drawLine(cx-half,y,cx-w*.010f,y,stroke);c.drawLine(cx+w*.010f,y,cx+half,y,stroke);}c.restore();

        float fpmX=cx+(float)beta*w*.007f,fpmY=h*.34f-(float)snap.flightPathDeg*h*.010f;stroke.setColor(hc);stroke.setStrokeWidth(Math.max(1.4f,w*.0015f));c.drawCircle(fpmX,fpmY,w*.010f,stroke);c.drawLine(fpmX-w*.029f,fpmY,fpmX-w*.010f,fpmY,stroke);c.drawLine(fpmX+w*.010f,fpmY,fpmX+w*.029f,fpmY,stroke);c.drawLine(fpmX,fpmY-w*.010f,fpmX,fpmY-w*.023f,stroke);
        float by=h*.42f-(float)(snap.aoaBracket01-.5)*h*.10f;c.drawLine(right-w*.034f,by-h*.020f,right-w*.034f,by+h*.020f,stroke);c.drawLine(right-w*.034f,by-h*.020f,right-w*.020f,by-h*.020f,stroke);c.drawLine(right-w*.034f,by+h*.020f,right-w*.020f,by+h*.020f,stroke);
        text.setTextAlign(Paint.Align.CENTER);text.setTextSize(w*.014f);c.drawText(String.format(Locale.US,"AOA %+.1f  G %+.1f",aoa,g),cx,bottom-h*.012f,text);
    }

    private void drawPanel(Canvas c,int w,int h){
        p.setStyle(Paint.Style.FILL);p.setColor(0xf016191c);c.drawRect(0,h*.675f,w,h,p);p.setColor(0xff24292e);c.drawRoundRect(w*.035f,h*.705f,w*.965f,h*.965f,w*.012f,w*.012f,p);
        p.setColor(0xff070b0d);c.drawRoundRect(w*.075f,h*.725f,w*.465f,h*.925f,w*.010f,w*.010f,p);c.drawRoundRect(w*.535f,h*.725f,w*.925f,h*.925f,w*.010f,w*.010f,p);
        stroke.setColor(0xff65717a);stroke.setStrokeWidth(Math.max(1f,w*.0015f));c.drawRoundRect(w*.075f,h*.725f,w*.465f,h*.925f,w*.010f,w*.010f,stroke);c.drawRoundRect(w*.535f,h*.725f,w*.925f,h*.925f,w*.010f,w*.010f,stroke);
        button(c,w,h,.075f,.935f,.205f,.982f,"PAGE");button(c,w,h,.215f,.935f,.345f,.982f,"HUD BRT");button(c,w,h,.355f,.935f,.485f,.982f,"GEAR");button(c,w,h,.495f,.935f,.625f,.982f,"SPD BRK");button(c,w,h,.635f,.935f,.775f,.982f,"MASTER");button(c,w,h,.785f,.935f,.925f,.982f,"EXIT");
    }

    private void drawMfd(Canvas c,int w,int h){
        float lx=w*.095f,rx=w*.445f,ty=h*.745f,by=h*.902f;int green=0xff68ff9c;text.setColor(green);text.setTextSize(w*.0135f);text.setTextAlign(Paint.Align.LEFT);
        String name=page==0?"PFD":page==1?"ENGINE":page==2?"RADAR":"FUEL";c.drawText(name,lx,ty,text);
        if(page==0)drawPfd(c,w,h,lx,rx,ty,by);else if(page==1)drawEngine(c,w,h,lx,rx,ty,by);else if(page==2)drawRadar(c,w,h,lx,rx,ty,by);else drawFuel(c,w,h,lx,rx,ty,by);
        float rlx=w*.555f; text.setTextAlign(Paint.Align.LEFT);c.drawText("SYSTEMS",rlx,ty,text);text.setTextSize(w*.012f);c.drawText(String.format(Locale.US,"GEAR  %s",gear>.92?"DOWN":gear<.08?"UP":"TRANS"),rlx,ty+h*.036f,text);c.drawText(String.format(Locale.US,"SPD BRK %.0f%%",speedBrake*100),rlx,ty+h*.066f,text);c.drawText(String.format(Locale.US,"FUEL %.0f kg",fuel),rlx,ty+h*.096f,text);c.drawText(String.format(Locale.US,"Q %.1f kPa",q/1000.0),rlx,ty+h*.126f,text);
        float gx=w*.82f,gy=ty+h*.055f;gearLamp(c,gx,gy,gear>.80);gearLamp(c,gx-w*.045f,gy+h*.055f,gear>.80);gearLamp(c,gx+w*.045f,gy+h*.055f,gear>.80);
    }

    private void drawPfd(Canvas c,int w,int h,float l,float r,float t,float b){float cx=(l+r)/2,cy=(t+b)/2;int green=0xff68ff9c;stroke.setColor(green);stroke.setStrokeWidth(Math.max(1f,w*.0012f));c.save();c.clipRect(l,t+h*.012f,r,b);c.rotate((float)-roll,cx,cy);float hy=cy+(float)pitch*h*.0035f;c.drawLine(l,hy,r,hy,stroke);c.drawLine(cx-w*.035f,hy-h*.025f,cx+w*.035f,hy-h*.025f,stroke);c.restore();text.setColor(green);text.setTextSize(w*.0115f);text.setTextAlign(Paint.Align.LEFT);c.drawText(String.format(Locale.US,"HDG %03.0f",hdg),l,t+h*.135f,text);c.drawText(String.format(Locale.US,"BETA %+.1f",beta),l+w*.18f,t+h*.135f,text);}
    private void drawEngine(Canvas c,int w,int h,float l,float r,float t,float b){text.setTextSize(w*.0118f);text.setTextAlign(Paint.Align.LEFT);c.drawText(String.format(Locale.US,"L/R CORE  %.0f%%",throttle*100),l,t+h*.040f,text);c.drawText(String.format(Locale.US,"THRUST %.0f kN",thrust/1000.0),l,t+h*.074f,text);c.drawText(String.format(Locale.US,"NOZZLE %s",throttle>.80?"OPEN/AB":"SCHEDULED"),l,t+h*.108f,text);c.drawText(String.format(Locale.US,"MACH %.2f",mach),l,t+h*.142f,text);}
    private void drawRadar(Canvas c,int w,int h,float l,float r,float t,float b){float cx=(l+r)/2,cy=t+(b-t)*.58f,rr=Math.min((r-l)*.33f,(b-t)*.38f);stroke.setColor(0xff68ff9c);stroke.setStrokeWidth(Math.max(1f,w*.0011f));for(int i=1;i<=3;i++)c.drawCircle(cx,cy,rr*i/3f,stroke);c.drawLine(cx-rr,cy,cx+rr,cy,stroke);c.drawLine(cx,cy-rr,cx,cy+rr,stroke);for(int i=0;i<4;i++){double a=(i*1.7+hdg*.01);float x=cx+(float)Math.sin(a)*rr*(.25f+.16f*i),y=cy-(float)Math.cos(a)*rr*(.25f+.12f*i);c.drawRect(x-3,y-3,x+3,y+3,p);}}
    private void drawFuel(Canvas c,int w,int h,float l,float r,float t,float b){float bw=(r-l)*.18f,bh=(b-t)*.62f,x=l+(r-l)*.18f,y=b-bh*.05f;p.setColor(0xff13231a);c.drawRect(x,t+h*.025f,x+bw,t+h*.025f+bh,p);p.setColor(0xff55e883);c.drawRect(x,t+h*.025f+bh*(1-(float)fuelFrac),x+bw,t+h*.025f+bh,p);text.setColor(0xff68ff9c);text.setTextAlign(Paint.Align.LEFT);text.setTextSize(w*.0118f);c.drawText(String.format(Locale.US,"%.0f kg",fuel),x+bw+w*.025f,t+h*.060f,text);c.drawText(String.format(Locale.US,"CG %.1f%% MAC",cg*100),x+bw+w*.025f,t+h*.095f,text);}

    private void drawWarnings(Canvas c,int w,int h){boolean active=snap.masterWarning&&!masterAck;if(active){p.setColor(0xffd62118);c.drawRoundRect(w*.41f,h*.665f,w*.59f,h*.705f,w*.008f,w*.008f,p);text.setTextAlign(Paint.Align.CENTER);text.setTextSize(w*.0145f);text.setColor(Color.WHITE);c.drawText("MASTER WARNING",w*.50f,h*.692f,text);}if(snap.primaryWarning.length()>0){text.setColor(0xffffbd45);text.setTextSize(w*.015f);c.drawText(snap.primaryWarning,w*.50f,h*.635f,text);}if(stall>.35||spin>.2){text.setColor(0xffff704f);text.setTextSize(w*.0115f);c.drawText(String.format(Locale.US,"STALL %.0f%%  SPIN %.0f%%",stall*100,spin*100),w*.50f,h*.615f,text);}}
    private void gearLamp(Canvas c,float x,float y,boolean on){p.setColor(on?0xff4fff74:0xff3b2b25);c.drawCircle(x,y,8,p);}
    private void button(Canvas c,int w,int h,float l,float t,float r,float b,String s){p.setColor(0xff30363b);c.drawRoundRect(w*l,h*t,w*r,h*b,w*.006f,w*.006f,p);text.setColor(0xffdce7ea);text.setTextAlign(Paint.Align.CENTER);text.setTextSize(w*.0105f);c.drawText(s,w*(l+r)*.5f,h*(t+b)*.5f+w*.003f,text);}

    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()/Math.max(1f,getWidth()),y=e.getY()/Math.max(1f,getHeight());if(y<.925f)return true;if(x<.21f){page=FighterCockpitSystemsModel.nextPage(page);}else if(x<.35f){hudBrightness=hudBrightness>.85f?.62f:hudBrightness>.5f?.34f:1f;}else if(x<.49f){if(listener!=null)listener.onGearToggle();}else if(x<.63f){if(listener!=null)listener.onSpeedBrakeToggle();}else if(x<.78f){masterAck=true;}else{if(listener!=null)listener.onExitCockpit();}invalidate();return true;}
    private static int argb(int a,int rgb){return ((Math.max(0,Math.min(255,a)))<<24)|(rgb&0x00ffffff);}
}
''')

# ---------------------------------------------------------------------------
# Hardpoint geometry: explicit wing adapter plate, ejector/rack body and four
# sway-brace arms/pads. Stores remain generic simulator geometry.
# ---------------------------------------------------------------------------
v=VIS.read_text()
v=rep(v,
'    public static final float PART_PYLON=30f;\n',
'    public static final float PART_PYLON=30f;\n    public static final float PART_ADAPTER=38f;\n    public static final float PART_SWAY_BRACE=39f;\n',
'hardpoint material IDs')
old='''        box(x,wingY-.335f*s,z+.04f*s,.115f*s,.085f*s,1.18f*s);\n        brace(x-.13f*s,wingY-.30f*s,z-.19f*s,-1,s);brace(x+.13f*s,wingY-.30f*s,z-.19f*s,1,s);\n        brace(x-.13f*s,wingY-.30f*s,z+.25f*s,-1,s);brace(x+.13f*s,wingY-.30f*s,z+.25f*s,1,s);\n        // Generic attachment hooks, sway pads and an umbilical-like lead.\n        box(x-.065f*s,wingY-.37f*s,z-.28f*s,.055f*s,.045f*s,.12f*s);\n        box(x+.065f*s,wingY-.37f*s,z-.28f*s,.055f*s,.045f*s,.12f*s);\n        box(x-.065f*s,wingY-.37f*s,z+.30f*s,.055f*s,.045f*s,.12f*s);\n        box(x+.065f*s,wingY-.37f*s,z+.30f*s,.055f*s,.045f*s,.12f*s);\n        cylinderBetween(x+.11f*s,wingY-.34f*s,z+.14f*s,x+.13f*s,wingY-.46f*s,z+.23f*s,.012f*s,7);\n'''
new='''        hardpointAdapterRack(x,wingY,z,s);\n'''
v=rep(v,old,new,'main hardpoint rack replacement')
v=rep(v,
'''        prism(new float[][]{{x-.14f*s,wingY,z-.30f*s},{x+.14f*s,wingY,z-.30f*s},{x+.11f*s,wingY,z+.31f*s},{x-.11f*s,wingY,z+.31f*s}},.15f*s);\n        box(x,wingY-.19f*s,z,.095f*s,.065f*s,.66f*s);\n''',
'''        prism(new float[][]{{x-.14f*s,wingY,z-.30f*s},{x+.14f*s,wingY,z-.30f*s},{x+.11f*s,wingY,z+.31f*s},{x-.11f*s,wingY,z+.31f*s}},.15f*s);\n        hardpointAdapterRack(x,wingY+.015f*s,z,s*.72f);\n''',
'compact hardpoint adapter')
insert='''    private void hardpointAdapterRack(float x,float wingY,float z,float s){\n        // Flush wing-to-pylon adapter: the store is never visually glued to the wing.\n        part=PART_ADAPTER;\n        box(x,wingY-.018f*s,z-.015f*s,.275f*s,.032f*s,.50f*s);\n        box(x,wingY-.095f*s,z+.015f*s,.185f*s,.070f*s,.76f*s);\n        part=PART_PYLON;\n        box(x,wingY-.305f*s,z+.04f*s,.112f*s,.080f*s,1.16f*s);\n        // Four independent sway-brace screws terminate in visible contact pads.\n        part=PART_SWAY_BRACE;\n        for(float zz:new float[]{-.22f,.25f})for(float side:new float[]{-1f,1f}){\n            cylinderBetween(x+side*.165f*s,wingY-.235f*s,z+zz*s,x+side*.095f*s,wingY-.430f*s,z+(zz+.015f)*s,.018f*s,9);\n            box(x+side*.088f*s,wingY-.438f*s,z+(zz+.015f)*s,.045f*s,.022f*s,.072f*s);\n        }\n        // Ejector/rack hook housings plus an umbilical-like service lead.\n        part=PART_DETAIL;\n        box(x-.064f*s,wingY-.380f*s,z-.29f*s,.052f*s,.042f*s,.115f*s);\n        box(x+.064f*s,wingY-.380f*s,z-.29f*s,.052f*s,.042f*s,.115f*s);\n        box(x-.064f*s,wingY-.380f*s,z+.30f*s,.052f*s,.042f*s,.115f*s);\n        box(x+.064f*s,wingY-.380f*s,z+.30f*s,.052f*s,.042f*s,.115f*s);\n        cylinderBetween(x+.11f*s,wingY-.34f*s,z+.14f*s,x+.13f*s,wingY-.46f*s,z+.23f*s,.012f*s,7);\n    }\n\n'''
v=rep(v,'    private void compactStation(float x,float wingY,float z,float s){\n',insert+'    private void compactStation(float x,float wingY,float z,float s){\n','hardpoint helper insertion')
VIS.write_text(v)

# Pure mesh regression: require explicit adapter and sway-brace geometry.
(TEST/'visual').mkdir(parents=True,exist_ok=True)
(TEST/'visual/VisualHardpointRealismTest.java').write_text(r'''package com.mg.fixturecockpitsim.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class VisualHardpointRealismTest {
    @Test public void adapterAndSwayBracePartsExist(){
        float[] d=VisualOrdnanceMesh.build();int adapters=0,braces=0;
        for(int i=0;i+6<d.length;i+=7){if(Math.abs(d[i+6]-VisualOrdnanceMesh.PART_ADAPTER)<.1f)adapters++;if(Math.abs(d[i+6]-VisualOrdnanceMesh.PART_SWAY_BRACE)<.1f)braces++;}
        assertTrue(adapters>30);assertTrue(braces>100);
    }
}
''')

# Give adapter/rack metal and sway-brace steel distinct material response.
j=JET.read_text()
old_shader='else if(vP>36.5&&vP<37.5){base=vec3(.012,.022,.017);emitc=vec3(.015,.46,.17)*(.65+.15*sin(uTime*2.));rough=.28;metal=.10;}else if(vP>7.5&&vP<8.5)'
new_shader='else if(vP>36.5&&vP<37.5){base=vec3(.012,.022,.017);emitc=vec3(.015,.46,.17)*(.65+.15*sin(uTime*2.));rough=.28;metal=.10;}else if(vP>37.5&&vP<38.5){base=vec3(.155,.165,.172);rough=.34;metal=.78;ao=.88;}else if(vP>38.5&&vP<39.5){base=vec3(.095,.102,.108);rough=.25;metal=.92;ao=.84;}else if(vP>7.5&&vP<8.5)'
j=rep(j,old_shader,new_shader,'hardpoint shader materials')
JET.write_text(j)

# ---------------------------------------------------------------------------
# Runtime integration: cockpit mode hides the external aircraft but keeps the
# world/weather scene visible. Gear and speed-brake switches drive manual controls.
# ---------------------------------------------------------------------------
r=RUNTIME.read_text()
r=rep(r,'    private Jet3DView jet;\n','    private Jet3DView jet;\n    private FighterCockpitView cockpit;\n','cockpit field')
r=rep(r,'    private double localThrottle=.10,localBrake,localYawHold;\n','    private double localThrottle=.10,localBrake,localYawHold,localSpeedBrake;\n','local speed brake')
r=rep(r,'    private boolean localGearDown=true;\n','    private boolean localGearDown=true,cockpitMode;\n','cockpit state')
r=rep(r,'    private Button resetButton,modeButton,linkButton,brakeButton,gearButton;\n','    private Button resetButton,modeButton,linkButton,brakeButton,gearButton,cockpitButton;\n','cockpit button field')

# Construct cockpit after all v95-v97 visual layers exist.
needle='supersonicFx=new SupersonicEffectsView(this);'
if 'cockpit=new FighterCockpitView(this);' not in r:
    if needle not in r: raise SystemExit('v100 cockpit/hardpoint patch anchor missing: cockpit construction')
    r=r.replace(needle,needle+'cockpit=new FighterCockpitView(this);',1)
layer='        root.addView(supersonicFx,new FrameLayout.LayoutParams(-1,-1));\n'
if 'root.addView(cockpit,new FrameLayout.LayoutParams(-1,-1));' not in r:
    if layer not in r: raise SystemExit('v100 cockpit/hardpoint patch anchor missing: cockpit layer')
    r=r.replace(layer,layer+'        cockpit.setVisibility(View.GONE);root.addView(cockpit,new FrameLayout.LayoutParams(-1,-1));\n        cockpit.setListener(new FighterCockpitView.Listener(){public void onGearToggle(){cockpitGearToggle();}public void onSpeedBrakeToggle(){cockpitSpeedBrakeToggle();}public void onExitCockpit(){toggleCockpit();}});\n',1)

# Add a dedicated CKPT control without removing the 12 cinematic cameras.
r=rep(r,
'        Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\n',
'        cockpitButton=bottomButton("CKPT");Button[] all={modeButton,linkButton,center,yawL,yawR,thrM,thrP,brakeButton,gearButton,cam,cockpitButton};for(Button b:all)bottomPanel.addView(b,new LinearLayout.LayoutParams(0,-1,1f));\n',
'cockpit bottom button')
r=rep(r,
'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%12;jet.setCameraMode(cameraMode);});updateButtons();',
'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%12;jet.setCameraMode(cameraMode);});cockpitButton.setOnClickListener(v->toggleCockpit());updateButtons();',
'cockpit button action')

# Manual pilot owns the dedicated air-brake command.
r=rep(r,
'            controls.throttle=localThrottle;controls.brake=localBrake;controls.gearDown=localGearDown;\n',
'            controls.throttle=localThrottle;controls.brake=localBrake;controls.speedBrake=localSpeedBrake;controls.gearDown=localGearDown;\n',
'manual speed brake command')
# Ensure leaving manual mode cannot strand the speed brake deployed.
r=rep(r,
'            localBrake=0;localYawHold=0;imuRoll=imuPitch=imuYaw=0;seedFreeNavigation();autoRecovery=true;autoRecoveryStableSec=0;\n',
'            localBrake=0;localYawHold=0;localSpeedBrake=0;controls.speedBrake=0;imuRoll=imuPitch=imuYaw=0;seedFreeNavigation();autoRecovery=true;autoRecoveryStableSec=0;\n',
'manual exit speed brake reset')

# Feed live state to cockpit each rendered frame.
anchor='jet.setWheelSpeed((float)(state.onGround?state.trueAirspeedMps:0));'
if 'cockpit.update(state,controls,localSpeedBrake);' not in r:
    if anchor not in r: raise SystemExit('v100 cockpit/hardpoint patch anchor missing: cockpit telemetry')
    r=r.replace(anchor,anchor+'if(cockpit!=null)cockpit.update(state,controls,localSpeedBrake);',1)

# Cockpit behavior and working switches.
method='''    private void toggleCockpit(){\n        cockpitMode=!cockpitMode;\n        if(cockpit!=null)cockpit.setVisibility(cockpitMode?View.VISIBLE:View.GONE);\n        if(jet!=null)jet.setVisibility(cockpitMode?View.INVISIBLE:View.VISIBLE);\n        if(supersonicFx!=null)supersonicFx.setVisibility(cockpitMode?View.INVISIBLE:View.VISIBLE);\n        if(hud!=null)hud.setVisibility(cockpitMode?View.GONE:View.VISIBLE);\n        updateButtons();\n    }\n    private void cockpitGearToggle(){\n        if(!localManual){Toast.makeText(this,"GEAR switch: önce MANUEL IMU",Toast.LENGTH_SHORT).show();return;}\n        localGearDown=!localGearDown;updateButtons();\n    }\n    private void cockpitSpeedBrakeToggle(){\n        if(!localManual){Toast.makeText(this,"SPD BRK switch: önce MANUEL IMU",Toast.LENGTH_SHORT).show();return;}\n        localSpeedBrake=localSpeedBrake>.5?0:1;controls.speedBrake=localSpeedBrake;updateButtons();\n    }\n\n'''
r=rep(r,'    private void updateButtons(){if(modeButton==null)return;',method+'    private void updateButtons(){if(modeButton==null)return;','cockpit methods')
r=rep(r,
'gearButton.setText(localGearDown?"GEAR D":"GEAR U");}',
'gearButton.setText(localGearDown?"GEAR D":"GEAR U");if(cockpitButton!=null)cockpitButton.setText(cockpitMode?"CKPT ●":"CKPT");}',
'cockpit button state')
RUNTIME.write_text(r)

# VersionCode 100 avoids collision with an existing unrelated/unverified v99 branch.
g=GRADLE.read_text()
g=rep(g,'        versionCode 98\n','        versionCode 100\n','version code')
g=rep(g,"        versionName '26.16-avm27.0-advanced-flight-physics'\n","        versionName '26.18-avm28.0-cockpit-hardpoint-realism'\n",'version name')
GRADLE.write_text(g)
