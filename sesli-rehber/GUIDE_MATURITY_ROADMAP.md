# Sesli Rehber — Gerçek Rehber Olgunluk Yol Haritası

## Ürün hedefi

Kullanıcı ekrana bakmadan "Hey Rehber, beni <hedef> adresine götür" dediğinde uygulama hedefi çözmeli, yaya rotasını başlatmalı, yürüyüş boyunca çevre/zemin/dinamik engel güvenliğini navigasyon konuşmasından daha yüksek öncelikte yönetmeli ve hedefin girişine kadar yardımcı olmalıdır.

Bu hedef, yalnız CI veya kamera demosuyla tamamlanmış sayılmaz. Her seviye kod + otomatik test + cihaz testi + kontrollü saha kabul kriterine sahiptir.

## M0 — Temel algı çekirdeği — DONE-CI
- CameraX + ARCore Depth16 çalışma yolları
- nesne tracking/geometri yaklaşma
- IMU kararlılık füzyonu
- zemin sürekliliği + Depth çoklu kanıt
- fail-safe STOP dili
- runtime watchdog + CameraX fallback
- Gate-C cihaz ölçüm CSV'si

## M1 — Yürüyüş güvenliği / v0.8 — DONE-CI, DEVICE-TEST bekliyor
- sol/orta/sağ göreli yürünebilir koridor
- yön adayı için çoklu kare kalıcılık
- karanlık, aşırı pozlama ve örtülü/düz kamera sağlık kanalı
- sabit sahnede yanlış watchdog STOP regresyonunun kaldırılması
- hiçbir koridor "güvenli yol" diye sınıflandırılmaz; yalnız "daha açık görünüyor" denir
- Safety > navigation > scene konuşma öncelik sözleşmesi

**Cihaz çıkış kapısı:** gerçek Depth verisinde yön hizası + kamera sağlık yanlış alarm testi + kontrollü koridor senaryoları.

## M2 — Eller serbest erişim / v0.9 — DONE-CI, DEVICE-TEST bekliyor
- API 31+ cihazda on-device `SpeechRecognizer` varsa foreground eller-serbest "Hey Rehber" dinleme döngüsü
- platform on-device tanıma yoksa çevrimiçi sürekli dinlemeye sessizce düşmez; tek-seferlik `Sesli Komut` fallback'i kalır
- uygulama arka plana geçince eller-serbest mikrofon oturumu kapanır; öne gelince kullanıcı modu açıksa devam eder
- mikrofon izni kamera rehberliğinden bağımsızdır
- Türkçe doğal niyet: başlat/durdur/tekrar/çevremi anlat/yazıyı oku/hedefe götür/yardım/Hey Rehber aç-kapat
- kısa sahne özeti yalnız taze algı kanıtlarından oluşturulur; semantik nesne veya güvenli yol uydurmaz
- CameraX ve ARCore CPU kamera yolunda isteğe bağlı bundled ML Kit OCR (`text-recognition:16.0.1`)
- hedef/adres komutundan destination çıkarımı; hedef M3 rota motoruna hazırlanır fakat rota yokken navigasyon başlamış gibi davranılmaz
- safety konuşması OCR/sahne/navigasyon konuşmasından yüksek öncelikte kalır

**Sınır:** Android platform `SpeechRecognizer` gerçek düşük-güç hotword DSP değildir ve Android dokümanı sürekli tanıma için tasarlanmadığını belirtir. Bu nedenle M2 kodu "düşük güç wake-word tamam" iddiası yapmaz; gerçek cihaz pil/ısınma, Türkçe yerel model ve wake doğruluk testi gerekir.

## M3 — Gerçek yaya navigasyonu / sıradaki seviye
- adres/hedef çözümü
- yaya rota motoru
- kavşak ve dönüş olayı modeli
- rota konuşması safety arbitrajından geçmeden seslendirilemez
- GPS zayıflığında IMU/visual odometry destekli rota sürekliliği
- rota sapması ve yeniden yönlendirme
- "karşıya geçmek güvenli" gibi kanıtlanamaz onay dili yasak

## M4 — Son metre ve günlük yaşam
- bina girişi/kapı/kapı numarası adayları
- yaya geçidi, kaldırım, merdiven, aşağı basamak ve çukur için doğrulanmış semantik + Depth + temporal fusion
- otobüs/durak/hat numarası OCR akışı
- para, ürün etiketi, kısa belge ve menü okuma
- telefon/SMS ve izin verilen mesajlaşma eylemleri

## M5 — Acil durum ve dayanıklılık
- yardım komutu, düşme adayı, iptal penceresi
- kullanıcı tarafından atanmış acil kişiye arama/konum paylaşma akışı
- düşük pil/termal durum degradasyon politikası
- kamera/Depth/IMU arızasında açık fail-safe davranış
- uzun süreli çalışma, yeniden başlama ve proses geri kazanım testleri

## M6 — Saha doğrulanmış ürün adayı
Kontrollü ve kayıtlı senaryolar:
- duvar, kapı, masa/sandalye, ince direk, düşük/yüksek engel
- yaklaşan/yan geçen insan
- park halindeki ve kontrollü yaklaşan araç
- yukarı/aşağı merdiven, kaldırım, seviye değişimi, kontrollü çukur maketi
- düşük ışık, karşı ışık, yağmur/ıslak zemin, lens kısmi örtme
- telefon sallanması ve yön değişimi
- 30+ dakika thermal/latency/Depth coverage
- rota + engel aynı anda geldiğinde safety konuşma önceliği

**Ürün iddiası kapısı:** yanlış alarm, kaçırma, P95 algı-karar gecikmesi, fallback başarısı ve erişilebilirlik kabul kriterleri kontrollü saha testinde ölçülmeden uygulama "bağımsız güvenli navigasyon" olarak tanımlanmaz.

## Değişmez güvenlik sözleşmeleri
1. Engel/zemin STOP her zaman navigasyon ve sahne anlatımından önceliklidir.
2. Düşük güven = daha iddialı dil değil, daha muhafazakâr dil/fallback.
3. Tek kamera/tek Depth ipucu "çukur", "güvenli geçiş" veya "karşıya geç" kararı veremez.
4. Kalibrasyonsuz görüntü geometrisi kullanıcıya metre cinsinden kesin mesafe olarak sunulmaz.
5. Cihazda yaşam-kritik kararlar internet/uzak AI servisine bağımlı olmaz.
6. Baston/kılavuz köpek ve yönelim-hareketlilik becerilerinin yerine geçtiği iddia edilmez.
