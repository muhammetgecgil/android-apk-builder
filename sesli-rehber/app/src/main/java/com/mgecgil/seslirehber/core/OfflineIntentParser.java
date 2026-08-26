package com.mgecgil.seslirehber.core;

import java.text.Normalizer;
import java.util.Locale;

public final class OfflineIntentParser {
    public enum Intent { DESCRIBE_SCENE, START_GUIDANCE, STOP_GUIDANCE, REPEAT, HELP, UNKNOWN }
    public record ParsedIntent(Intent intent, String rawText) {}
    public ParsedIntent parse(String text) {
        if (text == null || text.isBlank()) return new ParsedIntent(Intent.UNKNOWN, "");
        String s = normalize(text).replace("hey rehber", "").trim();
        if (containsAny(s, "etrafi anlat", "cevremi anlat", "onumde ne var", "ne goruyorsun", "goruntuyu anlat")) return new ParsedIntent(Intent.DESCRIBE_SCENE, text);
        if (containsAny(s, "rehberligi baslat", "yurumeyi baslat", "yonlendirmeyi baslat", "basla")) return new ParsedIntent(Intent.START_GUIDANCE, text);
        if (containsAny(s, "rehberligi durdur", "yonlendirmeyi durdur", "dur", "sus")) return new ParsedIntent(Intent.STOP_GUIDANCE, text);
        if (containsAny(s, "tekrar et", "bir daha soyle", "ne dedin")) return new ParsedIntent(Intent.REPEAT, text);
        if (containsAny(s, "yardim", "ne yapabilirsin", "komutlar")) return new ParsedIntent(Intent.HELP, text);
        return new ParsedIntent(Intent.UNKNOWN, text);
    }
    private static boolean containsAny(String s, String... choices) { for (String c : choices) if (s.contains(c)) return true; return false; }
    private static String normalize(String input) {
        String s = input.toLowerCase(new Locale("tr", "TR")).replace('ı','i').replace('ğ','g').replace('ü','u').replace('ş','s').replace('ö','o').replace('ç','c');
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
