# Structural AI Static — KİLİTLİ GEREKSİNİM TABANI V1.0

Durum: FROZEN / BASELINED
Tarih: 2026-08-25
Kapsam: Otonom statik yapısal analiz APK + FEM çekirdeği + AI model kurma/denetleme katmanı
Değişiklik kuralı: Bu dokümandaki MUST gereksinimleri bundan sonra sessizce silinemez veya zayıflatılamaz. Değişiklik için yeni revizyon, gerekçe ve etkilenen testlerin listesi gerekir.

## 0. Öncelik ve kabul sınıfları

- P0: Solver güvenilirliği / yanlış PASS'i önleme. İlk üretim sürümü için zorunlu.
- P1: Analist iş akışını otonomlaştıran temel özellik. İlk ana sürüm için zorunlu.
- P2: İleri analiz / ölçekleme / sektör genişlemesi.
- AC: Kabul kriteri. Gereksinim ancak karşılık gelen test geçerse karşılanmış sayılır.

Her gereksinim için durum: NOT_STARTED / PARTIAL / IMPLEMENTED / VERIFIED.

---

# 1. ÜRÜN DAVRANIŞI VE OTONOMİ

SR-PROD-001 [P0] Sistem, AI tahmini ile fizik solver sonucunu veri modelinde ayrı tutmalıdır.
AC: Aynı sonuç nesnesinde `source=AI_INFERENCE` ve `source=SOLVER` ayırt edilebilir olmalıdır.

SR-PROD-002 [P0] AI, solver tarafından hesaplanmış sayısal sonucu değiştirememelidir.
AC: Post-processing AI katmanı solver alanlarını immutable olarak tüketmelidir.

SR-PROD-003 [P0] Kritik fizik girdisi belirsizse sistem yeşil PASS üretememelidir.
AC: Unit/material/load/support/contact kritikliği unresolved olduğunda CredibilityGate FAIL/REVIEW vermelidir.

SR-PROD-004 [P1] Ana kullanıcı akışı üç temel adıma indirgenmelidir: Model Seç → Auto Analyze → Sonuç/Rapor.

SR-PROD-005 [P1] Sistem kullanıcıyı zorunlu olarak klasik preprocessor ekranlarında dolaştırmamalıdır.

SR-PROD-006 [P1] Her otomatik mühendislik kararı confidence değeri taşımalıdır.

SR-PROD-007 [P1] Her otomatik mühendislik kararı evidence listesi taşımalıdır.

SR-PROD-008 [P1] Her otomatik mühendislik kararı alternatif adaylarını saklayabilmelidir.

SR-PROD-009 [P1] Kullanıcı AI kararını override edebilmelidir.

SR-PROD-010 [P1] Override işlemi audit trail'e yazılmalıdır.

SR-PROD-011 [P1] Aynı problem için alternatif varsayım branch'leri paralel tutulabilmelidir.

SR-PROD-012 [P1] Sistem problem tipini otomatik sınıflandırmalıdır: linear-static uygun / nonlinear önerilir / buckling gerekli / input yetersiz.

SR-PROD-013 [P0] Input yetersizse sistem analizi durdurabilmelidir; sahte hassasiyet üretmemelidir.

SR-PROD-014 [P1] Lineer problemde yük büyüklüğü bilinmiyorsa birim-yük/influence sonucu üretme seçeneği bulunmalıdır.

SR-PROD-015 [P1] Sistem analiz sonunda “neden bu modeli kurdu?” açıklaması üretmelidir.

---

# 2. DOSYA, CAD VE BİRİM GEREKSİNİMLERİ

SR-CAD-001 [P1] STEP AP203 içe aktarımı desteklenmelidir.
SR-CAD-002 [P1] STEP AP214 içe aktarımı desteklenmelidir.
SR-CAD-003 [P1] STEP AP242 içe aktarımı hedeflenmelidir.
SR-CAD-004 [P1] IGES içe aktarımı desteklenmelidir.
SR-CAD-005 [P1] ASCII STL desteklenmelidir.
SR-CAD-006 [P1] Binary STL desteklenmelidir.
SR-CAD-007 [P1] OBJ desteklenmelidir.
SR-CAD-008 [P2] BREP/native CAD kernel yolu desteklenmelidir.
SR-CAD-009 [P1] Assembly ağacı korunmalıdır.
SR-CAD-010 [P1] Component kimlikleri korunmalıdır.
SR-CAD-011 [P1] CAD coordinate systems mümkünse korunmalıdır.
SR-CAD-012 [P1] CAD material metadata okunmalıdır.
SR-CAD-013 [P1] CAD unit metadata okunmalıdır.
SR-CAD-014 [P2] PMI/annotation metadata okunmalıdır.
SR-CAD-015 [P0] Model birimi belirsizse sessiz varsayım yasaktır.
SR-CAD-016 [P0] mm, m, inch ve cm adayları boyut/metadata ile puanlanmalıdır.
SR-CAD-017 [P0] Kullanılan çözüm birimi raporda açıkça görünmelidir.
SR-CAD-018 [P0] Tüm solver hesapları tek normalize edilmiş iç birim sisteminde yürütülmelidir.
SR-CAD-019 [P1] Orijinal CAD ölçeği ile normalize edilmiş solver ölçeği izlenebilir olmalıdır.
SR-CAD-020 [P1] Çok gövdeli modelde body/component eşleşmesi korunmalıdır.

---

# 3. GEOMETRY HEALING VE TOPOLOJİ

