package com.mg.machineelementspro;

import java.util.Locale;

public final class CalculationEngine {
    public static final String[] MODULES = {
            "Civata – birleşik çekme/kesme",
            "Mil – eğilme + burulma",
            "Rulman – L10 ömür",
            "Düz dişli – Lewis eğilme",
            "Basma yayı – gerilme + sehim",
            "Kama – kesme + ezilme",
            "Kaynak – köşe kaynağı",
            "Yorulma – Goodman"
    };

    public static final String[][] LABELS = {
            {"Eksenel yük F (N)", "Kesme yükü V (N)", "Civata çapı d (mm)", "Akma dayanımı Sy (MPa)", "", ""},
            {"Eğilme momenti M (N·m)", "Tork T (N·m)", "Mil çapı d (mm)", "Akma dayanımı Sy (MPa)", "", ""},
            {"Dinamik kapasite C (N)", "Eşdeğer yük P (N)", "Devir n (rpm)", "Üs p (3 bilyalı / 3.333 makaralı)", "", ""},
            {"Teğetsel kuvvet Ft (N)", "Yüz genişliği b (mm)", "Modül m (mm)", "Lewis Y", "İzin verilen gerilme (MPa)", ""},
            {"Kuvvet F (N)", "Ortalama çap D (mm)", "Tel çapı d (mm)", "Aktif sarım Na", "G (MPa)", "Ssy (MPa)"},
            {"Tork T (N·m)", "Mil çapı d (mm)", "Kama genişliği b (mm)", "Kama yüksekliği h (mm)", "Kama boyu L (mm)", "İzin verilen (MPa)"},
            {"Kuvvet F (N)", "Kaynak ayağı a (mm)", "Etkin uzunluk L (mm)", "İzin verilen kayma (MPa)", "", ""},
            {"Alternatif gerilme σa (MPa)", "Ortalama gerilme σm (MPa)", "Düzeltilmiş Se (MPa)", "Çekme dayanımı Sut (MPa)", "", ""}
    };

    private static double pos(double x, String name) {
        if (!(x > 0.0) || Double.isNaN(x) || Double.isInfinite(x)) throw new IllegalArgumentException(name + " sıfırdan büyük olmalı.");
        return x;
    }

    public static Result calculate(int module, double[] v) {
        switch (module) {
            case 0: return bolt(v);
            case 1: return shaft(v);
            case 2: return bearing(v);
            case 3: return gear(v);
            case 4: return spring(v);
            case 5: return key(v);
            case 6: return weld(v);
            case 7: return goodman(v);
            default: throw new IllegalArgumentException("Geçersiz modül");
        }
    }

