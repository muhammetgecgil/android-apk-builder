package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

/** v8.1 — first-screen load position and per-pad x/y/z + Fx/Fy/Fz input. */
public class V810Activity extends V801Activity {
  Spinner entryMode;
  EditText loadX,loadY,loadZ,firstPadCount;
  LinearLayout firstPadPanel;
  TextView entryHelp,positionMomentPreview;
  Button rebuildFirstPads;
  boolean firstUiReady=false,rebuildingFirst=false;
  ArrayList<FirstPadRow> firstRows=new ArrayList<>();

  static class FirstPadRow { EditText x,y,z,fx,fy,fz; }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    entryMode=new Spinner(this);
    entryMode.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
      "TOPLAM KUVVET + UYGULAMA POZİSYONU",
      "PAD BAZLI — her pad için x/y/z + Fx/Fy/Fz"
    }));
    loadX=fresh("0");loadY=fresh("0");loadZ=fresh("0");
    firstPadCount=fresh(wSections==null?"8":wSections.getText().toString());
    firstPadPanel=new LinearLayout(this);firstPadPanel.setOrientation(LinearLayout.VERTICAL);
    entryHelp=card("Yük giriş modu seç.",Color.rgb(12,47,61));
    positionMomentPreview=card("Pozisyon girildiğinde r×F momenti burada gösterilecek.",Color.rgb(19,43,55));
    rebuildFirstPads=designBtn("PAD TABLOSUNU OLUŞTUR / YENİLE",v->{ensureFirstPadRows(firstPadN(),true);showStep(1);});
    entryMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
      @Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(firstUiReady&&!rebuildingFirst)showStep(1);}
      @Override public void onNothingSelected(AdapterView<?> p){}
    });
    firstUiReady=true;ensureFirstPadRows(firstPadN(),false);showStep(1);
  }

  @Override void showStep(int s){
    super.showStep(s);
    if(!firstUiReady||s!=1)return;
    rebuildingFirst=true;
    try{
      if(pads!=null)body.removeView(pads);
      body.addView(selector("İLK EKRAN YÜK GİRİŞİ",entryMode),lp());
      if(entryMode.getSelectedItemPosition()==0){
        entryHelp.setText("TOPLAM KUVVET + POZİSYON: Fx/Fy/Fz yukarıdaki toplam yüklerdir. Aşağıdaki x/y/z, bu resultant kuvvetin test article referansına göre uygulama noktasıdır. Program M=r×F değerini hesaplar. Dağıtılmış gerçek pad yüklerin belliyse PAD BAZLI modu seç.");
        body.addView(entryHelp,lp());body.addView(pair("Uygulama x [mm]",loadX,"Uygulama y [mm]",loadY),lp());body.addView(field("Uygulama z [mm]",loadZ,"Moment referans noktası ile aynı koordinat sistemi kullanılmalı."),lp());
        refreshPositionMomentPreview();body.addView(positionMomentPreview,lp());
      }else{
        entryHelp.setText("PAD BAZLI: yükü baştan pad seviyesinde tanımla. Her pad için fiziksel koordinat [x,y,z] ve signed Fx/Fy/Fz girilir. ΣF ve Σ(r×F) doğrudan bu tablodan hesaplanır; whiffletree optimizer bu gerçek pad yüklerini kullanır.");
        body.addView(entryHelp,lp());body.addView(field("Pad sayısı",firstPadCount,"2–24; tabloyu yenileyince satır sayısı buna göre oluşturulur."),lp());body.addView(rebuildFirstPads,new LinearLayout.LayoutParams(-1,dp(54)));
        ensureFirstPadRows(firstPadN(),false);if(firstPadPanel.getParent()!=null)((android.view.ViewGroup)firstPadPanel.getParent()).removeView(firstPadPanel);body.addView(firstPadPanel,lp());
      }
    }finally{rebuildingFirst=false;}
  }

  LinearLayout pair(String la,EditText a,String lb,EditText b){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.addView(field(la,a,""),new LinearLayout.LayoutParams(0,-2,1));r.addView(field(lb,b,""),new LinearLayout.LayoutParams(0,-2,1));return r;}
  int firstPadN(){try{return Math.max(2,Math.min(24,(int)Math.round(Double.parseDouble(firstPadCount.getText().toString().trim()))));}catch(Exception e){return 8;}}
  EditText pc(String s){EditText e=small(s);e.setTextSize(10);e.setGravity(Gravity.CENTER);return e;}

  void ensureFirstPadRows(int n,boolean force){
    if(!force&&firstRows.size()==n&&firstPadPanel.getChildCount()>0)return;
    firstRows.clear();firstPadPanel.removeAllViews();firstPadPanel.addView(tx("PAD      x       y       z        Fx        Fy        Fz",9,true,Color.rgb(247,207,77)),lp());
    double L=Math.max(.1,val(hLen))*1000.0,fx=parse(wFx,0),fy=parse(wFy,0),fz=parse(wFz,0);
    for(int i=0;i<n;i++){
      FirstPadRow p=new FirstPadRow();double x=-L/2.0+(i+.5)*L/n;
      p.x=pc(String.format(Locale.US,"%.1f",x));p.y=pc("0");p.z=pc("0");p.fx=pc(String.format(Locale.US,"%.2f",fx/n));p.fy=pc(String.format(Locale.US,"%.2f",fy/n));p.fz=pc(String.format(Locale.US,"%.2f",fz/n));firstRows.add(p);
      LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.addView(tx("P"+(i+1),9,true,Color.WHITE),new LinearLayout.LayoutParams(dp(36),dp(46)));
      r.addView(p.x,new LinearLayout.LayoutParams(0,dp(46),1));r.addView(p.y,new LinearLayout.LayoutParams(0,dp(46),1));r.addView(p.z,new LinearLayout.LayoutParams(0,dp(46),1));r.addView(p.fx,new LinearLayout.LayoutParams(0,dp(46),1.15f));r.addView(p.fy,new LinearLayout.LayoutParams(0,dp(46),1.15f));r.addView(p.fz,new LinearLayout.LayoutParams(0,dp(46),1.15f));firstPadPanel.addView(r,lp());
    }
  }

  double parse(EditText e,double def){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception x){return def;}}
  void refreshPositionMomentPreview(){double x=parse(loadX,0),y=parse(loadY,0),z=parse(loadZ,0),fx=parse(wFx,0),fy=parse(wFy,0),fz=parse(wFz,0);double mx=y*fz-z*fy,my=z*fx-x*fz,mz=x*fy-y*fx;positionMomentPreview.setText(String.format(Locale.US,"RESULTANT POZİSYONUNDAN OLUŞAN MOMENT\nM = r × F\nMx %+.3f Nmm   My %+.3f Nmm   Mz %+.3f Nmm\n\nDağıtılmış yük ağacı için PAD BAZLI veya 6DOF modu kullan.",mx,my,mz));}

  @Override void calculateProfessional(){
    if(!firstUiReady){super.calculateProfessional();return;}
    if(entryMode.getSelectedItemPosition()==1){super.calculateProfessional();applyFirstPadLoads();return;}
    super.calculateProfessional();applyResultantPositionMetadata();
  }

  void applyResultantPositionMetadata(){refreshPositionMomentPreview();double x=parse(loadX,0),y=parse(loadY,0),z=parse(loadZ,0),fx=parse(wFx,0)*testFactor(),fy=parse(wFy,0)*testFactor(),fz=parse(wFz,0)*testFactor();double mx=y*fz-z*fy,my=z*fx-x*fz,mz=x*fy-y*fx;statusCard.setText(String.format(Locale.US,"RESULTANT LOAD POSITION ACTIVE — r=[%.1f, %.1f, %.1f] mm • F=[%+.1f,%+.1f,%+.1f] N • r×F=[%+.1f,%+.1f,%+.1f] Nmm\nPad bazlı gerçek dağılım gerekiyorsa ilk ekranda PAD BAZLI modu seç.",x,y,z,fx,fy,fz,mx,my,mz));}

  void applyFirstPadLoads(){
    int n=firstPadN();if(firstRows.size()!=n){ensureFirstPadRows(n,true);return;}
    double fac=testFactor(),stiffness=Math.max(1,qd(qStiffness)),gauge=Math.max(1,qd(qGaugeLength));ArrayList<SNode> custom=new ArrayList<>();double sx=0,sy=0,sz=0,smx=0,smy=0,smz=0;
    for(int i=0;i<n;i++){
      FirstPadRow p=firstRows.get(i);double x=parse(p.x,Double.NaN),y=parse(p.y,Double.NaN),z=parse(p.z,Double.NaN),fx=parse(p.fx,Double.NaN),fy=parse(p.fy,Double.NaN),fz=parse(p.fz,Double.NaN);
      if(Double.isNaN(x)||Double.isNaN(y)||Double.isNaN(z)||Double.isNaN(fx)||Double.isNaN(fy)||Double.isNaN(fz)){status.setText("PAD GİRİŞ HATASI — her pad için x/y/z ve Fx/Fy/Fz sayısal olmalı.");return;}
      SNode s=new SNode();s.section=i;s.x=x;s.y=y;s.z=z;s.fx=fx*fac;s.fy=fy*fac;s.fz=fz*fac;s.r=Math.sqrt(s.fx*s.fx+s.fy*s.fy+s.fz*s.fz);s.mx=s.y*s.fz-s.z*s.fy;s.my=s.z*s.fx-s.x*s.fz;s.mz=s.x*s.fy-s.y*s.fx;s.disp=s.r/stiffness;s.strain=s.disp/gauge*1e6;s.lc=s.r;custom.add(s);sx+=s.fx;sy+=s.fy;sz+=s.fz;smx+=s.mx;smy+=s.my;smz+=s.mz;
    }
    solved.clear();solved.addAll(custom);solvedValid=true;customZones=false;forcedActuatorGroups=null;buildSelectableCandidates();if(recommended!=null)applyDesign(recommended);else designAutomaticWhiffletree();
    hSections.setText(String.valueOf(n));hFx.setText(String.format(Locale.US,"%.3f",sx/fac));hFy.setText(String.format(Locale.US,"%.3f",sy/fac));hFz.setText(String.format(Locale.US,"%.3f",sz/fac));statusCard.setText(String.format(Locale.US,"PAD-BASED LOAD INPUT ACTIVE — %d pad\nΣF = [%+.2f, %+.2f, %+.2f] N\nΣM = [%+.2f, %+.2f, %+.2f] Nmm\nHer padın gerçek x/y/z ve signed Fx/Fy/Fz değeri aktif whiffletree optimizer'a aktarıldı.",n,sx,sy,sz,smx,smy,smz));
  }
}
