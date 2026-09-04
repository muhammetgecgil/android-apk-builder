# Quake Watch v1.0

Android uygulaması, USGS FDSN dünya deprem kataloğunun son 24 saatini indirir ve 2°x2° hücrelerde kısa dönem sismisite anomalilerini sıralar.

Model bileşenleri:
- 1 saat / 6 saat / 24 saat aktivite oranı
- Gutenberg–Richter b-değeri için yaklaşık MLE sinyali
- ETAS-benzeri zamanla sönümlenen tetiklenme/aftershock üretkenlik skoru
- büyüklük ve kümelenme katkıları
- 0–100 göreli aktivite puanı

Uygulama kesin deprem tahmini veya resmi erken uyarı sistemi değildir. Konum-zaman-büyüklük garantisi vermez; katalog anomalilerini izler. USGS kataloğunun kapsamadığı mikro-depremler doğal olarak uygulamaya ulaşmaz.

Sürekli İzle modu foreground service ile yaklaşık 15 dakikada bir yeniden analiz yapar ve yüksek skor oluşursa bildirim üretir.
