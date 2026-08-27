package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Shared low-cost luma-grid evidence core used by CameraX and ARCore camera paths.
 * The input grid is expected to be row-major, sensor/native orientation.
 */
public final class GridEvidenceEstimator {
    public record Result(
            MotionObservation motion,
            GroundObservation ground,
            SceneHealthObservation sceneHealth,
            boolean primed) {}

    private final int width;
    private final int height;
    private final byte[] previous;
    private final GroundContinuityEstimator groundEstimator = new GroundContinuityEstimator();
    private final SceneHealthEstimator sceneHealthEstimator = new SceneHealthEstimator();
    private boolean havePrevious;

    public GridEvidenceEstimator(int width, int height) {
        if (width < 8 || height < 8) throw new IllegalArgumentException("grid too small");
        this.width = width;
        this.height = height;
        this.previous = new byte[width * height];
    }

    public synchronized Result analyze(byte[] current, int rotationDegrees, long timestampMs) {
        if (current == null || current.length < width * height) {
            return new Result(
                    new MotionObservation(0f, -1f, -1f, 0f, timestampMs),
                    new GroundObservation(0f, 0f, 0f, 0f, 0f, 0f, -1f, timestampMs),
                    new SceneHealthObservation(0f, 0f, 0f, 0f, 0f, 0f, 0f, timestampMs),
                    havePrevious);
        }

        SceneHealthObservation sceneHealth = sceneHealthEstimator.analyze(current, timestampMs);
        PerceptionContext.noteSceneHealth(sceneHealth);
        GroundObservation ground = groundEstimator.estimate(
                current, previous, width, height, rotationDegrees, havePrevious, timestampMs);

        if (!havePrevious) {
            System.arraycopy(current, 0, previous, 0, previous.length);
            havePrevious = true;
            return new Result(
                    new MotionObservation(0f, -1f, -1f, 0f, timestampMs),
                    ground,
                    sceneHealth,
                    true);
        }

        int meanDiff = 0;
        for (int i = 0; i < previous.length; i++) {
            meanDiff += Math.abs((current[i] & 0xff) - (previous[i] & 0xff));
        }
        meanDiff /= previous.length;
        int threshold = Math.max(16, Math.min(46, meanDiff + 12));

        int changed = 0;
        long sumX = 0L;
        long sumY = 0L;
        for (int gy = 0; gy < height; gy++) {
            for (int gx = 0; gx < width; gx++) {
                int i = gy * width + gx;
                int diff = Math.abs((current[i] & 0xff) - (previous[i] & 0xff));
                if (diff > threshold) {
                    changed++;
                    sumX += gx;
                    sumY += gy;
                }
            }
        }
        System.arraycopy(current, 0, previous, 0, previous.length);

        float area = changed / (float) previous.length;
        float rawX = changed == 0 ? -1f : (sumX / (float) changed) / (width - 1f);
        float rawY = changed == 0 ? -1f : (sumY / (float) changed) / (height - 1f);
        float[] upright = rotateNormalized(rawX, rawY, rotationDegrees);
        float confidence = clamp((area - 0.015f) / 0.18f);
        if (area > 0.58f) confidence *= 0.40f;
        confidence *= (0.35f + 0.65f * sceneHealth.qualityScore());

        return new Result(
                new MotionObservation(area, upright[0], upright[1], confidence, timestampMs),
                ground,
                sceneHealth,
                true);
    }

    public synchronized void reset() {
        havePrevious = false;
        java.util.Arrays.fill(previous, (byte) 0);
        groundEstimator.reset();
        sceneHealthEstimator.reset();
    }

    static float[] rotateNormalized(float x, float y, int rotationDegrees) {
        if (x < 0f || y < 0f) return new float[]{x, y};
        return switch (rotationDegrees) {
            case 90 -> new float[]{clamp(1f - y), clamp(x)};
            case 180 -> new float[]{clamp(1f - x), clamp(1f - y)};
            case 270 -> new float[]{clamp(y), clamp(1f - x)};
            default -> new float[]{clamp(x), clamp(y)};
        };
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
