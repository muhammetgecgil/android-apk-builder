package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V51Activity extends V50Activity {
  EditText fxIn, fyIn, mxIn, mzIn;
  TextView sixInfo;
  SixDofView sixView;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    fxIn=field("6-DOF Fx [kN]","0");
    fyIn=field("6-DOF Fy [kN]","0");
    mxIn=field("6-DOF Mx [kN·m]","0");
    mzIn=field("6-DOF Mz [kN·m]","0");

    LinearLayout p=new LinearLayout(this);
    p.setOrientation(LinearLayout.VERTICAL);
    p.setPadding(dp(8),dp(8),dp(8),dp(8));
    p.setBackground(bg(Color.rgb(12,31,47),12));
    p.addView(tx("FULL 6-DOF LOAD VECTOR SOLVER",16,true,Color.WHITE));
    sixInfo=card("6-DOF solver running...",Color.rgb(20,48,68));
    p.addView(sixInfo,lp());
    sixView=new SixDofView();
    p.addView(sixView,new LinearLayout.LayoutParams(-1,dp(900)));
    root.addView(p,31,lp());

    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){refresh6();} public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{fxIn,fyIn,mxIn,mzIn,F,M,L,D,stations,actCount,actCapacity,sf})e.addTextChangedListener(w);
    refresh6();
  }

  static class SixResult {
    double fx,fy,fz,mx,my,mz,fmag,mmag,peakStation,peakAct;
    double[] stationMag,actMag;
    boolean closure;
  }

  SixResult solve6(){
    Calc c=compute(false); SixResult r=new SixResult();
    r.fx=d(fxIn); r.fy=d(fyIn); r.fz=d(F); r.mx=d(mxIn); r.my=d(M); r.mz=d(mzIn);
    r.fmag=Math.sqrt(r.fx*r.fx+r.fy*r.fy+r.fz*r.fz);
    r.mmag=Math.sqrt(r.mx*r.mx+r.my*r.my+r.mz*r.mz);
    r.stationMag=new double[c.n]; r.actMag=new double[c.nAct];
    double rad=Math.max(.05,c.D/2.0); double len=Math.max(.5,c.L);
    for(int i=0;i<c.n;i++){
      double x=(c.x[i]-len/2.0);
      double fx=r.fx/c.n + r.mz*x/Math.max(.1,len*len/12.0*c.n);
      double fy=r.fy/c.n + r.mx/(Math.max(1,c.n)*rad);
      double fz=c.fi[i] + r.my*x/Math.max(.1,len*len/12.0*c.n);
      double mag=Math.sqrt(fx*fx+fy*fy+fz*fz);
      r.stationMag[i]=mag; r.peakStation=Math.max(r.peakStation,mag);
      int a=c.stationAct[i]; if(a>=0&&a<r.actMag.length)r.actMag[a]+=mag*Math.max(1,c.sf);
    }
    for(double v:r.actMag)r.peakAct=Math.max(r.peakAct,v);
    double sx=0,sy=0,sz=0;
    for(int i=0;i<c.n;i++){
      double share=Math.max(1e-9,r.stationMag[i]);
      sx+=Math.abs(r.fx)/c.n; sy+=Math.abs(r.fy)/c.n; sz+=Math.abs(r.fz)/c.n;
    }
    double forceErr=100*Math.abs(Math.sqrt(sx*sx+sy*sy+sz*sz)-r.fmag)/Math.max(1,r.fmag);
    r.closure=forceErr<2.0 && r.peakAct<=Math.max(1,d(actCapacity))*1.15;
    return r;
  }

  void refresh6(){
    try{
      SixResult r=solve6();
      sixInfo.setText(String.format(Locale.US,
        "%s\nFORCE VECTOR  Fx %.1f | Fy %.1f | Fz %.1f kN | |F| %.1f kN\nMOMENT VECTOR Mx %.1f | My %.1f | Mz %.1f kN·m | |M| %.1f\nPeak station resultant %.1f kN | peak actuator resultant %.1f kN\nActuator nominal %.1f kN | utilization %.1f%%\n6-DOF distribution is a concept-level equilibrium/sizing model; final load introduction requires full 3D geometry and structural substantiation.",
        r.closure?"6-DOF SCREEN: PASS":"6-DOF SCREEN: REVIEW",r.fx,r.fy,r.fz,r.fmag,r.mx,r.my,r.mz,r.mmag,r.peakStation,r.peakAct,d(actCapacity),100*r.peakAct/Math.max(1,d(actCapacity))));
      sixView.r=r; sixView.invalidate();
    }catch(Exception e){sixInfo.setText("6-DOF solver could not evaluate current inputs.");}
  }

  class SixDofView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG); SixResult r;
    SixDofView(){super(V51Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c); if(r==null)return; int W=getWidth(); float cx=W/2f, cy=dp(210);
      t.setColor(Color.WHITE);t.setTextSize(dp(13));c.drawText("EFT 6-DOF VECTOR MAP",dp(16),dp(32),t);
      p.setColor(Color.rgb(70,82,95));c.drawRoundRect(new RectF(dp(35),cy-dp(40),W-dp(35),cy+dp(40)),dp(40),dp(40),p);
      drawArrow(c,cx,cy,cx+scale(r.fx),cy,Color.rgb(229,82,74),"Fx");
      drawArrow(c,cx,cy,cx,cy-scale(r.fz),Color.rgb(247,207,77),"Fz");
      drawArrow(c,cx,cy,cx-scale(r.fy)*.65f,cy+scale(r.fy)*.45f,Color.rgb(51,205,220),"Fy");
      t.setTextSize(dp(8));t.setColor(Color.rgb(160,110,230));c.drawText(String.format(Locale.US,"Mx %.0f",r.mx),dp(40),cy+dp(110),t);
      c.drawText(String.format(Locale.US,"My %.0f",r.my),W/2-dp(25),cy+dp(110),t);
      c.drawText(String.format(Locale.US,"Mz %.0f",r.mz),W-dp(95),cy+dp(110),t);
      float y=cy+dp(180);t.setColor(Color.WHITE);t.setTextSize(dp(9));c.drawText("STATION RESULTANTS",dp(18),y,t);y+=dp(25);
      double max=1;for(double v:r.stationMag)max=Math.max(max,v);
      for(int i=0;i<r.stationMag.length;i++){float x=dp(25)+(W-dp(50))*(i+.5f)/r.stationMag.length;float h=(float)(dp(140)*r.stationMag[i]/max);p.setColor(Color.rgb(247,207,77));c.drawRect(x-dp(5),y+dp(150)-h,x+dp(5),y+dp(150),p);t.setTextSize(dp(6));t.setColor(Color.rgb(190,215,232));c.drawText("S"+(i+1),x-dp(6),y+dp(170),t);}
      y+=dp(210);t.setColor(Color.WHITE);t.setTextSize(dp(9));c.drawText("ACTUATOR RESULTANTS",dp(18),y,t);y+=dp(35);
      for(int i=0;i<r.actMag.length;i++){double util=100*r.actMag[i]/Math.max(1,d(actCapacity));p.setColor(util>100?Color.rgb(229,82,74):(util>85?Color.rgb(247,207,77):Color.rgb(67,190,113)));c.drawRoundRect(new RectF(dp(22),y,W-dp(22),y+dp(55)),dp(6),dp(6),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));c.drawText(String.format(Locale.US,"A%d  %.1f kN   %.1f%%",i+1,r.actMag[i],util),dp(36),y+dp(34),t);y+=dp(65);}
    }
    float scale(double v){return (float)Math.max(-dp(120),Math.min(dp(120),v*dp(120)/Math.max(1,r.fmag)));}
    void drawArrow(Canvas c,float x1,float y1,float x2,float y2,int col,String s){p.setColor(col);p.setStrokeWidth(dp(4));c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);c.drawCircle(x2,y2,dp(6),p);t.setColor(col);t.setTextSize(dp(8));c.drawText(s,x2+dp(8),y2,t);}
  }
}
