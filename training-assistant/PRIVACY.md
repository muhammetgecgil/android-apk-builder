# Eğitim Asistanı – Gizlilik Politikası

Son güncelleme: 19 Ağustos 2026

Eğitim Asistanı, kullanıcı tarafından açıkça etkinleştirilen AccessibilityService API'sini yalnızca ekrandaki görünür gezinme kontrollerini (ör. İleri, Devam, Sonraki) tespit etmek ve kullanıcının başlattığı deterministik otomasyonu yürütmek için kullanır.

## Erişilebilirlik verileri

Uygulama, erişilebilirlik hizmeti etkin olduğunda aktif penceredeki görünür metin, içerik açıklaması, kontrol türü ve tıklanabilirlik gibi kullanıcı arayüzü bilgilerini cihaz üzerinde anlık olarak işleyebilir. Bu bilgiler sunucuya gönderilmez, reklam veya analiz amacıyla kullanılmaz, satılmaz, üçüncü taraflarla paylaşılmaz ve kalıcı olarak saklanmaz.

## Otomasyon sınırları

Uygulama sistem ayarları ve izin ekranlarında otomatik tıklama yapmaz. Sınav, quiz, değerlendirme, soru/cevap ve onay ekranları algılandığında otomatik ilerleme durdurulur. Uygulama eğitim süresini atlatmak, videoyu izlenmiş gibi göstermek veya sınav/quiz cevaplamak için tasarlanmamıştır.

## Kullanıcı kontrolü

Otomasyon uygulama içinden her zaman durdurulabilir. AccessibilityService izni Android Ayarları üzerinden her zaman kapatılabilir. Kullanıcı açık bir uygulama içi açıklamayı kabul etmeden otomasyon başlatılmaz.

## Veri paylaşımı

Eğitim Asistanı bu işlev kapsamında kişisel veya hassas verileri geliştirici sunucularına toplamaz veya üçüncü taraflarla paylaşmaz.
