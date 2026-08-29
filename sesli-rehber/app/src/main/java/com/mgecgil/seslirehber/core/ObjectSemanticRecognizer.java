package com.mgecgil.seslirehber.core;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/**
 * Low-rate crop-level semantic classifier for already detected/tracked objects.
 * Geometry/SafetyGate remains independent; this class only adds advisory identity + speech/HUD labels.
 */
public final class ObjectSemanticRecognizer implements AutoCloseable {
    private static final long SCAN_INTERVAL_MS = 760L;
    private static final int MAX_OBJECTS_PER_SCAN = 2;
    private static final int MAX_BITMAP_DIM = 480;
    private static final float MIN_LABEL_CONFIDENCE = 0.52f;
    private static final float MIN_BOX_AREA = 0.010f;

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final ObjectSemanticTracker tracker = new ObjectSemanticTracker();
    private final ImageLabeler labeler = ImageLabeling.getClient(
            new ImageLabelerOptions.Builder().setConfidenceThreshold(MIN_LABEL_CONFIDENCE).build());
    private volatile long nextScanMs;
    private volatile boolean closed;

    public void maybeAnalyze(
            Image image,
            int rotationDegrees,
            List<DetectedObject> detected,
            int uprightWidth,
            int uprightHeight,
            long nowMs) {
        if (closed || image == null || detected == null || detected.isEmpty()
                || uprightWidth <= 0 || uprightHeight <= 0 || nowMs < nextScanMs
                || !busy.compareAndSet(false, true)) return;
        nextScanMs = nowMs + SCAN_INTERVAL_MS;

        List<DetectedObject> chosen = selectObjects(detected, uprightWidth, uprightHeight);
        if (chosen.isEmpty()) {
            busy.set(false);
            return;
        }

        Bitmap full = null;
        try {
            float scale = MAX_BITMAP_DIM / (float) Math.max(uprightWidth, uprightHeight);
            int outW = Math.max(96, Math.round(uprightWidth * scale));
            int outH = Math.max(96, Math.round(uprightHeight * scale));
            full = UprightYuvBitmapExtractor.extract(image, rotationDegrees, outW, outH);
            if (full == null) {
                busy.set(false);
                return;
            }
            List<Crop> crops = buildCrops(full, chosen, uprightWidth, uprightHeight);
            full.recycle();
            full = null;
            if (crops.isEmpty()) {
                busy.set(false);
                return;
            }
            processNext(crops, 0);
        } catch (Throwable ignored) {
            if (full != null) try { full.recycle(); } catch (Throwable ignored2) {}
            busy.set(false);
        }
    }

    private void processNext(List<Crop> crops, int index) {
        if (closed || index >= crops.size()) {
            recycleFrom(crops, index);
            busy.set(false);
            return;
        }
        Crop crop = crops.get(index);
        labeler.process(InputImage.fromBitmap(crop.bitmap, 0))
                .addOnSuccessListener(labels -> {
                    Candidate candidate = selectCandidate(labels);
                    if (candidate == null) return;
                    ObjectSemanticTracker.Result result = tracker.observe(
                            crop.trackingId,
                            candidate.label,
                            candidate.confidence,
                            crop.direction,
                            System.currentTimeMillis());
                    if (result == null || result.observation() == null) return;
                    ObjectSemanticContext.note(result.observation());
                    if (result.announce()) {
                        String speech = ObjectSemanticSpeech.format(result.observation());
                        if (!speech.isEmpty()) GuidanceSpeaker.speakObjectRecognition(speech);
                    }
                })
                .addOnCompleteListener(task -> {
                    try { crop.bitmap.recycle(); } catch (Throwable ignored) {}
                    processNext(crops, index + 1);
                });
    }

