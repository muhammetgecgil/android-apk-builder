package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-16.0 advanced exterior-detail overlay.
 * Generic modern twin-engine fighter geometry for visual simulation only.
 * Preserves the v83 detail set and adds a higher-fidelity engine package:
 * intake/compressor depth, nacelle service bands, exhaust liner/flame-holder
 * hardware and layered vectoring-nozzle actuators.
 */
public final class AdvancedAirframeOverlay {
    private static final float CANOPY_FRAME=11f, NOZZLE_INNER=12f, GEAR_STRUT=13f,
            GEAR_WHEEL=14f, GEAR_DOOR=15f, COMPRESSOR_FACE=20f,
            NOZZLE_PETAL=21f, HEAT_SHIELD=28f, DETAIL=29f;

    private final List<Float> out=new ArrayList<>();
    private float part=DETAIL;

    private AdvancedAirframeOverlay(){}

    public static float[] build(){
        AdvancedAirframeOverlay b=new AdvancedAirframeOverlay();
        b.canopyHardware();
        b.noseSensors();
        b.intakeAndWingRoot();
        b.engineBayAndCoreDetails();
        b.controlSurfaceHardware();
        b.nozzleActuators();
        b.gearBayAndBrakeDetails();
        b.surfaceServiceDetails();
        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++)data[i]=b.out.get(i);
        return data;
    }

    private void canopyHardware(){
        part=CANOPY_FRAME;
        // Twin canopy sill rails and aft hinge beam.
        ribbon(new float[][]{{-.48f,.805f,-2.22f},{-.54f,.825f,-1.65f},{-.55f,.835f,-.92f},{-.51f,.820f,-.18f},{-.39f,.800f,.55f}},.030f,.030f);
        ribbon(new float[][]{{ .48f,.805f,-2.22f},{ .54f,.825f,-1.65f},{ .55f,.835f,-.92f},{ .51f,.820f,-.18f},{ .39f,.800f,.55f}},.030f,.030f);
        cylinderBetween(-.33f,.82f,.58f,.33f,.82f,.58f,.030f,12);
        // Canopy locking lugs.
        for(float z:new float[]{-1.78f,-1.18f,-.58f,.03f}){
            box(-.49f,.82f,z,.060f,.070f,.085f);box(.49f,.82f,z,.060f,.070f,.085f);
        }
        part=DETAIL;
        // Rear deck hydraulic/hinge actuators.
        cylinderBetween(-.25f,.74f,.52f,-.12f,.96f,.26f,.018f,10);
        cylinderBetween(.25f,.74f,.52f,.12f,.96f,.26f,.018f,10);
        box(0f,.76f,.72f,.42f,.055f,.14f);
    }

    private void noseSensors(){
        part=DETAIL;
        // Short generic pitot/probe under the radome, plus AOA vanes.
        cylinderBetween(0f,-.04f,-6.73f,0f,-.04f,-7.10f,.018f,12);
        cylinderBetween(0f,-.04f,-7.10f,0f,-.04f,-7.22f,.008f,10);
        for(float s:new float[]{-1f,1f}){
            cylinderBetween(.52f*s,.22f,-5.02f,.66f*s,.24f,-5.05f,.012f,9);
            prism(new float[][]{{.66f*s,.245f,-5.09f},{.82f*s,.245f,-5.02f},{.67f*s,.245f,-4.97f}},.018f);
            ellipsoid(.73f*s,.34f,-4.56f,.050f,.030f,.105f,14,8);
        }
        // Small EO/sensor fairing on forebody lower centerline.
        ellipsoid(0f,-.34f,-4.72f,.125f,.060f,.235f,18,10);
    }

    private void intakeAndWingRoot(){
        part=DETAIL;
        for(float s:new float[]{-1f,1f}){
            // Intake internal lip rails / boundary-layer edge.
            ribbon(new float[][]{{1.04f*s,.37f,-3.10f},{1.37f*s,.31f,-2.85f},{1.63f*s,.17f,-2.50f},{1.56f*s,-.10f,-2.34f}},.030f,.030f);
            ribbon(new float[][]{{1.01f*s,-.02f,-3.00f},{1.21f*s,-.17f,-2.70f},{1.53f*s,-.18f,-2.42f}},.020f,.024f);

            // Intake roof splitter and side-wall stiffeners add depth when viewed from ahead.
            prism(new float[][]{{1.17f*s,.105f,-2.82f},{1.47f*s,.080f,-2.58f},{1.39f*s,.070f,-2.34f},{1.12f*s,.095f,-2.54f}},.026f);
            for(int i=0;i<3;i++){
                float z=-2.63f+i*.22f;
                cylinderBetween(1.18f*s,-.03f,z,1.48f*s,-.10f,z+.04f,.010f,7);
            }

            // Wing-root blend ribs and actuator fairings.
            ribbon(new float[][]{{.86f*s,.645f,-2.46f},{1.30f*s,.565f,-2.10f},{1.76f*s,.485f,-1.55f},{2.12f*s,.405f,-.92f}},.018f,.020f);
            ribbon(new float[][]{{1.05f*s,.305f,.22f},{1.96f*s,.260f,.42f},{2.94f*s,.210f,.56f},{3.78f*s,.165f,.62f}},.014f,.018f);
            ellipsoid(1.23f*s,.49f,.72f,.105f,.060f,.215f,14,8);
            ellipsoid(2.72f*s,.30f,.68f,.095f,.050f,.190f,14,8);
            // Wingtip light/fairing housings.
            ellipsoid(5.18f*s,.18f,-.03f,.115f,.050f,.165f,16,8);
        }
        // ECS vents on upper shoulders.
        for(float s:new float[]{-1f,1f}) for(int i=0;i<5;i++){
            float z=.72f+i*.15f;
            box(.62f*s,.775f,z,.018f,.020f,.095f);
        }
    }

    /**
     * V84 engine visual package.
     * Keeps all geometry procedural and inexpensive enough for the mobile renderer.
     */
    private void engineBayAndCoreDetails(){
        // Deep compressor presentation: casing ring, spinner and eight front-frame struts.
        for(float side:new float[]{-1f,1f}){
            float cx=.94f*side,cy=-.09f;
            part=COMPRESSOR_FACE;
            ellipticRing(cx,cy,-1.145f,.267f,.74f,.028f,32);
            ellipsoid(cx,cy,-1.175f,.070f,.052f,.115f,18,10);
            for(int i=0;i<8;i++){
                double a=2*Math.PI*i/8.0;
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                cylinderBetween(cx+.078f*ca,cy+.078f*.74f*sa,-1.150f,
                        cx+.225f*ca,cy+.225f*.74f*sa,-1.135f,.0085f,7);
            }
        }

        for(float cx:new float[]{-.72f,.72f}){
            // Nacelle circumferential service bands break the long "single tube" silhouette.
            part=DETAIL;
            ellipticRing(cx,-.10f,.22f,.505f,.58f,.016f,30);
            ellipticRing(cx,-.10f,.92f,.595f,.59f,.014f,32);
            ellipticRing(cx,-.10f,1.70f,.655f,.59f,.014f,32);
            ellipticRing(cx,-.10f,2.42f,.640f,.59f,.014f,32);
            ellipticRing(cx,-.10f,2.92f,.575f,.59f,.016f,30);

            // Upper/lower longitudinal casing rails.
            float s=cx<0f?-1f:1f;
            ribbon(new float[][]{{1.07f*s,.255f,.05f},{1.21f*s,.285f,.86f},{1.28f*s,.285f,1.70f},{1.21f*s,.240f,2.48f},{1.08f*s,.175f,2.93f}},.015f,.016f);
            ribbon(new float[][]{{.43f*s,-.305f,.18f},{.36f*s,-.335f,.96f},{.34f*s,-.340f,1.78f},{.39f*s,-.315f,2.46f},{.47f*s,-.265f,2.91f}},.013f,.014f);

            // Access/service blisters around the hot-section casing.
            ellipsoid((.72f+.54f)*s,.035f,2.16f,.055f,.038f,.105f,12,7);
            ellipsoid((.72f+.50f)*s,-.235f,2.58f,.048f,.034f,.095f,12,7);
        }

        // Twin-engine center-bay bridge and plumbing hints.
        part=DETAIL;
        cylinderBetween(-.38f,.24f,2.42f,.38f,.24f,2.42f,.018f,9);
        cylinderBetween(-.34f,.18f,2.67f,.34f,.18f,2.67f,.015f,8);
    }

    private void controlSurfaceHardware(){
        part=DETAIL;
        for(float s:new float[]{-1f,1f}){
            // Stabilator root hinge drum and two rudder hinge fairings.
            cylinderBetween(.74f*s,.33f,1.88f,.92f*s,.33f,1.88f,.075f,14);
            ellipsoid(.95f*s,1.38f,2.42f,.055f,.105f,.080f,12,8);
            ellipsoid(1.05f*s,1.88f,2.53f,.050f,.100f,.075f,12,8);
            // Flaperon drive fairing.
            ellipsoid(2.10f*s,.37f,.86f,.080f,.050f,.235f,14,8);
        }
    }

    private void nozzleActuators(){
        for(float cx:new float[]{-.72f,.72f}){
            part=HEAT_SHIELD;
            // Elliptical bands follow the actual nacelle/nozzle section rather than a round ring.
            ellipticRing(cx,-.10f,3.18f,.525f,.61f,.035f,36);
            ellipticRing(cx,-.10f,3.40f,.455f,.61f,.025f,34);

            part=NOZZLE_PETAL;
            ellipticRing(cx,-.10f,3.56f,.397f,.61f,.016f,32);

            part=DETAIL;
            // Two concentric actuator families create visible mechanical depth.
            for(int i=0;i<16;i++){
                double a=2*Math.PI*i/16.0;
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                cylinderBetween(cx+.515f*ca,-.10f+.515f*.61f*sa,3.19f,
                        cx+.355f*ca,-.10f+.355f*.61f*sa,3.68f,.012f,8);
                // Pivot boss at each primary actuator.
                ellipsoid(cx+.475f*ca,-.10f+.475f*.61f*sa,3.29f,.022f,.018f,.030f,8,5);
            }
            for(int i=0;i<8;i++){
                double a=2*Math.PI*(i+.5)/8.0;
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                cylinderBetween(cx+.435f*ca,-.10f+.435f*.61f*sa,3.34f,
                        cx+.305f*ca,-.10f+.305f*.61f*sa,3.76f,.010f,7);
            }

            // Inner exhaust liner and flame-holder spider remain attached to thrust-vector motion.
            part=NOZZLE_INNER;
            ellipticRing(cx,-.10f,3.78f,.285f,.60f,.020f,30);
            ellipticRing(cx,-.10f,3.91f,.225f,.58f,.016f,28);
            ellipsoid(cx,-.10f,3.86f,.050f,.034f,.085f,14,8);
            for(int i=0;i<8;i++){
                double a=2*Math.PI*i/8.0;
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                cylinderBetween(cx+.055f*ca,-.10f+.055f*.58f*sa,3.86f,
                        cx+.215f*ca,-.10f+.215f*.58f*sa,3.86f,.008f,7);
            }
        }
        part=DETAIL;
    }

    private void gearBayAndBrakeDetails(){
        // Nose gear bay lip/sidewalls and actuator.
        part=GEAR_DOOR;
        box(-.29f,-.44f,-3.77f,.035f,.18f,1.05f);box(.29f,-.44f,-3.77f,.035f,.18f,1.05f);
        box(0f,-.49f,-3.26f,.48f,.055f,.040f);box(0f,-.49f,-4.27f,.48f,.055f,.040f);
        part=GEAR_STRUT;
        cylinderBetween(-.06f,-.90f,-3.76f,.20f,-1.24f,-3.48f,.024f,10);
        cylinderBetween(-.06f,-1.02f,-3.80f,-.25f,-1.36f,-3.98f,.022f,10);
        // Nose scissors/torque links.
        cylinderBetween(-.08f,-1.20f,-3.72f,-.22f,-1.38f,-3.60f,.018f,8);
        cylinderBetween(-.22f,-1.38f,-3.60f,-.06f,-1.52f,-3.74f,.018f,8);

        for(float s:new float[]{-1f,1f}){
            float x=1.70f*s;
            part=GEAR_DOOR;
            // Main gear bay depth/inner wall hints.
            box(1.31f*s,-.45f,.95f,.040f,.22f,1.18f);
            box(1.88f*s,-.45f,.95f,.040f,.22f,1.18f);
            box(1.60f*s,-.49f,.37f,.54f,.055f,.045f);
            box(1.60f*s,-.49f,1.52f,.54f,.055f,.045f);
            part=GEAR_STRUT;
            // Drag brace, side brace, torque link and hydraulic line.
            cylinderBetween(x,-.80f,.62f,1.25f*s,-1.28f,1.15f,.030f,10);
            cylinderBetween(1.25f*s,-1.28f,1.15f,x,-1.50f,1.16f,.026f,10);
            cylinderBetween(x,-1.12f,1.02f,1.87f*s,-1.42f,1.23f,.021f,9);
            cylinderBetween(x-.07f*s,-1.24f,1.08f,x+.08f*s,-1.48f,1.20f,.015f,8);
            part=GEAR_WHEEL;
            // Brake disc / hub layers visible through the wheel.
            cylinderBetween(x-.10f*s,-1.67f,1.18f,x+.10f*s,-1.67f,1.18f,.135f,22);
            cylinderBetween(x-.12f*s,-1.67f,1.18f,x+.12f*s,-1.67f,1.18f,.078f,20);
            part=DETAIL;
            box(x+.13f*s,-1.65f,1.06f,.045f,.070f,.16f); // caliper block
            cylinderBetween(x-.04f*s,-1.15f,.84f,x-.04f*s,-1.49f,1.12f,.010f,7); // hose
        }
    }

    private void surfaceServiceDetails(){
        part=DETAIL;
        // Low-profile access-panel seal strips and antennas.
        for(float s:new float[]{-1f,1f}){
            ribbon(new float[][]{{.38f*s,.78f,-3.34f},{.64f*s,.83f,-2.86f},{.78f*s,.88f,-2.30f}},.010f,.010f);
            ribbon(new float[][]{{.42f*s,.86f,.96f},{.55f*s,.84f,1.44f},{.61f*s,.79f,1.92f}},.010f,.010f);
        }
        prism(new float[][]{{-.025f,.96f,1.64f},{.025f,.96f,1.64f},{.018f,1.11f,1.78f},{-.018f,1.11f,1.78f}},.018f);
        prism(new float[][]{{-.030f,.69f,2.58f},{.030f,.69f,2.58f},{.020f,.83f,2.74f},{-.020f,.83f,2.74f}},.018f);
        // Belly antennas / drain masts.
        prism(new float[][]{{-.020f,-.61f,-2.18f},{.020f,-.61f,-2.18f},{.015f,-.76f,-2.02f},{-.015f,-.76f,-2.02f}},.015f);
        prism(new float[][]{{-.018f,-.60f,.78f},{.018f,-.60f,.78f},{.012f,-.72f,.92f},{-.012f,-.72f,.92f}},.014f);
    }

    private void ring(float x,float y,float z,float r,float width,int sides){
        cylinderBand(x,y,z-width*.5f,z+width*.5f,r,r,sides);
    }

    private void ellipticRing(float x,float y,float z,float r,float yScale,float width,int sides){
        ellipticBand(x,y,z-width*.5f,z+width*.5f,r,r,yScale,sides);
    }

    private void ellipticBand(float x,float y,float z0,float z1,float r0,float r1,float yScale,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] a={x+r0*(float)Math.cos(a0),y+r0*yScale*(float)Math.sin(a0),z0};
            float[] b={x+r1*(float)Math.cos(a0),y+r1*yScale*(float)Math.sin(a0),z1};
            float[] c={x+r1*(float)Math.cos(a1),y+r1*yScale*(float)Math.sin(a1),z1};
            float[] d={x+r0*(float)Math.cos(a1),y+r0*yScale*(float)Math.sin(a1),z0};
            quad(a,b,c,d);
        }
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
        float dx=x1-x0,dy=y1-y0,dz=z1-z0,len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<1e-5f)return;
        dx/=len;dy/=len;dz/=len;float ax=Math.abs(dy)<.88f?0f:1f,ay=Math.abs(dy)<.88f?1f:0f,az=0f;
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
            double p0=-Math.PI/2+Math.PI*j/stacks,p1=-Math.PI/2+Math.PI*(j+1)/stacks,a0=2*Math.PI*i/slices,a1=2*Math.PI*(i+1)/slices;
            float[] A=ell(cx,cy,cz,rx,ry,rz,p0,a0),B=ell(cx,cy,cz,rx,ry,rz,p1,a0),C=ell(cx,cy,cz,rx,ry,rz,p1,a1),D=ell(cx,cy,cz,rx,ry,rz,p0,a1);
            quad(A,B,C,D);
        }
    }

    private float[] ell(float cx,float cy,float cz,float rx,float ry,float rz,double p,double a){
        float cp=(float)Math.cos(p),sp=(float)Math.sin(p),ca=(float)Math.cos(a),sa=(float)Math.sin(a);
        return new float[]{cx+rx*cp*ca,cy+ry*sp,cz+rz*cp*sa};
    }

    private void ribbon(float[][] pts,float width,float depth){
        if(pts.length<2)return;
        for(int i=0;i<pts.length-1;i++){
            float[] a=pts[i],b=pts[i+1],d={b[0]-a[0],b[1]-a[1],b[2]-a[2]};
            float l=(float)Math.sqrt(d[0]*d[0]+d[2]*d[2]);if(l<1e-5f)l=1;
            float px=-d[2]/l*width*.5f,pz=d[0]/l*width*.5f;
            prism(new float[][]{{a[0]+px,a[1],a[2]+pz},{b[0]+px,b[1],b[2]+pz},{b[0]-px,b[1],b[2]-pz},{a[0]-px,a[1],a[2]-pz}},depth);
        }
    }

    private void box(float x,float y,float z,float sx,float sy,float sz){
        float hx=sx*.5f,hz=sz*.5f;prism(new float[][]{{x-hx,y+sy*.5f,z-hz},{x+hx,y+sy*.5f,z-hz},{x+hx,y+sy*.5f,z+hz},{x-hx,y+sy*.5f,z+hz}},sy);
    }

    private void prism(float[][] top,float thickness){
        if(top==null||top.length<3)return;int n=top.length;float[][] bot=new float[n][3];
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
