package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.SceneHealthObservation;

/**
 * Low-cost camera usability monitor for the shared luma grid.
 * It detects persistent darkness, saturation and near-flat/covered frames. A single bad frame is
 * never enough to declare the camera unusable.
 */
public final class SceneHealthEstimator {
    private float smoothedUnusable;
    private int badStreak;

    public SceneHealthObservation analyze(byte[] luma, long timestampMs) {
        if (luma == null || luma.length < 64) return quiet(timestampMs);

        float mean = 0f;
        int dark = 0;
        int bright = 0;
        int[] histogram = new int[16];
        for (byte valueByte : luma) {
            int value = valueByte & 0xff;
            mean += value;
            if (value <= 22) dark++;
            if (value >= 238) bright++;
            histogram[Math.min(15, value >>> 4)]++;
        }
        mean /= luma.length;
        float darkRatio = dark / (float) luma.length;
        float brightRatio = bright / (float) luma.length;

        float variance = 0f;
        for (byte valueByte : luma) {
            float d = (valueByte & 0xff) - mean;
            variance += d * d;
        }
        variance /= luma.length;
        float stddev = (float) Math.sqrt(variance);
        float contrastScore = clamp01((stddev - 4f) / 34f);

        int occupiedBins = 0;
        for (int count : histogram) if (count >= Math.max(1, luma.length / 100)) occupiedBins++;
        float tonalSpread = clamp01((occupiedBins - 2f) / 8f);

        float darkness = clamp01((darkRatio - 0.55f) / 0.40f);
        float saturation = clamp01((brightRatio - 0.58f) / 0.38f);
        float flatness = clamp01((0.24f - contrastScore) / 0.24f);
        float spreadPenalty = clamp01((0.22f - tonalSpread) / 0.22f);

        float rawUnusable = clamp01(Math.max(Math.max(darkness, saturation),
                0.72f * flatness + 0.28f * spreadPenalty));
        smoothedUnusable = smoothedUnusable * 0.66f + rawUnusable * 0.34f;

        if (smoothedUnusable >= 0.64f) badStreak = Math.min(10, badStreak + 1);
        else badStreak = Math.max(0, badStreak - 1);

        float persistence = clamp01((badStreak - 1f) / 4f);
        float quality = clamp01(
                0.46f * (1f - darkness)
                + 0.24f * (1f - saturation)
                + 0.20f * contrastScore
                + 0.10f * tonalSpread);

        return new SceneHealthObservation(
                mean,
                contrastScore,
                darkRatio,
                brightRatio,
                quality,
                smoothedUnusable,
                persistence,
                timestampMs);
    }

    public void reset() {
        smoothedUnusable = 0f;
        badStreak = 0;
    }

    private static SceneHealthObservation quiet(long timestampMs) {
        return new SceneHealthObservation(0f, 0f, 0f, 0f, 0f, 0f, 0f, timestampMs);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
