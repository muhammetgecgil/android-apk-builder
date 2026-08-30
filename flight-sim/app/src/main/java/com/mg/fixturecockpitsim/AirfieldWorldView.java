package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.util.Locale;

/** AVM-12.1 airfield/world view with runway cross-track, one-way hangar visibility and crash presentation. */
public final class AirfieldWorldView extends View {
    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Path path=new Path();
    private volatile float altitudeM,speedMps,headingDeg,crossTrackM,alongTrackM;
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

    public void setState(double altitude,double speed,boolean ground,String scenePhase,double heading,double crossTrack,double alongTrack,boolean crash,String reason){
        altitudeM=(float)Math.max(0,altitude);
        speedMps=(float)Math.max(0,speed);
        onGround=ground;
        phase=scenePhase==null?"":scenePhase;
        headingDeg=(float)heading;
        crossTrackM=(float)crossTrack;
        alongTrackM=(float)alongTrack;
        crashed=crash;
        crashReason=reason==null?"":reason;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime();
        float dt=lastNs==0?.016f:Math.min(.05f,(now-lastNs)/1e9f);lastNs=now;
        scroll=(scroll+speedMps*dt*.032f)%1f;
        int w=getWidth(),h=getHeight();
        float alt01=onGround?0:clamp(altitudeM/1800f,0,1);
        float horizon=h*(onGround?.455f:lerp(.455f,.225f,alt01));
        drawSky(c,w,h,horizon);

        boolean insideHangar=phase.contains("HANGAR_START")&&onGround&&alongTrackM<12f;
        if(insideHangar)drawHangar(c,w,h,horizon);
        else if(phase.contains("ORBIT")&&altitudeM>310f)drawHighTerrain(c,w,h,horizon);
        else drawRunwayWorld(c,w,h,horizon);

        if(crashed)drawCrash(c,w,h);
        if(speedMps>.3f||crashed)postInvalidateOnAnimation();
    }

