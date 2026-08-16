package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

/** V8.7: dynamic min-max rainbow map. Raw measured dB is preserved; colour is assigned only relative to scan min/max. */
public final class HeatmapOverlayView extends View {
    public interface TargetListener{void onTarget(float x01,float y01);}
    private static final int COLS=60,ROWS=96,N=COLS*ROWS;
    private final float[] raw=new float[N],weight=new float[N],finalRaw=new float[N],confidence=new float[N];
    private final boolean[] anchor=new boolean[N];
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG),line=new Paint(Paint.ANTI_ALIAS_FLAG),text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TargetListener listener; private float targetX=.5f,targetY=.72f,targetR=.08f,conf=0f,lastDb=0f;
    private float parkX=.14f,parkY=.70f,parkR=.11f; private boolean valid=false,parked=false,finalMode=false,scanActive=false,frozen=false;
    private int samples=0,observed=0,best=-1; private float scanMin=Float.POSITIVE_INFINITY,scanMax=Float.NEGATIVE_INFINITY;

    public HeatmapOverlayView(Context c){super(c);setWillNotDraw(false);setClickable(true);line.setStyle(Paint.Style.STROKE);text.setTextSize(dp(11));text.setFakeBoldText(true);}
    public void setTargetListener(TargetListener l){listener=l;}
    public synchronized void setPark(float x,float y,float r){parkX=clamp(x);parkY=clamp(y);parkR=Math.max(.05f,Math.min(.20f,r));invalidate();}
    public synchronized void setParked(boolean p){parked=p;postInvalidateOnAnimation();}
    public synchronized void setTracker(float x,float y,float radius,float c,boolean v,boolean f){targetX=clamp(x);targetY=clamp(y);targetR=Math.max(.035f,Math.min(.22f,radius));conf=clamp(c);valid=v;frozen=f;postInvalidateOnAnimation();}
    public synchronized void setIdle(){scanActive=false;finalMode=false;invalidate();}
    public synchronized void beginScan(){scanActive=true;finalMode=false;invalidate();}
    public synchronized void clearMap(){for(int i=0;i<N;i++){raw[i]=0;weight[i]=0;finalRaw[i]=0;confidence[i]=0;anchor[i]=false;}samples=observed=0;best=-1;scanMin=Float.POSITIVE_INFINITY;scanMax=Float.NEGATIVE_INFINITY;finalMode=false;scanActive=false;invalidate();}

    public synchronized void updateProbe(float x01,float y01,float measuredDb,boolean active){
        if(!active||finalMode||!scanActive||!valid||frozen||conf<.42f||Float.isNaN(measuredDb)||measuredDb<-119f)return;
        int cx=Math.max(0,Math.min(COLS-1,(int)(clamp(x01)*COLS))),cy=Math.max(0,Math.min(ROWS-1,(int)(clamp(y01)*ROWS))),i=cy*COLS+cx;
        if(weight[i]<.05f)observed++;
        float ow=weight[i],add=.75f+.25f*conf;raw[i]=ow<=0?measuredDb:(raw[i]*ow+measuredDb*add)/(ow+add);weight[i]=Math.min(40f,ow+add);anchor[i]=true;confidence[i]=Math.max(confidence[i],conf);samples++;lastDb=measuredDb;
        recomputeRange(false);postInvalidateOnAnimation();
    }

    public synchronized void finishScan(){
        scanActive=false;if(samples<2){finalMode=false;invalidate();return;}finalMode=true;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
            int i=y*COLS+x;if(anchor[i]){finalRaw[i]=raw[i];confidence[i]=1f;continue;}
            double sum=0,ws=0;float nearest=999f;
            for(int yy=0;yy<ROWS;yy++)for(int xx=0;xx<COLS;xx++){int j=yy*COLS+xx;if(!anchor[j])continue;float dx=x-xx,dy=y-yy,d2=dx*dx+dy*dy;if(d2>1225f)continue;float d=(float)Math.sqrt(d2);nearest=Math.min(nearest,d);double iw=1.0/Math.max(1.0,d2);sum+=raw[j]*iw;ws+=iw;}
            if(ws>0){finalRaw[i]=(float)(sum/ws);confidence[i]=clamp(1f-nearest/35f);}else{finalRaw[i]=0;confidence[i]=0;}
        }
        recomputeRange(true);best=-1;float bv=-999f;for(int i=0;i<N;i++){if(confidence[i]<.20f)continue;if(finalRaw[i]>bv){bv=finalRaw[i];best=i;}}invalidate();
    }

    private void recomputeRange(boolean fin){float mn=Float.POSITIVE_INFINITY,mx=Float.NEGATIVE_INFINITY;for(int i=0;i<N;i++){if(fin){if(confidence[i]<=0f)continue;mn=Math.min(mn,finalRaw[i]);mx=Math.max(mx,finalRaw[i]);}else if(anchor[i]){mn=Math.min(mn,raw[i]);mx=Math.max(mx,raw[i]);}}if(mn<Float.POSITIVE_INFINITY){scanMin=mn;scanMax=mx;}}
    private float norm(float db){float span=scanMax-scanMin;if(!(span>0.01f))return .5f;float margin=Math.max(.15f,span*.03f);return clamp((db-(scanMin-margin))/(span+2*margin));}

    @Override protected synchronized void onDraw(Canvas c){super.onDraw(c);float cw=getWidth()/(float)COLS,ch=getHeight()/(float)ROWS;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){int i=y*COLS+x;if(!finalMode&&!anchor[i])continue;if(finalMode&&confidence[i]<=0f)continue;float db=finalMode?finalRaw[i]:raw[i],v=norm(db);int a=finalMode?(anchor[i]?190:Math.max(65,Math.min(160,(int)(60+100*confidence[i])))):190;fill.setColor(rainbow(v,a));c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,fill);}
        if(!scanActive&&!finalMode)drawPark(c);if(!finalMode)drawProbe(c);if(finalMode&&best>=0)drawSource(c,cw,ch);drawState(c);drawLegend(c);
    }
    private void drawPark(Canvas c){float x=parkX*getWidth(),y=parkY*getHeight(),r=parkR*Math.min(getWidth(),getHeight());line.setStrokeWidth(dp(3));line.setColor(parked?Color.GREEN:Color.YELLOW);c.drawCircle(x,y,r,line);text.setColor(line.getColor());c.drawText(parked?"PROB HAZIR":"BAŞLANGIÇ • PROBU BURAYA GETİR",Math.max(dp(8),x-r),Math.max(dp(20),y-r-dp(8)),text);}
    private void drawProbe(Canvas c){float x=targetX*getWidth(),y=targetY*getHeight(),r=targetR*Math.min(getWidth(),getHeight());line.setStrokeWidth(dp(2.6f));line.setColor(valid?Color.CYAN:(frozen?Color.MAGENTA:Color.WHITE));c.drawCircle(x,y,r,line);c.drawCircle(x,y,dp(4),line);text.setColor(line.getColor());String s=frozen?"PROB KAYIP • MERKEZ DONDU":scanActive?String.format(Locale.US,"MERKEZ • %.1f dB",lastDb):"PROB MERKEZ";c.drawText(s,Math.min(getWidth()-dp(195),x+dp(16)),Math.max(dp(45),y-dp(10)),text);}
    private void drawSource(Canvas c,float cw,float ch){int bx=best%COLS,by=best/COLS;RectF r=new RectF(Math.max(0,(bx-2)*cw),Math.max(0,(by-2)*ch),Math.min(getWidth(),(bx+3)*cw),Math.min(getHeight(),(by+3)*ch));line.setStrokeWidth(dp(4));line.setColor(Color.RED);c.drawRoundRect(r,dp(8),dp(8),line);}
    private void drawState(Canvas c){String range=(scanMin<Float.POSITIVE_INFINITY)?String.format(Locale.US," %.1f..%.1f dB",scanMin,scanMax):"";String s=finalMode?"NİHAİ • DİNAMİK MIN-MAX"+range:scanActive?"TARAMA • "+samples+" örnek"+range:parked?"HAZIR • BAŞLAT":"PROB SOL ALTTA OLMALI";fill.setColor(Color.argb(175,0,0,0));c.drawRoundRect(dp(8),dp(8),Math.min(getWidth()-dp(8),dp(320)),dp(35),dp(7),dp(7),fill);text.setColor(Color.WHITE);c.drawText(s,dp(14),dp(27),text);}
    private void drawLegend(Canvas c){int left=(int)dp(10),right=Math.min(getWidth()-(int)dp(10),left+(int)dp(230)),top=getHeight()-(int)dp(16),h=(int)dp(7);for(int x=left;x<right;x++){float v=(x-left)/(float)Math.max(1,right-left-1);fill.setColor(rainbow(v,220));c.drawRect(x,top,x+1,top+h,fill);}}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){targetX=clamp(e.getX()/Math.max(1f,getWidth()));targetY=clamp(e.getY()/Math.max(1f,getHeight()));valid=true;frozen=false;conf=.90f;if(listener!=null)listener.onTarget(targetX,targetY);invalidate();return true;}return true;}
    private static int rainbow(float v,int alpha){v=clamp(v);float hue=275f*(1f-v);return Color.HSVToColor(Math.max(35,Math.min(230,alpha)),new float[]{hue,.96f,1f});}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
