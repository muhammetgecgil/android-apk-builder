package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V27Activity extends V26Activity {
  ActuatorDetailView actuatorView;
  TextView actuatorInfo;
  int selectedActuator=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("ACTUATOR / LOAD CELL / HYDRAULIC CHANNEL DETAIL",16,true,Color.WHITE));

    LinearLayout nav=new LinearLayout(this);
    nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this); prev.setText("◀ PREV ACTUATOR");
    Button next=new Button(this); next.setText("NEXT ACTUATOR ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);selectedActuator=Math.max(0,selectedActuator-1);refreshActuator(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.nAct>0)selectedActuator=(selectedActuator+1)%c.nAct;refreshActuator(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));
    panel.addView(nav);

    actuatorInfo=card("Actuator detayları hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(actuatorInfo,lp());
    actuatorView=new ActuatorDetailView();
    panel.addView(actuatorView,new LinearLayout.LayoutParams(-1,dp(500)));
    root.addView(panel,7,lp());

    TextWatcher refresh=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int b,int c){}
      public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshActuator(compute(false));}catch(Exception e){}}
      public void afterTextChanged(Editable e){}
    };
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    try{refreshActuator(compute(false));}catch(Exception e){}
  }

  void refreshActuator(Calc c){
    if(c.nAct<=0)return;
    if(selectedActuator>=c.nAct)selectedActuator=c.nAct-1;
    int a=Math.max(0,selectedActuator);
    double designLoad=c.actLoads[a];
    double cap=Math.max(1,d(actCapacity));
    double util=100.0*designLoad/cap;
    double lcUtil=100.0*designLoad/Math.max(1,c.lc);
    int s0=c.actStationStart[a],s1=c.actStationEnd[a];
    String status=util<=80?"PASS":(util<=100?"WARN":"FAIL");
    actuatorInfo.setText(String.format(Locale.US,
      "ACT%d • drives S%d–S%d • %s\nDesign group load %.2f kN | Nominal capacity %.1f kN | Utilization %.1f%%\nLoad cell preselect ≥ %.0f kN | LC utilization %.1f%%\nHydraulic pressure %.0f bar | Theoretical bore basis %.1f mm\nStroke requirement %.1f mm | Link angle %.2f°\nLoad path: station pads → beam layer(s) → LC%d → ACT%d → strongback",
      a+1,s0+1,s1+1,status,designLoad,cap,util,c.lc,lcUtil,c.pbar,c.bore,c.strokeReq,c.linkAngle,a+1,a+1));
    if(actuatorView!=null)actuatorView.invalidate();
  }

  class ActuatorDetailView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    ActuatorDetailView(){super(V27Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}

    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);
      Calc c;try{c=compute(false);}catch(Exception ex){return;}
      if(c.nAct<=0)return;
      int a=Math.max(0,Math.min(selectedActuator,c.nAct-1));
      double designLoad=c.actLoads[a];
      double cap=Math.max(1,d(actCapacity));
      double util=100.0*designLoad/cap;
      double lcUtil=100.0*designLoad/Math.max(1,c.lc);
      int W=getWidth();

      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("ACT"+(a+1)+" 2D CHANNEL DETAIL",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Beam output → load cell → clevis → hydraulic actuator → strongback",dp(12),dp(43),t);

      float cx=W*.50f;
      float y=dp(95);
      // beam
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(Color.rgb(247,207,77));cn.drawLine(cx-dp(95),y,cx+dp(95),y,p);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawCircle(cx,y,dp(7),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("TOP WHIFFLETREE OUTPUT",cx-dp(58),y-dp(16),t);

      // rod + LC
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.rgb(210,220,230));cn.drawLine(cx,y+dp(7),cx,y+dp(52),p);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(229,82,74));RectF lc=new RectF(cx-dp(32),y+dp(52),cx+dp(32),y+dp(90));cn.drawRoundRect(lc,dp(6),dp(6),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(9));cn.drawText("LC"+(a+1),cx-dp(13),y+dp(76),t);

      // clevis
      p.setColor(Color.rgb(51,205,220));RectF cl=new RectF(cx-dp(26),y+dp(96),cx+dp(26),y+dp(126));cn.drawRoundRect(cl,dp(5),dp(5),p);
      t.setColor(Color.rgb(7,20,34));t.setTextSize(dp(7));cn.drawText("CLEVIS",cx-dp(16),y+dp(115),t);

      // actuator body + rod
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.rgb(190,205,220));cn.drawLine(cx,y+dp(126),cx,y+dp(165),p);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(55,132,238));RectF body=new RectF(cx-dp(44),y+dp(165),cx+dp(44),y+dp(270));cn.drawRoundRect(body,dp(9),dp(9),p);
      p.setColor(Color.rgb(13,35,51));RectF bore=new RectF(cx-dp(26),y+dp(183),cx+dp(26),y+dp(248));cn.drawRoundRect(bore,dp(5),dp(5),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(10));cn.drawText("ACT "+(a+1),cx-dp(20),y+dp(220),t);
      t.setTextSize(dp(7));cn.drawText(String.format(Locale.US,"P %.0f bar",c.pbar),cx-dp(20),y+dp(240),t);

      // strongback
      p.setColor(Color.rgb(140,155,170));RectF sb=new RectF(dp(34),y+dp(292),W-dp(34),y+dp(322));cn.drawRoundRect(sb,dp(5),dp(5),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("STRONGBACK / FLOOR REACTION",cx-dp(68),y+dp(312),t);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.rgb(210,220,230));cn.drawLine(cx,y+dp(270),cx,y+dp(292),p);

      // metrics
      float my=y+dp(345);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),my,W-dp(16),my+dp(118)),dp(8),dp(8),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));
      cn.drawText(String.format(Locale.US,"S%d–S%d group | Design load %.2f kN",c.actStationStart[a]+1,c.actStationEnd[a]+1,designLoad),dp(28),my+dp(22),t);
      cn.drawText(String.format(Locale.US,"Actuator utilization %.1f%% | LC utilization %.1f%%",util,lcUtil),dp(28),my+dp(45),t);
      cn.drawText(String.format(Locale.US,"Bore basis %.1f mm | Stroke req. %.1f mm",c.bore,c.strokeReq),dp(28),my+dp(68),t);
      cn.drawText(String.format(Locale.US,"Link angle %.2f° | Pressure %.0f bar",c.linkAngle,c.pbar),dp(28),my+dp(91),t);
      int col=util<=80?Color.rgb(67,190,113):(util<=100?Color.rgb(247,207,77):Color.rgb(229,82,74));
      p.setColor(col);cn.drawCircle(W-dp(38),my+dp(87),dp(11),p);
    }
  }
}
