package com.mgecgil.seslirehber.core;

import java.util.Arrays;
import static com.mgecgil.seslirehber.core.GuidanceModels.LevelChangeKind;
import static com.mgecgil.seslirehber.core.GuidanceModels.LevelChangeObservation;

/**
 * Fits and removes the dominant vertical depth trend, then looks for persistent relative profile
 * breaks in the lower-centre walking corridor. The result is deliberately a candidate direction,
 * not a curb/hole/stair semantic classification.
 */
public final class DepthLevelChangeEstimator {
    private static final int BAND_COUNT = 8;
    private static final int MIN_MM = 250;
    private static final int MAX_MM = 10000;
    private static final long SAME_KIND_GAP_MS = 950L;

    private LevelChangeKind lastKind = LevelChangeKind.UNKNOWN;
    private int sameKindCount;
    private long firstSameKindMs;
    private long lastCandidateMs;

    public LevelChangeObservation analyze(short[] depthMm, int width, int height, long timestampMs) {
        if (depthMm == null || width < 12 || height < 12 || depthMm.length < width * height) {
            resetCandidate();
            return empty(timestampMs);
        }

        int x0 = Math.max(0, Math.round(width * 0.28f));
        int x1 = Math.min(width, Math.round(width * 0.72f));
        int y0 = Math.max(0, Math.round(height * 0.42f));
        int y1 = Math.min(height, Math.round(height * 0.94f));
        if (x1 <= x0 || y1 <= y0) {
            resetCandidate();
            return empty(timestampMs);
        }

        float[] medians = new float[BAND_COUNT];
        float[] centers = new float[BAND_COUNT];
        boolean[] valid = new boolean[BAND_COUNT];
        Arrays.fill(medians, Float.NaN);
        int expected = 0;
        int validSamples = 0;
        int validBands = 0;

        for (int band = 0; band < BAND_COUNT; band++) {
            int by0 = y0 + (y1 - y0) * band / BAND_COUNT;
            int by1 = y0 + (y1 - y0) * (band + 1) / BAND_COUNT;
            centers[band] = ((by0 + by1) * 0.5f) / Math.max(1f, height - 1f);
            int strideX = Math.max(1, (x1 - x0) / 24);
            int strideY = Math.max(1, (by1 - by0) / 8);
            int capacity = Math.max(8, ((x1 - x0) / strideX + 2) * ((by1 - by0) / strideY + 2));
            int[] values = new int[capacity];
            int count = 0;
            for (int y = by0; y < by1; y += strideY) {
                for (int x = x0; x < x1; x += strideX) {
                    expected++;
                    int mm = depthMm[y * width + x] & 0xffff;
                    if (mm < MIN_MM || mm > MAX_MM) continue;
                    validSamples++;
                    if (count < values.length) values[count++] = mm;
                }
            }
            if (count >= 6) {
                medians[band] = median(values, count);
                valid[band] = true;
                validBands++;
            }
        }

        if (validBands < 6) {
            resetCandidate();
            return empty(timestampMs);
        }

        float[] fit = linearFit(centers, medians, valid);
        float[] residual = new float[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            residual[i] = valid[i] ? medians[i] - (fit[0] + fit[1] * centers[i]) : Float.NaN;
        }

        int strongestBoundary = -1;
        float strongestJump = 0f;
        int strongJumpCount = 0;
        for (int i = 0; i + 1 < BAND_COUNT; i++) {
            if (!valid[i] || !valid[i + 1]) continue;
            float jump = Math.abs(residual[i + 1] - residual[i]);
            if (jump > strongestJump) {
                strongestJump = jump;
                strongestBoundary = i;
            }
            if (jump >= 360f) strongJumpCount++;
        }

        if (strongestBoundary < 0) {
            resetCandidate();
            return empty(timestampMs);
        }

        float upperResidual = meanResidual(residual, valid, 0, strongestBoundary + 1);
        float lowerResidual = meanResidual(residual, valid, strongestBoundary + 1, BAND_COUNT);
        float signedSeparation = upperResidual - lowerResidual;
        float separationMm = Math.abs(signedSeparation);

        float coverage = expected <= 0 ? 0f : validSamples / (float) expected;
        float coverageScore = clamp01((coverage - 0.30f) / 0.55f);
        float bandScore = validBands / (float) BAND_COUNT;
        float depthConfidence = clamp01(0.56f * coverageScore + 0.44f * bandScore);
        float boundaryScore = clamp01((strongestJump - 260f) / 1150f);
        float residualScore = clamp01((separationMm - 220f) / 1050f);
        float multiLevelScore = clamp01((strongJumpCount - 1f) / 2f)
                * clamp01(0.55f * boundaryScore + 0.45f * residualScore);
        float candidateScore = clamp01((0.52f * boundaryScore + 0.48f * residualScore)
                * (0.58f + 0.42f * depthConfidence));

        LevelChangeKind kind = LevelChangeKind.UNKNOWN;
        if (strongJumpCount >= 2 && multiLevelScore >= 0.52f && candidateScore >= 0.52f) {
            kind = LevelChangeKind.MULTI_LEVEL_CANDIDATE;
        } else if (candidateScore >= 0.50f && separationMm >= 420f) {
            // Upper image bands represent the farther part of the forward corridor after upright
            // alignment. A farther-than-trend upper side is a DOWNWARD candidate; a nearer-than-
            // trend upper side is an UPWARD candidate. These remain hypotheses until field tests.
            kind = signedSeparation > 0f
                    ? LevelChangeKind.DOWNWARD_CANDIDATE
                    : LevelChangeKind.UPWARD_CANDIDATE;
        }

        float persistence = updatePersistence(kind, candidateScore, depthConfidence, timestampMs);
        float boundaryY = (y0 + (y1 - y0) * (strongestBoundary + 1f) / BAND_COUNT)
                / Math.max(1f, height - 1f);

        return new LevelChangeObservation(
                kind,
                candidateScore,
                boundaryScore,
                residualScore,
                multiLevelScore,
                persistence,
                depthConfidence,
                clamp01(boundaryY),
                timestampMs);
    }

