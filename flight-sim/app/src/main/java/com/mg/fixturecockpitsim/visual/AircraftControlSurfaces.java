package com.mg.fixturecockpitsim.visual;

/** Maps normalized pilot commands to visual aircraft surface deflections. */
public final class AircraftControlSurfaces {
    public float leftStabilatorDeg;
    public float rightStabilatorDeg;
    public float leftRudderDeg;
    public float rightRudderDeg;
    public float leftFlaperonDeg;
    public float rightFlaperonDeg;
    public float nozzle01;

    public void update(float pitch, float roll, float yaw, float throttle) {
        pitch = clamp(pitch); roll = clamp(roll); yaw = clamp(yaw);
        leftStabilatorDeg  = clampDeg(-pitch * 22f + roll * 7f, 25f);
        rightStabilatorDeg = clampDeg(-pitch * 22f - roll * 7f, 25f);
        leftFlaperonDeg = clampDeg(-pitch * 8f + roll * 18f, 24f);
        rightFlaperonDeg = clampDeg(-pitch * 8f - roll * 18f, 24f);
        leftRudderDeg = clampDeg(yaw * 24f, 24f);
        rightRudderDeg = clampDeg(yaw * 24f, 24f);
        nozzle01 = Math.max(0f, Math.min(1f, throttle));
    }

    private static float clamp(float v){ return Math.max(-1f, Math.min(1f, v)); }
    private static float clampDeg(float v,float max){ return Math.max(-max, Math.min(max, v)); }
}
