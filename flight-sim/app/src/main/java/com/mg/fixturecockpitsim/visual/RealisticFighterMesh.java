package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-13.4 ultra-detail procedural fighter mesh.
 * Generic modern twin-engine fighter geometry for the simulator only.
 * Smooth fuselage/canopy/nacelle normals, separate flight controls, detailed gear,
 * cockpit, intake ducts, compressor faces and multi-petal exhausts.
 */
public final class RealisticFighterMesh {
    private static final float SKIN=0f, CANOPY=1f, NOZZLE=2f, INTAKE=3f,
            STAB_L=4f, STAB_R=5f, RUDDER_L=6f, RUDDER_R=7f,
            AFTERBURNER=8f, FLAPERON_L=9f, FLAPERON_R=10f,
            CANOPY_FRAME=11f, NOZZLE_INNER=12f, GEAR_STRUT=13f,
            GEAR_WHEEL=14f, GEAR_DOOR=15f, COCKPIT_TUB=16f,
            SEAT=17f, COAMING=18f, INTAKE_DUCT=19f,
            COMPRESSOR_FACE=20f, NOZZLE_PETAL=21f, FLAME_CORE=22f,
            UPPER_PANEL=23f, NAV_RED=25f, NAV_GREEN=26f, NAV_WHITE=27f,
            HEAT_SHIELD=28f, DETAIL=29f;

    private final List<Float> out=new ArrayList<>();
    private float part=SKIN;

    private RealisticFighterMesh(){}

