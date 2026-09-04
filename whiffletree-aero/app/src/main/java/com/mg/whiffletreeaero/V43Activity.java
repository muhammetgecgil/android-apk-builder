package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V43Activity extends V42Activity {
  final String[] steps={"PRELOAD","25%","50%","75%","100%","HOLD","UNLOAD"};
  final double[] frac={0.05,0.25,0.50,0.75,1.00,1.00,0.00};
  int seqStep=0;
  SeekBar seqSeek; TextView seqInfo,seqLabel; SequenceView seqView;
  EditText measuredError,nomPressure,holdTime;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    measuredError=field("Measured load tracking error [%]","1.5");
    nomPressure=field("Nominal hydraulic pressure [bar]","210");
    holdTime=field("100% hold duration [s]","30");
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("TEST SEQUENCE / COMMAND–MEASURED TRACKING",16,true,Color.WHITE));
    seqLabel=card("STEP 1/7 • PRELOAD",Color.rgb(20,48,68));panel.addView(seqLabel,lp());
    seqSeek=new SeekBar(this);seqSeek.setMax(6);seqSeek.setProgress(0);panel.addView(seqSeek,new LinearLayout.LayoutParams(-1,dp(52)));
    LinearLayout nav=new LinearLayout(this);Button prev=new Button(this),next=new Button(this);prev.setText("◀ PREV STEP");next.setText("NEXT STEP ▶");nav.addView(prev,new LinearLayout.LayoutParams(0,dp(46),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(46),1));panel.addView(nav);
    seqInfo=card("Sequence hazırlanıyor...",Color.rgb(20,48,68));panel.addView(seqInfo,lp());
    seqView=new SequenceView();panel.addView(seqView,new LinearLayout.LayoutParams(-1,dp(820)));root.addView(panel,23,lp());
    prev.setOnClickListener(v->{seqStep=Math.max(0,seqStep-1);seqSeek.setProgress(seqStep);refreshSequence();});
    next.setOnClickListener(v->{seqStep=Math.min(6,seqStep+1);seqSeek.setProgress(seqStep);refreshSequence();});
    seqSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean u){seqStep=p;refreshSequence();}});
    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){refreshSequence();}public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{measuredError,nomPressure,holdTime,F,M,deflection,actCount,actCapacity,sf})e.addTextChangedListener(w);refreshSequence();
  }

  double commandKn(){return Math.max(0,d(F))*frac[seqStep];}
  double measuredKn(){double err=Math.max(-20,Math.min(20,d(measuredError)))/100.0;return commandKn()*(1.0-err);}
  double displacementMm(){return Math.max(0,d(deflection))*frac[seqStep];}
  double pressureBar(){return Math.max(0,d(nomPressure))*frac[seqStep];}

  void refreshSequence(){
    try{
      Calc c=compute(false);double cmd=commandKn(),meas=measuredKn(),err=cmd>0?100*Math.abs(cmd-meas)/cmd:0;double disp=displacementMm(),press=pressureBar();
      int na=Math.max(1,c.nAct);double perAct=cmd/na;double cap=Math.max(1,d(actCapacity));double util=100*perAct*c.sf/cap;
      String state=err>5||util>100?"FAIL":(err>2||util>85?"WARN":"PASS");
      seqLabel.setText(String.format(Locale.US,"STEP %d/7 • %s",seqStep+1,steps[seqStep]));
      seqInfo.setText(String.format(Locale.US,"%s • %s\nCommand %.2f kN | simulated measured %.2f kN | tracking error %.2f%%\nPer-actuator command %.2f kN | design utilization %.1f%%\nDisplacement target %.2f mm | hydraulic pressure target %.1f bar\n%s%s\nSequence is an engineering simulator: real measured values must come from synchronized load-cell/LVDT/pressure DAQ.",steps[seqStep],state,cmd,meas,err,perAct,util,disp,press,seqStep==5?String.format(Locale.US,"Hold duration %.0f s. ",Math.max(0,d(holdTime))):"",seqStep==6?"Unload to zero command.":""));
      if(seqView!=null)seqView.invalidate();
    }catch(Exception e){}
  }

  class SequenceView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);SequenceView(){super(V43Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){super.onDraw(cn);int W=getWidth();t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("EFT STATIC TEST LOAD SEQUENCE",dp(14),dp(28),t);
      float left=dp(24),right=W-dp(24),y=dp(95);for(int i=0;i<7;i++){float x=left+(right-left)*i/6f;p.setColor(i<seqStep?Color.rgb(67,190,113):(i==seqStep?Color.rgb(247,207,77):Color.rgb(90,105,120)));cn.drawCircle(x,y,dp(9),p);if(i<6){p.setStrokeWidth(dp(4));p.setColor(i<seqStep?Color.rgb(67,190,113):Color.rgb(90,105,120));cn.drawLine(x+dp(9),y,left+(right-left)*(i+1)/6f-dp(9),y,p);}t.setTextSize(dp(6));t.setColor(Color.WHITE);cn.drawText(steps[i],x-dp(14),y+dp(28),t);}
      float chartTop=dp(175),chartBottom=dp(420);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.rgb(95,115,130));cn.drawRect(left,chartTop,right,chartBottom,p);double max=Math.max(1,d(F));Path cmd=new Path(),meas=new Path();for(int i=0;i<7;i++){float x=left+(right-left)*i/6f;float yc=(float)(chartBottom-(chartBottom-chartTop)*frac[i]);double err=Math.max(-20,Math.min(20,d(measuredError)))/100.0;float ym=(float)(chartBottom-(chartBottom-chartTop)*frac[i]*(1-err));if(i==0){cmd.moveTo(x,yc);meas.moveTo(x,ym);}else{cmd.lineTo(x,yc);meas.lineTo(x,ym);}}p.setStrokeWidth(dp(4));p.setColor(Color.rgb(51,205,220));cn.drawPath(cmd,p);p.setColor(Color.rgb(247,207,77));cn.drawPath(meas,p);p.setStyle(Paint.Style.FILL);t.setTextSize(dp(8));t.setColor(Color.rgb(51,205,220));cn.drawText("COMMAND",left,chartTop-dp(12),t);t.setColor(Color.rgb(247,207,77));cn.drawText("MEASURED (SIM)",left+dp(90),chartTop-dp(12),t);
      float rigY=dp(555);p.setColor(Color.rgb(70,82,95));cn.drawRoundRect(new RectF(left,rigY-dp(25),right,rigY+dp(25)),dp(25),dp(25),p);int na=Math.max(1,(int)Math.round(d(actCount)));double lf=frac[seqStep];for(int a=0;a<na;a++){float x=left+(right-left)*(a+.5f)/na;p.setColor(Color.rgb(55,132,238));cn.drawRoundRect(new RectF(x-dp(16),rigY+dp(55),x+dp(16),rigY+dp(125)),dp(6),dp(6),p);p.setStrokeWidth(dp(5));p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(205,215,225));cn.drawLine(x,rigY+dp(25),x,rigY+dp(55)+(float)(lf*dp(18)),p);p.setStyle(Paint.Style.FILL);t.setTextSize(dp(6));t.setColor(Color.WHITE);cn.drawText("A"+(a+1),x-dp(7),rigY+dp(95),t);}t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText(String.format(Locale.US,"Current: %s • %.0f%% load • %.1f kN command • %.1f mm displacement",steps[seqStep],100*frac[seqStep],commandKn(),displacementMm()),dp(18),dp(760),t);cn.drawText("Control concept: command → servo/hydraulic actuator → load cell/LVDT/pressure feedback → compare → hold/ramp/unload",dp(18),dp(790),t);}
  }
}
