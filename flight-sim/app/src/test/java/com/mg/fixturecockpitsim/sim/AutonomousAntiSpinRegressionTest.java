package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutonomousAntiSpinRegressionTest {
    @Test public void extremeAutorotationIsClampedBeforeNextPhysicsStep(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();
        FlightState s=new FlightState();s.onGround=false;s.rollDeg=168;s.rollRateDegSec=190;s.yawRateDegSec=120;s.spin01=.72;
        FlightControls c=new FlightControls();c.roll=.8;c.yaw=.8;c.pitch=.2;
        sm.apply(s,c,.05);
        assertTrue(Math.abs(s.rollDeg)<=55.01);
        assertTrue(Math.abs(s.rollRateDegSec)<=52.01);
        assertTrue(Math.abs(s.yawRateDegSec)<=28.01);
        assertTrue(c.roll*s.rollDeg<=0.001 || Math.abs(c.roll)<.03);
        assertTrue(Math.abs(c.yaw)<=.08);
    }

    @Test public void normalTurnIsNotFlattened(){
        AutonomousTurnSmoother sm=new AutonomousTurnSmoother();
        FlightState s=new FlightState();s.onGround=false;s.rollDeg=12;s.rollRateDegSec=4;s.yawRateDegSec=2;s.pitchDeg=3;
        FlightControls c=new FlightControls();c.roll=.22;c.yaw=.05;c.pitch=.08;
        for(int i=0;i<20;i++){c.roll=.22;c.yaw=.05;c.pitch=.08;sm.apply(s,c,.05);}
        assertTrue(sm.getRollOut()>0);
        assertTrue(Math.abs(s.rollDeg-12)<.001);
    }
}
