package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

/** First executable gate for M80 Engineering Beta. */
public class M80AcceptanceTest {
    @Test public void transmissionTargetHasPhysicallyConsistentRequirements(){
        double powerKw=7.5;
        double inputRpm=1450.0;
        double targetOutputRpm=100.0;
        double efficiency=0.94;
        double ratio=inputRpm/targetOutputRpm;
        double inputTorque=9550.0*powerKw/inputRpm;
        double outputTorque=9550.0*powerKw*efficiency/targetOutputRpm;

        assertEquals(14.5,ratio,1e-9);
        assertEquals(49.39655,inputTorque,1e-4);
        assertEquals(673.275,outputTorque,1e-3);
        assertTrue(outputTorque>inputTorque);
    }

    @Test public void productSelectionMustNotUndersizeMetricBolt(){
        int selected=EngineeringLibrary.selectMetricBolt(13.1);
        assertTrue(selected>=14);
    }

    @Test public void bearingSelectionMustMeetRequiredDynamicCapacity(){
        SelectionCatalog.Bearing b=SelectionCatalog.selectBearing(25,12000);
        assertTrue(b.bore>=25);
        assertTrue(b.C>=12000);
    }
}
