package com.mg.drawing2cad;

import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.*;

/** v3.2 feature-aware shell. Keeps v3.1 technical intelligence and adds an
 * explicit feature graph plus manual callout confirmation for uncertain OCR. */
public class FeatureAwareActivityV32 extends SmartMainActivityV31 {
  FeatureGraphEngine.Graph featureGraph;
  TextView featureBadge;

  @Override public void onCreate(Bundle b){super.onCreate(b);addFeatureTools();}

  void addFeatureTools(){
    View root=getWindow().getDecorView().findViewById(android.R.id.content);if(!(root instanceof ViewGroup))return;
    FrameLayout layer=new FrameLayout(this);
    Button tree=new Button(this);tree.setText("FEATURE TREE");tree.setTextColor(Color.WHITE);tree.setTextSize(10);tree.setAllCaps(false);
    GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(45,74,30));g.setCornerRadius(dp(9));g.setStroke(1,GREEN);tree.setBackground(g);
    FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(dp(138),dp(42),Gravity.TOP|Gravity.RIGHT);tp.setMargins(0,dp(202),dp(14),0);layer.addView(tree,tp);
    Button callout=new Button(this);callout.setText("ÖLÇÜ ÇAĞRISI");callout.setTextColor(Color.WHITE);callout.setTextSize(9);callout.setAllCaps(false);
    GradientDrawable g2=new GradientDrawable();g2.setColor(Color.rgb(86,55,15));g2.setCornerRadius(dp(9));g2.setStroke(1,ORANGE);callout.setBackground(g2);
    FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(138),dp(42),Gravity.TOP|Gravity.RIGHT);cp.setMargins(0,dp(248),dp(14),0);layer.addView(callout,cp);
    featureBadge=txt("Feature graph —",9,Color.rgb(200,235,190),true);featureBadge.setGravity(Gravity.CENTER);featureBadge.setBackgroundColor(Color.argb(210,9,31,18));
    FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(300),dp(38),Gravity.TOP|Gravity.RIGHT);fp.setMargins(0,dp(294),dp(14),0);layer.addView(featureBadge,fp);
    ((ViewGroup)root).addView(layer,new ViewGroup.LayoutParams(-1,-1));
    tree.setOnClickListener(v->showFeatureTree());callout.setOnClickListener(v->manualCallout());
  }

  void rebuildGraph(String manual){
    if(lastReport==null && source!=null){lastReport=TechnicalDrawingIntelligence.analyze(source);}
    featureGraph=FeatureGraphEngine.withManualCallouts(lastReport,manual);
    featureBadge.setText(featureGraph.summary());
    status.setText(featureGraph.summary()+" • eksik ölçüler uydurulmadı");
  }

  void showFeatureTree(){
    if(mode!=MODE_2D3D){status.setText("Feature tree şu an 2D→3D teknik resim akışında kullanılır.");return;}
    rebuildGraph(null);
    StringBuilder s=new StringBuilder(featureGraph.summary()).append("\n\n");
    int n=Math.min(24,featureGraph.nodes.size());
    for(int i=0;i<n;i++){FeatureGraphEngine.Node x=featureGraph.nodes.get(i);s.append(x.id).append("  ").append(x.label()).append("\n");}
    if(featureGraph.nodes.size()>n)s.append("… +").append(featureGraph.nodes.size()-n).append(" feature\n");
    s.append("\nBu ağaç doğrulanmış değer ile aday değeri ayırır. Sayısal ölçü yoksa CAD motoru değer üretmez.");
    new android.app.AlertDialog.Builder(this).setTitle("Feature Tree v3.2").setMessage(s.toString()).setPositiveButton("Tamam",null).show();
  }

  void manualCallout(){
    if(mode!=MODE_2D3D){status.setText("Ölçü çağrısı 2D teknik resim modundadır.");return;}
    final EditText e=new EditText(this);e.setHint("Örn: 4x Ø8 THRU; R6; M10x1.5; C2x45°");e.setSingleLine(false);e.setMinLines(3);
    new android.app.AlertDialog.Builder(this).setTitle("Okunan / doğrulanan teknik çağrılar").setView(e).setPositiveButton("FEATURE'A DÖNÜŞTÜR",(d,w)->{rebuildGraph(e.getText().toString());showFeatureTree();}).setNegativeButton("İptal",null).show();
  }

  @Override void createAction(){
    if(mode==MODE_2D3D && source!=null){runTechnicalIntelligence(false);rebuildGraph(null);}
    super.createAction();
  }
}
