package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class WalkableCorridorEstimatorTest {
    private static final int W = 72;
    private static final int H = 96;

    @Test
    public void oneFrameDoesNotCreateDirectionalAdvice() {
        WalkableCorridorEstimator estimator = new WalkableCorridorEstimator();
        short[] depth = sceneWithCenterBlocked();
        WalkableCorridorObservation observation = estimator.analyze(depth, W, H, 1000L);
        assertEquals(Direction.UNKNOWN, observation.moreOpenDirection());
        assertTrue(observation.persistenceScore() < 0.20f);
    }

    @Test
    public void persistentCenterBlockSelectsClearlyMoreOpenLeftLane() {
        WalkableCorridorEstimator estimator = new WalkableCorridorEstimator();
        short[] depth = sceneWithCenterBlocked();
        WalkableCorridorObservation observation = null;
        for (int i = 0; i < 8; i++) {
            observation = estimator.analyze(depth, W, H, 1000L + i * 45L);
        }
        assertNotNull(observation);
        assertEquals(Direction.LEFT, observation.moreOpenDirection());
        assertTrue(observation.leftOpenScore() > observation.centerOpenScore() + 0.18f);
        assertTrue(observation.hasPersistentCandidate());
    }

    @Test
    public void equallyOpenLanesDoNotInventTurnDirection() {
        WalkableCorridorEstimator estimator = new WalkableCorridorEstimator();
        short[] depth = new short[W * H];
        java.util.Arrays.fill(depth, (short) 4600);
        WalkableCorridorObservation observation = null;
        for (int i = 0; i < 8; i++) {
            observation = estimator.analyze(depth, W, H, 2000L + i * 45L);
        }
        assertNotNull(observation);
        assertEquals(Direction.UNKNOWN, observation.moreOpenDirection());
        assertTrue(observation.centerOpenScore() > 0.55f);
    }

    @Test
    public void sparseDepthCannotProduceConfidentDirection() {
        WalkableCorridorEstimator estimator = new WalkableCorridorEstimator();
        short[] sparse = new short[W * H];
        for (int y = 0; y < H; y += 12) {
            for (int x = 0; x < W; x += 12) sparse[y * W + x] = 5000;
        }
        WalkableCorridorObservation observation = null;
        for (int i = 0; i < 8; i++) {
            observation = estimator.analyze(sparse, W, H, 3000L + i * 45L);
        }
        assertNotNull(observation);
        assertTrue(observation.confidence() < 0.42f);
        assertEquals(Direction.UNKNOWN, observation.moreOpenDirection());
    }

    private static short[] sceneWithCenterBlocked() {
        short[] depth = new short[W * H];
        java.util.Arrays.fill(depth, (short) 4600);
        int y0 = Math.round(H * 0.22f);
        int y1 = Math.round(H * 0.66f);
        for (int y = y0; y < y1; y++) {
            for (int x = Math.round(W * 0.35f); x < Math.round(W * 0.65f); x++) {
                depth[y * W + x] = 850;
            }
            for (int x = Math.round(W * 0.66f); x < Math.round(W * 0.93f); x++) {
                depth[y * W + x] = 1800;
            }
        }
        return depth;
    }
}
