package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/** Temporary volumetric fighter mesh used until the production GLB is accepted. */
public final class ProceduralFighterMesh {
    public static final float PART_SKIN=0f, PART_CANOPY=1f, PART_NOZZLE=2f, PART_INTAKE=3f,
            PART_STAB_L=4f, PART_STAB_R=5f, PART_RUDDER_L=6f, PART_RUDDER_R=7f,
            PART_AFTERBURNER=8f, PART_FLAPERON_L=9f, PART_FLAPERON_R=10f,
            PART_CANOPY_FRAME=11f, PART_NOZZLE_INNER=12f, PART_GEAR_STRUT=13f,
            PART_GEAR_WHEEL=14f, PART_GEAR_DOOR=15f;
    public static final class Mesh {
        public final float[] data;
        public Mesh(float[] data){this.data=data;}
        public int vertexCount(){return data.length/7;}
    }
    private final List<Float> out=new ArrayList<>();
    private float part=PART_SKIN;

    public static Mesh build(){
        ProceduralFighterMesh b=new ProceduralFighterMesh();
        b.part=PART_SKIN;
        b.fuselage(); b.chine(-1f); b.chine(1f); b.upperDeck(); b.wing(-1f); b.wing(1f);
        b.part=PART_FLAPERON_L; b.flaperon(-1f); b.part=PART_FLAPERON_R; b.flaperon(1f);
        b.part=PART_STAB_L; b.stabilator(-1f); b.part=PART_STAB_R; b.stabilator(1f);
        b.part=PART_RUDDER_L; b.verticalTail(-1f); b.part=PART_RUDDER_R; b.verticalTail(1f);
        b.part=PART_INTAKE; b.intake(-1f); b.intake(1f);
        b.part=PART_SKIN; b.enginePod(-.68f); b.enginePod(.68f);
        b.part=PART_NOZZLE; b.nozzle(-.68f); b.nozzle(.68f);
        b.part=PART_NOZZLE_INNER; b.nozzleInner(-.68f); b.nozzleInner(.68f);
        b.part=PART_AFTERBURNER; b.afterburner(-.68f); b.afterburner(.68f);
        b.part=PART_CANOPY; b.canopy();
        b.part=PART_CANOPY_FRAME; b.canopyFrame();
        b.part=PART_GEAR_STRUT; b.gearStruts();
        b.part=PART_GEAR_WHEEL; b.gearWheels();
        b.part=PART_GEAR_DOOR; b.gearDoors();
        float[] data=new float[b.out.size()]; for(int i=0;i<data.length;i++) data[i]=b.out.get(i); return new Mesh(data);
    }

