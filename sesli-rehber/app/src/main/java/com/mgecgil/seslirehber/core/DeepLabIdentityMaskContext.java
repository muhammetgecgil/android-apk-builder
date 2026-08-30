package com.mgecgil.seslirehber.core;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Short-lived pixel-level identity evidence from the bundled Pascal/DeepLab category mask.
 * This context is advisory only: it can corroborate or weaken a semantic object name, but it is
 * never a SafetyGate input and never creates STOP/CAUTION by itself.
 */
public final class DeepLabIdentityMaskContext {
    public record Evidence(
            String bestLabel,
            float bestShare,
            float namedShare,
            boolean exact,
            boolean familyCompatible,
            boolean conflicting,
            long timestampMs) {
        public static Evidence none() {
            return new Evidence("", 0f, 0f, false, false, false, 0L);
        }
        public boolean usable() { return timestampMs > 0L && bestShare >= 0.06f && !bestLabel.isBlank(); }
    }

    private static final long FRESH_MS = 1700L;
    private static final float CONFLICT_SHARE = 0.16f;
    private static Frame frame;

    private DeepLabIdentityMaskContext() {}

    public static synchronized void publish(ByteBuffer source, int width, int height, long timestampMs) {
        if (source == null || width <= 0 || height <= 0 || timestampMs <= 0L) return;
        int pixels = width * height;
        if (source.capacity() < pixels) return;
        ByteBuffer copySource = source.duplicate();
        byte[] labels = new byte[pixels];
        for (int i = 0; i < pixels; i++) labels[i] = copySource.get(i);
        frame = new Frame(labels, width, height, timestampMs);
    }

    public static synchronized Evidence evidenceFor(
            String detectorLabel,
            float left,
            float top,
            float right,
            float bottom,
            long nowMs) {
        if (frame == null || detectorLabel == null || detectorLabel.isBlank()) return Evidence.none();
        if (nowMs < frame.timestampMs || nowMs - frame.timestampMs > FRESH_MS) return Evidence.none();
        if (!(right > left && bottom > top)) return Evidence.none();

        // Ignore a small border around the detector box so background pixels and neighboring objects
        // do not dominate the corroboration vote.
        float insetX = Math.min(0.08f * (right - left), 0.025f);
        float insetY = Math.min(0.08f * (bottom - top), 0.025f);
        float l = clamp(left + insetX);
        float t = clamp(top + insetY);
        float r = clamp(right - insetX);
        float b = clamp(bottom - insetY);
        if (r <= l || b <= t) return Evidence.none();

        int x0 = Math.max(0, Math.min(frame.width - 1, (int) Math.floor(l * frame.width)));
        int y0 = Math.max(0, Math.min(frame.height - 1, (int) Math.floor(t * frame.height)));
        int x1 = Math.max(x0 + 1, Math.min(frame.width, (int) Math.ceil(r * frame.width)));
        int y1 = Math.max(y0 + 1, Math.min(frame.height, (int) Math.ceil(b * frame.height)));

        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        int named = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                total++;
                String label = labelFor(frame.labels[y * frame.width + x] & 0xff);
                if (label.isEmpty()) continue;
                named++;
                counts.put(label, counts.getOrDefault(label, 0) + 1);
            }
        }
        if (total <= 0 || counts.isEmpty()) return Evidence.none();

        String best = "";
        int bestCount = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        float bestShare = bestCount / (float) total;
        float namedShare = named / (float) total;
        String clean = detectorLabel.trim();
        boolean exact = clean.equals(best);
        boolean compatible = !exact && sameFamily(clean, best);
        boolean conflicting = !exact && !compatible && bestShare >= CONFLICT_SHARE;
        return new Evidence(best, bestShare, namedShare, exact, compatible, conflicting, frame.timestampMs);
    }

    public static synchronized void reset() { frame = null; }

    static String labelFor(int id) {
        return switch (id) {
            case 1 -> "uçak";
            case 2 -> "bisiklet";
            case 3 -> "kuş";
            case 4 -> "tekne";
            case 5 -> "şişe";
            case 6 -> "otobüs";
            case 7 -> "araç";
            case 8 -> "kedi";
            case 9 -> "sandalye";
            case 10 -> "inek";
            case 11 -> "masa";
            case 12 -> "köpek";
            case 13 -> "at";
            case 14 -> "motosiklet";
            case 15 -> "insan";
            case 16 -> "saksı";
            case 17 -> "koyun";
            case 18 -> "koltuk";
            case 19 -> "tren";
            case 20 -> "televizyon";
            default -> "";
        };
    }

    static boolean sameFamily(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return false;
        return family(a).equals(family(b)) && !family(a).isEmpty();
    }

    private static String family(String label) {
        return switch (label) {
            case "araç", "otobüs", "kamyon", "tren" -> "vehicle";
            case "bisiklet", "motosiklet", "scooter" -> "two_wheeler";
            case "koltuk", "sandalye", "masa" -> "furniture";
            case "kedi", "köpek", "kuş", "at", "inek", "koyun", "hayvan" -> "animal";
            default -> label;
        };
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }

    private record Frame(byte[] labels, int width, int height, long timestampMs) {}
}
