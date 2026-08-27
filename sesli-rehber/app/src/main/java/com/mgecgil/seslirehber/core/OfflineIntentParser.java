package com.mgecgil.seslirehber.core;

import java.text.Normalizer;
import java.util.Locale;

/** Deterministic offline Turkish intent parser for safety-critical/common commands. */
public final class OfflineIntentParser {
    public enum Intent {
        DESCRIBE_SCENE,
        READ_TEXT,
        NAVIGATE_TO,
        START_GUIDANCE,
        STOP_GUIDANCE,
        WAKE_MODE_ON,
        WAKE_MODE_OFF,
        REPEAT,
        HELP,
        UNKNOWN
    }

    public record ParsedIntent(Intent intent, String rawText, String argument) {
        public ParsedIntent(Intent intent, String rawText) { this(intent, rawText, ""); }
        public boolean hasArgument() { return argument != null && !argument.trim().isEmpty(); }
    }

    public ParsedIntent parse(String text) {
        if (text == null || text.trim().isEmpty()) return new ParsedIntent(Intent.UNKNOWN, "");
        String s = normalize(text);
        s = removeWakePrefix(s);

        if (containsAny(s,
                "etrafi anlat", "cevremi anlat", "onumde ne var", "onumde neler var",
                "ne goruyorsun", "goruntuyu anlat", "sahneyi anlat")) {
            return new ParsedIntent(Intent.DESCRIBE_SCENE, text);
        }

        if (containsAny(s,
                "yaziyi oku", "yazilari oku", "metni oku", "tabelayi oku", "etiketi oku",
                "kapida ne yaziyor", "burada ne yaziyor", "ne yaziyor")) {
            return new ParsedIntent(Intent.READ_TEXT, text);
        }

        if (containsAny(s,
                "hey rehberi ac", "hey rehber ac", "eller serbest modu ac", "sesle uyandirmayi ac",
                "surekli dinlemeyi ac")) {
            return new ParsedIntent(Intent.WAKE_MODE_ON, text);
        }
        if (containsAny(s,
                "hey rehberi kapat", "hey rehber kapat", "eller serbest modu kapat",
                "sesle uyandirmayi kapat", "surekli dinlemeyi kapat")) {
            return new ParsedIntent(Intent.WAKE_MODE_OFF, text);
        }

        if (containsAny(s, "rehberligi durdur", "yonlendirmeyi durdur", "yurumeyi durdur")
                || equalsAny(s, "dur", "sus")) {
            return new ParsedIntent(Intent.STOP_GUIDANCE, text);
        }

        if (containsAny(s, "rehberligi baslat", "yurumeyi baslat", "yonlendirmeyi baslat")
                || equalsAny(s, "basla")) {
            return new ParsedIntent(Intent.START_GUIDANCE, text);
        }

        String destination = extractDestination(s);
        if (!destination.isEmpty()) {
            return new ParsedIntent(Intent.NAVIGATE_TO, text, destination);
        }

        if (containsAny(s, "tekrar et", "bir daha soyle", "ne dedin")) {
            return new ParsedIntent(Intent.REPEAT, text);
        }
        if (containsAny(s, "yardim", "ne yapabilirsin", "komutlar", "neler yapabilirsin")) {
            return new ParsedIntent(Intent.HELP, text);
        }
        return new ParsedIntent(Intent.UNKNOWN, text);
    }

    private static String extractDestination(String s) {
        String target = "";
        if (s.startsWith("beni ") && s.endsWith(" gotur")) {
            target = s.substring(5, s.length() - 6).trim();
        } else if (s.endsWith(" adresine gotur")) {
            target = s.substring(0, s.length() - " adresine gotur".length()).trim();
        } else if (s.endsWith(" konumuna gotur")) {
            target = s.substring(0, s.length() - " konumuna gotur".length()).trim();
        } else if (s.endsWith(" adresine git")) {
            target = s.substring(0, s.length() - " adresine git".length()).trim();
        } else if (s.endsWith(" konumuna git")) {
            target = s.substring(0, s.length() - " konumuna git".length()).trim();
        } else if (s.endsWith(" git")) {
            target = s.substring(0, s.length() - 4).trim();
        }

        target = target.replaceFirst("^(beni|bizi)\\s+", "").trim();
        target = target.replaceFirst("\\s+(adresine|konumuna)$", "").trim();
        target = target.replaceFirst("\\s+(e|a|ye|ya)$", "").trim();

        if (target.length() < 2 || equalsAny(target,
                "rehberligi", "yonlendirmeyi", "yurumeyi", "buraya", "oraya")) return "";
        return target;
    }

    private static String removeWakePrefix(String s) {
        if (s.startsWith("hey rehberim ")) return s.substring("hey rehberim ".length()).trim();
        if (s.startsWith("hey rehber ")) return s.substring("hey rehber ".length()).trim();
        if (s.equals("hey rehber") || s.equals("hey rehberim")) return "";
        return s;
    }

    private static boolean containsAny(String s, String... choices) {
        for (String c : choices) if (s.contains(c)) return true;
        return false;
    }

    private static boolean equalsAny(String s, String... choices) {
        for (String c : choices) if (s.equals(c)) return true;
        return false;
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
