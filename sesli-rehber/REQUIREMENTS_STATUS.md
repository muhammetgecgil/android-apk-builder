# Sesli Rehber — Gereksinim Uygulama Durumu

Bu dosya `PRODUCT_REQUIREMENTS.md` ve `GUIDE_MATURITY_ROADMAP.md` ile birlikte okunur. `DONE-CI` yalnız kodun ve otomatik kabul testlerinin geçtiğini gösterir. Gerçek cihaz/saha doğrulaması gereken maddeler Gate C / Gate D tamamlanana kadar ürün güvenilirliği iddiası değildir.

## Çekirdek durum

| ID / Alan | Durum | Not |
|---|---|---|
| VIS-001 | DONE-CI | CameraX arka kamera analiz akışı |
| VIS-002 | DONE-CI | KEEP_ONLY_LATEST backpressure |
| VIS-003 | DONE-CI | Portre rotation + 0/90/180/270 koordinat testleri |
| VIS-004 | DONE-CI | Statik sahnede de frame heartbeat; watchdog yalnız gerçek stale akışta tetiklenir |
| IMU-001 | DONE-CI | İvmeölçer + jiroskop kararlılık skoru |
| IMU-002 | DONE-CI | Görsel karar güveni IMU ile füzyon |
| IMU-004 | DONE-CI | Eksik/stale sensör fail-safe |
| OBJ-001..005 | DONE-CI | Generic tracking, yön, büyüme/yaklaşma; kalibrasyonsuz metre yok |
| TTC-001 | PARTIAL-CI | Göreli yaklaşma/risk var; fiziksel TTC kalibrasyonu yok |
| TTC-002 | DONE-CI | Dinamik merkez çarpışma koridoru |
| GROUND-001 | DONE-CI | Alt-merkez yürüyüş koridoru zemin sürekliliği kanıtı |
| GROUND-002..005 | PARTIAL-CI | Çoklu kanıt altyapısı var; semantik çukur/kaldırım/merdiven sınıfı cihaz/saha doğrulaması bekliyor |
| DEPTH-001 | DEVICE-TEST | ARCore canlı Depth16 yolu kodlandı/CI geçti; gerçek cihaz Depth coverage testi bekliyor |
| DEPTH-002 | DONE-CI | Kalibrasyon olmadan kullanıcıya metre söylenmiyor |
| WALK-001 | DONE-CI | Upright Depth16 üzerinde sol/orta/sağ göreli açıklık skoru |
| WALK-002 | DONE-CI | Tek kare yön önerisi yasak; çoklu kare kalıcılık gerekli |
| WALK-003 | DONE-CI | Yalnız “daha açık görünüyor” dili; “güvenli yol” veya zorunlu dönüş talimatı yok |
| WALK-004 | DEVICE-TEST | Gerçek cihazda koridor yön hizası, yanlış yön ve kapsama testi bekliyor |
| DEC-001..005 | DONE-CI | INFO/CAUTION/STOP, fail-safe, STOP önceliği/cooldown |
| DEC-006 | DONE-CI | Safety > navigation > scene konuşma öncelik sözleşmesi; hazard sonrası rota hold penceresi |
| A11Y-001..004 | PARTIAL-CI | Büyük kontroller, açıklamalar, ses/metin/titreşim; TalkBack cihaz testi bekliyor |
| HEALTH-001 | DONE-CI | Vision akış watchdog: stale kamera güvenli sayılmaz |
| HEALTH-002 | DONE-CI | Uzun Depth kaybında CameraX fallback politikası |
| HEALTH-003 | DONE-CI | Kalıcı karanlık/aşırı parlak/örtülü-düz görüntü ayrı kamera sağlık kanalı; tek kare STOP değil |
| VALID-001 | DONE-CI | Gate C CSV kayıt/özet altyapısı |
| VALID-002 | DONE-CI | P95 karar gecikmesi + Depth coverage + fallback + termal ölçüm özeti |
| VALID-003 | DONE-CI | Rapor FileProvider ile kullanıcı onayıyla paylaşılabilir |
| VOICE-001 | DONE-CI | Kamera rehberliği mikrofon izninden ayrıldı; mic yalnız ses özelliğinde istenir |
| VOICE-002 | PARTIAL-CI | API31+ on-device recognizer varsa foreground “Hey Rehber” döngüsü; gerçek düşük-güç hotword DSP değildir, cihaz/pil/Türkçe model testi bekliyor |
| VOICE-003 | DONE-CI | Offline deterministik Türkçe niyet parser: güvenlik, sahne, OCR, hedef ve wake-mode komutları |
| VOICE-004 | DEVICE-TEST | Gürültü, yanlış wake, TTS self-trigger ve uzun kullanım doğrulaması bekliyor |
| OCR-001 | DONE-CI | Bundled ML Kit Latin OCR; CameraX ve ARCore CPU frame üzerinde isteğe bağlı tek-kare okuma |
| OCR-002 | DEVICE-TEST | Tabela, kapı numarası, düşük ışık ve eğik metin doğruluk testi bekliyor |
| SCENE-001 | DONE-CI | “Çevremi anlat” yalnız taze scene/object/ground/depth/walkable kanıtından muhafazakâr özet üretir |
| NAV-001 | PARTIAL-CI | “Beni <hedef> götür” hedef metni çıkarılır; rota motoru henüz bağlı değildir |
| SEG-001 | DONE-CI | Genel DeepLab semantic segmentation advisory kanalı |
| SEG-002 | DONE-CI | PIDNet-S Cityscapes urban semantic segmentation; SafetyGate yetkisi yok |
| SEG-003 | DEVICE-TEST | S24 Ultra gerçek sahnede urban sınıf tekrarı, GPU/CPU backend, p95 ve termal doğrulama |
| VALID-004 | DONE-CI | Urban Gate CSV: senaryo, backend, p95, sınıf oranları, battery/thermal |
| VALID-005 | DONE-CI | Urban Gate otomatik PASS/REVIEW/FAIL kabul motoru; sonuç tek başına M1 DONE değildir |

