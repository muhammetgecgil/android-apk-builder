package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V45Activity extends V44Activity {
  EditText warnTrack,holdTrack,abortTrack,minPressurePct,maxDispPct,minStrokeReservePct;
  TextView interlockInfo; InterlockView interlockView;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    warnTrack=field("Warning tracking error [%]","2");
    holdTrack=field("Hold tracking error [%]","5");
    abortTrack=field("Abort tracking error [%]","10");
    minPressurePct=field("Minimum hydraulic pressure [% nominal]","70");
    maxDispPct=field("Maximum displacement [% target]","120");
    minStrokeReservePct=field("Minimum stroke reserve [%]","10");
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("SAFETY / INTERLOCK STATE MACHINE",16,true,Color.WHITE));
    interlockInfo=card("Interlock evaluation hazırlanıyor...",Color.rgb(20,48,68));panel.addView(interlockInfo,lp());
    interlockView=new InterlockView();panel.addView(interlockView,new LinearLayout.LayoutParams(-1,dp(840)));root.addView(panel,25,lp());
    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){refreshInterlock();} public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{warnTrack,holdTrack,abortTrack,minPressurePct,maxDispPct,minStrokeReservePct,measuredError,nomPressure,holdTime,deflection,actCapacity,actCount,F,sf})e.addTextChangedListener(w);
    if(faultSeek!=null)faultSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean u){faultIndex=p;refreshFault();refreshInterlock();}});
    refreshInterlock();
  }

  static class IState{String state,reason,action;double tracking,pressurePct,dispPct,strokeReserve;}
  IState istate(){
    IState q=new IState();double cmd=commandKn(),meas=measuredKn()*signalScale();if(faultIndex==7)meas=Double.NaN;
    q.tracking=cmd>0&&Double.isFinite(meas)?100*Math.abs(cmd-meas)/cmd:0;
    double press=pressureBar();if(faultIndex==3)press*=.55;q.pressurePct=Math.max(0,d(nomPressure))>0?100*press/Math.max(1,d(nomPressure)):100;
    double disp=displacementMm();if(faultIndex==5)disp*=1.65;q.dispPct=Math.max(.001,Math.max(0,d(deflection))*Math.max(.01,frac[seqStep]))>0?100*disp/Math.max(.001,Math.max(0,d(deflection))*Math.max(.01,frac[seqStep])):0;
    Calc c=compute(false);double minRes=100;for(int a=0;a<c.nAct;a++){ACheck ac=chk(c,a);double req=Math.abs(actuatorStrokeMm(c,a))*frac[seqStep];double util=100*req/Math.max(1,ac.strokeSel);minRes=Math.min(minRes,100-util);}q.strokeReserve=minRes;
    double w=Math.max(0,d(warnTrack)),h=Math.max(w,d(holdTrack)),ab=Math.max(h,d(abortTrack));double pmin=Math.max(0,d(minPressurePct)),dmax=Math.max(100,d(maxDispPct)),smin=Math.max(0,d(minStrokeReservePct));
    if(faultIndex==4||faultIndex==7||q.tracking>=ab||q.pressurePct<40||q.dispPct>150||q.strokeReserve<0){q.state="ABORT";q.action="Remove force command, isolate affected channel, retain data and require operator reset.";}
    else if(q.tracking>=h||q.pressurePct<pmin||q.dispPct>dmax||q.strokeReserve<smin||faultIndex==5||faultIndex==6){q.state="HOLD";q.action="Freeze current safe command, inhibit further ramp and evaluate cause.";}
    else if(q.tracking>=w||faultIndex==2||faultIndex==3){q.state="WARNING";q.action="Continue only under supervised limit monitoring; prepare HOLD if deviation grows.";}
    else q.state="NORMAL";
    if(q.state.equals("NORMAL")&&faultIndex==1){q.state="WARNING";q.action="Cross-check redundant load measurement and trend drift.";}
    q.reason=String.format(Locale.US,"tracking %.1f%% | pressure %.1f%% | displacement %.1f%% | stroke reserve %.1f%%",q.tracking,q.pressurePct,q.dispPct,q.strokeReserve);
    return q;
  }
  void refreshInterlock(){try{IState q=istate();interlockInfo.setText(String.format(Locale.US,"STATE: %s\n%s\nFault input: %s | sequence: %s\nRequired action: %s\nThresholds: warning %.1f%%, hold %.1f%%, abort %.1f%% tracking; pressure min %.0f%%; displacement max %.0f%%; stroke reserve min %.0f%%.\nConcept safety logic only. Final interlocks require independent hazard analysis, hardware fail-safe design, validated sensor voting and test-specific approval.",q.state,q.reason,faults[faultIndex],steps[seqStep],q.action,d(warnTrack),d(holdTrack),d(abortTrack),d(minPressurePct),d(maxDispPct),d(minStrokeReservePct)));if(interlockView!=null)interlockView.invalidate();}catch(Exception e){}}

  class InterlockView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);InterlockView(){super(V45Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    int col(String s){if(s.equals("NORMAL"))return Color.rgb(67,190,113);if(s.equals("WARNING"))return Color.rgb(247,207,77);if(s.equals("HOLD"))return Color.rgb(245,145,60);return Color.rgb(229,82,74);}
    void node(Canvas c,float x,float y,String s,boolean active){p.setStyle(Paint.Style.FILL);p.setColor(active?col(s):Color.rgb(55,70,82));c.drawRoundRect(new RectF(x-dp(48),y-dp(24),x+dp(48),y+dp(24)),dp(9),dp(9),p);t.setTextSize(dp(8));t.setColor(Color.WHITE);c.drawText(s,x-dp(27),y+dp(3),t);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);IState q;try{q=istate();}catch(Exception e){return;}int W=getWidth();t.setColor(Color.WHITE);t.setTextSize(dp(13));c.drawText("INTERLOCK STATE TRANSITION LOGIC",dp(14),dp(28),t);float cx=W/2f;String[] states={"NORMAL","WARNING","HOLD","RAMP DOWN","ABORT"};float y=dp(95);for(int i=0;i<states.length;i++){boolean a=q.state.equals(states[i])||(q.state.equals("HOLD")&&states[i].equals("RAMP DOWN")&&false);node(c,cx,y+i*dp(105),states[i],a);if(i<states.length-1){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(150,170,185));c.drawLine(cx,y+i*dp(105)+dp(24),cx,y+(i+1)*dp(105)-dp(24),p);}}
      t.setTextSize(dp(7));t.setColor(Color.rgb(185,210,230));c.drawText("Escalation inputs",dp(18),dp(115),t);c.drawText("tracking error",dp(18),dp(145),t);c.drawText("pressure low",dp(18),dp(170),t);c.drawText("displacement high",dp(18),dp(195),t);c.drawText("stroke reserve low",dp(18),dp(220),t);c.drawText("sensor invalid",dp(18),dp(245),t);
      float gy=dp(650);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(16,43,62));c.drawRoundRect(new RectF(dp(16),gy,W-dp(16),gy+dp(135)),dp(10),dp(10),p);t.setTextSize(dp(8));t.setColor(col(q.state));c.drawText("ACTIVE STATE: "+q.state,dp(28),gy+dp(28),t);t.setColor(Color.WHITE);t.setTextSize(dp(7));c.drawText(q.reason,dp(28),gy+dp(56),t);c.drawText("Action:",dp(28),gy+dp(82),t);t.setColor(Color.rgb(185,210,230));c.drawText(q.action.length()>70?q.action.substring(0,70):q.action,dp(28),gy+dp(106),t);c.drawText("Operator reset required after ABORT; automatic restart is intentionally not modeled.",dp(28),gy+dp(128),t);}
  }
}
