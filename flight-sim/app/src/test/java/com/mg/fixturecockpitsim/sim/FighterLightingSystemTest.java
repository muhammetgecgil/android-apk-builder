package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FighterLightingSystemTest {
    @Test public void strobeUsesDoubleFlashPattern(){
        FighterLightingSystem s=new FighterLightingSystem();
        assertEquals(1.0,s.strobeIntensity(10),0.001);
        assertEquals(0.0,s.strobeIntensity(100),0.001);
        assertEquals(1.0,s.strobeIntensity(190),0.001);
        assertEquals(0.0,s.strobeIntensity(500),0.001);
    }

    @Test public void gearMountedLandingLightFadesWithGear(){
        FighterLightingSystem s=new FighterLightingSystem();s.landing=true;
        assertEquals(0.0,s.landingIntensity(.1),0.001);
        assertTrue(s.landingIntensity(.55)>.4);
        assertEquals(1.0,s.landingIntensity(1.0),0.001);
    }

    @Test public void taxiLightRequiresGround(){
        FighterLightingSystem s=new FighterLightingSystem();s.taxi=true;
        assertEquals(0.0,s.taxiIntensity(1.0,false),0.001);
        assertEquals(1.0,s.taxiIntensity(1.0,true),0.001);
    }

    @Test public void hudAndFloodBrightnessCycle(){
        FighterLightingSystem s=new FighterLightingSystem();
        assertEquals(1.0,s.hudBrightness(),0.001);
        s.cycleHud();assertEquals(.70,s.hudBrightness(),0.001);
        s.cycleHud();assertEquals(.45,s.hudBrightness(),0.001);
        assertEquals(0.0,s.floodBrightness(),0.001);
        s.cycleFlood();assertEquals(.28,s.floodBrightness(),0.001);
    }
}
