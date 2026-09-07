package com.mg.machineelementspro;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class SystemSelectionEngineTest {
    @Test public void shaftBearingFitReturnsFiniteDesign(){
        SystemSelectionEngine.ShaftBearingFitResult r=SystemSelectionEngine.solveShaftBearingFit(120,60,530,2.0,2500,500,1500,5000,1.5);
        assertTrue(r.shaftRequiredMm>0);
        assertTrue(r.shaftPreferredMm>=r.shaftRequiredMm);
        assertNotNull(r.bearing);
    }
    @Test public void tighteningScatterBoundsNominal(){
        SystemSelectionEngine.TighteningResult r=SystemSelectionEngine.tighteningScatter(12,40000,0.20,15,20);
        assertTrue(r.preloadMinN<r.nominalPreloadN);
        assertTrue(r.preloadMaxN>r.nominalPreloadN);
        assertTrue(r.torqueMinNm<r.torqueNominalNm);
        assertTrue(r.torqueMaxNm>r.torqueNominalNm);
    }
    @Test public void paretoOptionsAreSortedByScore(){
        List<SystemSelectionEngine.ParetoOption> p=SystemSelectionEngine.paretoShaftOptions(150,80,2.0);
        assertEquals(4,p.size());
        for(int i=1;i<p.size();i++) assertTrue(p.get(i).normalizedScore>=p.get(i-1).normalizedScore);
    }
}
