package com.mgecgil.seslirehber.core;

import java.util.List;

/**
 * Pure geometry/context helpers for advisory semantic identity fusion.
 * This class never has SafetyGate authority.
 */
public final class SpatialIdentityPolicy {
    private SpatialIdentityPolicy() {}

    public static float iou(float l1, float t1, float r1, float b1,
                            float l2, float t2, float r2, float b2) {
        float il = Math.max(l1, l2);
        float it = Math.max(t1, t2);
        float ir = Math.min(r1, r2);
        float ib = Math.min(b1, b2);
        float iw = Math.max(0f, ir - il);
        float ih = Math.max(0f, ib - it);
        float intersection = iw * ih;
        float a = Math.max(0f, r1 - l1) * Math.max(0f, b1 - t1);
        float b = Math.max(0f, r2 - l2) * Math.max(0f, b2 - t2);
        float union = a + b - intersection;
        return union <= 0f ? 0f : clamp(intersection / union);
    }

    public static float centerDistance(float l1, float t1, float r1, float b1,
                                       float l2, float t2, float r2, float b2) {
        float dx = ((l1 + r1) - (l2 + r2)) * 0.5f;
        float dy = ((t1 + b1) - (t2 + b2)) * 0.5f;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** Same physical region for temporal semantic voting. */
    public static boolean samePhysicalRegion(
            boolean sameLabel,
            float l1, float t1, float r1, float b1,
            float l2, float t2, float r2, float b2) {
        float overlap = iou(l1, t1, r1, b1, l2, t2, r2, b2);
        float distance = centerDistance(l1, t1, r1, b1, l2, t2, r2, b2);
        if (sameLabel) return overlap >= 0.16f || distance <= 0.13f;
        // Different labels are fused only when boxes are nearly the same physical object.
        return overlap >= 0.58f && distance <= 0.10f;
    }

    public static WideObjectObservation bestOverlap(
            List<WideObjectObservation> values,
            float left, float top, float right, float bottom,
            float minIou) {
        if (values == null || values.isEmpty()) return null;
        WideObjectObservation best = null;
        float bestScore = minIou;
        for (WideObjectObservation o : values) {
            if (o == null || !o.usable()) continue;
            float score = iou(left, top, right, bottom, o.left(), o.top(), o.right(), o.bottom());
            if (score >= bestScore) {
                bestScore = score;
                best = o;
            }
        }
        return best;
    }

    /** Street-only labels are especially suspicious in a stable home/office context. */
    public static boolean streetOnly(String label) {
        if (label == null) return false;
        return switch (label) {
            case "araç", "otobüs", "kamyon", "motosiklet", "bisiklet", "trafik ışığı",
                    "trafik tabelası", "dur tabelası", "yangın musluğu", "parkmetre" -> true;
            default -> false;
        };
    }

    /**
     * Distant image-labeling is deliberately weakened when the current broad detector strongly
     * says the scene is indoor/home. This does not affect geometric obstacle detection.
     */
    public static boolean allowDistant(
            String label,
            float confidence,
            WideObjectContext.Environment environment) {
        if (label == null || label.isBlank()) return false;
        if (environment == WideObjectContext.Environment.HOME_OFFICE && streetOnly(label)) {
            return confidence >= 0.92f;
        }
        if (environment == WideObjectContext.Environment.MARKET && streetOnly(label)) {
            return confidence >= 0.88f;
        }
        return true;
    }

    /** Prefer the joint boxed detector over crop-only image labeling for the same physical region. */
    public static boolean cropShouldYieldToWide(
            String cropLabel,
            float left, float top, float right, float bottom,
            List<WideObjectObservation> wide) {
        WideObjectObservation overlap = bestOverlap(wide, left, top, right, bottom, 0.28f);
        if (overlap == null) return false;
        // Even if the label agrees, the wide detector already owns the visual/speech identity.
        return true;
    }

    public static boolean sameNamedObject(WideObjectObservation a, WideObjectObservation b) {
        if (a == null || b == null || !a.label().equals(b.label())) return false;
        return iou(a.left(), a.top(), a.right(), a.bottom(), b.left(), b.top(), b.right(), b.bottom()) >= 0.34f;
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
