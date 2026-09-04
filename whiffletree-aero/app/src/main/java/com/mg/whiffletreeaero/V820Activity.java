package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

/**
 * v8.2 — TEST ENGINEER CAMPAIGN.
 *
 * Purpose: move from a calculator to a structural-test-system workflow:
 * requirement -> load points -> reverse pad loads -> adjustable W/T -> root channels
 * -> BOM/instrumentation -> test profile -> readiness gates -> CMD/FDK verification.
 *
 * EFT PAPER preset intentionally keeps Fx translation and Mx roll as separate load trains,
 * while Fy+Mz and Fz+My are synthesized through pad/strap trees. This mirrors the
 * architecture described in the EFT structural-static-test paper; exact specimen transfer
 * functions are NOT fabricated here and remain a mandatory pre-test verification gate.
 */
public class V820Activity extends V810Activity {
  Button testEngineerEntry,testExit,buildPadsBtn,reverseSolveBtn,profileBtn,readinessBtn,prepareFeedbackBtn,verifyFeedbackBtn;
  LinearLayout testPanel,testPadPanel,gatePanel,feedbackPanel;
  TextView methodCard,reverseCard,treeCard,bomCard,profileCard,readyCard,feedbackCard;
  Spinner testPreset,testConstraint,profileMode;
  EditText trFx,trFy,trFz,trMx,trMy,trMz,trPadCount,zRootCount,yRootCount,rollArm,deadWeight,feedbackLimit,runPercent;
  ArrayList<TestPadRow> testRows=new ArrayList<>();
  ArrayList<CheckBox> manualGates=new ArrayList<>();
  ArrayList<RootChannel> roots=new ArrayList<>();
  ArrayList<FeedbackRow> feedbackRows=new ArrayList<>();
  Solve6 campaignSolve;
  boolean testUiReady=false;
  boolean physicalTreeCompatible=true;
  double campaignFx,campaignFy,campaignFz,campaignMx,campaignMy,campaignMz;

  static class TestPadRow {
    EditText x,y,z;
    Spinner dir;
    TextView solved;
    int axis; double sign,q;
  }
  static class RootChannel {
    String id,family;
    double force,x;
    ArrayList<TestPadRow> pads=new ArrayList<>();
  }
  static class FeedbackRow {
    RootChannel root;
    TextView cmd;
    EditText fdk;
    double command;
  }
  static class TreeNode {
    String id; double x,q;
    TreeNode(String id,double x,double q){this.id=id;this.x=x;this.q=q;}
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    testEngineerEntry=designBtn("TEST ENGINEER • TEST SİSTEMİ KUR / TESTE HAZIRLA",v->showTestEngineer());
    testEngineerEntry.setTextSize(11);
    wizard.addView(testEngineerEntry,Math.min(2,wizard.getChildCount()),new LinearLayout.LayoutParams(-1,dp(64)));

    testPanel=new LinearLayout(this);testPanel.setOrientation(LinearLayout.VERTICAL);testPanel.setPadding(dp(8),dp(8),dp(8),dp(18));testPanel.setBackground(bg(Color.rgb(5,25,39),14));testPanel.setVisibility(View.GONE);
    wizard.addView(testPanel,Math.min(3,wizard.getChildCount()),lp());
    buildTestEngineerPanel();
  }

