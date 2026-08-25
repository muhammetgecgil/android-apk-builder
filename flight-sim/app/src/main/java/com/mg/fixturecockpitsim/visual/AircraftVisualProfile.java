package com.mg.fixturecockpitsim.visual;

/**
 * Visual/physical scale contract for the v19 fighter model.
 * Keeps the renderer tied to real-world proportions instead of arbitrary GL units.
 */
public final class AircraftVisualProfile {
    private AircraftVisualProfile() {}

    public static final float LENGTH_M = 18.90f;
    public static final float WINGSPAN_M = 13.56f;
    public static final float HEIGHT_M = 5.08f;
    public static final float WING_AREA_M2 = 78.04f;
    public static final float HORIZONTAL_TAIL_SPAN_M = 8.84f;

    /** Render scale: one world unit equals one meter. */
    public static final float WORLD_SCALE = 1.0f;

    /** Camera presets tuned for a full-size 18.9 m fighter. */
    public static final float CHASE_DISTANCE_M = 31.0f;
    public static final float CLOSE_CHASE_DISTANCE_M = 22.0f;
    public static final float ORBIT_DISTANCE_M = 27.0f;
    public static final float CINEMATIC_DISTANCE_M = 35.0f;

    /** Avoid wide-angle toy-like distortion in exterior views. */
    public static final float EXTERIOR_FOV_DEG = 36.0f;
    public static final float COCKPIT_FOV_DEG = 58.0f;

    /** PBR targets for the RAM-coated grey airframe. */
    public static final float AIRFRAME_METALLIC = 0.08f;
    public static final float AIRFRAME_ROUGHNESS = 0.58f;
    public static final float CANOPY_METALLIC = 0.30f;
    public static final float CANOPY_ROUGHNESS = 0.12f;

    /** LOD switching distances in meters. */
    public static final float LOD0_MAX_M = 45.0f;
    public static final float LOD1_MAX_M = 120.0f;
    public static final float LOD2_MAX_M = 350.0f;
}
