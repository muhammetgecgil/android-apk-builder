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
| VOICE-001 | DONE-CI | Kamera rehberliği mikrofon izninden ayrıldı; mic yalnız sesli komutta istenir |
| VOICE-002 | NOT-STARTED | Sürekli yerel “Hey Rehber” wake-word |

## Release kapıları

- **Gate A — CI:** unit test + debug APK + artifact başarılı olmalı.
- **Gate B — algoritma:** sentetik merkez yaklaşma STOP, yan küçük nesne INFO, yandan koridora giriş CAUTION, zemin/depth çift-kanal davranışı, camera-health ve walkable persistence testleri geçmeli.
- **Gate C — cihaz:** gerçek cihazda ARCore↔CameraX handoff, Depth coverage, portre hizası, yürüyüş koridoru yön doğruluğu, kamera sağlık yanlış alarmı, karar gecikmesi, ısınma ve 30 dk kararlılık ölçülmeli.
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
- Kamera rehberliği RECORD_AUDIO izninden ayrıldı; mikrofon yalnız kullanıcı sesli komut istediğinde sorulur.
- Gate C / saha sonucu hâlâ `DEVICE-TEST`; CI gerçek telefon ve gerçek yürüyüş güvenilirliğini kanıtlamaz.
