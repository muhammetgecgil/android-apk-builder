package com.mg.hafizadostum.v4;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ProfileEngine {
    public static final String PREF = "hafiza_dostum_profile";
    public static final String[] PROFESSIONS = {
            "Mühendis", "Öğretmen", "Doktor", "Hemşire / Sağlık Çalışanı", "Tekniker / Teknisyen",
            "Yazılım / Bilgi Teknolojileri", "Yönetici", "Ofis Çalışanı", "Muhasebe / Finans", "Satın Alma",
            "Satış / Pazarlama", "İnsan Kaynakları", "Avukat / Hukuk", "Eczacı", "Mimar",
            "İnşaat / Saha", "Üretim / Operatör", "Kalite / Test", "Lojistik / Depo", "Şoför / Kurye",
            "Polis / Güvenlik", "İtfaiye / Acil Durum", "Pilot / Havacılık", "Akademisyen / Araştırmacı", "Tarım / Çiftçi",
            "Esnaf / İşletme Sahibi", "Aşçı / Gıda", "Bakım / Servis", "Öğrenci", "Freelancer / Evden Çalışan"
    };

    public static final String[] ROLES = {
            "Anne", "Baba", "Ev ve aile düzeni", "Ev / bütçe sorumluluğu", "Eş / partner",
            "Bakım veren", "Öğrenci / öğrenen", "Araç / ulaşım sorumluluğu", "Evcil hayvan sorumluluğu", "Sosyal / kişisel yaşam"
    };

    public static final String[] SUPPORT = {
            "Standart", "Unutkanlık desteği", "Yoğun hafıza desteği (demans / Alzheimer için)"
    };

    private ProfileEngine() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean isSaved(Context c) { return p(c).getBoolean("saved", false); }
    public static int profession(Context c) { return p(c).getInt("profession", 0); }
    public static String roles(Context c) { return p(c).getString("roles", ""); }
    public static int support(Context c) { return p(c).getInt("support", 0); }

    public static String summary(Context c) {
        int pi = Math.max(0, Math.min(PROFESSIONS.length - 1, profession(c)));
        String r = roles(c);
        String roleText = r.isEmpty() ? "Sosyal rol seçilmedi" : roleNames(r);
        return PROFESSIONS[pi] + " • " + roleText + " • " + SUPPORT[Math.max(0, Math.min(SUPPORT.length - 1, support(c)))];
    }

    private static String roleNames(String encoded) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ROLES.length; i++) {
            if (encoded.contains("|" + i + "|")) {
                if (b.length() > 0) b.append(", ");
                b.append(ROLES[i]);
            }
        }
        return b.length() == 0 ? "Sosyal rol seçilmedi" : b.toString();
    }

    public static void save(Context c, int profession, boolean[] roles, int support) {
        StringBuilder enc = new StringBuilder();
        for (int i = 0; i < roles.length; i++) if (roles[i]) enc.append('|').append(i).append('|');
        p(c).edit().putBoolean("saved", true).putInt("profession", profession)
                .putString("roles", enc.toString()).putInt("support", support).apply();
        c.getSharedPreferences("hafiza_dostum_ui", Context.MODE_PRIVATE).edit()
                .putBoolean("simple", support >= 2).apply();
        apply(c, profession, roles, support);
    }

    public static void applySaved(Context c) {
        if (!isSaved(c)) return;
        boolean[] rs = new boolean[ROLES.length];
        String e = roles(c);
        for (int i = 0; i < rs.length; i++) rs[i] = e.contains("|" + i + "|");
        apply(c, profession(c), rs, support(c));
    }

    private static void apply(Context c, int profession, boolean[] roles, int support) {
        clearGenerated(c);
        addProfession(c, profession);
        for (int i = 0; i < roles.length; i++) if (roles[i]) addRole(c, i);
        addSupport(c, support);
        ReminderScheduler.scheduleAll(c);
    }

    private static void clearGenerated(Context c) {
        JSONArray in = MemoryStore.getTasks(c), out = new JSONArray();
        for (int i = 0; i < in.length(); i++) {
            JSONObject t = in.optJSONObject(i);
            if (t != null && !t.optString("category", "").startsWith("profile_")) out.put(t);
        }
        MemoryStore.saveTasks(c, out);
    }

    private static void add(Context c, String name, int h, int m, boolean critical, String days, String category) {
        MemoryStore.addIfMissing(c, MemoryStore.task(MemoryStore.newId(), name, h, m, critical, days, "profile_" + category));
    }

    private static void addProfession(Context c, int i) {
        switch (i) {
            case 0: // Engineer
                add(c,"🧭 Bugünün teknik önceliklerini netleştirdim",8,10,false,"12345","engineer");
                add(c,"📐 Kritik ölçüm / veri / revizyon kaydını tamamladım",15,30,true,"12345","engineer");
                add(c,"⚠ Açık teknik risk ve aksiyonları güncelledim",17,20,false,"12345","engineer"); break;
            case 1:
                add(c,"📚 Bugünkü ders planı ve materyaller hazır",7,30,false,"12345","teacher");
                add(c,"👩‍🏫 Öğrenci takip / yoklama notlarını tamamladım",14,30,false,"12345","teacher");
                add(c,"📝 Ödev / geri bildirim ve yarın hazırlığını yaptım",18,0,false,"12345","teacher"); break;
            case 2:
                add(c,"🩺 Günün hasta / görev listesini gözden geçirdim",7,45,false,"12345","doctor");
                add(c,"📋 Kritik kayıt ve devir notlarını tamamladım",16,30,true,"12345","doctor");
                add(c,"☎ Bekleyen sonuç / geri dönüşleri kontrol ettim",18,0,false,"12345","doctor"); break;
            case 3:
                add(c,"🧾 Vardiya ve bakım planını kontrol ettim",7,30,false,"1234567","nurse");
                add(c,"✅ Uygulanan işlemlerin kayıtlarını tamamladım",15,0,true,"1234567","nurse");
                add(c,"🤝 Devir teslim notlarını hazırladım",18,30,false,"1234567","nurse"); break;
            case 4:
                add(c,"🧰 Takım / cihaz / yedek kontrolü yaptım",8,0,false,"12345","technician");
                add(c,"📏 Ölçüm ve servis kayıtlarını girdim",15,30,true,"12345","technician");
                add(c,"🔧 Açık bakım işlerini kapattım / devrettim",17,30,false,"12345","technician"); break;
            case 5:
                add(c,"💻 Günün 3 teknik hedefini seçtim",8,30,false,"12345","it");
                add(c,"🔐 Yedekleme / erişim / kritik servis kontrolü",16,0,true,"12345","it");
                add(c,"🧪 Kod / ticket / test durumunu güncelledim",17,30,false,"12345","it"); break;
            case 6:
                add(c,"🎯 Bugünün 3 karar ve önceliğini belirledim",8,15,false,"12345","manager");
                add(c,"👥 Bekleyen ekip aksiyonlarını kontrol ettim",14,30,false,"12345","manager");
                add(c,"📌 Yarın için açık karar ve riskleri not ettim",17,45,false,"12345","manager"); break;
            case 7:
                add(c,"📅 Takvim / toplantı / teslimleri gözden geçirdim",8,15,false,"12345","office");
                add(c,"📨 Bekleyen önemli e-posta / aksiyonları kapattım",15,30,false,"12345","office");
                add(c,"🗂 Masaüstü / belge / yarın planını toparladım",17,30,false,"12345","office"); break;
            case 8:
                add(c,"💳 Günlük ödeme / tahsilat / nakit akışı kontrolü",9,0,true,"12345","finance");
                add(c,"🧾 Belge / fatura / kayıt eksiğini kontrol ettim",15,0,false,"12345","finance");
                add(c,"📊 Gün sonu mutabakatını yaptım",17,30,true,"12345","finance"); break;
            case 9:
                add(c,"🛒 Kritik satın alma taleplerini önceliklendirdim",8,30,false,"12345","procurement");
                add(c,"☎ Bekleyen teklif / tedarikçi dönüşlerini kontrol ettim",14,30,false,"12345","procurement");
                add(c,"📦 Kritik teslimat ve termin riskini güncelledim",17,0,true,"12345","procurement"); break;
            case 10:
                add(c,"🤝 Bugünün müşteri temaslarını planladım",8,30,false,"12345","sales");
                add(c,"📞 Bekleyen teklif / takip aramalarını yaptım",15,0,false,"12345","sales");
                add(c,"📈 CRM / fırsat / sonraki adımı güncelledim",17,30,false,"12345","sales"); break;
            case 11:
                add(c,"👥 Bugünkü görüşme / aday / çalışan aksiyonlarını kontrol ettim",8,30,false,"12345","hr");
                add(c,"📄 Bekleyen evrak / onayları takip ettim",15,0,false,"12345","hr");
                add(c,"🗓 Yarınki görüşme ve duyuruları hazırladım",17,30,false,"12345","hr"); break;
            case 12:
                add(c,"⚖ Duruşma / süre / teslim tarihlerini kontrol ettim",8,0,true,"12345","law");
                add(c,"📁 Dosya ve delil / belge eksiğini kontrol ettim",14,30,false,"12345","law");
                add(c,"📝 Gün sonu süre ve takip notlarını güncelledim",17,30,true,"12345","law"); break;
            case 13:
                add(c,"💊 Stok / kritik ürün kontrolünü yaptım",8,30,true,"123456","pharmacy");
                add(c,"📋 Reçete / kayıt / teslim süreçlerini kontrol ettim",15,0,false,"123456","pharmacy");
                add(c,"📦 Eksik stok / sipariş listesini güncelledim",18,0,false,"123456","pharmacy"); break;
            case 14:
                add(c,"📐 Güncel revizyon / pafta listesini kontrol ettim",8,30,true,"12345","architect");
                add(c,"🏗 Saha / müşteri aksiyonlarını not ettim",15,0,false,"12345","architect");
                add(c,"🗂 Dosya / revizyon / teslim paketini güncelledim",17,30,false,"12345","architect"); break;
            case 15:
                add(c,"🦺 KKD ve saha başlangıç kontrolünü yaptım",7,30,true,"123456","site");
                add(c,"📏 Günlük imalat / ölçüm kontrolünü kaydettim",14,30,true,"123456","site");
                add(c,"⚠ Açık saha risklerini ve ertesi gün planını yazdım",17,30,false,"123456","site"); break;
            case 16:
                add(c,"🏭 Vardiya başlangıç makine / malzeme kontrolü",7,30,true,"1234567","production");
                add(c,"✅ Üretim / kalite kayıtlarını tamamladım",15,0,true,"1234567","production");
                add(c,"🤝 Vardiya devir notlarını hazırladım",18,0,false,"1234567","production"); break;
            case 17:
                add(c,"🧪 Günün test / kontrol planını doğruladım",8,0,true,"12345","quality");
                add(c,"📊 Ham veri / sonuç / uygunsuzluk kaydını tamamladım",15,30,true,"12345","quality");
                add(c,"⚠ Açık hata / risk / tekrar test listesini güncelledim",17,30,false,"12345","quality"); break;
            case 18:
                add(c,"📦 Kritik sevkiyat / stok listesini kontrol ettim",8,0,true,"123456","logistics");
                add(c,"🔎 Eksik / hasarlı / bekleyen malzemeyi kontrol ettim",15,0,false,"123456","logistics");
                add(c,"🚚 Yarınki sevkiyat planını hazırladım",17,30,false,"123456","logistics"); break;
            case 19:
                add(c,"🚗 Araç / yakıt / rota başlangıç kontrolü",7,30,true,"1234567","driver");
                add(c,"📦 Teslim / evrak / adres kontrolünü yaptım",13,30,false,"1234567","driver");
                add(c,"🔒 Araç ve teslimat gün sonu kontrolü",19,0,true,"1234567","driver"); break;
            case 20:
                add(c,"🛡 Görev / ekipman başlangıç kontrolünü yaptım",7,30,true,"1234567","security");
                add(c,"📝 Olay / devir / ziyaret kayıtlarını güncelledim",15,0,true,"1234567","security");
                add(c,"🤝 Vardiya devir teslimini yaptım",19,0,false,"1234567","security"); break;
            case 21:
                add(c,"🚒 Araç / ekipman / koruyucu donanım kontrolü",7,30,true,"1234567","emergency");
                add(c,"🧯 Kritik ekipman durumunu tekrar doğruladım",15,0,true,"1234567","emergency");
                add(c,"📝 Olay / bakım / devir kayıtlarını tamamladım",19,0,false,"1234567","emergency"); break;
            case 22:
                add(c,"✈ Uçuş / görev / doküman başlangıç kontrolü",7,0,true,"1234567","aviation");
                add(c,"🧭 Kritik limit / hava / görev değişikliğini kontrol ettim",12,0,true,"1234567","aviation");
                add(c,"📓 Uçuş / görev sonrası kayıtları tamamladım",18,0,false,"1234567","aviation"); break;
            case 23:
                add(c,"🔬 Bugünün araştırma hedefini belirledim",9,0,false,"12345","research");
                add(c,"💾 Veri / kaynak / notları yedekledim",16,0,true,"12345","research");
                add(c,"📝 Sonuç / sonraki deney / yazım notunu güncelledim",18,0,false,"12345","research"); break;
            case 24:
                add(c,"🌱 Günlük saha / sulama / bakım planını kontrol ettim",7,0,false,"1234567","farm");
                add(c,"🚜 Makine / yakıt / hayvan / ekipman kontrolü",12,0,true,"1234567","farm");
                add(c,"📋 Gün sonu ihtiyaç ve ertesi gün listesini yazdım",18,30,false,"1234567","farm"); break;
            case 25:
                add(c,"🏪 Açılış kasa / stok / sipariş kontrolü",8,0,true,"1234567","business");
                add(c,"☎ Bekleyen müşteri / tedarikçi işlerini kapattım",15,0,false,"1234567","business");
                add(c,"💰 Gün sonu kasa / ödeme / kapanış kontrolü",20,0,true,"1234567","business"); break;
            case 26:
                add(c,"🍳 Hazırlık / stok / soğuk zincir kontrolü",8,0,true,"1234567","food");
                add(c,"🧼 Hijyen / sıcaklık / temizlik kontrolü",15,0,true,"1234567","food");
                add(c,"📦 Ertesi gün stok / hazırlık listesini çıkardım",20,0,false,"1234567","food"); break;
            case 27:
                add(c,"🔧 Günün servis / bakım işlerini önceliklendirdim",8,0,false,"123456","service");
                add(c,"📋 Parça / ölçüm / yapılan işlem kaydını tamamladım",15,30,true,"123456","service");
                add(c,"✅ Açık iş / müşteri teslim listesini güncelledim",18,0,false,"123456","service"); break;
            case 28:
                add(c,"🎓 Bugünün ders / çalışma hedefini belirledim",8,30,false,"1234567","student");
                add(c,"📚 Ödev / proje / sınav tarihlerini kontrol ettim",16,0,true,"1234567","student");
                add(c,"🎒 Yarın için çanta / materyal hazırladım",21,0,false,"1234567","student"); break;
            default:
                add(c,"🏠 Günün 3 çalışma hedefini belirledim",8,30,false,"12345","freelance");
                add(c,"💾 Dosya / yedek / müşteri teslim kontrolü",16,0,true,"12345","freelance");
                add(c,"🧹 Çalışmayı kapattım ve yarın planını yazdım",18,0,false,"12345","freelance");
        }
    }

    private static void addRole(Context c, int i) {
        switch (i) {
            case 0:
                add(c,"👩‍👧 Çocukların gün / okul / çanta planını kontrol ettim",7,15,false,"1234567","mother");
                add(c,"🍱 Su / yemek / gerekli eşyaları hazırladım",7,30,false,"1234567","mother");
                add(c,"🌙 Yarın için çocukların önemli işlerini kontrol ettim",20,30,false,"1234567","mother"); break;
            case 1:
                add(c,"👨‍👧 Çocukların gün / okul / ihtiyaç planını kontrol ettim",7,20,false,"1234567","father");
                add(c,"🏠 Bugünkü aile sorumluluklarını gözden geçirdim",18,30,false,"1234567","father");
                add(c,"🌙 Yarın için ailece gerekli işleri kontrol ettim",21,0,false,"1234567","father"); break;
            case 2:
                add(c,"🏠 Evden çıkmadan kapı / ocak / gerekli eşyaları kontrol ettim",8,0,true,"1234567","home");
                add(c,"🧺 Bugünün ev işlerinden öncelikli olanı tamamladım",18,0,false,"1234567","home");
                add(c,"🔒 Gece kapı / ocak / pencere kontrolünü yaptım",22,0,true,"1234567","home"); break;
            case 3:
                add(c,"💳 Yaklaşan ödeme / fatura tarihlerini kontrol ettim",9,0,false,"1","budget");
                add(c,"🧾 Ödenen / bekleyen ev giderlerini güncelledim",20,0,false,"5","budget"); break;
            case 4:
                add(c,"❤️ Eşim / partnerimle bugünün önemli planını netleştirdim",8,0,false,"1234567","partner");
                add(c,"📅 Ortak plan / alışveriş / randevuları kontrol ettim",19,0,false,"1234567","partner"); break;
            case 5:
                add(c,"🤝 Bakım verdiğim kişinin bugünkü planını kontrol ettim",8,0,true,"1234567","caregiver");
                add(c,"📝 Yapılan bakım / ihtiyaç notlarını kaydettim",18,0,false,"1234567","caregiver"); break;
            case 6:
                add(c,"📖 Bugün öğrenmek istediğim tek konuyu seçtim",19,30,false,"1234567","learner");
                add(c,"✅ 20 dakikalık öğrenme / tekrar yaptım",21,0,false,"1234567","learner"); break;
            case 7:
                add(c,"🚗 Yakıt / lastik / gerekli araç eşyalarını kontrol ettim",8,0,false,"1","vehicle");
                add(c,"🔑 Araç anahtarı / ruhsat / gerekli eşyalar hazır",7,50,false,"1234567","vehicle"); break;
            case 8:
                add(c,"🐾 Mama / su / temel bakım kontrolünü yaptım",8,0,false,"1234567","pet");
                add(c,"🐕 Akşam bakım / yürüyüş kontrolünü yaptım",19,0,false,"1234567","pet"); break;
            default:
                add(c,"🌿 Bugün kendim için küçük bir mola planladım",13,0,false,"1234567","personal");
                add(c,"📞 Unutmak istemediğim sosyal görüşme / mesajı kontrol ettim",19,30,false,"1234567","personal");
        }
    }

    private static void addSupport(Context c, int support) {
        if (support == 1) {
            add(c,"🧠 Sabah bugünün önemli 3 işine baktım",8,0,false,"1234567","memory1");
            add(c,"🚪 Evden çıkış kontrolünü tamamladım",10,0,true,"1234567","memory1");
            add(c,"🌙 Gün sonu yaptıklarımı kısa kontrol ettim",21,30,false,"1234567","memory1");
        } else if (support >= 2) {
            add(c,"🧭 Bugünün tarih / gün / plan kartına baktım",8,0,false,"1234567","memory2");
            add(c,"🔑 Telefon • anahtar • ev güvenliği kontrolünü yaptım",10,0,true,"1234567","memory2");
            add(c,"🍽 Öğün / su rutinimi kontrol ettim",13,0,false,"1234567","memory2");
            add(c,"💊 Kendi ilaç planıma / reçeteme göre yapılacakları kontrol ettim",18,0,true,"1234567","memory2");
            add(c,"☎ Yakınımla günlük iletişim kontrolünü yaptım",19,0,false,"1234567","memory2");
        }
    }
}
