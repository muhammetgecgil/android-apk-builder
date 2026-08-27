package com.mgecgil.seslirehber.core;

import java.util.Arrays;
import static com.mgecgil.seslirehber.core.GuidanceModels.DepthObservation;

/**
 * Conservative geometry estimator for ARCore Depth16-style millimetre samples.
 *
 * It intentionally does not classify holes, curbs or steps. It only measures whether the
 * lower-centre walking corridor contains a persistent-sized depth discontinuity that can be fused
 * with an independent ground-continuity channel.
 */
public final class DepthGeometryEstimator {
    private static final int BAND_COUNT = 6;
    private static final int MIN_MM = 150;
    private static final int MAX_WALKING_MM = 12000;

    public DepthObservation analyze(short[] depthMm, int width, int height, long timestampMs) {
        if (depthMm == null || width < 8 || height < 8 || depthMm.length < width * height) {
            return empty(timestampMs);
        }

        int x0 = Math.max(0, Math.round(width * 0.30f));
        int x1 = Math.min(width, Math.round(width * 0.70f));
        int y0 = Math.max(0, Math.round(height * 0.42f));
        int y1 = Math.min(height, Math.round(height * 0.92f));
        if (x1 <= x0 || y1 <= y0) return empty(timestampMs);

        float[] bandMedians = new float[BAND_COUNT];
        Arrays.fill(bandMedians, Float.NaN);
        int totalExpected = 0;
        int totalValid = 0;
        int validBands = 0;

        int[] corridorValues = new int[(x1 - x0) * (y1 - y0)];
        int corridorCount = 0;

        for (int band = 0; band < BAND_COUNT; band++) {
            int by0 = y0 + (y1 - y0) * band / BAND_COUNT;
            int by1 = y0 + (y1 - y0) * (band + 1) / BAND_COUNT;
            int strideX = Math.max(1, (x1 - x0) / 28);
            int strideY = Math.max(1, (by1 - by0) / 10);
            int capacity = Math.max(1, ((x1 - x0) / strideX + 1) * ((by1 - by0) / strideY + 1));
            int[] values = new int[capacity];
            int count = 0;

            for (int y = by0; y < by1; y += strideY) {
                for (int x = x0; x < x1; x += strideX) {
                    totalExpected++;
                    int mm = depthMm[y * width + x] & 0xffff;
                    if (mm < MIN_MM || mm > MAX_WALKING_MM) continue;
                    totalValid++;
                    if (count < values.length) values[count++] = mm;
                    if (corridorCount < corridorValues.length) corridorValues[corridorCount++] = mm;
                }
            }

            if (count >= 5) {
                bandMedians[band] = median(values, count);
                validBands++;
            }
        }

        float validRatio = totalExpected == 0 ? 0f : totalValid / (float) totalExpected;
        float centerMedian = corridorCount < 5 ? 0f : median(corridorValues, corridorCount);
        float farMedian = medianBands(bandMedians, 0, 2);
        float nearMedian = medianBands(bandMedians, BAND_COUNT - 2, BAND_COUNT);

        float maxJump = 0f;
        for (int i = 0; i + 1 < BAND_COUNT; i++) {
            float a = bandMedians[i];
            float b = bandMedians[i + 1];
            if (Float.isNaN(a) || Float.isNaN(b)) continue;
            maxJump = Math.max(maxJump, Math.abs(a - b));
        }

        float coverage = clamp01((validRatio - 0.18f) / 0.62f);
        float bandCoverage = validBands / (float) BAND_COUNT;
        float confidence = clamp01(0.62f * coverage + 0.38f * bandCoverage);

        // Adjacent-band jumps below ~0.7 m are deliberately ignored. Normal perspective on a flat
        // path can create large near/far differences, so only abrupt local discontinuity matters.
        float jumpScore = clamp01((maxJump - 700f) / 1800f);
        float discontinuity = clamp01(jumpScore * (0.55f + 0.45f * confidence));

        return new DepthObservation(
                validRatio,
                centerMedian,
                nearMedian,
                farMedian,
                maxJump,
                discontinuity,
                confidence,
                timestampMs);
    }

    private static DepthObservation empty(long timestampMs) {
        return new DepthObservation(0f, 0f, 0f, 0f, 0f, 0f, 0f, timestampMs);
    }

    private static float median(int[] values, int count) {
        int[] copy = Arrays.copyOf(values, count);
        Arrays.sort(copy);
        int middle = count / 2;
        if ((count & 1) == 1) return copy[middle];
        return (copy[middle - 1] + copy[middle]) * 0.5f;
    }

    private static float medianBands(float[] bands, int start, int end) {
        float[] temp = new float[Math.max(1, end - start)];
        int count = 0;
        for (int i = start; i < end && i < bands.length; i++) {
            if (!Float.isNaN(bands[i])) temp[count++] = bands[i];
        }
        if (count == 0) return 0f;
        Arrays.sort(temp, 0, count);
        if ((count & 1) == 1) return temp[count / 2];
        return (temp[count / 2 - 1] + temp[count / 2]) * 0.5f;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
