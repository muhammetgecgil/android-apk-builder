package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class GearboxDesignEngineTest {
    @Test public void ratioAndTorqueAreConsistent(){
        GearboxDesignEngine.GearboxResult r=GearboxDesignEngine.sizeSingleStage(100,1500,20,60,3,30,20,0.97,530,2.0,250,10000);
        assertEquals(3.0,r.ratio,1e-9);
        assertEquals(291.0,r.outputTorqueNm,1e-9);
    }
    @Test public void pitchDiametersFollowModuleAndTeeth(){
        GearboxDesignEngine.GearboxResult r=GearboxDesignEngine.sizeSingleStage(80,1800,18,54,2.5,25,20,0.96,530,2.0,220,8000);
        assertEquals(45.0,r.pinionPitchDiameterMm,1e-9);
        assertEquals(135.0,r.gearPitchDiameterMm,1e-9);
    }
    @Test public void strongerShaftMaterialReducesRequiredDiameter(){
        GearboxDesignEngine.GearboxResult a=GearboxDesignEngine.sizeSingleStage(120,1400,20,50,3,30,20,0.97,310,2.0,250,10000);
        GearboxDesignEngine.GearboxResult b=GearboxDesignEngine.sizeSingleStage(120,1400,20,50,3,30,20,0.97,655,2.0,250,10000);
        assertTrue(b.shaft1RequiredMm<a.shaft1RequiredMm);
    }
}
