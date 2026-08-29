package com.mgecgil.seslirehber.core;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.SystemClock;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.ByteBufferExtractor;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter;
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult;
import com.mgecgil.seslirehber.SesliRehberApplication;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Low-rate real pixel-level segmentation path. It is deliberately advisory and independent from
 * the SafetyGate frame loop so heavy inference can never block a STOP decision.
 */
public final class SemanticSegmentationEngine implements AutoCloseable {
    private static final int INPUT = 257;
    private static final long MIN_INTERVAL_MS = 420L;
    private static final String MODEL = "deeplab_v3.tflite";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final SemanticSegmentationMaskAnalyzer maskAnalyzer = new SemanticSegmentationMaskAnalyzer();
    private final SemanticSegmentationTemporalFilter temporal = new SemanticSegmentationTemporalFilter();
    private volatile ImageSegmenter segmenter;
    private volatile boolean closed;
    private volatile long nextScanMs;

    public SemanticSegmentationEngine() {
        executor.execute(this::ensureSegmenter);
    }

    public void maybeAnalyze(Image image, int rotationDegrees, long nowMs) {
        if (closed || image == null || nowMs < nextScanMs || !busy.compareAndSet(false, true)) return;
        nextScanMs = nowMs + MIN_INTERVAL_MS;

        Bitmap bitmap = null;
        try {
            bitmap = UprightYuvBitmapExtractor.extract(image, rotationDegrees, INPUT, INPUT);
        } catch (Throwable ignored) {
            busy.set(false);
            return;
        }
        if (bitmap == null) {
            busy.set(false);
            return;
        }
        final Bitmap owned = bitmap;
        executor.execute(() -> runSegmentation(owned, nowMs));
    }

    private void runSegmentation(Bitmap bitmap, long sourceTimestampMs) {
        long started = SystemClock.elapsedRealtime();
        try {
            ImageSegmenter local = ensureSegmenter();
            if (local == null || closed) return;
            MPImage image = new BitmapImageBuilder(bitmap).build();
            ImageSegmenterResult result = local.segment(image);
            if (result == null || result.categoryMask().isEmpty()) return;
            MPImage maskImage = result.categoryMask().get();
            ByteBuffer mask = ByteBufferExtractor.extract(maskImage);
            SemanticSegmentationMaskAnalyzer.Raw raw = maskAnalyzer.analyze(
                    mask, maskImage.getWidth(), maskImage.getHeight());
            long inferenceMs = Math.max(0L, SystemClock.elapsedRealtime() - started);
            SemanticSegmentationObservation observation = temporal.update(raw, inferenceMs, sourceTimestampMs);
            if (observation != null) SituationalAwarenessHub.note(observation);
        } catch (Throwable ignored) {
            // Segmentation is advisory. Failure must never interrupt camera/depth safety guidance.
        } finally {
            try { bitmap.recycle(); } catch (Throwable ignored) {}
            busy.set(false);
        }
    }

    private synchronized ImageSegmenter ensureSegmenter() {
        if (closed) return null;
        if (segmenter != null) return segmenter;
        try {
            BaseOptions base = BaseOptions.builder()
                    .setModelAssetPath(MODEL)
                    .build();
            ImageSegmenter.ImageSegmenterOptions options = ImageSegmenter.ImageSegmenterOptions.builder()
                    .setBaseOptions(base)
                    .setRunningMode(RunningMode.IMAGE)
                    .setOutputCategoryMask(true)
                    .setOutputConfidenceMasks(false)
                    .build();
            segmenter = ImageSegmenter.createFromOptions(SesliRehberApplication.appContext(), options);
        } catch (Throwable ignored) {
            segmenter = null;
        }
        return segmenter;
    }

    public void reset() {
        nextScanMs = 0L;
        temporal.reset();
    }

    @Override
    public synchronized void close() {
        closed = true;
        executor.shutdownNow();
        temporal.reset();
        if (segmenter != null) {
            try { segmenter.close(); } catch (Throwable ignored) {}
            segmenter = null;
        }
    }
}
