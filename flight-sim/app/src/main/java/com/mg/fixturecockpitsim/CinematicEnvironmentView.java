package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/**
 * AVM-14.7 spatial-continuity environment.
 * The runway is revealed only in the departure/arrival windows.
 * Mid-flight scenery is distance-linked and biomes blend instead of popping.
 */
public class CinematicEnvironmentView extends View {
    private static final float CLOUD_BASE_M=880f;
    private static final float CLOUD_TOP_M=1230f;
    private static final float BIOME_LENGTH_M=6400f;
    private static final float BIOME_BLEND_M=1700f;
    private static final int MOUNTAIN=0, LAKE=1, COAST=2, ISLANDS=3, ARID=4;
    private static final int[] ROUTE={MOUNTAIN,LAKE,COAST,ISLANDS,COAST,ARID,MOUNTAIN};

    private static volatile String sharedPhase="";
    private static volatile float sharedAltitudeM,sharedSpeedMps,sharedPitchDeg,sharedRollDeg,sharedHeadingDeg,sharedPhaseTime;
    private static volatile boolean sharedOnGround=true;

    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Path path=new Path();
    private long lastNs;
    private float clock,seaFlow,worldDistanceM;
    private boolean headingInit;
    private float lastHeadingDeg,headingTravelDeg,turnPanPx;
    private float worldAlpha,seaAlpha,cloudAlpha;

