package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Lightweight runway/ground perspective cue driven by the autonomous mission state. */
public final class RunwayHudView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path runway = new Path();
    private volatile float altitudeM, speedMps;
    private volatile boolean onGround;
    private volatile String phase = "PREFLIGHT";
    private float scroll;
    private long lastNs;

    public RunwayHudView(Context context) {
        super(context);
        setWillNotDraw(false);
        fill.setStyle(Paint.Style.FILL);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
    }

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

        // Subtle terrain/horizon cue in every airborne phase.
        fill.setColor(0x20384a38);
        c.drawRect(0,horizon,w,h,fill);
        line.setColor(0x554d6b78); line.setStrokeWidth(2f);
        c.drawLine(0,horizon,w,horizon,line);

        if(!runwayPhase && !onGround) return;

        float farHalf=w*(.018f + .012f*near);
        float nearHalf=w*(onGround?.40f:(.15f+.22f*near));
        float farY=horizon + h*(.015f+.025f*near);
        float nearY=h*(onGround?.98f:(.72f+.25f*near));
        float cx=w*.5f;

        runway.reset(); runway.moveTo(cx-farHalf,farY); runway.lineTo(cx+farHalf,farY); runway.lineTo(cx+nearHalf,nearY); runway.lineTo(cx-nearHalf,nearY); runway.close();
        fill.setColor(0x90303539); c.drawPath(runway,fill);
        line.setColor(0xccd7dde0); line.setStrokeWidth(Math.max(2f,w*.003f));
        c.drawLine(cx-farHalf,farY,cx-nearHalf,nearY,line); c.drawLine(cx+farHalf,farY,cx+nearHalf,nearY,line);

        // Perspective centerline dashes; scroll produces runway-speed motion on the ground.
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

        // Threshold bars become prominent in flare/rollout.
        if(phase.contains("APPROACH")||phase.contains("FLARE")||phase.contains("ROLLOUT")){
            float p=.78f+.16f*near, y=farY+(nearY-farY)*p;
            float half=farHalf+(nearHalf-farHalf)*p;
            line.setStrokeWidth(Math.max(3f,w*.004f)); line.setColor(0xe8ffffff);
            for(int i=-4;i<=4;i+=2){float x=cx+i*half*.105f;c.drawLine(x,y,x,y+18f+30f*near,line);}
        }
        if(speedMps>1f) postInvalidateOnAnimation();
    }
}
