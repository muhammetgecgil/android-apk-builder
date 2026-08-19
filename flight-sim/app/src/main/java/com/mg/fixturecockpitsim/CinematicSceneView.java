package com.mg.fixturecockpitsim;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.FrameLayout;

/**
 * V11 cinematic compositor.
 * Keeps the OpenGL aircraft as the primary layer and adds atmospheric depth,
 * moving mountain silhouettes, sun haze, cloud wisps, speed streaks and a filmic vignette.
 */
public final class CinematicSceneView extends FrameLayout {
    private final JetWingStoreView jet;
    private final AtmosphereOverlay fx;

    public CinematicSceneView(Context c) {
        super(c);
        setBackgroundColor(Color.rgb(3,10,18));
        jet = new JetWingStoreView(c);
        fx = new AtmosphereOverlay(c);
        addView(jet, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(fx, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setClipChildren(false);
    }

    public void setTelemetry(float roll,float pitch,float yaw,float thr,float hz,int drops,boolean live){
        jet.setTelemetry(roll,pitch,yaw,thr,hz,drops,live);
        fx.setData(roll,pitch,yaw,thr,live);
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        return jet.onTouchEvent(e);
    }

    private static final class AtmosphereOverlay extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        private final Path path = new Path();
        private float roll,pitch,yaw,thr=.7f;
        private boolean live;
        private long start=System.currentTimeMillis();

        AtmosphereOverlay(Context c){
            super(c);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            setWillNotDraw(false);
        }

        void setData(float r,float pi,float y,float t,boolean l){
            roll=r;pitch=pi;yaw=y;thr=t;live=l;postInvalidateOnAnimation();
        }

        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight();
            if(w<=0||h<=0)return;
            long now=System.currentTimeMillis();
            float cx=w*.50f;
            float horizon=h*.46f + pitch*h*.0045f;
            float headingShift=yaw*w/360f;
            float speed=.22f+.78f*thr;
            float time=(now-start)*.001f;

            // High-altitude sky color grade, intentionally translucent so OpenGL metal remains visible.
            p.setShader(new LinearGradient(0,0,0,h*.70f,
                    new int[]{0x5505182B,0x334B9BC9,0x228EC4DB,0x10F0C88A},
                    null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);

            // Warm sun and forward scattering.
            float sx=w*(.72f+.08f*(float)Math.sin(Math.toRadians(yaw)));
            float sy=h*.17f;
            p.setShader(new RadialGradient(sx,sy,Math.min(w,h)*.30f,
                    new int[]{0xA8FFF4C2,0x44FFB75D,0x10FF5D29,0x00FFFFFF},
                    new float[]{0,.18f,.52f,1f},Shader.TileMode.CLAMP));
            c.drawCircle(sx,sy,Math.min(w,h)*.30f,p);p.setShader(null);

            c.save();
            c.rotate(-roll*.62f,cx,h*.48f);

            // Atmospheric haze line.
            p.setColor(0x50D7DDD0);
            c.drawRect(-w,horizon-4,w*2,horizon+18,p);

            // Three mountain depth layers with parallax.
            mountain(c,w,h,horizon+34, 92, 24, 0x665B707B, headingShift*.22f + time*11f*speed);
            mountain(c,w,h,horizon+70, 74, 36, 0x77506466, headingShift*.42f + time*18f*speed);
            mountain(c,w,h,horizon+118,58, 52, 0x88404F49, headingShift*.74f + time*28f*speed);

            // Valley floor / terrain depth gradient.
            p.setShader(new LinearGradient(0,horizon+85,0,h,
                    new int[]{0x204D5745,0x40434D3B,0x70404A35,0xAA1C261E},
                    null,Shader.TileMode.CLAMP));
            path.reset();path.moveTo(-w,horizon+118);path.lineTo(w*2,horizon+118);path.lineTo(w*2,h*2);path.lineTo(-w,h*2);path.close();
            c.drawPath(path,p);p.setShader(null);

            // Perspective ground streaks, stronger with throttle.
            p.setStrokeWidth(Math.max(1.5f,w*.0015f));
            p.setColor((int)(0x18+0x28*thr)<<24 | 0x00E8D6B5);
            for(int i=1;i<=12;i++){
                float k=i/12f;
                float y=horizon+118+(h-horizon-118)*(k*k);
                float half=w*(.08f+.70f*k);
                c.drawLine(cx-half,y,cx+half,y,p);
            }
            c.restore();

            // Thin high clouds.
            for(int i=0;i<7;i++){
                float x=(float)(((now*.010*(.6+.08*i))+i*241)%(w+420)-210);
                float y=h*(.11f+.045f*(i%4));
                p.setShader(new RadialGradient(x,y,150,
                        new int[]{0x35FFFFFF,0x10FFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP));
                c.drawOval(x-170,y-32,x+170,y+32,p);p.setShader(null);
            }

            // Motion streaks near edges; center remains clean for aircraft silhouette.
            if(live || thr>.45f){
                p.setStrokeWidth(Math.max(1.2f,w*.0012f));
                p.setColor(0x28FFF0D0);
                int n=10+(int)(thr*14);
                for(int i=0;i<n;i++){
                    float side=(i%2==0)?1f:-1f;
                    float yy=h*(.20f+.65f*((i*37)%100)/100f);
                    float x0=cx+side*w*(.30f+.20f*((i*17)%100)/100f);
                    float len=w*(.025f+.06f*thr);
                    c.drawLine(x0,yy,x0+side*len,yy+len*.18f,p);
                }
            }

            // Highlight bloom around aircraft center, simulates reflected sun on fuselage.
            p.setShader(new RadialGradient(cx,h*.50f,Math.min(w,h)*.34f,
                    new int[]{0x10FFF6DD,0x06FFFFFF,0x00000000},null,Shader.TileMode.CLAMP));
            c.drawCircle(cx,h*.50f,Math.min(w,h)*.34f,p);p.setShader(null);

            // Cinematic vignette; boosts perceived contrast and depth.
            p.setShader(new RadialGradient(cx,h*.47f,Math.max(w,h)*.74f,
                    new int[]{0x00000000,0x08000000,0x42000000,0x90000000},
                    new float[]{0,.50f,.78f,1f},Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);p.setShader(null);

            // Subtle warm lower-frame grade for the red mountain-world visual direction.
            p.setShader(new LinearGradient(0,h*.52f,0,h,
                    new int[]{0x00FF3A12,0x08FF3A12,0x22A51C0A},null,Shader.TileMode.CLAMP));
            c.drawRect(0,h*.52f,w,h,p);p.setShader(null);

            postInvalidateDelayed(16);
        }

        private void mountain(Canvas c,int w,int h,float base,float wavelength,float amp,int color,float shift){
            p.setColor(color);path.reset();path.moveTo(-w,base);
            for(int x=-w;x<=w*2;x+=24){
                float y=base
                        +(float)Math.sin((x+shift)/wavelength)*amp
                        +(float)Math.sin((x-shift*.41f)/(wavelength*.47f))*amp*.43f
                        +(float)Math.sin((x+shift*.25f)/(wavelength*.23f))*amp*.17f;
                path.lineTo(x,y);
            }
            path.lineTo(w*2,h*2);path.lineTo(-w,h*2);path.close();
            c.drawPath(path,p);
        }
    }
}
