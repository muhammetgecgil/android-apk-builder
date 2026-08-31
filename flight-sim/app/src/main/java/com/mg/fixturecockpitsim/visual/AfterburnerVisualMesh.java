package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-14.8 translucent twin-engine afterburner plume.
 * Part 22 is rendered by Jet3DView as throttle-linked emissive exhaust.
 */
public final class AfterburnerVisualMesh {
    public static final float PART_AFTERBURNER=22f;
    private final List<Float> out=new ArrayList<>();

    private AfterburnerVisualMesh(){}

    public static float[] build(){
        AfterburnerVisualMesh b=new AfterburnerVisualMesh();
        b.engine(-.72f);
        b.engine(.72f);
        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++)data[i]=b.out.get(i);
        return data;
    }

    private void engine(float cx){
        final float cy=-.10f;
        // Soft outer plume: broad at the nozzle, tapered downstream.
        coneBand(cx,cy,3.42f,.34f,6.62f,.065f,30);
        // Hot inner core.
        coneBand(cx,cy,3.46f,.205f,5.48f,.035f,24);
        // Shock-diamond bodies. Their brightness is handled in the shader.
        diamond(cx,cy,4.18f,.135f,.34f,20);
        diamond(cx,cy,4.92f,.115f,.30f,20);
        diamond(cx,cy,5.60f,.090f,.25f,18);
        // Crossed translucent ribbons keep the plume legible from chase and quarter cameras.
        ribbon(cx,cy,3.48f,6.55f,.16f,0f);
        ribbon(cx,cy,3.48f,6.55f,.13f,(float)(Math.PI*.5));
    }

    private void diamond(float cx,float cy,float z,float r,float halfLen,int sides){
        coneBand(cx,cy,z-halfLen,0.018f,z,r,sides);
        coneBand(cx,cy,z,r,z+halfLen,0.018f,sides);
    }

    private void coneBand(float cx,float cy,float z0,float r0,float z1,float r1,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] a={cx+r0*(float)Math.cos(a0),cy+r0*.72f*(float)Math.sin(a0),z0};
            float[] b={cx+r1*(float)Math.cos(a0),cy+r1*.72f*(float)Math.sin(a0),z1};
            float[] c={cx+r1*(float)Math.cos(a1),cy+r1*.72f*(float)Math.sin(a1),z1};
            float[] d={cx+r0*(float)Math.cos(a1),cy+r0*.72f*(float)Math.sin(a1),z0};
            quad(a,b,c,d);
        }
    }

    private void ribbon(float cx,float cy,float z0,float z1,float half,float angle){
        float ax=(float)Math.cos(angle)*half, ay=(float)Math.sin(angle)*half*.72f;
        float[] a={cx-ax,cy-ay,z0}, b={cx-ax*.18f,cy-ay*.18f,z1};
        float[] c={cx+ax*.18f,cy+ay*.18f,z1}, d={cx+ax,cy+ay,z0};
        quad(a,b,c,d);
        quad(d,c,b,a);
    }

    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}

    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2];
        float vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1f;
        emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);
    }

    private void emit(float[] p,float nx,float ny,float nz){
        out.add(p[0]);out.add(p[1]);out.add(p[2]);
        out.add(nx);out.add(ny);out.add(nz);out.add(PART_AFTERBURNER);
    }
}
