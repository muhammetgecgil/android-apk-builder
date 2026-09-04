# Structural AI Static — Teorik Altyapı ve Sistem Gereksinimleri

## 0. Amaç
Bu doküman, mobil/Android ön yüzlü otonom bir yapısal statik analiz sisteminin teorik ve yazılımsal gereksinim tabanıdır. Hedef, analistin rutin model kurma, mesh, sınır şartı, yük, temas, çözüm, sonuç kontrolü ve raporlama adımlarının mümkün olan en büyük kısmını otomatikleştirmek; ancak her otomatik varsayımı izlenebilir ve doğrulanabilir tutmaktır.

Temel kalite ilkesi: AI hiçbir mühendislik varsayımını gizli gerçek gibi ele almayacaktır. Her kararın kaynağı, güven puanı, alternatifleri ve sonucu üzerindeki etkisi kaydedilecektir.

---

## 1. Çözüm kapsamı

### 1.1 Birincil analiz ailesi
- 3B lineer elastik statik analiz.
- Küçük şekil değiştirme, küçük dönme başlangıç varsayımı.
- İzotropik lineer elastik malzeme ilk zorunlu modeldir.
- Sonraki aşamalarda ortotropik, plastik, hiperelastik, viskoelastik, sıcaklığa bağlı malzeme modelleri.
- Solid, shell ve beam idealizasyonu.
- Tek parça ve assembly.
- Bonded, frictionless, frictional ve ayrılabilir temas.
- Bolt, pin, weld, bearing ve connector idealizasyonları.

### 1.2 Genişleme ailesi
- Geometrik nonlineerlik.
- Malzeme nonlineerliği.
- Temas nonlineerliği.
- Özdeğer burkulma.
- Modal analiz.
- Termal gerilme.
- Yorulma ve spektral yorgunluk.
- Ön yüklemeli civata bağlantıları.

---

## 2. Temel sürekli ortam mekaniği

### 2.1 Denge denklemleri
Statik denge için hacim içinde:

∇·σ + b = 0

Burada σ Cauchy gerilme tensörü, b hacim kuvvetidir.

Sınır koşulları iki ana sınıftır:
- Dirichlet: u = ū
- Neumann: σ·n = t̄

Sistem aynı serbestlik derecesinde çelişkili sınır koşulu oluşturmamalıdır.

### 2.2 Kinematik
Küçük şekil değiştirme için:

ε = 1/2(∇u + ∇uᵀ)

Normal ve kayma şekil değiştirmeleri doğru tensörel/engineering convention ile tutulmalıdır.

### 2.3 Lineer elastik bağıntı

σ = C:ε

İzotropik malzemede bağımsız sabitler E ve ν'dür. Lamé sabitleri:

λ = Eν / ((1+ν)(1-2ν))
μ = E / (2(1+ν))

C matrisi 3B elastisite için bu sabitlerden kurulacaktır.

### 2.4 Enerji
Toplam potansiyel enerji yaklaşımı:

Π(u) = 1/2 ∫V εᵀDε dV - ∫V uᵀb dV - ∫S uᵀt dS

Denge, δΠ = 0 koşulundan elde edilir.

REQ-PHY-001: Solver, global dengeyi kuvvet ve moment seviyesinde kontrol etmelidir.
REQ-PHY-002: Enerji dengesi ve strain energy pozitifliği uygunsuz modelleri saptamak için kullanılmalıdır.

---

## 3. Sonlu elemanlar yöntemi

### 3.1 Ayrıklaştırma
Eleman içinde:

u(x) = N(x) q
ε = B q

Eleman rijitlik matrisi:

Ke = ∫Ve Bᵀ D B dV

Eleman yük vektörü hacim ve yüzey yüklerinden oluşturulur.

Global sistem:

K u = F

### 3.2 Eleman tipleri
Zorunlu minimum set:
- TET4: lineer tetrahedron.
- TET10: quadratic tetrahedron.
- HEX8/HEX20 sonraki sürüm.
- TRI/QUAD shell.
- 2-node beam/frame.

