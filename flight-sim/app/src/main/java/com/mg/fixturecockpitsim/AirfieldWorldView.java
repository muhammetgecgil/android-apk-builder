package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** AVM-13.9 world: stronger speed-linked taxi/runway flow, flowing runway edge lines, natural scenery and approach perspective. */
public final class AirfieldWorldView extends View {
    private static final float RUNWAY_VISIBLE_ALT_M=1000f;
    private static final float RUNWAY_VISIBLE_XTRACK_M=6500f;
    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Path path=new Path();
    private volatile float altitudeM,speedMps,headingDeg,pitchDeg,crossTrackM,alongTrackM;
    private volatile boolean onGround,crashed;
    private volatile String phase="",crashReason="";
    private long lastNs;
    private float runwayFlow,groundFlow,scenicClock;

    public AirfieldWorldView(Context c){super(c);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);}

    public void setState(double altitude,double speed,boolean ground,String scenePhase,double heading,double pitch,double crossTrack,double alongTrack,boolean crash,String reason){
        altitudeM=(float)Math.max(0,altitude);speedMps=(float)Math.max(0,speed);onGround=ground;phase=scenePhase==null?"":scenePhase;headingDeg=(float)heading;pitchDeg=(float)pitch;crossTrackM=(float)crossTrack;alongTrackM=(float)alongTrack;crashed=crash;crashReason=reason==null?"":reason;postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);long now=System.nanoTime();float dt=lastNs==0?.016f:Math.min(.05f,(now-lastNs)/1e9f);lastNs=now;
        float groundInfluence=onGround?1f:clamp(1f-altitudeM/150f,0f,1f);
        float speedRamp=.85f+.65f*clamp(speedMps/90f,0f,1f);
        float visualGain=onGround?(phase.contains("TAXI")?2.40f:phase.contains("TAKEOFF_ROLL")?2.10f:1.45f):1f;
        float opticalSpeed=speedMps*groundInfluence*visualGain*speedRamp;
        runwayFlow=(runwayFlow+opticalSpeed*dt/70f)%1f;groundFlow=(groundFlow+opticalSpeed*dt/88f)%1f;
        if(phase.contains("ORBIT"))scenicClock+=dt;else if(onGround)scenicClock=0;
        int w=getWidth(),h=getHeight();boolean approach=isApproachScene();float alt01=onGround?0:clamp(altitudeM/2200f,0,1);float horizon=h*(onGround?.455f:lerp(.455f,.205f,alt01));
        if(!onGround)horizon+=h*clamp(pitchDeg/32f,-.095f,.095f);if(approach){float a=approach01();horizon=lerp(h*.225f,h*.405f,a)+h*clamp(pitchDeg/35f,-.065f,.065f);}
        drawSky(c,w,h,horizon);if(phase.contains("ORBIT")&&altitudeM>420f&&!approach)drawScenicTerrain(c,w,h,horizon);else drawRunwayWorld(c,w,h,horizon);if(crashed)drawCrash(c,w,h);if(speedMps>.3f||crashed)postInvalidateOnAnimation();
    }

    private boolean runwayVisible(){if(onGround)return true;if(altitudeM>RUNWAY_VISIBLE_ALT_M)return false;if(Math.abs(crossTrackM)>RUNWAY_VISIBLE_XTRACK_M)return false;return Math.abs(angleError(headingDeg,270f))<92f;}
    private boolean isApproachScene(){if(onGround||!runwayVisible())return false;float hdgErr=Math.abs(angleError(headingDeg,270f));if(phase.contains("APPROACH")||phase.contains("RWY_CAPTURE_AIR"))return hdgErr<82f;return altitudeM<820f&&hdgErr<78f;}
    private float approach01(){return 1f-clamp((altitudeM-8f)/(RUNWAY_VISIBLE_ALT_M-8f),0f,1f);}

    private void drawSky(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{0xff176a9c,0xff43acd0,0xffc8e9ef},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,hz,p);p.setShader(null);
        for(int i=0;i<6;i++){float x=(w*(.10f+i*.19f)+alongTrackM*.17f)%Math.max(1,w),y=hz*(.10f+(i%3)*.07f),ww=w*(.09f+(i%2)*.035f);p.setColor(0x32ffffff);c.drawOval(x,y,x+ww,y+hz*.022f,p);c.drawOval(x+ww*.20f,y-hz*.012f,x+ww*.70f,y+hz*.017f,p);}
    }

    private int scenicSector(){return ((int)(scenicClock/32f))%5;}
    private void drawScenicTerrain(Canvas c,int w,int h,float hz){switch(scenicSector()){case 0:drawMountainValley(c,w,h,hz);break;case 1:drawLakeCountry(c,w,h,hz);break;case 2:drawDesert(c,w,h,hz);break;case 3:drawIslands(c,w,h,hz);break;default:drawCoastalTerrain(c,w,h,hz);break;}if(runwayVisible())drawRunway(c,w,h,hz,false);}

    private void drawMountainValley(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff7c9c67,0xff5e8059,0xff416349},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);drawMountains(c,w,h,hz,.23f*(1-clamp(altitudeM/2500f,0,.55f)));
        float gh=h-hz;for(int i=0;i<34;i++){float d=.08f+((i*23)%33)/36f,y=hz+gh*d*d,x=(i*117f+scenicClock*11f)%Math.max(1,w),ww=w*(.012f+.043f*d);p.setColor(i%2==0?0x36577f43:0x2f3f6c3a);c.drawOval(x,y,x+ww,y+h*.006f+gh*.008f*d,p);}
    }

    private void drawLakeCountry(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff819c69,0xff63835a,0xff45694d},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);drawMountains(c,w,h,hz,.15f);
        drawIrregularLake(c,w,h,hz,w*.33f,hz+(h-hz)*.38f,w*.28f,(h-hz)*.24f,11);
        drawIrregularLake(c,w,h,hz,w*.72f,hz+(h-hz)*.64f,w*.25f,(h-hz)*.22f,27);
        drawIrregularLake(c,w,h,hz,w*.48f,hz+(h-hz)*.86f,w*.16f,(h-hz)*.12f,41);
    }

    private void drawIrregularLake(Canvas c,int w,int h,float hz,float cx,float cy,float rx,float ry,int seed){
        Path lake=irregularPath(cx,cy,rx,ry,seed,34,1f);
        p.setShader(new LinearGradient(0,cy-ry,0,cy+ry,new int[]{0xff4fc5d8,0xff258bb5,0xff145f91},null,Shader.TileMode.CLAMP));c.drawPath(lake,p);p.setShader(null);
        stroke.setColor(0xa8c4c890);stroke.setStrokeWidth(Math.max(2f,w*.0022f));c.drawPath(lake,stroke);
        int save=c.save();c.clipPath(lake);drawNaturalWaterTexture(c,w,h,cx,cy,rx,ry,false);drawCelestialReflection(c,w,h,cx,cy,rx,ry);c.restoreToCount(save);
    }

    private void drawIslands(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff56c6da,0xff238eb9,0xff125f91},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);drawNaturalWaterTexture(c,w,h,w*.5f,hz+(h-hz)*.55f,w*.58f,(h-hz)*.55f,true);drawCelestialReflection(c,w,h,w*.5f,hz+(h-hz)*.58f,w*.54f,(h-hz)*.50f);
        float[][] islands={{.17f,.30f,.095f,.060f},{.42f,.24f,.13f,.075f},{.73f,.31f,.11f,.070f},{.86f,.62f,.085f,.055f},{.55f,.70f,.12f,.070f},{.24f,.76f,.10f,.060f}};
        for(int i=0;i<islands.length;i++){float cx=w*islands[i][0],cy=hz+(h-hz)*islands[i][1],rx=w*islands[i][2],ry=(h-hz)*islands[i][3];Path sand=irregularPath(cx,cy,rx,ry,70+i*13,28,1f),land=irregularPath(cx,cy,rx*.82f,ry*.78f,94+i*17,28,1f);p.setColor(0xffdccb96);c.drawPath(sand,p);p.setShader(new LinearGradient(0,cy-ry,0,cy+ry,new int[]{0xff699b5c,0xff426f49},null,Shader.TileMode.CLAMP));c.drawPath(land,p);p.setShader(null);stroke.setColor(0x75f4e5b1);stroke.setStrokeWidth(Math.max(1.5f,w*.0014f));c.drawPath(sand,stroke);}
    }

    private void drawDesert(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xffd7b672,0xffc99655,0xffa86f3f},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        for(int band=0;band<7;band++){path.reset();float yy=hz+(h-hz)*(.15f+band*.12f);path.moveTo(0,yy);for(int i=0;i<=10;i++){float x=w*i/10f,y=yy+(float)Math.sin(i*.92+band+scenicClock*.05f)*h*(.018f+.004f*band);path.lineTo(x,y);}path.lineTo(w,h);path.lineTo(0,h);path.close();p.setColor(band%2==0?0x34f2cf8b:0x28a96539);c.drawPath(path,p);}
    }

    private void drawCoastalTerrain(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff58c6dc,0xff258fb8,0xff155f90},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);drawNaturalWaterTexture(c,w,h,w*.70f,hz+(h-hz)*.55f,w*.42f,(h-hz)*.50f,true);drawCelestialReflection(c,w,h,w*.70f,hz+(h-hz)*.58f,w*.38f,(h-hz)*.48f);
        path.reset();path.moveTo(0,hz);path.lineTo(w*.36f,hz+h*.05f);path.lineTo(w*.48f,hz+h*.12f);path.lineTo(w*.39f,hz+h*.20f);path.lineTo(w*.52f,hz+h*.29f);path.lineTo(w*.34f,hz+h*.39f);path.lineTo(w*.46f,hz+h*.51f);path.lineTo(w*.31f,h);path.lineTo(0,h);path.close();p.setColor(0xff6b8d59);c.drawPath(path,p);
        path.reset();path.moveTo(w*.36f,hz+h*.05f);path.lineTo(w*.48f,hz+h*.12f);path.lineTo(w*.39f,hz+h*.20f);path.lineTo(w*.52f,hz+h*.29f);path.lineTo(w*.34f,hz+h*.39f);path.lineTo(w*.46f,hz+h*.51f);path.lineTo(w*.31f,h);stroke.setColor(0xffdecf9b);stroke.setStrokeWidth(Math.max(3,w*.0035f));c.drawPath(path,stroke);drawMountains(c,w,h,hz,.11f);
    }

    private Path irregularPath(float cx,float cy,float rx,float ry,int seed,int points,float scale){
        Path q=new Path();for(int i=0;i<points;i++){double a=Math.PI*2*i/points;float wobble=(float)(1+.14*Math.sin(i*1.71+seed*.37)+.07*Math.sin(i*3.19+seed*.11));float x=cx+(float)Math.cos(a)*rx*wobble*scale,y=cy+(float)Math.sin(a)*ry*(1+.11f*(float)Math.sin(i*1.39+seed))*scale;if(i==0)q.moveTo(x,y);else q.lineTo(x,y);}q.close();return q;
    }

    private void drawNaturalWaterTexture(Canvas c,int w,int h,float cx,float cy,float rx,float ry,boolean broad){
        float t=scenicClock*.23f;int count=broad?42:24;for(int i=0;i<count;i++){float yy=cy-ry+((i*.137f+t*.012f)%1f)*ry*2f;float norm=clamp((yy-(cy-ry))/(ry*2f),0,1);float band=rx*(.18f+.58f*(float)Math.sin((i*2.17+3.1)*.43)*(float)Math.sin((i*1.11+1.7)*.51));band=Math.abs(band);float x=cx-rx*.75f+((i*83.3f+t*17f)%(rx*1.50f));float len=Math.max(w*.008f,band*.18f+w*.012f*norm);stroke.setColor(i%5==0?0x36d8f6ff:0x1fa8dbe8);stroke.setStrokeWidth(Math.max(1f,w*(.0005f+.0007f*norm)));c.drawLine(x,yy,Math.min(cx+rx*.88f,x+len),yy,stroke);}
    }

    private void drawCelestialReflection(Canvas c,int w,int h,float cx,float cy,float rx,float ry){
        int mode=WeatherEffectsView.getSharedCelestialMode();float strength=WeatherEffectsView.getSharedCelestialStrength();if(mode==0||strength<.08f)return;float targetX=w*WeatherEffectsView.getSharedCelestialX01();float x=clamp(targetX,cx-rx*.75f,cx+rx*.75f);int base=mode==1?0x00ffe3a0:0x00e6f2ff;
        for(int i=0;i<30;i++){float q=i/29f,y=cy-ry*.72f+q*ry*1.55f;float spread=rx*(.035f+.16f*q)*strength;float wander=(float)Math.sin(i*2.37+scenicClock*.35f)*spread*.36f;float len=spread*(.55f+.50f*(float)Math.sin(i*.91+1.2));int alpha=(int)((22+95*q)*strength);stroke.setColor((Math.max(0,Math.min(150,alpha))<<24)|base);stroke.setStrokeWidth(Math.max(1f,w*(.0007f+.0012f*q)));c.drawLine(x-len*.5f+wander,y,x+len*.5f+wander,y,stroke);}
    }

    private void drawRunwayWorld(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff6f8d5f,0xff587650,0xff416246},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);drawMountains(c,w,h,hz,.18f);
        if(onGround&&speedMps>.6f)drawGroundOpticalFlow(c,w,h,hz);
        boolean taxi=onGround&&(phase.contains("TAXI_OUT")||phase.contains("TAXI_IN"));
        if(taxi)drawTaxiway(c,w,h,hz);else if(runwayVisible())drawRunway(c,w,h,hz,onGround);
        if(onGround&&!taxi&&Math.abs(crossTrackM)>31f){p.setColor(0xddffd45a);p.setTextSize(Math.max(16,w*.016f));p.setTextAlign(Paint.Align.CENTER);c.drawText("OFF RUNWAY  •  GRASS",w*.5f,h*.89f,p);p.setTextAlign(Paint.Align.LEFT);}
    }

    private void drawGroundOpticalFlow(Canvas c,int w,int h,float hz){
        float speed01=clamp(speedMps/42f,0f,1f);if(speed01<=.012f)return;
        for(int i=0;i<40;i++){
            float q=(i/40f+groundFlow*1.06f)%1f,z=q*q,y=lerp(hz+h*.022f,h*.999f,z);
            float len=w*(.009f+.060f*z)*(.34f+1.05f*speed01),leftX=w*(.020f+((i*37)%27)/100f),rightX=w-leftX;
            stroke.setColor(z>.52f?0x667ca06a:0x388fb27a);stroke.setStrokeWidth(Math.max(1f,w*(.0008f+.0030f*z)));
            c.drawLine(leftX,y,leftX-len,y+len*.24f,stroke);c.drawLine(rightX,y,rightX+len,y+len*.24f,stroke);
        }
    }

    private void drawTaxiway(Canvas c,int w,int h,float hz){
        float fy=hz+h*.016f,ny=h*.999f,farHalf=w*.018f,nearHalf=w*.255f;
        float bend=(float)Math.sin(alongTrackM*.030f)*w*.020f;
        float farCx=w*.5f-bend*.20f,nearCx=w*.5f+bend;
        quad(c,farCx-farHalf*1.45f,fy,farCx+farHalf*1.45f,fy,nearCx+nearHalf*1.12f,ny,nearCx-nearHalf*1.12f,ny,0xff8a8d83);
        path.reset();path.moveTo(farCx-farHalf,fy);path.lineTo(farCx+farHalf,fy);path.lineTo(nearCx+nearHalf,ny);path.lineTo(nearCx-nearHalf,ny);path.close();
        p.setShader(new LinearGradient(0,fy,0,ny,new int[]{0xff505457,0xff3b4042,0xff292e30},null,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);

        for(int i=0;i<34;i++){
            float q=(i/34f+groundFlow*1.14f)%1f,z=q*q,y=lerp(fy,ny,z),half=lerp(farHalf,nearHalf,z),cx=lerp(farCx,nearCx,z);
            stroke.setColor(z>.52f?0x54111517:0x2d15191b);stroke.setStrokeWidth(Math.max(1f,w*(.0005f+.0021f*z)));
            c.drawLine(cx-half*.96f,y,cx+half*.96f,y,stroke);
        }

        path.reset();path.moveTo(farCx,fy);path.quadTo(w*.5f-bend*.35f,lerp(fy,ny,.48f),nearCx,ny);
        stroke.setColor(0xffffd448);stroke.setStrokeWidth(Math.max(2f,w*.0032f));c.drawPath(path,stroke);
        stroke.setColor(0x88fff0a0);stroke.setStrokeWidth(Math.max(1f,w*.0011f));c.drawPath(path,stroke);

        for(int i=0;i<24;i++){
            float q=(i/24f+groundFlow*1.26f)%1f,z=q*q,y=lerp(fy,ny,z),cx=lerp(farCx,nearCx,z),r=.8f+3.2f*z;
            p.setColor(z>.52f?0xff8dff9d:0xff58c879);c.drawCircle(cx,y,r,p);
            if(speedMps>6f&&z>.50f){stroke.setColor(0x4e8dff9d);stroke.setStrokeWidth(Math.max(1f,r*.45f));c.drawLine(cx,y,cx,y+h*.010f*z*clamp(speedMps/18f,0,1),stroke);}
        }

        for(int i=0;i<36;i++){
            float q=(i/36f+runwayFlow*1.02f)%1f,z=q*q,y=lerp(fy,ny,z),half=lerp(farHalf,nearHalf,z),cx=lerp(farCx,nearCx,z),r=1f+5.0f*z;
            p.setColor(z>.55f?0xff74bfff:0xff5c9ed9);c.drawCircle(cx-half,y,r,p);c.drawCircle(cx+half,y,r,p);
            if(speedMps>6f&&z>.50f){float blur=h*.015f*z*clamp(speedMps/20f,0,1);stroke.setColor(0x607fc9ff);stroke.setStrokeWidth(Math.max(1f,r*.55f));c.drawLine(cx-half,y,cx-half,y+blur,stroke);c.drawLine(cx+half,y,cx+half,y+blur,stroke);}
        }

        if(phase.contains("TAXI_OUT")&&alongTrackM>72f){float q=clamp((alongTrackM-72f)/28f,0,1),z=.34f+.14f*q,y=lerp(fy,ny,z*z),half=lerp(farHalf,nearHalf,z),cx=lerp(farCx,nearCx,z);stroke.setColor(0xffffd448);stroke.setStrokeWidth(Math.max(2f,w*.0025f));c.drawLine(cx-half*.84f,y,cx+half*.84f,y,stroke);c.drawLine(cx-half*.84f,y+h*.012f,cx+half*.84f,y+h*.012f,stroke);}
    }

    private void drawRunway(Canvas c,int w,int h,float hz,boolean ground){
        if(!ground&&!runwayVisible())return;float headingErr=angleError(headingDeg,270f),lateralPixels=clamp(crossTrackM/44f,-2.5f,2.5f)*w*.235f,cx=w*.5f-lateralPixels+clamp(headingErr/32f,-1,1)*w*.095f;float farHalf,nearHalf,fy,ny;
        if(ground){farHalf=w*.032f;nearHalf=w*.44f;fy=hz+h*.015f;ny=h*.999f;}else{float a=approach01(),eased=(float)Math.pow(a,.82);nearHalf=w*lerp(.018f,.405f,eased);farHalf=w*lerp(.009f,.040f,a);fy=lerp(hz+h*.018f,hz+h*.050f,a);ny=lerp(hz+h*.10f,h*.985f,(float)Math.pow(a,.74));if(!isApproachScene()){nearHalf*=.58f;farHalf*=.72f;ny=lerp(hz+h*.09f,h*.55f,clamp(1-altitudeM/RUNWAY_VISIBLE_ALT_M,0,1));}}
        quad(c,cx-farHalf*1.52f,fy,cx+farHalf*1.52f,fy,cx+nearHalf*1.20f,ny,cx-nearHalf*1.20f,ny,0xff858982);path.reset();path.moveTo(cx-farHalf,fy);path.lineTo(cx+farHalf,fy);path.lineTo(cx+nearHalf,ny);path.lineTo(cx-nearHalf,ny);path.close();p.setShader(new LinearGradient(cx,fy,cx,ny,new int[]{0xff484c4e,0xff35393b,0xff282d2f},null,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);drawRunwaySurfaceFlow(c,w,h,cx,fy,ny,farHalf,nearHalf,ground);stroke.setColor(0xfff5f4ed);stroke.setStrokeWidth(Math.max(2,w*.0026f));c.drawLine(cx-farHalf,fy,cx-nearHalf,ny,stroke);c.drawLine(cx+farHalf,fy,cx+nearHalf,ny,stroke);drawRunwayEdgeFlow(c,w,h,cx,fy,ny,farHalf,nearHalf,ground);
        float speed01=clamp(speedMps/88f,0f,1f);
        for(int i=0;i<26;i++){float q=(i/26f+runwayFlow*1.10f)%1f,z=q*q,y=lerp(fy,ny,z),dash=lerp(3f,64f,z)*(1f+1.05f*speed01),gap=lerp(1f,14f,z);stroke.setColor(0xfff6f5ef);stroke.setStrokeWidth(1.5f+11*z);c.drawLine(cx,y,cx,Math.min(ny,y+dash+gap),stroke);}
        for(int i=0;i<34;i++){float q=(i/34f+runwayFlow*1.03f)%1f,z=q*q,y=lerp(fy,ny,z),hh=lerp(farHalf,nearHalf,z),r=1+5.3f*z;p.setColor(i>30?0xffffd465:0xfff5f0d9);c.drawCircle(cx-hh,y,r,p);c.drawCircle(cx+hh,y,r,p);if(ground&&speed01>.20f&&z>.42f){stroke.setColor(0x6afff8d9);stroke.setStrokeWidth(Math.max(1f,r*.55f));float blur=h*.026f*z*(.35f+.85f*speed01);c.drawLine(cx-hh,y,cx-hh,y+blur,stroke);c.drawLine(cx+hh,y,cx+hh,y+blur,stroke);}}
        for(int set=0;set<4;set++){float z=.30f+set*.085f,y=lerp(fy,ny,z*z),hh=lerp(farHalf,nearHalf,z),bw=Math.max(2,hh*.075f),bh=4+19*z;p.setColor(0xfff6f5ef);c.drawRect(cx-hh*.54f-bw,y,cx-hh*.54f+bw,y+bh,p);c.drawRect(cx+hh*.54f-bw,y,cx+hh*.54f+bw,y+bh,p);}if(!ground&&isApproachScene())drawApproachGuidance(c,w,h,cx,fy,ny,farHalf,nearHalf,headingErr);if(ground&&alongTrackM<115f){float z=clamp(.48f+alongTrackM/260f,0,1),y=lerp(fy,ny,z*z);p.setColor(0xfffaf9f4);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(30,w*(.038f+.02f*z)));c.drawText("27",cx,Math.min(ny-6,y+54),p);p.setTextAlign(Paint.Align.LEFT);}
    }

    private void drawRunwayEdgeFlow(Canvas c,int w,int h,float cx,float fy,float ny,float farHalf,float nearHalf,boolean ground){
        float lowAlt=ground?1f:clamp(1f-altitudeM/180f,0f,1f),speed01=clamp(speedMps/90f,0f,1f)*lowAlt;
        if(speed01<.025f)return;
        for(int i=0;i<28;i++){
            float q=(i/28f+runwayFlow*1.22f)%1f,z=q*q;
            float segQ=Math.max(0f,q-(.018f+.070f*speed01*(.30f+.70f*q))),segZ=segQ*segQ;
            float y0=lerp(fy,ny,segZ),y1=lerp(fy,ny,z),half0=lerp(farHalf,nearHalf,segZ),half1=lerp(farHalf,nearHalf,z);
            float xL0=cx-half0,xL1=cx-half1,xR0=cx+half0,xR1=cx+half1;
            int alpha=(int)clamp(70f+170f*z+45f*speed01,75f,255f);
            stroke.setColor((alpha<<24)|0x00fffef7);stroke.setStrokeWidth(Math.max(1f,w*(.0007f+.0032f*z)*(.55f+.70f*speed01)));
            c.drawLine(xL0,y0,xL1,y1,stroke);c.drawLine(xR0,y0,xR1,y1,stroke);
            if(speed01>.28f&&z>.35f){
                float trailQ=Math.max(0f,segQ-(.020f+.050f*speed01)),trailZ=trailQ*trailQ;
                float yt=lerp(fy,ny,trailZ),ht=lerp(farHalf,nearHalf,trailZ);
                int ta=(int)clamp(25f+70f*z*speed01,25f,105f);stroke.setColor((ta<<24)|0x00ffffff);stroke.setStrokeWidth(Math.max(1f,w*(.0010f+.0040f*z)));
                c.drawLine(cx-ht,yt,xL0,y0,stroke);c.drawLine(cx+ht,yt,xR0,y0,stroke);
            }
        }
    }

    private void drawRunwaySurfaceFlow(Canvas c,int w,int h,float cx,float fy,float ny,float farHalf,float nearHalf,boolean ground){
        float lowAlt=ground?1f:clamp(1f-altitudeM/150f,0f,1f),speed01=clamp(speedMps/92f,0f,1f)*lowAlt;if(speed01<=.012f)return;
        for(int i=0;i<34;i++){float q=(i/34f+runwayFlow*1.24f)%1f,z=q*q,y=lerp(fy,ny,z),half=lerp(farHalf,nearHalf,z)*.92f;stroke.setColor(z>.52f?0x4e101517:0x2c121719);stroke.setStrokeWidth(Math.max(1f,w*(.00045f+.0019f*z)));c.drawLine(cx-half,y,cx+half,y,stroke);}
        if(ground&&speed01>.08f){for(int i=0;i<24;i++){float q=(i/24f+runwayFlow*.98f)%1f,z=q*q,y=lerp(fy,ny,z),half=lerp(farHalf,nearHalf,z),x=cx+((i&1)==0?-1:1)*half*(.20f+.60f*((i*17)%10)/10f),len=h*(.008f+.068f*z)*(.25f+1.05f*speed01);stroke.setColor(z>.58f?0x589da3a6:0x349da3a6);stroke.setStrokeWidth(Math.max(1f,w*(.0005f+.0019f*z)));c.drawLine(x,y,x,y+len,stroke);}}
    }

    private void drawApproachGuidance(Canvas c,int w,int h,float cx,float fy,float ny,float farHalf,float nearHalf,float headingErr){float a=approach01(),py=lerp(fy,ny,.46f),ph=lerp(farHalf,nearHalf,.46f),r=Math.max(2,w*(.0023f+.0015f*a));for(int i=0;i<4;i++){p.setColor(i<2?0xfff5f4e8:0xffff3b31);c.drawCircle(cx-ph*1.42f+i*r*3.1f,py,r,p);}p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(13,w*.012f));p.setColor(Math.abs(headingErr)<8&&Math.abs(crossTrackM)<18?0xff8dff9c:0xffffd45a);c.drawText(String.format(java.util.Locale.US,"RWY27  ΔHDG %+.0f°  X-TRK %+.0f m",-headingErr,crossTrackM),w*.5f,h*.70f,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);}
    private void drawMountains(Canvas c,int w,int h,float hz,float scale){path.reset();path.moveTo(0,hz);for(int i=0;i<=12;i++){float x=w*i/12f,n=(float)(.45+.55*Math.abs(Math.sin(i*1.73+alongTrackM*.0008)));path.lineTo(x,hz-h*scale*n);}path.lineTo(w,hz);path.close();p.setColor(0xff456f55);c.drawPath(path,p);}
    private void drawCrash(Canvas c,int w,int h){p.setColor(0x54ff2000);c.drawRect(0,0,w,h,p);for(int i=0;i<11;i++){float x=w*(.38f+.026f*i),y=h*(.70f-.035f*(i%5)),r=w*(.018f+.006f*(i%4));p.setColor(i%2==0?0x70404040:0x654f5152);c.drawCircle(x,y,r,p);}p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setColor(0xfffff3e8);p.setTextSize(Math.max(30,w*.035f));c.drawText("AIRCRAFT IMPACT",w*.5f,h*.22f,p);p.setTextSize(Math.max(16,w*.017f));c.drawText(crashReason.isEmpty()?"UNSAFE LANDING":crashReason,w*.5f,h*.27f,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);}
    private void quad(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4,int color){path.reset();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.lineTo(x4,y4);path.close();p.setColor(color);c.drawPath(path,p);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}private static float angleError(float current,float target){float d=target-current;while(d>180)d-=360;while(d<-180)d+=360;return d;}
}