    private void fuselage(){
        // AVM-1: longer nose, fuller shoulders, flatter stealth belly and smoother aft taper.
        float[] z={-6.15f,-5.65f,-4.95f,-4.15f,-3.10f,-1.85f,-.55f,.85f,2.20f,3.28f,3.72f};
        float[] rx={.025f,.11f,.27f,.48f,.70f,.90f,1.06f,1.12f,1.00f,.66f,.30f};
        float[] ry={.020f,.09f,.20f,.32f,.44f,.56f,.66f,.72f,.64f,.43f,.20f};
        float[] cy={-.02f,-.02f,-.015f,0f,.015f,.035f,.055f,.045f,.015f,-.035f,-.08f};
        int sides=24;
        for(int s=0;s<z.length-1;s++) for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            quad(new float[]{rx[s]*(float)Math.cos(a0),cy[s]+ry[s]*(float)Math.sin(a0),z[s]},
                 new float[]{rx[s+1]*(float)Math.cos(a0),cy[s+1]+ry[s+1]*(float)Math.sin(a0),z[s+1]},
                 new float[]{rx[s+1]*(float)Math.cos(a1),cy[s+1]+ry[s+1]*(float)Math.sin(a1),z[s+1]},
                 new float[]{rx[s]*(float)Math.cos(a1),cy[s]+ry[s]*(float)Math.sin(a1),z[s]});
        }
    }

    private void chine(float side){
        prism(new float[][]{
                {.12f*side,.14f,-5.55f},{.54f*side,.24f,-4.25f},{1.08f*side,.30f,-3.05f},
                {1.55f*side,.28f,-1.90f},{1.82f*side,.22f,-.75f},{1.76f*side,.20f,.18f},
                {1.18f*side,.27f,.72f}},.10f);
    }
    private void upperDeck(){
        prism(new float[][]{
                {-.46f,.54f,-2.55f},{.46f,.54f,-2.55f},{.72f,.70f,-1.50f},{.80f,.80f,-.15f},
                {.74f,.78f,1.05f},{.58f,.72f,2.45f},{-.58f,.72f,2.45f},{-.74f,.78f,1.05f},
                {-.80f,.80f,-.15f},{-.72f,.70f,-1.50f}},.18f);
    }
    private void wing(float side){
        float y=.04f,t=.16f;
        prism(new float[][]{
                {.72f*side,y+t,-2.55f},{2.08f*side,y+t,-1.72f},{4.92f*side,y+t,-.08f},
                {4.22f*side,y+t,.92f},{3.28f*side,y+t,1.50f},{1.02f*side,y+t,1.95f}},t*2f);
    }
    private void flaperon(float side){float y=.16f,t=.07f;prism(new float[][]{{1.62f*side,y+t,.70f},{3.56f*side,y+t,.78f},{3.34f*side,y+t,1.34f},{1.46f*side,y+t,1.48f}},t*2f);}
    private void stabilator(float side){float y=.18f,t=.10f;prism(new float[][]{{.72f*side,y+t,1.72f},{2.78f*side,y+t,2.24f},{2.22f*side,y+t,3.28f},{.72f*side,y+t,2.88f}},t*2f);}
    private void verticalTail(float side){float[] a={.62f*side,.56f,1.56f},b={1.22f*side,2.38f,2.16f},c={.96f*side,.50f,3.12f};tri(a,b,c);tri(offset(a,0,-.12f,0),offset(c,0,-.12f,0),offset(b,0,-.12f,0));}
    private void intake(float side){float x=1.02f*side;prism(new float[][]{{x,.30f,-2.58f},{1.55f*side,.20f,-1.90f},{1.48f*side,.10f,-.50f},{.94f*side,.24f,-.76f}},.36f);}

    private void enginePod(float x){
        float[] z={-.12f,.85f,1.90f,2.85f,3.22f};
        float[] r={.40f,.54f,.59f,.53f,.44f};
        int sides=20;
        for(int s=0;s<z.length-1;s++)for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            quad(new float[]{x+r[s]*(float)Math.cos(a0),-.11f+r[s]*.58f*(float)Math.sin(a0),z[s]},
                 new float[]{x+r[s+1]*(float)Math.cos(a0),-.11f+r[s+1]*.58f*(float)Math.sin(a0),z[s+1]},
                 new float[]{x+r[s+1]*(float)Math.cos(a1),-.11f+r[s+1]*.58f*(float)Math.sin(a1),z[s+1]},
                 new float[]{x+r[s]*(float)Math.cos(a1),-.11f+r[s]*.58f*(float)Math.sin(a1),z[s]});
        }
    }
    private void nozzle(float x){float z0=3.12f,z1=3.64f,r0=.43f,r1=.34f;int sides=20;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;float ripple=(i%2==0?1.0f:.94f);quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0},new float[]{x+r1*ripple*(float)Math.cos(a0),-.10f+r1*ripple*.60f*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0});}}
    private void nozzleInner(float x){float z0=3.55f,z1=3.77f,r0=.255f,r1=.205f;int sides=18;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*.60f*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0});}}
    private void afterburner(float x){float z0=3.74f,z1=5.10f,r0=.20f,r1=.035f;int sides=16;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.55f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.55f*(float)Math.sin(a1),z0});}}

    private void canopy(){int n=18;for(int s=0;s<3;s++){float z0=-1.78f+s*.78f,z1=z0+.78f,rx0=.52f-.09f*s,rx1=.43f-.09f*s,h0=.77f+.18f*s,h1=.96f+.09f*s;for(int i=0;i<n/2;i++){double a0=Math.PI*i/(n/2),a1=Math.PI*(i+1)/(n/2);quad(new float[]{rx0*(float)Math.cos(a0),h0+.35f*(float)Math.sin(a0),z0},new float[]{rx1*(float)Math.cos(a0),h1+.29f*(float)Math.sin(a0),z1},new float[]{rx1*(float)Math.cos(a1),h1+.29f*(float)Math.sin(a1),z1},new float[]{rx0*(float)Math.cos(a1),h0+.35f*(float)Math.sin(a1),z0});}}}
    private void canopyFrame(){prism(new float[][]{{-.58f,.77f,-1.80f},{.58f,.77f,-1.80f},{.48f,.80f,-1.62f},{-.48f,.80f,-1.62f}},.07f);prism(new float[][]{{-.40f,.97f,-.42f},{.40f,.97f,-.42f},{.36f,1.02f,-.27f},{-.36f,1.02f,-.27f}},.055f);prism(new float[][]{{-.31f,.92f,.48f},{.31f,.92f,.48f},{.27f,.95f,.62f},{-.27f,.95f,.62f}},.05f);}

    private void gearStruts(){
        box(-.075f,-1.52f,-3.72f,.15f,1.10f,.16f);
        box(-1.72f,-1.48f,.62f,.18f,1.18f,.18f); box(1.54f,-1.48f,.62f,.18f,1.18f,.18f);
        box(-1.70f,-.98f,.55f,.16f,.18f,.84f); box(1.54f,-.98f,.55f,.16f,.18f,.84f);
    }
    private void gearWheels(){
        wheel(0f,-1.66f,-3.72f,.26f,.18f);
        wheel(-1.72f,-1.72f,1.12f,.38f,.24f); wheel(1.72f,-1.72f,1.12f,.38f,.24f);
    }
    private void gearDoors(){
        prism(new float[][]{{-.30f,-.53f,-4.18f},{-.03f,-.53f,-4.18f},{-.03f,-.53f,-3.23f},{-.30f,-.53f,-3.23f}},.045f);
        prism(new float[][]{{.03f,-.53f,-4.18f},{.30f,-.53f,-4.18f},{.30f,-.53f,-3.23f},{.03f,-.53f,-3.23f}},.045f);
        prism(new float[][]{{-1.82f,-.55f,.22f},{-.88f,-.55f,.22f},{-.88f,-.55f,1.56f},{-1.82f,-.55f,1.56f}},.055f);
        prism(new float[][]{{.88f,-.55f,.22f},{1.82f,-.55f,.22f},{1.82f,-.55f,1.56f},{.88f,-.55f,1.56f}},.055f);
    }
    private void box(float x,float y,float z,float sx,float sy,float sz){float hx=sx*.5f,hz=sz*.5f;prism(new float[][]{{x-hx,y+sy,z-hz},{x+hx,y+sy,z-hz},{x+hx,y+sy,z+hz},{x-hx,y+sy,z+hz}},sy);}
    private void wheel(float x,float y,float z,float r,float width){int n=16;float x0=x-width*.5f,x1=x+width*.5f;for(int i=0;i<n;i++){double a0=2*Math.PI*i/n,a1=2*Math.PI*(i+1)/n;float[] a={x0,y+r*(float)Math.cos(a0),z+r*(float)Math.sin(a0)},b={x1,y+r*(float)Math.cos(a0),z+r*(float)Math.sin(a0)},c={x1,y+r*(float)Math.cos(a1),z+r*(float)Math.sin(a1)},d={x0,y+r*(float)Math.cos(a1),z+r*(float)Math.sin(a1)};quad(a,b,c,d);} }

    private void prism(float[][] top,float thickness){int n=top.length;float[][] bot=new float[n][3];for(int i=0;i<n;i++)bot[i]=offset(top[i],0,-thickness,0);for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);}
    private static float[] offset(float[] p,float x,float y,float z){return new float[]{p[0]+x,p[1]+y,p[2]+z};}
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1f;emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);}
    private void emit(float[] p,float nx,float ny,float nz){out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);}
}