SR-GEO-001 [P0] Duplicate vertex tespit edilmelidir.
SR-GEO-002 [P0] Duplicate face tespit edilmelidir.
SR-GEO-003 [P0] Degenerate triangle/face tespit edilmelidir.
SR-GEO-004 [P0] Zero-area face tespit edilmelidir.
SR-GEO-005 [P0] Zero/negative volume body tespit edilmelidir.
SR-GEO-006 [P0] Non-manifold edge tespit edilmelidir.
SR-GEO-007 [P0] Open shell tespit edilmelidir.
SR-GEO-008 [P0] Watertight kontrolü yapılmalıdır.
SR-GEO-009 [P1] Tiny edge tespit edilmelidir.
SR-GEO-010 [P1] Tiny face tespit edilmelidir.
SR-GEO-011 [P1] Sliver face tespit edilmelidir.
SR-GEO-012 [P1] Self-intersection tespit edilmelidir.
SR-GEO-013 [P1] Inverted normal tespit edilmelidir.
SR-GEO-014 [P1] Disconnected island/body tespit edilmelidir.
SR-GEO-015 [P1] Gap ölçümü yapılmalıdır.
SR-GEO-016 [P1] Heal edilen her değişiklik loglanmalıdır.
SR-GEO-017 [P1] Heal öncesi ve sonrası geometri hash'i saklanmalıdır.
SR-GEO-018 [P1] Healing işlemi geri alınabilir olmalıdır.
SR-GEO-019 [P0] Solid volume mesh öncesi closed-volume şartı doğrulanmalıdır.
SR-GEO-020 [P1] Geometry healing confidence üretilmelidir.

---

# 4. FEATURE RECOGNITION

SR-FEA-001 [P1] Through-hole tanınmalıdır.
SR-FEA-002 [P1] Blind-hole tanınmalıdır.
SR-FEA-003 [P1] Counterbore tanınmalıdır.
SR-FEA-004 [P1] Countersink tanınmalıdır.
SR-FEA-005 [P1] Slot tanınmalıdır.
SR-FEA-006 [P1] Fillet tanınmalıdır.
SR-FEA-007 [P1] Chamfer tanınmalıdır.
SR-FEA-008 [P1] Flange tanınmalıdır.
SR-FEA-009 [P1] Rib/stiffener tanınmalıdır.
SR-FEA-010 [P1] Boss tanınmalıdır.
SR-FEA-011 [P1] Pocket tanınmalıdır.
SR-FEA-012 [P1] Thin-wall bölgesi tanınmalıdır.
SR-FEA-013 [P1] Tube/profile geometrisi tanınmalıdır.
SR-FEA-014 [P1] Bearing seat tanınmalıdır.
SR-FEA-015 [P1] Bolt-circle pattern tanınmalıdır.
SR-FEA-016 [P1] Repeated feature pattern tanınmalıdır.
SR-FEA-017 [P2] Weld seam adayı tanınmalıdır.
SR-FEA-018 [P1] Feature'lar persistent geometry IDs ile solver entity'lerine bağlanmalıdır.
SR-FEA-019 [P1] Feature confidence ayrı tutulmalıdır.
SR-FEA-020 [P1] Kritik feature kaybolursa CAD revision impact olarak işaretlenmelidir.

---

# 5. OTOMATİK İDEALİZASYON

SR-IDL-001 [P1] Her body solid/shell/beam/rigid adayı olarak sınıflandırılmalıdır.
SR-IDL-002 [P1] Local thickness dağılımı hesaplanmalıdır.
SR-IDL-003 [P1] Slenderness metrikleri hesaplanmalıdır.
SR-IDL-004 [P1] Surface-area/volume oranı hesaplanmalıdır.
SR-IDL-005 [P1] Shell midsurface extraction desteklenmelidir.
SR-IDL-006 [P1] Shell thickness field saklanmalıdır.
SR-IDL-007 [P1] Beam centerline extraction desteklenmelidir.
SR-IDL-008 [P1] Beam kesit A, Iy, Iz, J değerleri çıkarılmalıdır.
SR-IDL-009 [P1] Beam local axes tanımlanmalıdır.
SR-IDL-010 [P1] Karma assembly'de farklı body'ler farklı eleman ailesi kullanabilmelidir.
SR-IDL-011 [P1] İdealizasyon confidence üretilmelidir.
SR-IDL-012 [P1] Alternatif idealizasyonla yeniden çözüm desteklenmelidir.
SR-IDL-013 [P1] İdealizasyonun beklenen doğruluk etkisi raporlanmalıdır.
SR-IDL-014 [P0] AI düşük confidence ile sessiz shell/beam dönüşümü yapmamalıdır.
SR-IDL-015 [P1] Rigid-body idealizasyonu fiziksel stiffness oranına dayanmalıdır.

---

# 6. MALZEME MODELİ VE VERİTABANI

SR-MAT-001 [P0] Unknown material geçerli bir durum olmalıdır.
SR-MAT-002 [P0] AI tahmini ile solver'a uygulanan malzeme ayrı alanlarda tutulmalıdır.
SR-MAT-003 [P1] Isotropic linear elasticity desteklenmelidir.
SR-MAT-004 [P1] E saklanmalıdır.
SR-MAT-005 [P1] Poisson oranı saklanmalıdır.
SR-MAT-006 [P1] Density saklanmalıdır.
SR-MAT-007 [P1] Yield strength saklanmalıdır.
SR-MAT-008 [P1] Ultimate strength saklanmalıdır.
SR-MAT-009 [P2] Temperature-dependent E desteklenmelidir.
SR-MAT-010 [P2] Thermal expansion coefficient desteklenmelidir.
SR-MAT-011 [P2] Bilinear plasticity desteklenmelidir.
SR-MAT-012 [P2] Multilinear plasticity desteklenmelidir.
SR-MAT-013 [P2] Orthotropic elasticity desteklenmelidir.
SR-MAT-014 [P1] Malzeme kaynağı/provenance saklanmalıdır.
SR-MAT-015 [P1] Düşük confidence malzemede en az iki adayla sensitivity çözümü desteklenmelidir.
SR-MAT-016 [P1] Material revision numarası result provenance'a yazılmalıdır.
SR-MAT-017 [P0] E<=0 veya fiziksel olmayan ν solver'a kabul edilmemelidir.
SR-MAT-018 [P0] Density<=0 gravity case için kabul edilmemelidir.
SR-MAT-019 [P1] Allowable kaynağı raporlanmalıdır.
SR-MAT-020 [P1] Yield/ultimate allowable ayrımı korunmalıdır.

