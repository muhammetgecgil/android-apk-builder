package com.mg.drawing2cad;

import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

/** v3.1 shell: adds a pre-reconstruction intelligence pass without changing the stable v3 core. */
public class SmartMainActivity extends MainActivity {
  TextView intelligenceBadge;
  TechnicalDrawingIntelligence.Report lastReport;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    addSmartOverlay();
  }

  void addSmartOverlay(){
    View root=getWindow().getDecorView().findViewById(android.R.id.content);
    if(!(root instanceof ViewGroup))return;
    FrameLayout overlay=new FrameLayout(this);
    Button smart=new Button(this);smart.setText("AKILLI OKU");smart.setTextColor(Color.WHITE);smart.setTextSize(11);smart.setAllCaps(false);
    GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(4,70,95));g.setCornerRadius(dp(10));g.setStroke(1,Color.rgb(55,205,255));smart.setBackground(g);
    FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(132),dp(44),Gravity.TOP|Gravity.RIGHT);bp.setMargins(0,dp(112),dp(16),0);overlay.addView(smart,bp);
    intelligenceBadge=new TextView(this);intelligenceBadge.setText("Teknik zeka: hazır");intelligenceBadge.setTextColor(Color.rgb(170,220,235));intelligenceBadge.setTextSize(10);intelligenceBadge.setGravity(Gravity.CENTER);intelligenceBadge.setBackgroundColor(Color.argb(190,4,20,32));
    FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(dp(230),dp(34),Gravity.TOP|Gravity.RIGHT);tp.setMargins(0,dp(160),dp(16),0);overlay.addView(intelligenceBadge,tp);
    ((ViewGroup)root).addView(overlay,new ViewGroup.LayoutParams(-1,-1));
    smart.setOnClickListener(v->runSmartAnalysis(true));
  }

  void runSmartAnalysis(boolean dialog){
    if(source==null){status.setText("Önce 2D teknik resim yükle.");return;}
    lastReport=TechnicalDrawingIntelligence.analyze(source);
    intelligenceBadge.setText(lastReport.summary());
    status.setText("Teknik zeka: "+lastReport.summary()+" • belirsiz bilgiler uydurulmaz.");
    if(dialog){
      StringBuilder s=new StringBuilder();s.append(lastReport.summary()).append("\n\n");
      int lim=Math.min(12,lastReport.features.size());
      for(int i=0;i<lim;i++){
        TechnicalDrawingIntelligence.Feature f=lastReport.features.get(i);
        s.append("• ").append(f.type).append("  güven ").append(Math.round(f.confidence*100)).append("%\n");
      }
      if(lastReport.features.size()>lim)s.append("… +").append(lastReport.features.size()-lim).append(" aday\n");
      s.append("\nNot: Bu sürüm metin OCR değerlerini uydurmaz; geometrik adayları güven skoruyla verir.");
      new android.app.AlertDialog.Builder(this).setTitle("Teknik Resim Zekası v3.1").setMessage(s.toString()).setPositiveButton("Tamam",null).show();
    }
  }

  @Override void createAction(){
    if(mode==M23 && source!=null)runSmartAnalysis(false);
    super.createAction();
  }
}
