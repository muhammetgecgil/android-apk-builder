package com.mgecgil.seslirehber.core;

import android.graphics.Bitmap;
import android.media.Image;
import java.nio.ByteBuffer;

/** Downsamples YUV_420_888 to an upright RGB bitmap without changing camera zoom. */
public final class UprightYuvBitmapExtractor {
    private UprightYuvBitmapExtractor() {}

    public static Bitmap extract(Image image, int rotationDegrees, int outWidth, int outHeight) {
        if (image == null || image.getPlanes().length < 3 || outWidth <= 0 || outHeight <= 0) return null;
        Image.Plane yp = image.getPlanes()[0];
        Image.Plane up = image.getPlanes()[1];
        Image.Plane vp = image.getPlanes()[2];
        ByteBuffer yb = yp.getBuffer().duplicate();
        ByteBuffer ub = up.getBuffer().duplicate();
        ByteBuffer vb = vp.getBuffer().duplicate();
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[outWidth * outHeight];

        for (int oy = 0; oy < outHeight; oy++) {
            float uy = (oy + 0.5f) / outHeight;
            for (int ox = 0; ox < outWidth; ox++) {
                float ux = (ox + 0.5f) / outWidth;
                float[] raw = DepthImageAdapter.uprightToRawNormalized(ux, uy, rotationDegrees);
                int sx = clamp(Math.round(raw[0] * (width - 1f)), 0, width - 1);
                int sy = clamp(Math.round(raw[1] * (height - 1f)), 0, height - 1);

                int yIndex = sy * yp.getRowStride() + sx * yp.getPixelStride();
                int cx = sx / 2;
                int cy = sy / 2;
                int uIndex = cy * up.getRowStride() + cx * up.getPixelStride();
                int vIndex = cy * vp.getRowStride() + cx * vp.getPixelStride();
                int y = safeGet(yb, yIndex, 16);
                int u = safeGet(ub, uIndex, 128);
                int v = safeGet(vb, vIndex, 128);
                pixels[oy * outWidth + ox] = yuvToArgb(y, u, v);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, outWidth, 0, 0, outWidth, outHeight);
        return bitmap;
    }

    static int yuvToArgb(int y, int u, int v) {
        int c = Math.max(0, y - 16);
        int d = u - 128;
        int e = v - 128;
        int r = clamp((298 * c + 409 * e + 128) >> 8, 0, 255);
        int g = clamp((298 * c - 100 * d - 208 * e + 128) >> 8, 0, 255);
        int b = clamp((298 * c + 516 * d + 128) >> 8, 0, 255);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private static int safeGet(ByteBuffer b, int index, int fallback) {
        return index >= 0 && index < b.limit() ? (b.get(index) & 0xff) : fallback;
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
