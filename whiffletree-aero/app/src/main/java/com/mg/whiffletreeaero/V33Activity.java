package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V33Activity extends V32Activity {
  HydraulicSizingView hydraulicView;
  TextView hydraulicInfo;
  EditText targetSpeed, servoFlow, hoseId, hydraulicEff;
  int selectedHydraulic=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    targetSpeed=field("Actuator target speed [mm/s]","25");
    servoFlow=field("Servo-valve rated flow [L/min]","25");
    hoseId=field("Pressure hose inside diameter [mm]","12");
    hydraulicEff=field("Hydraulic efficiency [%]","85");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("HYDRAULIC CHANNEL / SERVO-VALVE / FLOW DETAIL",16,true,Color.WHITE));

    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this);prev.setText("◀ PREV CHANNEL");
    Button next=new Button(this);next.setText("NEXT CHANNEL ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);selectedHydraulic=Math.max(0,selectedHydraulic-1);refreshHydraulic(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.nAct>0)selectedHydraulic=(selectedHydraulic+1)%c.nAct;refreshHydraulic(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));
    panel.addView(nav);

    hydraulicInfo=card("Hydraulic channel hesabı hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(hydraulicInfo,lp());
    hydraulicView=new HydraulicSizingView();
    panel.addView(hydraulicView,new LinearLayout.LayoutParams(-1,dp(590)));
    root.addView(panel,13,lp());

    TextWatcher refresh=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int b,int c){}
      public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshHydraulic(compute(false));}catch(Exception e){}}
      public void afterTextChanged(Editable e){}
    };
    targetSpeed.addTextChangedListener(refresh);servoFlow.addTextChangedListener(refresh);hoseId.addTextChangedListener(refresh);hydraulicEff.addTextChangedListener(refresh);
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    try{refreshHydraulic(compute(false));}catch(Exception e){}
  }

  static class HydCheck{
    double loadKn,boreMm,areaMm2,pReqBar,pSupplyBar,speedMmS,flowLpm,servoLpm,servoUtil,hoseIdMm,hoseVel,powerKw,powerInKw,eff;
    String status;
  }

  HydCheck hydCheck(Calc c,int a){
    HydCheck q=new HydCheck();
    a=Math.max(0,Math.min(a,c.nAct-1));
    q.loadKn=c.actLoads[a];
    q.boreMm=Math.max(5,c.bore);
    double boreM=q.boreMm/1000.0;
    double areaM2=Math.PI*boreM*boreM/4.0;
    q.areaMm2=areaM2*1e6;
    q.pReqBar=(q.loadKn*1000.0/Math.max(1e-12,areaM2))/1e5;
    q.pSupplyBar=Math.max(1,c.pbar);
    q.speedMmS=Math.max(.1,d(targetSpeed));
    double speedMps=q.speedMmS/1000.0;
    double flowM3s=areaM2*speedMps;
    q.flowLpm=flowM3s*60000.0;
    q.servoLpm=Math.max(.1,d(servoFlow));
    q.servoUtil=100.0*q.flowLpm/q.servoLpm;
    q.hoseIdMm=Math.max(2,d(hoseId));
    double hoseM=q.hoseIdMm/1000.0;
    double hoseArea=Math.PI*hoseM*hoseM/4.0;
    q.hoseVel=flowM3s/Math.max(1e-12,hoseArea);
    q.powerKw=(q.pSupplyBar*1e5*flowM3s)/1000.0;
    q.eff=Math.max(1,Math.min(100,d(hydraulicEff)))/100.0;
    q.powerInKw=q.powerKw/q.eff;
    boolean pressureOk=q.pReqBar<=q.pSupplyBar;
    boolean servoOk=q.servoUtil<=100;
    boolean hoseOk=q.hoseVel<=6.0;
    if(pressureOk && servoOk && hoseOk && q.servoUtil<=80 && q.hoseVel<=4.5)q.status="PASS";
    else if(pressureOk && servoOk && hoseOk)q.status="WARN";
    else q.status="FAIL";
    return q;
  }

  void refreshHydraulic(Calc c){
    if(c.nAct<=0)return;
    if(selectedHydraulic>=c.nAct)selectedHydraulic=c.nAct-1;
    int a=Math.max(0,selectedHydraulic);
    HydCheck q=hydCheck(c,a);
    hydraulicInfo.setText(String.format(Locale.US,
      "CH%d / ACT%d • S%d–S%d • %s\nDesign load %.2f kN | bore basis Ø%.1f mm | piston area %.0f mm²\nRequired pressure %.1f bar | available %.1f bar\nTarget speed %.1f mm/s → flow %.2f L/min\nServo-valve %.1f L/min | utilization %.1f%%\nHose ID %.1f mm | mean oil velocity %.2f m/s\nHydraulic power %.2f kW | estimated input %.2f kW @ %.0f%% efficiency",
      a+1,a+1,c.actStationStart[a]+1,c.actStationEnd[a]+1,q.status,q.loadKn,q.boreMm,q.areaMm2,q.pReqBar,q.pSupplyBar,q.speedMmS,q.flowLpm,q.servoLpm,q.servoUtil,q.hoseIdMm,q.hoseVel,q.powerKw,q.powerInKw,q.eff*100));
    if(hydraulicView!=null)hydraulicView.invalidate();
  }

  class HydraulicSizingView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    HydraulicSizingView(){super(V33Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}

    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);
      Calc c;try{c=compute(false);}catch(Exception ex){return;}
      if(c.nAct<=0)return;
      int a=Math.max(0,Math.min(selectedHydraulic,c.nAct-1));
      HydCheck q=hydCheck(c,a);int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("CH"+(a+1)+" HYDRAULIC CHANNEL 2D SCHEMATIC",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Preliminary flow / pressure / power sizing — verify servo-valve Δp curves, return line and transient demand",dp(12),dp(43),t);

      float y=dp(125);
      box(cn,dp(18),y,dp(80),dp(54),"HPU",Color.rgb(55,132,238));
      box(cn,dp(122),y,dp(96),dp(54),"MANIFOLD",Color.rgb(51,205,220));
      box(cn,dp(242),y,dp(104),dp(54),"SERVO\nVALVE",Color.rgb(160,110,230));
      box(cn,W-dp(118),y,dp(100),dp(54),"ACT "+(a+1),Color.rgb(245,164,47));
      pipe(cn,dp(98),y+dp(27),dp(122),y+dp(27));pipe(cn,dp(218),y+dp(27),dp(242),y+dp(27));pipe(cn,dp(346),y+dp(27),W-dp(118),y+dp(27));

      float cylY=dp(245),cx=W*.5f;
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(70,88,105));cn.drawRoundRect(new RectF(cx-dp(125),cylY,cx+dp(125),cylY+dp(82)),dp(10),dp(10),p);
      p.setColor(Color.rgb(55,132,238));cn.drawRect(cx-dp(18),cylY+dp(8),cx+dp(18),cylY+dp(74),p);
      p.setColor(Color.rgb(205,215,225));cn.drawRect(cx+dp(18),cylY+dp(35),cx+dp(118),cylY+dp(47),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText(String.format(Locale.US,"Ø %.1f mm",q.boreMm),cx-dp(100),cylY+dp(30),t);
      cn.drawText(String.format(Locale.US,"Q %.2f L/min",q.flowLpm),cx-dp(100),cylY+dp(57),t);
      cn.drawText(String.format(Locale.US,"v %.1f mm/s",q.speedMmS),cx+dp(42),cylY+dp(70),t);

      float my=dp(365);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),my,W-dp(16),my+dp(165)),dp(8),dp(8),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));
      cn.drawText(String.format(Locale.US,"Pressure: required %.1f / available %.1f bar",q.pReqBar,q.pSupplyBar),dp(28),my+dp(25),t);
      cn.drawText(String.format(Locale.US,"Servo flow: %.2f / %.1f L/min = %.1f%%",q.flowLpm,q.servoLpm,q.servoUtil),dp(28),my+dp(50),t);
      cn.drawText(String.format(Locale.US,"Hose ID %.1f mm → oil velocity %.2f m/s",q.hoseIdMm,q.hoseVel),dp(28),my+dp(75),t);
      cn.drawText(String.format(Locale.US,"Hydraulic power %.2f kW",q.powerKw),dp(28),my+dp(100),t);
      cn.drawText(String.format(Locale.US,"Estimated HPU input %.2f kW",q.powerInKw),dp(28),my+dp(125),t);
      int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));
      p.setColor(col);cn.drawCircle(W-dp(40),my+dp(118),dp(13),p);
      t.setColor(col);t.setTextSize(dp(10));cn.drawText("HYDRAULIC CHANNEL: "+q.status,dp(28),my+dp(151),t);

      t.setColor(Color.rgb(185,210,230));t.setTextSize(dp(7));cn.drawText("Screening limits used: servo ≤100%, pressure demand ≤ supply, hose mean velocity ≤6 m/s. Final design needs valve pressure-drop curve, line losses, accumulator and dynamic simultaneity analysis.",dp(18),dp(565),t);
    }

    void pipe(Canvas cn,float x1,float y1,float x2,float y2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.rgb(205,215,225));cn.drawLine(x1,y1,x2,y2,p);}
    void box(Canvas cn,float x,float y,float w,float h,String s,int col){p.setStyle(Paint.Style.FILL);p.setColor(col);cn.drawRoundRect(new RectF(x,y,x+w,y+h),dp(7),dp(7),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));String[] lines=s.split("\\n");for(int i=0;i<lines.length;i++)cn.drawText(lines[i],x+dp(10),y+dp(22)+i*dp(15),t);}
  }
}
