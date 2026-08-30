package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** AVM-12.9 world view: realistic runway visibility envelope and approach growth. */
public final class AirfieldWorldView extends View {
    private static final float RUNWAY_VISIBLE_ALT_M=1200f;
    private static final float RUNWAY_VISIBLE_XTRACK_M=6500f;
    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Path path=new Path();
    private volatile float altitudeM,speedMps,headingDeg,pitchDeg,crossTrackM,alongTrackM;
    private volatile boolean onGround,crashed;
    private volatile String phase="",crashReason="";
    private long lastNs;
    private float scroll;

    public AirfieldWorldView(Context c){
        super(c);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setState(double altitude,double speed,boolean ground,String scenePhase,double heading,double pitch,double crossTrack,double alongTrack,boolean crash,String reason){
        altitudeM=(float)Math.max(0,altitude);
        speedMps=(float)Math.max(0,speed);
        onGround=ground;
        phase=scenePhase==null?"":scenePhase;
        headingDeg=(float)heading;
        pitchDeg=(float)pitch;
        crossTrackM=(float)crossTrack;
        alongTrackM=(float)alongTrack;
        crashed=crash;
        crashReason=reason==null?"":reason;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime();
        float dt=lastNs==0?.016f:Math.min(.05f,(now-lastNs)/1e9f);
        lastNs=now;
        scroll=(scroll+speedMps*dt*.032f)%1f;
        int w=getWidth(),h=getHeight();
        boolean approach=isApproachScene();
        float alt01=onGround?0:clamp(altitudeM/2200f,0,1);
        float horizon=h*(onGround?.455f:lerp(.455f,.205f,alt01));
        if(!onGround)horizon+=h*clamp(pitchDeg/32f,-.095f,.095f);
        if(approach){
            float a=approach01();
            horizon=lerp(h*.225f,h*.405f,a)+h*clamp(pitchDeg/35f,-.065f,.065f);
        }
        drawSky(c,w,h,horizon);
        if(phase.contains("ORBIT")&&altitudeM>520f&&!approach)drawHighTerrain(c,w,h,horizon);
        else drawRunwayWorld(c,w,h,horizon);
        if(crashed)drawCrash(c,w,h);
        if(speedMps>.3f||crashed)postInvalidateOnAnimation();
    }

    private boolean runwayVisible(){
        if(onGround)return true;
        if(altitudeM>RUNWAY_VISIBLE_ALT_M)return false;
        if(Math.abs(crossTrackM)>RUNWAY_VISIBLE_XTRACK_M)return false;
        float hdgErr=Math.abs(angleError(headingDeg,270f));
        return hdgErr<92f;
    }

    private boolean isApproachScene(){
        if(onGround||!runwayVisible())return false;
        float hdgErr=Math.abs(angleError(headingDeg,270f));
        if(phase.contains("APPROACH")||phase.contains("RWY_CAPTURE_AIR"))return hdgErr<82f;
        return altitudeM<900f&&hdgErr<78f;
    }

    private float approach01(){
        return 1f-clamp((altitudeM-8f)/(RUNWAY_VISIBLE_ALT_M-8f),0f,1f);
    }

    private void drawSky(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{0xff061621,0xff177a9d,0xffb8d9df},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,hz,p);p.setShader(null);
        for(int i=0;i<6;i++){
            float x=(w*(.10f+i*.19f)+alongTrackM*.17f)%Math.max(1,w);
            float y=hz*(.10f+(i%3)*.07f),ww=w*(.09f+(i%2)*.035f);
            p.setColor(0x25ffffff);
            c.drawOval(x,y,x+ww,y+hz*.022f,p);
            c.drawOval(x+ww*.20f,y-hz*.012f,x+ww*.70f,y+hz*.017f,p);
        }
    }

