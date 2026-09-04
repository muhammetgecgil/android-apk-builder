package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

/** v7.17 — guided AUTO WHIFFLETREE workflow + active rig sizing/test dashboard. */
public class V717Activity extends V716Activity {
  LinearLayout autoGuide;
  TextView autoInputSummary, autoResultSummary, componentCheckSummary, liveTestSummary;
  Button editLoadsBtn, autoDesignBtn, showRigBtn, showProofBtn, showPartsBtn, showTestBtn;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);

    autoGuide=new LinearLayout(this);autoGuide.setOrientation(LinearLayout.VERTICAL);autoGuide.setPadding(dp(10),dp(10),dp(10),dp(10));
    autoGuide.setBackground(bg(Color.rgb(7,31,45),14));
    autoGuide.addView(tx("AUTO WHIFFLETREE — ADIM ADIM",18,true,Color.WHITE));
    autoGuide.addView(tx("1 Yükleri tanımla  →  2 Rig sınırlarını gir  →  3 AUTO TASARLA  →  4 Aktif tasarımı doğrula  →  5 Parça/Test",10,false,Color.rgb(185,215,232)));

    autoInputSummary=card("GEREKLİ GİRDİLER\n• EFT L/D\n• Yük bölgesi sayısı ve x konumları\n• Her bölge için signed Fx/Fy/Fz (ve gelişmişte moment/ofset)\n• Actuator sayısı + kapasitesi + hydraulic/electric\n• İzin verilen whiffletree layer sayısı\n• LIMIT / ULTIMATE / UNLOAD seviyesi",Color.rgb(13,49,65));
    autoGuide.addView(autoInputSummary,lp());

    LinearLayout r1=new LinearLayout(this);r1.setOrientation(LinearLayout.HORIZONTAL);
    editLoadsBtn=designBtn("1 • BÖLGE YÜKLERİ",v->buildZoneEditor());
    autoDesignBtn=designBtn("2 • AUTO TASARLA",v->{calculateProfessional();refreshGuidedDashboard();});
    r1.addView(editLoadsBtn,new LinearLayout.LayoutParams(0,dp(56),1));r1.addView(autoDesignBtn,new LinearLayout.LayoutParams(0,dp(56),1));autoGuide.addView(r1,lp());

    autoResultSummary=card("AUTO TASARIM SONUCU\nHenüz çözüm yok. Önce yükleri gir, actuator/layer sınırlarını seç ve AUTO TASARLA'ya bas.",Color.rgb(15,45,56));
    autoGuide.addView(autoResultSummary,lp());

    componentCheckSummary=card("RIG ELEMAN KONTROLÜ\nAktif tasarım sonrası beam/pivot, pin-clevis, load-cell ve actuator talepleri burada özetlenecek.",Color.rgb(28,45,48));
    autoGuide.addView(componentCheckSummary,lp());

    liveTestSummary=card("TEST SİMÜLASYONU\nAktif rig için LIMIT / ULTIMATE / UNLOAD sırasında actuator, pad ve resultant yükleri burada özetlenecek.",Color.rgb(29,41,55));
    autoGuide.addView(liveTestSummary,lp());

    LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);
    showRigBtn=designBtn("2D RIG",v->showSection(visualGuide,navRig));showProofBtn=designBtn("İSPAT",v->showSection(matrixPanel,navProof));showPartsBtn=designBtn("PARÇA",v->showSection(equipPanel,navPart));showTestBtn=designBtn("TEST",v->showSection(simPanel,navTest));
    r2.addView(showRigBtn,new LinearLayout.LayoutParams(0,dp(52),1));r2.addView(showProofBtn,new LinearLayout.LayoutParams(0,dp(52),1));r2.addView(showPartsBtn,new LinearLayout.LayoutParams(0,dp(52),1));r2.addView(showTestBtn,new LinearLayout.LayoutParams(0,dp(52),1));autoGuide.addView(r2,lp());

    // Put the guided workflow immediately before the advanced controls.
    proHome.addView(autoGuide,Math.min(3,proHome.getChildCount()),lp());
    refreshGuidedDashboard();
  }

  @Override void calculateProfessional(){
    super.calculateProfessional();
    refreshGuidedDashboard();
  }

  @Override void applyDesign(AltDesign d){
    super.applyDesign(d);
    refreshGuidedDashboard();
  }

  void refreshGuidedDashboard(){
    if(autoInputSummary==null)return;
    int ns=ival(hSections,1,20),na=ival(hActs,1,12),nl=ival(hLayers,1,4);double cap=val(hCap);
    String mode=customZones?"CUSTOM SECTION LOADS":"UNIFORM TOTAL LOAD";
    autoInputSummary.setText(String.format(Locale.US,
      "GEREKLİ GİRDİLER — %s\nEFT: L %.3f m • D %.3f m\nYük uygulama bölgesi: %d adet\nToplam giriş: Fx %+.1f N • Fy %+.1f N • Fz %+.1f N\nRig sınırı: %d actuator × %.0f N • %d layer • %s\nTest seviyesi: %s\n\nGerçek test yüklerin eşit değilse önce BÖLGE YÜKLERİ'ne girip her S noktasının x/Fx/Fy/Fz değerini yaz.",
      mode,val(hLen),val(hDia),ns,val(hFx),val(hFy),val(hFz),na,cap,nl,hActType.getSelectedItem().toString(),hPhase.getSelectedItem().toString()));

    if(!solvedValid||solved==null||solved.isEmpty()){
      autoResultSummary.setText("AUTO TASARIM SONUCU\nHenüz çözüm yok. Yükleri ve rig sınırlarını girip AUTO TASARLA'ya bas.");return;
    }

    double sx=0,sy=0,sz=0;for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;}
    double totalR=Math.sqrt(sx*sx+sy*sy+sz*sz),peakAct=0,maxUtil=0;double[] ar=new double[na];
    for(SNode s:solved){int a=Math.max(0,Math.min(na-1,s.act));ar[a]+=s.r;}
    StringBuilder acts=new StringBuilder();for(int a=0;a<na;a++){peakAct=Math.max(peakAct,ar[a]);maxUtil=Math.max(maxUtil,ar[a]/Math.max(1,cap));if(a>0)acts.append(" • ");acts.append("A").append(a+1).append(" ").append(String.format(Locale.US,"%.0fN",ar[a]));}
    String rec=recommended==null?"-":("DESIGN "+recommended.id+" / "+recommended.name+" / "+String.format(Locale.US,"%.1f",recommended.score)+" puan");
    String active=activeDesign==null?"Henüz aktif tasarım yok":("DESIGN "+activeDesign.id+" — "+activeDesign.name);
    autoResultSummary.setText(String.format(Locale.US,
      "AUTO TASARIM SONUCU\nRequired resultant: %.1f N  [Fx %+.1f • Fy %+.1f • Fz %+.1f]\nPad / station: %d • calculated beam/pivot: %d\nÖNERİLEN: %s\nAKTİF: %s\nActuator kökleri: %s\nPeak actuator %.1f N / %.1f N → utilization %.1f%%\nForce closure %.6f N • worst beam ΣM residual %.6f Nmm\nArchitecture: %s\n\n2D RIG'e geçtiğinde her pad, beam, pivot ve actuator üzerinde gerçek kuvveti görürsün.",
      totalR,sx,sy,sz,solved.size(),wtBeams.size(),rec,active,acts.toString(),peakAct,cap,100*maxUtil,wtForceResidual,wtMomentResidual,wtArchitecture));

    // Preliminary rig sizing / demand dashboard. This is demand-based, not a release-to-manufacture calculation.
    double maxBeamLoad=0,maxBeamMoment=0,minArm=Double.POSITIVE_INFINITY,maxPivotEdge=0;
    for(WtBeam b:wtBeams){maxBeamLoad=Math.max(maxBeamLoad,b.total);maxBeamMoment=Math.max(maxBeamMoment,Math.max(Math.abs(b.fl*b.leftArm),Math.abs(b.fr*b.rightArm)));minArm=Math.min(minArm,Math.min(b.leftArm,b.rightArm));maxPivotEdge=Math.max(maxPivotEdge,Math.max(b.pivotRatio,1-b.pivotRatio));}
    if(Double.isInfinite(minArm))minArm=0;
    double suggestedLC=Math.ceil(peakAct*1.25/1000.0)*1000.0;double suggestedAct=Math.ceil(peakAct*1.20/1000.0)*1000.0;
    String capStatus=maxUtil<=.8?"PASS / COMFORTABLE":(maxUtil<=1?"PASS / LOW MARGIN":"OVERLOAD");
    componentCheckSummary.setText(String.format(Locale.US,
      "RIG ELEMAN KONTROLÜ — PRELIMINARY\nActuator demand: %.1f N → minimum suggested nominal ≈ %.0f N [%s]\nLoad-cell demand: %.1f N → suggested capacity ≈ %.0f N\nPeak whiffletree beam transferred load: %.1f N\nPeak lever moment demand: %.1f Nmm\nMinimum calculated pivot arm: %.1f mm • most eccentric pivot %.1f%%\n\nPARÇA sekmesi gerçek component adaylarını; İSPAT sekmesi ΣF/ΣM ve required/applied doğrulamasını göstermeli. Nihai beam/pin/clevis boyutu malzeme, kesit, fatigue, bearing ve fixture geometry ile doğrulanmalı.",
      peakAct,suggestedAct,capStatus,peakAct,suggestedLC,maxBeamLoad,maxBeamMoment,minArm,100*maxPivotEdge));

    double fac=testFactor();
    liveTestSummary.setText(String.format(Locale.US,
      "TEST SİMÜLASYONU — %s\nAktif load factor: %.3f\nApplied totals: Fx %+.1f N • Fy %+.1f N • Fz %+.1f N\nPeak actuator %.1f N • max utilization %.1f%%\nLoad path: EFT pad → whiffletree beam/pivot → load-cell → actuator → strongback\n\nTEST sekmesinde aynı aktif tasarım LIMIT / ULTIMATE / UNLOAD boyunca izlenir; tasarım değiştirilirse simülasyon da aynı station→actuator mapping ile yeniden hesaplanır.",
      hPhase.getSelectedItem().toString(),fac,sx,sy,sz,peakAct,100*maxUtil));
  }
}