    private static List<DetectedObject> selectObjects(List<DetectedObject> objects, int width, int height) {
        float frameArea = width * (float) height;
        List<Scored> scored = new ArrayList<>();
        for (DetectedObject object : objects) {
            if (object == null || object.getTrackingId() == null) continue;
            Rect b = object.getBoundingBox();
            if (b.width() <= 0 || b.height() <= 0) continue;
            float area = (b.width() * (float) b.height()) / Math.max(1f, frameArea);
            if (area < MIN_BOX_AREA) continue;
            float cx = b.exactCenterX() / Math.max(1f, width);
            float centerBonus = 0.16f * (1f - Math.min(1f, Math.abs(cx - 0.5f) * 2f));
            scored.add(new Scored(object, area + centerBonus));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        List<DetectedObject> out = new ArrayList<>(Math.min(MAX_OBJECTS_PER_SCAN, scored.size()));
        for (int i = 0; i < scored.size() && i < MAX_OBJECTS_PER_SCAN; i++) out.add(scored.get(i).object());
        return out;
    }

    private static List<Crop> buildCrops(
            Bitmap full,
            List<DetectedObject> objects,
            int uprightWidth,
            int uprightHeight) {
        List<Crop> out = new ArrayList<>();
        float sx = full.getWidth() / (float) uprightWidth;
        float sy = full.getHeight() / (float) uprightHeight;
        for (DetectedObject object : objects) {
            Integer id = object.getTrackingId();
            if (id == null || id < 0) continue;
            Rect b = object.getBoundingBox();
            int marginX = Math.max(4, Math.round(b.width() * 0.08f));
            int marginY = Math.max(4, Math.round(b.height() * 0.08f));
            int left = clamp(Math.round((b.left - marginX) * sx), 0, full.getWidth() - 1);
            int top = clamp(Math.round((b.top - marginY) * sy), 0, full.getHeight() - 1);
            int right = clamp(Math.round((b.right + marginX) * sx), left + 1, full.getWidth());
            int bottom = clamp(Math.round((b.bottom + marginY) * sy), top + 1, full.getHeight());
            if (right - left < 24 || bottom - top < 24) continue;
            Bitmap crop = Bitmap.createBitmap(full, left, top, right - left, bottom - top);
            float centerX = b.exactCenterX() / Math.max(1f, uprightWidth);
            Direction direction = centerX < 0.38f ? Direction.LEFT
                    : centerX > 0.62f ? Direction.RIGHT : Direction.CENTER;
            out.add(new Crop(id, direction, crop));
        }
        return out;
    }

    private static Candidate selectCandidate(List<ImageLabel> labels) {
        if (labels == null || labels.isEmpty()) return null;
        Candidate best = null;
        for (ImageLabel label : labels) {
            if (label == null || label.getConfidence() < MIN_LABEL_CONFIDENCE) continue;
            String tr = DistantLabelPolicy.toTurkishObject(label.getText());
            if (tr.isEmpty()) continue;
            float score = label.getConfidence() + specificityBonus(tr);
            if (best == null || score > best.score) best = new Candidate(tr, label.getConfidence(), score);
        }
        return best;
    }

    private static float specificityBonus(String label) {
        return switch (label) {
            case "koltuk", "sandalye", "masa", "yatak", "yastık", "televizyon", "dolap",
                    "insan", "araç", "otobüs", "kamyon", "motosiklet", "bisiklet" -> 0.14f;
            case "kapı", "saat", "lamba", "çöp kutusu", "bank", "bariyer", "direk" -> 0.09f;
            default -> 0f;
        };
    }

    private static void recycleFrom(List<Crop> crops, int index) {
        for (int i = Math.max(0, index); i < crops.size(); i++) {
            try { crops.get(i).bitmap.recycle(); } catch (Throwable ignored) {}
        }
    }

    public void reset() {
        nextScanMs = 0L;
        tracker.reset();
        ObjectSemanticContext.reset();
    }

    @Override public void close() {
        closed = true;
        reset();
        try { labeler.close(); } catch (Throwable ignored) {}
    }

    private record Crop(int trackingId, Direction direction, Bitmap bitmap) {}
    private record Scored(DetectedObject object, float score) {}
    private record Candidate(String label, float confidence, float score) {}

    private static int clamp(int value, int lo, int hi) { return Math.max(lo, Math.min(hi, value)); }
}