    private void drawHighTerrain(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff526b48,0xff324c39,0xff23362c},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,h,p);p.setShader(null);
        drawMountains(c,w,h,hz,.20f*(1-clamp(altitudeM/2500f,0,.55f)));
        float gh=h-hz;
        for(int i=0;i<38;i++){
            float depth=.05f+((i*19)%37)/40f,y=hz+gh*depth*depth;
            float x=(i*131f+alongTrackM*.45f)%Math.max(1,w),ww=w*(.018f+.052f*depth),hh=h*(.003f+.009f*depth);
            p.setColor(i%3==0?0x22334d2d:i%3==1?0x1f49603a:0x1c263c2b);
            c.drawOval(x,y,x+ww,y+hh,p);
        }
        if(runwayVisible())drawRunway(c,w,h,hz,false);
    }

    private void drawRunwayWorld(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff456247,0xff34523c,0xff274331},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,h,p);p.setShader(null);
        drawMountains(c,w,h,hz,.18f);
        if(runwayVisible())drawRunway(c,w,h,hz,onGround);
        if(onGround&&Math.abs(crossTrackM)>31f){
            p.setColor(0xddffd45a);p.setTextSize(Math.max(16,w*.016f));p.setTextAlign(Paint.Align.CENTER);
            c.drawText("OFF RUNWAY  •  GRASS",w*.5f,h*.89f,p);p.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawRunway(Canvas c,int w,int h,float hz,boolean ground){
        if(!ground&&!runwayVisible())return;
        float headingErr=angleError(headingDeg,270f);
        float lateralPixels=clamp(crossTrackM/44f,-2.5f,2.5f)*w*.235f;
        float cx=w*.5f-lateralPixels+clamp(headingErr/32f,-1,1)*w*.095f;
        float farHalf,nearHalf,fy,ny;
        if(ground){
            farHalf=w*.032f;nearHalf=w*.44f;fy=hz+h*.015f;ny=h*.999f;
        }else{
            float a=approach01();
            float eased=(float)Math.pow(a,.82);
            nearHalf=w*lerp(.018f,.405f,eased);
            farHalf=w*lerp(.009f,.040f,a);
            fy=lerp(hz+h*.018f,hz+h*.050f,a);
            ny=lerp(hz+h*.10f,h*.985f,(float)Math.pow(a,.74));
            if(!isApproachScene()){
                nearHalf*=.58f;
                farHalf*=.72f;
                ny=lerp(hz+h*.09f,h*.55f,clamp(1-altitudeM/RUNWAY_VISIBLE_ALT_M,0,1));
            }
        }

        quad(c,cx-farHalf*1.52f,fy,cx+farHalf*1.52f,fy,cx+nearHalf*1.20f,ny,cx-nearHalf*1.20f,ny,0xff777a72);
        path.reset();path.moveTo(cx-farHalf,fy);path.lineTo(cx+farHalf,fy);path.lineTo(cx+nearHalf,ny);path.lineTo(cx-nearHalf,ny);path.close();
        p.setShader(new LinearGradient(cx,fy,cx,ny,new int[]{0xff3a3e40,0xff292d2f,0xff1d2224},null,Shader.TileMode.CLAMP));
        c.drawPath(path,p);p.setShader(null);
        stroke.setColor(0xfff5f4ed);stroke.setStrokeWidth(Math.max(2,w*.0026f));
        c.drawLine(cx-farHalf,fy,cx-nearHalf,ny,stroke);c.drawLine(cx+farHalf,fy,cx+nearHalf,ny,stroke);

        for(int i=0;i<42;i++){
            float q=(i/42f+scroll)%1f,z=q*q,y=lerp(fy,ny,z),y2=Math.min(ny,y+3+46*z);
            stroke.setStrokeWidth(1.5f+10*z);c.drawLine(cx,y,cx,y2,stroke);
        }
        for(int i=0;i<34;i++){
            float q=(i/34f+scroll*.62f)%1f,z=q*q,y=lerp(fy,ny,z),hh=lerp(farHalf,nearHalf,z),r=1+4*z;
            p.setColor(i>29?0xffffd465:0xfff5f0d9);c.drawCircle(cx-hh,y,r,p);c.drawCircle(cx+hh,y,r,p);
        }
        for(int set=0;set<4;set++){
            float z=.30f+set*.085f,y=lerp(fy,ny,z*z),hh=lerp(farHalf,nearHalf,z),bw=Math.max(2,hh*.075f),bh=4+19*z;
            p.setColor(0xfff6f5ef);c.drawRect(cx-hh*.54f-bw,y,cx-hh*.54f+bw,y+bh,p);c.drawRect(cx+hh*.54f-bw,y,cx+hh*.54f+bw,y+bh,p);
        }
        if(!ground&&isApproachScene())drawApproachGuidance(c,w,h,cx,fy,ny,farHalf,nearHalf,headingErr);
        if(ground&&alongTrackM<115f){
            float z=clamp(.48f+alongTrackM/260f,0,1),y=lerp(fy,ny,z*z);
            p.setColor(0xfffaf9f4);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(30,w*(.038f+.02f*z)));
            c.drawText("27",cx,Math.min(ny-6,y+54),p);p.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawApproachGuidance(Canvas c,int w,int h,float cx,float fy,float ny,float farHalf,float nearHalf,float headingErr){
        float a=approach01();float py=lerp(fy,ny,.46f),ph=lerp(farHalf,nearHalf,.46f),r=Math.max(2,w*(.0023f+.0015f*a));
        for(int i=0;i<4;i++){p.setColor(i<2?0xfff5f4e8:0xffff3b31);c.drawCircle(cx-ph*1.42f+i*r*3.1f,py,r,p);}
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(Math.max(13,w*.012f));
        p.setColor(Math.abs(headingErr)<8&&Math.abs(crossTrackM)<18?0xff8dff9c:0xffffd45a);
        c.drawText(String.format(java.util.Locale.US,"RWY27  ΔHDG %+.0f°  X-TRK %+.0f m",-headingErr,crossTrackM),w*.5f,h*.70f,p);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawMountains(Canvas c,int w,int h,float hz,float scale){
        path.reset();path.moveTo(0,hz);
        for(int i=0;i<=12;i++){float x=w*i/12f,n=(float)(.45+.55*Math.abs(Math.sin(i*1.73+alongTrackM*.0008)));path.lineTo(x,hz-h*scale*n);}
        path.lineTo(w,hz);path.close();p.setColor(0xff294c3d);c.drawPath(path,p);
    }

    private void drawCrash(Canvas c,int w,int h){
        p.setColor(0x54ff2000);c.drawRect(0,0,w,h,p);
        for(int i=0;i<11;i++){float x=w*(.38f+.026f*i),y=h*(.70f-.035f*(i%5)),r=w*(.018f+.006f*(i%4));p.setColor(i%2==0?0x70404040:0x654f5152);c.drawCircle(x,y,r,p);}
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setColor(0xfffff3e8);p.setTextSize(Math.max(30,w*.035f));c.drawText("AIRCRAFT IMPACT",w*.5f,h*.22f,p);
        p.setTextSize(Math.max(16,w*.017f));c.drawText(crashReason.isEmpty()?"UNSAFE LANDING":crashReason,w*.5f,h*.27f,p);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextAlign(Paint.Align.LEFT);
    }

    private void quad(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4,int color){
        path.reset();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.lineTo(x4,y4);path.close();p.setColor(color);c.drawPath(path,p);
    }
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    private static float angleError(float current,float target){float d=target-current;while(d>180)d-=360;while(d<-180)d+=360;return d;}
}
