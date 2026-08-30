# Sesli Rehber — 5 Milestone Ürün Olgunluk Planı

## Nihai ürün hedefi
Kullanıcı ekrana bakmadan "Hey Rehber, beni <hedef> adresine götür" dediğinde uygulama hedefi çözmeli, yaya rotasını başlatmalı, çevreyi sürekli algılamalı, rota boyunca yakın/orta/uzak tehditleri ve yürünebilir alanları zamansal bir dünya modelinde birleştirmeli, güvenlik konuşmasını navigasyondan öncelikli tutmalı ve hedef girişine kadar yardımcı olmalıdır.

Bu ürün hedefi yalnız CI veya kamera demosuyla tamamlanmış sayılmaz. Her milestone şu beş kapıdan geçer:
1. KOD: milestone kapsamı branch üzerinde tamamlanır.
2. OTOMATİK TEST: unit + regresyon + safety-language testleri geçer.
3. APK: Android debug APK gerçek CI üzerinden başarıyla üretilir ve bütünlüğü doğrulanır.
4. CİHAZ TESTİ: Galaxy S24 Ultra üzerinde kontrollü senaryolar ve Gate-C/Gate-D kayıtları alınır.
5. KABUL: çıkış kriterleri geçerse milestone DONE olur; geçmezse aynı milestone içinde düzeltme yapılır.

---

## MILESTONE 1 — SAHNEYİ ANLAMA / SEMANTIC WORLD MODEL
**Hedef sürümler:** v0.15.x
**Durum:** ACTIVE — v0.14 world-model ve distant-object temeli hazır, semantic segmentation sıradaki ana iş.

### Kapsam
- Piksel-seviyesinde semantic segmentation / scene partitioning.
- İlk sınıflar: yürünebilir zemin, yol yüzeyi, kaldırım adayı, bina/duvar, insan, araç, bisiklet/motosiklet, direk/bariyer, giriş/kapı, bitki, bilinmeyen engel.
- Segmentasyon sonucu mevcut 3×3 world model'e bağlanır: sol/orta/sağ × yakın/orta/uzak.
- Mevcut çoklu nesne tracking, distant-object recognition, motion, ARCore Depth16, ground, level-change ve walkable-corridor aynı snapshot'ta birleşir.
- Tek kare segment sonucu doğrudan güvenlik kararı vermez; temporal smoothing/persistence gerekir.
- Segmentasyon ve world model SafetyGate STOP otoritesinin yerine geçmez.
- Kullanıcı komutları: "durum ne", "çevremi anlat", "çevrede ne var" güncel world-model özetini verir.

### Otomatik kabul
- Aynı karede en az sol/orta/sağ çoklu sahne kanıtı korunur.
- Stale segment/track TTL sonunda silinir.
- Tek yanlış segment label'ı STOP üretemez.
- Segmentasyon sonucu "yol güvenli" veya "kesin kaldırım/çukur/merdiven" dili üretmez.
- CameraX ve ARCore fallback yollarında aynı semantic fusion davranışı korunur.
- Eski collision/depth/ground/navigation/OCR/voice regresyonları geçer.

### S24 Ultra test seti
- açık kaldırım, bina duvarı, dar geçit, direk, insan, park etmiş araç, hareketli insan, araç, kapı/giriş, yol-kaldırım sınırı.
- gündüz, gölge, karşı ışık, düşük ışık.
- hedef: segment kararlılığı, yanlış sınıf oranı, world-model stale davranışı, FPS, pil/thermal kayıtları.

### Çıkış kriteri
Semantic segmentation + 3×3 temporal world model gerçek cihazda anlamlı ve kararlı çevre özeti üretmeden M2'ye geçilmez.

---

## MILESTONE 2 — HAREKET TAHMİNİ / PATH-CONFLICT / KAVŞAK FARKINDALIĞI
**Hedef sürümler:** v0.16.x
**Durum:** PENDING

### Kapsam
- Çok-kareli trajectory history.
- İnsan/araç/bisiklet için yaklaşma, uzaklaşma, yanal geçiş ve tahmini yol yönü.
- Kullanıcının tahmini yürüme koridoruyla nesne trajectory'sinin çakışma adayı.
- 1–3 saniyelik kısa ufuklu path-conflict skoru; metrik çarpışma süresi yalnız kalibrasyon sonrası.
- Rota bağlamlı attention: dönüş yönü + world model + dynamic object birlikte değerlendirilir.
- Yaya geçidi adayı: segment çizgileri + yol/kaldırım sınırı + OCR/visual pattern.
- Trafik ışığı adayı ve durum sınıflandırması ancak çok-kare/uygun güvenle advisory olarak konuşulur.
- Karşıya geçişte uygulama hiçbir zaman "geçmek güvenli" onayı vermez.

