package com.mgecgil.seslirehber.core;

import android.media.Image;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static com.mgecgil.seslirehber.core.GuidanceModels.DepthObservation;

/** Decodes Android/ARCore 16-bit depth images into the pure geometry estimator. */
public final class DepthImageAdapter {
    private static final int MAX_GRID_W = 96;
    private static final int MAX_GRID_H = 72;
    private final DepthGeometryEstimator estimator = new DepthGeometryEstimator();

    public DepthObservation analyze(Image image, long timestampMs) {
        if (image == null || image.getPlanes().length == 0) {
            return estimator.analyze(new short[0], 0, 0, timestampMs);
        }

        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int gridWidth = Math.max(8, Math.min(MAX_GRID_W, sourceWidth));
        int gridHeight = Math.max(8, Math.min(MAX_GRID_H, sourceHeight));
        short[] depth = new short[gridWidth * gridHeight];

        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();

        for (int gy = 0; gy < gridHeight; gy++) {
            int sy = Math.min(sourceHeight - 1, gy * sourceHeight / gridHeight);
            for (int gx = 0; gx < gridWidth; gx++) {
                int sx = Math.min(sourceWidth - 1, gx * sourceWidth / gridWidth);
                int offset = sy * rowStride + sx * pixelStride;
                if (offset < 0 || offset + 1 >= buffer.limit()) continue;
                depth[gy * gridWidth + gx] = buffer.getShort(offset);
            }
        }
        return estimator.analyze(depth, gridWidth, gridHeight, timestampMs);
    }
}
