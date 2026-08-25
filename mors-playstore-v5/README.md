# Mors Kod Çevirici 5.0

Play Store odaklı, tamamen çevrimdışı Android Mors uygulaması.

## Teknik
- `compileSdk 36` / `targetSdk 36`
- `minSdk 26`
- Java 17
- AGP 8.13.0 / Gradle 8.13
- Harici runtime kütüphanesi yok
- İnternet izni yok
- S24 Ultra / Android 16 için native `VibratorManager` desteği
- Kamera izni yalnızca el feneri ile Mors gönderimi için, kullanıcı özelliğe bastığında istenir

## Özellikler
- Metin → Mors / Mors → Metin
- Türkçe genişletilmiş karakter desteği
- WPM tabanlı Mors zaman oranları
- Ayarlanabilir ses frekansı ve titreşim şiddeti
- Native AudioTrack sinüs üretimi
- Native titreşim waveform
- Kamera torch ile Mors
- Kopyala / paylaş
- Cihaz içi geçmiş
- Sistem bar inset uyumu
- Android 12+ splash screen
- Erişilebilir minimum dokunma hedefleri

## Build
```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease
```

Debug APK telefona kurulabilir. Play Store yayını için release AAB bir upload key ile imzalanmalıdır; imza anahtarını public repository'ye koymayın.
