package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VisionFusionRotationTest {
    private static final float EPS = 0.0001f;

    @Test public void rotation90MapsSensorCoordinatesToPortraitCoordinates() {
        float[] p = VisionFusionAnalyzer.rotateNormalized(0.20f, 0.70f, 90);
        assertEquals(0.30f, p[0], EPS);
        assertEquals(0.20f, p[1], EPS);
    }

    @Test public void rotation270MapsSensorCoordinatesToPortraitCoordinates() {
        float[] p = VisionFusionAnalyzer.rotateNormalized(0.20f, 0.70f, 270);
        assertEquals(0.70f, p[0], EPS);
        assertEquals(0.80f, p[1], EPS);
    }

    @Test public void rotation180FlipsBothAxes() {
        float[] p = VisionFusionAnalyzer.rotateNormalized(0.20f, 0.70f, 180);
        assertEquals(0.80f, p[0], EPS);
        assertEquals(0.30f, p[1], EPS);
    }
}
