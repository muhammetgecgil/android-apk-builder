package com.mg.fixturecockpitsim.visual;

/**
 * Unified modern-fighter airframe section profile.
 *
 * AVM-6.1 concentrates on the upper body as one continuous aerodynamic volume:
 * the foredeck rises into the canopy shoulders without a shelf, the dorsal crown
 * stays full over the centre body, and the twin-engine shoulders merge into the
 * aft deck before tapering toward the exhausts. The shape remains generic.
 */
public final class AirframeShapeProfile {
    private AirframeShapeProfile() {}

    public static final float[] Z = {
            -6.28f, -5.86f, -5.30f, -4.66f, -3.92f, -3.10f,
            -2.18f, -1.18f, -0.18f, 0.82f, 1.76f, 2.56f, 3.18f, 3.62f
    };

    public static final float[] HALF_WIDTH = {
            0.020f, 0.070f, 0.18f, 0.34f, 0.53f, 0.74f,
            0.95f, 1.08f, 1.17f, 1.19f, 1.16f, 1.05f, 0.84f, 0.53f
    };

    public static final float[] HALF_HEIGHT = {
            0.018f, 0.055f, 0.13f, 0.23f, 0.35f, 0.49f,
            0.65f, 0.75f, 0.81f, 0.83f, 0.79f, 0.69f, 0.53f, 0.33f
    };

    public static final float[] CENTER_Y = {
            -0.030f, -0.028f, -0.024f, -0.016f, -0.002f, 0.028f,
            0.074f, 0.112f, 0.134f, 0.139f, 0.120f, 0.079f, 0.024f, -0.034f
    };

    public static final float[][] CHINE = {
            {0.08f, 0.10f, -5.86f},
            {0.27f, 0.17f, -5.02f},
            {0.55f, 0.23f, -4.20f},
            {0.90f, 0.30f, -3.40f},
            {1.25f, 0.35f, -2.62f},
            {1.57f, 0.38f, -1.78f},
            {1.82f, 0.36f, -0.92f},
            {1.92f, 0.34f, -0.10f},
            {1.78f, 0.33f,  0.54f},
            {1.46f, 0.36f,  1.06f}
    };

    public static final float[][] INTAKE_SHOULDER = {
            {1.06f, 0.39f, -2.96f},
            {1.39f, 0.42f, -2.54f},
            {1.62f, 0.41f, -1.96f},
            {1.76f, 0.38f, -1.24f},
            {1.76f, 0.36f, -0.48f},
            {1.60f, 0.36f,  0.20f},
            {1.30f, 0.38f,  0.82f}
    };

    public static final float[][] WING_ROOT = {
            {0.78f, 0.23f, -2.66f},
            {1.54f, 0.24f, -2.18f},
            {2.26f, 0.24f, -1.68f},
            {3.28f, 0.23f, -1.02f},
            {5.10f, 0.21f,  0.02f},
            {4.48f, 0.21f,  0.92f},
            {3.48f, 0.22f,  1.54f},
            {2.14f, 0.23f,  1.88f},
            {1.04f, 0.25f,  2.10f}
    };

    public static final float[] ENGINE_Z = {
            -0.78f, -0.42f, -0.02f, 0.44f, 0.98f, 1.56f, 2.12f, 2.62f, 3.02f, 3.34f
    };
    public static final float[] ENGINE_R = {
             0.24f,  0.32f,  0.43f, 0.53f, 0.61f, 0.66f, 0.67f, 0.63f, 0.55f, 0.45f
    };

    public static float upperCrown(int station) {
        return CENTER_Y[station] + HALF_HEIGHT[station];
    }

    public static float lowerBelly(int station) {
        return CENTER_Y[station] - HALF_HEIGHT[station];
    }

    public static float engineShoulderTop(int station) {
        return -0.11f + ENGINE_R[station] * 0.58f;
    }

    public static float foredeckRise(int from, int to) {
        return (upperCrown(to)-upperCrown(from)) / (Z[to]-Z[from]);
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

        if (upperCrown(7) < 0.75f || upperCrown(8) < 0.82f || upperCrown(9) < 0.84f) {
            throw new IllegalStateException("Upper cockpit/deck crown below AVM-5 target");
        }
        if (upperCrown(10) < 0.78f || upperCrown(11) < 0.66f) {
            throw new IllegalStateException("Dorsal spine collapses too quickly aft");
        }
        if (lowerBelly(8) < -0.70f || lowerBelly(9) < -0.70f) {
            throw new IllegalStateException("Upper-body maturity must not balloon the belly");
        }

        if (upperCrown(5) >= upperCrown(6) || upperCrown(6) >= upperCrown(7)) {
            throw new IllegalStateException("Forebody-to-canopy shoulder is not progressive");
        }
        if (upperCrown(6) < 0.70f || upperCrown(7) < 0.84f) {
            throw new IllegalStateException("Canopy shoulder lacks supporting fuselage volume");
        }
        if (HALF_WIDTH[6] < 0.93f || HALF_WIDTH[7] < 1.06f) {
            throw new IllegalStateException("Canopy shoulder body width below AVM-6.1 target");
        }
        if (foredeckRise(4,5) <= 0f || foredeckRise(5,6) <= 0f || foredeckRise(6,7) <= 0f) {
            throw new IllegalStateException("Foredeck crown must rise continuously toward canopy");
        }

        if (ENGINE_Z.length < 10 || ENGINE_R[0] > 0.26f || ENGINE_R[1] >= ENGINE_R[2]) {
            throw new IllegalStateException("Engine emergence is too abrupt");
        }
        if (ENGINE_R[5] < 0.65f || ENGINE_R[6] < 0.66f || engineShoulderTop(6) < 0.27f) {
            throw new IllegalStateException("Engine deck lacks integrated shoulder volume");
        }
        if (!(ENGINE_R[6] > ENGINE_R[7] && ENGINE_R[7] > ENGINE_R[8] && ENGINE_R[8] > ENGINE_R[9])) {
            throw new IllegalStateException("Aft engine deck must taper smoothly into nozzle shoulder");
        }
        if (upperCrown(9)-upperCrown(11) > 0.25f) {
            throw new IllegalStateException("Upper deck drops too abruptly behind cockpit");
        }
    }
}