REQ-FEM-001: İlk gerçek 3B solver TET4 ile çalışabilir, fakat üretim doğruluğu için TET10 zorunlu hedef olmalıdır.
REQ-FEM-002: Volumetric locking, shear locking ve hourglass riskleri eleman tipine göre kontrol edilmelidir.
REQ-FEM-003: Eleman Jacobian determinantı pozitif olmalıdır.

### 3.3 Nümerik integrasyon
Gauss quadrature eleman tipine uygun seçilecektir.

REQ-FEM-004: Yetersiz veya aşırı integrasyon bilinçli ve belgeli olmadıkça otomatik seçilmemelidir.

---

## 4. Lineer cebir ve çözümleyici

K matrisi lineer elastik, uygun kısıtlı sistemlerde simetrik ve çoğunlukla pozitif tanımlıdır.

Çözüm seçenekleri:
- Sparse Cholesky / LDLᵀ direct solver.
- Preconditioned Conjugate Gradient.
- Genel/nonlineer genişleme için GMRES/BiCGSTAB.

REQ-SOL-001: Sparse CSR/CSC veri yapısı kullanılmalıdır.
REQ-SOL-002: Dense global matris büyük modellerde yasaktır.
REQ-SOL-003: Residual norm ||Ku-F|| / ||F|| raporlanmalıdır.
REQ-SOL-004: Condition/ill-conditioning göstergesi üretilmelidir.
REQ-SOL-005: Rigid body mode şüphesi otomatik tespit edilmelidir.

---

## 5. Geometri alma ve temizleme

Destek hedefi:
- STEP
- IGES
- BREP
- STL binary/ASCII
- OBJ
- Parasolid dönüşümü için harici/opsiyonel katman

Geometri kalite işlemleri:
- Duplicate vertex/face temizleme.
- Degenerate face temizleme.
- Non-manifold edge tespiti.
- Open shell ve watertight kontrolü.
- Tiny edge/face tespiti.
- Sliver face tespiti.
- Gap/heal işlemleri.
- Birim tahmini ve kullanıcıya gösterimi.

REQ-GEO-001: Model birimi belirsizse AI tek birim varsayımını sessizce yapamaz.
REQ-GEO-002: mm/m/inch olasılıkları boyut istatistiği ve CAD metadata ile puanlanmalıdır.
REQ-GEO-003: Analiz öncesi watertight/closed volume gereksinimi solid mesh için doğrulanmalıdır.

---

## 6. Otomatik idealizasyon

AI parça geometrisini şu sınıflara ayıracaktır:
- 3B solid-dominant
- thin shell candidate
- slender beam candidate
- axisymmetric candidate
- rigid/very stiff body candidate

Ölçütler:
- principal bounding dimensions
- local thickness distribution
- medial axis yaklaşımı
- curvature
- surface-area/volume oranı
- slenderness
- repeated sections

REQ-IDL-001: AI, solid/shell/beam seçimini güven skoru ile vermelidir.
REQ-IDL-002: İdealizasyonun tahmini hata etkisi raporlanmalıdır.
REQ-IDL-003: Kullanıcı tek dokunuşla alternatif idealizasyonla yeniden çözebilmelidir.

---

## 7. Malzeme çıkarımı

Kaynaklar:
- CAD metadata
- parça adı
- BOM
- PMI
- kullanıcı proje bağlamı
- yoğunluk/kütle bilgisi
- görsel/renk yalnız düşük güvenli yardımcı sinyal

Malzeme veritabanı alanları:
- E
- ν
- ρ
- σy
- σu
- α thermal expansion
- sıcaklığa bağlı tablolar
- S-N eğrisi gelecekte

REQ-MAT-001: Malzeme bilinmiyorsa 'unknown' geçerli bir sonuçtur.
REQ-MAT-002: AI malzeme tahmini ile solver malzemesi ayrı kaydedilmelidir.
REQ-MAT-003: Güven düşükse duyarlılık analizi en az iki aday malzeme ile çalıştırılmalıdır.

---

## 8. Sınır şartı otomasyonu

Otomatik mesnet çıkarımı şu kanıtları kullanabilir:
- mounting holes
- flange yüzeyleri
- ground contact
- bearing seat
- bolt pattern
- assembly mating interfaces
- CAD constraints
- kullanıcı tarafından belirtilen yük yolu bağlamı

