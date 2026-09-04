package com.mg.fixturecockpitsim.visual;

/**
 * Dense GPU terrain used by the cinematic route.
 * Vertex layout: xyz, smooth normal xyz, partId.
 *
 * The mesh is deliberately a real triangulated surface rather than a painted
 * mountain silhouette.  Each vertex receives a gradient-derived normal so the
 * OpenGL light can describe ridges, valleys, cliffs and dune curvature.
 */
public final class CinematicTerrainMesh {
    private CinematicTerrainMesh(){}

    private static final int NX=72;
    private static final int NZ=54;
    private static final float XMIN=-58f,XMAX=58f,ZNEAR=-18f,ZFAR=-245f;
    private static final float NORMAL_EPS=.42f;

    public static float[] build(int kind){
        final int vertices=(NX-1)*(NZ-1)*6;
        float[] out=new float[vertices*7];
        int p=0;
        for(int iz=0;iz<NZ-1;iz++){
            float tz0=iz/(float)(NZ-1),tz1=(iz+1)/(float)(NZ-1);
            float z0=lerp(ZNEAR,ZFAR,tz0),z1=lerp(ZNEAR,ZFAR,tz1);
            for(int ix=0;ix<NX-1;ix++){
                float tx0=ix/(float)(NX-1),tx1=(ix+1)/(float)(NX-1);
                float x0=lerp(XMIN,XMAX,tx0),x1=lerp(XMIN,XMAX,tx1);
                float y00=h(kind,x0,z0),y10=h(kind,x1,z0),y01=h(kind,x0,z1),y11=h(kind,x1,z1);
                p=v(out,p,kind,x0,y00,z0);p=v(out,p,kind,x1,y10,z0);p=v(out,p,kind,x1,y11,z1);
                p=v(out,p,kind,x0,y00,z0);p=v(out,p,kind,x1,y11,z1);p=v(out,p,kind,x0,y01,z1);
            }
        }
        return out;
    }

    private static int v(float[] o,int p,int kind,float x,float y,float z){
        float hx=(h(kind,x+NORMAL_EPS,z)-h(kind,x-NORMAL_EPS,z))/(2f*NORMAL_EPS);
        float hz=(h(kind,x,z+NORMAL_EPS)-h(kind,x,z-NORMAL_EPS))/(2f*NORMAL_EPS);
        float nx=-hx,ny=1f,nz=-hz;
        float m=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(m<1e-5f)m=1f;
        o[p++]=x;o[p++]=y;o[p++]=z;o[p++]=nx/m;o[p++]=ny/m;o[p++]=nz/m;o[p++]=kind==4?65f:60f+kind;
        return p;
    }

    private static float h(int kind,float x,float z){
        float d=cl((-z-18f)/227f,0f,1f);
        if(kind==0)return toros(x,z,d);
        if(kind==1)return aegean(x,z,d);
        if(kind==2)return patara(x,z,d);
        if(kind==3)return karapinar(x,z,d);
        return moonlit(x,z,d);
    }

    /** High, broken limestone ridges with deep valleys and secondary chains. */
    private static float toros(float x,float z,float d){
        float env=smooth(.08f,.82f,d);
        float chainA=ridge(x*.060f+z*.026f+.35f,2.35f);
        float chainB=ridge(x*.103f-z*.034f+1.42f,2.05f);
        float chainC=ridge(x*.039f+z*.071f-1.10f,2.65f);
        float detail=.50f+.50f*(float)(Math.sin(x*.255f+z*.063f)*Math.sin(x*.091f-z*.177f));
        float massif=.49f*chainA+.31f*chainB+.20f*chainC;
        float ravine=ridge(x*.072f-z*.057f+2.20f,5.0f);
        float peak1=gauss(x+19f,z+148f,15f,31f);
        float peak2=gauss(x-22f,z+188f,18f,35f);
        float peak3=gauss(x+3f,z+218f,13f,24f);
        float relief=(8.0f+27.0f*d)*massif+5.4f*detail*d+12.0f*peak1+14.0f*peak2+10.0f*peak3-5.8f*ravine*d;
        return -10.8f+env*relief;
    }

    /** Aegean coast: low sea-side shelf, rising wooded/rocky coastal mountains. */
    private static float aegean(float x,float z,float d){
        float shore=-5.0f+4.2f*(float)Math.sin(z*.021f)+2.0f*(float)Math.sin(z*.061f+1.2f);
        float land=smooth(shore-4.5f,shore+7.0f,x);
        float env=smooth(.06f,.88f,d);
        float r1=ridge(x*.076f+z*.034f,.95f);
        float r2=ridge(x*.132f-z*.046f+1.7f,1.65f);
        float cliff=ridge(z*.083f+x*.032f,3.8f);
        float hills=(4.5f+14.5f*d)*(.62f*r1+.38f*r2)+3.3f*cliff*d;
        return -10.4f+land*env*hills-land*.8f;
    }

    /** Patara-like coastal dune field with long wind-shaped ridges. */
    private static float patara(float x,float z,float d){
        float env=smooth(.04f,.92f,d);
        float longDune=(float)Math.sin(x*.205f+z*.072f);
        float cross=(float)Math.sin(x*.074f-z*.118f+1.2f);
        float ripple=(float)Math.sin(x*.58f+z*.23f);
        float dunes=3.1f*(.5f+.5f*longDune)+1.75f*(.5f+.5f*cross)+.48f*ripple;
        float distant=4.0f*gauss(x-27f,z+210f,22f,42f)+3.4f*gauss(x+30f,z+194f,18f,36f);
        return -9.8f+env*(dunes+distant);
    }

    /** Karapinar-like dry plateau, eroded mesas and a broad volcanic cone. */
    private static float karapinar(float x,float z,float d){
        float env=smooth(.04f,.90f,d);
        float mesa=(float)Math.pow(.5f+.5f*Math.sin(x*.052f-z*.034f),4.3);
        float erode=ridge(x*.142f+z*.057f+1.1f,2.4f);
        float cone=11.5f*gauss(x-9f,z+142f,17f,29f);
        float rim=5.0f*gauss(x+24f,z+205f,13f,25f);
        return -10.0f+env*(2.8f+5.8f*mesa+2.6f*erode+cone+rim);
    }

    /** Dark coastal mountain chain for moonlit flight, still fully 3D. */
    private static float moonlit(float x,float z,float d){
        float env=smooth(.07f,.88f,d);
        float r1=ridge(x*.072f+z*.031f+.8f,1.55f),r2=ridge(x*.119f-z*.041f,2.15f);
        float peaks=7.0f*gauss(x+18f,z+177f,17f,33f)+8.8f*gauss(x-20f,z+220f,18f,31f);
        return -10.6f+env*((5.2f+14.0f*d)*(.64f*r1+.36f*r2)+peaks);
    }

    private static float ridge(float a,float power){return (float)Math.pow(Math.abs(Math.sin(a)),power);}
    private static float gauss(float x,float z,float sx,float sz){float q=(x*x)/(sx*sx)+(z*z)/(sz*sz);return (float)Math.exp(-q);}
    private static float smooth(float a,float b,float x){float t=cl((x-a)/(b-a),0f,1f);return t*t*(3f-2f*t);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;}
    private static float cl(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
