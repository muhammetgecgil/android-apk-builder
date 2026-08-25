package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V38Activity extends V37Activity {
  SeekBar loadSeek; TextView kinInfo,loadLabel; KinematicView kinView; int loadPct=100;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("DEFORMED EFT / KINEMATIC 2D",16,true,Color.WHITE));
    loadLabel=card("LOAD LEVEL: 100%",Color.rgb(20,48,68));panel.addView(loadLabel,lp());
    loadSeek=new SeekBar(this);loadSeek.setMax(100);loadSeek.setProgress(100);panel.addView(loadSeek,new LinearLayout.LayoutParams(-1,dp(52)));
    kinInfo=card("Kinematic geometry hazırlanıyor...",Color.rgb(20,48,68));panel.addView(kinInfo,lp());
    kinView=new KinematicView();panel.addView(kinView,new LinearLayout.LayoutParams(-1,dp(700)));
    root.addView(panel,18,lp());
    loadSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} public void onProgressChanged(SeekBar s,int v,boolean u){loadPct=v;refreshKin();}});
    refreshKin();
  }

  double loadFrac(){return Math.max(0,Math.min(100,loadPct))/100.0;}
  double stationDispMm(Calc c,int i){
    double lf=loadFrac(),base=Math.max(0,d(deflection));
    if(c.n<=1)return base*lf;
    double xi=-1.0+2.0*i/(double)(c.n-1);
    double shape=1.0-xi*xi;
    double asym=0.35*xi*(c.mnm==0?0:Math.signum(c.mnm));
    return base*lf*Math.max(-.2,shape+asym);
  }
  double actuatorStrokeMm(Calc c,int a){
    int s0=c.actStationStart[a],s1=c.actStationEnd[a];double sum=0;int n=0;
    for(int i=s0;i<=s1&&i<c.n;i++){sum+=stationDispMm(c,i);n++;}
    return n>0?sum/n:0;
  }
  void refreshKin(){
    try{
      Calc c=compute(false);double maxD=0,maxS=0,maxAng=0;
      for(int i=0;i<c.n;i++)maxD=Math.max(maxD,Math.abs(stationDispMm(c,i)));
      for(int a=0;a<c.nAct;a++)maxS=Math.max(maxS,Math.abs(actuatorStrokeMm(c,a)));
      double link=Math.max(50,d(linkLength));maxAng=Math.toDegrees(Math.atan2(maxD,link));
      loadLabel.setText("LOAD LEVEL: "+loadPct+"%");
      kinInfo.setText(String.format(Locale.US,"%d%% applied load • %d stations • %d layers • %d actuators\nMax displayed EFT station displacement %.2f mm\nMax actuator kinematic stroke demand %.2f mm\nEstimated max link angular change %.2f°\nThis view is a geometric/educational kinematic representation; final displacement field must come from structural analysis or measured test data.",loadPct,c.n,c.nl,c.nAct,maxD,maxS,maxAng));
      if(kinView!=null)kinView.invalidate();
    }catch(Exception e){}
  }

  class KinematicView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    KinematicView(){super(V38Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception e){return;}if(c.n<=0)return;int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("EFT + WHIFFLETREE KINEMATIC LOAD PATH",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Slider changes station deflection, link angle and actuator stroke together",dp(12),dp(43),t);

      float left=dp(28),right=W-dp(28),tankY=dp(145);float amp=dp(2.3f);
      Path tank=new Path();
      float[] sx=new float[c.n],sy=new float[c.n];
      for(int i=0;i<c.n;i++){
        sx[i]=left+(right-left)*(i+.5f)/c.n;
        sy[i]=tankY+(float)(stationDispMm(c,i)*amp);
        if(i==0)tank.moveTo(sx[i],sy[i]); else tank.lineTo(sx[i],sy[i]);
      }
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(18));p.setColor(Color.rgb(67,78,90));cn.drawPath(tank,p);
      p.setStrokeWidth(dp(4));p.setColor(Color.rgb(150,166,182));cn.drawPath(tank,p);

      float beamBase=dp(310),actBase=dp(545),strongY=dp(615);
      for(int i=0;i<c.n;i++){
        int a=c.stationAct[i];float ax=left+(right-left)*(a+.5f)/Math.max(1,c.nAct);float by=beamBase+(a%2)*dp(18);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(205,215,225));cn.drawLine(sx[i],sy[i]+dp(10),ax,by-dp(10),p);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawRoundRect(new RectF(sx[i]-dp(11),sy[i]-dp(7),sx[i]+dp(11),sy[i]+dp(7)),dp(4),dp(4),p);
        t.setColor(Color.WHITE);t.setTextSize(dp(6));cn.drawText("S"+(i+1),sx[i]-dp(6),sy[i]-dp(14),t);
      }

      for(int a=0;a<c.nAct;a++){
        float ax=left+(right-left)*(a+.5f)/Math.max(1,c.nAct);float by=beamBase+(a%2)*dp(18);double stroke=actuatorStrokeMm(c,a);float strokePx=(float)(stroke*amp*.65);
        int s0=c.actStationStart[a],s1=c.actStationEnd[a];float bx1=sx[Math.max(0,Math.min(s0,c.n-1))]-dp(18),bx2=sx[Math.max(0,Math.min(s1,c.n-1))]+dp(18);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawRoundRect(new RectF(bx1,by-dp(7),bx2,by+dp(7)),dp(4),dp(4),p);
        p.setColor(Color.rgb(51,205,220));cn.drawCircle(ax,by,dp(8),p);
        float top=by+dp(28),bodyTop=actBase+strokePx;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(6));p.setColor(Color.rgb(205,215,225));cn.drawLine(ax,top,ax,bodyTop,p);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(55,132,238));cn.drawRoundRect(new RectF(ax-dp(24),bodyTop,ax+dp(24),bodyTop+dp(72)),dp(7),dp(7),p);
        t.setColor(Color.WHITE);t.setTextSize(dp(7));cn.drawText("A"+(a+1),ax-dp(8),bodyTop+dp(28),t);cn.drawText(String.format(Locale.US,"%.1f",stroke),ax-dp(10),bodyTop+dp(49),t);
      }

      p.setColor(Color.rgb(110,125,140));cn.drawRect(dp(20),strongY,W-dp(20),strongY+dp(26),p);t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("STRONGBACK / FLOOR REACTION LINE",W/2-dp(78),strongY+dp(18),t);
      t.setColor(Color.rgb(185,210,230));t.setTextSize(dp(7));cn.drawText("Station displacement is scaled visually for clarity; actuator motion follows group-average station displacement.",dp(18),dp(680),t);
    }
  }
}
