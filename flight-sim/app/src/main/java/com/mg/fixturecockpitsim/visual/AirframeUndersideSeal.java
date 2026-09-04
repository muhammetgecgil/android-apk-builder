package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;

/** Opaque closure geometry for external belly/wing-root holes visible from below. */
public final class AirframeUndersideSeal {
    private AirframeUndersideSeal(){}

    public static float[] build(){
        ArrayList<Float> o=new ArrayList<>();
        // Central belly plates following the fuselage longitudinally.
        plate(o,-.58f,-.47f,-4.25f, .58f,-.47f,-4.25f, .88f,-.58f,-2.45f,-.88f,-.58f,-2.45f);
        plate(o,-.88f,-.58f,-2.45f, .88f,-.58f,-2.45f, 1.18f,-.72f,.15f,-1.18f,-.72f,.15f);
        plate(o,-1.18f,-.72f,.15f, 1.18f,-.72f,.15f, 1.00f,-.68f,2.65f,-1.00f,-.68f,2.65f);
        plate(o,-1.00f,-.68f,2.65f, 1.00f,-.68f,2.65f, .72f,-.58f,4.70f,-.72f,-.58f,4.70f);

        // Wing-root lower closures; kept shallow so landing gear and stores remain visible.
        plate(o,-1.05f,-.50f,-.35f,-4.55f,-.42f,.55f,-4.15f,-.40f,2.10f,-1.05f,-.58f,1.65f);
        plate(o, 1.05f,-.50f,-.35f, 4.55f,-.42f,.55f, 4.15f,-.40f,2.10f, 1.05f,-.58f,1.65f);

        // Intake / lower shoulder closure strips.
        plate(o,-1.95f,-.45f,-2.15f,-.75f,-.55f,-2.45f,-.86f,-.62f,.35f,-2.18f,-.46f,.62f);
        plate(o, 1.95f,-.45f,-2.15f, .75f,-.55f,-2.45f, .86f,-.62f,.35f, 2.18f,-.46f,.62f);

        float[] a=new float[o.size()];for(int i=0;i<a.length;i++)a[i]=o.get(i);return a;
    }

    private static void plate(ArrayList<Float> o,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz){
        tri(o,ax,ay,az,bx,by,bz,cx,cy,cz);tri(o,ax,ay,az,cx,cy,cz,dx,dy,dz);
    }
    private static void tri(ArrayList<Float> o,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){
        float ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az;float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;float m=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(m<1e-5f){nx=0;ny=-1;nz=0;m=1;}nx/=m;ny/=m;nz/=m;
        // Force belly-facing normal direction for stable external lighting.
        if(ny>0){nx=-nx;ny=-ny;nz=-nz;}
        v(o,ax,ay,az,nx,ny,nz);v(o,bx,by,bz,nx,ny,nz);v(o,cx,cy,cz,nx,ny,nz);
    }
    private static void v(ArrayList<Float> o,float x,float y,float z,float nx,float ny,float nz){o.add(x);o.add(y);o.add(z);o.add(nx);o.add(ny);o.add(nz);o.add(64f);}
}
