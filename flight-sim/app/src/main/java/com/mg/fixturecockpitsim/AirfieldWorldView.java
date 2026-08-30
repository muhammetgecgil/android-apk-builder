package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** AVM-13.2 world: scenic cruise, runway below 1000 m, and strong takeoff optical-flow/parallax. */
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
        super.onDraw(c);
        long now=System.nanoTime();
        float dt=lastNs==0?.016f:Math.min(.05f,(now-lastNs)/1e9f);lastNs=now;
        float groundInfluence=onGround?1f:clamp(1f-altitudeM/150f,0f,1f);
        float opticalSpeed=speedMps*groundInfluence;
        runwayFlow=(runwayFlow+opticalSpeed*dt/175f)%1f;
        groundFlow=(groundFlow+opticalSpeed*dt/240f)%1f;
        if(phase.contains("ORBIT"))scenicClock+=dt;else if(onGround)scenicClock=0;

        int w=getWidth(),h=getHeight();boolean approach=isApproachScene();
        float alt01=onGround?0:clamp(altitudeM/2200f,0,1);
        float horizon=h*(onGround?.455f:lerp(.455f,.205f,alt01));
        if(!onGround)horizon+=h*clamp(pitchDeg/32f,-.095f,.095f);
        if(approach){float a=approach01();horizon=lerp(h*.225f,h*.405f,a)+h*clamp(pitchDeg/35f,-.065f,.065f);}
        drawSky(c,w,h,horizon);
        if(phase.contains("ORBIT")&&altitudeM>420f&&!approach)drawScenicTerrain(c,w,h,horizon);else drawRunwayWorld(c,w,h,horizon);
        if(crashed)drawCrash(c,w,h);
        if(speedMps>.3f||crashed)postInvalidateOnAnimation();
    }

    private boolean runwayVisible(){if(onGround)return true;if(altitudeM>RUNWAY_VISIBLE_ALT_M)return false;if(Math.abs(crossTrackM)>RUNWAY_VISIBLE_XTRACK_M)return false;return Math.abs(angleError(headingDeg,270f))<92f;}
    private boolean isApproachScene(){if(onGround||!runwayVisible())return false;float hdgErr=Math.abs(angleError(headingDeg,270f));if(phase.contains("APPROACH")||phase.contains("RWY_CAPTURE_AIR"))return hdgErr<82f;return altitudeM<820f&&hdgErr<78f;}
    private float approach01(){return 1f-clamp((altitudeM-8f)/(RUNWAY_VISIBLE_ALT_M-8f),0f,1f);}

    private void drawSky(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{0xff176a9c,0xff43acd0,0xffc8e9ef},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,hz,p);p.setShader(null);
        for(int i=0;i<6;i++){float x=(w*(.10f+i*.19f)+alongTrackM*.17f)%Math.max(1,w),y=hz*(.10f+(i%3)*.07f),ww=w*(.09f+(i%2)*.035f);p.setColor(0x32ffffff);c.drawOval(x,y,x+ww,y+hz*.022f,p);c.drawOval(x+ww*.20f,y-hz*.012f,x+ww*.70f,y+hz*.017f,p);}
    }

    private int scenicSector(){return ((int)(scenicClock/32f))%5;}
    private void drawScenicTerrain(Canvas c,int w,int h,float hz){
        switch(scenicSector()){
            case 0:drawMountainValley(c,w,h,hz);break;
            case 1:drawLakeCountry(c,w,h,hz);break;
            case 2:drawDesert(c,w,h,hz);break;
            case 3:drawIslands(c,w,h,hz);break;
            default:drawCoastalTerrain(c,w,h,hz);break;
        }
        if(runwayVisible())drawRunway(c,w,h,hz,false);
    }

    private void drawMountainValley(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff7c9c67,0xff5e8059,0xff416349},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);drawMountains(c,w,h,hz,.23f*(1-clamp(altitudeM/2500f,0,.55f)));
        float gh=h-hz;for(int i=0;i<34;i++){float d=.08f+((i*23)%33)/36f,y=hz+gh*d*d,x=(i*117f+scenicClock*11f)%Math.max(1,w),ww=w*(.012f+.043f*d);p.setColor(i%2==0?0x36577f43:0x2f3f6c3a);c.drawOval(x,y,x+ww,y+h*.006f+gh*.008f*d,p);}
    }

    private void drawLakeCountry(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff82a26c,0xff63885d,0xff486e50},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);drawMountains(c,w,h,hz,.15f);
        p.setColor(0xff39a8cf);c.drawOval(w*.08f,hz+h*.10f,w*.58f,hz+h*.49f,p);p.setColor(0xff54b8d4);c.drawOval(w*.55f,hz+h*.25f,w*.96f,hz+h*.65f,p);p.setColor(0xff2c91bd);c.drawOval(w*.30f,hz+h*.48f,w*.72f,hz+h*.89f,p);
        stroke.setColor(0x70e3f7ff);stroke.setStrokeWidth(Math.max(1,w*.001f));for(int i=0;i<12;i++){float yy=hz+h*(.17f+i*.045f);c.drawLine(w*.18f,yy,w*.45f,yy,stroke);}
    }

    private void drawDesert(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xffd7b672,0xffc99655,0xffa86f3f},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        for(int band=0;band<7;band++){path.reset();float yy=hz+(h-hz)*(.15f+band*.12f);path.moveTo(0,yy);for(int i=0;i<=10;i++){float x=w*i/10f,y=yy+(float)Math.sin(i*.92+band+scenicClock*.05f)*h*(.018f+.004f*band);path.lineTo(x,y);}path.lineTo(w,h);path.lineTo(0,h);path.close();p.setColor(band%2==0?0x34f2cf8b:0x28a96539);c.drawPath(path,p);}
        p.setColor(0x506d512f);for(int i=0;i<8;i++){float x=w*(.08f+i*.13f),y=hz+h*(.32f+(i%3)*.12f);c.drawOval(x,y,x+w*.025f,y+h*.008f,p);}
    }

    private void drawIslands(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff67cee3,0xff2da6ca,0xff167aa9},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        for(int i=0;i<9;i++){float cx=w*(.08f+(i*.137f)% .88f),cy=hz+(h-hz)*(.18f+(i%4)*.18f),rx=w*(.035f+.012f*(i%3)),ry=h*(.018f+.010f*(i%2));p.setColor(0xffe4cf94);c.drawOval(cx-rx*1.15f,cy-ry*1.18f,cx+rx*1.15f,cy+ry*1.18f,p);p.setColor(i%2==0?0xff4f8d50:0xff5d9c57);c.drawOval(cx-rx,cy-ry,cx+rx,cy+ry,p);}
        drawSeaGlints(c,w,h,hz);
    }

    private void drawCoastalTerrain(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff62c6df,0xff2f9fc8,0xff1976a7},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        path.reset();path.moveTo(0,hz);path.lineTo(w*.46f,hz+h*.07f);path.lineTo(w*.38f,hz+h*.19f);path.lineTo(w*.51f,hz+h*.30f);path.lineTo(w*.33f,hz+h*.44f);path.lineTo(w*.43f,h);path.lineTo(0,h);path.close();p.setColor(0xff6f8f58);c.drawPath(path,p);
        path.reset();path.moveTo(w*.46f,hz+h*.07f);path.lineTo(w*.38f,hz+h*.19f);path.lineTo(w*.51f,hz+h*.30f);path.lineTo(w*.33f,hz+h*.44f);path.lineTo(w*.43f,h);stroke.setColor(0xffe1d19b);stroke.setStrokeWidth(Math.max(3,w*.004f));c.drawPath(path,stroke);drawSeaGlints(c,w,h,hz);drawMountains(c,w,h,hz,.11f);
    }

    private void drawSeaGlints(Canvas c,int w,int h,float hz){stroke.setColor(0x54e4f9ff);stroke.setStrokeWidth(Math.max(1,w*.001f));for(int i=0;i<18;i++){float yy=hz+(h-hz)*(.10f+i*.048f),xx=w*((.47f+(i%5)*.10f)%1f);c.drawLine(xx,yy,Math.min(w,xx+w*(.06f+.015f*(i%3))),yy,stroke);}}

    private void drawRunwayWorld(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff6f8d5f,0xff587650,0xff416246},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);drawMountains(c,w,h,hz,.18f);
        if(onGround&&speedMps>2f)drawGroundOpticalFlow(c,w,h,hz);
        if(runwayVisible())drawRunway(c,w,h,hz,onGround);
        if(onGround&&Math.abs(crossTrackM)>31f){p.setColor(0xddffd45a);p.setTextSize(Math.max(16,w*.016f));p.setTextAlign(Paint.Align.CENTER);c.drawText("OFF RUNWAY  •  GRASS",w*.5f,h*.89f,p);p.setTextAlign(Paint.Align.LEFT);}
    }

    private void drawGroundOpticalFlow(Canvas c,int w,int h,float hz){
        float speed01=clamp(speedMps/95f,0f,1f);
        if(speed01<=.02f)return;
        for(int i=0;i<24;i++){
            float q=(i/24f+groundFlow)%1f,z=q*q;
            float y=lerp(hz+h*.035f,h*.995f,z);
            float len=w*(.006f+.030f*z)*(.35f+.65f*speed01);
            float leftX=w*(.04f+((i*37)%22)/100f),rightX=w-leftX;
            stroke.setColor(z>.55f?0x557da36a:0x2c8fb27a);stroke.setStrokeWidth(Math.max(1f,w*(.0008f+.0023f*z)));
            c.drawLine(leftX,y,leftX-len,y+len*.18f,stroke);c.drawLine(rightX,y,rightX+len,y+len*.18f,stroke);
        }
    }

    private void drawRunway(Canvas c,int w,int h,float hz,boolean ground){
        if(!ground&&!runwayVisible())return;
        float headingErr=angleError(headingDeg,270f),lateralPixels=clamp(crossTrackM/44f,-2.5f,2.5f)*w*.235f,cx=w*.5f-lateralPixels+clamp(headingErr/32f,-1,1)*w*.095f;
        float farHalf,nearHalf,fy,ny;
        if(ground){farHalf=w*.032f;nearHalf=w*.44f;fy=hz+h*.015f;ny=h*.999f;}else{float a=approach01(),eased=(float)Math.pow(a,.82);nearHalf=w*lerp(.018f,.405f,eased);farHalf=w*lerp(.009f,.040f,a);fy=lerp(hz+h*.018f,hz+h*.050f,a);ny=lerp(hz+h*.10f,h*.985f,(float)Math.pow(a,.74));if(!isApproachScene()){nearHalf*=.58f;farHalf*=.72f;ny=lerp(hz+h*.09f,h*.55f,clamp(1-altitudeM/RUNWAY_VISIBLE_ALT_M,0,1));}}

        quad(c,cx-farHalf*1.52f,fy,cx+farHalf*1.52f,fy,cx+nearHalf*1.20f,ny,cx-nearHalf*1.20f,ny,0xff858982);
        path.reset();path.moveTo(cx-farHalf,fy);path.lineTo(cx+farHalf,fy);path.lineTo(cx+nearHalf,ny);path.lineTo(cx-nearHalf,ny);path.close();
        p.setShader(new LinearGradient(cx,fy,cx,ny,new int[]{0xff484c4e,0xff35393b,0xff282d2f},null,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);
        drawRunwaySurfaceFlow(c,w,h,cx,fy,ny,farHalf,nearHalf,ground);

        stroke.setColor(0xfff5f4ed);stroke.setStrokeWidth(Math.max(2,w*.0026f));c.drawLine(cx-farHalf,fy,cx-nearHalf,ny,stroke);c.drawLine(cx+farHalf,fy,cx+nearHalf,ny,stroke);

        float speed01=clamp(speedMps/100f,0f,1f);
        for(int i=0;i<18;i++){
            float q=(i/18f+runwayFlow)%1f,z=q*q,y=lerp(fy,ny,z),dash=lerp(3f,54f,z)*(1f+.7f*speed01),gap=lerp(1f,12f,z);
            stroke.setColor(0xfff6f5ef);stroke.setStrokeWidth(1.5f+10*z);
            c.drawLine(cx,y,cx,Math.min(ny,y+dash+gap),stroke);
        }
        for(int i=0;i<26;i++){
            float q=(i/26f+runwayFlow*.82f)%1f,z=q*q,y=lerp(fy,ny,z),hh=lerp(farHalf,nearHalf,z),r=1+5*z;
            p.setColor(i>22?0xffffd465:0xfff5f0d9);c.drawCircle(cx-hh,y,r,p);c.drawCircle(cx+hh,y,r,p);
            if(ground&&speed01>.45f&&z>.52f){stroke.setColor(0x58fff8d9);stroke.setStrokeWidth(Math.max(1f,r*.55f));float blur=h*.018f*z*speed01;c.drawLine(cx-hh,y,cx-hh,y+blur,stroke);c.drawLine(cx+hh,y,cx+hh,y+blur,stroke);}
        }
        for(int set=0;set<4;set++){float z=.30f+set*.085f,y=lerp(fy,ny,z*z),hh=lerp(farHalf,nearHalf,z),bw=Math.max(2,hh*.075f),bh=4+19*z;p.setColor(0xfff6f5ef);c.drawRect(cx-hh*.54f-bw,y,cx-hh*.54f+bw,y+bh,p);c.drawRect(cx+hh*.54f-bw,y,cx+hh*.54f+bw,y+bh,p);}

        if(!ground&&isApproachScene())drawApproachGuidance(c,w,h,cx,fy,ny,farHalf,nearHalf,headingErr);
        if(ground&&alongTrackM<115f){float z=clamp(.48f+alongTrackM/260f,0,1),y=lerp(fy,ny,z*z);p.setColor(0xfffaf9f4);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(30,w*(.038f+.02f*z)));c.drawText("27",cx,Math.min(ny-6,y+54),p);p.setTextAlign(Paint.Align.LEFT);}
    }

    private void drawRunwaySurfaceFlow(Canvas c,int w,int h,float cx,float fy,float ny,float farHalf,float nearHalf,boolean ground){
        float lowAlt=ground?1f:clamp(1f-altitudeM/150f,0f,1f);
        float speed01=clamp(speedMps/105f,0f,1f)*lowAlt;
        if(speed01<=.015f)return;

        for(int i=0;i<20;i++){
            float q=(i/20f+runwayFlow)%1f,z=q*q;
            float y=lerp(fy,ny,z),half=lerp(farHalf,nearHalf,z)*.92f;
            stroke.setColor(z>.55f?0x3f0f1315:0x25121719);stroke.setStrokeWidth(Math.max(1f,w*(.00045f+.0015f*z)));
            c.drawLine(cx-half,y,cx+half,y,stroke);
        }

        if(ground&&speed01>.20f){
            for(int i=0;i<14;i++){
                float q=(i/14f+runwayFlow*.73f)%1f,z=q*q,y=lerp(fy,ny,z),half=lerp(farHalf,nearHalf,z);
                float x=cx+((i&1)==0?-1:1)*half*(.24f+.52f*((i*17)%10)/10f);
                float len=h*(.004f+.045f*z)*speed01;
                stroke.setColor(z>.6f?0x4a9da3a6:0x289da3a6);stroke.setStrokeWidth(Math.max(1f,w*(.0005f+.0015f*z)));
                c.drawLine(x,y,x,y+len,stroke);
            }
        }
    }

    private void drawApproachGuidance(Canvas c,int w,int h,float cx,float fy,float ny,float farHalf,float nearHalf,float headingErr){float a=approach01(),py=lerp(fy,ny,.46f),ph=lerp(farHalf,nearHalf,.46f),r=Math.max(2,w*(.0023f+.0015f*a));for(int i=0;i<4;i++){p.setColor(i<2?0xfff5f4e8:0xffff3b31);c.drawCircle(cx-ph*1.42f+i*r*3.1f,py,r,p);}p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(13,w*.012f));p.setColor(Math.abs(headingErr)<8&&Math.abs(crossTrackM)<18?0xff8dff9c:0xffffd45a);c.drawText(String.format(java.util.Locale.US,"RWY27  ΔHDG %+.0f°  X-TRK %+.0f m",-headingErr,crossTrackM),w*.5f,h*.70f,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);}
    private void drawMountains(Canvas c,int w,int h,float hz,float scale){path.reset();path.moveTo(0,hz);for(int i=0;i<=12;i++){float x=w*i/12f,n=(float)(.45+.55*Math.abs(Math.sin(i*1.73+alongTrackM*.0008)));path.lineTo(x,hz-h*scale*n);}path.lineTo(w,hz);path.close();p.setColor(0xff456f55);c.drawPath(path,p);}
    private void drawCrash(Canvas c,int w,int h){p.setColor(0x54ff2000);c.drawRect(0,0,w,h,p);for(int i=0;i<11;i++){float x=w*(.38f+.026f*i),y=h*(.70f-.035f*(i%5)),r=w*(.018f+.006f*(i%4));p.setColor(i%2==0?0x70404040:0x654f5152);c.drawCircle(x,y,r,p);}p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setColor(0xfffff3e8);p.setTextSize(Math.max(30,w*.035f));c.drawText("AIRCRAFT IMPACT",w*.5f,h*.22f,p);p.setTextSize(Math.max(16,w*.017f));c.drawText(crashReason.isEmpty()?"UNSAFE LANDING":crashReason,w*.5f,h*.27f,p);p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);}
    private void quad(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4,int color){path.reset();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.lineTo(x4,y4);path.close();p.setColor(color);c.drawPath(path,p);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}private static float angleError(float current,float target){float d=target-current;while(d>180)d-=360;while(d<-180)d+=360;return d;}
}
