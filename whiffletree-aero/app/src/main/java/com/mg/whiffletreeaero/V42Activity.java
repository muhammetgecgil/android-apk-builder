package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V42Activity extends V41Activity {
  EditText fyInput,mxInput,caseFactor; TextView combinedInfo; CombinedView combinedView;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    fyInput=field("Combined case lateral force Fy [kN]","0");
    mxInput=field("Combined case roll moment Mx [kN·m]","0");
    caseFactor=field("Combined case factor","1.0");
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("COMBINED LOAD CASES / 6-DOF SCREENING",16,true,Color.WHITE));
    combinedInfo=card("Combined load case hesaplanıyor...",Color.rgb(20,48,68));panel.addView(combinedInfo,lp());
    combinedView=new CombinedView();panel.addView(combinedView,new LinearLayout.LayoutParams(-1,dp(760)));root.addView(panel,22,lp());
    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){refreshCombined();}public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{fyInput,mxInput,caseFactor,F,M,L,D,stations,actCount,actCapacity,sf})e.addTextChangedListener(w);refreshCombined();
  }
  double[] combinedLoads(Calc c){
    double fy=d(fyInput),mx=d(mxInput),cf=Math.max(0,d(caseFactor)),rad=Math.max(.05,d(D)/2.0),baseFy=fy/Math.max(1,c.n),roll=mx/(Math.max(1,c.n)*rad),sum=0;
    double[] out=new double[c.n];for(int i=0;i<c.n;i++){double side=(i%2==0?1:-1);out[i]=Math.max(0,(c.fi[i]+Math.abs(baseFy)+Math.abs(roll*side))*cf);sum+=out[i];}return out;
  }
  void refreshCombined(){try{Calc c=compute(false);double[] f=combinedLoads(c);double sum=0,peak=0,mom=0;for(int i=0;i<c.n;i++){sum+=f[i];peak=Math.max(peak,f[i]);mom+=f[i]*c.x[i];}double cap=Math.max(1,d(actCapacity)),actPeak=peak*Math.ceil(c.n/(double)Math.max(1,(int)Math.round(d(actCount))))*c.sf;String st=actPeak/cap>1?"FAIL":(actPeak/cap>.85?"WARN":"PASS");combinedInfo.setText(String.format(Locale.US,"COMBINED CASE: Fz %.1f kN + My %.1f kN·m + Fy %.1f kN + Mx %.1f kN·m\nEquivalent positive station demand sum %.1f kN | station peak %.1f kN\nScreened actuator peak %.1f kN | utilization %.1f%% | %s\nThis is a conservative concept-screening combination, not a substitute for a full vector/FE load introduction model.",d(F),d(M),d(fyInput),d(mxInput),sum,peak,actPeak,100*actPeak/cap,st));combinedView.invalidate();}catch(Exception e){}}
  class CombinedView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);CombinedView(){super(V42Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception e){return;}double[] f=combinedLoads(c);int W=getWidth();float left=dp(30),right=W-dp(30),cy=dp(180);p.setColor(Color.rgb(70,82,95));cn.drawRoundRect(new RectF(left,cy-dp(28),right,cy+dp(28)),dp(28),dp(28),p);t.setColor(Color.WHITE);t.setTextSize(dp(12));cn.drawText("EFT COMBINED LOAD INTRODUCTION",dp(16),dp(30),t);double max=1;for(double v:f)max=Math.max(max,v);for(int i=0;i<c.n;i++){float x=left+(right-left)*(i+.5f)/c.n;float h=(float)(dp(95)*f[i]/max);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(247,207,77));cn.drawLine(x,cy-dp(35),x,cy-dp(35)-h,p);p.setStyle(Paint.Style.FILL);cn.drawCircle(x,cy,dp(5),p);t.setTextSize(dp(6));cn.drawText(String.format(Locale.US,"S%d %.0f",i+1,f[i]),x-dp(10),cy-dp(45)-h,t);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(Color.rgb(229,82,74));cn.drawArc(new RectF(W/2-dp(65),cy+dp(75),W/2+dp(65),cy+dp(205)),190,160,false,p);p.setStyle(Paint.Style.FILL);t.setTextSize(dp(8));t.setColor(Color.rgb(229,82,74));cn.drawText("Mx roll contribution",W/2-dp(58),cy+dp(230),t);p.setColor(Color.rgb(51,205,220));cn.drawLine(dp(55),cy+dp(300),W-dp(55),cy+dp(300),p);t.setColor(Color.rgb(51,205,220));cn.drawText("Fy lateral contribution",dp(70),cy+dp(325),t);t.setColor(Color.rgb(185,210,230));t.setTextSize(dp(7));cn.drawText("Fz/My station distribution + |Fy| + |Mx/R| screening envelope",dp(18),cy+dp(390),t);cn.drawText("Use this screen to identify topology/capacity sensitivity before detailed 3D vector analysis.",dp(18),cy+dp(415),t);}
  }
}
