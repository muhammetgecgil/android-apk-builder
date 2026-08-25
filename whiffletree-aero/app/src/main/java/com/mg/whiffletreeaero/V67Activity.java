package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V67Activity extends V66Activity {
  EditText pSections;
  TextView sectionInfo;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    pSections=new EditText(this);
    pSections.setText(qSections.getText().toString());
    pSections.setTextColor(Color.WHITE);pSections.setTextSize(19);pSections.setSingleLine(true);
    pSections.setInputType(InputType.TYPE_CLASS_NUMBER);
    pSections.setBackground(bg(Color.rgb(24,52,73),9));pSections.setPadding(dp(12),0,dp(12),0);
    TextView lab=tx("YÜK UYGULAMA BÖLÜM SAYISI",12,true,Color.rgb(190,215,232));
    int idx=Math.max(0,primaryPanel.indexOfChild(pActs));
    primaryPanel.addView(lab,idx);
    primaryPanel.addView(pSections,idx+1,new LinearLayout.LayoutParams(-1,dp(50)));
    sectionInfo=card("Bölüm sayısını gir (1–8). Her bölüm EFT üzerinde ayrı yük uygulama noktası olarak çizilir ve hesaplanır.",Color.rgb(15,49,69));
    primaryPanel.addView(sectionInfo,idx+2,lp());
    refreshPrimaryPreview();
  }

  int sectionCount(){return Math.max(1,Math.min(8,(int)Math.round(pv(pSections))));}

  @Override void runPrimary(){
    int n=sectionCount();qSections.setText(String.valueOf(n));
    super.runPrimary();
    sectionInfo.setText(String.format(Locale.US,"%d YÜK UYGULAMA BÖLÜMÜ AKTİF\nZ1–Z%d EFT üzerinde ayrı hesaplanır. 2D sonuç ekranında her bölümün signed Fx/Fy/Fz değerleri gösterilir.",n,n));
  }

  @Override void refreshPrimaryPreview(){super.refreshPrimaryPreview();}

  @Override void calculateAndShow(){
    if(pSections!=null)qSections.setText(String.valueOf(sectionCount()));
    super.calculateAndShow();
  }
}
