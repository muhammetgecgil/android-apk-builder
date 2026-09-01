package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-18.1 solid exhaust-nozzle replacement.
 * Closes the rear view with overlapping metallic petals, a deep inner liner and
 * a dark turbine/flame-holder face so the exhaust no longer reads as a wire wheel.
 */
public final class SolidNozzleOverlay {
    private static final float NOZZLE_INNER=12f, NOZZLE_PETAL=21f, HEAT_SHIELD=28f;
    private final List<Float> out=new ArrayList<>();
    private float part=NOZZLE_PETAL;

    private SolidNozzleOverlay(){}

    public static float[] build(){
        SolidNozzleOverlay b=new SolidNozzleOverlay();
        b.nozzle(-.72f);
        b.nozzle(.72f);
        float[] a=new float[b.out.size()];
        for(int i=0;i<a.length;i++)a[i]=b.out.get(i);
        return a;
    }

    private void nozzle(float cx){
        // Outer hot-section bands tie the nozzle into the nacelle.
        part=HEAT_SHIELD;
        ellipticShell(cx,-.10f,3.18f,3.34f,.515f,.470f,.61f,32);

        // Sixteen overlapping petal panels. Each petal is a real surface, not a rod.
        part=NOZZLE_PETAL;
        final int n=16;
        for(int i=0;i<n;i++){
            double a0=2*Math.PI*(i-.04)/n;
            double a1=2*Math.PI*(i+1.04)/n;
            float[] A=ep(cx,-.10f,3.30f,.468f,a0,.61f);
            float[] B=ep(cx,-.10f,3.30f,.468f,a1,.61f);
            float[] C=ep(cx,-.10f,3.88f,.315f,a1,.60f);
            float[] D=ep(cx,-.10f,3.88f,.315f,a0,.60f);
            quad2(A,B,C,D);
            // Small aft lip thickness makes the exit read as a volumetric shell.
            float[] E=ep(cx,-.10f,3.93f,.303f,a0,.60f);
            float[] F=ep(cx,-.10f,3.93f,.303f,a1,.60f);
            quad2(D,C,F,E);
        }

        // Deep dark liner. This also prevents the forward compressor/fan from being
        // visible through the exhaust when viewed from the rear.
        part=NOZZLE_INNER;
        ellipticShell(cx,-.10f,3.43f,3.88f,.330f,.250f,.59f,36);
        ellipticRing(cx,-.10f,3.89f,.255f,.59f,.030f,36);
        disc(cx,-.10f,3.46f,.285f,.58f,36);
        // Flame-holder hub/ring sits in front of the dark back plate.
        ellipticRing(cx,-.10f,3.53f,.205f,.58f,.018f,28);
        disc(cx,-.10f,3.52f,.052f,.58f,20);
    }

    private void ellipticShell(float cx,float cy,float z0,float z1,float r0,float r1,float ys,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] A=ep(cx,cy,z0,r0,a0,ys),B=ep(cx,cy,z0,r0,a1,ys);
            float[] C=ep(cx,cy,z1,r1,a1,ys),D=ep(cx,cy,z1,r1,a0,ys);
            quad2(A,B,C,D);
        }
    }

    private void ellipticRing(float cx,float cy,float z,float r,float ys,float w,int sides){
        float ri=Math.max(.01f,r-w);
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            quad2(ep(cx,cy,z,ri,a0,ys),ep(cx,cy,z,r,a0,ys),ep(cx,cy,z,r,a1,ys),ep(cx,cy,z,ri,a1,ys));
        }
    }

    private void disc(float cx,float cy,float z,float r,float ys,int sides){
        float[] O={cx,cy,z};
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] A=ep(cx,cy,z,r,a0,ys),B=ep(cx,cy,z,r,a1,ys);
            tri(O,A,B);tri(O,B,A);
        }
    }

    private float[] ep(float cx,float cy,float z,float r,double a,float ys){
        return new float[]{cx+r*(float)Math.cos(a),cy+r*ys*(float)Math.sin(a),z};
    }
    private void quad2(float[] a,float[] b,float[] c,float[] d){quad(a,b,c,d);quad(d,c,b,a);}
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f){nx=0;ny=0;nz=1;l=1;}
        emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);
    }
    private void emit(float[] p,float nx,float ny,float nz){
        out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);
    }
}
