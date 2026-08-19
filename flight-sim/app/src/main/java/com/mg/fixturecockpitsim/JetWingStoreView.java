package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.FrameLayout;

/** V11 visual wrapper: poster-inspired wide-body fighter over moving mountain world. */
public final class JetWingStoreView extends FrameLayout {
    private final MountainWorldView world;
    private final Jet3DViewV11 jet;
    private final StoresOverlay overlay;

    public JetWingStoreView(Context c){
        super(c);
        setBackgroundColor(Color.rgb(5,20,42));
        world=new MountainWorldView(c);
        jet=new Jet3DViewV11(c);
        overlay=new StoresOverlay(c);
        addView(world,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        addView(jet,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        addView(overlay,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        setClickable(true);
    }

    public void setTelemetry(float roll,float pitch,float yaw,float thr,float hz,int drops,boolean live){
        world.setData(roll,pitch,yaw,thr);
        jet.setTelemetry(roll,pitch,yaw,thr,hz,drops,live);
        overlay.setData(roll,pitch,thr,live);
    }

    @Override public boolean onTouchEvent(MotionEvent e){ return jet.onTouchEvent(e); }

    private static final class MountainWorldView extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float roll,pitch,yaw,thr=.7f;
        private final long start=System.currentTimeMillis();
        MountainWorldView(Context c){super(c);setWillNotDraw(false);}
        void setData(float r,float pi,float y,float t){roll=r;pitch=pi;yaw=y;thr=t;postInvalidateOnAnimation();}
        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight();
            float horizon=h*.49f+pitch*3.1f;
            p.setShader(new LinearGradient(0,0,0,h*.72f,new int[]{Color.rgb(7,34,76),Color.rgb(41,116,183),Color.rgb(139,198,229),Color.rgb(226,222,199)},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setShader(new RadialGradient(w*.78f,h*.17f,Math.min(w,h)*.19f,new int[]{0xD8FFF3BD,0x55FFF0A8,0x00FFFFFF},null,Shader.TileMode.CLAMP));
            c.drawCircle(w*.78f,h*.17f,Math.min(w,h)*.20f,p);p.setShader(null);
            float t=(System.currentTimeMillis()-start)/1000f;
            float speed=28f+thr*90f;
            float heading=(float)Math.toRadians(yaw);
            float drift=t*speed*(.45f+.55f*Math.abs((float)Math.cos(heading)));
            c.save();c.rotate(-roll*.32f,w*.5f,h*.52f);
            p.setColor(0x55E6E4D6);c.drawRect(-w,horizon-8,w*2,horizon+18,p);
            layer(c,w,h,horizon+46,drift*.28f+yaw*1.8f,0xFF8896A0,125,31);
            layer(c,w,h,horizon+86,drift*.48f+yaw*2.5f,0xFF66737A,96,44);
            layer(c,w,h,horizon+132,drift*.75f+yaw*3.4f,0xFF46534F,70,58);
            p.setShader(new LinearGradient(0,horizon+110,0,h,new int[]{Color.rgb(81,92,67),Color.rgb(46,59,41),Color.rgb(20,29,24)},null,Shader.TileMode.CLAMP));
            c.drawRect(-w,horizon+118,w*2,h*2,p);p.setShader(null);
            p.setColor(0x22F0E7C5);p.setStrokeWidth(2);
            for(int i=1;i<=7;i++){float q=i/7f;float y=horizon+118+(h-horizon-118)*q*q;c.drawLine(-w,y,w*2,y,p);}c.restore();
            for(int i=0;i<6;i++){
                float x=((t*(18+thr*22))+i*221f)%(w+360)-180;
                float y=h*(.13f+.052f*(i%4));
                p.setShader(new RadialGradient(x,y,115,new int[]{0x48FFFFFF,0x16FFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP));
                c.drawOval(x-125,y-28,x+125,y+28,p);p.setShader(null);
            }
            postInvalidateDelayed(16);
        }
        private void layer(Canvas c,int w,int h,float base,float shift,int color,float wave,float amp){
            p.setColor(color);Path path=new Path();path.moveTo(-w,base);
            for(int x=-w;x<=w*2;x+=28){float y=base+(float)Math.sin((x+shift)/wave)*amp+(float)Math.sin((x-shift*.37f)/(wave*.43f))*amp*.38f;path.lineTo(x,y);}path.lineTo(w*2,h*2);path.lineTo(-w,h*2);path.close();c.drawPath(path,p);
        }
    }

    private static final class StoresOverlay extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float roll,pitch,thr; private boolean live;
        StoresOverlay(Context c){super(c);setWillNotDraw(false);}
        void setData(float r,float pi,float t,boolean l){roll=r;pitch=pi;thr=t;live=l;postInvalidateOnAnimation();}
        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight();float cx=w*.5f,cy=h*.57f+pitch*.30f;
            c.save();c.rotate(roll*.84f,cx,cy);
            float span=Math.min(w,h)*.42f;
            p.setStyle(Paint.Style.FILL);
            drawStore(c,cx-span*.50f,cy+span*.18f,span*.31f,span*.045f);
            drawStore(c,cx+span*.50f,cy+span*.18f,span*.31f,span*.045f);
            drawStore(c,cx-span*.69f,cy+span*.12f,span*.27f,span*.039f);
            drawStore(c,cx+span*.69f,cy+span*.12f,span*.27f,span*.039f);
            c.restore();
            if(live&&thr>.83f){p.setColor(0x55FFB24A);c.drawCircle(w*.50f,h*.79f,Math.min(w,h)*.035f,p);}
        }
        private void drawStore(Canvas c,float x,float y,float len,float rad){
            p.setColor(Color.rgb(193,198,202));RectF body=new RectF(x-rad,y-len*.38f,x+rad,y+len*.38f);c.drawRoundRect(body,rad,rad,p);
            Path nose=new Path();nose.moveTo(x-rad,y-len*.38f);nose.lineTo(x,y-len*.66f);nose.lineTo(x+rad,y-len*.38f);nose.close();c.drawPath(nose,p);
            p.setColor(Color.rgb(154,160,165));Path fins=new Path();fins.moveTo(x-rad,y+len*.18f);fins.lineTo(x-rad*2.3f,y+len*.38f);fins.lineTo(x-rad,y+len*.34f);fins.moveTo(x+rad,y+len*.18f);fins.lineTo(x+rad*2.3f,y+len*.38f);fins.lineTo(x+rad,y+len*.34f);c.drawPath(fins,p);
            p.setColor(0x66505A60);c.drawRect(x-rad*.34f,y-len*.18f,x+rad*.34f,y+len*.24f,p);
        }
    }
}