    public CinematicEnvironmentView(Context c){
        super(c);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    public static void setFlightScene(String phase,double altitude,double speed,double pitch,double heading,double phaseTime){
        sharedPhase=phase==null?"":phase;
        sharedAltitudeM=(float)Math.max(0,altitude);
        sharedSpeedMps=(float)Math.max(0,speed);
        sharedPitchDeg=(float)pitch;
        sharedHeadingDeg=(float)heading;
        sharedPhaseTime=(float)Math.max(0,phaseTime);
        sharedOnGround=altitude<=.08&&speed<35;
    }

    public static void setLiveFlightState(double altitude,double speed,double pitch,double roll,double heading,boolean onGround){
        sharedAltitudeM=(float)Math.max(0,altitude);
        sharedSpeedMps=(float)Math.max(0,speed);
        sharedPitchDeg=(float)pitch;
        sharedRollDeg=(float)roll;
        sharedHeadingDeg=(float)heading;
        sharedOnGround=onGround;
    }

    public static boolean isCinematicPhase(){return shouldCoverRunway();}

    private static boolean departureWindow(String ph){
        return ph.contains("TAXI_OUT")||ph.contains("RUNWAY_HOLD")||ph.contains("TAKEOFF_ROLL")||ph.contains("ROTATE_CLIMB");
    }

    private static boolean arrivalWindow(String ph){
        return ph.contains("APPROACH")||ph.contains("FLARE")||ph.contains("ROLLOUT")||
                ph.contains("TAXI_IN")||ph.contains("HANGAR_PARK")||ph.contains("RWY_CAPTURE")||
                ph.contains("COMPLETE");
    }

    private static boolean seaPhase(String ph){
        return ph.contains("DIVE_TO_SEA")||ph.contains("SEA_SKIM")||ph.contains("PULL_UP");
    }

    private static boolean shouldCoverRunway(){
        if(sharedPhase.contains("CRASH"))return true;
        if(departureWindow(sharedPhase)||arrivalWindow(sharedPhase))return false;
        if(sharedOnGround)return false;
        return true;
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime();
        float dt=lastNs==0?.016f:Math.min(.05f,Math.max(.001f,(now-lastNs)/1e9f));
        lastNs=now;clock+=dt;

        int w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        updateHeadingPan(dt,w);

        boolean cover=shouldCoverRunway();
        float targetWorld=cover?1f:0f;
        worldAlpha=approach(worldAlpha,targetWorld,dt*.30f);

        if(!sharedOnGround&&cover){
            float horizontal=sharedSpeedMps*(float)Math.cos(Math.toRadians(clamp(sharedPitchDeg,-70f,70f)));
            worldDistanceM+=Math.max(18f,horizontal)*dt;
            if(worldDistanceM>BIOME_LENGTH_M*ROUTE.length*100f)worldDistanceM%=BIOME_LENGTH_M*ROUTE.length;
        }else if(sharedOnGround&&departureWindow(sharedPhase)){
            worldDistanceM=0;
        }

        float flowGain=.35f+1.65f*clamp(sharedSpeedMps/260f,0,1);
        seaFlow=(seaFlow+dt*flowGain)%1f;
        float hz=h*(.33f+clamp(sharedPitchDeg/38f,-.125f,.125f));

        if(worldAlpha>.005f){
            int save=c.saveLayerAlpha(0,0,w,h,(int)(255*clamp(worldAlpha,0,1)));
            drawContinuousBiomeWorld(c,w,h,hz);
            c.restoreToCount(save);
        }

        float seaTarget=computeSeaTarget();
        seaAlpha=approach(seaAlpha,seaTarget,dt*.28f);
        if(worldAlpha>.005f&&seaAlpha>.005f){
            int save=c.saveLayerAlpha(0,0,w,h,(int)(255*clamp(worldAlpha*seaAlpha,0,1)));
            drawOpenOcean(c,w,h,hz);
            c.restoreToCount(save);
        }

        float coverage=WeatherEffectsView.getSharedCloudLayerCoverage();
        boolean cloudLayer=WeatherEffectsView.hasSharedCloudLayer()&&coverage>=.36f;
        float cloudTarget=cloudLayer&&sharedAltitudeM>CLOUD_BASE_M-90f?clamp((coverage-.28f)/.72f,0f,1f):0f;
        cloudAlpha=approach(cloudAlpha,cloudTarget,dt*.32f);

        if(worldAlpha>.005f&&cloudAlpha>.005f){
            int save=c.saveLayerAlpha(0,0,w,h,(int)(255*clamp(worldAlpha*cloudAlpha,0,1)));
            if(sharedAltitudeM>=CLOUD_TOP_M){
                drawCloudOcean(c,w,h,hz,1f);
            }else{
                float inside=clamp((sharedAltitudeM-CLOUD_BASE_M)/(CLOUD_TOP_M-CLOUD_BASE_M),0f,1f);
                drawCloudInterior(c,w,h,hz,inside,coverage);
            }
            c.restoreToCount(save);
        }

        if(worldAlpha>.005f||targetWorld>0||seaAlpha>.005f||cloudAlpha>.005f)postInvalidateOnAnimation();
    }

    private float computeSeaTarget(){
        if(sharedPhase.contains("SEA_SKIM"))return 1f;
        if(sharedPhase.contains("DIVE_TO_SEA"))return clamp((1000f-sharedAltitudeM)/820f,0f,1f);
        if(sharedPhase.contains("PULL_UP"))return clamp(1f-(sharedAltitudeM-170f)/760f,0f,1f);
        return 0f;
    }

    private void updateHeadingPan(float dt,int w){
        if(!headingInit){lastHeadingDeg=sharedHeadingDeg;headingInit=true;return;}
        float d=angleDelta(sharedHeadingDeg,lastHeadingDeg);lastHeadingDeg=sharedHeadingDeg;
        headingTravelDeg-=d;
        if(Math.abs(headingTravelDeg)>100000f)headingTravelDeg%=360f;
        float rate=d/Math.max(.004f,dt);
        float target=-clamp(rate/34f,-1f,1f)*w*.095f;
        turnPanPx+=(target-turnPanPx)*Math.min(1f,dt*5.5f);
    }

    private float headingPan(int w,float depth){return headingTravelDeg*(w/78f)*depth+turnPanPx*(.45f+.55f*depth);}

    private void drawContinuousBiomeWorld(Canvas c,int w,int h,float hz){
        float routeLength=BIOME_LENGTH_M*ROUTE.length;
        float d=worldDistanceM%routeLength;if(d<0)d+=routeLength;
        int slot=(int)(d/BIOME_LENGTH_M);
        float local=d-slot*BIOME_LENGTH_M;
        float blendStart=BIOME_LENGTH_M-BIOME_BLEND_M;
        float mix=smoothstep(blendStart,BIOME_LENGTH_M,local);
        int current=ROUTE[slot%ROUTE.length];
        int next=ROUTE[(slot+1)%ROUTE.length];
        drawBiome(c,w,h,hz,current,local);
        if(mix>.002f){
            int save=c.saveLayerAlpha(0,0,w,h,(int)(255*mix));
            drawBiome(c,w,h,hz,next,local-BIOME_LENGTH_M);
            c.restoreToCount(save);
        }
    }

    private void drawBiome(Canvas c,int w,int h,float hz,int biome,float local){
        drawBaseSky(c,w,h,hz,biome);
        switch(biome){
            case LAKE: drawLakeCountry(c,w,h,hz,local);break;
            case COAST: drawCoast(c,w,h,hz,local);break;
            case ISLANDS: drawIslands(c,w,h,hz,local);break;
            case ARID: drawArid(c,w,h,hz,local);break;
            default: drawMountainValley(c,w,h,hz,local);break;
        }
    }

    private void drawBaseSky(Canvas c,int w,int h,float hz,int biome){
        int top=biome==ARID?0xff2479a7:0xff176a9c;
        int mid=biome==ARID?0xff63b8d3:0xff43acd0;
        int low=biome==ARID?0xffe0dcc4:0xffc8e9ef;
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{top,mid,low},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,hz,p);p.setShader(null);
    }