### Otomatik kabul
- tek kare hareket trajectory sayılmaz.
- crossing object ile paralel uzaklaşan object ayrılır.
- stale trajectory yeni path-conflict üretmez.
- route speech safety hold'u aşamaz.
- trafik/yaya geçidi aday dili muhafazakâr kalır.

### S24 Ultra test seti
- sağdan sola geçen yaya, soldan sağa geçen bisiklet, yaklaşan/uzaklaşan araç, kullanıcının önünden kesişen kişi, park araçları, kontrollü kavşak/yaya geçidi.
- hedef: yanlış path-conflict, kaçırma, karar gecikmesi, yoğun sahnede track kimliği kararlılığı.

### Çıkış kriteri
Dinamik nesne hareket yönü ve yürüyüş hattı çakışması kontrollü saha senaryolarında güvenilir şekilde ayrışmadan M3'e geçilmez.

---

## MILESTONE 3 — ZEMİN / GİRİŞ / TOPLU TAŞIMA SEMANTİĞİ VE CİHAZ KALİBRASYONU
**Hedef sürümler:** v0.17.x
**Durum:** PENDING

### Kapsam
- ARCore Raw Depth / confidence image kullanılabiliyorsa kalite katmanı.
- IMU/gravity ile phone pitch/orientation güven aralığı.
- Ground plane + segmentation + Depth + temporal fusion.
- Yukarı basamak, aşağı basamak, kaldırım yükselmesi, çoklu basamak/merdiven ve çukur adaylarının ayrı geometri hipotezleri.
- Kesin semantik yalnız kontrollü saha verisi ve çoklu kanal doğrulaması sonrası açılır.
- Son 50 m giriş modu geliştirilir: kapı, giriş, blok, kapı numarası, bina yüzeyi.
- Otobüs/durak/hat kodu OCR + araç semantiği + rota bağlamı birlikte değerlendirilir.
- Para, ürün etiketi, kısa belge ve menü okuma günlük yaşam kanalına hazırlanır.

### Otomatik kabul
- phone pitch değişimi tek başına çukur/basamak oluşturmaz.
- depth confidence düşükse iddia zayıflar/fallback olur.
- tek OCR kapı numarası doğru giriş sayılmaz.
- transit-line adayı tek başına "doğru otobüs" diye konuşulmaz.
- semantic ground sınıfı SafetyGate çoklu kanıt politikasıyla uyumlu kalır.

### S24 Ultra Gate-D test seti
- gerçek kaldırım yukarı/aşağı geçişleri, tek basamak, çoklu merdiven, kontrollü çukur maketi, rampa, ıslak/parlak zemin, gölge, giriş kapıları ve farklı kapı numaraları, otobüs/durak/hat örnekleri.
- hedef: confusion matrix, false STOP, missed hazard, depth coverage/confidence, phone pitch etkisi.

### Çıkış kriteri
Zemin/seviye adayları gerçek saha veri setinde yeterli doğruluk ve düşük yanlış alarm göstermeden kesin semantik dili açılmaz ve M4'e geçilmez.

---

## MILESTONE 4 — ELLER SERBEST, EKRAN KAPALI, GÜNLÜK KULLANIM VE ACİL DURUM
**Hedef sürümler:** v0.18.x
**Durum:** PENDING

### Kapsam
- Uzun süreli foreground rehberlik mimarisi.
- Ekran kapalı GPS/rota yanında kamera/algı yaşam döngüsünün Android kurallarına uygun cihaz doğrulaması.
- Düşük güç gerçek wake-word seçeneği; platform SpeechRecognizer sürekli hotword olarak yanlış kullanılmaz.
- Spatial/stereo audio ve yönlü haptic desenleri.
- Pil ve thermal degradasyon politikası: ağır modellerin adaptif FPS/çözünürlük yönetimi.
- Kamera/Depth/IMU kanal kaybında açık fail-safe ve otomatik recovery.
- Yardım komutu, düşme adayı, iptal penceresi, kullanıcı tarafından atanmış acil kişiye arama/konum paylaşma akışı.
- Telefon/SMS ve izin verilen mesajlaşma işlemleri.
- Para/ürün/OCR/günlük hayat özelliklerinin erişilebilir tek-el/ekransız kullanımı.

