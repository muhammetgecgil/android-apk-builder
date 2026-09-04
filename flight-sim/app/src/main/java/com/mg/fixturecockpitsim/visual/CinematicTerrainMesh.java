package com.mg.fixturecockpitsim.visual;

import java.util.Arrays;

/**
 * High-detail GPU terrain used by the cinematic route.
 * Vertex layout: xyz, normal xyz, material/part id.
 *
 * The mountains are real OpenGL geometry, not a painted backdrop. Toros uses a
 * dense ridged height field plus closed irregular rock massifs so silhouettes,
 * slopes and snow caps react to the same lighting/depth system as the aircraft.
 */
public final class CinematicTerrainMesh {
    private CinematicTerrainMesh(){}

    public static final float PART_TOROS=60f;
    public static final float PART_AEGEAN=61f;
    public static final float PART_PATARA=62f;
    public static final float PART_KARAPINAR=63f;
    public static final float PART_MOONLIT=65f;
    public static final float PART_ROCK=66f;
    public static final float PART_SNOW=67f;
    public static final float PART_CLIFF=68f;

    public static float[] build(int kind){
        final int nx=kind==0?72:60;
        final int nz=kind==0?58:48;
        final float xmin=kind==0?-86f:-68f;
        final float xmax=kind==0?86f:68f;
        final float zNear=-14f;
        final float zFar=kind==0?-315f:-270f;
        Builder out=new Builder((nx-1)*(nz-1)*6*7+18000);

        for(int iz=0;iz<nz-1;iz++){
            float tz0=iz/(float)(nz-1),tz1=(iz+1)/(float)(nz-1);
            float z0=lerp(zNear,zFar,tz0),z1=lerp(zNear,zFar,tz1);
            for(int ix=0;ix<nx-1;ix++){
                float tx0=ix/(float)(nx-1),tx1=(ix+1)/(float)(nx-1);
                float x0=lerp(xmin,xmax,tx0),x1=lerp(xmin,xmax,tx1);
                float y00=h(kind,x0,z0),y10=h(kind,x1,z0),y01=h(kind,x0,z1),y11=h(kind,x1,z1);
                sv(out,kind,x0,y00,z0,material(kind,x0,y00,z0));
                sv(out,kind,x1,y10,z0,material(kind,x1,y10,z0));
                sv(out,kind,x1,y11,z1,material(kind,x1,y11,z1));
                sv(out,kind,x0,y00,z0,material(kind,x0,y00,z0));
                sv(out,kind,x1,y11,z1,material(kind,x1,y11,z1));
                sv(out,kind,x0,y01,z1,material(kind,x0,y01,z1));
            }
        }

        if(kind==0)appendTorosMassifs(out);
        else if(kind==1)appendAegeanIslands(out);
        else if(kind==4)appendMoonlitPeaks(out);
        return out.toArray();
    }

