# Ürün doğruluk matrisi

| Yetenek | Durum | Gerçek davranış |
| --- | --- | --- |
| Türkçe ses tanıma ve TTS | Çalışan | Android/Samsung konuşma hizmetini kullanır; çevrimdışı dil paketi cihaza bağlıdır. |
| Bağlamsal niyet motoru | Çalışan | Kişi, hedef, mesaj ve saat eksikse soru sorar; eş anlamlı/hatalı telaffuza sınırlı tolerans gösterir. |
| Arama/SMS/WhatsApp | Çalışan pilot | Kişiyi rehberden bulur ve kritik eylem öncesi onay ister. Son gönderim ekranı ilgili uygulamadadır. |
| Yaya rota isteği | Çalışan pilot | Hedefi Google Maps yaya moduna aktarır; uygulama kendi rota motorunu çalıştırmaz. |
| Ekran okuma | Kullanıcı etkinleştirirse | Android Erişilebilirlik hizmeti açıldığında görünür metni toplar ve seslendirir. |
| Düşme olasılığı | Deneysel | İvmeölçerde kısa serbest düşüş+darbe eşiği kullanır; tıbbi veya sertifikalı algılama değildir. |
| Hareket takibi | Çalışan pilot | Kamera karesindeki hareketli bölgeleri, izleri ve görüntü düzlemi hız/ivmesini gösterir. |
| Nesne/para/yazı tanıma | Harici yardımcı | Google Lens veya kamera uygulamasına yönlendirir; uygulama içinde model yoktur. |
| Araç/duvar/kaldırım/çukur sınıfı | Yok | V14 hareket algılar; nesne sınıfını güvenilir biçimde söylemez. |
| Kamera+GPS+IMU sensör füzyonu | Yok | Kaynaklar ayrı özelliklerde kullanılır; tek bir çarpışma/güvenli yön kararı üretilmez. |
| Arka planda sürekli “Hey Rehber” | Yok | Dinleme yalnızca ana ekran öndeyken sürdürülür. |
| İnternet olmadan temel niyet | Kısmi | Niyet motoru yereldir; ses tanımanın çevrimdışı olması cihaz dil paketine bağlıdır. |

“Çalışan” ifadesi kaynakta uygulanmış anlamındadır; S24 Ultra saha testi tamamlandı anlamına gelmez.
