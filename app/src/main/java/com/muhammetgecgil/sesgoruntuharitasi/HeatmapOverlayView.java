package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

/** V8 full-screen accumulated sound map. Live scan stores measurements; final mode interpolates and paints the whole camera image. */
public final class HeatmapOverlayView extends View {
    public interface TargetListener { void onTarget(float x01,float y01); }
    private static final int COLS=40,ROWS=64,N=COLS*ROWS;
    private final float[] value=new float[N];
    private final float[] weight=new float[N];
    private final float[] finalHeat=new float[N];
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TargetListener listener;
    private float targetX=.5f,targetY=.72f,trackerConfidence=0f;
    private boolean trackerValid=false,finalMode=false;
    private int best=-1,samples=0,observed=0;
    private float bestValue=0f,bestDelta=0f;
    private long lastNewCellMs=System.currentTimeMillis();

    public HeatmapOverlayView(Context c){super(c);setWillNotDraw(false);setClickable(true);line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(dp(2));text.setTextSize(dp(12));text.setFakeBoldText(true);}
    public void setTargetListener(TargetListener l){listener=l;}
    public synchronized boolean isFinalMode(){return finalMode;}
    public synchronized float getCoverage(){return observed/(float)N;}
    public synchronized long getLastNewCellMs(){return lastNewCellMs;}
    public synchronized int getSamples(){return samples;}

    public synchronized void setTracker(float x,float y,float confidence,boolean valid){targetX=clamp(x);targetY=clamp(y);trackerConfidence=clamp(confidence);trackerValid=valid;postInvalidateOnAnimation();}

    public synchronized void clearMap(){for(int i=0;i<N;i++){value[i]=0;weight[i]=0;finalHeat[i]=0;}best=-1;bestValue=0;bestDelta=0;samples=0;observed=0;finalMode=false;lastNewCellMs=System.currentTimeMillis();invalidate();}

    public synchronized void updateProbe(float x01,float y01,float energy01,float deltaDb,boolean active){
        if(!active||finalMode)return;
        int cx=Math.max(0,Math.min(COLS-1,(int)(clamp(x01)*COLS)));
        int cy=Math.max(0,Math.min(ROWS-1,(int)(clamp(y01)*ROWS)));
        float deltaScore=clamp((deltaDb+4f)/26f);
        float level=clamp(.58f*clamp(energy01)+.42f*deltaScore);
        boolean newCell=false;
        for(int dy=-4;dy<=4;dy++)for(int dx=-4;dx<=4;dx++){
            int x=cx+dx,y=cy+dy;if(x<0||x>=COLS||y<0||y>=ROWS)continue;
            float d2=dx*dx+dy*dy;float g=(float)Math.exp(-d2/7.2f);if(g<.045f)continue;
            int i=y*COLS+x;float add=g*(.65f+.35f*trackerConfidence);
            if(weight[i]<.05f){observed++;newCell=true;}
            float oldW=weight[i],newW=Math.min(24f,oldW+add);
            value[i]=oldW<=0?level:(value[i]*oldW+level*add)/(oldW+add);
            weight[i]=newW;
        }
        if(newCell)lastNewCellMs=System.currentTimeMillis();samples++;
        recomputeBest(deltaDb);postInvalidateOnAnimation();
    }