    private static float h(int kind,float x,float z){
        float d=cl((-z-14f)/301f,0,1);
        if(kind==0){
            // Multi-scale ridged terrain: broad Taurus massifs, secondary ridges,
            // narrow rock ribs and deep valleys. Height grows into the scene.
            float a=ridged(x*.054f+z*.025f+.55f*(float)Math.sin(z*.011f));
            float b=ridged(x*.096f-z*.039f+1.73f);
            float c=ridged(x*.177f+z*.066f+2.41f);
            float rough=.5f+.5f*(float)Math.sin(x*.31f-z*.117f+(float)Math.sin(x*.043f)*1.8f);
            float massif=cl(.58f*pow(a,1.45f)+.28f*pow(b,1.75f)+.12f*pow(c,2.0f)+.07f*rough,0,1.12f);
            float valley=.82f+.18f*ridged(x*.022f-z*.018f+1.1f);
            return -10.8f+(5.5f+35.5f*d)*massif*valley+1.35f*d*(float)Math.sin(x*.071f+z*.021f);
        }
        if(kind==1){
            // Aegean coast: a low sea shelf on the left and steep coastal hills
            // on the right. Closed island massifs are appended below.
            float coast=smooth(cl((x+11f)/24f,0,1));
            float r=.56f*pow(ridged(x*.081f+z*.033f),1.45f)+.31f*pow(ridged(x*.137f-z*.051f+1.2f),1.65f)+.13f*ridged(x*.23f+z*.074f);
            return -10.1f+coast*(2.5f+15.5f*d)*r;
        }
        if(kind==2){
            float dune=.5f+.5f*(float)Math.sin(x*.19f+z*.094f+.75f*(float)Math.sin(z*.031f));
            float ripple=.5f+.5f*(float)Math.sin(x*.49f-z*.287f);
            return -9.2f+3.5f*pow(dune,1.45f)+1.25f*ripple*(.35f+.65f*d);
        }
        if(kind==3){
            float dune=.5f+.5f*(float)Math.sin(x*.145f+z*.061f);
            float mesa=pow(ridged(x*.043f-z*.034f),1.35f);
            float cx=x*.72f,cz=z+145f,rr=(float)Math.sqrt(cx*cx+cz*cz);
            float crater=7.2f*(float)Math.exp(-sq((rr-25f)/8.5f))-4.8f*(float)Math.exp(-sq(rr/12.0f));
            return -9.4f+2.15f*dune+3.8f*mesa*d+crater;
        }
        float r=.60f*pow(ridged(x*.071f+z*.031f),1.55f)+.40f*pow(ridged(x*.028f-z*.054f+2.0f),1.7f);
        return -10.0f+(3.0f+13.5f*d)*r;
    }

    private static float material(int kind,float x,float y,float z){
        if(kind==0){
            if(y>15.5f)return PART_SNOW;
            if(y>2.5f || Math.abs((float)Math.sin(x*.11f-z*.07f))>.83f)return PART_ROCK;
            return PART_TOROS;
        }
        if(kind==1)return y>1.8f?PART_CLIFF:PART_AEGEAN;
        if(kind==2)return PART_PATARA;
        if(kind==3)return PART_KARAPINAR;
        return PART_MOONLIT;
    }

    private static void sv(Builder o,int kind,float x,float y,float z,float part){
        float e=.42f;
        float nx=h(kind,x-e,z)-h(kind,x+e,z);
        float ny=2f*e;
        float nz=h(kind,x,z-e)-h(kind,x,z+e);
        float m=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(m<1e-6f){nx=0;ny=1;nz=0;m=1;}
        fv(o,x,y,z,nx/m,ny/m,nz/m,part);
    }

    private static void appendTorosMassifs(Builder o){
        // Irregular closed rock bodies sit on top of the dense terrain. They add
        // readable parallax and side faces when the camera is low or banking.
        for(int i=0;i<28;i++){
            float z=-56f-i*8.25f;
            float x=(float)Math.sin(i*2.173f+0.4f)*(48f+8f*(i%3)/2f)+(i%2==0?-7f:7f);
            float base=h(0,x,z)-.15f;
            float height=5.0f+(i%7)*1.15f+4.2f*cl((-z-56f)/230f,0,1);
            float radius=2.7f+(i%5)*.62f;
            massif(o,x,base,z,radius,height,i,PART_ROCK,(base+height>16f)?PART_SNOW:PART_ROCK);
        }
        // A few broad foreground ridges stop the Taurus from reading as a flat
        // horizon strip.
        for(int i=0;i<7;i++){
            float z=-72f-i*27f,x=(i%2==0?-34f:31f)+(float)Math.sin(i*1.31f)*9f;
            float base=h(0,x,z)-.2f;
            massif(o,x,base,z,6.0f+i*.35f,7.5f+i*.9f,90+i,PART_ROCK,(base+8f+i*.9f>15f)?PART_SNOW:PART_ROCK);
        }
    }

