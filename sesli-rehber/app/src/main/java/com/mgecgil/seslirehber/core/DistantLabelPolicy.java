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

        // Indoor / daily-life identities used by the close-object semantic pass.
        if (containsAny(s, "couch", "sofa", "armchair", "recliner")) return "koltuk";
        if (containsAny(s, "chair", "stool")) return "sandalye";
        if (containsAny(s, "bench")) return "bank";
        if (containsAny(s, "table", "desk")) return "masa";
        if (containsAny(s, "bed", "mattress")) return "yatak";
        if (containsAny(s, "pillow", "cushion")) return "yastık";
        if (containsAny(s, "cabinet", "wardrobe", "cupboard", "dresser")) return "dolap";
        if (containsAny(s, "television", "tv", "monitor", "display")) return "televizyon";
        if (containsAny(s, "lamp", "lampshade", "light fixture")) return "lamba";
        if (containsAny(s, "clock", "wall clock")) return "saat";
        if (containsAny(s, "carpet", "rug")) return "halı";
        if (containsAny(s, "door", "gate")) return "kapı";
        if (containsAny(s, "window")) return "pencere";
        if (containsAny(s, "refrigerator", "fridge")) return "buzdolabı";
        if (containsAny(s, "oven")) return "fırın";
        if (containsAny(s, "microwave")) return "mikrodalga";
        if (containsAny(s, "sink", "washbasin")) return "lavabo";
        if (containsAny(s, "toilet")) return "tuvalet";
        if (containsAny(s, "book")) return "kitap";
        if (containsAny(s, "bottle")) return "şişe";
        if (containsAny(s, "cup", "mug")) return "bardak";
        if (containsAny(s, "phone", "smartphone", "mobile phone")) return "telefon";
        if (containsAny(s, "laptop", "notebook computer")) return "dizüstü bilgisayar";
        if (containsAny(s, "computer", "desktop computer")) return "bilgisayar";
        if (containsAny(s, "keyboard")) return "klavye";
        if (containsAny(s, "shoe", "footwear")) return "ayakkabı";
        if (containsAny(s, "bag", "handbag", "backpack")) return "çanta";
        if (containsAny(s, "umbrella")) return "şemsiye";
        if (containsAny(s, "trash", "garbage", "waste bin", "dustbin")) return "çöp kutusu";

        if (containsAny(s, "dog")) return "köpek";
        if (containsAny(s, "cat")) return "kedi";
        if (containsAny(s, "bird")) return "kuş";
        if (containsAny(s, "animal")) return "hayvan";
        if (containsAny(s, "tree")) return "ağaç";
        if (containsAny(s, "building", "house")) return "bina";
        return "";
    }

    /** Exact word/phrase matching prevents false positives such as outdoor -> door or woman -> man. */
    private static boolean containsAny(String text, String... terms) {
        String padded = " " + text + " ";
        for (String term : terms) {
            String normalizedTerm = normalize(term);
            if (!normalizedTerm.isEmpty() && padded.contains(" " + normalizedTerm + " ")) return true;
        }
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