## Release kapıları

- **Gate A — CI:** unit test + debug APK + artifact başarılı olmalı.
- **Gate B — algoritma:** sentetik merkez yaklaşma STOP, yan küçük nesne INFO, yandan koridora giriş CAUTION, zemin/depth çift-kanal davranışı, camera-health, walkable persistence, Türkçe intent ve scene-summary testleri geçmeli.
- **Gate C — cihaz:** gerçek cihazda ARCore↔CameraX handoff, Depth coverage, portre hizası, yürüyüş koridoru yön doğruluğu, kamera sağlık yanlış alarmı, OCR, Hey Rehber, karar gecikmesi, ısınma ve 30 dk kararlılık ölçülmeli.
- **Gate D — saha:** kontrollü kapalı alanda bastonlu senaryolar geçmeden “güvenilir yaya yardımcısı” iddiası yapılmaz.

## v0.4 — zemin sürekliliği
- Alt-merkez zemin sürekliliği, geniş yatay sınır, doku ve zamansal kalıcılık kanıtları.
- Tek kare/gölge doğrudan STOP vermez.

## v0.5 — Depth hazır füzyon
- ARCore optional + Depth16 decoder + geometri analizörü.
- Ground+Depth <=280 ms senkronize edilmeden çift-kanal STOP yok.

## v0.6 — canlı ARCore Depth16
- ARCore kamera CPU görüntüsü + canlı Depth16 + ortak nesne/zemin çekirdeği.
- CameraX→ARCore kontrollü handoff; ARCore hata verirse CameraX fallback.
- ARCore resmi koordinat dönüşümüyle CPU kamera/Depth crop hizası.

## v0.7 — Gate C cihaz doğrulama ve çalışma sağlığı
- `VisionHealthWatchdog`: 1.8 s vision stale => STOP/fail-safe; uzun Depth kaybı => fallback önerisi.
- Gate C kayıt: mod geçişleri, observation yaşları, IMU kararlılığı, Depth coverage/confidence, zemin/nesne metrikleri, CAUTION/STOP, fallback, P95 karar gecikmesi, batarya sıcaklığı ve Android thermal status.
- CSV yalnız uygulama özel cache alanında tutulur; dışarı ancak kullanıcı “Raporu Paylaş” dediğinde FileProvider ile çıkar.

