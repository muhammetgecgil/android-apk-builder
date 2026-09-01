package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-18.0 mechanical package.
 * Visual-simulation geometry only: landing-gear linkages, moving control-surface
 * hardware, variable-nozzle actuation, pylon attachment hardware and canopy hinges.
 * Existing renderer part IDs are deliberately reused so the new hardware follows
 * the same animation transforms as the parent mechanism.
 */
public final class MechanicalDynamicsOverlay {
    private static final float STAB_L=4f, STAB_R=5f, RUDDER_L=6f, RUDDER_R=7f,
            FLAPERON_L=9f, FLAPERON_R=10f, CANOPY_FRAME=11f,
            GEAR_STRUT=13f, GEAR_WHEEL=14f, GEAR_DOOR=15f,
            NOZZLE_PETAL=21f, HEAT_SHIELD=28f, PYLON=30f;

    private final List<Float> out=new ArrayList<>();
    private float part=GEAR_STRUT;

    private MechanicalDynamicsOverlay(){}

    public static float[] build(){
        MechanicalDynamicsOverlay b=new MechanicalDynamicsOverlay();
        b.noseGearMechanics();
        b.mainGearMechanics();
        b.controlSurfaceMechanics();
        b.nozzleMechanics();
        b.pylonMechanics();
        b.canopyMechanics();
        float[] a=new float[b.out.size()];
        for(int i=0;i<a.length;i++)a[i]=b.out.get(i);
        return a;
    }

    /** 1-4: telescopic oleo, steering yoke, torque scissors and door actuator. */
    private void noseGearMechanics(){
        part=GEAR_STRUT;
        // Polished sliding oleo nested inside the existing outer shock strut.
        cylinderBetween(-.06f,-1.04f,-3.78f,-.06f,-1.55f,-3.78f,.038f,16);
        ringY(-.06f,-1.08f,-3.78f,.063f,.022f,18);
        ringY(-.06f,-1.48f,-3.78f,.052f,.016f,16);
        // Steering collar/yoke and tie rod.
        cylinderBetween(-.18f,-1.47f,-3.78f,.09f,-1.47f,-3.78f,.030f,12);
        cylinderBetween(-.17f,-1.43f,-3.76f,-.28f,-1.57f,-3.60f,.018f,9);
        cylinderBetween(.08f,-1.43f,-3.80f,.20f,-1.57f,-3.95f,.018f,9);
        // Dual torque-scissor links.
        cylinderBetween(-.10f,-1.20f,-3.72f,-.24f,-1.38f,-3.61f,.017f,8);
        cylinderBetween(-.24f,-1.38f,-3.61f,-.08f,-1.54f,-3.72f,.017f,8);
        cylinderBetween(-.02f,-1.20f,-3.84f,.12f,-1.38f,-3.95f,.017f,8);
        cylinderBetween(.12f,-1.38f,-3.95f,-.04f,-1.54f,-3.84f,.017f,8);
        // Lower fork and wheel axle reinforcement.
        cylinderBetween(-.22f,-1.59f,-3.78f,.10f,-1.59f,-3.78f,.048f,14);
        part=GEAR_WHEEL;
        cylinderBetween(-.205f,-1.62f,-3.78f,.105f,-1.62f,-3.78f,.064f,18);
        ringX(-.19f,-1.62f,-3.78f,.091f,.018f,20);
        ringX(.08f,-1.62f,-3.78f,.091f,.018f,20);
        part=GEAR_DOOR;
        // Two hydraulic door jacks follow the door animation.
        cylinderBetween(-.20f,-.47f,-3.43f,-.33f,-.73f,-3.62f,.017f,9);
        cylinderBetween(.20f,-.47f,-3.43f,.33f,-.73f,-3.62f,.017f,9);
    }

