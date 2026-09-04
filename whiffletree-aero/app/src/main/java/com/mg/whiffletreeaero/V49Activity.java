package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V49Activity extends V48Activity {
  TextView reportInfo;
  ReportView reportView;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("AUTOMATIC ENGINEERING REPORT",16,true,Color.WHITE));
    reportInfo=card("Engineering report hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(reportInfo,lp());
    reportView=new ReportView();
    panel.addView(reportView,new LinearLayout.LayoutParams(-1,dp(1350)));
    root.addView(panel,29,lp());
    refreshReport();
  }

  void refreshReport(){
    try{
      Calc c=compute(false);
      double sum=0,peak=0,actPeak=0;
      for(double v:c.fi){sum+=v;peak=Math.max(peak,v);}for(double v:c.actLoads)actPeak=Math.max(actPeak,v);
      IState is=istate();
      reportInfo.setText(String.format(Locale.US,
        "REPORT READY\nTank %.2f m × %.2f m | Fz %.1f kN | My %.1f kN·m\nStations %d | Layers %d | Actuators %d\nΣF %.1f kN | peak station %.1f kN | peak actuator %.1f kN\nBeam stress %.1f MPa | Pin Ø%.0f mm | Pad mean pressure %.2f MPa\nSafety state: %s\nBOM and all detailed screens are included in the design basis below.",
        c.L,c.D,c.targetF,c.targetM,c.n,c.layers,c.nAct,sum,peak,actPeak,c.beamStress,c.pinSel,c.padPressure,is.state));
      reportView.invalidate();
    }catch(Exception e){reportInfo.setText("Report inputs could not be evaluated.");}
  }

  class ReportView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    ReportView(){super(V49Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    void section(Canvas c,float y,String h,String[] lines){
      int W=getWidth();p.setColor(Color.rgb(16,43,62));c.drawRoundRect(new RectF(dp(14),y,W-dp(14),y+dp(150)),dp(8),dp(8),p);
      t.setColor(Color.rgb(247,207,77));t.setTextSize(dp(8));c.drawText(h,dp(26),y+dp(25),t);
      t.setColor(Color.WHITE);t.setTextSize(dp(7));float yy=y+dp(50);for(String s:lines){c.drawText(s,dp(26),yy,t);yy+=dp(22);}
    }
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);Calc q;try{q=compute(false);}catch(Exception e){return;}int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(14));c.drawText("EFT WHIFFLETREE ENGINEERING REPORT",dp(14),dp(30),t);
      t.setColor(Color.rgb(185,210,230));t.setTextSize(dp(7));c.drawText("Concept design summary generated from current application inputs",dp(14),dp(52),t);
      double stationPeak=0,actPeak=0;for(double v:q.fi)stationPeak=Math.max(stationPeak,v);for(double v:q.actLoads)actPeak=Math.max(actPeak,v);
      float y=dp(75);
      section(c,y,"1. DESIGN INPUTS",new String[]{String.format(Locale.US,"Tank L %.2f m | D %.2f m",q.L,q.D),String.format(Locale.US,"Target Fz %.1f kN | My %.1f kN·m",q.targetF,q.targetM),String.format(Locale.US,"Stations %d | layers %d | actuators %d",q.n,q.layers,q.nAct),String.format(Locale.US,"Sizing factor %.2f | hydraulic %.0f bar",q.sf,q.pbar)});y+=dp(165);
      section(c,y,"2. LOAD DISTRIBUTION",new String[]{String.format(Locale.US,"ΣF %.1f kN | calculated moment %.1f kN·m",q.sumF,q.calcM),String.format(Locale.US,"Moment error %.2f%% | peak station %.1f kN",q.mErr,stationPeak),String.format(Locale.US,"Peak actuator group %.1f kN",actPeak),"Station forces are redistributed through the whiffletree beam hierarchy."});y+=dp(165);
      section(c,y,"3. MECHANICAL LOAD PATH",new String[]{String.format(Locale.US,"Beam count %d | beam stress %.1f MPa",q.beams,q.beamStress),String.format(Locale.US,"Beam deflection %.2f mm | pin Ø%.0f mm",q.beamDefl,q.pinSel),String.format(Locale.US,"Required lug thickness %.1f mm",q.lugT),"EFT → pad → tie rod → whiffletree → load cell → actuator → strongback."});y+=dp(165);
      section(c,y,"4. ACTUATION / HYDRAULICS",new String[]{String.format(Locale.US,"Nominal bore basis %.1f mm | stroke requirement %.1f mm",q.bore,q.strokeReq),String.format(Locale.US,"Hydraulic supply %.0f bar | link angle %.2f°",q.pbar,q.linkAngle),"Servo-valve, hose and manifold channels follow actuator count.","Final component ratings must be checked against supplier data."});y+=dp(165);
      section(c,y,"5. CONTACT / INSTRUMENTATION",new String[]{String.format(Locale.US,"Pad mean pressure %.2f MPa | pad area %.0f mm²",q.padPressure,q.padArea),String.format(Locale.US,"Load cell nominal basis %.0f kN",q.lc),"DAQ includes load cell, LVDT, pressure, strain and displacement channels.","Local EFT skin/stringer allowables require structural substantiation."});y+=dp(165);
      IState st=istate();section(c,y,"6. SAFETY / INTERLOCK",new String[]{"Active state: "+st.state,st.reason,"Action: "+(st.action==null?"monitor":st.action),"Final implementation requires independent hazard analysis and fail-safe hardware."});y+=dp(165);
      section(c,y,"7. BOM / RELEASE BASIS",new String[]{q.bom,"Use approved P/Ns for actuator, LC, joints, valves and sensors.","Verify FEM, fixture reactions, anchor loads and collision envelopes.","This report is a concept/training design basis, not final test-rig release authority."});
    }
  }
}
