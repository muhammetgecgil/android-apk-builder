package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class CinematicJourneyStateTest {
    @Test public void journeyAdvancesThroughMajorScenes(){
        CinematicJourneyState.reset();
        for(int i=0;i<18*20;i++)CinematicJourneyState.update(170,.05,false,"ORBIT");
        assertEquals(CinematicJourneyState.AEGEAN,CinematicJourneyState.getStage());
        for(int i=0;i<42*20;i++)CinematicJourneyState.update(190,.05,false,"ORBIT");
        assertTrue(CinematicJourneyState.getStage()>=CinematicJourneyState.CLOUD_SEA);
    }
    @Test public void approachForcesReturnScene(){
        CinematicJourneyState.reset();
        CinematicJourneyState.update(145,.05,false,"APPROACH");
        assertEquals(CinematicJourneyState.RETURN,CinematicJourneyState.getStage());
    }
    @Test public void runwayHoldResetsJourney(){
        CinematicJourneyState.reset();
        for(int i=0;i<700;i++)CinematicJourneyState.update(180,.05,false,"ORBIT");
        CinematicJourneyState.update(0,.05,true,"RUNWAY_HOLD");
        assertEquals(CinematicJourneyState.RUNWAY,CinematicJourneyState.getStage());
        assertEquals(0.0,CinematicJourneyState.getAirborneSec(),.001);
    }
}