Mesnet tipleri:
- fixed
- displacement/remote displacement
- symmetry
- cylindrical support
- frictionless support
- elastic support/spring
- remote point/MPC

REQ-BC-001: Sadece 'en alt yüzeyi fixed' gibi saf geometrik heuristik üretim modu için yeterli değildir.
REQ-BC-002: Mesnet önerisi mutlaka fiziksel bağlantı kanıtı göstermelidir.
REQ-BC-003: Overconstraint ve underconstraint kontrolü yapılmalıdır.
REQ-BC-004: Reaksiyon toplamları uygulanan yüklerle karşılaştırılmalıdır.

---

## 9. Yük otomasyonu

Yük tipleri:
- nodal force
- surface traction
- pressure
- bearing load
- gravity
- centrifugal
- torque/moment
- remote force
- bolt pretension
- thermal load

REQ-LOAD-001: Geometri tek başına yük büyüklüğünü güvenilir biçimde belirleyemez; yük bağlamı yoksa sistem senaryo üretebilir ancak gerçek yük iddiasında bulunamaz.
REQ-LOAD-002: Her yükün kaynak, birim, yön, büyüklük ve güven kaydı bulunmalıdır.
REQ-LOAD-003: Kullanıcı 'ne taşıyor/neye bağlı/ne kadar yük görüyor' gibi doğal dil girdisiyle yük bağlamını verebilmelidir.
REQ-LOAD-004: Load path sürekliliği kontrol edilmelidir.

---

## 10. Temas ve bağlantılar

Temas adayları geometrik yakınlık, yüzey normal yönü, assembly ilişkisi ve bağlantı elemanı geometri bilgisinden çıkarılır.

Temas tipleri:
- bonded/tie
- frictionless
- frictional
- no-separation
- general contact gelecekte

Coulomb sürtünme:
|τ| ≤ μ p

REQ-CON-001: Penetration/gap başlangıç durumu ölçülmelidir.
REQ-CON-002: Friction coefficient tahmini güvenli değilse kullanıcıya gösterilmeli veya duyarlılık bandı çözülmelidir.
REQ-CON-003: Contact status map ve contact pressure raporlanmalıdır.

---

## 11. Civata, pim ve kaynak modelleme

Civata için otomatik sınıflandırma:
- explicit solid bolt
- beam/connector bolt
- bolt pretension section
- bearing/contact load transfer

Kontroller:
- bolt axial force
- shear
- combined utilization
- bearing
- slip
- preload loss
- joint separation

Kaynak için:
- seam/path tanıma
- throat size
- weld group resultant
- local stress extraction

REQ-JNT-001: Bağlantı elemanları yalnız bonded contact ile körlemesine temsil edilmemelidir.
REQ-JNT-002: AI seçtiği connector idealizasyonunu açıklamalıdır.

---

## 12. Mesh teorisi ve otomasyonu

Global mesh size başlangıçta geometri ölçeği ile seçilir ancak nihai seçim fizik tabanlıdır.

Local refinement bölgeleri:
- holes
- fillets
- notches
- contact edges
- load introduction
- supports
- thin ligaments
- high curvature

Mesh kalite ölçütleri:
- aspect ratio
- skewness
- Jacobian
- minimum/maximum angle
- volume ratio
- warpage shell için

REQ-MESH-001: Mesh boyutu sadece tek global sayı olamaz.
REQ-MESH-002: Lokal curvature ve stress gradient adaptasyonu desteklenmelidir.
REQ-MESH-003: En az üç mesh seviyesi ile otomatik yakınsama opsiyonu bulunmalıdır.
REQ-MESH-004: Kritik sonuç için Richardson-benzeri yakınsama veya göreli değişim ölçütü hesaplanmalıdır.

---

## 13. Gerilme ve sonuç büyüklükleri

Zorunlu sonuçlar:
- displacement vector
- total displacement
- strain tensor
- stress tensor
- principal stresses
- Von Mises stress
- Tresca equivalent stress
- reaction forces/moments
- strain energy
- element energy

