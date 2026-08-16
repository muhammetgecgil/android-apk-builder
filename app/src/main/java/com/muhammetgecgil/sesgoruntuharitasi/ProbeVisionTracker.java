package com.muhammetgecgil.sesgoruntuharitasi;

import android.graphics.Bitmap;

/** V8.9: very fast appearance tracker. Learns microphone image after seed; no circle detection is required. */
public final class ProbeVisionTracker {
    public static final class Result {
        public final float x01,y01,radius01,confidence;
        public final boolean valid,moving,frozen;
        Result(float x,float y,float r,float c,boolean v,boolean m,boolean f){x01=x;y01=y;radius01=r;confidence=c;valid=v;moving=m;frozen=f;}
    }

    private int w,h;
    private float lastX=.14f,lastY=.70f,lastR=.075f,lastConf=0f,vx=0f,vy=0f;
    private boolean seeded=false,templateReady=false;
    private final int TS=15;
    private final int[] tpl=new int[15*15];
    private float tplMean=0f,tplStd=1f;
    private int missCount=0;

    public synchronized void seed(float x01,float y01){
        lastX=clamp(x01);lastY=clamp(y01);vx=vy=0f;lastConf=.90f;seeded=true;templateReady=false;missCount=0;
    }

    public synchronized Result track(Bitmap b){
        if(b==null||!seeded)return frozenResult();
        w=b.getWidth();h=b.getHeight();
        if(w<40||h<60)return frozenResult();

        if(!templateReady){
            captureTemplate(b,lastX,lastY);
            if(!templateReady)return frozenResult();
            return new Result(lastX,lastY,lastR,.92f,true,false,false);
        }

        float speed=(float)Math.hypot(vx,vy);
        float predX=clamp(lastX+vx*1.35f),predY=clamp(lastY+vy*1.35f);
        float search=Math.min(.22f,.075f+speed*5.5f+missCount*.018f);
        int cx=(int)(predX*w),cy=(int)(predY*h);
        int rx=Math.max(12,(int)(search*w)),ry=Math.max(14,(int)(search*h));

        int bestX=-1,bestY=-1;float best=-9f,second=-9f;
        int step=2;
        int half=TS/2;
        for(int y=Math.max(half,cy-ry);y<=Math.min(h-half-1,cy+ry);y+=step){
            for(int x=Math.max(half,cx-rx);x<=Math.min(w-half-1,cx+rx);x+=step){
                float s=scorePatch(b,x,y);
                float dx=x/(float)w-predX,dy=y/(float)h-predY;
                float d=(float)Math.sqrt(dx*dx+dy*dy);
                s-=Math.min(.22f,d*.55f);
                if(s>best){second=best;best=s;bestX=x;bestY=y;}else if(s>second)second=s;
            }
        }

        float uniqueness=best-second;
        boolean accepted=bestX>=0&&best>.53f&&(best>.60f||uniqueness>.018f);
        if(!accepted){
            missCount=Math.min(8,missCount+1);vx*=.55f;vy*=.55f;lastConf*=.76f;
            return new Result(lastX,lastY,lastR,lastConf,false,false,true);
        }

        float nx=bestX/(float)w,ny=bestY/(float)h;
        float jump=(float)Math.hypot(nx-predX,ny-predY);
        float maxJump=Math.min(.17f,.065f+speed*4f+missCount*.012f);
        if(jump>maxJump){
            missCount=Math.min(8,missCount+1);vx*=.60f;vy*=.60f;lastConf*=.80f;
            return new Result(lastX,lastY,lastR,lastConf,false,false,true);
        }

        float dx=nx-lastX,dy=ny-lastY;
        vx=.48f*vx+.52f*dx;vy=.48f*vy+.52f*dy;
        float alpha=speed>.010f?.78f:.62f;
        lastX=clamp(lastX*(1f-alpha)+nx*alpha);
        lastY=clamp(lastY*(1f-alpha)+ny*alpha);
        lastConf=clamp(.48f+(best-.50f)*1.65f+Math.min(.12f,uniqueness*2f));
        missCount=0;

        if(best>.70f&&speed<.020f)adaptTemplate(b,bestX,bestY,.035f);
        boolean moving=Math.hypot(vx,vy)>.0018;
        return new Result(lastX,lastY,lastR,lastConf,true,moving,false);
    }

    private void captureTemplate(Bitmap b,float x01,float y01){
        int cx=(int)(clamp(x01)*w),cy=(int)(clamp(y01)*h),half=TS/2;
        if(cx-half<0||cy-half<0||cx+half>=w||cy+half>=h)return;
        float sum=0f;
        for(int yy=-half;yy<=half;yy++)for(int xx=-half;xx<=half;xx++){
            int lum=lum(b.getPixel(cx+xx,cy+yy));tpl[(yy+half)*TS+(xx+half)]=lum;sum+=lum;
        }
        tplMean=sum/(TS*TS);float ss=0f;for(int v:tpl){float d=v-tplMean;ss+=d*d;}tplStd=(float)Math.sqrt(ss/(TS*TS));
        templateReady=tplStd>7f;
    }

    private float scorePatch(Bitmap b,int cx,int cy){
        int half=TS/2;float sum=0f,sum2=0f,cross=0f;
        int k=0;
        for(int yy=-half;yy<=half;yy++)for(int xx=-half;xx<=half;xx++){
            int v=lum(b.getPixel(cx+xx,cy+yy));sum+=v;sum2+=v*v;k++;
        }
        float mean=sum/k;float var=Math.max(1f,sum2/k-mean*mean);float std=(float)Math.sqrt(var);
        int idx=0;
        for(int yy=-half;yy<=half;yy++)for(int xx=-half;xx<=half;xx++){
            int v=lum(b.getPixel(cx+xx,cy+yy));cross+=(tpl[idx++]-tplMean)*(v-mean);
        }
        float ncc=cross/(k*Math.max(1f,tplStd*std));
        return (ncc+1f)*.5f;
    }

    private void adaptTemplate(Bitmap b,int cx,int cy,float a){
        int half=TS/2;float sum=0f;int idx=0;
        for(int yy=-half;yy<=half;yy++)for(int xx=-half;xx<=half;xx++){
            int v=lum(b.getPixel(cx+xx,cy+yy));tpl[idx]=(int)(tpl[idx]*(1f-a)+v*a);sum+=tpl[idx];idx++;
        }
        tplMean=sum/(TS*TS);float ss=0f;for(int v:tpl){float d=v-tplMean;ss+=d*d;}tplStd=(float)Math.sqrt(ss/(TS*TS));
    }

    private static int lum(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return (3*r+6*g+b)/10;}
    private Result frozenResult(){return new Result(lastX,lastY,lastR,lastConf,false,false,true);}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
