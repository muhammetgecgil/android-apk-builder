package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/** Temporary volumetric fighter mesh used until the production GLB is accepted. */
public final class ProceduralFighterMesh {
    public static final float PART_SKIN=0f, PART_CANOPY=1f, PART_NOZZLE=2f, PART_INTAKE=3f,
            PART_STAB_L=4f, PART_STAB_R=5f, PART_RUDDER_L=6f, PART_RUDDER_R=7f,
            PART_AFTERBURNER=8f, PART_FLAPERON_L=9f, PART_FLAPERON_R=10f,
            PART_CANOPY_FRAME=11f, PART_NOZZLE_INNER=12f, PART_GEAR_STRUT=13f,
            PART_GEAR_WHEEL=14f, PART_GEAR_DOOR=15f, PART_COCKPIT_TUB=16f,
            PART_SEAT=17f, PART_COAMING=18f, PART_INTAKE_DUCT=19f,
            PART_COMPRESSOR_FACE=20f, PART_NOZZLE_PETAL=21f, PART_FLAME_CORE=22f,
            PART_UPPER_PANEL=23f;

    public static final class Mesh {
        public final float[] data;
        public Mesh(float[] data){this.data=data;}
        public int vertexCount(){return data.length/7;}
    }

    private final List<Float> out=new ArrayList<>();
    private float part=PART_SKIN;

    public static Mesh build(){
        AirframeShapeProfile.validate();
        ProceduralFighterMesh b=new ProceduralFighterMesh();
        b.part=PART_SKIN;
        b.fuselage(); b.chine(-1f); b.chine(1f); b.noseCrown(); b.upperDeck(); b.canopySill(); b.cockpitRearDeck();
        b.part=PART_UPPER_PANEL;
        b.noseCanopyBlend(); b.upperSpinePanel(); b.centerlineDorsalRidge(); b.canopyShoulderPanels();
        b.wingRootUpperFairings(); b.engineDeckPanels(); b.engineDeckBulges(); b.twinEngineCenterValley();
        b.nozzleShoulderShrouds(); b.tailRootFairing(); b.verticalTailRootShoulders();
        b.part=PART_SKIN; b.wing(-1f); b.wing(1f); b.aftShoulderBridge(); b.boatTail();
        b.part=PART_FLAPERON_L; b.flaperon(-1f); b.part=PART_FLAPERON_R; b.flaperon(1f);
        b.part=PART_STAB_L; b.stabilator(-1f); b.part=PART_STAB_R; b.stabilator(1f);
        b.part=PART_RUDDER_L; b.verticalTail(-1f); b.part=PART_RUDDER_R; b.verticalTail(1f);
        b.part=PART_INTAKE; b.intake(-1f); b.intake(1f); b.intakeLip(-1f); b.intakeLip(1f);
        b.part=PART_INTAKE_DUCT; b.intakeDuct(-1f); b.intakeDuct(1f);
        b.part=PART_COMPRESSOR_FACE; b.compressorFace(-1f); b.compressorFace(1f);
        b.part=PART_SKIN; b.enginePod(-.70f); b.enginePod(.70f);
        b.part=PART_NOZZLE; b.nozzle(-.70f); b.nozzle(.70f);
        b.part=PART_NOZZLE_PETAL; b.nozzlePetals(-.70f); b.nozzlePetals(.70f);
        b.part=PART_NOZZLE_INNER; b.nozzleInner(-.70f); b.nozzleInner(.70f);
        b.part=PART_AFTERBURNER; b.afterburner(-.70f); b.afterburner(.70f);
        b.part=PART_FLAME_CORE; b.flameCore(-.70f); b.flameCore(.70f);
        b.part=PART_COCKPIT_TUB; b.cockpitTub();
        b.part=PART_SEAT; b.ejectionSeat();
        b.part=PART_COAMING; b.instrumentCoaming();
        b.part=PART_CANOPY; b.canopy();
        b.part=PART_CANOPY_FRAME; b.canopyFrame();
        b.part=PART_GEAR_STRUT; b.gearStruts();
        b.part=PART_GEAR_WHEEL; b.gearWheels();
        b.part=PART_GEAR_DOOR; b.gearDoors();
        float[] data=new float[b.out.size()]; for(int i=0;i<data.length;i++) data[i]=b.out.get(i); return new Mesh(data);
    }

