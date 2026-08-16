package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;

/** V8.3 circular Fyvadio tracker: finds the dark round probe body and returns its geometric centre/radius. */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,radius01,confidence;
        public final boolean valid,moving;
        Result(float x,float y,float r,float c,boolean v,boolean m){x01=x;y01=y;radius01=r;confidence=c;valid=v;moving=m;}
    }
    private int[] prev; private int w,h;
    private float lastX=.5f,lastY=.72f,lastR=.09f,lastConf=0f;
    private long lastSeenMs=0; private boolean seeded=false;

    public synchronized void seed(float x01,float y01){lastX=clamp(x01);lastY=clamp(y01);lastConf=.75f;lastSeenMs=System.currentTimeMillis();seeded=true;}

    public synchronized Result track(Bitmap b){
        if(b==null)return new Result(lastX,lastY,lastR,lastConf,false,false);
        int bw=b.getWidth(),bh=b.getHeight();
        if(prev==null||bw!=w||bh!=h){w=bw;h=bh;prev=new int[w*h];fillPrev(b);return new Result(lastX,lastY,lastR,0f,false,false);}
        int[] now=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int p=b.getPixel(x,y);int r=(p>>16)&255,g=(p>>8)&255,bl=p&255;now[y*w+x]=(3*r+6*g+bl)/10;}

        float search=seeded?.30f:.60f;
        int minX=Math.max(2,(int)((lastX-search)*w)),maxX=Math.min(w-3,(int)((lastX+search)*w));
        int minY=Math.max(2,(int)((lastY-search)*h)),maxY=Math.min(h-3,(int)((lastY+search)*h));
        if(!seeded){minX=2;minY=2;maxX=w-3;maxY=h-3;}

        double sx=0,sy=0,sw=0; int hits=0;
        for(int y=minY;y<=maxY;y+=2)for(int x=minX;x<=maxX;x+=2){
            int i=y*w+x, lum=now[i], motion=Math.abs(lum-prev[i]);
            int dark=Math.max(0,145-lum); if(dark<22)continue;
            double dx=x/(double)w-lastX,dy=y/(double)h-lastY;
            double spatial=seeded?Math.max(.12,1.0-Math.sqrt(dx*dx+dy*dy)/.34):1.0;
            double score=(dark*1.25+Math.min(70,motion)*.65)*spatial;
            if(score<34)continue; sx+=x*score;sy+=y*score;sw+=score;hits++;
        }
        if(sw>1100&&hits>8){
            double cx=sx/sw,cy=sy/sw; double rr=0,rw=0;
            for(int y=minY;y<=maxY;y+=2)for(int x=minX;x<=maxX;x+=2){
                int lum=now[y*w+x],dark=Math.max(0,145-lum);if(dark<26)continue;
                double d=Math.hypot(x-cx,y-cy); if(d>Math.min(w,h)*.22)continue;
                rr+=d*dark;rw+=dark;
            }
            float radiusPx=rw>0?(float)(rr/rw*1.55):Math.min(w,h)*lastR;
            float nr=clampRange(radiusPx/Math.min(w,h),.035f,.22f);
            float nx=(float)(cx/w),ny=(float)(cy/h);
            if(seeded){lastX=.55f*lastX+.45f*nx;lastY=.55f*lastY+.45f*ny;lastR=.68f*lastR+.32f*nr;}else{lastX=nx;lastY=ny;lastR=nr;seeded=true;}
            lastConf=clamp((float)(sw/13500.0));lastConf=Math.max(.34f,lastConf);lastSeenMs=System.currentTimeMillis();
            boolean moving=Math.hypot(lastX-nx,lastY-ny)>.006;
            prev=now;return new Result(lastX,lastY,lastR,lastConf,true,moving);
        }
        prev=now;long age=System.currentTimeMillis()-lastSeenMs;
        if(seeded&&age<1200){lastConf*=.95f;return new Result(lastX,lastY,lastR,lastConf,lastConf>.20f,false);}
        lastConf*=.80f;return new Result(lastX,lastY,lastR,lastConf,false,false);
    }
    private void fillPrev(Bitmap b){for(int y=0;y<h;y++)for(int x=0;x<w;x++){int p=b.getPixel(x,y);int r=(p>>16)&255,g=(p>>8)&255,bl=p&255;prev[y*w+x]=(3*r+6*g+bl)/10;}}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
    private static float clampRange(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
