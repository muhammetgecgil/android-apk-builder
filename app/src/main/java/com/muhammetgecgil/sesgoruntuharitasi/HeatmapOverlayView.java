package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** V9.1 robust acoustic map + persistent tracker overlay. */
public final class HeatmapOverlayView extends View {
    public interface TargetListener{void onTarget(float x01,float y01);}
    private static final int COLS=60,ROWS=96,N=COLS*ROWS;
    private static final float MIN_MEANINGFUL_SPAN_DB=2.0f;
    private final float[] raw=new float[N],weight=new float[N],finalRaw=new float[N],confidence=new float[N];
    private final boolean[] anchor=new boolean[N];
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG),line=new Paint(Paint.ANTI_ALIAS_FLAG),text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TargetListener listener;
    private float targetX=.5f,targetY=.72f,targetR=.08f,conf=0f,lastDb=0f;
    private boolean valid=false,finalMode=false,scanActive=false,frozen=false,homogeneous=false,reacquiring=true;
    private int samples=0,observed=0;
    private float rangeLow=Float.POSITIVE_INFINITY,rangeHigh=Float.NEGATIVE_INFINITY;
    private float sourceX=-1f,sourceY=-1f,sourceDb=-120f,sourceConfidence=0f;

    public HeatmapOverlayView(Context c){super(c);setWillNotDraw(false);setClickable(true);line.setStyle(Paint.Style.STROKE);text.setTextSize(dp(11));text.setFakeBoldText(true);}
    public void setTargetListener(TargetListener l){listener=l;}
    public synchronized void setPark(float x,float y,float r){}
    public synchronized void setParked(boolean p){}
    public synchronized void setTracker(float x,float y,float radius,float c,boolean v,boolean f){setTracker(x,y,radius,c,v,f,!v);}
    public synchronized void setTracker(float x,float y,float radius,float c,boolean v,boolean f,boolean q){targetX=clamp(x);targetY=clamp(y);targetR=Math.max(.025f,Math.min(.24f,radius));conf=clamp(c);valid=v;frozen=f;reacquiring=q;postInvalidateOnAnimation();}
    public synchronized void setIdle(){scanActive=false;finalMode=false;invalidate();}
    public synchronized void beginScan(){scanActive=true;finalMode=false;homogeneous=false;invalidate();}
    public synchronized boolean isHomogeneous(){return homogeneous;}

    public synchronized void clearMap(){
        for(int i=0;i<N;i++){raw[i]=0;weight[i]=0;finalRaw[i]=0;confidence[i]=0;anchor[i]=false;}
        samples=observed=0;rangeLow=Float.POSITIVE_INFINITY;rangeHigh=Float.NEGATIVE_INFINITY;
        sourceX=sourceY=-1f;sourceDb=-120f;sourceConfidence=0f;homogeneous=false;finalMode=false;scanActive=false;invalidate();
    }

    public synchronized void updateProbe(float x01,float y01,float measuredDb,boolean active){
        if(!active||finalMode||!scanActive||!valid||frozen||conf<.42f||Float.isNaN(measuredDb)||measuredDb<-119f)return;
        int cx=Math.max(0,Math.min(COLS-1,(int)(clamp(x01)*COLS))),cy=Math.max(0,Math.min(ROWS-1,(int)(clamp(y01)*ROWS))),i=cy*COLS+cx;
        if(weight[i]<.05f)observed++;
        float ow=weight[i],add=.70f+.30f*conf;
        raw[i]=ow<=0?measuredDb:(raw[i]*ow+measuredDb*add)/(ow+add);
        weight[i]=Math.min(50f,ow+add);anchor[i]=true;confidence[i]=Math.max(confidence[i],conf);samples++;lastDb=measuredDb;
        recomputeRobustRange();postInvalidateOnAnimation();
    }

    public synchronized void finishScan(){
        scanActive=false;
        if(samples<2||observed<2){finalMode=false;invalidate();return;}
        finalMode=true;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
            int i=y*COLS+x;
            if(anchor[i]){finalRaw[i]=raw[i];confidence[i]=1f;continue;}
            double sum=0,ws=0;float nearest=999f;
            for(int yy=0;yy<ROWS;yy++)for(int xx=0;xx<COLS;xx++){
                int j=yy*COLS+xx;if(!anchor[j])continue;
                float dx=x-xx,dy=y-yy,d2=dx*dx+dy*dy;if(d2>1225f)continue;
                float d=(float)Math.sqrt(d2);nearest=Math.min(nearest,d);
                double iw=1.0/Math.max(1.0,d2);iw*=.6+.4*confidence[j];sum+=raw[j]*iw;ws+=iw;
            }
            if(ws>0){finalRaw[i]=(float)(sum/ws);confidence[i]=clamp(1f-nearest/35f);}else{finalRaw[i]=0;confidence[i]=0;}
        }
        recomputeRobustRange();computeSourceFromMeasuredAnchors();invalidate();
    }

    private void recomputeRobustRange(){
        ArrayList<Float> vals=new ArrayList<>();for(int i=0;i<N;i++)if(anchor[i])vals.add(raw[i]);
        if(vals.isEmpty()){rangeLow=Float.POSITIVE_INFINITY;rangeHigh=Float.NEGATIVE_INFINITY;homogeneous=false;return;}
        Collections.sort(vals);int n=vals.size();int i10=Math.max(0,Math.min(n-1,(int)Math.floor(.10*(n-1)))),i90=Math.max(0,Math.min(n-1,(int)Math.ceil(.90*(n-1))));
        rangeLow=vals.get(i10);rangeHigh=vals.get(i90);homogeneous=(rangeHigh-rangeLow)<MIN_MEANINGFUL_SPAN_DB;
    }

    private void computeSourceFromMeasuredAnchors(){
        ArrayList<Float> vals=new ArrayList<>();for(int i=0;i<N;i++)if(anchor[i])vals.add(raw[i]);
        if(vals.isEmpty()){sourceX=sourceY=-1;sourceConfidence=0;return;}
        Collections.sort(vals);int n=vals.size();float threshold=vals.get(Math.max(0,(int)Math.floor(.90*(n-1))));
        double sx=0,sy=0,sw=0;float maxDb=-999f;int topCount=0;
        for(int i=0;i<N;i++){
            if(!anchor[i]||raw[i]<threshold)continue;int x=i%COLS,y=i/COLS;float excess=Math.max(.15f,raw[i]-threshold+.15f);float ww=excess*(.5f+.5f*confidence[i]);
            sx+=(x+.5)*ww;sy+=(y+.5)*ww;sw+=ww;topCount++;if(raw[i]>maxDb)maxDb=raw[i];
        }
        if(sw<=0){sourceX=sourceY=-1;sourceConfidence=0;return;}
        sourceX=(float)(sx/sw);sourceY=(float)(sy/sw);sourceDb=maxDb;
        float span=Math.max(0f,rangeHigh-rangeLow),spanScore=clamp((span-1.2f)/5f),countScore=clamp(observed/25f),clusterScore=clamp(topCount/8f);
        sourceConfidence=clamp(.55f*spanScore+.30f*countScore+.15f*clusterScore);if(homogeneous)sourceConfidence=Math.min(sourceConfidence,.28f);
    }

    private float norm(float db){
        if(rangeLow==Float.POSITIVE_INFINITY||rangeHigh==Float.NEGATIVE_INFINITY)return .5f;
        float span=rangeHigh-rangeLow;if(span<MIN_MEANINGFUL_SPAN_DB){float center=(rangeLow+rangeHigh)*.5f;return clamp(.5f+(db-center)/MIN_MEANINGFUL_SPAN_DB*.18f);}
        return clamp((db-rangeLow)/Math.max(.001f,span));
    }

    @Override protected synchronized void onDraw(Canvas c){
        super.onDraw(c);float cw=getWidth()/(float)COLS,ch=getHeight()/(float)ROWS;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
            int i=y*COLS+x;if(!finalMode&&!anchor[i])continue;if(finalMode&&confidence[i]<=0f)continue;
            float db=finalMode?finalRaw[i]:raw[i],v=norm(db);int a=finalMode?(anchor[i]?195:Math.max(55,Math.min(155,(int)(55+100*confidence[i])))):190;
            fill.setColor(rainbow(v,a));c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,fill);
        }
        if(!finalMode)drawProbe(c);
        if(finalMode&&sourceX>=0&&sourceY>=0&&!homogeneous)drawSource(c,cw,ch);
        drawState(c);drawLegend(c);
    }

    private void drawProbe(Canvas c){
        float x=targetX*getWidth(),y=targetY*getHeight(),r=targetR*Math.min(getWidth(),getHeight());
        line.setStrokeWidth(dp(valid?3.2f:2.5f));line.setColor(valid?Color.CYAN:(frozen?Color.MAGENTA:Color.WHITE));
        c.drawCircle(x,y,r,line);c.drawLine(x-dp(6),y,x+dp(6),y,line);c.drawLine(x,y-dp(6),x,y+dp(6),line);
        text.setColor(line.getColor());String s=valid?String.format(Locale.US,"PROB MERKEZ • çap %.0f px • %d%%",2*r,Math.round(conf*100)):reacquiring?"PROB KAYIP • DAİRE DONDU • OTOMATİK ARANIYOR":"PROB ARANIYOR";
        c.drawText(s,Math.min(getWidth()-dp(270),Math.max(dp(8),x+dp(12))),Math.max(dp(48),y-r-dp(8)),text);
    }

    private void drawSource(Canvas c,float cw,float ch){
        float px=sourceX*cw,py=sourceY*ch,rx=Math.max(dp(22),cw*3f),ry=Math.max(dp(22),ch*3f);RectF r=new RectF(Math.max(0,px-rx),Math.max(0,py-ry),Math.min(getWidth(),px+rx),Math.min(getHeight(),py+ry));
        line.setStrokeWidth(dp(sourceConfidence>.65f?4:3));line.setColor(sourceConfidence>.55f?Color.RED:Color.YELLOW);c.drawRoundRect(r,dp(8),dp(8),line);
        fill.setColor(Color.argb(205,0,0,0));float top=Math.max(dp(2),r.top-dp(28));c.drawRoundRect(r.left,top,Math.min(getWidth(),r.left+dp(220)),r.top,dp(6),dp(6),fill);
        text.setColor(line.getColor());c.drawText(String.format(Locale.US,"KAYNAK ADAYI • güven %d • %.1f dB",Math.round(sourceConfidence*100),sourceDb),r.left+dp(5),Math.max(dp(16),r.top-dp(7)),text);
    }

    private void drawState(Canvas c){
        String range=(rangeLow<Float.POSITIVE_INFINITY)?String.format(Locale.US," %.1f..%.1f dB",rangeLow,rangeHigh):"";
        String s=finalMode?(homogeneous?"NİHAİ • ALAN HOMOJEN"+range:"NİHAİ • ROBUST P10-P90"+range):scanActive?"TARAMA • "+samples+" örnek"+range:valid?"PROB KİLİTLİ • TARAMAYI BAŞLAT":"AUTO BULUYOR • İSTERSEN PROBA BİR KEZ DOKUN";
        fill.setColor(Color.argb(175,0,0,0));c.drawRoundRect(dp(8),dp(8),Math.min(getWidth()-dp(8),dp(390)),dp(35),dp(7),dp(7),fill);text.setColor(Color.WHITE);c.drawText(s,dp(14),dp(27),text);
    }

    private void drawLegend(Canvas c){int left=(int)dp(10),right=Math.min(getWidth()-(int)dp(10),left+(int)dp(230)),top=getHeight()-(int)dp(16),h=(int)dp(7);for(int x=left;x<right;x++){float v=(x-left)/(float)Math.max(1,right-left-1);fill.setColor(rainbow(v,220));c.drawRect(x,top,x+1,top+h,fill);}}

    /** Manual override is deliberately one tap only; dragging no longer repeatedly destroys/reseeds the tracker. */
    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){targetX=clamp(e.getX()/Math.max(1f,getWidth()));targetY=clamp(e.getY()/Math.max(1f,getHeight()));valid=false;frozen=true;reacquiring=true;conf=.85f;if(listener!=null)listener.onTarget(targetX,targetY);invalidate();return true;}return true;
    }
    private static int rainbow(float v,int alpha){v=clamp(v);float hue=275f*(1f-v);return Color.HSVToColor(Math.max(35,Math.min(230,alpha)),new float[]{hue,.96f,1f});}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
