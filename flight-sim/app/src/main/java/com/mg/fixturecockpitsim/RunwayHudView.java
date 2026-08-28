package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Detailed perspective runway, taxiway and airport scenery for the 3D aircraft display. */
public final class RunwayHudView extends View {
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path runway=new Path();
    private final Path path=new Path();
    private final Path mountains=new Path();
    private volatile float altitudeM,speedMps;
    private volatile boolean onGround,demoMode;
    private volatile String phase="PREFLIGHT";
    private volatile float demoProgressSec;
    private float scroll;
    private long lastNs;

    public RunwayHudView(Context context){
        super(context); setWillNotDraw(false); fill.setStyle(Paint.Style.FILL); line.setStyle(Paint.Style.STROKE); line.setStrokeCap(Paint.Cap.ROUND);
    }
    public void setDemoMode(boolean enabled){demoMode=enabled;postInvalidateOnAnimation();}
    public void setDemoProgress(double sec){demoProgressSec=(float)Math.max(0,sec);postInvalidateOnAnimation();}
    public void setFlightState(double altitude,double speed,boolean ground,String missionPhase){altitudeM=(float)Math.max(0,altitude);speedMps=(float)Math.max(0,speed);onGround=ground;phase=missionPhase==null?"":missionPhase;postInvalidateOnAnimation();}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime(); float dt=lastNs==0?.016f:Math.min(.05f,(now-lastNs)/1_000_000_000f); lastNs=now;
        scroll=(scroll+speedMps*dt*.020f)%1f;
        int w=getWidth(),h=getHeight(); if(w<2||h<2)return;
        float alt=Math.min(1800f,altitudeM),horizon=h*(.47f+.07f*Math.min(1f,alt/1200f));

        drawSky(c,w,h,horizon); drawDistantAirport(c,w,h,horizon);
        if(demoMode&&phase.contains("ORBIT"))drawScenery(c,w,h,horizon);

        boolean takeoff=phase.contains("RUNWAY")||phase.contains("TAKEOFF")||(phase.contains("ROTATE")&&altitudeM<170f);
        boolean landing=phase.contains("APPROACH")||phase.contains("FLARE")||phase.contains("ROLLOUT")||phase.contains("COMPLETE");
        if(!(onGround||takeoff||landing)){if(speedMps>1f)postInvalidateOnAnimation();return;}

        float near=1f-Math.min(1f,altitudeM/950f),farHalf=w*(.022f+.010f*near),nearHalf=w*(onGround?.45f:(.14f+.28f*near));
        float farY=horizon+h*(.012f+.018f*near),nearY=h*(onGround?.995f:(.70f+.28f*near)),cx=w*.5f;

