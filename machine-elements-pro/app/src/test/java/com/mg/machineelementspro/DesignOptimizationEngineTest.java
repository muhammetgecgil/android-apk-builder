package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class DesignOptimizationEngineTest {
    @Test public void bearingSelectionMeetsLifeAndStaticTargets(){
        DesignOptimizationEngine.BearingPick b=DesignOptimizationEngine.selectBearing(2500,400,1200,10000,1.5);
        assertFalse("OUT-OF-RANGE".equals(b.designation));
        assertTrue(b.lifeHours>=10000); assertTrue(b.staticFoS>=1.5);
    }
    @Test public void boltSelectionMeetsPreloadAndProofTargets(){
        DesignOptimizationEngine.BoltPick b=DesignOptimizationEngine.selectBolt(5000,12000,1.2);
        assertFalse("OUT-OF-RANGE".equals(b.size));
        assertTrue(b.preloadN>=11999); assertTrue(b.proofFoS>=1.2);
    }
    @Test public void h7g6ProducesPositiveClearance(){
        double[] x=DesignOptimizationEngine.basicHoleH7ShaftG6(40);
        assertTrue(x[1]>x[0]); assertTrue(x[3]>x[2]); assertTrue(x[4]>0); assertTrue(x[5]>x[4]);
    }
    @Test public void materialOptimizerReturnsFinitePositiveDesign(){
        DesignOptimizationEngine.MaterialOption o=DesignOptimizationEngine.optimizeShaft(250,120,2.0);
        assertNotNull(o); assertTrue(o.diameterMm>0); assertTrue(o.kgPerM>0); assertTrue(Double.isFinite(o.score));
    }
}
