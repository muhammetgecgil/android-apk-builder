package com.mg.fixturecockpitsim.sim;

/** Normalized pilot commands. Axes/trim are -1..+1, throttle/brakes are 0..1. */
public final class FlightControls {
    public double pitch;
    public double roll;
    public double yaw;
    public double pitchTrim;
    public double throttle = 0.65;
    /** Wheel brake command. */
    public double brake;
    /** Dorsal fighter speed-brake command; intentionally separate from wheel braking. */
    public double speedBrake;
    public boolean gearDown = true;

    public void clamp() {
        pitch=clampAxis(pitch); roll=clampAxis(roll); yaw=clampAxis(yaw); pitchTrim=clampAxis(pitchTrim);
        throttle=clamp01(throttle); brake=clamp01(brake); speedBrake=clamp01(speedBrake);
    }
    private static double clampAxis(double v){ return Math.max(-1.0,Math.min(1.0,v)); }
    private static double clamp01(double v){ return Math.max(0.0,Math.min(1.0,v)); }
}
