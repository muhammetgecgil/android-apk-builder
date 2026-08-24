package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V29Activity extends V28Activity {
  JointDetailView jointView;
  TextView jointInfo;
  EditText clevisT, lugWidth, edgeDistance;
  int selectedJoint=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);

    clevisT=field("Clevis plate thickness per side [mm]","20");
    lugWidth=field("Center lug width [mm]","60");
    edgeDistance=field("Pin center to free edge [mm]","40");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("PIN / CLEVIS / LUG DETAIL + CHECKS",16,true,Color.WHITE));

    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this);prev.setText("◀ PREV JOINT");
    Button next=new Button(this);next.setText("NEXT JOINT ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);selectedJoint=Math.max(0,selectedJoint-1);refreshJoint(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.nAct>0)selectedJoint=(selectedJoint+1)%c.nAct;refreshJoint(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));panel.addView(nav);

    jointInfo=card("Joint hesabı hazırlanıyor...",Color.rgb(20,48,68));panel.addView(jointInfo,lp());
    jointView=new JointDetailView();panel.addView(jointView,new LinearLayout.LayoutParams(-1,dp(540)));
    root.addView(panel,9,lp());

    TextWatcher refresh=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshJoint(compute(false));}catch(Exception e){}} public void afterTextChanged(Editable e){}};
    clevisT.addTextChangedListener(refresh);lugWidth.addTextChangedListener(refresh);edgeDistance.addTextChangedListener(refresh);
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    try{refreshJoint(compute(false));}catch(Exception e){}
  }

  void refreshJoint(Calc c){
    if(c.nAct<=0)return;
    if(selectedJoint>=c.nAct)selectedJoint=c.nAct-1;
    int a=Math.max(0,selectedJoint);
    double load=c.actLoads[a]*1000.0;
    double pinD=Math.max(2,c.pinSel)/1000.0;
    double tSide=Math.max(1,d(clevisT))/1000.0;
    double lugW=Math.max(d(lugWidth),c.pinSel+2)/1000.0;
    double edge=Math.max(1,d(edgeDistance))/1000.0;
    double pinArea=Math.PI*pinD*pinD/4.0;
    double pinTau=load/(2.0*pinArea)/1e6;
    double pinAllowV=Math.max(1,d(pinAllow));
    double sideBearing=load/2.0/(pinD*tSide)/1e6;
    double centerT=Math.max(c.lugT,d(clevisT))/1000.0;
    double centerBearing=load/(pinD*centerT)/1e6;
    double bearingAllowV=Math.max(1,d(bearingAllow));
    double netArea=Math.max(1e-9,(lugW-pinD)*centerT);
    double netStress=load/netArea/1e6;
    double shearOutArea=Math.max(1e-9,2.0*centerT*Math.max(.001,edge-pinD/2.0));
    double shearOut=load/shearOutArea/1e6;
    String status=(pinTau<=pinAllowV && sideBearing<=bearingAllowV && centerBearing<=bearingAllowV && edge>=1.5*pinD)?"PASS":"REVIEW";
    jointInfo.setText(String.format(Locale.US,
      "JOINT %d / ACT%d • %s • design load %.2f kN\nPin Ø%.0f mm • double-shear %.1f MPa / allow %.0f MPa\nClevis side bearing %.1f MPa | center lug bearing %.1f MPa / allow %.0f MPa\nCenter lug t≈%.1f mm | width %.1f mm | edge distance %.1f mm\nNet-section stress %.1f MPa | shear-out indicator %.1f MPa\nGeometry rule: e/d = %.2f (preliminary target ≥1.5)",
      a+1,a+1,status,c.actLoads[a],c.pinSel,pinTau,pinAllowV,sideBearing,centerBearing,bearingAllowV,centerT*1000,lugW*1000,edge*1000,netStress,shearOut,edge/pinD));
    if(jointView!=null)jointView.invalidate();
  }

  class JointDetailView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    JointDetailView(){super(V29Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}

    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception ex){return;}if(c.nAct<=0)return;
      int a=Math.max(0,Math.min(selectedJoint,c.nAct-1));double load=c.actLoads[a]*1000.0;
      double pinD=Math.max(2,c.pinSel)/1000.0,tSide=Math.max(1,d(clevisT))/1000.0,edge=Math.max(1,d(edgeDistance))/1000.0;
      double pinTau=load/(2.0*(Math.PI*pinD*pinD/4.0))/1e6;
      double sideBearing=load/2.0/(pinD*tSide)/1e6;
      double centerT=Math.max(c.lugT,d(clevisT))/1000.0;
      double centerBearing=load/(pinD*centerT)/1e6;
      boolean pass=pinTau<=Math.max(1,d(pinAllow)) && Math.max(sideBearing,centerBearing)<=Math.max(1,d(bearingAllow)) && edge>=1.5*pinD;
      int W=getWidth();float cx=W*.5f;
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("JOINT "+(a+1)+" — DOUBLE-SHEAR CLEVIS SECTION",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Idealized preliminary sizing view — verify actual fitting geometry and material allowables",dp(12),dp(43),t);

      float y=dp(150);float plateH=dp(150),gap=dp(34);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(51,205,220));
      cn.drawRoundRect(new RectF(cx-dp(100),y-plateH/2,cx-dp(48),y+plateH/2),dp(8),dp(8),p);
      cn.drawRoundRect(new RectF(cx+dp(48),y-plateH/2,cx+dp(100),y+plateH/2),dp(8),dp(8),p);
      p.setColor(Color.rgb(247,207,77));cn.drawRoundRect(new RectF(cx-gap,y-dp(62),cx+gap,y+dp(62)),dp(12),dp(12),p);
      p.setColor(Color.rgb(210,220,230));cn.drawCircle(cx,y,dp(25),p);p.setColor(Color.rgb(65,78,90));cn.drawCircle(cx,y,dp(14),p);
      t.setColor(Color.rgb(7,20,34));t.setTextSize(dp(8));cn.drawText("CLEVIS",cx-dp(92),y-dp(55),t);cn.drawText("CLEVIS",cx+dp(55),y-dp(55),t);cn.drawText("LUG",cx-dp(11),y-dp(47),t);
      t.setColor(Color.WHITE);cn.drawText("PIN",cx-dp(9),y+dp(4),t);

      p.setColor(Color.rgb(229,82,74));p.setStrokeWidth(4);cn.drawLine(cx,y-dp(105),cx,y-dp(67),p);Path ah=new Path();ah.moveTo(cx,y-dp(112));ah.lineTo(cx-dp(8),y-dp(98));ah.lineTo(cx+dp(8),y-dp(98));ah.close();cn.drawPath(ah,p);
      t.setColor(Color.WHITE);t.setTextSize(dp(9));cn.drawText(String.format(Locale.US,"F = %.1f kN",c.actLoads[a]),cx+dp(14),y-dp(85),t);

      float my=dp(300);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),my,W-dp(16),my+dp(180)),dp(8),dp(8),p);
      t.setTextSize(dp(8));t.setColor(Color.WHITE);
      cn.drawText(String.format(Locale.US,"Pin selected Ø%.0f mm | required basis Ø%.1f mm",c.pinSel,c.pinReq),dp(28),my+dp(25),t);
      cn.drawText(String.format(Locale.US,"Double-shear pin stress %.1f MPa",pinTau),dp(28),my+dp(50),t);
      cn.drawText(String.format(Locale.US,"Side plate bearing %.1f MPa",sideBearing),dp(28),my+dp(75),t);
      cn.drawText(String.format(Locale.US,"Center lug bearing %.1f MPa",centerBearing),dp(28),my+dp(100),t);
      cn.drawText(String.format(Locale.US,"Center lug min thickness basis %.1f mm",c.lugT),dp(28),my+dp(125),t);
      cn.drawText(String.format(Locale.US,"Edge ratio e/d = %.2f",edge/pinD),dp(28),my+dp(150),t);
      p.setColor(pass?Color.rgb(67,190,113):Color.rgb(229,82,74));cn.drawCircle(W-dp(42),my+dp(145),dp(13),p);
      t.setColor(pass?Color.rgb(130,230,175):Color.rgb(255,140,130));t.setTextSize(dp(9));cn.drawText(pass?"PRELIMINARY CHECK: PASS":"PRELIMINARY CHECK: REVIEW",dp(28),my+dp(173),t);
    }
  }
}
