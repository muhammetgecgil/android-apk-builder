from pathlib import Path

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
        raise SystemExit(f'v97 jet-blast patch anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Deterministic jet-blast strength/surface model.
# ---------------------------------------------------------------------------
(SIM/'JetBlastDynamicsModel.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** AVM-26 deterministic jet-blast / ground-interaction scheduling. */
public final class JetBlastDynamicsModel {
    public static final int RUNWAY_DRY=0, RUNWAY_WET=1, GRASS_DIRT=2;
    private JetBlastDynamicsModel(){}

    public static double blast01(double throttle, boolean afterburner, double groundInfluence, double speedMps){
        double t=clamp01(throttle);
        double core=Math.pow(Math.max(0,(t-.16)/.84),1.55);
        double ab=afterburner?(.26+.34*clamp01((t-.76)/.24)):0;
        double motionRelief=1.0-.26*clamp01(speedMps/95.0);
        return clamp01((core+ab)*clamp01(groundInfluence)*motionRelief);
    }

    public static int surfaceMode(double wetness,double crossTrackM){
        if(Math.abs(crossTrackM)>34.0)return GRASS_DIRT;
        if(wetness>.32)return RUNWAY_WET;
        return RUNWAY_DRY;
    }

    public static double distortion01(double throttle,boolean afterburner,double altitudeM){
        double g=clamp01(1.0-altitudeM/55.0);
        double d=.18+.58*Math.pow(clamp01(throttle),1.35);
        if(afterburner)d+=.34;
        return clamp01(d*(.72+.28*g));
    }

    public static double debrisImpulse(double blast01,int surfaceMode){
        double m=surfaceMode==GRASS_DIRT?1.15:surfaceMode==RUNWAY_WET?.72:.88;
        return Math.max(0,blast01-.34)*m;
    }

    private static double clamp01(double v){return Math.max(0,Math.min(1,v));}
}
''')

TEST.mkdir(parents=True,exist_ok=True)
(TEST/'JetBlastDynamicsModelTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class JetBlastDynamicsModelTest {
    @Test public void afterburnerIsStrongerThanDryPower(){
        double dry=JetBlastDynamicsModel.blast01(.88,false,1,0);
        double ab=JetBlastDynamicsModel.blast01(.88,true,1,0);
        assertTrue(ab>dry+.15);
    }
    @Test public void blastFadesAwayFromGround(){
        double ground=JetBlastDynamicsModel.blast01(.95,true,1,10);
        double air=JetBlastDynamicsModel.blast01(.95,true,.05,10);
        assertTrue(ground>air*8);
    }
    @Test public void wetAndGrassSurfacesAreDistinct(){
        assertEquals(JetBlastDynamicsModel.RUNWAY_WET,JetBlastDynamicsModel.surfaceMode(.8,2));
        assertEquals(JetBlastDynamicsModel.GRASS_DIRT,JetBlastDynamicsModel.surfaceMode(.8,50));
        assertEquals(JetBlastDynamicsModel.RUNWAY_DRY,JetBlastDynamicsModel.surfaceMode(.05,3));
    }
    @Test public void looseDebrisNeedsMeaningfulBlast(){
        assertEquals(0,JetBlastDynamicsModel.debrisImpulse(.20,JetBlastDynamicsModel.RUNWAY_DRY),1e-9);
        assertTrue(JetBlastDynamicsModel.debrisImpulse(.90,JetBlastDynamicsModel.GRASS_DIRT)>.5);
    }
    @Test public void afterburnerDistortionIsHigh(){
        assertTrue(JetBlastDynamicsModel.distortion01(.95,true,2)>.85);
    }
}
''')

# ---------------------------------------------------------------------------
# Publish engine state so the world-space effects layer can react to the jet.
# ---------------------------------------------------------------------------
j=JET.read_text()
j=rep(j,
'    private float st=.6f,sg=1f,sb;\n',
'    private float st=.6f,sg=1f,sb;\n    private static volatile float sharedThrottle01=.6f;\n    private static volatile boolean sharedAfterburner;\n',
'jet shared engine fields')
j=rep(j,
'    public void setTelemetry(float roll,float pitch,float yaw,float throttle,float linkHz,int drops,boolean live){r.tele(roll,pitch,yaw,throttle,live);st=cl(throttle,0,1);sound.update(st,st*230,sg,sb,ground);}\n',
'    public void setTelemetry(float roll,float pitch,float yaw,float throttle,float linkHz,int drops,boolean live){r.tele(roll,pitch,yaw,throttle,live);st=cl(throttle,0,1);sharedThrottle01=st;sharedAfterburner=st>=.78f;sound.update(st,st*230,sg,sb,ground);}\n',
'telemetry publishes throttle')
j=rep(j,
'    public int getCameraMode(){return r.cam;}\n',
'    public int getCameraMode(){return r.cam;}\n    public static float getSharedThrottle01(){return sharedThrottle01;}\n    public static boolean isSharedAfterburner(){return sharedAfterburner;}\n',
'jet engine getters')
JET.write_text(j)

# ---------------------------------------------------------------------------
# Screen-space jet blast visualizer, placed below the aircraft but above runway.
# ---------------------------------------------------------------------------
(PKG/'JetBlastEffectsView.java').write_text(r'''package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.mg.fixturecockpitsim.sim.JetBlastDynamicsModel;

/**
 * AVM-26 jet-blast / ground interaction layer.
 * Uses bounded deterministic particles: no per-frame allocations and no fixed-object motion.
 */
public final class JetBlastEffectsView extends View {
    private static final int N=72,FOD=10;
    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Path path=new Path();
    private final float[] seed=new float[N],age=new float[N],life=new float[N];
    private final float[] fodSeed=new float[FOD],fodAge=new float[FOD];
    private long lastNs;private float clock;

    public JetBlastEffectsView(Context c){
        super(c);
        stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<N;i++){seed[i]=hash(i*17.13f+3.7f);life[i]=.42f+1.05f*hash(i*9.1f+7f);age[i]=life[i]*hash(i*5.7f);}
        for(int i=0;i<FOD;i++){fodSeed[i]=hash(i*31.7f+5.1f);fodAge[i]=1.8f*hash(i*11.3f);}
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime();float dt=lastNs==0?.016f:clamp((now-lastNs)/1e9f,.001f,.05f);lastNs=now;clock+=dt;
        int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;

        boolean ground=AirfieldWorldView.isSharedOnGround();
        float alt=AirfieldWorldView.getSharedAltitudeM(),speed=AirfieldWorldView.getSharedSpeedMps();
        float xtrk=AirfieldWorldView.getSharedCrossTrackM(),wet=WeatherEffectsView.getSharedWetness01();
        float throttle=Jet3DView.getSharedThrottle01();boolean ab=Jet3DView.isSharedAfterburner();
        float groundInfluence=ground?1f:clamp(1f-alt/22f,0,1);
        float blast=(float)JetBlastDynamicsModel.blast01(throttle,ab,groundInfluence,speed);
        float distortion=(float)JetBlastDynamicsModel.distortion01(throttle,ab,alt);
        int surface=JetBlastDynamicsModel.surfaceMode(wet,xtrk);

        if(distortion>.08f)drawHeatDistortion(c,w,h,distortion,ab,groundInfluence);
        if(blast>.045f){
            if(surface==JetBlastDynamicsModel.RUNWAY_WET)drawWaterSpray(c,w,h,blast,ab,dt);
            else drawDust(c,w,h,blast,ab,surface,dt);
            drawLooseDebris(c,w,h,blast,surface,dt);
        }
        if(distortion>.05f||blast>.04f)postInvalidateOnAnimation();
    }

    private void drawHeatDistortion(Canvas c,int w,int h,float d,boolean ab,float groundInfluence){
        float ox=w*.5f,oy=h*.575f,len=h*(.13f+.25f*d+.09f*(ab?1:0));
        int bands=ab?12:8;
        for(int i=0;i<bands;i++){
            float q=i/(float)Math.max(1,bands-1),y0=oy+q*len*.88f;
            float amp=w*(.0025f+.0075f*d)*(1f+q*.8f);
            float width=w*(.026f+.095f*q)*(.65f+.55f*d);
            float phase=clock*(7.5f+q*4.0f)+i*.91f;
            path.reset();
            for(int k=0;k<=12;k++){
                float t=k/12f;
                float y=y0+t*len*.13f;
                float x=ox+(float)Math.sin(phase+t*9.2f)*amp + (t-.5f)*width*.10f*(float)Math.sin(i*2.2f);
                if(k==0)path.moveTo(x-width*.5f,y);else path.lineTo(x-width*.5f+width*t,y);
            }
            stroke.setStrokeWidth(Math.max(1f,w*(.0011f+.0018f*d)));
            int a=(int)(14+34*d*(.45f+.55f*groundInfluence));
            stroke.setColor((a<<24)|0x00d8e8ee);c.drawPath(path,stroke);
            stroke.setStrokeWidth(Math.max(1f,w*.0007f));stroke.setColor(((a/2)<<24)|0x00ffffff);
            c.drawLine(ox-width*.35f,y0,ox+width*.35f,y0+len*.06f,stroke);
        }
    }

    private void drawWaterSpray(Canvas c,int w,int h,float blast,boolean ab,float dt){
        float ox=w*.5f,oy=h*.72f;
        p.setStyle(Paint.Style.FILL);
        int fanA=(int)(20+54*blast);p.setColor((fanA<<24)|0x00d9edf3);
        path.reset();path.moveTo(ox-w*.035f,oy);path.lineTo(ox-w*(.16f+.14f*blast),h*.965f);path.lineTo(ox+w*(.16f+.14f*blast),h*.965f);path.lineTo(ox+w*.035f,oy);path.close();c.drawPath(path,p);
        particles(c,w,h,blast,ab,JetBlastDynamicsModel.RUNWAY_WET,dt);
    }

    private void drawDust(Canvas c,int w,int h,float blast,boolean ab,int surface,float dt){
        float ox=w*.5f,oy=h*.735f;
        int col=surface==JetBlastDynamicsModel.GRASS_DIRT?0x8b7652:0x9b8e80;
        int a=(int)(16+48*blast);p.setColor((a<<24)|col);
        for(int i=0;i<5;i++){
            float q=i/4f,rw=w*(.045f+.055f*q)*(1+.6f*blast),rh=h*(.012f+.025f*q);
            float x=ox+(i-2)*w*.025f+(float)Math.sin(clock*3+i)*w*.008f*blast;
            float y=oy+h*.035f*q;
            c.drawOval(x-rw,y-rh,x+rw,y+rh,p);
        }
        particles(c,w,h,blast,ab,surface,dt);
    }

    private void particles(Canvas c,int w,int h,float blast,boolean ab,int surface,float dt){
        float rate=.35f+2.6f*blast+(ab?.7f:0);
        for(int i=0;i<N;i++){
            age[i]+=dt*rate;
            if(age[i]>life[i])age[i]-=life[i];
            float t=age[i]/life[i];
            if(t>blast*.92f+.06f)continue;
            float s=seed[i],side=s<.5f?-1f:1f,spread=(.18f+.38f*hash(i*4.31f))*side;
            float x=w*.5f+w*spread*t*(.38f+.62f*blast);
            float y=h*.725f+h*(.04f+.22f*t)+h*.025f*(float)Math.sin(i+clock*4f)*t;
            float size=w*(.0011f+.0032f*(1-t))*((surface==JetBlastDynamicsModel.RUNWAY_WET)?1.3f:1f);
            int rgb=surface==JetBlastDynamicsModel.RUNWAY_WET?0xddeff4:(surface==JetBlastDynamicsModel.GRASS_DIRT?(i%3==0?0x596845:0x8c7755):0x978a7d);
            int a=(int)((24+100*blast)*(1-t));p.setColor((a<<24)|rgb);
            if(surface==JetBlastDynamicsModel.RUNWAY_WET)c.drawOval(x-size*1.8f,y-size*.45f,x+size*1.8f,y+size*.45f,p);
            else c.drawCircle(x,y,size,p);
        }
    }

    private void drawLooseDebris(Canvas c,int w,int h,float blast,int surface,float dt){
        float imp=(float)JetBlastDynamicsModel.debrisImpulse(blast,surface);if(imp<=0)return;
        for(int i=0;i<FOD;i++){
            fodAge[i]+=dt*(.42f+imp*1.8f);if(fodAge[i]>2.2f)fodAge[i]-=2.2f;
            float t=fodAge[i]/2.2f;if(t>imp*.82f+.12f)continue;
            float s=fodSeed[i],side=s<.5f?-1f:1f;
            float x=w*.5f+side*w*(.035f+.26f*t)*(.55f+imp);
            float y=h*.76f+h*(.14f*t-.08f*(float)Math.sin(t*3.14159f));
            float sz=w*(.0025f+.0035f*s)*(1-.45f*t);
            int col=surface==JetBlastDynamicsModel.GRASS_DIRT?0x514834:0x34383a;
            int aa=(int)((70+90*imp)*(1f-t));p.setColor((aa<<24)|col);
            c.save();c.rotate((clock*130f+i*37f)*(side),x,y);c.drawRect(x-sz,y-sz*.45f,x+sz,y+sz*.45f,p);c.restore();
        }
    }

    private static float hash(float x){double y=Math.sin(x*12.9898+78.233)*43758.5453;return (float)(y-Math.floor(y));}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
''')

# ---------------------------------------------------------------------------
# Runtime composition: world -> weather -> environment -> jet blast -> aircraft.
# ---------------------------------------------------------------------------
r=RUNTIME.read_text()
r=rep(r,
'    private EnvironmentRealismOverlayView environmentFx;\n',
'    private EnvironmentRealismOverlayView environmentFx;\n    private JetBlastEffectsView jetBlastFx;\n',
'jet blast field')
r=rep(r,
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);environmentFx=new EnvironmentRealismOverlayView(this);jet=new Jet3DView(this);supersonicFx=new SupersonicEffectsView(this);\n',
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);environmentFx=new EnvironmentRealismOverlayView(this);jetBlastFx=new JetBlastEffectsView(this);jet=new Jet3DView(this);supersonicFx=new SupersonicEffectsView(this);\n',
'jet blast creation')
r=rep(r,
'        root.addView(environmentFx,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n',
'        root.addView(environmentFx,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jetBlastFx,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n',
'jet blast layer order')
RUNTIME.write_text(r)

g=GRADLE.read_text()
g=rep(g,'        versionCode 96\n','        versionCode 97\n','version code')
g=rep(g,"        versionName '26.14-avm25.0-surface-material-realism'\n","        versionName '26.15-avm26.0-jet-blast-ground-effects'\n",'version name')
GRADLE.write_text(g)

print('v97 jet blast applied: heat distortion, runway dust, wet spray, grass/dirt plume and loose-object/FOD impulse')
