package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.*;

/** v7.11 — single-source engineering solver. */
public class V711Activity extends V79Activity {
  TextView engineeringHealth;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    engineeringHealth=card("ENGINEERING CORE: hazır — girişler HESAPLA VE GÖSTER ile tek solver'a aktarılır.",Color.rgb(12,52,65));
    proHome.addView(engineeringHealth, Math.min(4,proHome.getChildCount()), lp());
  }

  double testFactor(){
    int p=hPhase==null?0:hPhase.getSelectedItemPosition();
    double f=p==0?qd(qLimit):(p==1?qd(qUltimate):qd(qUnload));
    if(Double.isNaN(f)||Double.isInfinite(f))f=1.0;
    return Math.max(0,f);
  }

  @Override void calculateProfessional(){
    double Lm=val(hLen), Dm=val(hDia), fxTotal=val(hFx), fyTotal=val(hFy), fzTotal=val(hFz), cap=val(hCap);
    int ns=ival(hSections,1,20), na=ival(hActs,1,12), nl=ival(hLayers,1,4);
    if(Double.isNaN(Lm)||Double.isNaN(Dm)||Lm<=0||Dm<=0||Double.isNaN(fxTotal)||Double.isNaN(fyTotal)||Double.isNaN(fzTotal)||Double.isNaN(cap)||cap<=0){
      statusCard.setText("GİRİŞ HATASI — L, D ve actuator kapasitesi > 0 olmalı. Fx/Fy/Fz pozitif, sıfır veya negatif olabilir.");
      engineeringHealth.setText("ENGINEERING CORE: çözüm çalıştırılmadı — sayısal girişleri düzelt.");
      solvedValid=false;return;
    }

    qLength.setText(String.format(Locale.US,"%.3f",Lm*1000.0));qDiameter.setText(String.format(Locale.US,"%.3f",Dm*1000.0));
    qSections.setText(String.valueOf(ns));qFx.setText(String.format(Locale.US,"%.6f",fxTotal));qFy.setText(String.format(Locale.US,"%.6f",fyTotal));qFz.setText(String.format(Locale.US,"%.6f",fzTotal));
    qActs.setText(String.valueOf(na));qLayers.setText(String.valueOf(nl));qActType.setSelection(hActType.getSelectedItemPosition());qPhase.setSelection(hPhase.getSelectedItemPosition());
    pFx.setText(String.format(Locale.US,"%.6f",fxTotal));pFy.setText(String.format(Locale.US,"%.6f",fyTotal));pFz.setText(String.format(Locale.US,"%.6f",fzTotal));pActs.setText(String.valueOf(na));pLayers.setText(String.valueOf(nl));pActType.setSelection(hActType.getSelectedItemPosition());
    gLen.setText(String.format(Locale.US,"%.3f",Lm));gDia.setText(String.format(Locale.US,"%.3f",Dm));gSections.setText(String.valueOf(ns));gFx.setText(String.format(Locale.US,"%.6f",fxTotal));gFy.setText(String.format(Locale.US,"%.6f",fyTotal));gFz.setText(String.format(Locale.US,"%.6f",fzTotal));
    gActs.setText(String.valueOf(na));gLayers.setText(String.valueOf(nl));gActCap.setText(String.format(Locale.US,"%.0f",cap));gActType.setSelection(hActType.getSelectedItemPosition());gPhase.setSelection(hPhase.getSelectedItemPosition());
    for(int i=0;i<actKnown.length;i++)actKnown[i].setText(String.format(Locale.US,"%.0f",cap));

    double len=Math.max(1,Lm*1000.0), fac=testFactor(), stiffness=Math.max(1,qd(qStiffness)), gauge=Math.max(1,qd(qGaugeLength));
    double yoff=qd(qYoff),zoff=qd(qZoff);solved.clear();
    for(int i=0;i<ns;i++){
      SNode s=new SNode();s.section=i;s.act=Math.min(na-1,(int)Math.floor((double)i*na/ns));s.x=-len/2.0+(i+.5)*len/ns;s.y=yoff;s.z=zoff;
      s.fx=fxTotal*fac/ns;s.fy=fyTotal*fac/ns;s.fz=fzTotal*fac/ns;s.r=Math.sqrt(s.fx*s.fx+s.fy*s.fy+s.fz*s.fz);
      s.mx=s.y*s.fz-s.z*s.fy;s.my=s.z*s.fx-s.x*s.fz;s.mz=s.x*s.fy-s.y*s.fx;s.disp=s.r/stiffness;s.strain=s.disp/gauge*1e6;s.lc=s.r;solved.add(s);
    }
    solvedValid=!solved.isEmpty();

    structural3D.nodes=new ArrayList<>(solved);structural3D.invalidate();buildConnections();
    if(connection2D!=null)connection2D.invalidate();if(connection3D!=null)connection3D.invalidate();
    update2DResult();refreshVisualGuide();updateFocusedResults();refreshEquipment();refreshMatrixSolver();refreshSimulation();
    if(signed2D!=null)signed2D.invalidate();if(poster!=null)poster.invalidate();if(rigPoster70!=null)rigPoster70.invalidate();if(matrixView!=null)matrixView.invalidate();if(rigAssembly!=null)rigAssembly.invalidate();

    double sx=0,sy=0,sz=0,mx=0,my=0,mz=0,maxZone=0,maxDisp=0,maxStrain=0;double[] ax=new double[na],ay=new double[na],az=new double[na];
    for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;mx+=s.mx;my+=s.my;mz+=s.mz;maxZone=Math.max(maxZone,s.r);maxDisp=Math.max(maxDisp,Math.abs(s.disp));maxStrain=Math.max(maxStrain,Math.abs(s.strain));ax[s.act]+=s.fx;ay[s.act]+=s.fy;az[s.act]+=s.fz;}
    double peakAct=0,maxUtil=0,asx=0,asy=0,asz=0;for(int a=0;a<na;a++){double r=Math.sqrt(ax[a]*ax[a]+ay[a]*ay[a]+az[a]*az[a]);peakAct=Math.max(peakAct,r);maxUtil=Math.max(maxUtil,100*r/Math.max(1,av(a)));asx+=ax[a];asy+=ay[a];asz+=az[a];}
    double closure=Math.sqrt((sx-asx)*(sx-asx)+(sy-asy)*(sy-asy)+(sz-asz)*(sz-asz));double maxMoment=Math.sqrt(mx*mx+my*my+mz*mz);

    statusCard.setText("HESAP TAMAMLANDI — tüm sonuç sekmeleri aynı aktif solved-load setinden güncellendi.");
    resultCard.setText(String.format(Locale.US,"ANLIK MÜHENDİSLİK SONUCU\n%d yük bölgesi • %d layer • %d actuator • %s • %s\nUygulanan faktör %.3f\nΣFx %+.1f N   ΣFy %+.1f N   ΣFz %+.1f N\nΣMx %+.1f   ΣMy %+.1f   ΣMz %+.1f Nmm\nPeak zone %.1f N • Peak actuator %.1f N • Util %.1f%%\nMax displacement %.6f mm • Max strain %.1f µε\nForce closure residual %.6f N",ns,nl,na,hActType.getSelectedItem().toString(),hPhase.getSelectedItem().toString(),fac,sx,sy,sz,mx,my,mz,maxZone,peakAct,maxUtil,maxDisp,maxStrain,closure));
    engineeringHealth.setText(String.format(Locale.US,"ENGINEERING CORE — %s\nInput→solver→2D/3D→proof→component→test chain refreshed.\nForce closure %.6f N | resultant moment %.1f Nmm | actuator capacity %s\nSections requested/calculated: %d/%d",closure<1e-6?"PASS":"CHECK",closure,maxMoment,maxUtil<=100?"PASS":"OVERLOAD",ns,solved.size()));

    if(visualGuide!=null)visualGuide.setVisibility(View.VISIBLE);
  }
}
