package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Locale;

/** V15 aircraft display: embedded internet-sourced F-22 mesh + mountain world + pilot HMD. */
public final class AircraftDisplayView extends LinearLayout {
    private final JetWingStoreView jet;
    private final HelmetPanel panel;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final long demoStart=System.currentTimeMillis();
    private volatile long lastLiveMs;
    private volatile float roll,pitch,yaw,throttle=.72f,linkHz;
    private volatile int drops;
    private volatile boolean demoEnabled=true;
    private float dr,dp,dy,dt=.72f;

    public AircraftDisplayView(Context c){
        super(c);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER);setBackgroundColor(Color.rgb(2,7,12));
        jet=new JetWingStoreView(c);panel=new HelmetPanel(c);
        addView(jet,new LayoutParams(0,LayoutParams.MATCH_PARENT,.88f));
        addView(panel,new LayoutParams(0,LayoutParams.MATCH_PARENT,.12f));
        ui.post(tick);
    }
    public void setTelemetry(float r,float p,float y,float t,float hz,int d){roll=r;pitch=p;yaw=y;throttle=t;linkHz=hz;drops=d;lastLiveMs=System.currentTimeMillis();}
    public void setDemoEnabled(boolean e){demoEnabled=e;}
    public boolean isDemoActive(){return demoEnabled&&System.currentTimeMillis()-lastLiveMs>1200;}

    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!isAttachedToWindow())return;long now=System.currentTimeMillis();boolean live=now-lastLiveMs<=1200;
        if(!live&&demoEnabled){
            float t=(now-demoStart)/1000f;
            float rr=28f*(float)Math.sin(t*.38f)+7f*(float)Math.sin(t*.87f);
            float pp=8f*(float)Math.sin(t*.26f)+2.6f*(float)Math.cos(t*.61f);
            float yy=(t*11f)%360f;
            float tt=.76f+.12f*(float)Math.sin(t*.14f);
            dr+=angle(rr-dr)*.024f;dp+=(pp-dp)*.024f;dy+=angle(yy-dy)*.018f;dt+=(tt-dt)*.020f;
            roll=dr;pitch=dp;yaw=dy;throttle=dt;linkHz=50;drops=0;
        }else if(live){dr=roll;dp=pitch;dy=yaw;dt=throttle;}
        jet.setTelemetry(roll,pitch,yaw,throttle,linkHz,drops,live||demoEnabled);
        panel.setData(roll,pitch,yaw,throttle,linkHz,drops,live,!live&&demoEnabled);
        ui.postDelayed(this,16);
    }};
    private static float angle(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}
    @Override protected void onDetachedFromWindow(){ui.removeCallbacks(tick);super.onDetachedFromWindow();}

    private static final class HelmetPanel extends View{
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);float roll,pitch,yaw,thr,hz;int drops;boolean live,demo;
        HelmetPanel(Context c){super(c);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setData(float r,float pi,float y,float t,float h,int d,boolean l,boolean dm){roll=r;pitch=pi;yaw=y;thr=t;hz=h;drops=d;live=l;demo=dm;postInvalidateOnAnimation();}
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,0,h,new int[]{Color.rgb(17,37,43),Color.rgb(3,10,14)},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setShader(new RadialGradient(w*.52f,h*.45f,Math.max(w,h)*.72f,new int[]{0x0018FFD0,0x2210A080,0xD9000000},new float[]{0,.58f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(4,w*.018f));p.setColor(0xAA1B2427);RectF visor=new RectF(w*.04f,h*.035f,w*.96f,h*.95f);c.drawRoundRect(visor,w*.34f,h*.22f,p);p.setStyle(Paint.Style.FILL);
            int green=Color.rgb(105,255,160);p.setColor(green);p.setStrokeWidth(2.4f);p.setTextSize(Math.max(11,w*.047f));p.setTextAlign(Paint.Align.CENTER);
            c.drawText("PILOT HMD",w/2f,h*.055f,p);c.drawText(String.format(Locale.US,"%03.0f",(yaw+360)%360),w/2f,h*.115f,p);
            float cx=w/2f,cy=h*.39f;p.setStyle(Paint.Style.STROKE);c.drawCircle(cx,cy,w*.115f,p);c.drawCircle(cx,cy,w*.025f,p);c.drawLine(cx-w*.18f,cy,cx-w*.05f,cy,p);c.drawLine(cx+w*.05f,cy,cx+w*.18f,cy,p);c.drawLine(cx,cy-h*.09f,cx,cy-h*.035f,p);c.drawLine(cx,cy+h*.035f,cx,cy+h*.09f,p);
            c.save();c.rotate(-roll*.55f,cx,cy);for(int deg=-30;deg<=30;deg+=10){if(deg==0)continue;float yy=cy+(pitch-deg)*h*.0062f;float len=(deg%20==0?w*.15f:w*.10f);c.drawLine(cx-len,yy,cx-w*.025f,yy,p);c.drawLine(cx+w*.025f,yy,cx+len,yy,p);}c.restore();p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);p.setColor(live?Color.rgb(75,255,120):Color.rgb(255,190,70));p.setTextSize(Math.max(11,w*.045f));c.drawText(live?"IMU LIVE":(demo?"DEMO FLIGHT":"LINK STBY"),w/2f,h*.82f,p);
            p.setColor(0x88FFFFFF);p.setTextSize(Math.max(9,w*.035f));c.drawText("3D ekrana dokun: kamera",w/2f,h*.90f,p);
        }
    }
}
