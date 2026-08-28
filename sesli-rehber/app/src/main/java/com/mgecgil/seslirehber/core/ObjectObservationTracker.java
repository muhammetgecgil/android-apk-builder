package com.mgecgil.seslirehber.core;

import android.graphics.Rect;
import com.google.mlkit.vision.objects.DetectedObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static com.mgecgil.seslirehber.core.GuidanceModels.ObjectObservation;

/** Shared generic geometry/tracking logic for CameraX and ARCore image sources. */
public final class ObjectObservationTracker {
    private static final long TRACK_STALE_MS = 2200L;
    private static final float TRACK_ALPHA = 0.36f;
    private static final int MAX_FRAME_OBJECTS = 8;
    private final Map<Integer, TrackState> tracks = new ConcurrentHashMap<>();

    /**
     * Returns several relevant observations instead of collapsing the whole frame to one object.
     * This increases situational awareness without adding another detector inference pass.
     */
    public List<ObjectObservation> observeAll(List<DetectedObject> objects, int width, int height, long nowMs) {
        if (objects == null || objects.isEmpty() || width <= 0 || height <= 0) {
            prune(nowMs);
            return List.of();
        }

        List<ScoredObservation> scored = new ArrayList<>();
        float frameArea = width * (float) height;

        for (DetectedObject object : objects) {
            Rect box = object.getBoundingBox();
            if (box.width() <= 0 || box.height() <= 0) continue;

            float centerX = clamp(box.exactCenterX() / width);
            float centerY = clamp(box.exactCenterY() / height);
            float bottomY = clamp(box.bottom / (float) height);
            float areaRatio = clamp((box.width() * (float) box.height()) / frameArea);
            Integer idValue = object.getTrackingId();
            int trackingId = idValue == null ? -1 : idValue;

            float growthPerSecond = 0f;
            float centerVelocityX = 0f;
            int seenCount = 1;
            if (trackingId >= 0) {
                TrackState old = tracks.get(trackingId);
                if (old != null) {
                    long dtMs = Math.max(1L, nowMs - old.timeMs);
                    float seconds = dtMs / 1000f;
                    float rawGrowth = (areaRatio - old.areaRatio) / seconds;
                    float rawCenterVelocity = (centerX - old.centerX) / seconds;
                    growthPerSecond = ema(old.smoothedGrowth, rawGrowth, TRACK_ALPHA);
                    centerVelocityX = ema(old.smoothedCenterVelocityX, rawCenterVelocity, TRACK_ALPHA);
                    seenCount = Math.min(30, old.seenCount + 1);
                }
                tracks.put(trackingId, new TrackState(
                        areaRatio, centerX, growthPerSecond, centerVelocityX, nowMs, seenCount));
            }

            float persistenceConfidence = trackingId < 0
                    ? 0.46f
                    : clamp(0.52f + 0.075f * Math.min(seenCount, 5));
            ObjectObservation observation = new ObjectObservation(
                    centerX, centerY, bottomY, areaRatio, growthPerSecond, centerVelocityX,
                    trackingId, persistenceConfidence, nowMs);

            float centerDistance = Math.abs(centerX - 0.5f) * 2f;
            float centerBonus = (1f - clamp(centerDistance)) * 0.22f;
            float approachBonus = clamp(Math.max(0f, growthPerSecond) / 0.22f) * 0.28f;
            float towardCenter = Math.signum(0.5f - centerX) * centerVelocityX;
            float crossingBonus = clamp(Math.max(0f, towardCenter) / 0.16f) * 0.12f;
            float bottomBonus = Math.max(0f, bottomY - 0.45f) * 0.10f;
            float score = areaRatio + centerBonus + approachBonus + crossingBonus + bottomBonus;
            scored.add(new ScoredObservation(observation, score));
        }

        prune(nowMs);
        scored.sort(Comparator.comparingDouble(ScoredObservation::score).reversed());
        List<ObjectObservation> result = new ArrayList<>(Math.min(scored.size(), MAX_FRAME_OBJECTS));
        for (int i = 0; i < scored.size() && i < MAX_FRAME_OBJECTS; i++) {
            result.add(scored.get(i).observation());
        }
        return result;
    }

    /** Compatibility helper for existing callers/tests. */
    public ObjectObservation selectMostRelevant(List<DetectedObject> objects, int width, int height, long nowMs) {
        List<ObjectObservation> all = observeAll(objects, width, height, nowMs);
        return all.isEmpty() ? null : all.get(0);
    }

    public void reset() { tracks.clear(); }

    private void prune(long nowMs) {
        tracks.entrySet().removeIf(entry -> nowMs - entry.getValue().timeMs > TRACK_STALE_MS);
    }

    private static float ema(float previous, float current, float alpha) {
        return previous * (1f - alpha) + current * alpha;
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }

    private record ScoredObservation(ObjectObservation observation, float score) {}

    private static final class TrackState {
        final float areaRatio;
        final float centerX;
        final float smoothedGrowth;
        final float smoothedCenterVelocityX;
        final long timeMs;
        final int seenCount;

        TrackState(float areaRatio, float centerX, float smoothedGrowth,
                   float smoothedCenterVelocityX, long timeMs, int seenCount) {
            this.areaRatio = areaRatio;
            this.centerX = centerX;
            this.smoothedGrowth = smoothedGrowth;
            this.smoothedCenterVelocityX = smoothedCenterVelocityX;
            this.timeMs = timeMs;
            this.seenCount = seenCount;
        }
    }
}
