package com.mgecgil.seslirehber.core;

import java.util.Arrays;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;
import static com.mgecgil.seslirehber.core.GuidanceModels.WalkableCorridorObservation;

/**
 * Relative three-lane forward openness estimator from an upright depth grid.
 * It never labels a lane "safe"; it only identifies a persistent lane that appears more open than
 * the center lane. This is intentionally conservative for pre-field-validation use.
 */
public final class WalkableCorridorEstimator {
    private static final int MIN_MM = 250;
    private static final int MAX_MM = 10000;
    private final float[] smoothed = new float[3];
    private Direction lastCandidate = Direction.UNKNOWN;
    private int candidateStreak;

    public WalkableCorridorObservation analyze(short[] depthMm, int width, int height, long timestampMs) {
        if (depthMm == null || width < 12 || height < 12 || depthMm.length < width * height) {
            return quiet(timestampMs);
        }

        // Mid-forward band: low enough to represent the walking corridor but high enough that the
        // floor itself does not dominate all samples.
        int y0 = Math.max(0, Math.round(height * 0.22f));
        int y1 = Math.min(height, Math.round(height * 0.66f));
        float[] x0 = {0.07f, 0.35f, 0.66f};
        float[] x1 = {0.34f, 0.65f, 0.93f};
        float[] rawScores = new float[3];
        float[] coverages = new float[3];

        for (int lane = 0; lane < 3; lane++) {
            int lx0 = Math.max(0, Math.round(width * x0[lane]));
            int lx1 = Math.min(width, Math.round(width * x1[lane]));
            int strideX = Math.max(1, (lx1 - lx0) / 16);
            int strideY = Math.max(1, (y1 - y0) / 18);
            int expected = 0;
            int[] values = new int[Math.max(16, (lx1 - lx0) * (y1 - y0))];
            int count = 0;

            for (int y = y0; y < y1; y += strideY) {
                for (int x = lx0; x < lx1; x += strideX) {
                    expected++;
                    int mm = depthMm[y * width + x] & 0xffff;
                    if (mm < MIN_MM || mm > MAX_MM) continue;
                    if (count < values.length) values[count++] = mm;
                }
            }

            coverages[lane] = expected == 0 ? 0f : count / (float) expected;
            if (count < 8) {
                rawScores[lane] = 0f;
                continue;
            }
            Arrays.sort(values, 0, count);
            // 30th percentile is deliberately used instead of median: a nearby obstacle occupying a
            // meaningful part of the lane should reduce openness even when the background is far.
            int pIndex = Math.min(count - 1, Math.max(0, Math.round((count - 1) * 0.30f)));
            float p30 = values[pIndex];
            float distanceScore = clamp01((p30 - 900f) / 3000f);
            float coverageScore = clamp01((coverages[lane] - 0.22f) / 0.58f);
            rawScores[lane] = clamp01(distanceScore * (0.62f + 0.38f * coverageScore));
        }

        for (int i = 0; i < 3; i++) {
            smoothed[i] = smoothed[i] * 0.66f + rawScores[i] * 0.34f;
        }

        float coverageMean = (coverages[0] + coverages[1] + coverages[2]) / 3f;
        float confidence = clamp01((coverageMean - 0.18f) / 0.60f);
        Direction candidate = chooseCandidate(smoothed, confidence);
        if (candidate != Direction.UNKNOWN && candidate == lastCandidate) {
            candidateStreak = Math.min(10, candidateStreak + 1);
        } else if (candidate != Direction.UNKNOWN) {
            lastCandidate = candidate;
            candidateStreak = 1;
        } else {
            candidateStreak = Math.max(0, candidateStreak - 1);
            if (candidateStreak == 0) lastCandidate = Direction.UNKNOWN;
        }
        float persistence = clamp01((candidateStreak - 1f) / 4f);
        Direction stableDirection = persistence >= 0.40f ? lastCandidate : Direction.UNKNOWN;

        return new WalkableCorridorObservation(
                smoothed[0],
                smoothed[1],
                smoothed[2],
                clamp01(1f - smoothed[1]),
                stableDirection,
                confidence,
                persistence,
                timestampMs);
    }

    public void reset() {
        Arrays.fill(smoothed, 0f);
        lastCandidate = Direction.UNKNOWN;
        candidateStreak = 0;
    }

    private static Direction chooseCandidate(float[] scores, float confidence) {
        if (confidence < 0.42f) return Direction.UNKNOWN;
        float center = scores[1];
        Direction bestDirection = scores[0] >= scores[2] ? Direction.LEFT : Direction.RIGHT;
        float bestSide = Math.max(scores[0], scores[2]);

        // Direction advice only matters when center is meaningfully constrained and a side is
        // clearly more open. We intentionally do not announce small score differences.
        if (center > 0.62f || bestSide < 0.42f || bestSide - center < 0.18f) {
            return Direction.UNKNOWN;
        }
        return bestDirection;
    }

    private static WalkableCorridorObservation quiet(long timestampMs) {
        return new WalkableCorridorObservation(0f, 0f, 0f, 1f,
                Direction.UNKNOWN, 0f, 0f, timestampMs);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
