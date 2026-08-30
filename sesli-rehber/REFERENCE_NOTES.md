# Yüklenen referanslardan alınan teknik notlar

## Hareket Görüş V14 HD
- NativeActivity + Camera2 NDK.
- 1280×720 kamera, portrait işleme.
- 135×240 grid üzerinde kare farkı.
- Adaptif eşik yaklaşık `meanDiff + 11`, 14–42 aralığı.
- Büyük global değişimde kamera hareketi kabul edilip takip baskılanıyor.
- En fazla 12 hareket hedefi, kalıcı ID, hız/ivme ve rota izi.

## Yeni projeye taşınan prensipler
- Luma farkı yalnızca bir hareket ipucu olarak tutuldu.
- Kamera hareketini ayrı değerlendirme fikri IMU füzyonuna yükseltildi.
- Hareket tespiti engel sınıflandırması gibi sunulmuyor.
- Güven düşükken uyarı dili kesinlikten kaçınıyor.
