package com.mg.fixturecockpitsim.visual;

import java.util.ArrayList;
import java.util.List;

/** Temporary volumetric fighter mesh used until the production GLB is accepted. */
public final class ProceduralFighterMesh {
    public static final float PART_SKIN=0f, PART_CANOPY=1f, PART_NOZZLE=2f, PART_INTAKE=3f,
            PART_STAB_L=4f, PART_STAB_R=5f, PART_RUDDER_L=6f, PART_RUDDER_R=7f,
            PART_AFTERBURNER=8f, PART_FLAPERON_L=9f, PART_FLAPERON_R=10f,
            PART_CANOPY_FRAME=11f, PART_NOZZLE_INNER=12f, PART_GEAR_STRUT=13f,
            PART_GEAR_WHEEL=14f, PART_GEAR_DOOR=15f;

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
        b.wing(-1f); b.wing(1f); b.aftShoulderBridge(); b.boatTail();
        b.part=PART_FLAPERON_L; b.flaperon(-1f); b.part=PART_FLAPERON_R; b.flaperon(1f);
        b.part=PART_STAB_L; b.stabilator(-1f); b.part=PART_STAB_R; b.stabilator(1f);
        b.part=PART_RUDDER_L; b.verticalTail(-1f); b.part=PART_RUDDER_R; b.verticalTail(1f);
        b.part=PART_INTAKE; b.intake(-1f); b.intake(1f); b.intakeLip(-1f); b.intakeLip(1f);
        b.part=PART_SKIN; b.enginePod(-.70f); b.enginePod(.70f);
        b.part=PART_NOZZLE; b.nozzle(-.70f); b.nozzle(.70f);
        b.part=PART_NOZZLE_INNER; b.nozzleInner(-.70f); b.nozzleInner(.70f);
        b.part=PART_AFTERBURNER; b.afterburner(-.70f); b.afterburner(.70f);
        b.part=PART_CANOPY; b.canopy();
        b.part=PART_CANOPY_FRAME; b.canopyFrame();
        b.part=PART_GEAR_STRUT; b.gearStruts();
        b.part=PART_GEAR_WHEEL; b.gearWheels();
        b.part=PART_GEAR_DOOR; b.gearDoors();
        float[] data=new float[b.out.size()];
        for(int i=0;i<data.length;i++) data[i]=b.out.get(i);
        return new Mesh(data);
    }

    private void fuselage(){
        final float[] z=AirframeShapeProfile.Z;
        final float[] rx=AirframeShapeProfile.HALF_WIDTH;
        final float[] ry=AirframeShapeProfile.HALF_HEIGHT;
        final float[] cy=AirframeShapeProfile.CENTER_Y;
        final int sides=28;
        for(int s=0;s<z.length-1;s++) for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float sy0=sectionY((float)Math.sin(a0)), sy1=sectionY((float)Math.sin(a1));
            quad(new float[]{rx[s]*(float)Math.cos(a0),cy[s]+ry[s]*sy0,z[s]},
                 new float[]{rx[s+1]*(float)Math.cos(a0),cy[s+1]+ry[s+1]*sy0,z[s+1]},
                 new float[]{rx[s+1]*(float)Math.cos(a1),cy[s+1]+ry[s+1]*sy1,z[s+1]},
                 new float[]{rx[s]*(float)Math.cos(a1),cy[s]+ry[s]*sy1,z[s]});
        }
    }

    private float sectionY(float s){float a=Math.abs(s);float shaped=(float)Math.pow(a,.82);return Math.signum(s)*shaped;}

    private void chine(float side){
        float[][] src=AirframeShapeProfile.CHINE;
        float[][] top=new float[src.length][3];
        for(int i=0;i<src.length;i++) top[i]=new float[]{src[i][0]*side,src[i][1],src[i][2]};
        prism(top,.095f);
    }

    private void noseCrown(){
        prism(new float[][]{
                {-.035f,.04f,-6.12f},{.035f,.04f,-6.12f},{.17f,.18f,-5.18f},{.34f,.35f,-4.30f},
                {.50f,.49f,-3.48f},{.56f,.57f,-2.76f},{-.56f,.57f,-2.76f},{-.50f,.49f,-3.48f},
                {-.34f,.35f,-4.30f},{-.17f,.18f,-5.18f}},.075f);
    }

    private void upperDeck(){
        prism(new float[][]{
                {-.48f,.55f,-2.82f},{.48f,.55f,-2.82f},{.69f,.70f,-1.82f},{.79f,.84f,-.72f},
                {.82f,.88f,.34f},{.75f,.84f,1.30f},{.61f,.76f,2.18f},{.43f,.64f,2.78f},
                {-.43f,.64f,2.78f},{-.61f,.76f,2.18f},{-.75f,.84f,1.30f},{-.82f,.88f,.34f},
                {-.79f,.84f,-.72f},{-.69f,.70f,-1.82f}},.18f);
    }

    private void canopySill(){
        prism(new float[][]{
                {-.60f,.73f,-2.04f},{.60f,.73f,-2.04f},{.64f,.78f,-1.55f},{.61f,.82f,-.78f},
                {.53f,.84f,.02f},{.42f,.80f,.72f},{-.42f,.80f,.72f},{-.53f,.84f,.02f},
                {-.61f,.82f,-.78f},{-.64f,.78f,-1.55f}},.115f);
        prism(new float[][]{
                {-.50f,.60f,-2.82f},{.50f,.60f,-2.82f},{.59f,.72f,-2.04f},{-.59f,.72f,-2.04f}},.10f);
    }

    private void cockpitRearDeck(){
        // AVM-2 fixed rear crown: supports canopy trailing edge and prevents a hollow rear-cockpit look.
        prism(new float[][]{
                {-.43f,.80f,.55f},{.43f,.80f,.55f},{.50f,.83f,.86f},{.46f,.81f,1.20f},
                {.34f,.75f,1.53f},{-.34f,.75f,1.53f},{-.46f,.81f,1.20f},{-.50f,.83f,.86f}},.13f);
    }

    private void wing(float side){
        float[][] src=AirframeShapeProfile.WING_ROOT;
        float[][] top=new float[src.length][3];
        for(int i=0;i<src.length;i++) top[i]=new float[]{src[i][0]*side,src[i][1],src[i][2]};
        prism(top,.30f);
    }

    private void aftShoulderBridge(){
        prism(new float[][]{
                {-1.30f,.38f,.72f},{1.30f,.38f,.72f},{1.25f,.43f,1.62f},{1.16f,.39f,2.48f},
                {.98f,.28f,3.12f},{-.98f,.28f,3.12f},{-1.16f,.39f,2.48f},{-1.25f,.43f,1.62f}},.20f);
    }

    private void boatTail(){
        prism(new float[][]{
                {-1.12f,.24f,2.66f},{1.12f,.24f,2.66f},{1.04f,.18f,3.05f},{.94f,.10f,3.38f},
                {.38f,.02f,3.48f},{-.38f,.02f,3.48f},{-.94f,.10f,3.38f},{-1.04f,.18f,3.05f}},.24f);
        prism(new float[][]{
                {-.20f,.11f,2.72f},{.20f,.11f,2.72f},{.28f,.06f,3.40f},{-.28f,.06f,3.40f}},.19f);
    }

    private void flaperon(float side){float y=.16f,t=.07f;prism(new float[][]{{1.62f*side,y+t,.70f},{3.56f*side,y+t,.78f},{3.34f*side,y+t,1.34f},{1.46f*side,y+t,1.48f}},t*2f);}
    private void stabilator(float side){float y=.18f,t=.10f;prism(new float[][]{{.72f*side,y+t,1.72f},{2.78f*side,y+t,2.24f},{2.22f*side,y+t,3.28f},{.72f*side,y+t,2.88f}},t*2f);}
    private void verticalTail(float side){float[] a={.62f*side,.56f,1.56f},b={1.22f*side,2.38f,2.16f},c={.96f*side,.50f,3.12f};tri(a,b,c);tri(offset(a,0,-.12f,0),offset(c,0,-.12f,0),offset(b,0,-.12f,0));}

    private void intake(float side){
        float[][] src=AirframeShapeProfile.INTAKE_SHOULDER;
        float[][] top=new float[src.length][3];
        for(int i=0;i<src.length;i++) top[i]=new float[]{src[i][0]*side,src[i][1],src[i][2]};
        prism(top,.34f);
    }

    private void intakeLip(float side){
        float x0=1.03f*side,x1=1.47f*side;
        prism(new float[][]{
                {x0,.35f,-2.90f},{x1,.31f,-2.54f},{1.58f*side,.13f,-2.34f},{1.18f*side,.10f,-2.58f}},.095f);
        prism(new float[][]{
                {1.17f*side,.09f,-2.59f},{1.57f*side,.12f,-2.34f},{1.50f*side,-.13f,-2.20f},{1.16f*side,-.16f,-2.42f}},.055f);
    }

    private void enginePod(float x){
        float[] z=AirframeShapeProfile.ENGINE_Z;float[] r=AirframeShapeProfile.ENGINE_R;int sides=22;
        for(int s=0;s<z.length-1;s++)for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            quad(new float[]{x+r[s]*(float)Math.cos(a0),-.11f+r[s]*.58f*(float)Math.sin(a0),z[s]},
                 new float[]{x+r[s+1]*(float)Math.cos(a0),-.11f+r[s+1]*.58f*(float)Math.sin(a0),z[s+1]},
                 new float[]{x+r[s+1]*(float)Math.cos(a1),-.11f+r[s+1]*.58f*(float)Math.sin(a1),z[s+1]},
                 new float[]{x+r[s]*(float)Math.cos(a1),-.11f+r[s]*.58f*(float)Math.sin(a1),z[s]});
        }
    }

    private void nozzle(float x){float z0=3.12f,z1=3.64f,r0=.43f,r1=.34f;int sides=20;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;float ripple=(i%2==0?1.0f:.94f);quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0},new float[]{x+r1*ripple*(float)Math.cos(a0),-.10f+r1*ripple*.60f*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0});}}
    private void nozzleInner(float x){float z0=3.55f,z1=3.77f,r0=.255f,r1=.205f;int sides=18;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;quad(new float[]{x+r0*(float)Math.cos(a0),-.10f+r0*.60f*(float)Math.sin(a0),z0},new float[]{x+r1*(float)Math.cos(a0),-.10f+r1*.60f*(float)Math.sin(a0),z1},new float[]{x+r1*(float)Math.cos(a1),-.10f+r1*.60f*(float)Math.sin(a1),z1},new float[]{x+r0*(float)Math.cos(a1),-.10f+r0*.60f*(float)Math.sin(a1),z0});}}
    private void afterburner(float x){float z0=3.74f,z1=5.10f,r0=.20f,r1=.035f;int sides=16;for(int i=0;i<sides;i++){double a0=2*Math.PI*i/n,a1=2*Math.PI*(i+1)/n;}}

    private void canopy(){
        // AVM-2: four-section faceted bubble with separate windshield-to-main-canopy progression.
        float[] z={-2.02f,-1.60f,-.98f,-.28f,.36f,.68f};
        float[] rx={.43f,.53f,.57f,.53f,.42f,.27f};
        float[] base={.79f,.82f,.84f,.85f,.83f,.79f};
        float[] crown={1.02f,1.18f,1.30f,1.32f,1.18f,.98f};
        int arcs=14;
        for(int s=0;s<z.length-1;s++){
            for(int i=0;i<arcs;i++){
                double a0=Math.PI*i/arcs,a1=Math.PI*(i+1)/arcs;
                float y00=base[s]+(crown[s]-base[s])*(float)Math.sin(a0);
                float y01=base[s]+(crown[s]-base[s])*(float)Math.sin(a1);
                float y10=base[s+1]+(crown[s+1]-base[s+1])*(float)Math.sin(a0);
                float y11=base[s+1]+(crown[s+1]-base[s+1])*(float)Math.sin(a1);
                quad(new float[]{rx[s]*(float)Math.cos(a0),y00,z[s]},
                     new float[]{rx[s+1]*(float)Math.cos(a0),y10,z[s+1]},
                     new float[]{rx[s+1]*(float)Math.cos(a1),y11,z[s+1]},
                     new float[]{rx[s]*(float)Math.cos(a1),y01,z[s]});
            }
        }
    }

    private void canopyFrame(){
        // Forward windshield bow.
        prism(new float[][]{{-.47f,.80f,-2.04f},{.47f,.80f,-2.04f},{.42f,.91f,-1.88f},{-.42f,.91f,-1.88f}},.065f);
        // Mid bow defining windshield/main-canopy split.
        prism(new float[][]{{-.54f,.84f,-1.04f},{.54f,.84f,-1.04f},{.47f,1.00f,-.88f},{-.47f,1.00f,-.88f}},.060f);
        // Rear bow and canopy trailing frame.
        prism(new float[][]{{-.42f,.82f,.35f},{.42f,.82f,.35f},{.35f,.96f,.54f},{-.35f,.96f,.54f}},.060f);
        // Longitudinal lower rails, giving the glazing a structural base.
        prism(new float[][]{{-.57f,.79f,-1.92f},{-.48f,.82f,.46f},{-.39f,.79f,.70f},{-.48f,.77f,-1.90f}},.055f);
        prism(new float[][]{{.48f,.77f,-1.90f},{.39f,.79f,.70f},{.48f,.82f,.46f},{.57f,.79f,-1.92f}},.055f);
        // Narrow dorsal spine strip to make the bubble read as framed glazing in top/3-4 views.
        prism(new float[][]{{-.035f,1.03f,-1.88f},{.035f,1.03f,-1.88f},{.030f,1.19f,.48f},{-.030f,1.19f,.48f}},.025f);
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
