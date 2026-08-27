package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.util.*;

/** v7.19 — crash-safe simple AUTO WHIFFLETREE wizard with independent inputs. */
public class V719Activity extends V717Activity {
  LinearLayout wizard,body,resultBox;
  TextView title,status;
  EditText wLen,wDia,wSections,wFx,wFy,wFz,wActs,wCap,wLayers;
  Spinner wActType,wPhase;
  Button next,back,auto,advanced,pads;
  int step=1;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    proHome.setVisibility(View.GONE);
    wizard=new LinearLayout(this);wizard.setOrientation(LinearLayout.VERTICAL);wizard.setPadding(dp(14),dp(14),dp(14),dp(18));wizard.setBackground(bg(Color.rgb(5,24,36),16));
    root.addView(wizard,0,new LinearLayout.LayoutParams(-1,-2));
    title=tx("WHIFFLETREE TASARIMI",22,true,Color.WHITE);wizard.addView(title,lp());
    status=card("3 kısa adım. Yalnız bildiğin test gereksinimlerini gir; AUTO TASARLA whiffletree'yi kendi hesaplasın.",Color.rgb(12,49,65));wizard.addView(status,lp());
    body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);wizard.addView(body,lp());
    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    back=designBtn("GERİ",v->{if(step>1)showStep(step-1);});next=designBtn("DEVAM",v->{if(step<3)showStep(step+1);});
    nav.addView(back,new LinearLayout.LayoutParams(0,dp(54),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(54),1));wizard.addView(nav,lp());
    auto=designBtn("AUTO TASARLA VE GÖSTER",v->runWizardAuto());wizard.addView(auto,new LinearLayout.LayoutParams(-1,dp(64)));
    advanced=designBtn("GELİŞMİŞ EKRAN",v->{wizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);});wizard.addView(advanced,new LinearLayout.LayoutParams(-1,dp(46)));
    resultBox=new LinearLayout(this);resultBox.setOrientation(LinearLayout.VERTICAL);wizard.addView(resultBox,lp());
    initWizardValues();showStep(1);
  }

  EditText fresh(String value){EditText e=new EditText(this);e.setText(value);e.setTextColor(Color.WHITE);e.setTextSize(17);e.setSingleLine(true);e.setSelectAllOnFocus(true);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);e.setBackground(bg(Color.rgb(24,52,70),9));e.setPadding(dp(10),0,dp(10),0);return e;}
  LinearLayout field(String label,EditText e,String hint){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(10),dp(8),dp(10),dp(8));c.setBackground(bg(Color.rgb(12,40,54),12));c.addView(tx(label,12,true,Color.rgb(247,207,77)),lp());c.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));if(hint!=null&&!hint.isEmpty())c.addView(tx(hint,9,false,Color.rgb(178,205,220)),lp());return c;}
  LinearLayout selector(String label,Spinner s){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(10),dp(8),dp(10),dp(8));c.setBackground(bg(Color.rgb(12,40,54),12));c.addView(tx(label,12,true,Color.rgb(247,207,77)),lp());c.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));return c;}

  void initWizardValues(){
    wLen=fresh(hLen.getText().toString());wDia=fresh(hDia.getText().toString());wSections=fresh(hSections.getText().toString());
    wFx=fresh(hFx.getText().toString());wFy=fresh(hFy.getText().toString());wFz=fresh(hFz.getText().toString());
    wActs=fresh(hActs.getText().toString());wCap=fresh(hCap.getText().toString());wLayers=fresh(hLayers.getText().toString());
    wActType=new Spinner(this);wActType.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Hidrolik","Elektrikli"}));wActType.setSelection(Math.min(1,hActType.getSelectedItemPosition()));
    wPhase=new Spinner(this);wPhase.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"LIMIT","ULTIMATE","UNLOAD"}));wPhase.setSelection(Math.min(2,hPhase.getSelectedItemPosition()));
  }

  void showStep(int s){
    step=s;body.removeAllViews();resultBox.removeAllViews();back.setVisibility(s==1?View.INVISIBLE:View.VISIBLE);next.setVisibility(s==3?View.GONE:View.VISIBLE);auto.setVisibility(s==3?View.VISIBLE:View.GONE);
    if(s==1){
      title.setText("1/3 • EFT VE HEDEF YÜKLER");status.setText("Tankı ve yük uygulama noktalarını tanımla. Negatif kuvvet girebilirsin.");
      body.addView(field("EFT uzunluğu [m]",wLen,"Tank boyu"));body.addView(field("EFT çapı [m]",wDia,"Maksimum dış çap"));body.addView(field("Yük uygulama bölgesi / pad sayısı",wSections,"Örn. 8, 10, 12"));
      body.addView(field("Toplam Fx [N]",wFx,"+ / − işaretli"));body.addView(field("Toplam Fy [N]",wFy,"+ / − işaretli"));body.addView(field("Toplam Fz [N]",wFz,"+ / − işaretli"));
      pads=designBtn("PAD YÜKLERİ EŞİT DEĞİL → TEK TEK GİR",v->{syncWizardToCore();wizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);buildZoneEditor();});body.addView(pads,new LinearLayout.LayoutParams(-1,dp(58)));
    } else if(s==2){
      title.setText("2/3 • ELİNDEKİ TEST SİSTEMİ");status.setText("Bildiğin fiziksel sınırları gir. AUTO tasarım bu sınırları aşarsa açıkça uyaracak.");
      body.addView(field("Actuator sayısı",wActs,"Elindeki adet; bilmiyorsan mevcut değeri bırak"));body.addView(field("Actuator kapasitesi [N]",wCap,"Bir actuator nominal kapasitesi"));body.addView(field("Maksimum whiffletree layer",wLayers,"Tasarım kısıtı; 1–4"));body.addView(selector("Actuator tipi",wActType));body.addView(selector("Test seviyesi",wPhase));
    } else {
      title.setText("3/3 • AUTO TASARLA");status.setText("Hazır. Program eşit bölme varsayımına bağlı kalmadan actuator gruplarını, beam/pivot oranlarını ve kuvvet yolunu hesaplayacak.");
      body.addView(card(summaryBeforeAuto(),Color.rgb(16,48,60)),lp());
    }
  }

  String summaryBeforeAuto(){return String.format(Locale.US,"GİRDİ ÖZETİ\nEFT %s m × %s m • %s pad\nFx %s N • Fy %s N • Fz %s N\n%s actuator × %s N • max %s layer\n%s • %s",wLen.getText(),wDia.getText(),wSections.getText(),wFx.getText(),wFy.getText(),wFz.getText(),wActs.getText(),wCap.getText(),wLayers.getText(),wActType.getSelectedItem(),wPhase.getSelectedItem());}

  void syncWizardToCore(){
    hLen.setText(wLen.getText());hDia.setText(wDia.getText());hSections.setText(wSections.getText());hFx.setText(wFx.getText());hFy.setText(wFy.getText());hFz.setText(wFz.getText());hActs.setText(wActs.getText());hCap.setText(wCap.getText());hLayers.setText(wLayers.getText());
    hActType.setSelection(Math.min(hActType.getCount()-1,wActType.getSelectedItemPosition()));hPhase.setSelection(Math.min(hPhase.getCount()-1,wPhase.getSelectedItemPosition()));
  }

  void runWizardAuto(){
    syncWizardToCore();calculateProfessional();resultBox.removeAllViews();
    if(!solvedValid){status.setText("HESAP YAPILAMADI — giriş değerlerini kontrol et.");return;}
    title.setText("AUTO WHIFFLETREE SONUCU");status.setText("Tasarım tamamlandı. Önce önerilen rig ve kuvvetleri gör; ayrıntılar alt düğmelerde.");
    TextView primary=card(primaryResult(),Color.rgb(12,57,52));resultBox.addView(primary,lp());
    LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
    actions.addView(designBtn("2D RIG",v->openPanel(visualGuide,navRig)),new LinearLayout.LayoutParams(0,dp(58),1));actions.addView(designBtn("İSPAT",v->openPanel(matrixPanel,navProof)),new LinearLayout.LayoutParams(0,dp(58),1));actions.addView(designBtn("PARÇA",v->openPanel(equipPanel,navPart)),new LinearLayout.LayoutParams(0,dp(58),1));actions.addView(designBtn("TEST",v->openPanel(simPanel,navTest)),new LinearLayout.LayoutParams(0,dp(58),1));resultBox.addView(actions,lp());
    resultBox.addView(designBtn("ALTERNATİF / GELİŞMİŞ TASARIMLAR",v->{wizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);}),new LinearLayout.LayoutParams(-1,dp(48)));
  }

  void openPanel(View panel,Button nav){wizard.setVisibility(View.GONE);proHome.setVisibility(View.VISIBLE);showSection(panel,nav);}

  String primaryResult(){
    int na=Math.max(1,ival(hActs,1,12));double cap=Math.max(1,val(hCap));double sx=0,sy=0,sz=0;double[] ax=new double[na],ay=new double[na],az=new double[na];for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;int a=Math.max(0,Math.min(na-1,s.act));ax[a]+=s.fx;ay[a]+=s.fy;az[a]+=s.fz;}
    double peak=0,maxU=0;StringBuilder atext=new StringBuilder();for(int a=0;a<na;a++){double r=Math.sqrt(ax[a]*ax[a]+ay[a]*ay[a]+az[a]*az[a]);peak=Math.max(peak,r);maxU=Math.max(maxU,r/cap);if(a>0)atext.append("   ");atext.append("ACT-").append(a+1).append(": ").append(String.format(Locale.US,"%.1f kN",r/1000.0));}
    String rec=recommended==null?"AUTO":("Design "+recommended.id+" • "+recommended.name);String ok=maxU<=1&&wtForceResidual<1e-3?"UYGUN / DETAY KONTROLÜNE GEÇ":"KONTROL GEREKLİ";
    return String.format(Locale.US,"%s\n\nÖNERİLEN WHIFFLETREE: %s\n%d pad → %d beam/pivot → %d actuator\nKatman: %d\n\nHEDEF YÜK\nFx %+.2f kN   Fy %+.2f kN   Fz %+.2f kN\n\nACTUATOR KUVVETLERİ\n%s\nPeak %.2f kN / kapasite %.2f kN → kullanım %.1f%%\n\nDENGE KONTROLÜ\nForce closure %.6f N\nWorst beam ΣM residual %.6f Nmm\n\nWHIFFLETREE'Yİ GÖSTER ile her pad, beam, pivot, load-cell ve actuator üzerindeki kuvveti incele.",ok,rec,solved.size(),wtBeams.size(),na,Math.max(1,ival(hLayers,1,4)),sx/1000.0,sy/1000.0,sz/1000.0,atext.toString(),peak/1000.0,cap/1000.0,100*maxU,wtForceResidual,wtMomentResidual);
  }

  @Override public void onBackPressed(){if(wizard!=null&&wizard.getVisibility()!=View.VISIBLE){proHome.setVisibility(View.GONE);wizard.setVisibility(View.VISIBLE);return;}super.onBackPressed();}
}
