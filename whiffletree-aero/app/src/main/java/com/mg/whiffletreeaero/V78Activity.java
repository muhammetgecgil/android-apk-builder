package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V78Activity extends V76Activity {
  LinearLayout proHome, quickInput, proResult;
  EditText hLen,hDia,hSections,hFx,hFy,hFz,hActs,hLayers,hCap;
  Spinner hActType,hPhase;
  TextView statusCard, resultCard;
  Button calcBtn, advancedBtn;
  Button navInput, navRig, navProof, navPart, navTest;
  View activePanel;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    hideAllInherited();

    proHome=new LinearLayout(this);proHome.setOrientation(LinearLayout.VERTICAL);proHome.setPadding(dp(12),dp(12),dp(12),dp(16));proHome.setBackground(bg(Color.rgb(3,16,27),18));
    proHome.addView(tx("WHIFFLETREE AERO",25,true,Color.WHITE));
    proHome.addView(tx("EFT Structural Test Design Workspace",11,false,Color.rgb(174,207,228)));

    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    navInput=navBtn("GİRİŞ",v->showHome());
    navRig=navBtn("2D RIG",v->showSection(visualGuide,navRig));
    navProof=navBtn("İSPAT",v->showSection(matrixPanel,navProof));
    navPart=navBtn("PARÇA",v->showSection(equipPanel,navPart));
    navTest=navBtn("TEST",v->showSection(simPanel,navTest));
    nav.addView(navInput,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(navRig,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(navProof,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(navPart,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(navTest,new LinearLayout.LayoutParams(0,dp(46),1));
    proHome.addView(nav,lp());

    statusCard=card("1. Girdileri sırayla doldur  →  2. HESAPLA VE GÖSTER  →  3. 2D RIG / İSPAT / PARÇA / TEST sekmelerini incele",Color.rgb(13,47,67));
    proHome.addView(statusCard,lp());

    quickInput=new LinearLayout(this);quickInput.setOrientation(LinearLayout.VERTICAL);
    section("1 — EFT", "Test parçasının temel geometrisi");
    hLen=field("Tank uzunluğu L", "m", gLen.getText().toString(), false);
    hDia=field("Tank çapı D", "m", gDia.getText().toString(), false);

    section("2 — YÜKLEME", "Kaç bölüm ve toplam signed kuvvetler");
    hSections=field("Yük uygulama bölümü", "adet", gSections.getText().toString(), true);
    hFx=field("Fx  Boyuna", "N", gFx.getText().toString(), false);
    hFy=field("Fy  Yanal", "N", gFy.getText().toString(), false);
    hFz=field("Fz  Düşey", "N", gFz.getText().toString(), false);

    section("3 — TEST RIG", "Actuator ve whiffletree topolojisi");
    hActs=field("Actuator sayısı", "adet", gActs.getText().toString(), true);
    hLayers=field("Whiffletree layer", "kademe", gLayers.getText().toString(), true);
    hCap=field("Actuator kapasitesi", "N", gActCap.getText().toString(), false);
    quickInput.addView(tx("Actuator tipi",10,true,Color.rgb(210,225,235)));
    hActType=new Spinner(this);hActType.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"HYDRAULIC","ELECTRIC"}));hActType.setSelection(gActType.getSelectedItemPosition());quickInput.addView(hActType,new LinearLayout.LayoutParams(-1,dp(48)));

    section("4 — TEST SEVİYESİ", "Hangi yük durumu hesaplanacak?");
    hPhase=new Spinner(this);hPhase.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"LIMIT LOADING","ULTIMATE LOADING","UNLOADING"}));hPhase.setSelection(gPhase.getSelectedItemPosition());quickInput.addView(hPhase,new LinearLayout.LayoutParams(-1,dp(48)));

    calcBtn=new Button(this);calcBtn.setText("HESAPLA VE GÖSTER");calcBtn.setTextSize(18);calcBtn.setAllCaps(false);calcBtn.setOnClickListener(v->calculateProfessional());
    LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(64));bp.setMargins(0,dp(14),0,dp(8));quickInput.addView(calcBtn,bp);
    proHome.addView(quickInput,lp());

    resultCard=card("Henüz hesap yapılmadı.",Color.rgb(12,39,55));proHome.addView(resultCard,lp());
    advancedBtn=navBtn("GELİŞMİŞ GİRDİLER / TÜM MODÜLLER",v->showAdvanced());proHome.addView(advancedBtn,new LinearLayout.LayoutParams(-1,dp(50)));

    root.addView(proHome,0,lp());
    showHome();
  }

  Button navBtn(String s,View.OnClickListener l){
    Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setOnClickListener(l);return b;
  }

  void setActiveNav(Button active){
    Button[] bs=new Button[]{navInput,navRig,navProof,navPart,navTest};
    for(Button b:bs){if(b==null)continue;b.setBackgroundColor(b==active?Color.rgb(31,111,181):Color.rgb(28,43,56));b.setTextColor(Color.WHITE);}
  }

  void section(String title,String hint){
    LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(10),dp(8),dp(10),dp(8));c.setBackground(bg(Color.rgb(13,39,55),10));
    c.addView(tx(title,14,true,Color.rgb(247,207,77)));c.addView(tx(hint,8,false,Color.rgb(180,207,225)));
    LinearLayout.LayoutParams p=lp();p.setMargins(0,dp(10),0,dp(5));quickInput.addView(c,p);
  }

  EditText field(String label,String unit,String def,boolean integer){
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(3),0,dp(3));
    TextView l=tx(label,10,true,Color.WHITE);row.addView(l,new LinearLayout.LayoutParams(0,dp(48),1.15f));
    EditText e=new EditText(this);e.setText(def);e.setTextColor(Color.WHITE);e.setTextSize(17);e.setSingleLine(true);e.setSelectAllOnFocus(true);e.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);e.setPadding(dp(10),0,dp(10),0);e.setBackground(bg(Color.rgb(25,58,80),9));e.setInputType(InputType.TYPE_CLASS_NUMBER|(integer?0:(InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED)));row.addView(e,new LinearLayout.LayoutParams(0,dp(48),1));
    TextView u=tx(unit,9,true,Color.rgb(155,190,215));u.setGravity(Gravity.CENTER);row.addView(u,new LinearLayout.LayoutParams(dp(48),dp(48)));
    quickInput.addView(row,lp());return e;
  }

  void hideAllInherited(){for(int i=0;i<root.getChildCount();i++)root.getChildAt(i).setVisibility(View.GONE);}
  void setHomeBlocks(boolean visible){int v=visible?View.VISIBLE:View.GONE;if(statusCard!=null)statusCard.setVisibility(v);if(quickInput!=null)quickInput.setVisibility(v);if(resultCard!=null)resultCard.setVisibility(v);if(advancedBtn!=null)advancedBtn.setVisibility(v);}

  void showHome(){
    hideAllInherited();
    if(proHome!=null){proHome.setVisibility(View.VISIBLE);setHomeBlocks(true);}
    activePanel=null;setActiveNav(navInput);
  }

  void showSection(View target,Button tab){
    hideAllInherited();
    if(proHome!=null){proHome.setVisibility(View.VISIBLE);setHomeBlocks(false);}
    if(target!=null){target.setVisibility(View.VISIBLE);activePanel=target;}
    setActiveNav(tab);
    if(!solvedValid && target!=null){Toast.makeText(this,"Bu sekme hesap sonucu kullanır. Önce GİRİŞ ekranından HESAPLA VE GÖSTER'e bas.",Toast.LENGTH_SHORT).show();}
  }

  void showAdvanced(){
    hideAllInherited();
    if(proHome!=null){proHome.setVisibility(View.VISIBLE);setHomeBlocks(false);}
    if(guided!=null){guided.setVisibility(View.VISIBLE);activePanel=guided;}
    setActiveNav(null);
  }

  double val(EditText e){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception ex){return Double.NaN;}}
  int ival(EditText e,int lo,int hi){double v=val(e);if(Double.isNaN(v))return lo;return Math.max(lo,Math.min(hi,(int)Math.round(v)));}

  void calculateProfessional(){
    double L=val(hLen),D=val(hDia),fx=val(hFx),fy=val(hFy),fz=val(hFz),cap=val(hCap);
    int ns=ival(hSections,1,20),na=ival(hActs,1,12),nl=ival(hLayers,1,4);
    if(Double.isNaN(L)||Double.isNaN(D)||L<=0||D<=0||Double.isNaN(fx)||Double.isNaN(fy)||Double.isNaN(fz)||Double.isNaN(cap)||cap<=0){
      statusCard.setText("GİRİŞ KONTROLÜ: Boş alan bırakma. L, D ve kapasite > 0 olmalı; Fx/Fy/Fz pozitif veya negatif olabilir.");return;
    }
    gLen.setText(String.format(Locale.US,"%.3f",L));gDia.setText(String.format(Locale.US,"%.3f",D));gSections.setText(String.valueOf(ns));gFx.setText(String.format(Locale.US,"%.3f",fx));gFy.setText(String.format(Locale.US,"%.3f",fy));gFz.setText(String.format(Locale.US,"%.3f",fz));gActs.setText(String.valueOf(na));gLayers.setText(String.valueOf(nl));gActCap.setText(String.format(Locale.US,"%.0f",cap));gActType.setSelection(hActType.getSelectedItemPosition());gPhase.setSelection(hPhase.getSelectedItemPosition());
    runGuided();
    if(solvedValid&&solved!=null&&!solved.isEmpty()){
      double sx=0,sy=0,sz=0,peak=0,maxM=0,maxD=0;double[] ar=new double[na];
      for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;maxM=Math.max(maxM,Math.sqrt(s.mx*s.mx+s.my*s.my+s.mz*s.mz));maxD=Math.max(maxD,Math.abs(s.disp));ar[Math.min(na-1,s.act)]+=s.r;}
      for(double q:ar)peak=Math.max(peak,q);
      statusCard.setText("HESAP TAMAMLANDI — 2D RIG ile yük yolunu, İSPAT ile dengeyi, PARÇA ile ekipman seçimini, TEST ile simülasyonu incele.");
      resultCard.setText(String.format(Locale.US,"ANLIK SONUÇ\n%d yük bölgesi • %d layer • %d actuator • %s\nΣFx %+.0f N   ΣFy %+.0f N   ΣFz %+.0f N\nPeak actuator %.0f N / kapasite %.0f N\nMax moment %.0f Nmm   Max displacement %.3f mm",solved.size(),nl,na,hActType.getSelectedItem().toString(),sx,sy,sz,peak,cap,maxM,maxD));
      showSection(visualGuide,navRig);
    }else statusCard.setText("HESAP BLOKE — girişleri ve validation sonuçlarını kontrol et.");
  }

  @Override public void onBackPressed(){
    if(activePanel!=null){showHome();return;}
    super.onBackPressed();
  }
}
