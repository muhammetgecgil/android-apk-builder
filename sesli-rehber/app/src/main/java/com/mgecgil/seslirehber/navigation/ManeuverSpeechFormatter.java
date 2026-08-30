package com.mgecgil.seslirehber.navigation;

import static com.mgecgil.seslirehber.navigation.NavigationModels.Maneuver;

/**
 * Turkish pedestrian maneuver wording with a hard safety-language boundary.
 * Route guidance may say turn/continue, but it never claims a road crossing is safe.
 */
public final class ManeuverSpeechFormatter {
    private ManeuverSpeechFormatter() {}

    public static String prepare(Maneuver maneuver, double meters) {
        String action = action(maneuver);
        int rounded = Math.max(5, (int) Math.round(meters / 5d) * 5);
        return rounded + " metre sonra " + lowerFirst(action);
    }

    public static String now(Maneuver maneuver) {
        return action(maneuver);
    }

    private static String action(Maneuver m) {
        String street = cleanStreet(m.streetName());
        String suffix = street.isEmpty() ? "." : " ve " + street + " üzerinde ilerle.";
        return switch (m.type()) {
            case 2, 9, 10, 11, 18, 20, 23, 37 -> "Sağa yönel" + suffix;
            case 3, 14, 15, 16, 19, 21, 24, 38 -> "Sola yönel" + suffix;
            case 12, 13 -> "Rota geri dönüş istiyor. Çevreni bastonla doğrula ve uygun noktada yön değiştir.";
            case 26 -> "Döner kavşağa yaklaşıyorsun. Çevreyi bastonla doğrula ve rota yönünü takip et.";
            case 27 -> "Döner kavşak çıkışında rota yönünü takip et" + suffix;
            case 39 -> "Rota asansör kullanımına yönlendiriyor. Girişi doğrula.";
            case 40 -> "Rota merdivene yönlendiriyor. Basamakları bastonla doğrula.";
            case 41 -> "Rota yürüyen merdivene yönlendiriyor. Girişi doğrula.";
            case 42 -> "Rota bina girişine yönlendiriyor. Girişi doğrula.";
            case 43 -> "Rota bina çıkışına yönlendiriyor. Çıkışı doğrula.";
            case 4, 5, 6 -> "Hedef noktasına yaklaşıyorsun.";
            case 1, 7, 8, 17, 22, 25 -> street.isEmpty()
                    ? "Rota doğrultusunda ilerle."
                    : street + " üzerinde rota doğrultusunda ilerle.";
            default -> street.isEmpty()
                    ? "Rota doğrultusunda ilerle."
                    : street + " üzerinde rota doğrultusunda ilerle.";
        };
    }

    private static String cleanStreet(String street) {
        if (street == null) return "";
        String s = street.replaceAll("\\s+", " ").trim();
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }

    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) return "rota yönünü takip et.";
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
