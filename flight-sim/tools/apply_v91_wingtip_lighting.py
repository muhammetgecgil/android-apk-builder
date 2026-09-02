from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'
OVERLAY=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FighterLightingOverlayView.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v91 wingtip-lighting patch anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Publish real projected aircraft hardpoints from the OpenGL renderer.
# The wingtip hardpoints match the physical light/fairing housings already
# present in AdvancedAirframeOverlay at x=+/-5.18, y=.18, z=-.03.
# ---------------------------------------------------------------------------
j=JET.read_text()
j=rep(j,
'    public int getCameraMode(){return r.cam;}\n',
'    public int getCameraMode(){return r.cam;}\n    public boolean getLightingAnchors(float[] out){return r.copyLightingAnchors(out);}\n',
'public lighting anchors')

j=rep(j,
'        long last;\n',
'        long last;\n        volatile boolean lightingAnchorsValid;\n        volatile float lightPortX=.31f,lightPortY=.47f,lightStarX=.69f,lightStarY=.47f,lightTailX=.50f,lightTailY=.39f,lightBeaconX=.50f,lightBeaconY=.41f,lightGearX=.50f,lightGearY=.50f;\n        final float[] lightTmp=new float[10],lightVec=new float[4],lightClip=new float[4];\n',
'lighting anchor fields')

j=rep(j,
'            float runwayRelativeYaw=shortest(yaw-270f);Matrix.rotateM(md,0,-runwayRelativeYaw*.12f,0,1,0);Matrix.rotateM(md,0,pitch+vertical*.007f,1,0,0);Matrix.rotateM(md,0,-roll,0,0,1);Matrix.multiplyMM(mvp,0,vp,0,md,0);\n',
'            float runwayRelativeYaw=shortest(yaw-270f);Matrix.rotateM(md,0,-runwayRelativeYaw*.12f,0,1,0);Matrix.rotateM(md,0,pitch+vertical*.007f,1,0,0);Matrix.rotateM(md,0,-roll,0,0,1);Matrix.multiplyMM(mvp,0,vp,0,md,0);updateLightingAnchors();\n',
'project anchors each frame')

anchor_methods='''        boolean copyLightingAnchors(float[] out){
            if(out==null||out.length<10||!lightingAnchorsValid)return false;
            out[0]=lightPortX;out[1]=lightPortY;out[2]=lightStarX;out[3]=lightStarY;out[4]=lightTailX;out[5]=lightTailY;out[6]=lightBeaconX;out[7]=lightBeaconY;out[8]=lightGearX;out[9]=lightGearY;return true;
        }
        private boolean projectLight(float x,float y,float z,int off){
            lightVec[0]=x;lightVec[1]=y;lightVec[2]=z;lightVec[3]=1f;Matrix.multiplyMV(lightClip,0,mvp,0,lightVec,0);float w=lightClip[3];if(w<=.05f)return false;
            float nx=lightClip[0]/w,ny=lightClip[1]/w;if(Math.abs(nx)>1.35f||Math.abs(ny)>1.35f)return false;
            lightTmp[off]=nx*.5f+.5f;lightTmp[off+1]=.5f-ny*.5f;return true;
        }
        private void updateLightingAnchors(){
            boolean ok=projectLight(-5.18f,.18f,-.03f,0)&&projectLight(5.18f,.18f,-.03f,2)&&projectLight(0f,1.55f,2.55f,4)&&projectLight(0f,1.03f,-.35f,6)&&projectLight(0f,-.38f,-3.62f,8);
            if(ok){lightPortX=lightTmp[0];lightPortY=lightTmp[1];lightStarX=lightTmp[2];lightStarY=lightTmp[3];lightTailX=lightTmp[4];lightTailY=lightTmp[5];lightBeaconX=lightTmp[6];lightBeaconY=lightTmp[7];lightGearX=lightTmp[8];lightGearY=lightTmp[9];lightingAnchorsValid=true;}else lightingAnchorsValid=false;
        }

'''
j=rep(j,
'        private void bindAndDraw(FloatBuffer b,int vertices){',
anchor_methods+'        private void bindAndDraw(FloatBuffer b,int vertices){',
'lighting anchor projection methods')
JET.write_text(j)

# ---------------------------------------------------------------------------
# Bind the lighting overlay to the actual 3D aircraft renderer.
# ---------------------------------------------------------------------------
r=RUNTIME.read_text()
r=rep(r,
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);lightingOverlay=new FighterLightingOverlayView(this,lighting);\n',
'        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);lightingOverlay=new FighterLightingOverlayView(this,lighting);lightingOverlay.bindAircraft(jet);\n',
'bind overlay to aircraft')
RUNTIME.write_text(r)

