package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V30Activity extends V29Activity {
  BeamSectionView beamSectionView;
  TextView beamSectionInfo;
  EditText beamAllow, beamE, beamWall;
  int selectedBeamSection=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);

    beamAllow=field("Beam allowable bending stress [MPa]","180");
    beamE=field("Beam elastic modulus E [GPa]","70");
    beamWall=field("Beam wall thickness [mm] (0 = solid)","8");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("WHIFFLETREE BEAM SECTION / STRENGTH DETAIL",16,true,Color.WHITE));

    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this);prev.setText("◀ PREV BEAM");
    Button next=new Button(this);next.setText("NEXT BEAM ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);selectedBeamSection=Math.max(0,selectedBeamSection-1);refreshBeamSection(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.beamData!=null&&c.beamData.size()>0)selectedBeamSection=(selectedBeamSection+1)%c.beamData.size();refreshBeamSection(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));
    panel.addView(nav);

    beamSectionInfo=card("Beam section hesabı hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(beamSectionInfo,lp());
    beamSectionView=new BeamSectionView();
    panel.addView(beamSectionView,new LinearLayout.LayoutParams(-1,dp(560)));
    root.addView(panel,10,lp());

    TextWatcher refresh=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int b,int c){}
      public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshBeamSection(compute(false));}catch(Exception e){}}
      public void afterTextChanged(Editable e){}
    };
    beamAllow.addTextChangedListener(refresh);beamE.addTextChangedListener(refresh);beamWall.addTextChangedListener(refresh);
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    try{refreshBeamSection(compute(false));}catch(Exception e){}
  }

  static class BeamCheck {
    double spanMm,bMm,hMm,tMm,I,Z,momentNm,stressMpa,deflMm,util;
    String status;
  }

  BeamCheck beamCheck(BeamData bd){
    BeamCheck q=new BeamCheck();
    q.spanMm=Math.max(100,d(beamSpan));q.bMm=Math.max(10,d(beamB));q.hMm=Math.max(10,d(beamH));q.tMm=Math.max(0,d(beamWall));
    double b=q.bMm/1000.0,h=q.hMm/1000.0,t=q.tMm/1000.0;
    if(t>0 && 2*t<Math.min(b,h)) q.I=(b*Math.pow(h,3)-(b-2*t)*Math.pow(h-2*t,3))/12.0;
    else q.I=b*Math.pow(h,3)/12.0;
    q.Z=q.I/(h/2.0);
    double Lm=q.spanMm/1000.0;
    double fL=bd.fLeft*1000.0,fR=bd.fRight*1000.0;
    double lL=bd.leftArm/1000.0,lR=bd.rightArm/1000.0;
    q.momentNm=Math.max(fL*lL,fR*lR);
    q.stressMpa=(q.momentNm/Math.max(1e-12,q.Z))/1e6;
    double E=Math.max(1,d(beamE))*1e9;
    // Conservative simple-span estimate using total branch load at midspan basis.
    q.deflMm=((bd.total*1000.0)*Math.pow(Lm,3)/(48.0*E*Math.max(1e-12,q.I)))*1000.0;
    double allow=Math.max(1,d(beamAllow));q.util=100.0*q.stressMpa/allow;
    q.status=q.util<=80?"PASS":(q.util<=100?"WARN":"FAIL");
    return q;
  }

  void refreshBeamSection(Calc c){
    if(c.beamData==null||c.beamData.size()==0)return;
    if(selectedBeamSection>=c.beamData.size())selectedBeamSection=c.beamData.size()-1;
    BeamData bd=c.beamData.get(Math.max(0,selectedBeamSection));
    BeamCheck q=beamCheck(bd);
    beamSectionInfo.setText(String.format(Locale.US,
      "B%d • Layer %d • S%d–S%d • %s\nBranch load %.2f kN | Fleft %.2f kN | Fright %.2f kN\nSection %.0f × %.0f mm | wall %.1f mm | span %.0f mm\nI = %.3e m⁴ | Z = %.3e m³\nMmax basis %.2f kN·m | bending stress %.1f MPa / allow %.0f MPa\nUtilization %.1f%% | estimated deflection %.2f mm",
      bd.id+1,bd.layer,bd.s0+1,bd.s1+1,q.status,bd.total,bd.fLeft,bd.fRight,q.bMm,q.hMm,q.tMm,q.spanMm,q.I,q.Z,q.momentNm/1000.0,q.stressMpa,d(beamAllow),q.util,q.deflMm));
    if(beamSectionView!=null)beamSectionView.invalidate();
  }

  class BeamSectionView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    BeamSectionView(){super(V30Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}

    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception ex){return;}
      if(c.beamData==null||c.beamData.size()==0)return;
      BeamData bd=c.beamData.get(Math.max(0,Math.min(selectedBeamSection,c.beamData.size()-1)));
      BeamCheck q=beamCheck(bd);int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("B"+(bd.id+1)+" BEAM — SECTION + FBD",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Preliminary elastic beam sizing — final release requires actual section properties, joints and FEM",dp(12),dp(43),t);

      float x1=dp(42),x2=W-dp(42),y=dp(145);float pivot=x1+(x2-x1)*(float)(bd.leftArm/(bd.leftArm+bd.rightArm));
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(10);p.setColor(Color.rgb(247,207,77));cn.drawLine(x1,y,x2,y,p);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(51,205,220));Path tri=new Path();tri.moveTo(pivot,y+dp(4));tri.lineTo(pivot-dp(15),y+dp(30));tri.lineTo(pivot+dp(15),y+dp(30));tri.close();cn.drawPath(tri,p);
      drawForce(cn,x1,y-dp(75),y-dp(6),bd.fLeft,"F-L");drawForce(cn,x2,y-dp(75),y-dp(6),bd.fRight,"F-R");
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText(String.format(Locale.US,"L-L %.0f mm",bd.leftArm),x1+dp(12),y+dp(45),t);cn.drawText(String.format(Locale.US,"L-R %.0f mm",bd.rightArm),pivot+dp(12),y+dp(45),t);

      float sy=dp(245),cx=W*.27f;float boxW=dp(120),boxH=dp(150);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(170,185,198));cn.drawRect(cx-boxW/2,sy,cx+boxW/2,sy+boxH,p);
      if(q.tMm>0 && 2*q.tMm<Math.min(q.bMm,q.hMm)){
        float frac=(float)Math.min(.42,q.tMm/Math.min(q.bMm,q.hMm));p.setColor(Color.rgb(5,17,29));cn.drawRect(cx-boxW/2+boxW*frac,sy+boxH*frac,cx+boxW/2-boxW*frac,sy+boxH-boxH*frac,p);
      }
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText(String.format(Locale.US,"b %.0f mm",q.bMm),cx-dp(32),sy+boxH+dp(22),t);cn.drawText(String.format(Locale.US,"h %.0f mm",q.hMm),cx+boxW/2+dp(10),sy+boxH/2,t);

      float mx=W*.56f,my=dp(245);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(mx,my,W-dp(16),my+dp(185)),dp(8),dp(8),p);
      t.setTextSize(dp(8));t.setColor(Color.WHITE);
      cn.drawText(String.format(Locale.US,"Mmax %.2f kN·m",q.momentNm/1000.0),mx+dp(12),my+dp(25),t);
      cn.drawText(String.format(Locale.US,"I %.3e m⁴",q.I),mx+dp(12),my+dp(50),t);
      cn.drawText(String.format(Locale.US,"Z %.3e m³",q.Z),mx+dp(12),my+dp(75),t);
      cn.drawText(String.format(Locale.US,"σ %.1f MPa",q.stressMpa),mx+dp(12),my+dp(100),t);
      cn.drawText(String.format(Locale.US,"Util %.1f%%",q.util),mx+dp(12),my+dp(125),t);
      cn.drawText(String.format(Locale.US,"δ est. %.2f mm",q.deflMm),mx+dp(12),my+dp(150),t);
      int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));p.setColor(col);cn.drawCircle(W-dp(38),my+dp(150),dp(12),p);

      float gy=dp(470);p.setColor(Color.rgb(22,48,67));cn.drawRoundRect(new RectF(dp(16),gy,W-dp(16),gy+dp(62)),dp(8),dp(8),p);
      t.setColor(col);t.setTextSize(dp(11));cn.drawText("BEAM CHECK: "+q.status,dp(28),gy+dp(24),t);
      t.setColor(Color.rgb(200,220,235));t.setTextSize(dp(7));cn.drawText("Stress model is preliminary; include beam self-weight, local bearing, holes, welds/fasteners, lateral stability and deformed geometry in final verification.",dp(28),gy+dp(47),t);
    }

    void drawForce(Canvas cn,float x,float y1,float y2,double f,String name){
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.rgb(229,82,74));cn.drawLine(x,y1,x,y2,p);
      p.setStyle(Paint.Style.FILL);Path a=new Path();a.moveTo(x,y2+dp(2));a.lineTo(x-dp(8),y2-dp(12));a.lineTo(x+dp(8),y2-dp(12));a.close();cn.drawPath(a,p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText(String.format(Locale.US,"%s %.1f kN",name,f),x-dp(28),y1-dp(8),t);
    }
  }
}