---

# 7. ASSEMBLY GRAPH VE BAĞLANTI SINIFLANDIRMASI

SR-ASM-001 [P1] Component adjacency graph oluşturulmalıdır.
SR-ASM-002 [P1] Mating surface adayları bulunmalıdır.
SR-ASM-003 [P1] Geometric proximity ölçülmelidir.
SR-ASM-004 [P1] Surface normal uyumu değerlendirilmelidir.
SR-ASM-005 [P1] Fastener-mediated connection adayı tanınmalıdır.
SR-ASM-006 [P1] Pin-mediated connection adayı tanınmalıdır.
SR-ASM-007 [P2] Weld-mediated connection adayı tanınmalıdır.
SR-ASM-008 [P1] Bearing-seat relation tanınmalıdır.
SR-ASM-009 [P1] Connection confidence saklanmalıdır.
SR-ASM-010 [P1] Assembly graph CAD revisionları arasında karşılaştırılabilmelidir.

---

# 8. SINIR ŞARTLARI

SR-BC-001 [P0] Sadece en düşük global yüzeyi fixed yapmak production mode için yasaktır.
SR-BC-002 [P1] Mounting-hole kanıtı support inference'ta kullanılmalıdır.
SR-BC-003 [P1] Flange kanıtı support inference'ta kullanılmalıdır.
SR-BC-004 [P1] Bearing-seat kanıtı cylindrical/bearing support inference'ta kullanılmalıdır.
SR-BC-005 [P1] Ground contact kanıtı support inference'ta kullanılmalıdır.
SR-BC-006 [P1] Fixed support desteklenmelidir.
SR-BC-007 [P1] Prescribed displacement desteklenmelidir.
SR-BC-008 [P1] Symmetry desteklenmelidir.
SR-BC-009 [P1] Cylindrical support desteklenmelidir.
SR-BC-010 [P1] Frictionless support desteklenmelidir.
SR-BC-011 [P1] Elastic/spring support desteklenmelidir.
SR-BC-012 [P1] Remote point/MPC support desteklenmelidir.
SR-BC-013 [P0] Underconstraint detection yapılmalıdır.
SR-BC-014 [P0] Overconstraint detection yapılmalıdır.
SR-BC-015 [P0] Rigid-body translation mode tespit edilmelidir.
SR-BC-016 [P0] Rigid-body rotation mode tespit edilmelidir.
SR-BC-017 [P1] Support inference confidence saklanmalıdır.
SR-BC-018 [P1] Support evidence kullanıcıya gösterilebilir olmalıdır.
SR-BC-019 [P1] Support override persistent geometry ID üzerinden yapılmalıdır.
SR-BC-020 [P0] Çelişkili Dirichlet koşulları solver öncesi reddedilmelidir.

---

# 9. YÜK VE LOAD-CASE MOTORU

SR-LOAD-001 [P0] Geometri tek başına gerçek yük büyüklüğü üretmemelidir.
SR-LOAD-002 [P1] Nodal force desteklenmelidir.
SR-LOAD-003 [P1] Surface traction desteklenmelidir.
SR-LOAD-004 [P1] Pressure desteklenmelidir.
SR-LOAD-005 [P1] Gravity desteklenmelidir.
SR-LOAD-006 [P1] Remote force desteklenmelidir.
SR-LOAD-007 [P1] Moment/torque desteklenmelidir.
SR-LOAD-008 [P1] Bearing load desteklenmelidir.
SR-LOAD-009 [P2] Centrifugal load desteklenmelidir.
SR-LOAD-010 [P2] Thermal load desteklenmelidir.
SR-LOAD-011 [P2] Bolt pretension desteklenmelidir.
SR-LOAD-012 [P1] Her yükün magnitude değeri saklanmalıdır.
SR-LOAD-013 [P1] Her yükün direction değeri saklanmalıdır.
SR-LOAD-014 [P1] Her yükün unit bilgisi saklanmalıdır.
SR-LOAD-015 [P1] Her yükün source/provenance bilgisi saklanmalıdır.
SR-LOAD-016 [P1] Her yükün confidence bilgisi saklanmalıdır.
SR-LOAD-017 [P1] Natural-language load context parse edilebilmelidir.
SR-LOAD-018 [P1] Birim yük çözümü desteklenmelidir.
SR-LOAD-019 [P1] Linear load combination desteklenmelidir.
SR-LOAD-020 [P1] Load envelope hesaplanmalıdır.
SR-LOAD-021 [P1] Worst-case combination araması desteklenmelidir.
SR-LOAD-022 [P1] Load path continuity kontrol edilmelidir.
SR-LOAD-023 [P1] Resultant force hesabı yapılmalıdır.
SR-LOAD-024 [P1] Resultant moment hesabı yapılmalıdır.
SR-LOAD-025 [P0] Kaynağı belirsiz yükle PASS üretilememelidir.

