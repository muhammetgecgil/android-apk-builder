package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Lightweight runway/ground perspective cue plus scenic demo terrain. */
public final class RunwayHudView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path runway = new Path();
    private final Path mountains = new Path();
    private volatile float altitudeM, speedMps;
    private volatile boolean onGround, demoMode;
    private volatile String phase = "PREFLIGHT";
    private volatile float demoProgressSec;
    private float scroll;
    private long lastNs;

    public RunwayHudView(Context context) {
        super(context);
        setWillNotDraw(false);
        fill.setStyle(Paint.Style.FILL);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setDemoMode(boolean enabled){demoMode=enabled;postInvalidateOnAnimation();}
    public void setDemoProgress(double sec){demoProgressSec=(float)Math.max(0,sec);postInvalidateOnAnimation();}

    public void setFlightState(double altitude, double speed, boolean ground, String missionPhase) {
        altitudeM=(float)Math.max(0,altitude);
        speedMps=(float)Math.max(0,speed);
        onGround=ground;
        phase=missionPhase==null?"":missionPhase;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now=System.nanoTime();
        float dt=lastNs==0?0.016f:Math.min(.05f,(now-lastNs)/1_000_000_000f); lastNs=now;
        scroll=(scroll+speedMps*dt*.018f)%1f;
        int w=getWidth(), h=getHeight(); if(w<2||h<2)return;

        boolean runwayPhase=phase.contains("TAKEOFF")||phase.contains("ROTATE")||phase.contains("APPROACH")||phase.contains("FLARE")||phase.contains("ROLLOUT")||phase.contains("STOP");
        float alt=Math.min(1500f,altitudeM);
        float near=1f-Math.min(1f,alt/1200f);
        float horizon=h*(.50f + .08f*Math.min(1f,alt/1000f));

        // Sky / horizon / terrain base.
        fill.setColor(0x14344f64); c.drawRect(0,0,w,horizon,fill);
        fill.setColor(0x30384a38); c.drawRect(0,horizon,w,h,fill);
        line.setColor(0x554d6b78); line.setStrokeWidth(2f); c.drawLine(0,horizon,w,horizon,line);

        if(demoMode && phase.contains("ORBIT")) drawScenery(c,w,h,horizon);

        if(!runwayPhase && !onGround) {
            if(speedMps>1f) postInvalidateOnAnimation();
            return;
        }

        float farHalf=w*(.018f + .012f*near);
        float nearHalf=w*(onGround?.40f:(.15f+.22f*near));
        float farY=horizon + h*(.015f+.025f*near);
        float nearY=h*(onGround?.98f:(.72f+.25f*near));
        float cx=w*.5f;

        runway.reset(); runway.moveTo(cx-farHalf,farY); runway.lineTo(cx+farHalf,farY); runway.lineTo(cx+nearHalf,nearY); runway.lineTo(cx-nearHalf,nearY); runway.close();
        fill.setColor(0x90303539); c.drawPath(runway,fill);
        line.setColor(0xccd7dde0); line.setStrokeWidth(Math.max(2f,w*.003f));
        c.drawLine(cx-farHalf,farY,cx-nearHalf,nearY,line); c.drawLine(cx+farHalf,farY,cx+nearHalf,nearY,line);

        for(int i=0;i<13;i++){
            float q=(i+scroll)/13f; q=q-(float)Math.floor(q);
            float p=q*q;
            float y=farY+(nearY-farY)*p;
            float half=farHalf+(nearHalf-farHalf)*p;
            float dashW=Math.max(2f,half*.035f);
            float dashH=Math.max(4f,8f+36f*p);
            line.setStrokeWidth(dashW); line.setColor(0xe6ffffff);
            c.drawLine(cx,y,cx,Math.min(nearY,y+dashH),line);
        }

        if(phase.contains("APPROACH")||phase.contains("FLARE")||phase.contains("ROLLOUT")){
            float p=.78f+.16f*near, y=farY+(nearY-farY)*p;
            float half=farHalf+(nearHalf-farHalf)*p;
            line.setStrokeWidth(Math.max(3f,w*.004f)); line.setColor(0xe8ffffff);
            for(int i=-4;i<=4;i+=2){float x=cx+i*half*.105f;c.drawLine(x,y,x,y+18f+30f*near,line);}
        }
        if(speedMps>1f) postInvalidateOnAnimation();
    }

    private void drawScenery(Canvas c,int w,int h,float horizon){
        float cycle=(demoProgressSec%75f)/75f;
        float offset=(cycle*1.8f-.9f)*w;

        // Layered mountains moving slowly under the aircraft.
        mountains.reset(); mountains.moveTo(-w*.2f-offset,horizon+h*.08f);
        mountains.lineTo(w*.08f-offset,horizon-h*.08f);
        mountains.lineTo(w*.25f-offset,horizon+h*.04f);
        mountains.lineTo(w*.48f-offset,horizon-h*.12f);
        mountains.lineTo(w*.72f-offset,horizon+h*.05f);
        mountains.lineTo(w*1.08f-offset,horizon-h*.06f);
        mountains.lineTo(w*1.3f-offset,horizon+h*.10f); mountains.lineTo(w*1.3f,h); mountains.lineTo(-w*.3f,h); mountains.close();
        fill.setColor(0x704d5b4a); c.drawPath(mountains,fill);

        // Water / valley segment.
        float waterTop=horizon+h*.18f;
        fill.setColor(0x50355f73); c.drawRect(0,waterTop,w,h,fill);
        line.setColor(0x665e91a3); line.setStrokeWidth(2f);
        for(int i=0;i<6;i++){float y=waterTop+i*h*.045f; c.drawLine(0,y,w,y,line);}

        // Small city/settlement silhouettes to make the demo feel like a tour.
        fill.setColor(0x70424a47);
        float cityX=w*(.15f+.60f*((demoProgressSec%110f)/110f));
        for(int i=0;i<11;i++){
            float bw=w*(.012f+(i%3)*.004f), bh=h*(.035f+(i%4)*.012f);
            float x=cityX+i*w*.025f, y=horizon+h*.13f-bh;
            c.drawRect(x,y,x+bw,y+bh,fill);
        }

        line.setStyle(Paint.Style.FILL); line.setColor(0xb8ffffff); line.setTextSize(Math.max(20f,w*.022f));
        String scene;
        float q=demoProgressSec%300f;
        if(q<75)scene="DAĞ ROTASI"; else if(q<150)scene="VADİ / GÖL"; else if(q<225)scene="ŞEHİR ÜZERİ"; else scene="DÖNÜŞ ROTASI";
        c.drawText(scene,w*.04f,h*.88f,line); line.setStyle(Paint.Style.STROKE);
    }
}
