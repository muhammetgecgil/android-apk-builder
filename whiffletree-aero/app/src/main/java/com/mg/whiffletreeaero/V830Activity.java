package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.*;

/**
 * v8.3 — Requirement zone loads -> editable physical pad loads -> AUTO whiffletree.
 *
 * Engineering intent:
 *  - Zone loads are the TEST REQUIREMENT reference.
 *  - Pad loads are the PHYSICAL APPLIED loads and remain fully editable.
 *  - AUTO uses the edited pad loads, never silently forces equal distribution.
 *  - Zone-vs-pad force/moment residual is shown before release.
 *  - Final report is ordered from pad layer upward: L1, L2, ... root, actuator force.
 */
public class V830Activity extends V820Activity {
  Button chainEntry,chainExit,buildZoneBtn,zoneToPadBtn,autoTreeBtn;
  LinearLayout chainPanel,chainZonePanel,chainPadPanel;
  TextView chainIntro,chainStatus,chainResult;
  EditText chainZoneCount,chainPadCount;
  ArrayList<ZoneLoadRow> chainZones=new ArrayList<>();
  ArrayList<PadLoadRow> chainPads=new ArrayList<>();

  static class ZoneLoadRow {
    EditText x,fx,fy,fz;
  }
  static class PadLoadRow {
    EditText x,y,z,fx,fy,fz;
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);

    chainEntry=designBtn("BÖLGE → PAD → AUTO TREE • TEST YÜK AKIŞI",v->showChainWorkflow());
    chainEntry.setTextSize(11);
    wizard.addView(chainEntry,Math.min(2,wizard.getChildCount()),new LinearLayout.LayoutParams(-1,dp(64)));

