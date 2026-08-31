package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/**
 * AVM-15.1 airport world.
 * The runway exists only inside a true takeoff window or a geometrically valid final approach.
 * Mid-flight runway capture/recovery never exposes runway scenery.
 * A perspective 3-D control tower is visible beside the departure runway and fades after pass-by.
 */
public final class AirfieldWorldView extends View {
    private static final float TAKEOFF_FADE_ALT_M=210f;
    private static final float FINAL_ALT_M=650f;
    private static final float FINAL_XTRACK_M=550f;
    private static final float FINAL_HDG_ERR_DEG=30f;
    private static final float RUNWAY_HDG=270f;
    private static final float TOWER_ALONG_M=270f;

    private static volatile float sharedAltitudeM,sharedSpeedMps,sharedHeadingDeg,sharedPitchDeg,sharedCrossTrackM,sharedAlongTrackM;
    private static volatile boolean sharedOnGround=true,sharedCrashed;
    private static volatile String sharedPhase="",sharedCrashReason="";

    public static float getSharedAltitudeM(){return sharedAltitudeM;}
    public static float getSharedSpeedMps(){return sharedSpeedMps;}
    public static float getSharedHeadingDeg(){return sharedHeadingDeg;}
    public static float getSharedPitchDeg(){return sharedPitchDeg;}
    public static float getSharedCrossTrackM(){return sharedCrossTrackM;}
    public static float getSharedAlongTrackM(){return sharedAlongTrackM;}
    public static boolean isSharedOnGround(){return sharedOnGround;}
    public static boolean isSharedCrashed(){return sharedCrashed;}
    public static String getSharedPhase(){return sharedPhase;}
    public static String getSharedCrashReason(){return sharedCrashReason;}

    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Path path=new Path();
    private volatile float altitudeM,speedMps,headingDeg,pitchDeg,crossTrackM,alongTrackM;
    private volatile boolean onGround,crashed;
    private volatile String phase="",crashReason="";
    private long lastNs;
    private float runwayFlow,groundFlow;

    public AirfieldWorldView(Context c){
        super(c);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
        stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setState(double altitude,double speed,boolean ground,String scenePhase,double heading,double pitch,double crossTrack,double alongTrack,boolean crash,String reason){
        altitudeM=(float)Math.max(0,altitude);speedMps=(float)Math.max(0,speed);onGround=ground;
        phase=scenePhase==null?"":scenePhase;headingDeg=(float)heading;pitchDeg=(float)pitch;
        crossTrackM=(float)crossTrack;alongTrackM=(float)alongTrack;crashed=crash;crashReason=reason==null?"":reason;
        sharedAltitudeM=altitudeM;sharedSpeedMps=speedMps;sharedOnGround=onGround;sharedPhase=phase;
        sharedHeadingDeg=headingDeg;sharedPitchDeg=pitchDeg;sharedCrossTrackM=crossTrackM;sharedAlongTrackM=alongTrackM;
        sharedCrashed=crashed;sharedCrashReason=crashReason;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime();
        float dt=lastNs==0?.016f:Math.min(.05f,Math.max(.001f,(now-lastNs)/1e9f));lastNs=now;
        float groundInfluence=onGround?1f:clamp(1f-altitudeM/165f,0f,1f);
        float optical=speedMps*groundInfluence*(onGround?1.7f:1f);
        runwayFlow=(runwayFlow+optical*dt/72f)%1f;groundFlow=(groundFlow+optical*dt/92f)%1f;

        int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        boolean approach=isFinalApproach();
        float alt01=onGround?0f:clamp(altitudeM/2200f,0f,1f);
        float hz=h*(onGround?.455f:lerp(.455f,.205f,alt01));
        if(!onGround)hz+=h*clamp(pitchDeg/32f,-.095f,.095f);
        if(approach){float a=approach01();hz=lerp(h*.225f,h*.405f,a)+h*clamp(pitchDeg/35f,-.060f,.060f);}

        drawSky(c,w,h,hz);
        drawBaseTerrain(c,w,h,hz);
        if(onGround&&speedMps>.6f)drawGroundOpticalFlow(c,w,h,hz);

        if(taxiPhase())drawTaxiway(c,w,h,hz);
        if(runwayVisible())drawRunway(c,w,h,hz,onGround);
        if(towerVisible())drawTakeoffTower(c,w,h,hz);

        if(crashed&&!CinematicEnvironmentView.isWaterCrashActive())drawCrash(c,w,h);
        if(speedMps>.25f||crashed||towerVisible())postInvalidateOnAnimation();
    }

    private boolean taxiPhase(){return onGround&&(phase.contains("TAXI_OUT")||phase.contains("TAXI_IN"));}
    private boolean takeoffPhase(){
        return phase.contains("RUNWAY_HOLD")||phase.contains("TAKEOFF_ROLL")||
                (phase.contains("ROTATE_CLIMB")&&altitudeM<TAKEOFF_FADE_ALT_M);
    }
    private boolean landingPhase(){
        if(phase.contains("RWY_CAPTURE")||phase.contains("NAV_RECOVERY"))return false;
        return phase.equals("APPROACH")||phase.contains("APPROACH_MANUAL")||phase.contains("APPROACH_REMOTE")||
                phase.contains("FLARE")||phase.contains("ROLLOUT");
    }
    private boolean runwayVisible(){
        if(takeoffPhase()){
            if(onGround)return true;
            return altitudeM<TAKEOFF_FADE_ALT_M&&Math.abs(crossTrackM)<240f&&Math.abs(angleError(headingDeg,RUNWAY_HDG))<95f;
        }
        if(!landingPhase())return false;
        if(onGround)return phase.contains("ROLLOUT");
        return altitudeM<FINAL_ALT_M&&Math.abs(crossTrackM)<FINAL_XTRACK_M&&Math.abs(angleError(headingDeg,RUNWAY_HDG))<FINAL_HDG_ERR_DEG;
    }
    private boolean isFinalApproach(){return !onGround&&landingPhase()&&runwayVisible();}
    private float approach01(){return 1f-clamp((altitudeM-7f)/(FINAL_ALT_M-7f),0f,1f);}

    private boolean towerVisible(){
        if(!(phase.contains("RUNWAY_HOLD")||phase.contains("TAKEOFF_ROLL")||phase.contains("ROTATE_CLIMB")))return false;
        if(altitudeM>150f)return false;
        float rel=TOWER_ALONG_M-alongTrackM;
        return rel>-250f&&rel<620f;
    }

    private void drawSky(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{0xff176b9e,0xff4caed0,0xffc8e9ef},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,hz,p);p.setShader(null);
        for(int i=0;i<5;i++){float x=(w*(.08f+i*.21f)+alongTrackM*.06f)%Math.max(1,w),y=hz*(.13f+(i%2)*.08f),ww=w*(.075f+(i%2)*.025f);p.setColor(0x24ffffff);c.drawOval(x,y,x+ww,y+hz*.018f,p);}
    }

