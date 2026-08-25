package com.mg.fixturecockpitsim.sim;

/** Normalized pilot commands. Axes are -1..+1, throttle is 0..1. */
public final class FlightControls {
    public double pitch;
    public double roll;
    public double yaw;
    public double throttle = 0.65;

    public void clamp() {
        pitch=clampAxis(pitch); roll=clampAxis(roll); yaw=clampAxis(yaw);
        throttle=Math.max(0.0,Math.min(1.0,throttle));
    }
    private static double clampAxis(double v){ return Math.max(-1.0,Math.min(1.0,v)); }
}
