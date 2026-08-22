# Türk Radyo Profiles

Bu alan, Türk Radyo uygulamasını depodaki diğer Android projelerinden tamamen bağımsız yönetmek için ayrılmıştır.

## Temel kararlar
- Referans APK: `app-debug-18.apk` içindeki mevcut ana ekran Profil 1'in varsayılan görünümü olarak korunur.
- Uygulama 3 profile ayrılır: Profil 1 Sade, Profil 2 Dengeli, Profil 3 Gelişmiş.
- Profil değişimi ana ekrandan yapılır ve radyo çalmayı kesmez.
- Profil 1 için doğadan ilham alan 50 ileri seviye tema bulunur.
- Tema yöneticisinde her tema için: Aktif, Beğenilen, Pasif durumları tutulur.
- Pasif temalar otomatik dolaşıma girmez; beğenilenler ayrı filtrelenebilir.
- Tema seçimi ve profil seçimi yerel olarak kalıcı saklanır.
- Profil ve tema sistemi ileri/geri istasyon sırasını değiştirmez.

## Profil 1 — Sade
Referans APK'nın mevcut ekran düzeni, koyu siyah/bordo yüzeyleri, kırmızı vurgu rengi ve temel radyo kontrolleri korunur. 50 tema bu düzeni bozmadan arka plan, vurgu, ışık halesi, kart yüzeyi, çizgi ve tipografi kontrastı üzerinde çalışır.

## Profil 2 — Dengeli
Profil 1'in sadeliğini korur; Favoriler, Yakında Çalanlar, Top 20, Benzer Radyolar, temel EQ, bağlantı durumu ve tema erişimini düzenli katmanlar halinde ekler.

## Profil 3 — Gelişmiş
Radyo Radar, bağlantı sağlık puanı, buffer/gecikme/bitrate, istasyon DNA'sı, gelişmiş EQ, geçmiş, teknik modlar ve akıllı özellikleri gruplandırılmış bir uzman arayüzünde sunar.

## Bağımsız yapı
Bu klasör kendi Android proje dosyalarını, kaynaklarını, tema tanımlarını, testlerini ve build workflow'unu taşıyacak. Depodaki kök `app/` projesi veya diğer uygulamalar bundan etkilenmeyecek.
