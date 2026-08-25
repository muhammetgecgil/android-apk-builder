package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V44Activity extends V43Activity {
  final String[] faults={"NONE","LOAD CELL DRIFT","ACTUATOR MISMATCH","PRESSURE LOSS","STUCK SERVO VALVE","EXCESS DISPLACEMENT","PAD OVERLOAD","SENSOR DROPOUT"};
  int faultIndex=0; TextView faultInfo,faultLabel; SeekBar faultSeek; FaultView faultView;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("FAULT INJECTION / FAILURE RESPONSE",16,true,Color.WHITE));
    faultLabel=card("FAULT: NONE",Color.rgb(20,48,68));panel.addView(faultLabel,lp());
    faultSeek=new SeekBar(this);faultSeek.setMax(faults.length-1);faultSeek.setProgress(0);panel.addView(faultSeek,new LinearLayout.LayoutParams(-1,dp(52)));
    LinearLayout nav=new LinearLayout(this);Button prev=new Button(this),next=new Button(this);prev.setText("◀ PREV FAULT");next.setText("NEXT FAULT ▶");nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));panel.addView(nav);
    faultInfo=card("Fault model hazır.",Color.rgb(20,48,68));panel.addView(faultInfo,lp());
    faultView=new FaultView();panel.addView(faultView,new LinearLayout.LayoutParams(-1,dp(820)));root.addView(panel,24,lp());
    prev.setOnClickListener(v->{faultIndex=Math.max(0,faultIndex-1);faultSeek.setProgress(faultIndex);refreshFault();});
    next.setOnClickListener(v->{faultIndex=Math.min(faults.length-1,faultIndex+1);faultSeek.setProgress(faultIndex);refreshFault();});
    faultSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean u){faultIndex=p;refreshFault();}});
    refreshFault();
  }

  String action(){switch(faultIndex){case 1:return "WARN → compare redundant channels → HOLD if drift persists";case 2:return "HOLD → synchronize actuator commands → RAMP DOWN if mismatch grows";case 3:return "HOLD → verify HPU/manifold → controlled RAMP DOWN";case 4:return "ABORT command path → isolate valve/actuator channel";case 5:return "HOLD → displacement limit check → RAMP DOWN / ABORT";case 6:return "HOLD → reduce station demand → inspect pad/contact";case 7:return "HOLD if critical channel → switch redundant sensor / ABORT if no valid feedback";default:return "NORMAL TEST SEQUENCE";}}
  double signalScale(){switch(faultIndex){case 1:return .94;case 2:return 1.08;case 3:return .62;case 4:return 1.18;case 5:return 1.0;case 6:return 1.0;case 7:return 0.0;default:return 1.0;}}
  String severity(){if(faultIndex==0)return "PASS";if(faultIndex==1||faultIndex==2||faultIndex==3)return "WARN";return "FAIL";}
  void refreshFault(){try{double cmd=commandKn(),nomMeas=measuredKn(),sig=nomMeas*signalScale(),disp=displacementMm(),press=pressureBar();if(faultIndex==5)disp*=1.65;if(faultIndex==3)press*=.55;if(faultIndex==7)sig=Double.NaN;faultLabel.setText("FAULT: "+faults[faultIndex]);faultInfo.setText(String.format(Locale.US,"%s • %s\nCommand %.1f kN | affected feedback %s\nDisplacement %.2f mm | pressure %.1f bar\nExpected protection action: %s\nEducational fault injection only; real interlock thresholds must be independently verified against test requirements, hardware limits and safety analysis.",faults[faultIndex],severity(),cmd,Double.isNaN(sig)?"NO VALID DATA":String.format(Locale.US,"%.1f kN",sig),disp,press,action()));if(faultView!=null)faultView.invalidate();}catch(Exception e){}}

  class FaultView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);FaultView(){super(V44Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    void box(Canvas c,float l,float top,float r,float b,int col,String s){p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawRoundRect(new RectF(l,top,r,b),dp(8),dp(8),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));c.drawText(s,l+dp(8),(top+b)/2+dp(3),t);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int W=getWidth();t.setColor(Color.WHITE);t.setTextSize(dp(13));c.drawText("FAULT → DETECTION → PROTECTION ACTION",dp(14),dp(28),t);float y=dp(95);box(c,dp(18),y,dp(128),y+dp(55),Color.rgb(55,132,238),"COMMAND");box(c,dp(150),y,dp(280),y+dp(55),faultIndex==4?Color.rgb(229,82,74):Color.rgb(75,110,135),"SERVO / ACT");box(c,dp(302),y,W-dp(18),y+dp(55),faultIndex==1||faultIndex==7?Color.rgb(229,82,74):Color.rgb(75,110,135),"LOAD CELL / DAQ");p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(205,215,225));c.drawLine(dp(128),y+dp(28),dp(150),y+dp(28),p);c.drawLine(dp(280),y+dp(28),dp(302),y+dp(28),p);
      float tankY=dp(285),left=dp(35),right=W-dp(35);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(70,82,95));c.drawRoundRect(new RectF(left,tankY-dp(25),right,tankY+dp(25)),dp(25),dp(25),p);int na=Math.max(1,(int)Math.round(d(actCount)));for(int a=0;a<na;a++){float x=left+(right-left)*(a+.5f)/na;int col=(faultIndex==2&&a==0)||faultIndex==4&&a==0?Color.rgb(229,82,74):Color.rgb(55,132,238);p.setColor(col);c.drawRoundRect(new RectF(x-dp(15),tankY+dp(70),x+dp(15),tankY+dp(138)),dp(6),dp(6),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(Color.rgb(205,215,225));c.drawLine(x,tankY+dp(25),x,tankY+dp(70),p);p.setStyle(Paint.Style.FILL);t.setTextSize(dp(6));t.setColor(Color.WHITE);c.drawText("A"+(a+1),x-dp(7),tankY+dp(108),t);}if(faultIndex==5){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(Color.rgb(229,82,74));c.drawArc(new RectF(left,tankY-dp(70),right,tankY+dp(70)),200,140,false,p);}if(faultIndex==6){p.setColor(Color.rgb(229,82,74));c.drawCircle(W/2,tankY,dp(18),p);t.setTextSize(dp(7));t.setColor(Color.WHITE);c.drawText("PAD",W/2-dp(12),tankY+dp(3),t);}float py=dp(545);int scol=severity().equals("PASS")?Color.rgb(67,190,113):(severity().equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));box(c,dp(20),py,W-dp(20),py+dp(70),scol,"SYSTEM RESPONSE: "+action());t.setColor(Color.rgb(185,210,230));t.setTextSize(dp(7));c.drawText("Injected failures are deterministic training cases, not certified safety logic.",dp(18),dp(660),t);c.drawText("Next release will convert these responses into explicit interlock thresholds and state transitions.",dp(18),dp(685),t);}
  }
}
