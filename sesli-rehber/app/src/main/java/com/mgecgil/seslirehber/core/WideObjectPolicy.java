package com.mgecgil.seslirehber.core;

/** Turkish ontology for COCO/EfficientDet plus common crop-label synonyms. */
public final class WideObjectPolicy {
    private WideObjectPolicy() {}

    public static String toTurkish(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(java.util.Locale.US).replace('_', ' ');
        return switch (s) {
            case "person" -> "insan";
            case "bicycle" -> "bisiklet";
            case "car" -> "araç";
            case "motorcycle" -> "motosiklet";
            case "airplane" -> "uçak";
            case "bus" -> "otobüs";
            case "train" -> "tren";
            case "truck" -> "kamyon";
            case "boat" -> "tekne";
            case "traffic light" -> "trafik ışığı";
            case "fire hydrant" -> "yangın musluğu";
            case "stop sign" -> "dur tabelası";
            case "parking meter" -> "parkmetre";
            case "bench" -> "bank";
            case "bird" -> "kuş";
            case "cat" -> "kedi";
            case "dog" -> "köpek";
            case "horse" -> "at";
            case "sheep" -> "koyun";
            case "cow" -> "inek";
            case "elephant" -> "fil";
            case "bear" -> "ayı";
            case "zebra" -> "zebra";
            case "giraffe" -> "zürafa";
            case "backpack" -> "sırt çantası";
            case "umbrella" -> "şemsiye";
            case "handbag" -> "çanta";
            case "tie" -> "kravat";
            case "suitcase" -> "valiz";
            case "frisbee" -> "frizbi";
            case "skis" -> "kayak";
            case "snowboard" -> "snowboard";
            case "sports ball" -> "top";
            case "kite" -> "uçurtma";
            case "baseball bat" -> "sopa";
            case "baseball glove" -> "eldiven";
            case "skateboard" -> "kaykay";
            case "surfboard" -> "sörf tahtası";
            case "tennis racket" -> "tenis raketi";
            case "bottle" -> "şişe";
            case "wine glass" -> "kadeh";
            case "cup" -> "bardak";
            case "fork" -> "çatal";
            case "knife" -> "bıçak";
            case "spoon" -> "kaşık";
            case "bowl" -> "kase";
            case "banana" -> "muz";
            case "apple" -> "elma";
            case "sandwich" -> "sandviç";
            case "orange" -> "portakal";
            case "broccoli" -> "brokoli";
            case "carrot" -> "havuç";
            case "hot dog" -> "sosisli";
            case "pizza" -> "pizza";
            case "donut" -> "donut";
            case "cake" -> "pasta";
            case "chair" -> "sandalye";
            case "couch" -> "koltuk";
            case "potted plant" -> "saksı";
            case "bed" -> "yatak";
            case "dining table" -> "masa";
            case "toilet" -> "tuvalet";
            case "tv" -> "televizyon";
            case "laptop" -> "laptop";
            case "mouse" -> "fare";
            case "remote" -> "kumanda";
            case "keyboard" -> "klavye";
            case "cell phone" -> "telefon";
            case "microwave" -> "mikrodalga";
            case "oven" -> "fırın";
            case "toaster" -> "ekmek kızartma makinesi";
            case "sink" -> "lavabo";
            case "refrigerator" -> "buzdolabı";
            case "book" -> "kitap";
            case "clock" -> "saat";
            case "vase" -> "vazo";
            case "scissors" -> "makas";
            case "teddy bear" -> "oyuncak ayı";
            case "hair drier", "hair dryer" -> "saç kurutma makinesi";
            case "toothbrush" -> "diş fırçası";
            default -> DistantLabelPolicy.toTurkishObject(raw);
        };
    }

    public static boolean important(String label) {
        return switch (label) {
            case "bıçak", "makas", "araç", "otobüs", "kamyon", "motosiklet", "bisiklet", "trafik ışığı" -> true;
            default -> false;
        };
    }
}
