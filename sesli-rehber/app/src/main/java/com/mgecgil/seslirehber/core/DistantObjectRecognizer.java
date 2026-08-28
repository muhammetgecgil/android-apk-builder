package com.mgecgil.seslirehber.core;

import android.media.Image;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Low-rate semantic recognizer for small/distant image regions. It is intentionally advisory and
 * separate from the collision STOP path. Each scan enlarges one tile without changing camera zoom.
 */
public final class DistantObjectRecognizer implements AutoCloseable {
    public interface Callback { void onDistantObject(DistantObjectObservation observation); }

    private static final long SCAN_INTERVAL_MS = 650L;
    private static final float MIN_LABEL_CONFIDENCE = 0.60f;
    private static final float MIN_TILE_CONTRAST = 0.10f;

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final FarTileExtractor tileExtractor = new FarTileExtractor();
    private final DistantObjectTracker tracker = new DistantObjectTracker();
    private final ImageLabeler labeler = ImageLabeling.getClient(
            new ImageLabelerOptions.Builder().setConfidenceThreshold(MIN_LABEL_CONFIDENCE).build());
    private volatile long nextScanMs;
    private volatile boolean closed;

    public void maybeAnalyze(Image image, int rotationDegrees, long nowMs, Callback callback) {
        if (closed || image == null || callback == null || nowMs < nextScanMs) return;
        if (!busy.compareAndSet(false, true)) return;
        nextScanMs = nowMs + SCAN_INTERVAL_MS;

        FarTileExtractor.Tile tile;
        try {
            tile = tileExtractor.extractNext(image, rotationDegrees);
        } catch (Throwable error) {
            busy.set(false);
            return;
        }
        if (tile == null || tile.bitmap() == null || tile.contrastScore() < MIN_TILE_CONTRAST) {
            if (tile != null && tile.bitmap() != null) tile.bitmap().recycle();
            busy.set(false);
            return;
        }

        InputImage input = InputImage.fromBitmap(tile.bitmap(), 0);
        labeler.process(input)
                .addOnSuccessListener(labels -> {
                    Candidate candidate = selectCandidate(labels);
                    if (candidate == null) return;
                    DistantObjectObservation matured = tracker.observe(
                            candidate.turkishLabel,
                            tile.direction(),
                            candidate.confidence,
                            tile.zoomFactor(),
                            tile.contrastScore(),
                            System.currentTimeMillis());
                    if (matured != null && matured.mature()) {
                        SituationalAwarenessContext.noteDistant(matured);
                        callback.onDistantObject(matured);
                    }
                })
                .addOnCompleteListener(task -> {
                    try { tile.bitmap().recycle(); } catch (Throwable ignored) {}
                    busy.set(false);
                });
    }

    private static Candidate selectCandidate(List<ImageLabel> labels) {
        if (labels == null || labels.isEmpty()) return null;
        Candidate best = null;
        for (ImageLabel label : labels) {
            if (label == null || label.getConfidence() < MIN_LABEL_CONFIDENCE) continue;
            String turkish = DistantLabelPolicy.toTurkishObject(label.getText());
            if (turkish.isEmpty()) continue;
            float score = label.getConfidence() + priorityBonus(turkish);
            if (best == null || score > best.score) {
                best = new Candidate(turkish, label.getConfidence(), score);
            }
        }
        return best;
    }

    private static float priorityBonus(String label) {
        return switch (label) {
            case "insan", "araç", "otobüs", "kamyon", "motosiklet", "bisiklet" -> 0.12f;
            case "trafik ışığı", "trafik tabelası", "bariyer", "direk" -> 0.08f;
            default -> 0f;
        };
    }

    public void reset() {
        nextScanMs = 0L;
        tileExtractor.reset();
        tracker.reset();
    }

    @Override public void close() {
        closed = true;
        reset();
        try { labeler.close(); } catch (Throwable ignored) {}
    }

    private record Candidate(String turkishLabel, float confidence, float score) {}
}
