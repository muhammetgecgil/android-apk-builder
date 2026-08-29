package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/** Advisory named-object detection from a broad on-device detector. Never has SafetyGate authority. */
public record WideObjectObservation(
        String label,
        float confidence,
        float left,
        float top,
        float right,
        float bottom,
        Direction direction,
        boolean definite,
        boolean important,
        long timestampMs) {

    public WideObjectObservation {
        label = label == null ? "" : label.trim();
        confidence = clamp(confidence);
        left = clamp(left);
        top = clamp(top);
        right = clamp(right);
        bottom = clamp(bottom);
        direction = direction == null ? Direction.UNKNOWN : direction;
    }

    public boolean usable() {
        return !label.isEmpty() && confidence >= 0.50f && right > left && bottom > top;
    }

    public float areaRatio() {
        return Math.max(0f, right - left) * Math.max(0f, bottom - top);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
