package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V46Activity extends V45Activity {
  final String[] titles={
    "Uniform vertical load","Vertical load + pitch moment","High nose-side load","High tail-side load","Two-actuator introduction","Four-actuator introduction","One-layer feasibility","Two-layer whiffletree","Three-layer whiffletree","Unequal station loads",
    "Pivot ratio design","Beam strength sizing","Pin / clevis sizing","Load-cell selection","Actuator bore and stroke","Pad contact pressure","Hydraulic flow sizing","DAQ and sensor layout","Combined load case","Full EFT rig concept"
  };
  final double[] exF={80,100,120,120,160,200,120,160,200,180,160,200,180,220,240,160,200,180,220,260};
  final double[] exM={0,25,40,-40,20,30,0,20,35,45,50,30,20,40,35,25,20,30,50,60};
  final int[] exStations={4,4,5,5,6,8,4,6,8,8,6,8,6,8,8,6,8,8,8,10};
  final int[] exAct={1,2,2,2,2,4,2,2,4,4,2,4,3,4,4,3,4,4,4,6};
  final int[] exLayers={1,1,2,2,2,2,1,2,3,3,2,3,2,3,3,2,3,3,3,4};
  int problem=0,solStep=0; SeekBar problemSeek,stepSeek; TextView problemInfo,solutionInfo; TrainingView trainView;
  final String[] solNames={"1. LOAD DEFINITION","2. STATION DISTRIBUTION","3. TOPOLOGY","4. BEAM / PIVOT","5. ACTUATOR / LOAD CELL","6. PAD / JOINT CHECK","7. BOM / RESULT"};

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("20 EFT TRAINING PROBLEMS / STEP-BY-STEP SOLVER",16,true,Color.WHITE));
    problemInfo=card("Problem 1/20",Color.rgb(20,48,68));panel.addView(problemInfo,lp());
    problemSeek=new SeekBar(this);problemSeek.setMax(19);panel.addView(problemSeek,new LinearLayout.LayoutParams(-1,dp(48)));
    solutionInfo=card("Solution step",Color.rgb(20,48,68));panel.addView(solutionInfo,lp());
    stepSeek=new SeekBar(this);stepSeek.setMax(solNames.length-1);panel.addView(stepSeek,new LinearLayout.LayoutParams(-1,dp(48)));
    trainView=new TrainingView();panel.addView(trainView,new LinearLayout.LayoutParams(-1,dp(900)));root.addView(panel,26,lp());
    problemSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean u){problem=p;applyExample();refreshTraining();}});
    stepSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean u){solStep=p;refreshTraining();}});
    applyExample();refreshTraining();
  }
  void applyExample(){try{F.setText(String.valueOf(exF[problem]));M.setText(String.valueOf(exM[problem]));stations.setText(String.valueOf(exStations[problem]));actCount.setText(String.valueOf(exAct[problem]));layers.setText(String.valueOf(exLayers[problem]));}catch(Exception e){}}
  String explain(Calc c){
    double sum=0,peak=0;for(double v:c.fi){sum+=v;peak=Math.max(peak,v);}int beams=Math.max(1,c.n-exAct[problem]);
    switch(solStep){
      case 0:return String.format(Locale.US,"Define target loads: Fz = %.1f kN, My = %.1f kN·m. Use tank length and station x-locations as the load-introduction basis.",exF[problem],exM[problem]);
      case 1:return String.format(Locale.US,"Distribute load to %d stations so ΣFi ≈ %.1f kN and Σ(Fi·xi) reproduces the target moment. Peak station load ≈ %.1f kN.",c.n,sum,peak);
      case 2:return String.format(Locale.US,"Candidate architecture: %d actuators and %d whiffletree layer(s). About %d station(s) are grouped per actuator.",exAct[problem],exLayers[problem],(int)Math.ceil(c.n/(double)exAct[problem]));
      case 3:return String.format(Locale.US,"Build the beam tree from station groups. Approximate beam count = %d. Each pivot must satisfy Fleft·Lleft = Fright·Lright before strength sizing.",beams);
      case 4:return String.format(Locale.US,"Per-actuator nominal load ≈ %.1f kN before local imbalance. Select actuator and load cell with design factor %.2f and adequate stroke/resolution margin.",sum/Math.max(1,exAct[problem]),c.sf);
      case 5:return "Check pad pressure, tie-rod axial stress/buckling, spherical-bearing articulation, pin double shear, clevis/lug bearing, stroke reserve and collision clearance.";
      default:return String.format(Locale.US,"Concept BOM: %d actuator(s), %d load cell(s), %d station pad(s), whiffletree beams/pivots, tie-rods, clevises, pins, hoses, manifold/servo valves and synchronized DAQ channels. Final result must pass independent structural/safety review.",exAct[problem],exAct[problem],c.n);
    }
  }
  void refreshTraining(){try{Calc c=compute(false);problemInfo.setText(String.format(Locale.US,"PROBLEM %d/20 • %s\nFz %.1f kN • My %.1f kN·m • %d stations • %d actuators • %d layers",problem+1,titles[problem],exF[problem],exM[problem],exStations[problem],exAct[problem],exLayers[problem]));solutionInfo.setText(solNames[solStep]+"\n"+explain(c));if(trainView!=null)trainView.invalidate();}catch(Exception e){}}

  class TrainingView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);TrainingView(){super(V46Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception e){return;}int W=getWidth();float left=dp(28),right=W-dp(28),tankY=dp(170);t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("EFT TRAINING PROBLEM "+(problem+1),dp(14),dp(28),t);t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText(titles[problem],dp(14),dp(48),t);
      p.setColor(Color.rgb(67,78,90));cn.drawRoundRect(new RectF(left,tankY-dp(30),right,tankY+dp(30)),dp(30),dp(30),p);double max=1;for(double v:c.fi)max=Math.max(max,v);float[] sx=new float[c.n];for(int i=0;i<c.n;i++){sx[i]=left+(right-left)*(i+.5f)/c.n;float h=(float)(dp(92)*c.fi[i]/max);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(247,207,77));cn.drawLine(sx[i],tankY-dp(35),sx[i],tankY-dp(35)-h,p);p.setStyle(Paint.Style.FILL);cn.drawCircle(sx[i],tankY,dp(5),p);t.setColor(Color.WHITE);t.setTextSize(dp(6));cn.drawText("S"+(i+1),sx[i]-dp(6),tankY+dp(48),t);}
      float groupY=dp(360);for(int a=0;a<exAct[problem];a++){int s0=(int)Math.floor(a*c.n/(double)exAct[problem]),s1=(int)Math.floor((a+1)*c.n/(double)exAct[problem])-1;if(a==exAct[problem]-1)s1=c.n-1;s0=Math.max(0,Math.min(c.n-1,s0));s1=Math.max(s0,Math.min(c.n-1,s1));float gx=(sx[s0]+sx[s1])/2;for(int i=s0;i<=s1;i++){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.rgb(205,215,225));cn.drawLine(sx[i],tankY+dp(30),gx,groupY-dp(12),p);}p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawRoundRect(new RectF(sx[s0]-dp(10),groupY-dp(7),sx[s1]+dp(10),groupY+dp(7)),dp(4),dp(4),p);float py=groupY;for(int l=0;l<exLayers[problem];l++){py+=dp(43);p.setColor(l==exLayers[problem]-1?Color.rgb(51,205,220):Color.rgb(110,145,170));cn.drawCircle(gx,py,dp(7),p);t.setTextSize(dp(6));t.setColor(Color.WHITE);cn.drawText("L"+(l+1),gx+dp(10),py+dp(2),t);}p.setColor(Color.rgb(55,132,238));cn.drawRoundRect(new RectF(gx-dp(18),py+dp(28),gx+dp(18),py+dp(82)),dp(7),dp(7),p);t.setTextSize(dp(7));t.setColor(Color.WHITE);cn.drawText("A"+(a+1),gx-dp(7),py+dp(58),t);}
      float y=dp(675);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),y,W-dp(16),y+dp(170)),dp(10),dp(10),p);t.setTextSize(dp(9));t.setColor(Color.rgb(247,207,77));cn.drawText(solNames[solStep],dp(28),y+dp(28),t);t.setColor(Color.WHITE);t.setTextSize(dp(7));String s=explain(c);int maxChars=82;int line=0;for(int i=0;i<s.length();i+=maxChars){String part=s.substring(i,Math.min(s.length(),i+maxChars));cn.drawText(part,dp(28),y+dp(58)+line*dp(22),t);line++;if(line>4)break;}t.setColor(Color.rgb(185,210,230));cn.drawText("Use the solution-step slider to move from physics to hardware/BOM.",dp(28),y+dp(150),t);}
  }
}
