package com.mgecgil.seslirehber.core;

/** Process-local visual-only bridge for the latest PIDNet Cityscapes label mask. */
public final class UrbanHudMaskContext {
    public record Frame(
            int width,
            int height,
            byte[] labels,
            float sourceAspect,
            long timestampMs) {
        public Frame {
            labels = labels == null ? new byte[0] : labels.clone();
            sourceAspect = sourceAspect > 0f ? sourceAspect : 9f / 16f;
        }

        @Override public byte[] labels() { return labels.clone(); }
    }

    private static final long FRESH_MS = 3000L;
    private static Frame latest;

    private UrbanHudMaskContext() {}

    public static synchronized void publish(
            int width,
            int height,
            byte[] labels,
            float sourceAspect,
            long timestampMs) {
        if (width <= 0 || height <= 0 || labels == null || labels.length < width * height) return;
        latest = new Frame(width, height, labels, sourceAspect, timestampMs);
    }

    public static synchronized Frame latest(long nowMs) {
        Frame value = latest;
        if (value == null || value.timestampMs() <= 0L || nowMs < value.timestampMs()
                || nowMs - value.timestampMs() > FRESH_MS) return null;
        return value;
    }

    public static synchronized void reset() { latest = null; }
}