    private void drawBaseTerrain(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff78906a,0xff587553,0xff3d5d44},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,h,p);p.setShader(null);
        drawMountains(c,w,h,hz,.15f);
        float gh=h-hz;
        for(int i=0;i<26;i++){float q=.10f+((i*29)%80)/100f,y=hz+gh*q*q,x=(i*123f-alongTrackM*.035f)%Math.max(1,w);if(x<0)x+=w;p.setColor(i%2==0?0x295a744d:0x21496843);c.drawOval(x,y,x+w*(.008f+.025f*q),y+h*(.003f+.006f*q),p);}
    }

    private void drawGroundOpticalFlow(Canvas c,int w,int h,float hz){
        float s=clamp(speedMps/48f,0,1);
        for(int i=0;i<40;i++){float q=(i/40f+groundFlow)%1f,z=q*q,y=lerp(hz+h*.02f,h*.999f,z),len=w*(.008f+.055f*z)*(.35f+s),lx=w*(.025f+((i*37)%25)/100f),rx=w-lx;stroke.setColor(z>.5f?0x587d9c6c:0x3289a577);stroke.setStrokeWidth(Math.max(1f,w*(.0007f+.0026f*z)));c.drawLine(lx,y,lx-len,y+len*.24f,stroke);c.drawLine(rx,y,rx+len,y+len*.24f,stroke);}
    }

    private void drawTaxiway(Canvas c,int w,int h,float hz){
        float fy=hz+h*.016f,ny=h*.999f,far=w*.018f,near=w*.255f,bend=(float)Math.sin(alongTrackM*.025f)*w*.018f,fc=w*.5f-bend*.2f,nc=w*.5f+bend;
        quad(c,fc-far*1.45f,fy,fc+far*1.45f,fy,nc+near*1.12f,ny,nc-near*1.12f,ny,0xff8b8f87);
        path.reset();path.moveTo(fc-far,fy);path.lineTo(fc+far,fy);path.lineTo(nc+near,ny);path.lineTo(nc-near,ny);path.close();
        p.setShader(new LinearGradient(0,fy,0,ny,new int[]{0xff505457,0xff393e40,0xff292e30},null,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);
        path.reset();path.moveTo(fc,fy);path.quadTo(w*.5f-bend*.35f,lerp(fy,ny,.48f),nc,ny);stroke.setColor(0xffffd448);stroke.setStrokeWidth(Math.max(2f,w*.0030f));c.drawPath(path,stroke);
        for(int i=0;i<26;i++){float q=(i/26f+runwayFlow)%1f,z=q*q,y=lerp(fy,ny,z),half=lerp(far,near,z),cx=lerp(fc,nc,z),r=1+4.5f*z;p.setColor(0xff70b9ff);c.drawCircle(cx-half,y,r,p);c.drawCircle(cx+half,y,r,p);}
    }

    private void drawRunway(Canvas c,int w,int h,float hz,boolean ground){
        if(!runwayVisible())return;
        float err=angleError(headingDeg,RUNWAY_HDG),lat=clamp(crossTrackM/42f,-2.2f,2.2f)*w*.22f,cx=w*.5f-lat+clamp(err/30f,-1,1)*w*.085f;
        float far,near,fy,ny;
        if(ground){far=w*.032f;near=w*.44f;fy=hz+h*.014f;ny=h*.999f;}
        else{float a=approach01(),e=(float)Math.pow(a,.82);near=w*lerp(.020f,.405f,e);far=w*lerp(.009f,.040f,a);fy=lerp(hz+h*.016f,hz+h*.050f,a);ny=lerp(hz+h*.10f,h*.985f,(float)Math.pow(a,.74));}

        quad(c,cx-far*1.55f,fy,cx+far*1.55f,fy,cx+near*1.22f,ny,cx-near*1.22f,ny,0xff8b8e88);
        path.reset();path.moveTo(cx-far,fy);path.lineTo(cx+far,fy);path.lineTo(cx+near,ny);path.lineTo(cx-near,ny);path.close();
        p.setShader(new LinearGradient(cx,fy,cx,ny,new int[]{0xff515457,0xff393d40,0xff252a2d},null,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);

        drawRubberAndSurface(c,w,h,cx,fy,ny,far,near,ground);
        stroke.setColor(0xfff5f4ee);stroke.setStrokeWidth(Math.max(2f,w*.0025f));c.drawLine(cx-far,fy,cx-near,ny,stroke);c.drawLine(cx+far,fy,cx+near,ny,stroke);

        for(int i=0;i<23;i++){float q=(i/23f+runwayFlow*1.04f)%1f,z=q*q,y=lerp(fy,ny,z),dash=lerp(3f,62f,z);stroke.setColor(0xfff7f6f0);stroke.setStrokeWidth(1.5f+10*z);c.drawLine(cx,y,cx,Math.min(ny,y+dash),stroke);}
        for(int i=0;i<32;i++){float q=(i/32f+runwayFlow)%1f,z=q*q,y=lerp(fy,ny,z),hh=lerp(far,near,z),r=1f+5f*z;p.setColor(0xfff6f1dc);c.drawCircle(cx-hh,y,r,p);c.drawCircle(cx+hh,y,r,p);}

        drawThresholdAndTouchdown(c,w,h,cx,fy,ny,far,near,ground);
        drawMovingSideBars(c,w,h,cx,fy,ny,far,near,ground);
        if(!ground&&isFinalApproach())drawPapi(c,w,h,cx,fy,ny,far,near,err);
    }

    private void drawThresholdAndTouchdown(Canvas c,int w,int h,float cx,float fy,float ny,float far,float near,boolean ground){
        float q0=ground?.11f:.08f,z0=q0*q0,y0=lerp(fy,ny,z0),hh0=lerp(far,near,z0),barW=Math.max(2f,hh0*.10f),barH=Math.max(3f,h*(.006f+.012f*z0));
        p.setColor(0xfffbfaf5);
        for(int i=0;i<6;i++){float off=(i-2.5f)*hh0*.29f;c.drawRect(cx+off-barW*.5f,y0,cx+off+barW*.5f,y0+barH,p);}
        float[] qs={.28f,.38f,.49f};
        for(int k=0;k<qs.length;k++){float q=qs[k],z=q*q,y=lerp(fy,ny,z),hh=lerp(far,near,z),bw=hh*(.13f-.018f*k),bh=h*(.006f+.018f*z);c.drawRect(cx-hh*.55f-bw*.5f,y,cx-hh*.55f+bw*.5f,y+bh,p);c.drawRect(cx+hh*.55f-bw*.5f,y,cx+hh*.55f+bw*.5f,y+bh,p);}
        if(ground&&alongTrackM<150f){p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(28f,w*.046f));c.drawText("27",cx,Math.min(ny-20f,lerp(fy,ny,.62f)),p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);}
    }

    private void drawRubberAndSurface(Canvas c,int w,int h,float cx,float fy,float ny,float far,float near,boolean ground){
        float s=clamp(speedMps/95f,0,1)*(ground?1f:clamp(1-altitudeM/170f,0f,1f));
        for(int i=0;i<30;i++){float q=(i/30f+runwayFlow*1.20f)%1f,z=q*q,y=lerp(fy,ny,z),half=lerp(far,near,z)*.90f;stroke.setColor(0x36101214);stroke.setStrokeWidth(Math.max(1f,w*(.0004f+.0017f*z)));c.drawLine(cx-half,y,cx+half,y,stroke);}
        for(int i=0;i<18;i++){float q=.18f+(i%9)*.065f,z=q*q,y=lerp(fy,ny,z),half=lerp(far,near,z),x=cx+((i&1)==0?-1:1)*half*(.08f+.15f*((i*7)%6)/6f);stroke.setColor(0x55141618);stroke.setStrokeWidth(Math.max(1f,w*(.0007f+.0022f*z)));c.drawLine(x,y,x+(i%3-1)*w*.003f,y+h*(.008f+.030f*z),stroke);}
        if(ground&&s>.1f)for(int i=0;i<18;i++){float q=(i/18f+runwayFlow)%1f,z=q*q,y=lerp(fy,ny,z),half=lerp(far,near,z),x=cx+((i&1)==0?-1:1)*half*.35f,len=h*(.008f+.055f*z)*s;stroke.setColor(0x449aa0a3);stroke.setStrokeWidth(Math.max(1f,w*(.0005f+.0015f*z)));c.drawLine(x,y,x,y+len,stroke);}
    }

    private void drawMovingSideBars(Canvas c,int w,int h,float cx,float fy,float ny,float far,float near,boolean ground){
        float influence=ground?1f:clamp(1f-altitudeM/210f,0f,1f),speed01=clamp(speedMps/95f,0f,1f)*influence;
        for(int i=0;i<10;i++){float q=(i/10f+runwayFlow*1.16f)%1f,z=.13f+.83f*q*q,y=lerp(fy,ny,z),hh=lerp(far,near,z),bw=Math.max(2f,hh*(.045f+.030f*z)),bh=h*(.006f+.037f*z)*(1f+.35f*speed01),lx=cx-hh*.56f,rx=cx+hh*.56f;p.setColor(0xeafffffa);c.drawRect(lx-bw,y,lx+bw,y+bh,p);c.drawRect(rx-bw,y,rx+bw,y+bh,p);}
    }

    private void drawPapi(Canvas c,int w,int h,float cx,float fy,float ny,float far,float near,float err){
        float py=lerp(fy,ny,.43f),ph=lerp(far,near,.43f),r=Math.max(2f,w*.003f),start=cx-ph*1.48f;
        for(int i=0;i<4;i++){p.setColor(i<2?0xfff8f5e9:0xffff3b31);c.drawCircle(start+i*r*3.1f,py,r,p);}
        p.setTextAlign(Paint.Align.CENTER);p.setColor(Math.abs(err)<7f&&Math.abs(crossTrackM)<16f?0xff91ff9d:0xffffd45a);p.setTextSize(Math.max(13f,w*.012f));c.drawText(String.format(java.util.Locale.US,"RWY27  ΔHDG %+.0f°  X-TRK %+.0f m",-err,crossTrackM),w*.5f,h*.70f,p);p.setTextAlign(Paint.Align.LEFT);
    }

    /** Perspective 3-D airport control tower. It grows as the aircraft reaches it, then slides aft and fades. */
    private void drawTakeoffTower(Canvas c,int w,int h,float hz){
        float rel=TOWER_ALONG_M-alongTrackM;
        float near=clamp(1f-Math.max(0f,rel)/560f,.18f,1f);
        float passed=Math.max(0f,-rel),fade=1f-clamp(passed/230f,0f,1f);
        int a=(int)(255f*fade);if(a<=3)return;
        float x=w*(.80f+.08f*(1f-near))-passed*w/720f;
        float groundY=hz+(h-hz)*(.32f+.63f*near);
        float towerH=h*(.13f+.39f*near),shaftW=w*(.018f+.040f*near),depth=shaftW*.34f;
        float topY=groundY-towerH,shaftTop=topY+towerH*.28f;

        p.setColor(withAlpha(0x55000000,fade));c.drawOval(x-shaftW*1.8f,groundY-shaftW*.18f,x+shaftW*1.9f,groundY+shaftW*.30f,p);

        path.reset();path.moveTo(x-shaftW*.52f,shaftTop);path.lineTo(x+shaftW*.52f,shaftTop);path.lineTo(x+shaftW*.62f,groundY);path.lineTo(x-shaftW*.62f,groundY);path.close();p.setColor(withAlpha(0xffc5c8c5,fade));c.drawPath(path,p);
        path.reset();path.moveTo(x+shaftW*.52f,shaftTop);path.lineTo(x+shaftW*.52f+depth,shaftTop-depth*.55f);path.lineTo(x+shaftW*.62f+depth,groundY-depth*.20f);path.lineTo(x+shaftW*.62f,groundY);path.close();p.setColor(withAlpha(0xff8e9697,fade));c.drawPath(path,p);

        float cabW=shaftW*2.45f,cabH=towerH*.19f,cabY=topY+towerH*.08f;
        path.reset();path.moveTo(x-cabW*.52f,cabY);path.lineTo(x+cabW*.52f,cabY);path.lineTo(x+cabW*.44f,cabY+cabH);path.lineTo(x-cabW*.44f,cabY+cabH);path.close();p.setColor(withAlpha(0xff2a3d47,fade));c.drawPath(path,p);
        path.reset();path.moveTo(x+cabW*.52f,cabY);path.lineTo(x+cabW*.52f+depth,cabY-depth*.45f);path.lineTo(x+cabW*.44f+depth,cabY+cabH-depth*.20f);path.lineTo(x+cabW*.44f,cabY+cabH);path.close();p.setColor(withAlpha(0xff182a34,fade));c.drawPath(path,p);

        float winTop=cabY+cabH*.18f,winBot=cabY+cabH*.76f;
        for(int i=0;i<4;i++){float lx=lerp(x-cabW*.42f,x+cabW*.22f,i/3f),rw=lx+cabW*.16f;p.setShader(new LinearGradient(0,winTop,0,winBot,new int[]{withAlpha(0xff83c9de,fade),withAlpha(0xff183846,fade)},null,Shader.TileMode.CLAMP));c.drawRect(lx,winTop,rw,winBot,p);p.setShader(null);}

        float roofY=cabY-cabH*.07f;p.setColor(withAlpha(0xffd4d7d2,fade));c.drawOval(x-cabW*.60f,roofY-cabH*.08f,x+cabW*.65f,roofY+cabH*.12f,p);
        stroke.setColor(withAlpha(0xffd9d9d5,fade));stroke.setStrokeWidth(Math.max(2f,w*.0014f));c.drawLine(x+depth*.2f,roofY,x+depth*.2f,roofY-towerH*.17f,stroke);
        p.setColor(withAlpha(0xffff3b30,fade*(.65f+.35f*(float)Math.sin(System.nanoTime()/1e8))));c.drawCircle(x+depth*.2f,roofY-towerH*.17f,Math.max(2f,w*.0022f),p);
    }

    private void drawMountains(Canvas c,int w,int h,float hz,float scale){
        path.reset();path.moveTo(0,hz);for(int i=0;i<=12;i++){float x=w*i/12f,n=(float)(.45+.55*Math.abs(Math.sin(i*1.73+alongTrackM*.0007)));path.lineTo(x,hz-h*scale*n);}path.lineTo(w,hz);path.close();p.setColor(0xff486f57);c.drawPath(path,p);
        path.reset();path.moveTo(0,hz);for(int i=0;i<=10;i++){float x=w*i/10f,n=(float)(.30+.36*Math.abs(Math.sin(i*1.25+1.4+alongTrackM*.00035)));path.lineTo(x,hz-h*scale*.58f*n);}path.lineTo(w,hz);path.close();p.setColor(0x884f775e);c.drawPath(path,p);
    }

    private void drawCrash(Canvas c,int w,int h){
        p.setColor(0x54ff2000);c.drawRect(0,0,w,h,p);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setColor(0xfffff3e8);p.setTextSize(Math.max(30f,w*.035f));c.drawText("AIRCRAFT IMPACT",w*.5f,h*.22f,p);p.setTextSize(Math.max(16f,w*.017f));c.drawText(crashReason.isEmpty()?"UNSAFE LANDING":crashReason,w*.5f,h*.27f,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);
    }

    private void quad(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4,int color){path.reset();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.lineTo(x4,y4);path.close();p.setColor(color);c.drawPath(path,p);}
    private static int withAlpha(int color,float a){int aa=(int)(((color>>>24)&255)*clamp(a,0f,1f));return (aa<<24)|(color&0x00ffffff);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    private static float angleError(float current,float target){float d=target-current;while(d>180)d-=360;while(d<-180)d+=360;return d;}
}
