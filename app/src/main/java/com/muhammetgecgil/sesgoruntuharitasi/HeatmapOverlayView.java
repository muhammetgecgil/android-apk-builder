package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

/** V8.1: absolute dB color scale, full-screen final map, and live-probe source-box exclusion. */
public final class HeatmapOverlayView extends View {
    public interface TargetListener { void onTarget(float x01,float y01); }
    private static final int COLS=40,ROWS=64,N=COLS*ROWS;
    private final float[] value=new float[N];
    private final float[] weight=new float[N];
    private final float[] finalHeat=new float[N];
    private final boolean[] anchor=new boolean[N];
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TargetListener listener;
    private float targetX=.5f,targetY=.72f,trackerConfidence=0f;
    private boolean trackerValid=false,finalMode=false;
    private int best=-1,samples=0,observed=0;
    private float bestValue=0f,bestDelta=0f;
    private long lastNewCellMs=System.currentTimeMillis();

    public HeatmapOverlayView(Context c){
        super(c);setWillNotDraw(false);setClickable(true);
        line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(dp(2));
        text.setTextSize(dp(12));text.setFakeBoldText(true);
    }
    public void setTargetListener(TargetListener l){listener=l;}
    public synchronized boolean isFinalMode(){return finalMode;}
    public synchronized float getCoverage(){return observed/(float)N;}
    public synchronized long getLastNewCellMs(){return lastNewCellMs;}
    public synchronized int getSamples(){return samples;}

    public synchronized void setTracker(float x,float y,float confidence,boolean valid){
        targetX=clamp(x);targetY=clamp(y);trackerConfidence=clamp(confidence);trackerValid=valid;
        if(!finalMode)recomputeBest(bestDelta);
        postInvalidateOnAnimation();
    }

    public synchronized void clearMap(){
        for(int i=0;i<N;i++){value[i]=0;weight[i]=0;finalHeat[i]=0;anchor[i]=false;}
        best=-1;bestValue=0;bestDelta=0;samples=0;observed=0;finalMode=false;
        lastNewCellMs=System.currentTimeMillis();invalidate();
    }

