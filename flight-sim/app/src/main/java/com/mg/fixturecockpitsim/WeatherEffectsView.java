package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/** AVM-12.5 lightweight visual weather/time-of-day layer for the 3D flight scene. */
public final class WeatherEffectsView extends View {
    private static final int SUNNY=0,CLOUDY=1,RAIN=2,SNOW=3,DAWN=4,SUNSET=5,NIGHT_MOON=6,NIGHT_STARS=7;
    private static final String[] LABELS={"SUNNY","CLOUDY","RAIN","SNOW","DAWN","SUNSET","NIGHT / MOON","NIGHT / STARS"};
    private final Paint p=new Paint(3),stroke=new Paint(3);
    private final Random random=new Random(System.nanoTime());
    private final float[] starX=new float[72],starY=new float[72],starA=new float[72];
    private long epochMs=System.currentTimeMillis(),nextChangeMs;
    private int mode=-1;

    public WeatherEffectsView(Context c){
        super(c);setBackgroundColor(Color.TRANSPARENT);stroke.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<starX.length;i++){starX[i]=random.nextFloat();starY[i]=random.nextFloat()*.48f;starA[i]=.35f+random.nextFloat()*.65f;}
        chooseNext(System.currentTimeMillis());
    }

    public String getModeLabel(){return mode>=0&&mode<LABELS.length?LABELS[mode]:"WEATHER";}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);long now=System.currentTimeMillis();if(now>=nextChangeMs)chooseNext(now);
        int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        drawAtmosphere(c,w,h,now);
        postInvalidateOnAnimation();
    }

    private void chooseNext(long now){
        int n=random.nextInt(LABELS.length);if(n==mode)n=(n+1+random.nextInt(LABELS.length-1))%LABELS.length;
        mode=n;epochMs=now;nextChangeMs=now+38000L+random.nextInt(57000);
    }

    private void drawAtmosphere(Canvas c,int w,int h,long now){
        switch(mode){
            case SUNNY:
                tint(c,w,h,0x08000000,0x08ffd88a);drawSun(c,w,h,w*.78f,h*.16f,w*.035f,0xffffe9a6);drawClouds(c,w,h,now,0x35ffffff,3);break;
            case CLOUDY:
                tint(c,w,h,0x4936424b,0x2f72808a);drawClouds(c,w,h,now,0x95aeb9bf,8);break;
            case RAIN:
                tint(c,w,h,0x6b182938,0x533b5362);drawClouds(c,w,h,now,0xb04c5962,9);drawRain(c,w,h,now);drawRainFlash(c,w,h,now);break;
            case SNOW:
                tint(c,w,h,0x39384b58,0x2f9aabb6);drawClouds(c,w,h,now,0x9ec8d0d3,7);drawSnow(c,w,h,now);break;
            case DAWN:
                tint(c,w,h,0x60412859,0x45ffb467);drawSun(c,w,h,w*.20f,h*.39f,w*.030f,0xffffc46a);drawClouds(c,w,h,now,0x45ffd4bb,4);break;
            case SUNSET:
                tint(c,w,h,0x70412858,0x55ff6c3d);drawSun(c,w,h,w*.79f,h*.37f,w*.034f,0xffff9f52);drawClouds(c,w,h,now,0x554f4153,5);break;
            case NIGHT_MOON:
                tint(c,w,h,0xb0051020,0x97101b2d);drawStars(c,w,h,now,true);drawMoon(c,w,h,w*.77f,h*.16f,w*.030f);drawClouds(c,w,h,now,0x353e4b57,3);break;
            case NIGHT_STARS:
            default:
                tint(c,w,h,0xb3070c1a,0x99101a2a);drawStars(c,w,h,now,false);drawClouds(c,w,h,now,0x252c3945,2);break;
        }
    }

    private void tint(Canvas c,int w,int h,int top,int bottom){
        p.setShader(new LinearGradient(0,0,0,h,new int[]{top,bottom},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
    }

    private void drawSun(Canvas c,int w,int h,float x,float y,float r,int color){
        p.setColor((color&0x00ffffff)|0x22000000);c.drawCircle(x,y,r*2.3f,p);p.setColor((color&0x00ffffff)|0x55000000);c.drawCircle(x,y,r*1.55f,p);p.setColor(color);c.drawCircle(x,y,r,p);
    }

    private void drawMoon(Canvas c,int w,int h,float x,float y,float r){
        p.setColor(0x55d9e8ff);c.drawCircle(x,y,r*1.8f,p);p.setColor(0xffe6efff);c.drawCircle(x,y,r,p);p.setColor(0xff172238);c.drawCircle(x+r*.34f,y-r*.08f,r*.88f,p);
    }

    private void drawStars(Canvas c,int w,int h,long now,boolean fewer){
        int n=fewer?44:starX.length;float pulse=(float)(.72+.28*Math.sin(now*.003));
        for(int i=0;i<n;i++){
            int alpha=(int)(255*starA[i]*(i%5==0?pulse:1));alpha=Math.max(35,Math.min(255,alpha));
            p.setColor((alpha<<24)|0x00e9f4ff);float r=(i%9==0?2.7f:i%3==0?1.8f:1.1f);c.drawCircle(starX[i]*w,starY[i]*h,r,p);
        }
    }

    private void drawClouds(Canvas c,int w,int h,long now,int color,int count){
        float travel=(now-epochMs)*.000018f*w;
        for(int i=0;i<count;i++){
            float base=(i*.173f*w+travel*(.65f+(i%3)*.15f))%(w*1.25f)-w*.13f;
            float y=h*(.07f+(i%4)*.073f);float cw=w*(.075f+.018f*(i%3)),ch=h*(.018f+.004f*(i%2));
            p.setColor(color);c.drawOval(base,y,base+cw,y+ch*1.35f,p);c.drawOval(base+cw*.18f,y-ch*.55f,base+cw*.63f,y+ch*.75f,p);c.drawOval(base+cw*.48f,y-ch*.28f,base+cw*.89f,y+ch*.86f,p);
        }
    }

    private void drawRain(Canvas c,int w,int h,long now){
        stroke.setColor(0x8aaed8ef);stroke.setStrokeWidth(Math.max(1.2f,w*.0011f));
        float fall=(now%1500L)/1500f;
        for(int i=0;i<88;i++){
            float x=((i*97.37f)+(now*.08f))%(w+100)-50;float base=((i*53.11f)/h+fall)%1f*h;float len=h*(.028f+.008f*(i%4));c.drawLine(x,base,x-w*.009f,base+len,stroke);
        }
        p.setColor(0x2235a7d0);for(int i=0;i<7;i++){float y=h*(.69f+i*.043f);c.drawRect(0,y,w,y+1.5f,p);}
    }

    private void drawRainFlash(Canvas c,int w,int h,long now){long q=now%17000L;if(q<85){p.setColor(0x30ffffff);c.drawRect(0,0,w,h,p);}}

    private void drawSnow(Canvas c,int w,int h,long now){
        float fall=(now%7000L)/7000f;
        for(int i=0;i<76;i++){
            float x=(i*83.2f+(float)Math.sin(now*.001+i)*24f)%w;if(x<0)x+=w;float y=((i*.083f+fall*(.70f+(i%4)*.09f))%1f)*h;float r=1.8f+(i%5)*.55f;
            p.setColor(i%6==0?0xe8ffffff:0xb8edf7ff);c.drawCircle(x,y,r,p);
        }
    }
}
