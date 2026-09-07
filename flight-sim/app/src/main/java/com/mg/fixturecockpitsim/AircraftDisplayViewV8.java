package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.Locale;

/** V8 aircraft display: V7 3D jet + moving tactical world + arcade/simulation target and missile effects. */
public final class AircraftDisplayViewV8 extends LinearLayout {
    private final Jet3DViewV7 jet;
    private final CombatOverlay overlay;
    private final WorldHelmetPanel panel;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final long start=System.currentTimeMillis();
    private volatile long lastLiveMs;
    private volatile float roll,pitch,yaw,throttle=.72f,linkHz;
    private volatile int drops;
    private volatile boolean demoEnabled=true;
    private float dr,dp,dy,dt=.72f, worldX,worldZ;
    private int targetIndex;

    public AircraftDisplayViewV8(Context c){
        super(c); setOrientation(HORIZONTAL); setBackgroundColor(Color.rgb(2,7,12));
        FrameLayout scene=new FrameLayout(c);
        jet=new Jet3DViewV7(c); overlay=new CombatOverlay(c);
        scene.addView(jet,new FrameLayout.LayoutParams(-1,-1));
        scene.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        panel=new WorldHelmetPanel(c);
        addView(scene,new LayoutParams(0,LayoutParams.MATCH_PARENT,.76f));
        addView(panel,new LayoutParams(0,LayoutParams.MATCH_PARENT,.24f));
        ui.post(tick);
    }
    public void setTelemetry(float r,float p,float y,float t,float hz,int d){roll=r;pitch=p;yaw=y;throttle=t;linkHz=hz;drops=d;lastLiveMs=System.currentTimeMillis();}
    public void setDemoEnabled(boolean e){demoEnabled=e;}
    public boolean isDemoActive(){return demoEnabled&&System.currentTimeMillis()-lastLiveMs>1200;}
    public void selectNextTarget(){targetIndex=(targetIndex+1)%3;overlay.select(targetIndex);panel.select(targetIndex);}
    public boolean fireMissile(){return overlay.fire(targetIndex);}

    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!isAttachedToWindow())return; long now=System.currentTimeMillis(); boolean live=now-lastLiveMs<=1200;
        if(!live&&demoEnabled){float t=(now-start)/1000f;float rr=23f*(float)Math.sin(t*.39f)+5f*(float)Math.sin(t*.87f);float pp=7f*(float)Math.sin(t*.25f)+2f*(float)Math.cos(t*.58f);float yy=(t*9.5f)%360f;float tt=.74f+.10f*(float)Math.sin(t*.15f);dr+=ang(rr-dr)*.026f;dp+=(pp-dp)*.026f;dy+=ang(yy-dy)*.018f;dt+=(tt-dt)*.02f;roll=dr;pitch=dp;yaw=dy;throttle=dt;linkHz=50;drops=0;}else if(live){dr=roll;dp=pitch;dy=yaw;dt=throttle;}
        float speed=300+throttle*900; float sec=.016f; float hd=(float)Math.toRadians(yaw);
        worldX+=(float)Math.sin(hd)*speed*sec*.0028f; worldZ+=(float)Math.cos(hd)*speed*sec*.0028f;
        jet.setTelemetry(roll,pitch,yaw,throttle,linkHz,drops,live||demoEnabled);
        overlay.setData(roll,pitch,yaw,throttle,worldX,worldZ,live,!live&&demoEnabled);
        panel.setData(roll,pitch,yaw,throttle,worldX,worldZ,live,!live&&demoEnabled);
        ui.postDelayed(this,16);
    }};
    private static float ang(float d){while(d>180)d-=360;while(d<-180)d+=360;return d;}
    @Override protected void onDetachedFromWindow(){ui.removeCallbacks(tick);super.onDetachedFromWindow();}

    private static final class CombatOverlay extends View{
        final Paint p=new Paint(3);float roll,pitch,yaw,thr,x,z;boolean live,demo;int sel;long missileStart;boolean missile;
        final float[][] targets={{-1.7f,4.6f},{2.2f,6.4f},{.7f,8.3f}};
        CombatOverlay(Context c){super(c);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));}
        void setData(float r,float pi,float y,float t,float wx,float wz,boolean l,boolean d){roll=r;pitch=pi;yaw=y;thr=t;x=wx;z=wz;live=l;demo=d;postInvalidateOnAnimation();}
        void select(int i){sel=i;invalidate();}
        boolean fire(int i){if(missile)return false;sel=i;missile=true;missileStart=System.currentTimeMillis();invalidate();return true;}
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();long now=System.currentTimeMillis();int green=Color.rgb(104,255,146);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xBB68FF92);
            // moving tactical map
            float ml=18,mt=h*.70f,mw=w*.25f,mh=h*.25f;c.drawRoundRect(ml,mt,ml+mw,mt+mh,14,14,p);p.setColor(0x665EDB88);for(int i=1;i<5;i++){c.drawLine(ml+i*mw/5,mt,ml+i*mw/5,mt+mh,p);c.drawLine(ml,mt+i*mh/5,ml+mw,mt+i*mh/5,p);}p.setStyle(Paint.Style.FILL);p.setColor(green);c.drawCircle(ml+mw/2,mt+mh/2,5,p);
            for(int i=0;i<3;i++){float tx=targets[i][0],tz=targets[i][1];float rx=(tx-x)*13,rz=(tz-z)*13;float px=ml+mw/2+rx,py=mt+mh/2-rz;if(px>ml&&px<ml+mw&&py>mt&&py<mt+mh){p.setColor(i==sel?Color.YELLOW:0xFF7CFF9C);c.drawCircle(px,py,i==sel?6:4,p);}}
            // screen-space target cues tied loosely to world/yaw
            for(int i=0;i<3;i++){float phase=(i*113f + (yaw*2.1f));float sx=w*.50f+(float)Math.sin(Math.toRadians(phase))*w*.27f;float sy=h*.39f+(float)Math.cos(Math.toRadians(phase*.63f))*h*.16f+pitch*2.0f;float s=i==sel?34:22;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(i==sel?3:1.5f);p.setColor(i==sel?Color.YELLOW:0xAA80FF9F);c.drawRect(sx-s,sy-s,sx+s,sy+s,p);if(i==sel){c.drawLine(sx-s-22,sy,sx-s,sy,p);c.drawLine(sx+s,sy,sx+s+22,sy,p);}p.setStyle(Paint.Style.FILL);p.setTextSize(13);c.drawText("TGT-"+(i+1),sx-s,sy-s-6,p);}
            // missile visual simulation
            if(missile){float q=(now-missileStart)/2200f;if(q>=1f){missile=false;p.setColor(Color.rgb(255,205,70));p.setTextSize(18);c.drawText("SIM HIT",w*.46f,h*.18f,p);}else{float t=q*q*(3-2*q);float ex=w*.50f+(float)Math.sin(Math.toRadians(sel*113f+yaw*2.1f))*w*.27f;float ey=h*.39f+(float)Math.cos(Math.toRadians((sel*113f+yaw*2.1f)*.63f))*h*.16f+pitch*2;float sx=w*.50f,sy=h*.76f;float mx=sx+(ex-sx)*t,my=sy+(ey-sy)*t;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(0xCCFFB33A);c.drawLine(sx,sy,mx,my,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);c.drawCircle(mx,my,5,p);postInvalidateDelayed(16);}}
            p.setTextSize(14);p.setColor(green);c.drawText(live?"BT IMU LIVE":(demo?"DEMO WORLD":"LINK STBY"),w*.02f,h*.05f,p);c.drawText("TARGET "+(sel+1)+"  •  GAME SIM",w*.36f,h*.95f,p);
        }
    }

    private static final class WorldHelmetPanel extends View{
        final Paint p=new Paint(3);float roll,pitch,yaw,thr,x,z;boolean live,demo;int sel;
        WorldHelmetPanel(Context c){super(c);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setData(float r,float pi,float y,float t,float wx,float wz,boolean l,boolean d){roll=r;pitch=pi;yaw=y;thr=t;x=wx;z=wz;live=l;demo=d;postInvalidateOnAnimation();}
        void select(int i){sel=i;invalidate();}
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();float horizon=h*.47f+pitch*3.5f;
            p.setShader(new LinearGradient(0,0,0,horizon+60,new int[]{Color.rgb(8,30,66),Color.rgb(53,126,188),Color.rgb(174,208,225)},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            c.save();c.rotate(-roll*.35f,w/2f,h/2f);for(int layer=0;layer<3;layer++){float base=horizon+35+layer*34;Path m=new Path();m.moveTo(-w,base);for(int px=-w;px<w*2;px+=20){float yy=base+(float)Math.sin((px+x*35+layer*51)/38.0)*(18+layer*8)+(float)Math.sin((px-z*22)/17.0)*6;m.lineTo(px,yy);}m.lineTo(w*2,h*2);m.lineTo(-w,h*2);m.close();p.setColor(layer==0?0xFF6D7F85:layer==1?0xFF4B625F:0xFF30443C);c.drawPath(m,p);}c.restore();
            p.setShader(new RadialGradient(w*.76f,h*.14f,w*.28f,new int[]{0x99FFF3C2,0x00FFFFFF},null,Shader.TileMode.CLAMP));c.drawCircle(w*.76f,h*.14f,w*.28f,p);p.setShader(null);
            int g=Color.rgb(108,255,151);p.setColor(g);p.setStrokeWidth(2);p.setTextSize(Math.max(10,w*.048f));p.setTextAlign(Paint.Align.CENTER);c.drawText(String.format(Locale.US,"HDG %03.0f",(yaw+360)%360),w/2f,h*.07f,p);float cx=w/2f,cy=h*.42f;p.setStyle(Paint.Style.STROKE);c.drawCircle(cx,cy,w*.10f,p);c.drawLine(cx-w*.18f,cy,cx-w*.04f,cy,p);c.drawLine(cx+w*.04f,cy,cx+w*.18f,cy,p);p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.LEFT);c.drawText(String.format(Locale.US,"SPD %4.0f",300+thr*900),w*.06f,h*.24f,p);c.drawText(String.format(Locale.US,"MAP %.1f %.1f",x,z),w*.06f,h*.29f,p);c.drawText("TGT "+(sel+1),w*.06f,h*.34f,p);p.setTextAlign(Paint.Align.RIGHT);c.drawText(String.format(Locale.US,"ALT %5.0f",6200+pitch*42),w*.94f,h*.24f,p);c.drawText(String.format(Locale.US,"THR %3.0f%%",thr*100),w*.94f,h*.29f,p);p.setTextAlign(Paint.Align.CENTER);p.setColor(live?Color.rgb(80,255,120):Color.rgb(255,190,70));c.drawText(live?"IMU LIVE":(demo?"DEMO WORLD":"STBY"),w/2f,h*.90f,p);
        }
    }
}