    chainPanel=new LinearLayout(this);
    chainPanel.setOrientation(LinearLayout.VERTICAL);
    chainPanel.setPadding(dp(8),dp(8),dp(8),dp(18));
    chainPanel.setBackground(bg(Color.rgb(5,25,39),14));
    chainPanel.setVisibility(View.GONE);
    wizard.addView(chainPanel,Math.min(3,wizard.getChildCount()),lp());
    buildChainPanel();
  }

  void buildChainPanel(){
    chainPanel.removeAllViews();
    chainIntro=card(
      "TEST LOAD CHAIN\n\n"+
      "1) BÖLGE YÜKLERİ = test/FEA gereksinimi.\n"+
      "2) PAD KUVVETLERİ = specimen üzerine fiziksel uygulanacak yükler; x/y/z ve Fx/Fy/Fz elle değiştirilebilir.\n"+
      "3) AUTO TASARLA = düzenlediğin pad kuvvetlerini kullanır; 50/50 varsayımı yapmaz.\n"+
      "4) SONUÇ = Layer-1 → Layer-2 → ... → root → gerekli actuator kuvveti ve uygulama x pozisyonu.\n\n"+
      "Bölge ile pad aynı şeyi üretmiyorsa ΣF/ΣM farkı ayrıca gösterilir; program bu farkı gizlemez.",
      Color.rgb(8,55,70));
    chainPanel.addView(chainIntro,lp());

    chainZoneCount=fresh(hSections==null?"8":hSections.getText().toString());
    chainPadCount=fresh(hSections==null?"8":hSections.getText().toString());
    chainPanel.addView(pairChain("Bölge sayısı",chainZoneCount,"Pad sayısı",chainPadCount),lp());

    buildZoneBtn=designBtn("1 • BÖLGE YÜK TABLOSUNU OLUŞTUR / YENİLE",v->buildChainZones());
    chainPanel.addView(buildZoneBtn,new LinearLayout.LayoutParams(-1,dp(56)));
    chainZonePanel=new LinearLayout(this);chainZonePanel.setOrientation(LinearLayout.VERTICAL);chainPanel.addView(chainZonePanel,lp());

    zoneToPadBtn=designBtn("2 • BÖLGE YÜKLERİNİ PAD TABLOSUNA AKTAR",v->buildPadsFromZones());
    zoneToPadBtn.setTextSize(11);
    chainPanel.addView(zoneToPadBtn,new LinearLayout.LayoutParams(-1,dp(60)));
    chainPadPanel=new LinearLayout(this);chainPadPanel.setOrientation(LinearLayout.VERTICAL);chainPanel.addView(chainPadPanel,lp());

    autoTreeBtn=designBtn("3 • PAD KUVVETLERİNDEN AUTO WHIFFLETREE TASARLA",v->autoDesignFromEditedPads());
    autoTreeBtn.setTextSize(11);
    chainPanel.addView(autoTreeBtn,new LinearLayout.LayoutParams(-1,dp(66)));

    chainStatus=card("Önce bölge yüklerini düzenle. Sonra PAD tablosuna aktar ve pad kuvvet/pozisyonlarını son kez düzenle.",Color.rgb(12,47,61));
    chainPanel.addView(chainStatus,lp());
    chainResult=card("AUTO sonucu henüz yok.",Color.rgb(16,45,56));
    chainPanel.addView(chainResult,lp());

    chainExit=designBtn("← WHIFFLETREE ANA EKRAN",v->leaveChainWorkflow());
    chainPanel.addView(chainExit,new LinearLayout.LayoutParams(-1,dp(52)));

    buildChainZones();
  }

  LinearLayout pairChain(String la,EditText a,String lb,EditText b){
    LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);
    r.addView(field(la,a,""),new LinearLayout.LayoutParams(0,-2,1));
    r.addView(field(lb,b,""),new LinearLayout.LayoutParams(0,-2,1));
    return r;
  }

  void showChainWorkflow(){
    chainEntry.setVisibility(View.GONE);
    if(testEngineerEntry!=null)testEngineerEntry.setVisibility(View.GONE);
    if(testPanel!=null)testPanel.setVisibility(View.GONE);
    body.setVisibility(View.GONE);navRow.setVisibility(View.GONE);auto.setVisibility(View.GONE);advanced.setVisibility(View.GONE);resultBox.setVisibility(View.GONE);
    if(dofPanel!=null)dofPanel.setVisibility(View.GONE);
    chainPanel.setVisibility(View.VISIBLE);
    title.setText("WHIFFLETREE AERO • BÖLGE → PAD → AUTO TREE");
    status.setText("Bölge gereksinimini tanımla, fiziksel pad yüklerini düzenle, sonra AUTO ağacı oluştur.");
  }

  void leaveChainWorkflow(){
    chainPanel.setVisibility(View.GONE);
    chainEntry.setVisibility(View.VISIBLE);
    if(testEngineerEntry!=null)testEngineerEntry.setVisibility(View.VISIBLE);
    resultBox.setVisibility(View.VISIBLE);
    showStep(1);
  }

  @Override public void onBackPressed(){
    if(chainPanel!=null&&chainPanel.getVisibility()==View.VISIBLE){leaveChainWorkflow();return;}
    super.onBackPressed();
  }

  int chainInt(EditText e,int def,int lo,int hi){
    try{return Math.max(lo,Math.min(hi,(int)Math.round(Double.parseDouble(e.getText().toString().trim()))));}
    catch(Exception x){return def;}
  }
  double chainD(EditText e,double def){
    try{return Double.parseDouble(e.getText().toString().trim());}
    catch(Exception x){return def;}
  }
  EditText chainCell(String s){
    EditText e=small(s);e.setTextSize(10);return e;
  }

  void buildChainZones(){
    int n=chainInt(chainZoneCount,8,1,24);
    double L=Math.max(.001,val(hLen))*1000.0;
    double fx=val(hFx),fy=val(hFy),fz=val(hFz);
    chainZones.clear();chainZonePanel.removeAllViews();
    chainZonePanel.addView(tx("BÖLGE      x[mm]       Fx[N]       Fy[N]       Fz[N]",9,true,Color.rgb(247,207,77)),lp());
    for(int i=0;i<n;i++){
      ZoneLoadRow z=new ZoneLoadRow();
      double x=-L/2.0+(i+.5)*L/n;
      z.x=chainCell(String.format(Locale.US,"%.1f",x));
      z.fx=chainCell(String.format(Locale.US,"%.2f",fx/n));
      z.fy=chainCell(String.format(Locale.US,"%.2f",fy/n));
      z.fz=chainCell(String.format(Locale.US,"%.2f",fz/n));
      chainZones.add(z);
      LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);
      r.addView(tx("S"+(i+1),9,true,Color.WHITE),new LinearLayout.LayoutParams(dp(42),dp(46)));
      r.addView(z.x,new LinearLayout.LayoutParams(0,dp(46),1));
      r.addView(z.fx,new LinearLayout.LayoutParams(0,dp(46),1));
      r.addView(z.fy,new LinearLayout.LayoutParams(0,dp(46),1));
      r.addView(z.fz,new LinearLayout.LayoutParams(0,dp(46),1));
      chainZonePanel.addView(r,lp());
    }
    chainPadPanel.removeAllViews();chainPads.clear();
    chainResult.setText("AUTO sonucu henüz yok.");
    chainStatus.setText("BÖLGE YÜKLERİ hazır. S noktalarının x/Fx/Fy/Fz değerlerini gerçek test/FEA gereksinimine göre düzenle; sonra PAD tablosuna aktar.");
  }

  boolean validZones(){
    if(chainZones.isEmpty()){chainStatus.setText("Bölge tablosu boş. Önce BÖLGE YÜK TABLOSUNU oluştur.");return false;}
    for(int i=0;i<chainZones.size();i++){
      ZoneLoadRow z=chainZones.get(i);
      if(!finite(chainD(z.x,Double.NaN))||!finite(chainD(z.fx,Double.NaN))||!finite(chainD(z.fy,Double.NaN))||!finite(chainD(z.fz,Double.NaN))){
        chainStatus.setText("BÖLGE GİRİŞ HATASI — S"+(i+1)+" x/Fx/Fy/Fz alanlarını sayısal doldur.");return false;
      }
    }
    return true;
  }

  boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}

  void buildPadsFromZones(){
    if(!validZones())return;
    int m=chainInt(chainPadCount,chainZones.size(),1,24);
    chainPads.clear();chainPadPanel.removeAllViews();
    chainPadPanel.addView(tx("PAD        x       y       z        Fx        Fy        Fz",9,true,Color.rgb(247,207,77)),lp());
    double L=Math.max(.001,val(hLen))*1000.0;
    boolean oneToOne=m==chainZones.size();
    for(int i=0;i<m;i++){
      PadLoadRow p=new PadLoadRow();
      double x=-L/2.0+(i+.5)*L/m,fx=0,fy=0,fz=0;
      if(oneToOne){
        ZoneLoadRow z=chainZones.get(i);x=chainD(z.x,x);fx=chainD(z.fx,0);fy=chainD(z.fy,0);fz=chainD(z.fz,0);
      }
      p.x=chainCell(String.format(Locale.US,"%.1f",x));p.y=chainCell("0");p.z=chainCell("0");
      p.fx=chainCell(String.format(Locale.US,"%.2f",fx));p.fy=chainCell(String.format(Locale.US,"%.2f",fy));p.fz=chainCell(String.format(Locale.US,"%.2f",fz));
      chainPads.add(p);
      LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);
      r.addView(tx("P"+(i+1),9,true,Color.WHITE),new LinearLayout.LayoutParams(dp(36),dp(46)));
      r.addView(p.x,new LinearLayout.LayoutParams(0,dp(46),1));r.addView(p.y,new LinearLayout.LayoutParams(0,dp(46),.85f));r.addView(p.z,new LinearLayout.LayoutParams(0,dp(46),.85f));
      r.addView(p.fx,new LinearLayout.LayoutParams(0,dp(46),1.05f));r.addView(p.fy,new LinearLayout.LayoutParams(0,dp(46),1.05f));r.addView(p.fz,new LinearLayout.LayoutParams(0,dp(46),1.05f));
      chainPadPanel.addView(r,lp());
    }
    if(oneToOne){
      chainStatus.setText("PAD tablosu bölge yüklerinden 1:1 başlangıç olarak dolduruldu. Şimdi fiziksel pad x/y/z ve Fx/Fy/Fz değerlerini istediğin gibi düzenle. AUTO, bu son PAD tablosunu kullanacak.");
    }else{
      chainStatus.setText("Pad sayısı bölge sayısından farklı olduğu için program kuvvet dağılımı UYDURMADI. Pad pozisyonları oluşturuldu, kuvvetler 0 bırakıldı. Fiziksel pad Fx/Fy/Fz değerlerini elle gir; AUTO yalnız girdiğin pad yüklerini kullanacak.");
    }
    chainResult.setText("PAD kuvvetleri düzenlenebilir. Hazır olduğunda AUTO WHIFFLETREE TASARLA'ya bas.");
  }

  boolean validPads(){
    if(chainPads.isEmpty()){chainStatus.setText("PAD tablosu yok. Önce BÖLGE → PAD aktarımını yap.");return false;}
    for(int i=0;i<chainPads.size();i++){
      PadLoadRow p=chainPads.get(i);
      double[] v={chainD(p.x,Double.NaN),chainD(p.y,Double.NaN),chainD(p.z,Double.NaN),chainD(p.fx,Double.NaN),chainD(p.fy,Double.NaN),chainD(p.fz,Double.NaN)};
      for(double q:v)if(!finite(q)){chainStatus.setText("PAD GİRİŞ HATASI — P"+(i+1)+" x/y/z/Fx/Fy/Fz alanlarını sayısal doldur.");return false;}
    }
    return true;
  }

  void autoDesignFromEditedPads(){
    if(!validZones()||!validPads())return;

    double[] zF=new double[3],zM=new double[3],pF=new double[3],pM=new double[3];
    for(ZoneLoadRow z:chainZones){
      double x=chainD(z.x,0),fx=chainD(z.fx,0),fy=chainD(z.fy,0),fz=chainD(z.fz,0);
      zF[0]+=fx;zF[1]+=fy;zF[2]+=fz;
      // Zone table is a 1-D station requirement: r=[x,0,0].
      zM[1]+=-x*fz;zM[2]+=x*fy;
    }

    double fac=testFactor(),stiffness=Math.max(1,qd(qStiffness)),gauge=Math.max(1,qd(qGaugeLength));
    ArrayList<SNode> nodes=new ArrayList<>();
    for(int i=0;i<chainPads.size();i++){
      PadLoadRow p=chainPads.get(i);
      double x=chainD(p.x,0),y=chainD(p.y,0),z=chainD(p.z,0),fx=chainD(p.fx,0),fy=chainD(p.fy,0),fz=chainD(p.fz,0);
      pF[0]+=fx;pF[1]+=fy;pF[2]+=fz;
      pM[0]+=y*fz-z*fy;pM[1]+=z*fx-x*fz;pM[2]+=x*fy-y*fx;
      SNode s=new SNode();s.section=i;s.x=x;s.y=y;s.z=z;s.fx=fx*fac;s.fy=fy*fac;s.fz=fz*fac;
      s.r=Math.sqrt(s.fx*s.fx+s.fy*s.fy+s.fz*s.fz);s.mx=s.y*s.fz-s.z*s.fy;s.my=s.z*s.fx-s.x*s.fz;s.mz=s.x*s.fy-s.y*s.fx;
      s.disp=s.r/stiffness;s.strain=s.disp/gauge*1e6;s.lc=s.r;nodes.add(s);
    }

    solved.clear();solved.addAll(nodes);solvedValid=true;customZones=false;forcedActuatorGroups=null;
    hSections.setText(String.valueOf(nodes.size()));
    hFx.setText(String.format(Locale.US,"%.6f",pF[0]));hFy.setText(String.format(Locale.US,"%.6f",pF[1]));hFz.setText(String.format(Locale.US,"%.6f",pF[2]));

    buildSelectableCandidates();
    if(recommended!=null)applyDesign(recommended);else designAutomaticWhiffletree();

    renderChainResult(zF,zM,pF,pM);
  }

  void renderChainResult(double[] zF,double[] zM,double[] pF,double[] pM){
    double dFx=pF[0]-zF[0],dFy=pF[1]-zF[1],dFz=pF[2]-zF[2];
    double dMx=pM[0]-zM[0],dMy=pM[1]-zM[1],dMz=pM[2]-zM[2];
    double fErr=Math.sqrt(dFx*dFx+dFy*dFy+dFz*dFz),mErr=Math.sqrt(dMx*dMx+dMy*dMy+dMz*dMz);
    double fRef=Math.max(1,Math.sqrt(zF[0]*zF[0]+zF[1]*zF[1]+zF[2]*zF[2]));
    double mRef=Math.max(1,Math.sqrt(zM[0]*zM[0]+zM[1]*zM[1]+zM[2]*zM[2]));
    double fPct=100*fErr/fRef,mPct=100*mErr/mRef;

    StringBuilder out=new StringBuilder();
    out.append("AUTO WHIFFLETREE — PAD KUVVETLERİNDEN TASARLANDI\n\n");
    out.append("A • BÖLGE GEREKSİNİMİ ↔ PAD UYGULAMASI\n");
    out.append(String.format(Locale.US,"Zone ΣF = [%+.2f, %+.2f, %+.2f] N\n",zF[0],zF[1],zF[2]));
    out.append(String.format(Locale.US,"Pad  ΣF = [%+.2f, %+.2f, %+.2f] N\n",pF[0],pF[1],pF[2]));
    out.append(String.format(Locale.US,"ΔF = %.3f N (%.3f%%)\n",fErr,fPct));
    out.append(String.format(Locale.US,"Zone ΣM = [%+.2f, %+.2f, %+.2f] Nmm\n",zM[0],zM[1],zM[2]));
    out.append(String.format(Locale.US,"Pad  ΣM = [%+.2f, %+.2f, %+.2f] Nmm\n",pM[0],pM[1],pM[2]));
    out.append(String.format(Locale.US,"ΔM = %.3f Nmm (%.3f%%)\n",mErr,mPct));
    out.append((fPct<=.5&&mPct<=.5)?"MATCH: global requirement closure iyi.\n\n":"REVIEW: pad dağılımı bölge hedefinden sapıyor; AUTO yine senin PAD tablonu kullandı.\n\n");

    out.append("B • WHIFFLETREE KATMAN KUVVETLERİ\n");
    TreeMap<Integer,ArrayList<WtBeam>> layers=new TreeMap<>();
    for(WtBeam b:wtBeams){if(!layers.containsKey(b.level))layers.put(b.level,new ArrayList<>());layers.get(b.level).add(b);}
    if(layers.isEmpty())out.append("Beam katmanı yok — pad doğrudan root/load train'e gidiyor.\n");
    for(Map.Entry<Integer,ArrayList<WtBeam>> e:layers.entrySet()){
      int level=e.getKey();out.append("\nLAYER-").append(level).append("\n");
      Collections.sort(e.getValue(),(a,b)->{int c=Integer.compare(a.act,b.act);return c!=0?c:Double.compare(a.x,b.x);});
      for(WtBeam b:e.getValue()){
        double rOut=Math.sqrt(b.fx*b.fx+b.fy*b.fy+b.fz*b.fz);
        out.append(String.format(Locale.US,
          "%s  [%s %.1f N + %s %.1f N] → R %.1f N\n  pivot x=%+.1f mm • left arm %.1f • right arm %.1f mm • pivot %.1f%% • ΣMres %.4f Nmm\n",
          b.id,b.leftId,b.fl,b.rightId,b.fr,rOut,b.x,b.leftArm,b.rightArm,100*b.pivotRatio,b.momentResidual));
      }
    }

    out.append("\nC • ROOT / ACTUATOR KUVVETİ — EN SON\n");
    ArrayList<WtRoot> rs=new ArrayList<>(wtRoots);Collections.sort(rs,(a,b)->Integer.compare(a.act,b.act));
    double sfx=0,sfy=0,sfz=0;
    for(WtRoot r:rs){
      sfx+=r.fx;sfy+=r.fy;sfz+=r.fz;
      out.append(String.format(Locale.US,
        "ACT-%d REQUIRED FORCE = %.2f N\n  Fx %+.2f • Fy %+.2f • Fz %+.2f N\n  equivalent application x = %+.1f mm\n",
        r.act+1,r.r,r.fx,r.fy,r.fz,r.x));
    }
    double sys=Math.sqrt(sfx*sfx+sfy*sfy+sfz*sfz);
    if(rs.size()==1)out.append(String.format(Locale.US,"\nSONUÇ: GEREKLİ AKTÜATÖR KUVVETİ = %.2f N (%.3f kN)\n",rs.get(0).r,rs.get(0).r/1000.0));
    else out.append(String.format(Locale.US,"\nToplam sistem resultantı = %.2f N; fiziksel olarak %d ayrı actuator/root kuvveti yukarıda verilmiştir.\n",sys,rs.size()));

    out.append(String.format(Locale.US,
      "\nTREE CHECK\nArchitecture: %s\nDirection spread %.2f°\nTree force closure %.6f N\nWorst beam moment residual %.6f Nmm\n",
      wtArchitecture,wtDirectionSpread,wtForceResidual,wtMomentResidual));
    if(wtDirectionSpread>8)out.append("UYARI: pad kuvvet yönleri ortak değil; tek düzlemsel pasif beam yerine ayrı eksen tree / 3D load-train geometrisi kontrolü gerekebilir.\n");

    chainResult.setText(out.toString());
    chainStatus.setText("AUTO TASARIM TAMAMLANDI — sonuç sırası pad → layer'lar → root → actuator. Pad değerlerini değiştirirsen AUTO'ya tekrar basarak ağacı yeniden sentezleyebilirsin.");
  }
}
