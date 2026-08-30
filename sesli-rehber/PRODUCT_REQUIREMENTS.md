# Sesli Rehber — Ana Ürün ve Güvenlik Gereksinimleri

Bu dosya projenin ana geliştirme sözleşmesidir. Yeni bir özellik yalnızca kodlandığı için tamamlanmış sayılmaz; ilgili gereksinim ve kabul kriteri karşılanmalı, test edilebilir olmalı ve güvenlik kapısından geçmelidir.

## 1. Ürün amacı

Sesli Rehber, görme engelli bir kullanıcının telefonunu elde veya göğüs hizasında taşıyarak çevresindeki tehlikeleri daha erken fark etmesine, yürünebilir alanı anlamasına, çevreyi sesli olarak sorgulamasına ve yaya navigasyonu desteği almasına yardım eden Android uygulamasıdır.

**Temel güvenlik ilkesi:** Uygulama beyaz baston, rehber köpek veya bağımsız hareket eğitiminin yerine geçmez. Algı güveni düşükse sistem kesin ifade kullanmaz; gerekirse kullanıcıya durmasını ve fiziksel yöntemle doğrulamasını söyler.

## 2. Değişmez geliştirme kuralları

- **G-001 — Güvenlik önce:** Yanlış negatif ve yanlış pozitif riskleri ölçülmeden 'güvenli', 'yol açık', 'çukur yok' gibi kesin ifadeler kullanılmaz.
- **G-002 — Fail-safe:** Kamera, model, sensör veya zamanlama güvenilir değilse rehberlik daha ihtiyatlı moda geçer.
- **G-003 — Kaynak ayrımı:** Hareket algısı; nesne algısı; derinlik; zemin/çukur; navigasyon ayrı kanallardır. Bir kanalın çıktısı başka bir kanal varmış gibi sunulmaz.
- **G-004 — On-device first:** Hayati uyarılar internet bağlantısına bağımlı olmayacak şekilde tasarlanır. İnternet yalnızca zenginleştirici işlevler için kullanılabilir.
- **G-005 — Ölçülebilir kabul:** Her yüksek öncelikli özellik için test senaryosu ve kabul eşiği yazılır.
- **G-006 — Gürültü kontrolü:** Sesli uyarılar üst üste binmez; STOP > CAUTION > INFO önceliği korunur.
- **G-007 — Erişilebilirlik:** Uygulama yalnızca görsel UI ile kullanılamaz hale gelmeyecek; TalkBack ve sesli komut akışı birinci sınıf kullanım yoludur.
- **G-008 — Geri dönüş:** Yeni algılayıcı başarısız olursa önceki güvenli çekirdek tamamen kaybolmaz; düşük seviyeli hareket/sensör güven kapısı yedek olarak kalır.
- **G-009 — Derleme kapısı:** GitHub Actions unit test + debug APK üretimi geçmeden sürüm APK olarak sunulmaz.
- **G-010 — Saha iddiası yok:** S24 Ultra üzerinde gerçek cihaz testi yapılmamış özellik 'doğrulandı' olarak etiketlenmez.

## 3. Öncelik seviyeleri

- **P0:** Hayati güvenlik / temel kullanım. Sürüm bloklayıcı.
- **P1:** Ürünün ana değerini oluşturan işlev.
- **P2:** Yardımcı / zenginleştirici özellik.
- **P3:** İleri dönem / deneysel.

## 4. Algılama ve sensör gereksinimleri

### Kamera ve zamanlama

- **VIS-001 (P0):** Arka kamera canlı analiz akışı sağlamalı.
  - Kabul: Kamera izni verildiğinde analiz başlar; hata halinde rehberlik pasifleşir ve sesli hata verilir.
- **VIS-002 (P0):** Analiz kuyruğu gecikme biriktirmemeli.
  - Kabul: CameraX `STRATEGY_KEEP_ONLY_LATEST` veya eşdeğer davranış.
- **VIS-003 (P0):** Kamera dönüş bilgisi her görüntü analizinde hesaba katılmalı.
- **VIS-004 (P1):** Algı gecikmesi cihaz üzerinde ölçülebilir olmalı.
  - Hedef: p95 algı+karar gecikmesi <= 350 ms; STOP yolu için hedef <= 250 ms.

### IMU sensör füzyonu

