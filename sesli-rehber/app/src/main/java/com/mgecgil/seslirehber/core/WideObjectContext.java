package com.mgecgil.seslirehber.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recent named-object inventory for speech, scene summary and the visual HUD. */
public final class WideObjectContext {
    public enum Environment { HOME_OFFICE, MARKET, STREET, UNKNOWN }

    private static final long FRESH_MS = 2800L;
    private static final int MAX = 24;
    private static final int MAX_SNAPSHOT = 12;
    private static final Map<String, WideObjectObservation> recent = new LinkedHashMap<>();

    private WideObjectContext() {}

    public static synchronized void note(WideObjectObservation value) {
        if (value == null || !value.usable()) return;
        float cx = (value.left() + value.right()) * 0.5f;
        float cy = (value.top() + value.bottom()) * 0.5f;
        int gx = Math.max(0, Math.min(4, (int) (cx * 5f)));
        int gy = Math.max(0, Math.min(4, (int) (cy * 5f)));
        String key = value.label() + ":" + gx + ":" + gy;
        recent.put(key, value);
        while (recent.size() > MAX) recent.remove(recent.keySet().iterator().next());
    }

    public static synchronized List<WideObjectObservation> snapshot(long nowMs) {
        recent.entrySet().removeIf(e -> !fresh(e.getValue(), nowMs));
        List<WideObjectObservation> raw = new ArrayList<>(recent.values());
        raw.sort((a, b) -> {
            if (a.definite() != b.definite()) return a.definite() ? -1 : 1;
            return Float.compare(b.confidence(), a.confidence());
        });

        List<WideObjectObservation> out = new ArrayList<>();
        for (WideObjectObservation candidate : raw) {
            boolean duplicate = false;
            for (WideObjectObservation kept : out) {
                float overlap = SpatialIdentityPolicy.iou(
                        candidate.left(), candidate.top(), candidate.right(), candidate.bottom(),
                        kept.left(), kept.top(), kept.right(), kept.bottom());
                if (SpatialIdentityPolicy.sameNamedObject(candidate, kept) || overlap >= 0.66f) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) out.add(candidate);
            if (out.size() >= MAX_SNAPSHOT) break;
        }
        return List.copyOf(out);
    }

    public static synchronized Environment environment(long nowMs) {
        int home = 0, market = 0, street = 0;
        for (WideObjectObservation o : snapshot(nowMs)) {
            String l = o.label();
            int weight = o.definite() ? 2 : 1;
            if (in(l, "koltuk", "sandalye", "masa", "yatak", "televizyon", "laptop", "klavye", "fare", "kumanda", "kitap", "saat", "vazo", "saksı", "buzdolabı", "mikrodalga", "fırın")) home += weight;
            if (in(l, "şişe", "bardak", "kase", "muz", "elma", "portakal", "brokoli", "havuç", "sandviç", "pizza", "donut", "pasta")) market += weight;
            if (in(l, "insan", "araç", "otobüs", "kamyon", "motosiklet", "bisiklet", "trafik ışığı", "trafik tabelası", "dur tabelası", "yangın musluğu", "parkmetre", "bank")) street += weight;
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
