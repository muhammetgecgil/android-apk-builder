package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-13.6 exterior detail overlay.
 * Generic simulator-only geometry: landing gear, cockpit/pilot, aerodynamic fairings,
 * nozzle mechanics and non-specific external-store attachment detail.
 */
public final class VisualOrdnanceMesh {
    public static final float PART_SKIN=0f;
    public static final float PART_NOZZLE_PETAL=21f;
    public static final float PART_HEAT_SHIELD=28f;
    public static final float PART_DETAIL=29f;
    public static final float PART_GEAR_STRUT=13f;
    public static final float PART_GEAR_WHEEL=14f;
    public static final float PART_GEAR_DOOR=15f;
    public static final float PART_PYLON=30f;
    public static final float PART_STORE_BODY=31f;
    public static final float PART_STORE_FIN=32f;
    public static final float PART_PILOT_SUIT=33f;
    public static final float PART_PILOT_HELMET=34f;
    public static final float PART_PILOT_VISOR=35f;
    public static final float PART_HUD_GLASS=36f;
    public static final float PART_COCKPIT_DISPLAY=37f;

    private final List<Float> out=new ArrayList<>();
    private float part=PART_STORE_BODY;

    public static float[] build(){
        VisualOrdnanceMesh b=new VisualOrdnanceMesh();
        b.station(-1.72f,-.30f,-.42f,1.00f);
        b.station( 1.72f,-.30f,-.42f,1.00f);
        b.station(-2.82f,-.245f,-.02f,.83f);
        b.station( 2.82f,-.245f,-.02f,.83f);
        b.compactStation(-.53f,-.49f,.52f,.72f);
        b.compactStation( .53f,-.49f,.52f,.72f);
        b.aerodynamicBlendOverlays();
        b.nozzleMechanics();
        b.pilotAndCockpit();
        b.detailedLandingGear();
        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++)data[i]=b.out.get(i);
        return data;
    }

    private void aerodynamicBlendOverlays(){
        // Thin upper fairings bridge the procedural fuselage/wing and intake shoulders.
        part=PART_SKIN;
        for(float s:new float[]{-1f,1f}){
            prism(new float[][]{
                    {.72f*s,.645f,-2.82f},{1.04f*s,.585f,-2.56f},{1.50f*s,.505f,-2.03f},
                    {1.80f*s,.445f,-1.36f},{1.82f*s,.425f,-.58f},{1.58f*s,.445f,.16f},
                    {1.20f*s,.505f,.76f},{.86f*s,.585f,.94f},{.73f*s,.645f,.22f}
            },.018f);
            prism(new float[][]{
                    {.90f*s,.445f,-3.22f},{1.18f*s,.425f,-3.06f},{1.48f*s,.385f,-2.84f},
                    {1.72f*s,.285f,-2.50f},{1.70f*s,.205f,-2.31f},{1.45f*s,.245f,-2.42f},
                    {1.15f*s,.335f,-2.70f},{.94f*s,.405f,-3.02f}
            },.016f);
        }
        part=PART_DETAIL;
        for(float s:new float[]{-1f,1f}){
            ribbon(new float[][]{{.91f*s,.658f,-2.56f},{1.30f*s,.565f,-2.28f},{1.67f*s,.475f,-1.86f},{1.88f*s,.425f,-1.34f}},.015f,.012f);
            ribbon(new float[][]{{1.00f*s,.438f,-3.16f},{1.31f*s,.402f,-2.94f},{1.60f*s,.335f,-2.62f}},.012f,.012f);
        }
    }

    private void nozzleMechanics(){
        // Generic actuator/hinge rods and thermal bands around each variable exhaust.
        for(float cx:new float[]{-.72f,.72f}){
            part=PART_HEAT_SHIELD;
            ring(cx,-.10f,3.18f,.515f,.045f,32);
            ring(cx,-.10f,3.34f,.465f,.030f,30);
            part=PART_NOZZLE_PETAL;
            for(int i=0;i<12;i++){
                double a=2*Math.PI*i/12.0;
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                cylinderBetween(cx+.525f*ca,-.10f+.525f*.61f*sa,3.18f,
                        cx+.392f*ca,-.10f+.392f*.61f*sa,3.59f,.014f,8);
            }
            part=PART_DETAIL;
            for(int i=0;i<10;i++){
                double a=2*Math.PI*(i+.5)/10.0;
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                cylinderBetween(cx+.43f*ca,-.10f+.43f*.61f*sa,3.32f,
                        cx+.34f*ca,-.10f+.34f*.61f*sa,3.66f,.010f,7);
            }
        }
    }

    private void pilotAndCockpit(){
        // Pilot body deliberately generic; it gives the canopy scale and depth.
        part=PART_PILOT_SUIT;
        ellipsoid(0f,1.055f,-.18f,.235f,.285f,.235f,22,12);
        ellipsoid(-.19f,1.02f,-.36f,.075f,.18f,.075f,14,8);
        ellipsoid(.19f,1.02f,-.36f,.075f,.18f,.075f,14,8);
        cylinderBetween(-.15f,1.08f,-.29f,-.08f,.92f,-.83f,.045f,10);
        cylinderBetween(.15f,1.08f,-.29f,.08f,.92f,-.83f,.045f,10);

        part=PART_PILOT_HELMET;
        ellipsoid(0f,1.285f,-.50f,.175f,.175f,.170f,24,14);
        ellipsoid(0f,1.305f,-.455f,.155f,.135f,.125f,22,12);

        part=PART_PILOT_VISOR;
        // Shallow front visor, not a complete sphere.
        ellipsoid(0f,1.305f,-.620f,.135f,.075f,.045f,20,10);

        part=PART_COCKPIT_DISPLAY;
        box(-.19f,.975f,-1.46f,.23f,.115f,.035f);
        box(.19f,.975f,-1.46f,.23f,.115f,.035f);
        box(0f,.925f,-1.22f,.18f,.080f,.030f);
        box(-.34f,.895f,-.82f,.055f,.070f,.42f);
        box(.34f,.895f,-.82f,.055f,.070f,.42f);

        part=PART_HUD_GLASS;
        // Double-sided thin HUD combiner ahead of the pilot.
        float[] a={-.165f,1.00f,-1.54f},b={.165f,1.00f,-1.54f},c={.145f,1.205f,-1.46f},d={-.145f,1.205f,-1.46f};
        quad(a,b,c,d);quad(d,c,b,a);
    }

    private void detailedLandingGear(){
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
        for(float s:new float[]{-1f,1f}){
            float x=1.68f*s;
            cylinderBetween(x,-.62f,.73f,x,-1.18f,.82f,.092f,18);
            cylinderBetween(x,-1.10f,.82f,x,-1.56f,1.08f,.057f,16);
            cylinderBetween(x,-.80f,.58f,1.28f*s,-1.30f,1.15f,.040f,12);
            cylinderBetween(1.28f*s,-1.30f,1.15f,x,-1.48f,1.12f,.034f,12);
            cylinderBetween(x,-.72f,.92f,2.00f*s,-1.22f,1.18f,.035f,12);
            cylinderBetween(2.00f*s,-1.22f,1.18f,x,-1.52f,1.18f,.030f,10);
            cylinderBetween(x-.18f*s,-1.34f,1.00f,x+.06f*s,-1.52f,1.16f,.026f,10);
            cylinderBetween(x,-1.62f,1.18f,1.93f*s,-1.62f,1.18f,.058f,14);
        }
        part=PART_GEAR_WHEEL;
        cylinderBetween(-.18f,-1.62f,-3.78f,.08f,-1.62f,-3.78f,.122f,24);
        cylinderBetween(-.15f,-1.62f,-3.78f,.05f,-1.62f,-3.78f,.072f,20);
        for(float s:new float[]{-1f,1f}){
            float x=1.70f*s;
            cylinderBetween(x-.12f*s,-1.67f,1.18f,x+.12f*s,-1.67f,1.18f,.185f,28);
            cylinderBetween(x-.13f*s,-1.67f,1.18f,x+.13f*s,-1.67f,1.18f,.112f,24);
        }
        part=PART_GEAR_DOOR;
        prism(new float[][]{{-.34f,-.47f,-4.30f},{-.055f,-.47f,-4.30f},{-.055f,-.47f,-3.47f},{-.30f,-.47f,-3.34f}},.035f);
        prism(new float[][]{{.055f,-.47f,-4.30f},{.34f,-.47f,-4.30f},{.30f,-.47f,-3.34f},{.055f,-.47f,-3.47f}},.035f);
        prism(new float[][]{{-.29f,-.49f,-3.42f},{-.045f,-.49f,-3.50f},{-.055f,-.49f,-3.06f},{-.23f,-.49f,-2.98f}},.030f);
        prism(new float[][]{{.045f,-.49f,-3.50f},{.29f,-.49f,-3.42f},{.23f,-.49f,-2.98f},{.055f,-.49f,-3.06f}},.030f);
        for(float s:new float[]{-1f,1f}){
            prism(new float[][]{{.92f*s,-.50f,.18f},{1.53f*s,-.50f,.22f},{1.92f*s,-.50f,1.38f},{1.24f*s,-.50f,1.62f}},.038f);
            prism(new float[][]{{1.52f*s,-.515f,.42f},{1.88f*s,-.515f,.48f},{2.06f*s,-.515f,1.42f},{1.70f*s,-.515f,1.55f}},.032f);
            cylinderBetween(1.05f*s,-.50f,.34f,1.43f*s,-.50f,.34f,.026f,12);
            cylinderBetween(1.60f*s,-.51f,.58f,1.86f*s,-.51f,.58f,.021f,10);
        }
    }

    private void station(float x,float wingY,float z,float s){
        part=PART_PYLON;
        prism(new float[][]{{x-.23f*s,wingY+.015f*s,z-.44f*s},{x+.23f*s,wingY+.015f*s,z-.44f*s},{x+.21f*s,wingY+.015f*s,z+.41f*s},{x-.21f*s,wingY+.015f*s,z+.41f*s}},.075f*s);
        prism(new float[][]{{x-.135f*s,wingY-.055f*s,z-.38f*s},{x+.135f*s,wingY-.055f*s,z-.38f*s},{x+.120f*s,wingY-.055f*s,z+.33f*s},{x+.080f*s,wingY-.055f*s,z+.54f*s},{x-.080f*s,wingY-.055f*s,z+.54f*s},{x-.120f*s,wingY-.055f*s,z+.33f*s}},.255f*s);
        box(x,wingY-.335f*s,z+.04f*s,.115f*s,.085f*s,1.18f*s);
        brace(x-.13f*s,wingY-.30f*s,z-.19f*s,-1,s);brace(x+.13f*s,wingY-.30f*s,z-.19f*s,1,s);
        brace(x-.13f*s,wingY-.30f*s,z+.25f*s,-1,s);brace(x+.13f*s,wingY-.30f*s,z+.25f*s,1,s);
        // Generic attachment hooks, sway pads and an umbilical-like lead.
        box(x-.065f*s,wingY-.37f*s,z-.28f*s,.055f*s,.045f*s,.12f*s);
        box(x+.065f*s,wingY-.37f*s,z-.28f*s,.055f*s,.045f*s,.12f*s);
        box(x-.065f*s,wingY-.37f*s,z+.30f*s,.055f*s,.045f*s,.12f*s);
        box(x+.065f*s,wingY-.37f*s,z+.30f*s,.055f*s,.045f*s,.12f*s);
        cylinderBetween(x+.11f*s,wingY-.34f*s,z+.14f*s,x+.13f*s,wingY-.46f*s,z+.23f*s,.012f*s,7);
        part=PART_STORE_BODY;
        float cy=wingY-.47f*s;
        spindle(x,cy,z-1.08f*s,z+.93f*s,.090f*s,.145f*s,26);
        ring(x,cy,z-.42f*s,.151f*s,.050f*s,22);ring(x,cy,z+.48f*s,.142f*s,.045f*s,22);
        part=PART_STORE_FIN;tailFins(x,cy,z+.72f*s,s);
    }

    private void compactStation(float x,float wingY,float z,float s){
        part=PART_PYLON;
        prism(new float[][]{{x-.14f*s,wingY,z-.30f*s},{x+.14f*s,wingY,z-.30f*s},{x+.11f*s,wingY,z+.31f*s},{x-.11f*s,wingY,z+.31f*s}},.15f*s);
        box(x,wingY-.19f*s,z,.095f*s,.065f*s,.66f*s);
        part=PART_STORE_BODY;float cy=wingY-.30f*s;spindle(x,cy,z-.68f*s,z+.68f*s,.080f*s,.135f*s,22);ring(x,cy,z+.28f*s,.138f*s,.038f*s,20);
        part=PART_STORE_FIN;tailFins(x,cy,z+.50f*s,s*.72f);
    }

    private void brace(float x,float y,float z,float side,float s){prism(new float[][]{{x,y,z-.09f*s},{x+.085f*side*s,y,z+.01f*s},{x+.025f*side*s,y,z+.11f*s}},.055f*s);}

    private void tailFins(float x,float y,float aft,float s){
        prism(new float[][]{{x-.065f*s,y,aft-.18f*s},{x-.36f*s,y,aft+.13f*s},{x-.055f*s,y,aft+.10f*s}},.032f*s);
        prism(new float[][]{{x+.065f*s,y,aft-.18f*s},{x+.36f*s,y,aft+.13f*s},{x+.055f*s,y,aft+.10f*s}},.032f*s);
        prism(new float[][]{{x,y+.045f*s,aft-.17f*s},{x,y+.31f*s,aft+.12f*s},{x,y+.045f*s,aft+.10f*s}},.030f*s);
        prism(new float[][]{{x,y-.045f*s,aft-.17f*s},{x,y-.31f*s,aft+.12f*s},{x,y-.045f*s,aft+.10f*s}},.030f*s);
    }

    private void spindle(float x,float y,float z0,float z1,float noseR,float bodyR,int sides){
        float len=z1-z0,nose=z0,shoulder=z0+len*.22f,mid=z0+len*.57f,tail=z1;
        for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;float[] tip={x,y,nose},p0={x+noseR*(float)Math.cos(a0),y+noseR*(float)Math.sin(a0),shoulder},p1={x+noseR*(float)Math.cos(a1),y+noseR*(float)Math.sin(a1),shoulder};tri(tip,p1,p0);}
        cylinderBand(x,y,shoulder,mid,noseR,bodyR,sides);cylinderBand(x,y,mid,tail,bodyR,bodyR*.69f,sides);
    }

    private void ring(float x,float y,float z,float r,float width,int sides){cylinderBand(x,y,z-width*.5f,z+width*.5f,r,r,sides);}

    private void cylinderBand(float x,float y,float z0,float z1,float r0,float r1,int sides){
        for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;float[] a={x+r0*(float)Math.cos(a0),y+r0*(float)Math.sin(a0),z0},b={x+r1*(float)Math.cos(a0),y+r1*(float)Math.sin(a0),z1},c={x+r1*(float)Math.cos(a1),y+r1*(float)Math.sin(a1),z1},d={x+r0*(float)Math.cos(a1),y+r0*(float)Math.sin(a1),z0};quad(a,b,c,d);}
    }

    private void cylinderBetween(float x0,float y0,float z0,float x1,float y1,float z1,float r,int sides){
        float dx=x1-x0,dy=y1-y0,dz=z1-z0,len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<1e-5f)return;dx/=len;dy/=len;dz/=len;
        float ax=Math.abs(dy)<.88f?0f:1f,ay=Math.abs(dy)<.88f?1f:0f,az=0f;
        float ux=dy*az-dz*ay,uy=dz*ax-dx*az,uz=dx*ay-dy*ax,ul=(float)Math.sqrt(ux*ux+uy*uy+uz*uz);if(ul<1e-5f)return;ux/=ul;uy/=ul;uz/=ul;
        float vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux;float[][] a=new float[sides][3],b=new float[sides][3];
        for(int i=0;i<sides;i++){double t=2*Math.PI*i/sides;float ct=(float)Math.cos(t),st=(float)Math.sin(t),ox=r*(ux*ct+vx*st),oy=r*(uy*ct+vy*st),oz=r*(uz*ct+vz*st);a[i]=new float[]{x0+ox,y0+oy,z0+oz};b[i]=new float[]{x1+ox,y1+oy,z1+oz};}
        for(int i=0;i<sides;i++)quad(a[i],b[i],b[(i+1)%sides],a[(i+1)%sides]);
        for(int i=1;i<sides-1;i++){tri(a[0],a[i+1],a[i]);tri(b[0],b[i],b[i+1]);}
    }

    private void ellipsoid(float cx,float cy,float cz,float rx,float ry,float rz,int slices,int stacks){
        for(int j=0;j<stacks;j++)for(int i=0;i<slices;i++){
            double p0=-Math.PI/2+Math.PI*j/stacks,p1=-Math.PI/2+Math.PI*(j+1)/stacks,a0=2*Math.PI*i/slices,a1=2*Math.PI*(i+1)/slices;
            V A=ellV(cx,cy,cz,rx,ry,rz,p0,a0),B=ellV(cx,cy,cz,rx,ry,rz,p1,a0),C=ellV(cx,cy,cz,rx,ry,rz,p1,a1),D=ellV(cx,cy,cz,rx,ry,rz,p0,a1);quadSmooth(A,B,C,D);
        }
    }

    private V ellV(float cx,float cy,float cz,float rx,float ry,float rz,double p,double a){float cp=(float)Math.cos(p),sp=(float)Math.sin(p),ca=(float)Math.cos(a),sa=(float)Math.sin(a),nx=cp*ca,ny=sp,nz=cp*sa;return new V(cx+rx*nx,cy+ry*ny,cz+rz*nz,nx,ny,nz);}

    private void ribbon(float[][] pts,float width,float depth){
        if(pts.length<2)return;for(int i=0;i<pts.length-1;i++){float[] a=pts[i],b=pts[i+1];float dx=b[0]-a[0],dz=b[2]-a[2],l=(float)Math.sqrt(dx*dx+dz*dz);if(l<1e-5f)l=1;float px=-dz/l*width*.5f,pz=dx/l*width*.5f;prism(new float[][]{{a[0]+px,a[1],a[2]+pz},{b[0]+px,b[1],b[2]+pz},{b[0]-px,b[1],b[2]-pz},{a[0]-px,a[1],a[2]-pz}},depth);}
    }

    private void box(float x,float y,float z,float sx,float sy,float sz){float hx=sx*.5f,hz=sz*.5f,top=y+sy*.5f;prism(new float[][]{{x-hx,top,z-hz},{x+hx,top,z-hz},{x+hx,top,z+hz},{x-hx,top,z+hz}},sy);}

    private void prism(float[][] top,float thickness){
        if(top==null||top.length<3)return;int n=top.length;float[][] bot=new float[n][3];for(int i=0;i<n;i++)bot[i]=new float[]{top[i][0],top[i][1]-thickness,top[i][2]};for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1;emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);}
    private void quadSmooth(V a,V b,V c,V d){emit(a);emit(b);emit(c);emit(a);emit(c);emit(d);}
    private void emit(V v){float l=(float)Math.sqrt(v.nx*v.nx+v.ny*v.ny+v.nz*v.nz);if(l<1e-6f)l=1;emit(new float[]{v.x,v.y,v.z},v.nx/l,v.ny/l,v.nz/l);}
    private void emit(float[] q,float nx,float ny,float nz){out.add(q[0]);out.add(q[1]);out.add(q[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);}
    private static final class V{final float x,y,z,nx,ny,nz;V(float x,float y,float z,float nx,float ny,float nz){this.x=x;this.y=y;this.z=z;this.nx=nx;this.ny=ny;this.nz=nz;}}
}
