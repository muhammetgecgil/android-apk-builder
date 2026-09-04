package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

import com.mg.fixturecockpitsim.sim.CinematicJourneyState;

/** Route-specific atmosphere layered over the normal world and under the 3D aircraft. */
public final class CinematicJourneyOverlayView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public CinematicJourneyOverlayView(Context c){super(c);setWillNotDraw(false);}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;long now=System.currentTimeMillis();int s=CinematicJourneyState.getStage();float b=CinematicJourneyState.getStageBlend01();
        if(s==CinematicJourneyState.TOROS)drawTorosHaze(c,w,h,b);
        else if(s==CinematicJourneyState.AEGEAN)drawAegean(c,w,h,now,b);
        else if(s==CinematicJourneyState.PATARA||s==CinematicJourneyState.KARAPINAR)drawArid(c,w,h,now,s==CinematicJourneyState.PATARA);
        else if(s==CinematicJourneyState.CLOUD_SEA)drawCloudSea(c,w,h,now);
        else if(s==CinematicJourneyState.STORM)drawStorm(c,w,h,now);
        else if(s==CinematicJourneyState.SNOW)drawSnow(c,w,h,now);
        else if(s==CinematicJourneyState.MOONLIT)drawMoonlit(c,w,h,now,b);
        else if(s==CinematicJourneyState.RETURN)drawReturn(c,w,h,b);
        postInvalidateOnAnimation();
    }

    private void drawTorosHaze(Canvas c,int w,int h,float b){
        p.setShader(new LinearGradient(0,h*.18f,0,h*.72f,new int[]{0x001b2731,0x183c5160,0x244a5a61},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
    }
    private void drawAegean(Canvas c,int w,int h,long now,float b){
        float hz=h*.56f;p.setShader(new LinearGradient(0,hz,0,h,new int[]{0x7a2c82aa,0xa91b628e,0xbc123f67},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        float sx=w*(.62f+.06f*(float)Math.sin(now*.00008));for(int i=0;i<22;i++){float y=hz+(h-hz)*(i+1)/23f;float ww=w*(.02f+.11f*i/22f);p.setColor((0x10+(i%4)*6)<<24|0x00dff8ff);c.drawRect(sx-ww,y,sx+ww,y+1.6f+(i%3),p);}
    }
    private void drawArid(Canvas c,int w,int h,long now,boolean coastal){
        int tint=coastal?0x2bc88a45:0x35b87937;p.setColor(tint);c.drawRect(0,h*.48f,w,h,p);
        for(int i=0;i<15;i++){float y=h*(.58f+i*.027f),amp=h*(.008f+i*.0008f),off=(float)Math.sin(now*.00018+i*1.9)*amp;p.setColor(coastal?0x30e0b66f:0x32c99c55);c.drawOval(-w*.08f+off,y,w*1.08f+off,y+h*.022f,p);}
    }
    private void drawCloudSea(Canvas c,int w,int h,long now){
        p.setColor(0x26e9f1f3);c.drawRect(0,0,w,h,p);for(int i=0;i<28;i++){float q=(i%7)/6f,r=i/7f;float x=w*(q*.18f+.02f)+((now*.014f+i*63)%w),y=h*(.48f+r*.105f)+(float)Math.sin(now*.001+i)*h*.012f;float rw=w*(.075f+.025f*(i%4)),rh=h*(.035f+.012f*(i%3));p.setColor(0x86f6f8f7);c.drawOval(x% (w+rw)-rw,y,x%(w+rw)+rw,y+rh,p);}
        p.setShader(new LinearGradient(0,h*.42f,0,h,new int[]{0x00ffffff,0x55ffffff,0x9adfe8eb},null,Shader.TileMode.CLAMP));c.drawRect(0,h*.38f,w,h,p);p.setShader(null);
    }
    private void drawStorm(Canvas c,int w,int h,long now){
        p.setShader(new LinearGradient(0,0,0,h,new int[]{0x9b101a25,0x8e27323a,0x68434b4c},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        for(int i=0;i<74;i++){float x=(float)((i*83+now*.31)% (w+160))-80,y=(float)((i*47+now*.58)% (h+120))-60;p.setColor(0x75c7d7df);c.drawLine(x,y,x-14,y+42,p);}
        if(now%7200L<115){p.setColor(0x55e9f5ff);c.drawRect(0,0,w,h,p);p.setStrokeWidth(3f);p.setColor(0xcdf1f6ff);float x=w*.68f;c.drawLine(x,0,x-34,h*.22f,p);c.drawLine(x-34,h*.22f,x+8,h*.39f,p);p.setStrokeWidth(1f);}
    }
    private void drawSnow(Canvas c,int w,int h,long now){
        p.setColor(0x2633424a);c.drawRect(0,0,w,h,p);for(int i=0;i<88;i++){float x=(float)((i*97+now*.019*(1+i%3))%(w+30))-15,y=(float)((i*53+now*.052*(1+i%4))%(h+40))-20,r=1.5f+(i%5)*.55f;p.setColor(0xbceff5f7);c.drawCircle(x,y,r,p);}
    }
    private void drawMoonlit(Canvas c,int w,int h,long now,float blend){
        p.setShader(new LinearGradient(0,0,0,h,new int[]{0xd20a1326,0xba10264b,0x9a163d62,0x7e102943},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        for(int i=0;i<110;i++){float x=(i*73%997)/997f*w,y=(i*151%463)/463f*h*.53f;int a=110+(i%5)*28;p.setColor((a<<24)|0x00e8f1ff);c.drawCircle(x,y,.7f+(i%3)*.45f,p);}
        float mx=w*.76f,my=h*.18f,mr=Math.min(w,h)*.055f;p.setShader(new RadialGradient(mx,my,mr*1.9f,new int[]{0xb8fff6d6,0x94eadba3,0x00fff2c7},null,Shader.TileMode.CLAMP));c.drawCircle(mx,my,mr*1.9f,p);p.setShader(null);p.setColor(0xfffff2c9);c.drawCircle(mx,my,mr,p);
        float hz=h*.59f;p.setShader(new LinearGradient(0,hz,0,h,new int[]{0x5b173b5c,0xa80c2a49,0xbc061b35},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,p);p.setShader(null);
        for(int i=0;i<28;i++){float y=hz+(h-hz)*(i+1)/29f,spread=w*(.008f+.13f*i/27f);p.setColor((0x18+(i%4)*7)<<24|0x00fff1bd);c.drawRect(mx-spread,y,mx+spread,y+1.2f+(i%2),p);}
    }
    private void drawReturn(Canvas c,int w,int h,float b){p.setColor(0x2a10263f);c.drawRect(0,0,w,h,p);}
}
