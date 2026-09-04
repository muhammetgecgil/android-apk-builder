package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class RealWeatherProfileTest {
    @Test public void stormCreatesRoughSeaAndHeavyHaze(){
        RealWeatherProfile p=RealWeatherProfile.fromValues(95,14,92,4.2,0,96,90,85,70,2800,15,310,24,true,.55);
        assertEquals(RealWeatherProfile.STORM,p.kind);assertTrue(p.seaRoughness01>.85);assertTrue(p.skyHaze01>.65);assertTrue(p.mountainHaze01>.75);
    }
    @Test public void clearWeatherKeepsLongVisibility(){
        RealWeatherProfile p=RealWeatherProfile.fromValues(0,24,45,0,0,8,2,3,7,45000,2,120,3,true,.50);
        assertEquals(RealWeatherProfile.CLEAR,p.kind);assertTrue(p.skyHaze01<.25);assertTrue(p.seaRoughness01<.35);
    }
    @Test public void coldSnowProducesMountainSnowCover(){
        RealWeatherProfile p=RealWeatherProfile.fromValues(73,-5,88,.8,1.2,90,82,75,40,7000,7,20,10,false,.90);
        assertEquals(RealWeatherProfile.SNOW,p.kind);assertTrue(p.snowCover01>.70);
    }
    @Test public void fogCodeOverridesCloudClassification(){
        RealWeatherProfile p=RealWeatherProfile.fromValues(45,8,99,0,0,100,100,20,0,500,1,0,2,true,.30);
        assertEquals(RealWeatherProfile.FOG,p.kind);assertTrue(p.mountainHaze01>.90);
    }
}
