package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;

/** Lightweight camera tracker for the moving dark Fyvadio probe. Uses frame-to-frame motion + darkness, no external CV library. */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,confidence;
        public final boolean valid,moving;
        Result(float x,float y,float c,boolean v,boolean m){x01=x;y01=y;confidence=c;valid=v;moving=m;}
    }
    private int[] prev;
    private int w,h;
    private float lastX=.5f,lastY=.72f,lastConf=0f;
    private long lastSeenMs=0;
    private boolean seeded=false;

    public synchronized void seed(float x01,float y01){lastX=clamp(x01);lastY=clamp(y01);lastConf=.75f;lastSeenMs=System.currentTimeMillis();seeded=true;}

    public synchronized Result track(Bitmap b){
        if(b==null)return new Result(lastX,lastY,lastConf,false,false);
        int bw=b.getWidth(),bh=b.getHeight();
        if(prev==null||bw!=w||bh!=h){w=bw;h=bh;prev=new int[w*h];fillPrev(b);return new Result(lastX,lastY,0f,false,false);}
        double sx=0,sy=0,sw=0;int hits=0;
        float radius=seeded?.38f:.70f;
        int minX=Math.max(1,(int)((lastX-radius)*w)),maxX=Math.min(w-2,(int)((lastX+radius)*w));
        int minY=Math.max(1,(int)((lastY-radius)*h)),maxY=Math.min(h-2,(int)((lastY+radius)*h));
        if(!seeded){minX=1;minY=1;maxX=w-2;maxY=h-2;}
        int[] now=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int p=b.getPixel(x,y);int r=(p>>16)&255,g=(p>>8)&255,bl=p&255;now[y*w+x]=(3*r+6*g+bl)/10;
        }
        for(int y=minY;y<=maxY;y+=2)for(int x=minX;x<=maxX;x+=2){
            int i=y*w+x;int lum=now[i],d=Math.abs(lum-prev[i]);
            int darkness=Math.max(0,150-lum);
            if(d<16||darkness<18)continue;
            double spatial=1.0;
            if(seeded){double dx=x/(double)w-lastX,dy=y/(double)h-lastY;double rr=Math.sqrt(dx*dx+dy*dy);spatial=Math.max(.15,1.0-rr/.42);}
            double score=(d*1.8+darkness*.9)*spatial;
            if(score<45)continue;
            sx+=x*score;sy+=y*score;sw+=score;hits++;
        }
        prev=now;
        boolean moving=sw>1800&&hits>5;
        if(moving){
            float nx=(float)(sx/sw)/w,ny=(float)(sy/sw)/h;
            if(seeded){lastX=.62f*lastX+.38f*nx;lastY=.62f*lastY+.38f*ny;}else{lastX=nx;lastY=ny;seeded=true;}
            lastConf=clamp((float)(sw/18000.0));lastConf=Math.max(.28f,lastConf);lastSeenMs=System.currentTimeMillis();
            return new Result(lastX,lastY,lastConf,true,true);
        }
        long age=System.currentTimeMillis()-lastSeenMs;
        if(seeded&&age<1400){lastConf*=.96f;return new Result(lastX,lastY,lastConf,lastConf>.18f,false);}
        lastConf*=.82f;return new Result(lastX,lastY,lastConf,false,false);
    }

    private void fillPrev(Bitmap b){for(int y=0;y<h;y++)for(int x=0;x<w;x++){int p=b.getPixel(x,y);int r=(p>>16)&255,g=(p>>8)&255,bl=p&255;prev[y*w+x]=(3*r+6*g+bl)/10;}}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
