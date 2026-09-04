package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;

/** GPU terrain mesh used by the cinematic route. Vertex layout: xyz normal xyz partId. */
public final class CinematicTerrainMesh {
    private CinematicTerrainMesh(){}

    public static float[] build(int kind){
        final int nx=32,nz=24; final float xmin=-48f,xmax=48f,zNear=-18f,zFar=-225f;
        ArrayList<Float> out=new ArrayList<>((nx-1)*(nz-1)*6*7);
        for(int iz=0;iz<nz-1;iz++)for(int ix=0;ix<nx-1;ix++){
            float x0=lerp(xmin,xmax,ix/(float)(nx-1)),x1=lerp(xmin,xmax,(ix+1)/(float)(nx-1));
            float z0=lerp(zNear,zFar,iz/(float)(nz-1)),z1=lerp(zNear,zFar,(iz+1)/(float)(nz-1));
            float y00=h(kind,x0,z0),y10=h(kind,x1,z0),y01=h(kind,x0,z1),y11=h(kind,x1,z1);
            tri(out,kind,x0,y00,z0,x1,y10,z0,x1,y11,z1);
            tri(out,kind,x0,y00,z0,x1,y11,z1,x0,y01,z1);
        }
        float[] a=new float[out.size()];for(int i=0;i<a.length;i++)a[i]=out.get(i);return a;
    }

    private static float h(int kind,float x,float z){
        float d=(-z-18f)/207f; d=cl(d,0,1);
        if(kind==0){
            float r=.60f*(float)Math.abs(Math.sin(x*.105+z*.037))+.27f*(float)Math.abs(Math.sin(x*.041-z*.082+1.4))+.13f*(float)Math.abs(Math.sin(x*.29+z*.019));
            return -9.4f+(5.5f+18.5f*d)*r;
        }
        if(kind==1){
            float coast=cl((x+8f)/20f,0,1),r=.62f*(float)Math.abs(Math.sin(x*.12+z*.044))+.38f*(float)Math.abs(Math.sin(x*.055-z*.073));
            return -9.2f+coast*(2.2f+10.5f*d)*r;
        }
        if(kind==2){
            float dune=.55f+.45f*(float)Math.sin(x*.24+z*.105);float ripple=.5f+.5f*(float)Math.sin(x*.52-z*.31);
            return -8.7f+2.8f*dune+1.15f*ripple*d;
        }
        if(kind==3){
            float dune=.52f+.48f*(float)Math.sin(x*.17+z*.064);float mesa=(float)Math.abs(Math.sin(x*.047-z*.038));
            return -8.8f+2.1f*dune+3.0f*mesa*d;
        }
        float r=.6f*(float)Math.abs(Math.sin(x*.095+z*.039))+.4f*(float)Math.abs(Math.sin(x*.031-z*.061+2.0));
        return -9.4f+(2.5f+9.0f*d)*r;
    }

    private static void tri(ArrayList<Float> o,int kind,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz){
        float ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az;
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;float m=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(m<1e-5f){nx=0;ny=1;nz=0;m=1;}nx/=m;ny/=m;nz/=m;
        float part=kind==4?65f:60f+kind;v(o,ax,ay,az,nx,ny,nz,part);v(o,bx,by,bz,nx,ny,nz,part);v(o,cx,cy,cz,nx,ny,nz,part);
    }
    private static void v(ArrayList<Float> o,float x,float y,float z,float nx,float ny,float nz,float p){o.add(x);o.add(y);o.add(z);o.add(nx);o.add(ny);o.add(nz);o.add(p);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
