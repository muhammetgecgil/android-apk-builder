package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import java.util.Locale;
import java.util.Random;

/** AVM-15.0 weather: balanced clouds plus continuous dawn/day/sunset/night transitions. */
public final class WeatherEffectsView extends CinematicEnvironmentView {
    private static final int DAWN=0,MORNING=1,NOON=2,SUNSET=3,EVENING=4,NIGHT=5;
    private static final String[] DAY_LABELS={"DAWN","MORNING","NOON","SUNSET","EVENING","NIGHT"};
    private static final int CLEAR=0,CLOUDY=1,RAIN=2,SNOW=3;
    private static final String[] WEATHER_LABELS={"CLEAR","CLOUDY","RAIN","SNOW"};
    private static final long DAY_CYCLE_MS=360000L;

    private static volatile int sharedCelestialMode;
    private static volatile float sharedCelestialX01=.5f,sharedCelestialStrength;
    private static volatile boolean sharedWindy;
    private static volatile float sharedWindStrength=.20f;
    private static volatile int sharedWindSign=1;
    private static volatile float sharedCloudLayerCoverage;

    public static int getSharedCelestialMode(){return sharedCelestialMode;}
    public static float getSharedCelestialX01(){return sharedCelestialX01;}
    public static float getSharedCelestialStrength(){return sharedCelestialStrength;}
    public static boolean isSharedWindy(){return sharedWindy;}
    public static float getSharedWindStrength(){return sharedWindStrength;}
    public static int getSharedWindSign(){return sharedWindSign;}
    public static float getSharedCrosswindMps(){return sharedWindy?sharedWindSign*(5f+15f*sharedWindStrength):0f;}
    public static float getSharedCloudLayerCoverage(){return sharedCloudLayerCoverage;}
    public static boolean hasSharedCloudLayer(){return sharedCloudLayerCoverage>=.36f;}

    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Random random=new Random(System.nanoTime());
    private final float[] starX=new float[86],starY=new float[86],starA=new float[86];
    private final long dayEpochMs;
    private long weatherEpochMs,nextWeatherChangeMs;
    private int dayPhase=MORNING,weather=CLEAR,cloudCount=1,windSign=1;
    private boolean windy;
    private float windStrength=.25f;

