package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;

/** V9.0: automatic black circular Fyvadio acquisition + fast local tracking + global reacquisition. */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,radius01,confidence;
        public final boolean valid,moving,frozen;
        Result(float x,float y,float r,float c,boolean v,boolean m,boolean f){x01=x;y01=y;radius01=r;confidence=c;valid=v;moving=m;frozen=f;}
    }

    private float lastX=.14f,lastY=.70f,lastR=.075f,lastConf=0f,vx=0f,vy=0f;
    private boolean locked=false;
    private int missCount=0,reacquireFrame=0;
    private float candidateX=-1f,candidateY=-1f,candidateR=.075f;
    private int candidateStreak=0;

    /** Optional manual seed remains available, but normal operation does not need it. */
    public synchronized void seed(float x01,float y01){
        lastX=clamp(x01);lastY=clamp(y01);vx=vy=0f;lastConf=.92f;locked=true;missCount=0;candidateStreak=0;
    }

    public synchronized Result track(Bitmap b){
        if(b==null)return frozenResult();
        final int w=b.getWidth(),h=b.getHeight();
        if(w<60||h<80)return frozenResult();

        // Fast local tracker while locked. After only a few misses, immediately fall back to full-frame search.
        if(locked&&missCount<3){
            Result r=localTrack(b,w,h);
            if(r!=null)return r;
        }

        // Full-frame reacquisition: no user intervention. Run every frame after loss and at startup.
        Result g=globalAcquire(b,w,h);
        if(g!=null)return g;

        missCount=Math.min(20,missCount+1);reacquireFrame++;
        vx*=.45f;vy*=.45f;lastConf*=.78f;
        return new Result(lastX,lastY,lastR,lastConf,false,false,true);
    }

    private Result localTrack(Bitmap b,int w,int h){
        float speed=(float)Math.hypot(vx,vy);
        float px=clamp(lastX+vx*1.35f),py=clamp(lastY+vy*1.35f);
        float search=Math.min(.19f,.060f+speed*4.8f+missCount*.030f);
        int x0=Math.max(8,(int)((px-search)*w)),x1=Math.min(w-9,(int)((px+search)*w));
        int y0=Math.max(8,(int)((py-search)*h)),y1=Math.min(h-9,(int)((py+search)*h));
        int minR=Math.max(5,(int)(Math.min(w,h)*Math.max(.035f,lastR*.58f)));
        int maxR=Math.max(minR+2,(int)(Math.min(w,h)*Math.min(.17f,lastR*1.62f)));
        Candidate c=findBest(b,w,h,x0,x1,y0,y1,minR,maxR,2,px,py,true);
        if(c==null||c.score<.50f){missCount++;return null;}
        float nx=c.x/(float)w,ny=c.y/(float)h,nr=c.r/(float)Math.min(w,h);
        float jump=(float)Math.hypot(nx-px,ny-py);
        float maxJump=Math.min(.17f,.055f+speed*4.2f+missCount*.018f);
        if(jump>maxJump||nr<lastR*.52f||nr>lastR*1.75f){missCount++;return null;}
        return accept(nx,ny,nr,c.score,c.uniqueness,false);
    }

    private Result globalAcquire(Bitmap b,int w,int h){
        // If we have seen this probe before, keep radius prior fairly broad; on first launch use full plausible range.
        int minR=Math.max(5,(int)(Math.min(w,h)*(locked?Math.max(.030f,lastR*.45f):.030f)));
        int maxR=Math.max(minR+3,(int)(Math.min(w,h)*(locked?Math.min(.19f,lastR*1.95f):.18f)));
        Candidate c=findBest(b,w,h,8,w-9,8,h-9,minR,maxR,4,lastX,lastY,false);
        if(c==null||c.score<(locked?.61f:.65f)){candidateStreak=0;return null;}

        float nx=c.x/(float)w,ny=c.y/(float)h,nr=c.r/(float)Math.min(w,h);
        // Require two compatible frames before jumping to a new location. This prevents latching to random black objects.
        if(candidateX>=0f&&Math.hypot(nx-candidateX,ny-candidateY)<.055f&&Math.abs(nr-candidateR)<.055f){candidateStreak++;}
        else{candidateX=nx;candidateY=ny;candidateR=nr;candidateStreak=1;}
        if(candidateStreak<2)return null;

        locked=true;missCount=0;candidateStreak=0;vx=vy=0f;
        lastX=nx;lastY=ny;lastR=nr;lastConf=clamp(.58f+(c.score-.60f)*1.45f);
        return new Result(lastX,lastY,lastR,lastConf,true,false,false);
    }

    private Result accept(float nx,float ny,float nr,float score,float uniqueness,boolean global){
        float dx=nx-lastX,dy=ny-lastY;
        vx=.30f*vx+.70f*dx;vy=.30f*vy+.70f*dy;
        float speed=(float)Math.hypot(vx,vy),alpha=speed>.008f?.92f:.80f;
        lastX=clamp(lastX*(1f-alpha)+nx*alpha);lastY=clamp(lastY*(1f-alpha)+ny*alpha);lastR=.70f*lastR+.30f*nr;
        lastConf=clamp(.48f+(score-.48f)*1.55f+Math.min(.10f,uniqueness*2f));missCount=0;
        return new Result(lastX,lastY,lastR,lastConf,true,Math.hypot(vx,vy)>.0015,false);
    }

    private static final class Candidate{int x,y,r;float score,uniqueness;}

    private Candidate findBest(Bitmap b,int w,int h,int x0,int x1,int y0,int y1,int minR,int maxR,int step,float px,float py,boolean local){
        float best=-99f,second=-99f;int bx=-1,by=-1,br=minR;
        int rStep=Math.max(2,(maxR-minR)/4);
        for(int y=y0;y<=y1;y+=step)for(int x=x0;x<=x1;x+=step){
            int l=lum(b.getPixel(x,y));if(l>105)continue;
            for(int r=minR;r<=maxR;r+=rStep){
                float s=circleScore(b,x,y,r,w,h);
                if(local){float dx=x/(float)w-px,dy=y/(float)h-py;s-=Math.min(.28f,(float)Math.hypot(dx,dy)*1.05f);float rr=r/(float)Math.min(w,h);s-=Math.min(.16f,Math.abs(rr-lastR)*1.9f);}
                else if(locked){float d=(float)Math.hypot(x/(float)w-lastX,y/(float)h-lastY);s+=Math.max(0f,.06f-d*.05f);}
                if(s>best){second=best;best=s;bx=x;by=y;br=r;}else if(s>second)second=s;
            }
        }
        if(bx<0)return null;
        // Fine refinement around the strongest candidate.
        int fx=bx,fy=by,fr=br;
        for(int y=Math.max(7,fy-3);y<=Math.min(h-8,fy+3);y++)for(int x=Math.max(7,fx-3);x<=Math.min(w-8,fx+3);x++)for(int r=Math.max(5,fr-2);r<=Math.min(maxR,fr+2);r++){
            float s=circleScore(b,x,y,r,w,h);if(s>best){second=best;best=s;bx=x;by=y;br=r;}
        }
        Candidate c=new Candidate();c.x=bx;c.y=by;c.r=br;c.score=best;c.uniqueness=best-second;return c;
    }

    private float circleScore(Bitmap b,int cx,int cy,int r,int w,int h){
        if(cx-r-3<0||cy-r-3<0||cx+r+3>=w||cy+r+3>=h)return -9f;
        final float[] cs={1f,.7071f,0f,-.7071f,-1f,-.7071f,0f,.7071f};
        final float[] sn={0f,.7071f,1f,.7071f,0f,-.7071f,-1f,-.7071f};
        float inside=darkness(lum(b.getPixel(cx,cy)));
        for(int k=0;k<8;k++)inside+=darkness(lum(b.getPixel(cx+Math.round(cs[k]*r*.45f),cy+Math.round(sn[k]*r*.45f))));
        inside/=9f;
        float rimDark=0f,outBright=0f,rimMean=0f,rimSq=0f;
        for(int k=0;k<8;k++){
            int xr=cx+Math.round(cs[k]*r*.86f),yr=cy+Math.round(sn[k]*r*.86f);int xo=cx+Math.round(cs[k]*r*1.25f),yo=cy+Math.round(sn[k]*r*1.25f);
            int lr=lum(b.getPixel(xr,yr)),lo=lum(b.getPixel(xo,yo));rimDark+=darkness(lr);outBright+=Math.max(0,lo-lr)/155f;rimMean+=lr;rimSq+=lr*lr;
        }
        rimDark/=8f;outBright/=8f;rimMean/=8f;float var=Math.max(0f,rimSq/8f-rimMean*rimMean);float symmetry=1f-Math.min(1f,(float)Math.sqrt(var)/60f);
        // Blackness dominates; circular symmetry and edge contrast reject most random dark objects.
        return inside*.52f+rimDark*.20f+outBright*.17f+symmetry*.11f;
    }

    private static float darkness(int lum){return Math.max(0f,Math.min(1f,(145f-lum)/125f));}
    private static int lum(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return (3*r+6*g+b)/10;}
    private Result frozenResult(){return new Result(lastX,lastY,lastR,lastConf,false,false,true);}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