Von Mises:
σvm = sqrt(0.5[(σ1-σ2)^2 + (σ2-σ3)^2 + (σ3-σ1)^2])

REQ-RES-001: Nodal averaged ve unaveraged element stress ayrı tutulmalıdır.
REQ-RES-002: Peak stress tek başına raporlanamaz; singularity ihtimali değerlendirilmelidir.
REQ-RES-003: Kritik bölgenin yük yolu ve fiziksel nedeni doğal dille açıklanmalıdır.

---

## 14. Dayanım kriterleri

Ductile metal:
- Von Mises yield
- Tresca alternatif

Brittle:
- max principal stress/strain

Composite gelecekte:
- Tsai-Hill
- Tsai-Wu
- Hashin

Safety factor:
FoS = allowable / demand

REQ-STR-001: Allowable kaynağı açıkça belirtilmelidir.
REQ-STR-002: Yield/ultimate/limit/ultimate load kavramları karıştırılmamalıdır.
REQ-STR-003: Safety factor ile applied load factor ayrı kavram olarak tutulmalıdır.

---

## 15. Singularity ve hotspot zekâsı

Singularity adayları:
- point load
- point constraint
- sharp re-entrant corner
- contact edge
- bonded/free transition
- zero-radius notch

REQ-SNG-001: Mesh inceldikçe stress sınırsız artıyorsa sistem 'gerçek maksimum stress' üretmemelidir.
REQ-SNG-002: Kritik karar path/area averaged veya structural/hot-spot yaklaşımıyla alınmalıdır.
REQ-SNG-003: AI 'yüksek stress' ile 'matematiksel singularity' arasında ayrım yapmalıdır.

---

## 16. Yakınsama

Lineer solver convergence ile mesh convergence ayrı kavramlardır.

REQ-CNV-001: Solver residual toleransı raporlanmalıdır.
REQ-CNV-002: Mesh convergence en az displacement, reaction ve seçilmiş stress metric üzerinde kontrol edilmelidir.
REQ-CNV-003: Contact/nonlinear durumda increment ve Newton residual geçmişi kaydedilmelidir.

---

## 17. Nonlineer genişleme teorisi

Genel residual:
R(u) = Fext - Fint(u) = 0

Newton-Raphson:
Kt Δu = R
u(n+1)=u(n)+Δu

REQ-NL-001: Load stepping/adaptive stepping desteklenmelidir.
REQ-NL-002: Tangent stiffness ve residual norm geçmişi raporlanmalıdır.
REQ-NL-003: Divergence nedenleri sınıflandırılmalıdır: contact, material, rigid motion, ill-conditioning vb.

---

## 18. Burkulma

Linear eigenvalue buckling:
(K + λ Kg) φ = 0

REQ-BUCK-001: Eigenvalue buckling gerçek nonlinear collapse load gibi sunulamaz.
REQ-BUCK-002: İnce yapılar için geometrik imperfection ve nonlinear takip analizi önerilmelidir.

---

## 19. Doğrulama ve geçerleme

Üç seviye ayrı tutulacaktır:
1. Code verification: denklemleri yazılım doğru çözüyor mu?
2. Solution verification: bu model yeterince yakınsamış mı?
3. Validation: model gerçek fiziksel sistemi yeterince temsil ediyor mu?

Benchmark seti minimum:
- axial bar
- cantilever beam
- pure bending beam
- 3D patch test
- plate with hole
- thick cylinder
- gravity block
- contact patch

REQ-VV-001: Her solver sürümü regression benchmark setinden geçmelidir.
REQ-VV-002: Analitik çözümü bilinen problemlerle otomatik karşılaştırma yapılmalıdır.
REQ-VV-003: Sonuç raporu model-form belirsizliği ile numerik hatayı ayırmalıdır.

---

## 20. AI karar motoru

AI görevleri:
- geometry understanding
- feature recognition
- part/assembly semantic classification
- idealization selection
- material candidate generation
- boundary condition inference
- load case proposal
- contact/joint inference
- mesh strategy
- solver strategy
- anomaly detection
- result interpretation
- report generation

