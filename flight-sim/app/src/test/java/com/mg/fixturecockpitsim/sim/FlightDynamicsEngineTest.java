package com.mg.fixturecockpitsim.sim;

import static org.junit.Assert.*;
import org.junit.Test;

public final class FlightDynamicsEngineTest {
    @Test public void atmosphereProducesIasMachAndDynamicPressure(){
        FlightState s=new FlightState();s.altitudeM=9000;s.trueAirspeedMps=200;s.onGround=false;FlightControls c=new FlightControls();c.throttle=.58;c.gearDown=false;
        new FlightDynamicsEngine().step(s,c,.02);
        assertTrue(s.airDensityKgM3<1.0);assertTrue(s.airDensityKgM3>0.25);assertTrue(s.indicatedAirspeedMps<s.trueAirspeedMps);assertTrue(s.mach>.4);assertTrue(s.dynamicPressurePa>1000);
    }

    @Test public void lowEnergyHighAlphaFlightRaisesStallWarningAndSink(){
        FlightState s=new FlightState();s.altitudeM=1200;s.trueAirspeedMps=48;s.indicatedAirspeedMps=48;s.onGround=false;s.gearPosition=0;FlightControls c=new FlightControls();c.throttle=.1;c.pitch=1;c.gearDown=false;FlightDynamicsEngine d=new FlightDynamicsEngine();
        for(int i=0;i<80;i++)d.step(s,c,.02);
        assertTrue(s.stallWarning);assertTrue(s.stallMargin01<.4);assertTrue(s.verticalSpeedMps<0);assertTrue(Math.abs(s.angleOfAttackDeg)>15);
    }

    @Test public void landingConfigurationAndOverspeedWarningsAreAvailable(){
        FlightDynamicsEngine d=new FlightDynamicsEngine();FlightControls c=new FlightControls();
        FlightState gear=new FlightState();gear.altitudeM=120;gear.trueAirspeedMps=95;gear.verticalSpeedMps=-5;gear.onGround=false;gear.gearPosition=0;c.gearDown=false;d.step(gear,c,.02);assertTrue(gear.gearWarning);
        FlightState fast=new FlightState();fast.altitudeM=150;fast.trueAirspeedMps=320;fast.onGround=false;fast.gearPosition=0;c.throttle=1;c.gearDown=false;d.step(fast,c,.02);assertTrue(fast.overspeedWarning);
    }

    @Test public void fullManualRollStillReachesInvertedRegion(){
        FlightState s=new FlightState();s.altitudeM=1500;s.onGround=false;FlightControls c=new FlightControls();c.roll=1;c.throttle=.75;c.gearDown=false;FlightDynamicsEngine d=new FlightDynamicsEngine();
        for(int i=0;i<100;i++)d.step(s,c,.02);
        assertTrue("full stick should retain aerobatic authority",s.rollDeg>150);
    }
}
