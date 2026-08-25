package com.mg.fixturecockpitsim.sim;

/** Mutable simulation state in SI units where practical. */
public final class FlightState {
    public double timeSec;
    public double latitudeDeg = 40.0;
    public double longitudeDeg = 29.0;
    public double altitudeM = 1500.0;
    public double trueAirspeedMps = 120.0;
    public double verticalSpeedMps;
    public double headingDeg;
    public double pitchDeg;
    public double rollDeg;
    public double throttle = 0.65;
    public double angleOfAttackDeg;
    public double loadFactor = 1.0;

    public FlightState copy() {
        FlightState c = new FlightState();
        c.timeSec=timeSec; c.latitudeDeg=latitudeDeg; c.longitudeDeg=longitudeDeg;
        c.altitudeM=altitudeM; c.trueAirspeedMps=trueAirspeedMps; c.verticalSpeedMps=verticalSpeedMps;
        c.headingDeg=headingDeg; c.pitchDeg=pitchDeg; c.rollDeg=rollDeg; c.throttle=throttle;
        c.angleOfAttackDeg=angleOfAttackDeg; c.loadFactor=loadFactor;
        return c;
    }
}