    public void reset() { resetCandidate(); }

    private float updatePersistence(
            LevelChangeKind kind, float candidateScore, float confidence, long timestampMs) {
        if (kind == LevelChangeKind.UNKNOWN || candidateScore < 0.50f || confidence < 0.42f) {
            resetCandidate();
            return 0f;
        }
        if (kind == lastKind && lastCandidateMs > 0L
                && timestampMs >= lastCandidateMs
                && timestampMs - lastCandidateMs <= SAME_KIND_GAP_MS) {
            sameKindCount++;
        } else {
            lastKind = kind;
            sameKindCount = 1;
            firstSameKindMs = timestampMs;
        }
        lastCandidateMs = timestampMs;
        float countScore = clamp01((sameKindCount - 1f) / 4f);
        float timeScore = clamp01((timestampMs - firstSameKindMs) / 800f);
        return Math.min(countScore, timeScore);
    }

    private void resetCandidate() {
        lastKind = LevelChangeKind.UNKNOWN;
        sameKindCount = 0;
        firstSameKindMs = 0L;
        lastCandidateMs = 0L;
    }

    private static float[] linearFit(float[] x, float[] y, boolean[] valid) {
        float sx = 0f, sy = 0f, sxx = 0f, sxy = 0f;
        int n = 0;
        for (int i = 0; i < x.length; i++) {
            if (!valid[i]) continue;
            sx += x[i];
            sy += y[i];
            sxx += x[i] * x[i];
            sxy += x[i] * y[i];
            n++;
        }
        float denominator = n * sxx - sx * sx;
        if (n < 2 || Math.abs(denominator) < 1e-5f) return new float[]{sy / Math.max(1, n), 0f};
        float slope = (n * sxy - sx * sy) / denominator;
        float intercept = (sy - slope * sx) / n;
        return new float[]{intercept, slope};
    }

    private static float meanResidual(float[] residual, boolean[] valid, int start, int end) {
        float sum = 0f;
        int count = 0;
        for (int i = Math.max(0, start); i < Math.min(end, residual.length); i++) {
            if (!valid[i] || Float.isNaN(residual[i])) continue;
            sum += residual[i];
            count++;
        }
        return count == 0 ? 0f : sum / count;
    }

    private static float median(int[] values, int count) {
        int[] copy = Arrays.copyOf(values, count);
        Arrays.sort(copy);
        int middle = count / 2;
        if ((count & 1) == 1) return copy[middle];
        return (copy[middle - 1] + copy[middle]) * 0.5f;
    }

    private static LevelChangeObservation empty(long timestampMs) {
        return new LevelChangeObservation(
                LevelChangeKind.UNKNOWN, 0f, 0f, 0f, 0f, 0f, 0f, 0f, timestampMs);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
