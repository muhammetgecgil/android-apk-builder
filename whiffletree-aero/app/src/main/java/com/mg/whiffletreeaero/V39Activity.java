package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V39Activity extends V38Activity {
  TextView geomInfo; GeometryCheckView geomView;
  EditText jointAngleLimit, linkAngleLimit, minClearance, strokeReservePct;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    jointAngleLimit=field("Spherical-joint angular limit [deg]","12");
    linkAngleLimit=field("Preferred tie-rod angle limit [deg]","8");
    minClearance=field("Minimum beam/tank clearance [mm]","25");
    strokeReservePct=field("Minimum actuator stroke reserve [%]","15");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("COLLISION / BIND / OVERTRAVEL CHECK",16,true,Color.WHITE));
    geomInfo=card("Geometry envelope hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(geomInfo,lp());
    geomView=new GeometryCheckView();
    panel.addView(geomView,new LinearLayout.LayoutParams(-1,dp(720)));
    root.addView(panel,19,lp());

    TextWatcher w=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int b,int c){}
      public void onTextChanged(CharSequence s,int a,int b,int c){refreshGeometry();}
      public void afterTextChanged(Editable e){}
    };
    for(EditText e:new EditText[]{jointAngleLimit,linkAngleLimit,minClearance,strokeReservePct,deflection,linkLength,layers,actCount,stations,F,M,L,D})e.addTextChangedListener(w);
    if(loadSeek!=null)loadSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onStartTrackingTouch(SeekBar s){}
      public void onStopTrackingTouch(SeekBar s){}
      public void onProgressChanged(SeekBar s,int v,boolean u){loadPct=v;refreshKin();refreshGeometry();}
    });
    refreshGeometry();
  }

  static class GCheck{
    double maxLinkDeg,maxJointUtil,maxStrokeUtil,minStrokeReserve,minClearMm;
    int strokeFail,jointFail,clearFail,beamConflict;
    String status;
  }

  GCheck gcheck(Calc c){
    GCheck q=new GCheck();
    double link=Math.max(50,d(linkLength));
    double jointLim=Math.max(1,d(jointAngleLimit));
    double linkLim=Math.max(1,d(linkAngleLimit));
    double reserveReq=Math.max(0,Math.min(80,d(strokeReservePct)));
    q.minStrokeReserve=100;
    q.minClearMm=Double.POSITIVE_INFINITY;
    for(int i=0;i<c.n;i++){
      double ang=Math.toDegrees(Math.atan2(Math.abs(stationDispMm(c,i)),link));
      q.maxLinkDeg=Math.max(q.maxLinkDeg,ang);
      q.maxJointUtil=Math.max(q.maxJointUtil,100*ang/jointLim);
      if(ang>jointLim)q.jointFail++;
    }
    for(int a=0;a<c.nAct;a++){
      double req=Math.abs(actuatorStrokeMm(c,a));
      ACheck ac=chk(c,a);
      double selected=Math.max(1,ac.strokeSel);
      double util=100*req/selected;
      q.maxStrokeUtil=Math.max(q.maxStrokeUtil,util);
      double reserve=100-util;
      q.minStrokeReserve=Math.min(q.minStrokeReserve,reserve);
      if(util>100 || reserve<reserveReq)q.strokeFail++;
    }
    int layerCount=Math.max(1,(int)Math.round(d(layers)));
    double nominalClear=Math.max(0,d(minClearance));
    double maxDisp=0;for(int i=0;i<c.n;i++)maxDisp=Math.max(maxDisp,Math.abs(stationDispMm(c,i)));
    q.minClearMm=nominalClear-maxDisp*0.55-Math.max(0,layerCount-1)*2.5;
    if(q.minClearMm<0)q.clearFail=1;
    if(layerCount>=3 && c.nAct>=5 && q.maxLinkDeg>linkLim*.8)q.beamConflict=1;
    if(q.strokeFail>0||q.jointFail>0||q.clearFail>0||q.beamConflict>0)q.status="FAIL";
    else if(q.maxStrokeUtil>85||q.maxJointUtil>80||q.minClearMm<nominalClear*.35||q.maxLinkDeg>linkLim)q.status="WARN";
    else q.status="PASS";
    return q;
  }

  void refreshGeometry(){
    try{
      Calc c=compute(false);GCheck q=gcheck(c);
      geomInfo.setText(String.format(Locale.US,
        "%s • load level %d%% • %d layers • %d actuators\nMax tie-rod angle %.2f° | joint utilization %.1f%%\nMax actuator stroke utilization %.1f%% | minimum reserve %.1f%%\nEstimated minimum beam/tank clearance %.1f mm\nFlags: stroke %d | joint %d | clearance %d | beam/beam %d\nGeometry screening only: final rig release requires actual CAD interference analysis, supplier articulation limits and measured/FE displacement envelopes.",
        q.status,loadPct,Math.max(1,(int)Math.round(d(layers))),c.nAct,q.maxLinkDeg,q.maxJointUtil,q.maxStrokeUtil,q.minStrokeReserve,q.minClearMm,q.strokeFail,q.jointFail,q.clearFail,q.beamConflict));
      if(geomView!=null)geomView.invalidate();
    }catch(Exception e){}
  }

  class GeometryCheckView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    GeometryCheckView(){super(V39Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception e){return;}if(c.n<=0)return;GCheck q=gcheck(c);int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("KINEMATIC ENVELOPE / INTERFERENCE MAP",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Stroke, articulation and clearance limits evaluated at current load slider position",dp(12),dp(43),t);

      float left=dp(28),right=W-dp(28),tankY=dp(145);float amp=dp(2)*1.15f;
      float[] sx=new float[c.n],sy=new float[c.n];Path tank=new Path();
      for(int i=0;i<c.n;i++){
        sx[i]=left+(right-left)*(i+.5f)/c.n;
        sy[i]=tankY+(float)(stationDispMm(c,i)*amp);
        if(i==0)tank.moveTo(sx[i],sy[i]);else tank.lineTo(sx[i],sy[i]);
      }
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(18));p.setColor(Color.rgb(67,78,90));cn.drawPath(tank,p);
      p.setStrokeWidth(dp(4));p.setColor(Color.rgb(150,166,182));cn.drawPath(tank,p);

      float beamBase=dp(315),actBase=dp(535),strongY=dp(635);
      double jointLim=Math.max(1,d(jointAngleLimit));
      for(int i=0;i<c.n;i++){
        int a=c.stationAct[i];float ax=left+(right-left)*(a+.5f)/Math.max(1,c.nAct);float by=beamBase+(a%2)*dp(18);
        double ang=Math.toDegrees(Math.atan2(Math.abs(stationDispMm(c,i)),Math.max(50,d(linkLength))));
        int col=ang>jointLim?Color.rgb(229,82,74):(ang>jointLim*.8?Color.rgb(247,207,77):Color.rgb(205,215,225));
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(col);cn.drawLine(sx[i],sy[i]+dp(10),ax,by-dp(10),p);
        p.setStyle(Paint.Style.FILL);p.setColor(col);cn.drawCircle(sx[i],sy[i],dp(5),p);
      }

      double reserveReq=Math.max(0,Math.min(80,d(strokeReservePct)));
      for(int a=0;a<c.nAct;a++){
        float ax=left+(right-left)*(a+.5f)/Math.max(1,c.nAct);float by=beamBase+(a%2)*dp(18);
        int s0=c.actStationStart[a],s1=c.actStationEnd[a];float bx1=sx[Math.max(0,Math.min(s0,c.n-1))]-dp(18),bx2=sx[Math.max(0,Math.min(s1,c.n-1))]+dp(18);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawRoundRect(new RectF(bx1,by-dp(7),bx2,by+dp(7)),dp(4),dp(4),p);
        ACheck ac=chk(c,a);double req=Math.abs(actuatorStrokeMm(c,a));double util=100*req/Math.max(1,ac.strokeSel);double reserve=100-util;
        int acol=(util>100||reserve<reserveReq)?Color.rgb(229,82,74):(util>85?Color.rgb(247,207,77):Color.rgb(55,132,238));
        float bodyTop=actBase+(float)(req*amp*.45);p.setColor(acol);cn.drawRoundRect(new RectF(ax-dp(22),bodyTop,ax+dp(22),bodyTop+dp(70)),dp(7),dp(7),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(Color.rgb(205,215,225));cn.drawLine(ax,by+dp(12),ax,bodyTop,p);
        t.setTextSize(dp(6));t.setColor(Color.WHITE);cn.drawText(String.format(Locale.US,"A%d %.0f%%",a+1,util),ax-dp(17),bodyTop+dp(35),t);
      }

      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(110,125,140));cn.drawRect(dp(20),strongY,W-dp(20),strongY+dp(24),p);
      int scol=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));
      p.setColor(scol);cn.drawCircle(W-dp(40),dp(75),dp(13),p);t.setColor(scol);t.setTextSize(dp(10));cn.drawText("GEOMETRY: "+q.status,dp(18),dp(80),t);

      float y=dp(675);t.setTextSize(dp(7));t.setColor(Color.rgb(185,210,230));
      cn.drawText(String.format(Locale.US,"Joint max %.1f° / %.1f° | stroke max %.1f%% | min clearance %.1f mm",q.maxLinkDeg,jointLim,q.maxStrokeUtil,q.minClearMm),dp(18),y,t);
      cn.drawText("Red = limit exceeded, amber = low margin. Beam collision is a screening flag, not a CAD replacement.",dp(18),y+dp(22),t);
    }
  }
}
