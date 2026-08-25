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
            "Yorulma – Goodman",
            "Pim – kesme + yatak basıncı",
            "Kolon – Euler burkulma",
            "Kiriş – basit mesnet orta yük",
            "Mil – burulma açısı",
            "Güç vidası – kare diş",
            "Diş sıyırma – ön kontrol"
    };

    public static final String[][] LABELS = {
            {"Eksenel yük F (N)", "Kesme yükü V (N)", "Civata çapı d (mm)", "Akma dayanımı Sy (MPa)", "", ""},
            {"Eğilme momenti M (N·m)", "Tork T (N·m)", "Mil çapı d (mm)", "Akma dayanımı Sy (MPa)", "", ""},
            {"Dinamik kapasite C (N)", "Eşdeğer yük P (N)", "Devir n (rpm)", "Üs p (3 bilyalı / 3.333 makaralı)", "", ""},
            {"Teğetsel kuvvet Ft (N)", "Yüz genişliği b (mm)", "Modül m (mm)", "Lewis Y", "İzin verilen gerilme (MPa)", ""},
            {"Kuvvet F (N)", "Ortalama çap D (mm)", "Tel çapı d (mm)", "Aktif sarım Na", "G (MPa)", "Ssy (MPa)"},
            {"Tork T (N·m)", "Mil çapı d (mm)", "Kama genişliği b (mm)", "Kama yüksekliği h (mm)", "Kama boyu L (mm)", "İzin verilen (MPa)"},
            {"Kuvvet F (N)", "Kaynak ayağı a (mm)", "Etkin uzunluk L (mm)", "İzin verilen kayma (MPa)", "", ""},
            {"Alternatif gerilme σa (MPa)", "Ortalama gerilme σm (MPa)", "Düzeltilmiş Se (MPa)", "Çekme dayanımı Sut (MPa)", "", ""},
            {"Kuvvet F (N)", "Pim çapı d (mm)", "Kulak kalınlığı t (mm)", "Kesme düzlemi (1/2)", "İzin verilen kayma (MPa)", "İzin verilen yatak (MPa)"},
            {"Eksenel basma P (N)", "E (MPa)", "Atalet I (mm⁴)", "Boy L (mm)", "Etkin boy katsayısı K", "Hedef emniyet katsayısı"},
            {"Orta nokta yükü F (N)", "Açıklık L (mm)", "E (MPa)", "Atalet I (mm⁴)", "Kesit modülü Z (mm³)", "Akma Sy (MPa)"},
            {"Tork T (N·m)", "Boy L (mm)", "Çap d (mm)", "G (MPa)", "İzin verilen açı (deg)", ""},
            {"Yük W (N)", "Ortalama çap dm (mm)", "Lead l (mm)", "Sürtünme μ", "Yaka ort. çap dc (mm)", "Yaka sürtünme μc"},
            {"Eksenel yük F (N)", "Ortalama diş çapı d2 (mm)", "Kavrama boyu Le (mm)", "Etkin diş payı q (0-1)", "İzin verilen kayma (MPa)", "Hedef emniyet katsayısı"}
    };

    private static double pos(double x, String name) {
        if (!(x > 0.0) || Double.isNaN(x) || Double.isInfinite(x))
            throw new IllegalArgumentException(name + " sıfırdan büyük olmalı.");
        return x;
    }

    private static double nonNeg(double x, String name) {
        if (x < 0.0 || Double.isNaN(x) || Double.isInfinite(x))
            throw new IllegalArgumentException(name + " negatif olamaz.");
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
            case 8: return pin(v);
            case 9: return euler(v);
            case 10: return beam(v);
            case 11: return torsion(v);
            case 12: return powerScrew(v);
            case 13: return threadStrip(v);
            default: throw new IllegalArgumentException("Geçersiz modül");
        }
    }

    private static Result bolt(double[] v) {
        double F = nonNeg(Math.abs(v[0]), "F"), V = nonNeg(Math.abs(v[1]), "V"), d = pos(v[2], "Çap"), sy = pos(v[3], "Sy");
        double area = Math.PI * d * d / 4.0;
        double sigma = F / area;
        double tau = V / area;
        double vm = Math.sqrt(sigma * sigma + 3.0 * tau * tau);
        double fos = safeFos(sy, vm);
        return new Result("Birleşik civata kontrolü",
                fmt("Nominal alan", area, "mm²") + "\n" + fmt("Çekme gerilmesi", sigma, "MPa") + "\n" + fmt("Kesme gerilmesi", tau, "MPa") + "\n" + fmt("von Mises", vm, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Seviye B ön kontrol. Detaylı sürüm tensile stress area, ön yük, bağlantı rijitliği, ayrılma, yorulma, diş sıyırma ve kayma kontrollerini ayrı yürütecek.");
    }

    private static Result shaft(double[] v) {
        double M = nonNeg(Math.abs(v[0]), "M") * 1000.0, T = nonNeg(Math.abs(v[1]), "T") * 1000.0, d = pos(v[2], "Çap"), sy = pos(v[3], "Sy");
        double sigma = 32.0 * M / (Math.PI * Math.pow(d, 3));
        double tau = 16.0 * T / (Math.PI * Math.pow(d, 3));
        double vm = Math.sqrt(sigma * sigma + 3.0 * tau * tau);
        double fos = safeFos(sy, vm);
        return new Result("Mil statik dayanım",
                fmt("Eğilme gerilmesi", sigma, "MPa") + "\n" + fmt("Burulma kayması", tau, "MPa") + "\n" + fmt("von Mises", vm, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Dolu dairesel mil ön kontrolüdür. Omuz, kama kanalı, reaksiyonlar, sehim, kritik hız ve yorulma ayrı doğrulama katmanıdır.");
    }

    private static Result bearing(double[] v) {
        double C = pos(v[0], "C"), P = pos(v[1], "P"), rpm = pos(v[2], "Devir"), p = pos(v[3], "Üs");
        double l10m = Math.pow(C / P, p);
        double rev = l10m * 1_000_000.0;
        double hours = rev / (60.0 * rpm);
        return new Result("Rulman temel ömür",
                fmt("L10", l10m, "milyon dev") + "\n" + fmt("Toplam devir", rev, "dev") + "\n" + fmt("L10h", hours, "saat"),
                hours >= 10000 ? "UZUN ÖMÜR" : hours >= 3000 ? "ORTA ÖMÜR" : "KISA ÖMÜR",
                "ISO 281 temel L10 yaklaşımıyla uyumlu ön hesap. Gelişmiş yolda eşdeğer radial/axial yük, güvenilirlik, yağlama, kirlilik, değişken görev ve C0 statik kontrolü bulunacak.");
    }

    private static Result gear(double[] v) {
        double ft = pos(Math.abs(v[0]), "Ft"), b = pos(v[1], "b"), m = pos(v[2], "m"), y = pos(v[3], "Y"), allow = pos(v[4], "İzin verilen gerilme");
        double sigma = ft / (b * m * y);
        double fos = safeFos(allow, sigma);
        return new Result("Dişli diş dibi – Lewis",
                fmt("Diş dibi eğilme", sigma, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Yalnız ön boyutlandırmadır. ISO 6336 yönelimli detaylı modda kök eğilme ve temas gerilmesi ayrı kontrol edilecek.");
    }

    private static Result spring(double[] v) {
        double F = pos(Math.abs(v[0]), "F"), D = pos(v[1], "D"), d = pos(v[2], "d"), na = pos(v[3], "Na"), G = pos(v[4], "G"), ssy = pos(v[5], "Ssy");
        double c = D / d;
        if (c <= 1.0) throw new IllegalArgumentException("Yay indeksi D/d > 1 olmalı.");
        double kw = (4.0 * c - 1.0) / (4.0 * c - 4.0) + 0.615 / c;
        double tau = kw * 8.0 * F * D / (Math.PI * Math.pow(d, 3));
        double k = G * Math.pow(d, 4) / (8.0 * Math.pow(D, 3) * na);
        double defl = F / k;
        double fos = safeFos(ssy, tau);
        return new Result("Helisel basma yayı",
                fmt("Yay indeksi C", c, "") + "\n" + fmt("Wahl K", kw, "") + "\n" + fmt("Maks. kayma", tau, "MPa") + "\n" + fmt("Yay katsayısı k", k, "N/mm") + "\n" + fmt("Sehim", defl, "mm") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Statik ön boyutlandırma. Burkulma, solid boy, clash allowance, set alma ve yorulma geliştirme planındadır.");
    }

    private static Result key(double[] v) {
        double T = pos(Math.abs(v[0]), "T") * 1000.0, d = pos(v[1], "d"), b = pos(v[2], "b"), h = pos(v[3], "h"), L = pos(v[4], "L"), allow = pos(v[5], "İzin verilen");
        double shear = 2.0 * T / (d * b * L);
        double crush = 4.0 * T / (d * h * L);
        double governing = Math.max(shear, crush);
        double fos = safeFos(allow, governing);
        return new Result("Paralel kama kontrolü",
                fmt("Kesme gerilmesi", shear, "MPa") + "\n" + fmt("Ezilme gerilmesi", crush, "MPa") + "\n" + fmt("Kritik gerilme", governing, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Ön kontrol. Kama ve göbek için ayrı izin verilen gerilmeler ile tercih edilen standart ölçüler detaylı sürümde seçilecek.");
    }

    private static Result weld(double[] v) {
        double F = nonNeg(Math.abs(v[0]), "F"), a = pos(v[1], "a"), L = pos(v[2], "L"), allow = pos(v[3], "İzin verilen kayma");
        double throat = 0.70710678118 * a;
        double tau = F / (throat * L);
        double fos = safeFos(allow, tau);
        return new Result("Köşe kaynağı statik kontrol",
                fmt("Etkin boğaz", throat, "mm") + "\n" + fmt("Kayma gerilmesi", tau, "MPa") + "\n" + fmt("Emniyet katsayısı", fos, ""),
                verdict(fos), "Tek doğrusal kaynak ön kontrolüdür. Kaynak grubu, eksantrik yük, çoklu segment ve yorulma ayrı modüllere ayrılacaktır.");
    }

    private static Result goodman(double[] v) {
        double sa = nonNeg(Math.abs(v[0]), "σa"), sm = v[1], se = pos(v[2], "Se"), sut = pos(v[3], "Sut");
        double invN = sa / se + sm / sut;
        if (invN <= 0.0) throw new IllegalArgumentException("Goodman toplamı pozitif çıkmadı; girişleri kontrol edin.");
        double n = 1.0 / invN;
        return new Result("Goodman yorulma kontrolü",
                fmt("σa / Se", sa / se, "") + "\n" + fmt("σm / Sut", sm / sut, "") + "\n" + fmt("Emniyet katsayısı", n, ""),
                verdict(n), "Se girdisi yüzey, boyut, yük, sıcaklık ve güvenilirlik etkileriyle düzeltilmiş olmalıdır. Marin katsayıları ayrı modüle taşınacaktır.");
    }

    private static Result pin(double[] v) {
        double F = pos(Math.abs(v[0]), "F"), d = pos(v[1], "d"), t = pos(v[2], "t"), planes = pos(v[3], "Kesme düzlemi"), allowShear = pos(v[4], "İzin verilen kayma"), allowBearing = pos(v[5], "İzin verilen yatak");
        if (Math.abs(planes - 1.0) > 1e-9 && Math.abs(planes - 2.0) > 1e-9)
            throw new IllegalArgumentException("Kesme düzlemi yalnız 1 veya 2 olabilir.");
        double area = planes * Math.PI * d * d / 4.0;
        double tau = F / area;
        double bearing = F / (d * t);
        double fsShear = safeFos(allowShear, tau);
        double fsBearing = safeFos(allowBearing, bearing);
        double governing = Math.min(fsShear, fsBearing);
        return new Result("Pim ve kulak ön kontrolü",
                fmt("Pim kesme", tau, "MPa") + "\n" + fmt("Yatak basıncı", bearing, "MPa") + "\n" + fmt("Kesme FoS", fsShear, "") + "\n" + fmt("Yatak FoS", fsBearing, "") + "\n" + "Yöneten: " + (fsShear <= fsBearing ? "PİM KESME" : "YATAK BASINCI"),
                verdict(governing), "Lug net-section, shear-out, pim eğilmesi ve temas dağılımı henüz bu ön kontrolde yoktur.");
    }

    private static Result euler(double[] v) {
        double P = pos(Math.abs(v[0]), "P"), E = pos(v[1], "E"), I = pos(v[2], "I"), L = pos(v[3], "L"), K = pos(v[4], "K"), target = pos(v[5], "Hedef FoS");
        double le = K * L;
        double pcr = Math.PI * Math.PI * E * I / (le * le);
        double fos = pcr / P;
        String status = fos >= target ? "UYGUN – hedef burkulma marjı sağlandı" : "UYGUN DEĞİL – hedef marj sağlanmadı";
        return new Result("Euler elastik burkulma",
                fmt("Etkin boy", le, "mm") + "\n" + fmt("Kritik yük Pcr", pcr, "N") + "\n" + fmt("Burkulma FoS", fos, "") + "\n" + fmt("Hedef FoS", target, ""),
                status, "Euler yalnız uzun, ideal, elastik kolon bölgesinde uygundur. Kısa/orta kolonlarda malzeme ve inelastik burkulma yöntemi seçilmelidir.");
    }

    private static Result beam(double[] v) {
        double F = pos(Math.abs(v[0]), "F"), L = pos(v[1], "L"), E = pos(v[2], "E"), I = pos(v[3], "I"), Z = pos(v[4], "Z"), sy = pos(v[5], "Sy");
        double mmax = F * L / 4.0;
        double sigma = mmax / Z;
        double defl = F * Math.pow(L, 3) / (48.0 * E * I);
        double fos = safeFos(sy, sigma);
        return new Result("Basit mesnetli kiriş – orta tekil yük",
                fmt("Maks. moment", mmax / 1000.0, "N·m") + "\n" + fmt("Maks. eğilme", sigma, "MPa") + "\n" + fmt("Orta nokta sehim", defl, "mm") + "\n" + fmt("Akma FoS", fos, ""),
                verdict(fos), "Bu modül yalnız iki basit mesnet ve tam orta noktadaki tekil yük içindir; sehim kabul limiti kullanıcı projesine göre ayrıca değerlendirilmelidir.");
    }

    private static Result torsion(double[] v) {
        double T = pos(Math.abs(v[0]), "T") * 1000.0, L = pos(v[1], "L"), d = pos(v[2], "d"), G = pos(v[3], "G"), allowDeg = pos(v[4], "İzin verilen açı");
        double J = Math.PI * Math.pow(d, 4) / 32.0;
        double phiRad = T * L / (J * G);
        double phiDeg = Math.toDegrees(phiRad);
        double tau = 16.0 * T / (Math.PI * Math.pow(d, 3));
        String status = phiDeg <= allowDeg ? "UYGUN – burulma rijitliği" : "UYGUN DEĞİL – açı limiti aşıldı";
        return new Result("Dolu mil burulma rijitliği",
                fmt("Polar atalet J", J, "mm⁴") + "\n" + fmt("Maks. kayma", tau, "MPa") + "\n" + fmt("Burulma açısı", phiDeg, "deg") + "\n" + fmt("İzin verilen", allowDeg, "deg"),
                status, "Rijitlik kontrolüdür; dayanım için malzeme izin verilen kayma/akma değeri ayrıca kontrol edilmelidir.");
    }

    private static Result powerScrew(double[] v) {
        double W = pos(Math.abs(v[0]), "W"), dm = pos(v[1], "dm"), lead = pos(v[2], "lead"), mu = nonNeg(v[3], "μ"), dc = nonNeg(v[4], "dc"), muc = nonNeg(v[5], "μc");
        double lambda = Math.atan(lead / (Math.PI * dm));
        double phi = Math.atan(mu);
        if (lambda + phi >= Math.PI / 2.0) throw new IllegalArgumentException("Geometri/sürtünme kombinasyonu geçersiz.");
        double threadTorque = W * dm / 2.0 * Math.tan(phi + lambda);
        double collarTorque = W * muc * dc / 2.0;
        double total = threadTorque + collarTorque;
        double usefulPerRad = W * lead / (2.0 * Math.PI);
        double eta = usefulPerRad / total;
        boolean selfLock = phi > lambda;
        return new Result("Kare diş güç vidası – kaldırma",
                fmt("Lead açısı", Math.toDegrees(lambda), "deg") + "\n" + fmt("Diş torku", threadTorque / 1000.0, "N·m") + "\n" + fmt("Yaka torku", collarTorque / 1000.0, "N·m") + "\n" + fmt("Toplam tork", total / 1000.0, "N·m") + "\n" + fmt("Verim", eta * 100.0, "%") + "\n" + "Kendinden kilitleme eğilimi: " + (selfLock ? "EVET" : "HAYIR"),
                "BİLGİSEL – dayanım kontrolleri ayrıca gerekli", "Kare diş ve basitleştirilmiş yaka sürtünmesi varsayımı. Vida çekirdek gerilmesi, burkulma, diş basıncı ve kritik hız ayrı kontrollerdir.");
    }

    private static Result threadStrip(double[] v) {
        double F = pos(Math.abs(v[0]), "F"), d2 = pos(v[1], "d2"), Le = pos(v[2], "Le"), q = pos(v[3], "q"), allow = pos(v[4], "İzin verilen kayma"), target = pos(v[5], "Hedef FoS");
        if (q > 1.0) throw new IllegalArgumentException("Etkin diş payı q 0 ile 1 arasında olmalı.");
        double shearArea = Math.PI * d2 * Le * q;
        double tau = F / shearArea;
        double fos = safeFos(allow, tau);
        String status = fos >= target ? "UYGUN – hedef diş marjı sağlandı" : "UYGUN DEĞİL – diş kavraması yetersiz";
        return new Result("Diş sıyırma – ön kontrol",
                fmt("Yaklaşık kesme alanı", shearArea, "mm²") + "\n" + fmt("Ortalama diş kayması", tau, "MPa") + "\n" + fmt("Diş FoS", fos, "") + "\n" + fmt("Hedef FoS", target, ""),
                status, "Bu bir geometri-temelli ön taramadır. Gerçek metrik/UN diş sıyırma hesabı iç/dış diş geometrisi, malzeme çifti ve seçilen standardın ayrıntılı kesme alanı modelini kullanmalıdır.");
    }

    private static double safeFos(double capacity, double demand) {
        if (demand == 0.0) return Double.POSITIVE_INFINITY;
        return capacity / demand;
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
        if (Double.isInfinite(value)) return name + ": ∞" + u;
        String pattern = Math.abs(value) >= 1e6 || (Math.abs(value) > 0 && Math.abs(value) < 1e-3) ? "%.5e" : "%.5g";
        return String.format(Locale.US, "%s: " + pattern + "%s", name, value, u);
    }

    public static final class Result {
        public final String title;
        public final String body;
        public final String status;
        public final String note;
        Result(String title, String body, String status, String note) {
            this.title = title;
            this.body = body;
            this.status = status;
            this.note = note;
        }
    }
}
