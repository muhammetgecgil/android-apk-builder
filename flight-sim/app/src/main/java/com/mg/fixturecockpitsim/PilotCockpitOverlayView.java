package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import java.util.Locale;

/**
 * AVM-17.1 pilot cockpit overlay.
 * Generic modern-fighter inspired layout: HUD, two MFDs, UFCP and warning strip.
 * The MFD regions are intentionally modular so later versions can replace each page
 * with radar, navigation, stores, engine, fuel and systems implementations.
 */
public final class PilotCockpitOverlayView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final RectF r=new RectF();

    private float altitude,speed,heading,roll,pitch,vertical,throttle,gear,brake;
    private String phase="DEMO",weather="";
    private boolean onGround;
    private double simTime;

    public PilotCockpitOverlayView(Context c){
        super(c);
        setClickable(false);setFocusable(false);
        stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
    }

    public void setState(double alt,double spd,double hdg,double bank,double pit,double vs,
                         double thr,double gr,double brk,boolean ground,String ph,String wx,double time){
        altitude=(float)alt;speed=(float)spd;heading=(float)hdg;roll=(float)bank;pitch=(float)pit;vertical=(float)vs;
        throttle=(float)thr;gear=(float)gr;brake=(float)brk;onGround=ground;phase=ph==null?"":ph;weather=wx==null?"":wx;simTime=time;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        drawCanopyFrame(c,w,h);drawHud(c,w,h);drawInstrumentPanel(c,w,h);drawStatusStrip(c,w,h);
    }

    private void drawCanopyFrame(Canvas c,int w,int h){
        stroke.setColor(0xaa101619);stroke.setStrokeWidth(Math.max(8f,w*.008f));
        path.reset();path.moveTo(w*.08f,h);path.quadTo(w*.12f,h*.35f,w*.30f,h*.10f);c.drawPath(path,stroke);
        path.reset();path.moveTo(w*.92f,h);path.quadTo(w*.88f,h*.35f,w*.70f,h*.10f);c.drawPath(path,stroke);
        stroke.setColor(0x701d282c);stroke.setStrokeWidth(Math.max(3f,w*.0025f));
        c.drawLine(w*.30f,h*.10f,w*.70f,h*.10f,stroke);
    }

    private void drawHud(Canvas c,int w,int h){
        final float cx=w*.50f,cy=h*.37f,hw=w*.165f,hh=h*.245f;
        p.setColor(0x1410ff78);r.set(cx-hw,cy-hh,cx+hw,cy+hh);c.drawRoundRect(r,14,14,p);
        stroke.setColor(0xc830ff8a);stroke.setStrokeWidth(Math.max(1.5f,w*.00115f));
        c.drawLine(cx-hw*.72f,cy-hh,cx-hw,cy-hh*.72f,stroke);c.drawLine(cx+hw*.72f,cy-hh,cx+hw,cy-hh*.72f,stroke);
        c.drawLine(cx-hw,cy+hh*.72f,cx-hw*.72f,cy+hh,stroke);c.drawLine(cx+hw,cy+hh*.72f,cx+hw*.72f,cy+hh,stroke);

        text(c,String.format(Locale.US,"%03.0f",norm360(heading)),cx,cy-hh+28,22,0xff52ff96,Paint.Align.CENTER,true);
        text(c,String.format(Locale.US,"%.0f",speed),cx-hw-22,cy-10,20,0xff52ff96,Paint.Align.RIGHT,true);
        text(c,String.format(Locale.US,"%.0f",altitude),cx+hw+22,cy-10,20,0xff52ff96,Paint.Align.LEFT,true);
        text(c,"SPD",cx-hw-22,cy-34,10,0xaa52ff96,Paint.Align.RIGHT,false);
        text(c,"ALT",cx+hw+22,cy-34,10,0xaa52ff96,Paint.Align.LEFT,false);

        // Pitch ladder follows aircraft pitch and rolls with the aircraft attitude.
        c.save();c.rotate(-roll,cx,cy);
        float pitchPx=pitch*h*.0042f;
        for(int deg=-30;deg<=30;deg+=5){
            if(deg==0)continue;float y=cy+pitchPx-deg*h*.0105f;if(y<cy-hh*.72f||y>cy+hh*.72f)continue;
            float len=(deg%10==0)?hw*.32f:hw*.20f;
            c.drawLine(cx-len,y,cx-len*.22f,y,stroke);c.drawLine(cx+len*.22f,y,cx+len,y,stroke);
            if(deg%10==0){text(c,Integer.toString(Math.abs(deg)),cx-len-6,y+4,10,0xcc52ff96,Paint.Align.RIGHT,false);text(c,Integer.toString(Math.abs(deg)),cx+len+6,y+4,10,0xcc52ff96,Paint.Align.LEFT,false);}
        }
        c.drawLine(cx-hw*.24f,cy+pitchPx,cx-hw*.055f,cy+pitchPx,stroke);c.drawLine(cx+hw*.055f,cy+pitchPx,cx+hw*.24f,cy+pitchPx,stroke);
        c.restore();

        // Flight-path marker and boresight.
        float fpy=cy-clamp(vertical,-45,45)*h*.0030f;
        c.drawCircle(cx,fpy,9,stroke);c.drawLine(cx-24,fpy,cx-9,fpy,stroke);c.drawLine(cx+9,fpy,cx+24,fpy,stroke);c.drawLine(cx,fpy+9,cx,fpy+18,stroke);
        c.drawLine(cx-14,cy-5,cx-5,cy-5,stroke);c.drawLine(cx+5,cy-5,cx+14,cy-5,stroke);c.drawLine(cx,cy-10,cx,cy,stroke);

        String gearCue=gear>.85f?"GEAR DN":gear<.12f?"GEAR UP":"GEAR TRANS";
        if(altitude<220||onGround)text(c,gearCue,cx,cy+hh-16,13,gear>.85f?0xff70ff92:0xffffc05a,Paint.Align.CENTER,true);
    }

    private void drawInstrumentPanel(Canvas c,int w,int h){
        float top=h*.675f;
        p.setColor(0xe70a0f12);c.drawRect(0,top,w,h,p);
        p.setColor(0xff111a1e);c.drawRect(0,top,w,h*.705f,p);
        stroke.setColor(0xff34434a);stroke.setStrokeWidth(Math.max(2f,w*.0015f));c.drawLine(0,top,w,top,stroke);

        float mfdSize=Math.min(w*.245f,h*.295f),my=h-mfdSize-h*.018f;
        float lx=w*.035f,rx=w-lx-mfdSize;
        drawMfdFrame(c,lx,my,mfdSize,"FCR / NAV");
        drawMfdFrame(c,rx,my,mfdSize,"ENG / SYS");
        drawLeftMfd(c,lx,my,mfdSize);drawRightMfd(c,rx,my,mfdSize);
        drawCenterConsole(c,w,h,top,lx+mfdSize,rx);
    }

    private void drawMfdFrame(Canvas c,float x,float y,float s,String label){
        p.setColor(0xff1d272b);r.set(x,y,x+s,y+s);c.drawRoundRect(r,12,12,p);
        p.setColor(0xff020807);r.set(x+s*.075f,y+s*.075f,x+s*.925f,y+s*.865f);c.drawRect(r,p);
        stroke.setColor(0xff53646a);stroke.setStrokeWidth(2);c.drawRoundRect(x,y,x+s,y+s,12,12,stroke);
        text(c,label,x+s*.5f,y+s*.94f,12,0xff99aaa9,Paint.Align.CENTER,true);
        p.setColor(0xff29373c);
        for(int i=0;i<5;i++){float bx=x+s*(.10f+i*.20f);c.drawCircle(bx,y+s*.035f,s*.018f,p);c.drawCircle(bx,y+s*.965f,s*.018f,p);}
        for(int i=0;i<4;i++){float by=y+s*(.17f+i*.20f);c.drawCircle(x+s*.035f,by,s*.018f,p);c.drawCircle(x+s*.965f,by,s*.018f,p);}
    }

    private void drawLeftMfd(Canvas c,float x,float y,float s){
        float x0=x+s*.09f,y0=y+s*.09f,x1=x+s*.91f,y1=y+s*.84f,cx=(x0+x1)*.5f,cy=(y0+y1)*.5f;
        stroke.setColor(0x8840e880);stroke.setStrokeWidth(1.2f);
        for(int i=1;i<4;i++){float q=i/4f;c.drawLine(lerp(x0,x1,q),y0,lerp(x0,x1,q),y1,stroke);c.drawLine(x0,lerp(y0,y1,q),x1,lerp(y0,y1,q),stroke);}
        c.drawCircle(cx,cy,s*.22f,stroke);c.drawCircle(cx,cy,s*.11f,stroke);
        float ang=(float)Math.toRadians(heading-270f),rr=s*.19f;
        float tx=cx+(float)Math.sin(ang)*rr,ty=cy-(float)Math.cos(ang)*rr;
        stroke.setColor(0xff4dff92);stroke.setStrokeWidth(2.3f);c.drawLine(cx,cy,tx,ty,stroke);
        path.reset();path.moveTo(cx,cy-s*.035f);path.lineTo(cx-s*.022f,cy+s*.028f);path.lineTo(cx+s*.022f,cy+s*.028f);path.close();p.setColor(0xff62ff9d);c.drawPath(path,p);
        text(c,"FCR",x0+5,y0+16,11,0xff65ff9d,Paint.Align.LEFT,true);
        text(c,"STBY",x1-5,y0+16,11,0xffffc85c,Paint.Align.RIGHT,true);
        text(c,String.format(Locale.US,"HDG %03.0f",norm360(heading)),x0+5,y1-8,11,0xff65ff9d,Paint.Align.LEFT,false);
        text(c,phase.replace('_',' '),x1-5,y1-8,9,0xaa65ff9d,Paint.Align.RIGHT,false);
    }

    private void drawRightMfd(Canvas c,float x,float y,float s){
        float x0=x+s*.10f,y0=y+s*.10f,x1=x+s*.90f,y1=y+s*.83f;
        text(c,"ENG",x0,y0+14,12,0xff69ff9e,Paint.Align.LEFT,true);
        float rpm=58f+throttle*42f;
        gauge(c,x+s*.31f,y+s*.39f,s*.18f,rpm,"L RPM");gauge(c,x+s*.69f,y+s*.39f,s*.18f,rpm*.992f,"R RPM");
        float fuel=clamp(100f-(float)simTime*.028f,18f,100f);
        text(c,String.format(Locale.US,"FUEL  %4.0f%%",fuel),x0,y+s*.66f,12,0xff69ff9e,Paint.Align.LEFT,true);
        text(c,String.format(Locale.US,"THR   %3.0f%%",throttle*100),x0,y+s*.73f,11,0xff69ff9e,Paint.Align.LEFT,false);
        text(c,String.format(Locale.US,"GEAR  %3.0f%%",gear*100),x1,y+s*.66f,11,gear>.85f?0xff7cff91:0xffffcb63,Paint.Align.RIGHT,true);
        text(c,String.format(Locale.US,"BRK   %3.0f%%",brake*100),x1,y+s*.73f,11,0xff69ff9e,Paint.Align.RIGHT,false);
    }

    private void gauge(Canvas c,float cx,float cy,float rad,float value,String label){
        stroke.setStrokeWidth(Math.max(2f,rad*.08f));stroke.setColor(0xff253c35);r.set(cx-rad,cy-rad,cx+rad,cy+rad);c.drawArc(r,135,270,false,stroke);
        stroke.setColor(value>95?0xffffa84f:0xff52f58c);c.drawArc(r,135,270*clamp(value/100f,0,1),false,stroke);
        text(c,String.format(Locale.US,"%.0f",value),cx,cy+5,14,0xff7dffa8,Paint.Align.CENTER,true);text(c,label,cx,cy+rad+15,9,0xff91a79f,Paint.Align.CENTER,false);
    }

    private void drawCenterConsole(Canvas c,int w,int h,float top,float leftEdge,float rightEdge){
        float cx=w*.5f,avail=Math.max(180,rightEdge-leftEdge),cw=Math.min(avail*.74f,w*.34f),x0=cx-cw*.5f,x1=cx+cw*.5f;
        p.setColor(0xff151e22);r.set(x0,top+h*.018f,x1,h*.977f);c.drawRoundRect(r,12,12,p);
        stroke.setColor(0xff3a4a50);stroke.setStrokeWidth(2);c.drawRoundRect(r,12,12,stroke);
        text(c,"UFCP",cx,top+h*.050f,11,0xff9cb0b4,Paint.Align.CENTER,true);
        p.setColor(0xff030b09);r.set(x0+cw*.12f,top+h*.070f,x1-cw*.12f,top+h*.145f);c.drawRect(r,p);
        text(c,String.format(Locale.US,"%03.0f  %4.0f  %3.0f",norm360(heading),altitude,speed),cx,top+h*.118f,16,0xff72ff9f,Paint.Align.CENTER,true);
        String[] keys={"COM1","IFF","NAV","AP","A-A","A-G"};
        for(int i=0;i<keys.length;i++){float bw=cw*.26f,bh=h*.050f,bx=x0+cw*.08f+(i%3)*cw*.29f,by=top+h*.175f+(i/3)*h*.064f;p.setColor(0xff27343a);r.set(bx,by,bx+bw,by+bh);c.drawRoundRect(r,5,5,p);text(c,keys[i],bx+bw*.5f,by+bh*.67f,9,0xffb7c5c8,Paint.Align.CENTER,true);}
    }

    private void drawStatusStrip(Canvas c,int w,int h){
        p.setColor(0xb8000000);c.drawRect(w*.31f,h*.615f,w*.69f,h*.662f,p);
        int col=0xff6cff9d;String msg="AUTO DEMO";
        if(gear<.75f&&altitude<180&&!onGround){col=0xffffb84d;msg="GEAR UP";}
        if(vertical<-18&&altitude<220){col=0xffff6b55;msg="PULL UP";}
        text(c,msg,w*.50f,h*.646f,12,col,Paint.Align.CENTER,true);
        text(c,weather,w*.50f,h*.674f,9,0xcca6b6b9,Paint.Align.CENTER,false);
    }

    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextAlign(align);p.setTextSize(size*getResources().getDisplayMetrics().scaledDensity);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);
    }
    private static float norm360(float v){v%=360;if(v<0)v+=360;return v;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