AI tek başına fizik solverı yerine geçmez. Deterministik/nümerik solver truth engine'dir; AI model kurucu, kontrolcü ve yorumlayıcıdır.

REQ-AI-001: Her AI kararı evidence listesi taşımalıdır.
REQ-AI-002: Her karar confidence [0,1] taşımalıdır.
REQ-AI-003: Düşük confidence kararlar sensitivity branch üretebilmelidir.
REQ-AI-004: AI solver sonucunu değiştiremez; yalnız yeni analiz senaryosu oluşturabilir.
REQ-AI-005: Hallucination guard: mevcut olmayan malzeme, standart veya load case kaynağı uydurulamaz.
REQ-AI-006: 'Unknown' kabul edilen bir mühendislik durumudur.

---

## 21. Otonom analiz döngüsü

1. CAD import
2. Unit inference
3. Geometry health
4. Feature recognition
5. Assembly graph
6. Idealization proposal
7. Material inference
8. Joint/contact inference
9. BC inference
10. Load context inference
11. Load-case generation
12. Mesh generation
13. Mesh QC
14. Solve
15. Residual/equilibrium checks
16. Result extraction
17. Singularity detection
18. Adaptive remesh
19. Mesh convergence
20. Sensitivity analyses
21. Engineering acceptance checks
22. Natural-language explanation
23. Traceable report

REQ-AUTO-001: Bu pipeline tek 'AUTO ANALYZE' komutuyla çalışabilmelidir.
REQ-AUTO-002: Kullanıcı isterse her aşamayı inspect/edit edebilmelidir.
REQ-AUTO-003: Sistem kullanıcı müdahalesi olmadan tamamlayamadığı noktada yalnız gerçekten fiziksel bilgi eksikse soru sormalıdır.

---

## 22. Belirsizlik ve duyarlılık

Belirsizlik kaynakları:
- material
- load
- support stiffness
- friction
- geometry tolerance
- mesh

REQ-UQ-001: Kritik belirsizlikler için one-at-a-time veya DoE tabanlı sensitivity çalıştırılmalıdır.
REQ-UQ-002: Tek deterministik sayı yerine gerektiğinde band/range gösterilmelidir.
REQ-UQ-003: Sonuç güven puanı yalnız AI confidence değildir; geometry, BC, load, material, mesh, convergence ve V&V bileşenlerinden oluşmalıdır.

Örnek birleşik güven bileşenleri:
- Geometry credibility
- Material credibility
- BC credibility
- Load credibility
- Contact credibility
- Mesh credibility
- Solver convergence
- Validation coverage

---

## 23. Birim sistemi

Dahili sistem SI olacaktır:
- m
- kg
- s
- N
- Pa

UI dönüşümleri mm, MPa, kN vb. sunabilir.

REQ-UNIT-001: Birimler quantity type ile birlikte tutulmalıdır.
REQ-UNIT-002: Salt double değerler birimsiz dolaştırılmamalıdır.
REQ-UNIT-003: Unit sanity checker geometri, E, density ve load büyüklüklerini kontrol etmelidir.

---

## 24. Yazılım mimarisi

Katmanlar:
- cad-core
- geometry-healer
- feature-ai
- material-engine
- assembly-graph
- bc-load-ai
- contact-engine
- mesher
- fem-kernel
- sparse-solver
- nonlinear-solver
- postprocessor
- verification-engine
- confidence-engine
- report-engine
- Android UI

REQ-ARCH-001: FEM çekirdeği UI'dan bağımsız test edilebilir native/core modül olmalıdır.
REQ-ARCH-002: Analysis input/output sürümlenebilir JSON/protobuf benzeri şemaya sahip olmalıdır.
REQ-ARCH-003: Solver deck export/import desteklenmelidir.
REQ-ARCH-004: Uzun solve işlemleri foreground service/work manager veya remote compute katmanına taşınabilir olmalıdır.
REQ-ARCH-005: Küçük modeller cihazda offline çözülebilmelidir.

---

## 25. Performans hedefleri

Telefon hedefi için kademeli strateji:
- Tier A: <50k DOF cihaz içi
- Tier B: 50k–500k DOF optimize native solver
- Tier C: >500k DOF remote/desktop/cloud optional compute

