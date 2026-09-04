package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V32Activity extends V31Activity {
  ActMisalignView actMisalignView; TextView actMisalignInfo; EditText actuatorStrokeAllow, actuatorAngleAllow, sideLoadAllowPct; int selectedActMisalign=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    actuatorStrokeAllow=field("Actuator available stroke [mm]","150");
    actuatorAngleAllow=field("Actuator spherical joint allowable angle [deg]","8");
    sideLoadAllowPct=field("Allowable side-load ratio [% of axial]","5");

    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("ACTUATOR SIDE-LOAD / MISALIGNMENT / STROKE ENVELOPE",16,true,Color.WHITE));
    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this);prev.setText("◀ PREV ACTUATOR");Button next=new Button(this);next.setText("NEXT ACTUATOR ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);selectedActMisalign=Math.max(0,selectedActMisalign-1);refreshActMisalign(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.nAct>0)selectedActMisalign=(selectedActMisalign+1)%c.nAct;refreshActMisalign(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));panel.addView(nav);
    actMisalignInfo=card("Actuator geometri kontrolü hazırlanıyor...",Color.rgb(20,48,68));panel.addView(actMisalignInfo,lp());
    actMisalignView=new ActMisalignView();panel.addView(actMisalignView,new LinearLayout.LayoutParams(-1,dp(560)));root.addView(panel,12,lp());

    TextWatcher refresh=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshActMisalign(compute(false));}catch(Exception e){}} public void afterTextChanged(Editable e){}};
    actuatorStrokeAllow.addTextChangedListener(refresh);actuatorAngleAllow.addTextChangedListener(refresh);sideLoadAllowPct.addTextChangedListener(refresh);
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    try{refreshActMisalign(compute(false));}catch(Exception e){}
  }

  static class ActCheck{double axialKn,angleDeg,sideKn,sidePct,strokeReq,strokeAvail,strokeUtil,angleUtil;String status;}
  ActCheck actCheck(Calc c,int a){
    ActCheck q=new ActCheck();a=Math.max(0,Math.min(a,c.nAct-1));q.axialKn=c.actLoads[a];q.angleDeg=Math.abs(c.linkAngle);
    double rad=Math.toRadians(q.angleDeg);q.sideKn=q.axialKn*Math.sin(rad);q.sidePct=100*q.sideKn/Math.max(.001,q.axialKn);
    q.strokeReq=c.strokeReq;q.strokeAvail=Math.max(1,d(actuatorStrokeAllow));q.strokeUtil=100*q.strokeReq/q.strokeAvail;q.angleUtil=100*q.angleDeg/Math.max(.1,d(actuatorAngleAllow));
    double sideAllow=Math.max(.1,d(sideLoadAllowPct));boolean okSide=q.sidePct<=sideAllow,okStroke=q.strokeUtil<=100,okAng=q.angleUtil<=100;
    q.status=(okSide&&okStroke&&okAng&&q.strokeUtil<=80&&q.angleUtil<=80)?"PASS":((okSide&&okStroke&&okAng)?"WARN":"FAIL");return q;
  }
  void refreshActMisalign(Calc c){if(c.nAct<=0)return;if(selectedActMisalign>=c.nAct)selectedActMisalign=c.nAct-1;int a=Math.max(0,selectedActMisalign);ActCheck q=actCheck(c,a);
    actMisalignInfo.setText(String.format(Locale.US,"ACT%d • %s • drives S%d–S%d\nAxial design load %.2f kN | rod/link angle %.2f°\nTheoretical side component %.2f kN (%.2f%% of axial) | allow %.2f%%\nStroke req %.1f mm / available %.1f mm | utilization %.1f%%\nJoint-angle utilization %.1f%% of %.1f° allowable\nGeometry risks: side-load, spherical-bearing travel, bind and overtravel",a+1,q.status,c.actStationStart[a]+1,c.actStationEnd[a]+1,q.axialKn,q.angleDeg,q.sideKn,q.sidePct,d(sideLoadAllowPct),q.strokeReq,q.strokeAvail,q.strokeUtil,q.angleUtil,d(actuatorAngleAllow)));
    if(actMisalignView!=null)actMisalignView.invalidate();}

  class ActMisalignView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    ActMisalignView(){super(V32Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception ex){return;}if(c.nAct<=0)return;int a=Math.max(0,Math.min(selectedActMisalign,c.nAct-1));ActCheck q=actCheck(c,a);int W=getWidth();float cx=W*.5f;
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("ACT"+(a+1)+" MISALIGNMENT + STROKE ENVELOPE",dp(12),dp(25),t);t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Idealized geometry screening — final side-load limits must come from actuator supplier data",dp(12),dp(43),t);
      float topY=dp(110),botY=dp(355),ang=(float)Math.toRadians(Math.max(-12,Math.min(12,q.angleDeg)));float dx=(float)Math.tan(ang)*(botY-topY);float xTop=cx-dx/2,xBot=cx+dx/2;
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawRect(dp(45),topY-dp(18),W-dp(45),topY+dp(18),p);t.setColor(Color.rgb(7,20,34));t.setTextSize(dp(8));cn.drawText("WHIFFLETREE OUTPUT",cx-dp(38),topY+dp(4),t);
      p.setColor(Color.rgb(51,205,220));cn.drawCircle(xTop,topY+dp(30),dp(12),p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.rgb(205,215,225));cn.drawLine(xTop,topY+dp(42),xBot,botY-dp(120),p);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(55,132,238));RectF body=new RectF(xBot-dp(42),botY-dp(120),xBot+dp(42),botY);cn.drawRoundRect(body,dp(8),dp(8),p);p.setColor(Color.rgb(13,35,51));cn.drawRoundRect(new RectF(xBot-dp(24),botY-dp(100),xBot+dp(24),botY-dp(20)),dp(5),dp(5),p);t.setColor(Color.WHITE);t.setTextSize(dp(9));cn.drawText("ACT "+(a+1),xBot-dp(20),botY-dp(55),t);
      p.setColor(Color.rgb(130,145,160));cn.drawRect(dp(35),botY+dp(18),W-dp(35),botY+dp(46),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("STRONGBACK",cx-dp(27),botY+dp(37),t);
      p.setColor(Color.rgb(229,82,74));p.setStrokeWidth(4);cn.drawLine(xTop,topY-dp(68),xTop,topY-dp(24),p);Path ah=new Path();ah.moveTo(xTop,topY-dp(74));ah.lineTo(xTop-dp(8),topY-dp(60));ah.lineTo(xTop+dp(8),topY-dp(60));ah.close();cn.drawPath(ah,p);t.setColor(Color.WHITE);cn.drawText(String.format(Locale.US,"Fax %.1f kN",q.axialKn),xTop+dp(12),topY-dp(44),t);
      if(q.sideKn>0.01){p.setColor(Color.rgb(160,110,230));cn.drawLine(xTop,topY-dp(47),xTop+dp(55),topY-dp(47),p);Path sh=new Path();sh.moveTo(xTop+dp(62),topY-dp(47));sh.lineTo(xTop+dp(48),topY-dp(55));sh.lineTo(xTop+dp(48),topY-dp(39));sh.close();cn.drawPath(sh,p);t.setColor(Color.WHITE);cn.drawText(String.format(Locale.US,"Fside %.2f kN",q.sideKn),xTop+dp(16),topY-dp(57),t);}
      float my=dp(420);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),my,W-dp(16),my+dp(110)),dp(8),dp(8),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));
      cn.drawText(String.format(Locale.US,"Angle %.2f° | Joint util %.1f%%",q.angleDeg,q.angleUtil),dp(28),my+dp(24),t);cn.drawText(String.format(Locale.US,"Side component %.2f kN | %.2f%% axial",q.sideKn,q.sidePct),dp(28),my+dp(48),t);cn.drawText(String.format(Locale.US,"Stroke %.1f / %.1f mm | %.1f%% util",q.strokeReq,q.strokeAvail,q.strokeUtil),dp(28),my+dp(72),t);
      int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));p.setColor(col);cn.drawCircle(W-dp(40),my+dp(72),dp(12),p);t.setColor(col);t.setTextSize(dp(10));cn.drawText("ACTUATOR GEOMETRY: "+q.status,dp(28),my+dp(98),t);
    }
  }
}
