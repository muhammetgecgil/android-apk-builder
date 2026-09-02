from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/sim'
WEATHER=PKG/'WeatherEffectsView.java'
RUNTIME=PKG/'FlightRuntimeActivity.java'
JET=PKG/'Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v95 environment patch anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Pure model: weather visibility/wetness/light scheduling is unit-testable.
# ---------------------------------------------------------------------------
(SIM/'EnvironmentRealismModel.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** AVM-24 deterministic environment scheduling used by the visual layers. */
public final class EnvironmentRealismModel {
    private EnvironmentRealismModel(){}

    // Weather codes follow WeatherEffectsView: 0 clear, 1 cloudy, 2 rain, 3 snow.
    // Day codes: 0 dawn, 1 morning, 2 noon, 3 sunset, 4 evening, 5 night.
    public static double fog01(int weather,int dayPhase,double cloudCoverage,double bank){
        double f=0;
        if(weather==1)f=.08+.17*clamp01(cloudCoverage);
        else if(weather==2)f=.30+.28*clamp01(cloudCoverage);
        else if(weather==3)f=.42+.28*clamp01(cloudCoverage);
        if(dayPhase==0)f+=.12;
        return clamp01(f+Math.max(0,bank));
    }

    public static double visibilityMeters(int weather,int dayPhase,double cloudCoverage,double bank){
        double f=fog01(weather,dayPhase,cloudCoverage,bank);
        double base=weather==0?32000:weather==1?18000:weather==2?7600:5200;
        double v=base*(1.0-.72*f);
        return Math.max(1200,Math.min(35000,v));
    }

    public static double wetnessTarget(int weather){
        if(weather==2)return .94;
        if(weather==3)return .30;
        return 0;
    }

    public static double nightFactor(int dayPhase){
        if(dayPhase>=5)return 1.0;
        if(dayPhase==4)return .82;
        if(dayPhase==3)return .48;
        if(dayPhase==0)return .30;
        return .08;
    }

    public static double runwayLightGain(int dayPhase,int weather,double fog){
        double n=nightFactor(dayPhase);
        double badWx=weather==2||weather==3?.22:weather==1?.09:0;
        return clamp(.12+.78*n+badWx+.18*clamp01(fog),.12,1.0);
    }

    private static double clamp01(double v){return clamp(v,0,1);}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
''')

(TEST/'EnvironmentRealismModelTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class EnvironmentRealismModelTest {
    @Test public void rainCutsVisibilityAndCreatesWetRunway(){
        double clear=EnvironmentRealismModel.visibilityMeters(0,2,0,0);
        double rain=EnvironmentRealismModel.visibilityMeters(2,2,.9,.08);
        assertTrue(clear>25000);
        assertTrue(rain<7000);
        assertTrue(EnvironmentRealismModel.wetnessTarget(2)>.9);
    }

    @Test public void nightLightsAreStrongerThanNoon(){
        double noon=EnvironmentRealismModel.runwayLightGain(2,0,0);
        double night=EnvironmentRealismModel.runwayLightGain(5,0,0);
        assertTrue(night>noon+.6);
    }

    @Test public void fogBankReducesVisibility(){
        double clean=EnvironmentRealismModel.visibilityMeters(1,1,.5,0);
        double fog=EnvironmentRealismModel.visibilityMeters(1,1,.5,.45);
        assertTrue(fog<clean);
    }

    @Test public void dawnCarriesMoreFogThanNoon(){
        assertTrue(EnvironmentRealismModel.fog01(0,0,0,.08)>EnvironmentRealismModel.fog01(0,2,0,.08));
    }
}
''')

# ---------------------------------------------------------------------------
# Weather publishes visibility, fog, wetness and light-gain state.
# ---------------------------------------------------------------------------
w=WEATHER.read_text()
w=rep(w,
'import java.util.Locale;\n',
'import com.mg.fixturecockpitsim.sim.EnvironmentRealismModel;\n\nimport java.util.Locale;\n',
'environment model import')
w=rep(w,
'    private static volatile float sharedCloudLayerCoverage;\n',
'    private static volatile float sharedCloudLayerCoverage;\n    private static volatile int sharedWeatherCode=CLEAR,sharedDayPhase=MORNING;\n    private static volatile float sharedVisibilityM=32000f,sharedWetness01,sharedFog01,sharedNightFactor=.08f,sharedRunwayLightGain=.18f;\n',
'shared environment fields')
w=rep(w,
'    public static float getSharedCloudLayerCoverage(){return sharedCloudLayerCoverage;}\n    public static boolean hasSharedCloudLayer(){return sharedCloudLayerCoverage>=.36f;}\n',
'    public static float getSharedCloudLayerCoverage(){return sharedCloudLayerCoverage;}\n    public static boolean hasSharedCloudLayer(){return sharedCloudLayerCoverage>=.36f;}\n    public static int getSharedWeatherCode(){return sharedWeatherCode;}\n    public static int getSharedDayPhase(){return sharedDayPhase;}\n    public static float getSharedVisibilityM(){return sharedVisibilityM;}\n    public static float getSharedWetness01(){return sharedWetness01;}\n    public static float getSharedFog01(){return sharedFog01;}\n    public static float getSharedNightFactor(){return sharedNightFactor;}\n    public static float getSharedRunwayLightGain(){return sharedRunwayLightGain;}\n    public static boolean isSharedRain(){return sharedWeatherCode==RAIN;}\n    public static boolean isSharedNight(){return sharedNightFactor>.70f;}\n',
'environment getters')
w=rep(w,
'    private float windStrength=.25f;\n',
'    private float windStrength=.25f,wetness01;\n',
'wetness state')
w=rep(w,
'        super.onDraw(c);long now=System.currentTimeMillis();dayPhase=computeDayPhase(now);if(now>=nextWeatherChangeMs)chooseWeather(now,false);\n',
'        super.onDraw(c);long now=System.currentTimeMillis();dayPhase=computeDayPhase(now);if(now>=nextWeatherChangeMs)chooseWeather(now,false);updateSharedEnvironment(now);\n',
'environment update hook')
w=rep(w,
'    private int computeDayPhase(long now){float f=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(f<0)f+=1f;if(f<.14f)return DAWN;if(f<.34f)return MORNING;if(f<.58f)return NOON;if(f<.72f)return SUNSET;if(f<.82f)return EVENING;return NIGHT;}\n',
'''    private void updateSharedEnvironment(long now){
        sharedWeatherCode=weather;sharedDayPhase=dayPhase;
        float bank=(weather==RAIN||weather==CLOUDY||dayPhase==DAWN)?(.03f+.08f*(float)(.5+.5*Math.sin(now*.000045))):0f;
        sharedFog01=(float)EnvironmentRealismModel.fog01(weather,dayPhase,sharedCloudLayerCoverage,bank);
        sharedVisibilityM=(float)EnvironmentRealismModel.visibilityMeters(weather,dayPhase,sharedCloudLayerCoverage,bank);
        float target=(float)EnvironmentRealismModel.wetnessTarget(weather);wetness01+=(target-wetness01)*.025f;sharedWetness01=clamp(wetness01,0,1);
        sharedNightFactor=(float)EnvironmentRealismModel.nightFactor(dayPhase);
        sharedRunwayLightGain=(float)EnvironmentRealismModel.runwayLightGain(dayPhase,weather,sharedFog01);
    }

    private int computeDayPhase(long now){float f=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(f<0)f+=1f;if(f<.14f)return DAWN;if(f<.34f)return MORNING;if(f<.58f)return NOON;if(f<.72f)return SUNSET;if(f<.82f)return EVENING;return NIGHT;}\n''',
'environment scheduler')
WEATHER.write_text(w)

# ---------------------------------------------------------------------------
# Screen-space airport/environment realism layer. It is placed below the jet so
# runway/city/fog cues remain part of the world rather than floating on the aircraft.
# ---------------------------------------------------------------------------
(PKG/'EnvironmentRealismOverlayView.java').write_text(r'''package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/** AVM-24 airport surface, night-light, wet-runway, fog and low-altitude motion cues. */
public final class EnvironmentRealismOverlayView extends View {
    private static final float RUNWAY_HDG=270f;
    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Path path=new Path();
    private long lastNs;private float clock;

    public EnvironmentRealismOverlayView(Context c){super(c);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);long n=System.nanoTime();float dt=lastNs==0?.016f:Math.min(.05f,Math.max(.001f,(n-lastNs)/1e9f));lastNs=n;clock+=dt;
        int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        float night=WeatherEffectsView.getSharedNightFactor(),fog=WeatherEffectsView.getSharedFog01(),wet=WeatherEffectsView.getSharedWetness01();
        if(night>.10f&&!AirfieldWorldView.isSharedOnGround())drawCityLights(c,w,h,night,fog);
        RunwayFrame f=runwayFrame(w,h);if(f!=null){drawRunwayMicroDetail(c,w,h,f,wet);drawAirportLights(c,w,h,f,night,fog,wet);}
        drawLowAltitudeMotion(c,w,h);
        if(fog>.025f)drawFogBanks(c,w,h,fog,WeatherEffectsView.getSharedVisibilityM());
        postInvalidateOnAnimation();
    }

    private RunwayFrame runwayFrame(int w,int h){
        String ph=AirfieldWorldView.getSharedPhase();float alt=AirfieldWorldView.getSharedAltitudeM(),pitch=AirfieldWorldView.getSharedPitchDeg(),xtrk=AirfieldWorldView.getSharedCrossTrackM(),hdg=AirfieldWorldView.getSharedHeadingDeg();boolean ground=AirfieldWorldView.isSharedOnGround();
        boolean takeoff=ph.contains("RUNWAY_HOLD")||ph.contains("TAKEOFF_ROLL")||(ph.contains("ROTATE_CLIMB")&&alt<210f);
        boolean landing=!ph.contains("RWY_CAPTURE")&&!ph.contains("NAV_RECOVERY")&&(ph.equals("APPROACH")||ph.contains("APPROACH_MANUAL")||ph.contains("APPROACH_REMOTE")||ph.contains("FLARE")||ph.contains("ROLLOUT"));
        if(!takeoff&&!landing)return null;if(!ground&&landing&&(alt>650f||Math.abs(xtrk)>550f||Math.abs(angleError(hdg,RUNWAY_HDG))>30f))return null;
        if(!ground&&takeoff&&alt>210f)return null;
        float alt01=ground?0:clamp(alt/2200f,0,1),hz=h*(ground?.455f:lerp(.455f,.205f,alt01));if(!ground)hz+=h*clamp(pitch/32f,-.095f,.095f);
        if(landing&&!ground){float a=1-clamp((alt-7f)/(650f-7f),0,1);hz=lerp(h*.225f,h*.405f,a)+h*clamp(pitch/35f,-.060f,.060f);}
        float err=angleError(hdg,RUNWAY_HDG),lat=clamp(xtrk/42f,-2.2f,2.2f)*w*.22f,cx=w*.5f-lat+clamp(err/30f,-1,1)*w*.085f;
        float far,near,fy,ny;if(ground){far=w*.032f;near=w*.44f;fy=hz+h*.014f;ny=h*.999f;}else{float a=1-clamp((alt-7f)/(650f-7f),0,1),e=(float)Math.pow(a,.82);near=w*lerp(.020f,.405f,e);far=w*lerp(.009f,.040f,a);fy=lerp(hz+h*.016f,hz+h*.050f,a);ny=lerp(hz+h*.10f,h*.985f,(float)Math.pow(a,.74));}
        return new RunwayFrame(cx,fy,ny,far,near,ground,landing,ph);
    }

    private void drawRunwayMicroDetail(Canvas c,int w,int h,RunwayFrame f,float wet){
        path.reset();path.moveTo(f.cx-f.far,f.fy);path.lineTo(f.cx+f.far,f.fy);path.lineTo(f.cx+f.near,f.ny);path.lineTo(f.cx-f.near,f.ny);path.close();int s=c.save();c.clipPath(path);
        if(wet>.03f){p.setShader(new LinearGradient(0,f.fy,0,f.ny,new int[]{argb((int)(18+42*wet),0xaec5ce),argb((int)(8+24*wet),0x607078),0x00000000},null,Shader.TileMode.CLAMP));c.drawRect(f.cx-f.near,f.fy,f.cx+f.near,f.ny,p);p.setShader(null);}
        stroke.setStrokeWidth(Math.max(1f,w*.0007f));for(int i=0;i<24;i++){float q=.08f+i/25f*.88f,z=q*q,y=lerp(f.fy,f.ny,z),half=lerp(f.far,f.near,z),x=f.cx+(float)Math.sin(i*8.17+AirfieldWorldView.getSharedAlongTrackM()*.0017)*half*.72f;stroke.setColor(i%3==0?0x38202629:0x2b161a1c);path.reset();path.moveTo(x,y);path.lineTo(x+half*.07f*(i%2==0?1:-1),y+h*(.004f+.010f*z));path.lineTo(x-half*.04f,y+h*(.009f+.016f*z));c.drawPath(path,stroke);}
        // Large aiming points complement the existing touchdown-zone blocks.
        float q=.43f,z=q*q,y=lerp(f.fy,f.ny,z),half=lerp(f.far,f.near,z),bw=half*.17f,bh=h*(.009f+.024f*z);p.setColor(0xbff7f6ef);c.drawRect(f.cx-half*.52f-bw*.5f,y,f.cx-half*.52f+bw*.5f,y+bh,p);c.drawRect(f.cx+half*.52f-bw*.5f,y,f.cx+half*.52f+bw*.5f,y+bh,p);
        c.restoreToCount(s);
    }

    private void drawAirportLights(Canvas c,int w,int h,RunwayFrame f,float night,float fog,float wet){
        float gain=WeatherEffectsView.getSharedRunwayLightGain();if(gain<.14f)return;float phase=(AirfieldWorldView.getSharedAlongTrackM()/950f)%1f;if(phase<0)phase+=1;
        for(int i=0;i<34;i++){float q=(i/34f+phase)%1f,z=q*q,y=lerp(f.fy,f.ny,z),half=lerp(f.far,f.near,z),r=1.2f+4.8f*z;light(c,f.cx-half,y,r,0xf4f1db,gain,wet);light(c,f.cx+half,y,r,0xf4f1db,gain,wet);if(i%2==0){int col=q>.82f?0xff5b52:0xf7f5e8;light(c,f.cx,y,r*.76f,col,gain*.92f,wet);}}
        if(f.landing){for(int i=0;i<18;i++){float q=i/18f,y=f.fy-h*(.010f+.20f*q*q),spread=w*(.003f+.030f*q);float r=1.2f+2.4f*q;light(c,f.cx-spread,y,r,0xf6f1d8,gain,wet);light(c,f.cx+spread,y,r,0xf6f1d8,gain,wet);if(i%3==0)light(c,f.cx,y,r,0xf6f1d8,gain,wet);}}
        if(f.phase.contains("TAXI")){for(int i=0;i<18;i++){float q=i/18f,z=q*q,y=lerp(f.fy,f.ny,z),half=lerp(f.far*1.45f,f.near*1.15f,z);light(c,f.cx-half,y,1.5f+3*z,0x64aef9,gain*.85f,wet);light(c,f.cx+half,y,1.5f+3*z,0x64aef9,gain*.85f,wet);light(c,f.cx,y,1.2f+2.4f*z,0x50d993,gain*.72f,wet);}}
    }

    private void light(Canvas c,float x,float y,float r,int rgb,float gain,float wet){int core=(int)(235*gain),halo=(int)(54*gain);p.setColor(argb(halo,rgb));c.drawCircle(x,y,r*2.6f,p);p.setColor(argb(core,rgb));c.drawCircle(x,y,r,p);if(wet>.08f){stroke.setColor(argb((int)(72*gain*wet),rgb));stroke.setStrokeWidth(Math.max(1f,r*.62f));c.drawLine(x,y+r*1.2f,x,y+r*(4.5f+8f*wet),stroke);}}

    private void drawCityLights(Canvas c,int w,int h,float night,float fog){float vis=night*(1-.58f*fog),hz=h*.42f,travel=AirfieldWorldView.getSharedAlongTrackM()*.014f;for(int i=0;i<72;i++){float x=((i*173.3f+travel*(i%3+1))%w+w)%w,y=hz+h*(.03f+((i*37)%44)/100f),r=1f+(i%5==0?1.8f:.55f);int rgb=i%7==0?0xffb76a:i%5==0?0xaed8ff:0xffdc83;p.setColor(argb((int)((60+(i%4)*35)*vis),rgb));c.drawCircle(x,y,r,p);if(i%11==0){stroke.setColor(argb((int)(28*vis),rgb));stroke.setStrokeWidth(1f);c.drawLine(x,y,x,y+h*.018f,stroke);}}}

    private void drawLowAltitudeMotion(Canvas c,int w,int h){float alt=AirfieldWorldView.getSharedAltitudeM(),sp=AirfieldWorldView.getSharedSpeedMps();if(AirfieldWorldView.isSharedOnGround()||alt>220||sp<105)return;float k=clamp((220-alt)/220f,0,1)*clamp((sp-105)/260f,0,1),flow=(clock*(.7f+sp/120f))%1f;for(int i=0;i<20;i++){float q=(i/20f+flow)%1f,z=q*q,y=lerp(h*.52f,h*.995f,z),len=w*(.006f+.045f*z)*k,x=w*(.08f+((i*53)%84)/100f);stroke.setColor(argb((int)(18+54*k*z),0xd8e1d6));stroke.setStrokeWidth(1f+2f*z);c.drawLine(x,y,x+(x<w*.5f?-len:len),y+len*.20f,stroke);}}

    private void drawFogBanks(Canvas c,int w,int h,float fog,float visibility){float horizon=h*(.30f+clamp(AirfieldWorldView.getSharedPitchDeg()/45f,-.07f,.07f)),a=clamp(fog*(1.15f+clamp((6000f-visibility)/6000f,0,.55f)),0,1);p.setShader(new LinearGradient(0,horizon-h*.16f,0,h,new int[]{0x00dce5e5,argb((int)(120*a),0xc9d2d2),argb((int)(52*a),0xdce3e2),0x00dce5e5},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);for(int i=0;i<5;i++){float x=((i*.29f+(clock*.004f*(i%2==0?1:-1)))%1f)*w,y=h*(.34f+.06f*(i%3)),rw=w*(.22f+.05f*(i%2)),rh=h*(.055f+.018f*(i%3));p.setColor(argb((int)(22*a),0xe8eeee));c.drawOval(x-rw*.5f,y-rh*.5f,x+rw*.5f,y+rh*.5f,p);}}

    private static int argb(int a,int rgb){a=Math.max(0,Math.min(255,a));return (a<<24)|(rgb&0x00ffffff);}
    private static float angleError(float a,float b){float d=(a-b)%360f;if(d>180)d-=360;if(d<-180)d+=360;return d;}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}

    private static final class RunwayFrame{final float cx,fy,ny,far,near;final boolean ground,landing;final String phase;RunwayFrame(float cx,float fy,float ny,float far,float near,boolean ground,boolean landing,String phase){this.cx=cx;this.fy=fy;this.ny=ny;this.far=far;this.near=near;this.ground=ground;this.landing=landing;this.phase=phase;}}
}
''')

# ---------------------------------------------------------------------------
# Runtime integration and cinematic camera cycling.
# ---------------------------------------------------------------------------
r=RUNTIME.read_text()
r=rep(r,
'    private SupersonicEffectsView supersonicFx;\n',
'    private SupersonicEffectsView supersonicFx;\n    private EnvironmentRealismOverlayView environmentFx;\n',
'environment overlay field')
r=rep(r,
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);supersonicFx=new SupersonicEffectsView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(supersonicFx,new FrameLayout.LayoutParams(-1,-1));\n',
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);environmentFx=new EnvironmentRealismOverlayView(this);jet=new Jet3DView(this);supersonicFx=new SupersonicEffectsView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(environmentFx,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(supersonicFx,new FrameLayout.LayoutParams(-1,-1));\n',
'environment overlay layer')
r=rep(r,
'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%5;jet.setCameraMode(cameraMode);});',
'cam.setOnClickListener(v->{cameraMode=(cameraMode+1)%12;jet.setCameraMode(cameraMode);});',
'cinematic camera cycle')
r=rep(r,
'        sound.setSupersonicState(state.mach,state.sonicBoomPulse,jet!=null&&jet.getCameraMode()==Jet3DView.CAMERA_GROUND_OBSERVER);\n',
'        sound.setSupersonicState(state.mach,state.sonicBoomPulse,jet!=null&&jet.isWorldFixedObserverCamera());\n',
'observer audio camera policy')
RUNTIME.write_text(r)