REQ-PERF-001: Memory estimate solve öncesi verilmelidir.
REQ-PERF-002: OOM oluşmadan önce mesh otomatik coarsen veya remote option sunulmalıdır.
REQ-PERF-003: Solver ilerlemesi, iteration ve residual canlı gösterilmelidir.

---

## 26. Sonuç ekranı

Zorunlu görseller:
- original model
- mesh
- deformed shape scale control
- Von Mises contour
- principal stress contour
- displacement contour
- reaction vectors
- load/support/contact glyphs
- confidence overlay
- hotspot list

REQ-UI-001: Deformation scale gerçek ölçekten ayrı ve açıkça etiketli olmalıdır.
REQ-UI-002: Legend min/max ve unit göstermelidir.
REQ-UI-003: Kullanıcı model üzerinde bir noktaya dokunarak lokal sonuçları görebilmelidir.

---

## 27. Raporlama

Rapor bölümleri:
- amaç ve kapsam
- geometry/model source
- idealization
- material
- loads
- boundary conditions
- contacts
- mesh statistics
- solver settings
- convergence
- results
- singularity/hotspot assessment
- sensitivity
- limitations
- AI assumptions/evidence/confidence
- V&V status
- conclusion

REQ-RPT-001: Her rapor analysis hash ve software version içermelidir.
REQ-RPT-002: Tek tık PDF ve solver deck export hedeflenmelidir.
REQ-RPT-003: AI tarafından üretilen cümleler sayısal sonuçlarla çapraz doğrulanmalıdır.

---

## 28. Kalite kapıları

Analiz 'PASS' diyebilmek için minimum:
- geometry health pass
- units resolved
- material resolved or bounded sensitivity
- supports physically credible
- loads traceable
- no uncontrolled rigid body mode
- mesh quality pass
- solver residual pass
- force/moment equilibrium pass
- mesh convergence pass veya gerekçeli waiver
- singularity assessment complete

REQ-QA-001: Bu kapılardan biri kritik fail ise uygulama yeşil 'safe' sonucu veremez.

---

## 29. Sürüm yol haritası

### V0.1 — mevcut
- OBJ / ASCII STL import
- geometry bbox/slenderness
- heuristic material/support/load
- beam-like first-pass screening
- confidence

### V0.2 — gerçek lineer 3B FEM çekirdeği
- binary STL
- volumetric tetra mesh
- TET4 Ke
- sparse assembly
- boundary-condition elimination
- solver
- displacement/stress/Von Mises
- equilibrium/residual checks
- benchmark suite

### V0.3 — CAD + akıllı preprocessor
- STEP/IGES
- healing
- feature recognition
- unit inference
- real material database
- BC/load evidence model

### V0.4 — assembly
- contact candidates
- bonded/frictionless/frictional
- bolts/pins/connectors
- assembly graph

### V0.5 — auto convergence
- adaptive local mesh
- convergence study
- singularity classifier
- sensitivity branches

### V1.0 — analyst-grade autonomous static
- one-tap autonomous pipeline
- full traceability
- engineering report
- benchmark/V&V suite
- device + optional remote compute

---

## 30. Kabul kriteri
Ürünün hedefi 'AI bir FEA sonucu gösterdi' değildir. Hedef: deneyimli bir analistin kuracağı modelle karşılaştırıldığında kritik displacement, reaction ve non-singular stress sonuçlarında tanımlı tolerans içinde kalan; hangi varsayımları neden yaptığını açıklayan; belirsizlikleri saklamayan; mesh/solver/V&V kontrollerini otomatik yapan otonom analiz sistemidir.

İlk V1.0 kabul hedefi örnek benchmarklarda:
- displacement error: ≤ %2 analitik/reference
- reaction equilibrium: ≤ %0.5
- non-singular stress metric: ≤ %5
- mesh convergence metric: ≤ %3 successive refinement
- solver normalized residual: ≤ 1e-8 lineer benchmark

Bu toleranslar her problem sınıfı ve element formulation için ayrıca doğrulanacaktır.
