# Kapalı test ve yayın kapısı

## Zorunlu cihazlar

- Samsung Galaxy S24 Ultra / Android 16
- En az bir Android 13–15 telefon
- TalkBack açık ve kapalı senaryolar

## Yayın kapıları

1. GitHub Actions birim testi, lint, debug APK ve release AAB üretimi başarılı olmalı.
2. Temiz kurulum, tüm izin reddi ve sonradan izin verme akışlarında çökme/ANR olmamalı.
3. 30 dakika kamera kullanımı boyunca çökme, donma ve kontrolsüz ısınma olmamalı.
4. Arama, SMS, WhatsApp ve acil işlemlerde alıcı/işlem kullanıcıya okunmadan eylem başlamamalı.
5. TalkBack odak sırası, düğme açıklamaları ve büyük yazı ayarı doğrulanmalı.
6. Yanlış düşme alarmı ve kaçırma oranı kontrollü senaryolarla ölçülmeli.
7. Araç/çukur/kaldırım özellikleri gerçek model ve saha doğrulaması olmadan mağaza iddiasına eklenmemeli.

## Kontrollü yürüyüş testi

- Trafiğe kapalı alanda, refakatçi ve beyaz bastonla yapılır.
- Gün, gece, yağmur, ters ışık ve kalabalık ayrı kayıt edilir.
- Kameranın yalnızca hareket algıladığı; nesne türü veya mesafe garantisi vermediği test kullanıcısına başta sesli anlatılır.
- Her yanlış pozitif, kaçırma, gecikme ve kamera donması zaman damgasıyla kaydedilir.