    public synchronized void updateProbe(float x01,float y01,float energy01,float deltaDb,boolean active){
        if(!active||finalMode)return;
        int cx=Math.max(0,Math.min(COLS-1,(int)(clamp(x01)*COLS)));
        int cy=Math.max(0,Math.min(ROWS-1,(int)(clamp(y01)*ROWS)));

        // Primary scale is USB - phone-reference dB. It deliberately uses the whole blue->red palette.
        float dbLevel=dbToColorLevel(deltaDb);
        float level=clamp(.86f*dbLevel+.14f*clamp(energy01));
        boolean newCell=false;

        // Small brush: a high reading no longer floods a large part of the screen red.
        for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){
            int x=cx+dx,y=cy+dy;if(x<0||x>=COLS||y<0||y>=ROWS)continue;
            float d2=dx*dx+dy*dy;float g=(float)Math.exp(-d2/2.7f);if(g<.08f)continue;
            int i=y*COLS+x;float add=g*(.72f+.28f*trackerConfidence);
            if(weight[i]<.05f){observed++;newCell=true;}
            float oldW=weight[i];
            // Average repeated measurements instead of max-hold, preventing permanent red saturation.
            value[i]=oldW<=0?level:(value[i]*oldW+level*add)/(oldW+add);
            weight[i]=Math.min(30f,oldW+add);
            anchor[i]=true;
        }
        if(newCell)lastNewCellMs=System.currentTimeMillis();samples++;
        recomputeBest(deltaDb);postInvalidateOnAnimation();
    }

    /** Paints the whole image. Unmeasured regions remain blue and measured regions diffuse locally. */
    public synchronized void finishScan(){
        if(samples<3)return;
        finalMode=true;
        final float BLUE_BASE=.08f;
        for(int i=0;i<N;i++)finalHeat[i]=weight[i]>.03f?value[i]:BLUE_BASE;

        float[] tmp=new float[N];
        // Gentle spatial interpolation. Anchored measurements remain fixed, so red cannot spread everywhere.
        for(int pass=0;pass<11;pass++){
            System.arraycopy(finalHeat,0,tmp,0,N);
            for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
                int i=y*COLS+x;if(anchor[i])continue;
                float s=finalHeat[i]*2.2f,w=2.2f;
                if(x>0){s+=finalHeat[i-1];w++;}if(x<COLS-1){s+=finalHeat[i+1];w++;}
                if(y>0){s+=finalHeat[i-COLS];w++;}if(y<ROWS-1){s+=finalHeat[i+COLS];w++;}
                tmp[i]=clamp(s/w);
            }
            System.arraycopy(tmp,0,finalHeat,0,N);
        }
        recomputeBest(bestDelta);invalidate();
    }

    public synchronized void resumeScan(){finalMode=false;recomputeBest(bestDelta);invalidate();}

    /** Current Fyvadio body is never boxed as the source. Once probe moves away, the old hot cell may qualify. */
    private boolean insideLiveProbeMask(int i){
        if(finalMode||!trackerValid)return false;
        int x=i%COLS,y=i/COLS;
        float px=targetX*COLS,py=targetY*ROWS;
        float dx=x+.5f-px,dy=y+.5f-py;
        // Approx. visual microphone body exclusion zone around the tracked probe centre.
        return (dx*dx)/(4.2f*4.2f)+(dy*dy)/(6.0f*6.0f)<1f;
    }

    private void recomputeBest(float delta){
        best=-1;bestValue=-1;
        for(int i=0;i<N;i++){
            if(insideLiveProbeMask(i))continue;
            float v=finalMode?finalHeat[i]:value[i];
            if(!finalMode&&weight[i]<=.12f)continue;
            if(v>bestValue){bestValue=v;best=i;}
        }
        bestDelta=delta;
    }

    @Override protected synchronized void onDraw(Canvas c){
        super.onDraw(c);float cw=getWidth()/(float)COLS,ch=getHeight()/(float)ROWS;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
            int i=y*COLS+x;float v=finalMode?finalHeat[i]:value[i];
            if(!finalMode&&weight[i]<.035f)continue;
            fill.setStyle(Paint.Style.FILL);
            int alpha=finalMode?178:Math.min(185,42+(int)(weight[i]*14));
            fill.setColor(heatColor(v,alpha));
            c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,fill);
        }
        if(!finalMode)drawProbe(c);
        if(best>=0&&bestValue>.67f)drawSource(c,cw,ch);
        String mode=finalMode?"NİHAİ SES HARİTASI":"CANLI TARAMA";
        String info=String.format(Locale.US,"%s • örnek %d • kapsama %d%%",mode,samples,Math.round(getCoverage()*100));
        fill.setColor(Color.argb(185,0,0,0));c.drawRoundRect(dp(8),dp(8),Math.min(getWidth()-dp(8),dp(315)),dp(34),dp(8),dp(8),fill);
        text.setColor(Color.WHITE);c.drawText(info,dp(14),dp(26),text);
    }

    private void drawProbe(Canvas c){
        float x=targetX*getWidth(),y=targetY*getHeight();
        line.setStrokeWidth(dp(2.5f));line.setColor(trackerValid?Color.CYAN:Color.WHITE);
        c.drawCircle(x,y,dp(21),line);c.drawLine(x-dp(17),y,x+dp(17),y,line);c.drawLine(x,y-dp(17),x,y+dp(17),line);
        text.setColor(line.getColor());
        c.drawText(trackerValid?"FYVADIO PROB • KAYNAK DEĞİL":"PROB HEDEF",Math.min(getWidth()-dp(190),x+dp(23)),Math.max(dp(50),y-dp(8)),text);
    }

    private void drawSource(Canvas c,float cw,float ch){
        int bx=best%COLS,by=best/COLS;float pad=Math.max(cw,ch)*2.0f;
        RectF r=new RectF(Math.max(0,bx*cw-pad),Math.max(0,by*ch-pad),Math.min(getWidth(),(bx+1)*cw+pad),Math.min(getHeight(),(by+1)*ch+pad));
        line.setStrokeWidth(dp(bestValue>.86f?4:3));line.setColor(bestValue>.86f?Color.RED:Color.YELLOW);c.drawRoundRect(r,dp(9),dp(9),line);
        String s=(bestValue>.86f?"KAYNAK BULUNDU":"MUHTEMEL KAYNAK")+" %"+Math.round(bestValue*100)+" Δ "+String.format(Locale.US,"%.1f dB",bestDelta);
        fill.setColor(Color.argb(210,0,0,0));c.drawRect(r.left,Math.max(0,r.top-dp(27)),Math.min(getWidth(),r.left+dp(270)),r.top,fill);
        text.setColor(line.getColor());c.drawText(s,r.left+dp(5),Math.max(dp(15),r.top-dp(7)),text);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){
            targetX=clamp(e.getX()/Math.max(1f,getWidth()));targetY=clamp(e.getY()/Math.max(1f,getHeight()));
            trackerValid=true;trackerConfidence=.85f;if(listener!=null)listener.onTarget(targetX,targetY);invalidate();return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}

    /** 0 dB blue; 3 dB blue; 6 cyan; 9 green; 12 yellow; 15 orange; >=18 red. */
    private static float dbToColorLevel(float db){
        if(db<=0f)return .06f;
        if(db<3f)return .06f+(db/3f)*.10f;
        if(db<6f)return .16f+((db-3f)/3f)*.17f;
        if(db<9f)return .33f+((db-6f)/3f)*.17f;
        if(db<12f)return .50f+((db-9f)/3f)*.16f;
        if(db<15f)return .66f+((db-12f)/3f)*.14f;
        if(db<18f)return .80f+((db-15f)/3f)*.10f;
        if(db<24f)return .90f+((db-18f)/6f)*.07f;
        return .97f;
    }

    private int heatColor(float v,int alpha){
        v=clamp(v);int a=Math.max(38,Math.min(210,alpha));
        if(v<.16f){float t=v/.16f;return Color.argb(a,5,(int)(25+45*t),210+(int)(45*t));}
        if(v<.33f){float t=(v-.16f)/.17f;return Color.argb(a,0,(int)(70+170*t),255);}
        if(v<.50f){float t=(v-.33f)/.17f;return Color.argb(a,0,240,(int)(255*(1f-t)));}
        if(v<.66f){float t=(v-.50f)/.16f;return Color.argb(a,(int)(235*t),245,0);}
        if(v<.80f){float t=(v-.66f)/.14f;return Color.argb(a,245+(int)(10*t),(int)(245-105*t),0);}
        if(v<.90f){float t=(v-.80f)/.10f;return Color.argb(a,255,(int)(140-85*t),0);}
        float t=(v-.90f)/.10f;return Color.argb(a,255,(int)(55*(1f-t)),0);
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
