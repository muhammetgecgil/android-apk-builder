package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/**
 * AVM-14.6 game-linked environment.
 * Aircraft right turn makes the outside world flow left, and vice versa.
 * Cloud layer is weather-dependent instead of being permanently forced by altitude.
 */
public class CinematicEnvironmentView extends View {
    private static final float CLOUD_BASE_M=880f;
    private static final float CLOUD_TOP_M=1230f;

    private static volatile String sharedPhase="";
    private static volatile float sharedAltitudeM,sharedSpeedMps,sharedPitchDeg,sharedRollDeg,sharedHeadingDeg,sharedPhaseTime;
    private static volatile boolean sharedOnGround=true;

    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Path path=new Path();
    private long lastNs;
    private float clock,seaFlow;
    private boolean headingInit;
    private float lastHeadingDeg,headingTravelDeg,turnPanPx;

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
    }

    public static void setLiveFlightState(double altitude,double speed,double pitch,double roll,double heading,boolean onGround){
        sharedAltitudeM=(float)Math.max(0,altitude);
        sharedSpeedMps=(float)Math.max(0,speed);
        sharedPitchDeg=(float)pitch;
        sharedRollDeg=(float)roll;
        sharedHeadingDeg=(float)heading;
        sharedOnGround=onGround;
    }

    public static boolean isCinematicPhase(){return shouldDraw();}
    private static boolean seaPhase(String ph){
        return ph.contains("DIVE_TO_SEA")||ph.contains("SEA_SKIM")||ph.contains("PULL_UP");
    }
    private static boolean shouldDraw(){
        if(sharedOnGround)return false;
        if(seaPhase(sharedPhase))return true;
        return WeatherEffectsView.hasSharedCloudLayer()&&sharedAltitudeM>=CLOUD_BASE_M-70f;
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(!shouldDraw())return;
        long now=System.nanoTime();
        float dt=lastNs==0?.016f:Math.min(.05f,(now-lastNs)/1e9f);lastNs=now;
        clock+=dt;
        float flowGain=.35f+1.65f*clamp(sharedSpeedMps/260f,0,1);
        seaFlow=(seaFlow+dt*flowGain)%1f;
        int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        updateHeadingPan(dt,w);

        float hz=h*(.33f+clamp(sharedPitchDeg/38f,-.125f,.125f));
        boolean sea=seaPhase(sharedPhase);
        float coverage=WeatherEffectsView.getSharedCloudLayerCoverage();
        boolean cloudLayer=coverage>=.36f;

        if(cloudLayer&&sharedAltitudeM>=CLOUD_TOP_M){
            drawCloudOcean(c,w,h,hz,clamp(.58f+.48f*coverage,.62f,1f));
        }else if(cloudLayer&&sharedAltitudeM>CLOUD_BASE_M){
            float inside=clamp((sharedAltitudeM-CLOUD_BASE_M)/(CLOUD_TOP_M-CLOUD_BASE_M),0f,1f);
            drawCloudInterior(c,w,h,hz,inside,coverage);
        }else if(sea){
            drawOpenOcean(c,w,h,hz);
        }else{
            return;
        }
        postInvalidateOnAnimation();
    }

    private void updateHeadingPan(float dt,int w){
        if(!headingInit){lastHeadingDeg=sharedHeadingDeg;headingInit=true;return;}
        float d=angleDelta(sharedHeadingDeg,lastHeadingDeg);lastHeadingDeg=sharedHeadingDeg;
        // Pilot-view parallax: aircraft turns right -> outside world travels left.
        headingTravelDeg-=d;
        if(Math.abs(headingTravelDeg)>100000f)headingTravelDeg%=360f;
        float rate=d/Math.max(.004f,dt);
        float target=-clamp(rate/34f,-1f,1f)*w*.095f;
        turnPanPx+=(target-turnPanPx)*Math.min(1f,dt*5.5f);
    }

    private float headingPan(int w,float depth){
        return headingTravelDeg*(w/78f)*depth+turnPanPx*(.45f+.55f*depth);
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
            float q=(row+.55f)/11f,z=q*q;
            float y=hz+(h-hz)*(.035f+.94f*z);
            int cols=10+row*2;
            float cell=w/(float)cols,r=cell*(.48f+.42f*q);
            float yaw=headingPan(w,.18f+.82f*q);
            float rowShift=(speedShift*(.15f+q*.95f)+(row&1)*cell*.45f+yaw)%cell;
            for(int i=-2;i<=cols+1;i++){
                float x=i*cell+rowShift;
                float wob=(float)Math.sin(i*1.91+row*2.37+clock*.28f)*r*.22f;
                float cy=y+wob*.18f;
                int shadow=(int)((24+58*q)*alpha);
                p.setColor(argb(shadow,0x71818a));
                c.drawOval(x-r*1.05f,cy-r*.14f,x+r*1.05f,cy+r*.36f,p);
                int white=(int)((190+60*(1-q))*alpha);
                p.setColor(argb(white,0xf8fbfc));
                c.drawOval(x-r*.95f,cy-r*.43f,x+r*.90f,cy+r*.26f,p);
                c.drawOval(x-r*.48f,cy-r*.72f,x+r*.20f,cy+r*.18f,p);
                c.drawOval(x+r*.02f,cy-r*.60f,x+r*.70f,cy+r*.20f,p);
                p.setColor(argb((int)(70*alpha),0xffffff));
                c.drawOval(x-r*.36f,cy-r*.55f,x+r*.05f,cy-r*.22f,p);
            }
        }

        for(int i=0;i<18;i++){
            float q=(i+.4f)/18f,z=q*q,y=hz+(h-hz)*(.10f+.86f*z);
            float period=w+w*.30f;
            float x=wrap(i*113f-clock*(18+q*44)+headingPan(w,.25f+.75f*q),period)-w*.15f;
            stroke.setColor(argb((int)((18+34*q)*alpha),0x6e858e));
            stroke.setStrokeWidth(Math.max(1f,w*(.0008f+.0045f*q)));
            c.drawLine(x,y,x+w*(.035f+.10f*q),y+h*(.002f+.010f*q),stroke);
        }
    }

    private void drawCloudInterior(Canvas c,int w,int h,float hz,float position01,float coverage){
        float bright=.72f+.20f*position01;
        int top=rgb((int)(205+30*bright),(int)(215+27*bright),(int)(219+28*bright));
        int bot=rgb((int)(226+20*bright),(int)(231+19*bright),(int)(232+20*bright));
        p.setShader(new LinearGradient(0,0,0,h,new int[]{top,0xffeef2f2,bot},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);

        float pan=headingPan(w,.8f);
        int blobs=12+(int)(12*coverage);
        for(int i=0;i<blobs;i++){
            float q=(i+.5f)/Math.max(1f,blobs);
            float x=wrap(i*91f+pan+clock*(5f+(i%3)*3f),w+w*.25f)-w*.12f;
            float y=h*(.06f+((i*37)%83)/100f);
            float r=w*(.035f+.075f*q);
            int aa=(int)((35+75*(1f-Math.abs(position01-.5f)))*coverage);
            p.setColor(argb(aa,i%2==0?0xffffff:0xaab7bb));
            c.drawOval(x-r,y-r*.38f,x+r,y+r*.38f,p);
        }
        p.setColor(argb((int)(66*coverage),0xffffff));c.drawRect(0,hz-h*.05f,w,hz+h*.08f,p);
    }

    private void drawOpenOcean(Canvas c,int w,int h,float hz){
        p.setShader(new LinearGradient(0,0,0,hz,new int[]{0xff135f92,0xff49add2,0xffc7e9f0},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,hz,p);p.setShader(null);
        p.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff2a8eae,0xff126584,0xff073d59,0xff032c43},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        p.setColor(0x8ad9f2f4);c.drawRect(0,hz,w,hz+Math.max(2,h*.006f),p);

        float low=clamp((120f-sharedAltitudeM)/120f,0,1),spd=clamp(sharedSpeedMps/270f,0,1);
        float waveAmp=.45f+.75f*WeatherEffectsView.getSharedWindStrength();
        drawDistantOceanClouds(c,w,h,hz);

        for(int i=0;i<76;i++){
            float q=(i/76f+seaFlow*(.34f+spd*.90f))%1f,z=q*q,y=hz+(h-hz)*z;
            float seg=w*(.012f+.075f*z)*(1f+.55f*spd);
            float period=w+seg;
            float yaw=headingPan(w,.20f+.80f*z);
            float x=wrap(i*97.3f+clock*(12f+sharedSpeedMps*.36f)*(i%2==0?1f:-.45f)+yaw,period)-seg*.5f;
            float dy=(float)Math.sin(i*1.73+clock*(1.8f+waveAmp))*h*(.0005f+.0032f*z)*waveAmp;
            int aa=(int)(30+88*z+45*low*z);
            stroke.setColor((Math.min(175,aa)<<24)|0x00bfeefa);
            stroke.setStrokeWidth(Math.max(1f,w*(.00045f+.0020f*z)));
            path.reset();path.moveTo(x,y+dy);
            path.quadTo(x+seg*.34f,y-dy*.55f,x+seg*.66f,y+dy*.22f);
            path.quadTo(x+seg*.84f,y-dy*.42f,x+seg,y+dy*.05f);c.drawPath(path,stroke);
        }

        int caps=18+(int)(28*WeatherEffectsView.getSharedWindStrength());
        for(int i=0;i<caps;i++){
            float q=((i*.071f+seaFlow*.71f)%1f),z=.16f+.84f*q*q,y=hz+(h-hz)*z;
            float period=w+w*.12f;
            float x=wrap(i*131f+clock*sharedSpeedMps*.25f+headingPan(w,.30f+.70f*z),period)-w*.06f;
            float len=w*(.006f+.032f*z)*(1f+.65f*low);
            stroke.setColor((int)(0x38+0x78*z)<<24|0x00ffffff);
            stroke.setStrokeWidth(Math.max(1f,w*(.0007f+.0022f*z)));
            c.drawLine(x,y,x+len,y+h*.0025f*z,stroke);
        }

        drawReflection(c,w,h,hz);

        if(low>.05f&&spd>.18f){
            for(int i=0;i<28;i++){
                float q=(i/28f+seaFlow*1.42f)%1f,z=q*q,y=hz+(h-hz)*(.25f+.75f*z);
                float x=wrap(w*((i*37%101)/101f)+headingPan(w,.45f+.55f*z),w);
                float len=h*(.008f+.10f*z)*low*spd;
                stroke.setColor(0x50d9f6ff);stroke.setStrokeWidth(Math.max(1f,w*(.0006f+.0018f*z)));
                c.drawLine(x,y,x+(i%2==0?1:-1)*len*.12f,y+len,stroke);
            }
        }
    }

    private void drawDistantOceanClouds(Canvas c,int w,int h,float hz){
        float coverage=WeatherEffectsView.getSharedCloudLayerCoverage();
        if(coverage<.10f)return;
        float pan=headingPan(w,.18f);
        int count=2+(int)(7*coverage);
        for(int i=0;i<count;i++){
            float period=w+w*.22f;
            float x=wrap(i*w*.19f+pan*.28f,period)-w*.11f;
            float y=hz-h*(.018f+.008f*(i%3));
            float ww=w*(.055f+.018f*(i%2));
            p.setColor(argb((int)(55+70*coverage),0xe8f0f2));c.drawOval(x,y,x+ww,y+h*.014f,p);
            c.drawOval(x+ww*.22f,y-h*.010f,x+ww*.65f,y+h*.012f,p);
        }
    }

    private void drawReflection(Canvas c,int w,int h,float hz){
        int mode=WeatherEffectsView.getSharedCelestialMode();float strength=WeatherEffectsView.getSharedCelestialStrength();if(mode==0||strength<.05f)return;
        float x=w*WeatherEffectsView.getSharedCelestialX01()+turnPanPx*.12f;int rgb=mode==1?0xffe6a3:0xe6f2ff;
        for(int i=0;i<34;i++){
            float q=i/33f,y=hz+(h-hz)*(.08f+.78f*q),spread=w*(.008f+.075f*q)*strength;
            float wander=(float)Math.sin(i*2.2+clock*.65f)*spread*.45f,len=spread*(.55f+.50f*(float)Math.sin(i*.87+1.1));
            int alpha=(int)((20+80*q)*strength);stroke.setColor((Math.min(125,alpha)<<24)|rgb);stroke.setStrokeWidth(Math.max(1f,w*(.0006f+.0014f*q)));
            c.drawLine(x-len*.5f+wander,y,x+len*.5f+wander,y,stroke);
        }
    }

    private static float angleDelta(float current,float previous){float d=current-previous;while(d>180)d-=360;while(d<-180)d+=360;return d;}
    private static float wrap(float v,float period){if(period<=0)return v;v%=period;if(v<0)v+=period;return v;}
    private static int rgb(int r,int g,int b){return 0xff000000|(Math.max(0,Math.min(255,r))<<16)|(Math.max(0,Math.min(255,g))<<8)|Math.max(0,Math.min(255,b));}
    private static int argb(int a,int rgb){return (Math.max(0,Math.min(255,a))<<24)|(rgb&0x00ffffff);}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