    private void drawSky(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{0xff061621,0xff177a9d,0xffb8d9df},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,hz,p);p.setShader(null);
        for(int i=0;i<6;i++){
            float x=(w*(.10f+i*.19f)+alongTrackM*.17f)%Math.max(1,w),y=hz*(.10f+(i%3)*.07f),ww=w*(.09f+(i%2)*.035f);
            p.setColor(0x25ffffff);c.drawOval(x,y,x+ww,y+hz*.022f,p);c.drawOval(x+ww*.20f,y-hz*.012f,x+ww*.70f,y+hz*.017f,p);
        }
    }

    private void drawHangar(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,0,0,h,new int[]{0xff0b1115,0xff1b2428,0xff353c3e},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        float cx=w*.5f,top=h*.17f,bottom=h*.80f,half=w*.27f;
        p.setColor(0xff88b5c3);c.drawRect(cx-half,top,cx+half,bottom,p);
        drawMountains(c,w,h,bottom*.48f,.14f);
        quad(c,cx-w*.032f,top+h*.075f,cx+w*.032f,top+h*.075f,cx+w*.28f,bottom,cx-w*.28f,bottom,0xff454b4d);
        stroke.setColor(0xffefc92c);stroke.setStrokeWidth(5);c.drawLine(cx,top+h*.075f,cx,bottom,stroke);
        stroke.setColor(0xff667177);stroke.setStrokeWidth(7);
        for(int i=-7;i<=7;i++){float x=cx+i*w*.067f;c.drawLine(x,h,cx+(x-cx)*.30f,bottom,stroke);}
        p.setColor(0xeaffffff);p.setTextSize(Math.max(18,w*.018f));c.drawText("HANGAR EXIT  •  TAXI A  •  RWY 27",w*.035f,h*.94f,p);
    }

    private void drawHighTerrain(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff526b48,0xff324c39,0xff23362c},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        drawMountains(c,w,h,hz,.20f*(1-clamp(altitudeM/2500f,0,.55f)));
        float gh=h-hz;
        for(int i=0;i<38;i++){
            float depth=.05f+((i*19)%37)/40f;
            float y=hz+gh*depth*depth;
            float x=(i*131f+alongTrackM*.45f)%Math.max(1,w);
            float ww=w*(.018f+.052f*depth),hh=h*(.003f+.009f*depth);
            p.setColor(i%3==0?0x22334d2d:i%3==1?0x1f49603a:0x1c263c2b);c.drawOval(x,y,x+ww,y+hh,p);
        }
        // A distant runway remains visible when the aircraft is low enough to be approaching the field.
        if(altitudeM<650f)drawRunway(c,w,h,hz,false);
    }

    private void drawRunwayWorld(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff456247,0xff34523c,0xff274331},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        drawMountains(c,w,h,hz,.18f);
        drawRunway(c,w,h,hz,onGround);
        if(onGround&&alongTrackM>12f&&alongTrackM<62f&&!phase.contains("HANGAR"))drawTower(c,w,h);
        if(onGround&&Math.abs(crossTrackM)>31f){
            p.setColor(0xddffd45a);p.setTextSize(Math.max(16,w*.016f));p.setTextAlign(Paint.Align.CENTER);
            c.drawText("OFF RUNWAY  •  GRASS",w*.5f,h*.89f,p);p.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawRunway(Canvas c,int w,int h,float hz,boolean ground){
        float alt01=ground?0:clamp(altitudeM/1400f,0,1);
        float near=1-clamp(altitudeM/1100f,0,1);
        float headingErr=angleError(headingDeg,270f);
        float lateralPixels=clamp(crossTrackM/44f,-2.3f,2.3f)*w*.24f;
        float cx=w*.5f-lateralPixels+clamp(headingErr/30f,-1,1)*w*.08f;
        float farHalf=w*(.018f+.014f*near),nearHalf=w*(ground?.44f:(.11f+.32f*near));
        float fy=hz+h*.015f,ny=h*(ground?.999f:(.73f+.25f*near));

        quad(c,cx-farHalf*1.45f,fy,cx+farHalf*1.45f,fy,cx+nearHalf*1.19f,ny,cx-nearHalf*1.19f,ny,0xff777a72);
        path.reset();path.moveTo(cx-farHalf,fy);path.lineTo(cx+farHalf,fy);path.lineTo(cx+nearHalf,ny);path.lineTo(cx-nearHalf,ny);path.close();
        p.setShader(new LinearGradient(cx,fy,cx,ny,new int[]{0xff363a3c,0xff292d2f,0xff202426},null,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);

        stroke.setColor(0xfff5f4ed);stroke.setStrokeWidth(Math.max(2,w*.0026f));c.drawLine(cx-farHalf,fy,cx-nearHalf,ny,stroke);c.drawLine(cx+farHalf,fy,cx+nearHalf,ny,stroke);
        for(int i=0;i<38;i++){
            float q=(i/38f+scroll)%1f,z=q*q,y=lerp(fy,ny,z),y2=Math.min(ny,y+4+43*z);
            stroke.setStrokeWidth(2+10*z);c.drawLine(cx,y,cx,y2,stroke);
        }
        for(int i=0;i<32;i++){
            float q=(i/32f+scroll*.62f)%1f,z=q*q,y=lerp(fy,ny,z),hh=lerp(farHalf,nearHalf,z),r=1+4*z;
            p.setColor(i>27?0xffffd465:0xfff5f0d9);c.drawCircle(cx-hh,y,r,p);c.drawCircle(cx+hh,y,r,p);
        }
        // Touchdown / aiming rectangles.
        for(int set=0;set<3;set++){
            float z=.34f+set*.12f,y=lerp(fy,ny,z*z),hh=lerp(farHalf,nearHalf,z),bw=hh*.09f,bh=5+17*z;
            p.setColor(0xfff6f5ef);c.drawRect(cx-hh*.53f-bw,y,cx-hh*.53f+bw,y+bh,p);c.drawRect(cx+hh*.53f-bw,y,cx+hh*.53f+bw,y+bh,p);
        }
        if(ground&&alongTrackM<115f){
            float z=clamp(.48f+alongTrackM/260f,0,1),y=lerp(fy,ny,z*z),hh=lerp(farHalf,nearHalf,z);
            p.setColor(0xfffaf9f4);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(30,w*(.038f+.02f*z)));c.drawText("27",cx,Math.min(ny-6,y+54),p);p.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawMountains(Canvas c,int w,int h,float hz,float scale){
        path.reset();path.moveTo(0,hz);
        for(int i=0;i<=12;i++){
            float x=w*i/12f;
            float n=(float)(.45+.55*Math.abs(Math.sin(i*1.73+alongTrackM*.0008)));
            path.lineTo(x,hz-h*scale*n);
        }
        path.lineTo(w,hz);path.close();p.setColor(0xff294c3d);c.drawPath(path,p);
    }

    private void drawTower(Canvas c,int w,int h){
        // Tower is deliberately a nearby airfield object only. It is removed completely after 62 m.
        float q=clamp((alongTrackM-12f)/50f,0,1),cx=w*(.82f+.10f*q),base=h*(.80f+.06f*q),s=1f-.38f*q;
        float shaftW=w*.020f*s,shaftH=h*.115f*s,cabW=w*.060f*s,cabH=h*.043f*s;
        float top=base-shaftH;
        p.setColor(0xffaeb7ba);c.drawRect(cx-shaftW*.5f,top,cx+shaftW*.5f,base,p);
        p.setColor(0xff183c4b);c.drawRect(cx-cabW*.5f,top-cabH,cx+cabW*.5f,top,p);
        stroke.setColor(0xff80b6c5);stroke.setStrokeWidth(1.5f);for(int i=-2;i<=2;i++)c.drawLine(cx+i*cabW/5f,top-cabH,cx+i*cabW/5f,top,stroke);
        p.setColor(0xff6c767a);c.drawRect(cx-cabW*.58f,top-cabH-5,cx+cabW*.58f,top-cabH,p);
        stroke.setColor(0xffd6d4bc);stroke.setStrokeWidth(2);c.drawLine(cx,top-cabH-5,cx,top-cabH-18*s,stroke);
    }

    private void drawCrash(Canvas c,int w,int h){
        p.setColor(0x54ff2000);c.drawRect(0,0,w,h,p);
        for(int i=0;i<11;i++){
            float x=w*(.38f+.026f*i),y=h*(.70f-.035f*(i%5)),r=w*(.018f+.006f*(i%4));
            p.setColor(i%2==0?0x70404040:0x654f5152);c.drawCircle(x,y,r,p);
        }
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
