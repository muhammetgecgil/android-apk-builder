# MGtürk Radyo 2.8.7.4

## Gezinme

- Üstteki önceki/sonraki tuşları kullanıcının ana listesini izler. Grup içinden
  ana listede olmayan bir istasyon dinleniyorsa son ana liste konumundan devam eder.
- Shazam yanındaki alt tuşlar o anki istasyonun türünü izler. Türkiye Grupları
  ekranı ile aynı sınıflandırmayı ve kataloğu kullanır; haberden habere, poptan
  popa ilerler ve listenin sonunda başa döner.
- Tür adı alt tuşlarda görünür. Bilinmeyen tür veya tek uygun istasyonda başka
  bir türe atlanmaz; mevcut yayın korunarak bilgi verilir.
- Ülke, URL, istasyon kimliği, tekrar eden akışlar ve bilinen bozuk kaynaklar
  denetlenir. Kayıtlı katalog çevrimdışı kullanılabilir.
- Geciken katalog yanıtı, sonradan seçilen istasyonu veya oynatma/duraklatma
  kararını geçersiz kılamaz. Peş peşe komutların yalnız son geçerli isteği uygulanır.
- Eski Tür Modu olay dinleyicileri aynı tıklamayı ikinci kez işleyemez.
- Ana liste anahtarları ve favoriler grup gezinmesi nedeniyle değiştirilmez.

Türler istasyon adları/etiketleriyle belirlenir. Yanlış yayıncı etiketleri yanlış
sınıflandırmaya yol açabilir; sunucu erişilebilirliği garantisi verilmez.

## Görsel düzen

Mevcut ana kadran, kart sırası, iki profil ve tema renkleri korunur.
Kartlarda çift kenar, metalik halka, küçük kadran çizgileri ve koyu cam yüzeyi;
ana düğmelerde ölçülü ışık ve metal kenar; alt tuşlarda belirgin tür etiketi kullanılır.
DNA sarmalı ve perspektifli kum saati yeniden çizildi. Beyaz metal yansımalar
tema renginin önüne geçmeden tüm özel simgelerde ortaklaşır.
Yeni sürekli animasyon, DOM gözlemcisi veya görsel ağ isteği eklenmez.

## Dikey ekran

MainActivity manifestte `portrait` olarak sabitlendi. Android 16 / hedef API 36
geniş ekran uyumluluk özelliği yalnız bu Activity için tanımlandı. Paketlenen
APK'nın manifesti de doğrulama adımında kontrol edilir.

Platform kaynağı: https://developer.android.com/about/versions/16/behavior-changes-16#adaptive-layouts
API 37 hedeflemesine geçildiğinde geniş ekranlarda bu istisna yeniden
değerlendirilmelidir; fiziksel cihaz dönüş testi burada yapılmadı.

## Doğrulama ve kurulum

Yeni tarayıcı senaryoları haber/pop dolaşımı, Türkiye Grupları eşleşmesi,
üst/alt düğme ayrımı, çevrimdışı katalog, iki yönlü sarma, kopya/bozuk/yabancı
akışların elenmesi, eski Tür Modu çakışması, geciken yanıt ve tek/belirsiz grubu kapsar.
Mevcut oynatma ve Android testleri de yeniden çalıştırılır.

Sürüm 2.8.7.4, versionCode 293; test kimliği
`com.muhammetgecgil.turkradyo.test.v293`. Orijinal release imzası ve önceki
CI debug anahtarı mevcut olmadığından ayrı kurulur. Önceki uygulamaları
kaldırmak gerekmez; favoriler otomatik aktarılmaz. Üretim paket kimliği korunur.
