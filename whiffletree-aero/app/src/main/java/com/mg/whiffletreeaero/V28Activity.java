package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V28Activity extends V27Activity {
  StrongbackView strongbackView;
  TextView strongbackInfo;
  EditText strongbackSpan, anchorCount;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);

    strongbackSpan=field("Strongback support span [m]","6.0");
    anchorCount=field("Total floor anchor count","8");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("STRONGBACK / FLOOR REACTION DETAIL",16,true,Color.WHITE));
    strongbackInfo=card("Strongback reaksiyonları hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(strongbackInfo,lp());
    strongbackView=new StrongbackView();
    panel.addView(strongbackView,new LinearLayout.LayoutParams(-1,dp(520)));
    root.addView(panel,8,lp());

    TextWatcher refresh=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int b,int c){}
      public void onTextChanged(CharSequence s,int a,int b,int c){refreshStrongback();}
      public void afterTextChanged(Editable e){}
    };
    strongbackSpan.addTextChangedListener(refresh);
    anchorCount.addTextChangedListener(refresh);
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    refreshStrongback();
  }

  void refreshStrongback(){
    try{
      Calc c=compute(false);
      double span=Math.max(.5,d(strongbackSpan));
      int anchors=Math.max(2,(int)Math.round(d(anchorCount)));
      double total=0,m=0;
      for(int a=0;a<c.nAct;a++){
        double x=groupX(c,a);
        total+=c.actLoads[a];
        m+=c.actLoads[a]*(x-c.L/2.0);
      }
      double rL=total/2.0-m/span;
      double rR=total/2.0+m/span;
      double perAnchor=Math.max(Math.abs(rL),Math.abs(rR))/Math.max(1,anchors/2.0);
      String status=(rL>=0&&rR>=0)?"PASS":"REVIEW UPLIFT";
      strongbackInfo.setText(String.format(Locale.US,
        "%s • Preliminary rigid-strongback equilibrium\nTotal vertical reaction %.2f kN | Resultant moment about center %.2f kN·m\nLeft support %.2f kN | Right support %.2f kN\nSupport span %.2f m | Anchors %d | Governing average anchor share %.2f kN\nLoad path: ACT channels → strongback → support lines → floor anchors",
        status,total,m,rL,rR,span,anchors,perAnchor));
      if(strongbackView!=null)strongbackView.invalidate();
    }catch(Exception e){}
  }

  double groupX(Calc c,int a){
    int s=c.actStationStart[a], e=c.actStationEnd[a];
    double sw=0,sx=0;
    for(int i=s;i<=e&&i<c.n;i++){double f=Math.max(.001,c.fi[i]);sw+=f;sx+=f*c.x[i];}
    return sw>0?sx/sw:c.L*(a+.5)/c.nAct;
  }

  class StrongbackView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    StrongbackView(){super(V28Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}

    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);
      Calc c;try{c=compute(false);}catch(Exception ex){return;}
      double span=Math.max(.5,d(strongbackSpan));
      int anchors=Math.max(2,(int)Math.round(d(anchorCount)));
      double total=0,m=0;
      for(int a=0;a<c.nAct;a++){double x=groupX(c,a);total+=c.actLoads[a];m+=c.actLoads[a]*(x-c.L/2.0);}
      double rL=total/2.0-m/span, rR=total/2.0+m/span;
      int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("STRONGBACK FREE-BODY + FLOOR REACTIONS",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Preliminary rigid-body equilibrium; final anchor design requires fixture FEM and floor allowables",dp(12),dp(43),t);

      float x1=dp(35),x2=W-dp(35),y=dp(220);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(125,145,160));
      cn.drawRoundRect(new RectF(x1,y-dp(18),x2,y+dp(18)),dp(5),dp(5),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("STRONGBACK",W/2-dp(30),y+dp(4),t);

      for(int a=0;a<c.nAct;a++){
        double gx=groupX(c,a)/Math.max(.001,c.L);
        float x=x1+(x2-x1)*(float)gx;
        int[] cols={Color.rgb(55,132,238),Color.rgb(51,205,220),Color.rgb(245,164,47),Color.rgb(160,110,230),Color.rgb(67,190,113),Color.rgb(229,82,74),Color.rgb(247,207,77),Color.rgb(120,180,220)};
        int col=cols[a%cols.length];
        p.setColor(col);p.setStrokeWidth(4);cn.drawLine(x,dp(95),x,y-dp(18),p);
        p.setStyle(Paint.Style.FILL);Path ah=new Path();ah.moveTo(x,y-dp(18));ah.lineTo(x-dp(7),y-dp(31));ah.lineTo(x+dp(7),y-dp(31));ah.close();cn.drawPath(ah,p);
        t.setColor(Color.WHITE);t.setTextSize(dp(7));cn.drawText("A"+(a+1),x-dp(6),dp(78),t);cn.drawText(String.format(Locale.US,"%.0f",c.actLoads[a]),x-dp(9),dp(91),t);
      }

      float sy=dp(330);
      p.setColor(Color.rgb(75,85,95));cn.drawRect(dp(18),sy,W-dp(18),sy+dp(38),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("FLOOR / FOUNDATION",W/2-dp(40),sy+dp(24),t);
      p.setColor(Color.rgb(210,220,230));p.setStrokeWidth(3);cn.drawLine(x1+dp(18),y+dp(18),x1+dp(18),sy,p);cn.drawLine(x2-dp(18),y+dp(18),x2-dp(18),sy,p);
      drawReaction(cn,x1+dp(18),sy+dp(72),rL,"R-L");
      drawReaction(cn,x2-dp(18),sy+dp(72),rR,"R-R");

      int perSide=Math.max(1,anchors/2);
      t.setColor(Color.rgb(200,220,235));t.setTextSize(dp(8));
      cn.drawText(String.format(Locale.US,"Support span %.2f m",span),dp(20),dp(405),t);
      cn.drawText("Anchors total "+anchors+" (≈ "+perSide+" per support line)",dp(20),dp(430),t);
      cn.drawText(String.format(Locale.US,"ΣR = %.1f kN  |  ΣMcenter = %.1f kN·m",rL+rR,m),dp(20),dp(455),t);
      if(rL<0||rR<0){t.setColor(Color.rgb(255,130,120));cn.drawText("UPLIFT REVIEW: one support reaction is negative",dp(20),dp(482),t);}else{t.setColor(Color.rgb(130,230,175));cn.drawText("REACTION SIGN CHECK: PASS",dp(20),dp(482),t);}
    }

    void drawReaction(Canvas cn,float x,float y,double r,String label){
      int col=r>=0?Color.rgb(67,190,113):Color.rgb(229,82,74);
      p.setColor(col);p.setStrokeWidth(4);cn.drawLine(x,y,x,y-dp(52),p);
      p.setStyle(Paint.Style.FILL);Path ah=new Path();ah.moveTo(x,y-dp(58));ah.lineTo(x-dp(8),y-dp(45));ah.lineTo(x+dp(8),y-dp(45));ah.close();cn.drawPath(ah,p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText(label,x-dp(13),y+dp(17),t);cn.drawText(String.format(Locale.US,"%.1f kN",r),x-dp(21),y+dp(34),t);
    }
  }
}
