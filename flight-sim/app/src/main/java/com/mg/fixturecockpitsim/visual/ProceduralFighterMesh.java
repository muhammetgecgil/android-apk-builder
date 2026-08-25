package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/** Temporary volumetric fighter mesh used until the production GLB is accepted. */
public final class ProceduralFighterMesh {
    public static final float PART_SKIN=0f, PART_CANOPY=1f, PART_NOZZLE=2f, PART_INTAKE=3f,
            PART_STAB_L=4f, PART_STAB_R=5f, PART_RUDDER_L=6f, PART_RUDDER_R=7f, PART_AFTERBURNER=8f;
    public static final class Mesh {
        public final float[] data; // position xyz + normal xyz + part id
        public Mesh(float[] data) { this.data=data; }
        public int vertexCount(){ return data.length/7; }
    }
    private final List<Float> out=new ArrayList<>();
    private float part=PART_SKIN;

    public static Mesh build(){
        ProceduralFighterMesh b=new ProceduralFighterMesh();
        b.part=PART_SKIN; b.fuselage(); b.wing(-1f); b.wing(1f);
        b.part=PART_STAB_L; b.stabilator(-1f); b.part=PART_STAB_R; b.stabilator(1f);
        b.part=PART_RUDDER_L; b.verticalTail(-1f); b.part=PART_RUDDER_R; b.verticalTail(1f);
        b.part=PART_INTAKE; b.intake(-1f); b.intake(1f);
        b.part=PART_SKIN; b.enginePod(-0.62f); b.enginePod(0.62f);
        b.part=PART_NOZZLE; b.nozzle(-0.62f); b.nozzle(0.62f);
        b.part=PART_AFTERBURNER; b.afterburner(-0.62f); b.afterburner(0.62f);
        b.part=PART_CANOPY; b.canopy();
        float[] data=new float[b.out.size()]; for(int i=0;i<data.length;i++) data[i]=b.out.get(i); return new Mesh(data);
    }

    private void fuselage(){
        float[] z={-5.6f,-4.7f,-3.4f,-1.5f,0.6f,2.5f,3.7f},rx={0.04f,0.28f,0.62f,0.94f,1.05f,0.82f,0.30f},ry={0.04f,0.24f,0.48f,0.68f,0.78f,0.60f,0.22f};
        int sides=18; for(int s=0;s<z.length-1;s++) for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{rx[s]*(float)Math.cos(a0),ry[s]*(float)Math.sin(a0),z[s]},new float[]{rx[s+1]*(float)Math.cos(a0),ry[s+1]*(float)Math.sin(a0),z[s+1]},new float[]{rx[s+1]*(float)Math.cos(a1),ry[s+1]*(float)Math.sin(a1),z[s+1]},new float[]{rx[s]*(float)Math.cos(a1),ry[s]*(float)Math.sin(a1),z[s]});}
    }
    private void wing(float side){float y=.05f,t=.17f;prism(new float[][]{{.72f*side,y+t,-2.2f},{4.65f*side,y+t,-.05f},{3.35f*side,y+t,1.25f},{.86f*side,y+t,1.95f}},t*2f);}
    private void stabilator(float side){float y=.18f,t=.10f;prism(new float[][]{{.72f*side,y+t,1.7f},{2.75f*side,y+t,2.25f},{2.20f*side,y+t,3.25f},{.72f*side,y+t,2.85f}},t*2f);}
    private void verticalTail(float side){float[] a={.60f*side,.55f,1.55f},b={1.20f*side,2.35f,2.15f},c={.94f*side,.48f,3.1f};tri(a,b,c);tri(offset(a,0,-.11f,0),offset(c,0,-.11f,0),offset(b,0,-.11f,0));}
    private void intake(float side){float x=1f*side;prism(new float[][]{{x,.28f,-2.5f},{1.52f*side,.18f,-1.85f},{1.46f*side,.10f,-.55f},{.92f*side,.22f,-.78f}},.34f);}
    private void enginePod(float x){float[] z={.2f,1.9f,3.15f},r={.46f,.54f,.43f};int sides=16;for(int s=0;s<z.length-1;s++)for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r[s]*(float)Math.cos(a0),-.10f+r[s]*.60f*(float)Math.sin(a0),z[s]},new float[]{x+r[s+1]*(float)Math.cos(a0),-.10f+r[s+1]*.60f*(float)Math.sin(a0),z[s+1]},new float[]{x+r[s+1]*(float)Math.cos(a1),-.10f+r[s+1]*.60f*(float)Math.sin(a1),z[s+1]},new float[]{x+r[s]*(float)Math.cos(a1),-.10f+r[s]*.60f*(float)Math.sin(a1),z[s]});}}
    private void nozzle(float x){float z0=3.12f,z1=3.62f,r0=.43f,r1=.34f;int sides=18;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*.60f*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0});}}
    private void afterburner(float x){float z0=3.60f,z1=5.05f,r0=.25f,r1=.04f;int sides=14;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.55f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.55f*(float)Math.sin(a1),z0});}}
    private void canopy(){int n=16;for(int s=0;s<3;s++){float z0=-1.75f+s*.78f,z1=z0+.78f,rx0=.52f-.09f*s,rx1=.43f-.09f*s,h0=.76f+.18f*s,h1=.94f+.09f*s;for(int i=0;i<n/2;i++){double a0=Math.PI*i/(n/2),a1=Math.PI*(i+1)/(n/2);quad(new float[]{rx0*(float)Math.cos(a0),h0+.34f*(float)Math.sin(a0),z0},new float[]{rx1*(float)Math.cos(a0),h1+.28f*(float)Math.sin(a0),z1},new float[]{rx1*(float)Math.cos(a1),h1+.28f*(float)Math.sin(a1),z1},new float[]{rx0*(float)Math.cos(a1),h0+.34f*(float)Math.sin(a1),z0});}}}
    private void prism(float[][] top,float thickness){int n=top.length;float[][] bot=new float[n][3];for(int i=0;i<n;i++)bot[i]=offset(top[i],0,-thickness,0);for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);}
    private static float[] offset(float[] p,float x,float y,float z){return new float[]{p[0]+x,p[1]+y,p[2]+z};}
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);} private void tri(float[] a,float[] b,float[] c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1f;emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);} private void emit(float[] p,float nx,float ny,float nz){out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);}
}
