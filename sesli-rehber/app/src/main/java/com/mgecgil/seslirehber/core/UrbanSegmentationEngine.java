package com.mgecgil.seslirehber.core;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.SystemClock;
import com.google.ai.edge.litert.Accelerator;
import com.google.ai.edge.litert.CompiledModel;
import com.google.ai.edge.litert.TensorBuffer;
import com.mgecgil.seslirehber.SesliRehberApplication;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Low-rate Cityscapes urban segmentation using PIDNet-S + LiteRT CompiledModel.
 * It is advisory and deliberately isolated from the SafetyGate frame loop.
 */
public final class UrbanSegmentationEngine implements AutoCloseable {
    private static final int CAPTURE = 512;
    private static final int INPUT = 1024;
    private static final int OUTPUT = 128;
    private static final int CLASSES = 19;
    private static final long GPU_INTERVAL_MS = 850L;
    private static final long CPU_INTERVAL_MS = 2200L;
    private static final String MODEL = "pidnet_s.tflite";
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final UrbanSegmentationLogitAnalyzer analyzer = new UrbanSegmentationLogitAnalyzer();
    private final UrbanSegmentationTemporalFilter temporal = new UrbanSegmentationTemporalFilter();

    private volatile CompiledModel model;
    private volatile List<TensorBuffer> inputBuffers;
    private volatile List<TensorBuffer> outputBuffers;
    private volatile boolean gpuMode;
    private volatile boolean permanentlyUnavailable;
    private volatile boolean closed;
    private volatile long nextScanMs;

    public UrbanSegmentationEngine() {
        executor.execute(this::ensureModel);
    }

    public void maybeAnalyze(Image image, int rotationDegrees, long nowMs) {
        if (closed || permanentlyUnavailable || image == null || nowMs < nextScanMs
                || !busy.compareAndSet(false, true)) return;
        nextScanMs = nowMs + (gpuMode ? GPU_INTERVAL_MS : CPU_INTERVAL_MS);

        Bitmap snapshot;
        float sourceAspect;
        try {
            int uprightWidth = (rotationDegrees == 90 || rotationDegrees == 270)
                    ? image.getHeight() : image.getWidth();
            int uprightHeight = (rotationDegrees == 90 || rotationDegrees == 270)
                    ? image.getWidth() : image.getHeight();
            sourceAspect = uprightHeight <= 0 ? 9f / 16f : uprightWidth / (float) uprightHeight;
            // Keep camera-thread work bounded. Full 1024 preprocessing happens on the urban worker.
            snapshot = UprightYuvBitmapExtractor.extract(image, rotationDegrees, CAPTURE, CAPTURE);
        } catch (Throwable ignored) {
            UrbanValidationTelemetry.noteFailure();
            busy.set(false);
            return;
        }
        if (snapshot == null) {
            UrbanValidationTelemetry.noteFailure();
            busy.set(false);
            return;
        }
        final float aspect = sourceAspect;
        executor.execute(() -> runInference(snapshot, nowMs, aspect));
    }