    /** 5-8: main oleos, drag/side braces, wheel/brake hardware and door links. */
    private void mainGearMechanics(){
        for(float s:new float[]{-1f,1f}){
            float x=1.69f*s;
            part=GEAR_STRUT;
            // Inner chrome piston and gland collar.
            cylinderBetween(x,-1.04f,.92f,x,-1.57f,1.16f,.043f,16);
            ringY(x,-1.07f,.94f,.073f,.020f,18);
            // Locking drag brace with central over-centre knuckle.
            cylinderBetween(x,-.78f,.64f,1.30f*s,-1.28f,1.13f,.030f,11);
            ellipsoid(1.30f*s,-1.28f,1.13f,.055f,.045f,.060f,12,7);
            cylinderBetween(1.30f*s,-1.28f,1.13f,x,-1.51f,1.17f,.028f,11);
            // Lateral side brace and torque-scissor pair.
            cylinderBetween(x,-1.03f,.97f,1.98f*s,-1.35f,1.18f,.022f,9);
            cylinderBetween(x-.08f*s,-1.25f,1.05f,x+.09f*s,-1.48f,1.18f,.016f,8);
            cylinderBetween(x+.09f*s,-1.48f,1.18f,x-.06f*s,-1.58f,1.10f,.016f,8);
            // Brake hydraulic line clipped to the strut.
            cylinderBetween(x-.055f*s,-1.04f,.92f,x-.055f*s,-1.50f,1.13f,.008f,7);
            for(int i=0;i<3;i++) ringY(x-.055f*s,-1.16f-i*.13f,1.00f+i*.06f,.019f,.010f,8);

            part=GEAR_WHEEL;
            // Axle, hub cap, vented brake disc and caliper carrier.
            cylinderBetween(x-.16f*s,-1.67f,1.18f,x+.16f*s,-1.67f,1.18f,.076f,20);
            ringX(x-.10f*s,-1.67f,1.18f,.145f,.018f,26);
            ringX(x+.10f*s,-1.67f,1.18f,.145f,.018f,26);
            cylinderBetween(x-.08f*s,-1.67f,1.18f,x+.08f*s,-1.67f,1.18f,.098f,22);
            box(x+.145f*s,-1.63f,1.06f,.045f,.075f,.18f);

            part=GEAR_DOOR;
            // Main-door jacks and hinge barrels.
            cylinderBetween(1.14f*s,-.49f,.36f,1.43f*s,-.74f,.55f,.018f,9);
            cylinderBetween(1.73f*s,-.50f,.60f,1.96f*s,-.77f,.80f,.016f,9);
            cylinderBetween(1.03f*s,-.49f,.34f,1.45f*s,-.49f,.34f,.024f,11);
        }
    }

    /** 9-11: moving stabilator, flaperon and rudder hinge/actuator hardware. */
    private void controlSurfaceMechanics(){
        for(float s:new float[]{-1f,1f}){
            part=s<0?STAB_L:STAB_R;
            cylinderBetween(.72f*s,.30f,1.78f,.96f*s,.30f,1.78f,.070f,16);
            cylinderBetween(.78f*s,.38f,1.58f,1.10f*s,.30f,1.96f,.020f,9);
            ellipsoid(.91f*s,.30f,1.78f,.085f,.060f,.075f,12,7);

            part=s<0?FLAPERON_L:FLAPERON_R;
            for(int i=0;i<3;i++){
                float x=(1.55f+i*.72f)*s;
                cylinderBetween(x,.22f,.68f,x,.30f,.82f,.026f,10);
            }
            cylinderBetween(1.78f*s,.31f,.56f,2.15f*s,.25f,.83f,.017f,8);

            part=s<0?RUDDER_L:RUDDER_R;
            cylinderBetween(.94f*s,1.18f,2.35f,.94f*s,1.58f,2.44f,.030f,10);
            cylinderBetween(.99f*s,1.54f,2.42f,1.04f*s,1.89f,2.54f,.026f,10);
            cylinderBetween(.93f*s,1.42f,2.31f,1.11f*s,1.58f,2.58f,.016f,8);
        }
    }

