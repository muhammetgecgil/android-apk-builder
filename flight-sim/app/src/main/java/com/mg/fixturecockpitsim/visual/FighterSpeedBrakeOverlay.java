package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/** Dorsal speed-brake panel for the AVM-19 fighter configuration. */
public final class FighterSpeedBrakeOverlay {
    public static final float SPEED_BRAKE = 52f;
    private static final float DETAIL = 29f;

    private final List<Float> out = new ArrayList<>();
    private float part;

    private FighterSpeedBrakeOverlay() {}

    public static float[] build() {
        FighterSpeedBrakeOverlay b = new FighterSpeedBrakeOverlay();
        b.part = SPEED_BRAKE;
        b.panel();
        b.part = DETAIL;
        b.hingeHardware();
        float[] a = new float[b.out.size()];
        for (int i=0;i<a.length;i++) a[i]=b.out.get(i);
        return a;
    }

    private void panel() {
        // Low-profile dorsal panel, hinged at its forward edge. It stays flush at 0°
        // and rises into the flow up to 45° under the fighter FCS command.
        prism(new float[][]{
                {-.46f,.918f,.52f},{.46f,.918f,.52f},{.57f,.884f,1.36f},
                {.40f,.838f,1.78f},{-.40f,.838f,1.78f},{-.57f,.884f,1.36f}
        }, .030f);
        // Small central spine gives the panel visible structural depth when open.
        prism(new float[][]{
                {-.055f,.943f,.62f},{.055f,.943f,.62f},{.060f,.914f,1.57f},
                {-.060f,.914f,1.57f}
        }, .018f);
    }

    private void hingeHardware() {
        cylinderBetween(-.40f,.905f,.55f,.40f,.905f,.55f,.022f,12);
        cylinderBetween(-.29f,.885f,.62f,-.29f,.815f,.86f,.012f,8);
        cylinderBetween(.29f,.885f,.62f,.29f,.815f,.86f,.012f,8);
    }

    private void prism(float[][] poly,float t) {
        int n=poly.length;
        float[][] top=new float[n][3],bot=new float[n][3];
        for(int i=0;i<n;i++){
            top[i]=new float[]{poly[i][0],poly[i][1]+t*.5f,poly[i][2]};
            bot[i]=new float[]{poly[i][0],poly[i][1]-t*.5f,poly[i][2]};
        }
        for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}
        for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private void cylinderBetween(float ax,float ay,float az,float bx,float by,float bz,float r,int seg) {
        float dx=bx-ax,dy=by-ay,dz=bz-az;
        float len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz); if(len<1e-5f)return;
        dx/=len;dy/=len;dz/=len;
        float rx=Math.abs(dy)<.9f?0f:1f, ry=Math.abs(dy)<.9f?1f:0f, rz=0f;
        float ux=dy*rz-dz*ry,uy=dz*rx-dx*rz,uz=dx*ry-dy*rx;
        float ul=(float)Math.sqrt(ux*ux+uy*uy+uz*uz);ux/=ul;uy/=ul;uz/=ul;
        float vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux;
        for(int i=0;i<seg;i++){
            double a=2*Math.PI*i/seg,b=2*Math.PI*(i+1)/seg;
            float[] p0={ax+r*(ux*(float)Math.cos(a)+vx*(float)Math.sin(a)),ay+r*(uy*(float)Math.cos(a)+vy*(float)Math.sin(a)),az+r*(uz*(float)Math.cos(a)+vz*(float)Math.sin(a))};
            float[] p1={ax+r*(ux*(float)Math.cos(b)+vx*(float)Math.sin(b)),ay+r*(uy*(float)Math.cos(b)+vy*(float)Math.sin(b)),az+r*(uz*(float)Math.cos(b)+vz*(float)Math.sin(b))};
            float[] q1={p1[0]+(bx-ax),p1[1]+(by-ay),p1[2]+(bz-az)};
            float[] q0={p0[0]+(bx-ax),p0[1]+(by-ay),p0[2]+(bz-az)};
            quad(p0,p1,q1,q0);
        }
    }

    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(l<1e-6f){nx=0;ny=1;nz=0;l=1;} nx/=l;ny/=l;nz/=l;
        put(a,nx,ny,nz);put(b,nx,ny,nz);put(c,nx,ny,nz);
    }
    private void put(float[] v,float nx,float ny,float nz){out.add(v[0]);out.add(v[1]);out.add(v[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);}
}
