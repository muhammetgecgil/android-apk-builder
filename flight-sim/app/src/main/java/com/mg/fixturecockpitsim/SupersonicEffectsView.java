package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Lightweight Mach-dependent screen-space effects layered over the 3D aircraft. */
public final class SupersonicEffectsView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private volatile float mach,buffet,shock,boom,roll,pitch;
    private float phase;
    private long lastNs;

    public SupersonicEffectsView(Context c){super(c);setWillNotDraw(false);}

    public void setState(double m,double b,double s,double bp,double r,double pt){
        mach=(float)m;buffet=cl((float)b,0,1);shock=cl((float)s,0,1);boom=cl((float)bp,0,1);roll=(float)r;pitch=(float)pt;invalidate();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime();float dt=lastNs==0?.016f:Math.min(.05f,(now-lastNs)/1_000_000_000f);lastNs=now;phase+=dt;
        float w=getWidth(),h=getHeight(),cx=w*.50f,cy=h*.47f;

        // Transonic condensation / moving shock halo. Peak intensity is driven by
        // the same buffet value used by the flight model, so it appears only near M1.
        if(buffet>.01f){
            float wob=(float)Math.sin(phase*17f)*3.5f*buffet;
            float base=Math.min(w,h)*(.12f+.018f*(float)Math.sin(phase*8f));
            for(int i=0;i<3;i++){
                float rr=base*(1f+i*.22f)+wob;
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1.5f,5.2f-i*1.2f)*buffet);
                p.setColor(Color.argb((int)(95*buffet/(i+1)),235,245,255));
                c.drawOval(cx-rr*1.38f,cy-rr*.52f,cx+rr*1.38f,cy+rr*.52f,p);
            }
        }

        // Screen-space Mach cone. The included angle narrows naturally as Mach rises.
        if(mach>1.001f&&shock>.01f){
            double mu=Math.asin(1.0/Math.max(1.001,mach));
            float spread=(float)Math.tan(mu)*Math.min(w,h)*.28f;
            float len=Math.min(w,h)*(.34f+.12f*shock);
            float x0=cx,y0=cy+Math.min(w,h)*.045f;
            float rot=cl(roll/85f,-1,1)*spread*.13f;
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.0f+2.8f*shock);p.setColor(Color.argb((int)(105*shock),205,235,255));
            path.reset();path.moveTo(x0,y0);path.lineTo(x0-spread+rot,y0+len);path.moveTo(x0,y0);path.lineTo(x0+spread+rot,y0+len);c.drawPath(path,p);
            p.setStrokeWidth(1.2f);p.setColor(Color.argb((int)(70*shock),255,255,255));
            path.reset();path.moveTo(x0,y0+8);path.lineTo(x0-spread*.73f+rot,y0+len*.76f);path.moveTo(x0,y0+8);path.lineTo(x0+spread*.73f+rot,y0+len*.76f);c.drawPath(path,p);
        }

        // Crossing pulse: brief N-wave inspired white ring/flash, not a permanent glow.
        if(boom>.01f){
            float e=1f-boom;
            float rr=Math.min(w,h)*(.12f+.52f*e);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7f*boom+1.5f);p.setColor(Color.argb((int)(180*boom),255,255,255));
            c.drawCircle(cx,cy,rr,p);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.argb((int)(28*boom),245,250,255));c.drawRect(0,0,w,h,p);
        }

        if(buffet>.01f||shock>.01f||boom>.01f)postInvalidateOnAnimation();
    }

    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
