# Hafıza Dostum 5.0 — Play Store Yayın Kontrol Listesi

## Teknik paket
- [x] Üretim applicationId: com.mg.hafizadostum.app
- [x] versionCode 50 / versionName 5.0
- [x] compileSdk 36 / targetSdk 36
- [x] Android App Bundle (AAB) üretimi
- [x] Release R8 küçültme ve resource shrinking
- [x] Adaptive launcher icon
- [x] Android 16 edge-to-edge / system inset uyumu
- [x] INTERNET izni yok
- [x] Cleartext trafik kapalı
- [x] Otomatik Android backup kapalı; kullanıcı kontrollü JSON yedek var
- [x] 6 aylık SQLite arşiv
- [x] Veri silme ekranı
- [x] Uygulama içi gizlilik ve sağlık açıklaması

## Play Console
- [ ] Uygulama oluştur ve paket adını doğrula
- [ ] Play App Signing'i etkinleştir
- [ ] İmzalı AAB'yi Internal testing kanalına yükle
- [ ] Gizlilik politikası için herkese açık, aktif, PDF olmayan URL ekle
- [ ] Data Safety formunu mevcut kod ve SDK davranışına göre doldur
- [ ] Health Apps declaration formunu doldur
- [ ] Content rating anketini tamamla
- [ ] Target audience / çocuklara yönelik olup olmadığını doğru belirt
- [ ] Ads bölümünde reklam kullanılmadığını belirt (reklam eklenmediyse)
- [ ] App access bölümünü tamamla (hesap/giriş yoksa buna göre)
- [ ] Store listing: ad, kısa açıklama, uzun açıklama
- [ ] 512×512 mağaza ikonu
- [ ] Feature graphic 1024×500
- [ ] Telefon ekran görüntüleri
- [ ] İletişim e-postası ve destek bilgisi
- [ ] Internal testing üzerinde gerçek cihaz testleri
- [ ] Pre-launch report sonuçlarını incele ve kritik sorunları kapat
- [ ] Kapalı/açık test ve ardından production rollout

## Yayından önce fonksiyon testleri
- [ ] İlk kurulum profil akışı
- [ ] Profil değiştirme ve özel rutinlerin korunması
- [ ] Yeni rutin / düzenleme / silme
- [ ] Kritik rutinde çift kayıt koruması
- [ ] Bildirim > YAPTIM
- [ ] Bildirim > 10 DK SONRA
- [ ] Telefon yeniden başlatma sonrası reminder kurulumu
- [ ] Takvim arşivinde gün seçme
- [ ] JSON yedek dışa aktar / geri yükle
- [ ] Tüm verileri sil
- [ ] Büyük yazı / sade görünüm
- [ ] TalkBack ile ana akış
- [ ] Android 16 ve en az bir eski desteklenen Android sürümünde test