# ---------------------------------------------------------------------------
# Replace the oversized camera-relative blobs with compact lights attached to
# projected aircraft hardpoints. Existing physical lamp housings stay visible;
# this overlay only adds a restrained emissive core + soft halo.
# ---------------------------------------------------------------------------
OVERLAY.write_text(r'''package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import com.mg.fixturecockpitsim.sim.FighterLightingSystem;

/** Fighter lights attached to real projected aircraft hardpoints. */
public final class FighterLightingOverlayView extends View {
    private final FighterLightingSystem lights;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] anchor=new float[10];
    private Jet3DView aircraft;
    private int cameraMode=Jet3DView.CAMERA_CHASE;
    private boolean onGround=true;
    private float gear=1f,speed;

    public FighterLightingOverlayView(Context context,FighterLightingSystem system){super(context);lights=system;setClickable(false);setFocusable(false);}
    public void bindAircraft(Jet3DView view){aircraft=view;}

    public void setAircraftState(int camera,boolean ground,float gearPosition,float speedMps){cameraMode=Math.max(0,Math.min(3,camera));onGround=ground;gear=Math.max(0,Math.min(1,gearPosition));speed=Math.max(0,speedMps);invalidate();}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);final float w=getWidth(),h=getHeight();if(w<=1||h<=1)return;final long now=SystemClock.elapsedRealtime();

        boolean attached=aircraft!=null&&aircraft.getLightingAnchors(anchor);
        float portX,portY,starX,starY,tailX,tailY,beaconX,beaconY,gearX,gearY;
        if(attached){
            portX=anchor[0]*w;portY=anchor[1]*h;starX=anchor[2]*w;starY=anchor[3]*h;tailX=anchor[4]*w;tailY=anchor[5]*h;beaconX=anchor[6]*w;beaconY=anchor[7]*h;gearX=anchor[8]*w;gearY=anchor[9]*h;
        }else{
            // Conservative fallback used only before the first valid GL frame.
            float cx=.50f*w,cy=.44f*h,span=.185f*w;if(cameraMode==Jet3DView.CAMERA_REAR)span=.195f*w;else if(cameraMode==Jet3DView.CAMERA_RIGHT_QUARTER){cx=.47f*w;span=.175f*w;}else if(cameraMode==Jet3DView.CAMERA_LEFT_QUARTER){cx=.53f*w;span=.175f*w;}
            portX=cx-span;portY=cy+.045f*h;starX=cx+span;starY=portY;tailX=cx;tailY=cy-.050f*h;beaconX=cx;beaconY=cy-.018f*h;gearX=cx;gearY=cy+.055f*h;
        }

        float land=(float)lights.landingIntensity(gear),taxi=(float)lights.taxiIntensity(gear,onGround);
        if(land>.01f)drawBeam(c,gearX,gearY,land,false);if(taxi>.01f)drawBeam(c,gearX,gearY,taxi,true);
        if(lights.formation)drawFormation(c,portX,portY,starX,starY,tailX,tailY);

        if(lights.navigation){
            // Small emissive cores reinforce the real wingtip housings without creating detached blobs.
            glow(c,portX,portY,Color.rgb(255,36,30),2.8f,.72f,1.85f);
            glow(c,starX,starY,Color.rgb(28,255,76),2.8f,.72f,1.85f);
            glow(c,tailX,tailY,Color.WHITE,2.3f,.52f,1.70f);
        }

        float st=(float)lights.strobeIntensity(now);if(st>0){glow(c,portX,portY,Color.WHITE,5.0f,st*.82f,2.0f);glow(c,starX,starY,Color.WHITE,5.0f,st*.82f,2.0f);}
        float bc=(float)lights.beaconIntensity(now);if(bc>0)glow(c,beaconX,beaconY,Color.rgb(255,42,28),4.2f,bc*.62f,1.9f);
        if(lights.strobe||lights.beacon)postInvalidateDelayed(35);
    }

    private void drawFormation(Canvas c,float px,float py,float sx,float sy,float tx,float ty){
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(Math.max(1.5f,getWidth()*.0018f));p.setColor(Color.argb(92,154,238,116));
        float cx=(px+sx)*.5f,cy=(py+sy)*.5f;c.drawLine(px+(cx-px)*.18f,py+(cy-py)*.18f,px+(cx-px)*.42f,py+(cy-py)*.42f,p);c.drawLine(sx+(cx-sx)*.18f,sy+(cy-sy)*.18f,sx+(cx-sx)*.42f,sy+(cy-sy)*.42f,p);c.drawLine(cx,cy,tx+(cx-tx)*.28f,ty+(cy-ty)*.28f,p);p.setStyle(Paint.Style.FILL);
    }

    private void drawBeam(Canvas c,float x,float y,float intensity,boolean taxi){
        float h=getHeight(),w=getWidth(),len=(taxi?.16f:.31f)*h,half=(taxi?.036f:.060f)*w;Path path=new Path();path.moveTo(x,y);path.lineTo(x-half,y+len);path.lineTo(x+half,y+len);path.close();int a=(int)(intensity*(taxi?44:72));int near=Color.argb(a,255,250,220),far=Color.argb(0,255,250,220);p.setShader(new LinearGradient(x,y,x,y+len,near,far,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawPath(path,p);p.setShader(null);glow(c,x,y,Color.rgb(255,248,215),taxi?4.2f:5.2f,intensity*.72f,1.9f);
    }

    private void glow(Canvas c,float x,float y,int color,float radiusDp,float intensity,float haloScale){
        float d=getResources().getDisplayMetrics().density,r=Math.max(2f,radiusDp*d);intensity=Math.max(0,Math.min(1,intensity));int rgb=color&0x00ffffff,cr=(rgb>>16)&255,cg=(rgb>>8)&255,cb=rgb&255;float outer=r*Math.max(1.35f,haloScale);int center=Color.argb((int)(170*intensity),cr,cg,cb),edge=Color.argb(0,cr,cg,cb);p.setShader(new RadialGradient(x,y,outer,center,edge,Shader.TileMode.CLAMP));c.drawCircle(x,y,outer,p);p.setShader(null);p.setColor(Color.argb((int)(245*intensity),cr,cg,cb));c.drawCircle(x,y,r*.34f,p);
    }
}
''')

g=GRADLE.read_text();g=rep(g,'        versionCode 90\n','        versionCode 91\n','version code');g=rep(g,"        versionName '26.8-avm20.0-fighter-lighting'\n","        versionName '26.9-avm20.1-attached-wingtip-lighting'\n",'version name');GRADLE.write_text(g)

print('v91 wingtip lighting applied: real projected hardpoint attachment, compact nav glow, restrained strobe/beacon and attached landing/taxi origins')
