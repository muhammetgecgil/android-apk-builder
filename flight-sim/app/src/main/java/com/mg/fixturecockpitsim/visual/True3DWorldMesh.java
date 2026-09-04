package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;

/**
 * Procedural OpenGL world geometry used by Jet3DView.
 *
 * Coordinates are local-world units around the aircraft.  The renderer moves the
 * mesh beneath the aircraft as flight progresses.  Every vertex uses the same
 * seven-float layout as the aircraft meshes: xyz, normal xyz, part id.
 */
public final class True3DWorldMesh {
    public static final float PART_WORLD = 60f;
    private True3DWorldMesh() {}

    public static float[] sea() {
        ArrayList<Float> a = new ArrayList<>();
        final int nx=26, nz=34;
        final float x0=-132f, x1=132f, z0=-230f, z1=105f;
        for(int iz=0; iz<nz; iz++){
            float za=lerp(z0,z1,iz/(float)nz), zb=lerp(z0,z1,(iz+1f)/nz);
            for(int ix=0; ix<nx; ix++){
                float xa=lerp(x0,x1,ix/(float)nx), xb=lerp(x0,x1,(ix+1f)/nx);
                float[] A={xa,seaY(xa,za),za}, B={xa,seaY(xa,zb),zb}, C={xb,seaY(xb,zb),zb}, D={xb,seaY(xb,za),za};
                tri(a,A,B,C,PART_WORLD);tri(a,A,C,D,PART_WORLD);
            }
        }
        return out(a);
    }

    /** Low mainland surface with an irregular, non-straight coastline. */
    public static float[] coastLand() {
        ArrayList<Float> a = new ArrayList<>();
        final int nx=28, nz=38;
        final float x0=-132f, x1=44f, z0=-230f, z1=105f;
        for(int iz=0; iz<nz; iz++){
            float za=lerp(z0,z1,iz/(float)nz), zb=lerp(z0,z1,(iz+1f)/nz);
            for(int ix=0; ix<nx; ix++){
                float xa=lerp(x0,x1,ix/(float)nx), xb=lerp(x0,x1,(ix+1f)/nx);
                float xc=(xa+xb)*.5f, zc=(za+zb)*.5f;
                if(xc>coastX(zc)) continue;
                float[] A={xa,landY(xa,za),za}, B={xa,landY(xa,zb),zb}, C={xb,landY(xb,zb),zb}, D={xb,landY(xb,za),za};
                tri(a,A,B,C,PART_WORLD);tri(a,A,C,D,PART_WORLD);
            }
        }
        return out(a);
    }

    /** Sloped beach/rock shelf making the coast visibly three-dimensional. */
    public static float[] shoreline() {
        ArrayList<Float> a = new ArrayList<>();
        final int n=72; final float z0=-230f,z1=105f;
        for(int i=0;i<n;i++){
            float za=lerp(z0,z1,i/(float)n),zb=lerp(z0,z1,(i+1f)/n);
            float ca=coastX(za),cb=coastX(zb);
            float[] A={ca-4.2f,.30f,za},B={cb-4.2f,.30f,zb},C={cb+1.4f,.025f,zb},D={ca+1.4f,.025f,za};
            tri(a,A,B,C,PART_WORLD);tri(a,A,C,D,PART_WORLD);
        }
        return out(a);
    }

    /** Several actual island bodies, each with a beach ring and raised interior. */
    public static float[] islands() {
        ArrayList<Float> a=new ArrayList<>();
        addIsland(a, 36f,-54f,13f,8f,3.0f,0.35f);
        addIsland(a, 62f,-112f,19f,11f,4.1f,1.15f);
        addIsland(a, 22f,-166f,10f,6.5f,2.3f,2.25f);
        addIsland(a, 76f,-196f,8f,5f,1.7f,3.10f);
        return out(a);
    }

    /** Mountain ridges are independent solid triangle meshes, not painted silhouettes. */
    public static float[] mountains() {
        ArrayList<Float> a=new ArrayList<>();
        addMountain(a,-83f,-42f,22f,15f,10.8f,.25f);
        addMountain(a,-56f,-78f,17f,13f,8.2f,1.4f);
        addMountain(a,-104f,-118f,27f,18f,12.6f,2.2f);
        addMountain(a,-70f,-158f,24f,16f,10.0f,3.0f);
        addMountain(a,-111f,-196f,20f,14f,8.8f,4.2f);
        // A modest mountainous island so islands do not look like flat plates.
        addMountain(a,62f,-112f,9.5f,6.5f,4.6f,1.8f);
        return out(a);
    }

    /** Optional cold-weather summit caps rendered only when the weather is cold. */
    public static float[] snowCaps() {
        ArrayList<Float> a=new ArrayList<>();
        addCap(a,-83f,-42f,7.2f,5.0f,10.9f);
        addCap(a,-104f,-118f,8.5f,5.7f,12.7f);
        addCap(a,-70f,-158f,6.8f,4.6f,10.1f);
        return out(a);
    }

