# Sesli Rehber — kör kullanıcılar için erişilebilir çevre yardımcısı

Bu klasör, yüklenen Hareket Görüş V14 HD ve ürünleştirme paketindeki çalışan kamera/hareket yaklaşımını referans alarak sıfırdan kurulmuş yeni Android temelidir.

## v0.1'de çalışan temel
- CameraX arka kamera akışı.
- Düşük çözünürlüklü luma-frame farkı ile hareket bölgesi algısı.
- İvmeölçer + jiroskop ile cihaz kararlılığı ve basit sensör füzyonu.
- Sol / orta / sağ hareket konumu.
- Türkçe TTS ve titreşim.
- Kullanıcı tetiklemeli, çevrimdışı tercihli sesli komut ve yerel Türkçe niyet ayrıştırıcı.
- TalkBack odaklı büyük kontroller ve erişilebilirlik duyuruları.
- Güvenlik kapısı: düşük kararlılıkta “Dur ve bastonla doğrula.”
- API 36 ve GitHub Actions ile APK derleme.

## Henüz iddia edilmeyen özellikler
Hareket algısı statik duvar, kaldırım, çukur, direk veya merdiveni güvenilir biçimde tanıyamaz. Bunlar için doğrulanmış on-device nesne algılama + yürünebilir alan + derinlik modeli gerekir. `SceneUnderstandingEngine` bunun bağlantı noktasıdır.

Sürekli “Hey Rehber” v0.1'de etkin değildir; şimdilik kullanıcı Sesli Komut düğmesiyle tek seferlik konuşma başlatır.

## Güvenlik
Bu yazılım beyaz baston, rehber köpek veya profesyonel yönelim-hareketlilik eğitiminin yerine geçmez. Görsel güven düşükse sistem kesin engel adı üretmez.

## Derleme
Repo kökündeki `.github/workflows/build-sesli-rehber.yml` Android SDK 36 + Gradle 8.13 ile test ve debug APK üretir.

## Sıradaki katmanlar
1. S24 Ultra kamera kalibrasyonu ve fiziksel lens seçimi.
2. On-device nesne algılama.
3. Yürünebilir alan segmentasyonu ve kaldırım/çukur derinliği.
4. Kamera + IMU + GNSS + manyetometre füzyonu ve yaya rotası.
5. Sürekli çevrimdışı “Hey Rehber”.
6. Kör kullanıcı saha testleri ve yanlış alarm/kaçırma ölçümü.