    /** 12-13: synchronising rings, petal jacks and spherical pivots. */
    private void nozzleMechanics(){
        for(float cx:new float[]{-.72f,.72f}){
            part=HEAT_SHIELD;
            ellipticRing(cx,-.10f,3.24f,.500f,.61f,.018f,38);
            ellipticRing(cx,-.10f,3.39f,.446f,.61f,.016f,36);
            part=NOZZLE_PETAL;
            ellipticRing(cx,-.10f,3.53f,.396f,.61f,.013f,34);
            for(int i=0;i<16;i++){
                double a=2*Math.PI*i/16.0;
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                float ax=cx+.503f*ca, ay=-.10f+.503f*.61f*sa;
                float bx=cx+.346f*ca, by=-.10f+.346f*.61f*sa;
                cylinderBetween(ax,ay,3.22f,bx,by,3.67f,.011f,8);
                ellipsoid(cx+.455f*ca,-.10f+.455f*.61f*sa,3.34f,.021f,.017f,.028f,8,5);
            }
            // Cross-links between every second actuator make the synchronisation mechanism readable.
            for(int i=0;i<8;i++){
                double a0=2*Math.PI*i/8.0,a1=2*Math.PI*(i+1)/8.0;
                cylinderBetween(cx+.382f*(float)Math.cos(a0),-.10f+.382f*.61f*(float)Math.sin(a0),3.58f,
                        cx+.382f*(float)Math.cos(a1),-.10f+.382f*.61f*(float)Math.sin(a1),3.58f,.0075f,7);
            }
        }
    }

    /** 14: mechanical pylon attachment: sway braces, hooks and lug blocks. */
    private void pylonMechanics(){
        part=PYLON;
        for(float[] st:new float[][]{{-1.72f,.14f,-.42f,1.0f},{1.72f,.14f,-.42f,1.0f},{-2.82f,.13f,-.02f,.83f},{2.82f,.13f,-.02f,.83f}}){
            float x=st[0],y=st[1],z=st[2],s=st[3];
            for(float dz:new float[]{-.24f,.25f}){
                box(x-.075f*s,y-.20f*s,z+dz*s,.060f*s,.060f*s,.100f*s);
                box(x+.075f*s,y-.20f*s,z+dz*s,.060f*s,.060f*s,.100f*s);
                cylinderBetween(x-.16f*s,y-.12f*s,z+dz*s,x-.095f*s,y-.34f*s,z+dz*s,.012f*s,7);
                cylinderBetween(x+.16f*s,y-.12f*s,z+dz*s,x+.095f*s,y-.34f*s,z+dz*s,.012f*s,7);
            }
            cylinderBetween(x-.105f*s,y-.24f*s,z-.33f*s,x+.105f*s,y-.24f*s,z-.33f*s,.015f*s,8);
            cylinderBetween(x-.105f*s,y-.24f*s,z+.34f*s,x+.105f*s,y-.24f*s,z+.34f*s,.015f*s,8);
        }
    }

    /** 15: canopy hinge pins, lift jacks and locking hooks. */
    private void canopyMechanics(){
        part=CANOPY_FRAME;
        cylinderBetween(-.34f,.82f,.58f,.34f,.82f,.58f,.035f,14);
        ringX(-.27f,.82f,.58f,.060f,.020f,12);
        ringX(.27f,.82f,.58f,.060f,.020f,12);
        cylinderBetween(-.28f,.77f,.52f,-.12f,.98f,.24f,.020f,10);
        cylinderBetween(.28f,.77f,.52f,.12f,.98f,.24f,.020f,10);
        for(float z:new float[]{-1.76f,-1.18f,-.60f,.02f}){
            box(-.50f,.82f,z,.050f,.065f,.080f);
            box(.50f,.82f,z,.050f,.065f,.080f);
        }
    }

    private float[] array(float x,float y,float z){return new float[]{x,y,z};}

    private void ringX(float x,float y,float z,float r,float width,int sides){
        cylinderBetween(x-width*.5f,y,z,x+width*.5f,y,z,r,sides);
    }

    private void ringY(float x,float y,float z,float r,float width,int sides){
        cylinderBetween(x,y-width*.5f,z,x,y+width*.5f,z,r,sides);
    }

