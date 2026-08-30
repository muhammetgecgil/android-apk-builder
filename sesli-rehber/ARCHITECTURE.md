# Mimari v0.1

`MainActivity` → CameraX `VisionMotionAnalyzer` → IMU `SensorFusionManager` → `SafetyGate` → `GuidanceSpeaker`.

Ses komutu: `VoiceCommandController` → `OfflineIntentParser` → uygulama komutu.

Gelecek: Camera frame → object detector + free-space segmentation + depth → `SceneUnderstandingEngine` → safety fusion → guidance.

## Öncelik
1. Sensör/kamera geçersiz veya çok kararsız: DUR / doğrula.
2. Fiziksel engel riski navigasyon komutunun önüne geçer.
3. Güven düşükse nesne adı kesin söylenmez.
4. İnternet yoksa temel güvenlik fonksiyonları yerelde çalışmalıdır.
