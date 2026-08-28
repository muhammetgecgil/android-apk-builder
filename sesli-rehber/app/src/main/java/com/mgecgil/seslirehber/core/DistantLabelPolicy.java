package com.mgecgil.seslirehber.core;

import java.text.Normalizer;
import java.util.Locale;

/** Maps a conservative subset of ML Kit image labels to Turkish object-like terms. */
public final class DistantLabelPolicy {
    private DistantLabelPolicy() {}

    public static String toTurkishObject(String raw) {
        if (raw == null) return "";
        String s = normalize(raw);
        if (s.isEmpty()) return "";

        // Safety-critical ground semantics are intentionally excluded from this advisory channel.
        if (containsAny(s, "stair", "staircase", "step", "curb", "kerb", "hole", "pothole")) return "";

        if (containsAny(s, "person", "human", "pedestrian", "man", "woman", "boy", "girl", "child")) return "insan";
        if (containsAny(s, "bus", "coach")) return "otobüs";
        if (containsAny(s, "truck", "lorry")) return "kamyon";
        if (containsAny(s, "motorcycle", "motorbike")) return "motosiklet";
        if (containsAny(s, "scooter")) return "scooter";
        if (containsAny(s, "bicycle", "bike")) return "bisiklet";
        if (containsAny(s, "car", "vehicle", "automobile", "taxi", "sedan", "van")) return "araç";
        if (containsAny(s, "train", "tram")) return "raylı araç";
        if (containsAny(s, "traffic light", "signal light")) return "trafik ışığı";
        if (containsAny(s, "traffic sign", "road sign", "stop sign", "signage")) return "trafik tabelası";
        if (containsAny(s, "barrier", "barricade")) return "bariyer";
        if (containsAny(s, "traffic cone", "cone")) return "koni";
        if (containsAny(s, "pole", "post")) return "direk";
        if (containsAny(s, "fence", "railing")) return "çit veya korkuluk";
        if (containsAny(s, "bench")) return "bank";
        if (containsAny(s, "chair", "stool")) return "sandalye";
        if (containsAny(s, "table", "desk")) return "masa";
        if (containsAny(s, "door", "gate")) return "kapı";
        if (containsAny(s, "trash", "garbage", "waste bin", "dustbin")) return "çöp kutusu";
        if (containsAny(s, "dog")) return "köpek";
        if (containsAny(s, "cat")) return "kedi";
        if (containsAny(s, "bird")) return "kuş";
        if (containsAny(s, "animal")) return "hayvan";
        if (containsAny(s, "tree")) return "ağaç";
        if (containsAny(s, "building", "house")) return "bina";
        return "";
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    static String normalize(String input) {
        String s = input.toLowerCase(Locale.US);
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
