# Sesli Rehber — Requirements Status

Bu dosya güvenlik-kritik erişilebilirlik geliştirmesinde tamamlanan, kısmi ve cihaz testi bekleyen başlıkları izler. `DONE-CI` yalnız otomatik test/derleme kapısının geçtiğini; `DEVICE-TEST` gerçek telefon doğrulamasının gerekli olduğunu ifade eder.

## M1 — Görsel Durumsal Farkındalık / Segmentasyon
- Genel semantic segmentation (DeepLab): DONE-CI.
- Urban semantic segmentation (PIDNet-S / Cityscapes): DONE-CI.
- Urban channel SafetyGate'ten izole, ayrı worker: DONE-CI.
- GPU tercih + düşük frekans CPU fallback: DONE-CI; cihaz backend doğrulaması DEVICE-TEST.
- Urban Gate CSV: DONE-CI.
- Urban Gate senaryoları (kaldırım, yol kenarı, bina/duvar, direk/çit, trafik kontrolü, insan/araç, düşük ışık): DONE-CI; gerçek sahne DEVICE-TEST.
- Urban Gate otomatik kabul motoru: DONE-CI. Sonuçlar PASS / REVIEW / FAIL; model backend, inference hata oranı, p95 latency, batarya/thermal ve senaryo kanıt tekrarlarını birlikte değerlendirir.
- Urban Gate PASS tek başına M1 DONE değildir. M1 kapanışı için gerçek Galaxy S24 Ultra Urban Gate sonucu ve aynı cihazda Gate C safety-latency incelemesi gerekir.

## Safety invariants
- STOP > CAUTION > navigation/OCR/scene önceliği: DONE-CI.
- Frozen/stale vision fail-safe STOP: DONE-CI; DEVICE-TEST bekliyor.
- Depth tek başına STOP üretmez: DONE-CI.
- Segmentasyon tek başına güvenli yol/geçiş onayı üretmez: DONE-CI.
- Urban Gate/telemetry/recorder hataları SafetyGate kararlarını değiştirmez: DONE-CI.

## M2 — Sonraki milestone
M1 cihaz kapısı geçtikten sonra trajectory/path-conflict aşamasına geçilecek: hareket eden nesnelerin yön ve hız eğilimi, kullanıcının yürüyüş hattıyla zamansal çakışma ve safety-priority tahmini. Bu katman gerçek cihaz/saha doğrulaması olmadan bağımsız kör navigasyon güvenliği iddia etmeyecektir.