    public static ProceduralFighterMesh.Mesh build(){
        RealisticFighterMesh b=new RealisticFighterMesh();

        b.part=SKIN;
        b.smoothFuselage();
        b.forebodyChines();
        b.bellyBlend();
        b.fixedWing(-1f); b.fixedWing(1f);
        b.wingLeadingEdge(-1f); b.wingLeadingEdge(1f);
        b.fixedFin(-1f); b.fixedFin(1f);
        b.engineShoulder(-1f); b.engineShoulder(1f);
        b.boatTail();

        b.part=UPPER_PANEL;
        b.upperDeck();
        b.dorsalSpine();
        b.wingRootFairing(-1f); b.wingRootFairing(1f);
        b.engineDeck(-1f); b.engineDeck(1f);
        b.radomeBreak();
        b.panelDetails();

        b.part=FLAPERON_L; b.flaperon(-1f);
        b.part=FLAPERON_R; b.flaperon(1f);
        b.part=STAB_L; b.stabilator(-1f);
        b.part=STAB_R; b.stabilator(1f);
        b.part=RUDDER_L; b.rudder(-1f);
        b.part=RUDDER_R; b.rudder(1f);

        b.part=INTAKE; b.intakeLip(-1f); b.intakeLip(1f);
        b.part=INTAKE_DUCT; b.intakeDuct(-1f); b.intakeDuct(1f);
        b.part=COMPRESSOR_FACE; b.compressorFace(-1f); b.compressorFace(1f);

        b.part=SKIN; b.engineNacelle(-.72f); b.engineNacelle(.72f);
        b.part=HEAT_SHIELD; b.heatShield(-.72f); b.heatShield(.72f);
        b.part=NOZZLE; b.nozzleOuter(-.72f); b.nozzleOuter(.72f);
        b.part=NOZZLE_PETAL; b.nozzlePetals(-.72f); b.nozzlePetals(.72f);
        b.part=NOZZLE_INNER; b.nozzleInner(-.72f); b.nozzleInner(.72f);
        b.part=AFTERBURNER; b.afterburner(-.72f); b.afterburner(.72f);
        b.part=FLAME_CORE; b.flameCore(-.72f); b.flameCore(.72f);

        b.part=COCKPIT_TUB; b.cockpitTub();
        b.part=SEAT; b.ejectionSeat();
        b.part=COAMING; b.instrumentCoaming();
        b.part=CANOPY; b.canopy();
        b.part=CANOPY_FRAME; b.canopyFrame();

        b.part=GEAR_STRUT; b.gearStruts();
        b.part=GEAR_WHEEL; b.gearWheels();
        b.part=GEAR_DOOR; b.gearDoors();

        b.part=DETAIL; b.airframeDetails(); b.gearDetails();
        b.part=NAV_RED; b.navLight(-1f);
        b.part=NAV_GREEN; b.navLight(1f);
        b.part=NAV_WHITE; b.tailLight();

        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++)data[i]=b.out.get(i);
        return new ProceduralFighterMesh.Mesh(data);
    }

    private void smoothFuselage(){
        float[] z={-6.72f,-6.60f,-6.43f,-6.20f,-5.92f,-5.60f,-5.24f,-4.84f,-4.40f,-3.92f,-3.40f,-2.84f,-2.25f,-1.64f,-1.02f,-.38f,.28f,.92f,1.50f,2.02f,2.46f,2.84f,3.16f,3.40f,3.58f,3.70f};
        float[] rx={.008f,.020f,.050f,.105f,.190f,.300f,.430f,.580f,.735f,.875f,.995f,1.095f,1.170f,1.225f,1.255f,1.270f,1.265f,1.235f,1.175f,1.095f,1.000f,.880f,.730f,.550f,.350f,.120f};
        float[] ry={.008f,.018f,.045f,.090f,.155f,.235f,.325f,.430f,.540f,.645f,.735f,.805f,.855f,.885f,.902f,.905f,.890f,.850f,.790f,.710f,.620f,.520f,.415f,.305f,.185f,.065f};
        float[] cy={-.045f,-.045f,-.044f,-.042f,-.038f,-.030f,-.018f,.000f,.026f,.058f,.091f,.122f,.145f,.158f,.164f,.163f,.151f,.126f,.090f,.045f,-.005f,-.055f,-.095f,-.124f,-.142f,-.155f};
        smoothLoft(z,rx,ry,cy,56,.80f);
    }

    private void smoothLoft(float[] z,float[] rx,float[] ry,float[] cy,int sides,float exp){
        int n=Math.min(Math.min(z.length,rx.length),Math.min(ry.length,cy.length));
        for(int s=0;s<n-1;s++){
            float dz=z[s+1]-z[s];
            float drx=(rx[s+1]-rx[s])/Math.max(.001f,dz);
            float dry=(ry[s+1]-ry[s])/Math.max(.001f,dz);
            for(int i=0;i<sides;i++){
                double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
                V p00=section(z[s],rx[s],ry[s],cy[s],a0,exp,drx,dry);
                V p10=section(z[s+1],rx[s+1],ry[s+1],cy[s+1],a0,exp,drx,dry);
                V p11=section(z[s+1],rx[s+1],ry[s+1],cy[s+1],a1,exp,drx,dry);
                V p01=section(z[s],rx[s],ry[s],cy[s],a1,exp,drx,dry);
                quadSmooth(p00,p10,p11,p01);
            }
        }
    }

    private V section(float z,float rx,float ry,float cy,double a,float exp,float drx,float dry){
        float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
        float sy=Math.signum(sa)*(float)Math.pow(Math.abs(sa),exp);
        float x=rx*ca;
        float y=cy+ry*sy;
        // Slight faceting/chine character without losing smooth specular flow.
        float shoulder=(float)Math.pow(Math.abs(ca),4.2);
        y+=ry*.035f*shoulder;
        float nx=ca/Math.max(.03f,rx);
        float ny=sy/Math.max(.03f,ry);
        float nz=-(drx*ca+dry*sy)*.55f;
        return new V(x,y,z,nx,ny,nz);
    }

    private void forebodyChines(){chine(-1f);chine(1f);}
    private void chine(float side){
        ribbon(new float[][]{
                {.06f*side,.03f,-6.30f},{.18f*side,.10f,-5.80f},{.39f*side,.19f,-5.24f},{.68f*side,.28f,-4.66f},
                {1.00f*side,.36f,-4.04f},{1.33f*side,.42f,-3.38f},{1.61f*side,.45f,-2.70f},{1.84f*side,.45f,-2.00f},
                {2.02f*side,.42f,-1.28f},{2.13f*side,.38f,-.56f},{2.12f*side,.35f,.08f},{1.98f*side,.35f,.58f},
                {1.72f*side,.38f,.98f},{1.38f*side,.42f,1.28f}
        },.065f,.050f);
    }

    private void bellyBlend(){
        prism(new float[][]{{-.31f,-.34f,-4.72f},{.31f,-.34f,-4.72f},{.60f,-.52f,-3.45f},{.78f,-.62f,-2.00f},{.84f,-.64f,-.45f},{.81f,-.61f,1.12f},{.64f,-.51f,2.35f},{.36f,-.35f,3.10f},{-.36f,-.35f,3.10f},{-.64f,-.51f,2.35f},{-.81f,-.61f,1.12f},{-.84f,-.64f,-.45f},{-.78f,-.62f,-2.00f},{-.60f,-.52f,-3.45f}},.065f);
    }

    private void fixedWing(float side){
        airfoil(new float[][]{
                {.78f*side,-2.76f},{1.12f*side,-2.60f},{2.16f*side,-2.02f},{3.34f*side,-1.34f},{4.45f*side,-.69f},{5.30f*side,-.18f},
                {5.12f*side,.04f},{4.50f*side,.48f},{3.56f*side,.69f},{2.32f*side,.72f},{1.28f*side,.72f},{.86f*side,.48f}
        },.32f,.105f);
    }

    private void wingLeadingEdge(float side){
        part=DETAIL;
        ribbon(new float[][]{{.82f*side,.43f,-2.72f},{1.30f*side,.40f,-2.48f},{2.26f*side,.33f,-1.94f},{3.38f*side,.27f,-1.30f},{4.47f*side,.20f,-.66f},{5.28f*side,.145f,-.18f}},.020f,.026f);
        part=SKIN;
    }

    private void flaperon(float side){
        airfoil(new float[][]{{1.25f*side,.70f},{2.25f*side,.71f},{3.55f*side,.67f},{4.50f*side,.50f},{4.15f*side,1.12f},{3.22f*side,1.40f},{1.40f*side,1.48f}},.245f,.060f);
    }

    private void stabilator(float side){
        airfoil(new float[][]{{.68f*side,1.70f},{1.38f*side,1.86f},{2.98f*side,2.28f},{2.56f*side,3.27f},{1.36f*side,3.09f},{.70f*side,2.87f}},.29f,.065f);
    }

    private void fixedFin(float side){
        finVolume(side,new float[][]{{.54f,1.56f},{.75f,1.78f},{1.54f,2.14f},{2.60f,2.34f},{2.45f,2.58f},{.82f,2.73f},{.54f,3.14f}},.090f);
    }

    private void rudder(float side){
        finVolume(side,new float[][]{{2.43f,2.56f},{.83f,2.73f},{.56f,3.14f},{1.18f,3.00f},{2.34f,2.72f}},.065f);
    }

    private void finVolume(float side,float[][] yz,float halfThick){
        float cant=.96f*side;
        int n=yz.length;
        float[][] a=new float[n][3],b=new float[n][3];
        for(int i=0;i<n;i++){
            float y=yz[i][0],z=yz[i][1];
            float center=cant+side*(y-.50f)*.17f;
            a[i]=new float[]{center-halfThick,y,z};
            b[i]=new float[]{center+halfThick,y,z};
        }
        for(int i=1;i<n-1;i++){tri(a[0],a[i+1],a[i]);tri(b[0],b[i],b[i+1]);}
        for(int i=0;i<n;i++)quad(a[i],b[i],b[(i+1)%n],a[(i+1)%n]);
    }

    private void upperDeck(){
        prism(new float[][]{{-.48f,.63f,-3.02f},{.48f,.63f,-3.02f},{.68f,.79f,-2.18f},{.81f,.93f,-1.28f},{.87f,1.00f,-.34f},{.85f,.98f,.58f},{.77f,.91f,1.44f},{.63f,.79f,2.20f},{.45f,.64f,2.84f},{-.45f,.64f,2.84f},{-.63f,.79f,2.20f},{-.77f,.91f,1.44f},{-.85f,.98f,.58f},{-.87f,1.00f,-.34f},{-.81f,.93f,-1.28f},{-.68f,.79f,-2.18f}},.075f);
    }

    private void dorsalSpine(){
        prism(new float[][]{{-.24f,.99f,.48f},{.24f,.99f,.48f},{.31f,.96f,1.15f},{.29f,.89f,1.78f},{.23f,.79f,2.33f},{.14f,.67f,2.80f},{-.14f,.67f,2.80f},{-.23f,.79f,2.33f},{-.29f,.89f,1.78f},{-.31f,.96f,1.15f}},.022f);
    }

    private void wingRootFairing(float side){
        prism(new float[][]{{.69f*side,.61f,-2.75f},{1.02f*side,.55f,-2.48f},{1.48f*side,.47f,-1.92f},{1.75f*side,.41f,-1.22f},{1.72f*side,.40f,-.44f},{1.50f*side,.42f,.28f},{1.16f*side,.49f,.88f},{.86f*side,.57f,1.02f},{.72f*side,.63f,.20f}},.024f);
    }

    private void engineShoulder(float side){
        prism(new float[][]{{.48f*side,.68f,.48f},{1.28f*side,.50f,.60f},{1.39f*side,.47f,1.32f},{1.34f*side,.44f,2.05f},{1.22f*side,.38f,2.70f},{1.02f*side,.29f,3.18f},{.54f*side,.53f,2.72f},{.42f*side,.69f,1.66f}},.052f);
    }

    private void engineDeck(float side){
        prism(new float[][]{{.42f*side,.75f,.64f},{1.20f*side,.54f,.68f},{1.26f*side,.51f,1.42f},{1.21f*side,.47f,2.12f},{1.08f*side,.40f,2.75f},{.86f*side,.30f,3.20f},{.55f*side,.52f,2.72f},{.45f*side,.69f,1.52f}},.020f);
    }

    private void boatTail(){
        prism(new float[][]{{-1.13f,.29f,2.62f},{1.13f,.29f,2.62f},{1.05f,.20f,3.12f},{.90f,.08f,3.53f},{.40f,-.02f,3.72f},{-.40f,-.02f,3.72f},{-.90f,.08f,3.53f},{-1.05f,.20f,3.12f}},.17f);
    }

    private void radomeBreak(){
        ribbon(new float[][]{{-.56f,.44f,-4.90f},{-.46f,.48f,-4.88f},{0f,.54f,-4.86f},{.46f,.48f,-4.88f},{.56f,.44f,-4.90f}},.012f,.012f);
    }

    private void panelDetails(){
        for(float s:new float[]{-1f,1f}){
            ribbon(new float[][]{{.94f*s,.405f,-2.20f},{1.78f*s,.34f,-1.70f},{2.70f*s,.27f,-1.15f},{3.54f*s,.22f,-.66f}},.010f,.014f);
            ribbon(new float[][]{{1.06f*s,.285f,.16f},{2.00f*s,.245f,.36f},{3.10f*s,.19f,.56f},{4.08f*s,.15f,.60f}},.008f,.010f);
        }
        ribbon(new float[][]{{-.38f,.985f,-.72f},{.38f,.985f,-.72f}},.010f,.012f);
        ribbon(new float[][]{{-.34f,.905f,1.05f},{.34f,.905f,1.05f}},.010f,.012f);
    }

    private void intakeLip(float side){
        float s=side;
        prism(new float[][]{{.98f*s,.40f,-3.16f},{1.42f*s,.36f,-2.86f},{1.75f*s,.24f,-2.43f},{1.70f*s,-.15f,-2.27f},{1.18f*s,-.25f,-2.55f},{.93f*s,-.03f,-3.00f}},.10f);
        part=DETAIL;
        ribbon(new float[][]{{1.01f*s,.39f,-3.13f},{1.43f*s,.35f,-2.84f},{1.72f*s,.23f,-2.44f},{1.68f*s,-.13f,-2.30f}},.020f,.020f);
        part=INTAKE;
    }

    private void intakeDuct(float side){
        duct(side,1.38f,-.02f,-2.55f,.29f,.25f,1.18f,-.07f,-1.84f,.25f,.22f,20);
        duct(side,1.18f,-.07f,-1.84f,.25f,.22f,.94f,-.09f,-1.16f,.23f,.20f,20);
    }

    private void duct(float side,float cx0,float cy0,float z0,float rx0,float ry0,float cx1,float cy1,float z1,float rx1,float ry1,int n){
        for(int i=0;i<n;i++){
            double a0=2*Math.PI*i/n,a1=2*Math.PI*(i+1)/n;
            float[] a={side*(cx0+rx0*(float)Math.cos(a0)),cy0+ry0*(float)Math.sin(a0),z0};
            float[] b={side*(cx1+rx1*(float)Math.cos(a0)),cy1+ry1*(float)Math.sin(a0),z1};
            float[] c={side*(cx1+rx1*(float)Math.cos(a1)),cy1+ry1*(float)Math.sin(a1),z1};
            float[] d={side*(cx0+rx0*(float)Math.cos(a1)),cy0+ry0*(float)Math.sin(a1),z0};
            quad(a,b,c,d);
        }
    }

    private void compressorFace(float side){
        float cx=.94f*side,cy=-.09f,z=-1.12f,r=.245f,hub=.055f;
        int blades=32;
        for(int i=0;i<blades;i++){
            double a0=2*Math.PI*i/blades,a1=2*Math.PI*(i+1)/blades;
            float[] h0={cx+hub*(float)Math.cos(a0),cy+hub*.74f*(float)Math.sin(a0),z-.012f};
            float[] h1={cx+hub*(float)Math.cos(a1),cy+hub*.74f*(float)Math.sin(a1),z-.012f};
            float[] p0={cx+r*(float)Math.cos(a0+.07),cy+r*.74f*(float)Math.sin(a0+.07),z};
            float[] p1={cx+r*(float)Math.cos(a1-.03),cy+r*.74f*(float)Math.sin(a1-.03),z};
            quad(h0,p0,p1,h1);
        }
        ellipsoid(cx,cy,z-.018f,.062f,.050f,.030f,16,8);
    }

    private void engineNacelle(float x){
        float[] z={-.98f,-.66f,-.28f,.16f,.66f,1.20f,1.72f,2.18f,2.58f,2.90f,3.15f,3.32f};
        float[] rx={.24f,.31f,.40f,.50f,.58f,.63f,.66f,.66f,.63f,.58f,.52f,.46f};
        float[] ry={.15f,.19f,.24f,.29f,.34f,.37f,.39f,.39f,.37f,.33f,.29f,.25f};
        smoothTube(x,-.10f,z,rx,ry,40);
    }

    private void smoothTube(float cx,float cy,float[] z,float[] rx,float[] ry,int sides){
        for(int s=0;s<z.length-1;s++)for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            V a=tubeV(cx,cy,z[s],rx[s],ry[s],a0),b=tubeV(cx,cy,z[s+1],rx[s+1],ry[s+1],a0),c=tubeV(cx,cy,z[s+1],rx[s+1],ry[s+1],a1),d=tubeV(cx,cy,z[s],rx[s],ry[s],a1);
            quadSmooth(a,b,c,d);
        }
    }

    private V tubeV(float cx,float cy,float z,float rx,float ry,double a){float ca=(float)Math.cos(a),sa=(float)Math.sin(a);return new V(cx+rx*ca,cy+ry*sa,z,ca,sa,0);}

    private void heatShield(float x){tubeSurface(x,-.10f,2.70f,3.42f,.61f,.44f,.61f,36);}
    private void nozzleOuter(float x){tubeSurface(x,-.10f,3.08f,3.56f,.49f,.405f,.61f,40);}

    private void nozzlePetals(float x){
        int petals=20;float z0=3.34f,z1=3.82f,r0=.425f,r1=.315f;
        for(int i=0;i<petals;i++){
            double mid=2*Math.PI*(i+.5)/petals;
            double a0=2*Math.PI*i/petals+.020,a1=2*Math.PI*(i+1)/petals-.020;
            float bulge=.020f*(float)Math.cos(mid*2);
            quad(new float[]{x+(r0+bulge)*(float)Math.cos(a0),-.10f+(r0+bulge)*.61f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*.61f*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.61f*(float)Math.sin(a1),z1},new float[]{x+(r0+bulge)*(float)Math.cos(a1),-.10f+(r0+bulge)*.61f*(float)Math.sin(a1),z0});
        }
        part=DETAIL;
        for(int i=0;i<petals;i++){
            double a=2*Math.PI*i/petals;
            float x0=x+.405f*(float)Math.cos(a),y0=-.10f+.405f*.61f*(float)Math.sin(a);
            box(x0,y0,3.55f,.018f,.018f,.28f);
        }
        part=NOZZLE_PETAL;
    }

    private void nozzleInner(float x){tubeSurface(x,-.10f,3.55f,3.96f,.285f,.205f,.60f,36);}
    private void afterburner(float x){tubeSurface(x,-.10f,3.92f,5.10f,.205f,.035f,.55f,26);}
    private void flameCore(float x){tubeSurface(x,-.10f,3.94f,4.72f,.105f,.012f,.52f,20);}

    private void tubeSurface(float x,float y,float z0,float z1,float r0,float r1,float yScale,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            quad(new float[]{x+r0*(float)Math.cos(a0),y+r0*yScale*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),y+r1*yScale*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),y+r1*yScale*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),y+r0*yScale*(float)Math.sin(a1),z0});
        }
    }

    private void canopy(){
        float[] z={-2.38f,-2.20f,-1.96f,-1.66f,-1.32f,-.94f,-.54f,-.14f,.24f,.56f,.82f};
        float[] rx={.18f,.31f,.42f,.50f,.55f,.57f,.57f,.53f,.46f,.34f,.18f};
        float[] base={.79f,.80f,.82f,.84f,.85f,.86f,.86f,.85f,.83f,.81f,.79f};
        float[] crown={.88f,.99f,1.10f,1.20f,1.28f,1.33f,1.34f,1.30f,1.22f,1.08f,.90f};
        int arcs=30;
        for(int s=0;s<z.length-1;s++)for(int i=0;i<arcs;i++){
            double a0=Math.PI*i/arcs,a1=Math.PI*(i+1)/arcs;
            V p00=canopyV(z[s],rx[s],base[s],crown[s],a0),p10=canopyV(z[s+1],rx[s+1],base[s+1],crown[s+1],a0),p11=canopyV(z[s+1],rx[s+1],base[s+1],crown[s+1],a1),p01=canopyV(z[s],rx[s],base[s],crown[s],a1);
            quadSmooth(p00,p10,p11,p01);
        }
    }

    private V canopyV(float z,float rx,float base,float crown,double a){
        float ca=(float)Math.cos(a),sa=(float)Math.sin(a);float x=rx*ca,y=base+(crown-base)*(float)Math.pow(Math.max(0,sa),.86);return new V(x,y,z,ca,Math.max(.15f,sa),-.05f);
    }

    private void canopyFrame(){
        frameBand(-2.36f,.20f,.80f,-2.16f,.34f,.85f,.036f);
        frameBand(-1.39f,.55f,.85f,-1.24f,.56f,.94f,.032f);
        frameBand(.48f,.39f,.81f,.64f,.31f,.87f,.032f);
        ribbon(new float[][]{{-.30f,.82f,-2.30f},{-.25f,.93f,-2.10f},{-.18f,1.08f,-1.82f}},.030f,.024f);
        ribbon(new float[][]{{.30f,.82f,-2.30f},{.25f,.93f,-2.10f},{.18f,1.08f,-1.82f}},.030f,.024f);
    }

    private void frameBand(float z0,float rx0,float y0,float z1,float rx1,float y1,float th){prism(new float[][]{{-rx0,y0,z0},{rx0,y0,z0},{rx1,y1,z1},{-rx1,y1,z1}},th);}

    private void cockpitTub(){
        prism(new float[][]{{-.44f,.78f,-1.98f},{.44f,.78f,-1.98f},{.49f,.79f,-.50f},{.39f,.79f,.48f},{-.39f,.79f,.48f},{-.49f,.79f,-.50f}},.25f);
        box(-.41f,.85f,-.74f,.06f,.16f,1.15f);box(.41f,.85f,-.74f,.06f,.16f,1.15f);
    }

    private void ejectionSeat(){
        box(0f,.80f,.02f,.36f,.31f,.50f);prism(new float[][]{{-.19f,.94f,.04f},{.19f,.94f,.04f},{.17f,1.18f,.42f},{-.17f,1.18f,.42f}},.10f);box(0f,1.12f,.44f,.30f,.16f,.16f);
        part=DETAIL;box(-.14f,1.02f,.20f,.035f,.055f,.44f);box(.14f,1.02f,.20f,.035f,.055f,.44f);part=SEAT;
    }

    private void instrumentCoaming(){
        prism(new float[][]{{-.39f,.90f,-1.90f},{.39f,.90f,-1.90f},{.33f,.99f,-1.43f},{-.33f,.99f,-1.43f}},.075f);box(0f,.93f,-1.36f,.31f,.12f,.12f);
    }

    private void gearStruts(){
        cylinderY(-.06f,-1.04f,-3.78f,.060f,.96f,12);
        cylinderY(-1.68f,-1.02f,.74f,.075f,1.02f,14);cylinderY(1.68f,-1.02f,.74f,.075f,1.02f,14);
        box(-1.68f,-.96f,.82f,.14f,.16f,.82f);box(1.68f,-.96f,.82f,.14f,.16f,.82f);
    }

    private void gearWheels(){
        torusWheel(0f,-1.62f,-3.78f,.205f,.065f,22,10);
        torusWheel(-1.70f,-1.67f,1.18f,.295f,.082f,24,10);
        torusWheel(1.70f,-1.67f,1.18f,.295f,.082f,24,10);
    }

    private void gearDoors(){
        prism(new float[][]{{-.30f,-.51f,-4.20f},{-.03f,-.51f,-4.20f},{-.03f,-.51f,-3.24f},{-.30f,-.51f,-3.24f}},.035f);prism(new float[][]{{.03f,-.51f,-4.20f},{.30f,-.51f,-4.20f},{.30f,-.51f,-3.24f},{.03f,-.51f,-3.24f}},.035f);
        prism(new float[][]{{-1.86f,-.54f,.24f},{-.90f,-.54f,.24f},{-.90f,-.54f,1.65f},{-1.86f,-.54f,1.65f}},.040f);prism(new float[][]{{.90f,-.54f,.24f},{1.86f,-.54f,.24f},{1.86f,-.54f,1.65f},{.90f,-.54f,1.65f}},.040f);
    }

    private void gearDetails(){
        cylinderY(0f,-1.63f,-3.78f,.070f,.10f,12);cylinderY(-1.70f,-1.68f,1.18f,.095f,.12f,12);cylinderY(1.70f,-1.68f,1.18f,.095f,.12f,12);
        box(-1.62f,-1.18f,.98f,.055f,.18f,.30f);box(1.62f,-1.18f,.98f,.055f,.18f,.30f);
    }

    private void airframeDetails(){
        ellipsoid(0f,.49f,-4.58f,.10f,.055f,.24f,14,8);
        ellipsoid(-.38f,.75f,2.33f,.07f,.05f,.13f,12,7);ellipsoid(.38f,.75f,2.33f,.07f,.05f,.13f,12,7);
        box(-.97f,.57f,1.26f,.035f,.055f,.38f);box(.97f,.57f,1.26f,.035f,.055f,.38f);
        box(-.84f,.56f,2.16f,.028f,.050f,.32f);box(.84f,.56f,2.16f,.028f,.050f,.32f);
    }

    private void navLight(float side){ellipsoid(5.08f*side,.18f,.00f,.058f,.034f,.080f,12,7);}
    private void tailLight(){ellipsoid(0f,.12f,3.61f,.055f,.045f,.070f,12,7);}

    private void airfoil(float[][] xz,float yCenter,float halfThickness){
        int n=xz.length;float[][] top=new float[n][3],bot=new float[n][3];
        for(int i=0;i<n;i++){float camber=.012f*(float)Math.sin(Math.PI*i/Math.max(1,n-1));top[i]=new float[]{xz[i][0],yCenter+halfThickness+camber,xz[i][1]};bot[i]=new float[]{xz[i][0],yCenter-halfThickness+camber,xz[i][1]};}
        for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}
        for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private void ribbon(float[][] pts,float width,float depth){
        if(pts.length<2)return;
        for(int i=0;i<pts.length-1;i++){
            float[] a=pts[i],b=pts[i+1];float dx=b[0]-a[0],dz=b[2]-a[2],l=(float)Math.sqrt(dx*dx+dz*dz);if(l<1e-5f)l=1;float px=-dz/l*width*.5f,pz=dx/l*width*.5f;
            prism(new float[][]{{a[0]+px,a[1],a[2]+pz},{b[0]+px,b[1],b[2]+pz},{b[0]-px,b[1],b[2]-pz},{a[0]-px,a[1],a[2]-pz}},depth);
        }
    }

    private void cylinderY(float x,float y,float z,float r,float len,int sides){
        float y0=y-len*.5f,y1=y+len*.5f;
        for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r*(float)Math.cos(a0),y0,z+r*(float)Math.sin(a0)},new float[]{x+r*(float)Math.cos(a1),y0,z+r*(float)Math.sin(a1)},new float[]{x+r*(float)Math.cos(a1),y1,z+r*(float)Math.sin(a1)},new float[]{x+r*(float)Math.cos(a0),y1,z+r*(float)Math.sin(a0)});}
    }

    private void torusWheel(float x,float y,float z,float R,float r,int seg,int tube){
        for(int i=0;i<seg;i++)for(int j=0;j<tube;j++){
            double t0=2*Math.PI*i/seg,t1=2*Math.PI*(i+1)/seg,p0=2*Math.PI*j/tube,p1=2*Math.PI*(j+1)/tube;
            V a=torusV(x,y,z,R,r,t0,p0),b=torusV(x,y,z,R,r,t1,p0),c=torusV(x,y,z,R,r,t1,p1),d=torusV(x,y,z,R,r,t0,p1);quadSmooth(a,b,c,d);
        }
    }

    private V torusV(float x,float y,float z,float R,float r,double t,double p){float cp=(float)Math.cos(p),sp=(float)Math.sin(p),ct=(float)Math.cos(t),st=(float)Math.sin(t),rr=R+r*cp;return new V(x+r*sp,y+rr*ct,z+rr*st,sp,cp*ct,cp*st);}

    private void ellipsoid(float cx,float cy,float cz,float rx,float ry,float rz,int slices,int stacks){
        for(int j=0;j<stacks;j++)for(int i=0;i<slices;i++){
            double p0=-Math.PI/2+Math.PI*j/stacks,p1=-Math.PI/2+Math.PI*(j+1)/stacks,a0=2*Math.PI*i/slices,a1=2*Math.PI*(i+1)/slices;
            V A=ellV(cx,cy,cz,rx,ry,rz,p0,a0),B=ellV(cx,cy,cz,rx,ry,rz,p1,a0),C=ellV(cx,cy,cz,rx,ry,rz,p1,a1),D=ellV(cx,cy,cz,rx,ry,rz,p0,a1);quadSmooth(A,B,C,D);
        }
    }

    private V ellV(float cx,float cy,float cz,float rx,float ry,float rz,double p,double a){float cp=(float)Math.cos(p),sp=(float)Math.sin(p),ca=(float)Math.cos(a),sa=(float)Math.sin(a);float nx=cp*ca,ny=sp,nz=cp*sa;return new V(cx+rx*nx,cy+ry*ny,cz+rz*nz,nx,ny,nz);}

    private void box(float x,float y,float z,float sx,float sy,float sz){float hx=sx*.5f,hz=sz*.5f;prism(new float[][]{{x-hx,y+sy*.5f,z-hz},{x+hx,y+sy*.5f,z-hz},{x+hx,y+sy*.5f,z+hz},{x-hx,y+sy*.5f,z+hz}},sy);}

    private void prism(float[][] top,float thickness){
        if(top==null||top.length<3)return;int n=top.length;float[][] bot=new float[n][3];for(int i=0;i<n;i++)bot[i]=new float[]{top[i][0],top[i][1]-thickness,top[i][2]};
        for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}
        for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1;emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);}

    private void quadSmooth(V a,V b,V c,V d){emit(a);emit(b);emit(c);emit(a);emit(c);emit(d);}
    private void emit(V v){float l=(float)Math.sqrt(v.nx*v.nx+v.ny*v.ny+v.nz*v.nz);if(l<1e-6f)l=1;emit(new float[]{v.x,v.y,v.z},v.nx/l,v.ny/l,v.nz/l);}
    private void emit(float[] p,float nx,float ny,float nz){out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);}

    private static final class V{final float x,y,z,nx,ny,nz;V(float x,float y,float z,float nx,float ny,float nz){this.x=x;this.y=y;this.z=z;this.nx=nx;this.ny=ny;this.nz=nz;}}
}
