package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-13.5 external visual stores plus high-detail landing-gear overlay.
 * Generic simulator geometry only; not certified aircraft or weapon design data.
 */
public final class VisualOrdnanceMesh {
    public static final float PART_GEAR_STRUT=13f;
    public static final float PART_GEAR_WHEEL=14f;
    public static final float PART_GEAR_DOOR=15f;
    public static final float PART_PYLON=30f;
    public static final float PART_STORE_BODY=31f;
    public static final float PART_STORE_FIN=32f;

    private final List<Float> out=new ArrayList<>();
    private float part=PART_STORE_BODY;

    public static float[] build(){
        VisualOrdnanceMesh b=new VisualOrdnanceMesh();
        // Main under-wing stations, symmetric and aligned with the AVM-13 wing sweep.
        b.station(-1.72f,-.30f,-.42f,1.00f);
        b.station( 1.72f,-.30f,-.42f,1.00f);
        b.station(-2.82f,-.245f,-.02f,.83f);
        b.station( 2.82f,-.245f,-.02f,.83f);
        b.compactStation(-.53f,-.49f,.52f,.72f);
        b.compactStation( .53f,-.49f,.52f,.72f);

        // Separate detail overlay shares the existing animated gear part IDs.
        b.detailedLandingGear();

        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++)data[i]=b.out.get(i);
        return data;
    }

    private void detailedLandingGear(){
        // NOSE GEAR: outer oleo, polished sliding tube, fork, torque links and steering/drag links.
        part=PART_GEAR_STRUT;
        cylinderBetween(-.06f,-.72f,-3.78f,-.06f,-1.24f,-3.78f,.078f,18);
        cylinderBetween(-.06f,-1.17f,-3.78f,-.06f,-1.56f,-3.78f,.048f,16);
        cylinderBetween(-.19f,-1.38f,-3.78f,-.19f,-1.62f,-3.78f,.033f,12);
        cylinderBetween(.07f,-1.38f,-3.78f,.07f,-1.62f,-3.78f,.033f,12);
        cylinderBetween(-.06f,-1.18f,-3.74f,-.20f,-1.42f,-3.58f,.026f,10);
        cylinderBetween(-.06f,-1.18f,-3.82f,.08f,-1.42f,-3.98f,.026f,10);
        cylinderBetween(-.06f,-.80f,-3.70f,.24f,-1.18f,-3.43f,.032f,12);
        cylinderBetween(.24f,-1.18f,-3.43f,.08f,-1.43f,-3.67f,.027f,10);
        cylinderBetween(-.19f,-1.60f,-3.78f,.08f,-1.60f,-3.78f,.050f,14);

        // MAIN GEAR: telescoping oleos plus dual drag/side braces and axle stubs.
        for(float s:new float[]{-1f,1f}){
            float x=1.68f*s;
            cylinderBetween(x,-.62f,.73f,x,-1.18f,.82f,.092f,18);
            cylinderBetween(x,-1.10f,.82f,x,-1.56f,1.08f,.057f,16);
            cylinderBetween(x,-.80f,.58f,1.28f*s,-1.30f,1.15f,.040f,12);
            cylinderBetween(1.28f*s,-1.30f,1.15f,x,-1.48f,1.12f,.034f,12);
            cylinderBetween(x,-.72f,.92f,2.00f*s,-1.22f,1.18f,.035f,12);
            cylinderBetween(2.00f*s,-1.22f,1.18f,x,-1.52f,1.18f,.030f,10);
            cylinderBetween(x-.18f*s,-1.34f,1.00f,x+.06f*s,-1.52f,1.16f,.026f,10);
            cylinderBetween(x,-1.62f,1.18f,(1.93f*s),-1.62f,1.18f,.058f,14);
        }

        // Wheel hubs and brake-disc visual layers. Tires themselves are in the main airframe mesh.
        part=PART_GEAR_WHEEL;
        cylinderBetween(-.18f,-1.62f,-3.78f,.08f,-1.62f,-3.78f,.122f,24);
        cylinderBetween(-.15f,-1.62f,-3.78f,.05f,-1.62f,-3.78f,.072f,20);
        for(float s:new float[]{-1f,1f}){
            float x=1.70f*s;
            cylinderBetween(x-.12f*s,-1.67f,1.18f,x+.12f*s,-1.67f,1.18f,.185f,28);
            cylinderBetween(x-.13f*s,-1.67f,1.18f,x+.13f*s,-1.67f,1.18f,.112f,24);
        }

        // Multi-piece doors. Existing gear shader gives them the open/move/close sequence.
        part=PART_GEAR_DOOR;
        // Nose: two long outer doors + two shorter aft doors.
        prism(new float[][]{{-.34f,-.47f,-4.30f},{-.055f,-.47f,-4.30f},{-.055f,-.47f,-3.47f},{-.30f,-.47f,-3.34f}},.035f);
        prism(new float[][]{{.055f,-.47f,-4.30f},{.34f,-.47f,-4.30f},{.30f,-.47f,-3.34f},{.055f,-.47f,-3.47f}},.035f);
        prism(new float[][]{{-.29f,-.49f,-3.42f},{-.045f,-.49f,-3.50f},{-.055f,-.49f,-3.06f},{-.23f,-.49f,-2.98f}},.030f);
        prism(new float[][]{{.045f,-.49f,-3.50f},{.29f,-.49f,-3.42f},{.23f,-.49f,-2.98f},{.055f,-.49f,-3.06f}},.030f);

        // Main bays: large inner door plus smaller outboard/strut door per side.
        for(float s:new float[]{-1f,1f}){
            float a=.92f*s,b=1.53f*s,c=1.92f*s,d=1.24f*s;
            prism(new float[][]{{a,-.50f,.18f},{b,-.50f,.22f},{c,-.50f,1.38f},{d,-.50f,1.62f}},.038f);
            float e=1.52f*s,f=1.88f*s,g=2.06f*s,h=1.70f*s;
            prism(new float[][]{{e,-.515f,.42f},{f,-.515f,.48f},{g,-.515f,1.42f},{h,-.515f,1.55f}},.032f);
            // Small hinge fairings at the door roots.
            cylinderBetween(1.05f*s,-.50f,.34f,1.43f*s,-.50f,.34f,.026f,12);
            cylinderBetween(1.60f*s,-.51f,.58f,1.86f*s,-.51f,.58f,.021f,10);
        }
    }

    private void station(float x,float wingY,float z,float s){
        part=PART_PYLON;
        prism(new float[][]{
                {x-.23f*s,wingY+.015f*s,z-.44f*s},{x+.23f*s,wingY+.015f*s,z-.44f*s},
                {x+.21f*s,wingY+.015f*s,z+.41f*s},{x-.21f*s,wingY+.015f*s,z+.41f*s}
        },.075f*s);
        prism(new float[][]{
                {x-.135f*s,wingY-.055f*s,z-.38f*s},{x+.135f*s,wingY-.055f*s,z-.38f*s},
                {x+.120f*s,wingY-.055f*s,z+.33f*s},{x+.080f*s,wingY-.055f*s,z+.54f*s},
                {x-.080f*s,wingY-.055f*s,z+.54f*s},{x-.120f*s,wingY-.055f*s,z+.33f*s}
        },.255f*s);
        box(x,wingY-.335f*s,z+.04f*s,.115f*s,.085f*s,1.18f*s);
        brace(x-.13f*s,wingY-.30f*s,z-.19f*s,-1,s);
        brace(x+.13f*s,wingY-.30f*s,z-.19f*s, 1,s);
        brace(x-.13f*s,wingY-.30f*s,z+.25f*s,-1,s);
        brace(x+.13f*s,wingY-.30f*s,z+.25f*s, 1,s);

        part=PART_STORE_BODY;
        float cy=wingY-.47f*s;
        spindle(x,cy,z-1.08f*s,z+.93f*s,.090f*s,.145f*s,24);
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
        prism(new float[][]{
                {x-.065f*s,y,aft-.18f*s},{x-.36f*s,y,aft+.13f*s},{x-.055f*s,y,aft+.10f*s}
        },.032f*s);
        prism(new float[][]{
                {x+.065f*s,y,aft-.18f*s},{x+.36f*s,y,aft+.13f*s},{x+.055f*s,y,aft+.10f*s}
        },.032f*s);
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

    private void cylinderBetween(float x0,float y0,float z0,float x1,float y1,float z1,float r,int sides){
        float dx=x1-x0,dy=y1-y0,dz=z1-z0;
        float len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<1e-5f)return;
        dx/=len;dy/=len;dz/=len;
        // Pick a helper axis that is not parallel to the cylinder direction.
        float ax=Math.abs(dy)<.88f?0f:1f,ay=Math.abs(dy)<.88f?1f:0f,az=0f;
        float ux=dy*az-dz*ay,uy=dz*ax-dx*az,uz=dx*ay-dy*ax;
        float ul=(float)Math.sqrt(ux*ux+uy*uy+uz*uz);if(ul<1e-5f)return;ux/=ul;uy/=ul;uz/=ul;
        float vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux;
        float[][] a=new float[sides][3],b=new float[sides][3];
        for(int i=0;i<sides;i++){
            double t=2*Math.PI*i/sides;float ct=(float)Math.cos(t),st=(float)Math.sin(t);
            float ox=r*(ux*ct+vx*st),oy=r*(uy*ct+vy*st),oz=r*(uz*ct+vz*st);
            a[i]=new float[]{x0+ox,y0+oy,z0+oz};b[i]=new float[]{x1+ox,y1+oy,z1+oz};
        }
        for(int i=0;i<sides;i++)quad(a[i],b[i],b[(i+1)%sides],a[(i+1)%sides]);
        for(int i=1;i<sides-1;i++){tri(a[0],a[i+1],a[i]);tri(b[0],b[i],b[i+1]);}
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
