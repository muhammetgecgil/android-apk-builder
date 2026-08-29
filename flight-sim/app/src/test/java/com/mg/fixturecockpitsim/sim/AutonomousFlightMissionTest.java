package com.mg.fixturecockpitsim.sim;

import static org.junit.Assert.*;
import org.junit.Test;

public final class AutonomousFlightMissionTest {
    @Test public void completesHangarTaxiFiveMinuteFlightLandingAndStop() {
        FlightState s=new FlightState();FlightControls c=new FlightControls();FlightDynamicsEngine dynamics=new FlightDynamicsEngine();AutonomousFlightMission mission=new AutonomousFlightMission();mission.reset(s);
        boolean sawTaxi=false,becameAirborne=false,gearRetracted=false,returnedToGround=false;double maxAltitude=0;final double dt=.02;final int maxSteps=(int)(520.0/dt);
        for(int i=0;i<maxSteps&&mission.getPhase()!=AutonomousFlightMission.Phase.COMPLETE;i++){
            mission.update(s,c,dt);dynamics.step(s,c,dt);
            if(mission.getPhase()==AutonomousFlightMission.Phase.TAXI_OUT)sawTaxi=true;
            if(!s.onGround&&s.altitudeM>5)becameAirborne=true;if(s.gearPosition<.2)gearRetracted=true;if(becameAirborne&&s.onGround)returnedToGround=true;maxAltitude=Math.max(maxAltitude,s.altitudeM);
        }
        assertTrue("aircraft must taxi out from hangar",sawTaxi);assertTrue("aircraft must leave runway",becameAirborne);assertTrue("landing gear must retract in flight",gearRetracted);assertTrue("aircraft must climb near cruise altitude",maxAltitude>800);assertTrue("scenic flight must last five minutes",mission.getOrbitTimeSec()>=299.9);assertTrue("aircraft must touch runway again",returnedToGround);assertEquals("mission must finish rollout",AutonomousFlightMission.Phase.COMPLETE,mission.getPhase());assertTrue("aircraft must be stopped on runway",s.onGround&&s.trueAirspeedMps<2);assertTrue("gear must be down after landing",s.gearPosition>.82);
    }
}
