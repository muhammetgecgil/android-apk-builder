package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/**
 * AVM-12.0 high-detail procedural fighter mesh.
 *
 * Geometry is a generic modern twin-engine fighter silhouette built for the simulator.
 * It keeps the existing part IDs so animated stabilators, rudders, flaperons,
 * landing gear and nozzle vectoring continue to work in Jet3DView.
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
        b.fuselage();
        b.forebodyChines();
        b.lowerBellyKeel();
        b.fixedWings();
        b.fixedFin(-1f);
        b.fixedFin(1f);
        b.engineShoulders();
        b.boatTail();

        b.part=UPPER_PANEL;
        b.upperDeck();
        b.dorsalSpine();
        b.wingRootFairings();
        b.engineDeck();
        b.accessPanels();

        b.part=FLAPERON_L;b.flaperon(-1f);
        b.part=FLAPERON_R;b.flaperon(1f);

        b.part=STAB_L;b.stabilator(-1f);
        b.part=STAB_R;b.stabilator(1f);

        b.part=RUDDER_L;b.rudder(-1f);
        b.part=RUDDER_R;b.rudder(1f);

        b.part=INTAKE;b.intake(-1f);b.intake(1f);
        b.part=INTAKE_DUCT;b.intakeDuct(-1f);b.intakeDuct(1f);
        b.part=COMPRESSOR_FACE;b.compressorFace(-1f);b.compressorFace(1f);

        b.part=SKIN;b.engineNacelle(-.72f);b.engineNacelle(.72f);
        b.part=HEAT_SHIELD;b.heatShield(-.72f);b.heatShield(.72f);
        b.part=NOZZLE;b.nozzle(-.72f);b.nozzle(.72f);
        b.part=NOZZLE_PETAL;b.nozzlePetals(-.72f);b.nozzlePetals(.72f);
        b.part=NOZZLE_INNER;b.nozzleInner(-.72f);b.nozzleInner(.72f);
        b.part=AFTERBURNER;b.afterburner(-.72f);b.afterburner(.72f);
        b.part=FLAME_CORE;b.flameCore(-.72f);b.flameCore(.72f);

        b.part=COCKPIT_TUB;b.cockpitTub();
        b.part=SEAT;b.ejectionSeat();
        b.part=COAMING;b.instrumentCoaming();
        b.part=CANOPY;b.canopy();
        b.part=CANOPY_FRAME;b.canopyFrame();

        b.part=GEAR_STRUT;b.gearStruts();
        b.part=GEAR_WHEEL;b.gearWheels();
        b.part=GEAR_DOOR;b.gearDoors();

        b.part=DETAIL;b.antennasAndSensors();
        b.part=NAV_RED;b.navLight(-1f);
        b.part=NAV_GREEN;b.navLight(1f);
        b.part=NAV_WHITE;b.tailLight();

        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++)data[i]=b.out.get(i);
        return new ProceduralFighterMesh.Mesh(data);
    }

    private void fuselage(){
        float[] z={-6.55f,-6.30f,-6.00f,-5.62f,-5.18f,-4.68f,-4.12f,-3.52f,-2.88f,-2.20f,
                -1.48f,-.74f,.00f,.72f,1.42f,2.08f,2.64f,3.08f,3.42f,3.66f};
        float[] rx={.012f,.038f,.085f,.17f,.30f,.46f,.64f,.81f,.96f,1.08f,
                1.17f,1.23f,1.27f,1.28f,1.25f,1.18f,1.08f,.94f,.74f,.48f};
        float[] ry={.012f,.035f,.075f,.14f,.23f,.34f,.46f,.58f,.69f,.78f,
                .84f,.88f,.90f,.89f,.84f,.76f,.66f,.54f,.41f,.27f};
        float[] cy={-.045f,-.043f,-.040f,-.034f,-.024f,-.008f,.015f,.046f,.082f,.116f,
                .142f,.156f,.158f,.150f,.126f,.090f,.045f,-.010f,-.068f,-.105f};
        loft(z,rx,ry,cy,44,true);
    }

    private void loft(float[] z,float[] rx,float[] ry,float[] cy,int sides,boolean stealthSection){
        int n=Math.min(Math.min(z.length,rx.length),Math.min(ry.length,cy.length));
        for(int s=0;s<n-1;s++){
            for(int i=0;i<sides;i++){
                double a0=2.0*Math.PI*i/sides,a1=2.0*Math.PI*(i+1)/sides;
                float[] p00=sectionPoint(z[s],rx[s],ry[s],cy[s],a0,stealthSection);
                float[] p10=sectionPoint(z[s+1],rx[s+1],ry[s+1],cy[s+1],a0,stealthSection);
                float[] p11=sectionPoint(z[s+1],rx[s+1],ry[s+1],cy[s+1],a1,stealthSection);
                float[] p01=sectionPoint(z[s],rx[s],ry[s],cy[s],a1,stealthSection);
                quad(p00,p10,p11,p01);
            }
        }
    }

    private float[] sectionPoint(float z,float rx,float ry,float cy,double a,boolean stealth){
        float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
        float x=rx*ca;
        float sy=Math.signum(sa)*(float)Math.pow(Math.abs(sa),.82);
        float y=cy+ry*sy;
        if(stealth){
            float shoulder=(float)Math.pow(Math.abs(ca),3.2);
            y+=ry*.055f*shoulder;
            if(sa<0)y+=ry*.035f*(1f-Math.abs(ca));
        }
        return new float[]{x,y,z};
    }

    private void forebodyChines(){chine(-1f);chine(1f);}

    private void chine(float side){
        prism(new float[][]{
                {.05f*side,.06f,-6.08f},{.20f*side,.13f,-5.42f},{.45f*side,.22f,-4.72f},
                {.78f*side,.31f,-4.02f},{1.12f*side,.39f,-3.28f},{1.46f*side,.43f,-2.50f},
                {1.74f*side,.42f,-1.70f},{1.94f*side,.39f,-.90f},{2.02f*side,.35f,-.18f},
                {1.91f*side,.34f,.40f},{1.64f*side,.37f,.92f},{1.31f*side,.40f,1.26f}
        },.055f);
    }

    private void lowerBellyKeel(){
        prism(new float[][]{
                {-.34f,-.34f,-4.55f},{.34f,-.34f,-4.55f},{.62f,-.54f,-3.25f},
                {.76f,-.60f,-1.78f},{.82f,-.61f,-.20f},{.78f,-.58f,1.20f},
                {.60f,-.49f,2.38f},{.34f,-.34f,3.10f},{-.34f,-.34f,3.10f},
                {-.60f,-.49f,2.38f},{-.78f,-.58f,1.20f},{-.82f,-.61f,-.20f},
                {-.76f,-.60f,-1.78f},{-.62f,-.54f,-3.25f}
        },.075f);
    }

    private void fixedWings(){fixedWing(-1f);fixedWing(1f);}

    private void fixedWing(float side){
        float[][] root={
                {.86f*side,.34f,-2.62f},{1.24f*side,.35f,-2.42f},
                {1.30f*side,.27f,.66f},{1.14f*side,.25f,.73f}
        };
        float[][] mid={
                {1.20f*side,.33f,-2.42f},{3.18f*side,.27f,-1.16f},
                {3.55f*side,.19f,.63f},{1.30f*side,.25f,.68f}
        };
        float[][] outer={
                {3.14f*side,.25f,-1.16f},{5.18f*side,.17f,-.10f},
                {4.52f*side,.14f,.63f},{3.55f*side,.18f,.63f}
        };
        prism(root,.22f);prism(mid,.145f);prism(outer,.075f);
        prism(new float[][]{
                {.74f*side,.44f,-2.74f},{1.12f*side,.42f,-2.55f},
                {3.20f*side,.30f,-1.17f},{5.18f*side,.19f,-.10f},
                {4.86f*side,.17f,.10f},{2.92f*side,.27f,-.84f},{1.02f*side,.39f,-2.28f}
        },.026f);
    }

    private void flaperon(float side){
        prism(new float[][]{
                {1.28f*side,.255f,.70f},{3.56f*side,.185f,.65f},
                {4.48f*side,.145f,.66f},{4.16f*side,.135f,1.10f},
                {3.30f*side,.165f,1.38f},{1.42f*side,.235f,1.46f}
        },.072f);
    }

    private void wingRootFairings(){wingRootFairing(-1f);wingRootFairing(1f);}
    private void wingRootFairing(float side){
        prism(new float[][]{
                {.70f*side,.58f,-2.62f},{1.02f*side,.51f,-2.38f},{1.40f*side,.43f,-1.92f},
                {1.63f*side,.39f,-1.22f},{1.60f*side,.38f,-.44f},{1.42f*side,.40f,.30f},
                {1.12f*side,.46f,.88f},{.86f*side,.53f,.98f},{.73f*side,.60f,.24f}
        },.022f);
    }

    private void stabilator(float side){
        prism(new float[][]{
                {.70f*side,.31f,1.72f},{1.42f*side,.30f,1.88f},{2.86f*side,.225f,2.26f},
                {2.42f*side,.205f,3.20f},{1.34f*side,.245f,3.06f},{.72f*side,.285f,2.86f}
        },.075f);
    }

    private void fixedFin(float side){
        prism(new float[][]{
                {.60f*side,.55f,1.52f},{.79f*side,1.42f,1.76f},
                {1.18f*side,2.52f,2.14f},{1.13f*side,2.38f,2.38f},
                {1.02f*side,.78f,2.55f},{.91f*side,.50f,3.08f}
        },.060f);
    }

    private void rudder(float side){
        prism(new float[][]{
                {1.10f*side,2.36f,2.36f},{1.02f*side,.78f,2.54f},
                {.92f*side,.52f,3.08f},{1.05f*side,1.22f,2.94f},
                {1.15f*side,2.28f,2.63f}
        },.055f);
    }

    private void upperDeck(){
        prism(new float[][]{
                {-.50f,.61f,-2.92f},{.50f,.61f,-2.92f},{.70f,.77f,-2.10f},
                {.82f,.91f,-1.18f},{.86f,.96f,-.22f},{.82f,.94f,.72f},
                {.73f,.87f,1.54f},{.60f,.75f,2.26f},{.44f,.61f,2.82f},
                {-.44f,.61f,2.82f},{-.60f,.75f,2.26f},{-.73f,.87f,1.54f},
                {-.82f,.94f,.72f},{-.86f,.96f,-.22f},{-.82f,.91f,-1.18f},
                {-.70f,.77f,-2.10f}
        },.10f);
    }

    private void dorsalSpine(){
        prism(new float[][]{
                {-.26f,.96f,.56f},{.26f,.96f,.56f},{.31f,.93f,1.20f},
                {.29f,.86f,1.82f},{.22f,.75f,2.36f},{.13f,.63f,2.82f},
                {-.13f,.63f,2.82f},{-.22f,.75f,2.36f},{-.29f,.86f,1.82f},
                {-.31f,.93f,1.20f}
        },.028f);
    }

    private void engineShoulders(){engineShoulder(-1f);engineShoulder(1f);}
    private void engineShoulder(float side){
        prism(new float[][]{
                {.52f*side,.65f,.56f},{1.24f*side,.48f,.64f},{1.32f*side,.46f,1.34f},
                {1.28f*side,.43f,2.08f},{1.16f*side,.37f,2.70f},{.99f*side,.27f,3.16f},
                {.55f*side,.51f,2.70f},{.44f*side,.66f,1.70f}
        },.055f);
    }

    private void engineDeck(){engineDeckHalf(-1f);engineDeckHalf(1f);}

    private void engineDeckHalf(float side){
        prism(new float[][]{
                {.42f*side,.73f,.72f},{1.18f*side,.52f,.70f},{1.22f*side,.50f,1.42f},
                {1.18f*side,.46f,2.10f},{1.05f*side,.39f,2.72f},{.84f*side,.29f,3.18f},
                {.56f*side,.50f,2.70f},{.46f*side,.66f,1.56f}
        },.022f);
    }

    private void boatTail(){
        prism(new float[][]{
                {-1.10f,.28f,2.62f},{1.10f,.28f,2.62f},{1.02f,.19f,3.10f},
                {.88f,.08f,3.52f},{.38f,-.02f,3.68f},{-.38f,-.02f,3.68f},
                {-.88f,.08f,3.52f},{-1.02f,.19f,3.10f}
        },.20f);
    }

    private void intake(float side){
        float s=side;
        prism(new float[][]{
                {1.04f*s,.38f,-3.05f},{1.46f*s,.34f,-2.72f},{1.70f*s,.23f,-2.32f},
                {1.66f*s,-.14f,-2.18f},{1.20f*s,-.23f,-2.46f},{.96f*s,-.02f,-2.88f}
        },.11f);
        prism(new float[][]{
                {1.18f*s,.31f,-2.92f},{1.54f*s,.26f,-2.66f},{1.64f*s,.08f,-2.46f},
                {1.60f*s,-.18f,-2.38f},{1.24f*s,-.22f,-2.55f},{1.08f*s,-.05f,-2.78f}
        },.045f);
    }

    private void intakeDuct(float side){
        float s=side;
        ductRing(s,1.37f,-.02f,-2.48f,.27f,.25f,
                   1.18f,-.07f,-1.82f,.24f,.22f);
        ductRing(s,1.18f,-.07f,-1.82f,.24f,.22f,
                   .95f,-.08f,-1.18f,.22f,.20f);
    }

    private void ductRing(float side,float cx0,float cy0,float z0,float rx0,float ry0,
                          float cx1,float cy1,float z1,float rx1,float ry1){
        int n=14;
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
        float cx=.94f*side,cy=-.08f,z=-1.12f,r=.235f,hub=.060f;
        int n=24;
        for(int i=0;i<n;i++){
            double a0=2*Math.PI*i/n,a1=2*Math.PI*(i+1)/n;
            float[] h0={cx+hub*(float)Math.cos(a0),cy+hub*.72f*(float)Math.sin(a0),z-.012f};
            float[] h1={cx+hub*(float)Math.cos(a1),cy+hub*.72f*(float)Math.sin(a1),z-.012f};
            float[] p0={cx+r*(float)Math.cos(a0+.08),cy+r*.72f*(float)Math.sin(a0+.08),z};
            float[] p1={cx+r*(float)Math.cos(a1-.05),cy+r*.72f*(float)Math.sin(a1-.05),z};
            quad(h0,p0,p1,h1);
        }
    }

    private void engineNacelle(float x){
        float[] z={-.96f,-.62f,-.24f,.20f,.72f,1.28f,1.84f,2.34f,2.74f,3.04f,3.28f};
        float[] rx={.25f,.32f,.41f,.50f,.58f,.63f,.65f,.64f,.60f,.54f,.47f};
        float[] ry={.15f,.19f,.24f,.29f,.33f,.36f,.37f,.36f,.33f,.29f,.25f};
        int sides=30;
        for(int s=0;s<z.length-1;s++)for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            quad(
                    new float[]{x+rx[s]*(float)Math.cos(a0),-.10f+ry[s]*(float)Math.sin(a0),z[s]},
                    new float[]{x+rx[s+1]*(float)Math.cos(a0),-.10f+ry[s+1]*(float)Math.sin(a0),z[s+1]},
                    new float[]{x+rx[s+1]*(float)Math.cos(a1),-.10f+ry[s+1]*(float)Math.sin(a1),z[s+1]},
                    new float[]{x+rx[s]*(float)Math.cos(a1),-.10f+ry[s]*(float)Math.sin(a1),z[s]}
            );
        }
    }

    private void heatShield(float x){
        tubeSurface(x,-.10f,2.72f,3.40f,.60f,.43f,.58f,28);
    }

    private void nozzle(float x){
        tubeSurface(x,-.10f,3.10f,3.62f,.48f,.355f,.60f,32);
    }

    private void nozzlePetals(float x){
        int petals=16;
        float z0=3.36f,z1=3.78f,r0=.415f,r1=.325f;
        for(int i=0;i<petals;i++){
            double a0=2*Math.PI*i/petals+.018,a1=2*Math.PI*(i+1)/petals-.018;
            quad(
                    new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0},
                    new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*.60f*(float)Math.sin(a0),z1},
                    new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1},
                    new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0}
            );
        }
    }

    private void nozzleInner(float x){tubeSurface(x,-.10f,3.54f,3.92f,.275f,.205f,.60f,30);}
    private void afterburner(float x){tubeSurface(x,-.10f,3.88f,5.18f,.205f,.035f,.55f,22);}
    private void flameCore(float x){tubeSurface(x,-.10f,3.90f,4.76f,.105f,.012f,.52f,18);}

    private void tubeSurface(float x,float y,float z0,float z1,float r0,float r1,float yScale,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            quad(
                    new float[]{x+r0*(float)Math.cos(a0),y+r0*yScale*(float)Math.sin(a0),z0},
                    new float[]{x+r1*(float)Math.cos(a0),y+r1*yScale*(float)Math.sin(a0),z1},
                    new float[]{x+r1*(float)Math.cos(a1),y+r1*yScale*(float)Math.sin(a1),z1},
                    new float[]{x+r0*(float)Math.cos(a1),y+r0*yScale*(float)Math.sin(a1),z0}
            );
        }
    }

    private void canopy(){
        float[] z={-2.30f,-2.05f,-1.70f,-1.28f,-.82f,-.34f,.12f,.52f,.82f};
        float[] rx={.25f,.39f,.49f,.55f,.57f,.55f,.48f,.37f,.20f};
        float[] base={.77f,.80f,.83f,.85f,.86f,.86f,.84f,.81f,.78f};
        float[] crown={.90f,1.04f,1.16f,1.25f,1.30f,1.30f,1.22f,1.08f,.91f};
        int arcs=22;
        for(int s=0;s<z.length-1;s++){
            for(int i=0;i<arcs;i++){
                double a0=Math.PI*i/arcs,a1=Math.PI*(i+1)/arcs;
                float[] p00=canopyPoint(z[s],rx[s],base[s],crown[s],a0);
                float[] p10=canopyPoint(z[s+1],rx[s+1],base[s+1],crown[s+1],a0);
                float[] p11=canopyPoint(z[s+1],rx[s+1],base[s+1],crown[s+1],a1);
                float[] p01=canopyPoint(z[s],rx[s],base[s],crown[s],a1);
                quad(p00,p10,p11,p01);
            }
        }
    }

    private float[] canopyPoint(float z,float rx,float base,float crown,double a){
        float x=rx*(float)Math.cos(a);
        float s=(float)Math.sin(a);
        float y=base+(crown-base)*(float)Math.pow(Math.max(0,s),.82);
        return new float[]{x,y,z};
    }

    private void canopyFrame(){
        frameBand(-2.28f,.27f,.79f,-2.05f,.40f,.85f,.040f);
        frameBand(-1.34f,.55f,.85f,-1.20f,.55f,.93f,.038f);
        frameBand(.44f,.40f,.81f,.61f,.34f,.88f,.036f);
        prism(new float[][]{{-.29f,.79f,-2.24f},{-.24f,.90f,-2.10f},{-.18f,1.02f,-1.92f},{-.13f,1.12f,-1.72f}},.030f);
        prism(new float[][]{{.29f,.79f,-2.24f},{.24f,.90f,-2.10f},{.18f,1.02f,-1.92f},{.13f,1.12f,-1.72f}},.030f);
    }

    private void frameBand(float z0,float rx0,float y0,float z1,float rx1,float y1,float th){
        prism(new float[][]{{-rx0,y0,z0},{rx0,y0,z0},{rx1,y1,z1},{-rx1,y1,z1}},th);
    }

    private void cockpitTub(){
        prism(new float[][]{
                {-.43f,.76f,-1.92f},{.43f,.76f,-1.92f},{.48f,.78f,-.52f},
                {.38f,.78f,.48f},{-.38f,.78f,.48f},{-.48f,.78f,-.52f}
        },.24f);
    }

    private void ejectionSeat(){
        box(0f,.78f,.02f,.34f,.32f,.48f);
        prism(new float[][]{{-.18f,.91f,.04f},{.18f,.91f,.04f},{.16f,1.15f,.38f},{-.16f,1.15f,.38f}},.10f);
        box(0f,1.10f,.40f,.28f,.15f,.15f);
    }

    private void instrumentCoaming(){
        prism(new float[][]{{-.38f,.88f,-1.86f},{.38f,.88f,-1.86f},{.32f,.97f,-1.42f},{-.32f,.97f,-1.42f}},.080f);
        box(0f,.90f,-1.35f,.30f,.11f,.11f);
    }

    private void gearStruts(){
        box(-.07f,-1.48f,-3.78f,.14f,1.06f,.15f);
        box(-1.70f,-1.43f,.72f,.17f,1.10f,.18f);
        box(1.54f,-1.43f,.72f,.17f,1.10f,.18f);
        box(-1.68f,-.96f,.66f,.15f,.17f,.78f);
        box(1.52f,-.96f,.66f,.15f,.17f,.78f);
    }

    private void gearWheels(){
        wheel(0f,-1.62f,-3.78f,.25f,.17f);
        wheel(-1.70f,-1.67f,1.18f,.36f,.23f);
        wheel(1.70f,-1.67f,1.18f,.36f,.23f);
    }

    private void gearDoors(){
        prism(new float[][]{{-.29f,-.51f,-4.20f},{-.03f,-.51f,-4.20f},{-.03f,-.51f,-3.26f},{-.29f,-.51f,-3.26f}},.038f);
        prism(new float[][]{{.03f,-.51f,-4.20f},{.29f,-.51f,-4.20f},{.29f,-.51f,-3.26f},{.03f,-.51f,-3.26f}},.038f);
        prism(new float[][]{{-1.82f,-.53f,.28f},{-.91f,-.53f,.28f},{-.91f,-.53f,1.62f},{-1.82f,-.53f,1.62f}},.045f);
        prism(new float[][]{{.91f,-.53f,.28f},{1.82f,-.53f,.28f},{1.82f,-.53f,1.62f},{.91f,-.53f,1.62f}},.045f);
    }

    private void accessPanels(){
        prism(new float[][]{{-.62f,.972f,-.94f},{.62f,.972f,-.94f},{.55f,.966f,-.88f},{-.55f,.966f,-.88f}},.006f);
        prism(new float[][]{{-.54f,.902f,.86f},{.54f,.902f,.86f},{.50f,.892f,.93f},{-.50f,.892f,.93f}},.006f);
        for(float side:new float[]{-1f,1f}){
            prism(new float[][]{
                    {1.18f*side,.365f,-1.62f},{2.45f*side,.285f,-.84f},
                    {2.37f*side,.278f,-.76f},{1.16f*side,.355f,-1.48f}
            },.005f);
        }
    }

    private void antennasAndSensors(){
        ellipsoid(0f,.47f,-4.45f,.10f,.055f,.24f,12,8);
        ellipsoid(-.38f,.73f,2.30f,.07f,.05f,.13f,10,6);
        ellipsoid(.38f,.73f,2.30f,.07f,.05f,.13f,10,6);
    }

    private void navLight(float side){ellipsoid(5.02f*side,.18f,.02f,.055f,.032f,.075f,10,6);}
    private void tailLight(){ellipsoid(0f,.12f,3.59f,.055f,.045f,.065f,10,6);}

    private void ellipsoid(float cx,float cy,float cz,float rx,float ry,float rz,int slices,int stacks){
        for(int j=0;j<stacks;j++){
            double p0=-Math.PI/2+Math.PI*j/stacks,p1=-Math.PI/2+Math.PI*(j+1)/stacks;
            for(int i=0;i<slices;i++){
                double a0=2*Math.PI*i/slices,a1=2*Math.PI*(i+1)/slices;
                float[] p00={cx+rx*(float)(Math.cos(p0)*Math.cos(a0)),cy+ry*(float)Math.sin(p0),cz+rz*(float)(Math.cos(p0)*Math.sin(a0))};
                float[] p10={cx+rx*(float)(Math.cos(p1)*Math.cos(a0)),cy+ry*(float)Math.sin(p1),cz+rz*(float)(Math.cos(p1)*Math.sin(a0))};
                float[] p11={cx+rx*(float)(Math.cos(p1)*Math.cos(a1)),cy+ry*(float)Math.sin(p1),cz+rz*(float)(Math.cos(p1)*Math.sin(a1))};
                float[] p01={cx+rx*(float)(Math.cos(p0)*Math.cos(a1)),cy+ry*(float)Math.sin(p0),cz+rz*(float)(Math.cos(p0)*Math.sin(a1))};
                quad(p00,p10,p11,p01);
            }
        }
    }

    private void wheel(float x,float y,float z,float r,float width){
        int n=20;
        float x0=x-width*.5f,x1=x+width*.5f;
        for(int i=0;i<n;i++){
            double a0=2*Math.PI*i/n,a1=2*Math.PI*(i+1)/n;
            float[] a={x0,y+r*(float)Math.cos(a0),z+r*(float)Math.sin(a0)};
            float[] b={x1,y+r*(float)Math.cos(a0),z+r*(float)Math.sin(a0)};
            float[] c={x1,y+r*(float)Math.cos(a1),z+r*(float)Math.sin(a1)};
            float[] d={x0,y+r*(float)Math.cos(a1),z+r*(float)Math.sin(a1)};
            quad(a,b,c,d);
        }
    }

    private void box(float x,float y,float z,float sx,float sy,float sz){
        float hx=sx*.5f,hz=sz*.5f;
        prism(new float[][]{{x-hx,y+sy,z-hz},{x+hx,y+sy,z-hz},{x+hx,y+sy,z+hz},{x-hx,y+sy,z+hz}},sy);
    }

    private void prism(float[][] top,float thickness){
        if(top==null||top.length<3)return;
        int n=top.length;
        float[][] bot=new float[n][3];
        for(int i=0;i<n;i++)bot[i]=offset(top[i],0,-thickness,0);
        for(int i=1;i<n-1;i++){
            tri(top[0],top[i],top[i+1]);
            tri(bot[0],bot[i+1],bot[i]);
        }
        for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);
    }

    private static float[] offset(float[] p,float x,float y,float z){return new float[]{p[0]+x,p[1]+y,p[2]+z};}
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}

    private void tri(float[] a,float[] b,float[] c){
        float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2];
        float vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(l<1e-6f)l=1f;
        emit(a,nx/l,ny/l,nz/l);
        emit(b,nx/l,ny/l,nz/l);
        emit(c,nx/l,ny/l,nz/l);
    }

    private void emit(float[] p,float nx,float ny,float nz){
        out.add(p[0]);out.add(p[1]);out.add(p[2]);
        out.add(nx);out.add(ny);out.add(nz);out.add(part);
    }
}
