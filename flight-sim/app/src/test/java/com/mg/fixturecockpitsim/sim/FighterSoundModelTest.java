package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class FighterSoundModelTest {
    @Test public void afterburnerRisesOnlyAtHighThrottle(){
        FighterSoundModel m=new FighterSoundModel();
        FighterSoundModel.Mix dry=m.evaluate(.65,220,2000,0,0,false,0,0);
        FighterSoundModel.Mix ab=m.evaluate(.98,220,2000,0,0,false,0,0);
        assertTrue(dry.afterburner<.02);
        assertTrue(ab.afterburner>.30);
        assertTrue(ab.exhaust>dry.exhaust);
    }

    @Test public void tyreAndBrakeRequireWeightOnWheels(){
        FighterSoundModel m=new FighterSoundModel();
        FighterSoundModel.Mix air=m.evaluate(.30,70,0,1,1,false,0,0);
        FighterSoundModel.Mix ground=m.evaluate(.30,70,0,1,1,true,0,0);
        assertEquals(0.0,air.tyre,1e-9);
        assertEquals(0.0,air.brake,1e-9);
        assertTrue(ground.tyre>0);
        assertTrue(ground.brake>0);
    }

    @Test public void hydraulicLayersFollowMechanicalMotion(){
        FighterSoundModel m=new FighterSoundModel();
        FighterSoundModel.Mix still=m.evaluate(.4,100,1000,.5,0,false,0,0);
        FighterSoundModel.Mix moving=m.evaluate(.4,100,1000,.5,0,false,1,1);
        assertEquals(0.0,still.gearHydraulic,1e-9);
        assertEquals(0.0,still.surfaceHydraulic,1e-9);
        assertTrue(moving.gearHydraulic>.15);
        assertTrue(moving.surfaceHydraulic>.04);
    }

    @Test public void transonicBuffetPeaksNearMachOne(){
        FighterSoundModel m=new FighterSoundModel();
        double a=FighterSoundModel.speedOfSoundMps(5000);
        FighterSoundModel.Mix near=m.evaluate(.8,a,5000,0,0,false,0,0);
        FighterSoundModel.Mix far=m.evaluate(.8,a*.70,5000,0,0,false,0,0);
        assertTrue(near.transonic>.10);
        assertEquals(0.0,far.transonic,1e-9);
    }

    @Test public void sonicBoomNeedsWorldFixedObserver(){
        assertFalse(FighterSoundModel.shouldTriggerSonicBoom(.98,1.02,false));
        assertTrue(FighterSoundModel.shouldTriggerSonicBoom(.98,1.02,true));
        assertFalse(FighterSoundModel.shouldTriggerSonicBoom(1.01,1.05,true));
    }

    @Test public void speedOfSoundFallsWithAltitudeInTroposphere(){
        assertTrue(FighterSoundModel.speedOfSoundMps(10000)<FighterSoundModel.speedOfSoundMps(0));
    }
}