    public synchronized void finishScan(){
        if(samples<3)return;
        finalMode=true;
        float min=1f,max=0f;int known=0;
        for(int i=0;i<N;i++)if(weight[i]>.03f){finalHeat[i]=value[i];min=Math.min(min,value[i]);max=Math.max(max,value[i]);known++;}else finalHeat[i]=-1f;
        if(known==0){finalMode=false;return;}
        float[] tmp=new float[N];
        for(int pass=0;pass<42;pass++){
            System.arraycopy(finalHeat,0,tmp,0,N);
            boolean any=false;
            for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
                int i=y*COLS+x;if(finalHeat[i]>=0)continue;
                float s=0,w=0;
                for(int yy=Math.max(0,y-1);yy<=Math.min(ROWS-1,y+1);yy++)for(int xx=Math.max(0,x-1);xx<=Math.min(COLS-1,x+1);xx++){
                    if(xx==x&&yy==y)continue;float v=finalHeat[yy*COLS+xx];if(v<0)continue;float k=(xx==x||yy==y)?1f:.72f;s+=v*k;w+=k;
                }
                if(w>0){tmp[i]=s/w;any=true;}
            }
            System.arraycopy(tmp,0,finalHeat,0,N);if(!any)break;
        }
        float baseline=min;
        float span=Math.max(.10f,max-min);
        for(int i=0;i<N;i++){
            float raw=finalHeat[i]<0?baseline:finalHeat[i];
            float norm=clamp((raw-min)/span);
            finalHeat[i]=clamp(.28f*raw+.72f*norm);
        }
        recomputeBest(bestDelta);invalidate();
    }

    public synchronized void resumeScan(){finalMode=false;invalidate();}

    private void recomputeBest(float delta){best=-1;bestValue=-1;for(int i=0;i<N;i++){float v=finalMode?finalHeat[i]:value[i];if(v>bestValue&&((finalMode)||weight[i]>.04f)){bestValue=v;best=i;}}bestDelta=delta;}

    @Override protected synchronized void onDraw(Canvas c){
        super.onDraw(c);float cw=getWidth()/(float)COLS,ch=getHeight()/(float)ROWS;
        for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++){
            int i=y*COLS+x;float v=finalMode?finalHeat[i]:value[i];
            if(!finalMode&&weight[i]<.035f)continue;
            if(v<0)v=0;
            fill.setStyle(Paint.Style.FILL);fill.setColor(heatColor(v,finalMode?205:Math.min(205,55+(int)(weight[i]*18))));
            c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,fill);
        }
        if(!finalMode)drawProbe(c);
        if(best>=0&&bestValue>.56f)drawSource(c,cw,ch);
        String mode=finalMode?"NİHAİ SES HARİTASI":"CANLI TARAMA";
        String info=String.format(Locale.US,"%s  •  örnek %d  •  kapsama %d%%",mode,samples,Math.round(getCoverage()*100));
        fill.setColor(Color.argb(185,0,0,0));c.drawRoundRect(dp(8),dp(8),Math.min(getWidth()-dp(8),dp(315)),dp(34),dp(8),dp(8),fill);text.setColor(Color.WHITE);c.drawText(info,dp(14),dp(26),text);
    }

    private void drawProbe(Canvas c){float x=targetX*getWidth(),y=targetY*getHeight();line.setStrokeWidth(dp(2.5f));line.setColor(trackerValid?Color.CYAN:Color.WHITE);c.drawCircle(x,y,dp(21),line);c.drawLine(x-dp(17),y,x+dp(17),y,line);c.drawLine(x,y-dp(17),x,y+dp(17),line);text.setColor(line.getColor());c.drawText(trackerValid?"FYVADIO AUTO":"PROB HEDEF",Math.min(getWidth()-dp(105),x+dp(23)),Math.max(dp(50),y-dp(8)),text);}

    private void drawSource(Canvas c,float cw,float ch){int bx=best%COLS,by=best/COLS;float pad=Math.max(cw,ch)*2.2f;RectF r=new RectF(Math.max(0,bx*cw-pad),Math.max(0,by*ch-pad),Math.min(getWidth(),(bx+1)*cw+pad),Math.min(getHeight(),(by+1)*ch+pad));line.setStrokeWidth(dp(bestValue>.78f?4:3));line.setColor(bestValue>.78f?Color.RED:Color.YELLOW);c.drawRoundRect(r,dp(9),dp(9),line);String s=(bestValue>.78f?"KAYNAK BULUNDU":"MUHTEMEL KAYNAK")+"  %"+Math.round(bestValue*100)+"  Δ "+String.format(Locale.US,"%.1f dB",bestDelta);fill.setColor(Color.argb(210,0,0,0));c.drawRect(r.left,Math.max(0,r.top-dp(27)),Math.min(getWidth(),r.left+dp(265)),r.top,fill);text.setColor(line.getColor());c.drawText(s,r.left+dp(5),Math.max(dp(15),r.top-dp(7)),text);}

    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){targetX=clamp(e.getX()/Math.max(1f,getWidth()));targetY=clamp(e.getY()/Math.max(1f,getHeight()));trackerValid=true;trackerConfidence=.85f;if(listener!=null)listener.onTarget(targetX,targetY);invalidate();return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}

    private int heatColor(float v,int alpha){v=clamp(v);int a=Math.max(45,Math.min(225,alpha));if(v<.18f)return Color.argb(a,15,35,220);if(v<.36f)return Color.argb(a,0,180,255);if(v<.54f)return Color.argb(a,0,235,90);if(v<.70f)return Color.argb(a,245,240,0);if(v<.86f)return Color.argb(a,255,125,0);return Color.argb(a,255,0,0);}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
