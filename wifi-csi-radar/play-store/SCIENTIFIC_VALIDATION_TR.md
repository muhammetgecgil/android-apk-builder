# Bilimsel doğrulama planı

## Sensörsüz RF modu
Her senaryo için en az 30 tekrar önerilir:
- boş oda / telefon sabit
- insan yok ama kapı hareketi
- tek kişi yürüyüş
- iki kişi hareket
- fan/perde hareketi
- telefon elde hareketli

Raporlanacak metrikler: false alarm rate, RF değişim skor dağılımı, tarama yaşı, ortak BSSID sayısı ve telefon hareket indeksi.

## ESP32 CSI modu
1, 2, 3 ve 4 düğüm ayrı ayrı test edilir. Sensör kalitesi düşürülen bir düğümle füzyon ağırlığının azaldığı doğrulanır. Düğümler çeliştiğinde güven değerinin düşmesi beklenir.

Sınıflandırma için confusion matrix, balanced accuracy, F1 ve algılama gecikmesi raporlanmalıdır. Model eşikleri odaya göre kalibre edilmeden genel doğruluk iddiası yapılmamalıdır.
