# MGtürk Radyo 2.8.7.2 — görsel ayrıntılar ve işlev denetimi

19 Eylül 2026. Önceki 2.8.7.1 düzeltmelerinin üzerine uygulanır.
Kadran, mevcut profiller, tema paletleri ve ana kart sıralaması korunur.

## Görsel ayrıntılar

- Oynatma, kanal değiştirme, ses ve alt menü simgeleri aynı çizgi stilini kullanır.
- Kart etiketlerinin satır aralığı ve ağırlığı; panel, liste, arama alanı,
  düğme ve odak çerçeveleri tutarlı hale getirildi.
- Ana tema rengi korunarak daha hafif kenarlar, iç ışık ve gölgeler uygulandı.
- Yayın durumu gerçek oynatıcı verisinden gösterilir. Duraklatılan yayın için
  sahte “canlı” ve “100/100 stabil” etiketleri gösterilmez.
- Küçük ekranlar, azaltılmış hareket tercihi ve arka planda animasyonların
  durdurulması desteklenir.

## Bulunup düzeltilen işlevler

| İşlev | Düzeltme |
| --- | --- |
| Zamanlayıcıda kademeli ses azaltma | Son 60 saniyede servis üzerinden gerçek ses azaltma; Activity kapalıyken de süre kontrolü |
| Zamanlayıcı iptali / sayaç | Native kayıtla eşleşen sayaç; iptalde asıl ses seviyesine dönüş |
| Süresi dolan uyku alarmı | Kapalı oynatıcı servisini gereksiz yere yeniden başlatmaz |
| EQ / normalizasyon / yumuşak geçiş | Kaydedilmiş gerçek ayarlar köprü üzerinden arayüze aktarılır |
| Peş peşe medya komutları | İframe yerine doğrudan, ana iş parçacığına yönlendirilmiş Android köprüsü; EQ'nun beş bandı ayrı ayrı iletilir |
| Tema sıfırlama | Tema değişkenleri tamamen kaldırılır |
| Geri hareketi | Tema/profil pencereleri önce kapatılır; radyo ekranından yanlışlıkla çıkılmaz |
| Durum ve istasyon eşleştirme | Hazır, bağlanıyor, duraklatıldı ve canlı ayrımı; native istasyon seçimi ekrana yansır |
| NOTA AI | Çıkış yakalama uygulamanın kendi ses oturumuna bağlandı; yaklaşık sonuç ve mikrofon yedeği açıklaması netleştirildi |

## Doğrulamanın sınırları

Chromium testleri kontrollü radyo verileri ve Android köprüsü örnekleri kullanır.
Robolectric testleri Android durum geçişlerini denetler. Bunlar fiziksel telefon,
Bluetooth, Android Auto, mikrofon, gerçek yayın sunucusu ve saatler süren kilitli
ekran dinlemesinin yerine geçmez.

- NOTA AI deneysel baskın perde analizi yapar. Çok sesli müzikte doğruluk ve
  bazı cihazlarda ses yakalama ayrıca değerlendirilmelidir.
- Son 50 şarkı, istasyonun yayınladığı metadata bilgisinin bulunmasına bağlıdır.
- Shazam için uygulama veya internet erişimi; alarm için Android alarm izni ve
  cihazın pil yönetimi koşulları gerekir. Telefon yeniden başlatıldığında
  alarmın tekrar kurulması gerekir.
- Canlı istasyonların tamamının sürekli erişilebilir olduğu iddia edilmez.

## Kurulum

Sürüm: `2.8.7.2`, versionCode: `291`.
Release kimliği değişmez. Orijinal release anahtarı mevcut olmadığı için
teslim edilen test APK'sı `com.muhammetgecgil.turkradyo.test.v291` kimliğiyle,
“MGtürk Radyo 2.8.7.2” adı altında ayrı kurulur. Önceki test paketinin imzası
yeniden üretilemediğinden ona güncelleme olarak verilmez. Eski uygulamaları
kaldırmak gerekmez; tercih ve favoriler otomatik taşınmaz.

CI, gerçek APK imzasını, uygulama kimliğini, sürümü, etiketi ve açılış Activity'sini
kontrol eder. Derleme sonuçları ve ekran görüntüleri PR #54 / Actions içindedir.
