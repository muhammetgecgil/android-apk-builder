package com.mg.drawing2cad;

import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

/** v3.1 technical-intelligence shell layered on the stable v3 requirements-driven core. */
public class SmartMainActivityV31 extends MainActivityV3 {
  TextView intelligenceBadge;
  TechnicalDrawingIntelligence.Report lastReport;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    addSmartTools();
  }

  void addSmartTools(){
    View root=getWindow().getDecorView().findViewById(android.R.id.content);
    if(!(root instanceof ViewGroup))return;
    FrameLayout overlay=new FrameLayout(this);
    Button smart=new Button(this);smart.setText("AKILLI OKU");smart.setTextColor(Color.WHITE);smart.setTextSize(10);smart.setAllCaps(false);
    GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(7,76,104));g.setCornerRadius(dp(9));g.setStroke(1,CYAN);smart.setBackground(g);
    FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(128),dp(42),Gravity.TOP|Gravity.RIGHT);bp.setMargins(0,dp(112),dp(14),0);overlay.addView(smart,bp);
    intelligenceBadge=txt("Teknik zeka —",9,Color.rgb(180,225,240),true);intelligenceBadge.setGravity(Gravity.CENTER);intelligenceBadge.setBackgroundColor(Color.argb(205,4,20,32));
    FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(280),dp(36),Gravity.TOP|Gravity.RIGHT);ip.setMargins(0,dp(158),dp(14),0);overlay.addView(intelligenceBadge,ip);
    ((ViewGroup)root).addView(overlay,new ViewGroup.LayoutParams(-1,-1));
    smart.setOnClickListener(v->runTechnicalIntelligence(true));
  }

  void runTechnicalIntelligence(boolean dialog){
    if(mode!=MODE_2D3D){status.setText("AKILLI OKU 2D teknik resim modunda kullanılır.");return;}
    if(source==null){status.setText("Önce 2D teknik resim yükle.");return;}
    lastReport=TechnicalDrawingIntelligence.analyze(source);
    int conf=Math.round(lastReport.geometryConfidence*100f);
    intelligenceBadge.setText("Zeka "+conf+"% • "+lastReport.circularCandidates+" delik/daire adayı");
    qualityInfo.setText(RequirementRegistry.summary()+"\nGÜVEN "+conf+"% • Teknik yorum aktif");
    status.setText("Teknik zeka: "+lastReport.summary()+" • belirsiz ölçü uydurulmaz.");
    if(dialog){
      StringBuilder s=new StringBuilder(lastReport.summary()).append("\n\nFeature adayları:\n");
      int lim=Math.min(14,lastReport.features.size());
      for(int i=0;i<lim;i++){
        TechnicalDrawingIntelligence.Feature f=lastReport.features.get(i);
        s.append("• ").append(f.type).append(" — ").append(Math.round(f.confidence*100f)).append("%\n");
      }
      if(lastReport.features.size()>lim)s.append("… +").append(lastReport.features.size()-lim).append(" aday\n");
      s.append("\nØ/R/C/THRU/M ve tolerans çağrıları metin katmanından geldiğinde semantik parser ile sınıflandırılır. Okunmayan değer uydurulmaz.");
      new android.app.AlertDialog.Builder(this).setTitle("Technical Intelligence v3.1").setMessage(s.toString()).setPositiveButton("Tamam",null).show();
    }
  }

  @Override void createAction(){
    if(mode==MODE_2D3D && source!=null)runTechnicalIntelligence(false);
    super.createAction();
  }
}
