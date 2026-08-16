package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

/** V8.6: rainbow scale + exact probe anchors + IDW final fill. */
public final class HeatmapOverlayView extends View {
    public interface TargetListener{void onTarget(float x01,float y01);}
    private static final int COLS=60,ROWS=96,N=COLS*ROWS;
    private final float[] value=new float[N],weight=new float[N],finalHeat=new float[N],confidence=new float[N];
    private final boolean[] anchor=new boolean[N];
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG),line=new Paint(Paint.ANTI_ALIAS_FLAG),text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TargetListener listener;
    private float targetX=.5f,targetY=.72f,targetR=.08f,conf=0f,lastDelta=0f;
    private float parkX=.14f,parkY=.70f,parkR=.11f;
    private boolean valid=false,parked=false,finalMode=false,scanActive=false,frozen=false;
    private int samples=0,observed=0,best=-1; private float bestValue=0;
    private long lastNewCellMs=System.currentTimeMillis();

    public HeatmapOverlayView(Context c){super(c);setWillNotDraw(false);setClickable(true);line.setStyle(Paint.Style.STROKE);text.setTextSize(dp(11));text.setFakeBoldText(true);}
    public void setTargetListener(TargetListener l){listener=l;}
    public synchronized void setPark(float x,float y,float r){parkX=clamp(x);parkY=clamp(y);parkR=Math.max(.05f,Math.min(.20f,r));invalidate();}
    public synchronized void setParked(boolean p){parked=p;postInvalidateOnAnimation();}
    public synchronized float getCoverage(){return observed/(float)N;} public synchronized int getSamples(){return samples;} public synchronized long getLastNewCellMs(){return lastNewCellMs;}
    public synchronized void setTracker(float x,float y,float radius,float confidenceIn,boolean v){setTracker(x,y,radius,confidenceIn,v,!v);}
    public synchronized void setTracker(float x,float y,float radius,float confidenceIn,boolean v,boolean frozenIn){targetX=clamp(x);targetY=clamp(y);targetR=Math.max(.035f,Math.min(.22f,radius));conf=clamp(confidenceIn);valid=v;frozen=frozenIn;postInvalidateOnAnimation();}
    public synchronized void setIdle(){scanActive=false;finalMode=false;invalidate();}
    public synchronized void beginScan(){scanActive=true;finalMode=false;invalidate();}

    public synchronized void clearMap(){for(int i=0;i<N;i++){value[i]=0;weight[i]=0;finalHeat[i]=0;confidence[i]=0;anchor[i]=false;}samples=observed=0;best=-1;bestValue=0;lastDelta=0;finalMode=false;scanActive=false;lastNewCellMs=System.currentTimeMillis();invalidate();}

    public synchronized void updateProbe(float x01,float y01,float energy01,float deltaDb,boolean active){
        if(!active||finalMode||!scanActive||!valid||frozen||conf<.42f)return;
        int cx=Math.max(0,Math.min(COLS-1,(int)(clamp(x01)*COLS))),cy=Math.max(0,Math.min(ROWS-1,(int)(clamp(y01)*ROWS)));int i=cy*COLS+cx;
        float level=clamp(.94f*dbToLevel(deltaDb)+.06f*clamp(energy01));
        if(weight[i]<.05f){observed++;lastNewCellMs=System.currentTimeMillis();}
        float ow=weight[i],add=.75f+.25f*conf;
        value[i]=ow<=0?level:(value[i]*ow+level*add)/(ow+add);weight[i]=Math.min(40f,ow+add);anchor[i]=true;confidence[i]=Math.max(confidence[i],conf);samples++;lastDelta=deltaDb;postInvalidateOnAnimation();
    }

    public synchronized void finishScan(){
        scanActive=false;if(samples<2){finalMode=false;invalidate();return;}finalMode=true;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
            int i=y*COLS+x;if(anchor[i]){finalHeat[i]=value[i];confidence[i]=1f;continue;}
            double sum=0,ws=0;float nearest=999f;
            for(int yy=0;yy<ROWS;yy++)for(int xx=0;xx<COLS;xx++){
                int j=yy*COLS+xx;if(!anchor[j])continue;float dx=x-xx,dy=y-yy,d2=dx*dx+dy*dy;if(d2>1225f)continue;
                float d=(float)Math.sqrt(d2);nearest=Math.min(nearest,d);double iw=1.0/Math.max(1.0,d2);iw*=.55+.45*confidence[j];sum+=value[j]*iw;ws+=iw;
            }
            if(ws>0){finalHeat[i]=clamp((float)(sum/ws));confidence[i]=clamp(1f-nearest/35f);}else{finalHeat[i]=0f;confidence[i]=0f;}
        }
        float[] tmp=finalHeat.clone();
        for(int y=1;y<ROWS-1;y++)for(int x=1;x<COLS-1;x++){int i=y*COLS+x;if(anchor[i])continue;tmp[i]=clamp((finalHeat[i]*2f+finalHeat[i-1]+finalHeat[i+1]+finalHeat[i-COLS]+finalHeat[i+COLS])/6f);}
        System.arraycopy(tmp,0,finalHeat,0,N);recomputeBest();invalidate();
    }

    private void recomputeBest(){best=-1;bestValue=-1;for(int i=0;i<N;i++){if(confidence[i]<.20f)continue;float v=finalHeat[i];if(v>bestValue){bestValue=v;best=i;}}}

    @Override protected synchronized void onDraw(Canvas c){
        super.onDraw(c);float cw=getWidth()/(float)COLS,ch=getHeight()/(float)ROWS;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
            int i=y*COLS+x;if(!finalMode&&weight[i]<.05f)continue;if(finalMode&&confidence[i]<=0f)continue;
            float v=finalMode?finalHeat[i]:value[i];int alpha=finalMode?(anchor[i]?190:Math.max(70,Math.min(165,(int)(65+100*confidence[i])))):190;
            fill.setColor(rainbow(v,alpha));c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,fill);
        }
        if(!scanActive&&!finalMode)drawPark(c);if(!finalMode)drawProbe(c);if(finalMode&&best>=0&&bestValue>.58f)drawSource(c,cw,ch);drawState(c);drawLegend(c);
    }

    private void drawPark(Canvas c){float x=parkX*getWidth(),y=parkY*getHeight(),r=parkR*Math.min(getWidth(),getHeight());line.setStrokeWidth(dp(3));line.setColor(parked?Color.GREEN:Color.YELLOW);c.drawCircle(x,y,r,line);c.drawCircle(x,y,dp(5),line);text.setColor(line.getColor());c.drawText(parked?"PROB HAZIR":"BAŞLANGIÇ • PROBU BURAYA GETİR",Math.max(dp(8),x-r),Math.max(dp(20),y-r-dp(8)),text);}
    private void drawProbe(Canvas c){float x=targetX*getWidth(),y=targetY*getHeight(),r=targetR*Math.min(getWidth(),getHeight());line.setStrokeWidth(dp(2.6f));line.setColor(valid?Color.CYAN:(frozen?Color.MAGENTA:Color.WHITE));c.drawCircle(x,y,r,line);c.drawCircle(x,y,dp(4),line);c.drawLine(x-dp(14),y,x+dp(14),y,line);c.drawLine(x,y-dp(14),x,y+dp(14),line);text.setColor(line.getColor());String s=frozen?"PROB KAYIP • MERKEZ DONDURULDU":scanActive?String.format(Locale.US,"PROB MERKEZ • Δ %+.1f dB",lastDelta):"PROB MERKEZ";c.drawText(s,Math.min(getWidth()-dp(205),x+dp(16)),Math.max(dp(45),y-dp(10)),text);}
    private void drawSource(Canvas c,float cw,float ch){int bx=best%COLS,by=best/COLS;RectF r=new RectF(Math.max(0,(bx-2)*cw),Math.max(0,(by-2)*ch),Math.min(getWidth(),(bx+3)*cw),Math.min(getHeight(),(by+3)*ch));line.setStrokeWidth(dp(bestValue>.82f?4:3));line.setColor(bestValue>.82f?Color.RED:Color.YELLOW);c.drawRoundRect(r,dp(8),dp(8),line);String s=(bestValue>.82f?"KAYNAK BULUNDU":"MUHTEMEL KAYNAK")+" • %"+Math.round(bestValue*100);fill.setColor(Color.argb(210,0,0,0));c.drawRoundRect(r.left,Math.max(0,r.top-dp(26)),Math.min(getWidth(),r.left+dp(190)),r.top,dp(6),dp(6),fill);text.setColor(line.getColor());c.drawText(s,r.left+dp(5),Math.max(dp(15),r.top-dp(7)),text);}
    private void drawState(Canvas c){String s=finalMode?"NİHAİ HARİTA • RAINBOW + IDW":scanActive?String.format(Locale.US,"TARAMA AKTİF • %d örnek",samples):parked?"HAZIR • TARAMAYI BAŞLAT":"BEKLEME • PROB SOL ALTTA OLMALI";fill.setColor(Color.argb(175,0,0,0));c.drawRoundRect(dp(8),dp(8),Math.min(getWidth()-dp(8),dp(300)),dp(35),dp(7),dp(7),fill);text.setColor(Color.WHITE);c.drawText(s,dp(14),dp(27),text);}
    private void drawLegend(Canvas c){int left=(int)dp(10),right=Math.min(getWidth()-(int)dp(10),left+(int)dp(220)),top=getHeight()-(int)dp(16),h=(int)dp(7);for(int x=left;x<right;x++){float v=(x-left)/(float)Math.max(1,right-left-1);fill.setColor(rainbow(v,220));c.drawRect(x,top,x+1,top+h,fill);}}

    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){targetX=clamp(e.getX()/Math.max(1f,getWidth()));targetY=clamp(e.getY()/Math.max(1f,getHeight()));valid=true;frozen=false;conf=.90f;if(listener!=null)listener.onTarget(targetX,targetY);invalidate();return true;}return true;}

    /** -2 dB starts violet/blue; +12 dB reaches red. This makes small real differences visible. */
    private static float dbToLevel(float db){return clamp((db+2f)/14f);}
    private static int rainbow(float v,int alpha){
        v=clamp(v);
        // violet (275°) -> blue -> cyan -> green -> yellow -> orange -> red (0°)
        float hue=275f*(1f-v);
        return Color.HSVToColor(Math.max(35,Math.min(230,alpha)),new float[]{hue,.96f,1f});
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
