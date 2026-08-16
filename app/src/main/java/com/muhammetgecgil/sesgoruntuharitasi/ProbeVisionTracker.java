package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;

/** V8.9: very fast tracker specialized for the black circular Fyvadio probe.
 * Uses blackness + circular radial symmetry + temporal prediction. If confidence is lost,
 * the last trusted centre is frozen and acoustic samples must stop.
 */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,radius01,confidence;
        public final boolean valid,moving,frozen;
        Result(float x,float y,float r,float c,boolean v,boolean m,boolean f){x01=x;y01=y;radius01=r;confidence=c;valid=v;moving=m;frozen=f;}
    }

    private float lastX=.14f,lastY=.70f,lastR=.075f,lastConf=0f,vx=0f,vy=0f;
    private boolean seeded=false;
    private int missCount=0,goodStreak=0;

    public synchronized void seed(float x01,float y01){
        lastX=clamp(x01);lastY=clamp(y01);vx=vy=0f;lastConf=.92f;seeded=true;missCount=0;goodStreak=2;
    }

    public synchronized Result track(Bitmap b){
        if(b==null)return frozenResult();
        final int w=b.getWidth(),h=b.getHeight();
        if(w<60||h<80)return frozenResult();

        // Predict where the probe should be. While locked, search a small window for speed.
        float speed=(float)Math.hypot(vx,vy);
        float px=seeded?clamp(lastX+vx*1.25f):.5f;
        float py=seeded?clamp(lastY+vy*1.25f):.55f;
        float search=seeded?Math.min(.18f,.070f+speed*4.2f+missCount*.014f):.62f;
        int x0=seeded?Math.max(8,(int)((px-search)*w)):8;
        int x1=seeded?Math.min(w-9,(int)((px+search)*w)):w-9;
        int y0=seeded?Math.max(8,(int)((py-search)*h)):8;
        int y1=seeded?Math.min(h-9,(int)((py+search)*h)):h-9;

        int minR=Math.max(5,(int)(Math.min(w,h)*(seeded?Math.max(.035f,lastR*.58f):.035f)));
        int maxR=Math.max(minR+2,(int)(Math.min(w,h)*(seeded?Math.min(.16f,lastR*1.60f):.15f)));

        float best=-99f,second=-99f;int bestX=-1,bestY=-1,bestR=minR;
        // Coarse pass. Step 2 is fast at 160x284 and still precise enough.
        for(int y=y0;y<=y1;y+=2){
            for(int x=x0;x<=x1;x+=2){
                int centerLum=lum(b.getPixel(x,y));
                if(centerLum>105)continue; // microphone body is black
                for(int r=minR;r<=maxR;r+=Math.max(2,(maxR-minR)/4)){
                    float s=circleScore(b,x,y,r,w,h);
                    if(seeded){
                        float dx=x/(float)w-px,dy=y/(float)h-py;
                        s-=Math.min(.30f,(float)Math.hypot(dx,dy)*1.15f);
                        float rr=r/(float)Math.min(w,h);
                        s-=Math.min(.20f,Math.abs(rr-lastR)*2.2f);
                    }
                    if(s>best){second=best;best=s;bestX=x;bestY=y;bestR=r;}else if(s>second)second=s;
                }
            }
        }

        // Fine pass around the strongest candidate, pixel precision.
        if(bestX>=0){
            int fx=bestX,fy=bestY,fr=bestR;
            for(int y=Math.max(7,fy-3);y<=Math.min(h-8,fy+3);y++)for(int x=Math.max(7,fx-3);x<=Math.min(w-8,fx+3);x++){
                for(int r=Math.max(5,fr-2);r<=Math.min(maxR,fr+2);r++){
                    float s=circleScore(b,x,y,r,w,h);
                    if(seeded){float dx=x/(float)w-px,dy=y/(float)h-py;s-=Math.min(.30f,(float)Math.hypot(dx,dy)*1.15f);}
                    if(s>best){second=best;best=s;bestX=x;bestY=y;bestR=r;}
                }
            }
        }

        float uniqueness=best-second;
        // Unseeded global acquisition requires a very strong black-circle signature.
        float threshold=seeded?.50f:.67f;
        boolean accepted=bestX>=0&&best>threshold&&(best>.61f||uniqueness>.015f);
        if(!accepted){return miss();}

        float nx=bestX/(float)w,ny=bestY/(float)h,nr=bestR/(float)Math.min(w,h);
        if(seeded){
            float jump=(float)Math.hypot(nx-px,ny-py);
            float maxJump=Math.min(.15f,.052f+speed*3.8f+missCount*.010f);
            if(jump>maxJump)return miss();
            if(nr<lastR*.55f||nr>lastR*1.65f)return miss();
        }

        goodStreak++;
        if(seeded&&missCount>0&&goodStreak<2)return miss(); // require two compatible frames after loss

        float dx=nx-lastX,dy=ny-lastY;
        vx=.34f*vx+.66f*dx;vy=.34f*vy+.66f*dy;
        float alpha=speed>.008f?.90f:.78f; // very low lag
        if(!seeded){lastX=nx;lastY=ny;lastR=nr;seeded=true;}
        else{lastX=clamp(lastX*(1f-alpha)+nx*alpha);lastY=clamp(lastY*(1f-alpha)+ny*alpha);lastR=.72f*lastR+.28f*nr;}
        lastConf=clamp(.48f+(best-.48f)*1.55f+Math.min(.10f,uniqueness*2.0f));
        missCount=0;goodStreak=Math.min(4,goodStreak);
        boolean moving=Math.hypot(vx,vy)>.0015;
        return new Result(lastX,lastY,lastR,lastConf,true,moving,false);
    }

    private Result miss(){
        missCount=Math.min(10,missCount+1);goodStreak=0;vx*=.50f;vy*=.50f;lastConf*=.76f;
        return new Result(lastX,lastY,lastR,lastConf,false,false,true);
    }

    private float circleScore(Bitmap b,int cx,int cy,int r,int w,int h){
        if(cx-r-3<0||cy-r-3<0||cx+r+3>=w||cy+r+3>=h)return -9f;
        // Black interior: centre + 8 points at 0.45R.
        float inside=0f;int n=0;
        inside+=darkness(lum(b.getPixel(cx,cy)));n++;
        final float[] cs={1f,0.7071f,0f,-0.7071f,-1f,-0.7071f,0f,0.7071f};
        final float[] sn={0f,0.7071f,1f,0.7071f,0f,-0.7071f,-1f,-0.7071f};
        for(int k=0;k<8;k++){int x=cx+Math.round(cs[k]*r*.45f),y=cy+Math.round(sn[k]*r*.45f);inside+=darkness(lum(b.getPixel(x,y)));n++;}
        inside/=n;

        // A circular body should have similar luminance around its rim. Outside is usually brighter.
        float rimDark=0f,outBright=0f;float rimMean=0f,rimSq=0f;
        for(int k=0;k<8;k++){
            int xr=cx+Math.round(cs[k]*r*.86f),yr=cy+Math.round(sn[k]*r*.86f);
            int xo=cx+Math.round(cs[k]*r*1.25f),yo=cy+Math.round(sn[k]*r*1.25f);
            int lr=lum(b.getPixel(xr,yr)),lo=lum(b.getPixel(xo,yo));
            rimDark+=darkness(lr);outBright+=Math.max(0,lo-lr)/155f;rimMean+=lr;rimSq+=lr*lr;
        }
        rimDark/=8f;outBright/=8f;rimMean/=8f;
        float var=Math.max(0f,rimSq/8f-rimMean*rimMean);
        float symmetry=1f-Math.min(1f,(float)Math.sqrt(var)/60f);
        return inside*.50f+rimDark*.20f+outBright*.18f+symmetry*.12f;
    }

    private static float darkness(int lum){return Math.max(0f,Math.min(1f,(145f-lum)/125f));}
    private static int lum(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return (3*r+6*g+b)/10;}
    private Result frozenResult(){return new Result(lastX,lastY,lastR,lastConf,false,false,true);}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