    private void fuselage(){float[] z=AirframeShapeProfile.Z,rx=AirframeShapeProfile.HALF_WIDTH,ry=AirframeShapeProfile.HALF_HEIGHT,cy=AirframeShapeProfile.CENTER_Y;int sides=28;for(int s=0;s<z.length-1;s++)for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;float sy0=sectionY((float)Math.sin(a0)),sy1=sectionY((float)Math.sin(a1));quad(new float[]{rx[s]*(float)Math.cos(a0),cy[s]+ry[s]*sy0,z[s]},new float[]{rx[s+1]*(float)Math.cos(a0),cy[s+1]+ry[s+1]*sy0,z[s+1]},new float[]{rx[s+1]*(float)Math.cos(a1),cy[s+1]+ry[s+1]*sy1,z[s+1]},new float[]{rx[s]*(float)Math.cos(a1),cy[s]+ry[s]*sy1,z[s]});}}
    private float sectionY(float s){float a=Math.abs(s);return Math.signum(s)*(float)Math.pow(a,.82);}
    private void chine(float side){float[][] src=AirframeShapeProfile.CHINE,top=new float[src.length][3];for(int i=0;i<src.length;i++)top[i]=new float[]{src[i][0]*side,src[i][1],src[i][2]};prism(top,.095f);}
    private void noseCrown(){prism(new float[][]{{-.035f,.04f,-6.12f},{.035f,.04f,-6.12f},{.17f,.18f,-5.18f},{.34f,.35f,-4.30f},{.50f,.49f,-3.48f},{.56f,.57f,-2.76f},{-.56f,.57f,-2.76f},{-.50f,.49f,-3.48f},{-.34f,.35f,-4.30f},{-.17f,.18f,-5.18f}},.075f);}
    private void upperDeck(){prism(new float[][]{{-.48f,.55f,-2.82f},{.48f,.55f,-2.82f},{.69f,.70f,-1.82f},{.79f,.84f,-.72f},{.82f,.88f,.34f},{.75f,.84f,1.30f},{.61f,.76f,2.18f},{.43f,.64f,2.78f},{-.43f,.64f,2.78f},{-.61f,.76f,2.18f},{-.75f,.84f,1.30f},{-.82f,.88f,.34f},{-.79f,.84f,-.72f},{-.69f,.70f,-1.82f}},.18f);}
    private void canopySill(){prism(new float[][]{{-.60f,.73f,-2.16f},{.60f,.73f,-2.16f},{.64f,.78f,-1.62f},{.61f,.82f,-.78f},{.53f,.84f,.06f},{.41f,.80f,.82f},{-.41f,.80f,.82f},{-.53f,.84f,.06f},{-.61f,.82f,-.78f},{-.64f,.78f,-1.62f}},.115f);prism(new float[][]{{-.50f,.60f,-2.82f},{.50f,.60f,-2.82f},{.59f,.72f,-2.16f},{-.59f,.72f,-2.16f}},.10f);}
    private void cockpitRearDeck(){prism(new float[][]{{-.41f,.80f,.68f},{.41f,.80f,.68f},{.49f,.83f,.98f},{.45f,.81f,1.30f},{.34f,.75f,1.58f},{-.34f,.75f,1.58f},{-.45f,.81f,1.30f},{-.49f,.83f,.98f}},.13f);}

    private void noseCanopyBlend(){
        prism(new float[][]{{-.46f,.615f,-2.86f},{-.20f,.66f,-2.74f},{-.28f,.75f,-2.36f},{-.43f,.80f,-2.08f},{-.59f,.735f,-2.16f},{-.56f,.64f,-2.62f}},.030f);
        prism(new float[][]{{.20f,.66f,-2.74f},{.46f,.615f,-2.86f},{.56f,.64f,-2.62f},{.59f,.735f,-2.16f},{.43f,.80f,-2.08f},{.28f,.75f,-2.36f}},.030f);
        prism(new float[][]{{-.18f,.675f,-2.76f},{.18f,.675f,-2.76f},{.27f,.755f,-2.34f},{.22f,.805f,-2.10f},{-.22f,.805f,-2.10f},{-.27f,.755f,-2.34f}},.020f);
    }
    private void upperSpinePanel(){
        prism(new float[][]{{-.31f,.865f,.72f},{.31f,.865f,.72f},{.36f,.86f,1.15f},{.34f,.82f,1.68f},{.28f,.75f,2.18f},{.20f,.66f,2.62f},{-.20f,.66f,2.62f},{-.28f,.75f,2.18f},{-.34f,.82f,1.68f},{-.36f,.86f,1.15f}},.035f);
        prism(new float[][]{{-.10f,.895f,.84f},{.10f,.895f,.84f},{.12f,.88f,1.54f},{.10f,.80f,2.20f},{.06f,.70f,2.58f},{-.06f,.70f,2.58f},{-.10f,.80f,2.20f},{-.12f,.88f,1.54f}},.024f);
    }
    private void centerlineDorsalRidge(){
        prism(new float[][]{{-.055f,.925f,.76f},{.055f,.925f,.76f},{.060f,.925f,1.10f},{.058f,.905f,1.46f},{.052f,.865f,1.82f},{.046f,.815f,2.16f},{.038f,.755f,2.46f},{.028f,.705f,2.68f},{-.028f,.705f,2.68f},{-.038f,.755f,2.46f},{-.046f,.815f,2.16f},{-.052f,.865f,1.82f},{-.058f,.905f,1.46f},{-.060f,.925f,1.10f}},.020f);
        prism(new float[][]{{-.045f,.914f,.98f},{.045f,.914f,.98f},{.050f,.900f,1.34f},{.046f,.855f,1.72f},{.038f,.800f,2.08f},{.028f,.742f,2.42f},{-.028f,.742f,2.42f},{-.038f,.800f,2.08f},{-.046f,.855f,1.72f},{-.050f,.900f,1.34f}},.012f);
    }
    private void canopyShoulderPanels(){
        prism(new float[][]{{-.63f,.805f,-1.92f},{-.50f,.885f,-1.78f},{-.48f,.90f,-.40f},{-.42f,.865f,.48f},{-.54f,.815f,.70f},{-.61f,.79f,-.62f}},.030f);
        prism(new float[][]{{.50f,.885f,-1.78f},{.63f,.805f,-1.92f},{.61f,.79f,-.62f},{.54f,.815f,.70f},{.42f,.865f,.48f},{.48f,.90f,-.40f}},.030f);
    }
    private void wingRootUpperFairings(){wingRootUpperFairing(-1f);wingRootUpperFairing(1f);}
    private void wingRootUpperFairing(float side){
        prism(new float[][]{{.72f*side,.42f,-2.62f},{1.18f*side,.40f,-2.52f},{1.62f*side,.36f,-2.18f},{2.16f*side,.31f,-1.72f},{2.60f*side,.27f,-1.28f},{2.18f*side,.30f,-.78f},{1.62f*side,.36f,-.52f},{1.12f*side,.48f,-.64f},{.82f*side,.58f,-1.28f}},.055f);
        prism(new float[][]{{.86f*side,.54f,-1.34f},{1.22f*side,.47f,-1.08f},{1.66f*side,.39f,-.62f},{1.92f*side,.33f,-.18f},{1.56f*side,.38f,.32f},{1.16f*side,.49f,.48f},{.88f*side,.62f,.16f}},.038f);
    }
    private void engineDeckPanels(){
        prism(new float[][]{{-1.22f,.46f,.72f},{-.48f,.68f,.90f},{-.45f,.65f,2.18f},{-.60f,.54f,2.84f},{-1.00f,.39f,3.08f},{-1.18f,.44f,1.72f}},.045f);
        prism(new float[][]{{.48f,.68f,.90f},{1.22f,.46f,.72f},{1.18f,.44f,1.72f},{1.00f,.39f,3.08f},{.60f,.54f,2.84f},{.45f,.65f,2.18f}},.045f);
        prism(new float[][]{{-.98f,.50f,1.05f},{-.48f,.69f,1.10f},{-.44f,.64f,2.02f},{-.57f,.54f,2.64f},{-.89f,.43f,2.78f}},.022f);
        prism(new float[][]{{.48f,.69f,1.10f},{.98f,.50f,1.05f},{.89f,.43f,2.78f},{.57f,.54f,2.64f},{.44f,.64f,2.02f}},.022f);
    }
    private void engineDeckBulges(){engineDeckBulge(-1f);engineDeckBulge(1f);}
    private void engineDeckBulge(float side){
        float[] z={.78f,1.24f,1.78f,2.30f,2.74f,3.08f};
        float[] inner={.43f,.42f,.43f,.47f,.55f,.66f};
        float[] outer={1.18f,1.20f,1.18f,1.13f,1.03f,.91f};
        float[] yi={.70f,.71f,.69f,.64f,.56f,.44f};
        float[] yo={.47f,.48f,.47f,.44f,.40f,.34f};
        for(int i=0;i<z.length-1;i++){
            float[] a={inner[i]*side,yi[i],z[i]}, b={outer[i]*side,yo[i],z[i]}, c={outer[i+1]*side,yo[i+1],z[i+1]}, d={inner[i+1]*side,yi[i+1],z[i+1]};
            if(side<0f) quad(b,a,d,c); else quad(a,b,c,d);
            float[] ai=offset(a,0,-.035f,0),bi=offset(b,0,-.035f,0),ci=offset(c,0,-.035f,0),di=offset(d,0,-.035f,0);
            if(side<0f) quad(ai,bi,ci,di); else quad(bi,ai,di,ci);
            quad(a,ai,di,d);quad(b,c,ci,bi);
        }
    }
    private void twinEngineCenterValley(){
        float[] z=AirframeShapeProfile.ENGINE_VALLEY_Z,y=AirframeShapeProfile.ENGINE_VALLEY_Y;
        float[] half={.38f,.40f,.42f,.44f,.46f,.44f,.36f};
        for(int i=0;i<z.length-1;i++){
            float[] a={-half[i],y[i],z[i]},b={half[i],y[i],z[i]},c={half[i+1],y[i+1],z[i+1]},d={-half[i+1],y[i+1],z[i+1]};
            quad(a,b,c,d);
            float[] ai=offset(a,0,-.026f,0),bi=offset(b,0,-.026f,0),ci=offset(c,0,-.026f,0),di=offset(d,0,-.026f,0);
            quad(di,ci,bi,ai);quad(a,ai,bi,b);quad(d,c,ci,di);
        }
    }
    private void nozzleShoulderShrouds(){nozzleShoulderShroud(-1f);nozzleShoulderShroud(1f);}
    private void nozzleShoulderShroud(float side){
        float[] z=AirframeShapeProfile.NOZZLE_SHROUD_Z,w=AirframeShapeProfile.NOZZLE_SHROUD_HALF_WIDTH,y=AirframeShapeProfile.NOZZLE_SHROUD_Y;
        for(int i=0;i<z.length-1;i++){
            float cx=.70f*side;
            float[] a={cx-w[i]*side,y[i],z[i]},b={cx+w[i]*side,y[i]-.08f,z[i]},c={cx+w[i+1]*side,y[i+1]-.08f,z[i+1]},d={cx-w[i+1]*side,y[i+1],z[i+1]};
            if(side<0f) quad(b,a,d,c); else quad(a,b,c,d);
            float[] ai=offset(a,0,-.045f,0),bi=offset(b,0,-.045f,0),ci=offset(c,0,-.045f,0),di=offset(d,0,-.045f,0);
            if(side<0f) quad(ai,bi,ci,di); else quad(bi,ai,di,ci);
            quad(a,ai,di,d);quad(b,c,ci,bi);
        }
    }
    private void tailRootFairing(){
        prism(new float[][]{{-.72f,.55f,1.48f},{-.57f,.70f,1.58f},{-.68f,.70f,2.50f},{-.91f,.53f,2.98f},{-1.03f,.46f,2.30f}},.050f);
        prism(new float[][]{{.57f,.70f,1.58f},{.72f,.55f,1.48f},{1.03f,.46f,2.30f},{.91f,.53f,2.98f},{.68f,.70f,2.50f}},.050f);
    }
    private void verticalTailRootShoulders(){verticalTailRootShoulder(-1f);verticalTailRootShoulder(1f);}
    private void verticalTailRootShoulder(float side){
        prism(new float[][]{{.52f*side,.61f,1.42f},{.76f*side,.67f,1.54f},{1.02f*side,.58f,1.92f},{1.10f*side,.48f,2.54f},{.92f*side,.47f,3.04f},{.70f*side,.61f,2.62f},{.58f*side,.69f,2.02f}},.075f);
    }

    private void wing(float side){float[][] src=AirframeShapeProfile.WING_ROOT,top=new float[src.length][3];for(int i=0;i<src.length;i++)top[i]=new float[]{src[i][0]*side,src[i][1],src[i][2]};prism(top,.30f);}
    private void aftShoulderBridge(){prism(new float[][]{{-1.30f,.38f,.72f},{1.30f,.38f,.72f},{1.25f,.43f,1.62f},{1.16f,.39f,2.48f},{.98f,.28f,3.12f},{-.98f,.28f,3.12f},{-1.16f,.39f,2.48f},{-1.25f,.43f,1.62f}},.20f);}
    private void boatTail(){prism(new float[][]{{-1.12f,.24f,2.66f},{1.12f,.24f,2.66f},{1.04f,.18f,3.05f},{.94f,.10f,3.38f},{.38f,.02f,3.48f},{-.38f,.02f,3.48f},{-.94f,.10f,3.38f},{-1.04f,.18f,3.05f}},.24f);prism(new float[][]{{-.20f,.11f,2.72f},{.20f,.11f,2.72f},{.28f,.06f,3.40f},{-.28f,.06f,3.40f}},.19f);}

    private void flaperon(float side){float y=.16f,t=.07f;prism(new float[][]{{1.62f*side,y+t,.70f},{3.56f*side,y+t,.78f},{3.34f*side,y+t,1.34f},{1.46f*side,y+t,1.48f}},t*2f);}
    private void stabilator(float side){float y=.18f,t=.10f;prism(new float[][]{{.72f*side,y+t,1.72f},{2.78f*side,y+t,2.24f},{2.22f*side,y+t,3.28f},{.72f*side,y+t,2.88f}},t*2f);}
    private void verticalTail(float side){float[] a={.62f*side,.56f,1.56f},b={1.22f*side,2.38f,2.16f},c={.96f*side,.50f,3.12f};tri(a,b,c);tri(offset(a,0,-.12f,0),offset(c,0,-.12f,0),offset(b,0,-.12f,0));}
    private void intake(float side){float[][] src=AirframeShapeProfile.INTAKE_SHOULDER,top=new float[src.length][3];for(int i=0;i<src.length;i++)top[i]=new float[]{src[i][0]*side,src[i][1],src[i][2]};prism(top,.34f);}
    private void intakeLip(float side){
        float x0=1.03f*side,x1=1.47f*side;
        prism(new float[][]{{x0,.35f,-2.90f},{x1,.31f,-2.54f},{1.60f*side,.13f,-2.32f},{1.18f*side,.10f,-2.58f}},.11f);
        prism(new float[][]{{1.17f*side,.09f,-2.59f},{1.60f*side,.12f,-2.32f},{1.52f*side,-.15f,-2.18f},{1.15f*side,-.18f,-2.42f}},.065f);
        prism(new float[][]{{1.03f*side,.34f,-2.88f},{1.16f*side,.10f,-2.58f},{1.15f*side,-.18f,-2.42f},{1.01f*side,-.05f,-2.64f}},.045f);
    }
    private void intakeDuct(float side){
        float[] a={1.34f*side,.10f,-2.42f}, b={1.58f*side,.08f,-2.42f}, c={1.58f*side,-.18f,-2.42f}, d={1.34f*side,-.20f,-2.42f};
        float[] e={1.09f*side,.14f,-1.82f}, f={1.34f*side,.12f,-1.82f}, g={1.34f*side,-.18f,-1.82f}, h={1.09f*side,-.21f,-1.82f};
        float[] i={.82f*side,.09f,-1.18f}, j={1.10f*side,.08f,-1.18f}, k={1.10f*side,-.20f,-1.18f}, l={.82f*side,-.22f,-1.18f};
        ductSegment(a,b,c,d,e,f,g,h); ductSegment(e,f,g,h,i,j,k,l);
    }
    private void ductSegment(float[] a,float[] b,float[] c,float[] d,float[] e,float[] f,float[] g,float[] h){quad(a,e,f,b);quad(d,c,g,h);quad(a,d,h,e);quad(b,f,g,c);}
    private void compressorFace(float side){
        float cx=.96f*side,cy=-.06f,z=-1.12f,r=.255f,hub=.065f;int n=18;
        for(int i=0;i<n;i++){
            double a0=2*Math.PI*i/n,a1=2*Math.PI*(i+1)/n;
            float[] h0={cx+hub*(float)Math.cos(a0),cy+hub*.62f*(float)Math.sin(a0),z-.012f};
            float[] h1={cx+hub*(float)Math.cos(a1),cy+hub*.62f*(float)Math.sin(a1),z-.012f};
            float[] p0={cx+r*(float)Math.cos(a0+.10),cy+r*.62f*(float)Math.sin(a0+.10),z};
            float[] p1={cx+r*(float)Math.cos(a1-.08),cy+r*.62f*(float)Math.sin(a1-.08),z};
            quad(h0,p0,p1,h1);
        }
        float[] center={cx,cy,z-.018f};
        for(int i=0;i<n;i++){double a0=2*Math.PI*i/n,a1=2*Math.PI*(i+1)/n;tri(center,new float[]{cx+hub*(float)Math.cos(a0),cy+hub*.62f*(float)Math.sin(a0),z-.018f},new float[]{cx+hub*(float)Math.cos(a1),cy+hub*.62f*(float)Math.sin(a1),z-.018f});}
    }

    private void enginePod(float x){float[] z=AirframeShapeProfile.ENGINE_Z,r=AirframeShapeProfile.ENGINE_R;int sides=22;for(int s=0;s<z.length-1;s++)for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r[s]*(float)Math.cos(a0),-.11f+r[s]*.58f*(float)Math.sin(a0),z[s]},new float[]{x+r[s+1]*(float)Math.cos(a0),-.11f+r[s+1]*.58f*(float)Math.sin(a0),z[s+1]},new float[]{x+r[s+1]*(float)Math.cos(a1),-.11f+r[s+1]*.58f*(float)Math.sin(a1),z[s+1]},new float[]{x+r[s]*(float)Math.cos(a1),-.11f+r[s]*.58f*(float)Math.sin(a1),z[s]});}}
    private void nozzle(float x){float z0=3.06f,z1=3.61f,r0=.47f,r1=.355f;int sides=24;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*.60f*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0});}}
    private void nozzlePetals(float x){
        float z0=3.30f,z1=3.72f,r0=.415f,r1=.332f,depth=.045f;int petals=12;
        for(int i=0;i<petals;i++){
            double a0=2*Math.PI*i/petals+.020,a1=2*Math.PI*(i+1)/petals-.020;
            float[] a={x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0};
            float[] b={x+r1*(float)Math.cos(a0),-.10f+r1*.60f*(float)Math.sin(a0),z1};
            float[] c={x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1};
            float[] d={x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0};
            float ir0=r0-depth,ir1=r1-depth;
            float[] ai={x+ir0*(float)Math.cos(a0),-.10f+ir0*.60f*(float)Math.sin(a0),z0+.018f};
            float[] bi={x+ir1*(float)Math.cos(a0),-.10f+ir1*.60f*(float)Math.sin(a0),z1-.018f};
            float[] ci={x+ir1*(float)Math.cos(a1),-.10f+ir1*.60f*(float)Math.sin(a1),z1-.018f};
            float[] di={x+ir0*(float)Math.cos(a1),-.10f+ir0*.60f*(float)Math.sin(a1),z0+.018f};
            quad(a,b,c,d); quad(di,ci,bi,ai); quad(a,ai,bi,b); quad(d,c,ci,di); quad(b,bi,ci,c);
        }
    }
    private void nozzleInner(float x){float z0=3.50f,z1=3.88f,r0=.275f,r1=.205f;int sides=24;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*.60f*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0});}}
    private void afterburner(float x){float z0=3.84f,z1=5.18f,r0=.205f,r1=.040f;int sides=18;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.55f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.55f*(float)Math.sin(a1),z0});}}
    private void flameCore(float x){float z0=3.88f,z1=4.72f,r0=.105f,r1=.018f;int sides=14;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.52f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.52f*(float)Math.sin(a1),z0});}}

    private void cockpitTub(){
        prism(new float[][]{{-.43f,.75f,-1.78f},{.43f,.75f,-1.78f},{.46f,.77f,-.36f},{.37f,.76f,.50f},{-.37f,.76f,.50f},{-.46f,.77f,-.36f}},.22f);
        prism(new float[][]{{-.41f,.82f,-1.58f},{-.30f,.84f,-1.46f},{-.28f,.86f,.34f},{-.37f,.83f,.42f}},.05f);
        prism(new float[][]{{.30f,.84f,-1.46f},{.41f,.82f,-1.58f},{.37f,.83f,.42f},{.28f,.86f,.34f}},.05f);
    }
    private void ejectionSeat(){
        box(0f,.77f,.05f,.32f,.31f,.45f);
        prism(new float[][]{{-.17f,.91f,.02f},{.17f,.91f,.02f},{.15f,1.11f,.34f},{-.15f,1.11f,.34f}},.09f);
        box(0f,1.08f,.37f,.26f,.14f,.14f);
    }
    private void instrumentCoaming(){
        prism(new float[][]{{-.37f,.87f,-1.78f},{.37f,.87f,-1.78f},{.31f,.94f,-1.38f},{-.31f,.94f,-1.38f}},.075f);
        box(0f,.89f,-1.32f,.28f,.10f,.10f);
    }

    private void canopy(){float[] z={-2.16f,-1.72f,-1.10f,-.40f,.28f,.82f};float[] rx={.39f,.50f,.56f,.54f,.43f,.24f};float[] base={.78f,.81f,.83f,.84f,.82f,.79f};float[] crown={.94f,1.10f,1.22f,1.25f,1.13f,.94f};int arcs=14;for(int s=0;s<z.length-1;s++)for(int i=0;i<arcs;i++){double a0=Math.PI*i/arcs,a1=Math.PI*(i+1)/arcs;float y00=base[s]+(crown[s]-base[s])*(float)Math.sin(a0),y01=base[s]+(crown[s]-base[s])*(float)Math.sin(a1),y10=base[s+1]+(crown[s+1]-base[s+1])*(float)Math.sin(a0),y11=base[s+1]+(crown[s+1]-base[s+1])*(float)Math.sin(a1);quad(new float[]{rx[s]*(float)Math.cos(a0),y00,z[s]},new float[]{rx[s+1]*(float)Math.cos(a0),y10,z[s+1]},new float[]{rx[s+1]*(float)Math.cos(a1),y11,z[s+1]},new float[]{rx[s]*(float)Math.cos(a1),y01,z[s]});}}
    private void canopyFrame(){
        prism(new float[][]{{-.425f,.79f,-2.16f},{.425f,.79f,-2.16f},{.385f,.88f,-1.96f},{-.385f,.88f,-1.96f}},.047f);
        prism(new float[][]{{-.505f,.83f,-1.10f},{.505f,.83f,-1.10f},{.45f,.97f,-.92f},{-.45f,.97f,-.92f}},.043f);
        prism(new float[][]{{-.40f,.81f,.28f},{.40f,.81f,.28f},{.33f,.93f,.49f},{-.33f,.93f,.49f}},.043f);
        prism(new float[][]{{-.53f,.78f,-2.04f},{-.45f,.81f,.58f},{-.37f,.78f,.81f},{-.46f,.76f,-2.02f}},.038f);
        prism(new float[][]{{.46f,.76f,-2.02f},{.37f,.78f,.81f},{.45f,.81f,.58f},{.53f,.78f,-2.04f}},.038f);
        prism(new float[][]{{-.023f,.99f,-1.96f},{.023f,.99f,-1.96f},{.020f,1.14f,.62f},{-.020f,1.14f,.62f}},.016f);
    }

    private void gearStruts(){box(-.075f,-1.52f,-3.72f,.15f,1.10f,.16f);box(-1.72f,-1.48f,.62f,.18f,1.18f,.18f);box(1.54f,-1.48f,.62f,.18f,1.18f,.18f);box(-1.70f,-.98f,.55f,.16f,.18f,.84f);box(1.54f,-.98f,.55f,.16f,.18f,.84f);}
    private void gearWheels(){wheel(0f,-1.66f,-3.72f,.26f,.18f);wheel(-1.72f,-1.72f,1.12f,.38f,.24f);wheel(1.72f,-1.72f,1.12f,.38f,.24f);}
    private void gearDoors(){prism(new float[][]{{-.30f,-.53f,-4.18f},{-.03f,-.53f,-4.18f},{-.03f,-.53f,-3.23f},{-.30f,-.53f,-3.23f}},.045f);prism(new float[][]{{.03f,-.53f,-4.18f},{.30f,-.53f,-4.18f},{.30f,-.53f,-3.23f},{.03f,-.53f,-3.23f}},.045f);prism(new float[][]{{-1.82f,-.55f,.22f},{-.88f,-.55f,.22f},{-.88f,-.55f,1.56f},{-1.82f,-.55f,1.56f}},.055f);prism(new float[][]{{.88f,-.55f,.22f},{1.82f,-.55f,.22f},{1.82f,-.55f,1.56f},{.88f,-.55f,1.56f}},.055f);}
    private void box(float x,float y,float z,float sx,float sy,float sz){float hx=sx*.5f,hz=sz*.5f;prism(new float[][]{{x-hx,y+sy,z-hz},{x+hx,y+sy,z-hz},{x+hx,y+sy,z+hz},{x-hx,y+sy,z+hz}},sy);}
    private void wheel(float x,float y,float z,float r,float width){int n=16;float x0=x-width*.5f,x1=x+width*.5f;for(int i=0;i<n;i++){double a0=2*Math.PI*i/n,a1=2*Math.PI*(i+1)/n;float[] a={x0,y+r*(float)Math.cos(a0),z+r*(float)Math.sin(a0)},b={x1,y+r*(float)Math.cos(a0),z+r*(float)Math.sin(a0)},c={x1,y+r*(float)Math.cos(a1),z+r*(float)Math.sin(a1)},d={x0,y+r*(float)Math.cos(a1),z+r*(float)Math.sin(a1)};quad(a,b,c,d);}}
    private void prism(float[][] top,float thickness){int n=top.length;float[][] bot=new float[n][3];for(int i=0;i<n;i++)bot[i]=offset(top[i],0,-thickness,0);for(int i=1;i<n-1;i++){tri(top[0],top[i],top[i+1]);tri(bot[0],bot[i+1],bot[i]);}for(int i=0;i<n;i++)quad(top[i],bot[i],bot[(i+1)%n],top[(i+1)%n]);}
    private static float[] offset(float[] p,float x,float y,float z){return new float[]{p[0]+x,p[1]+y,p[2]+z};}
    private void quad(float[] a,float[] b,float[] c,float[] d){tri(a,b,c);tri(a,c,d);}
    private void tri(float[] a,float[] b,float[] c){float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2],nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,l=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(l<1e-6f)l=1f;emit(a,nx/l,ny/l,nz/l);emit(b,nx/l,ny/l,nz/l);emit(c,nx/l,ny/l,nz/l);}
    private void emit(float[] p,float nx,float ny,float nz){out.add(p[0]);out.add(p[1]);out.add(p[2]);out.add(nx);out.add(ny);out.add(nz);out.add(part);}
}
