package com.mgecgil.seslirehber.core;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UrbanSegmentationLogitAnalyzerTest {
    private final UrbanSegmentationLogitAnalyzer analyzer = new UrbanSegmentationLogitAnalyzer();

    @Test public void cityscapesClassesRemainDistinct() {
        int w = 6, h = 6, n = w * h;
        float[] logits = skyBaseline(n);
        set(logits, n, 0, 0, w, 0);   // road
        set(logits, n, 1, 0, w, 1);   // sidewalk
        set(logits, n, 2, 1, w, 2);   // building
        set(logits, n, 3, 1, w, 5);   // pole
        set(logits, n, 4, 2, w, 6);   // traffic light
        set(logits, n, 0, 3, w, 11);  // person
        set(logits, n, 2, 4, w, 13);  // car
        set(logits, n, 5, 5, w, 18);  // bicycle

        UrbanSegmentationLogitAnalyzer.Raw r = analyzer.analyze(logits, w, h);
        assertEquals(1f / n, r.roadRatio(), 0.0001f);
        assertEquals(1f / n, r.sidewalkRatio(), 0.0001f);
        assertEquals(1f / n, r.buildingWallRatio(), 0.0001f);
        assertEquals(1f / n, r.fencePoleRatio(), 0.0001f);
        assertEquals(1f / n, r.trafficControlRatio(), 0.0001f);
        assertEquals(1f / n, r.personRiderRatio(), 0.0001f);
        assertEquals(1f / n, r.vehicleRatio(), 0.0001f);
        assertEquals(1f / n, r.twoWheelerRatio(), 0.0001f);
        assertEquals((n - 8f) / n, r.skyRatio(), 0.0001f);
    }

    @Test public void lowerCenterSeparatesRoadSidewalkAndObstacle() {
        int w = 9, h = 9, n = w * h;
        float[] logits = skyBaseline(n);
        for (int y = 5; y < 9; y++) {
            for (int x = 3; x < 6; x++) {
                int label = x == 3 ? 0 : (x == 4 ? 1 : 13);
                set(logits, n, x, y, w, label);
            }
        }
        UrbanSegmentationLogitAnalyzer.Raw r = analyzer.analyze(logits, w, h);
        assertTrue(r.lowerCenterRoadRatio() > 0.25f);
        assertTrue(r.lowerCenterSidewalkRatio() > 0.25f);
        assertTrue(r.lowerCenterObstacleRatio() > 0.25f);
    }

    @Test public void obstacleOccupancyKeepsLeftCenterRightSeparate() {
        int w = 9, h = 6, n = w * h;
        float[] logits = skyBaseline(n);
        for (int y = 0; y < h; y++) for (int x = 0; x < 3; x++) set(logits, n, x, y, w, 2);
        for (int y = 0; y < 2; y++) for (int x = 3; x < 6; x++) set(logits, n, x, y, w, 13);
        UrbanSegmentationLogitAnalyzer.Raw r = analyzer.analyze(logits, w, h);
        assertTrue(r.leftObstacleOccupancy() > 0.95f);
        assertTrue(r.centerObstacleOccupancy() > 0.25f);
        assertTrue(r.rightObstacleOccupancy() < 0.01f);
    }

    private static float[] skyBaseline(int pixels) {
        float[] logits = new float[UrbanSegmentationLogitAnalyzer.CLASSES * pixels];
        Arrays.fill(logits, -5f);
        for (int i = 0; i < pixels; i++) logits[10 * pixels + i] = 5f;
        return logits;
    }

    private static void set(float[] logits, int pixels, int x, int y, int width, int label) {
        int p = y * width + x;
        for (int c = 0; c < UrbanSegmentationLogitAnalyzer.CLASSES; c++) logits[c * pixels + p] = -5f;
        logits[label * pixels + p] = 8f;
    }
}