---

# 10. CONTACT, BOLT, PIN VE WELD

SR-CON-001 [P1] Bonded contact desteklenmelidir.
SR-CON-002 [P1] Frictionless contact desteklenmelidir.
SR-CON-003 [P2] Frictional contact desteklenmelidir.
SR-CON-004 [P2] No-separation contact desteklenmelidir.
SR-CON-005 [P1] Initial gap ölçülmelidir.
SR-CON-006 [P1] Initial penetration ölçülmelidir.
SR-CON-007 [P1] Contact pair confidence saklanmalıdır.
SR-CON-008 [P1] Friction coefficient source saklanmalıdır.
SR-CON-009 [P1] μ belirsizse sensitivity bandı desteklenmelidir.
SR-CON-010 [P2] Contact pressure alanı raporlanmalıdır.
SR-CON-011 [P2] Contact status open/closed/stick/slip raporlanmalıdır.
SR-JNT-001 [P1] Bolt geometry tanınmalıdır.
SR-JNT-002 [P1] Bolt çapı çıkarılmalıdır.
SR-JNT-003 [P1] Grip length çıkarılmalıdır.
SR-JNT-004 [P1] Washer/nut ilişkisi çıkarılmalıdır.
SR-JNT-005 [P1] Explicit-solid/connector bolt idealizasyonu seçilebilmelidir.
SR-JNT-006 [P1] Bolt axial force hesaplanmalıdır.
SR-JNT-007 [P1] Bolt shear hesaplanmalıdır.
SR-JNT-008 [P1] Combined utilization hesaplanmalıdır.
SR-JNT-009 [P2] Joint separation kontrolü yapılmalıdır.
SR-JNT-010 [P2] Slip kontrolü yapılmalıdır.
SR-JNT-011 [P1] Pin bearing kontrolü desteklenmelidir.
SR-JNT-012 [P1] Pin shear kontrolü desteklenmelidir.
SR-JNT-013 [P2] Weld throat modellemesi desteklenmelidir.
SR-JNT-014 [P2] Weld group load dağılımı desteklenmelidir.
SR-JNT-015 [P0] Bağlantılar kör bonded contact ile otomatik olarak temsil edilmemelidir.

---

# 11. MESH ÜRETİMİ VE KALİTESİ

SR-MESH-001 [P0] Volume mesh solid geometriyi doldurmalıdır.
SR-MESH-002 [P1] TET4 desteklenmelidir.
SR-MESH-003 [P1] TET10 hedef eleman olmalıdır.
SR-MESH-004 [P2] HEX8/HEX20 desteklenmelidir.
SR-MESH-005 [P2] Shell TRI/QUAD mesh desteklenmelidir.
SR-MESH-006 [P2] Beam mesh desteklenmelidir.
SR-MESH-007 [P1] Global base size üretilebilmelidir.
SR-MESH-008 [P1] Curvature-based local refinement yapılmalıdır.
SR-MESH-009 [P1] Proximity-based local refinement yapılmalıdır.
SR-MESH-010 [P1] Hole refinement yapılmalıdır.
SR-MESH-011 [P1] Fillet refinement yapılmalıdır.
SR-MESH-012 [P1] Contact region refinement yapılmalıdır.
SR-MESH-013 [P1] Support/load introduction refinement yapılmalıdır.
SR-MESH-014 [P0] Negative Jacobian eleman solver'a girememelidir.
SR-MESH-015 [P0] Zero-volume eleman solver'a girememelidir.
SR-MESH-016 [P1] Aspect ratio hesaplanmalıdır.
SR-MESH-017 [P1] Skewness hesaplanmalıdır.
SR-MESH-018 [P1] Minimum angle hesaplanmalıdır.
SR-MESH-019 [P1] Jacobian kalite metriği hesaplanmalıdır.
SR-MESH-020 [P1] Mesh quality histogram üretilebilmelidir.
SR-MESH-021 [P1] Kötü mesh için otomatik remesh döngüsü bulunmalıdır.
SR-MESH-022 [P1] Coarse/medium/fine üç seviye üretilebilmelidir.
SR-MESH-023 [P0] Mesh convergence kritik sonuçlar için hesaplanmalıdır.
SR-MESH-024 [P1] Displacement convergence izlenmelidir.
SR-MESH-025 [P1] Reaction convergence izlenmelidir.
SR-MESH-026 [P1] Strain energy convergence izlenmelidir.
SR-MESH-027 [P1] Non-singular stress convergence izlenmelidir.
SR-MESH-028 [P1] Mesh hash result provenance'a yazılmalıdır.
SR-MESH-029 [P1] Mesh generation deterministik seed ile tekrar üretilebilir olmalıdır.
SR-MESH-030 [P2] Error-estimator tabanlı adaptive refinement desteklenmelidir.

---

# 12. FEM ELEMAN ÇEKİRDEĞİ

