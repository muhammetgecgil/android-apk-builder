package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/** AVM-17.0 low-cost dynamic control-surface geometry for the mobile renderer. */
public final class DynamicControlSurfaceOverlay {
    public static final float LE_FLAP_L=50f;
    public static final float LE_FLAP_R=51f;
    public static final float SPEED_BRAKE=52f;

    private final List<Float> out=new ArrayList<>();
    private float part;

    private DynamicControlSurfaceOverlay(){}

    public static float[] build(){
        DynamicControlSurfaceOverlay b=new DynamicControlSurfaceOverlay();
        b.part=LE_FLAP_L;b.leadingEdge(-1f);
        b.part=LE_FLAP_R;b.leadingEdge(1f);
        b.part=SPEED_BRAKE;b.speedBrake();
        float[] a=new float[b.out.size()];
        for(int i=0;i<a.length;i++)a[i]=b.out.get(i);
        return a;
    }

    private void leadingEdge(float s){
        // Thin articulated leading-edge panel following the real procedural wing sweep.
        prism(new float[][]{
                {1.02f*s,.20f,-2.58f},{1.55f*s,.18f,-2.30f},{2.38f*s,.14f,-1.83f},
                {3.25f*s,.10f,-1.32f},{4.10f*s,.06f,-.82f},{4.62f*s,.035f,-.50f},
                {4.43f*s,.14f,-.32f},{3.78f*s,.17f,-.69f},{2.98f*s,.20f,-1.15f},
                {2.14f*s,.23f,-1.63f},{1.42f*s,.25f,-2.04f},{1.00f*s,.26f,-2.29f}
        },.028f);
    }

    private void speedBrake(){
        // Dorsal split panel; hinge is animated in Jet3DView around its forward edge.
        prism(new float[][]{
                {-.47f,.915f,.58f},{.47f,.915f,.58f},{.56f,.875f,1.46f},{.36f,.825f,1.83f},
                {-.36f,.825f,1.83f},{-.56f,.875f,1.46f}
        },.026f);
    }

    private void prism(float[][] poly,float t){
        int n=poly.length;float[][] top=new float[n][3],bot=new float[n][3];
        for(int i=0;i<n;i++){top[i]=new float[]{poly[i][0],poly[i][1]+t*.5f,poly[i][2]};bot[i]=new float[]{poly[i][0],poly[i][1]-t*.5f,poly[i][2]};}
        for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}
        for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(l<1e-6f){nx=0;ny=1;nz=0;l=1;}nx/=l;ny/=l;nz/=l;
        put(a,nx,ny,nz);put(b,nx,ny,nz);put(c,nx,ny,nz);
    }
    private void put(float[] v,float nx,float ny,float nz){out.add(v[0]);out.add(v[1]);out.add(v[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);}
}
