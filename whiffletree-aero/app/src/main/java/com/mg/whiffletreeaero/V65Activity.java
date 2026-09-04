package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V65Activity extends V64Activity {
  EditText pFx,pFy,pFz,pActs,pLayers;
  Spinner pActType;
  TextView primarySummary;
  Button primaryCalc,advancedToggle;
  LinearLayout primaryPanel;
  boolean advancedShown=false;
  PrimaryTreeView primaryTree;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    primaryPanel=new LinearLayout(this);primaryPanel.setOrientation(LinearLayout.VERTICAL);primaryPanel.setPadding(dp(10),dp(10),dp(10),dp(10));primaryPanel.setBackground(bg(Color.rgb(3,20,32),14));
    primaryPanel.addView(tx("EFT XYZ WHIFFLETREE DESIGN — ANA EKRAN",21,true,Color.WHITE));
    primaryPanel.addView(tx("1) Fx/Fy/Fz gir  2) Actuator sayısını seç  3) Layer seç  4) HESAPLA VE 2D/3D GÖSTER",9,false,Color.rgb(180,210,230)));
    primarySummary=card("Signed kuvvetleri gir. Negatif değer serbesttir.",Color.rgb(15,49,69));primaryPanel.addView(primarySummary,lp());

    pFx=mainField("Fx [N]","0");
    pFy=mainField("Fy [N]","0");
    pFz=mainField("Fz [N]",String.format(Locale.US,"%.0f",qd(qFz)));
    pActs=mainField("ACTUATOR SAYISI",qActs.getText().toString());
    pLayers=mainField("WHIFFLETREE LAYER",qLayers.getText().toString());

    primaryPanel.addView(tx("ACTUATOR TİPİ",11,true,Color.rgb(185,210,230)));
    pActType=new Spinner(this);pActType.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"HYDRAULIC","ELECTRIC"}));pActType.setSelection(qActType.getSelectedItemPosition());primaryPanel.addView(pActType,new LinearLayout.LayoutParams(-1,dp(50)));

    primaryCalc=new Button(this);primaryCalc.setText("HESAPLA VE 2D / 3D GÖSTER");primaryCalc.setTextSize(18);primaryCalc.setOnClickListener(v->runPrimary());primaryPanel.addView(primaryCalc,new LinearLayout.LayoutParams(-1,dp(64)));

    advancedToggle=new Button(this);advancedToggle.setText("GELİŞMİŞ AYARLAR AŞAĞIDA");advancedToggle.setOnClickListener(v->{advancedShown=!advancedShown;advancedToggle.setText(advancedShown?"GELİŞMİŞ AYARLAR AÇIK":"GELİŞMİŞ AYARLAR AŞAĞIDA");Toast.makeText(this,advancedShown?"Detaylı zone/pad/strain alanları aşağıda":"Ana ekran öncelikli",Toast.LENGTH_SHORT).show();});primaryPanel.addView(advancedToggle,lp());

    primaryTree=new PrimaryTreeView();primaryPanel.addView(primaryTree,new LinearLayout.LayoutParams(-1,dp(760)));
    root.addView(primaryPanel,0,lp());
    refreshPrimaryPreview();
  }

  EditText mainField(String label,String def){
    primaryPanel.addView(tx(label,12,true,Color.rgb(190,215,232)));
    EditText e=new EditText(this);e.setText(def);e.setTextColor(Color.WHITE);e.setTextSize(19);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);e.setBackground(bg(Color.rgb(24,52,73),9));e.setPadding(dp(12),0,dp(12),0);primaryPanel.addView(e,new LinearLayout.LayoutParams(-1,dp(50)));return e;
  }

  double pv(EditText e){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception ex){return 0;}}

  void runPrimary(){
    int acts=Math.max(1,Math.min(12,(int)Math.round(pv(pActs))));
    int lays=Math.max(1,Math.min(4,(int)Math.round(pv(pLayers))));
    double fx=pv(pFx),fy=pv(pFy),fz=pv(pFz);
    qFx.setText(String.format(Locale.US,"%.3f",fx));qFy.setText(String.format(Locale.US,"%.3f",fy));qFz.setText(String.format(Locale.US,"%.3f",fz));
    qActs.setText(String.valueOf(acts));qLayers.setText(String.valueOf(lays));qActType.setSelection(pActType.getSelectedItemPosition());
    actCount.setText(String.valueOf(acts));layers.setText(String.valueOf(Math.min(3,lays)));
    seedZones();
    calculateAndShow();
    refreshPrimaryPreview();
    primarySummary.setText(String.format(Locale.US,"CALCULATED\nFx %+.1f N | Fy %+.1f N | Fz %+.1f N | R %.1f N\n%d actuator • %d layer • %s\nAşağıdaki şemada her layer ve actuator load-path görülür. Detaylı 2D/3D node değerleri hemen devamındaki ekranlardadır.",fx,fy,fz,Math.sqrt(fx*fx+fy*fy+fz*fz),acts,lays,pActType.getSelectedItemPosition()==0?"HYDRAULIC":"ELECTRIC"));
  }

  void refreshPrimaryPreview(){if(primaryTree!=null)primaryTree.invalidate();}

  class PrimaryTreeView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    PrimaryTreeView(){super(V65Activity.this);setBackgroundColor(Color.rgb(1,11,19));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    void line(Canvas c,float x1,float y1,float x2,float y2,int col,int w){p.setColor(col);p.setStrokeWidth(dp(w));c.drawLine(x1,y1,x2,y2,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int W=getWidth();int acts=Math.max(1,Math.min(12,(int)Math.round(pv(pActs))));int lays=Math.max(1,Math.min(4,(int)Math.round(pv(pLayers))));double fx=pv(pFx),fy=pv(pFy),fz=pv(pFz),r=Math.sqrt(fx*fx+fy*fy+fz*fz);t.setColor(Color.WHITE);t.setTextSize(dp(13));c.drawText("AUTO WHIFFLETREE TOPOLOGY — XYZ",dp(14),dp(28),t);t.setTextSize(dp(7));t.setColor(Color.rgb(180,205,222));c.drawText(String.format(Locale.US,"Fx %+.0f  Fy %+.0f  Fz %+.0f N   |   %d actuator / %d layer",fx,fy,fz,acts,lays),dp(14),dp(50),t);
      float left=dp(28),right=W-dp(28),tankY=dp(115);p.setColor(Color.rgb(68,80,92));c.drawRoundRect(new RectF(left,tankY-dp(30),right,tankY+dp(30)),dp(28),dp(28),p);
      int zones=Math.max(acts,Math.min(8,acts*2));ArrayList<float[]> current=new ArrayList<>();for(int i=0;i<zones;i++){float x=left+(right-left)*(i+.5f)/zones;p.setColor(Color.WHITE);c.drawCircle(x,tankY,dp(4),p);current.add(new float[]{x,tankY+dp(32)});}float y=dp(205);
      for(int l=1;l<=lays;l++){ArrayList<float[]> next=new ArrayList<>();int target=Math.max(acts,(int)Math.ceil(current.size()/2.0));for(int j=0;j<target;j++){int s0=(int)Math.floor((double)j*current.size()/target),s1=Math.min(current.size()-1,(int)Math.floor((double)(j+1)*current.size()/target)-1);if(s1<s0)s1=s0;float x=0;int cnt=0;for(int k=s0;k<=s1;k++){x+=current.get(k)[0];cnt++;}x/=Math.max(1,cnt);for(int k=s0;k<=s1;k++)line(c,current.get(k)[0],current.get(k)[1],x,y,Color.rgb(145,168,185),2);p.setColor(Color.rgb(51,205,220));c.drawCircle(x,y,dp(6),p);t.setColor(Color.rgb(51,205,220));t.setTextSize(dp(6));c.drawText("L"+l,x+dp(6),y-dp(4),t);next.add(new float[]{x,y+dp(8)});}current=next;y+=dp(95);}
      float actY=Math.min(dp(620),y+dp(55));for(int a=0;a<acts;a++){float ax=left+(right-left)*(a+.5f)/acts;float[] src=current.get(Math.min(current.size()-1,(int)Math.floor((double)a*current.size()/acts)));line(c,src[0],src[1],ax,actY-dp(42),Color.rgb(180,195,205),2);p.setColor(pActType.getSelectedItemPosition()==0?Color.rgb(67,190,113):Color.rgb(160,110,230));c.drawRoundRect(new RectF(ax-dp(15),actY-dp(42),ax+dp(15),actY+dp(25)),dp(6),dp(6),p);t.setColor(Color.WHITE);t.setTextSize(dp(6));c.drawText("A"+(a+1),ax-dp(7),actY+dp(42),t);}
      p.setColor(Color.rgb(48,58,66));c.drawRect(left,actY+dp(75),right,actY+dp(90),p);t.setTextSize(dp(7));t.setColor(Color.rgb(190,210,225));c.drawText("STRONGBACK / GROUND",left,actY+dp(112),t);
      t.setTextSize(dp(7));t.setColor(Color.rgb(230,80,80));c.drawText("X",W-dp(105),dp(73),t);t.setColor(Color.rgb(80,210,120));c.drawText("Y",W-dp(75),dp(73),t);t.setColor(Color.rgb(80,150,240));c.drawText("Z",W-dp(45),dp(73),t);
    }
  }
}
