package com.mgecgil.seslirehber.core;

import android.graphics.Bitmap;
import android.media.Image;
import java.nio.ByteBuffer;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/**
 * Extracts one enlarged upright greyscale tile at a time from the Y plane. Cycling tiles avoids
 * changing camera zoom (which would disturb Depth/ground geometry) while making small distant
 * visual structures occupy a larger fraction of the recognition input.
 */
public final class FarTileExtractor {
    public record Tile(Bitmap bitmap, Direction direction, float zoomFactor, float contrastScore, int slot) {}

    private static final int OUT = 288;
    private static final TileDef[] TILES = new TileDef[]{
            new TileDef(0.30f, 0.70f, 0.10f, 0.58f, Direction.CENTER),
            new TileDef(0.03f, 0.48f, 0.14f, 0.66f, Direction.LEFT),
            new TileDef(0.36f, 0.64f, 0.16f, 0.50f, Direction.CENTER),
            new TileDef(0.52f, 0.97f, 0.14f, 0.66f, Direction.RIGHT)
    };

    private int scanIndex;

    public synchronized Tile extractNext(Image image, int rotationDegrees) {
        if (image == null || image.getPlanes().length == 0 || image.getWidth() < 32 || image.getHeight() < 32) {
            return null;
        }
        int slot = scanIndex++ % TILES.length;
        TileDef def = TILES[slot];
        Image.Plane yPlane = image.getPlanes()[0];
        ByteBuffer buffer = yPlane.getBuffer().duplicate();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = yPlane.getRowStride();
        int pixelStride = yPlane.getPixelStride();
        int[] pixels = new int[OUT * OUT];
        double sum = 0.0;
        double sumSq = 0.0;

        for (int oy = 0; oy < OUT; oy++) {
            float uy = def.y0 + (oy + 0.5f) / OUT * (def.y1 - def.y0);
            for (int ox = 0; ox < OUT; ox++) {
                float ux = def.x0 + (ox + 0.5f) / OUT * (def.x1 - def.x0);
                float[] raw = DepthImageAdapter.uprightToRawNormalized(ux, uy, rotationDegrees);
                int sx = Math.min(width - 1, Math.max(0, Math.round(raw[0] * (width - 1f))));
                int sy = Math.min(height - 1, Math.max(0, Math.round(raw[1] * (height - 1f))));
                int offset = sy * rowStride + sx * pixelStride;
                int y = (offset >= 0 && offset < buffer.limit()) ? (buffer.get(offset) & 0xff) : 0;
                sum += y;
                sumSq += y * (double) y;
                pixels[oy * OUT + ox] = 0xff000000 | (y << 16) | (y << 8) | y;
            }
        }

        double n = OUT * (double) OUT;
        double mean = sum / n;
        double variance = Math.max(0.0, sumSq / n - mean * mean);
        float contrast = clamp01((float) Math.sqrt(variance) / 64f);
        Bitmap bitmap = Bitmap.createBitmap(OUT, OUT, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, OUT, 0, 0, OUT, OUT);
        float zoom = 1f / Math.max(def.x1 - def.x0, def.y1 - def.y0);
        return new Tile(bitmap, def.direction, zoom, contrast, slot);
    }

    public synchronized void reset() { scanIndex = 0; }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private record TileDef(float x0, float x1, float y0, float y1, Direction direction) {}
}
