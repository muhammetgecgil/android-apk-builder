package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class DepthLevelChangeEstimatorTest {
    private static final int W = 72;
    private static final int H = 96;

    @Test public void smoothPlanarTrendDoesNotBecomeLevelChange() {
        DepthLevelChangeEstimator estimator = new DepthLevelChangeEstimator();
        LevelChangeObservation last = null;
        for (int i = 0; i < 8; i++) {
            last = estimator.analyze(grid(Mode.PLANAR), W, H, 1000L + i * 220L);
        }
        assertNotNull(last);
        assertEquals(LevelChangeKind.UNKNOWN, last.kind());
        assertTrue(last.candidateScore() < 0.30f);
        assertEquals(0f, last.persistenceScore(), 0.001f);
    }

    @Test public void persistentFartherUpperSideBecomesDownwardCandidate() {
        DepthLevelChangeEstimator estimator = new DepthLevelChangeEstimator();
        LevelChangeObservation last = null;
        for (int i = 0; i < 7; i++) {
            last = estimator.analyze(grid(Mode.DOWN), W, H, 2000L + i * 220L);
        }
        assertNotNull(last);
        assertEquals(LevelChangeKind.DOWNWARD_CANDIDATE, last.kind());
        assertTrue(last.candidateScore() >= 0.58f);
        assertTrue(last.persistenceScore() >= 0.56f);
        assertTrue(last.boundaryY() > 0.50f && last.boundaryY() < 0.82f);
    }

    @Test public void persistentNearerUpperSideBecomesUpwardCandidate() {
        DepthLevelChangeEstimator estimator = new DepthLevelChangeEstimator();
        LevelChangeObservation last = null;
        for (int i = 0; i < 7; i++) {
            last = estimator.analyze(grid(Mode.UP), W, H, 3000L + i * 220L);
        }
        assertNotNull(last);
        assertEquals(LevelChangeKind.UPWARD_CANDIDATE, last.kind());
        assertTrue(last.candidateScore() >= 0.58f);
        assertTrue(last.persistenceScore() >= 0.56f);
    }

    @Test public void oneFrameCandidateNeverCountsAsPersistent() {
        DepthLevelChangeEstimator estimator = new DepthLevelChangeEstimator();
        LevelChangeObservation one = estimator.analyze(grid(Mode.DOWN), W, H, 4000L);
        assertEquals(LevelChangeKind.DOWNWARD_CANDIDATE, one.kind());
        assertEquals(0f, one.persistenceScore(), 0.001f);
        assertFalse(one.persistentCandidate());
    }

    @Test public void sparseDepthCannotProduceCandidate() {
        DepthLevelChangeEstimator estimator = new DepthLevelChangeEstimator();
        short[] sparse = new short[W * H];
        for (int y = 0; y < H; y += 8) {
            for (int x = 0; x < W; x += 10) sparse[y * W + x] = (short) 2400;
        }
        LevelChangeObservation o = estimator.analyze(sparse, W, H, 5000L);
        assertEquals(LevelChangeKind.UNKNOWN, o.kind());
        assertTrue(o.depthConfidence() < 0.42f);
    }

    private enum Mode { PLANAR, DOWN, UP }

    private static short[] grid(Mode mode) {
        short[] out = new short[W * H];
        for (int y = 0; y < H; y++) {
            float ny = y / (float) (H - 1);
            int base = Math.round(7200f - 5200f * ny);
            int offset = 0;
            if (ny < 0.68f && mode == Mode.DOWN) offset = 1500;
            if (ny < 0.68f && mode == Mode.UP) offset = -1500;
            int value = Math.max(300, Math.min(9500, base + offset));
            for (int x = 0; x < W; x++) {
                int texture = ((x * 13 + y * 7) % 31) - 15;
                out[y * W + x] = (short) Math.max(300, value + texture);
            }
        }
        return out;
    }
}
