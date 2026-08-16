package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;

/** V8.5: stabilized circular Fyvadio tracker with jump rejection and smoothed centre/radius. */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,radius01,confidence;
        public final boolean valid,moving;
        Result(float x,float y,float r,float c,boolean v,boolean m){x01=x;y01=y;radius01=r;confidence=c;valid=v;moving=m;}
    }
    private int[] prev; private int w,h;
    private float lastX=.14f,lastY=.70f,lastR=.09f,lastConf=0f;
    private float vx=0f,vy=0f;
    private long lastSeenMs=0; private boolean seeded=false;

    public synchronized void seed(float x01,float y01){lastX=clamp(x01);lastY=clamp(y01);vx=vy=0f;lastConf=.82f;lastSeenMs=System.currentTimeMillis();seeded=true;}

    public synchronized Result track(Bitmap b){
        if(b==null)return new Result(lastX,lastY,lastR,lastConf,false,false);
        int bw=b.getWidth(),bh=b.getHeight();
        if(prev==null||bw!=w||bh!=h){w=bw;h=bh;prev=new int[w*h];fillPrev(b);return new Result(lastX,lastY,lastR,0f,false,false);}
        int[] now=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int p=b.getPixel(x,y);int r=(p>>16)&255,g=(p>>8)&255,bl=p&255;now[y*w+x]=(3*r+6*g+bl)/10;}

        float predX=clamp(lastX+vx), predY=clamp(lastY+vy);
        float search=seeded?.22f:.60f;
        int minX=Math.max(2,(int)((predX-search)*w)),maxX=Math.min(w-3,(int)((predX+search)*w));
        int minY=Math.max(2,(int)((predY-search)*h)),maxY=Math.min(h-3,(int)((predY+search)*h));
        if(!seeded){minX=2;minY=2;maxX=w-3;maxY=h-3;}

        double sx=0,sy=0,sw=0; int hits=0;
        for(int y=minY;y<=maxY;y+=2)for(int x=minX;x<=maxX;x+=2){
            int i=y*w+x, lum=now[i], motion=Math.abs(lum-prev[i]);
            int dark=Math.max(0,150-lum); if(dark<24)continue;
            double dx=x/(double)w-predX,dy=y/(double)h-predY;
            double dist=Math.sqrt(dx*dx+dy*dy);
            double spatial=seeded?Math.max(.08,1.0-dist/.24):1.0;
            double score=(dark*1.35+Math.min(80,motion)*.55)*spatial;
            if(score<36)continue; sx+=x*score;sy+=y*score;sw+=score;hits++;
        }
        if(sw>1250&&hits>8){
            float nx=(float)(sx/sw)/w, ny=(float)(sy/sw)/h;
            float jump=(float)Math.hypot(nx-predX,ny-predY);
            if(seeded && jump>.095f){
                prev=now; lastConf*=.72f;
                return new Result(lastX,lastY,lastR,lastConf,false,false);
            }
            double cx=nx*w,cy=ny*h,rr=0,rw=0;
            for(int y=minY;y<=maxY;y+=2)for(int x=minX;x<=maxX;x+=2){
                int lum=now[y*w+x],dark=Math.max(0,150-lum);if(dark<28)continue;
                double d=Math.hypot(x-cx,y-cy); if(d>Math.min(w,h)*.20)continue;
                rr+=d*dark;rw+=dark;
            }
            float radiusPx=rw>0?(float)(rr/rw*1.50):Math.min(w,h)*lastR;
            float nr=clampRange(radiusPx/Math.min(w,h),.035f,.20f);
            float dx=nx-lastX,dy=ny-lastY;
            vx=.72f*vx+.28f*dx; vy=.72f*vy+.28f*dy;
            float alpha=Math.min(.34f,.16f+.22f*clamp((float)(sw/16000.0)));
            if(!seeded){lastX=nx;lastY=ny;lastR=nr;seeded=true;}else{
                lastX=(1f-alpha)*lastX+alpha*nx;
                lastY=(1f-alpha)*lastY+alpha*ny;
                lastR=.82f*lastR+.18f*nr;
            }
            lastConf=clamp((float)(sw/15000.0));lastConf=Math.max(.38f,lastConf);lastSeenMs=System.currentTimeMillis();
            boolean moving=Math.hypot(vx,vy)>.0035;
            prev=now;return new Result(lastX,lastY,lastR,lastConf,true,moving);
        }
        prev=now;long age=System.currentTimeMillis()-lastSeenMs;
        vx*=.75f;vy*=.75f;
        if(seeded&&age<900){lastConf*=.93f;return new Result(lastX,lastY,lastR,lastConf,lastConf>.28f,false);}
        lastConf*=.76f;return new Result(lastX,lastY,lastR,lastConf,false,false);
    }
    private void fillPrev(Bitmap b){for(int y=0;y<h;y++)for(int x=0;x<w;x++){int p=b.getPixel(x,y);int r=(p>>16)&255,g=(p>>8)&255,bl=p&255;prev[y*w+x]=(3*r+6*g+bl)/10;}}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
    private static float clampRange(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
