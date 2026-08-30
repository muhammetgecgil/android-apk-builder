package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * Stylized external stores for the simulator exterior model.
 * These are generic visual shapes and do not represent a real weapon design.
 */
public final class VisualOrdnanceMesh {
    public static final float PART_PYLON=30f;
    public static final float PART_STORE_BODY=31f;
    public static final float PART_STORE_FIN=32f;

    private final List<Float> out=new ArrayList<>();
    private float part=PART_STORE_BODY;

    public static float[] build(){
        VisualOrdnanceMesh b=new VisualOrdnanceMesh();

        b.station(-1.55f,-.38f,-.58f,1.00f);
        b.station( 1.55f,-.38f,-.58f,1.00f);
        b.station(-2.55f,-.29f,-.18f,.82f);
        b.station( 2.55f,-.29f,-.18f,.82f);

        b.compactStore(-.48f,-.57f,.38f,.78f);
        b.compactStore( .48f,-.57f,.38f,.78f);

        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++)data[i]=b.out.get(i);
        return data;
    }

    private void station(float x,float wingY,float z,float scale){
        part=PART_PYLON;
        box(x,wingY-.14f*scale,z+.10f*scale,.18f*scale,.28f*scale,.62f*scale);

        part=PART_STORE_BODY;
        spindle(x,wingY-.42f*scale,z-1.02f*scale,z+.88f*scale,.115f*scale,.145f*scale,14);

        part=PART_STORE_FIN;
        float y=wingY-.42f*scale;
        float aft=z+.70f*scale;
        prism(new float[][]{
                {x-.08f*scale,y,aft-.18f*scale},
                {x-.34f*scale,y,aft+.14f*scale},
                {x-.07f*scale,y,aft+.12f*scale}
        },.035f*scale);
        prism(new float[][]{
                {x+.08f*scale,y,aft-.18f*scale},
                {x+.34f*scale,y,aft+.14f*scale},
                {x+.07f*scale,y,aft+.12f*scale}
        },.035f*scale);
        prism(new float[][]{
                {x,y+.05f*scale,aft-.18f*scale},
                {x,y+.31f*scale,aft+.14f*scale},
                {x,y+.05f*scale,aft+.12f*scale}
        },.035f*scale);
    }

    private void compactStore(float x,float y,float z,float scale){
        part=PART_PYLON;
        box(x,y+.16f*scale,z,.15f*scale,.18f*scale,.42f*scale);
        part=PART_STORE_BODY;
        spindle(x,y,z-.62f*scale,z+.62f*scale,.13f*scale,.17f*scale,12);
        part=PART_STORE_FIN;
        float aft=z+.48f*scale;
        prism(new float[][]{
                {x-.05f*scale,y,aft-.12f*scale},
                {x-.24f*scale,y,aft+.10f*scale},
                {x-.04f*scale,y,aft+.08f*scale}
        },.028f*scale);
        prism(new float[][]{
                {x+.05f*scale,y,aft-.12f*scale},
                {x+.24f*scale,y,aft+.10f*scale},
                {x+.04f*scale,y,aft+.08f*scale}
        },.028f*scale);
    }

    private void spindle(float x,float y,float z0,float z1,float rNose,float rBody,int sides){
        float nose=z0,shoulder=z0+(z1-z0)*.20f,tail=z1;
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] tip={x,y,nose};
            float[] p0={x+rNose*(float)Math.cos(a0),y+rNose*(float)Math.sin(a0),shoulder};
            float[] p1={x+rNose*(float)Math.cos(a1),y+rNose*(float)Math.sin(a1),shoulder};
            tri(tip,p1,p0);
        }
        float tailR=rBody*.78f;
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] a={x+rBody*(float)Math.cos(a0),y+rBody*(float)Math.sin(a0),shoulder};
            float[] b={x+tailR*(float)Math.cos(a0),y+tailR*(float)Math.sin(a0),tail};
            float[] c={x+tailR*(float)Math.cos(a1),y+tailR*(float)Math.sin(a1),tail};
            float[] d={x+rBody*(float)Math.cos(a1),y+rBody*(float)Math.sin(a1),shoulder};
            quad(a,b,c,d);
        }
    }

    private void box(float x,float y,float z,float sx,float sy,float sz){
        float hx=sx*.5f,hz=sz*.5f;
        prism(new float[][]{
                {x-hx,y+sy,z-hz},{x+hx,y+sy,z-hz},{x+hx,y+sy,z+hz},{x-hx,y+sy,z+hz}
        },sy);
    }

    private void prism(float[][] top,float thickness){
        if(top==null||top.length<3)return;
        int n=top.length;
        float[][] bot=new float[n][3];
        for(int i=0;i<n;i++)bot[i]=new float[]{top[i][0],top[i][1]-thickness,top[i][2]};
        for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}
        for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}

    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2];
        float vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(l<1e-6f)l=1f;
        emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);
    }

    private void emit(float[] p,float nx,float ny,float nz){
        out.add(p[0]);out.add(p[1]);out.add(p[2]);
        out.add(nx);out.add(ny);out.add(nz);
        out.add(part);
    }
}
