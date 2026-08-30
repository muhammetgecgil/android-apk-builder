package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.GroundObservation;

/**
 * Lightweight lower-frame ground continuity estimator.
 *
 * It searches the upright lower-center walking corridor for a broad horizontal appearance
 * discontinuity and tracks whether that evidence persists. It is deliberately conservative:
 * it does not classify holes, curbs or steps and it does not estimate physical distance.
 */
public final class GroundContinuityEstimator {
    private static final float EMA_ALPHA = 0.34f;
    private float smoothedAnomaly;
    private int suspiciousStreak;

    public GroundObservation estimate(
            byte[] current,
            byte[] previous,
            int rawWidth,
            int rawHeight,
            int rotationDegrees,
            boolean havePrevious,
            long timestampMs) {
        if (current == null || current.length < rawWidth * rawHeight || rawWidth < 8 || rawHeight < 8) {
            return quiet(timestampMs);
        }

        final int samplesX = 20;
        final int samplesY = 26;
        final float x0 = 0.24f;
        final float x1 = 0.76f;
        final float y0 = 0.43f;
        final float y1 = 0.95f;

        float[][] values = new float[samplesY][samplesX];
        float mean = 0f;
        float horizontalTexture = 0f;
        int textureCount = 0;

        for (int y = 0; y < samplesY; y++) {
            float ny = lerp(y0, y1, y / (float) (samplesY - 1));
            for (int x = 0; x < samplesX; x++) {
                float nx = lerp(x0, x1, x / (float) (samplesX - 1));
                float value = sampleUpright(current, rawWidth, rawHeight, nx, ny, rotationDegrees);
                values[y][x] = value;
                mean += value;
                if (x > 0) {
                    horizontalTexture += Math.abs(value - values[y][x - 1]);
                    textureCount++;
                }
            }
        }
        mean /= samplesX * samplesY;
        horizontalTexture = textureCount == 0 ? 0f : horizontalTexture / textureCount;

        float bestRowDiff = 0f;
        float bestSupport = 0f;
        int bestRow = -1;
        float rowDiffMean = 0f;
        int rowCount = 0;

        for (int y = 2; y < samplesY - 2; y++) {
            float diffSum = 0f;
            int supported = 0;
            for (int x = 0; x < samplesX; x++) {
                float d = Math.abs(values[y][x] - values[y - 1][x]);
                diffSum += d;
                if (d >= 18f) supported++;
            }
            float rowDiff = diffSum / samplesX;
            float support = supported / (float) samplesX;
            rowDiffMean += rowDiff;
            rowCount++;
            float score = rowDiff * (0.58f + 0.42f * support);
            if (score > bestRowDiff) {
                bestRowDiff = score;
                bestSupport = support;
                bestRow = y;
            }
        }
        rowDiffMean = rowCount == 0 ? 0f : rowDiffMean / rowCount;

        float normalizedBoundary = clamp01((bestRowDiff - Math.max(9f, rowDiffMean * 1.30f)) / 28f);
        float broadBoundary = clamp01(0.62f * normalizedBoundary + 0.38f * clamp01((bestSupport - 0.28f) / 0.58f));

        float textureAbove = 0f;
        float textureBelow = 0f;
        int aboveCount = 0;
        int belowCount = 0;
        if (bestRow >= 1) {
            for (int y = 0; y < samplesY; y++) {
                for (int x = 1; x < samplesX; x++) {
                    float d = Math.abs(values[y][x] - values[y][x - 1]);
                    if (y < bestRow) {
                        textureAbove += d;
                        aboveCount++;
                    } else {
                        textureBelow += d;
                        belowCount++;
                    }
                }
            }
        }
        textureAbove = aboveCount == 0 ? horizontalTexture : textureAbove / aboveCount;
        textureBelow = belowCount == 0 ? horizontalTexture : textureBelow / belowCount;
        float textureDelta = Math.abs(textureBelow - textureAbove);
        float textureChange = clamp01((textureDelta - 3f) / 20f);

        float temporalChange = 0f;
        if (havePrevious && previous != null && previous.length >= rawWidth * rawHeight) {
            float temporalSum = 0f;
            int temporalCount = 0;
            for (int y = 0; y < samplesY; y += 2) {
                float ny = lerp(y0, y1, y / (float) (samplesY - 1));
                for (int x = 0; x < samplesX; x += 2) {
                    float nx = lerp(x0, x1, x / (float) (samplesX - 1));
                    float a = sampleUpright(current, rawWidth, rawHeight, nx, ny, rotationDegrees);
                    float b = sampleUpright(previous, rawWidth, rawHeight, nx, ny, rotationDegrees);
                    temporalSum += Math.abs(a - b);
                    temporalCount++;
                }
            }
            float meanTemporal = temporalCount == 0 ? 0f : temporalSum / temporalCount;
            temporalChange = clamp01((meanTemporal - 5f) / 32f);
        }

        float exposureConfidence = 1f - clamp01(Math.abs(mean - 128f) / 118f);
        float textureConfidence = clamp01((horizontalTexture - 1.8f) / 10f);
        float viewConfidence = clamp01(0.58f * exposureConfidence + 0.42f * textureConfidence);

        float rawAnomaly = clamp01(
                0.58f * broadBoundary
                + 0.24f * textureChange
                + 0.10f * temporalChange
                + 0.08f * clamp01((horizontalTexture - 2f) / 20f));
        rawAnomaly *= (0.58f + 0.42f * viewConfidence);
        smoothedAnomaly = ema(smoothedAnomaly, rawAnomaly, EMA_ALPHA);

        if (smoothedAnomaly >= 0.56f && viewConfidence >= 0.40f) {
            suspiciousStreak = Math.min(8, suspiciousStreak + 1);
        } else {
            suspiciousStreak = Math.max(0, suspiciousStreak - 1);
        }
        float persistence = clamp01((suspiciousStreak - 1f) / 4f);
        float boundaryY = bestRow < 0 ? -1f : lerp(y0, y1, bestRow / (float) (samplesY - 1));

        return new GroundObservation(
                clamp01(smoothedAnomaly),
                broadBoundary,
                textureChange,
                temporalChange,
                persistence,
                viewConfidence,
                boundaryY,
                timestampMs);
    }

    public void reset() {
        smoothedAnomaly = 0f;
        suspiciousStreak = 0;
    }

    private static GroundObservation quiet(long timestampMs) {
        return new GroundObservation(0f, 0f, 0f, 0f, 0f, 0f, -1f, timestampMs);
    }

    private static float sampleUpright(
            byte[] data,
            int rawWidth,
            int rawHeight,
            float uprightX,
            float uprightY,
            int rotationDegrees) {
        float rx;
        float ry;
        switch (rotationDegrees) {
            case 90 -> {
                rx = uprightY;
                ry = 1f - uprightX;
            }
            case 180 -> {
                rx = 1f - uprightX;
                ry = 1f - uprightY;
            }
            case 270 -> {
                rx = 1f - uprightY;
                ry = uprightX;
            }
            default -> {
                rx = uprightX;
                ry = uprightY;
            }
        }
        int x = Math.min(rawWidth - 1, Math.max(0, Math.round(clamp01(rx) * (rawWidth - 1))));
        int y = Math.min(rawHeight - 1, Math.max(0, Math.round(clamp01(ry) * (rawHeight - 1))));
        return data[y * rawWidth + x] & 0xff;
    }

    private static float ema(float previous, float current, float alpha) {
        return previous * (1f - alpha) + current * alpha;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
