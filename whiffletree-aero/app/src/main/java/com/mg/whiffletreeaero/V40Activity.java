package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V40Activity extends V39Activity {
  TextView autoInfo; AutoDesignView autoView;
  EditText maxDesignerAct,maxDesignerLayers,targetActUtil;
  Button applyAuto;
  DesignChoice best;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    maxDesignerAct=field("Designer max actuator count","8");
    maxDesignerLayers=field("Designer max whiffletree layers","4");
    targetActUtil=field("Preferred actuator utilization [%]","70");

    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("AUTOMATIC WHIFFLETREE DESIGNER",16,true,Color.WHITE));
    autoInfo=card("Topology optimizer hazırlanıyor...",Color.rgb(20,48,68));panel.addView(autoInfo,lp());
    applyAuto=new Button(this);applyAuto.setText("APPLY RECOMMENDED TOPOLOGY");panel.addView(applyAuto,new LinearLayout.LayoutParams(-1,dp(48)));
    autoView=new AutoDesignView();panel.addView(autoView,new LinearLayout.LayoutParams(-1,dp(760)));
    root.addView(panel,20,lp());

    applyAuto.setOnClickListener(v->{
      try{Calc c=compute(false);best=choose(c);if(best!=null){actCount.setText(String.valueOf(best.actuators));layers.setText(String.valueOf(best.layers));refreshAuto();}}catch(Exception e){}
    });
    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){refreshAuto();}public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{maxDesignerAct,maxDesignerLayers,targetActUtil,F,M,L,D,stations,actCapacity,sf})e.addTextChangedListener(w);
    refreshAuto();
  }

  static class DesignChoice{
    int actuators,layers,maxStationsPerAct;
    double peakLoad,avgLoad,peakUtil,imbalance,complexity,score;
    String status;
  }

  DesignChoice eval(Calc c,int na,int nl){
    DesignChoice q=new DesignChoice();q.actuators=na;q.layers=nl;
    double cap=Math.max(1,d(actCapacity)),sum=0,peak=0,min=Double.POSITIVE_INFINITY;
    int maxCount=0;
    for(int a=0;a<na;a++){
      int s0=(int)Math.floor(a*c.n/(double)na),s1=(int)Math.floor((a+1)*c.n/(double)na)-1;if(a==na-1)s1=c.n-1;
      double gl=0;int cnt=0;for(int i=Math.max(0,s0);i<=Math.min(c.n-1,s1);i++){gl+=c.fi[i]*c.sf;cnt++;}
      sum+=gl;peak=Math.max(peak,gl);min=Math.min(min,gl);maxCount=Math.max(maxCount,cnt);
    }
    q.maxStationsPerAct=maxCount;q.peakLoad=peak;q.avgLoad=sum/Math.max(1,na);q.peakUtil=100*peak/cap;
    q.imbalance=q.avgLoad>0?100*(peak-Math.max(0,min))/q.avgLoad:0;
    int requiredLayers=1;int branches=2;while(branches<maxCount){requiredLayers++;branches*=2;}
    double layerPenalty=nl<requiredLayers?120*(requiredLayers-nl):5*Math.abs(nl-requiredLayers);
    double target=Math.max(20,Math.min(90,d(targetActUtil)));
    double utilPenalty=Math.abs(q.peakUtil-target)*1.4+(q.peakUtil>100?(q.peakUtil-100)*8:0);
    q.complexity=na*8+nl*11+Math.max(0,na-4)*3;
    q.score=1000-utilPenalty-layerPenalty-q.imbalance*.6-q.complexity;
    q.status=q.peakUtil>100||nl<requiredLayers?"FAIL":(q.peakUtil>85||q.peakUtil<25||q.imbalance>35?"WARN":"PASS");
    return q;
  }

  DesignChoice choose(Calc c){
    int maxA=Math.max(1,Math.min(c.n,(int)Math.round(d(maxDesignerAct))));int maxL=Math.max(1,Math.min(6,(int)Math.round(d(maxDesignerLayers))));
    DesignChoice b=null;for(int a=1;a<=maxA;a++)for(int l=1;l<=maxL;l++){DesignChoice q=eval(c,a,l);if(b==null||q.score>b.score)b=q;}return b;
  }

  void refreshAuto(){
    try{Calc c=compute(false);best=choose(c);if(best==null)return;
      autoInfo.setText(String.format(Locale.US,"RECOMMENDED: %d actuators • %d layers • %s\nPeak actuator demand %.2f kN | utilization %.1f%%\nAverage actuator load %.2f kN | load imbalance %.1f%%\nMax stations per actuator %d | topology score %.0f\nDesigner searches contiguous station groups and penalizes overload, too few layers, poor utilization, imbalance and unnecessary complexity.\nUse APPLY RECOMMENDED TOPOLOGY to write the selected actuator/layer counts into the main model.",best.actuators,best.layers,best.status,best.peakLoad,best.peakUtil,best.avgLoad,best.imbalance,best.maxStationsPerAct,best.score));
      if(autoView!=null)autoView.invalidate();
    }catch(Exception e){}
  }

  class AutoDesignView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    AutoDesignView(){super(V40Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);}catch(Exception e){return;}DesignChoice q=choose(c);if(q==null||c.n<=0)return;int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("AUTO-GENERATED EFT LOAD DISTRIBUTION",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Station → group → whiffletree layers → actuator topology",dp(12),dp(43),t);

      float left=dp(25),right=W-dp(25),tankY=dp(115);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(18));p.setColor(Color.rgb(68,80,92));cn.drawLine(left,tankY,right,tankY,p);
      p.setStrokeWidth(dp(3));p.setColor(Color.rgb(150,165,180));cn.drawLine(left,tankY,right,tankY,p);
      float[] sx=new float[c.n];for(int i=0;i<c.n;i++){sx[i]=left+(right-left)*(i+.5f)/c.n;p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawCircle(sx[i],tankY,dp(5),p);t.setTextSize(dp(6));t.setColor(Color.WHITE);cn.drawText("S"+(i+1),sx[i]-dp(5),tankY-dp(13),t);}

      float groupY=dp(245);for(int a=0;a<q.actuators;a++){
        int s0=(int)Math.floor(a*c.n/(double)q.actuators),s1=(int)Math.floor((a+1)*c.n/(double)q.actuators)-1;if(a==q.actuators-1)s1=c.n-1;
        s0=Math.max(0,Math.min(c.n-1,s0));s1=Math.max(s0,Math.min(c.n-1,s1));float gx=(sx[s0]+sx[s1])/2;
        for(int i=s0;i<=s1;i++){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.rgb(205,215,225));cn.drawLine(sx[i],tankY+dp(8),gx,groupY-dp(10),p);}
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(247,207,77));cn.drawRoundRect(new RectF(sx[s0]-dp(12),groupY-dp(7),sx[s1]+dp(12),groupY+dp(7)),dp(4),dp(4),p);
        float py=groupY;for(int l=1;l<=q.layers;l++){py+=dp(48);p.setColor(l==q.layers?Color.rgb(51,205,220):Color.rgb(110,145,170));cn.drawCircle(gx,py,dp(7),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(205,215,225));cn.drawLine(gx,py-dp(40),gx,py-dp(7),p);p.setStyle(Paint.Style.FILL);t.setTextSize(dp(6));t.setColor(Color.WHITE);cn.drawText("L"+l,gx+dp(10),py+dp(3),t);}
        float actY=groupY+q.layers*dp(48)+dp(35);p.setColor(Color.rgb(55,132,238));cn.drawRoundRect(new RectF(gx-dp(20),actY,gx+dp(20),actY+dp(62)),dp(7),dp(7),p);t.setTextSize(dp(7));t.setColor(Color.WHITE);cn.drawText("A"+(a+1),gx-dp(8),actY+dp(27),t);
      }

      float y=dp(610);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(16),y,W-dp(16),y+dp(110)),dp(8),dp(8),p);t.setTextSize(dp(8));t.setColor(Color.WHITE);
      cn.drawText(String.format(Locale.US,"Recommended topology: %d actuators / %d layers",q.actuators,q.layers),dp(28),y+dp(25),t);
      cn.drawText(String.format(Locale.US,"Peak %.1f kN | utilization %.1f%% | imbalance %.1f%%",q.peakLoad,q.peakUtil,q.imbalance),dp(28),y+dp(50),t);
      cn.drawText(String.format(Locale.US,"Maximum %d station(s) per actuator group",q.maxStationsPerAct),dp(28),y+dp(75),t);
      int col=q.status.equals("PASS")?Color.rgb(67,190,113):(q.status.equals("WARN")?Color.rgb(247,207,77):Color.rgb(229,82,74));t.setColor(col);cn.drawText("AUTO DESIGN: "+q.status,dp(28),y+dp(100),t);
    }
  }
}
