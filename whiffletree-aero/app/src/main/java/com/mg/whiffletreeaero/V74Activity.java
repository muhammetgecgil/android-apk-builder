package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import java.util.*;

public class V74Activity extends V73Activity {
  LinearLayout equipPanel;
  TextView equipSummary, bomSummary, materialSummary;
  Spinner beamMaterial, pinMaterial;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    equipPanel=new LinearLayout(this);equipPanel.setOrientation(LinearLayout.VERTICAL);equipPanel.setPadding(dp(10),dp(10),dp(10),dp(10));equipPanel.setBackground(bg(Color.rgb(2,16,27),16));
    equipPanel.addView(tx("REAL EQUIPMENT SELECTOR — FİZİKSEL EKİPMAN ÖNERİSİ",20,true,Color.WHITE));
    equipPanel.addView(tx("Aktif test hesabındaki actuator/load-cell talebini gerçek üretici seri aralıklarıyla eşleştirir. Nihai sipariş öncesi güncel üretici datasheet'i ve test şartları doğrulanmalıdır.",9,false,Color.rgb(180,210,230)));
    equipSummary=card("HESAPLA VE GÖSTER sonrası gerçek ekipman adayları otomatik önerilir.",Color.rgb(14,45,64));equipPanel.addView(equipSummary,lp());

