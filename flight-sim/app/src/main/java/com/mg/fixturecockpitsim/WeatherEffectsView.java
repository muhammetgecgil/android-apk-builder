package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/** AVM-12.6: continuous day/night cycle plus independent cloud, rain, snow and wind weather. */
public final class WeatherEffectsView extends View {
    private static final int DAWN=0,MORNING=1,NOON=2,SUNSET=3,EVENING=4,NIGHT=5;
    private static final String[] DAY_LABELS={"DAWN","MORNING","NOON","SUNSET","EVENING","NIGHT"};
    private static final int CLEAR=0,CLOUDY=1,RAIN=2,SNOW=3;
    private static final String[] WEATHER_LABELS={"CLEAR","CLOUDY","RAIN","SNOW"};

    // One complete visual day takes six minutes, so a normal demo flight sees the full sequence.
    private static final long DAY_CYCLE_MS=360000L;

    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Random random=new Random(System.nanoTime());
    private final float[] starX=new float[86],starY=new float[86],starA=new float[86];
    private final long dayEpochMs;
    private long weatherEpochMs,nextWeatherChangeMs;
    private int dayPhase=MORNING,weather=CLEAR,cloudCount=1,windSign=1;
    private boolean windy;
    private float windStrength=.25f;

    public WeatherEffectsView(Context c){
        super(c);setBackgroundColor(Color.TRANSPARENT);stroke.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<starX.length;i++){starX[i]=random.nextFloat();starY[i]=random.nextFloat()*.52f;starA[i]=.35f+random.nextFloat()*.65f;}
        long now=System.currentTimeMillis();
        // Start around early morning, then progress deterministically to noon, sunset, evening, night and dawn.
        dayEpochMs=now-(long)(DAY_CYCLE_MS*.14f);
        chooseWeather(now,true);
    }

    public String getModeLabel(){
        String d=DAY_LABELS[Math.max(0,Math.min(DAY_LABELS.length-1,dayPhase))];
        String w=WEATHER_LABELS[Math.max(0,Math.min(WEATHER_LABELS.length-1,weather))];
        return d+" / "+w+(windy?" + WIND":"");
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.currentTimeMillis();
        dayPhase=computeDayPhase(now);
        if(now>=nextWeatherChangeMs)chooseWeather(now,false);
        int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        float day01=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(day01<0)day01+=1f;
        drawTimeOfDay(c,w,h,now,day01);
        drawWeather(c,w,h,now);
        postInvalidateOnAnimation();
    }

    private int computeDayPhase(long now){
        float f=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(f<0)f+=1f;
        if(f<.14f)return DAWN;
        if(f<.34f)return MORNING;
        if(f<.58f)return NOON;
        if(f<.72f)return SUNSET;
        if(f<.82f)return EVENING;
        return NIGHT;
    }

    private void chooseWeather(long now,boolean first){
        int r=random.nextInt(100);
        if(r<38){weather=CLEAR;cloudCount=random.nextInt(2);}
        else if(r<65){weather=CLOUDY;cloudCount=4+random.nextInt(5);}
        else if(r<84){weather=RAIN;cloudCount=8+random.nextInt(3);}
        else{weather=SNOW;cloudCount=7+random.nextInt(3);}
        windy=random.nextInt(100)<42;
        if(weather==RAIN&&random.nextBoolean())windy=true;
        windStrength=windy?.45f+random.nextFloat()*.55f:.12f+random.nextFloat()*.16f;
        windSign=random.nextBoolean()?1:-1;
        weatherEpochMs=now;
        long min=first?52000L:45000L;
        nextWeatherChangeMs=now+min+random.nextInt(46000);
    }

    private void drawTimeOfDay(Canvas c,int w,int h,long now,float day01){
        switch(dayPhase){
            case DAWN:
                tint(c,w,h,0x6d201c43,0x4dff9a59);break;
            case MORNING:
                tint(c,w,h,0x1c9bd8ef,0x10ffe4ad);break;
            case NOON:
                tint(c,w,h,0x0b7fc7e9,0x05ffffff);break;
            case SUNSET:
                tint(c,w,h,0x70422362,0x62ff713d);break;
            case EVENING:
                tint(c,w,h,0xa5161b35,0x8c2b263a);drawStars(c,w,h,now,28,.40f);break;
            case NIGHT:
            default:
                tint(c,w,h,0xe3080d1c,0xd80b1427);drawNightSky(c,w,h,now);break;
        }

        // Sun exists only from dawn through sunset. There is never a sun in evening/night darkness.
        boolean daylight=dayPhase==DAWN||dayPhase==MORNING||dayPhase==NOON||dayPhase==SUNSET;
        boolean sunVisible=daylight&&weather!=RAIN&&weather!=SNOW&&!(weather==CLOUDY&&cloudCount>=7);
        if(sunVisible){
            float travel=clamp(day01/.72f,0,1);
            float sx=w*(.12f+.76f*travel);
            float sy=h*(.39f-.30f*(float)Math.sin(Math.PI*travel));
            int col=dayPhase==SUNSET?0xffff9650:dayPhase==DAWN?0xffffc36d:0xffffe9a6;
            drawSun(c,w,h,sx,sy,w*(dayPhase==NOON?.032f:.030f),col,weather==CLOUDY?.58f:1f);
        }
    }

    private void drawNightSky(Canvas c,int w,int h,long now){
        if(weather==RAIN||weather==SNOW||cloudCount>=8){drawStars(c,w,h,now,18,.18f);return;}
        int stars=weather==CLOUDY?38:starX.length;
        drawStars(c,w,h,now,stars,weather==CLOUDY?.45f:1f);
        float moonX=w*.74f,moonY=h*.15f;
        drawMoon(c,w,h,moonX,moonY,w*.030f,weather==CLOUDY?.60f:1f);
    }

    private void drawWeather(Canvas c,int w,int h,long now){
        if(weather==CLEAR){
            if(cloudCount>0)drawClouds(c,w,h,now,0x32ffffff,cloudCount);
        }else if(weather==CLOUDY){
            tint(c,w,h,0x2a394852,0x184b5962);drawClouds(c,w,h,now,0x90aeb9bf,cloudCount);
        }else if(weather==RAIN){
            tint(c,w,h,0x5a172836,0x493a4d5b);drawClouds(c,w,h,now,0xb04a5862,cloudCount);drawRain(c,w,h,now);drawRainFlash(c,w,h,now);
        }else if(weather==SNOW){
            tint(c,w,h,0x34404f5c,0x2b91a5b3);drawClouds(c,w,h,now,0x9ec7d0d5,cloudCount);drawSnow(c,w,h,now);
        }
        if(windy)drawWind(c,w,h,now);
    }

    private void tint(Canvas c,int w,int h,int top,int bottom){
        p.setShader(new LinearGradient(0,0,0,h,new int[]{top,bottom},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
    }

    private void drawSun(Canvas c,int w,int h,float x,float y,float r,int color,float visibility){
        int a1=(int)(0x22*visibility),a2=(int)(0x55*visibility),a3=(int)(255*visibility);
        p.setColor((Math.max(0,Math.min(255,a1))<<24)|(color&0x00ffffff));c.drawCircle(x,y,r*2.4f,p);
        p.setColor((Math.max(0,Math.min(255,a2))<<24)|(color&0x00ffffff));c.drawCircle(x,y,r*1.55f,p);
        p.setColor((Math.max(0,Math.min(255,a3))<<24)|(color&0x00ffffff));c.drawCircle(x,y,r,p);
    }

    private void drawMoon(Canvas c,int w,int h,float x,float y,float r,float visibility){
        int glow=(int)(0x55*visibility),solid=(int)(255*visibility);
        p.setColor((glow<<24)|0x00d9e8ff);c.drawCircle(x,y,r*1.8f,p);
        p.setColor((solid<<24)|0x00e6efff);c.drawCircle(x,y,r,p);
        p.setColor(((int)(230*visibility)<<24)|0x00172238);c.drawCircle(x+r*.34f,y-r*.08f,r*.88f,p);
    }

    private void drawStars(Canvas c,int w,int h,long now,int count,float visibility){
        int n=Math.min(count,starX.length);float pulse=(float)(.72+.28*Math.sin(now*.003));
        for(int i=0;i<n;i++){
            int alpha=(int)(255*starA[i]*(i%5==0?pulse:1)*visibility);alpha=Math.max(12,Math.min(255,alpha));
            p.setColor((alpha<<24)|0x00e9f4ff);float r=(i%9==0?2.7f:i%3==0?1.8f:1.1f);c.drawCircle(starX[i]*w,starY[i]*h,r,p);
        }
    }

    private void drawClouds(Canvas c,int w,int h,long now,int color,int count){
        float speed=(windy?.000030f:.000015f)*(1f+windStrength*.75f);
        float travel=(now-weatherEpochMs)*speed*w*windSign;
        for(int i=0;i<count;i++){
            float period=w*1.30f;
            float base=(i*.173f*w+travel*(.62f+(i%3)*.15f))%period;if(base<0)base+=period;base-=w*.15f;
            float y=h*(.07f+(i%4)*.073f);float cw=w*(.075f+.018f*(i%3)),ch=h*(.018f+.004f*(i%2));
            p.setColor(color);c.drawOval(base,y,base+cw,y+ch*1.35f,p);c.drawOval(base+cw*.18f,y-ch*.55f,base+cw*.63f,y+ch*.75f,p);c.drawOval(base+cw*.48f,y-ch*.28f,base+cw*.89f,y+ch*.86f,p);
        }
    }

    private void drawRain(Canvas c,int w,int h,long now){
        stroke.setColor(0x8aaed8ef);stroke.setStrokeWidth(Math.max(1.2f,w*.0011f));
        float fall=(now%1500L)/1500f;float slant=windSign*w*(.006f+.016f*windStrength);
        for(int i=0;i<88;i++){
            float x=((i*97.37f)+(now*.08f*windSign))%(w+100)-50;if(x<-50)x+=w+100;
            float base=((i*53.11f)/Math.max(1,h)+fall)%1f*h;float len=h*(.028f+.008f*(i%4));c.drawLine(x,base,x+slant,base+len,stroke);
        }
        p.setColor(0x2235a7d0);for(int i=0;i<7;i++){float y=h*(.69f+i*.043f);c.drawRect(0,y,w,y+1.5f,p);}
    }

    private void drawRainFlash(Canvas c,int w,int h,long now){long q=now%19000L;if(q<70){p.setColor(0x28ffffff);c.drawRect(0,0,w,h,p);}}

    private void drawSnow(Canvas c,int w,int h,long now){
        float fall=(now%7000L)/7000f;float drift=windSign*w*.025f*windStrength;
        for(int i=0;i<76;i++){
            float x=(i*83.2f+(float)Math.sin(now*.001+i)*24f+drift*(i%5))%w;if(x<0)x+=w;
            float y=((i*.083f+fall*(.70f+(i%4)*.09f))%1f)*h;float r=1.8f+(i%5)*.55f;
            p.setColor(i%6==0?0xe8ffffff:0xb8edf7ff);c.drawCircle(x,y,r,p);
        }
    }

    private void drawWind(Canvas c,int w,int h,long now){
        stroke.setColor(weather==RAIN?0x385fc2dd:0x28d9eff8);stroke.setStrokeWidth(Math.max(1f,w*.0008f));
        float phase=(now%2800L)/2800f;
        for(int i=0;i<20;i++){
            float y=h*(.10f+(i%10)*.075f);float x=((i*137f+phase*w*1.2f*windSign)%w+w)%w;
            float len=w*(.018f+.018f*windStrength);c.drawLine(x,y,x+windSign*len,y,stroke);
        }
    }

    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
