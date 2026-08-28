package com.mg.fixturecockpitsim.visual;

/**
 * Unified modern-fighter airframe section profile.
 *
 * AVM-6 concentrates on continuity: the forebody now grows into the canopy
 * shoulders without a sudden shelf, while the twin-engine volumes emerge from
 * the centre body over more longitudinal stations.  The result remains a
 * generic modern-fighter shape rather than a copy of a specific aircraft.
 */
public final class AirframeShapeProfile {
    private AirframeShapeProfile() {}

    /** Longitudinal stations, nose to exhaust shoulder. */
    public static final float[] Z = {
            -6.28f, -5.86f, -5.30f, -4.66f, -3.92f, -3.10f,
            -2.18f, -1.18f, -0.18f, 0.82f, 1.76f, 2.56f, 3.18f, 3.62f
    };

    /** Half-width of the central pressure/body volume. */
    public static final float[] HALF_WIDTH = {
            0.020f, 0.070f, 0.18f, 0.34f, 0.52f, 0.72f,
            0.92f, 1.06f, 1.15f, 1.17f, 1.14f, 1.03f, 0.82f, 0.52f
    };

    /**
     * Half-height of the central body. AVM-6 raises the shoulder progressively
     * before the canopy and keeps useful volume over the engine bay.
     */
    public static final float[] HALF_HEIGHT = {
            0.018f, 0.055f, 0.13f, 0.23f, 0.34f, 0.47f,
            0.62f, 0.72f, 0.78f, 0.80f, 0.76f, 0.66f, 0.51f, 0.32f
    };

    /**
     * Vertical centre shift. The crown rises smoothly into the cockpit shoulder
     * while the lower-body datum stays controlled.
     */
    public static final float[] CENTER_Y = {
            -0.030f, -0.028f, -0.024f, -0.016f, -0.004f, 0.022f,
            0.064f, 0.100f, 0.120f, 0.125f, 0.105f, 0.064f, 0.012f, -0.040f
    };

    /** Outboard chine target at each major forward-body station. */
    public static final float[][] CHINE = {
            {0.08f, 0.10f, -5.86f},
            {0.27f, 0.17f, -5.02f},
            {0.55f, 0.23f, -4.20f},
            {0.90f, 0.30f, -3.40f},
            {1.25f, 0.34f, -2.62f},
            {1.57f, 0.36f, -1.78f},
            {1.82f, 0.34f, -0.92f},
            {1.92f, 0.32f, -0.10f},
            {1.78f, 0.31f,  0.54f},
            {1.46f, 0.34f,  1.06f}
    };

    /** Intake shoulder outer line, forward to aft. */
    public static final float[][] INTAKE_SHOULDER = {
            {1.06f, 0.38f, -2.96f},
            {1.39f, 0.40f, -2.54f},
            {1.62f, 0.39f, -1.96f},
            {1.76f, 0.36f, -1.24f},
            {1.76f, 0.34f, -0.48f},
            {1.60f, 0.34f,  0.20f},
            {1.30f, 0.36f,  0.82f}
    };

    /** Wing planform control points; x is unsigned and mirrored per side. */
    public static final float[][] WING_ROOT = {
            {0.78f, 0.22f, -2.66f},
            {1.54f, 0.23f, -2.18f},
            {2.26f, 0.23f, -1.68f},
            {3.28f, 0.22f, -1.02f},
            {5.10f, 0.20f,  0.02f},
            {4.48f, 0.20f,  0.92f},
            {3.48f, 0.21f,  1.54f},
            {2.14f, 0.22f,  1.88f},
            {1.04f, 0.24f,  2.10f}
    };

    /**
     * Twin-engine shoulder radius profile. AVM-6 adds two intermediate stages
     * so the pods grow out of the centre body instead of reading as cylinders
     * attached underneath it.
     */
    public static final float[] ENGINE_Z = {
            -0.70f, -0.26f, 0.22f, 0.82f, 1.48f, 2.10f, 2.64f, 3.02f, 3.34f
    };
    public static final float[] ENGINE_R = {
             0.28f,  0.39f, 0.50f, 0.59f, 0.65f, 0.66f, 0.63f, 0.55f, 0.45f
    };

    /** Expected upper crown, used as a regression target for visual maturity. */
    public static float upperCrown(int station) {
        return CENTER_Y[station] + HALF_HEIGHT[station];
    }

    /** Expected lower belly, used to ensure upper-body work does not balloon downward. */
    public static float lowerBelly(int station) {
        return CENTER_Y[station] - HALF_HEIGHT[station];
    }

    /** Approximate top of an engine shoulder using the renderer's pod ellipse. */
    public static float engineShoulderTop(int station) {
        return -0.11f + ENGINE_R[station] * 0.58f;
    }

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
        for (int i=1;i<ENGINE_Z.length;i++) {
            if (ENGINE_Z[i] <= ENGINE_Z[i-1]) throw new IllegalStateException("Engine stations must increase aftward");
        }
        if (WING_ROOT[4][0] < 5.0f) throw new IllegalStateException("Wing span below AVM-4 target");
        if (HALF_WIDTH[8] < 1.05f || HALF_WIDTH[9] < 1.05f) throw new IllegalStateException("Centre body too narrow");
        if (ENGINE_R[4] < 0.60f || ENGINE_R[5] < 0.60f) throw new IllegalStateException("Engine shoulders too weak");

        // AVM-5 upper-body acceptance: strong cockpit crown and a continuous aft spine.
        if (upperCrown(7) < 0.75f || upperCrown(8) < 0.82f || upperCrown(9) < 0.84f) {
            throw new IllegalStateException("Upper cockpit/deck crown below AVM-5 target");
        }
        if (upperCrown(10) < 0.78f || upperCrown(11) < 0.66f) {
            throw new IllegalStateException("Dorsal spine collapses too quickly aft");
        }
        if (lowerBelly(8) < -0.70f || lowerBelly(9) < -0.70f) {
            throw new IllegalStateException("Upper-body maturity must not balloon the belly");
        }

        // AVM-6 continuity acceptance: canopy shoulder must rise progressively.
        if (upperCrown(5) >= upperCrown(6) || upperCrown(6) >= upperCrown(7)) {
            throw new IllegalStateException("Forebody-to-canopy shoulder is not progressive");
        }
        if (upperCrown(6) < 0.67f || upperCrown(7) < 0.80f) {
            throw new IllegalStateException("Canopy shoulder lacks supporting fuselage volume");
        }
        if (HALF_WIDTH[6] < 0.90f || HALF_WIDTH[7] < 1.04f) {
            throw new IllegalStateException("Canopy shoulder body width below AVM-6 target");
        }
        if (ENGINE_Z.length < 9 || ENGINE_R[0] > 0.32f || ENGINE_R[1] >= ENGINE_R[2]) {
            throw new IllegalStateException("Engine emergence is too abrupt");
        }
        if (ENGINE_R[4] < 0.64f || ENGINE_R[5] < 0.65f || engineShoulderTop(5) < 0.26f) {
            throw new IllegalStateException("Engine deck lacks integrated shoulder volume");
        }
    }
}