SR-FEM-001 [P0] TET4 3 DOF/node eleman rijitliği doğru hesaplanmalıdır.
SR-FEM-002 [P0] B matrisi engineering shear convention ile tutarlı olmalıdır.
SR-FEM-003 [P0] 3B izotropik D matrisi doğrulanmalıdır.
SR-FEM-004 [P0] Eleman Jacobian determinantı kontrol edilmelidir.
SR-FEM-005 [P0] Degenerate tetra reddedilmelidir.
SR-FEM-006 [P0] Eleman rijitliği simetrik olmalıdır.
SR-FEM-007 [P0] Global assembly DOF eşlemesi deterministik olmalıdır.
SR-FEM-008 [P1] Distributed/body load element force vector'a dönüştürülmelidir.
SR-FEM-009 [P1] Element stress recovery desteklenmelidir.
SR-FEM-010 [P1] Element strain recovery desteklenmelidir.
SR-FEM-011 [P1] Von Mises hesaplanmalıdır.
SR-FEM-012 [P1] Principal stress hesaplanmalıdır.
SR-FEM-013 [P1] Tresca hesaplanmalıdır.
SR-FEM-014 [P1] Strain energy hesaplanmalıdır.
SR-FEM-015 [P1] Reaction recovery yapılmalıdır.
SR-FEM-016 [P2] TET10 eklenmelidir.
SR-FEM-017 [P2] Locking riskleri eleman tipine göre işaretlenmelidir.
SR-FEM-018 [P2] Shell/beam eleman çekirdekleri eklenmelidir.
SR-FEM-019 [P0] Solver-grade sonuçlar heuristic screening sonuçlarından ayrı etiketlenmelidir.
SR-FEM-020 [P0] Aynı input/solver version ile sonuç tekrarlanabilir olmalıdır.

---

# 13. SPARSE SOLVER VE NÜMERİK KALİTE

SR-SOL-001 [P0] Büyük modellerde dense global K yasaktır.
SR-SOL-002 [P0] Sparse symmetric storage kullanılmalıdır.
SR-SOL-003 [P0] PCG desteklenmelidir.
SR-SOL-004 [P0] Jacobi preconditioner desteklenmelidir.
SR-SOL-005 [P2] IC/ILU/AMG sınıfı gelişmiş preconditioner hedeflenmelidir.
SR-SOL-006 [P2] Sparse direct LDLT/Cholesky yolu eklenmelidir.
SR-SOL-007 [P0] Normalized residual raporlanmalıdır.
SR-SOL-008 [P0] Iteration count raporlanmalıdır.
SR-SOL-009 [P0] Solver convergence flag saklanmalıdır.
SR-SOL-010 [P0] NaN/Inf sonucu reddedilmelidir.
SR-SOL-011 [P0] Zero/near-zero diagonal tespit edilmelidir.
SR-SOL-012 [P0] Ill-conditioning şüphesi raporlanmalıdır.
SR-SOL-013 [P0] Singular matrix nedeniyle false PASS engellenmelidir.
SR-SOL-014 [P1] Problem özelliklerine göre solver seçimi otomatik yapılabilmelidir.
SR-SOL-015 [P1] Linear solve tolerance konfigüre edilebilir ancak QA minimumundan gevşek olmamalıdır.
SR-SOL-016 [P0] V1 doğrulama hedefi residual <= 1e-8 olmalıdır.
SR-SOL-017 [P1] Sparse matrix memory footprint izlenmelidir.
SR-SOL-018 [P2] Multi-core assembly desteklenmelidir.
SR-SOL-019 [P2] SIMD/native acceleration hedeflenmelidir.
SR-SOL-020 [P2] Çok büyük modeller için out-of-core/remote compute seçeneği desteklenmelidir.

---

# 14. POST-PROCESSING VE SONUÇ ALANLARI

SR-RES-001 [P1] Nodal displacement vector saklanmalıdır.
SR-RES-002 [P1] Total displacement hesaplanmalıdır.
SR-RES-003 [P1] Element strain tensor saklanmalıdır.
SR-RES-004 [P1] Element stress tensor saklanmalıdır.
SR-RES-005 [P1] Principal stresses hesaplanmalıdır.
SR-RES-006 [P1] Von Mises hesaplanmalıdır.
SR-RES-007 [P1] Tresca hesaplanmalıdır.
SR-RES-008 [P1] Reaction force saklanmalıdır.
SR-RES-009 [P1] Reaction moment saklanmalıdır.
SR-RES-010 [P1] Strain energy saklanmalıdır.
SR-RES-011 [P1] Nodal averaged stress ile unaveraged stress ayrılmalıdır.
SR-RES-012 [P1] Deformed shape gösterilebilmelidir.
SR-RES-013 [P1] Undeformed overlay gösterilebilmelidir.
SR-RES-014 [P1] Contour legend units göstermelidir.
SR-RES-015 [P1] Critical region listesi oluşturulmalıdır.
SR-RES-016 [P1] Critical-region navigator bulunmalıdır.
SR-RES-017 [P1] Result provenance kullanıcıya erişilebilir olmalıdır.
SR-RES-018 [P1] Load case envelope görüntülenebilmelidir.
SR-RES-019 [P2] Stress path extraction desteklenmelidir.
SR-RES-020 [P2] Stress linearization desteklenmelidir.

---

# 15. DENGE, YAKINSAMA VE CREDIBILITY GATE

SR-QA-001 [P0] Toplam applied force hesaplanmalıdır.
SR-QA-002 [P0] Toplam reaction force hesaplanmalıdır.
SR-QA-003 [P0] Force equilibrium error hesaplanmalıdır.
SR-QA-004 [P0] Toplam applied moment hesaplanmalıdır.
SR-QA-005 [P0] Toplam reaction moment hesaplanmalıdır.
SR-QA-006 [P0] Moment equilibrium error hesaplanmalıdır.
SR-QA-007 [P0] Solver residual QA gate'e girmelidir.
SR-QA-008 [P0] Mesh convergence QA gate'e girmelidir.
SR-QA-009 [P0] Singularity review QA gate'e girmelidir.
SR-QA-010 [P0] Material confidence QA gate'e girmelidir.
SR-QA-011 [P0] Load confidence QA gate'e girmelidir.
SR-QA-012 [P0] BC confidence QA gate'e girmelidir.
SR-QA-013 [P0] Unit resolution QA gate'e girmelidir.
SR-QA-014 [P0] Contact confidence kritik assembly'de QA gate'e girmelidir.
SR-QA-015 [P0] Her kritik gate PASS olmadan genel SAFE/PASS gösterilemez.
SR-QA-016 [P0] Reaction force equilibrium hedefi <= %0.5 olmalıdır.
SR-QA-017 [P0] Mesh convergence kritik displacement için <= %3 hedeflenmelidir.
SR-QA-018 [P0] Non-singular stress convergence <= %5 hedeflenmelidir.
SR-QA-019 [P1] QA gate hangi maddede kaldığını açıklamalıdır.
SR-QA-020 [P1] QA sonuçları rapora otomatik eklenmelidir.

