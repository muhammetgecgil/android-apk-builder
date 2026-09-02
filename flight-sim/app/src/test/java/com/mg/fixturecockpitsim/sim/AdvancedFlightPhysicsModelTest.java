package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdvancedFlightPhysicsModelTest {
    @Test public void liftTableRisesBeforeStallAndRollsOffAfterPeak(){
        double cl5=AdvancedFlightPhysicsModel.liftCoefficientAtAoA(5);
        double cl15=AdvancedFlightPhysicsModel.liftCoefficientAtAoA(15);
        double cl30=AdvancedFlightPhysicsModel.liftCoefficientAtAoA(30);
        assertTrue(cl15>cl5);
        assertTrue(cl30<cl15);
    }

    @Test public void dynamicPressureFollowsVelocitySquared(){
        double q100=AdvancedFlightPhysicsModel.dynamicPressurePa(0,100);
        double q200=AdvancedFlightPhysicsModel.dynamicPressurePa(0,200);
        assertEquals(4.0,q200/q100,.03);
    }

    @Test public void densityFallsWithAltitude(){
        assertTrue(AdvancedFlightPhysicsModel.airDensityKgM3(10000)<AdvancedFlightPhysicsModel.airDensityKgM3(0));
    }

    @Test public void fuelBurnReducesMassAndMovesCgForward(){
        FlightState s=new FlightState();
        s.fuelKg=AdvancedFlightPhysicsModel.INITIAL_FUEL_KG;
        s.massKg=AdvancedFlightPhysicsModel.DRY_MASS_KG+s.fuelKg;
        s.cgMac=AdvancedFlightPhysicsModel.FULL_FUEL_CG_MAC;
        s.throttle=1;
        double m0=s.massKg,cg0=s.cgMac;
        for(int i=0;i<4000;i++)AdvancedFlightPhysicsModel.updateMassAndFuel(s,.05);
        assertTrue(s.massKg<m0);
        assertTrue(s.cgMac<cg0);
        assertTrue(s.fuelFraction01<1.0);
    }

    @Test public void groundEffectAddsLiftAndReducesDrag(){
        AdvancedFlightPhysicsModel m=new AdvancedFlightPhysicsModel();
        FighterFlightControlSystem fcsModel=new FighterFlightControlSystem();
        FlightControls c=new FlightControls();c.throttle=.65;c.pitch=.25;c.clamp();

        FlightState near=new FlightState();
        near.altitudeM=2;near.trueAirspeedMps=120;near.verticalSpeedMps=0;near.pitchDeg=10;near.onGround=false;
        AdvancedFlightPhysicsModel.updateMassAndFuel(near,0);
        FighterFlightControlSystem.Output f1=fcsModel.update(near,c,.02);
        AdvancedFlightPhysicsModel.Output a=m.evaluate(near,c,f1);

        FlightState high=near.copy();high.altitudeM=200;
        FighterFlightControlSystem.Output f2=fcsModel.update(high,c,.02);
        AdvancedFlightPhysicsModel.Output b=m.evaluate(high,c,f2);
        // Correct the comparison for density by using coefficients rather than absolute forces.
        assertTrue(a.groundEffect01>b.groundEffect01);
        assertTrue(a.cl>b.cl);
        assertTrue(a.cd<b.cd);
    }

    @Test public void adverseYawMomentOpposesRollCommand(){
        AdvancedFlightPhysicsModel m=new AdvancedFlightPhysicsModel();
        FlightState s=new FlightState();s.altitudeM=1000;s.trueAirspeedMps=180;s.pitchDeg=3;s.onGround=false;
        AdvancedFlightPhysicsModel.updateMassAndFuel(s,0);
        FlightControls c=new FlightControls();c.roll=.7;c.throttle=.7;c.clamp();
        FighterFlightControlSystem.Output f=new FighterFlightControlSystem().update(s,c,.02);
        AdvancedFlightPhysicsModel.Output o=m.evaluate(s,c,f);
        assertTrue(o.adverseYawMomentNm<0);
    }

    @Test public void deepStallWithSideslipCanProduceSpinCue(){
        AdvancedFlightPhysicsModel m=new AdvancedFlightPhysicsModel();
        FlightState s=new FlightState();s.altitudeM=2500;s.trueAirspeedMps=95;s.verticalSpeedMps=0;s.pitchDeg=34;s.sideslipDeg=14;s.yawRateDegSec=45;s.rollRateDegSec=30;s.onGround=false;
        AdvancedFlightPhysicsModel.updateMassAndFuel(s,0);
        FlightControls c=new FlightControls();c.throttle=.55;c.yaw=.6;c.clamp();
        FighterFlightControlSystem.Output f=new FighterFlightControlSystem().update(s,c,.02);
        AdvancedFlightPhysicsModel.Output o=m.evaluate(s,c,f);
        assertTrue(o.stall01>.5);
        assertTrue(o.spin01>.15);
    }

    @Test public void transonicDragExceedsLowSubsonicDragAtSameAoA(){
        AdvancedFlightPhysicsModel m=new AdvancedFlightPhysicsModel();
        FlightControls c=new FlightControls();c.throttle=.8;c.clamp();
        FlightState low=new FlightState();low.altitudeM=5000;low.pitchDeg=5;low.trueAirspeedMps=190;low.onGround=false;AdvancedFlightPhysicsModel.updateMassAndFuel(low,0);
        FighterFlightControlSystem.Output fl=new FighterFlightControlSystem().update(low,c,.02);
        AdvancedFlightPhysicsModel.Output a=m.evaluate(low,c,fl);
        FlightState tr=low.copy();tr.trueAirspeedMps=AdvancedFlightPhysicsModel.speedOfSoundMps(5000)*.98;
        FighterFlightControlSystem.Output ft=new FighterFlightControlSystem().update(tr,c,.02);
        AdvancedFlightPhysicsModel.Output b=m.evaluate(tr,c,ft);
        assertTrue(b.waveDrag01>a.waveDrag01);
        assertTrue(b.cd>a.cd);
    }
}
