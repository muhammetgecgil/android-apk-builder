package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

/** Advisory wording only; it never turns far semantic recognition into a safety instruction. */
public final class DistantObjectSpeech {
    private DistantObjectSpeech() {}

    public static String format(DistantObjectObservation observation) {
        if (observation == null || !observation.mature()) return "";
        String side = switch (observation.direction()) {
            case LEFT -> "sol tarafta";
            case RIGHT -> "sağ tarafta";
            case CENTER -> "ileride";
            default -> "uzakta";
        };
        return "Uzak görüş: " + side + " " + observation.label()
                + " olabilecek bir görüntü birkaç taramada tekrar görüldü.";
    }
}
