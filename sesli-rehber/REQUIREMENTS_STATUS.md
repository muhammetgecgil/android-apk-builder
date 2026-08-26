# Sesli Rehber — Gereksinim Uygulama Durumu

Bu dosya `PRODUCT_REQUIREMENTS.md` ile birlikte okunur. `DONE` yalnız kod yazıldığı anlamına gelmez; CI ve ilgili kabul kriteri de geçmelidir. S24 Ultra saha doğrulaması gerektiren maddeler cihaz testi yapılana kadar `DEVICE-TEST` kalır.

## v0.3 kapsamı

| ID | Durum | Not |
|---|---|---|
| VIS-001 | DONE-CI | CameraX arka kamera analiz akışı |
| VIS-002 | DONE-CI | `STRATEGY_KEEP_ONLY_LATEST` |
| VIS-003 | DONE-CI | Target rotation + analiz `rotationDegrees`; hareket centroid dönüşümü testli |
| IMU-001 | DONE-CI | İvmeölçer + jiroskop kararlılık skoru |
| IMU-002 | DONE-CI | Görsel karar güveni IMU kararlılığıyla düşürülüyor |
| IMU-004 | DONE-CI | Eksik/stale sensör artık tam güvenli kabul edilmiyor |
| OBJ-001 | DONE-CI | ML Kit STREAM_MODE bounding box + tracking ID |
| OBJ-002 | DONE-CI | LEFT/CENTER/RIGHT yönü |
| OBJ-003 | DONE-CI | Takip ID bazlı EMA kutu büyüme hızı |
| OBJ-004 | DONE-CI | Merkez koridorda büyük/hızlı yaklaşan nesne STOP |
| OBJ-005 | DONE-CI | Bounding-box büyümesi metre mesafesi diye sunulmuyor |
| TTC-001 | PARTIAL-CI | Yaklaşma + yanal merkeze giriş + geometri + IMU risk skoru var; kalibre fiziksel TTC yok |
| TTC-002 | DONE-CI | Dinamik merkez çarpışma koridoru |
| DEC-001..004 | DONE-CI | INFO/CAUTION/STOP, fail-safe ve düşük güven dili |
| DEC-005 | DONE-CI | AnnouncementGate ile STOP önceliği/cooldown |
| A11Y-001..004 | PARTIAL-CI | Büyük kontroller + açıklamalar + ses/metin/titreşim; tam TalkBack cihaz testi bekliyor |
| GROUND-001..005 | NOT-STARTED | v0.4 hedefi: zemin/serbest alan/merdiven/kaldırım/çukur çoklu kanıt |
| DEPTH-001..002 | NOT-STARTED | v0.4/v0.5; kalibrasyonsuz metre söylenmeyecek |
| VOICE-002 | NOT-STARTED | Sürekli yerel “Hey Rehber” |

## Release kapıları

- **Gate A — CI:** unit test + debug APK başarılı olmalı.
- **Gate B — algoritma:** yapay senaryolarda merkez yaklaşma STOP, yan küçük nesne INFO, koridora giren yan nesne CAUTION testleri geçmeli.
- **Gate C — cihaz:** S24 Ultra kamera yönü, yanlış alarm, kaçırma, gecikme ve ısınma ölçülmeli.
- **Gate D — saha:** kontrollü kapalı alanda bastonlu senaryolar geçmeden “güvenilir yaya yardımcısı” iddiası yapılmaz.

## v0.4 hedefi

1. Yürünebilir alan maskesi için on-device segmentasyon bağlantı noktası.
2. Zemin sürekliliği ve perspektif/geometri kanalı.
3. Monoküler göreli derinlik bağlantı noktası.
4. Çukur/aşağı basamak için en az iki bağımsız kanıt + IMU yönelimi şartı.
5. Merdiven/kaldırım adaylarını semantik model doğrulamasına hazırlayan olay modeli.
6. P95 algı+karar gecikmesi ölçümü.
