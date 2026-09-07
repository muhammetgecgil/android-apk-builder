# Google Play mağaza metni

**Uygulama adı:** WiFi Bilimsel Radar

**Kısa açıklama:**
Wi‑Fi RF değişimi ve ESP32 CSI sensör füzyonu için yerel ölçüm laboratuvarı.

**Tam açıklama:**
WiFi Bilimsel Radar; kablosuz ortamı ölçmek, deneysel RF değişimlerini incelemek ve uyumlu ESP32 CSI düğümlerinden gelen telemetriyi tek ekranda birleştirmek için tasarlanmış mühendislik uygulamasıdır.

Sensörsüz laboratuvar modu Android'in erişime açtığı Wi‑Fi BSSID/RSSI ölçümlerini değerlendirir. ESP32 CSI modu ise 1–4 yerel düğümden alınan varlık kanıtı, hareket yoğunluğu, kalite, kararlılık ve baseline drift ölçümlerini kalite/tazelik ağırlıklı bir füzyon modeliyle birleştirir.

Öne çıkanlar:
- 1–4 ESP32 CSI düğümü
- RF kanıt ısı haritası
- HMM tabanlı zamansal aktivite sınıflandırması
- Sensör kalitesi ve düğümler arası uyum göstergesi
- BSSID/RSSI parmak izi analizi
- Telefon hareketi etkisini azaltmak için ivmeölçer/jiroskop kullanımı
- Boş oda RF referansı
- Öğrenilmiş telefon bölgesi tahmini
- Yerel işlem; hesap, reklam veya bulut telemetrisi yok

Bilimsel not: Android'in standart Wi‑Fi tarama API'si ham CSI sağlamaz. Sensörsüz mod insanın kesin konumunu veya iskeletini göstermez. CSI haritası da fiziksel kişi koordinatı değil, RF kanıt dağılımıdır.