---

# 16. SINGULARITY VE HOTSPOT AI

SR-SNG-001 [P1] Point-load singularity adayı tespit edilmelidir.
SR-SNG-002 [P1] Point/fixed constraint singularity adayı tespit edilmelidir.
SR-SNG-003 [P1] Re-entrant corner singularity adayı tespit edilmelidir.
SR-SNG-004 [P1] Contact-edge singularity adayı tespit edilmelidir.
SR-SNG-005 [P1] Zero-radius notch singularity adayı tespit edilmelidir.
SR-SNG-006 [P1] Mesh refinement boyunca peak stress trendi takip edilmelidir.
SR-SNG-007 [P0] Diverging singular peak gerçek maksimum stress olarak raporlanmamalıdır.
SR-SNG-008 [P1] Physical hotspot ile numerical singularity ayrı sınıflandırılmalıdır.
SR-SNG-009 [P1] Hotspot bölgesel olarak tanımlanmalıdır; tek node maksimumuna indirgenmemelidir.
SR-SNG-010 [P1] Singularity confidence raporlanmalıdır.

---

# 17. DAYANIM VE ACCEPTANCE

SR-STR-001 [P1] Ductile metal için Von Mises utilization desteklenmelidir.
SR-STR-002 [P1] Tresca alternatif utilization desteklenmelidir.
SR-STR-003 [P1] Brittle malzemede principal-stress tabanlı kontrol desteklenmelidir.
SR-STR-004 [P1] FoS = allowable/demand ayrı hesaplanmalıdır.
SR-STR-005 [P0] Applied load factor ile safety factor karıştırılmamalıdır.
SR-STR-006 [P0] Yield ve ultimate kriterleri ayrı tutulmalıdır.
SR-STR-007 [P1] User-defined allowable desteklenmelidir.
SR-STR-008 [P1] Deformation acceptance criterion desteklenmelidir.
SR-STR-009 [P2] Buckling acceptance criterion desteklenmelidir.
SR-STR-010 [P1] Joint-specific acceptance criterion desteklenmelidir.

---

# 18. NONLINEAR VE İLERİ ANALİZ YOL HARİTASI

SR-NL-001 [P2] Geometric nonlinearity desteklenmelidir.
SR-NL-002 [P2] Large displacement desteklenmelidir.
SR-NL-003 [P2] Plasticity desteklenmelidir.
SR-NL-004 [P2] Contact open/close nonlinearity desteklenmelidir.
SR-NL-005 [P2] Newton-Raphson çözüm döngüsü desteklenmelidir.
SR-NL-006 [P2] Automatic load stepping desteklenmelidir.
SR-NL-007 [P2] Cutback mekanizması desteklenmelidir.
SR-NL-008 [P2] Eigenvalue buckling desteklenmelidir.
SR-NL-009 [P2] Imperfection-aware nonlinear buckling önerilebilmelidir.
SR-NL-010 [P2] Thermal stress desteklenmelidir.
SR-NL-011 [P2] Modal diagnostic analizi desteklenmelidir.
SR-NL-012 [P2] Fatigue modülüne static stress transfer altyapısı sağlanmalıdır.
SR-NL-013 [P2] Composite failure criteria altyapısı hedeflenmelidir.

---

# 19. AI CONFIDENCE, EXPLAINABILITY VE LEARNING

SR-AI-001 [P1] Geometry confidence ayrı hesaplanmalıdır.
SR-AI-002 [P1] Unit confidence ayrı hesaplanmalıdır.
SR-AI-003 [P1] Material confidence ayrı hesaplanmalıdır.
SR-AI-004 [P1] BC confidence ayrı hesaplanmalıdır.
SR-AI-005 [P1] Load confidence ayrı hesaplanmalıdır.
SR-AI-006 [P1] Contact confidence ayrı hesaplanmalıdır.
SR-AI-007 [P1] Mesh confidence ayrı hesaplanmalıdır.
SR-AI-008 [P1] Solver confidence deterministik QA'dan türetilmelidir.
SR-AI-009 [P1] Convergence confidence ayrı tutulmalıdır.
SR-AI-010 [P1] Validation coverage confidence ayrı tutulmalıdır.
SR-AI-011 [P1] Tek birleşik confidence skoru alt skorları gizlememelidir.
SR-AI-012 [P1] “Neden bu yüzeyi support seçtin?” açıklanabilmelidir.
SR-AI-013 [P1] “Neden bu malzemeyi seçtin?” açıklanabilmelidir.
SR-AI-014 [P1] “Neden bu contact tipini seçtin?” açıklanabilmelidir.
SR-AI-015 [P1] Alternatif varsayımların sonuç etkisi gösterilebilmelidir.
SR-AI-016 [P1] Kullanıcı override'ları proje hafızasında öğrenme sinyali olarak saklanabilmelidir.
SR-AI-017 [P1] Öğrenilmiş tercih solver fiziğini bypass edememelidir.
SR-AI-018 [P1] Similar-model fingerprinting desteklenmelidir.
SR-AI-019 [P1] CAD revision impact analysis desteklenmelidir.
SR-AI-020 [P1] AI önerdiği redesign'i yeniden FEM ile doğrulamadan “iyileşti” diyememelidir.

