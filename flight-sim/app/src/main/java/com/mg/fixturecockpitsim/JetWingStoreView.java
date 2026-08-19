package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.FrameLayout;

/** V9 visual wrapper: keeps the high-detail 3D fighter and adds refined wing-edge cues
 *  plus symmetric under-wing game-store silhouettes. Purely visual simulator content. */
public final class JetWingStoreView extends FrameLayout {
    private final Jet3DViewV7 jet;
    private final StoresOverlay overlay;

    public JetWingStoreView(Context c){
        super(c);
        jet=new Jet3DViewV7(c);
        overlay=new StoresOverlay(c);
        addView(jet,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        addView(overlay,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        setClickable(true);
    }

    public void setTelemetry(float roll,float pitch,float yaw,float thr,float hz,int drops,boolean live){
        jet.setTelemetry(roll,pitch,yaw,thr,hz,drops,live);
        overlay.setData(roll,pitch,thr,live);
    }

    @Override public boolean onTouchEvent(MotionEvent e){ return jet.onTouchEvent(e); }

    private static final class StoresOverlay extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float roll,pitch,thr; private boolean live;
        StoresOverlay(Context c){ super(c); setWillNotDraw(false); }
        void setData(float r,float pi,float t,boolean l){roll=r;pitch=pi;thr=t;live=l;postInvalidateOnAnimation();}
        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight(); float cx=w*.5f,cy=h*.57f+pitch*.30f;
            c.save(); c.rotate(roll*.84f,cx,cy);
            float span=Math.min(w,h)*.42f;
            // subtle leading/trailing-edge highlights that visually sharpen the wing planform
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,w*.0024f));p.setColor(0x669EB0BB);
            Path le=new Path();le.moveTo(cx-span*.12f,cy-span*.22f);le.lineTo(cx-span*.88f,cy-span*.02f);le.moveTo(cx+span*.12f,cy-span*.22f);le.lineTo(cx+span*.88f,cy-span*.02f);c.drawPath(le,p);
            p.setColor(0x445C6870);Path te=new Path();te.moveTo(cx-span*.16f,cy+span*.10f);te.lineTo(cx-span*.62f,cy+span*.24f);te.moveTo(cx+span*.16f,cy+span*.10f);te.lineTo(cx+span*.62f,cy+span*.24f);c.drawPath(te,p);
            p.setStyle(Paint.Style.FILL);
            drawStore(c,cx-span*.48f,cy+span*.16f,span*.28f,span*.042f);
            drawStore(c,cx+span*.48f,cy+span*.16f,span*.28f,span*.042f);
            drawStore(c,cx-span*.67f,cy+span*.10f,span*.24f,span*.036f);
            drawStore(c,cx+span*.67f,cy+span*.10f,span*.24f,span*.036f);
            c.restore();
            if(live&&thr>.83f){p.setColor(0x55FFB24A);c.drawCircle(w*.50f,h*.79f,Math.min(w,h)*.035f,p);}
        }
        private void drawStore(Canvas c,float x,float y,float len,float rad){
            p.setColor(Color.rgb(184,190,194));RectF body=new RectF(x-rad,y-len*.38f,x+rad,y+len*.38f);c.drawRoundRect(body,rad,rad,p);
            Path nose=new Path();nose.moveTo(x-rad,y-len*.38f);nose.lineTo(x,y-len*.64f);nose.lineTo(x+rad,y-len*.38f);nose.close();c.drawPath(nose,p);
            p.setColor(Color.rgb(150,157,162));Path fins=new Path();fins.moveTo(x-rad,y+len*.20f);fins.lineTo(x-rad*2.2f,y+len*.38f);fins.lineTo(x-rad,y+len*.34f);fins.moveTo(x+rad,y+len*.20f);fins.lineTo(x+rad*2.2f,y+len*.38f);fins.lineTo(x+rad,y+len*.34f);c.drawPath(fins,p);
            p.setColor(0x66505A60);c.drawRect(x-rad*.35f,y-len*.20f,x+rad*.35f,y+len*.22f,p);
        }
    }
}
