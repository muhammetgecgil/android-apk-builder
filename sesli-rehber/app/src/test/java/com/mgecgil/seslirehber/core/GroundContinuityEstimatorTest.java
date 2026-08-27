package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static com.mgecgil.seslirehber.core.GuidanceModels.GroundObservation;

public class GroundContinuityEstimatorTest {
    private static final int W = 48;
    private static final int H = 72;

    @Test public void smoothTexturedGroundStaysLowRisk() {
        GroundContinuityEstimator estimator = new GroundContinuityEstimator();
        byte[] frame = smoothFrame();
        GroundObservation result = null;
        for (int i = 0; i < 8; i++) {
            result = estimator.estimate(frame, frame, W, H, 0, true, 1000L + i * 50L);
        }
        assertTrue(result != null);
        assertTrue(result.anomalyScore() < 0.45f);
        assertTrue(result.persistenceScore() < 0.30f);
    }

    @Test public void broadPersistentLowerBoundaryBuildsCautionEvidence() {
        GroundContinuityEstimator estimator = new GroundContinuityEstimator();
        byte[] frame = discontinuityFrame();
        GroundObservation result = null;
        for (int i = 0; i < 12; i++) {
            result = estimator.estimate(frame, frame, W, H, 0, true, 1000L + i * 50L);
        }
        assertTrue(result != null);
        assertTrue(result.broadBoundaryScore() > 0.55f);
        assertTrue(result.anomalyScore() > 0.54f);
        assertTrue(result.persistenceScore() > 0.55f);
        assertTrue(result.boundaryY() > 0.50f);
    }

    @Test public void darkBlankViewHasLowConfidence() {
        GroundContinuityEstimator estimator = new GroundContinuityEstimator();
        byte[] frame = new byte[W * H];
        GroundObservation result = estimator.estimate(frame, frame, W, H, 0, true, 1000L);
        assertTrue(result.viewConfidence() < 0.25f);
    }

    private static byte[] smoothFrame() {
        byte[] data = new byte[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int checker = ((x / 3 + y / 3) & 1) == 0 ? 5 : -5;
                int value = 92 + y + x / 3 + checker;
                data[y * W + x] = (byte) Math.max(0, Math.min(255, value));
            }
        }
        return data;
    }

    private static byte[] discontinuityFrame() {
        byte[] data = new byte[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int value;
                if (y < 50) {
                    int checker = ((x / 4 + y / 4) & 1) == 0 ? 4 : -4;
                    value = 92 + y / 2 + checker;
                } else {
                    int checker = ((x + y) & 1) == 0 ? 22 : -22;
                    value = 188 + checker;
                }
                data[y * W + x] = (byte) Math.max(0, Math.min(255, value));
            }
        }
        return data;
    }
}
