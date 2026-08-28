package com.mg.fixturecockpitsim.visual;

/**
 * AVM-7.3 final upper-airframe continuity profile for the generic modern fighter.
 * The foredeck, canopy shoulders, wing roots, twin-engine deck and exhaust shoulder
 * are constrained as one aerodynamic volume rather than stacked procedural parts.
 */
public final class AirframeShapeProfile {
    private AirframeShapeProfile() {}

    public static final float[] Z = {-6.28f,-5.86f,-5.30f,-4.66f,-3.92f,-3.10f,-2.18f,-1.18f,-0.18f,.82f,1.76f,2.56f,3.18f,3.62f};
    public static final float[] HALF_WIDTH = {.020f,.070f,.18f,.34f,.53f,.74f,.95f,1.08f,1.17f,1.19f,1.16f,1.06f,.88f,.57f};
    public static final float[] HALF_HEIGHT = {.018f,.055f,.13f,.23f,.35f,.49f,.65f,.75f,.81f,.83f,.79f,.70f,.56f,.35f};
    public static final float[] CENTER_Y = {-.030f,-.028f,-.024f,-.016f,-.002f,.028f,.074f,.112f,.134f,.139f,.120f,.082f,.030f,-.025f};

    public static final float[][] CHINE = {
            {.08f,.10f,-5.86f},{.27f,.17f,-5.02f},{.55f,.23f,-4.20f},{.90f,.30f,-3.40f},
            {1.25f,.35f,-2.62f},{1.57f,.38f,-1.78f},{1.82f,.36f,-.92f},{1.92f,.34f,-.10f},
            {1.78f,.33f,.54f},{1.46f,.36f,1.06f}
    };
    public static final float[][] INTAKE_SHOULDER = {
            {1.06f,.39f,-2.96f},{1.39f,.42f,-2.54f},{1.62f,.41f,-1.96f},{1.76f,.38f,-1.24f},
            {1.76f,.36f,-.48f},{1.60f,.36f,.20f},{1.30f,.38f,.82f}
    };
    public static final float[][] WING_ROOT = {
            {.78f,.23f,-2.66f},{1.54f,.24f,-2.18f},{2.26f,.24f,-1.68f},{3.28f,.23f,-1.02f},
            {5.10f,.21f,.02f},{4.48f,.21f,.92f},{3.48f,.22f,1.54f},{2.14f,.23f,1.88f},{1.04f,.25f,2.10f}
    };

    /* Engine pod overlaps the nozzle shoulder longitudinally so the exhaust does not read as pasted on. */
    public static final float[] ENGINE_Z = {-.78f,-.42f,-.02f,.44f,.98f,1.56f,2.12f,2.56f,2.88f,3.10f,3.34f};
    public static final float[] ENGINE_R = {.24f,.32f,.43f,.53f,.61f,.66f,.67f,.65f,.60f,.53f,.46f};

    public static final float[] UPPER_BLEND_Z = {-2.82f,-2.38f,-1.92f,-1.30f,-.62f,.10f,.76f,1.36f,1.94f,2.46f,2.88f,3.18f};
    public static final float[] UPPER_BLEND_HALF_WIDTH = {.48f,.58f,.68f,.79f,.88f,.94f,.91f,.86f,.79f,.72f,.65f,.58f};
    public static final float[] UPPER_BLEND_Y = {.60f,.70f,.80f,.87f,.91f,.93f,.90f,.84f,.76f,.66f,.55f,.43f};

    /* Seven stations deliberately match the procedural valley skin width guide. */
    public static final float[] ENGINE_VALLEY_Y = {.82f,.79f,.72f,.62f,.50f,.38f,.27f};
    public static final float[] ENGINE_VALLEY_Z = {.82f,1.28f,1.78f,2.28f,2.72f,3.10f,3.42f};

    public static final float[] NOZZLE_SHROUD_Z = {2.74f,2.94f,3.12f,3.30f,3.46f};
    public static final float[] NOZZLE_SHROUD_HALF_WIDTH = {1.12f,1.08f,1.02f,.94f,.86f};
    public static final float[] NOZZLE_SHROUD_Y = {.47f,.43f,.38f,.31f,.24f};

    public static float upperCrown(int station){return CENTER_Y[station]+HALF_HEIGHT[station];}
    public static float lowerBelly(int station){return CENTER_Y[station]-HALF_HEIGHT[station];}
    public static float engineShoulderTop(int station){return -.11f+ENGINE_R[station]*.58f;}
    public static float foredeckRise(int from,int to){return (upperCrown(to)-upperCrown(from))/(Z[to]-Z[from]);}

