package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

/** V8.3: each sound sample is anchored exactly at the detected circular probe centre. */
public final class HeatmapOverlayView extends View {
    public interface TargetListener{void onTarget(float x01,float y01);}
    private static final int COLS=60,ROWS=96,N=COLS*ROWS;
    private final float[] value=new float[N],weight=new float[N],finalHeat=new float[N];
    private final boolean[] anchor=new boolean[N];
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG),line=new Paint(Paint.ANTI_ALIAS_FLAG),text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TargetListener listener; private float targetX=.5f,targetY=.72f,targetR=.08f,conf=0f,lastDelta=0f; private boolean valid=false,finalMode=false;
    private int samples=0,observed=0,best=-1; private float bestValue=0; private long lastNewCellMs=System.currentTimeMillis();
    public HeatmapOverlayView(Context c){super(c);setWillNotDraw(false);setClickable(true);line.setStyle(Paint.Style.STROKE);text.setTextSize(dp(11));text.setFakeBoldText(true);}
    public void setTargetListener(TargetListener l){listener=l;}
    public synchronized float getCoverage(){return observed/(float)N;} public synchronized int getSamples(){return samples;} public synchronized long getLastNewCellMs(){return lastNewCellMs;}
    public synchronized void setTracker(float x,float y,float radius,float confidence,boolean v){targetX=clamp(x);targetY=clamp(y);targetR=Math.max(.035f,Math.min(.22f,radius));conf=clamp(confidence);valid=v;postInvalidateOnAnimation();}
    public synchronized void clearMap(){for(int i=0;i<N;i++){value[i]=0;weight[i]=0;finalHeat[i]=0;anchor[i]=false;}samples=observed=0;best=-1;bestValue=0;finalMode=false;lastNewCellMs=System.currentTimeMillis();invalidate();}
    public synchronized void updateProbe(float x01,float y01,float energy01,float deltaDb,boolean active){
        if(!active||finalMode)return;int cx=Math.max(0,Math.min(COLS-1,(int)(clamp(x01)*COLS))),cy=Math.max(0,Math.min(ROWS-1,(int)(clamp(y01)*ROWS)));int i=cy*COLS+cx;
        float level=clamp(.90f*dbToLevel(deltaDb)+.10f*clamp(energy01));
        if(weight[i]<.05f){observed++;lastNewCellMs=System.currentTimeMillis();}
        float ow=weight[i],add=.8f+.2f*conf;value[i]=ow<=0?level:(value[i]*ow+level*add)/(ow+add);weight[i]=Math.min(40f,ow+add);anchor[i]=true;samples++;lastDelta=deltaDb;recomputeBest();postInvalidateOnAnimation();
    }
    public synchronized void finishScan(){if(samples<2)return;finalMode=true;for(int i=0;i<N;i++)finalHeat[i]=anchor[i]?value[i]:.06f;float[] tmp=new float[N];for(int pass=0;pass<14;pass++){System.arraycopy(finalHeat,0,tmp,0,N);for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){int i=y*COLS+x;if(anchor[i])continue;float s=finalHeat[i]*3f,w=3f;if(x>0){s+=finalHeat[i-1];w++;}if(x<COLS-1){s+=finalHeat[i+1];w++;}if(y>0){s+=finalHeat[i-COLS];w++;}if(y<ROWS-1){s+=finalHeat[i+COLS];w++;}tmp[i]=s/w;}System.arraycopy(tmp,0,finalHeat,0,N);}recomputeBest();invalidate();}
    public synchronized void resumeScan(){finalMode=false;recomputeBest();invalidate();}
    private void recomputeBest(){best=-1;bestValue=-1;for(int i=0;i<N;i++){if(!finalMode&&weight[i]<.05f)continue;if(insideProbe(i))continue;float v=finalMode?finalHeat[i]:value[i];if(v>bestValue){bestValue=v;best=i;}}}
    private boolean insideProbe(int i){if(finalMode||!valid)return false;float x=((i%COLS)+.5f)/COLS,y=((i/COLS)+.5f)/ROWS,dx=x-targetX,dy=y-targetY;float r=Math.max(.045f,targetR*1.12f);return dx*dx+dy*dy<r*r;}
    @Override protected synchronized void onDraw(Canvas c){super.onDraw(c);float cw=getWidth()/(float)COLS,ch=getHeight()/(float)ROWS;for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){int i=y*COLS+x;if(!finalMode&&weight[i]<.05f)continue;float v=finalMode?finalHeat[i]:value[i];fill.setColor(color(v,finalMode?165:185));c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,fill);}if(!finalMode)drawProbe(c);if(best>=0&&bestValue>.67f)drawSource(c,cw,ch);}
    private void drawProbe(Canvas c){float x=targetX*getWidth(),y=targetY*getHeight(),r=targetR*Math.min(getWidth(),getHeight());line.setStrokeWidth(dp(2.6f));line.setColor(valid?Color.CYAN:Color.WHITE);c.drawCircle(x,y,r,line);c.drawCircle(x,y,dp(4),line);c.drawLine(x-dp(14),y,x+dp(14),y,line);c.drawLine(x,y-dp(14),x,y+dp(14),line);text.setColor(line.getColor());String s=String.format(Locale.US,"PROB MERKEZ • Δ %+.1f dB",lastDelta);c.drawText(s,Math.min(getWidth()-dp(155),x+dp(16)),Math.max(dp(45),y-dp(10)),text);}
    private void drawSource(Canvas c,float cw,float ch){int bx=best%COLS,by=best/COLS;RectF r=new RectF(Math.max(0,(bx-2)*cw),Math.max(0,(by-2)*ch),Math.min(getWidth(),(bx+3)*cw),Math.min(getHeight(),(by+3)*ch));line.setStrokeWidth(dp(3));line.setColor(bestValue>.86f?Color.RED:Color.YELLOW);c.drawRoundRect(r,dp(8),dp(8),line);}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){targetX=clamp(e.getX()/Math.max(1f,getWidth()));targetY=clamp(e.getY()/Math.max(1f,getHeight()));valid=true;conf=.85f;if(listener!=null)listener.onTarget(targetX,targetY);invalidate();return true;}return true;}
    private static float dbToLevel(float db){if(db<=0)return .06f;if(db<3)return .06f+db/3f*.10f;if(db<6)return .16f+(db-3)/3f*.17f;if(db<9)return .33f+(db-6)/3f*.17f;if(db<12)return .50f+(db-9)/3f*.16f;if(db<15)return .66f+(db-12)/3f*.14f;if(db<18)return .80f+(db-15)/3f*.10f;return Math.min(.98f,.90f+(db-18)/12f*.08f);}
    private int color(float v,int a){v=clamp(v);if(v<.16f)return Color.argb(a,5,35,230);if(v<.33f)return Color.argb(a,0,170,255);if(v<.50f)return Color.argb(a,0,235,120);if(v<.66f)return Color.argb(a,220,245,0);if(v<.80f)return Color.argb(a,255,180,0);if(v<.90f)return Color.argb(a,255,90,0);return Color.argb(a,255,0,0);}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
