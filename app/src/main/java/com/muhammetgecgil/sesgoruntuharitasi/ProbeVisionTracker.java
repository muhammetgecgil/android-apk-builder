package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;
import java.util.Arrays;

/** V9.2 perspective-aware persistent tracker.
 * Centre tracking and visible outer-radius estimation are independent.
 * The circle follows apparent probe size when the hand moves toward/away from camera.
 */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,radius01,confidence;
        public final boolean valid,moving,frozen,reacquiring;
        Result(float x,float y,float r,float c,boolean v,boolean m,boolean f,boolean q){x01=x;y01=y;radius01=r;confidence=c;valid=v;moving=m;frozen=f;reacquiring=q;}
    }

    private static final int RAYS=24;
    private static final float[] CS=new float[RAYS],SN=new float[RAYS];
    static{for(int i=0;i<RAYS;i++){double a=2.0*Math.PI*i/RAYS;CS[i]=(float)Math.cos(a);SN[i]=(float)Math.sin(a);}}

    private float lastX=.14f,lastY=.70f,lastR=.075f,lastConf=0f,vx=0f,vy=0f,vr=0f;
    private boolean locked=false,manualSeedPending=false;
    private int missCount=0,candidateStreak=0;
    private float candidateX=-1,candidateY=-1,candidateR=.075f,candidateScore=0f;

    public synchronized void seed(float x01,float y01){
        lastX=clamp(x01);lastY=clamp(y01);vx=vy=vr=0;lastConf=.9f;locked=true;manualSeedPending=true;missCount=0;candidateStreak=0;
    }
    public synchronized void autoFind(){locked=false;manualSeedPending=false;missCount=0;candidateStreak=0;vx=vy=vr=0;lastConf=0;}

    public synchronized Result track(Bitmap b){
        if(b==null)return frozen(true);
        int w=b.getWidth(),h=b.getHeight(),m=Math.min(w,h);
        if(w<80||h<100)return frozen(true);

        if(manualSeedPending){
            manualSeedPending=false;
            Candidate c=findAround(b,w,h,lastX,lastY,.15f,.022f,.24f,2,false);
            if(c!=null){acceptFresh(b,c,w,h);return result(true,false,false,false);}
        }

        if(locked&&missCount<3){
            float speed=(float)Math.hypot(vx,vy);
            float px=clamp(lastX+vx*1.45f),py=clamp(lastY+vy*1.45f);
            float search=Math.min(.25f,.055f+speed*5.5f+Math.abs(vr)*2.2f+missCount*.035f);
            float rPred=clampR(lastR+vr*1.3f);
            float minR=Math.max(.018f,rPred*.42f),maxR=Math.min(.30f,rPred*2.35f);
            Candidate c=findAround(b,w,h,px,py,search,minR,maxR,2,true);
            if(c!=null&&c.score>.47f){
                float nx=c.x/(float)w,ny=c.y/(float)h;
                int fitted=fitVisibleOuterRadius(b,c.x,c.y,c.r,w,h);
                float nr=clampR(fitted/(float)m);
                float jump=(float)Math.hypot(nx-px,ny-py);
                if(jump<Math.min(.23f,.07f+speed*5.2f+missCount*.02f)){
                    updatePose(nx,ny,nr,c.score);
                    return result(true,Math.hypot(vx,vy)>.0013,false,false);
                }
            }
            missCount++;
        }

        Candidate g=findAround(b,w,h,.5f,.5f,.72f,.018f,.30f,3,false);
        if(g!=null&&g.score>.61f){
            float nx=g.x/(float)w,ny=g.y/(float)h;
            float nr=clampR(fitVisibleOuterRadius(b,g.x,g.y,g.r,w,h)/(float)m);
            boolean same=candidateX>=0&&Math.hypot(nx-candidateX,ny-candidateY)<.06&&Math.abs(nr-candidateR)<.08;
            if(same){candidateStreak++;candidateScore=Math.max(candidateScore,g.score);}else{candidateX=nx;candidateY=ny;candidateR=nr;candidateScore=g.score;candidateStreak=1;}
            if(candidateStreak>=2){
                lastX=candidateX;lastY=candidateY;lastR=candidateR;lastConf=clamp(.58f+(candidateScore-.55f)*1.35f);vx=vy=vr=0;locked=true;missCount=0;candidateStreak=0;
                return result(true,false,false,false);
            }
        }else candidateStreak=0;

        missCount=Math.min(80,missCount+1);vx*=.45f;vy*=.45f;vr*=.35f;lastConf*=.86f;
        return frozen(true);
    }

    private Candidate findAround(Bitmap b,int w,int h,float cx01,float cy01,float span,float minR01,float maxR01,int step,boolean local){
        int m=Math.min(w,h),cx=Math.round(cx01*w),cy=Math.round(cy01*h);
        int sx=Math.round(span*w),sy=Math.round(span*h);
        int x0=Math.max(8,cx-sx),x1=Math.min(w-9,cx+sx),y0=Math.max(8,cy-sy),y1=Math.min(h-9,cy+sy);
        int minR=Math.max(4,Math.round(minR01*m)),maxR=Math.max(minR+4,Math.round(maxR01*m));
        maxR=Math.min(maxR,m/3);
        float best=-99;int bx=-1,by=-1,br=minR;
        int rStep=Math.max(2,(maxR-minR)/7);
        for(int y=y0;y<=y1;y+=step)for(int x=x0;x<=x1;x+=step){
            if(lum(b.getPixel(x,y))>125)continue;
            for(int r=minR;r<=maxR;r+=rStep){
                float s=scoreCircle(b,x,y,r,w,h);
                if(local){
                    float d=(float)Math.hypot(x/(float)w-cx01,y/(float)h-cy01);s-=Math.min(.22f,d*.8f);
                    float rr=r/(float)m;s-=Math.min(.07f,Math.abs(rr-(lastR+vr))*0.55f);
                }
                if(s>best){best=s;bx=x;by=y;br=r;}
            }
        }
        if(bx<0)return null;
        int fx=bx,fy=by,fr=br;
        for(int y=Math.max(7,fy-4);y<=Math.min(h-8,fy+4);y++)for(int x=Math.max(7,fx-4);x<=Math.min(w-8,fx+4);x++)for(int r=Math.max(4,fr-5);r<=Math.min(maxR,fr+5);r++){
            float s=scoreCircle(b,x,y,r,w,h);if(s>best){best=s;bx=x;by=y;br=r;}
        }
        Candidate c=new Candidate();c.x=bx;c.y=by;c.r=br;c.score=best;return c;
    }

    private float scoreCircle(Bitmap b,int cx,int cy,int r,int w,int h){
        if(r<4||cx-r-5<0||cy-r-5<0||cx+r+5>=w||cy+r+5>=h)return -9;
        float center=dark(lum(b.getPixel(cx,cy))),inner=0,rim=0,contrast=0;float mean=0,sq=0;
        for(int k=0;k<RAYS;k++){
            int xi=cx+Math.round(CS[k]*r*.48f),yi=cy+Math.round(SN[k]*r*.48f);
            int xr=cx+Math.round(CS[k]*r*.82f),yr=cy+Math.round(SN[k]*r*.82f);
            int xo=cx+Math.round(CS[k]*r*1.18f),yo=cy+Math.round(SN[k]*r*1.18f);
            int li=lum(b.getPixel(xi,yi)),lr=lum(b.getPixel(xr,yr)),lo=lum(b.getPixel(xo,yo));
            inner+=dark(li);rim+=dark(lr);contrast+=clamp01((lo-lr)/95f);mean+=lr;sq+=lr*lr;
        }
        inner/=RAYS;rim/=RAYS;contrast/=RAYS;mean/=RAYS;
        float symmetry=1f-Math.min(1f,(float)Math.sqrt(Math.max(0,sq/RAYS-mean*mean))/62f);
        return center*.18f+inner*.29f+rim*.18f+contrast*.25f+symmetry*.10f;
    }

    /** Uses the visible dark-to-bright circumference, independent of the detector's guessed radius. */
    private int fitVisibleOuterRadius(Bitmap b,int cx,int cy,int guess,int w,int h){
        int m=Math.min(w,h);
        int min=Math.max(4,Math.min(Math.round(lastR*m*.30f),Math.round(guess*.38f)));
        int max=Math.max(min+6,Math.max(Math.round(lastR*m*2.70f),Math.round(guess*2.20f)));
        max=Math.min(max,Math.min(Math.min(cx,w-1-cx),Math.min(cy,h-1-cy))-3);
        max=Math.min(max,Math.round(m*.31f));
        if(max<=min)return Math.max(5,guess);
        int[] radii=new int[RAYS];float[] qualities=new float[RAYS];int n=0;
        for(int k=0;k<RAYS;k++){
            float best=-999;int br=guess;
            for(int r=min;r<=max;r++){
                int r1=Math.max(1,r-3),r2=Math.min(max,r+3);
                int x1=cx+Math.round(CS[k]*r1),y1=cy+Math.round(SN[k]*r1);
                int x2=cx+Math.round(CS[k]*r2),y2=cy+Math.round(SN[k]*r2);
                if(x1<0||y1<0||x2<0||y2<0||x1>=w||x2>=w||y1>=h||y2>=h)continue;
                int li=lum(b.getPixel(x1,y1)),lo=lum(b.getPixel(x2,y2));
                float grad=lo-li;
                float darkBonus=li<120?18:0;
                float q=grad+darkBonus;
                if(q>best){best=q;br=r;}
            }
            if(best>10){radii[n]=br;qualities[n]=best;n++;}
        }
        if(n<8)return Math.max(5,guess);
        Arrays.sort(radii,0,n);
        int lo=n/5,hi=n-1-n/5;
        int sum=0,cnt=0;for(int i=lo;i<=hi;i++){sum+=radii[i];cnt++;}
        return cnt>0?Math.round(sum/(float)cnt):radii[n/2];
    }

    private void updatePose(float nx,float ny,float nr,float score){
        float dx=nx-lastX,dy=ny-lastY,dr=nr-lastR;
        vx=.22f*vx+.78f*dx;vy=.22f*vy+.78f*dy;vr=.35f*vr+.65f*dr;
        float speed=(float)Math.hypot(vx,vy),a=speed>.006f?.94f:.86f;
        lastX=clamp(lastX*(1-a)+nx*a);lastY=clamp(lastY*(1-a)+ny*a);
        float ratio=nr/Math.max(.001f,lastR);
        float ra=(ratio>1.18f||ratio<.84f)?.70f:.48f;
        lastR=clampR(lastR*(1-ra)+nr*ra);
        lastConf=clamp(.48f+(score-.45f)*1.45f);missCount=0;candidateStreak=0;
    }

    private void acceptFresh(Bitmap b,Candidate c,int w,int h){
        int m=Math.min(w,h);lastX=c.x/(float)w;lastY=c.y/(float)h;lastR=clampR(fitVisibleOuterRadius(b,c.x,c.y,c.r,w,h)/(float)m);lastConf=.94f;locked=true;missCount=0;vx=vy=vr=0;
    }

    private Result result(boolean v,boolean moving,boolean f,boolean q){return new Result(lastX,lastY,lastR,lastConf,v,moving,f,q);}
    private Result frozen(boolean q){return new Result(lastX,lastY,lastR,lastConf,false,false,true,q);}
    private static final class Candidate{int x,y,r;float score;}
    private static int lum(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return (3*r+6*g+b)/10;}
    private static float dark(int l){return clamp01((150f-l)/130f);}
    private static float clamp(float v){return Math.max(0,Math.min(1,v));}
    private static float clampR(float v){return Math.max(.018f,Math.min(.31f,v));}
    private static float clamp01(float v){return Math.max(0,Math.min(1,v));}
}
