package com.mgecgil.seslirehber.core;

/**
 * Conservative semantic identity fusion. DeepLab pixel evidence may strengthen a matching detector
 * label or weaken a strong contradiction. It never changes SafetyGate geometry/depth decisions.
 */
public final class IdentityFusionPolicy {
    public record Result(
            String label,
            float confidence,
            boolean corroborated,
            boolean familyCompatible,
            boolean conflicting,
            String maskLabel,
            float maskShare) {}

    private IdentityFusionPolicy() {}

    public static Result fuse(
            String detectorLabel,
            float detectorConfidence,
            DeepLabIdentityMaskContext.Evidence evidence) {
        String clean = detectorLabel == null ? "" : detectorLabel.trim();
        float base = clamp(detectorConfidence);
        if (clean.isEmpty() || evidence == null || !evidence.usable()) {
            return new Result(clean, base, false, false, false, "", 0f);
        }

        float confidence = base;
        boolean corroborated = false;
        boolean compatible = false;
        boolean conflict = false;

        if (evidence.exact()) {
            float boost = 0.07f + Math.min(0.08f, evidence.bestShare() * 0.28f);
            confidence = clamp(base + boost);
            corroborated = true;
        } else if (evidence.familyCompatible()) {
            // Same coarse family is supportive, but does not certify the precise class.
            confidence = clamp(base + Math.min(0.045f, evidence.bestShare() * 0.14f));
            compatible = true;
        } else if (evidence.conflicting()) {
            // A large contradictory pixel region should prevent a single classifier from becoming
            // overconfident. Stronger contradiction receives a steeper penalty.
            float factor = evidence.bestShare() >= 0.28f ? 0.58f
                    : evidence.bestShare() >= 0.21f ? 0.66f : 0.74f;
            confidence = clamp(base * factor);
            conflict = true;
        }

        return new Result(
                clean,
                confidence,
                corroborated,
                compatible,
                conflict,
                evidence.bestLabel(),
                evidence.bestShare());
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
