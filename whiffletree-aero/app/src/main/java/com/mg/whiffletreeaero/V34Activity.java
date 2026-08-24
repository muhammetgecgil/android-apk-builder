package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V34Activity extends V33Activity {
  SensorDaqView daqView; TextView daqInfo;
  EditText strainPerStation, extraDisp, sampleRate, adcBits;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    strainPerStation=field("Strain-gauge channels per station","4");
    extraDisp=field("Extra displacement channels","2");
    sampleRate=field("DAQ sample rate per channel [Hz]","1000");
    adcBits=field("ADC resolution [bit]","24");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("SENSOR / DAQ ARCHITECTURE",16,true,Color.WHITE));
    daqInfo=card("Sensor ve DAQ mimarisi hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(daqInfo,lp());
    daqView=new SensorDaqView();
    panel.addView(daqView,new LinearLayout.LayoutParams(-1,dp(600)));
    root.addView(panel,14,lp());

    TextWatcher refresh=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int b,int c){}
      public void onTextChanged(CharSequence s,int a,int b,int c){refreshDaq();}
      public void afterTextChanged(Editable e){}
    };
    strainPerStation.addTextChangedListener(refresh);extraDisp.addTextChangedListener(refresh);sampleRate.addTextChangedListener(refresh);adcBits.addTextChangedListener(refresh);
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
    refreshDaq();
  }

  static class DaqCheck{int lc,lvdt,press,strain,disp,total;double fs,bits,dataMbps;String status;}
  DaqCheck daqCheck(Calc c){
    DaqCheck q=new DaqCheck();
    q.lc=c.nAct; q.lvdt=c.nAct; q.press=c.nAct;
    q.strain=Math.max(0,(int)Math.round(d(strainPerStation)))*c.n;
    q.disp=Math.max(0,(int)Math.round(d(extraDisp)));
    q.total=q.lc+q.lvdt+q.press+q.strain+q.disp;
    q.fs=Math.max(1,d(sampleRate)); q.bits=Math.max(8,d(adcBits));
    q.dataMbps=q.total*q.fs*q.bits/1e6;
    q.status=q.total<=128&&q.dataMbps<=10?"PASS":(q.total<=256&&q.dataMbps<=25?"WARN":"REVIEW");
    return q;
  }

  void refreshDaq(){
    try{
      Calc c=compute(false);DaqCheck q=daqCheck(c);
      daqInfo.setText(String.format(Locale.US,
        "%s • %d total measurement channels\nLoad cells %d | actuator LVDT %d | pressure transducers %d\nStrain gauges %d (%d/station × %d stations) | extra displacement %d\nSample rate %.0f Hz/ch | ADC %.0f bit | raw payload ≈ %.2f Mbit/s\nRecommended grouping: FORCE / POSITION / PRESSURE / STRAIN / DISPLACEMENT\nArchitecture: sensors → signal conditioning → synchronized DAQ → safety/interlock + test controller",
        q.status,q.total,q.lc,q.lvdt,q.press,q.strain,Math.max(0,(int)Math.round(d(strainPerStation))),c.n,q.disp,q.fs,q.bits,q.dataMbps));
      if(daqView!=null)daqView.invalidate();
    }catch(Exception e){}
  }

  class SensorDaqView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    SensorDaqView(){super(V34Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception ex){return;}DaqCheck q=daqCheck(c);int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("EFT TEST SENSOR + DAQ 2D MAP",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Channel count follows station and actuator configuration",dp(12),dp(43),t);

      float y=dp(105);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(62,72,84));cn.drawRoundRect(new RectF(dp(28),y,W-dp(28),y+dp(64)),dp(28),dp(28),p);
      for(int i=0;i<c.n;i++){
        float x=dp(45)+(W-dp(90))*(i+.5f)/Math.max(1,c.n);
        p.setColor(Color.rgb(247,207,77));cn.drawCircle(x,y+dp(18),dp(5),p);
        t.setColor(Color.WHITE);t.setTextSize(dp(6));cn.drawText("S"+(i+1),x-dp(5),y-dp(5),t);
      }
      t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("EFT + strain/displacement stations",W/2-dp(62),y+dp(48),t);

      float ay=dp(215);for(int a=0;a<c.nAct;a++){
        float x=dp(35)+(W-dp(70))*(a+.5f)/Math.max(1,c.nAct);
        p.setColor(Color.rgb(55,132,238));cn.drawRoundRect(new RectF(x-dp(23),ay,x+dp(23),ay+dp(56)),dp(6),dp(6),p);
        t.setColor(Color.WHITE);t.setTextSize(dp(7));cn.drawText("ACT"+(a+1),x-dp(14),ay+dp(18),t);cn.drawText("LC/LVDT/P",x-dp(19),ay+dp(39),t);
      }

      float by=dp(330);
      box(cn,dp(18),by,dp(105),dp(58),"SIGNAL\nCOND.",Color.rgb(51,205,220));
      box(cn,dp(143),by,dp(110),dp(58),"SYNC DAQ\n"+q.total+" CH",Color.rgb(160,110,230));
      box(cn,dp(273),by,dp(105),dp(58),"TEST\nCONTROL",Color.rgb(55,132,238));
      box(cn,W-dp(118),by,dp(100),dp(58),"SAFETY /\nINTERLOCK",Color.rgb(229,82,74));
      wire(cn,dp(123),by+dp(29),dp(143),by+dp(29));wire(cn,dp(253),by+dp(29),dp(273),by+dp(29));wire(cn,dp(378),by+dp(29),W-dp(118),by+dp(29));

      float my=dp(440);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),my,W-dp(16),my+dp(125)),dp(8),dp(8),p);
      t.setColor(Color.WHITE);t.setTextSize(dp(8));
      cn.drawText("FORCE      "+q.lc+" ch  • Load cells",dp(28),my+dp(23),t);
      cn.drawText("POSITION   "+q.lvdt+" ch  • Actuator LVDT",dp(28),my+dp(46),t);
      cn.drawText("PRESSURE   "+q.press+" ch  • Hydraulic channels",dp(28),my+dp(69),t);
      cn.drawText("STRAIN     "+q.strain+" ch  • EFT stations",dp(28),my+dp(92),t);
      cn.drawText("DISP.      "+q.disp+" ch  • Independent references",dp(28),my+dp(115),t);
      int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));
      p.setColor(col);cn.drawCircle(W-dp(40),my+dp(98),dp(12),p);
      t.setColor(col);t.setTextSize(dp(9));cn.drawText(String.format(Locale.US,"DAQ: %s • %.2f Mbit/s raw",q.status,q.dataMbps),dp(20),dp(590),t);
    }
    void wire(Canvas cn,float x1,float y1,float x2,float y2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.rgb(205,215,225));cn.drawLine(x1,y1,x2,y2,p);}
    void box(Canvas cn,float x,float y,float w,float h,String s,int col){p.setStyle(Paint.Style.FILL);p.setColor(col);cn.drawRoundRect(new RectF(x,y,x+w,y+h),dp(7),dp(7),p);t.setColor(Color.WHITE);t.setTextSize(dp(7));String[] a=s.split("\\n");for(int i=0;i<a.length;i++)cn.drawText(a[i],x+dp(9),y+dp(22)+i*dp(15),t);}
  }
}
