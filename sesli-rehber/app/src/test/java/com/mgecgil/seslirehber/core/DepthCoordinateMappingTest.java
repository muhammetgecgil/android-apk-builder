package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DepthCoordinateMappingTest {
    private static final float EPS = 0.0001f;

    @Test public void rotationZeroKeepsCoordinates() {
        float[] p = DepthImageAdapter.uprightToRawNormalized(0.20f, 0.30f, 0);
        assertEquals(0.20f, p[0], EPS);
        assertEquals(0.30f, p[1], EPS);
    }

    @Test public void rotation90MapsUprightIntoRawSensor() {
        float[] p = DepthImageAdapter.uprightToRawNormalized(0.20f, 0.30f, 90);
        assertEquals(0.30f, p[0], EPS);
        assertEquals(0.80f, p[1], EPS);
    }

    @Test public void rotation180MapsUprightIntoRawSensor() {
        float[] p = DepthImageAdapter.uprightToRawNormalized(0.20f, 0.30f, 180);
        assertEquals(0.80f, p[0], EPS);
        assertEquals(0.70f, p[1], EPS);
    }

    @Test public void rotation270MapsUprightIntoRawSensor() {
        float[] p = DepthImageAdapter.uprightToRawNormalized(0.20f, 0.30f, 270);
        assertEquals(0.70f, p[0], EPS);
        assertEquals(0.20f, p[1], EPS);
    }
}
