package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-16.1 dynamic engine visual geometry.
 * Provides animated compressor/fan hardware, afterburner shock rings and a low-cost
 * refractive-like heat-haze shell for the mobile OpenGL renderer.
 */
public final class EngineDynamicsOverlay {
    public static final float ENGINE_FAN=46f;
    public static final float HEAT_HAZE=47f;
    public static final float AFTERBURNER_RING=48f;
    public static final float FAN_BLUR=49f;

    private final List<Float> out=new ArrayList<>();
    private float part=ENGINE_FAN;

    private EngineDynamicsOverlay(){}

    public static float[] buildSolid(){
        EngineDynamicsOverlay b=new EngineDynamicsOverlay();
        b.part=ENGINE_FAN;
        b.fanStage(-1f);
        b.fanStage(1f);
        return b.array();
    }

    public static float[] buildTransparent(){
        EngineDynamicsOverlay b=new EngineDynamicsOverlay();
        for(float cx:new float[]{-.72f,.72f}){
            b.part=AFTERBURNER_RING;
            b.shockDiamonds(cx);
            b.part=HEAT_HAZE;
            b.heatHaze(cx);
        }
        b.part=FAN_BLUR;
        b.fanBlur(-1f);
        b.fanBlur(1f);
        return b.array();
    }

    private float[] array(){
        float[] a=new float[out.size()];
        for(int i=0;i<a.length;i++)a[i]=out.get(i);
        return a;
    }

    private void fanStage(float side){
        final float cx=.94f*side,cy=-.09f,z=-1.145f,inner=.064f,outer=.235f,ys=.74f;
        final int blades=36;
        // Swept front rotor blades. Both faces are emitted so the stage remains visible
        // from oblique intake camera angles even with back-face culling enabled.
        for(int i=0;i<blades;i++){
            double a=2*Math.PI*i/blades;
            double a0=a-.030,a1=a+.030;
            double o0=a+.085,o1=a+.145;
            float[] p0=ep(cx,cy,z,inner,a0,ys);
            float[] p1=ep(cx,cy,z,outer,o0,ys);
            float[] p2=ep(cx,cy,z,outer,o1,ys);
            float[] p3=ep(cx,cy,z,inner,a1,ys);
            quad2(p0,p1,p2,p3);
        }
        // Spinner and casing ring add depth and make rotation readable at low RPM.
        ellipsoid(cx,cy,z-.034f,.071f,.054f,.110f,18,10);
        ellipticRing(cx,cy,z+.004f,.252f,ys,.018f,40);
    }

    private void fanBlur(float side){
        float cx=.94f*side,cy=-.09f,z=-1.139f,ys=.74f;
        annulus(cx,cy,z,.078f,.232f,ys,44);
    }

    private void shockDiamonds(float cx){
        float[] z={4.08f,4.34f,4.63f,4.96f,5.30f};
        float[] r={.180f,.160f,.137f,.109f,.078f};
        float[] w={.018f,.017f,.015f,.013f,.011f};
        for(int i=0;i<z.length;i++) annulus(cx,-.10f,z[i],Math.max(.018f,r[i]-w[i]),r[i],.56f,28);
        // A faint axial core disc helps the familiar diamond train read from rear-quarter views.
        annulus(cx,-.10f,4.20f,.018f,.060f,.56f,22);
    }

    private void heatHaze(float cx){
        float[] z={3.88f,4.24f,4.72f,5.28f,5.92f,6.62f};
        float[] rx={.235f,.245f,.265f,.290f,.315f,.345f};
        float[] ry={.145f,.152f,.166f,.181f,.196f,.214f};
        int sides=22;
        for(int s=0;s<z.length-1;s++){
            for(int i=0;i<sides;i++){
                double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
                float[] A={cx+rx[s]*(float)Math.cos(a0),-.10f+ry[s]*(float)Math.sin(a0),z[s]};
                float[] B={cx+rx[s+1]*(float)Math.cos(a0),-.10f+ry[s+1]*(float)Math.sin(a0),z[s+1]};
                float[] C={cx+rx[s+1]*(float)Math.cos(a1),-.10f+ry[s+1]*(float)Math.sin(a1),z[s+1]};
                float[] D={cx+rx[s]*(float)Math.cos(a1),-.10f+ry[s]*(float)Math.sin(a1),z[s]};
                quad(A,B,C,D);
            }
        }
    }

    private void annulus(float cx,float cy,float z,float ri,float ro,float ys,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] A=ep(cx,cy,z,ri,a0,ys),B=ep(cx,cy,z,ro,a0,ys),C=ep(cx,cy,z,ro,a1,ys),D=ep(cx,cy,z,ri,a1,ys);
            quad2(A,B,C,D);
        }
    }

    private void ellipticRing(float cx,float cy,float z,float r,float ys,float width,int sides){
        annulus(cx,cy,z,Math.max(.004f,r-width),r,ys,sides);
    }

    private float[] ep(float cx,float cy,float z,float r,double a,float ys){
        return new float[]{cx+r*(float)Math.cos(a),cy+r*ys*(float)Math.sin(a),z};
    }

    private void ellipsoid(float cx,float cy,float cz,float rx,float ry,float rz,int slices,int stacks){
        for(int j=0;j<stacks;j++)for(int i=0;i<slices;i++){
            double p0=-Math.PI/2+Math.PI*j/stacks,p1=-Math.PI/2+Math.PI*(j+1)/stacks;
            double a0=2*Math.PI*i/slices,a1=2*Math.PI*(i+1)/slices;
            float[] A=ell(cx,cy,cz,rx,ry,rz,p0,a0),B=ell(cx,cy,cz,rx,ry,rz,p1,a0),C=ell(cx,cy,cz,rx,ry,rz,p1,a1),D=ell(cx,cy,cz,rx,ry,rz,p0,a1);
            quad(A,B,C,D);
        }
    }

    private float[] ell(float cx,float cy,float cz,float rx,float ry,float rz,double p,double a){
        float cp=(float)Math.cos(p),sp=(float)Math.sin(p),ca=(float)Math.cos(a),sa=(float)Math.sin(a);
        return new float[]{cx+rx*cp*ca,cy+ry*sp,cz+rz*cp*sa};
    }

    private void quad2(float[] a,float[] b,float[] c,float[] d){quad(a,b,c,d);quad(d,c,b,a);}
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2];
        float vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1f;
        emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);
    }
    private void emit(float[] p,float nx,float ny,float nz){
        out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);
    }
}
