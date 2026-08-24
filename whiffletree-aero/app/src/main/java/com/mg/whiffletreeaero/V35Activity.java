package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V35Activity extends V34Activity {
  TextView info; ActuatorSelectView view; EditText rodDiaAct, proofFactor, bucklingK, stdBores, stdStrokes; int sel=0;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    rodDiaAct=field("Actuator rod diameter [mm]","40");
    proofFactor=field("Proof pressure factor","1.5");
    bucklingK=field("Rod buckling K factor","1.0");
    stdBores=field("Standard bore series [mm]","40,50,63,80,100,125");
    stdStrokes=field("Standard stroke series [mm]","50,100,150,200,250,300");
    LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(8),dp(8),dp(8),dp(8));p.setBackground(bg(Color.rgb(12,31,47),12));
    p.addView(tx("REAL ACTUATOR SELECTION",16,true,Color.WHITE));
    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);Button prev=new Button(this),next=new Button(this);prev.setText("◀ PREV ACT");next.setText("NEXT ACT ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);sel=Math.max(0,sel-1);refresh(c);}catch(Exception e){}});next.setOnClickListener(v->{try{Calc c=compute(false);if(c.nAct>0)sel=(sel+1)%c.nAct;refresh(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));p.addView(nav);
    info=card("Actuator selection hazırlanıyor...",Color.rgb(20,48,68));p.addView(info,lp());view=new ActuatorSelectView();p.addView(view,new LinearLayout.LayoutParams(-1,dp(590)));root.addView(p,15,lp());
    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){try{refresh(compute(false));}catch(Exception e){}} public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{rodDiaAct,proofFactor,bucklingK,stdBores,stdStrokes,F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,deflection,linkLength,targetSpeed,servoFlow})e.addTextChangedListener(w);
    try{refresh(compute(false));}catch(Exception e){}
  }

  static class ACheck{double load,boreReq,boreSel,rod,push,pull,strokeReq,strokeSel,pcr,proofBar,flow,servoUtil;String status;}
  double nextSeries(String s,double req){double best=Double.POSITIVE_INFINITY;for(String z:s.split(",")){try{double v=Double.parseDouble(z.trim());if(v>=req&&v<best)best=v;}catch(Exception e){}}return Double.isFinite(best)?best:req;}
  ACheck chk(Calc c,int a){
    ACheck q=new ACheck();a=Math.max(0,Math.min(a,c.nAct-1));q.load=c.actLoads[a];double pbar=Math.max(1,c.pbar);double area=q.load*1000/(pbar*1e5);q.boreReq=Math.sqrt(4*area/Math.PI)*1000;q.boreSel=nextSeries(stdBores.getText().toString(),q.boreReq);q.rod=Math.max(5,d(rodDiaAct));
    double A=Math.PI*Math.pow(q.boreSel/1000,2)/4.0,Ar=Math.PI*Math.pow(q.rod/1000,2)/4.0;q.push=pbar*1e5*A/1000;q.pull=pbar*1e5*Math.max(1e-9,A-Ar)/1000;
    q.strokeReq=c.strokeReq;q.strokeSel=nextSeries(stdStrokes.getText().toString(),q.strokeReq);
    double E=210e9,I=Math.PI*Math.pow(q.rod/1000,4)/64.0,Lm=Math.max(.05,q.strokeSel/1000.0),K=Math.max(.2,d(bucklingK));q.pcr=Math.PI*Math.PI*E*I/Math.pow(K*Lm,2)/1000.0;
    q.proofBar=pbar*Math.max(1,d(proofFactor));double speed=Math.max(.1,d(targetSpeed))/1000.0;q.flow=A*speed*60000;q.servoUtil=100*q.flow/Math.max(.1,d(servoFlow));
    boolean ok=q.push>=q.load&&q.pull>=q.load&&q.strokeSel>=q.strokeReq&&q.pcr>=q.load*1.5&&q.servoUtil<=100;q.status=ok?((q.servoUtil<=80&&q.pcr>=q.load*2)?"PASS":"WARN"):"FAIL";return q;
  }
  void refresh(Calc c){if(c.nAct<=0)return;if(sel>=c.nAct)sel=c.nAct-1;ACheck q=chk(c,sel);info.setText(String.format(Locale.US,"ACT%d • %s • design %.2f kN\nRequired bore %.1f mm → selected Ø%.0f mm | rod Ø%.0f mm\nPush %.1f kN | pull %.1f kN @ %.0f bar\nStroke req %.1f mm → selected %.0f mm\nRod Euler buckling %.1f kN | proof pressure %.0f bar\nFlow %.2f L/min | servo utilization %.1f%%",sel+1,q.status,q.load,q.boreReq,q.boreSel,q.rod,q.push,q.pull,c.pbar,q.strokeReq,q.strokeSel,q.pcr,q.proofBar,q.flow,q.servoUtil));if(view!=null)view.invalidate();}

  class ActuatorSelectView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);ActuatorSelectView(){super(V35Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    protected void onDraw(Canvas cn){super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception e){return;}if(c.nAct<=0)return;ACheck q=chk(c,Math.max(0,Math.min(sel,c.nAct-1)));int W=getWidth();t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("ACT"+(sel+1)+" REAL SELECTION 2D",dp(12),dp(25),t);
      float cx=W*.5f,y=dp(115);p.setColor(Color.rgb(55,132,238));cn.drawRoundRect(new RectF(cx-dp(65),y,cx+dp(65),y+dp(170)),dp(12),dp(12),p);p.setColor(Color.rgb(13,35,51));cn.drawRoundRect(new RectF(cx-dp(42),y+dp(20),cx+dp(42),y+dp(145)),dp(7),dp(7),p);p.setColor(Color.rgb(205,215,225));cn.drawRect(cx-dp(10),y-dp(65),cx+dp(10),y+dp(20),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText(String.format(Locale.US,"BORE Ø%.0f",q.boreSel),cx-dp(32),y+dp(65),t);cn.drawText(String.format(Locale.US,"ROD Ø%.0f",q.rod),cx-dp(29),y-dp(30),t);cn.drawText(String.format(Locale.US,"STROKE %.0f",q.strokeSel),cx-dp(31),y+dp(110),t);
      float my=dp(335);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),my,W-dp(16),my+dp(175)),dp(8),dp(8),p);t.setTextSize(dp(8));t.setColor(Color.WHITE);cn.drawText(String.format(Locale.US,"Required / selected bore: %.1f / %.0f mm",q.boreReq,q.boreSel),dp(28),my+dp(25),t);cn.drawText(String.format(Locale.US,"Push / pull: %.1f / %.1f kN",q.push,q.pull),dp(28),my+dp(50),t);cn.drawText(String.format(Locale.US,"Stroke req / sel: %.1f / %.0f mm",q.strokeReq,q.strokeSel),dp(28),my+dp(75),t);cn.drawText(String.format(Locale.US,"Buckling Pcr: %.1f kN",q.pcr),dp(28),my+dp(100),t);cn.drawText(String.format(Locale.US,"Proof pressure: %.0f bar",q.proofBar),dp(28),my+dp(125),t);cn.drawText(String.format(Locale.US,"Servo flow util: %.1f%%",q.servoUtil),dp(28),my+dp(150),t);int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));p.setColor(col);cn.drawCircle(W-dp(40),my+dp(145),dp(13),p);t.setColor(col);t.setTextSize(dp(10));cn.drawText("ACTUATOR SELECTION: "+q.status,dp(28),my+dp(170),t);
    }
  }
}
