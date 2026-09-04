package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V31Activity extends V30Activity {
  LinkDetailView linkView; TextView linkInfo; EditText rodDia,rodE,rodK,bearingAngle; int selectedLink=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    rodDia=field("Tie-rod / link diameter [mm]","24");
    rodE=field("Rod elastic modulus E [GPa]","210");
    rodK=field("Effective length factor K","1.0");
    bearingAngle=field("Spherical bearing allowable misalignment [deg]","8");

    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("TIE-ROD / LINK / SPHERICAL BEARING DETAIL",16,true,Color.WHITE));
    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this);prev.setText("◀ PREV LINK");Button next=new Button(this);next.setText("NEXT LINK ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);selectedLink=Math.max(0,selectedLink-1);refreshLink(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.n>0)selectedLink=(selectedLink+1)%c.n;refreshLink(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));panel.addView(nav);
    linkInfo=card("Link hesabı hazırlanıyor...",Color.rgb(20,48,68));panel.addView(linkInfo,lp());
    linkView=new LinkDetailView();panel.addView(linkView,new LinearLayout.LayoutParams(-1,dp(560)));root.addView(panel,11,lp());

    TextWatcher refresh=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshLink(compute(false));}catch(Exception e){}} public void afterTextChanged(Editable e){}};
    rodDia.addTextChangedListener(refresh);rodE.addTextChangedListener(refresh);rodK.addTextChangedListener(refresh);bearingAngle.addTextChangedListener(refresh);
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    try{refreshLink(compute(false));}catch(Exception e){}
  }

  static class LinkCheck{double loadKn,dMm,lenMm,angle,stressMpa,pcrKn,bucklingUtil,bearingUtil;String status;}
  LinkCheck check(Calc c,int i){
    LinkCheck q=new LinkCheck();i=Math.max(0,Math.min(i,c.n-1));q.loadKn=c.fi[i]*c.sf;q.dMm=Math.max(4,d(rodDia));q.lenMm=Math.max(50,d(linkLength));q.angle=c.linkAngle;
    double dM=q.dMm/1000.0,Lm=q.lenMm/1000.0,E=Math.max(1,d(rodE))*1e9,K=Math.max(.2,d(rodK));
    double A=Math.PI*dM*dM/4.0,I=Math.PI*Math.pow(dM,4)/64.0;q.stressMpa=q.loadKn*1000/A/1e6;
    q.pcrKn=Math.PI*Math.PI*E*I/Math.pow(K*Lm,2)/1000.0;q.bucklingUtil=100*q.loadKn/Math.max(.001,q.pcrKn);
    q.bearingUtil=100*Math.abs(q.angle)/Math.max(.1,d(bearingAngle));
    q.status=(q.bucklingUtil<=60&&q.bearingUtil<=80)?"PASS":((q.bucklingUtil<=100&&q.bearingUtil<=100)?"WARN":"FAIL");return q;
  }
  void refreshLink(Calc c){if(c.n<=0)return;if(selectedLink>=c.n)selectedLink=c.n-1;int i=Math.max(0,selectedLink),a=c.stationAct[i];LinkCheck q=check(c,i);
    linkInfo.setText(String.format(Locale.US,"LINK S%d → A%d • %s\nDesign axial load %.2f kN | rod Ø%.1f mm | length %.0f mm\nAxial stress %.1f MPa\nEuler Pcr %.1f kN | buckling utilization %.1f%%\nLink angle %.2f° | spherical-bearing misalignment utilization %.1f%%\nLoad path: PAD → LINK → WHIFFLETREE → LC%d → ACT%d",i+1,a+1,q.status,q.loadKn,q.dMm,q.lenMm,q.stressMpa,q.pcrKn,q.bucklingUtil,q.angle,q.bearingUtil,a+1,a+1));if(linkView!=null)linkView.invalidate();}

  class LinkDetailView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    LinkDetailView(){super(V31Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception ex){return;}if(c.n<=0)return;int i=Math.max(0,Math.min(selectedLink,c.n-1));LinkCheck q=check(c,i);int W=getWidth();float cx=W*.5f;
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("S"+(i+1)+" LINK / ROD 2D DETAIL",dp(12),dp(25),t);t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Axial load + Euler buckling + spherical-bearing misalignment",dp(12),dp(43),t);
      float y1=dp(95),y2=dp(330),ang=(float)Math.toRadians(Math.max(-15,Math.min(15,q.angle)));float dx=(float)Math.tan(ang)*(y2-y1);float x1=cx-dx/2,x2=cx+dx/2;
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawRect(dp(45),y1-dp(18),W-dp(45),y1+dp(18),p);t.setColor(Color.rgb(7,20,34));t.setTextSize(dp(8));cn.drawText("WHIFFLETREE BEAM",cx-dp(39),y1+dp(4),t);
      p.setColor(Color.rgb(245,164,47));cn.drawRoundRect(new RectF(x1-dp(28),y1+dp(22),x1+dp(28),y1+dp(55)),dp(6),dp(6),p);p.setColor(Color.rgb(51,205,220));cn.drawCircle(x1,y1+dp(38),dp(10),p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(9);p.setColor(Color.rgb(205,215,225));cn.drawLine(x1,y1+dp(55),x2,y2-dp(55),p);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(245,164,47));cn.drawRoundRect(new RectF(x2-dp(28),y2-dp(55),x2+dp(28),y2-dp(22)),dp(6),dp(6),p);p.setColor(Color.rgb(51,205,220));cn.drawCircle(x2,y2-dp(38),dp(10),p);
      p.setColor(Color.rgb(65,78,90));cn.drawRoundRect(new RectF(dp(45),y2-dp(18),W-dp(45),y2+dp(18)),dp(6),dp(6),p);t.setColor(Color.WHITE);cn.drawText("LOAD PAD / EFT INTERFACE",cx-dp(55),y2+dp(4),t);
      p.setColor(Color.rgb(229,82,74));p.setStrokeWidth(4);cn.drawLine(cx,y1-dp(58),cx,y1-dp(23),p);Path ah=new Path();ah.moveTo(cx,y1-dp(65));ah.lineTo(cx-dp(8),y1-dp(51));ah.lineTo(cx+dp(8),y1-dp(51));ah.close();cn.drawPath(ah,p);t.setColor(Color.WHITE);cn.drawText(String.format(Locale.US,"F %.1f kN",q.loadKn),cx+dp(12),y1-dp(43),t);
      float my=dp(380);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),my,W-dp(16),my+dp(145)),dp(8),dp(8),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));
      cn.drawText(String.format(Locale.US,"Rod Ø %.1f mm | L %.0f mm | K %.2f",q.dMm,q.lenMm,d(rodK)),dp(28),my+dp(24),t);cn.drawText(String.format(Locale.US,"Axial stress %.1f MPa",q.stressMpa),dp(28),my+dp(49),t);cn.drawText(String.format(Locale.US,"Euler Pcr %.1f kN | Util %.1f%%",q.pcrKn,q.bucklingUtil),dp(28),my+dp(74),t);cn.drawText(String.format(Locale.US,"Angle %.2f° | Bearing util %.1f%%",q.angle,q.bearingUtil),dp(28),my+dp(99),t);
      int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));p.setColor(col);cn.drawCircle(W-dp(40),my+dp(105),dp(12),p);t.setColor(col);t.setTextSize(dp(10));cn.drawText("LINK CHECK: "+q.status,dp(28),my+dp(130),t);
    }
  }
}