    private static void addIsland(ArrayList<Float>a,float cx,float cz,float rx,float rz,float h,float phase){
        final int seg=30,rings=7;
        float[][] prev=null;
        for(int r=0;r<=rings;r++){
            float q=r/(float)rings;
            float[][] cur=new float[seg][3];
            for(int i=0;i<seg;i++){
                float ang=(float)(Math.PI*2*i/seg);
                float noise=1f+.08f*(float)Math.sin(i*2.7+phase)+.045f*(float)Math.sin(i*5.1+phase*.7);
                float rr=q*noise;
                float x=cx+(float)Math.cos(ang)*rx*rr,z=cz+(float)Math.sin(ang)*rz*rr;
                float crown=(float)Math.pow(Math.max(0,1-q),1.45);
                float y=.055f+h*crown+.13f*(float)Math.sin(ang*3+phase)*(1-q);
                cur[i]=new float[]{x,y,z};
            }
            if(r==0){
                float[] center={cx,h+.055f,cz};
                for(int i=0;i<seg;i++)tri(a,center,cur[i],cur[(i+1)%seg],PART_WORLD);
            }else if(prev!=null){
                for(int i=0;i<seg;i++){
                    int j=(i+1)%seg;
                    tri(a,prev[i],cur[i],cur[j],PART_WORLD);tri(a,prev[i],cur[j],prev[j],PART_WORLD);
                }
            }
            prev=cur;
        }
    }

    private static void addMountain(ArrayList<Float>a,float cx,float cz,float rx,float rz,float h,float phase){
        final int seg=28,rings=6;
        float[][] prev=null;
        for(int r=0;r<=rings;r++){
            float q=r/(float)rings;
            float[][] cur=new float[seg][3];
            for(int i=0;i<seg;i++){
                float ang=(float)(Math.PI*2*i/seg);
                float jag=1f+.10f*(float)Math.sin(i*2.3+phase)+.055f*(float)Math.sin(i*4.9+phase*.4);
                float rr=q*jag;
                float x=cx+(float)Math.cos(ang)*rx*rr,z=cz+(float)Math.sin(ang)*rz*rr;
                float ridge=(float)Math.pow(Math.max(0,1-q),1.18);
                float asym=.82f+.18f*(float)Math.sin(ang+phase);
                float y=.55f+h*ridge*asym;
                cur[i]=new float[]{x,y,z};
            }
            if(r==0){
                float[] peak={cx,.55f+h,cz};
                for(int i=0;i<seg;i++)tri(a,peak,cur[i],cur[(i+1)%seg],PART_WORLD);
            }else if(prev!=null){
                for(int i=0;i<seg;i++){
                    int j=(i+1)%seg;
                    tri(a,prev[i],cur[i],cur[j],PART_WORLD);tri(a,prev[i],cur[j],prev[j],PART_WORLD);
                }
            }
            prev=cur;
        }
    }

    private static void addCap(ArrayList<Float>a,float cx,float cz,float rx,float rz,float y){
        final int seg=24;
        float[] top={cx,y+.18f,cz};
        for(int i=0;i<seg;i++){
            float a0=(float)(Math.PI*2*i/seg),a1=(float)(Math.PI*2*(i+1)/seg);
            float[] A={cx+(float)Math.cos(a0)*rx,y-1.25f,cz+(float)Math.sin(a0)*rz};
            float[] B={cx+(float)Math.cos(a1)*rx,y-1.25f,cz+(float)Math.sin(a1)*rz};
            tri(a,top,A,B,PART_WORLD);
        }
    }

    private static float seaY(float x,float z){
        return .065f*(float)Math.sin(x*.16+z*.085)+.038f*(float)Math.sin(z*.23-x*.07+1.3);
    }
    private static float coastX(float z){
        return -22f+13f*(float)Math.sin(z*.036+1.1)+6.5f*(float)Math.sin(z*.081-.45)+3f*(float)Math.sin(z*.17);
    }
    private static float landY(float x,float z){
        float inland=Math.max(0,coastX(z)-x);
        return .28f+.0045f*inland+.16f*(float)Math.sin(x*.08+z*.045)+.08f*(float)Math.sin(z*.14-x*.03);
    }

    private static void tri(ArrayList<Float>a,float[]A,float[]B,float[]C,float part){
        float ux=B[0]-A[0],uy=B[1]-A[1],uz=B[2]-A[2];
        float vx=C[0]-A[0],vy=C[1]-A[1],vz=C[2]-A[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f){nx=0;ny=1;nz=0;l=1;}
        nx/=l;ny/=l;nz/=l;put(a,A,nx,ny,nz,part);put(a,B,nx,ny,nz,part);put(a,C,nx,ny,nz,part);
    }
    private static void put(ArrayList<Float>a,float[]v,float nx,float ny,float nz,float p){
        a.add(v[0]);a.add(v[1]);a.add(v[2]);a.add(nx);a.add(ny);a.add(nz);a.add(p);
    }
    private static float[] out(ArrayList<Float>a){float[] r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
