package com.mgecgil.seslirehber.core;

import android.media.Image;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static com.mgecgil.seslirehber.core.GuidanceModels.DepthObservation;

/** Decodes ARCore 16-bit depth images into an upright grid for the geometry estimator. */
public final class DepthImageAdapter {
    private static final int MAX_UPRIGHT_W = 72;
    private static final int MAX_UPRIGHT_H = 96;
    private final DepthGeometryEstimator estimator = new DepthGeometryEstimator();

    public DepthObservation analyze(Image image, long timestampMs) {
        return analyze(image, 0, timestampMs);
    }

    public DepthObservation analyze(Image image, int rotationDegrees, long timestampMs) {
        if (image == null || image.getPlanes().length == 0) {
            return estimator.analyze(new short[0], 0, 0, timestampMs);
        }

        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        boolean quarterTurn = rotationDegrees == 90 || rotationDegrees == 270;
        int uprightSourceWidth = quarterTurn ? sourceHeight : sourceWidth;
        int uprightSourceHeight = quarterTurn ? sourceWidth : sourceHeight;
        int gridWidth = Math.max(8, Math.min(MAX_UPRIGHT_W, uprightSourceWidth));
        int gridHeight = Math.max(8, Math.min(MAX_UPRIGHT_H, uprightSourceHeight));
        short[] depth = new short[gridWidth * gridHeight];

        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();

        for (int gy = 0; gy < gridHeight; gy++) {
            float uy = gridHeight == 1 ? 0f : gy / (float) (gridHeight - 1);
            for (int gx = 0; gx < gridWidth; gx++) {
                float ux = gridWidth == 1 ? 0f : gx / (float) (gridWidth - 1);
                float rx;
                float ry;
                switch (rotationDegrees) {
                    case 90 -> {
                        rx = uy;
                        ry = 1f - ux;
                    }
                    case 180 -> {
                        rx = 1f - ux;
                        ry = 1f - uy;
                    }
                    case 270 -> {
                        rx = 1f - uy;
                        ry = ux;
                    }
                    default -> {
                        rx = ux;
                        ry = uy;
                    }
                }
                int sx = Math.min(sourceWidth - 1, Math.max(0, Math.round(rx * (sourceWidth - 1))));
                int sy = Math.min(sourceHeight - 1, Math.max(0, Math.round(ry * (sourceHeight - 1))));
                int offset = sy * rowStride + sx * pixelStride;
                if (offset < 0 || offset + 1 >= buffer.limit()) continue;
                depth[gy * gridWidth + gx] = buffer.getShort(offset);
            }
        }
        return estimator.analyze(depth, gridWidth, gridHeight, timestampMs);
    }
}
