package com.mg.fixturecockpitsim.visual;

/**
 * Maps normalized maneuver commands to visual fighter control-surface deflections.
 * Positive pitch is nose-up command, positive roll is right-roll command and
 * positive yaw is right-yaw command. Values are visual degrees around each
 * surface hinge and are rate-smoothed by the renderer.
 */
public final class AircraftControlSurfaces {
    public float leftStabilatorDeg;
    public float rightStabilatorDeg;
    public float leftRudderDeg;
    public float rightRudderDeg;
    public float leftFlaperonDeg;
    public float rightFlaperonDeg;
    public float flapDroopDeg;
    public float nozzle01;

    /** Backwards-compatible mapping with a nominal airborne condition. */
    public void update(float pitch, float roll, float yaw, float throttle) {
        update(pitch, roll, yaw, throttle, 0f, 150f, false);
    }

    /**
     * Fighter-like control mixing:
     * - stabilators move together for pitch and differentially for roll,
     * - flaperons move differentially for roll and droop at low speed/gear-down,
     * - twin rudders move together for yaw,
     * - extreme visual deflection is reduced progressively at high airspeed.
     */
    public void update(float pitch, float roll, float yaw, float throttle,
                       float gear01, float speedMps, boolean onGround) {
        pitch = clamp(pitch);
        roll = clamp(roll);
        yaw = clamp(yaw);
        throttle = clamp01(throttle);
        gear01 = clamp01(gear01);
        speedMps = Math.max(0f, speedMps);

        // High-speed command scheduling: surfaces still move, but not to low-speed extremes.
        float highSpeed = clamp01((speedMps - 165f) / 155f);
        float primaryAuthority = lerp(1f, 0.62f, highSpeed);
        float secondaryAuthority = lerp(1f, 0.70f, highSpeed);

        // All-moving horizontal tails: strong symmetric pitch + modest differential roll assist.
        float pitchMix = -pitch * 22.0f * primaryAuthority;
        float rollTailMix = roll * 7.5f * secondaryAuthority;
        leftStabilatorDeg = clampDeg(pitchMix + rollTailMix, 25f);
        rightStabilatorDeg = clampDeg(pitchMix - rollTailMix, 25f);

        // Low-speed / approach flaperon droop. Fade it out before high-speed flight.
        float lowSpeed = 1f - clamp01((speedMps - 85f) / 85f);
        float configured = Math.max(gear01, onGround ? 0.65f : 0f);
        flapDroopDeg = 11.5f * lowSpeed * configured;

        // Flaperons: differential roll, small symmetric pitch contribution and configuration droop.
        float flapPitchMix = -pitch * 3.5f * secondaryAuthority;
        float flapRollMix = roll * 18.0f * secondaryAuthority;
        leftFlaperonDeg = clampDeg(flapDroopDeg + flapPitchMix + flapRollMix, 25f);
        rightFlaperonDeg = clampDeg(flapDroopDeg + flapPitchMix - flapRollMix, 25f);

        // Twin rudders: same aerodynamic yaw command. Small roll-to-yaw coordination improves visuals.
        float rudderMix = (yaw * 23.0f + roll * 2.0f) * secondaryAuthority;
        leftRudderDeg = clampDeg(rudderMix, 24f);
        rightRudderDeg = clampDeg(rudderMix, 24f);

        nozzle01 = throttle;
    }

    private static float clamp(float v) { return Math.max(-1f, Math.min(1f, v)); }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float clampDeg(float v, float max) { return Math.max(-max, Math.min(max, v)); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * clamp01(t); }
}