- **IMU-001 (P0):** İvmeölçer ve jiroskop cihaz kararlılığı üretmeli.
- **IMU-002 (P0):** Aşırı telefon hareketinde görsel güven düşürülmeli.
- **IMU-003 (P1):** Telefon yönelimi yürüyüş/zemin analizi için kullanılmalı.
- **IMU-004 (P1):** Sensör kaybı veya anormal veri sessizce güvenli kabul edilmemeli.

### Genel nesne algılama

- **OBJ-001 (P0):** Canlı akışta nesne sınırlayıcı kutuları ve takip kimliği alınmalı.
- **OBJ-002 (P0):** Nesnenin ekrandaki yönü LEFT/CENTER/RIGHT olarak hesaplanmalı.
- **OBJ-003 (P0):** Aynı takip kimliği için kutu alanı değişiminden yaklaşma göstergesi hesaplanmalı.
- **OBJ-004 (P0):** Merkezde hızlı yaklaşan büyük nesne STOP seviyesine çıkabilmeli.
- **OBJ-005 (P0):** Bir nesnenin sadece var olması mesafe bilgisi varmış gibi sunulmamalı.
- **OBJ-006 (P1):** Özel on-device model ile en az şu sınıflar hedeflenecek: insan, otomobil, motosiklet/bisiklet, otobüs/kamyon, direk, duvar/bariyer, kapı, sandalye/masa, çöp kutusu, merdiven.
- **OBJ-007 (P1):** Sınıf güveni eşik altındaysa sınıf adı söylenmeyecek; sadece 'engel/nesne' denecek.

### Yürünebilir alan ve zemin tehlikesi

- **GROUND-001 (P0 hedef):** Yürünebilir zemin maskesi ayrı model/algoritmadan gelmeli.
- **GROUND-002 (P0 hedef):** Çukur, aşağı basamak ve kaldırım kenarı yalnız hareket algısından türetilmemeli.
- **GROUND-003 (P0 hedef):** Çukur/aşağı basamak uyarısı çoklu kanıtla doğrulanmalı: geometri/derinlik + zemin sürekliliği + IMU yönelimi.
- **GROUND-004 (P0 hedef):** Çukur doğruluğu saha testi olmadan ürün iddiası yapılmayacak.
- **GROUND-005 (P1):** Yukarı merdiven, aşağı merdiven ve kaldırım yükselmesi ayrılmalı.

### Derinlik ve çarpışma riski

- **DEPTH-001 (P0 hedef):** Monoküler derinlik veya cihaz destekli derinlik ayrı güven skoru üretmeli.
- **DEPTH-002 (P0 hedef):** Metre cinsinden mesafe yalnız kalibre edilmiş doğrulama sonrası kullanıcıya söylenmeli.
- **TTC-001 (P0):** Yaklaşma riski; nesne büyüme oranı + göreli konum + cihaz kararlılığı ile hesaplanmalı.
- **TTC-002 (P0):** STOP kararında merkez çarpışma koridoru yan nesnelerden daha yüksek ağırlık taşımalı.
- **TTC-003 (P1):** Gerçek zamanlı time-to-collision tahmini eklendiğinde kalibrasyon hatası raporlanmalı.

## 5. Rehberlik karar motoru

- **DEC-001 (P0):** Risk seviyeleri `INFO`, `CAUTION`, `STOP` olmalı.
- **DEC-002 (P0):** Öncelik sırası STOP > CAUTION > INFO.
- **DEC-003 (P0):** STOP uyarısı kısa ve eylem odaklı olmalı: ör. 'Dur. Önünde yaklaşan engel.'
- **DEC-004 (P0):** Düşük güven 'yol açık' sonucuna çevrilmemeli.
- **DEC-005 (P0):** Aynı uyarı için konuşma cooldown uygulanmalı fakat yeni STOP önceki cooldown tarafından engellenmemeli.
- **DEC-006 (P1):** Sol/sağ yön bilgisi titreşim paterniyle de verilmeli.
- **DEC-007 (P1):** Kullanıcı profiline göre uyarı yoğunluğu: minimal / normal / ayrıntılı.

## 6. Sesli asistan ve çevrimdışı kullanım

- **VOICE-001 (P0):** Türkçe temel komutlar internet olmadan ayrıştırılmalı: başlat, durdur, tekrar et, çevremi anlat, yardım.
- **VOICE-002 (P1):** 'Hey Rehber' sürekli uyandırma yerel wake-word motoruyla çalışmalı.
- **VOICE-003 (P0):** Wake-word hatası rehberlik uyarı kanalını kapatmamalı.
- **VOICE-004 (P1):** TTS kritik uyarıları diğer uzun açıklamalardan keserek öne alabilmeli.
- **VOICE-005 (P1):** 'Çevremi anlat' tek bir kareye değil, kısa zaman penceresinde kararlı nesne setine dayanmalı.

