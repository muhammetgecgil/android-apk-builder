package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.SceneHealthObservation;

public class SceneHealthEstimatorTest {
    @Test
    public void singleDarkFrameDoesNotBecomePersistentFailure() {
        SceneHealthEstimator estimator = new SceneHealthEstimator();
        byte[] dark = filled(48 * 72, 4);
        SceneHealthObservation observation = estimator.analyze(dark, 1000L);
        assertFalse(observation.persistentlyUnusable());
        assertTrue(observation.persistenceScore() < 0.20f);
    }

    @Test
    public void persistentDarkFramesBecomeUnusable() {
        SceneHealthEstimator estimator = new SceneHealthEstimator();
        byte[] dark = filled(48 * 72, 5);
        SceneHealthObservation observation = null;
        for (int i = 0; i < 8; i++) {
            observation = estimator.analyze(dark, 1000L + i * 40L);
        }
        assertNotNull(observation);
        assertTrue(observation.darkRatio() > 0.95f);
        assertTrue(observation.persistentlyUnusable());
    }

    @Test
    public void texturedNormalExposureStaysUsable() {
        SceneHealthEstimator estimator = new SceneHealthEstimator();
        byte[] textured = new byte[48 * 72];
        for (int i = 0; i < textured.length; i++) {
            int x = i % 48;
            int y = i / 48;
            textured[i] = (byte) (70 + ((x * 13 + y * 7) % 120));
        }
        SceneHealthObservation observation = null;
        for (int i = 0; i < 8; i++) {
            observation = estimator.analyze(textured, 2000L + i * 40L);
        }
        assertNotNull(observation);
        assertFalse(observation.persistentlyUnusable());
        assertTrue(observation.qualityScore() > 0.45f);
    }

    private static byte[] filled(int size, int value) {
        byte[] data = new byte[size];
        java.util.Arrays.fill(data, (byte) value);
        return data;
    }
}