## v0.8 — M1 yürüyüş güvenliği / olgunluk temeli
- `SceneHealthEstimator`: kalıcı karanlık, aşırı pozlama ve düşük dokulu/örtülü görüntü fail-safe kanalı.
- `WalkableCorridorEstimator`: üç şeritli göreli açıklık analizi ve kalıcı yön adayı.
- `PerceptionContext`: scene/walkable kanıtları SafetyGate’e yalnız taze zaman penceresinde taşınır; stale kanıt füzyonu yasak.
- `GuidancePriorityArbiter`: navigasyon ve sahne konuşması güvenlik konuşmasını bastıramaz.
- Kamera frame heartbeat statik sahnede de watchdog’a gider; “hareket yok = kamera dondu” regresyonu kapatıldı.
- Kamera rehberliği RECORD_AUDIO izninden ayrıldı.

## v0.9 — M2 eller serbest erişim ve bilgi alma
- `VoiceCommandController`: on-device recognizer varsa foreground “Hey Rehber” dinleme döngüsü; uygulama arka planda mikrofon oturumunu kapatır.
- Sürekli çevrimiçi tanımaya sessiz fallback yok; yerel destek yoksa tek-seferlik Sesli Komut kalır.
- Türkçe parser hedef/adres, OCR, sahne özeti ve wake-mode komutlarını çıkarır; “durak” substring’i yanlış STOP üretmez.
- `SceneSummaryState`: yalnız taze algı kanıtlarından kısa, semantik iddiası sınırlı çevre özeti.
- Bundled ML Kit OCR hem CameraX hem ARCore kamera yolunda bir sonraki kareyi okuyabilir; safety luma/zemin heartbeat’i OCR sırasında korunur.
- Hedef metni M3 rota motoruna hazırlanır; rota olmadığı halde yönlendirme başlamış gibi konuşulmaz.
- **Final Gate A:** GitHub Actions run `33102884137`, head `0f9cb09d0f177017e6aa74d1d67d60648090e0fe`: unit test SUCCESS, debug APK SUCCESS, artifact SUCCESS.
- Gate C / saha sonucu hâlâ `DEVICE-TEST`; CI gerçek telefon, ses modeli ve gerçek yürüyüş güvenilirliğini kanıtlamaz.

## v0.15–v0.16 — semantic + urban segmentation
- Genel DeepLab semantic segmentation ve PIDNet-S Cityscapes urban segmentation eklendi.
- Urban kanal ayrı worker’da çalışır; kamera/safety thread’ini bloke etmez.
- GPU tercih edilir; CPU yalnız düşük-frekans fallback’tir.
- Road/sidewalk/building/wall/fence/pole/traffic-control/person/vehicle/two-wheeler gibi urban kanıtlar advisory world-model girdisidir.
- Segmentasyon tek başına “güvenli yol”, “geç” veya zorunlu dönüş komutu üretmez.

## v0.17 — M1 Urban Gate cihaz ölçümü
- Ayrı Urban Gate CSV akışı eklendi.
- Senaryolar: Kaldırım, Yol kenarı, Bina/Duvar, Direk/Çit, Trafik ışığı/Tabela, İnsan/Araç, Düşük ışık.
- Backend, inference sayıları, p95, temporal stability, sınıf oranları, lower-center yüzey/engel, batarya sıcaklığı ve Android thermal status kaydedilir.
- Urban Gate telemetrisi SafetyGate kararlarını değiştirmez.

## v0.18 — M1 otomatik cihaz kabul motoru
- `UrbanGateAcceptance` test sonunda muhafazakâr `PASS / REVIEW / FAIL` sonucu üretir.
- Değerlendirme: backend, toplam inference, hata oranı, GPU/CPU p95, batarya sıcaklığı, thermal status, her zorunlu urban senaryoda minimum kare ve beklenen kanıt tekrar oranı.
- CPU fallback otomatik PASS değildir; en iyi durumda REVIEW olur.
- Missing scenario, ciddi inference hatası, aşırı p95/ısı/thermal veya belirgin zayıf semantik kanıt FAIL üretir.
- Düşük ışık semantik güvenlik kanıtı değildir; scene-health kanalı otoritesini korur.
- Sonuç CSV’ye `ACCEPTANCE` marker olarak yazılır ve uygulama özetinde gösterilir.
- PASS yalnız Urban Gate mühendislik sonucudur. **M1 ancak gerçek S24 Ultra Urban Gate sonucu + aynı cihaz Gate C safety-latency incelemesi kabul edilirse DONE olabilir.**
- **Gate A:** GitHub Actions run `33242690144`, code head `9ab8abab6f76652a6190d82d478cd6761927ae05`: pinned model doğrulama SUCCESS, unit/regression SUCCESS, debug APK SUCCESS, artifact SUCCESS.