    equipPanel.addView(tx("WHIFFLETREE BEAM MALZEMESİ",11,true,Color.rgb(247,207,77)));
    beamMaterial=new Spinner(this);beamMaterial.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"7075-T6 Aluminum","6061-T6 Aluminum","42CrMo4 / 4140 Steel","17-4PH Stainless"}));equipPanel.addView(beamMaterial,new LinearLayout.LayoutParams(-1,dp(50)));
    equipPanel.addView(tx("PIN / CLEVIS MALZEMESİ",11,true,Color.rgb(247,207,77)));
    pinMaterial=new Spinner(this);pinMaterial.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"42CrMo4 / 4140 Steel","17-4PH Stainless","7075-T6 Aluminum"}));equipPanel.addView(pinMaterial,new LinearLayout.LayoutParams(-1,dp(50)));

    materialSummary=card("Malzeme ön seçimi hesap sonrası peak load ve moment ile birlikte gösterilir.",Color.rgb(13,39,54));equipPanel.addView(materialSummary,lp());
    bomSummary=card("Gerçek ekipman adaylı BOM hesap sonrası oluşur.",Color.rgb(13,39,54));equipPanel.addView(bomSummary,lp());
    root.addView(equipPanel,4,lp());
  }

  @Override void runGuided(){super.runGuided();refreshEquipment();}
  @Override void runPrimary(){super.runPrimary();refreshEquipment();}
  @Override void calculateAndShow(){super.calculateAndShow();refreshEquipment();}

  void refreshEquipment(){
    if(equipSummary==null)return;
    if(!solvedValid||solved==null||solved.isEmpty()){
      equipSummary.setText("Öneri hazır değil — önce HESAPLA VE GÖSTER.");return;
    }
    int na=Math.max(1,qi(qActs,1,12));double peakAct=0,peakLC=0,maxM=0;
    double[] ar=new double[na];
    for(int a=0;a<na;a++){double fx=0,fy=0,fz=0;for(SNode s:solved)if(s.act==a){fx+=s.fx;fy+=s.fy;fz+=s.fz;}ar[a]=Math.sqrt(fx*fx+fy*fy+fz*fz);peakAct=Math.max(peakAct,ar[a]);}
    peakLC=peakAct*1.25;
    for(SNode s:solved){maxM=Math.max(maxM,Math.sqrt(s.mx*s.mx+s.my*s.my+s.mz*s.mz));}

    boolean hyd=qActType.getSelectedItemPosition()==0;
    String actuator;
    if(hyd){
      if(peakAct<=100000) actuator="MTS Series 244 hydraulic actuator — standard family up to 100 kN class; standard strokes 100–500 mm";
      else if(peakAct<=1000000) actuator="MTS Series 244 hydraulic actuator — family spans approximately 15–1000 kN; choose force/stroke variant from current catalog";
      else actuator="Custom/high-force hydraulic actuator required — calculated demand exceeds the standard Series 244 family range used by this selector";
    }else{
      if(peakAct<=72300) actuator="Moog Standard Electric Linear Servoactuator — Size 5 class, peak force up to 72.3 kN; 180/300 mm stroke options in published family";
      else if(peakAct<=115600) actuator="Moog Flexible Electric Linear Servoactuator — Size 6 family, published peak force up to 115.6 kN";
      else actuator="Electric actuator demand exceeds the Moog standard/flexible family limits used by this selector; use hydraulic or custom multi-actuator architecture";
    }
    String lc=peakLC<=2500000?"HBK U10M force transducer — tensile/compressive family 1.25 kN to 2.5 MN; select nearest rated capacity above design demand":"Load-cell demand exceeds HBK U10M 2.5 MN family range used by this selector";

    equipSummary.setText(String.format(Locale.US,"CALCULATED EQUIPMENT DEMAND\nPeak actuator demand %.0f N\nRecommended design load-cell capacity ≥ %.0f N (1.25× demand)\n\nACTUATOR CANDIDATE\n%s\n\nLOAD-CELL CANDIDATE\n%s\n\nSelection status: PRELIMINARY — verify current datasheet, fatigue rating, stroke, speed, attachment, temperature and test duty cycle before release.",peakAct,peakLC,actuator,lc));

    String bm=beamMaterial.getSelectedItem().toString(),pm=pinMaterial.getSelectedItem().toString();
    String bnote=bm.startsWith("7075")?"High specific strength; check bearing/fatigue/corrosion and actual temper allowables":(bm.startsWith("6061")?"Good fabrication/corrosion behavior; lower strength, usually larger section required":(bm.startsWith("42")?"High strength steel; heavier but strong for compact beams/joints":"Corrosion resistant high-strength option; verify heat treatment and fatigue data"));
    String pnote=pm.startsWith("42")?"Preferred preliminary pin/clevis family for high shear/bearing loads; verify heat treatment":(pm.startsWith("17")?"Good corrosion-resistant pin candidate; verify condition and fatigue allowables":"Use cautiously for highly loaded pins; steel is normally preferred for compact joints");
    materialSummary.setText(String.format(Locale.US,"MATERIAL PRE-SELECTION\nWhiffletree beam: %s\n%s\nPin / clevis: %s\n%s\nPeak reconstructed moment %.0f Nmm\nMaterial choice changes allowable stress, section size, mass and margin. Final values require approved material allowables.",bm,bnote,pm,pnote,maxM));

    StringBuilder bom=new StringBuilder("REAL-EQUIPMENT CANDIDATE BOM\n");
    bom.append(String.format(Locale.US,"• %d × actuator — %s\n",na,hyd?"MTS Series 244 family / force-stroke variant by demand":"Moog Electric Linear Servoactuator family / size by demand"));
    bom.append(String.format(Locale.US,"• %d × force transducer — HBK U10M family, rated capacity ≥ %.0f N\n",na,peakLC));
    bom.append(String.format(Locale.US,"• Whiffletree beams — %s, section to be sized from calculated bending/shear\n",bm));
    bom.append(String.format(Locale.US,"• Pivot pins / clevises — %s, diameter from shear + bearing + bending checks\n",pm));
    bom.append("• Spherical bearings / rod ends — catalog part to be selected from radial load, misalignment angle and fatigue duty\n");
    bom.append("• Load pads / saddles — geometry from tank contact pressure and local structural limits\n");
    bom.append("• Strongback / base — structural frame sized from summed actuator reactions and moments\n");
    bom.append("\nPhysical product recommendation is a candidate shortlist, not a release approval. Current manufacturer datasheets govern.");
    bomSummary.setText(bom.toString());
  }
}
