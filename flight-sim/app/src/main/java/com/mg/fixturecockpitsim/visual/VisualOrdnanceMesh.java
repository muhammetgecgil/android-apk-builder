package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-12.1 external visual stores.
 * Generic simulator geometry only: swept pylons, adapter shoes, rails and sway-brace details.
 * It intentionally does not reproduce a specific real weapon or certified carriage installation.
 */
public final class VisualOrdnanceMesh {
    public static final float PART_PYLON=30f;
    public static final float PART_STORE_BODY=31f;
    public static final float PART_STORE_FIN=32f;

    private final List<Float> out=new ArrayList<>();
    private float part=PART_STORE_BODY;

    public static float[] build(){
        VisualOrdnanceMesh b=new VisualOrdnanceMesh();
        // Main under-wing stations, symmetric and aligned with the AVM-12 wing sweep.
        b.station(-1.72f,-.30f,-.42f,1.00f);
        b.station( 1.72f,-.30f,-.42f,1.00f);
        b.station(-2.82f,-.245f,-.02f,.83f);
        b.station( 2.82f,-.245f,-.02f,.83f);
        // Compact fuselage shoulder stations remain smaller and closer to the body.
        b.compactStation(-.53f,-.49f,.52f,.72f);
        b.compactStation( .53f,-.49f,.52f,.72f);

        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++)data[i]=b.out.get(i);
        return data;
    }

    private void station(float x,float wingY,float z,float s){
        part=PART_PYLON;
        // Wide, shallow adapter shoe follows the underside instead of hanging from a single block.
        prism(new float[][]{
                {x-.23f*s,wingY+.015f*s,z-.44f*s},{x+.23f*s,wingY+.015f*s,z-.44f*s},
                {x+.21f*s,wingY+.015f*s,z+.41f*s},{x-.21f*s,wingY+.015f*s,z+.41f*s}
        },.075f*s);
        // Swept pylon side body: deeper aft, tapered at both ends.
        prism(new float[][]{
                {x-.135f*s,wingY-.055f*s,z-.38f*s},{x+.135f*s,wingY-.055f*s,z-.38f*s},
                {x+.120f*s,wingY-.055f*s,z+.33f*s},{x+.080f*s,wingY-.055f*s,z+.54f*s},
                {x-.080f*s,wingY-.055f*s,z+.54f*s},{x-.120f*s,wingY-.055f*s,z+.33f*s}
        },.255f*s);
        // Long launcher rail below the pylon.
        box(x,wingY-.335f*s,z+.04f*s,.115f*s,.085f*s,1.18f*s);
        // Two small sway-brace / attachment ears each side of the store shoulder.
        brace(x-.13f*s,wingY-.30f*s,z-.19f*s,-1,s);
        brace(x+.13f*s,wingY-.30f*s,z-.19f*s, 1,s);
        brace(x-.13f*s,wingY-.30f*s,z+.25f*s,-1,s);
        brace(x+.13f*s,wingY-.30f*s,z+.25f*s, 1,s);

        part=PART_STORE_BODY;
        float cy=wingY-.47f*s;
        spindle(x,cy,z-1.08f*s,z+.93f*s,.090f*s,.145f*s,24);
        // Slight collar bands make the store read as an assembled external object rather than one primitive.
        ring(x,cy,z-.42f*s,.151f*s,.050f*s,20);
        ring(x,cy,z+.48f*s,.142f*s,.045f*s,20);

        part=PART_STORE_FIN;
        tailFins(x,cy,z+.72f*s,s);
    }

    private void compactStation(float x,float wingY,float z,float s){
        part=PART_PYLON;
        prism(new float[][]{
                {x-.14f*s,wingY,z-.30f*s},{x+.14f*s,wingY,z-.30f*s},
                {x+.11f*s,wingY,z+.31f*s},{x-.11f*s,wingY,z+.31f*s}
        },.15f*s);
        box(x,wingY-.19f*s,z,.095f*s,.065f*s,.66f*s);
        part=PART_STORE_BODY;
        float cy=wingY-.30f*s;
        spindle(x,cy,z-.68f*s,z+.68f*s,.080f*s,.135f*s,20);
        ring(x,cy,z+.28f*s,.138f*s,.038f*s,18);
        part=PART_STORE_FIN;
        tailFins(x,cy,z+.50f*s,s*.72f);
    }

    private void brace(float x,float y,float z,float side,float s){
        prism(new float[][]{
                {x,y,z-.09f*s},{x+.085f*side*s,y,z+.01f*s},{x+.025f*side*s,y,z+.11f*s}
        },.055f*s);
    }

    private void tailFins(float x,float y,float aft,float s){
        // Horizontal pair.
        prism(new float[][]{
                {x-.065f*s,y,aft-.18f*s},{x-.36f*s,y,aft+.13f*s},{x-.055f*s,y,aft+.10f*s}
        },.032f*s);
        prism(new float[][]{
                {x+.065f*s,y,aft-.18f*s},{x+.36f*s,y,aft+.13f*s},{x+.055f*s,y,aft+.10f*s}
        },.032f*s);
        // Upper and lower fins use small lateral thickness, improving silhouette from rear cameras.
        prism(new float[][]{
                {x,y+.045f*s,aft-.17f*s},{x,y+.31f*s,aft+.12f*s},{x,y+.045f*s,aft+.10f*s}
        },.030f*s);
        prism(new float[][]{
                {x,y-.045f*s,aft-.17f*s},{x,y-.31f*s,aft+.12f*s},{x,y-.045f*s,aft+.10f*s}
        },.030f*s);
    }

    private void spindle(float x,float y,float z0,float z1,float noseR,float bodyR,int sides){
        float len=z1-z0;
        float nose=z0,shoulder=z0+len*.22f,mid=z0+len*.57f,tail=z1;
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] tip={x,y,nose};
            float[] p0={x+noseR*(float)Math.cos(a0),y+noseR*(float)Math.sin(a0),shoulder};
            float[] p1={x+noseR*(float)Math.cos(a1),y+noseR*(float)Math.sin(a1),shoulder};
            tri(tip,p1,p0);
        }
        cylinderBand(x,y,shoulder,mid,noseR,bodyR,sides);
        cylinderBand(x,y,mid,tail,bodyR,bodyR*.69f,sides);
    }

    private void cylinderBand(float x,float y,float z0,float z1,float r0,float r1,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] a={x+r0*(float)Math.cos(a0),y+r0*(float)Math.sin(a0),z0};
            float[] b={x+r1*(float)Math.cos(a0),y+r1*(float)Math.sin(a0),z1};
            float[] c={x+r1*(float)Math.cos(a1),y+r1*(float)Math.sin(a1),z1};
            float[] d={x+r0*(float)Math.cos(a1),y+r0*(float)Math.sin(a1),z0};
            quad(a,b,c,d);
        }
    }

    private void ring(float x,float y,float z,float r,float width,int sides){
        cylinderBand(x,y,z-width*.5f,z+width*.5f,r,r,sides);
    }

    private void box(float x,float y,float z,float sx,float sy,float sz){
        float hx=sx*.5f,hz=sz*.5f,top=y+sy*.5f;
        prism(new float[][]{{x-hx,top,z-hz},{x+hx,top,z-hz},{x+hx,top,z+hz},{x-hx,top,z+hz}},sy);
    }

    private void prism(float[][] top,float thickness){
        if(top==null||top.length<3)return;
        int n=top.length;float[][] bot=new float[n][3];
        for(int i=0;i<n;i++)bot[i]=new float[]{top[i][0],top[i][1]-thickness,top[i][2]};
        for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}
        for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1;
        emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);
    }
    private void emit(float[] q,float nx,float ny,float nz){out.add(q[0]);out.add(q[1]);out.add(q[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);}
}