    private void drawMountainValley(Canvas c,int w,int h,float hz,float local){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff829c69,0xff5f7f59,0xff3f6248},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,h,p);p.setShader(null);
        float pan=headingPan(w,.22f)+worldDistanceM*.012f;
        drawMountainLayer(c,w,h,hz,pan,.17f,0xff6b8968,1.2f);
        drawMountainLayer(c,w,h,hz,pan*.72f,.115f,0xff4d7359,2.0f);
        float gh=h-hz;
        for(int i=0;i<34;i++){
            float q=.08f+((i*23)%33)/36f;
            float y=hz+gh*q*q;
            float x=wrap(i*117f-worldDistanceM*(.020f+.055f*q)+headingPan(w,.35f+.55f*q),w+w*.12f)-w*.06f;
            float ww=w*(.012f+.043f*q);
            p.setColor(i%2==0?0x36577f43:0x2f3f6c3a);
            c.drawOval(x,y,x+ww,y+h*.006f+gh*.008f*q,p);
        }
    }

    private void drawMountainLayer(Canvas c,int w,int h,float hz,float pan,float scale,int color,float freq){
        path.reset();path.moveTo(0,hz);
        for(int i=0;i<=18;i++){
            float x=w*i/18f;
            float n=.42f+.58f*Math.abs((float)Math.sin(i*freq+pan/w*2.2f));
            path.lineTo(x,hz-h*scale*n);
        }
        path.lineTo(w,hz);path.close();p.setColor(color);c.drawPath(path,p);
    }

    private void drawLakeCountry(Canvas c,int w,int h,float hz,float local){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff829d6b,0xff64845b,0xff45694d},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,h,p);p.setShader(null);
        drawMountainLayer(c,w,h,hz,headingPan(w,.18f),.10f,0xff58775f,1.6f);
        float pan=headingPan(w,.55f)-worldDistanceM*.026f;
        drawLake(c,w,h,wrap(w*.30f+pan,w*1.2f)-w*.1f,hz+(h-hz)*.40f,w*.25f,(h-hz)*.21f,17);
        drawLake(c,w,h,wrap(w*.78f+pan*.73f,w*1.25f)-w*.1f,hz+(h-hz)*.70f,w*.22f,(h-hz)*.18f,31);
    }

    private void drawLake(Canvas c,int w,int h,float cx,float cy,float rx,float ry,int seed){
        Path lake=irregularPath(cx,cy,rx,ry,seed,32);
        p.setShader(new LinearGradient(0,cy-ry,0,cy+ry,new int[]{0xff55c7d9,0xff258db5,0xff145f91},null,Shader.TileMode.CLAMP));
        c.drawPath(lake,p);p.setShader(null);
        stroke.setColor(0x9ad9d4a2);stroke.setStrokeWidth(Math.max(1f,w*.0018f));c.drawPath(lake,stroke);
        int save=c.save();c.clipPath(lake);drawWaterTexture(c,w,cx,cy,rx,ry,false);drawReflection(c,w,h,cy-rx*.02f);c.restoreToCount(save);
    }

    private void drawCoast(Canvas c,int w,int h,float hz,float local){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff58c6dc,0xff258fb8,0xff125b86},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,h,p);p.setShader(null);
        drawWaterTexture(c,w,w*.66f,hz+(h-hz)*.55f,w*.55f,(h-hz)*.50f,true);
        float pan=headingPan(w,.42f)-worldDistanceM*.010f;
        float edge=w*(.40f+.08f*(float)Math.sin(worldDistanceM*.00045f))+pan*.10f;
        path.reset();path.moveTo(0,hz);path.lineTo(edge,hz+h*.03f);path.lineTo(edge+w*.08f,hz+h*.14f);
        path.lineTo(edge-w*.04f,hz+h*.25f);path.lineTo(edge+w*.05f,hz+h*.40f);path.lineTo(edge-w*.08f,h);
        path.lineTo(0,h);path.close();
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff829763,0xff5d7e54,0xff3f6748},null,Shader.TileMode.CLAMP));
        c.drawPath(path,p);p.setShader(null);
        stroke.setColor(0xbad8c99a);stroke.setStrokeWidth(Math.max(2f,w*.003f));c.drawPath(path,stroke);
        drawReflection(c,w,h,hz);
    }

    private void drawIslands(Canvas c,int w,int h,float hz,float local){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff59c7dc,0xff238eb9,0xff105681},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,h,p);p.setShader(null);
        drawWaterTexture(c,w,w*.5f,hz+(h-hz)*.55f,w*.62f,(h-hz)*.56f,true);
        float pan=headingPan(w,.48f)-worldDistanceM*.022f;
        float[][] a={{.16f,.28f,.095f,.060f},{.43f,.23f,.13f,.075f},{.75f,.34f,.11f,.070f},{.86f,.64f,.085f,.055f},{.56f,.72f,.12f,.070f},{.25f,.79f,.10f,.060f}};
        for(int i=0;i<a.length;i++){
            float cx=wrap(w*a[i][0]+pan*(.45f+.08f*i),w*1.25f)-w*.12f;
            float cy=hz+(h-hz)*a[i][1],rx=w*a[i][2],ry=(h-hz)*a[i][3];
            Path sand=irregularPath(cx,cy,rx,ry,70+i*13,28);
            Path land=irregularPath(cx,cy,rx*.82f,ry*.78f,94+i*17,28);
            p.setColor(0xffdccb96);c.drawPath(sand,p);
            p.setShader(new LinearGradient(0,cy-ry,0,cy+ry,new int[]{0xff699b5c,0xff426f49},null,Shader.TileMode.CLAMP));
            c.drawPath(land,p);p.setShader(null);
        }
        drawReflection(c,w,h,hz);
    }

    private void drawArid(Canvas c,int w,int h,float hz,float local){
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xffd9ba78,0xffc99655,0xffa86f3f},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,h,p);p.setShader(null);
        float pan=headingPan(w,.35f)-worldDistanceM*.010f;
        for(int b=0;b<8;b++){
            path.reset();float yy=hz+(h-hz)*(.12f+b*.115f);path.moveTo(0,yy);
            for(int i=0;i<=12;i++){
                float x=w*i/12f;
                float y=yy+(float)Math.sin(i*.92+b+pan/w*2.5f)*h*(.016f+.004f*b);
                path.lineTo(x,y);
            }
            path.lineTo(w,h);path.lineTo(0,h);path.close();p.setColor(b%2==0?0x30f2cf8b:0x28a96539);c.drawPath(path,p);
        }
    }

    private void drawCloudOcean(Canvas c,int w,int h,float hz,float alpha){
        int a=(int)(255*clamp(alpha,0,1));
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{argb(a,0x176da4),argb(a,0x4bb4d7),argb(a,0xc9edf5)},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,hz,p);p.setShader(null);
        p.setColor(argb((int)(238*alpha),0xeef6f7));c.drawRect(0,hz,w,h,p);
        p.setShader(new LinearGradient(0,hz,0,hz+h*.20f,new int[]{argb((int)(235*alpha),0xffffff),argb((int)(95*alpha),0xc5d1d5),0x00ffffff},null,Shader.TileMode.CLAMP));
        c.drawRect(0,hz,w,hz+h*.23f,p);p.setShader(null);
        float speedShift=clock*(10f+sharedSpeedMps*.18f);
        for(int row=0;row<11;row++){
            float q=(row+.55f)/11f,z=q*q,y=hz+(h-hz)*(.035f+.94f*z);
            int cols=10+row*2;float cell=w/(float)cols,r=cell*(.48f+.42f*q);
            float yaw=headingPan(w,.18f+.82f*q);
            float rowShift=(speedShift*(.15f+q*.95f)+(row&1)*cell*.45f+yaw)%cell;
            for(int i=-2;i<=cols+1;i++){
                float x=i*cell+rowShift,wob=(float)Math.sin(i*1.91+row*2.37+clock*.28f)*r*.22f,cy=y+wob*.18f;
                int shadow=(int)((24+58*q)*alpha);p.setColor(argb(shadow,0x71818a));c.drawOval(x-r*1.05f,cy-r*.14f,x+r*1.05f,cy+r*.36f,p);
                int white=(int)((190+60*(1-q))*alpha);p.setColor(argb(white,0xf8fbfc));
                c.drawOval(x-r*.95f,cy-r*.43f,x+r*.90f,cy+r*.26f,p);c.drawOval(x-r*.48f,cy-r*.72f,x+r*.20f,cy+r*.18f,p);c.drawOval(x+r*.02f,cy-r*.60f,x+r*.70f,cy+r*.20f,p);
            }
        }
    }

    private void drawCloudInterior(Canvas c,int w,int h,float hz,float position01,float coverage){
        float bright=.72f+.20f*position01;
        int top=rgb((int)(205+30*bright),(int)(215+27*bright),(int)(219+28*bright));
        int bot=rgb((int)(226+20*bright),(int)(231+19*bright),(int)(232+20*bright));
        p.setShader(new LinearGradient(0,0,0,h,new int[]{top,0xffeef2f2,bot},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        float pan=headingPan(w,.8f);int blobs=12+(int)(12*coverage);
        for(int i=0;i<blobs;i++){
            float q=(i+.5f)/Math.max(1f,blobs),x=wrap(i*91f+pan+clock*(5f+(i%3)*3f),w+w*.25f)-w*.12f;
            float y=h*(.06f+((i*37)%83)/100f),r=w*(.035f+.075f*q);
            int aa=(int)((35+75*(1f-Math.abs(position01-.5f)))*coverage);p.setColor(argb(aa,i%2==0?0xffffff:0xaab7bb));c.drawOval(x-r,y-r*.38f,x+r,y+r*.38f,p);
        }
        p.setColor(argb((int)(66*coverage),0xffffff));c.drawRect(0,hz-h*.05f,w,hz+h*.08f,p);
    }

    private void drawOpenOcean(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{0xff135f92,0xff49add2,0xffc7e9f0},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,hz,p);p.setShader(null);
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff2a8eae,0xff126584,0xff073d59,0xff032c43},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        p.setColor(0x8ad9f2f4);c.drawRect(0,hz,w,hz+Math.max(2,h*.006f),p);
        float low=clamp((120f-sharedAltitudeM)/120f,0,1),spd=clamp(sharedSpeedMps/270f,0,1),waveAmp=.45f+.75f*WeatherEffectsView.getSharedWindStrength();
        drawDistantOceanClouds(c,w,h,hz);
        for(int i=0;i<76;i++){
            float q=(i/76f+seaFlow*(.34f+spd*.90f))%1f,z=q*q,y=hz+(h-hz)*z;
            float seg=w*(.012f+.075f*z)*(1f+.55f*spd),period=w+seg,yaw=headingPan(w,.20f+.80f*z);
            float x=wrap(i*97.3f+clock*(12f+sharedSpeedMps*.36f)*(i%2==0?1f:-.45f)+yaw,period)-seg*.5f;
            float dy=(float)Math.sin(i*1.73+clock*(1.8f+waveAmp))*h*(.0005f+.0032f*z)*waveAmp;
            int aa=(int)(30+88*z+45*low*z);stroke.setColor((Math.min(175,aa)<<24)|0x00bfeefa);stroke.setStrokeWidth(Math.max(1f,w*(.00045f+.0020f*z)));
            path.reset();path.moveTo(x,y+dy);path.quadTo(x+seg*.34f,y-dy*.55f,x+seg*.66f,y+dy*.22f);path.quadTo(x+seg*.84f,y-dy*.42f,x+seg,y+dy*.05f);c.drawPath(path,stroke);
        }
        int caps=18+(int)(28*WeatherEffectsView.getSharedWindStrength());
        for(int i=0;i<caps;i++){
            float q=((i*.071f+seaFlow*.71f)%1f),z=.16f+.84f*q*q,y=hz+(h-hz)*z,period=w+w*.12f;
            float x=wrap(i*131f+clock*sharedSpeedMps*.25f+headingPan(w,.30f+.70f*z),period)-w*.06f,len=w*(.006f+.032f*z)*(1f+.65f*low);
            stroke.setColor((int)(0x38+0x78*z)<<24|0x00ffffff);stroke.setStrokeWidth(Math.max(1f,w*(.0007f+.0022f*z)));c.drawLine(x,y,x+len,y+h*.0025f*z,stroke);
        }
        drawReflection(c,w,h,hz);
    }

    private void drawDistantOceanClouds(Canvas c,int w,int h,float hz){
        float coverage=WeatherEffectsView.getSharedCloudLayerCoverage();if(coverage<.10f)return;
        float pan=headingPan(w,.18f);int count=2+(int)(7*coverage);
        for(int i=0;i<count;i++){
            float period=w+w*.22f,x=wrap(i*w*.19f+pan*.28f,period)-w*.11f,y=hz-h*(.018f+.008f*(i%3)),ww=w*(.055f+.018f*(i%2));
            p.setColor(argb((int)(55+70*coverage),0xe8f0f2));c.drawOval(x,y,x+ww,y+h*.014f,p);c.drawOval(x+ww*.22f,y-h*.010f,x+ww*.65f,y+h*.012f,p);
        }
    }

    private void drawWaterTexture(Canvas c,int w,float cx,float cy,float rx,float ry,boolean broad){
        int n=broad?46:26;
        for(int i=0;i<n;i++){
            float q=(i+.5f)/n,yy=cy-ry+wrap(i*.137f+seaFlow*.18f,1f)*ry*2f;
            float x=wrap(cx-rx*.85f+i*83.3f-worldDistanceM*(.012f+.025f*q)+headingPan(w,.30f+.60f*q),Math.max(1f,rx*1.7f))+cx-rx*.85f;
            float len=Math.max(w*.008f,rx*(.02f+.04f*((i%7)/7f)));stroke.setColor(i%5==0?0x42d8f6ff:0x25a8dbe8);stroke.setStrokeWidth(Math.max(1f,w*.0007f));c.drawLine(x,yy,Math.min(cx+rx*.90f,x+len),yy,stroke);
        }
    }

    private void drawReflection(Canvas c,int w,int h,float hz){
        int mode=WeatherEffectsView.getSharedCelestialMode();float strength=WeatherEffectsView.getSharedCelestialStrength();if(mode==0||strength<.05f)return;
        float x=w*WeatherEffectsView.getSharedCelestialX01()+turnPanPx*.12f;int rgb=mode==1?0xffe6a3:0xe6f2ff;
        for(int i=0;i<32;i++){
            float q=i/31f,y=hz+(h-hz)*(.08f+.78f*q),spread=w*(.008f+.075f*q)*strength,wander=(float)Math.sin(i*2.2+clock*.65f)*spread*.45f,len=spread*(.55f+.50f*(float)Math.sin(i*.87+1.1));
            int alpha=(int)((20+80*q)*strength);stroke.setColor((Math.min(125,alpha)<<24)|rgb);stroke.setStrokeWidth(Math.max(1f,w*(.0006f+.0014f*q)));c.drawLine(x-len*.5f+wander,y,x+len*.5f+wander,y,stroke);
        }
    }

    private Path irregularPath(float cx,float cy,float rx,float ry,int seed,int pts){
        Path q=new Path();
        for(int i=0;i<pts;i++){
            double a=Math.PI*2*i/pts;float wob=(float)(1+.14*Math.sin(i*1.71+seed*.37)+.07*Math.sin(i*3.19+seed*.11));
            float x=cx+(float)Math.cos(a)*rx*wob,y=cy+(float)Math.sin(a)*ry*(1+.11f*(float)Math.sin(i*1.39+seed));if(i==0)q.moveTo(x,y);else q.lineTo(x,y);
        }
        q.close();return q;
    }

    private static float approach(float v,float target,float delta){if(v<target)return Math.min(target,v+delta);return Math.max(target,v-delta);}
    private static float smoothstep(float a,float b,float x){float t=clamp((x-a)/Math.max(.0001f,b-a),0f,1f);return t*t*(3f-2f*t);}
    private static float angleDelta(float current,float previous){float d=current-previous;while(d>180)d-=360;while(d<-180)d+=360;return d;}
    private static float wrap(float v,float period){if(period<=0)return v;v%=period;if(v<0)v+=period;return v;}
    private static int rgb(int r,int g,int b){return 0xff000000|(Math.max(0,Math.min(255,r))<<16)|(Math.max(0,Math.min(255,g))<<8)|Math.max(0,Math.min(255,b));}
    private static int argb(int a,int rgb){return (Math.max(0,Math.min(255,a))<<24)|(rgb&0x00ffffff);}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
