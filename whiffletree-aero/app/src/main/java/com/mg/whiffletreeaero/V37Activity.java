package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V37Activity extends V36Activity {
  TextView padInfo; PadContactView padView;
  EditText padPressureAllow, contactFactor, linerThk, saddleAngle;
  int padSel=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    padPressureAllow=field("Preliminary allowable average pad pressure [MPa]","2.0");
    contactFactor=field("Effective contact factor [0..1]","0.75");
    linerThk=field("Compliant liner thickness [mm]","8");
    saddleAngle=field("Saddle wrap angle [deg]","20");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("LOAD PAD / SADDLE / EFT CONTACT ANALYSIS",16,true,Color.WHITE));

    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this);prev.setText("◀ PREV STATION");
    Button next=new Button(this);next.setText("NEXT STATION ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);padSel=Math.max(0,padSel-1);refreshPad(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.n>0)padSel=(padSel+1)%c.n;refreshPad(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));
    panel.addView(nav);

    padInfo=card("Pad contact hesabı hazırlanıyor...",Color.rgb(20,48,68));panel.addView(padInfo,lp());
    padView=new PadContactView();panel.addView(padView,new LinearLayout.LayoutParams(-1,dp(620)));
    root.addView(panel,17,lp());

    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshPad(compute(false));}catch(Exception e){}} public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{padPressureAllow,contactFactor,linerThk,saddleAngle,padW,padL,D,F,M,L,stations,layers,actCount,sf})e.addTextChangedListener(w);
    try{refreshPad(compute(false));}catch(Exception e){}
  }

  static class PadCheck{
    double loadKn,wMm,lMm,areaMm2,effAreaMm2,avgMpa,allowMpa,utilPct,diamMm,radiusMm,wrapDeg,arcMm,sagittaMm,linerMm,reqAreaMm2,reqWidthMm;
    String status;
  }

  PadCheck padCheck(Calc c,int i){
    PadCheck q=new PadCheck();i=Math.max(0,Math.min(i,c.n-1));
    q.loadKn=c.fi[i]*c.sf;
    q.wMm=Math.max(20,d(padW));q.lMm=Math.max(20,d(padL));
    q.areaMm2=q.wMm*q.lMm;
    double cf=Math.max(.1,Math.min(1.0,d(contactFactor)));
    q.effAreaMm2=q.areaMm2*cf;
    q.avgMpa=q.loadKn*1000.0/Math.max(1.0,q.effAreaMm2);
    q.allowMpa=Math.max(.05,d(padPressureAllow));q.utilPct=100*q.avgMpa/q.allowMpa;
    q.diamMm=Math.max(50,d(D)*1000.0);q.radiusMm=q.diamMm/2.0;
    q.wrapDeg=Math.max(1,Math.min(120,d(saddleAngle)));
    double rad=Math.toRadians(q.wrapDeg);q.arcMm=q.radiusMm*rad;
    double halfChord=q.radiusMm*Math.sin(rad/2.0);q.sagittaMm=q.radiusMm-Math.sqrt(Math.max(0,q.radiusMm*q.radiusMm-halfChord*halfChord));
    q.linerMm=Math.max(0,d(linerThk));
    q.reqAreaMm2=q.loadKn*1000.0/q.allowMpa/cf;
    q.reqWidthMm=q.reqAreaMm2/q.lMm;
    if(q.utilPct<=70)q.status="PASS"; else if(q.utilPct<=100)q.status="WARN"; else q.status="FAIL";
    return q;
  }

  void refreshPad(Calc c){
    if(c.n<=0)return;if(padSel>=c.n)padSel=c.n-1;int i=Math.max(0,padSel);PadCheck q=padCheck(c,i);int a=c.stationAct[i];
    padInfo.setText(String.format(Locale.US,
      "S%d → ACT%d • %s\nDesign station load %.2f kN\nPad %.0f × %.0f mm | nominal area %.0f mm² | effective area %.0f mm²\nAverage contact pressure %.3f MPa / preliminary allow %.3f MPa | utilization %.1f%%\nEFT diameter %.0f mm | saddle wrap %.1f° | arc length %.1f mm | curvature sagitta %.2f mm\nCompliant liner %.1f mm\nRequired effective sizing basis: area %.0f mm² → width ≈ %.0f mm at current pad length\nLocal skin/stringer/frame allowables are NOT inferred; final pad release requires actual EFT structure data and local FEM/analysis.",
      i+1,a+1,q.status,q.loadKn,q.wMm,q.lMm,q.areaMm2,q.effAreaMm2,q.avgMpa,q.allowMpa,q.utilPct,q.diamMm,q.wrapDeg,q.arcMm,q.sagittaMm,q.linerMm,q.reqAreaMm2,q.reqWidthMm));
    if(padView!=null)padView.invalidate();
  }

  class PadContactView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    PadContactView(){super(V37Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception ex){return;}if(c.n<=0)return;
      int i=Math.max(0,Math.min(padSel,c.n-1));PadCheck q=padCheck(c,i);int W=getWidth();float cx=W*.5f;
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("S"+(i+1)+" PAD / EFT CONTACT 2D SECTION",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Curvature-aware preliminary contact-pressure screening",dp(12),dp(43),t);

      float cy=dp(215);float r=dp(145);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(16));p.setColor(Color.rgb(72,82,94));cn.drawArc(new RectF(cx-r,cy-r,cx+r,cy+r),205,130,false,p);
      p.setStrokeWidth(dp(5));p.setColor(Color.rgb(130,145,160));cn.drawArc(new RectF(cx-r,cy-r,cx+r,cy+r),205,130,false,p);

      float padY=cy-r+dp(13);float padWpx=Math.min(W-dp(80),dp(230));
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawRoundRect(new RectF(cx-padWpx/2,padY-dp(17),cx+padWpx/2,padY+dp(17)),dp(9),dp(9),p);
      int linerPx=Math.max(dp(5),dp((int)Math.min(15.0,q.linerMm/2.0)));
      p.setColor(Color.rgb(67,190,113));cn.drawRoundRect(new RectF(cx-padWpx/2,padY+dp(18),cx+padWpx/2,padY+dp(18)+linerPx),dp(5),dp(5),p);
      t.setColor(Color.rgb(7,20,34));t.setTextSize(dp(8));cn.drawText(String.format(Locale.US,"PAD %.0f×%.0f",q.wMm,q.lMm),cx-dp(42),padY+dp(4),t);

      p.setColor(Color.rgb(229,82,74));cn.drawRect(cx-dp(4),dp(65),cx+dp(4),padY-dp(25),p);Path ah=new Path();ah.moveTo(cx,padY-dp(13));ah.lineTo(cx-dp(10),padY-dp(30));ah.lineTo(cx+dp(10),padY-dp(30));ah.close();cn.drawPath(ah,p);
      t.setColor(Color.WHITE);t.setTextSize(dp(9));cn.drawText(String.format(Locale.US,"F %.1f kN",q.loadKn),cx+dp(15),dp(95),t);

      float my=dp(390);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),my,W-dp(16),my+dp(175)),dp(8),dp(8),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));
      cn.drawText(String.format(Locale.US,"Nominal area %.0f mm²",q.areaMm2),dp(28),my+dp(24),t);
      cn.drawText(String.format(Locale.US,"Effective area %.0f mm²",q.effAreaMm2),dp(28),my+dp(49),t);
      cn.drawText(String.format(Locale.US,"Average pressure %.3f MPa",q.avgMpa),dp(28),my+dp(74),t);
      cn.drawText(String.format(Locale.US,"Allowable basis %.3f MPa | util %.1f%%",q.allowMpa,q.utilPct),dp(28),my+dp(99),t);
      cn.drawText(String.format(Locale.US,"Wrap %.1f° | sagitta %.2f mm",q.wrapDeg,q.sagittaMm),dp(28),my+dp(124),t);
      cn.drawText(String.format(Locale.US,"Suggested width basis ≈ %.0f mm",q.reqWidthMm),dp(28),my+dp(149),t);
      int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));p.setColor(col);cn.drawCircle(W-dp(42),my+dp(145),dp(13),p);
      t.setColor(col);t.setTextSize(dp(10));cn.drawText("PAD CONTACT: "+q.status,dp(28),my+dp(171),t);

      t.setColor(Color.rgb(185,210,230));t.setTextSize(dp(7));cn.drawText("Screening only: shell thickness, frames/stringers, cutouts, local bending, pad compliance and actual contact distribution must be verified separately.",dp(18),dp(600),t);
    }
  }
}