    private void runInference(Bitmap snapshot, long sourceTimestampMs, float sourceAspect) {
        long started = SystemClock.elapsedRealtime();
        Bitmap modelBitmap = null;
        boolean success = false;
        try {
            if (!ensureModel() || closed) return;
            modelBitmap = snapshot.getWidth() == INPUT && snapshot.getHeight() == INPUT
                    ? snapshot
                    : Bitmap.createScaledBitmap(snapshot, INPUT, INPUT, true);
            float[] input = toNchw(modelBitmap);
            List<TensorBuffer> in = inputBuffers;
            List<TensorBuffer> out = outputBuffers;
            CompiledModel local = model;
            if (local == null || in == null || out == null || in.isEmpty() || out.isEmpty()) return;

            in.get(0).writeFloat(input);
            local.run(in, out, 0);
            float[] logits = out.get(0).readFloat();
            if (logits.length < CLASSES * OUTPUT * OUTPUT) return;

            UrbanSegmentationLogitAnalyzer.Raw raw = analyzer.analyze(logits, OUTPUT, OUTPUT);
            byte[] labels = analyzer.labels(logits, OUTPUT, OUTPUT);
            if (labels.length == OUTPUT * OUTPUT) {
                UrbanHudMaskContext.publish(OUTPUT, OUTPUT, labels, sourceAspect, sourceTimestampMs);
            }
            long inferenceMs = Math.max(0L, SystemClock.elapsedRealtime() - started);
            UrbanSegmentationObservation observation = temporal.update(raw, inferenceMs, sourceTimestampMs);
            if (observation != null) {
                SituationalAwarenessContext.noteUrbanSegmentation(observation);
                UrbanValidationTelemetry.noteSuccess(observation);
                success = true;
            }
        } catch (Throwable ignored) {
            // Urban segmentation is advisory. A runtime failure must not affect safety guidance.
        } finally {
            if (!success) UrbanValidationTelemetry.noteFailure();
            if (modelBitmap != null && modelBitmap != snapshot) {
                try { modelBitmap.recycle(); } catch (Throwable ignored) {}
            }
            try { snapshot.recycle(); } catch (Throwable ignored) {}
            busy.set(false);
        }
    }

    private synchronized boolean ensureModel() {
        if (closed || permanentlyUnavailable) return false;
        if (model != null) return true;

        if (tryCreate(Accelerator.GPU)) {
            gpuMode = true;
            UrbanValidationTelemetry.noteBackend(UrbanValidationTelemetry.Backend.GPU);
            return true;
        }
        if (tryCreate(Accelerator.CPU)) {
            gpuMode = false;
            UrbanValidationTelemetry.noteBackend(UrbanValidationTelemetry.Backend.CPU);
            return true;
        }
        permanentlyUnavailable = true;
        UrbanValidationTelemetry.noteBackend(UrbanValidationTelemetry.Backend.UNAVAILABLE);
        return false;
    }

    private boolean tryCreate(Accelerator accelerator) {
        try {
            CompiledModel.Options options = new CompiledModel.Options(accelerator);
            CompiledModel candidate = CompiledModel.create(
                    SesliRehberApplication.appContext().getAssets(), MODEL, options, null);
            List<TensorBuffer> in = candidate.createInputBuffers(0);
            List<TensorBuffer> out = candidate.createOutputBuffers(0);
            if (in.size() != 1 || out.size() != 1) {
                closeBuffers(in);
                closeBuffers(out);
                candidate.close();
                return false;
            }
            model = candidate;
            inputBuffers = in;
            outputBuffers = out;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static float[] toNchw(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int pixels = width * height;
        int[] argb = new int[pixels];
        bitmap.getPixels(argb, 0, width, 0, 0, width, height);
        float[] out = new float[pixels * 3];
        for (int i = 0; i < pixels; i++) {
            int c = argb[i];
            float r = ((c >> 16) & 0xff) / 255f;
            float g = ((c >> 8) & 0xff) / 255f;
            float b = (c & 0xff) / 255f;
            out[i] = (r - MEAN[0]) / STD[0];
            out[pixels + i] = (g - MEAN[1]) / STD[1];
            out[pixels * 2 + i] = (b - MEAN[2]) / STD[2];
        }
        return out;
    }

    public boolean isGpuMode() { return gpuMode && model != null; }
    public boolean isAvailable() { return model != null && !permanentlyUnavailable; }

    public synchronized void reset() {
        nextScanMs = 0L;
        temporal.reset();
        UrbanHudMaskContext.reset();
    }

    @Override
    public synchronized void close() {
        closed = true;
        executor.shutdownNow();
        temporal.reset();
        UrbanHudMaskContext.reset();
        closeBuffers(inputBuffers);
        closeBuffers(outputBuffers);
        inputBuffers = null;
        outputBuffers = null;
        if (model != null) {
            try { model.close(); } catch (Throwable ignored) {}
            model = null;
        }
    }

    private static void closeBuffers(List<TensorBuffer> buffers) {
        if (buffers == null) return;
        for (TensorBuffer buffer : buffers) {
            if (buffer != null) try { buffer.close(); } catch (Throwable ignored) {}
        }
    }
}