    private static Result bolt(double[] v) {
        double F = Math.abs(v[0]), V = Math.abs(v[1]), d = pos(v[2], "Çap"), sy = pos(v[3], "Sy");
        double area = Math.PI * d * d / 4.0;
        double sigma = F / area;
        double tau = V / area;
        double vm = Math.sqrt(sigma * sigma + 3.0 * tau * tau);
        double fos = sy / vm;
        return new Result("Birleşik civata kontrolü",
                fmt("Çekme gerilmesi", sigma, "MPa") + "\n" + fmt("Kesme gerilmesi", tau, "MPa") + "\n" + fmt("von Mises", vm, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Çekme + tek kesme düzlemi; diş dibi, ön yük, yatak basıncı ve bağlantı rijitliği ileri sürümde ayrıca kontrol edilir.");
    }

    private static Result shaft(double[] v) {
        double M = Math.abs(v[0]) * 1000.0, T = Math.abs(v[1]) * 1000.0, d = pos(v[2], "Çap"), sy = pos(v[3], "Sy");
        double sigma = 32.0 * M / (Math.PI * Math.pow(d, 3));
        double tau = 16.0 * T / (Math.PI * Math.pow(d, 3));
        double vm = Math.sqrt(sigma * sigma + 3.0 * tau * tau);
        double fos = sy / vm;
        return new Result("Mil statik dayanım",
                fmt("Eğilme gerilmesi", sigma, "MPa") + "\n" + fmt("Burulma kayması", tau, "MPa") + "\n" + fmt("von Mises", vm, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Dolu dairesel mil. Omuz, kama kanalı ve yorulma çentik katsayıları ayrıca eklenebilir.");
    }

    private static Result bearing(double[] v) {
        double C = pos(v[0], "C"), P = pos(v[1], "P"), rpm = pos(v[2], "Devir"), p = pos(v[3], "Üs");
        double l10m = Math.pow(C / P, p);
        double rev = l10m * 1_000_000.0;
        double hours = rev / (60.0 * rpm);
        return new Result("Rulman temel ömür",
                fmt("L10", l10m, "milyon dev") + "\n" + fmt("Toplam devir", rev, "dev") + "\n" + fmt("L10h", hours, "saat"),
                hours >= 10000 ? "UZUN ÖMÜR" : hours >= 3000 ? "ORTA ÖMÜR" : "KISA ÖMÜR",
                "ISO 281 temel dinamik ömür yaklaşımı. Güvenilirlik, yağlama, kirlilik, sıcaklık ve statik C0 kontrolü ayrıca değerlendirilmelidir.");
    }

    private static Result gear(double[] v) {
        double ft = pos(Math.abs(v[0]), "Ft"), b = pos(v[1], "b"), m = pos(v[2], "m"), y = pos(v[3], "Y"), allow = pos(v[4], "İzin verilen gerilme");
        double sigma = ft / (b * m * y);
        double fos = allow / sigma;
        return new Result("Dişli diş dibi – Lewis",
                fmt("Diş dibi eğilme", sigma, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Ön boyutlandırma. AGMA/ISO hız, yük dağılımı, dinamik katsayılar ve temas gerilmesi ileri modülde uygulanmalıdır.");
    }

    private static Result spring(double[] v) {
        double F = pos(Math.abs(v[0]), "F"), D = pos(v[1], "D"), d = pos(v[2], "d"), na = pos(v[3], "Na"), G = pos(v[4], "G"), ssy = pos(v[5], "Ssy");
        double c = D / d;
        if (c <= 1.0) throw new IllegalArgumentException("Yay indeksi D/d > 1 olmalı.");
        double kw = (4.0 * c - 1.0) / (4.0 * c - 4.0) + 0.615 / c;
        double tau = kw * 8.0 * F * D / (Math.PI * Math.pow(d, 3));
        double k = G * Math.pow(d, 4) / (8.0 * Math.pow(D, 3) * na);
        double defl = F / k;
        double fos = ssy / tau;
        return new Result("Helisel basma yayı",
                fmt("Yay indeksi C", c, "") + "\n" + fmt("Wahl K", kw, "") + "\n" + fmt("Maks. kayma", tau, "MPa") + "\n" + fmt("Yay katsayısı k", k, "N/mm") + "\n" + fmt("Sehim", defl, "mm") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Statik ön boyutlandırma. Burkulma, solid boy, set alma ve yorulma ayrı kontrol edilmelidir.");
    }

    private static Result key(double[] v) {
        double T = pos(Math.abs(v[0]), "T") * 1000.0, d = pos(v[1], "d"), b = pos(v[2], "b"), h = pos(v[3], "h"), L = pos(v[4], "L"), allow = pos(v[5], "İzin verilen");
        double shear = 2.0 * T / (d * b * L);
        double crush = 4.0 * T / (d * h * L);
        double governing = Math.max(shear, crush);
        double fos = allow / governing;
        return new Result("Paralel kama kontrolü",
                fmt("Kesme gerilmesi", shear, "MPa") + "\n" + fmt("Ezilme gerilmesi", crush, "MPa") + "\n" + fmt("Kritik gerilme", governing, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Kama ve göbek malzemelerinin izin verilen gerilmeleri ayrı tanımlanırsa daha doğru sonuç verir.");
    }

    private static Result weld(double[] v) {
        double F = pos(Math.abs(v[0]), "F"), a = pos(v[1], "a"), L = pos(v[2], "L"), allow = pos(v[3], "İzin verilen kayma");
        double throat = 0.707 * a;
        double tau = F / (throat * L);
        double fos = allow / tau;
        return new Result("Köşe kaynağı statik kontrol",
                fmt("Etkin boğaz", throat, "mm") + "\n" + fmt("Kayma gerilmesi", tau, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Tek doğrusal kaynak için temel statik kontrol. Kaynak grubu momenti ve eksantrik yük ileri hesapta eklenmelidir.");
    }

    private static Result goodman(double[] v) {
        double sa = Math.abs(v[0]), sm = v[1], se = pos(v[2], "Se"), sut = pos(v[3], "Sut");
        double invN = sa / se + sm / sut;
        if (invN <= 0.0) throw new IllegalArgumentException("Goodman paydası pozitif çıkmadı; girişleri kontrol edin.");
        double n = 1.0 / invN;
        return new Result("Goodman yorulma kontrolü",
                fmt("σa / Se", sa / se, "") + "\n" + fmt("σm / Sut", sm / sut, "") + "\n" + fmt("Emniyet katsayısı", n, ""),
                verdict(n), "Se değeri yüzey, boyut, yük, sıcaklık ve güvenilirlik Marin katsayılarıyla düzeltilmiş olmalıdır.");
    }

    private static String verdict(double fos) {
        if (Double.isInfinite(fos)) return "YÜK YOK";
        if (fos >= 2.0) return "UYGUN – yüksek marj";
        if (fos >= 1.5) return "UYGUN – normal marj";
        if (fos >= 1.0) return "SINIRDA – yeniden değerlendir";
        return "UYGUN DEĞİL";
    }

    private static String fmt(String name, double value, String unit) {
        String u = unit.isEmpty() ? "" : " " + unit;
        return String.format(Locale.US, "%s: %.4g%s", name, value, u);
    }

    public static final class Result {
        public final String title;
        public final String body;
        public final String status;
        public final String note;
        Result(String title, String body, String status, String note) {
            this.title = title; this.body = body; this.status = status; this.note = note;
        }
    }
}
