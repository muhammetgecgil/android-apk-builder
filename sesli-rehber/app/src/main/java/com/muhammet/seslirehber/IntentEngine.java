package com.muhammet.seslirehber;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntentEngine {
    private static long pendingAt;

    private static final Locale f0TR = new Locale("tr", "TR");
    private static String pending = "";
    private static String slot = "";

    public static final class Result {
        public final String clarification;
        public final String command;
        public final float confidence;
        public final String intent;

        Result(String str, String str2, float f, String str3) {
            this.command = str;
            this.intent = str2;
            this.confidence = f;
            this.clarification = str3;
        }
    }

    private IntentEngine() {
    }

    public static boolean hasPending() {
        expire();
        return !pending.isEmpty();
    }

    public static void cancelPending() {
        pending = "";
        slot = "";
        pendingAt = 0L;
    }

    public static Result understand(String str) {
        String str2;
        String str3;
        expire();
        String strTrim = str == null ? "" : str.trim();
        String strClean = clean(fold(strTrim.toLowerCase(f0TR).replace((char) 8217, '\'')));
        if (strClean.isEmpty()) {
            return unknown(strTrim);
        }
        String str4 = strTrim;
        if (pending.isEmpty()) {
            str2 = str4;
        } else {
            String str5 = pending;
            String str6 = slot;
            cancelPending();
            if (str5.equals("CALL_TARGET")) {
                return hit(stripPerson(strClean) + " ara", "CALL", 0.93f);
            }
            if (str5.equals("NAV_DEST")) {
                return hit(stripDative(strClean) + " yol tarifi", "NAVIGATE", 0.93f);
            }
            if (str5.equals("APP_NAME")) {
                return hit(strClean + " aç", "OPEN_APP", 0.91f);
            }
            if (str5.equals("REMINDER_TIME")) {
                return hit(strClean + " alarm kur", "REMINDER", 0.89f);
            }
            if (str5.equals("MESSAGE_TARGET")) {
                pending = "MESSAGE_BODY";
                slot = stripPerson(strClean);
                pendingAt = System.currentTimeMillis();
                return clarify(str4, "MESSAGE", "Mesaj ne olsun?", 0.78f);
            }
            str2 = str4;
            if (str5.equals("MESSAGE_BODY")) {
                return hit(str6 + " mesaj gönder " + str2, "MESSAGE", 0.94f);
            }
        }
        if (matches(strClean, "saat kac", "kac oldu", "simdi saat", "zamani soyle", "saati merak")) {
            return hit("saati söyle", "TIME", 0.99f);
        }
        if (matches(strClean, "bugun hangi gun", "ayin kaci", "bugunun tarihi", "tarihi soyle", "hangi tarihteyiz")) {
            return hit("tarihi söyle", "DATE", 0.99f);
        }
        if (matches(strClean, "sarjim ne kadar", "ne kadar sarjim", "telefon ne kadar dayanir", "pilim kac", "batarya kac")) {
            return hit("pil kaç", "BATTERY", 0.97f);
        }
        if (matches(strClean, "onumu goremiyorum", "karanlik oldu", "isik lazim", "etraf karanlik", "biraz aydinlat") || (fuzzyHas(strClean, "feneri", 2) && strClean.contains("ac"))) {
            return hit("feneri aç", "TORCH_ON", 0.95f);
        }
        if (matches(strClean, "artik aydinlik", "isiga gerek yok", "fener gerekmiyor", "karanlik degil") || (fuzzyHas(strClean, "feneri", 2) && strClean.contains("kapat"))) {
            return hit("feneri kapat", "TORCH_OFF", 0.94f);
        }
        if (matches(strClean, "duyamiyorum", "ses cok az", "biraz daha yuksek", "sesi artir")) {
            return hit("sesi yükselt", "VOLUME_UP", 0.92f);
        }
        if (matches(strClean, "ses cok yuksek", "biraz sessiz", "basimi agritti", "sesi dusur")) {
            return hit("sesi azalt", "VOLUME_DOWN", 0.91f);
        }
        if (matches(strClean, "onumde ne var", "etrafta ne var", "cevre nasil", "nereden yuruyeyim", "yolum acik mi", "guvenli yol neresi", "guvenli yuruyus", "hareket gorus")) {
            return hit("çevreyi anlat", "SCENE", 0.96f);
        }
        if (matches(strClean, "bu ne", "elimde ne var", "ne tutuyorum", "bunu tani", "bu nesne ne")) {
            return hit("nesneyi tanı", "OBJECT", 0.94f);
        }
        if (matches(strClean, "bu kac para", "hangi banknot", "paranin degeri", "bu para kac lira")) {
            return hit("parayı tanı", "MONEY", 0.98f);
        }
        if (matches(strClean, "burada ne yaziyor", "yaziyi oku", "etiketi oku", "tabelada ne yaziyor")) {
            return hit("kamerayla bak", "READ_TEXT", 0.93f);
        }
        if (matches(strClean, "bu mesaj guvenli mi", "dolandirici olabilir mi", "benden para istiyor", "linke tiklayayim mi", "sifre istiyor", "iban gondermis", "telefonuma kod geldi")) {
            return hit("panoyu dolandırıcılık için kontrol et", "FRAUD_CHECK", 0.96f);
        }
        if (matches(strClean, "dustum", "basim dertte", "yardima ihtiyacim var", "kendimi guvende hissetmiyorum", "acil yardim lazim")) {
            return hit("yardım et", "EMERGENCY", 0.96f);
        }
        Matcher matcher = Pattern.compile("^(.+?)(?:ye|ya|'e|'a|e|a) (?:nasil giderim|gitmek istiyorum|goturur musun|gidecegim|rota ciz|yol bul)$").matcher(strClean);
        if (matcher.find()) {
            return hit(stripDative(matcher.group(1)) + " yol tarifi", "NAVIGATE", 0.94f);
        }
        Matcher matcher2 = Pattern.compile("^(.+?) (?:nerede|nerededir|hangi tarafta)$").matcher(strClean);
        if (matcher2.find()) {
            return hit(matcher2.group(1).trim() + " yol tarifi", "NAVIGATE", 0.86f);
        }
        if (matches(strClean, "kayboldum", "yolumu bulamiyorum", "nereye gidecegimi bilmiyorum", "yol tarifi ver", "beni gotur")) {
            return begin("NAV_DEST", "", "NAVIGATE", "Nereye gitmek istediğinizi söyleyin.");
        }
        Matcher matcher3 = Pattern.compile("^(.+?)(?:ye|ya|'e|'a|e|a) (?:ulas|baglan|telefon et|sesini duymak istiyorum|arama yap|arayabilir misin)$").matcher(strClean);
        if (matcher3.find()) {
            return hit(stripPerson(matcher3.group(1)) + " ara", "CALL", 0.95f);
        }
        Matcher matcher4 = Pattern.compile("^(.+?)(?:yle|la|le) (?:konusmak istiyorum|konustur|goruseyim)$").matcher(strClean);
        if (matcher4.find()) {
            return hit(stripPerson(matcher4.group(1)) + " ara", "CALL", 0.94f);
        }
        Matcher matcher5 = Pattern.compile("^(.+?)(?:yi|i) ara(?:yabilir misin|r misin)?$").matcher(strClean);
        if (matcher5.find()) {
            return hit(stripPerson(matcher5.group(1)) + " ara", "CALL", 0.96f);
        }
        if (matches(strClean, "birini ara", "birisini ara", "onu ara", "arama yap", "telefon ac")) {
            return begin("CALL_TARGET", "", "CALL", "Kimi aramak istediğinizi söyleyin.");
        }
        Matcher matcher6 = Pattern.compile("^(.+?)(?:ye|ya|'e|'a|e|a) (?:haber ver|soyle|mesaj at) (.+)$").matcher(strClean);
        if (matcher6.find()) {
            return hit(stripPerson(matcher6.group(1)) + " mesaj gönder " + matcher6.group(2).trim(), "MESSAGE", 0.93f);
        }
        Matcher matcher7 = Pattern.compile("^(.+?)(?:ye|ya|'e|'a|e|a) (?:haber ver|mesaj at|mesaj gonder)$").matcher(strClean);
        if (matcher7.find()) {
            pending = "MESSAGE_BODY";
            slot = stripPerson(matcher7.group(1));
            pendingAt = System.currentTimeMillis();
            return clarify(str2, "MESSAGE", "Mesaj ne olsun?", 0.86f);
        }
        if (matches(strClean, "mesaj at", "haber ver", "mesaj gonder", "sms gonder")) {
            return begin("MESSAGE_TARGET", "", "MESSAGE", "Kime mesaj göndermek istediğinizi söyleyin.");
        }
        if (Pattern.compile("(?:saat )?(\\d{1,2})(?:[.: ](\\d{2}))?.*(unutmayayim|unutmamaliyim|hatirlat|ilac|randevu)").matcher(strClean).find()) {
            return hit(str2 + " alarm kur", "REMINDER", 0.94f);
        }
        if (Pattern.compile("(unutmayayim|unutmamaliyim|hatirlat|ilacim var|randevum var)").matcher(strClean).find()) {
            return begin("REMINDER_TIME", "", "REMINDER", "Saat kaçta hatırlatayım?");
        }
        Matcher matcher8 = Pattern.compile("^(.+?)(?:ya|ye|'a|'e) (?:gec|git|ac onu|beni sok)$").matcher(strClean);
        if (matcher8.find()) {
            return hit(matcher8.group(1).trim() + " aç", "OPEN_APP", 0.88f);
        }
        if (matches(strClean, "uygulama ac", "bir uygulama ac")) {
            return begin("APP_NAME", "", "OPEN_APP", "Hangi uygulamayı açayım?");
        }
        if (fuzzyHas(strClean, "whatsapp", 2)) {
            str3 = "ac";
            if (matches(strClean, str3, "gec", "gir")) {
                return hit("whatsapp aç", "OPEN_APP", 0.91f);
            }
        } else {
            str3 = "ac";
        }
        return (fuzzyHas(strClean, "kamera", 2) && matches(strClean, str3, "bak", "goster")) ? hit("çevreyi anlat", "SCENE", 0.87f) : unknown(str2);
    }

    private static Result begin(String str, String str2, String str3, String str4) {
        pending = str;
        slot = str2;
        pendingAt = System.currentTimeMillis();
        return clarify("", str3, str4, 0.65f);
    }

    private static Result hit(String str, String str2, float f) {
        return new Result(str, str2, f, null);
    }

    private static Result clarify(String str, String str2, String str3, float f) {
        return new Result(str, str2, f, str3);
    }

    private static Result unknown(String str) {
        return new Result(str, "UNKNOWN", 0.42f, null);
    }

    private static void expire() {
        if (pending.isEmpty() || System.currentTimeMillis() - pendingAt <= 45000) {
            return;
        }
        cancelPending();
    }

    private static boolean matches(String str, String... strArr) {
        for (String str2 : strArr) {
            if (str.contains(str2)) {
                return true;
            }
        }
        return false;
    }

    private static String stripPerson(String str) {
        return str.replaceFirst("^(lutfen|hemen) ", "").replaceFirst(" (lutfen)$", "").trim();
    }

    private static String stripDative(String str) {
        return str.replaceFirst("(ya|ye|'a|'e)$", "").trim();
    }

    private static String clean(String str) {
        return str.replaceAll("[?!,;]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String fold(String str) {
        return str.replace("ı", "i").replace("ş", "s").replace("ğ", "g").replace("ü", "u").replace("ö", "o").replace("ç", "c");
    }

    private static boolean fuzzyHas(String str, String str2, int i) {
        for (String str3 : str.split(" ")) {
            if (distance(str3, str2) <= i) {
                return true;
            }
        }
        return false;
    }

    private static int distance(String str, String str2) {
        int[] iArr = new int[str2.length() + 1];
        int[] iArr2 = new int[str2.length() + 1];
        for (int i = 0; i <= str2.length(); i++) {
            iArr[i] = i;
        }
        int i2 = 1;
        while (i2 <= str.length()) {
            iArr2[0] = i2;
            for (int i3 = 1; i3 <= str2.length(); i3++) {
                int i4 = i3 - 1;
                iArr2[i3] = Math.min(Math.min(iArr2[i4] + 1, iArr[i3] + 1), iArr[i4] + (str.charAt(i2 + (-1)) == str2.charAt(i4) ? 0 : 1));
            }
            i2++;
            int[] iArr3 = iArr2;
            iArr2 = iArr;
            iArr = iArr3;
        }
        return iArr[str2.length()];
    }
}
