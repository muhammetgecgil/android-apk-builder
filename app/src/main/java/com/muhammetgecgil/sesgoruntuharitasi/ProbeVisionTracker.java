package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;
import java.util.Arrays;

/**
 * V9.1 persistent dynamic object tracker for the black circular Fyvadio probe.
 * - Auto full-frame acquisition at startup.
 * - Optional one-tap manual seed.
 * - Tracks centre + actual outer boundary radius.
 * - Never abandons the target during a scan: on loss it freezes the last trusted pose,
 *   stops measurement, and continuously searches for the same object.
 * - Uses geometry + blackness + learned radial appearance + motion prediction.
 */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,radius01,confidence;
        public final boolean valid,moving,frozen,reacquiring;
        Result(float x,float y,float r,float c,boolean v,boolean m,boolean f,boolean q){
            x01=x;y01=y;radius01=r;confidence=c;valid=v;moving=m;frozen=f;reacquiring=q;
        }
    }

    private static final float[] CS={1f,.9239f,.7071f,.3827f,0f,-.3827f,-.7071f,-.9239f,-1f,-.9239f,-.7071f,-.3827f,0f,.3827f,.7071f,.9239f};
    private static final float[] SN={0f,.3827f,.7071f,.9239f,1f,.9239f,.7071f,.3827f,0f,-.3827f,-.7071f,-.9239f,-1f,-.9239f,-.7071f,-.3827f};

    private float lastX=.14f,lastY=.70f,lastR=.075f,lastConf=0f,vx=0f,vy=0f;
    private boolean locked=false,appearanceReady=false,manualSeedPending=false;
    private int missCount=0,frameNo=0;

    // Learned target signature. Values are luminance means at radial fractions.
    private final float[] signature=new float[6];
    private float signatureContrast=35f;

    // Reacquisition candidate must persist before we jump away from frozen pose.
    private float candidateX=-1f,candidateY=-1f,candidateR=.075f,candidateScore=0f;
    private int candidateStreak=0;

    /** One tap is enough. The point can be approximate; the next frame fits the outer circle. */
    public synchronized void seed(float x01,float y01){
        lastX=clamp(x01);lastY=clamp(y01);vx=vy=0f;lastConf=.86f;
        locked=true;manualSeedPending=true;missCount=0;candidateStreak=0;
    }

    /** Forget the pose and let the detector find the probe anywhere by itself. */
    public synchronized void autoFind(){
        locked=false;appearanceReady=false;manualSeedPending=false;missCount=0;candidateStreak=0;vx=vy=0f;lastConf=0f;
    }

    public synchronized Result track(Bitmap b){
        frameNo++;
        if(b==null)return frozenResult(true);
        final int w=b.getWidth(),h=b.getHeight();
        if(w<80||h<100)return frozenResult(true);

        // A manual tap is an approximate centre. Snap it to the best nearby black circular body.
        if(manualSeedPending){
            Candidate m=manualRefine(b,w,h,lastX,lastY);
            manualSeedPending=false;
            if(m!=null){
                float nx=m.x/(float)w,ny=m.y/(float)h;
                float rr=fitOuterRadius(b,m.x,m.y,m.r,w,h)/(float)Math.min(w,h);
                lastX=nx;lastY=ny;lastR=rr;lastConf=.94f;locked=true;missCount=0;vx=vy=0f;
                learnAppearance(b,m.x,m.y,Math.max(5,Math.round(rr*Math.min(w,h))));
                return new Result(lastX,lastY,lastR,lastConf,true,false,false,false);
            }
        }

        // Normal high-speed local tracking.
        if(locked && missCount<4){
            Candidate c=localFind(b,w,h);
            if(c!=null){
                float nx=c.x/(float)w,ny=c.y/(float)h;
                float nr=fitOuterRadius(b,c.x,c.y,c.r,w,h)/(float)Math.min(w,h);
                if(acceptLocal(nx,ny,nr,c.score)){
                    updatePose(nx,ny,nr,c.score);
                    if(c.score>.72f && frameNo%8==0)learnAppearanceSlow(b,c.x,c.y,Math.max(5,Math.round(lastR*Math.min(w,h))));
                    return new Result(lastX,lastY,lastR,lastConf,true,Math.hypot(vx,vy)>.0013,false,false);
                }
            }
            missCount++;
        }

        // Loss never terminates tracking. Freeze the last trusted circle and search the whole image forever.
        Candidate g=globalFind(b,w,h);
        if(g!=null){
            float nx=g.x/(float)w,ny=g.y/(float)h;
            float nr=fitOuterRadius(b,g.x,g.y,g.r,w,h)/(float)Math.min(w,h);
            boolean same=(candidateX>=0f && Math.hypot(nx-candidateX,ny-candidateY)<.050f && Math.abs(nr-candidateR)<.045f);
            if(same){candidateStreak++;candidateScore=Math.max(candidateScore,g.score);}
            else{candidateX=nx;candidateY=ny;candidateR=nr;candidateScore=g.score;candidateStreak=1;}

            // Strong appearance match may reacquire in 2 frames; otherwise require 3 stable frames.
            int need=(appearanceReady && g.score>.76f)?2:3;
            if(candidateStreak>=need){
                lastX=candidateX;lastY=candidateY;lastR=candidateR;lastConf=clamp(.58f+(candidateScore-.55f)*1.25f);
                vx=vy=0f;missCount=0;locked=true;candidateStreak=0;
                if(!appearanceReady)learnAppearance(b,g.x,g.y,Math.max(5,Math.round(lastR*Math.min(w,h))));
                return new Result(lastX,lastY,lastR,lastConf,true,false,false,false);
            }
        }else{
            candidateStreak=0;
        }

        missCount=Math.min(60,missCount+1);vx*=.45f;vy*=.45f;lastConf*=.86f;
        return frozenResult(true);
    }

    private Candidate manualRefine(Bitmap b,int w,int h,float x01,float y01){
        int cx=Math.round(clamp(x01)*w),cy=Math.round(clamp(y01)*h);
        int span=Math.max(18,(int)(Math.min(w,h)*.12f));
        int minR=Math.max(5,(int)(Math.min(w,h)*.025f));
        int maxR=Math.max(minR+4,(int)(Math.min(w,h)*.19f));
        return findBest(b,w,h,Math.max(8,cx-span),Math.min(w-9,cx+span),Math.max(8,cy-span),Math.min(h-9,cy+span),minR,maxR,2,cx/(float)w,cy/(float)h,true,false);
    }

    private Candidate localFind(Bitmap b,int w,int h){
        float speed=(float)Math.hypot(vx,vy);
        float px=clamp(lastX+vx*1.45f),py=clamp(lastY+vy*1.45f);
        float search=Math.min(.22f,.055f+speed*5.5f+missCount*.032f);
        int x0=Math.max(8,(int)((px-search)*w)),x1=Math.min(w-9,(int)((px+search)*w));
        int y0=Math.max(8,(int)((py-search)*h)),y1=Math.min(h-9,(int)((py+search)*h));
        int minR=Math.max(5,(int)(Math.min(w,h)*Math.max(.025f,lastR*.58f)));
        int maxR=Math.max(minR+3,(int)(Math.min(w,h)*Math.min(.20f,lastR*1.72f)));
        return findBest(b,w,h,x0,x1,y0,y1,minR,maxR,2,px,py,true,true);
    }

    private Candidate globalFind(Bitmap b,int w,int h){
        int minR=Math.max(5,(int)(Math.min(w,h)*(appearanceReady?Math.max(.022f,lastR*.45f):.025f)));
        int maxR=Math.max(minR+4,(int)(Math.min(w,h)*(appearanceReady?Math.min(.22f,lastR*2.05f):.20f)));
        Candidate c=findBest(b,w,h,8,w-9,8,h-9,minR,maxR,3,lastX,lastY,false,appearanceReady);
        float th=appearanceReady?.60f:.68f;
        return c!=null&&c.score>=th?c:null;
    }

    private boolean acceptLocal(float nx,float ny,float nr,float score){
        float speed=(float)Math.hypot(vx,vy);
        float px=clamp(lastX+vx*1.45f),py=clamp(lastY+vy*1.45f);
        float jump=(float)Math.hypot(nx-px,ny-py);
        float maxJump=Math.min(.20f,.060f+speed*5.0f+missCount*.018f);
        return score>.49f && jump<=maxJump && nr>=lastR*.48f && nr<=lastR*1.95f;
    }

    private void updatePose(float nx,float ny,float nr,float score){
        float dx=nx-lastX,dy=ny-lastY;
        vx=.24f*vx+.76f*dx;vy=.24f*vy+.76f*dy;
        float speed=(float)Math.hypot(vx,vy);
        float alpha=speed>.006f?.94f:.84f;
        lastX=clamp(lastX*(1f-alpha)+nx*alpha);lastY=clamp(lastY*(1f-alpha)+ny*alpha);
        lastR=.76f*lastR+.24f*nr;lastConf=clamp(.48f+(score-.46f)*1.42f);missCount=0;candidateStreak=0;
    }

    private static final class Candidate{int x,y,r;float score;}

    private Candidate findBest(Bitmap b,int w,int h,int x0,int x1,int y0,int y1,int minR,int maxR,int step,float px,float py,boolean local,boolean useAppearance){
        float best=-99f;int bx=-1,by=-1,br=minR;
        int rStep=Math.max(2,(maxR-minR)/5);
        for(int y=y0;y<=y1;y+=step)for(int x=x0;x<=x1;x+=step){
            if(lum(b.getPixel(x,y))>118)continue;
            for(int r=minR;r<=maxR;r+=rStep){
                float s=geometryScore(b,x,y,r,w,h);
                if(s<-1f)continue;
                if(useAppearance && appearanceReady)s=.68f*s+.32f*appearanceScore(b,x,y,r,w,h);
                if(local){
                    float d=(float)Math.hypot(x/(float)w-px,y/(float)h-py);s-=Math.min(.24f,d*.85f);
                    float rr=r/(float)Math.min(w,h);s-=Math.min(.13f,Math.abs(rr-lastR)*1.5f);
                }
                if(s>best){best=s;bx=x;by=y;br=r;}
            }
        }
        if(bx<0)return null;
        // Pixel-level centre/radius refinement.
        int fx=bx,fy=by,fr=br;
        for(int y=Math.max(7,fy-3);y<=Math.min(h-8,fy+3);y++)for(int x=Math.max(7,fx-3);x<=Math.min(w-8,fx+3);x++)for(int r=Math.max(5,fr-3);r<=Math.min(maxR,fr+3);r++){
            float s=geometryScore(b,x,y,r,w,h);
            if(useAppearance&&appearanceReady)s=.68f*s+.32f*appearanceScore(b,x,y,r,w,h);
            if(s>best){best=s;bx=x;by=y;br=r;}
        }
        Candidate c=new Candidate();c.x=bx;c.y=by;c.r=br;c.score=best;return c;
    }

    /** Geometry score favours a dark filled circular body with a brighter exterior and radial symmetry. */
    private float geometryScore(Bitmap b,int cx,int cy,int r,int w,int h){
        if(r<4||cx-r-4<0||cy-r-4<0||cx+r+4>=w||cy+r+4>=h)return -9f;
        float inner=darkness(lum(b.getPixel(cx,cy))),mid=0f,rim=0f,edge=0f;
        float rimMean=0f,rimSq=0f;
        for(int k=0;k<16;k++){
            int xm=cx+Math.round(CS[k]*r*.48f),ym=cy+Math.round(SN[k]*r*.48f);
            int xr=cx+Math.round(CS[k]*r*.82f),yr=cy+Math.round(SN[k]*r*.82f);
            int xo=cx+Math.round(CS[k]*r*1.18f),yo=cy+Math.round(SN[k]*r*1.18f);
            int lm=lum(b.getPixel(xm,ym)),lr=lum(b.getPixel(xr,yr)),lo=lum(b.getPixel(xo,yo));
            mid+=darkness(lm);rim+=darkness(lr);edge+=clamp01((lo-lr)/100f);rimMean+=lr;rimSq+=lr*lr;
        }
        mid/=16f;rim/=16f;edge/=16f;rimMean/=16f;
        float var=Math.max(0f,rimSq/16f-rimMean*rimMean);
        float symmetry=1f-Math.min(1f,(float)Math.sqrt(var)/58f);
        return inner*.20f+mid*.30f+rim*.20f+edge*.20f+symmetry*.10f;
    }

    /** Fit the visible outer circumference from radial dark-to-bright transitions; median rejects occlusion. */
    private int fitOuterRadius(Bitmap b,int cx,int cy,int guessR,int w,int h){
        int min=Math.max(4,(int)(guessR*.58f));
        int max=Math.max(min+3,(int)(guessR*1.55f));
        max=Math.min(max,Math.min(Math.min(cx,w-1-cx),Math.min(cy,h-1-cy))-2);
        if(max<=min)return Math.max(5,guessR);
        int[] rs=new int[16];int n=0;
        for(int k=0;k<16;k++){
            float best=-999f;int br=guessR;
            for(int r=min;r<=max;r++){
                int xi=cx+Math.round(CS[k]*Math.max(1,r-2)),yi=cy+Math.round(SN[k]*Math.max(1,r-2));
                int xo=cx+Math.round(CS[k]*Math.min(max,r+2)),yo=cy+Math.round(SN[k]*Math.min(max,r+2));
                if(xi<0||yi<0||xo<0||yo<0||xi>=w||xo>=w||yi>=h||yo>=h)continue;
                int li=lum(b.getPixel(xi,yi)),lo=lum(b.getPixel(xo,yo));
                float grad=lo-li;
                // Prefer edge after a dark interior.
                float q=grad + (li<115?22f:0f) - Math.abs(r-guessR)*.35f;
                if(q>best){best=q;br=r;}
            }
            if(best>8f)rs[n++]=br;
        }
        if(n<6)return Math.max(5,guessR);
        Arrays.sort(rs,0,n);return rs[n/2];
    }

    private void learnAppearance(Bitmap b,int cx,int cy,int r){
        sampleSignature(b,cx,cy,r,signature);signatureContrast=Math.max(18f,signature[5]-signature[3]);appearanceReady=true;
    }

    private void learnAppearanceSlow(Bitmap b,int cx,int cy,int r){
        float[] now=new float[6];sampleSignature(b,cx,cy,r,now);
        for(int i=0;i<6;i++)signature[i]=signature[i]*.96f+now[i]*.04f;
        signatureContrast=Math.max(18f,signatureContrast*.96f+Math.max(0f,now[5]-now[3])*.04f);
    }

    private void sampleSignature(Bitmap b,int cx,int cy,int r,float[] out){
        final float[] f={0f,.30f,.55f,.78f,1.00f,1.25f};
        for(int j=0;j<f.length;j++){
            if(j==0){out[j]=lumSafe(b,cx,cy);continue;}
            float sum=0f;int n=0;
            for(int k=0;k<16;k++){
                int x=cx+Math.round(CS[k]*r*f[j]),y=cy+Math.round(SN[k]*r*f[j]);
                if(x>=0&&y>=0&&x<b.getWidth()&&y<b.getHeight()){sum+=lum(b.getPixel(x,y));n++;}
            }
            out[j]=n>0?sum/n:128f;
        }
    }

    private float appearanceScore(Bitmap b,int cx,int cy,int r,int w,int h){
        float[] now=new float[6];sampleSignature(b,cx,cy,r,now);
        float err=0f;
        for(int i=0;i<6;i++)err+=Math.abs(now[i]-signature[i]);
        err/=6f;
        float contrast=Math.max(0f,now[5]-now[3]);
        float sim=1f-Math.min(1f,err/70f);
        float contrastSim=1f-Math.min(1f,Math.abs(contrast-signatureContrast)/80f);
        return .78f*sim+.22f*contrastSim;
    }

    private static int lumSafe(Bitmap b,int x,int y){if(x<0||y<0||x>=b.getWidth()||y>=b.getHeight())return 128;return lum(b.getPixel(x,y));}
    private static float darkness(int l){return clamp01((150f-l)/130f);}
    private static int lum(int p){int r=(p>>16)&255,g=(p>>8)&255,bb=p&255;return (3*r+6*g+bb)/10;}
    private Result frozenResult(boolean reacq){return new Result(lastX,lastY,lastR,lastConf,false,false,true,reacq);}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
    private static float clamp01(float v){return Math.max(0f,Math.min(1f,v));}
}
