package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V69Activity extends V68Activity {
  LinearLayout guided;
  EditText gLen,gDia,gSections,gFx,gFy,gFz,gActs,gLayers,gActCap;
  Spinner gActType,gPhase;
  TextView guideState;
  Button guideCalc;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    guided=new LinearLayout(this);guided.setOrientation(LinearLayout.VERTICAL);guided.setPadding(dp(10),dp(10),dp(10),dp(10));guided.setBackground(bg(Color.rgb(2,15,25),16));
    guided.addView(tx("EFT TEST KURULUMU — ADIM ADIM GİRİŞ",22,true,Color.WHITE));
    guided.addView(tx("Alanlar test sistemini kurarken izlenen mühendislik sırasına göre dizildi. Her kartta yalnız o adımın girdileri var.",9,false,Color.rgb(180,210,230)));
    guideState=card("ADIM 1'den başla → geometri → yük bölgeleri → kuvvetler → actuator → whiffletree → test seviyesi → HESAPLA VE GÖSTER",Color.rgb(14,45,64));guided.addView(guideState,lp());

    addStepTitle("1", "EFT GEOMETRİSİ", "Test parçasının temel boyutlarını gir.");
    gLen=guidedField("Tank uzunluğu  L  [m]", String.format(Locale.US,"%.3f",qd(qLength)), false);
    gDia=guidedField("Tank çapı  D  [m]", String.format(Locale.US,"%.3f",qd(qDiameter)), false);

    addStepTitle("2", "YÜK UYGULAMA BÖLGELERİ", "EFT üzerinde kaç ayrı yük uygulama bölgesi / station kullanılacak?");
    gSections=guidedField("Yük uygulama bölüm sayısı  [adet]", qSections.getText().toString(), true);
    guided.addView(tx("Örnek: 12 station varsa 12 gir. Aktif bölgeler hesap ve 2D görünümde S1...Sn olarak gösterilir.",8,false,Color.rgb(170,200,220)));

    addStepTitle("3", "UYGULANACAK KUVVETLER", "İşaretli kuvvetleri Newton olarak gir. Negatif değer ters yön demektir.");
    gFx=guidedField("Fx — Boyuna kuvvet  [N]   (+X / −X)", pFx.getText().toString(), false);
    gFy=guidedField("Fy — Yanal kuvvet  [N]   (+Y / −Y)", pFy.getText().toString(), false);
    gFz=guidedField("Fz — Düşey kuvvet  [N]   (+Z / −Z)", pFz.getText().toString(), false);
    guided.addView(tx("Not: Bölge bazlı farklı Fx/Fy/Fz gerekiyorsa aşağıdaki LOAD ZONES tablosu bu toplam/ana yükü istasyonlara özelleştirir.",8,false,Color.rgb(170,200,220)));

    addStepTitle("4", "ACTUATOR / SİLİNDİR", "Kaç actuator kullanılacağını ve tipini gir.");
    gActs=guidedField("Actuator sayısı  [adet]", pActs.getText().toString(), true);
    guided.addView(tx("Actuator tipi",10,true,Color.rgb(200,220,235)));
    gActType=new Spinner(this);gActType.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"HYDRAULIC","ELECTRIC"}));gActType.setSelection(pActType.getSelectedItemPosition());guided.addView(gActType,new LinearLayout.LayoutParams(-1,dp(50)));
    gActCap=guidedField("Varsayılan actuator kapasitesi  [N]", "100000", false);
    guided.addView(tx("Hesap sonrasında her ACT-n için demand / capacity / utilization gösterilir. İstersen aşağıdaki detay alanında actuator kapasitelerini tek tek değiştirebilirsin.",8,false,Color.rgb(170,200,220)));

    addStepTitle("5", "WHIFFLETREE TOPOLOJİSİ", "Test rig yük dağıtım ağacının katman sayısını seç.");
    gLayers=guidedField("Whiffletree layer / kademe sayısı  [1–4]", pLayers.getText().toString(), true);
    guided.addView(tx("Layer arttıkça çok sayıdaki station yükü daha az actuator hattına kademeli olarak toplanır.",8,false,Color.rgb(170,200,220)));

    addStepTitle("6", "TEST YÜK SEVİYESİ", "Hangi yapısal test fazının hesaplanacağını seç.");
    gPhase=new Spinner(this);gPhase.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"LIMIT LOADING","ULTIMATE LOADING","UNLOADING"}));gPhase.setSelection(qPhase.getSelectedItemPosition());guided.addView(gPhase,new LinearLayout.LayoutParams(-1,dp(50)));

    addStepTitle("7", "HESAPLA VE GÖSTER", "Girilen değerleri modele aktar, dengeyi çöz ve 2D/3D sonuçları üret.");
    guideCalc=new Button(this);guideCalc.setText("HESAPLA VE 2D / 3D GÖSTER");guideCalc.setTextSize(18);guideCalc.setOnClickListener(v->runGuided());guided.addView(guideCalc,new LinearLayout.LayoutParams(-1,dp(68)));

    guided.addView(tx("SONUÇTA: tank üzerindeki station yükleri → pad → beam/pivot → load-cell → actuator → strongback kuvvet/moment/deplasman zinciri gösterilir.",9,true,Color.rgb(247,207,77)));
    root.addView(guided,0,lp());
  }

  void addStepTitle(String no,String title,String hint){
    LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(9),dp(10),dp(9));card.setBackground(bg(Color.rgb(12,38,55),10));
    card.addView(tx("ADIM "+no+"  —  "+title,15,true,Color.rgb(247,207,77)));
    card.addView(tx(hint,8,false,Color.rgb(185,210,228)));
    LinearLayout.LayoutParams cp=lp();cp.setMargins(0,dp(10),0,dp(4));guided.addView(card,cp);
  }

  EditText guidedField(String label,String def,boolean integer){
    TextView l=tx(label,11,true,Color.WHITE);guided.addView(l);
    EditText e=new EditText(this);e.setText(def);e.setTextColor(Color.WHITE);e.setTextSize(18);e.setSingleLine(true);e.setSelectAllOnFocus(true);
    e.setInputType(InputType.TYPE_CLASS_NUMBER | (integer?0:(InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED)));
    e.setBackground(bg(Color.rgb(25,58,80),9));e.setPadding(dp(12),0,dp(12),0);guided.addView(e,new LinearLayout.LayoutParams(-1,dp(52)));return e;
  }

  double gv(EditText e){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception ex){return 0;}}
  int gi(EditText e,int lo,int hi){return Math.max(lo,Math.min(hi,(int)Math.round(gv(e))));}

  void runGuided(){
    double L=gv(gLen),D=gv(gDia),fx=gv(gFx),fy=gv(gFy),fz=gv(gFz),cap=Math.max(1,gv(gActCap));
    int ns=gi(gSections,1,20),na=gi(gActs,1,12),nl=gi(gLayers,1,4);
    if(L<=0||D<=0){guideState.setText("GİRİŞ HATASI — Tank uzunluğu ve çapı sıfırdan büyük olmalı.");return;}
    qLength.setText(String.format(Locale.US,"%.3f",L));qDiameter.setText(String.format(Locale.US,"%.3f",D));
    qSections.setText(String.valueOf(Math.min(8,ns)));pSections.setText(String.valueOf(Math.min(8,ns)));
    pFx.setText(String.format(Locale.US,"%.3f",fx));pFy.setText(String.format(Locale.US,"%.3f",fy));pFz.setText(String.format(Locale.US,"%.3f",fz));
    pActs.setText(String.valueOf(na));pLayers.setText(String.valueOf(nl));pActType.setSelection(gActType.getSelectedItemPosition());qActType.setSelection(gActType.getSelectedItemPosition());qPhase.setSelection(gPhase.getSelectedItemPosition());
    for(int i=0;i<actKnown.length;i++)actKnown[i].setText(String.format(Locale.US,"%.0f",cap));
    runPrimary();
    guideState.setText(String.format(Locale.US,"GİRDİLER AKTARILDI VE HESAPLANDI\nL %.3f m • D %.3f m • %d yük bölümü • Fx %+.0f N • Fy %+.0f N • Fz %+.0f N\n%d actuator • %d layer • %s • %s\nAşağıdaki poster/2D görünümde kuvvet zincirini incele.",L,D,Math.min(8,ns),fx,fy,fz,na,nl,gActType.getSelectedItem().toString(),gPhase.getSelectedItem().toString()));
  }
}
