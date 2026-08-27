package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.*;

/** v7.18 — simple first-screen AUTO WHIFFLETREE wizard. */
public class V718Activity extends V717Activity {
  LinearLayout simpleWizard, simpleInputs, simpleResult;
  TextView stepTitle, simpleStatus;
  Button nextBtn, autoBtn, advancedBtn;
  int step=1;

  @Override public void onCreate(Bundle b){
    super.onCreate(b); proHome.setVisibility(View.GONE);
    simpleWizard=new LinearLayout(this); simpleWizard.setOrientation(LinearLayout.VERTICAL); simpleWizard.setPadding(dp(14),dp(14),dp(14),dp(14)); simpleWizard.setBackground(bg(Color.rgb(6,25,37),16));
    root.addView(simpleWizard,0,new LinearLayout.LayoutParams(-1,-2));
    stepTitle=tx("WHIFFLETREE TASARIMI — ADIM 1/3",20,true,Color.WHITE); simpleWizard.addView(stepTitle,lp());
    simpleStatus=card("Sadece aşağıdaki soruları cevapla. AUTO TASARLA dediğinde uygulama topoloji, actuator grupları, beam/pivot oranları ve kuvvetleri hesaplayacak.",Color.rgb(12,49,65)); simpleWizard.addView(simpleStatus,lp());
    simpleInputs=new LinearLayout(this);simpleInputs.setOrientation(LinearLayout.VERTICAL);simpleWizard.addView(simpleInputs,lp());
    nextBtn=designBtn("DEVAM",v->nextStep()); autoBtn=designBtn("AUTO TASARLA VE GÖSTER",v->runAuto()); advancedBtn=designBtn("GELİŞMİŞ EKRAN",v->{simpleWizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);});
    simpleWizard.addView(nextBtn,new LinearLayout.LayoutParams(-1,dp(58)));simpleWizard.addView(autoBtn,new LinearLayout.LayoutParams(-1,dp(62)));simpleWizard.addView(advancedBtn,new LinearLayout.LayoutParams(-1,dp(48)));
    simpleResult=new LinearLayout(this);simpleResult.setOrientation(LinearLayout.VERTICAL);simpleWizard.addView(simpleResult,lp()); showStep(1);
  }

  void showStep(int s){step=s;simpleInputs.removeAllViews();simpleResult.removeAllViews();stepTitle.setText("WHIFFLETREE TASARIMI — ADIM "+s+"/3");autoBtn.setVisibility(s==3?View.VISIBLE:View.GONE);nextBtn.setVisibility(s<3?View.VISIBLE:View.GONE);
    if(s==1){simpleStatus.setText("1 • TEST PARÇASI VE YÜKLER\nÖnce tankı ve kaç noktadan yük uygulayacağını tanımla.");simpleInputs.addView(fieldCard("EFT uzunluğu [m]",hLen));simpleInputs.addView(fieldCard("EFT çapı [m]",hDia));simpleInputs.addView(fieldCard("Yük uygulama bölgesi / pad sayısı",hSections));Button zones=designBtn("HER PAD İÇİN x / Fx / Fy / Fz GİR",v->{proHome.setVisibility(View.VISIBLE);buildZoneEditor();simpleWizard.setVisibility(View.GONE);});simpleInputs.addView(zones,new LinearLayout.LayoutParams(-1,dp(58)));simpleInputs.addView(tx("Yüklerin eşitse toplam Fx/Fy/Fz değerlerini kullanabilirsin. Eşit değilse PAD butonundan her noktayı ayrı gir.",11,false,Color.rgb(185,215,232)));simpleInputs.addView(fieldCard("Toplam Fx [N] (+/−)",hFx));simpleInputs.addView(fieldCard("Toplam Fy [N] (+/−)",hFy));simpleInputs.addView(fieldCard("Toplam Fz [N] (+/−)",hFz));}
    else if(s==2){simpleStatus.setText("2 • ELİNDEKİ TEST SİSTEMİ SINIRLARI\nWhiffletree bu sınırlar içinde otomatik tasarlanacak.");simpleInputs.addView(fieldCard("Actuator sayısı",hActs));simpleInputs.addView(fieldCard("Bir actuator kapasitesi [N]",hCap));simpleInputs.addView(fieldCard("Maksimum whiffletree layer",hLayers));simpleInputs.addView(labeledView("Actuator tipi",hActType));simpleInputs.addView(labeledView("Test seviyesi",hPhase));}
    else {simpleStatus.setText("3 • HAZIR\nAUTO TASARLA yükleri okuyup gerekli lever-arm/pivot oranlarını hesaplayacak.");simpleInputs.addView(tx(String.format(Locale.US,"EFT %.2f m × %.2f m\n%d pad • Fx %+.0f N • Fy %+.0f N • Fz %+.0f N\n%d actuator × %.0f N • max %d layer • %s",val(hLen),val(hDia),ival(hSections,1,20),val(hFx),val(hFy),val(hFz),ival(hActs,1,12),val(hCap),ival(hLayers,1,4),hActType.getSelectedItem().toString()),14,true,Color.WHITE));}}

  LinearLayout fieldCard(String label,EditText e){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(10),dp(8),dp(10),dp(8));c.setBackground(bg(Color.rgb(14,43,57),12));c.addView(tx(label,12,true,Color.rgb(247,207,77)),lp());if(e.getParent()!=null)((android.view.ViewGroup)e.getParent()).removeView(e);c.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));return c;}
  LinearLayout labeledView(String label,View v){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(10),dp(8),dp(10),dp(8));c.setBackground(bg(Color.rgb(14,43,57),12));c.addView(tx(label,12,true,Color.rgb(247,207,77)),lp());if(v.getParent()!=null)((android.view.ViewGroup)v.getParent()).removeView(v);c.addView(v,new LinearLayout.LayoutParams(-1,dp(48)));return c;}
  void nextStep(){if(step<3)showStep(step+1);}
  void addCopy(String title,String body,int color){TextView v=card(title+"\n"+body,color);simpleResult.addView(v,lp());}

  void runAuto(){
    try{
      calculateProfessional(); simpleResult.removeAllViews();
      if(!solvedValid||solved==null||solved.isEmpty()){simpleStatus.setText("HESAP YAPILAMADI — girişleri kontrol et.");return;}
      simpleStatus.setText("AUTO WHIFFLETREE TAMAMLANDI");
      // Never move TextViews already attached to the professional dashboard; Android throws IllegalStateException.
      addCopy("AUTO TASARIM SONUCU",autoResultSummary==null?"Çözüm üretildi.":autoResultSummary.getText().toString(),Color.rgb(15,45,56));
      addCopy("AKTİF TASARIM",activeDesignSummary==null?"Önerilen tasarım aktif edildi.":activeDesignSummary.getText().toString(),Color.rgb(15,49,61));
      addCopy("RIG ELEMAN KONTROLÜ",componentCheckSummary==null?"Kapasite kontrolü tamamlandı.":componentCheckSummary.getText().toString(),Color.rgb(28,45,48));
      LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
      actions.addView(designBtn("2D RIG",v->{simpleWizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);showSection(visualGuide,navRig);}),new LinearLayout.LayoutParams(0,dp(56),1));actions.addView(designBtn("İSPAT",v->{simpleWizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);showSection(matrixPanel,navProof);}),new LinearLayout.LayoutParams(0,dp(56),1));actions.addView(designBtn("PARÇA",v->{simpleWizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);showSection(equipPanel,navPart);}),new LinearLayout.LayoutParams(0,dp(56),1));actions.addView(designBtn("TEST",v->{simpleWizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);showSection(simPanel,navTest);}),new LinearLayout.LayoutParams(0,dp(56),1));simpleResult.addView(actions,lp());
    }catch(Throwable e){simpleResult.removeAllViews();simpleStatus.setText("AUTO TASARIM HATASI\n"+e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage())+"\nGirdiler korunuyor; GELİŞMİŞ EKRAN'dan devam edebilirsin.");}
  }
}
