package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Locale;

/** Aircraft-side composite screen: real 3D jet + cockpit instruments.
 *  If no Bluetooth telemetry arrives, a built-in demo flight drives both views.
 */
public final class AircraftDisplayView extends LinearLayout {
    private final Jet3DView jet;
    private final CockpitPanel panel;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final long demoStart = System.currentTimeMillis();
    private volatile long lastLiveMs;
    private volatile float roll, pitch, yaw, throttle = .72f, linkHz;
    private volatile int drops;
    private volatile boolean demoEnabled = true;

    public AircraftDisplayView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.rgb(2,8,13));
        jet = new Jet3DView(context);
        panel = new CockpitPanel(context);
        addView(jet, new LayoutParams(0, LayoutParams.MATCH_PARENT, .72f));
        addView(panel, new LayoutParams(0, LayoutParams.MATCH_PARENT, .28f));
        ui.post(tick);
    }

    public void setTelemetry(float r,float p,float y,float t,float hz,int d) {
        roll=r; pitch=p; yaw=y; throttle=t; linkHz=hz; drops=d;
        lastLiveMs=System.currentTimeMillis();
    }

    public void setDemoEnabled(boolean enabled){ demoEnabled=enabled; }
    public boolean isDemoActive(){ return demoEnabled && System.currentTimeMillis()-lastLiveMs>1200; }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!isAttachedToWindow()) return;
            boolean live = System.currentTimeMillis()-lastLiveMs <= 1200;
            if (!live && demoEnabled) {
                float t=(System.currentTimeMillis()-demoStart)/1000f;
                roll=30f*(float)Math.sin(t*.62f)+7f*(float)Math.sin(t*1.55f);
                pitch=9f*(float)Math.sin(t*.39f)+3f*(float)Math.cos(t*.83f);
                yaw=(t*16f)%360f;
                throttle=.72f+.16f*(float)Math.sin(t*.21f);
                linkHz=50f; drops=0;
            }
            jet.setTelemetry(roll,pitch,yaw,throttle,linkHz,drops,live || demoEnabled);
            panel.setData(roll,pitch,yaw,throttle,linkHz,drops,live,!live&&demoEnabled);
            ui.postDelayed(this,16);
        }
    };

    @Override protected void onDetachedFromWindow(){ ui.removeCallbacks(tick); super.onDetachedFromWindow(); }

    private static final class CockpitPanel extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float roll,pitch,yaw,thr,hz; private int drops; private boolean live,demo;
        CockpitPanel(Context c){ super(c); p.setTypeface(Typeface.create("monospace",Typeface.BOLD)); }
        void setData(float r,float pi,float y,float t,float h,int d,boolean l,boolean dm){roll=r;pitch=pi;yaw=y;thr=t;hz=h;drops=d;live=l;demo=dm;postInvalidateOnAnimation();}
        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,0,h,new int[]{Color.rgb(29,34,36),Color.rgb(6,9,10)},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setColor(Color.rgb(74,79,80));c.drawRect(0,0,4,h,p);c.drawRect(0,h*.09f,w,h*.105f,p);
            p.setColor(Color.rgb(115,255,155));p.setTextSize(Math.max(12,w*.055f));p.setTextAlign(Paint.Align.CENTER);c.drawText("F-22 CLASS COCKPIT",w/2f,h*.06f,p);
            drawMfd(c, w*.07f,h*.14f,w*.93f,h*.44f,"PRIMARY FLIGHT");
            drawMfd(c, w*.07f,h*.49f,w*.93f,h*.76f,"ENGINE / DATALINK");
            p.setTextAlign(Paint.Align.LEFT);p.setTextSize(Math.max(11,w*.048f));p.setColor(Color.rgb(108,255,148));
            c.drawText(String.format(Locale.US,"ROLL   %+05.1f°",roll),w*.13f,h*.205f,p);
            c.drawText(String.format(Locale.US,"PITCH  %+05.1f°",pitch),w*.13f,h*.255f,p);
            c.drawText(String.format(Locale.US,"HDG    %03.0f°",(yaw+360)%360),w*.13f,h*.305f,p);
            c.drawText(String.format(Locale.US,"SPD    %4.0f kt",300+thr*900),w*.13f,h*.355f,p);
            c.drawText(String.format(Locale.US,"ALT    %5.0f ft",6200+pitch*42),w*.13f,h*.405f,p);
            c.drawText(String.format(Locale.US,"THR    %3.0f %%",thr*100),w*.13f,h*.555f,p);
            c.drawText(String.format(Locale.US,"RPM    %3.0f %%",44+thr*56),w*.13f,h*.605f,p);
            c.drawText(String.format(Locale.US,"LINK   %.0f Hz",hz),w*.13f,h*.655f,p);
            c.drawText(String.format(Locale.US,"DROP   %d",drops),w*.13f,h*.705f,p);
            p.setColor(live?Color.rgb(75,255,115):Color.rgb(255,184,58));
            c.drawCircle(w*.16f,h*.84f,Math.max(6,w*.025f),p);p.setTextSize(Math.max(12,w*.052f));
            p.setTextAlign(Paint.Align.LEFT);c.drawText(live?"BLUETOOTH IMU LIVE":(demo?"DEMO FLIGHT ACTIVE":"LINK STANDBY"),w*.24f,h*.85f,p);
            p.setTextSize(Math.max(9,w*.038f));p.setColor(Color.LTGRAY);c.drawText("3D ekrana dokun: kamera değiştir",w*.10f,h*.93f,p);
        }
        private void drawMfd(Canvas c,float l,float t,float r,float b,String title){
            p.setColor(Color.rgb(7,15,14));c.drawRoundRect(l,t,r,b,12,12,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(90,255,135));c.drawRoundRect(l,t,r,b,12,12,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(10,getWidth()*.043f));c.drawText(title,(l+r)/2,t+Math.max(16,getHeight()*.028f),p);
        }
    }
}
