package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V26Activity extends ProActivity {
  StationPadView stationView;
  TextView stationInfo;
  int selectedStation=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("STATION / LOAD PAD DETAIL",16,true,Color.WHITE));

    LinearLayout nav=new LinearLayout(this);
    nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this); prev.setText("◀ PREV STATION");
    Button next=new Button(this); next.setText("NEXT STATION ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);selectedStation=Math.max(0,selectedStation-1);refreshStation(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.n>0)selectedStation=(selectedStation+1)%c.n;refreshStation(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));
    panel.addView(nav);

    stationInfo=card("Station detayları hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(stationInfo,lp());
    stationView=new StationPadView();
    panel.addView(stationView,new LinearLayout.LayoutParams(-1,dp(470)));
    root.addView(panel,6,lp());

    TextWatcher refresh=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int b,int c){}
      public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshStation(compute(false));}catch(Exception e){}}
      public void afterTextChanged(Editable e){}
    };
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    try{refreshStation(compute(false));}catch(Exception e){}
  }

  void refreshStation(Calc c){
    if(c.n<=0)return;
    if(selectedStation>=c.n)selectedStation=c.n-1;
    int i=Math.max(0,selectedStation);
    int a=c.stationAct[i];
    double fi=c.fi[i];
    double designFi=fi*c.sf;
    double moment=fi*c.x[i];
    double areaMm2=Math.max(1,d(padW)*d(padL));
    double pMpa=fi*1000.0/(areaMm2/1e6)/1e6;
    int beam=-1;
    if(c.beamData!=null){
      for(BeamData bd:c.beamData){if(bd.s0<=i&&i<=bd.s1){beam=bd.id;break;}}
    }
    stationInfo.setText(String.format(Locale.US,
      "S%d • x = %.3f m • ACT%d group\nWorking load %.2f kN | Design load %.2f kN\nMoment contribution about x=0: %.2f kN·m\nPad %.0f × %.0f mm | Area %.0f mm² | Mean pressure %.3f MPa\nFirst connected beam: %s | Load path: PAD → LINK → BEAM(S) → LC%d → ACT%d",
      i+1,c.x[i],a+1,fi,designFi,moment,d(padW),d(padL),areaMm2,pMpa,beam>=0?("B"+(beam+1)):"direct",a+1,a+1));
    if(stationView!=null)stationView.invalidate();
  }

  class StationPadView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    StationPadView(){super(V26Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}

    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);
      Calc c; try{c=compute(false);}catch(Exception ex){return;}
      if(c.n<=0)return;
      int i=Math.max(0,Math.min(selectedStation,c.n-1));
      int a=c.stationAct[i];
      double fi=c.fi[i];
      double areaMm2=Math.max(1,d(padW)*d(padL));
      double pMpa=fi*1000.0/(areaMm2/1e6)/1e6;
      int W=getWidth();

      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("S"+(i+1)+" STATION / LOAD PAD 2D DETAIL",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Local load introduction + connection path",dp(12),dp(43),t);

      float cx=W*.50f, tankY=dp(120);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(55,67,80));
      cn.drawRoundRect(new RectF(dp(38),tankY-dp(25),W-dp(38),tankY+dp(25)),dp(25),dp(25),p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(160,180,198));
      cn.drawRoundRect(new RectF(dp(38),tankY-dp(25),W-dp(38),tankY+dp(25)),dp(25),dp(25),p);

      float padWpx=Math.min(dp(150),(float)(W*.35));
      float padHpx=dp(28);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(245,164,47));
      RectF pad=new RectF(cx-padWpx/2,tankY-dp(25)-padHpx,cx+padWpx/2,tankY-dp(25));
      cn.drawRoundRect(pad,dp(5),dp(5),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("LOAD PAD",cx-dp(24),tankY-dp(34),t);

      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.rgb(55,132,238));
      cn.drawLine(cx,tankY-dp(55),cx,tankY-dp(95),p);
      p.setStyle(Paint.Style.FILL);Path ah=new Path();ah.moveTo(cx,tankY-dp(100));ah.lineTo(cx-dp(8),tankY-dp(88));ah.lineTo(cx+dp(8),tankY-dp(88));ah.close();cn.drawPath(ah,p);
      t.setColor(Color.WHITE);t.setTextSize(dp(9));cn.drawText(String.format(Locale.US,"F = %.2f kN",fi),cx+dp(12),tankY-dp(76),t);

      float y=dp(230);
      box(cn,dp(20),y,dp(86),dp(42),"PAD",Color.rgb(245,164,47));
      box(cn,dp(126),y,dp(86),dp(42),"LINK",Color.rgb(51,205,220));
      box(cn,dp(232),y,dp(86),dp(42),"BEAM",Color.rgb(247,207,77));
      box(cn,dp(338),y,dp(70),dp(42),"LC"+(a+1),Color.rgb(229,82,74));
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.WHITE);
      cn.drawLine(dp(106),y+dp(21),dp(126),y+dp(21),p);cn.drawLine(dp(212),y+dp(21),dp(232),y+dp(21),p);cn.drawLine(dp(318),y+dp(21),dp(338),y+dp(21),p);

      y=dp(310);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(16,43,62));
      cn.drawRoundRect(new RectF(dp(18),y,W-dp(18),y+dp(120)),dp(8),dp(8),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(9));
      cn.drawText(String.format(Locale.US,"x = %.3f m",c.x[i]),dp(30),y+dp(24),t);
      cn.drawText(String.format(Locale.US,"Moment contribution = %.2f kN·m",fi*c.x[i]),dp(30),y+dp(48),t);
      cn.drawText(String.format(Locale.US,"Pad = %.0f × %.0f mm",d(padW),d(padL)),dp(30),y+dp(72),t);
      cn.drawText(String.format(Locale.US,"Mean contact pressure = %.3f MPa",pMpa),dp(30),y+dp(96),t);

      int statusCol=pMpa<4?Color.rgb(67,190,113):(pMpa<8?Color.rgb(247,207,77):Color.rgb(229,82,74));
      p.setColor(statusCol);cn.drawCircle(W-dp(45),y+dp(93),dp(10),p);
    }

    void box(Canvas cn,float x,float y,float w,float h,String s,int col){
      p.setStyle(Paint.Style.FILL);p.setColor(col);cn.drawRoundRect(new RectF(x,y,x+w,y+h),dp(6),dp(6),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText(s,x+dp(10),y+h/2+dp(4),t);
    }
  }
}
