package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Realistic stylised runway/airport overlay plus scenic demo terrain. */
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
        scroll=(scroll+speedMps*dt*.020f)%1f;
        int w=getWidth(), h=getHeight(); if(w<2||h<2)return;

        float alt=Math.min(1800f,altitudeM);
        float horizon=h*(.47f + .07f*Math.min(1f,alt/1200f));

        // Atmosphere / distant airport landscape.
        fill.setColor(0x16355770); c.drawRect(0,0,w,horizon,fill);
        drawDistantAirport(c,w,h,horizon);

        if(demoMode && phase.contains("ORBIT")) drawScenery(c,w,h,horizon);

        boolean takeoffVisual=phase.contains("RUNWAY")||phase.contains("TAKEOFF")||(phase.contains("ROTATE")&&altitudeM<170f);
        boolean landingVisual=phase.contains("APPROACH")||phase.contains("FLARE")||phase.contains("ROLLOUT")||phase.contains("COMPLETE");
        boolean runwayVisible=onGround||takeoffVisual||landingVisual;
        if(!runwayVisible){ if(speedMps>1f)postInvalidateOnAnimation(); return; }

        float near=1f-Math.min(1f,altitudeM/950f);
        float farHalf=w*(.022f+.010f*near);
        float nearHalf=w*(onGround?.45f:(.14f+.28f*near));
        float farY=horizon+h*(.012f+.018f*near);
        float nearY=h*(onGround?.995f:(.70f+.28f*near));
        float cx=w*.5f;

        // Grass infield and paved shoulders.
        fill.setColor(0x70354425); c.drawRect(0,horizon,w,h,fill);
        Path shoulder=new Path();
        shoulder.moveTo(cx-farHalf*1.34f,farY); shoulder.lineTo(cx+farHalf*1.34f,farY); shoulder.lineTo(cx+nearHalf*1.10f,nearY); shoulder.lineTo(cx-nearHalf*1.10f,nearY); shoulder.close();
        fill.setColor(0xbb696b68); c.drawPath(shoulder,fill);

        runway.reset(); runway.moveTo(cx-farHalf,farY); runway.lineTo(cx+farHalf,farY); runway.lineTo(cx+nearHalf,nearY); runway.lineTo(cx-nearHalf,nearY); runway.close();
        fill.setColor(0xf03c3d3f); c.drawPath(runway,fill);

        // Asphalt longitudinal tonal bands / rubbered centre area.
        for(int i=-5;i<=5;i++){
            float t=i/5f;
            float x0=cx+t*nearHalf*.82f;
            line.setColor((i==0)?0x50301010:0x18202020); line.setStrokeWidth(Math.max(3f,w*.0032f));
            c.drawLine(cx+t*farHalf*.82f,farY,x0,nearY,line);
        }
        line.setColor(0x70301010); line.setStrokeWidth(Math.max(5f,w*.004f));
        for(int i=0;i<7;i++){
            float x=cx+(i-3)*nearHalf*.045f;
            c.drawLine(cx+(i-3)*farHalf*.045f,farY+h*.018f,x,nearY-h*.02f,line);
        }

        // Runway edge lines.
        line.setColor(0xffeeeeeb); line.setStrokeWidth(Math.max(2f,w*.0024f));
        c.drawLine(cx-farHalf,farY,cx-nearHalf,nearY,line);
        c.drawLine(cx+farHalf,farY,cx+nearHalf,nearY,line);

        // Perspective centreline dashes, animated with ground speed.
        for(int i=0;i<17;i++){
            float q=(i+scroll)/17f; q=q-(float)Math.floor(q); float p=q*q;
            float y=farY+(nearY-farY)*p;
            float half=farHalf+(nearHalf-farHalf)*p;
            float dashW=Math.max(2f,half*.026f), dashH=Math.max(4f,7f+34f*p);
            line.setStrokeWidth(dashW); line.setColor(0xf4ffffff);
            c.drawLine(cx,y,cx,Math.min(nearY,y+dashH),line);
        }

        // Edge lights and centreline lights.
        for(int i=0;i<22;i++){
            float q=i/21f, p=q*q;
            float y=farY+(nearY-farY)*p;
            float half=farHalf+(nearHalf-farHalf)*p;
            float r=1.2f+4.5f*p;
            fill.setColor(0xfff6f0d4); c.drawCircle(cx-half,y,r,fill); c.drawCircle(cx+half,y,r,fill);
            if(i%2==0){fill.setColor(i<14?0xfff2f0d0:0xff55d879);c.drawCircle(cx,y,r*.7f,fill);}
        }

        // Threshold bars and runway designation when close/on ground.
        if(onGround||altitudeM<130f||phase.contains("FLARE")||phase.contains("ROLLOUT")){
            float p=.80f+.13f*near;
            float y=farY+(nearY-farY)*p;
            float half=farHalf+(nearHalf-farHalf)*p;
            float barW=half*.11f, barH=20f+42f*near;
            fill.setColor(0xf0ffffff);
            for(int i=-4;i<=4;i++){
                if(i==0)continue;
                float x=cx+i*half*.105f;
                c.drawRect(x-barW*.34f,y,x+barW*.34f,Math.min(nearY,y+barH),fill);
            }
            fill.setColor(0xe8ffffff); fill.setTextAlign(Paint.Align.CENTER); fill.setTextSize(Math.max(26f,w*.038f));
            c.save(); c.rotate(0,cx,nearY); c.drawText("27",cx,Math.min(nearY-18f,y+barH+70f),fill); c.restore(); fill.setTextAlign(Paint.Align.LEFT);
        }

        // PAPI lights on left side during approach/landing.
        if(landingVisual){
            float py=farY+(nearY-farY)*.58f; float ph=farHalf+(nearHalf-farHalf)*.58f;
            for(int i=0;i<4;i++){fill.setColor(i<2?0xffff3c30:0xfff4f2e9);c.drawCircle(cx-ph*1.18f+i*10f,py,3.5f,fill);}
        }

        if(speedMps>1f)postInvalidateOnAnimation();
    }

    private void drawDistantAirport(Canvas c,int w,int h,float horizon){
        // Distant mountain ridge.
        mountains.reset(); mountains.moveTo(0,horizon+h*.045f);
        for(int i=0;i<=10;i++){
            float x=w*i/10f;
            float y=horizon-h*(.015f+(i%3)*.015f+(float)Math.sin(i*1.7)*.009f);
            mountains.lineTo(x,y);
        }
        mountains.lineTo(w,horizon+h*.07f); mountains.lineTo(0,horizon+h*.07f); mountains.close();
        fill.setColor(0x8a2d3934); c.drawPath(mountains,fill);

        // Terminal/hangar blocks, tower and apron lights.
        fill.setColor(0xb0444849);
        for(int i=0;i<8;i++){
            float x=w*(.035f+i*.055f), bw=w*(.035f+(i%2)*.010f), bh=h*(.020f+(i%3)*.008f);
            c.drawRect(x,horizon-bh,x+bw,horizon,fill);
        }
        float tx=w*.77f; fill.setColor(0xcc3c4142); c.drawRect(tx-5,horizon-h*.065f,tx+5,horizon,fill); c.drawRect(tx-14,horizon-h*.078f,tx+14,horizon-h*.060f,fill);
        fill.setColor(0xffffdd72);
        for(int i=0;i<16;i++)c.drawCircle(w*(.02f+i*.061f),horizon-3f,1.8f,fill);
    }

    private void drawScenery(Canvas c,int w,int h,float horizon){
        float cycle=(demoProgressSec%75f)/75f; float offset=(cycle*1.8f-.9f)*w;
        mountains.reset(); mountains.moveTo(-w*.2f-offset,horizon+h*.08f);
        mountains.lineTo(w*.08f-offset,horizon-h*.08f); mountains.lineTo(w*.25f-offset,horizon+h*.04f); mountains.lineTo(w*.48f-offset,horizon-h*.12f); mountains.lineTo(w*.72f-offset,horizon+h*.05f); mountains.lineTo(w*1.08f-offset,horizon-h*.06f); mountains.lineTo(w*1.3f-offset,horizon+h*.10f); mountains.lineTo(w*1.3f,h); mountains.lineTo(-w*.3f,h); mountains.close();
        fill.setColor(0x704d5b4a); c.drawPath(mountains,fill);
        float waterTop=horizon+h*.18f; fill.setColor(0x50355f73); c.drawRect(0,waterTop,w,h,fill);
        line.setColor(0x665e91a3); line.setStrokeWidth(2f); for(int i=0;i<6;i++){float y=waterTop+i*h*.045f;c.drawLine(0,y,w,y,line);}
        fill.setColor(0x70424a47); float cityX=w*(.15f+.60f*((demoProgressSec%110f)/110f));
        for(int i=0;i<11;i++){float bw=w*(.012f+(i%3)*.004f),bh=h*(.035f+(i%4)*.012f);float x=cityX+i*w*.025f,y=horizon+h*.13f-bh;c.drawRect(x,y,x+bw,y+bh,fill);}
        fill.setColor(0xb8ffffff); fill.setTextSize(Math.max(20f,w*.022f)); String scene; float q=demoProgressSec%300f;
        if(q<75)scene="DAĞ ROTASI"; else if(q<150)scene="VADİ / GÖL"; else if(q<225)scene="ŞEHİR ÜZERİ"; else scene="DÖNÜŞ ROTASI";
        c.drawText(scene,w*.04f,h*.88f,fill);
    }
}