### Otomatik kabul
- Safety > navigation > world-model > günlük bilgi konuşma sırası değişmez.
- thermal degradasyon SafetyGate'i kapatmaz; ağır semantic kanalları azaltır.
- proses yeniden başlarsa kritik kullanıcı durumu güvenli şekilde geri kurulur veya açıkça kayıp olduğu söylenir.
- mikrofon reddi kamera güvenliğini durdurmaz.

### S24 Ultra uzun süre testi
- 30, 60 ve 120 dakika yürüyüş simülasyonu.
- ekran açık/kapalı, düşük pil, cihaz ısınması, ağ kaybı, ARCore dropout, CameraX fallback, telefon kilit/aç.
- hedef: crash-free süre, battery drain, thermal state, P95 latency, recovery başarısı.

### Çıkış kriteri
Uzun süreli rehberlik ve recovery testleri geçmeden ürün-adayı sahaya çıkmaz.

---

## MILESTONE 5 — ENTEGRE SAHA DOĞRULAMA / ÜRÜN ADAYI
**Hedef sürümler:** v0.19.x → v1.0 candidate
**Durum:** PENDING

### Kapsam
- M1–M4 tüm algı, navigasyon, voice, OCR, emergency ve durability kanallarının tek ürün davranışında birleştirilmesi.
- TalkBack tam erişilebilirlik turu.
- izin onboarding, privacy, local-first veri politikası, hata raporu, Play Store hazırlığı.
- gerçek rota + engel + dinamik kişi + zemin değişimi + son giriş aynı senaryoda test edilir.
- controlled-field metrics dashboard/report.

### Zorunlu saha senaryoları
- duvar, kapı, masa/sandalye, ince direk, düşük/yüksek engel.
- yaklaşan/yan geçen insan, bisiklet ve kontrollü araç.
- kaldırım, yukarı/aşağı basamak, merdiven, rampa, kontrollü çukur.
- kavşak, yaya geçidi, trafik ışığı adayı.
- bina girişi/kapı numarası, durak/otobüs/hat.
- düşük ışık, karşı ışık, yağmur/ıslak zemin, lens kısmi örtme.
- telefon sallanması, hızlı yön değişimi, rota sapması, internet kaybı, ARCore/Depth kaybı.
- safety + navigation aynı anda geldiğinde safety konuşma önceliği.

### Ölçülecek ürün KPI'ları
- hazard false-negative / false-positive oranı,
- semantic confusion matrix,
- trajectory/path-conflict precision/recall,
- P50/P95 algı→karar→ses gecikmesi,
- Depth availability/coverage/confidence,
- CameraX↔ARCore fallback başarısı,
- route completion ve reroute başarısı,
- entrance/transit OCR doğruluğu,
- battery/thermal/crash-free süre,
- TalkBack görev tamamlama oranı.

### Nihai ürün kapısı
Bu metrikler kontrollü saha kabul sınırlarını geçmeden uygulama "bağımsız güvenli kör navigasyonu" olarak tanımlanmaz. Uygulama baston/kılavuz köpek ve yönelim-hareketlilik becerilerinin yerine geçtiğini iddia etmez.

---

## İlerleme kuralı
- Kullanıcı "Devam" dediğinde ACTIVE milestone içindeki en yüksek riskli eksik madde seçilir ve uygulanır.
- Her anlamlı kod paketinden sonra otomatik test + CI APK kapısı çalıştırılır.
- Milestone cihaz testine hazır olduğunda APK ve net test senaryosu verilir.
- Kullanıcı test sonucunu/screenshot/log/Gate raporunu gönderir; başarısız madde aynı milestone içinde düzeltilir.
- Çıkış kriteri geçmeden milestone numarası yükseltilmez.

## Şu anki pozisyon
- v0.14: temporal 3×3 world model, çoklu nesne tracking ve uzak semantic awareness CI seviyesinde hazır.
- ACTIVE: MILESTONE 1.
- Sıradaki geliştirme: semantic segmentation + scene partition + temporal smoothing + world-model fusion.
