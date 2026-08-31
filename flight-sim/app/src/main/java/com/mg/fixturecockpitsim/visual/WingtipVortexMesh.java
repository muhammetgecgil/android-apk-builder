package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-14.8 aerodynamic-effects buffer.
 * Keeps translucent wingtip vortices, advanced airframe detail and the
 * throttle-linked twin-engine afterburner plume in one effects pass.
 */
public final class WingtipVortexMesh {
    public static final float PART_WINGTIP_VORTEX=40f;
    private final List<Float> out=new ArrayList<>();

    private WingtipVortexMesh(){}

    public static float[] build(){
        WingtipVortexMesh b=new WingtipVortexMesh();
        b.vortex(-1f);
        b.vortex(1f);
        float[] vortex=new float[b.out.size()];
        for(int i=0;i<vortex.length;i++)vortex[i]=b.out.get(i);
        return concat(concat(vortex,AdvancedAirframeOverlay.build()),AfterburnerVisualMesh.build());
    }

    private static float[] concat(float[] a,float[] b){
        float[] out=new float[a.length+b.length];
        System.arraycopy(a,0,out,0,a.length);
        System.arraycopy(b,0,out,a.length,b.length);
        return out;
    }

    private void vortex(float side){
        final int segments=30;
        for(int i=0;i<segments;i++){
            float q0=i/(float)segments,q1=(i+1)/(float)segments;
            P a=center(side,q0),b=center(side,q1);
            float w0=.035f+.080f*q0,w1=.035f+.080f*q1;

            quad(
                    new float[]{a.x,a.y-w0,a.z},
                    new float[]{b.x,b.y-w1,b.z},
                    new float[]{b.x,b.y+w1,b.z},
                    new float[]{a.x,a.y+w0,a.z});

            float wx0=w0*.72f,wx1=w1*.72f;
            quad(
                    new float[]{a.x-wx0,a.y,a.z},
                    new float[]{b.x-wx1,b.y,b.z},
                    new float[]{b.x+wx1,b.y,b.z},
                    new float[]{a.x+wx0,a.y,a.z});
        }
    }

    private P center(float side,float q){
        float radius=.018f+.175f*q;
        double phi=side*(q*8.2+q*q*3.2);
        float x=5.08f*side+side*radius*(float)Math.cos(phi);
        float y=.17f+radius*.62f*(float)Math.sin(phi);
        float z=.02f+5.85f*q;
        return new P(x,y,z);
    }

    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}

    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2];
        float vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1;
        emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);
    }

    private void emit(float[] p,float nx,float ny,float nz){
        out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);out.add(PART_WINGTIP_VORTEX);
    }

    private static final class P{final float x,y,z;P(float x,float y,float z){this.x=x;this.y=y;this.z=z;}}
}
