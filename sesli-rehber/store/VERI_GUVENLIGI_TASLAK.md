# Play Console Veri Güvenliği taslağı

Bu taslak `3.0.0-alpha01` kaynak koduna göre hazırlanmıştır; Play Console'a gönderilmeden önce imzalı yayın paketi ve kullanılan konuşma/harita sağlayıcısıyla yeniden doğrulanmalıdır.

## Geliştiricinin kendi sunucusuna aktarım

- Hesap oluşturma: Yok.
- Reklam: Yok.
- Analitik/çökme SDK'sı: Yok.
- Kamera görüntüsü: Geliştirici sunucusuna aktarılmaz.
- Rehber ve telefon numarası: Geliştirici sunucusuna aktarılmaz.
- Kesin konum: Geliştirici sunucusuna aktarılmaz; kullanıcı isterse Android paylaşım sayfasına harita bağlantısı hazırlanır.
- CSV hareket telemetrisi: Uygulamanın özel yerel alanında tutulur.

## Harici hizmetler

Android konuşma tanıma sağlayıcısı ses verisini çevrimiçi işleyebilir. Google Maps, Lens, WhatsApp, SMS ve telefon uygulamaları ayrı uygulama olarak açılır. Bunların beyanları, dağıtım yapılan cihaz ve hizmet sürümüne göre ayrıca değerlendirilmelidir.

## Güvenlik ve silme

- Açık metin ağ trafiği kapalıdır.
- Kullanıcı verileri uygulama kaldırılarak veya Android uygulama verileri silinerek kaldırılabilir.
- Play Store hesap/veri silme URL'si gerekmiyor; uygulamada hesap yoktur.