    public WeatherEffectsView(Context c){
        super(c);stroke.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<starX.length;i++){starX[i]=random.nextFloat();starY[i]=random.nextFloat()*.52f;starA[i]=.35f+random.nextFloat()*.65f;}
        long now=System.currentTimeMillis();dayEpochMs=now-(long)(DAY_CYCLE_MS*.14f);chooseWeather(now,true);
    }

    public String getModeLabel(){String d=DAY_LABELS[Math.max(0,Math.min(DAY_LABELS.length-1,dayPhase))],w=WEATHER_LABELS[Math.max(0,Math.min(WEATHER_LABELS.length-1,weather))],layer=hasSharedCloudLayer()?" / LAYER":"";return d+" / "+w+layer+(windy?String.format(Locale.US," + WIND %.0f m/s",Math.abs(getSharedCrosswindMps())):"");}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);long now=System.currentTimeMillis();dayPhase=computeDayPhase(now);if(now>=nextWeatherChangeMs)chooseWeather(now,false);
        int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;float day01=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(day01<0)day01+=1f;
        updateSharedCelestial(day01);drawTimeTint(c,w,h,now,day01);drawWeather(c,w,h,now);postInvalidateOnAnimation();
    }

    private int computeDayPhase(long now){float f=((now-dayEpochMs)%DAY_CYCLE_MS)/(float)DAY_CYCLE_MS;if(f<0)f+=1f;if(f<.14f)return DAWN;if(f<.34f)return MORNING;if(f<.58f)return NOON;if(f<.72f)return SUNSET;if(f<.82f)return EVENING;return NIGHT;}

    private void updateSharedCelestial(float day01){
        boolean daylight=day01<.76f;float sunFade=(1f-smoothstep(.69f,.78f,day01))*smoothstep(.015f,.10f,day01);
        boolean sunAllowed=weather!=RAIN&&weather!=SNOW&&!(weather==CLOUDY&&sharedCloudLayerCoverage>.78f);
        if(daylight&&sunAllowed&&sunFade>.04f){float travel=clamp(day01/.72f,0,1);sharedCelestialMode=1;sharedCelestialX01=.12f+.76f*travel;sharedCelestialStrength=sunFade*(weather==CLOUDY?clamp(1f-sharedCloudLayerCoverage*.55f,.35f,.82f):1f);return;}
        float moonFade=smoothstep(.80f,.89f,day01)*(1f-smoothstep(.975f,1f,day01));boolean moonVisible=moonFade>.03f&&weather!=RAIN&&weather!=SNOW&&sharedCloudLayerCoverage<.78f;
        if(moonVisible){sharedCelestialMode=2;sharedCelestialX01=.74f;sharedCelestialStrength=moonFade*(weather==CLOUDY?.55f:1f);return;}sharedCelestialMode=0;sharedCelestialStrength=0;
    }

    private void chooseWeather(long now,boolean first){
        int r=random.nextInt(100);if(r<40){weather=CLEAR;cloudCount=random.nextInt(3);sharedCloudLayerCoverage=0f;}else if(r<68){weather=CLOUDY;cloudCount=3+random.nextInt(6);sharedCloudLayerCoverage=random.nextInt(100)<32?.20f+random.nextFloat()*.12f:.48f+random.nextFloat()*.40f;}else if(r<86){weather=RAIN;cloudCount=8+random.nextInt(3);sharedCloudLayerCoverage=.82f+random.nextFloat()*.18f;}else{weather=SNOW;cloudCount=7+random.nextInt(3);sharedCloudLayerCoverage=.72f+random.nextFloat()*.24f;}
        windy=random.nextInt(100)<42;if(weather==RAIN&&random.nextBoolean())windy=true;windStrength=windy?.45f+random.nextFloat()*.55f:.12f+random.nextFloat()*.16f;windSign=random.nextBoolean()?1:-1;sharedWindy=windy;sharedWindStrength=windStrength;sharedWindSign=windSign;weatherEpochMs=now;long min=first?52000L:60000L;nextWeatherChangeMs=now+min+random.nextInt(62000);
    }

    /** Continuous atmospheric keyframes: no hard visual switch at phase labels. */
    private void drawTimeTint(Canvas c,int w,int h,long now,float day01){
        float[] k={0f,.14f,.34f,.58f,.72f,.82f,1f};
        int[] top={0x48201c43,0x109bd8ef,0x067fc7e9,0x067fc7e9,0x4f422362,0x78161b35,0x120d1225};
        int[] bot={0x31ff9a59,0x08ffe4ad,0x03ffffff,0x03ffffff,0x42ff713d,0x632b263a,0x10121b2e};
        int seg=0;while(seg<k.length-2&&day01>k[seg+1])seg++;float q=smoothstep(k[seg],k[seg+1],day01);tint(c,w,h,mixArgb(top[seg],top[seg+1],q),mixArgb(bot[seg],bot[seg+1],q));
        float eveningStars=smoothstep(.76f,.88f,day01)*(1f-smoothstep(.975f,1f,day01));if(eveningStars>.02f){int n=weather==RAIN||weather==SNOW?18:weather==CLOUDY?42:starX.length;drawStars(c,w,h,now,n,eveningStars*(weather==CLOUDY?.48f:1f));}
        boolean sunVisible=day01<.77f&&weather!=RAIN&&weather!=SNOW&&!(weather==CLOUDY&&sharedCloudLayerCoverage>.78f);if(sunVisible){float travel=clamp(day01/.72f,0,1),sx=w*(.12f+.76f*travel),sy=h*(.39f-.30f*(float)Math.sin(Math.PI*travel));float warm=smoothstep(.58f,.74f,day01)+1f-smoothstep(.03f,.14f,day01);int col=mixArgb(0xffffe9a6,0xffff9650,clamp(warm,0,1));float vis=(1f-smoothstep(.70f,.78f,day01))*smoothstep(.01f,.08f,day01);drawSun(c,sx,sy,w*.030f,col,vis*(weather==CLOUDY?.58f:1f));}
        if(eveningStars>.10f&&weather!=RAIN&&weather!=SNOW&&sharedCloudLayerCoverage<.80f)drawMoon(c,w*.74f,h*.15f,w*.030f,eveningStars*(weather==CLOUDY?.60f:1f));
    }

    private void drawWeather(Canvas c,int w,int h,long now){if(weather==CLEAR){if(cloudCount>0)drawClouds(c,w,h,now,0x32ffffff,cloudCount);}else if(weather==CLOUDY){tint(c,w,h,0x24394852,0x124b5962);drawClouds(c,w,h,now,0x8caeb9bf,cloudCount);}else if(weather==RAIN){tint(c,w,h,0x4c172836,0x383a4d5b);drawClouds(c,w,h,now,0xa84a5862,cloudCount);drawRain(c,w,h,now);drawRainFlash(c,w,h,now);}else{tint(c,w,h,0x2e404f5c,0x2291a5b3);drawClouds(c,w,h,now,0x92c7d0d5,cloudCount);drawSnow(c,w,h,now);}if(windy)drawWind(c,w,h,now);}
    private void tint(Canvas c,int w,int h,int top,int bottom){p.setShader(new LinearGradient(0,0,0,h,new int[]{top,bottom},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);}
    private void drawSun(Canvas c,float x,float y,float r,int color,float visibility){int a1=(int)(0x22*visibility),a2=(int)(0x55*visibility),a3=(int)(255*visibility);p.setColor((a1<<24)|(color&0x00ffffff));c.drawCircle(x,y,r*2.4f,p);p.setColor((a2<<24)|(color&0x00ffffff));c.drawCircle(x,y,r*1.55f,p);p.setColor((a3<<24)|(color&0x00ffffff));c.drawCircle(x,y,r,p);}
    private void drawMoon(Canvas c,float x,float y,float r,float visibility){int glow=(int)(0x55*visibility),solid=(int)(255*visibility);p.setColor((glow<<24)|0x00d9e8ff);c.drawCircle(x,y,r*1.8f,p);p.setColor((solid<<24)|0x00e6efff);c.drawCircle(x,y,r,p);p.setColor(((int)(230*visibility)<<24)|0x00172238);c.drawCircle(x+r*.34f,y-r*.08f,r*.88f,p);}
    private void drawStars(Canvas c,int w,int h,long now,int count,float visibility){int n=Math.min(count,starX.length);float pulse=(float)(.72+.28*Math.sin(now*.003));for(int i=0;i<n;i++){int alpha=(int)(255*starA[i]*(i%5==0?pulse:1)*visibility);alpha=Math.max(0,Math.min(255,alpha));if(alpha<3)continue;p.setColor((alpha<<24)|0x00e9f4ff);float r=i%9==0?2.7f:i%3==0?1.8f:1.1f;c.drawCircle(starX[i]*w,starY[i]*h,r,p);}}
    private void drawClouds(Canvas c,int w,int h,long now,int color,int count){float speed=(windy?.000030f:.000015f)*(1f+windStrength*.75f),travel=(now-weatherEpochMs)*speed*w*windSign;for(int i=0;i<count;i++){float period=w*1.30f,base=(i*.173f*w+travel*(.62f+(i%3)*.15f))%period;if(base<0)base+=period;base-=w*.15f;float y=h*(.07f+(i%4)*.073f),cw=w*(.075f+.018f*(i%3)),ch=h*(.018f+.004f*(i%2));p.setColor(color);c.drawOval(base,y,base+cw,y+ch*1.35f,p);c.drawOval(base+cw*.18f,y-ch*.55f,base+cw*.63f,y+ch*.75f,p);c.drawOval(base+cw*.48f,y-ch*.28f,base+cw*.89f,y+ch*.86f,p);}}
    private void drawRain(Canvas c,int w,int h,long now){stroke.setColor(0x68aed8ef);stroke.setStrokeWidth(Math.max(1.0f,w*.0009f));long elapsed=Math.max(0L,now-weatherEpochMs);float fall=(elapsed%1700L)/1700f,travel=(elapsed%12000L)/12000f,period=w+140f,slant=windSign*w*(.004f+.011f*windStrength);for(int i=0;i<64;i++){float raw=i*83.73f+travel*period*4.2f*windSign,x=((raw%period)+period)%period-70f,y=((i*.071f+fall*(.82f+(i%5)*.035f))%1f)*h,len=h*(.018f+.006f*(i%4));c.drawLine(x,y,x+slant,y+len,stroke);}}
    private void drawRainFlash(Canvas c,int w,int h,long now){if(now%19000L<70){p.setColor(0x20ffffff);c.drawRect(0,0,w,h,p);}}
    private void drawSnow(Canvas c,int w,int h,long now){float fall=(now%7000L)/7000f,drift=windSign*w*.025f*windStrength;for(int i=0;i<76;i++){float x=(i*83.2f+(float)Math.sin(now*.001+i)*24f+drift*(i%5))%w;if(x<0)x+=w;float y=((i*.083f+fall*(.70f+(i%4)*.09f))%1f)*h,r=1.8f+(i%5)*.55f;p.setColor(i%6==0?0xe8ffffff:0xb8edf7ff);c.drawCircle(x,y,r,p);}}
    private void drawWind(Canvas c,int w,int h,long now){stroke.setColor(weather==RAIN?0x245fc2dd:0x22d9eff8);stroke.setStrokeWidth(Math.max(1f,w*.0007f));float phase=(now%2800L)/2800f;for(int i=0;i<16;i++){float y=h*(.10f+(i%8)*.09f),x=((i*149f+phase*w*1.2f*windSign)%w+w)%w,len=w*(.015f+.014f*windStrength);c.drawLine(x,y,x+windSign*len,y,stroke);}}

    private static int mixArgb(int a,int b,float t){t=clamp(t,0,1);int aa=(a>>>24)&255,ar=(a>>16)&255,ag=(a>>8)&255,ab=a&255,ba=(b>>>24)&255,br=(b>>16)&255,bg=(b>>8)&255,bb=b&255;return ((int)(aa+(ba-aa)*t)<<24)|((int)(ar+(br-ar)*t)<<16)|((int)(ag+(bg-ag)*t)<<8)|(int)(ab+(bb-ab)*t);}
    private static float smoothstep(float a,float b,float x){float t=clamp((x-a)/Math.max(.0001f,b-a),0,1);return t*t*(3-2*t);}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
