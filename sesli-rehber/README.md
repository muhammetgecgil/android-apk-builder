# Sesli Rehber 3 — GitHub Alpha

Görme engelli kullanıcılar için Türkçe sesli telefon yardımcısı ile Hareket Görüş V14 kamera çekirdeğini aynı Android projesinde birleştiren açık geliştirme paketi.

## Bu sürümde çalışanlar

- Büyük, yüksek kontrastlı mikrofon arayüzü ve Türkçe konuşma geri bildirimi
- Uygulama açıkken “Hey Rehber” dinleme modu
- Bağlamsal niyet anlama, eksik kişi/hedef/mesaj/saat bilgisini sorma
- Arama, SMS ve WhatsApp paylaşımından önce sesli onay
- Saat, tarih, pil, ses, fener, uygulama açma ve ekran okuma yardımcısı
- Yaya rota isteğini Google Maps'e `mode=w` ile aktarma
- Konum görüntüleme/paylaşma, alarm hazırlama ve çevrimdışı dolandırıcılık kelime analizi
- İvmeölçerden deneysel düşme olasılığı uyarısı
- Hareket Görüş V14: cihaz üzerinde kamera akışı, hareketli bölge takibi, rota izi ve görüntü düzlemi hız/ivme bilgisi

## Henüz tamamlanmayan güvenlik özellikleri

Bu alfa sürüm; araç, duvar, kaldırım, çukur, basamak veya güvenli geçiş yönünü doğrulanmış bir yapay zekâ modeliyle sınıflandırmaz. Kamera ve telefon sensörleri tek güvenlik kararında henüz birleşmez. Harita yönlendirmesi ile kamera uyarısı eşzamanlı değildir. Ayrıntılar [ürün doğruluk matrisi](docs/CAPABILITY_MATRIX.md) içindedir.

Uygulama beyaz bastonun, rehber köpeğin veya yönelim-hareketlilik eğitiminin yerine geçmez. Kontrollü saha doğrulaması olmadan trafikte bağımsız güvenlik sistemi olarak kullanılmamalıdır.

## Derleme

Gereksinimler: JDK 17, Android SDK 36, Gradle 8.11.1. Depodaki GitHub Actions akışı native ARM64 kütüphaneyi yeniden üretir, testleri çalıştırır ve iki çıktı verir:

- `SesliRehber-3.0.0-alpha01-debug.apk`: S24 Ultra'da deneme kurulumu
- `SesliRehber-3.0.0-alpha01-release-unsigned.aab`: Play Store yükleme anahtarıyla ayrıca imzalanması gereken paket

Yerel derleme:

```bash
gradle --no-daemon -p sesli-rehber :app:testDebugUnitTest :app:assembleDebug :app:bundleRelease
```

## Paket kimliği ve güncelleme

GitHub alfa paket kimliği `com.muhammet.seslirehber.github`'dır. Eski v2.2 farklı bir anahtarla imzalandığı için bu sürüm onun yanında kurulabilir. Play Store yayını öncesinde kalıcı uygulama kimliği ve gizli yükleme anahtarı seçilmelidir.

## Klasörler

- `app/`: Derlenebilir Android uygulaması ve niyet motoru
- `native-src/`: Hareket Görüş V14 C kaynak kodu
- `store/`: Play Store açıklaması, gizlilik ve veri güvenliği taslakları
- `docs/`: yetenek matrisi, kaynak geri kazanım notu ve test planı
- `releases/`: önceki imzalı APK ve kaynak paketleri
