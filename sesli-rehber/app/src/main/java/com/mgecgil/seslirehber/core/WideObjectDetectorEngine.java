package com.mgecgil.seslirehber.core;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.Image;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult;
import com.mgecgil.seslirehber.SesliRehberApplication;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Broad on-device named-object detector (EfficientDet-Lite0 / COCO). It is advisory only and runs
 * off the collision/SafetyGate loop. Existing geometry/Depth remain authoritative for STOP/CAUTION.
 */
public final class WideObjectDetectorEngine implements AutoCloseable {
    private static final String MODEL = "efficientdet_lite0.tflite";
    private static final int INPUT = 320;
    private static final long MIN_INTERVAL_MS = 520L;
    private static final float SCORE_THRESHOLD = 0.45f;
    private static final int MAX_RESULTS = 12;
    private static final long GLOBAL_SPEECH_GAP_MS = 2600L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final WideObjectTracker tracker = new WideObjectTracker();
    private volatile ObjectDetector detector;
    private volatile boolean closed;
    private volatile long nextScanMs;
    private volatile long lastGlobalSpeechMs;

    public WideObjectDetectorEngine() {
        executor.execute(this::ensureDetector);
    }

    public void maybeAnalyze(Image image, int rotationDegrees, long nowMs) {
        if (closed || image == null || nowMs < nextScanMs || !busy.compareAndSet(false, true)) return;
        nextScanMs = nowMs + MIN_INTERVAL_MS;
        Bitmap bitmap;
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
        executor.execute(() -> detect(bitmap, nowMs));
    }

    private void detect(Bitmap bitmap, long sourceTimestampMs) {
        try {
            ObjectDetector local = ensureDetector();
            if (local == null || closed) return;
            MPImage image = new BitmapImageBuilder(bitmap).build();
            ObjectDetectorResult result = local.detect(image);
            if (result == null || result.detections() == null) return;

            WideObjectObservation bestAnnouncement = null;
            float bestAnnouncementScore = -1f;
            for (Detection detection : result.detections()) {
                if (detection == null || detection.categories() == null || detection.categories().isEmpty()) continue;
                Category best = detection.categories().get(0);
                if (best == null || best.score() < SCORE_THRESHOLD) continue;
                String label = WideObjectPolicy.toTurkish(best.categoryName());
                if (label.isEmpty()) continue;
                RectF box = detection.boundingBox();
                if (box == null || box.width() <= 0f || box.height() <= 0f) continue;
                float left = clamp(box.left / INPUT);
                float top = clamp(box.top / INPUT);
                float right = clamp(box.right / INPUT);
                float bottom = clamp(box.bottom / INPUT);
                WideObjectTracker.Result tracked = tracker.observe(
                        label, best.score(), left, top, right, bottom, sourceTimestampMs);
                if (tracked == null || tracked.observation() == null) continue;
                WideObjectObservation observation = tracked.observation();
                WideObjectContext.note(observation);
                if (tracked.announce()) {
                    float center = 1f - Math.min(1f, Math.abs((left + right) * 0.5f - 0.5f) * 2f);
                    float salience = observation.confidence()
                            + (observation.important() ? 0.28f : 0f)
                            + Math.min(0.16f, observation.areaRatio())
                            + center * 0.05f;
                    if (salience > bestAnnouncementScore) {
                        bestAnnouncementScore = salience;
                        bestAnnouncement = observation;
                    }
                }
            }

            if (bestAnnouncement != null
                    && sourceTimestampMs - lastGlobalSpeechMs >= GLOBAL_SPEECH_GAP_MS) {
                String speech = speech(bestAnnouncement);
                if (!speech.isEmpty()) {
                    lastGlobalSpeechMs = sourceTimestampMs;
                    GuidanceSpeaker.speakObjectRecognition(speech);
                }
            }
        } catch (Throwable ignored) {
            // Broad identity is advisory. Never interrupt camera/depth safety on model/runtime failure.
        } finally {
            try { bitmap.recycle(); } catch (Throwable ignored) {}
            busy.set(false);
        }
    }

    private synchronized ObjectDetector ensureDetector() {
        if (closed) return null;
        if (detector != null) return detector;
        try {
            BaseOptions base = BaseOptions.builder().setModelAssetPath(MODEL).build();
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setBaseOptions(base)
                    .setRunningMode(RunningMode.IMAGE)
                    .setMaxResults(MAX_RESULTS)
                    .setScoreThreshold(SCORE_THRESHOLD)
                    .build();
            detector = ObjectDetector.createFromOptions(SesliRehberApplication.appContext(), options);
        } catch (Throwable ignored) {
            detector = null;
        }
        return detector;
    }

    static String speech(WideObjectObservation o) {
        if (o == null || !o.usable()) return "";
        String where = switch (o.direction()) {
            case LEFT -> "Solda";
            case RIGHT -> "Sağda";
            default -> "Önde";
        };
        if (o.definite()) return where + " " + o.label() + " var.";
        return where + " " + o.label() + " olabilir. Güven yüzde " + Math.round(o.confidence() * 100f) + ".";
    }

    public void reset() {
        nextScanMs = 0L;
        lastGlobalSpeechMs = 0L;
        tracker.reset();
        WideObjectContext.reset();
    }

    @Override public synchronized void close() {
        closed = true;
        executor.shutdownNow();
        tracker.reset();
        if (detector != null) {
            try { detector.close(); } catch (Throwable ignored) {}
            detector = null;
        }
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