## 7. Navigasyon

- **NAV-001 (P1):** Yaya rotası navigasyon motorundan, yakın engel kaçınması yerel algı motorundan gelmeli; iki kavram karıştırılmamalı.
- **NAV-002 (P1):** GPS doğruluğu kötü olduğunda kullanıcıya açıkça söylenmeli.
- **NAV-003 (P1):** Kavşak/yaya geçidi kararı sadece harita verisine dayandırılmamalı; görsel doğrulama ayrı kanal olmalı.
- **NAV-004 (P2):** Çevrimdışı harita paketi desteği.

## 8. OCR, çevre anlatımı ve günlük yardım

- **OCR-001 (P1):** Tabela/kapı/ürün üzerindeki kısa metin yerel OCR ile okunabilmeli.
- **OCR-002 (P1):** Hareket halinde uzun OCR konuşması kritik tehlike uyarısını bastırmamalı.
- **SCENE-001 (P1):** Sahne anlatımı 'önde insan, sağda masa' gibi konumsal ve kısa olmalı.
- **SCENE-002 (P1):** Sahne anlatımı doğrulanmamış nesne sınıfını kesin gerçek olarak söylememeli.
- **DAILY-001 (P2):** Para/ürün/renk tanıma ayrı görev modu olarak eklenebilir; yürüme güvenlik döngüsünü yavaşlatmamalı.

## 9. Acil durum

- **SOS-001 (P1):** Kullanıcı tek büyük kontrol veya sesli komutla acil dur moduna geçebilmeli.
- **SOS-002 (P1):** Düşme algılama tek sensör eşiğiyle otomatik çağrı başlatmamalı; çoklu kanıt + kullanıcı iptal süresi kullanılmalı.
- **SOS-003 (P1):** Acil kişi/konum paylaşımı kullanıcı izni ve Android politika sınırları içinde tasarlanmalı.

## 10. Erişilebilirlik gereksinimleri

- **A11Y-001 (P0):** Tüm etkileşimli kontroller en az 48dp odak/touch alanına sahip olmalı; kritik kontroller tercihen >=64dp.
- **A11Y-002 (P0):** Her etkileşimli öğe benzersiz ve eylem odaklı erişilebilirlik açıklamasına sahip olmalı.
- **A11Y-003 (P0):** TalkBack ile temel akış kamera görüntüsünü görmeden yönetilebilmeli.
- **A11Y-004 (P0):** Sadece renk ile durum belirtilmemeli; ses/metin/titreşim eşlik etmeli.
- **A11Y-005 (P1):** Açılışta kullanıcının dokunmadan rehberliği başlatabileceği ayar desteklenmeli.
- **A11Y-006 (P1):** Kulaklık/bone-conduction kullanımında kritik çevre seslerini tamamen kapatmayacak ses tasarımı hedeflenmeli.

## 11. Performans ve enerji

- **PERF-001 (P0):** Analiz ana UI thread'ini bloke etmemeli.
- **PERF-002 (P0):** Frame backlog oluşmamalı.
- **PERF-003 (P1):** 30 dakikalık yürüyüşte termal düşüş ve pil tüketimi ölçülmeli.
- **PERF-004 (P1):** Cihaz ısınırsa model frekansı dinamik düşürülebilmeli; güvenlik STOP kanalı tamamen kapanmamalı.

## 12. Gizlilik ve veri

- **PRIV-001 (P0):** Kamera görüntüsü varsayılan olarak cihaz dışına gönderilmemeli.
- **PRIV-002 (P0):** Kayıt yapılmıyorsa kareler kalıcı depoya yazılmamalı.
- **PRIV-003 (P0):** Mikrofon yalnız gerekli komut/wake-word akışında kullanılmalı ve Android izin modeli izlenmeli.
- **PRIV-004 (P1):** Telemetri varsa opt-in ve anonim/ölçüm odaklı olmalı.

## 13. Play Store / Android uyumluluğu