  void buildTestEngineerPanel(){
    testPanel.removeAllViews();
    methodCard=card(
      "TEST ENGINEER MODE — amaç yalnız W/T hesabı değil, test sistemini kurup test release seviyesine getirmektir.\n\n"+
      "Akış: 1) 6DOF gereksinim  2) uygulanabilir pad/strap noktaları  3) ters yük çözümü  4) ayarlanabilir pivot/slider konumları  5) root kanal/kuvvet/uygulama pozisyonu  6) BOM ve enstrümantasyon  7) dry-run / limit / ultimate profil  8) CMD-FDK feedback  9) post-test inspection.\n\n"+
      "NOT: Global ΣF/ΣM kapanışı tek başına test release değildir. Section shear/moment transfer-function/FEA doğrulaması ayrıca zorunludur.",Color.rgb(8,55,70));
    testPanel.addView(methodCard,lp());

    testPreset=new Spinner(this);
    testPreset.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
      "EFT PAPER MODE — 11 Z + 6 Y pad; Fx ve Mx ayrı load-train",
      "Z + PITCH TREE — Fz + My",
      "Y + YAW TREE — Fy + Mz",
      "CUSTOM 6DOF PAD DIRECTIONS"
    }));
    testConstraint=new Spinner(this);
    testConstraint.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
      "TENSION / COMPRESSION — signed pad load",
      "TENSION ONLY — q >= 0; ters yön için -X/-Y/-Z seç"
    }));
    testPanel.addView(selector("TEST MİMARİSİ",testPreset),lp());
    testPanel.addView(selector("LOAD-TRAIN FİZİĞİ",testConstraint),lp());

    trFx=fresh(wFx.getText().toString());trFy=fresh(wFy.getText().toString());trFz=fresh(wFz.getText().toString());
    trMx=fresh("0");trMy=fresh("0");trMz=fresh("0");
    testPanel.addView(tx("100% DLL / REFERANS 6DOF YÜK — N ve Nmm",12,true,Color.rgb(247,207,77)),lp());
    testPanel.addView(pairFields("Fx",trFx,"Fy",trFy),lp());
    testPanel.addView(pairFields("Fz",trFz,"Mx",trMx),lp());
    testPanel.addView(pairFields("My",trMy,"Mz",trMz),lp());

    trPadCount=fresh("12");zRootCount=fresh("3");yRootCount=fresh("2");rollArm=fresh("0");deadWeight=fresh("0");
    testPanel.addView(pairFields("Custom pad sayısı",trPadCount,"Dead weight [N]",deadWeight),lp());
    testPanel.addView(pairFields("Z W/T root kanal",zRootCount,"Y W/T root kanal",yRootCount),lp());
    testPanel.addView(field("Roll couple effective arm [mm]",rollArm,"EFT PAPER modunda Mx ayrı force-couple load train olarak çözülür. 0 ise roll kanalı RELEASE edilmez."),lp());

    buildPadsBtn=designBtn("PAD / STRAP NOKTALARINI OLUŞTUR",v->generateTestPads());
    testPanel.addView(buildPadsBtn,new LinearLayout.LayoutParams(-1,dp(56)));
    testPadPanel=new LinearLayout(this);testPadPanel.setOrientation(LinearLayout.VERTICAL);testPanel.addView(testPadPanel,lp());

    reverseSolveBtn=designBtn("TERSİNE ÇÖZ • PAD KUVVETLERİ → W/T → ROOT KANALLAR",v->solveTestCampaign());
    reverseSolveBtn.setTextSize(11);testPanel.addView(reverseSolveBtn,new LinearLayout.LayoutParams(-1,dp(66)));
    reverseCard=card("Henüz pad-load çözümü yok.",Color.rgb(12,47,61));testPanel.addView(reverseCard,lp());
    treeCard=card("W/T slider / pivot konumları çözümden sonra burada gösterilecek.",Color.rgb(19,43,55));testPanel.addView(treeCard,lp());
    bomCard=card("TEST RIG BOM — actuator hariç — çözümden sonra oluşturulacak.",Color.rgb(30,45,49));testPanel.addView(bomCard,lp());

    profileMode=new Spinner(this);profileMode.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
      "LIMIT PROFILE — 115% DLL",
      "ULTIMATE PROFILE — 150% DLL"
    }));
    feedbackLimit=fresh("0.5");runPercent=fresh("30");
    testPanel.addView(selector("TEST PROFİLİ",profileMode),lp());
    testPanel.addView(pairFields("CMD-FDK izin [%]",feedbackLimit,"Run seviyesi [%DLL]",runPercent),lp());
    profileBtn=designBtn("TEST PROFİLİNİ OLUŞTUR",v->buildTestProfile());testPanel.addView(profileBtn,new LinearLayout.LayoutParams(-1,dp(54)));
    profileCard=card("Profil henüz oluşturulmadı.",Color.rgb(14,48,57));testPanel.addView(profileCard,lp());

    testPanel.addView(tx("TEST RELEASE GATES",12,true,Color.rgb(247,207,77)),lp());
    gatePanel=new LinearLayout(this);gatePanel.setOrientation(LinearLayout.VERTICAL);testPanel.addView(gatePanel,lp());
    String[] gates={
      "FEA / transfer-function ile section shear + moment dağılımı doğrulandı",
      "Pad/strap local stress ve load-introduction yüzeyi doğrulandı",
      "Test fixture stress / margin ve sınır şartları onaylandı",
      "W/T beam + pin + clevis + spherical bearing + slider/lock boyutlandırması onaylandı",
      "Load-cell kapasite, kalibrasyon ve kanal polaritesi doğrulandı",
      "LCS interlock, emergency stop, stroke/force limitleri doğrulandı",
      "DT / SG enstrümantasyon planı, zero ve kanal eşlemesi tamamlandı",
      "EFT internal pressure / leak-check ve basınç monitoring hazır",
      "30% DLL preliminary / dry-run tamamlandı ve anomali yok",
      "TRR / test authorization tamamlandı",
      "Post-test visual + disassembly/NDT inspection planı hazır"
    };
    for(String g:gates){CheckBox c=new CheckBox(this);c.setText(g);c.setTextColor(Color.WHITE);c.setTextSize(11);c.setPadding(dp(4),dp(4),dp(4),dp(4));manualGates.add(c);gatePanel.addView(c,lp());}
    readinessBtn=designBtn("TEST READY? • RELEASE KONTROLÜ",v->evaluateReadiness());testPanel.addView(readinessBtn,new LinearLayout.LayoutParams(-1,dp(58)));
    readyCard=card("TEST RELEASE henüz değerlendirilmedi.",Color.rgb(50,35,25));testPanel.addView(readyCard,lp());

    testPanel.addView(tx("TEST RUN • CMD / FDK FEEDBACK",12,true,Color.rgb(247,207,77)),lp());
    prepareFeedbackBtn=designBtn("SEÇİLİ %DLL İÇİN KANAL CMD'LERİNİ HAZIRLA",v->prepareFeedback());testPanel.addView(prepareFeedbackBtn,new LinearLayout.LayoutParams(-1,dp(54)));
    feedbackPanel=new LinearLayout(this);feedbackPanel.setOrientation(LinearLayout.VERTICAL);testPanel.addView(feedbackPanel,lp());
    verifyFeedbackBtn=designBtn("FDK GERİBİLDİRİMİNİ DOĞRULA",v->verifyFeedback());testPanel.addView(verifyFeedbackBtn,new LinearLayout.LayoutParams(-1,dp(54)));
    feedbackCard=card("Test feedback henüz girilmedi.",Color.rgb(25,45,59));testPanel.addView(feedbackCard,lp());

    testExit=designBtn("← WHIFFLETREE ANA EKRAN",v->leaveTestEngineer());testPanel.addView(testExit,new LinearLayout.LayoutParams(-1,dp(52)));
    testPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(testUiReady)generateTestPads();}public void onNothingSelected(AdapterView<?> p){}});
    testUiReady=true;generateTestPads();
  }

  void showTestEngineer(){
    testEngineerEntry.setVisibility(View.GONE);body.setVisibility(View.GONE);navRow.setVisibility(View.GONE);auto.setVisibility(View.GONE);advanced.setVisibility(View.GONE);resultBox.setVisibility(View.GONE);dofPanel.setVisibility(View.GONE);
    testPanel.setVisibility(View.VISIBLE);title.setText("WHIFFLETREE AERO • TEST ENGINEER");status.setText("Test requirement → rig → test release → CMD/FDK doğrulama akışı aktif.");
  }
  void leaveTestEngineer(){testPanel.setVisibility(View.GONE);testEngineerEntry.setVisibility(View.VISIBLE);resultBox.setVisibility(View.VISIBLE);showStep(1);}
  @Override public void onBackPressed(){if(testPanel!=null&&testPanel.getVisibility()==View.VISIBLE){leaveTestEngineer();return;}super.onBackPressed();}

  int iv(EditText e,int def,int lo,int hi){try{return Math.max(lo,Math.min(hi,(int)Math.round(Double.parseDouble(e.getText().toString().trim()))));}catch(Exception x){return def;}}
  double dv(EditText e,double def){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception x){return def;}}
  Spinner dirSpinner(String selected){Spinner s=new Spinner(this);String[] d={"+X","-X","+Y","-Y","+Z","-Z"};s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,d));for(int i=0;i<d.length;i++)if(d[i].equals(selected))s.setSelection(i);return s;}

  void generateTestPads(){
    if(testPadPanel==null)return;
    testRows.clear();testPadPanel.removeAllViews();
    int preset=testPreset.getSelectedItemPosition();double L=Math.max(.1,val(hLen))*1000.0;
    testPadPanel.addView(tx("PAD/STRAP     x[mm]      y[mm]      z[mm]      izinli yön       ÇÖZÜLEN q",9,true,Color.rgb(247,207,77)),lp());
    if(preset==0){
      for(int i=0;i<11;i++)addTestPad("Sz"+(i+1),-L/2.0+(i+.5)*L/11.0,0,0,"+Z");
      for(int i=0;i<6;i++)addTestPad("Sy"+(i+1),-L/2.0+(i+.5)*L/6.0,0,0,"+Y");
      methodCard.setText("EFT PAPER MODE\n11 Z-direction + 6 Y-direction load point oluşturuldu. x konumları yalnız test-article uzunluğuna göre başlangıç tahminidir; gerçek uygulanabilir strap/pad noktalarını sen düzenlemelisin. Fx ayrı translational channel, Mx ayrı roll-couple channel olarak tutulur. Fy+Mz ve Fz+My pad ağacında ters çözülür. Section transfer-function/FEA gate zorunludur.");
    }else if(preset==1){for(int i=0;i<11;i++)addTestPad("Pz"+(i+1),-L/2.0+(i+.5)*L/11.0,0,0,"+Z");}
    else if(preset==2){for(int i=0;i<6;i++)addTestPad("Py"+(i+1),-L/2.0+(i+.5)*L/6.0,0,0,"+Y");}
    else{
      int n=iv(trPadCount,12,2,24);double D=Math.max(.05,val(hDia))*1000.0;
      for(int i=0;i<n;i++){int a=i%3;String dir=a==0?"+X":(a==1?"+Y":"+Z");double x=-L/2.0+(i+.5)*L/n;double y=(i%2==0?-1:1)*D*.35;double z=((i/2)%2==0?-1:1)*D*.25;addTestPad("P"+(i+1),x,y,z,dir);}
    }
    roots.clear();campaignSolve=null;reverseCard.setText("Pad/strap koordinatlarını ve yönlerini kontrol et; sonra TERSİNE ÇÖZ.");treeCard.setText("Henüz W/T slider çözümü yok.");bomCard.setText("Henüz BOM yok.");feedbackPanel.removeAllViews();feedbackRows.clear();
  }

  void addTestPad(String id,double x,double y,double z,String dir){
    TestPadRow p=new TestPadRow();p.x=pc(String.format(Locale.US,"%.1f",x));p.y=pc(String.format(Locale.US,"%.1f",y));p.z=pc(String.format(Locale.US,"%.1f",z));p.dir=dirSpinner(dir);p.solved=tx("—",9,true,Color.WHITE);p.solved.setGravity(Gravity.CENTER);p.solved.setTag(id);testRows.add(p);
    LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.addView(tx(id,9,true,Color.WHITE),new LinearLayout.LayoutParams(dp(48),dp(48)));r.addView(p.x,new LinearLayout.LayoutParams(0,dp(48),1));r.addView(p.y,new LinearLayout.LayoutParams(0,dp(48),1));r.addView(p.z,new LinearLayout.LayoutParams(0,dp(48),1));r.addView(p.dir,new LinearLayout.LayoutParams(0,dp(48),1.25f));r.addView(p.solved,new LinearLayout.LayoutParams(0,dp(48),1.15f));testPadPanel.addView(r,lp());
  }

  void decodeDirection(TestPadRow p){int i=p.dir.getSelectedItemPosition();p.axis=i/2;p.sign=(i%2==0)?1:-1;}
  MatrixBuild buildCampaignMatrix(){
    int n=testRows.size();if(n<2)throw new IllegalArgumentException("En az 2 pad gerekli");MatrixBuild m=new MatrixBuild();m.rawA=new double[6][n];m.scaledA=new double[6][n];m.ux=new double[n];m.uy=new double[n];m.uz=new double[n];m.x=new double[n];m.y=new double[n];m.z=new double[n];m.lref=Math.max(100.0,Math.max(.1,val(hLen))*1000.0);
    for(int i=0;i<n;i++){
      TestPadRow p=testRows.get(i);decodeDirection(p);double x=dv(p.x,Double.NaN),y=dv(p.y,Double.NaN),z=dv(p.z,Double.NaN);if(Double.isNaN(x)||Double.isNaN(y)||Double.isNaN(z))throw new IllegalArgumentException("Pad koordinatları sayısal olmalı");double ux=0,uy=0,uz=0;if(p.axis==0)ux=p.sign;else if(p.axis==1)uy=p.sign;else uz=p.sign;m.x[i]=x;m.y[i]=y;m.z[i]=z;m.ux[i]=ux;m.uy[i]=uy;m.uz[i]=uz;m.rawA[0][i]=ux;m.rawA[1][i]=uy;m.rawA[2][i]=uz;m.rawA[3][i]=y*uz-z*uy;m.rawA[4][i]=z*ux-x*uz;m.rawA[5][i]=x*uy-y*ux;for(int r=0;r<6;r++)m.scaledA[r][i]=m.rawA[r][i];m.scaledA[3][i]/=m.lref;m.scaledA[4][i]/=m.lref;m.scaledA[5][i]/=m.lref;
    }return m;
  }

  void solveTestCampaign(){
    try{
      campaignFx=dv(trFx,0);campaignFy=dv(trFy,0);campaignFz=dv(trFz,0);campaignMx=dv(trMx,0);campaignMy=dv(trMy,0);campaignMz=dv(trMz,0);
      MatrixBuild mb=buildCampaignMatrix();double[] target={campaignFx,campaignFy,campaignFz,campaignMx,campaignMy,campaignMz};int preset=testPreset.getSelectedItemPosition();
      if(preset==0){target[0]=0;target[3]=0;}else if(preset==1){target[0]=0;target[1]=0;target[3]=0;target[5]=0;}else if(preset==2){target[0]=0;target[2]=0;target[3]=0;target[4]=0;}
      campaignSolve=solve6(mb,target,testConstraint.getSelectedItemPosition()==1);
      for(int i=0;i<testRows.size();i++){TestPadRow p=testRows.get(i);p.q=campaignSolve.q[i];p.solved.setText(String.format(Locale.US,"%+.2f N",p.q));}
      if(!campaignSolve.feasible){reverseCard.setText(renderCampaignRejected(campaignSolve));roots.clear();treeCard.setText("TREE RELEASE EDİLMEDİ — pad/yön geometrisi hedef yükü kapatmıyor.");bomCard.setText("BOM release edilmedi.");return;}
      apply6DofToCore(campaignSolve);buildRootChannels();renderCampaignSolution();renderRigBom();buildTestProfile();
    }catch(Exception ex){reverseCard.setText("TEST CAMPAIGN ÇÖZÜM HATASI\n"+ex.getMessage());}
  }

  String renderCampaignRejected(Solve6 s){StringBuilder b=new StringBuilder("REVERSE PAD SOLVER — NOT RELEASED\n");b.append(String.format(Locale.US,"rank %d/6 • force error %.5f%% • moment error %.5f%%\n",s.rank,s.forceErrorPct,s.momentErrorPct));b.append("Bu pad koordinat/yön seti istenen yükü fiziksel olarak kapatmıyor. EFT PAPER modunda Fx ve Mx'in ayrı load-train olduğunu unutma. Pad konumu/yönü ekle-değiştir veya tension/compression kullan.\n");for(int i=0;i<6;i++)b.append(String.format(Locale.US,"Δ[%d] = %+.3f\n",i,s.residual[i]));return b.toString();}

  void buildRootChannels(){
    roots.clear();physicalTreeCompatible=true;ArrayList<TestPadRow> xs=new ArrayList<>(),ys=new ArrayList<>(),zs=new ArrayList<>();for(TestPadRow p:testRows){decodeDirection(p);if(Math.abs(p.q)<1e-7)continue;if(p.axis==0)xs.add(p);else if(p.axis==1)ys.add(p);else zs.add(p);}Comparator<TestPadRow> byX=(a,b)->Double.compare(dv(a.x,0),dv(b.x,0));Collections.sort(xs,byX);Collections.sort(ys,byX);Collections.sort(zs,byX);
    createFamilyRoots("X",xs,1);createFamilyRoots("Y",ys,iv(yRootCount,2,1,8));createFamilyRoots("Z",zs,iv(zRootCount,3,1,8));
    int preset=testPreset.getSelectedItemPosition();if(preset==0&&Math.abs(campaignFx)>1e-9){RootChannel r=new RootChannel();r.id="X-SEP";r.family="X SEPARATE";r.force=campaignFx;r.x=0;roots.add(r);}if(preset==0&&Math.abs(campaignMx)>1e-9){double arm=Math.abs(dv(rollArm,0));if(arm>1e-6){double f=campaignMx/arm;RootChannel a=new RootChannel();a.id="ROLL-A";a.family="ROLL COUPLE";a.force=f;a.x=0;RootChannel c=new RootChannel();c.id="ROLL-B";c.family="ROLL COUPLE";c.force=-f;c.x=0;roots.add(a);roots.add(c);}else physicalTreeCompatible=false;}
  }

  void createFamilyRoots(String family,ArrayList<TestPadRow> pads,int k){if(pads.isEmpty())return;k=Math.min(Math.max(1,k),pads.size());int[] cuts=partitionByLoad(pads,k);for(int g=0;g<k;g++){int a=cuts[g],b=cuts[g+1];RootChannel r=new RootChannel();r.id=family+"-R"+(g+1);r.family=family;double sf=0,sfx=0;for(int i=a;i<b;i++){TestPadRow p=pads.get(i);r.pads.add(p);sf+=p.q;sfx+=p.q*dv(p.x,0);}r.force=sf;r.x=Math.abs(sf)>1e-9?sfx/sf:0;roots.add(r);}}

  int[] partitionByLoad(ArrayList<TestPadRow> p,int k){int n=p.size();double[] pref=new double[n+1];for(int i=0;i<n;i++)pref[i+1]=pref[i]+Math.abs(p.get(i).q);double target=Math.max(1e-9,pref[n]/k),INF=1e100;double[][] dp=new double[k+1][n+1];int[][] prev=new int[k+1][n+1];for(double[] row:dp)Arrays.fill(row,INF);dp[0][0]=0;for(int g=1;g<=k;g++)for(int j=g;j<=n;j++)for(int i=g-1;i<j;i++){if(dp[g-1][i]>=INF/2)continue;double s=pref[j]-pref[i],cost=dp[g-1][i]+Math.pow((s-target)/target,2);if(cost<dp[g][j]){dp[g][j]=cost;prev[g][j]=i;}}int[] cut=new int[k+1];cut[k]=n;int j=n;for(int g=k;g>=1;g--){j=prev[g][j];cut[g-1]=j;}return cut;}

  void renderCampaignSolution(){
    StringBuilder out=new StringBuilder("REVERSE PAD LOAD SOLVER — RELEASED TO W/T SYNTHESIS\n");out.append(String.format(Locale.US,"Matrix rank %d/6 • Force error %.6f%% • Moment error %.6f%%\n",campaignSolve.rank,campaignSolve.forceErrorPct,campaignSolve.momentErrorPct));out.append("Pad forces are solved from coordinates + allowed load directions; equal division is NOT assumed.\n\n");for(int i=0;i<testRows.size();i++){TestPadRow p=testRows.get(i);out.append(String.format(Locale.US,"%-5s x=%8.1f  dir=%-2s  q=%+10.2f N\n",String.valueOf(p.solved.getTag()),dv(p.x,0),p.dir.getSelectedItem().toString(),p.q));}
    if(testPreset.getSelectedItemPosition()==0){out.append(String.format(Locale.US,"\nSEPARATE LOAD TRAINS: Fx=%+.2f N",campaignFx));if(Math.abs(campaignMx)>1e-9){double arm=Math.abs(dv(rollArm,0));out.append(arm>0?String.format(Locale.US," • Mx=%+.2f Nmm -> roll couple each |F|=%.2f N at %.1f mm effective arm",campaignMx,Math.abs(campaignMx/arm),arm):" • Mx requested but roll arm=0: ROLL NOT RELEASED");}}
    reverseCard.setText(out.toString());renderTreeSettings();
  }

  void renderTreeSettings(){StringBuilder b=new StringBuilder("ADJUSTABLE WHIFFLETREE / ROOT LOAD LINES\n");b.append("Her passive beam için pivot FL·a = FR·b ile çözülür; slider ortada olmak zorunda değildir.\n\n");int beamTotal=0;for(RootChannel r:roots){b.append(String.format(Locale.US,"%s  F=%+10.2f N  equivalent x=%8.1f mm  pads=%d\n",r.id,r.force,r.x,r.pads.size()));if(r.pads.size()>1){ArrayList<TreeNode> nodes=new ArrayList<>();for(TestPadRow p:r.pads)nodes.add(new TreeNode(String.valueOf(p.solved.getTag()),dv(p.x,0),p.q));int stage=1;while(nodes.size()>1){ArrayList<TreeNode> next=new ArrayList<>();for(int i=0;i<nodes.size();i+=2){if(i+1>=nodes.size()){next.add(nodes.get(i));continue;}TreeNode L=nodes.get(i),R=nodes.get(i+1);double span=Math.abs(R.x-L.x),fl=Math.abs(L.q),fr=Math.abs(R.q);if(L.q*R.q<0){physicalTreeCompatible=false;b.append("  ⚠ "+L.id+" / "+R.id+" opposite-sign child loads: passive same-direction beam uygun değil; separate/tension-compression load path gerekir.\n");}double px=(fl+fr)>1e-9?Math.min(L.x,R.x)+span*fr/(fl+fr):(L.x+R.x)/2;double a=Math.abs(px-L.x),bb=Math.abs(R.x-px);b.append(String.format(Locale.US,"  L%d %s+%s: FL=%+.2f FR=%+.2f  a=%.1f b=%.1f mm  PIVOT x=%.1f mm\n",stage,L.id,R.id,L.q,R.q,a,bb,px));next.add(new TreeNode("B"+(++beamTotal),px,L.q+R.q));}nodes=next;stage++;}}}
    if(!physicalTreeCompatible)b.append("\nPHYSICAL TREE WARNING: bir veya daha fazla load path pasif tek-yön beam ile uyumlu değil veya roll arm tanımsız. Test release gate FAIL.");treeCard.setText(b.toString());}

  void renderRigBom(){int padN=testRows.size(),beamN=0,rootN=roots.size();for(RootChannel r:roots)beamN+=Math.max(0,r.pads.size()-1);StringBuilder b=new StringBuilder("TEST RIG BOM / ENSTRÜMANTASYON — ACTUATOR HARİÇ\n");b.append(String.format(Locale.US,"• load-introduction pad/strap: %d\n• adjustable W/T beam: yaklaşık %d\n• sliding pivot saddle + positive mechanical lock: %d\n• W/T root/load-cell adapter: %d\n• root load cell: %d (minimum, one per controlled channel)\n",padN,beamN,beamN,rootN,rootN));b.append("• spherical bearings / clevis / hardened pins / tension-compression links: CAD ve load-path sizing sonrası finalize\n• DT: kritik deformasyon noktaları — test article/FEA'ya göre\n• SG: kritik bulkhead/spar/skin bölgeleri — FEA + stress planına göre\n• EFT pressure transducer + leak monitoring\n• LCS / DAS channel map, E-stop/interlock, secondary restraint\n\n");b.append("Pad, beam, pin, clevis ve slider yük kapasitesi bu APK tarafından otomatik sertifikalandırılmaz; proof/ultimate sizing ve fixture analysis test release gate'idir.");bomCard.setText(b.toString());}

  void buildTestProfile(){int max=profileMode.getSelectedItemPosition()==0?115:150;StringBuilder b=new StringBuilder(profileMode.getSelectedItemPosition()==0?"LIMIT TEST PROFILE":"ULTIMATE TEST PROFILE");b.append("\nPaper-inspired sequence: 30% preliminary/dry-run, unload, then 10% steps to 100%, 5% steps above 100%. Project procedure may override.\n\n0 → 30 → 0");for(int p=10;p<=100;p+=10)b.append(" → ").append(p);for(int p=105;p<=max;p+=5)b.append(" → ").append(p);b.append(" → 0 %DLL\n\nROOT CHANNEL @ TARGET\n");for(RootChannel r:roots)b.append(String.format(Locale.US,"%s: %+.2f N @ x %.1f mm\n",r.id,r.force,r.x));profileCard.setText(b.toString());}

  void evaluateReadiness(){int checked=0;for(CheckBox c:manualGates)if(c.isChecked())checked++;boolean solvedOk=campaignSolve!=null&&campaignSolve.feasible;boolean allManual=checked==manualGates.size();boolean rootOk=!roots.isEmpty();boolean release=solvedOk&&rootOk&&physicalTreeCompatible&&allManual;StringBuilder b=new StringBuilder(release?"TEST READY — RELEASE CONDITIONS COMPLETE":"NOT TEST READY");b.append(String.format(Locale.US,"\nAuto gates: reverse solve %s • root channels %s • physical W/T compatibility %s\nManual gates: %d/%d complete\n",solvedOk?"PASS":"FAIL",rootOk?"PASS":"FAIL",physicalTreeCompatible?"PASS":"FAIL",checked,manualGates.size()));if(!release)b.append("Eksik gate tamamlanmadan bu APK TEST READY vermez. Özellikle transfer-function/FEA section load verification global equilibriumden bağımsız zorunludur.");readyCard.setText(b.toString());readyCard.setBackground(bg(release?Color.rgb(18,82,54):Color.rgb(85,39,35),12));}

  void prepareFeedback(){feedbackPanel.removeAllViews();feedbackRows.clear();if(roots.isEmpty()){feedbackCard.setText("Önce reverse solver ile root kanalları oluştur.");return;}double pct=dv(runPercent,30);feedbackPanel.addView(tx(String.format(Locale.US,"RUN %.1f%% DLL — CMD / FDK [N]",pct),10,true,Color.rgb(247,207,77)),lp());for(RootChannel r:roots){FeedbackRow f=new FeedbackRow();f.root=r;f.command=r.force*pct/100.0;LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(tx(r.id,9,true,Color.WHITE),new LinearLayout.LayoutParams(dp(72),dp(48)));f.cmd=tx(String.format(Locale.US,"CMD %+.2f",f.command),9,true,Color.WHITE);f.cmd.setGravity(Gravity.CENTER);row.addView(f.cmd,new LinearLayout.LayoutParams(0,dp(48),1));f.fdk=pc(String.format(Locale.US,"%.2f",f.command));row.addView(f.fdk,new LinearLayout.LayoutParams(0,dp(48),1));feedbackRows.add(f);feedbackPanel.addView(row,lp());}feedbackCard.setText("FDK alanlarına load-cell feedback gir; ardından doğrula.");}

  void verifyFeedback(){if(feedbackRows.isEmpty()){feedbackCard.setText("Önce CMD satırlarını hazırla.");return;}double lim=Math.max(0,dv(feedbackLimit,.5));boolean pass=true;StringBuilder b=new StringBuilder(String.format(Locale.US,"CMD / FDK VERIFICATION — limit %.3f%%\n",lim));for(FeedbackRow f:feedbackRows){double fb=dv(f.fdk,Double.NaN);if(Double.isNaN(fb)){pass=false;b.append(f.root.id+": FDK INVALID\n");continue;}double err=Math.abs(f.command)>1e-9?Math.abs((f.command-fb)/f.command)*100.0:Math.abs(fb-f.command);boolean ok=Math.abs(f.command)>1e-9?err<=lim:Math.abs(fb)<1e-3;if(!ok)pass=false;b.append(String.format(Locale.US,"%s CMD=%+.2f FDK=%+.2f  error=%s %.4f %s\n",f.root.id,f.command,fb,Math.abs(f.command)>1e-9?"":"abs",err,ok?"PASS":"FAIL"));}b.append(pass?"\nALL ACTIVE CHANNELS WITHIN FEEDBACK LIMIT":"\nFEEDBACK OUT OF LIMIT — HOLD / investigate before load increase");feedbackCard.setText(b.toString());feedbackCard.setBackground(bg(pass?Color.rgb(18,82,54):Color.rgb(85,39,35),12));}
}
