package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Camera overlay that accumulates roaming USB-probe measurements into a 32x24 map. */
public final class HeatmapOverlayView extends View {
    public interface TargetListener { void onTarget(float x01, float y01); }
    private static final int COLS=32, ROWS=24;
    private final float[] heat=new float[COLS*ROWS];
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TargetListener listener;
    private float targetX=.5f,targetY=.5f;
    private int best=-1;
    private float bestValue=0f,bestDelta=0f;

    public HeatmapOverlayView(Context c){
        super(c); setWillNotDraw(false);
        line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(dp(2)); line.setColor(Color.WHITE);
        text.setColor(Color.WHITE); text.setTextSize(dp(13)); text.setFakeBoldText(true);
    }

    public void setTargetListener(TargetListener l){listener=l;}
    public void clearMap(){for(int i=0;i<heat.length;i++)heat[i]=0f;best=-1;bestValue=0f;invalidate();}

    public synchronized void updateProbe(float x01,float y01,float energy01,float deltaDb,boolean active){
        if(!active)return;
        for(int i=0;i<heat.length;i++) heat[i]*=.997f;
        int cx=Math.max(0,Math.min(COLS-1,(int)(x01*COLS)));
        int cy=Math.max(0,Math.min(ROWS-1,(int)(y01*ROWS)));
        float boost=clamp01(.70f*energy01+.30f*clamp01((deltaDb+3f)/24f));
        for(int dy=-2;dy<=2;dy++) for(int dx=-2;dx<=2;dx++){
            int x=cx+dx,y=cy+dy;if(x<0||x>=COLS||y<0||y>=ROWS)continue;
            float w=(float)Math.exp(-(dx*dx+dy*dy)/3.2f);
            int idx=y*COLS+x; heat[idx]=Math.max(heat[idx],clamp01(boost*w));
        }
        best=0;bestValue=heat[0];for(int i=1;i<heat.length;i++)if(heat[i]>bestValue){bestValue=heat[i];best=i;}
        bestDelta=deltaDb; invalidate();
    }

    @Override protected synchronized void onDraw(Canvas c){
        super.onDraw(c); float cw=getWidth()/(float)COLS,ch=getHeight()/(float)ROWS;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
            float v=heat[y*COLS+x]; if(v<.06f)continue;
            fill.setColor(color(v)); fill.setStyle(Paint.Style.FILL);
            c.drawRect(x*cw,y*ch,(x+1)*cw,(y+1)*ch,fill);
        }
        float tx=targetX*getWidth(),ty=targetY*getHeight();
        line.setColor(Color.WHITE);line.setStrokeWidth(dp(2));
        c.drawLine(tx-dp(16),ty,tx+dp(16),ty,line);c.drawLine(tx,ty-dp(16),tx,ty+dp(16),line);
        c.drawCircle(tx,ty,dp(22),line);
        if(best>=0 && bestValue>=.58f){
            int bx=best%COLS,by=best/COLS;float l=bx*cw,t=by*ch,r=(bx+1)*cw,b=(by+1)*ch;
            float pad=Math.max(cw,ch)*1.8f;RectF box=new RectF(Math.max(0,l-pad),Math.max(0,t-pad),Math.min(getWidth(),r+pad),Math.min(getHeight(),b+pad));
            line.setColor(bestValue>=.78f?Color.RED:Color.YELLOW);line.setStrokeWidth(dp(bestValue>=.78f?4:3));c.drawRect(box,line);
            String label=bestValue>=.78f?"KAYNAK BULUNDU":"MUHTEMEL KAYNAK";
            String info=label+"  %"+(int)(bestValue*100)+"  Δ "+String.format(java.util.Locale.US,"%.1f dB",bestDelta);
            fill.setColor(Color.argb(210,0,0,0));fill.setStyle(Paint.Style.FILL);c.drawRect(box.left,Math.max(0,box.top-dp(28)),Math.min(getWidth(),box.left+dp(250)),box.top,fill);
            text.setColor(line.getColor());c.drawText(info,box.left+dp(5),box.top-dp(8),text);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){
            targetX=clamp01(e.getX()/Math.max(1f,getWidth()));targetY=clamp01(e.getY()/Math.max(1f,getHeight()));
            if(listener!=null)listener.onTarget(targetX,targetY);invalidate();return true;
        }return true;
    }

    private int color(float v){
        int a=(int)(55+190*clamp01(v));
        if(v<.20f)return Color.argb(a,0,40,255);
        if(v<.38f)return Color.argb(a,0,220,255);
        if(v<.55f)return Color.argb(a,0,255,70);
        if(v<.72f)return Color.argb(a,255,235,0);
        if(v<.86f)return Color.argb(a,255,120,0);
        return Color.argb(a,255,0,0);
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static float clamp01(float v){return Math.max(0f,Math.min(1f,v));}
}
