package com.mg.fixturecockpitsim.visual;

/**
 * AVM-1 unified stealth airframe section profile.
 *
 * The procedural renderer is moving away from independent ad-hoc prisms. These
 * stations describe a single longitudinal shape language for nose, canopy
 * shoulder, intake shoulder, wing root and aft engine-body transitions.
 */
public final class AirframeShapeProfile {
    private AirframeShapeProfile() {}

    /** Longitudinal stations, nose to exhaust shoulder. */
    public static final float[] Z = {
            -6.20f, -5.72f, -5.10f, -4.42f, -3.58f, -2.62f,
            -1.58f, -0.52f, 0.58f, 1.62f, 2.52f, 3.22f, 3.70f
    };

    /** Half-width of the central pressure/body volume. */
    public static final float[] HALF_WIDTH = {
            0.025f, 0.095f, 0.23f, 0.42f, 0.62f, 0.82f,
            0.99f, 1.10f, 1.14f, 1.08f, 0.91f, 0.60f, 0.30f
    };

    /** Half-height of the central body section. */
    public static final float[] HALF_HEIGHT = {
            0.020f, 0.075f, 0.17f, 0.29f, 0.40f, 0.51f,
            0.61f, 0.68f, 0.72f, 0.69f, 0.58f, 0.40f, 0.20f
    };

    /** Vertical centre shift used to flatten belly and raise cockpit shoulder. */
    public static final float[] CENTER_Y = {
            -0.025f, -0.025f, -0.020f, -0.010f, 0.005f, 0.025f,
            0.050f, 0.070f, 0.075f, 0.050f, 0.015f, -0.035f, -0.080f
    };

    /** Outboard chine target at each major forward-body station. */
    public static final float[][] CHINE = {
            {0.11f, 0.13f, -5.62f},
            {0.44f, 0.21f, -4.58f},
            {0.83f, 0.27f, -3.62f},
            {1.18f, 0.31f, -2.72f},
            {1.52f, 0.30f, -1.78f},
            {1.78f, 0.25f, -0.82f},
            {1.86f, 0.22f,  0.06f},
            {1.56f, 0.25f,  0.66f},
            {1.16f, 0.30f,  1.04f}
    };

    /** Intake shoulder outer line, forward to aft. */
    public static final float[][] INTAKE_SHOULDER = {
            {1.10f, 0.33f, -2.82f},
            {1.48f, 0.32f, -2.30f},
            {1.68f, 0.29f, -1.60f},
            {1.70f, 0.25f, -0.72f},
            {1.52f, 0.24f,  0.10f},
            {1.18f, 0.28f,  0.68f}
    };

    /** Wing-root planform control points; x is unsigned and mirrored per side. */
    public static final float[][] WING_ROOT = {
            {0.72f, 0.19f, -2.58f},
            {1.55f, 0.20f, -2.08f},
            {2.20f, 0.20f, -1.58f},
            {4.96f, 0.20f, -0.06f},
            {4.30f, 0.20f,  0.92f},
            {3.28f, 0.20f,  1.55f},
            {1.00f, 0.20f,  2.00f}
    };

    /** Engine shoulder radius profile. */
    public static final float[] ENGINE_Z = {-0.28f, 0.48f, 1.28f, 2.12f, 2.86f, 3.24f};
    public static final float[] ENGINE_R = { 0.38f, 0.50f, 0.58f, 0.60f, 0.52f, 0.44f};

    public static void validate() {
        if (Z.length != HALF_WIDTH.length || Z.length != HALF_HEIGHT.length || Z.length != CENTER_Y.length) {
            throw new IllegalStateException("Airframe station arrays must have equal length");
        }
        if (ENGINE_Z.length != ENGINE_R.length) {
            throw new IllegalStateException("Engine profile arrays must have equal length");
        }
        for (int i=1;i<Z.length;i++) {
            if (Z[i] <= Z[i-1]) throw new IllegalStateException("Airframe stations must increase aftward");
        }
    }
}
