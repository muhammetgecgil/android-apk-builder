package com.mg.fixturecockpitsim.sim;

/** Mutable simulation state in SI units where practical. */
public final class FlightState {
    public double timeSec;
    public double latitudeDeg = 40.0;
    public double longitudeDeg = 29.0;
    public double altitudeM = 1500.0;
    public double trueAirspeedMps = 120.0;
    public double indicatedAirspeedMps = 120.0;
    public double verticalSpeedMps;
    public double headingDeg;
    public double pitchDeg;
    public double rollDeg;
    public double throttle = 0.65;
    public double angleOfAttackDeg;
    public double loadFactor = 1.0;

    // V20 atmosphere / flight-envelope telemetry.
    public double airDensityKgM3 = 1.225;
    public double dynamicPressurePa;
    public double mach;
    public double stallMargin01 = 1.0;
    public boolean stallWarning;
    public boolean overspeedWarning;
    public boolean gearWarning;
    public double windNorthMps;
    public double windEastMps;
    public double turbulence01;

    // Ground / landing system state.
    public double gearPosition = 1.0; // 0=retracted, 1=extended
    public boolean onGround;
    public double mainStrutCompression01;
    public double noseStrutCompression01;
    public double brake01;
    public double touchdownSinkMps;

    public FlightState copy() {
        FlightState c = new FlightState();
        c.timeSec=timeSec; c.latitudeDeg=latitudeDeg; c.longitudeDeg=longitudeDeg;
        c.altitudeM=altitudeM; c.trueAirspeedMps=trueAirspeedMps; c.indicatedAirspeedMps=indicatedAirspeedMps; c.verticalSpeedMps=verticalSpeedMps;
        c.headingDeg=headingDeg; c.pitchDeg=pitchDeg; c.rollDeg=rollDeg; c.throttle=throttle;
        c.angleOfAttackDeg=angleOfAttackDeg; c.loadFactor=loadFactor;
        c.airDensityKgM3=airDensityKgM3; c.dynamicPressurePa=dynamicPressurePa; c.mach=mach; c.stallMargin01=stallMargin01;
        c.stallWarning=stallWarning; c.overspeedWarning=overspeedWarning; c.gearWarning=gearWarning;
        c.windNorthMps=windNorthMps; c.windEastMps=windEastMps; c.turbulence01=turbulence01;
        c.gearPosition=gearPosition; c.onGround=onGround; c.mainStrutCompression01=mainStrutCompression01;
        c.noseStrutCompression01=noseStrutCompression01; c.brake01=brake01; c.touchdownSinkMps=touchdownSinkMps;
        return c;
    }
}
