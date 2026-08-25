package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/** Temporary volumetric fighter mesh used until the production GLB is accepted. */
public final class ProceduralFighterMesh {
    public static final class Mesh {
        public final float[] data; // position xyz + normal xyz
        public Mesh(float[] data) { this.data = data; }
        public int vertexCount() { return data.length / 6; }
    }

    private final List<Float> out = new ArrayList<>();

    public static Mesh build() {
        ProceduralFighterMesh b = new ProceduralFighterMesh();
        b.fuselage();
        b.wing(-1f); b.wing(1f);
        b.stabilator(-1f); b.stabilator(1f);
        b.verticalTail(-1f); b.verticalTail(1f);
        b.enginePod(-0.62f); b.enginePod(0.62f);
        b.canopy();
        float[] data = new float[b.out.size()];
        for (int i=0;i<data.length;i++) data[i]=b.out.get(i);
        return new Mesh(data);
    }

    private void fuselage() {
        float[] z={-5.2f,-4.2f,-3.0f,-1.2f,0.8f,2.5f,3.6f};
        float[] rx={0.05f,0.38f,0.72f,0.95f,1.03f,0.78f,0.28f};
        float[] ry={0.05f,0.32f,0.58f,0.72f,0.78f,0.60f,0.22f};
        int sides=14;
        for(int s=0;s<z.length-1;s++){
            for(int i=0;i<sides;i++){
                double a0=2*Math.PI*i/sides, a1=2*Math.PI*(i+1)/sides;
                float[] p00={rx[s]*(float)Math.cos(a0),ry[s]*(float)Math.sin(a0),z[s]};
                float[] p01={rx[s]*(float)Math.cos(a1),ry[s]*(float)Math.sin(a1),z[s]};
                float[] p10={rx[s+1]*(float)Math.cos(a0),ry[s+1]*(float)Math.sin(a0),z[s+1]};
                float[] p11={rx[s+1]*(float)Math.cos(a1),ry[s+1]*(float)Math.sin(a1),z[s+1]};
                quad(p00,p10,p11,p01);
            }
        }
    }

    private void wing(float side){
        float y=0.04f, t=0.16f;
        float[][] top={{0.72f*side,y+t,-2.0f},{4.55f*side,y+t,0.10f},{3.25f*side,y+t,1.15f},{0.85f*side,y+t,1.9f}};
        prism(top,t*2f);
    }

    private void stabilator(float side){
        float y=0.16f,t=0.10f;
        float[][] top={{0.72f*side,y+t,1.65f},{2.65f*side,y+t,2.15f},{2.20f*side,y+t,3.15f},{0.72f*side,y+t,2.8f}};
        prism(top,t*2f);
    }

    private void verticalTail(float side){
        float x=0.58f*side;
        float[] a={x,0.55f,1.55f}, b={1.18f*side,2.20f,2.15f}, c={0.94f*side,0.48f,3.0f};
        tri(a,b,c); tri(offset(a,0,-0.10f,0),offset(c,0,-0.10f,0),offset(b,0,-0.10f,0));
    }

    private void enginePod(float x){
        float[] z={0.5f,1.9f,3.25f}; float[] r={0.46f,0.52f,0.42f}; int sides=12;
        for(int s=0;s<z.length-1;s++) for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] p00={x+r[s]*(float)Math.cos(a0),-0.12f+r[s]*0.62f*(float)Math.sin(a0),z[s]};
            float[] p01={x+r[s]*(float)Math.cos(a1),-0.12f+r[s]*0.62f*(float)Math.sin(a1),z[s]};
            float[] p10={x+r[s+1]*(float)Math.cos(a0),-0.12f+r[s+1]*0.62f*(float)Math.sin(a0),z[s+1]};
            float[] p11={x+r[s+1]*(float)Math.cos(a1),-0.12f+r[s+1]*0.62f*(float)Math.sin(a1),z[s+1]};
            quad(p00,p10,p11,p01);
        }
    }

    private void canopy(){
        int n=12;
        for(int s=0;s<3;s++){
            float z0=-1.5f+s*0.75f,z1=z0+0.75f;
            float rx0=0.48f-0.08f*s,rx1=0.40f-0.09f*s;
            float h0=0.78f+0.18f*s,h1=0.96f+0.08f*s;
            for(int i=0;i<n/2;i++){
                double a0=Math.PI*i/(n/2),a1=Math.PI*(i+1)/(n/2);
                float[] p00={rx0*(float)Math.cos(a0),h0+0.30f*(float)Math.sin(a0),z0};
                float[] p01={rx0*(float)Math.cos(a1),h0+0.30f*(float)Math.sin(a1),z0};
                float[] p10={rx1*(float)Math.cos(a0),h1+0.26f*(float)Math.sin(a0),z1};
                float[] p11={rx1*(float)Math.cos(a1),h1+0.26f*(float)Math.sin(a1),z1};
                quad(p00,p10,p11,p01);
            }
        }
    }

    private void prism(float[][] top,float thickness){
        int n=top.length; float[][] bot=new float[n][3];
        for(int i=0;i<n;i++) bot[i]=offset(top[i],0,-thickness,0);
        for(int i=1;i<n-1;i++){ tri(top[0],top[i],top[i+1]); tri(bot[0],bot[i+1],bot[i]); }
        for(int i=0;i<n;i++) quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private static float[] offset(float[] p,float x,float y,float z){return new float[]{p[0]+x,p[1]+y,p[2]+z};}
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz); if(l<1e-6f) l=1f; nx/=l;ny/=l;nz/=l;
        emit(a,nx,ny,nz);emit(b,nx,ny,nz);emit(c,nx,ny,nz);
    }
    private void emit(float[] p,float nx,float ny,float nz){out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);}
}
