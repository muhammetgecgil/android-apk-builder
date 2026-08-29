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

## v0.18 — M1 automatic urban acceptance
- `UrbanGateAcceptance` gerçek cihaz Urban Gate ölçümünü PASS / REVIEW / FAIL olarak muhafazakâr değerlendirir.
- Backend, inference hata oranı, p95 latency, batarya sıcaklığı, Android thermal status ve yedi urban saha senaryosunun minimum kare/kanıt tekrar oranı birlikte kullanılır.
- CPU fallback otomatik PASS değildir; en iyi durumda REVIEW olur. Eksik/zayıf senaryo, ciddi hata/ısı/thermal veya aşırı latency FAIL üretebilir.
- Sonuç CSV içine `ACCEPTANCE` marker olarak yazılır ve test özetine eklenir.
- Urban Gate PASS yalnız segmentasyon cihaz-kabul sonucudur; bağımsız kör navigasyon güvenliği değildir.

## v0.20 — camera-first situational HUD
- Ana arayüz kamera görüntüsünü neredeyse tam ekran tutar; mühendislik/test kontrolleri varsayılan olarak gizli `Test / Geliştirici` çekmecesine taşınır.
- Ana ekranda kalıcı üç kontrol vardır: `SES`, `TEST …`, `ACİL DUR`.
- PIDNet Cityscapes 128x128 label maskesi gerçek CameraX görüntüsü üstünde yarı saydam piksel overlay olarak çizilir.
- HUD, 3x3 world-model occupancy, çoklu nesne izleri, yaklaşma/hareket işaretleri, zemin sınırı, göreli açık koridor adayı ve awareness/complexity telemetrisi gösterir.
- ARCore kamera sahibi olduğunda CameraX PreviewView gizlense bile düşük oranlı gerçek ARCore CPU kamera kareleri HUD arka planına verilir; görsel HUD hiçbir zaman SafetyGate girdisi değildir.
- ARCore görsel frame üretimi Depth evidence işlendikten sonra yapılır ve yalnız görselleştirme amaçlı düşük çözünürlük/rate ile sınırlandırılır.
- Segmentasyon maskesi ve HUD izleri stale olduğunda otomatik söner.
- HUD ve UI katmanı konuşulan/yazılı güvenlik kararlarını değiştirmez; mevcut STOP > CAUTION > navigation/OCR/test sırası korunur.
- **Final Gate A:** run `33246766653`, head `7bd18b6d1384cc867f51a3f683375a5253174ba3`: pinned DeepLab/PIDNet verification SUCCESS, unit/regression SUCCESS, debug APK SUCCESS, artifact SUCCESS.
- Artifact id `9713118736`; ZIP digest `sha256:4b2f0fcc9c48b5f0b78678caaa57cc5abd5dffec653415818492142413200b6f`.
- Final APK size `196,378,376` bytes; SHA-256 `9c34b95455eeff6dcc87394c9517e93628d81db7ae158fb818666546ca34126e`.
- APK ZIP integrity passed, `APK Sig Block 42` mevcut; packaged DeepLab/PIDNet asset hashes pinned build contract ile eşleşti.
- v0.20 gerçek S24 Ultra ekran/ARCore performans testi hâlâ DEVICE-TEST’tir.
