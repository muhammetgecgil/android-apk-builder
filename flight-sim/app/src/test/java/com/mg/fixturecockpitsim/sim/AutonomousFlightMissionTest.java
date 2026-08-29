package com.mg.fixturecockpitsim.sim;

import static org.junit.Assert.*;
import org.junit.Test;

public final class AutonomousFlightMissionTest {
    @Test public void completesFullLoopAndReturnsToHangarWithoutStoppingDemo() {
        FlightState s=new FlightState();FlightControls c=new FlightControls();FlightDynamicsEngine dynamics=new FlightDynamicsEngine();AutonomousFlightMission mission=new AutonomousFlightMission();mission.reset(s);
        boolean sawTaxiOut=false,becameAirborne=false,gearRetracted=false,returnedToGround=false,sawTaxiIn=false,sawHangarPark=false,looped=false;double maxAltitude=0;final double dt=.02;final int maxSteps=(int)(620.0/dt);
        assertEquals("hangar start must face runway",0.0,s.headingDeg,.001);
        AutonomousFlightMission.Phase prev=mission.getPhase();
        for(int i=0;i<maxSteps&&!looped;i++){
            mission.update(s,c,dt);dynamics.step(s,c,dt);AutonomousFlightMission.Phase ph=mission.getPhase();
            if(ph==AutonomousFlightMission.Phase.TAXI_OUT){sawTaxiOut=true;assertTrue("taxi-out aligned to runway",Math.abs(s.headingDeg)<8||Math.abs(s.headingDeg-360)<8);}
            if(!s.onGround&&s.altitudeM>5)becameAirborne=true;if(s.gearPosition<.2)gearRetracted=true;if(becameAirborne&&s.onGround)returnedToGround=true;if(ph==AutonomousFlightMission.Phase.TAXI_IN)sawTaxiIn=true;if(ph==AutonomousFlightMission.Phase.HANGAR_PARK)sawHangarPark=true;if(prev==AutonomousFlightMission.Phase.HANGAR_PARK&&ph==AutonomousFlightMission.Phase.HANGAR_START)looped=true;prev=ph;maxAltitude=Math.max(maxAltitude,s.altitudeM);
        }
        assertTrue(sawTaxiOut);assertTrue(becameAirborne);assertTrue(gearRetracted);assertTrue(maxAltitude>800);assertTrue(returnedToGround);assertTrue(sawTaxiIn);assertTrue(sawHangarPark);assertTrue("demo must loop back to hangar instead of ending",looped);assertEquals(AutonomousFlightMission.Phase.HANGAR_START,mission.getPhase());assertTrue(s.onGround);assertTrue(s.gearPosition>.82);
    }

    @Test public void groundPhaseProgressIsNormalized(){FlightState s=new FlightState();FlightControls c=new FlightControls();AutonomousFlightMission m=new AutonomousFlightMission();m.reset(s);assertEquals(0.0,m.getPhaseProgress01(),1e-9);m.update(s,c,2);assertEquals(.5,m.getPhaseProgress01(),.03);m.update(s,c,2.05);assertEquals(AutonomousFlightMission.Phase.TAXI_OUT,m.getPhase());assertTrue(m.getPhaseProgress01()>=0&&m.getPhaseProgress01()<=1);}
}
