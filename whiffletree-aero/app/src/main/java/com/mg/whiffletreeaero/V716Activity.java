package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

/** v7.16 — per-section signed load definition drives the active whiffletree. */
public class V716Activity extends V715Activity {
  LinearLayout zoneEditor;
  Button buildZonesBtn, uniformBtn;
  TextView zoneSummary;
  ArrayList<EditText> zX=new ArrayList<>(), zFx=new ArrayList<>(), zFy=new ArrayList<>(), zFz=new ArrayList<>();
  boolean customZones=false;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    zoneSummary=card("SECTION LOADS: varsayılan olarak toplam Fx/Fy/Fz eşit dağıtılır. Gerçek test için her bölgeye ayrı signed yük girebilirsin.",Color.rgb(16,47,61));
    buildZonesBtn=designBtn("BÖLGE YÜKLERİNİ DÜZENLE",v->buildZoneEditor());
    uniformBtn=designBtn("EŞİT DAĞILIMA DÖN",v->{customZones=false;zoneEditor.removeAllViews();zoneSummary.setText("SECTION LOADS: eşit dağılım aktif.");});
    zoneEditor=new LinearLayout(this);zoneEditor.setOrientation(LinearLayout.VERTICAL);
    proHome.addView(zoneSummary,Math.min(11,proHome.getChildCount()),lp());
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.addView(buildZonesBtn,new LinearLayout.LayoutParams(0,dp(50),1));row.addView(uniformBtn,new LinearLayout.LayoutParams(0,dp(50),1));proHome.addView(row,Math.min(12,proHome.getChildCount()),lp());
    proHome.addView(zoneEditor,Math.min(13,proHome.getChildCount()),lp());
  }

  void buildZoneEditor(){
    int ns=ival(hSections,1,20);double L=Math.max(.001,val(hLen))*1000.0;double fx=val(hFx),fy=val(hFy),fz=val(hFz);
    zoneEditor.removeAllViews();zX.clear();zFx.clear();zFy.clear();zFz.clear();
    zoneEditor.addView(tx("Bölge   x [mm]       Fx [N]       Fy [N]       Fz [N]",9,true,Color.rgb(247,207,77)));
    for(int i=0;i<ns;i++){
      LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);
      TextView id=tx("S"+(i+1),9,true,Color.WHITE);r.addView(id,new LinearLayout.LayoutParams(dp(38),dp(48)));
      double x=-L/2.0+(i+.5)*L/ns;
      EditText ex=small(String.format(Locale.US,"%.1f",x));EditText efx=small(String.format(Locale.US,"%.1f",fx/ns));EditText efy=small(String.format(Locale.US,"%.1f",fy/ns));EditText efz=small(String.format(Locale.US,"%.1f",fz/ns));
      r.addView(ex,new LinearLayout.LayoutParams(0,dp(48),1));r.addView(efx,new LinearLayout.LayoutParams(0,dp(48),1));r.addView(efy,new LinearLayout.LayoutParams(0,dp(48),1));r.addView(efz,new LinearLayout.LayoutParams(0,dp(48),1));
      zX.add(ex);zFx.add(efx);zFy.add(efy);zFz.add(efz);zoneEditor.addView(r,lp());
    }
    customZones=true;zoneSummary.setText("SECTION LOADS: CUSTOM aktif — her S noktasının x/Fx/Fy/Fz değeri ana solver ve AUTO WHIFFLETREE tarafından kullanılacak.");
  }

  EditText small(String s){EditText e=new EditText(this);e.setText(s);e.setTextColor(Color.WHITE);e.setTextSize(12);e.setSingleLine(true);e.setSelectAllOnFocus(true);e.setGravity(Gravity.CENTER);return e;}
  double zv(EditText e){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception x){return Double.NaN;}}

  @Override void calculateProfessional(){
    super.calculateProfessional();
    if(!solvedValid || !customZones)return;
    int ns=ival(hSections,1,20);if(zFx.size()!=ns){buildZoneEditor();return;}
    double fac=testFactor(), stiffness=Math.max(1,qd(qStiffness)), gauge=Math.max(1,qd(qGaugeLength)), yoff=qd(qYoff),zoff=qd(qZoff);
    ArrayList<SNode> custom=new ArrayList<>();
    for(int i=0;i<ns;i++){
      double x=zv(zX.get(i)),fx=zv(zFx.get(i)),fy=zv(zFy.get(i)),fz=zv(zFz.get(i));
      if(Double.isNaN(x)||Double.isNaN(fx)||Double.isNaN(fy)||Double.isNaN(fz)){zoneSummary.setText("SECTION LOAD HATASI — tüm x/Fx/Fy/Fz alanlarını sayısal doldur.");return;}
      SNode s=new SNode();s.section=i;s.x=x;s.y=yoff;s.z=zoff;s.fx=fx*fac;s.fy=fy*fac;s.fz=fz*fac;s.r=Math.sqrt(s.fx*s.fx+s.fy*s.fy+s.fz*s.fz);s.mx=s.y*s.fz-s.z*s.fy;s.my=s.z*s.fx-s.x*s.fz;s.mz=s.x*s.fy-s.y*s.fx;s.disp=s.r/stiffness;s.strain=s.disp/gauge*1e6;s.lc=s.r;custom.add(s);
    }
    solved.clear();solved.addAll(custom);solvedValid=true;forcedActuatorGroups=null;
    buildSelectableCandidates();if(recommended!=null)applyDesign(recommended);else designAutomaticWhiffletree();
    double sx=0,sy=0,sz=0;for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;}
    hFx.setText(String.format(Locale.US,"%.3f",sx/fac));hFy.setText(String.format(Locale.US,"%.3f",sy/fac));hFz.setText(String.format(Locale.US,"%.3f",sz/fac));
    zoneSummary.setText(String.format(Locale.US,"CUSTOM SECTION LOADS ACTIVE — %d bölge\nΣFx %+.1f N   ΣFy %+.1f N   ΣFz %+.1f N\nAUTO WHIFFLETREE, A/B/C trade study ve aktif rig bu gerçek bölge yüklerinden yeniden hesaplandı.",ns,sx,sy,sz));
  }
}
