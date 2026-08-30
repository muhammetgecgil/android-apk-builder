package com.mgecgil.seslirehber.core;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative last-meter OCR evidence. It never certifies a building entrance. A candidate must
 * repeat across OCR samples before a user-facing hint is produced.
 */
public final class EntranceEvidenceEngine {
    private static final long ACTIVE_MS = 4 * 60_000L;
    private static final long SAME_CANDIDATE_MS = 14_000L;
    private static final long SPEECH_COOLDOWN_MS = 18_000L;
    private static final long AUTO_SCAN_MS = 4_000L;
    private static final Pattern NUMBER = Pattern.compile("\\b\\d{1,4}[a-z]?\\b");

    private final Set<String> targetNumbers = new HashSet<>();
    private String destinationLabel = "";
    private long activeUntilMs;
    private long nextAutoScanMs;
    private long lastCandidateMs;
    private long lastSpokenMs;
    private String lastFingerprint = "";
    private int candidateStreak;

    public synchronized void activate(String destinationLabel, long nowMs) {
        this.destinationLabel = destinationLabel == null ? "" : destinationLabel.trim();
        activeUntilMs = nowMs + ACTIVE_MS;
        nextAutoScanMs = nowMs + 900L;
        lastCandidateMs = 0L;
        lastSpokenMs = 0L;
        lastFingerprint = "";
        candidateStreak = 0;
        targetNumbers.clear();
        Matcher matcher = NUMBER.matcher(normalize(this.destinationLabel));
        while (matcher.find()) {
            String token = matcher.group();
            // Five-digit postal codes are excluded by the regex. Tiny tokens like 1/2 are kept
            // because Turkish door numbers can legitimately be one digit.
            targetNumbers.add(token);
        }
    }

    public synchronized void deactivate() {
        destinationLabel = "";
        activeUntilMs = 0L;
        nextAutoScanMs = 0L;
        lastCandidateMs = 0L;
        lastSpokenMs = 0L;
        lastFingerprint = "";
        candidateStreak = 0;
        targetNumbers.clear();
    }

    public synchronized boolean isActive(long nowMs) {
        if (activeUntilMs <= nowMs) {
            deactivate();
            return false;
        }
        return activeUntilMs > 0L;
    }

    /** Allows one automatic OCR sample every few seconds during the last-meter window. */
    public synchronized boolean consumeAutoScanPermit(long nowMs) {
        if (!isActive(nowMs) || nowMs < nextAutoScanMs) return false;
        nextAutoScanMs = nowMs + AUTO_SCAN_MS;
        return true;
    }

    /** Returns an advisory phrase only after the same entrance evidence repeats. */
    public synchronized String observeOcr(String rawText, long nowMs) {
        if (!isActive(nowMs) || rawText == null || rawText.trim().isEmpty()) return "";
        String normalized = normalize(rawText);
        String numberMatch = matchingTargetNumber(normalized);
        boolean entranceWord = containsEntranceWord(normalized);
        if (numberMatch.isEmpty() && !entranceWord) {
            if (nowMs - lastCandidateMs > SAME_CANDIDATE_MS) resetStreak();
            return "";
        }

        String fingerprint = !numberMatch.isEmpty() ? "number:" + numberMatch : "entrance-word";
        if (fingerprint.equals(lastFingerprint) && nowMs - lastCandidateMs <= SAME_CANDIDATE_MS) {
            candidateStreak++;
        } else {
            lastFingerprint = fingerprint;
            candidateStreak = 1;
        }
        lastCandidateMs = nowMs;

        // The cooldown applies only after an advisory has actually been spoken. This keeps the
        // first persistent candidate responsive while still suppressing repeated OCR chatter.
        if (candidateStreak < 2
                || (lastSpokenMs > 0L && nowMs - lastSpokenMs < SPEECH_COOLDOWN_MS)) return "";
        lastSpokenMs = nowMs;
        candidateStreak = 0;

        if (!numberMatch.isEmpty() && entranceWord) {
            return "Hedef adresindeki " + numberMatch
                    + " numarasına benzeyen yazı ve giriş işareti tekrar görüldü. "
                    + "Burası giriş adayı olabilir; kapıyı ve çevreyi bastonla doğrula.";
        }
        if (!numberMatch.isEmpty()) {
            return "Hedef adresindeki " + numberMatch
                    + " numarasına benzeyen yazı tekrar görüldü. "
                    + "Bu yalnız giriş adayıdır; doğru kapı olduğunu bastonla ve çevreyle doğrula.";
        }
        return "Giriş veya kapı işareti tekrar görüldü. "
                + "Burası giriş adayı olabilir; kapı numarasını ve çevreyi doğrula.";
    }

    public synchronized String destinationLabel() { return destinationLabel; }

    private String matchingTargetNumber(String normalizedOcr) {
        if (targetNumbers.isEmpty()) return "";
        Matcher matcher = NUMBER.matcher(normalizedOcr);
        while (matcher.find()) {
            String token = matcher.group();
            if (targetNumbers.contains(token)) return token;
        }
        return "";
    }

    private static boolean containsEntranceWord(String text) {
        return containsAny(text,
                "giris", "ana giris", "kapi", "kapi no", "blok", "bina girisi",
                "entrance", "entry", "door", "lobi", "resepsiyon");
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }

    private void resetStreak() {
        lastFingerprint = "";
        candidateStreak = 0;
    }

    static String normalize(String input) {
        String s = input.toLowerCase(new Locale("tr", "TR"))
                .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
                .replace('ş', 's').replace('ö', 'o').replace('ç', 'c');
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
