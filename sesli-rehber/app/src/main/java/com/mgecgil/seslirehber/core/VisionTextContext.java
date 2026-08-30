package com.mgecgil.seslirehber.core;

/** Shared OCR context between the navigation last-meter state and either camera pipeline. */
public final class VisionTextContext {
    private static final EntranceEvidenceEngine ENTRANCE = new EntranceEvidenceEngine();

    private VisionTextContext() {}

    public static void activateFinalApproach(String destinationLabel, long nowMs) {
        ENTRANCE.activate(destinationLabel, nowMs);
    }

    public static void deactivateFinalApproach() {
        ENTRANCE.deactivate();
    }

    public static boolean finalApproachActive(long nowMs) {
        return ENTRANCE.isActive(nowMs);
    }

    public static boolean shouldAutoScan(long nowMs) {
        return ENTRANCE.consumeAutoScanPermit(nowMs);
    }

    /**
     * Adds cautious entrance/transit interpretation while preserving the OCR text. During final
     * approach, entrance evidence has priority and transit hints are suppressed to reduce chatter.
     */
    public static String enrichOcr(String rawText, long nowMs) {
        String text = rawText == null ? "" : rawText.trim();
        String entrance = ENTRANCE.observeOcr(text, nowMs);
        String hint = entrance;
        if (hint.isEmpty() && !ENTRANCE.isActive(nowMs)) {
            hint = TransitTextInterpreter.interpret(text);
        }
        if (hint.isEmpty()) return text;
        if (text.isEmpty()) return hint;
        return hint + " Okunan metin: " + text;
    }
}
