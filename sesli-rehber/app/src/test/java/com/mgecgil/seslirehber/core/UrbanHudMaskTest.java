package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class UrbanHudMaskTest {
    @Test public void labelsFollowPerPixelArgmax() {
        int width = 2;
        int height = 2;
        int pixels = width * height;
        float[] logits = new float[UrbanSegmentationLogitAnalyzer.CLASSES * pixels];
        for (int i = 0; i < logits.length; i++) logits[i] = -5f;
        logits[0 * pixels + 0] = 3f;   // road
        logits[1 * pixels + 1] = 4f;   // sidewalk
        logits[11 * pixels + 2] = 5f;  // person
        logits[13 * pixels + 3] = 6f;  // car

        byte[] labels = new UrbanSegmentationLogitAnalyzer().labels(logits, width, height);
        assertArrayEquals(new byte[]{0, 1, 11, 13}, labels);
    }

    @Test public void invalidLogitsProduceNoHudMask() {
        byte[] labels = new UrbanSegmentationLogitAnalyzer().labels(new float[3], 4, 4);
        assertEquals(0, labels.length);
    }

    @Test public void contextExpiresStaleMask() {
        UrbanHudMaskContext.reset();
        UrbanHudMaskContext.publish(2, 2, new byte[]{0,1,2,3}, 0.56f, 1000L);
        assertNotNull(UrbanHudMaskContext.latest(3000L));
        assertNull(UrbanHudMaskContext.latest(5001L));
    }
}
