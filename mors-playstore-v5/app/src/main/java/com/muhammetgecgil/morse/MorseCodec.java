package com.muhammetgecgil.morse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MorseCodec {
    private static final Map<String, String> CHAR_TO_MORSE;
    private static final Map<String, String> MORSE_TO_CHAR;

    static {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        putLetters(m);
        putNumbers(m);
        putTurkish(m);
        putPunctuation(m);
        CHAR_TO_MORSE = Collections.unmodifiableMap(m);

        LinkedHashMap<String, String> reverse = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : m.entrySet()) {
            reverse.putIfAbsent(e.getValue(), e.getKey());
        }
        MORSE_TO_CHAR = Collections.unmodifiableMap(reverse);
    }

    private MorseCodec() {}

    private static void putLetters(Map<String, String> m) {
        String[][] entries = {
                {"A", ".-"}, {"B", "-..."}, {"C", "-.-."}, {"D", "-.."},
                {"E", "."}, {"F", "..-."}, {"G", "--."}, {"H", "...."},
                {"I", ".."}, {"J", ".---"}, {"K", "-.-"}, {"L", ".-.."},
                {"M", "--"}, {"N", "-."}, {"O", "---"}, {"P", ".--."},
                {"Q", "--.-"}, {"R", ".-."}, {"S", "..."}, {"T", "-"},
                {"U", "..-"}, {"V", "...-"}, {"W", ".--"}, {"X", "-..-"},
                {"Y", "-.--"}, {"Z", "--.."}
        };
        for (String[] e : entries) m.put(e[0], e[1]);
    }

    private static void putNumbers(Map<String, String> m) {
        String[][] entries = {
                {"0", "-----"}, {"1", ".----"}, {"2", "..---"}, {"3", "...--"},
                {"4", "....-"}, {"5", "....."}, {"6", "-...."}, {"7", "--..."},
                {"8", "---.."}, {"9", "----."}
        };
        for (String[] e : entries) m.put(e[0], e[1]);
    }

    private static void putTurkish(Map<String, String> m) {
        m.put("Ç", "-.-..");
        m.put("Ğ", "--.-.");
        m.put("İ", "..-..");
        m.put("Ö", "---.");
        m.put("Ş", "----");
        m.put("Ü", "..--");
    }

    private static void putPunctuation(Map<String, String> m) {
        m.put(".", ".-.-.-");
        m.put(",", "--..--");
        m.put("?", "..--..");
        m.put("'", ".----.");
        m.put("!", "-.-.--");
        m.put("/", "-..-.");
        m.put("(", "-.--.");
        m.put(")", "-.--.-");
        m.put("&", ".-...");
        m.put(":", "---...");
        m.put(";", "-.-.-.");
        m.put("=", "-...-");
        m.put("+", ".-.-.");
        m.put("-", "-....-");
        m.put("_", "..--.-");
        m.put("\"", ".-..-.");
        m.put("$", "...-..-");
        m.put("@", ".--.-.");
    }

    public static String toMorse(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String upper = input.toUpperCase(Locale.forLanguageTag("tr-TR"));
        StringBuilder out = new StringBuilder();
        boolean previousWasSpace = true;

        for (int i = 0; i < upper.length(); i++) {
            String c = String.valueOf(upper.charAt(i));
            if (Character.isWhitespace(upper.charAt(i))) {
                if (!previousWasSpace && out.length() > 0) {
                    trimTrailingSpace(out);
                    out.append(" / ");
                }
                previousWasSpace = true;
                continue;
            }

            String code = CHAR_TO_MORSE.get(c);
            if (code == null) continue;

            if (out.length() > 0 && !endsWithWordSeparator(out) && out.charAt(out.length() - 1) != ' ') {
                out.append(' ');
            }
            out.append(code).append(' ');
            previousWasSpace = false;
        }
        trimTrailingSpace(out);
        trimTrailingWordSeparator(out);
        return out.toString();
    }

    public static String fromMorse(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String normalized = input.trim()
                .replace('|', '/')
                .replace('•', '.')
                .replace('·', '.')
                .replace('–', '-')
                .replace('—', '-');

        String[] words = normalized.split("\\s*/\\s*");
        StringBuilder out = new StringBuilder();
        for (int w = 0; w < words.length; w++) {
            if (w > 0) out.append(' ');
            String word = words[w].trim();
            if (word.isEmpty()) continue;
            String[] tokens = word.split("\\s+");
            for (String token : tokens) {
                if (token.isEmpty()) continue;
                String c = MORSE_TO_CHAR.get(token);
                out.append(c != null ? c : "?");
            }
        }
        return out.toString();
    }

    public static boolean looksLikeMorse(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        String t = input.trim();
        int valid = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '.' || c == '-' || c == '/' || Character.isWhitespace(c) || c == '•' || c == '·' || c == '–' || c == '—') {
                valid++;
            }
        }
        return valid == t.length();
    }

    private static void trimTrailingSpace(StringBuilder b) {
        while (b.length() > 0 && b.charAt(b.length() - 1) == ' ') b.deleteCharAt(b.length() - 1);
    }

    private static boolean endsWithWordSeparator(StringBuilder b) {
        int n = b.length();
        return n >= 3 && b.substring(n - 3).equals(" / ");
    }

    private static void trimTrailingWordSeparator(StringBuilder b) {
        while (b.length() >= 2) {
            String s = b.toString();
            if (s.endsWith(" /")) b.delete(b.length() - 2, b.length());
            else if (s.endsWith("/")) b.deleteCharAt(b.length() - 1);
            else break;
            trimTrailingSpace(b);
        }
    }
}