    private static void appendAegeanIslands(Builder o){
        for(int i=0;i<9;i++){
            float z=-78f-i*18f,x=-33f+(float)Math.sin(i*1.73f)*18f;
            float base=-9.7f;
            massif(o,x,base,z,3.0f+(i%4)*.7f,3.4f+(i%5)*.75f,150+i,PART_CLIFF,PART_CLIFF);
        }
    }

    private static void appendMoonlitPeaks(Builder o){
        for(int i=0;i<11;i++){
            float z=-88f-i*14.5f,x=(float)Math.sin(i*1.91f)*42f;
            float base=h(4,x,z)-.2f;
            massif(o,x,base,z,3.4f+(i%3)*.8f,4.8f+(i%4)*1.0f,220+i,PART_MOONLIT,PART_MOONLIT);
        }
    }

    private static void massif(Builder o,float cx,float base,float cz,float radius,float height,int seed,float lowerPart,float upperPart){
        final int n=9;
        float midY=base+height*.58f,topY=base+height;
        float topX=cx+(float)Math.sin(seed*1.17f)*radius*.18f;
        float topZ=cz+(float)Math.cos(seed*.93f)*radius*.15f;
        for(int i=0;i<n;i++){
            float a0=(float)(Math.PI*2*i/n),a1=(float)(Math.PI*2*(i+1)/n);
            float r0=radius*(.82f+.18f*(float)Math.sin(seed*1.37f+i*2.11f));
            float r1=radius*(.82f+.18f*(float)Math.sin(seed*1.37f+(i+1)*2.11f));
            float m0=radius*.48f*(.86f+.14f*(float)Math.cos(seed*.71f+i*1.61f));
            float m1=radius*.48f*(.86f+.14f*(float)Math.cos(seed*.71f+(i+1)*1.61f));
            float bx0=cx+(float)Math.cos(a0)*r0,bz0=cz+(float)Math.sin(a0)*r0;
            float bx1=cx+(float)Math.cos(a1)*r1,bz1=cz+(float)Math.sin(a1)*r1;
            float mx0=cx+(float)Math.cos(a0)*m0,mz0=cz+(float)Math.sin(a0)*m0;
            float mx1=cx+(float)Math.cos(a1)*m1,mz1=cz+(float)Math.sin(a1)*m1;
            tri(o,bx0,base,bz0,bx1,base,bz1,mx1,midY,mz1,lowerPart);
            tri(o,bx0,base,bz0,mx1,midY,mz1,mx0,midY,mz0,lowerPart);
            tri(o,mx0,midY,mz0,mx1,midY,mz1,topX,topY,topZ,upperPart);
        }
    }

    private static void tri(Builder o,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float part){
        float ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az;
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float m=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(m<1e-6f){nx=0;ny=1;nz=0;m=1;}nx/=m;ny/=m;nz/=m;
        if(ny<-.93f){nx=-nx;ny=-ny;nz=-nz;}
        fv(o,ax,ay,az,nx,ny,nz,part);fv(o,bx,by,bz,nx,ny,nz,part);fv(o,cx,cy,cz,nx,ny,nz,part);
    }

    private static void fv(Builder o,float x,float y,float z,float nx,float ny,float nz,float p){o.add(x);o.add(y);o.add(z);o.add(nx);o.add(ny);o.add(nz);o.add(p);}
    private static float ridged(float v){return 1f-Math.abs((float)Math.sin(v));}
    private static float pow(float a,float b){return (float)Math.pow(Math.max(0,a),b);}
    private static float sq(float v){return v*v;}
    private static float smooth(float t){return t*t*(3f-2f*t);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}

    private static final class Builder{
        private float[] a;private int n;
        Builder(int cap){a=new float[Math.max(256,cap)];}
        void add(float v){if(n==a.length)a=Arrays.copyOf(a,a.length*2);a[n++]=v;}
        float[] toArray(){return Arrays.copyOf(a,n);}
    }
}