- **PLAY-001 (P0):** Yeni sürümler 31 Ağustos 2026 sonrası Google Play gereği en az target API 36 hedeflemeli.
- **PLAY-002 (P0):** Gereksiz hassas izin istenmemeli.
- **PLAY-003 (P0):** Uygulama çökme/ANR ve temel erişilebilirlik testlerinden geçmeden release adayı sayılmaz.
- **PLAY-004 (P1):** AAB üretimi ve release signing ayrı CI yolu olarak eklenecek.

## 14. Test ve doğrulama kapıları

### Gate A — Derleme
- Unit testler geçer.
- Debug APK GitHub Actions üzerinde üretilir.
- APK artifact'i boş/yanlış paket değildir.

### Gate B — Fonksiyon
- Kamera açılır.
- Sesli uyarı çalışır.
- TalkBack ile ana kontroller erişilir.
- İzin reddinde uygulama güvenli biçimde davranır.

### Gate C — Algı güvenliği
- Sabit telefon + sabit sahne yanlış STOP üretmemeli.
- Telefon hızlı sallandığında 'görüntü kararsız' güvenlik davranışı çalışmalı.
- Merkezde kutusu hızla büyüyen takipli nesne STOP yolunu tetikleyebilmeli.
- Yan bölgede küçük/uzak nesne gereksiz STOP üretmemeli.

### Gate D — S24 Ultra saha testi
Aşağıdaki senaryolar gerçek cihaz videosu veya kontrollü saha ile test edilmeden özellik 'doğrulandı' sayılmaz:
1. Koridorda duvara yaklaşma.
2. Karşıdan yürüyen insan.
3. Yandan geçen insan.
4. Park halindeki otomobil.
5. Yaklaşan otomobil (güvenli kontrollü ortam).
6. Sandalye/masa gibi bel hizası engel.
7. Direk/ince engel.
8. Yukarı merdiven.
9. Aşağı merdiven.
10. Kaldırım kenarı.
11. Çukur/zemin boşluğu.
12. Düşük ışık.
13. Güçlü arka ışık.
14. Telefon sallanması.
15. Kamera kısmen kapalı.

### Gate E — Ürün adayı
- P0 gereksinimlerin tamamı uygulanmış veya açıkça 'ürün dışı' kapsam kararı verilmiş.
- Kritik yanlış negatif/pozitif metrikleri belgelenmiş.
- En az bir görme engelli kullanıcıyla kontrollü kullanılabilirlik değerlendirmesi planlanmış ve etik/güvenli test yöntemi hazırlanmış.

## 15. Sürüm yol haritası

### v0.2 — Nesne yaklaşma çekirdeği
- ML Kit on-device object tracking.
- Bounding-box yönü.
- Tracking ID bazlı büyüme/approach hesabı.
- SafetyGate nesne risk girişi.
- Unit test + CI APK.

### v0.3 — Görsel füzyon ve çarpışma koridoru
- Hareket + nesne + IMU tek karar çerçevesi.
- Ekranın merkezinde dinamik çarpışma koridoru.
- STOP preemption ve uyarı zamanlayıcısı.
- Performans telemetrisi (yalnız yerel debug).

### v0.4 — Yürünebilir alan / zemin
- On-device segmentation.
- Zemin sürekliliği.
- Merdiven/kaldırım prototipi.
- Çukur için çoklu-kanıt deneysel mod; ürün iddiası yok.

### v0.5 — Semantik nesne modeli
- İnsan/araç/direk/engel/merdiven sınıfları için özel model.
- Kalibrasyon ve saha veri seti.

### v0.6 — Sürekli 'Hey Rehber' + sahne anlatımı
- Yerel wake-word.
- Kısa kararlı sahne özeti.
- OCR.

### v0.7+ — Yaya navigasyonu / acil durum / ürünleştirme
- Navigasyon füzyonu.
- Offline harita.
- SOS/düşme algılama.
- Play Store release AAB, gizlilik, test raporları.

## 16. Definition of Done

Bir görev ancak aşağıdakilerin tümü doğruysa **DONE** olur:
1. Gereksinim ID'si belli.
2. Kod tamam.
3. Unit/integration testi veya saha test planı var.
4. Güvenlik etkisi değerlendirildi.
5. Erişilebilirlik etkisi değerlendirildi.
6. CI yeşil.
7. Kullanıcıya verilen iddia, gerçekten doğrulanan kabiliyeti aşmıyor.

Bu rehber sürüm ilerledikçe güncellenir; ancak G-001, G-002, G-003, G-005, G-009 ve G-010 güvenlik ilkeleri gevşetilmez.
