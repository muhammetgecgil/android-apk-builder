package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

/** AVM-11.1 isolated airfield: realistic RWY27, one airfield building, zero civil settlement. */
public final class RunwayHudView extends View {
    private final Paint fill=new Paint(3), stroke=new Paint(3);
    private final Path path=new Path();
    private volatile float altitudeM,speedMps,route,groundProgress,headingDeg;
    private volatile boolean onGround,demoMode;
    private volatile String phase="";
    private float scroll,runwayMeters;
    private long lastNs;

    public RunwayHudView(Context c){super(c);fill.setStyle(Paint.Style.FILL);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);}
    public void setDemoMode(boolean e){demoMode=e;postInvalidateOnAnimation();}
    public void setDemoProgress(double s){route=(float)Math.max(0,s);postInvalidateOnAnimation();}
    public void setGroundProgress(double q,double hdg){groundProgress=clamp((float)q,0,1);headingDeg=(float)hdg;postInvalidateOnAnimation();}
    public void setFlightState(double a,double s,boolean g,String ph){altitudeM=(float)Math.max(0,a);speedMps=(float)Math.max(0,s);onGround=g;phase=ph==null?"":ph;postInvalidateOnAnimation();}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c); long now=System.nanoTime(); float dt=lastNs==0?.016f:Math.min(.05f,(now-lastNs)/1e9f); lastNs=now;
        scroll=(scroll+speedMps*dt*.043f)%1f;
        if(phase.contains("TAKEOFF")||phase.contains("ROLLOUT"))runwayMeters+=speedMps*dt;
        if(phase.contains("HANGAR_START")||phase.contains("RUNWAY_HOLD"))runwayMeters=0;
        int w=getWidth(),h=getHeight(); float hz=h*(.455f+.075f*Math.min(1,altitudeM/1300f));
        drawSky(c,w,h,hz);
        if(phase.contains("HANGAR"))drawHangar(c,w,h,hz);
        else if(phase.contains("TAXI"))drawTaxi(c,w,h,hz);
        else if(demoMode&&phase.contains("ORBIT"))drawScenery(c,w,h,hz);
        else drawRunway(c,w,h,hz);
        if(speedMps>1||demoMode)postInvalidateOnAnimation();
    }

    private void drawSky(Canvas c,int w,int h,float hz){
        fill.setShader(new LinearGradient(0,0,0,hz,new int[]{0xff07131f,0xff3c7fa3,0xffc7e2eb},null,Shader.TileMode.CLAMP)); c.drawRect(0,0,w,hz,fill); fill.setShader(null);
        fill.setShader(new LinearGradient(0,hz*.70f,0,hz,0x00ffffff,0x45f3f0df,Shader.TileMode.CLAMP)); c.drawRect(0,hz*.70f,w,hz,fill); fill.setShader(null);
        for(int i=0;i<6;i++){float x=(w*(.04f+i*.18f)+(route*1.3f)%w)%w,y=hz*(.10f+(i%3)*.06f),ww=w*(.10f+(i%2)*.04f);fill.setColor(0x20ffffff);c.drawOval(x,y,x+ww,y+hz*.024f,fill);c.drawOval(x+ww*.22f,y-hz*.015f,x+ww*.70f,y+hz*.018f,fill);}
    }

    private void drawHangar(Canvas c,int w,int h,float hz){
        float q=phase.contains("PARK")?1-groundProgress:groundProgress,cx=w*.5f,doorHalf=w*(.22f+.16f*q),top=h*.16f,bottom=h*.78f;
        fill.setShader(new LinearGradient(0,0,0,h,new int[]{0xff0b1014,0xff182126,0xff353d41},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,fill);fill.setShader(null);
        drawOutside(c,cx-doorHalf,top,cx+doorHalf,bottom,w,h);
        quad(c,cx-doorHalf,bottom,cx+doorHalf,bottom,w,h,0,h,0xff363c3f);
        stroke.setColor(0xff646e74);stroke.setStrokeWidth(2);for(int i=-9;i<=9;i++){float x=cx+i*w*.064f;c.drawLine(x,h,cx+(x-cx)*.29f,bottom,stroke);}for(int i=1;i<8;i++){float t=i/8f,y=lerp(bottom,h,t*t);c.drawLine(0,y,w,y,stroke);}
        stroke.setColor(0xffe6c52e);stroke.setStrokeWidth(5);c.drawLine(cx,bottom,cx,h,stroke);
        stroke.setColor(0xff5e6970);for(int i=0;i<7;i++){float t=i/6f,y=lerp(h*.06f,bottom,t),half=lerp(w*.48f,doorHalf,t);stroke.setStrokeWidth(7-3*t);c.drawLine(cx-half,y,cx-half,bottom,stroke);c.drawLine(cx+half,y,cx+half,bottom,stroke);c.drawLine(cx-half,y,cx+half,y,stroke);c.drawLine(cx-half,y,cx,y-h*.055f,stroke);c.drawLine(cx+half,y,cx,y-h*.055f,stroke);}
        fill.setColor(0xe8ffffff);fill.setTextSize(Math.max(15,w*.016f));c.drawText("AIRFIELD OPS  •  TAXI A  •  RWY 27",w*.035f,h*.93f,fill);
    }

    private void drawOutside(Canvas c,float x0,float y0,float x1,float y1,int w,int h){
        fill.setShader(new LinearGradient(0,y0,0,y1,new int[]{0xff9dd2e5,0xff759fac,0xff496548},null,Shader.TileMode.CLAMP));c.drawRect(x0,y0,x1,y1,fill);fill.setShader(null);
        float cx=(x0+x1)/2;quad(c,cx-(x1-x0)*.055f,y0+(y1-y0)*.10f,cx+(x1-x0)*.055f,y0+(y1-y0)*.10f,x1,y1,x0,y1,0xff4a5053);stroke.setColor(0xffe6c42d);stroke.setStrokeWidth(4);c.drawLine(cx,y0+(y1-y0)*.10f,cx,y1,stroke);
    }

    private void drawTaxi(Canvas c,int w,int h,float hz){
        drawNaturalTerrain(c,w,h,hz,.18f);float cx=w*.5f,far=w*.043f,near=w*.30f;
        quad(c,cx-far*1.20f,hz+h*.014f,cx+far*1.20f,hz+h*.014f,cx+near*1.12f,h,cx-near*1.12f,h,0xff656a6c);
        quad(c,cx-far*.82f,hz+h*.014f,cx+far*.82f,hz+h*.014f,cx+near*.82f,h,cx-near*.82f,h,0xff383d40);
        stroke.setColor(0xfff1c821);stroke.setStrokeWidth(5);c.drawLine(cx,hz+h*.015f,cx,h,stroke);
        for(int i=0;i<26;i++){float q=(i/26f+scroll*.56f)%1f,z=q*q,y=lerp(hz+h*.025f,h,z),hh=lerp(far,near,z);fill.setColor(0xff4ca3ff);float r=1.2f+4.2f*z;c.drawCircle(cx-hh*.95f,y,r,fill);c.drawCircle(cx+hh*.95f,y,r,fill);}
        drawAirfieldBuilding(c,w,h,hz,.79f,.88f);
        if(groundProgress>.55f){float y=h*.66f,hh=w*.145f;stroke.setColor(0xffffd43e);stroke.setStrokeWidth(5);c.drawLine(cx-hh,y,cx+hh,y,stroke);stroke.setStrokeWidth(2);for(int i=0;i<8;i++){float x=cx-hh+i*(2*hh/7f);c.drawLine(x,y+7,x+10,y+18,stroke);}fill.setColor(0xeeffffff);fill.setTextSize(Math.max(14,w*.015f));c.drawText("RWY 27 HOLD SHORT",w*.67f,h*.72f,fill);}
        fill.setColor(0xe8ffffff);fill.setTextSize(Math.max(14,w*.015f));c.drawText("HDG "+String.format(Locale.US,"%03.0f",norm(headingDeg))+"  •  TAXI A",w*.04f,h*.92f,fill);
    }

    private void drawRunway(Canvas c,int w,int h,float hz){
        float near=1-Math.min(1,altitudeM/1050f),err=angleError(headingDeg,270f),cx=w*.5f+clamp(err/22f,-1,1)*w*.095f;
        float fh=w*(.018f+.012f*near),nh=w*(onGround?.45f:(.12f+.30f*near)),fy=hz+h*.014f,ny=h*(onGround?.999f:(.72f+.26f*near));
        drawNaturalTerrain(c,w,h,hz,.24f);drawAirfieldBuilding(c,w,h,hz,.79f,.88f);
        quad(c,cx-fh*1.38f,fy,cx+fh*1.38f,fy,cx+nh*1.18f,ny,cx-nh*1.18f,ny,0xff777b73);
        quad(c,cx-fh*1.12f,fy,cx+fh*1.12f,fy,cx+nh*1.07f,ny,cx-nh*1.07f,ny,0xff53585a);
        path.reset();path.moveTo(cx-fh,fy);path.lineTo(cx+fh,fy);path.lineTo(cx+nh,ny);path.lineTo(cx-nh,ny);path.close();fill.setShader(new LinearGradient(cx,fy,cx,ny,new int[]{0xff303436,0xff292d2f,0xff222628},null,Shader.TileMode.CLAMP));c.drawPath(path,fill);fill.setShader(null);
        for(int i=-9;i<=9;i++){float a=i/9f;stroke.setColor(i%3==0?0x2a5d6061:0x18101011);stroke.setStrokeWidth(1+Math.abs(a)*1.1f);c.drawLine(cx+fh*a*.84f,fy,cx+nh*a*.84f,ny,stroke);}
        stroke.setColor(0xfff7f7f2);stroke.setStrokeWidth(Math.max(2,w*.0027f));c.drawLine(cx-fh,fy,cx-nh,ny,stroke);c.drawLine(cx+fh,fy,cx+nh,ny,stroke);
        for(int i=0;i<36;i++){float q=(i/36f+scroll)%1f,z=q*q,y=lerp(fy,ny,z),y2=Math.min(ny,y+4+44*z);stroke.setColor(0xfff7f7f1);stroke.setStrokeWidth(2+11*z);c.drawLine(cx,y,cx,y2,stroke);}
        for(int i=0;i<30;i++){float q=(i/30f+scroll*.62f)%1f,z=q*q,y=lerp(fy,ny,z),hh=lerp(fh,nh,z);fill.setColor(i>25?0xffffd56b:0xfff4f1dc);float r=1+4*z;c.drawCircle(cx-hh,y,r,fill);c.drawCircle(cx+hh,y,r,fill);}
        for(int set=0;set<3;set++){float q=(.30f+set*.15f+scroll*.10f)%1f,z=q*q,y=lerp(fy,ny,z),hh=lerp(fh,nh,z),bw=hh*.095f,bh=5+18*z;fill.setColor(0xfff3f3ef);for(int s=-1;s<=1;s+=2){c.drawRect(cx+s*hh*.47f-bw,y,cx+s*hh*.47f+bw,y+bh,fill);c.drawRect(cx+s*hh*.68f-bw*.72f,y,cx+s*hh*.68f+bw*.72f,y+bh,fill);}}
        float nz=.46f+(phase.contains("TAKEOFF")?runwayMeters/310f:0);if(nz<=1.04f){float z=clamp(nz,0,1),y=lerp(fy,ny,z*z),hh=lerp(fh,nh,z);fill.setColor(0xfffafaf5);for(int i=-6;i<=6;i++){if(i==0)continue;float x=cx+i*hh*.078f;c.drawRect(x-hh*.026f,y,x+hh*.026f,y+12+34*z,fill);}float ay=Math.min(ny,y+28+62*z),aw=hh*.12f,ah=8+24*z;c.drawRect(cx-hh*.48f-aw,ay,cx-hh*.48f+aw,ay+ah,fill);c.drawRect(cx+hh*.48f-aw,ay,cx+hh*.48f+aw,ay+ah,fill);fill.setTextAlign(Paint.Align.CENTER);fill.setTextSize(Math.max(27,w*(.033f+.030f*z)));c.drawText("27",cx,Math.min(ny-5,y+58+42*z),fill);fill.setTextAlign(Paint.Align.LEFT);}
        if(!onGround||phase.contains("APPROACH")||phase.contains("FLARE")){float pz=.36f,py=lerp(fy,ny,pz),ph=lerp(fh,nh,pz),sx=cx-ph*1.34f;for(int i=0;i<4;i++){fill.setColor(i<2?0xffffffff:0xffff3b30);c.drawCircle(sx+i*7,py,3.4f,fill);}}
        if(phase.contains("APPROACH")||phase.contains("FLARE")){fill.setColor(Math.abs(err)<2.5?0xff7fe889:0xffffd35a);fill.setTextSize(Math.max(14,w*.015f));c.drawText(Math.abs(err)<2.5?"RWY 27 • CENTERLINE CAPTURED":"RWY 27 • ALIGNING",w*.66f,h*.92f,fill);}
    }

    private void drawNaturalTerrain(Canvas c,int w,int h,float hz,float mountainScale){
        fill.setShader(new LinearGradient(0,hz,0,h,new int[]{0xff49634a,0xff3d563f,0xff2e4735},null,Shader.TileMode.CLAMP));c.drawRect(0,hz,w,h,fill);fill.setShader(null);drawMountains(c,w,h,hz,mountainScale,false);
        for(int i=0;i<22;i++){float x=(i*97f+route*2.1f)%Math.max(1,w),y=hz+h*(.07f+.035f*((i*13)%8));fill.setColor(i%2==0?0x17314227:0x183c5631);c.drawOval(x,y,x+w*.075f,y+h*.012f,fill);}
    }

    private void drawAirfieldBuilding(Canvas c,int w,int h,float hz,float xNorm,float baseNorm){
        float baseY=Math.max(hz+h*.045f,h*baseNorm),bw=w*.115f,bh=h*.085f,x=w*xNorm-bw*.5f;
        fill.setColor(0x30000000);c.drawRect(x+5,baseY-bh+5,x+bw+7,baseY+5,fill);fill.setColor(0xff69747a);c.drawRect(x,baseY-bh,x+bw,baseY,fill);
        path.reset();path.moveTo(x-bw*.04f,baseY-bh);path.lineTo(x+bw*.50f,baseY-bh-h*.025f);path.lineTo(x+bw*1.04f,baseY-bh);path.close();fill.setColor(0xff41484c);c.drawPath(path,fill);
        fill.setColor(0xff25455a);for(int i=0;i<4;i++){float wx=x+bw*(.10f+i*.22f);c.drawRect(wx,baseY-bh*.68f,wx+bw*.14f,baseY-bh*.43f,fill);}fill.setColor(0xffc8cdd0);c.drawRect(x+bw*.39f,baseY-bh*.35f,x+bw*.62f,baseY,fill);
        stroke.setColor(0xff8c969c);stroke.setStrokeWidth(2);c.drawLine(x+bw*.76f,baseY-bh,x+bw*.76f,baseY-bh-h*.055f,stroke);fill.setColor(0xffd94b45);c.drawCircle(x+bw*.76f,baseY-bh-h*.055f,2.6f,fill);
    }

    private void drawScenery(Canvas c,int w,int h,float hz){
        float t=Math.min(300,route);
        if(t<55){drawDepartureCoast(c,w,h,hz,t/55f);label(c,w,h,"IZOLE HAVA SAHASI • KIYI");}
        else if(t<115){fill.setColor(0xff36503b);c.drawRect(0,hz,w,h,fill);drawMountains(c,w,h,hz,.58f,false);label(c,w,h,"YERLESIMSIZ DAGLIK ARAZI");}
        else if(t<185){fill.setColor(0xff344c39);c.drawRect(0,hz,w,h,fill);drawMountains(c,w,h,hz,.86f,true);label(c,w,h,"YERLESIMSIZ VADI");}
        else if(t<245){drawLake(c,w,h,hz);label(c,w,h,"DOGAL GOL GECISI");}
        else if(t<280){fill.setColor(0xff354c38);c.drawRect(0,hz,w,h,fill);drawMountains(c,w,h,hz,.72f,false);label(c,w,h,"YUKSEK ARAZI");}
        else{drawNaturalTerrain(c,w,h,hz,.24f);drawAirfieldBuilding(c,w,h,hz,.79f,.88f);label(c,w,h,"RWY 27 HIZALANMA");}
    }

    private void drawDepartureCoast(Canvas c,int w,int h,float hz,float t){float shore=hz+h*(.055f+.045f*t);drawMountains(c,w,h,hz,.20f+.13f*t,false);fill.setColor(0xff3d583c);path.reset();path.moveTo(0,shore);for(int i=0;i<=14;i++){float x=w*i/14f,y=shore+h*.020f*(float)Math.sin(i*.8+route*.03);path.lineTo(x,y);}path.lineTo(w,h);path.lineTo(0,h);path.close();c.drawPath(path,fill);drawWater(c,w,h,shore+h*.055f);}
    private void drawWater(Canvas c,int w,int h,float top){fill.setShader(new LinearGradient(0,top,0,h,new int[]{0xff5ea8bd,0xff1a6784,0xff062c45},null,Shader.TileMode.CLAMP));c.drawRect(0,top,w,h,fill);fill.setShader(null);float motion=route*12f+scroll*w*.9f;for(int i=0;i<58;i++){float z=((i*37)%100)/100f,y=top+(h-top)*(.05f+.90f*z),x=(i*113+motion*(.22f+z))%Math.max(1,w),ww=10+w*.025f*z,hh=1.1f+2.6f*z;fill.setColor(i%5==0?0x52ffffff:0x247ed2e4);c.drawOval(x,y,x+ww,y+hh,fill);}}
    private void drawMountains(Canvas c,int w,int h,float hz,float scale,boolean valley){mountainLayer(c,w,h,hz,scale*.54f,0xff71868a,1.12f,valley);mountainLayer(c,w,h,hz,scale*.77f,0xff526a63,1.38f,valley);mountainLayer(c,w,h,hz,scale,0xff30483d,1.67f,valley);}
    private void mountainLayer(Canvas c,int w,int h,float hz,float scale,int col,float freq,boolean valley){path.reset();path.moveTo(0,h);for(int i=0;i<=24;i++){float x=w*i/24f,y=hz-h*scale*(.035f+.13f*(float)Math.abs(Math.sin(i*freq+route*.010)));if(valley&&i>10&&i<14)y+=h*.12f;path.lineTo(x,y);}path.lineTo(w,h);path.close();fill.setColor(col);c.drawPath(path,fill);}
    private void drawLake(Canvas c,int w,int h,float hz){fill.setColor(0xff36503d);c.drawRect(0,hz,w,h,fill);drawMountains(c,w,h,hz,.56f,true);drawWater(c,w,h,hz+h*.12f);}
    private void label(Canvas c,int w,int h,String s){fill.setColor(0xe8ffffff);fill.setTextSize(Math.max(18,w*.019f));c.drawText(s,w*.04f,h*.89f,fill);}
    private void quad(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4,int col){path.reset();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.lineTo(x4,y4);path.close();fill.setColor(col);c.drawPath(path,fill);}
    private static float norm(float a){a%=360;if(a<0)a+=360;return a;}
    private static float angleError(float h,float t){float d=t-h;while(d>180)d-=360;while(d<-180)d+=360;return d;}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
