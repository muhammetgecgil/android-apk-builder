package com.mgecgil.seslirehber.core;

import android.graphics.Rect;
import android.media.Image;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.mgecgil.seslirehber.core.GuidanceModels.MotionObservation;
import static com.mgecgil.seslirehber.core.GuidanceModels.ObjectObservation;

/**
 * P0 visual fusion analyzer.
 *
 * Motion and object tracking are intentionally kept as separate evidence channels. The ML Kit
 * default detector is used only for generic bounding boxes + tracking IDs; it is NOT treated as
 * a validated semantic navigation model.
 */
public final class VisionFusionAnalyzer implements ImageAnalysis.Analyzer, AutoCloseable {
    public interface Listener {
        void onMotion(MotionObservation observation);
        void onObject(ObjectObservation observation);
        void onVisionError(String message);
    }

    private static final int GRID_W = 48;
    private static final int GRID_H = 72;
    private static final long TRACK_STALE_MS = 2200L;

    private final byte[] previous = new byte[GRID_W * GRID_H];
    private final byte[] current = new byte[GRID_W * GRID_H];
    private final Map<Integer, TrackState> tracks = new ConcurrentHashMap<>();
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final Listener listener;
    private final ObjectDetector objectDetector;
    private boolean havePrevious;

    public VisionFusionAnalyzer(Listener listener) {
        this.listener = listener;
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .build();
        objectDetector = ObjectDetection.getClient(options);
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (!processing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        try {
            emitMotionEvidence(imageProxy);
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                processing.set(false);
                imageProxy.close();
                return;
            }

            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, rotation);
            final int sourceWidth = imageProxy.getWidth();
            final int sourceHeight = imageProxy.getHeight();
            final int uprightWidth = (rotation == 90 || rotation == 270) ? sourceHeight : sourceWidth;
            final int uprightHeight = (rotation == 90 || rotation == 270) ? sourceWidth : sourceHeight;
            final long now = System.currentTimeMillis();

            objectDetector.process(inputImage)
                    .addOnSuccessListener(objects -> emitMostRelevantObject(objects, uprightWidth, uprightHeight, now))
                    .addOnFailureListener(error -> listener.onVisionError("Nesne algılama geçici olarak kullanılamıyor."))
                    .addOnCompleteListener(task -> {
                        pruneTracks(System.currentTimeMillis());
                        processing.set(false);
                        imageProxy.close();
                    });
        } catch (Throwable error) {
            processing.set(false);
            imageProxy.close();
            listener.onVisionError("Görüntü analizi geçici olarak kullanılamıyor.");
        }
    }

    private void emitMotionEvidence(ImageProxy image) {
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ByteBuffer buffer = yPlane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = yPlane.getRowStride();
        int pixelStride = yPlane.getPixelStride();

        for (int gy = 0; gy < GRID_H; gy++) {
            int sy = Math.min(height - 1, gy * height / GRID_H);
            for (int gx = 0; gx < GRID_W; gx++) {
                int sx = Math.min(width - 1, gx * width / GRID_W);
                int index = sy * rowStride + sx * pixelStride;
                current[gy * GRID_W + gx] = buffer.get(index);
            }
        }

        if (!havePrevious) {
            System.arraycopy(current, 0, previous, 0, current.length);
            havePrevious = true;
            return;
        }

        int meanDiff = 0;
        for (int i = 0; i < current.length; i++) {
            meanDiff += Math.abs((current[i] & 0xff) - (previous[i] & 0xff));
        }
        meanDiff /= current.length;
        int threshold = Math.max(16, Math.min(46, meanDiff + 12));

        int changed = 0;
        long sumX = 0;
        long sumY = 0;
        for (int gy = 0; gy < GRID_H; gy++) {
            for (int gx = 0; gx < GRID_W; gx++) {
                int i = gy * GRID_W + gx;
                int diff = Math.abs((current[i] & 0xff) - (previous[i] & 0xff));
                if (diff > threshold) {
                    changed++;
                    sumX += gx;
                    sumY += gy;
                }
            }
        }
        System.arraycopy(current, 0, previous, 0, current.length);

        float area = changed / (float) current.length;
        float cx = changed == 0 ? -1f : (sumX / (float) changed) / (GRID_W - 1f);
        float cy = changed == 0 ? -1f : (sumY / (float) changed) / (GRID_H - 1f);
        float confidence = clamp((area - 0.015f) / 0.18f);
        listener.onMotion(new MotionObservation(area, cx, cy, confidence, System.currentTimeMillis()));
    }

    private void emitMostRelevantObject(List<DetectedObject> objects, int width, int height, long nowMs) {
        if (objects == null || objects.isEmpty() || width <= 0 || height <= 0) return;

        ObjectObservation best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        float frameArea = width * (float) height;

        for (DetectedObject object : objects) {
            Rect box = object.getBoundingBox();
            if (box.width() <= 0 || box.height() <= 0) continue;

            float centerX = clamp(box.exactCenterX() / width);
            float centerY = clamp(box.exactCenterY() / height);
            float areaRatio = clamp((box.width() * (float) box.height()) / frameArea);
            Integer idValue = object.getTrackingId();
            int trackingId = idValue == null ? -1 : idValue;

            float growthPerSecond = 0f;
            int seenCount = 1;
            if (trackingId >= 0) {
                TrackState previousState = tracks.get(trackingId);
                if (previousState != null) {
                    long dtMs = Math.max(1L, nowMs - previousState.timeMs);
                    growthPerSecond = (areaRatio - previousState.areaRatio) * (1000f / dtMs);
                    seenCount = Math.min(20, previousState.seenCount + 1);
                }
                tracks.put(trackingId, new TrackState(areaRatio, nowMs, seenCount));
            }

            float persistenceConfidence = trackingId < 0 ? 0.50f : clamp(0.54f + 0.07f * Math.min(seenCount, 5));
            ObjectObservation observation = new ObjectObservation(
                    centerX,
                    centerY,
                    areaRatio,
                    growthPerSecond,
                    trackingId,
                    persistenceConfidence,
                    nowMs);

            float centerDistance = Math.abs(centerX - 0.5f) * 2f;
            float centerBonus = (1f - clamp(centerDistance)) * 0.18f;
            float approachBonus = Math.max(0f, Math.min(0.30f, growthPerSecond * 1.5f));
            float score = areaRatio + centerBonus + approachBonus;
            if (score > bestScore) {
                bestScore = score;
                best = observation;
            }
        }

        if (best != null) listener.onObject(best);
    }

    private void pruneTracks(long nowMs) {
        tracks.entrySet().removeIf(entry -> nowMs - entry.getValue().timeMs > TRACK_STALE_MS);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    @Override
    public void close() {
        objectDetector.close();
        tracks.clear();
    }

    private static final class TrackState {
        final float areaRatio;
        final long timeMs;
        final int seenCount;

        TrackState(float areaRatio, long timeMs, int seenCount) {
            this.areaRatio = areaRatio;
            this.timeMs = timeMs;
            this.seenCount = seenCount;
        }
    }
}