# ---------------------------------------------------------------------------
# Camera package: add low chase, wing, fly-by, runway, tower, orbit and auto cinema.
# ---------------------------------------------------------------------------
j=JET.read_text()
j=rep(j,
'    public static final int CAMERA_CHASE=0, CAMERA_REAR=1, CAMERA_RIGHT_QUARTER=2, CAMERA_LEFT_QUARTER=3, CAMERA_GROUND_OBSERVER=4;\n',
'    public static final int CAMERA_CHASE=0, CAMERA_REAR=1, CAMERA_RIGHT_QUARTER=2, CAMERA_LEFT_QUARTER=3, CAMERA_GROUND_OBSERVER=4, CAMERA_LOW_CHASE=5, CAMERA_WING=6, CAMERA_FLY_BY=7, CAMERA_RUNWAY=8, CAMERA_TOWER=9, CAMERA_ORBIT=10, CAMERA_CINEMA=11;\n',
'camera constants')
j=rep(j,
'    public void setWheelSpeed(float v){r.ws=Math.max(0,v);}\n    public void setSupersonicState(float mach,float buffet,float boom){r.sup(mach,buffet,boom);}\n    public void setCameraMode(int m){r.cam=Math.max(0,Math.min(4,m));}\n    public int getCameraMode(){return r.cam;}\n    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.cam=(r.cam+1)%5;return true;}\n',
'    public void setWheelSpeed(float v){r.ws=Math.max(0,v);}\n    public void setSupersonicState(float mach,float buffet,float boom){r.sup(mach,buffet,boom);}\n    public void setCameraMode(int m){r.cam=Math.max(0,Math.min(11,m));}\n    public int getCameraMode(){return r.cam;}\n    public boolean isWorldFixedObserverCamera(){int c=r.cam;return c==CAMERA_GROUND_OBSERVER||c==CAMERA_FLY_BY||c==CAMERA_RUNWAY||c==CAMERA_TOWER||c==CAMERA_CINEMA;}\n    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.cam=(r.cam+1)%12;return true;}\n',
'camera API')
j=rep(j,
'            float sp=cl(speed/520f,0,1),machBoost=cl((mach-.78f)/.62f,0,1),fov=29.5f+sp*3.6f+machBoost*4.8f+(cam==CAMERA_GROUND_OBSERVER?2.2f:0f);Matrix.perspectiveM(pr,0,fov,aspect,.08f,260f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);\n',
'            float sp=cl(speed/520f,0,1),machBoost=cl((mach-.78f)/.62f,0,1),observerFov=(cam==CAMERA_GROUND_OBSERVER||cam==CAMERA_FLY_BY||cam==CAMERA_RUNWAY||cam==CAMERA_TOWER||cam==CAMERA_CINEMA?2.2f:0f),fov=29.5f+sp*3.6f+machBoost*4.8f+observerFov;Matrix.perspectiveM(pr,0,fov,aspect,.08f,280f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);\n',
'cinematic FOV')
old_cam='''        void camera(float sp){float lag=sp*1.65f+cl((mach-1f)/.8f,0,1)*.65f,bob=(float)Math.sin(t*1.5f)*.018f*(1-sp);if(cam==1){camX=0;camY=1.50f;camZ=12.8f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.02f,1.42f,0,1,0);}else if(cam==2){camX=11.6f+sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else if(cam==3){camX=-11.6f-sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else if(cam==CAMERA_GROUND_OBSERVER){camX=8.9f;camY=1.18f;camZ=8.0f;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.35f,0,1,0);}else{camX=0;camY=4.72f+bob;camZ=18.2f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.10f,-.65f-sp*.20f,0,1,0);}}\n'''
new_cam='''        void camera(float sp){float lag=sp*1.65f+cl((mach-1f)/.8f,0,1)*.65f,bob=(float)Math.sin(t*1.5f)*.018f*(1-sp);int mode=cam;if(mode==CAMERA_CINEMA){int q=((int)(t/7.0f))%6;mode=q==0?CAMERA_LOW_CHASE:q==1?CAMERA_WING:q==2?CAMERA_FLY_BY:q==3?CAMERA_TOWER:q==4?CAMERA_ORBIT:CAMERA_RUNWAY;}if(mode==CAMERA_REAR){camX=0;camY=1.50f;camZ=12.8f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.02f,1.42f,0,1,0);}else if(mode==CAMERA_RIGHT_QUARTER){camX=11.6f+sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else if(mode==CAMERA_LEFT_QUARTER){camX=-11.6f-sp*.7f;camY=4.55f;camZ=12.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.12f,0,1,0);}else if(mode==CAMERA_GROUND_OBSERVER){camX=8.9f;camY=1.18f;camZ=8.0f;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.05f,.35f,0,1,0);}else if(mode==CAMERA_LOW_CHASE){camX=0;camY=1.32f+bob*.2f;camZ=16.5f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.20f,-.40f,0,1,0);}else if(mode==CAMERA_WING){camX=-7.2f;camY=1.30f;camZ=3.6f;Matrix.setLookAtM(vw,0,camX,camY,camZ,1.6f,.25f,-1.2f,0,1,0);}else if(mode==CAMERA_FLY_BY){float a=t*.22f;camX=(float)Math.sin(a)*19f;camY=2.3f+(float)Math.sin(a*.6f)*1.5f;camZ=(float)Math.cos(a)*20f;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.20f,0,0,1,0);}else if(mode==CAMERA_RUNWAY){camX=-7.8f;camY=.62f;camZ=25.5f;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.85f,-1.5f,0,1,0);}else if(mode==CAMERA_TOWER){camX=17.5f;camY=8.8f;camZ=22.0f;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.55f,0,0,1,0);}else if(mode==CAMERA_ORBIT){float a=t*.30f;camX=(float)Math.sin(a)*14.5f;camY=4.6f+(float)Math.sin(t*.17f)*2.0f;camZ=(float)Math.cos(a)*16.5f;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.20f,0,0,1,0);}else{camX=0;camY=4.72f+bob;camZ=18.2f+lag;Matrix.setLookAtM(vw,0,camX,camY,camZ,0,.10f,-.65f-sp*.20f,0,1,0);}}\n'''
j=rep(j,old_cam,new_cam,'cinematic camera geometry')
JET.write_text(j)

# v94 is applied immediately before this patch in CI.
g=GRADLE.read_text()
g=rep(g,'        versionCode 94\n','        versionCode 95\n','version code')
g=rep(g,"        versionName '26.12-avm23.0-supersonic-flight'\n","        versionName '26.13-avm24.0-airfield-environment-realism'\n",'version name')
GRADLE.write_text(g)

print('v95 airfield/environment realism applied: visibility/fog, wet runway reflections, airport/city lights, runway microdetail, low-altitude speed cues and 12-mode cinematic camera package')
