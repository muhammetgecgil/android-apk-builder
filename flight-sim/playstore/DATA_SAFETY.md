# Google Play Data Safety — Draft Answers

Bu dosya Play Console Data Safety formu için çalışma belgesidir. Yayın öncesinde uygulamanın gerçek kodu, manifest izinleri ve tüm üçüncü taraf SDK'lar tekrar denetlenmelidir.

## Mevcut hedef beyan
- Kullanıcı hesabı: Yok
- Reklam SDK'sı: Yok
- Kişisel veri satışı: Yok
- Kişisel veri paylaşımı: Hedef tasarımda yok
- Analitik SDK: Hedef tasarımda yok
- Bulut kayıt: Yok
- Hassas konum: Yok
- Kişiler: Yok
- Fotoğraf/video erişimi: Yok
- Sağlık/finans verisi: Yok
- Mesaj/SMS/arama kaydı: Yok

## Bluetooth / yakın cihazlar
İki cihazlı pilot + aircraft-display modu üretim sürümünde bulunuyorsa Android yakın cihaz/Bluetooth izinleri teknik bağlantı amacıyla kullanılabilir. Bu izinlerin kişisel veri aktarımı yapıp yapmadığı gerçek uygulama davranışına göre beyan edilmelidir.

## Play Console önerilen cevap akışı
1. Uygulama gerekli kullanıcı veri türlerinden herhangi birini topluyor veya paylaşıyor mu? Kod ve SDK denetimi sonucuna göre cevapla. Mevcut hedef: Hayır.
2. Tüm ağ trafiği aktarım sırasında şifreleniyor mu? İnternet üzerinden kullanıcı verisi gönderilmiyorsa bağlama göre formu doldur; ileride ağ servisi eklenirse HTTPS/TLS zorunlu olsun.
3. Kullanıcı hesap oluşturabiliyor mu? Mevcut hedef: Hayır.
4. Veri silme mekanizması: Hesap ve sunucu taraflı kullanıcı verisi yoksa buna uygun beyan seçilmeli.

## Yayın öncesi kanıt kontrolü
- AndroidManifest.xml izinleri
- Gradle bağımlılıkları
- Filament ve diğer kütüphanelerin veri davranışı
- Ağ çağrıları
- Bluetooth veri paketleri
- Log/telemetri
- Crash reporting
- Reklam/analitik SDK'ları

Data Safety formu, gizlilik politikası ve gerçek uygulama davranışı aynı şeyi söylemelidir.
