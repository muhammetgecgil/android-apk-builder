package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class EngineeringLibraryTest {
    @Test public void preferredShaftRoundsUp(){
        assertEquals(25.0, EngineeringLibrary.selectPreferredShaft(23.1), 1e-9);
        assertEquals(40.0, EngineeringLibrary.selectPreferredShaft(40.0), 1e-9);
    }

    @Test public void metricBoltRoundsUp(){
        assertEquals(12, EngineeringLibrary.selectMetricBolt(11.2));
        assertEquals(20, EngineeringLibrary.selectMetricBolt(20.0));
    }

    @Test public void requiredShaftIncreasesWithMoment(){
        double d1=EngineeringLibrary.requiredSolidShaftDiameter(100,50,350,2.0);
        double d2=EngineeringLibrary.requiredSolidShaftDiameter(200,50,350,2.0);
        assertTrue(d2>d1);
        assertTrue(EngineeringLibrary.selectPreferredShaft(d1)>=d1);
    }

    @Test public void materialsHavePhysicalProperties(){
        assertTrue(EngineeringLibrary.MATERIALS.length>=8);
        for(EngineeringLibrary.Material m:EngineeringLibrary.MATERIALS){
            assertTrue(m.E>0); assertTrue(m.G>0); assertTrue(m.Sy>0); assertTrue(m.Sut>=m.Sy); assertTrue(m.density>0);
        }
    }
}
