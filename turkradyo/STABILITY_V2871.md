# MGtürk Radyo 2.8.7.1 — 19 Eylül 2026

Temel: `dev/turkradyo-v2.8.7-ui`, `3a1880e199e0652c9b6771938be53f3b9835a825`.
Radyo dalları commit tarihine göre karşılaştırıldığında en güncel kaynak:
13 Eylül 2026 18:49:22 (Türkiye saati). `turkradyo-latest` adı taşıyan dal
6 Eylül tarihli daha eski kodu gösteriyordu. Yeni sürümün versionCode değeri
290'dır; daha önce üretilmiş farklı 288/289 paketleriyle numara çakışmaz.

## Düzeltilen sorunlar

- Slow Mod aynı metni sürekli yazıyor, kendi MutationObserver'ını yeniden
  tetikliyordu. Kalite rozetleri de liste açıkken aynı döngüyü oluşturuyordu.
  Değişmeyen içerik artık yeniden yazılmaz. Premium Slow Mod ikonuna eski
  süsleyicinin müdahalesi de önlenir.
- Oynatıcı oluşturma, kapatma, efekt ve yeniden bağlantı işlemleri WebView'in
  ana iş parçacığında çalışıyordu. Artık bütün ExoPlayer erişimleri tek bir
  özel HandlerThread üzerinde; ağ ve MediaSession geri çağrıları da aynı
  kuyruğa yönlendirilir. Hızlı, art arda gelen PLAY isteklerinde son seçim
  bekleyen eski seçimlerin önüne geçer.
- Eski istasyonun asenkron onarımı yeni istasyona geçtikten sonra sonuç
  uygulayabiliyordu. İstek nesli kontrolü, Future iptali ve pause/stop
  geçersizleştirmesi eklendi. JavaScript, nativeRecovery işaretli servisin
  onarımına paralel otomatik oynatma başlatmaz.
- Duraklatma ve durdurma sonrasında eski telemetri Android Auto'da oynuyor
  görünümünü koruyabiliyordu. Durum hemen kaydedilir ve manuel duraklatma
  eski oynuyor bitinden önceliklidir.
- Ağ geri geldiğinde oynatıcının henüz oluşturulamamış olması kurtarmayı
  engelleyebiliyordu. Boş oynatıcı da yeniden bağlantı yoluna alınır.
- Arka planda katalog ve onarım talepleri kontrolsüz iş parçacığı üretiyordu.
  Katalog talepleri birleştirilir, taramalar seri çalışır ve kaynak onarımı
  sınırlı iş parçacığı havuzundan yürütülür.
- WebView yok olduğunda eski Activity'nin tekrar ekran oluşturması ve bağlı
  görünümün doğrudan destroy edilmesi düzeltildi. Otomatik ekran kurtarma
  60 saniyede üç denemeyle sınırlıdır; ardından yeniden açma düğmesi sunulur.
- `profile2-design-v13.js` ve `profile1-radio-engine-v5.js` içindeki eksik
  kapanışlar düzeltildi. Dinamik betikler eklenme sırasıyla yürütülür.

## İşlev iyileştirmeleri

- Başlangıç kataloğunda yanıt gövdesini de kapsayan 8 saniyelik sunucu zaman
  aşımı ve başarısız isteklerde kayıtlı listeye dönüş.
- Bozuk favori/geçmiş verisi uygulamanın tüm başlangıcını durdurmaz.
- EQ bantları ve ses kazancı servis tekrar açıldığında korunur.
- Manuel onarım ve kalite seçimi beklenirken başka radyoya geçilirse eski
  sonuç yeni seçime uygulanmaz. Onarım araması aynı istasyonda kalır.
- Derleme dosya adları gerçek 2.8.7.1 sürümünü taşır; emekli Android SDK
  `tools` paketi artık talep edilmez.

## Doğrulama ve kapsam

Arayüz: `node --test turkradyo/tests/ui-regression.cjs` (Playwright/Chromium).
Android: `gradle testDebugUnitTest lintRelease assembleRelease assembleDebug bundleRelease`.
Başarılı koşumun kesin commit'i ve sonuçları ilgili GitHub Actions kaydından
izlenebilir. Ağ/istasyon cevapları arayüz testlerinde kontrollü örneklerle
verilir; bunlar canlı radyo sunucusu veya telefon donanımı testi değildir.

Cihazda son kabul: hızlı kanal değiştirme; Wi-Fi/mobil veri geçişi; internet
kapatıp açma; kesinti sırasında pause/stop; ekran kilitli uzun dinleme;
kulaklık çıkarma/telefon görüşmesi; Android Auto oynat-duraklat; profil,
favori, arama, alarm ve uyku zamanlayıcısı.

İmzalama: mevcut Gradle release anahtarı yapılandırması korunur. Anahtar
sağlanmadığında release APK/AAB imzasız, debug APK test imzalıdır. Üzerine
güncelleme kurulabilmesi için telefondaki paketle aynı imza gerekir; mevcut
uygulamayı kaldırmak veri kaybına neden olabileceğinden güncelleme yöntemi
olarak önerilmez. Referans dalları ve ana dal değiştirilmedi.
