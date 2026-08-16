package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;

/** V8.6: conservative circular probe tracker. If the probe is lost, the last trusted centre freezes. */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,radius01,confidence;
        public final boolean valid,moving,frozen;
        Result(float x,float y,float r,float c,boolean v,boolean m,boolean f){x01=x;y01=y;radius01=r;confidence=c;valid=v;moving=m;frozen=f;}
    }
    private int[] prev; private int w,h;
    private float lastX=.14f,lastY=.70f,lastR=.09f,lastConf=0f;
    private float vx=0f,vy=0f;
    private long lastSeenMs=0; private boolean seeded=false;
    private int goodStreak=0;

    public synchronized void seed(float x01,float y01){
        lastX=clamp(x01);lastY=clamp(y01);vx=vy=0f;lastConf=.86f;
        lastSeenMs=System.currentTimeMillis();seeded=true;goodStreak=2;
    }

    public synchronized Result track(Bitmap b){
        if(b==null)return frozenResult();
        int bw=b.getWidth(),bh=b.getHeight();
        if(prev==null||bw!=w||bh!=h){w=bw;h=bh;prev=new int[w*h];fillPrev(b);return frozenResult();}
        int[] now=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int p=b.getPixel(x,y),r=(p>>16)&255,g=(p>>8)&255,bl=p&255;
            now[y*w+x]=(3*r+6*g+bl)/10;
        }

        float predX=seeded?clamp(lastX+vx):lastX;
        float predY=seeded?clamp(lastY+vy):lastY;
        float search=seeded?.13f:.60f; // once locked, never hunt across the whole frame
        int minX=Math.max(2,(int)((predX-search)*w)),maxX=Math.min(w-3,(int)((predX+search)*w));
        int minY=Math.max(2,(int)((predY-search)*h)),maxY=Math.min(h-3,(int)((predY+search)*h));
        if(!seeded){minX=2;minY=2;maxX=w-3;maxY=h-3;}

        double sx=0,sy=0,sw=0; int hits=0;
        for(int y=minY;y<=maxY;y+=2)for(int x=minX;x<=maxX;x+=2){
            int i=y*w+x,lum=now[i],motion=Math.abs(lum-prev[i]);
            int dark=Math.max(0,152-lum);if(dark<26)continue;
            double dx=x/(double)w-predX,dy=y/(double)h-predY,dist=Math.sqrt(dx*dx+dy*dy);
            double spatial=seeded?Math.max(.03,1.0-dist/.14):1.0;
            double score=(dark*1.45+Math.min(70,motion)*.40)*spatial;
            if(score<40)continue;sx+=x*score;sy+=y*score;sw+=score;hits++;
        }

        if(sw>1500&&hits>10){
            float nx=(float)(sx/sw)/w,ny=(float)(sy/sw)/h;
            float jump=(float)Math.hypot(nx-predX,ny-predY);
            if(seeded&&jump>.050f){prev=now;goodStreak=0;return freezeAfterMiss();}

            double cx=nx*w,cy=ny*h,rr=0,rw=0;
            for(int y=minY;y<=maxY;y+=2)for(int x=minX;x<=maxX;x+=2){
                int lum=now[y*w+x],dark=Math.max(0,152-lum);if(dark<30)continue;
                double d=Math.hypot(x-cx,y-cy);if(d>Math.min(w,h)*.16)continue;
                rr+=d*dark;rw+=dark;
            }
            float radiusPx=rw>0?(float)(rr/rw*1.48):Math.min(w,h)*lastR;
            float nr=clampRange(radiusPx/Math.min(w,h),.035f,.18f);
            if(seeded && (nr<lastR*.58f || nr>lastR*1.58f)){prev=now;goodStreak=0;return freezeAfterMiss();}

            goodStreak++;
            if(goodStreak<2 && seeded){prev=now;return freezeAfterMiss();}

            float dx=nx-lastX,dy=ny-lastY;
            vx=.78f*vx+.22f*dx;vy=.78f*vy+.22f*dy;
            float alpha=.20f;
            if(!seeded){lastX=nx;lastY=ny;lastR=nr;seeded=true;}else{
                lastX=(1f-alpha)*lastX+alpha*nx;
                lastY=(1f-alpha)*lastY+alpha*ny;
                lastR=.86f*lastR+.14f*nr;
            }
            lastConf=clamp((float)(sw/17000.0));lastConf=Math.max(.48f,lastConf);
            lastSeenMs=System.currentTimeMillis();prev=now;
            boolean moving=Math.hypot(vx,vy)>.0028;
            return new Result(lastX,lastY,lastR,lastConf,true,moving,false);
        }

        prev=now;goodStreak=0;return freezeAfterMiss();
    }

    private Result freezeAfterMiss(){
        vx*=.45f;vy*=.45f;lastConf*=.78f;
        return new Result(lastX,lastY,lastR,lastConf,false,false,true);
    }
    private Result frozenResult(){return new Result(lastX,lastY,lastR,lastConf,false,false,true);}
    private void fillPrev(Bitmap b){for(int y=0;y<h;y++)for(int x=0;x<w;x++){int p=b.getPixel(x,y),r=(p>>16)&255,g=(p>>8)&255,bl=p&255;prev[y*w+x]=(3*r+6*g+bl)/10;}}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
    private static float clampRange(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