        drawGroundAndShoulders(c,w,h,horizon,cx,farY,nearY,farHalf,nearHalf);
        drawRunwayBase(c,cx,farY,nearY,farHalf,nearHalf);
        drawSurfaceWear(c,w,h,cx,farY,nearY,farHalf,nearHalf);
        drawTaxiway(c,w,h,cx,farY,nearY,farHalf,nearHalf);
        drawRunwayMarkings(c,w,h,cx,farY,nearY,farHalf,nearHalf,near,landing);
        drawRunwayLights(c,w,cx,farY,nearY,farHalf,nearHalf,landing);
        if(landing)drawApproachLights(c,w,h,cx,horizon,farY,farHalf);
        if(onGround||altitudeM<160f)drawAirportFurniture(c,w,h,cx,farY,nearY,farHalf,nearHalf);
        if(speedMps>1f)postInvalidateOnAnimation();
    }

    private void drawSky(Canvas c,int w,int h,float horizon){
        fill.setColor(0xff17283c); c.drawRect(0,0,w,horizon,fill);
        fill.setColor(0x553a5f78); c.drawRect(0,horizon-h*.09f,w,horizon,fill);
        fill.setColor(0x183f6f8a); c.drawCircle(w*.78f,h*.12f,w*.10f,fill);
    }

    private void drawGroundAndShoulders(Canvas c,int w,int h,float horizon,float cx,float farY,float nearY,float farHalf,float nearHalf){
        fill.setColor(0xff283326);c.drawRect(0,horizon,w,h,fill);
        for(int i=0;i<18;i++){
            float y=horizon+(h-horizon)*i/18f; line.setColor((i&1)==0?0x183f522e:0x10212e20);line.setStrokeWidth(2f+i*.15f);c.drawLine(0,y,w,y,line);
        }
        path.reset();path.moveTo(cx-farHalf*1.42f,farY);path.lineTo(cx+farHalf*1.42f,farY);path.lineTo(cx+nearHalf*1.14f,nearY);path.lineTo(cx-nearHalf*1.14f,nearY);path.close();
        fill.setColor(0xff696b69);c.drawPath(path,fill);
        line.setColor(0x7050524f);line.setStrokeWidth(Math.max(2f,w*.0015f));
        c.drawLine(cx-farHalf*1.23f,farY,cx-nearHalf*1.08f,nearY,line);c.drawLine(cx+farHalf*1.23f,farY,cx+nearHalf*1.08f,nearY,line);
    }

    private void drawRunwayBase(Canvas c,float cx,float farY,float nearY,float farHalf,float nearHalf){
        runway.reset();runway.moveTo(cx-farHalf,farY);runway.lineTo(cx+farHalf,farY);runway.lineTo(cx+nearHalf,nearY);runway.lineTo(cx-nearHalf,nearY);runway.close();
        fill.setColor(0xff343638);c.drawPath(runway,fill);
        for(int i=-6;i<=6;i++){float t=i/6f;line.setColor(i==0?0x49302a2a:0x17252829);line.setStrokeWidth(4f);c.drawLine(cx+t*farHalf*.86f,farY,cx+t*nearHalf*.86f,nearY,line);}
    }

    private void drawSurfaceWear(Canvas c,int w,int h,float cx,float farY,float nearY,float farHalf,float nearHalf){
        line.setStrokeWidth(Math.max(1f,w*.0010f));line.setColor(0x38202425);
        for(int i=1;i<16;i++){float q=i/16f,p=q*q,y=lerp(farY,nearY,p),half=lerp(farHalf,nearHalf,p);c.drawLine(cx-half*.96f,y,cx+half*.96f,y,line);}
        line.setStrokeCap(Paint.Cap.SQUARE);
        for(int i=-5;i<=5;i++){
            float off=i*.027f;float p0=.48f+(Math.abs(i)%3)*.018f,p1=.96f;float y0=lerp(farY,nearY,p0),y1=lerp(farY,nearY,p1);float h0=lerp(farHalf,nearHalf,p0),h1=lerp(farHalf,nearHalf,p1);
            line.setColor(0x70311617);line.setStrokeWidth(Math.max(2f,w*(.0015f+.00025f*Math.abs(i))));c.drawLine(cx+off*h0,y0,cx+off*h1,y1,line);
        }
        line.setStrokeCap(Paint.Cap.ROUND);
        drawPatch(c,cx,farY,nearY,farHalf,nearHalf,.37f,-.52f,.19f,.045f);drawPatch(c,cx,farY,nearY,farHalf,nearHalf,.58f,.48f,.17f,.035f);drawPatch(c,cx,farY,nearY,farHalf,nearHalf,.76f,-.64f,.12f,.030f);
    }

    private void drawPatch(Canvas c,float cx,float farY,float nearY,float farHalf,float nearHalf,float q,float lateral,float width,float depth){
        float p=q*q,y=lerp(farY,nearY,p),half=lerp(farHalf,nearHalf,p),hh=Math.max(3f,(nearY-farY)*depth*p);fill.setColor(0x55303132);c.drawRect(cx+lateral*half-width*half,y,cx+lateral*half+width*half,y+hh,fill);
    }

    private void drawTaxiway(Canvas c,int w,int h,float cx,float farY,float nearY,float farHalf,float nearHalf){
        float p0=.43f,p1=.76f,y0=lerp(farY,nearY,p0),y1=lerp(farY,nearY,p1),r0=lerp(farHalf,nearHalf,p0),r1=lerp(farHalf,nearHalf,p1);
        path.reset();path.moveTo(cx+r0*.98f,y0);path.cubicTo(cx+r0*1.55f,y0+h*.025f,cx+r1*1.18f,y1-h*.035f,w*.98f,y1);path.lineTo(w*.98f,y1+h*.065f);path.cubicTo(cx+r1*1.34f,y1+h*.02f,cx+r0*1.66f,y0+h*.065f,cx+r0*1.02f,y0+h*.025f);path.close();fill.setColor(0xff555957);c.drawPath(path,fill);
        line.setColor(0xffe3bc38);line.setStrokeWidth(Math.max(2f,w*.0022f));path.reset();path.moveTo(cx+r0*1.05f,y0+h*.014f);path.cubicTo(cx+r0*1.55f,y0+h*.042f,cx+r1*1.22f,y1-h*.012f,w*.98f,y1+h*.033f);c.drawPath(path,line);
        fill.setColor(0xff4d8dff);for(int i=0;i<7;i++){float t=i/6f,x=lerp(cx+r0*1.10f,w*.95f,t),y=lerp(y0+h*.035f,y1+h*.060f,t);c.drawCircle(x,y,1.8f+2.0f*t,fill);}
    }

    private void drawRunwayMarkings(Canvas c,int w,int h,float cx,float farY,float nearY,float farHalf,float nearHalf,float near,boolean landing){
        line.setColor(0xfff2f1eb);line.setStrokeWidth(Math.max(2f,w*.0024f));c.drawLine(cx-farHalf,farY,cx-nearHalf,nearY,line);c.drawLine(cx+farHalf,farY,cx+nearHalf,nearY,line);
        for(int i=0;i<18;i++){float q=(i+scroll)/18f;q-=Math.floor(q);float p=q*q,y=lerp(farY,nearY,p),half=lerp(farHalf,nearHalf,p);line.setStrokeWidth(Math.max(2f,half*.026f));line.setColor(0xf5ffffff);c.drawLine(cx,y,cx,Math.min(nearY,y+6f+34f*p),line);}
        drawPairedBars(c,cx,farY,nearY,farHalf,nearHalf,.56f,.30f,.15f,.035f);drawPairedBars(c,cx,farY,nearY,farHalf,nearHalf,.68f,.30f,.18f,.045f);
        drawPairedBars(c,cx,farY,nearY,farHalf,nearHalf,.78f,.30f,.12f,.030f);
        if(onGround||altitudeM<150f||landing){
            float p=.82f+.10f*near,y=lerp(farY,nearY,p),half=lerp(farHalf,nearHalf,p),barW=half*.10f,barH=18f+44f*near;fill.setColor(0xf5ffffff);
            for(int i=-4;i<=4;i++){if(i==0)continue;float x=cx+i*half*.105f;c.drawRect(x-barW*.34f,y,x+barW*.34f,Math.min(nearY,y+barH),fill);}
            fill.setTextAlign(Paint.Align.CENTER);fill.setTextSize(Math.max(30f,w*.040f));c.drawText("27",cx,Math.min(nearY-18f,y+barH+72f),fill);fill.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawPairedBars(Canvas c,float cx,float farY,float nearY,float farHalf,float nearHalf,float p,float lateral,float width,float depth){
        float y=lerp(farY,nearY,p),half=lerp(farHalf,nearHalf,p),hh=Math.max(4f,(nearY-farY)*depth*p);fill.setColor(0xeef5f4ef);float hw=half*width*.5f;float lx=cx-half*lateral,rx=cx+half*lateral;c.drawRect(lx-hw,y,lx+hw,y+hh,fill);c.drawRect(rx-hw,y,rx+hw,y+hh,fill);
    }

    private void drawRunwayLights(Canvas c,int w,float cx,float farY,float nearY,float farHalf,float nearHalf,boolean landing){
        for(int i=0;i<25;i++){float q=i/24f,p=q*q,y=lerp(farY,nearY,p),half=lerp(farHalf,nearHalf,p),r=1.1f+4.8f*p;fill.setColor(i>19?0xffffd54f:0xfff6f0dc);c.drawCircle(cx-half,y,r,fill);c.drawCircle(cx+half,y,r,fill);if((i&1)==0){fill.setColor(i>19?0xffff4545:(i>16?0xffffeeee:0xffffffff));c.drawCircle(cx,y,r*.62f,fill);}}
        if(landing){float p=.91f,y=lerp(farY,nearY,p),half=lerp(farHalf,nearHalf,p);fill.setColor(0xff52e890);for(int i=-6;i<=6;i++)c.drawCircle(cx+i*half*.075f,y,2.2f+2.2f*p,fill);}
        if(landing){float p=.58f,py=lerp(farY,nearY,p),ph=lerp(farHalf,nearHalf,p),baseX=cx-ph*1.22f;fill.setColor(0xdd161718);c.drawRoundRect(baseX-8,py-7,baseX+42,py+7,4,4,fill);for(int i=0;i<4;i++){fill.setColor(i<2?0xffff3b30:0xfff6f4ea);c.drawCircle(baseX+i*11f,py,4.0f,fill);}}
    }

    private void drawApproachLights(Canvas c,int w,int h,float cx,float horizon,float farY,float farHalf){
        for(int i=0;i<9;i++){float t=i/8f,y=farY-(farY-horizon)*(.08f+.55f*t),r=1.3f+1.5f*(1f-t);fill.setColor(0xfffff7df);c.drawCircle(cx,y,r,fill);if(i==2||i==5||i==8){float span=farHalf*(1.8f-.7f*t);for(int k=-3;k<=3;k++)c.drawCircle(cx+k*span/3f,y,r*.85f,fill);}}
    }

    private void drawAirportFurniture(Canvas c,int w,int h,float cx,float farY,float nearY,float farHalf,float nearHalf){
        float p=.70f,y=lerp(farY,nearY,p),half=lerp(farHalf,nearHalf,p);float sx=cx+half*1.42f;fill.setColor(0xff161815);c.drawRoundRect(sx,y-19,sx+72,y+5,4,4,fill);fill.setColor(0xffffca35);fill.setTextSize(Math.max(12f,w*.012f));fill.setTextAlign(Paint.Align.CENTER);c.drawText("A  27",sx+36,y-2,fill);fill.setTextAlign(Paint.Align.LEFT);
        line.setColor(0xffded5b8);line.setStrokeWidth(2f);float wx=cx-half*1.62f;c.drawLine(wx,y-35,wx,y+5,line);fill.setColor(0xffff8b32);path.reset();path.moveTo(wx,y-34);path.lineTo(wx+28,y-27);path.lineTo(wx,y-20);path.close();c.drawPath(path,fill);
    }

    private void drawDistantAirport(Canvas c,int w,int h,float horizon){
        mountains.reset();mountains.moveTo(0,horizon+h*.045f);for(int i=0;i<=12;i++){float x=w*i/12f,y=horizon-h*(.014f+(i%4)*.011f+(float)Math.sin(i*1.53)*.010f);mountains.lineTo(x,y);}mountains.lineTo(w,horizon+h*.07f);mountains.lineTo(0,horizon+h*.07f);mountains.close();fill.setColor(0xff26322f);c.drawPath(mountains,fill);
        for(int i=0;i<7;i++){float x=w*(.025f+i*.062f),bw=w*(.046f+(i%2)*.012f),bh=h*(.022f+(i%3)*.009f);fill.setColor(0xff42484a);c.drawRect(x,horizon-bh,x+bw,horizon,fill);path.reset();path.moveTo(x,horizon-bh);path.lineTo(x+bw*.5f,horizon-bh-h*.010f);path.lineTo(x+bw,horizon-bh);path.close();fill.setColor(0xff555b5c);c.drawPath(path,fill);line.setColor(0x88b9d8e7);line.setStrokeWidth(1.5f);for(int k=1;k<4;k++)c.drawLine(x+bw*k/4f,horizon-bh+2,x+bw*k/4f,horizon-2,line);}
        float tx=w*.77f;fill.setColor(0xff3d4547);c.drawRect(tx-5,horizon-h*.071f,tx+5,horizon,fill);fill.setColor(0xff202a2f);c.drawRect(tx-16,horizon-h*.086f,tx+16,horizon-h*.066f,fill);fill.setColor(0xff8dd7ff);c.drawRect(tx-12,horizon-h*.082f,tx+12,horizon-h*.073f,fill);fill.setColor(0xffff624e);c.drawCircle(tx,horizon-h*.093f,2.5f,fill);
        line.setColor(0x555b6670);line.setStrokeWidth(1f);c.drawLine(0,horizon+2,w,horizon+2,line);fill.setColor(0xffffdb6c);for(int i=0;i<18;i++)c.drawCircle(w*(.018f+i*.057f),horizon-3f,1.6f,fill);
    }

    private void drawScenery(Canvas c,int w,int h,float horizon){
        float cycle=(demoProgressSec%75f)/75f,offset=(cycle*1.8f-.9f)*w;mountains.reset();mountains.moveTo(-w*.2f-offset,horizon+h*.08f);mountains.lineTo(w*.08f-offset,horizon-h*.08f);mountains.lineTo(w*.25f-offset,horizon+h*.04f);mountains.lineTo(w*.48f-offset,horizon-h*.12f);mountains.lineTo(w*.72f-offset,horizon+h*.05f);mountains.lineTo(w*1.08f-offset,horizon-h*.06f);mountains.lineTo(w*1.3f-offset,horizon+h*.10f);mountains.lineTo(w*1.3f,h);mountains.lineTo(-w*.3f,h);mountains.close();fill.setColor(0x704d5b4a);c.drawPath(mountains,fill);float waterTop=horizon+h*.18f;fill.setColor(0x50355f73);c.drawRect(0,waterTop,w,h,fill);line.setColor(0x665e91a3);line.setStrokeWidth(2f);for(int i=0;i<6;i++)c.drawLine(0,waterTop+i*h*.045f,w,waterTop+i*h*.045f,line);fill.setColor(0x70424a47);float cityX=w*(.15f+.60f*((demoProgressSec%110f)/110f));for(int i=0;i<11;i++){float bw=w*(.012f+(i%3)*.004f),bh=h*(.035f+(i%4)*.012f),x=cityX+i*w*.025f,y=horizon+h*.13f-bh;c.drawRect(x,y,x+bw,y+bh,fill);}fill.setColor(0xb8ffffff);fill.setTextSize(Math.max(20f,w*.022f));String scene;float q=demoProgressSec%300f;if(q<75)scene="DAĞ ROTASI";else if(q<150)scene="VADİ / GÖL";else if(q<225)scene="ŞEHİR ÜZERİ";else scene="DÖNÜŞ ROTASI";c.drawText(scene,w*.04f,h*.88f,fill);
    }

    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