---

# 20. VERIFICATION & VALIDATION

SR-VV-001 [P0] TET4 unit stiffness test bulunmalıdır.
SR-VV-002 [P0] Constitutive matrix unit test bulunmalıdır.
SR-VV-003 [P0] Global assembly unit test bulunmalıdır.
SR-VV-004 [P0] BC application unit test bulunmalıdır.
SR-VV-005 [P0] Reaction recovery unit test bulunmalıdır.
SR-VV-006 [P0] Unit-tetra benchmark bulunmalıdır.
SR-VV-007 [P0] Axial bar benchmark bulunmalıdır.
SR-VV-008 [P0] Cantilever benchmark bulunmalıdır.
SR-VV-009 [P0] Patch test bulunmalıdır.
SR-VV-010 [P1] Plate-with-hole benchmark bulunmalıdır.
SR-VV-011 [P1] Thick-cylinder benchmark bulunmalıdır.
SR-VV-012 [P1] Gravity block benchmark bulunmalıdır.
SR-VV-013 [P2] Contact benchmark bulunmalıdır.
SR-VV-014 [P1] Golden result sistemi bulunmalıdır.
SR-VV-015 [P1] Solver version değişiminde golden regression çalışmalıdır.
SR-VV-016 [P0] V1 displacement error hedefi doğrulanabilir benchmarklarda <= %2 olmalıdır.
SR-VV-017 [P0] Reaction equilibrium error <= %0.5 olmalıdır.
SR-VV-018 [P0] Non-singular stress error hedefi <= %5 olmalıdır.
SR-VV-019 [P1] Verification ile validation raporda ayrı başlık olmalıdır.
SR-VV-020 [P1] Validation kapsamı olmayan problemde sistem bunu açıkça söylemelidir.

---

# 21. PROVENANCE, AUDIT VE REPRODUCIBILITY

SR-PRV-001 [P0] CAD revision/hash saklanmalıdır.
SR-PRV-002 [P0] Mesh hash saklanmalıdır.
SR-PRV-003 [P0] Material revision saklanmalıdır.
SR-PRV-004 [P0] Load-case revision saklanmalıdır.
SR-PRV-005 [P0] Solver version saklanmalıdır.
SR-PRV-006 [P0] AI model/rules version saklanmalıdır.
SR-PRV-007 [P1] User override geçmişi saklanmalıdır.
SR-PRV-008 [P1] Analysis timestamp saklanmalıdır.
SR-PRV-009 [P1] Analysis branch ID saklanmalıdır.
SR-PRV-010 [P0] Aynı provenance ile tekrar koşu karşılaştırılabilmelidir.
SR-PRV-011 [P1] Rev A vs Rev B otomatik sonuç karşılaştırması yapılabilmelidir.
SR-PRV-012 [P1] Değişen feature'ların sonuç etkisi raporlanabilmelidir.
SR-PRV-013 [P1] Audit log silinemez/değiştirilemez kayıt mantığında tutulmalıdır.
SR-PRV-014 [P1] Export edilen rapor provenance özeti içermelidir.
SR-PRV-015 [P1] Solver deck export'u bağımsız cross-check için desteklenmelidir.

---

# 22. ANDROID, OFFLINE, PERFORMANS VE GÜVENLİK

SR-AND-001 [P1] Küçük modeller tamamen offline çözülebilmelidir.
SR-AND-002 [P1] Küçük model çözümü için zorunlu cloud bağlantısı olmamalıdır.
SR-AND-003 [P1] Native/optimized solver yolu desteklenmelidir.
SR-AND-004 [P1] Uzun analiz UI thread'i bloklamamalıdır.
SR-AND-005 [P1] Analiz iptal edilebilmelidir.
SR-AND-006 [P1] Analiz ilerleme aşamaları gösterilmelidir.
SR-AND-007 [P1] RAM yetersizliği önceden tahmin edilmeye çalışılmalıdır.
SR-AND-008 [P0] Out-of-memory durumunda uygulama sessiz yanlış sonuç üretmemelidir.
SR-AND-009 [P2] Orta modellerde multi-core kullanım desteklenmelidir.
SR-AND-010 [P2] Çok büyük modeller için isteğe bağlı remote compute desteklenmelidir.
SR-AND-011 [P0] CAD dosyası varsayılan olarak cihaz dışına gönderilmemelidir.
SR-AND-012 [P1] Remote compute kullanımı açık kullanıcı iznine bağlı olmalıdır.
SR-AND-013 [P1] Proje verileri şifreli saklanabilmelidir.
SR-AND-014 [P1] Autosave/recovery bulunmalıdır.
SR-AND-015 [P1] Analysis result cache bulunmalıdır.

---

# 23. UI, 3B SONUÇ VE RAPOR

