package com.mgecgil.seslirehber.core;

import android.media.Image;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static com.mgecgil.seslirehber.core.GuidanceModels.DepthObservation;
import static com.mgecgil.seslirehber.core.GuidanceModels.WalkableCorridorObservation;

/**
 * Decodes ARCore Depth16. The live path maps upright CPU-camera coordinates through ARCore's
 * IMAGE_PIXELS -> TEXTURE_NORMALIZED transform because the depth image may be a crop of the CPU
 * camera image and can have a different aspect ratio.
 */
public final class DepthImageAdapter {
    public record AlignedEvidence(
            DepthObservation depth,
            WalkableCorridorObservation walkable) {}

    private static final int GRID_W = 72;
    private static final int GRID_H = 96;
    private final DepthGeometryEstimator estimator = new DepthGeometryEstimator();
    private final WalkableCorridorEstimator walkableEstimator = new WalkableCorridorEstimator();

    /** Legacy/direct decoder used by pure image adapters and compatibility tests. */
    public DepthObservation analyze(Image image, long timestampMs) {
        if (image == null || image.getPlanes().length == 0) {
            return estimator.analyze(new short[0], 0, 0, timestampMs);
        }
        short[] grid = new short[Math.max(8, Math.min(96, image.getWidth()))
                * Math.max(8, Math.min(72, image.getHeight()))];
        int width = Math.max(8, Math.min(96, image.getWidth()));
        int height = Math.max(8, Math.min(72, image.getHeight()));
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer().duplicate().order(ByteOrder.nativeOrder());
        for (int y = 0; y < height; y++) {
            int sy = Math.min(image.getHeight() - 1, y * image.getHeight() / height);
            for (int x = 0; x < width; x++) {
                int sx = Math.min(image.getWidth() - 1, x * image.getWidth() / width);
                grid[y * width + x] = readDepth(plane, buffer, sx, sy);
            }
        }
        return estimator.analyze(grid, width, height, timestampMs);
    }

    /** Compatibility method returning the depth part of the aligned evidence. */
    public DepthObservation analyzeAligned(
            Frame frame,
            Image depthImage,
            int cpuWidth,
            int cpuHeight,
            int rotationDegrees,
            long timestampMs) {
        return analyzeAlignedEvidence(
                frame, depthImage, cpuWidth, cpuHeight, rotationDegrees, timestampMs).depth();
    }

    /**
     * Produces a single upright CPU-camera-aligned depth grid, then derives both depth-discontinuity
     * and relative three-lane openness from exactly the same pixels/timestamp.
     */
    public AlignedEvidence analyzeAlignedEvidence(
            Frame frame,
            Image depthImage,
            int cpuWidth,
            int cpuHeight,
            int rotationDegrees,
            long timestampMs) {
        if (frame == null || depthImage == null || depthImage.getPlanes().length == 0
                || cpuWidth <= 0 || cpuHeight <= 0) {
            short[] empty = new short[0];
            WalkableCorridorObservation walkable = walkableEstimator.analyze(empty, 0, 0, timestampMs);
            PerceptionContext.noteWalkable(walkable);
            return new AlignedEvidence(
                    estimator.analyze(empty, 0, 0, timestampMs),
                    walkable);
        }

        short[] grid = new short[GRID_W * GRID_H];
        Image.Plane plane = depthImage.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer().duplicate().order(ByteOrder.nativeOrder());
        float[] cpuPoint = new float[2];
        float[] depthUv = new float[2];

        for (int gy = 0; gy < GRID_H; gy++) {
            float uy = gy / (float) (GRID_H - 1);
            for (int gx = 0; gx < GRID_W; gx++) {
                float ux = gx / (float) (GRID_W - 1);
                float[] raw = uprightToRawNormalized(ux, uy, rotationDegrees);
                cpuPoint[0] = raw[0] * (cpuWidth - 1f);
                cpuPoint[1] = raw[1] * (cpuHeight - 1f);
                frame.transformCoordinates2d(
                        Coordinates2d.IMAGE_PIXELS,
                        cpuPoint,
                        Coordinates2d.TEXTURE_NORMALIZED,
                        depthUv);

                if (depthUv[0] < 0f || depthUv[1] < 0f || depthUv[0] > 1f || depthUv[1] > 1f) {
                    continue;
                }
                int dx = Math.min(depthImage.getWidth() - 1,
                        Math.max(0, Math.round(depthUv[0] * (depthImage.getWidth() - 1f))));
                int dy = Math.min(depthImage.getHeight() - 1,
                        Math.max(0, Math.round(depthUv[1] * (depthImage.getHeight() - 1f))));
                grid[gy * GRID_W + gx] = readDepth(plane, buffer, dx, dy);
            }
        }
        WalkableCorridorObservation walkable =
                walkableEstimator.analyze(grid, GRID_W, GRID_H, timestampMs);
        PerceptionContext.noteWalkable(walkable);
        return new AlignedEvidence(
                estimator.analyze(grid, GRID_W, GRID_H, timestampMs),
                walkable);
    }

    public void reset() {
        walkableEstimator.reset();
    }

    static float[] uprightToRawNormalized(float ux, float uy, int rotationDegrees) {
        return switch (rotationDegrees) {
            case 90 -> new float[]{uy, 1f - ux};
            case 180 -> new float[]{1f - ux, 1f - uy};
            case 270 -> new float[]{1f - uy, ux};
            default -> new float[]{ux, uy};
        };
    }

    private static short readDepth(Image.Plane plane, ByteBuffer buffer, int x, int y) {
        int offset = y * plane.getRowStride() + x * plane.getPixelStride();
        if (offset < 0 || offset + 1 >= buffer.limit()) return 0;
        return buffer.getShort(offset);
    }
}
