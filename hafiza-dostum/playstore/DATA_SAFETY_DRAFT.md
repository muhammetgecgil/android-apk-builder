# Hafıza Dostum 5.0 — Data Safety Taslağı

Bu belge Play Console formunu doldururken geliştiriciye yardımcı olan teknik taslaktır. Nihai beyan, Google Play formundaki güncel sorulara göre geliştirici tarafından doğrulanmalıdır.

## Uygulamanın kendi veri davranışı
- Uygulamanın AndroidManifest'inde INTERNET izni yoktur.
- Reklam SDK'sı, analiz SDK'sı veya geliştirici sunucusuna veri gönderme kodu yoktur.
- Profil, rutin, tamamlanma kayıtları, eşya notları ve güvenilen kişi bilgisi cihaz içinde tutulur.
- 6 aylık arşiv SQLite içinde yerel olarak saklanır.
- JSON yedeği yalnızca kullanıcı manuel olarak dışa aktarır ve hedef konumu kullanıcı seçer.
- Günlük özet veya yedek paylaşımı yalnızca Android paylaşım/dosya seçici arayüzünde kullanıcı eylemiyle gerçekleşir.

## Play Console için başlangıç değerlendirmesi
Uygulama geliştiricisinin kendisi açısından: kullanıcı verisi geliştirici sunucusuna toplanmıyor ve geliştirici tarafından üçüncü taraflarla paylaşılmıyor.

## Sesli soru notu
“Sesle sor” Android'in cihazda seçili konuşma tanıma hizmetini kullanıcı eylemiyle açar. Sesin cihazda veya konuşma tanıma sağlayıcısının altyapısında işlenmesi o sağlayıcının davranışıdır. Hafıza Dostum kendi sunucusuna ses kaydı göndermez ve konuşma sesini kalıcı olarak saklamaz. Play Console beyanı doldurulurken seçili sistem konuşma tanıma hizmetiyle olan bu entegrasyon güncel Google Play tanımlarına göre ayrıca değerlendirilmelidir.

## Gizlilik politikasında açıklanan yerel veriler
- Meslek ve yaşam rolü profili
- Rutinler ve hatırlatma zamanları
- Kullanıcının “yaptım” kayıtları ve zaman damgaları
- Eşya konumuna ilişkin kullanıcı notları
- Güvenilen kişi adı/telefonu
- Son 6 aylık yerel arşiv

## Silme
Ayarlar > Tüm yerel verilerimi sil ile uygulama verileri cihazdan silinebilir. Uygulamanın kaldırılması da Android uygulama verilerini kaldırır.
