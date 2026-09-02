package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class SupersonicFlightModelTest {
    @Test public void waveDragPeaksAroundMachOne(){
        SupersonicFlightModel m=new SupersonicFlightModel();
        double a=FighterSoundModel.speedOfSoundMps(7000);
        SupersonicFlightModel.Output sub=m.evaluate(7000,a*.65,.9,0,0,0);
        SupersonicFlightModel.Output trans=m.evaluate(7000,a*1.02,.9,0,0,0);
        SupersonicFlightModel.Output sup=m.evaluate(7000,a*1.70,.99,0,0,0);
        assertTrue(trans.waveDrag01>sub.waveDrag01+.5);
        assertTrue(trans.waveDrag01>sup.waveDrag01+.5);
    }

    @Test public void transonicBuffetIsNarrowAndStrongNearMachOne(){
        SupersonicFlightModel m=new SupersonicFlightModel();
        double a=FighterSoundModel.speedOfSoundMps(4000);
        assertTrue(m.evaluate(4000,a*.99,.9,0,0,0).transonicBuffet01>.85);
        assertTrue(m.evaluate(4000,a*.72,.9,0,0,0).transonicBuffet01<.01);
        assertTrue(m.evaluate(4000,a*1.30,.99,0,0,0).transonicBuffet01<.01);
    }

    @Test public void afterburnerProvidesSupersonicTargetSpeed(){
        SupersonicFlightModel m=new SupersonicFlightModel();
        double a=FighterSoundModel.speedOfSoundMps(9000);
        SupersonicFlightModel.Output dry=m.evaluate(9000,250,.72,0,0,0);
        SupersonicFlightModel.Output ab=m.evaluate(9000,250,1.0,0,0,0);
        assertTrue(ab.afterburner01>.95);
        assertTrue(ab.targetSpeedMps>a*1.45);
        assertTrue(ab.targetSpeedMps>dry.targetSpeedMps+180);
    }

    @Test public void gearAndSpeedBrakePenalizeHighSpeedTarget(){
        SupersonicFlightModel m=new SupersonicFlightModel();
        SupersonicFlightModel.Output clean=m.evaluate(3000,250,1,0,0,0);
        SupersonicFlightModel.Output dirty=m.evaluate(3000,250,1,1,1,0);
        assertTrue(clean.targetSpeedMps>dirty.targetSpeedMps+150);
    }

    @Test public void machCrossingIsEdgeTriggered(){
        assertTrue(SupersonicFlightModel.crossedMachOne(.99,1.001));
        assertFalse(SupersonicFlightModel.crossedMachOne(1.01,1.20));
        assertFalse(SupersonicFlightModel.crossedMachOne(.80,.95));
    }
}
