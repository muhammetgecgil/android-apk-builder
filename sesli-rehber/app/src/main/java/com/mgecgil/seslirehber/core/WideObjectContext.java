package com.mgecgil.seslirehber.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recent named-object inventory for speech, scene summary and the visual HUD. */
public final class WideObjectContext {
    public enum Environment { HOME_OFFICE, MARKET, STREET, UNKNOWN }

    private static final long FRESH_MS = 2600L;
    private static final int MAX = 16;
    private static final Map<String, WideObjectObservation> recent = new LinkedHashMap<>();

    private WideObjectContext() {}

    public static synchronized void note(WideObjectObservation value) {
        if (value == null || !value.usable()) return;
        String key = value.label() + ":" + value.direction();
        recent.put(key, value);
        while (recent.size() > MAX) recent.remove(recent.keySet().iterator().next());
    }

    public static synchronized List<WideObjectObservation> snapshot(long nowMs) {
        recent.entrySet().removeIf(e -> !fresh(e.getValue(), nowMs));
        List<WideObjectObservation> out = new ArrayList<>(recent.values());
        out.sort(Comparator.comparingDouble(WideObjectObservation::confidence).reversed());
        return List.copyOf(out);
    }

    public static synchronized Environment environment(long nowMs) {
        int home = 0, market = 0, street = 0;
        for (WideObjectObservation o : snapshot(nowMs)) {
            String l = o.label();
            if (in(l, "koltuk", "sandalye", "masa", "yatak", "televizyon", "laptop", "klavye", "fare", "kumanda", "kitap", "saat", "vazo", "saksı", "buzdolabı", "mikrodalga", "fırın")) home += 2;
            if (in(l, "şişe", "bardak", "kase", "muz", "elma", "portakal", "brokoli", "havuç", "sandviç", "pizza", "donut", "pasta")) market += 2;
            if (in(l, "insan", "araç", "otobüs", "kamyon", "motosiklet", "bisiklet", "trafik ışığı", "trafik tabelası", "yangın musluğu", "parkmetre", "bank")) street += 2;
        }
        if (street >= 4 && street >= home && street >= market) return Environment.STREET;
        if (market >= 4 && market > home) return Environment.MARKET;
        if (home >= 4) return Environment.HOME_OFFICE;
        return Environment.UNKNOWN;
    }

    public static synchronized String inventorySummary(long nowMs) {
        List<WideObjectObservation> list = snapshot(nowMs);
        if (list.isEmpty()) return "";
        StringBuilder out = new StringBuilder("Görülen nesneler: ");
        int count = 0;
        for (WideObjectObservation o : list) {
            if (!o.definite() || o.confidence() < 0.68f) continue;
            if (count++ > 0) out.append(", ");
            out.append(where(o)).append(" ").append(o.label());
            if (count >= 6) break;
        }
        if (count == 0) return "";
        out.append(".");
        return out.toString();
    }

    public static synchronized void reset() { recent.clear(); }

    private static String where(WideObjectObservation o) {
        return switch (o.direction()) {
            case LEFT -> "solda";
            case RIGHT -> "sağda";
            default -> "önde";
        };
    }

    private static boolean in(String value, String... values) {
        for (String x : values) if (x.equals(value)) return true;
        return false;
    }

    private static boolean fresh(WideObjectObservation o, long nowMs) {
        return o.timestampMs() > 0L && nowMs >= o.timestampMs() && nowMs - o.timestampMs() <= FRESH_MS;
    }
}