SR-UI-001 [P1] Auto Analyze ana aksiyon olmalıdır.
SR-UI-002 [P1] 3B deformed shape gösterilmelidir.
SR-UI-003 [P1] Von Mises contour gösterilmelidir.
SR-UI-004 [P1] Displacement contour gösterilmelidir.
SR-UI-005 [P1] Principal stress contour gösterilmelidir.
SR-UI-006 [P2] Contact pressure contour gösterilmelidir.
SR-UI-007 [P1] Critical region listesi gösterilmelidir.
SR-UI-008 [P1] Confidence dashboard gösterilmelidir.
SR-UI-009 [P1] QA gate sonuçları görünür olmalıdır.
SR-UI-010 [P1] Assumption/evidence paneli bulunmalıdır.
SR-UI-011 [P1] Critical region seçilince kamera otomatik odaklanmalıdır.
SR-UI-012 [P1] “Why critical?” açıklaması bulunmalıdır.
SR-UI-013 [P2] Load-path visualization desteklenmelidir.
SR-UI-014 [P1] 1 sayfalık executive summary üretilebilmelidir.
SR-UI-015 [P1] Tam teknik analyst report üretilebilmelidir.
SR-UI-016 [P1] Raporda mesh özeti bulunmalıdır.
SR-UI-017 [P1] Raporda BC/load özeti bulunmalıdır.
SR-UI-018 [P1] Raporda convergence özeti bulunmalıdır.
SR-UI-019 [P1] Raporda singularity değerlendirmesi bulunmalıdır.
SR-UI-020 [P1] Raporda provenance ve confidence bulunmalıdır.

---

# 24. REDESIGN, SENSITIVITY VE OPTİMİZASYON

SR-OPT-001 [P1] Thickness sensitivity desteklenmelidir.
SR-OPT-002 [P1] Material sensitivity desteklenmelidir.
SR-OPT-003 [P1] Load sensitivity desteklenmelidir.
SR-OPT-004 [P1] Friction sensitivity desteklenmelidir.
SR-OPT-005 [P1] BC sensitivity desteklenmelidir.
SR-OPT-006 [P1] En etkili belirsizlikler sıralanmalıdır.
SR-OPT-007 [P2] Fillet büyütme önerisi üretilebilmelidir.
SR-OPT-008 [P2] Rib ekleme önerisi üretilebilmelidir.
SR-OPT-009 [P2] Thickness artırma/azaltma önerisi üretilebilmelidir.
SR-OPT-010 [P2] Bolt spacing/diameter önerisi üretilebilmelidir.
SR-OPT-011 [P2] Kütle azaltma önerisi üretilebilmelidir.
SR-OPT-012 [P0] Her redesign önerisi yeniden solver ile doğrulanmalıdır.
SR-OPT-013 [P1] Baseline ve redesign sonuçları yan yana karşılaştırılmalıdır.
SR-OPT-014 [P2] Topology optimization yol haritası desteklenmelidir.
SR-OPT-015 [P1] Uncertainty bandı kritik FoS sonucuna yansıtılabilmelidir.

---

# 25. KİLİTLEME / CHANGE CONTROL

SR-CHG-001 [P0] Bu V1.0 baseline'daki P0 gereksinimler release öncesi silinemez.
SR-CHG-002 [P0] P0 gereksinim zayıflatması major requirement revision gerektirir.
SR-CHG-003 [P1] Her gereksinim ileride bir veya daha fazla test ID'sine bağlanmalıdır.
SR-CHG-004 [P1] Her kod modülü karşıladığı requirement ID'lerini kaynak veya metadata seviyesinde referanslamalıdır.
SR-CHG-005 [P1] Requirement değişiklikleri changelog'a yazılmalıdır.
SR-CHG-006 [P1] Requirement durumu NOT_STARTED/PARTIAL/IMPLEMENTED/VERIFIED olarak takip edilmelidir.
SR-CHG-007 [P1] VERIFIED yalnız otomatik veya tanımlı manuel kabul testi geçince verilebilir.
SR-CHG-008 [P1] Gereksinim karşılanmadan UI'da destekleniyor gibi gösterilemez.
SR-CHG-009 [P1] Heuristic screening ile solver-grade FEA aynı feature status altında birleştirilemez.
SR-CHG-010 [P0] Safety/credibility gereksinimlerinde “temporary bypass” production build'de yasaktır.

---

# 26. V1 MİNUMUM RELEASE GATE

V1 ana sürüm için aşağıdaki minimum bloklar VERIFIED olmadan release “analist-grade” olarak adlandırılamaz:

1. SR-CAD-015..019
2. SR-GEO-001..008 ve SR-GEO-019
3. SR-MAT-001..008 ve SR-MAT-017..020
4. SR-BC-013..020
5. SR-LOAD-001 ve SR-LOAD-012..016 ve SR-LOAD-025
6. SR-MESH-001..003, 014..029
7. SR-FEM-001..020 içinde P0/P1 maddeler
8. SR-SOL-001..020 içinde P0/P1 maddeler
9. SR-QA-001..020
10. SR-SNG-001..010
11. SR-VV-001..020 içinde P0/P1 maddeler
12. SR-PRV-001..015
13. SR-CHG-001..010

Bu gate tamamlanmadan uygulama kendisini “nihai mühendislik doğrulaması yapan otomatik yapısal analiz sistemi” olarak etiketlememelidir.

---

# 27. BASELINE ÖZETİ

Bu doküman V1.0 için 300'ün üzerinde atomik, izlenebilir gereksinim ailesi tanımlar. Bundan sonraki geliştirme sırası requirement ID → kod modülü → test ID → VERIFIED zincirini izleyecektir.

İlk kod eşleştirmeleri:
- Tet4Element → SR-FEM-001..011
- LinearElasticMaterial → SR-MAT-003..008, SR-FEM-003
- SparseSymmetricMatrix/PCG → SR-SOL-002..009
- LinearStaticTetSolver → SR-FEM-007..015, SR-QA-001..007
- CredibilityGate → SR-PROD-003, SR-QA-007..020

Bu baseline bundan sonra kod geliştirme sözleşmesidir.
