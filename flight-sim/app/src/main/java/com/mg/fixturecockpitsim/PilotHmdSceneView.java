package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.*;
import android.view.View;

import java.util.Locale;

/** Immersive pilot helmet/HMD view with sky, distant terrain and minimal flight symbology. */
public final class PilotHmdSceneView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float roll, pitch, yaw, throttle = .68f, rttMs;
    private boolean connected;
    private long startMs = System.currentTimeMillis();

    public PilotHmdSceneView(Context c){
        super(c);
        p.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setData(float r,float pi,float y,float thr,boolean link,float rtt){
        roll=r; pitch=pi; yaw=y; throttle=thr; connected=link; rttMs=rtt;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        int w=getWidth(), h=getHeight();
        long now=System.currentTimeMillis();
        float cx=w*.5f;
        float horizon=h*.48f + pitch*5.1f;

        drawWorld(c,w,h,horizon,now);
        drawVisor(c,w,h);
        drawHmd(c,w,h,cx,horizon);
        postInvalidateDelayed(16);
    }

    private void drawWorld(Canvas c,int w,int h,float horizon,long now){
        p.setShader(new LinearGradient(0,0,0,Math.max(h*.62f,horizon+120),
                new int[]{Color.rgb(8,34,72),Color.rgb(42,112,177),Color.rgb(125,190,226),Color.rgb(226,226,205)},
                null, Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p); p.setShader(null);

        // sun glow
        p.setShader(new RadialGradient(w*.77f,h*.17f,Math.min(w,h)*.21f,
                new int[]{0xD8FFF5BF,0x66FFF1B0,0x00FFFFFF},null,Shader.TileMode.CLAMP));
        c.drawCircle(w*.77f,h*.17f,Math.min(w,h)*.22f,p); p.setShader(null);

        c.save();
        c.rotate(-roll*.48f,w*.5f,h*.5f);

        // distant haze layer
        p.setColor(0x65D8E1D7);
        c.drawRect(-w,horizon-6,w*2,horizon+22,p);

        float drift=(now-startMs)*.018f;
        drawMountainLayer(c,w,h,horizon+38,drift*.35f,0xFF71818B,105,30);
        drawMountainLayer(c,w,h,horizon+72,drift*.58f,0xFF52636A,84,44);
        drawMountainLayer(c,w,h,horizon+118,drift*.82f,0xFF394B48,62,52);

        // foreground terrain gradient
        p.setShader(new LinearGradient(0,horizon+70,0,h,
                new int[]{Color.rgb(76,91,72),Color.rgb(42,58,42),Color.rgb(18,28,24)},
                null,Shader.TileMode.CLAMP));
        Path g=new Path(); g.moveTo(-w,horizon+120); g.lineTo(w*2,horizon+120); g.lineTo(w*2,h*2); g.lineTo(-w,h*2); g.close(); c.drawPath(g,p); p.setShader(null);

        // subtle perspective lines for speed perception
        p.setColor(0x24EDE5CC); p.setStrokeWidth(2);
        for(int i=1;i<8;i++){
            float y=horizon+120+(h-horizon-120)*(i*i)/64f;
            c.drawLine(-w,y,w*2,y,p);
        }
        c.restore();

        // cloud wisps in screen space
        for(int i=0;i<6;i++){
            float x=((now*.011f)+i*213f)%(w+360)-180;
            float y=h*(.14f+.055f*(i%4));
            p.setShader(new RadialGradient(x,y,120, new int[]{0x4DFFFFFF,0x18FFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP));
            c.drawOval(x-130,y-32,x+130,y+32,p); p.setShader(null);
        }
    }

    private void drawMountainLayer(Canvas c,int w,int h,float base,float shift,int color,float wavelength,float amp){
        p.setColor(color);
        Path m=new Path(); m.moveTo(-w,base);
        for(int x=-w;x<=w*2;x+=34){
            float y=base
                    +(float)Math.sin((x+shift)/wavelength)*amp
                    +(float)Math.sin((x-shift*.55f)/(wavelength*.47f))*amp*.34f;
            m.lineTo(x,y);
        }
        m.lineTo(w*2,h*2); m.lineTo(-w,h*2); m.close(); c.drawPath(m,p);
    }

    private void drawVisor(Canvas c,int w,int h){
        // helmet/visor edge vignette
        p.setShader(new RadialGradient(w*.5f,h*.45f,Math.max(w,h)*.70f,
                new int[]{0x00000000,0x08000000,0xB9000000},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p); p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(10,w*.015f)); p.setColor(0x802A3033);
        RectF visor=new RectF(w*.045f,h*.035f,w*.955f,h*.91f);
        c.drawArc(visor,194,152,false,p);
        p.setStrokeWidth(2.5f); p.setColor(0x557CA0A8); c.drawArc(visor,196,148,false,p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawHmd(Canvas c,int w,int h,float cx,float horizon){
        int green=Color.rgb(116,255,154);
        p.setColor(green); p.setStrokeWidth(2.5f); p.setTextSize(Math.max(15,w*.020f));

        // heading tape
        p.setTextAlign(Paint.Align.CENTER);
        float hdg=(yaw+360f)%360f;
        for(int d=-40;d<=40;d+=10){
            float x=cx+d*(w/100f);
            c.drawLine(x,h*.075f,x,h*.095f,p);
            c.drawText(String.format(Locale.US,"%03.0f",(hdg+d+360)%360),x,h*.12f,p);
        }
        Path carat=new Path(); carat.moveTo(cx-8,h*.068f); carat.lineTo(cx,h*.085f); carat.lineTo(cx+8,h*.068f); c.drawPath(carat,p);

        // pitch ladder rotates with bank
        c.save(); c.rotate(-roll,cx,h*.44f);
        for(int deg=-30;deg<=30;deg+=5){
            float y=h*.44f-(deg-pitch)*5.1f;
            if(y<h*.16f||y>h*.72f) continue;
            float len=(deg%10==0)?w*.075f:w*.048f;
            c.drawLine(cx-len,y,cx-18,y,p); c.drawLine(cx+18,y,cx+len,y,p);
            if(deg%10==0 && deg!=0){
                p.setTextSize(Math.max(12,w*.016f)); c.drawText(Integer.toString(Math.abs(deg)),cx-len-20,y+5,p); c.drawText(Integer.toString(Math.abs(deg)),cx+len+20,y+5,p);
            }
        }
        c.restore();

        // flight path marker / aiming reticle
        float fy=h*.44f; p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
        c.drawCircle(cx,fy,26,p); c.drawLine(cx-58,fy,cx-27,fy,p); c.drawLine(cx+27,fy,cx+58,fy,p); c.drawLine(cx,fy-45,cx,fy-27,p);
        c.drawCircle(cx,fy,4,p); p.setStyle(Paint.Style.FILL);

        // left speed block
        p.setTextAlign(Paint.Align.LEFT); p.setTextSize(Math.max(16,w*.021f));
        float spd=310+throttle*890; float mach=spd/660f;
        c.drawText(String.format(Locale.US,"%4.0f KT",spd),w*.055f,h*.30f,p);
        c.drawText(String.format(Locale.US,"M %.2f",mach),w*.055f,h*.34f,p);
        c.drawText(String.format(Locale.US,"AOA %+3.0f",pitch*.22f),w*.055f,h*.38f,p);

        // right altitude / vertical information
        p.setTextAlign(Paint.Align.RIGHT);
        float alt=6400+pitch*48;
        c.drawText(String.format(Locale.US,"%5.0f FT",alt),w*.945f,h*.30f,p);
        c.drawText(String.format(Locale.US,"V/S %+4.0f",pitch*58),w*.945f,h*.34f,p);
        c.drawText(String.format(Locale.US,"THR %3.0f%%",throttle*100),w*.945f,h*.38f,p);

        // bottom HMD status strip
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(Math.max(13,w*.017f));
        p.setColor(connected?Color.rgb(105,255,145):Color.rgb(255,195,80));
        c.drawText(connected?String.format(Locale.US,"DATALINK LIVE  RTT %.0f ms",rttMs):"DATALINK STANDBY",cx,h*.84f,p);
        p.setColor(green); c.drawText("HMD • NAV • FCS NORMAL",cx,h*.88f,p);
        p.setTextAlign(Paint.Align.LEFT);
    }
}
