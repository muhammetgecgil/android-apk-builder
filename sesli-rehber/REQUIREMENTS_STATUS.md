# Sesli Rehber — Requirements Status

Bu dosya milestone ve cihaz-kabul durumunu özetler. Ayrıntılı geçmiş PR #45 ve Git geçmişinde korunur.

## Aktif ürün yol haritası
- M1 — Sahneyi Anlama: semantic + urban segmentation, temporal world model, kamera-first HUD, cihaz doğrulama.
- M2 — Hareket Tahmini: trajectory / path-conflict.
- M3 — Zemin ve çevre semantiği: Raw Depth/confidence + IMU + basamak/çukur/kaldırım adayları.
- M4 — Sürekli Rehber: ekran kapalı, güç/termal, spatial audio/haptic, recovery.
- M5 — Ürün Adayı: bütünleşik saha doğrulama.

## v0.21 — confidence-gated tracked object identity
- KameraX ve ARCore'daki mevcut çoklu nesne kutuları için ayrı crop-level ML Kit image-labeling sınıflandırması eklendi.
- Sınıflandırma tüm kare yerine takip edilen nesne crop'unda çalışır; aynı anda en fazla iki ilgili nesne ve yaklaşık 760 ms düşük frekans politikası kullanılır.
- ML Kit bundled base image-labeler 400+ genel etiketten Türkçe günlük nesne sözlüğüne çevrilir; koltuk/sofa, sandalye, masa, yatak, yastık, dolap, televizyon, lamba, saat, halı, kapı, pencere, ev aletleri, kişi/araç ve diğer seçili nesneler desteklenir.
- Kesin konuşma politikası: tek okumada >=%86 veya en az iki tutarlı okumada yumuşatılmış >=%72 => `Önde/sağda/solda X var.`
- İki tutarlı okumada >=%58 fakat kesin eşiğin altında => `X olabilir. Güven yüzde ...`.
- Tek zayıf kare kullanıcıya semantik kimlik olarak çıkmaz.
- Aynı etiket+yön için konuşma cooldown'u vardır; adaydan kesin kimliğe yükseliş ayrıca söylenebilir.
- HUD kutuları `NESNE #id` yerine taze semantik varsa `KOLTUK 81%` veya `KOLTUK? 64%` gösterir; hareket/yaklaşma etiketi korunur.
- Semantik isimlendirme SafetyGate girdisi değildir; geometri/Depth/ground STOP-CAUTION zinciri değişmedi.
- Nesne semantik konuşması STOP/CAUTION'u kesmez; TTS QUEUE_ADD ve safety hold ile daha düşük öncelikte kalır.

### v0.21 Gate A
Final code head: `f78deb4a6ecda2ea683375d67517f2ae688c8bd4`
GitHub Actions run: `33247667527`
- Exact DeepLab verification: SUCCESS
- Exact PIDNet-S verification: SUCCESS
- Unit/regression + semantic confidence/speech/mapping tests: SUCCESS
- Build debug APK: SUCCESS
- Upload APK: SUCCESS

Artifact: `SesliRehber-debug-apk` id `9713387331`
Artifact ZIP digest: `sha256:ced6bfbc6c5a9d0ca3fbf2afb513d55147a3f52f7cbbdaf8dab914bf4005fb5d`
Final APK size: `196,394,764` bytes
Final APK SHA-256: `9558e120f340542edc0a0e61d517c8b7da786e3f838cbada77cce7002b6d4ce5`
APK ZIP integrity passed and `APK Sig Block 42` exists. Packaged DeepLab/PIDNet assets match pinned hashes.

## M1 status
M1 code/CI tarafında semantic/urban segmentation + temporal world model + camera-first HUD + confidence-gated object identity bulunuyor. M1 DONE değildir; Galaxy S24 Ultra cihaz testinde HUD hizası, nesne isim doğruluğu, p95 safety latency, Urban Gate ve termal sonuçlar kabul edilmelidir.