    private void ellipticRing(float x,float y,float z,float r,float ys,float width,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float r0=Math.max(.004f,r-width),r1=r;
            float[] A={x+r0*(float)Math.cos(a0),y+r0*ys*(float)Math.sin(a0),z};
            float[] B={x+r1*(float)Math.cos(a0),y+r1*ys*(float)Math.sin(a0),z};
            float[] C={x+r1*(float)Math.cos(a1),y+r1*ys*(float)Math.sin(a1),z};
            float[] D={x+r0*(float)Math.cos(a1),y+r0*ys*(float)Math.sin(a1),z};
            quad2(A,B,C,D);
        }
    }

    private void cylinderBetween(float x0,float y0,float z0,float x1,float y1,float z1,float r,int sides){
        float dx=x1-x0,dy=y1-y0,dz=z1-z0,len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<1e-5f)return;
        dx/=len;dy/=len;dz/=len;
        float ax=Math.abs(dy)<.88f?0f:1f,ay=Math.abs(dy)<.88f?1f:0f,az=0f;
        float ux=dy*az-dz*ay,uy=dz*ax-dx*az,uz=dx*ay-dy*ax,ul=(float)Math.sqrt(ux*ux+uy*uy+uz*uz);if(ul<1e-5f)return;
        ux/=ul;uy/=ul;uz/=ul;float vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux;
        float[][] a=new float[sides][3],b=new float[sides][3];
        for(int i=0;i<sides;i++){
            double t=2*Math.PI*i/sides;float ct=(float)Math.cos(t),st=(float)Math.sin(t);
            float ox=r*(ux*ct+vx*st),oy=r*(uy*ct+vy*st),oz=r*(uz*ct+vz*st);
            a[i]=new float[]{x0+ox,y0+oy,z0+oz};b[i]=new float[]{x1+ox,y1+oy,z1+oz};
        }
        for(int i=0;i<sides;i++)quad(a[i],b[i],b[(i+1)%sides],a[(i+1)%sides]);
    }

    private void ellipsoid(float cx,float cy,float cz,float rx,float ry,float rz,int slices,int stacks){
        for(int j=0;j<stacks;j++)for(int i=0;i<slices;i++){
            double p0=-Math.PI/2+Math.PI*j/stacks,p1=-Math.PI/2+Math.PI*(j+1)/stacks;
            double a0=2*Math.PI*i/slices,a1=2*Math.PI*(i+1)/slices;
            float[] A=ell(cx,cy,cz,rx,ry,rz,p0,a0),B=ell(cx,cy,cz,rx,ry,rz,p1,a0),C=ell(cx,cy,cz,rx,ry,rz,p1,a1),D=ell(cx,cy,cz,rx,ry,rz,p0,a1);
            quad(A,B,C,D);
        }
    }

    private float[] ell(float cx,float cy,float cz,float rx,float ry,float rz,double p,double a){
        float cp=(float)Math.cos(p),sp=(float)Math.sin(p),ca=(float)Math.cos(a),sa=(float)Math.sin(a);
        return new float[]{cx+rx*cp*ca,cy+ry*sp,cz+rz*cp*sa};
    }

    private void box(float x,float y,float z,float sx,float sy,float sz){
        float hx=sx*.5f,hz=sz*.5f;
        prism(new float[][]{{x-hx,y+sy*.5f,z-hz},{x+hx,y+sy*.5f,z-hz},{x+hx,y+sy*.5f,z+hz},{x-hx,y+sy*.5f,z+hz}},sy);
    }

    private void prism(float[][] top,float thickness){
        if(top==null||top.length<3)return;int n=top.length;float[][] bot=new float[n][3];
        for(int i=0;i<n;i++)bot[i]=new float[]{top[i][0],top[i][1]-thickness,top[i][2]};
        for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}
        for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private void quad2(float[] a,float[] b,float[] c,float[] d){quad(a,b,c,d);quad(d,c,b,a);}
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1f;
        emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);
    }
    private void emit(float[] p,float nx,float ny,float nz){
        out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);
    }
}
