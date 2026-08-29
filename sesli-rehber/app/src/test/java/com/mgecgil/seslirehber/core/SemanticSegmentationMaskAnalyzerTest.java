package com.mgecgil.seslirehber.core;

import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SemanticSegmentationMaskAnalyzerTest {
    private final SemanticSegmentationMaskAnalyzer analyzer = new SemanticSegmentationMaskAnalyzer();

    @Test public void backgroundOnlyHasNoForeground() {
        ByteBuffer mask = ByteBuffer.allocate(9);
        SemanticSegmentationMaskAnalyzer.Raw r = analyzer.analyze(mask, 3, 3);
        assertEquals(0f, r.foregroundRatio(), 0.0001f);
        assertEquals(0f, r.leftOccupancy(), 0.0001f);
        assertEquals(0f, r.centerOccupancy(), 0.0001f);
        assertEquals(0f, r.rightOccupancy(), 0.0001f);
    }

    @Test public void classAndSectorRatiosComeFromPixels() {
        int w = 6, h = 6;
        ByteBuffer mask = ByteBuffer.allocate(w * h);
        // Person in left/far, car in center/mid, bicycle in right/near.
        set(mask, w, 0, 0, 15); set(mask, w, 1, 0, 15);
        set(mask, w, 2, 2, 7);  set(mask, w, 3, 2, 7);
        set(mask, w, 4, 5, 2);  set(mask, w, 5, 5, 2);
        SemanticSegmentationMaskAnalyzer.Raw r = analyzer.analyze(mask, w, h);

        assertEquals(2f / 36f, r.personRatio(), 0.0001f);
        assertEquals(2f / 36f, r.vehicleRatio(), 0.0001f);
        assertEquals(2f / 36f, r.twoWheelerRatio(), 0.0001f);
        assertEquals(6f / 36f, r.foregroundRatio(), 0.0001f);
        assertTrue(r.leftOccupancy() > 0f);
        assertTrue(r.centerOccupancy() > 0f);
        assertTrue(r.rightOccupancy() > 0f);
        assertTrue(r.farOccupancy() > 0f);
        assertTrue(r.midOccupancy() > 0f);
        assertTrue(r.nearOccupancy() > 0f);
    }

    @Test public void lowerCenterOccupancyDetectsRecognizedObstacleArea() {
        int w = 9, h = 9;
        ByteBuffer mask = ByteBuffer.allocate(w * h);
        // Fill lower-center third with chair class.
        for (int y = 6; y < 9; y++) for (int x = 3; x < 6; x++) set(mask, w, x, y, 9);
        SemanticSegmentationMaskAnalyzer.Raw r = analyzer.analyze(mask, w, h);
        assertEquals(1f, r.lowerCenterOccupancy(), 0.0001f);
        assertTrue(r.furnitureRatio() > 0.10f);
        assertTrue(r.centerOccupancy() > r.leftOccupancy());
    }

    private static void set(ByteBuffer b, int width, int x, int y, int label) {
        b.put(y * width + x, (byte) label);
    }
}
