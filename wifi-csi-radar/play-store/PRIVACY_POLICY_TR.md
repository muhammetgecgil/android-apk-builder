# Gizlilik Politikası — WiFi Bilimsel Radar

Son güncelleme: 26 Ağustos 2026

WiFi Bilimsel Radar çevredeki Wi‑Fi erişim noktalarının teknik tarama sonuçlarını (ör. BSSID, RSSI ve frekans) cihaz üzerinde işler. Uygulama bu verileri geliştiricinin bulut sunucusuna göndermez, reklam veya üçüncü taraf analitik SDK'sı kullanmaz ve kullanıcı hesabı oluşturmaz.

## İzinler
- **Yakındaki Wi‑Fi cihazları:** Yakındaki Wi‑Fi ağlarının teknik ölçüm verilerine erişmek için.
- **Konum:** Android platformu bazı Wi‑Fi tarama sonuçlarına erişim için konum izni isteyebilir. Uygulama GPS konum geçmişi oluşturmaz.
- **İnternet/ağ erişimi:** Kullanıcının girdiği özel/yerel IP adresindeki ESP32 CSI düğümlerine bağlanmak için.

## Yerel saklama
Boş oda referansı ve kullanıcı tarafından adlandırılan Wi‑Fi bölge parmak izleri yalnız uygulamanın yerel ayar alanında saklanır. Uygulama kaldırıldığında bu yerel veriler silinir.

## Ağ sınırı
ESP32 ekranı yalnız özel, link-local veya loopback IP adreslerini kabul eder. Kamu internetindeki IP adreslerine CSI telemetrisi göndermek için tasarlanmamıştır.

## Bilimsel ve mahremiyet sınırı
Uygulama kimlik tespiti, yüz tanıma veya biyometrik tanımlama yapmaz. Tek telefonla insanın kesin X‑Y konumunu veya iskelet pozunu tespit ettiğini iddia etmez.
