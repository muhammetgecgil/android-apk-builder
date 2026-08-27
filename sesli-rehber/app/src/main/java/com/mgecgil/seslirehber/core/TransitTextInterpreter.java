package com.mgecgil.seslirehber.core;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative OCR interpreter for bus-stop / line-code candidates. */
public final class TransitTextInterpreter {
    private static final Pattern ROUTE = Pattern.compile(
            "\\b(?:[A-Z]{1,3}-?\\d{1,3}[A-Z]?|\\d{1,3}[A-Z]{1,2})\\b");

    private TransitTextInterpreter() {}

    public static String interpret(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return "";
        String upper = Normalizer.normalize(rawText.toUpperCase(new Locale("tr", "TR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('İ', 'I').replace('Ş', 'S').replace('Ğ', 'G')
                .replace('Ü', 'U').replace('Ö', 'O').replace('Ç', 'C');
        String lower = upper.toLowerCase(Locale.US);
        boolean transitContext = containsAny(lower,
                "durak", "iett", "otobus", "otobüs", "bus stop", "peron", "hat", "platform");
        Matcher matcher = ROUTE.matcher(upper);
        String route = matcher.find() ? matcher.group().replace(" ", "") : "";
        if (!route.isEmpty()) {
            return "Toplu taşıma hattı olabilecek " + route
                    + " yazısı görülüyor. Araca binmeden durak tabelası ve araç göstergesiyle doğrula.";
        }
        if (transitContext) {
            return "Durak veya toplu taşıma tabelası olabilecek yazı görülüyor. Hat numarasını ayrıca doğrula.";
        }
        return "";
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }
}
