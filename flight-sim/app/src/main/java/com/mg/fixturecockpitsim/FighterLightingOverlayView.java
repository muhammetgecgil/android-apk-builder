package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import com.mg.fixturecockpitsim.sim.FighterLightingSystem;

/**
 * Lightweight external-light renderer layered over the 3D fighter.
 * Lights follow the current camera framing and remain independent from the
 * aircraft mesh, avoiding changes to the proven v89 geometry/shader chain.
 */
public final class FighterLightingOverlayView extends View {
    private final FighterLightingSystem lights;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int cameraMode=Jet3DView.CAMERA_CHASE;
    private boolean onGround=true;
    private float gear=1f,speed;

    public FighterLightingOverlayView(Context context,FighterLightingSystem system){
        super(context);lights=system;setClickable(false);setFocusable(false);
    }

    public void setAircraftState(int camera,boolean ground,float gearPosition,float speedMps){
        cameraMode=Math.max(0,Math.min(3,camera));onGround=ground;gear=Math.max(0,Math.min(1,gearPosition));speed=Math.max(0,speedMps);invalidate();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        final float w=getWidth(),h=getHeight();
        if(w<=1||h<=1)return;
        final long now=SystemClock.elapsedRealtime();

        // Generic camera-relative anchor around the rendered fighter.
        float cx=.50f*w, cy=.43f*h, span=.235f*w;
        if(cameraMode==Jet3DView.CAMERA_REAR){cy=.445f*h;span=.245f*w;}
        else if(cameraMode==Jet3DView.CAMERA_RIGHT_QUARTER){cx=.46f*w;cy=.445f*h;span=.215f*w;}
        else if(cameraMode==Jet3DView.CAMERA_LEFT_QUARTER){cx=.54f*w;cy=.445f*h;span=.215f*w;}

        float portX=cx-span, starX=cx+span;
        float wingY=cy+.015f*h, tailY=cy-.075f*h;

        // Landing/taxi beams are intentionally tied to gear extension.
        float land=(float)lights.landingIntensity(gear);
        float taxi=(float)lights.taxiIntensity(gear,onGround);
        if(land>0.01f)drawBeam(c,cx-.018f*w,cy+.02f*h,land,false);
        if(taxi>0.01f)drawBeam(c,cx+.018f*w,cy+.02f*h,taxi,true);

        if(lights.formation)drawFormation(c,cx,cy,span);

        if(lights.navigation){
            glow(c,portX,wingY,Color.rgb(255,35,28),9f,1f);
            glow(c,starX,wingY,Color.rgb(30,255,78),9f,1f);
            glow(c,cx,tailY,Color.WHITE,7f,.85f);
        }

        float st=(float)lights.strobeIntensity(now);
        if(st>0){
            glow(c,portX-.012f*w,wingY-.005f*h,Color.WHITE,18f,st);
            glow(c,starX+.012f*w,wingY-.005f*h,Color.WHITE,18f,st);
        }

        float bc=(float)lights.beaconIntensity(now);
        if(bc>0)glow(c,cx,cy-.035f*h,Color.rgb(255,38,24),14f,bc);

        // Keep time-based flashes alive even if no other state changes.
        if(lights.strobe||lights.beacon)postInvalidateDelayed(35);
    }

    private void drawFormation(Canvas c,float cx,float cy,float span){
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(Math.max(3f,getWidth()*.0042f));
        p.setColor(Color.argb(155,150,255,112));
        float y=cy+.012f*getHeight();
        c.drawLine(cx-span*.74f,y,cx-span*.47f,y+.010f*getHeight(),p);
        c.drawLine(cx-span*.39f,y+.006f*getHeight(),cx-span*.18f,y+.012f*getHeight(),p);
        c.drawLine(cx+span*.18f,y+.012f*getHeight(),cx+span*.39f,y+.006f*getHeight(),p);
        c.drawLine(cx+span*.47f,y+.010f*getHeight(),cx+span*.74f,y,p);
        c.drawLine(cx-span*.10f,cy-.055f*getHeight(),cx-span*.04f,cy-.087f*getHeight(),p);
        c.drawLine(cx+span*.10f,cy-.055f*getHeight(),cx+span*.04f,cy-.087f*getHeight(),p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawBeam(Canvas c,float x,float y,float intensity,boolean taxi){
        float h=getHeight(),w=getWidth();
        float len=(taxi?.22f:.42f)*h;
        float half=(taxi?.055f:.095f)*w;
        Path path=new Path();path.moveTo(x,y);path.lineTo(x-half,y+len);path.lineTo(x+half,y+len);path.close();
        int a=(int)(intensity*(taxi?72:108));
        int near=Color.argb(a,255,250,220),far=Color.argb(0,255,250,220);
        p.setShader(new LinearGradient(x,y,x,y+len,near,far,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawPath(path,p);p.setShader(null);
        glow(c,x,y,Color.rgb(255,248,215),taxi?12f:16f,intensity);
    }

    private void glow(Canvas c,float x,float y,int color,float radiusDp,float intensity){
        float d=getResources().getDisplayMetrics().density;
        float r=Math.max(4f,radiusDp*d);intensity=Math.max(0,Math.min(1,intensity));
        int rgb=color&0x00ffffff;
        int center=Color.argb((int)(245*intensity),(rgb>>16)&255,(rgb>>8)&255,rgb&255);
        int edge=Color.argb(0,(rgb>>16)&255,(rgb>>8)&255,rgb&255);
        p.setShader(new RadialGradient(x,y,r*2.7f,center,edge,Shader.TileMode.CLAMP));c.drawCircle(x,y,r*2.7f,p);p.setShader(null);
        p.setColor(Color.argb((int)(255*intensity),(rgb>>16)&255,(rgb>>8)&255,rgb&255));c.drawCircle(x,y,r*.42f,p);
    }
}
