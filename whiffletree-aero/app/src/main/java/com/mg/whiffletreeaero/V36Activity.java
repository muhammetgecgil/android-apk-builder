package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V36Activity extends V35Activity {
  TextView lcInfo; LoadCellSelectView lcView;
  EditText lcSeries, lcProofPct, lcOverloadPct, lcResolutionPct;
  int lcSel=0;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    lcSeries=field("Load-cell capacity series [kN]","10,20,30,50,75,100,150,200,300,500");
    lcProofPct=field("Load-cell proof rating [%FS]","150");
    lcOverloadPct=field("Load-cell safe overload [%FS]","200");
    lcResolutionPct=field("Required force resolution [%FS]","0.05");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("LOAD CELL SELECTION ENGINE",16,true,Color.WHITE));

    LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    Button prev=new Button(this);prev.setText("◀ PREV LOAD CELL");
    Button next=new Button(this);next.setText("NEXT LOAD CELL ▶");
    prev.setOnClickListener(v->{try{Calc c=compute(false);lcSel=Math.max(0,lcSel-1);refreshLc(c);}catch(Exception e){}});
    next.setOnClickListener(v->{try{Calc c=compute(false);if(c.nAct>0)lcSel=(lcSel+1)%c.nAct;refreshLc(c);}catch(Exception e){}});
    nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));
    nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));
    panel.addView(nav);

    lcInfo=card("Load-cell selection hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(lcInfo,lp());
    lcView=new LoadCellSelectView();
    panel.addView(lcView,new LinearLayout.LayoutParams(-1,dp(610)));
    root.addView(panel,16,lp());

    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){try{refreshLc(compute(false));}catch(Exception e){}} public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{lcSeries,lcProofPct,lcOverloadPct,lcResolutionPct,F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf})e.addTextChangedListener(w);
    try{refreshLc(compute(false));}catch(Exception e){}
  }

  static class LcCheck{
    double designKn, capKn, utilPct, proofKn, overloadKn, marginProof, marginOverload, resolutionKn, resolutionPct;
    String status;
  }

  LcCheck lcCheck(Calc c,int a){
    LcCheck q=new LcCheck();
    a=Math.max(0,Math.min(a,c.nAct-1));
    q.designKn=c.actLoads[a];
    q.capKn=nextSeries(lcSeries.getText().toString(),q.designKn);
    q.utilPct=100.0*q.designKn/Math.max(.001,q.capKn);
    q.proofKn=q.capKn*Math.max(100,d(lcProofPct))/100.0;
    q.overloadKn=q.capKn*Math.max(100,d(lcOverloadPct))/100.0;
    q.marginProof=(q.proofKn-q.designKn)/Math.max(.001,q.designKn)*100.0;
    q.marginOverload=(q.overloadKn-q.designKn)/Math.max(.001,q.designKn)*100.0;
    q.resolutionPct=Math.max(.001,d(lcResolutionPct));
    q.resolutionKn=q.capKn*q.resolutionPct/100.0;
    boolean proofOk=q.designKn<=q.proofKn, overOk=q.designKn<=q.overloadKn;
    if(proofOk&&overOk&&q.utilPct>=20&&q.utilPct<=80)q.status="PASS";
    else if(proofOk&&overOk&&q.utilPct<=100)q.status="WARN";
    else q.status="FAIL";
    return q;
  }

  void refreshLc(Calc c){
    if(c.nAct<=0)return;
    if(lcSel>=c.nAct)lcSel=c.nAct-1;
    int a=Math.max(0,lcSel);
    LcCheck q=lcCheck(c,a);
    lcInfo.setText(String.format(Locale.US,
      "LC%d / ACT%d • %s • S%d–S%d\nDesign load %.2f kN → selected nominal %.0f kN\nUtilization %.1f%% FS\nProof %.0f%% FS = %.1f kN | margin to design %.1f%%\nSafe overload %.0f%% FS = %.1f kN | margin %.1f%%\nRequired resolution %.3f%% FS → %.3f kN per count basis\nSelection target: enough overload margin without oversizing the measurement range",
      a+1,a+1,q.status,c.actStationStart[a]+1,c.actStationEnd[a]+1,q.designKn,q.capKn,q.utilPct,d(lcProofPct),q.proofKn,q.marginProof,d(lcOverloadPct),q.overloadKn,q.marginOverload,q.resolutionPct,q.resolutionKn));
    if(lcView!=null)lcView.invalidate();
  }

  class LoadCellSelectView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    LoadCellSelectView(){super(V36Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception ex){return;}if(c.nAct<=0)return;
      int a=Math.max(0,Math.min(lcSel,c.nAct-1));LcCheck q=lcCheck(c,a);int W=getWidth();float cx=W*.5f;
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("LC"+(a+1)+" SELECTION + LOAD PATH",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Nominal range, utilization, proof/overload margin and resolution screening",dp(12),dp(43),t);

      float y=dp(115);
      box(cn,cx-dp(145),y,dp(92),dp(58),"BEAM\nOUTPUT",Color.rgb(247,207,77));
      box(cn,cx-dp(35),y,dp(70),dp(58),"LC"+(a+1),Color.rgb(51,205,220));
      box(cn,cx+dp(53),y,dp(92),dp(58),"ACT"+(a+1),Color.rgb(55,132,238));
      line(cn,cx-dp(53),y+dp(29),cx-dp(35),y+dp(29));
      line(cn,cx+dp(35),y+dp(29),cx+dp(53),y+dp(29));
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText(String.format(Locale.US,"F = %.1f kN",q.designKn),cx-dp(33),y-dp(18),t);

      float gy=dp(225);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(18),gy,W-dp(18),gy+dp(210)),dp(9),dp(9),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));
      cn.drawText(String.format(Locale.US,"Nominal capacity: %.0f kN",q.capKn),dp(30),gy+dp(28),t);
      cn.drawText(String.format(Locale.US,"Working utilization: %.1f%% FS",q.utilPct),dp(30),gy+dp(55),t);
      cn.drawText(String.format(Locale.US,"Proof load: %.1f kN  | margin %.1f%%",q.proofKn,q.marginProof),dp(30),gy+dp(82),t);
      cn.drawText(String.format(Locale.US,"Safe overload: %.1f kN | margin %.1f%%",q.overloadKn,q.marginOverload),dp(30),gy+dp(109),t);
      cn.drawText(String.format(Locale.US,"Resolution target: %.3f%% FS",q.resolutionPct),dp(30),gy+dp(136),t);
      cn.drawText(String.format(Locale.US,"Equivalent increment: %.3f kN",q.resolutionKn),dp(30),gy+dp(163),t);
      int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));
      p.setColor(col);cn.drawCircle(W-dp(42),gy+dp(165),dp(13),p);
      t.setColor(col);t.setTextSize(dp(10));cn.drawText("LOAD CELL: "+q.status,dp(30),gy+dp(195),t);

      float by=dp(470);p.setColor(Color.rgb(22,48,67));cn.drawRoundRect(new RectF(dp(18),by,W-dp(18),by+dp(105)),dp(9),dp(9),p);
      t.setColor(Color.rgb(200,220,235));t.setTextSize(dp(8));
      cn.drawText("Sizing rule used:",dp(30),by+dp(22),t);
      cn.drawText("• choose first standard range ≥ design load",dp(30),by+dp(45),t);
      cn.drawText("• prefer working range roughly 20–80% FS",dp(30),by+dp(68),t);
      cn.drawText("• verify supplier proof, overload, fatigue and calibration data",dp(30),by+dp(91),t);
    }
    void line(Canvas cn,float x1,float y1,float x2,float y2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.rgb(205,215,225));cn.drawLine(x1,y1,x2,y2,p);}
    void box(Canvas cn,float x,float y,float w,float h,String s,int col){p.setStyle(Paint.Style.FILL);p.setColor(col);cn.drawRoundRect(new RectF(x,y,x+w,y+h),dp(7),dp(7),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));String[] a=s.split("\\n");for(int i=0;i<a.length;i++)cn.drawText(a[i],x+dp(10),y+dp(22)+i*dp(16),t);}
  }
}
