package com.mgecgil.seslirehber.core;

import android.graphics.Bitmap;

/** Visual-only ARCore preview bridge. Safety logic never depends on this frame. */
public final class ArCoreVisualFrameContext {
    public record Frame(
            int width,
            int height,
            int[] argb,
            float sourceAspect,
            long timestampMs) {
        public Frame {
            argb = argb == null ? new int[0] : argb.clone();
            sourceAspect = sourceAspect > 0f ? sourceAspect : 9f / 16f;
        }

        @Override public int[] argb() { return argb.clone(); }
    }

    private static final long FRESH_MS = 850L;
    private static Frame latest;

    private ArCoreVisualFrameContext() {}

    public static synchronized void publish(Bitmap bitmap, float sourceAspect, long timestampMs) {
        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) return;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        latest = new Frame(width, height, pixels, sourceAspect, timestampMs);
    }

    public static synchronized Frame latest(long nowMs) {
        Frame value = latest;
        if (value == null || value.timestampMs() <= 0L || nowMs < value.timestampMs()
                || nowMs - value.timestampMs() > FRESH_MS) return null;
        return value;
    }

    public static synchronized void reset() { latest = null; }
}