    public static void validate(){
        if(Z.length!=HALF_WIDTH.length||Z.length!=HALF_HEIGHT.length||Z.length!=CENTER_Y.length) throw new IllegalStateException("Airframe station arrays must have equal length");
        if(ENGINE_Z.length!=ENGINE_R.length) throw new IllegalStateException("Engine profile arrays must have equal length");
        if(UPPER_BLEND_Z.length!=UPPER_BLEND_HALF_WIDTH.length||UPPER_BLEND_Z.length!=UPPER_BLEND_Y.length) throw new IllegalStateException("Upper blend guides must align");
        if(ENGINE_VALLEY_Y.length!=ENGINE_VALLEY_Z.length) throw new IllegalStateException("Engine valley guides must align");
        if(NOZZLE_SHROUD_Z.length!=NOZZLE_SHROUD_HALF_WIDTH.length||NOZZLE_SHROUD_Z.length!=NOZZLE_SHROUD_Y.length) throw new IllegalStateException("Nozzle shroud guides must align");
        for(int i=1;i<Z.length;i++) if(Z[i]<=Z[i-1]) throw new IllegalStateException("Airframe stations must increase aftward");
        for(int i=1;i<ENGINE_Z.length;i++) if(ENGINE_Z[i]<=ENGINE_Z[i-1]) throw new IllegalStateException("Engine stations must increase aftward");
        for(int i=1;i<UPPER_BLEND_Z.length;i++) if(UPPER_BLEND_Z[i]<=UPPER_BLEND_Z[i-1]) throw new IllegalStateException("Upper blend stations must increase aftward");
        for(int i=1;i<ENGINE_VALLEY_Z.length;i++) if(ENGINE_VALLEY_Z[i]<=ENGINE_VALLEY_Z[i-1]) throw new IllegalStateException("Engine valley must increase aftward");
        for(int i=1;i<NOZZLE_SHROUD_Z.length;i++) if(NOZZLE_SHROUD_Z[i]<=NOZZLE_SHROUD_Z[i-1]) throw new IllegalStateException("Nozzle shroud must increase aftward");
        if(WING_ROOT[4][0]<5f) throw new IllegalStateException("Wing span below AVM target");
        if(HALF_WIDTH[8]<1.05f||HALF_WIDTH[9]<1.05f) throw new IllegalStateException("Centre body too narrow");
        if(upperCrown(7)<.75f||upperCrown(8)<.82f||upperCrown(9)<.84f) throw new IllegalStateException("Upper cockpit/deck crown below target");
        if(upperCrown(10)<.78f||upperCrown(11)<.66f) throw new IllegalStateException("Dorsal spine collapses too quickly aft");
        if(lowerBelly(8)<-.70f||lowerBelly(9)<-.70f) throw new IllegalStateException("Upper-body maturity must not balloon the belly");
        if(upperCrown(5)>=upperCrown(6)||upperCrown(6)>=upperCrown(7)) throw new IllegalStateException("Forebody-to-canopy shoulder is not progressive");
        if(foredeckRise(4,5)<=0f||foredeckRise(5,6)<=0f||foredeckRise(6,7)<=0f) throw new IllegalStateException("Foredeck crown must rise continuously toward canopy");
        if(ENGINE_Z.length<11||ENGINE_R[0]>.26f||ENGINE_R[1]>=ENGINE_R[2]) throw new IllegalStateException("Engine emergence is too abrupt");
        if(ENGINE_R[5]<.65f||ENGINE_R[6]<.66f||engineShoulderTop(6)<.27f) throw new IllegalStateException("Engine deck lacks integrated shoulder volume");
        for(int i=7;i<ENGINE_R.length;i++) if(ENGINE_R[i]>=ENGINE_R[i-1]) throw new IllegalStateException("Aft engine deck must taper smoothly into nozzle shoulder");
        if(upperCrown(9)-upperCrown(11)>.25f) throw new IllegalStateException("Upper deck drops too abruptly behind cockpit");
        if(UPPER_BLEND_Y[0]<.58f||UPPER_BLEND_Y[5]<.90f||UPPER_BLEND_Y[UPPER_BLEND_Y.length-1]>.46f) throw new IllegalStateException("Upper surface blend is not mature");
        for(int i=1;i<ENGINE_VALLEY_Y.length;i++) if(ENGINE_VALLEY_Y[i]>=ENGINE_VALLEY_Y[i-1]) throw new IllegalStateException("Engine centre valley must taper aftward");
        for(int i=1;i<NOZZLE_SHROUD_HALF_WIDTH.length;i++) if(NOZZLE_SHROUD_HALF_WIDTH[i]>=NOZZLE_SHROUD_HALF_WIDTH[i-1]) throw new IllegalStateException("Nozzle shroud width must taper aftward");
        for(int i=1;i<NOZZLE_SHROUD_Y.length;i++) if(NOZZLE_SHROUD_Y[i]>=NOZZLE_SHROUD_Y[i-1]) throw new IllegalStateException("Nozzle shroud crown must taper aftward");
        if(ENGINE_VALLEY_Z[ENGINE_VALLEY_Z.length-1]<3.4f||ENGINE_VALLEY_Y[ENGINE_VALLEY_Y.length-1]>.30f) throw new IllegalStateException("Engine valley must blend into boat-tail");
        if(ENGINE_Z[ENGINE_Z.length-2]<3.05f||ENGINE_R[ENGINE_R.length-2]<.50f) throw new IllegalStateException("Engine pod does not overlap nozzle shoulder enough");
    }
}
