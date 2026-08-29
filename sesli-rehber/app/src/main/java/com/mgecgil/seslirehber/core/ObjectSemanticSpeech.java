package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/** User-facing wording for advisory object identity. */
public final class ObjectSemanticSpeech {
    private ObjectSemanticSpeech() {}

    public static String format(ObjectSemanticObservation observation) {
        if (observation == null || !observation.usable()) return "";
        String where = switch (observation.direction()) {
            case LEFT -> "Solda ";
            case RIGHT -> "Sağda ";
            case CENTER -> "Önde ";
            default -> "Çevrede ";
        };
        if (observation.definite()) {
            return where + observation.label() + " var.";
        }
        int pct = Math.max(0, Math.min(100, Math.round(observation.confidence() * 100f)));
        return where + observation.label() + " olabilir. Güven yüzde " + pct + ".";
    }
}
