# Ses Görüntü Haritası V7 Probe Fusion

S24 Ultra kamera + IMU referansı, telefon mikrofon referansı ve Fyvadio USB gezen prob mikrofonu ile renkli ses haritası üretir.

GitHub Actions her main güncellemesinde kurulabilir debug APK üretir.

## PCB Zekâ Pro Native

`pcb-analyzer/` bağımsız Android projesidir. CameraX ile gerçek arka kamera önizlemesi,
dokunarak AF/AE netleme, donanımsal pinch/slider zoom, flaş ve cihaz üzerinde ML Kit OCR içerir.

GitHub Actions içindeki **Build PCB Analyzer APK** iş akışı şu kurulabilir dosyayı üretir:

`PCB_Zeka_Pro_v1.0.0_Native_S24.apk`
