# M80 Verification Matrix

Milestone M80 kabulü için her kritik hesap, bağımsız referans problem ve sayısal tolerans ile izlenir. Bir test başarısızsa M80 kapatılamaz.

| ID | Alan | Motor / Modül | Referans kriter | Kabul |
|---|---|---|---|---|
| VM-001 | Civata | CalculationEngine / bolt | A=pi*d^2/4, sigma=F/A | <=0.1% |
| VM-002 | Mil | CalculationEngine / shaft | sigma_b=32M/(pi*d^3) | <=0.1% |
| VM-003 | Rulman | CalculationEngine / bearing | L10=(C/P)^p | <=0.1% |
| VM-004 | Dişli | CalculationEngine / Lewis | sigma=Ft/(b*m*Y) | <=0.1% |
| VM-005 | Yay | CalculationEngine / spring | F=k*delta tutarlılığı | <=0.3% |
| VM-006 | Kama | CalculationEngine / key | kesme ve ezilme kapalı form | <=0.2% |
| VM-007 | Kaynak | CalculationEngine / weld | tau=F/(0.707*a*L) | <=0.2% |
| VM-008 | Yorulma | CalculationEngine / Goodman | 1/n=sa/Se+sm/Sut | <=0.2% |
| VM-009 | Pim | CalculationEngine / pin | çift kesmede tau yarıya iner | <=0.2% |
| VM-010 | Kolon | CalculationEngine / Euler | Pcr=pi^2EI/(KL)^2 | <=0.2% |
| VM-011 | Kiriş | CalculationEngine / beam | Mmax=FL/4 | <=0.1% |
| VM-012 | Burulma | CalculationEngine / torsion | theta=TL/(JG) | <=0.3% |
| VM-013 | Güç vidası | CalculationEngine / power screw | yükseltme torku pozitif ve self-lock mantığı | davranış |
| VM-014 | Diş sıyırma | CalculationEngine / thread strip | tau=F/Athread | <=0.3% |
| VM-015 | Civata ön yük | CalculationEngine / preload | yük paylaşımı ve kalan sıkma | <=0.3% |
| VM-016 | Kayış | CalculationEngine / belt | T1>T2 ve güç aktarım dengesi | davranış |
| VM-017 | Zincir | CalculationEngine / chain | Ft=P/v, servis katsayısı uygulanır | <=0.3% |
| VM-018 | Kaplin civatası | CalculationEngine / coupling bolts | torktan çevresel kuvvet | <=0.3% |
| VM-019 | Rulman eşdeğer yük | CalculationEngine / bearing equivalent | P=XFr+YFa | <=0.1% |
| VM-020 | Fren | CalculationEngine / brake | T=mu*F*rm*z | <=0.1% |
| VM-021 | Drivetrain | DrivetrainEngine | Ft=2T/d ve statik reaksiyon dengesi | <=0.5% |
| VM-022 | Assembly shaft | AssemblyCalculationEngine | RA+RB=sum(F) | <=0.5% |
| VM-023 | Bolt group | AssemblyCalculationEngine | simetrik direct+torsion dağılımı | <=0.5% |
| VM-024 | Gearbox | GearboxDesignEngine | i=z2/z1, Tout=Tin*i*eta | <=0.5% |
| VM-025 | M80 E2E | M80Acceptance | 7.5 kW, 1450->100 rpm zinciri | tüm kapılar geçmeli |

## Gate kuralları

1. VM-001..VM-024 otomatik CI testlerinde yeşil olmalı.
2. VM-025, güç/devirden ürün adayına kadar uçtan uca veri aktarımını doğrulamalı.
3. Standart tabanlı sonuçlarda kullanılan standart ve revizyon raporda saklanmalı.
4. Katalog seçimi hesaplanan minimum kapasite/ölçünün altına inemez.
5. Canlı fiyat/stok bilinmiyorsa `bilinmiyor / doğrulama gerekli` olarak gösterilir; tahmin edilmez.
