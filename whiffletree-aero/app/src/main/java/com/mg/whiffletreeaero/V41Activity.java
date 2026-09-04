package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V41Activity extends V40Activity {
  TextView tradeInfo; TradeStudyView tradeView;
  EditText wPeak,wBalance,wComplexity,wStroke,wDaq;
  ArrayList<DesignChoice> ranked=new ArrayList<>();

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    wPeak=field("Trade weight: peak load","1.0");
    wBalance=field("Trade weight: load balance","0.8");
    wComplexity=field("Trade weight: complexity","0.6");
    wStroke=field("Trade weight: stroke/joint risk","0.8");
    wDaq=field("Trade weight: hydraulic/DAQ burden","0.5");

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    panel.setBackground(bg(Color.rgb(12,31,47),12));
    panel.addView(tx("TOPOLOGY OPTIMIZATION / TRADE STUDY",16,true,Color.WHITE));
    tradeInfo=card("Alternative trade study hazırlanıyor...",Color.rgb(20,48,68));
    panel.addView(tradeInfo,lp());
    tradeView=new TradeStudyView();
    panel.addView(tradeView,new LinearLayout.LayoutParams(-1,dp(830)));
    root.addView(panel,21,lp());

    TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){refreshTrade();} public void afterTextChanged(Editable e){}};
    for(EditText e:new EditText[]{wPeak,wBalance,wComplexity,wStroke,wDaq,maxDesignerAct,maxDesignerLayers,targetActUtil,F,M,L,D,stations,actCapacity,sf,deflection,linkLength})e.addTextChangedListener(w);
    refreshTrade();
  }

  double wd(EditText e,double def){try{return Math.max(0,Double.parseDouble(e.getText().toString().trim()));}catch(Exception x){return def;}}

  double tradeScore(Calc c,DesignChoice q){
    double peakPenalty=Math.max(0,q.peakUtil-70)+Math.max(0,25-q.peakUtil)*0.5;
    double balancePenalty=q.imbalance;
    double complexityPenalty=q.complexity;
    double strokeRisk=Math.max(0,q.maxStationsPerAct-3)*12 + Math.max(0,q.layers-3)*5;
    double daqBurden=q.actuators*4 + q.layers*2;
    double p=wd(wPeak,1.0)*peakPenalty + wd(wBalance,.8)*balancePenalty + wd(wComplexity,.6)*complexityPenalty + wd(wStroke,.8)*strokeRisk + wd(wDaq,.5)*daqBurden;
    if(q.status.equals("FAIL"))p+=400;
    else if(q.status.equals("WARN"))p+=80;
    return 1000-p;
  }

  void rank(Calc c){
    ranked.clear();
    int maxA=Math.max(1,Math.min(c.n,(int)Math.round(d(maxDesignerAct))));
    int maxL=Math.max(1,Math.min(6,(int)Math.round(d(maxDesignerLayers))));
    for(int a=1;a<=maxA;a++)for(int l=1;l<=maxL;l++){
      DesignChoice q=eval(c,a,l);
      q.score=tradeScore(c,q);
      ranked.add(q);
    }
    Collections.sort(ranked,new Comparator<DesignChoice>(){public int compare(DesignChoice a,DesignChoice b){return Double.compare(b.score,a.score);}});
  }

  void refreshTrade(){
    try{
      Calc c=compute(false);rank(c);if(ranked.size()==0)return;
      StringBuilder sb=new StringBuilder();
      int n=Math.min(3,ranked.size());
      sb.append("TOP ").append(n).append(" ALTERNATIVES\n");
      for(int i=0;i<n;i++){
        DesignChoice q=ranked.get(i);
        sb.append(String.format(Locale.US,"#%d  A%d / L%d • %s • score %.0f • peak %.1f%% • imbalance %.1f%% • complexity %.0f\n",i+1,q.actuators,q.layers,q.status,q.score,q.peakUtil,q.imbalance,q.complexity));
      }
      DesignChoice b=ranked.get(0);
      sb.append(String.format(Locale.US,"\nWHY #1? Lower weighted combination of overload risk, imbalance, complexity, stroke/joint exposure and hydraulic/DAQ burden.\nRecommended: %d actuator / %d layer. Compare the three 2D cards below before applying.",b.actuators,b.layers));
      tradeInfo.setText(sb.toString());
      if(tradeView!=null)tradeView.invalidate();
    }catch(Exception e){}
  }

  class TradeStudyView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    TradeStudyView(){super(V41Activity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    @Override protected void onDraw(Canvas cn){
      super.onDraw(cn);Calc c;try{c=compute(false);rank(c);}catch(Exception e){return;}if(ranked.size()==0)return;int W=getWidth();
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("TOP-3 WHIFFLETREE TOPOLOGY TRADE STUDY",dp(12),dp(25),t);
      t.setTextSize(dp(8));t.setColor(Color.rgb(185,210,230));cn.drawText("Same EFT load case, different actuator/layer architectures",dp(12),dp(43),t);
      int n=Math.min(3,ranked.size());float top=dp(72),cardH=dp(215);
      for(int k=0;k<n;k++){
        DesignChoice q=ranked.get(k);float y=top+k*(cardH+dp(18));
        int border=k==0?Color.rgb(67,190,113):(q.status.equals("FAIL")?Color.rgb(229,82,74):Color.rgb(247,207,77));
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(16,43,62));cn.drawRoundRect(new RectF(dp(14),y,W-dp(14),y+cardH),dp(10),dp(10),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(border);cn.drawRoundRect(new RectF(dp(14),y,W-dp(14),y+cardH),dp(10),dp(10),p);
        p.setStyle(Paint.Style.FILL);
        t.setColor(Color.WHITE);t.setTextSize(dp(10));cn.drawText("#"+(k+1)+"  "+q.actuators+" ACTUATORS / "+q.layers+" LAYERS",dp(28),y+dp(25),t);
        t.setTextSize(dp(7));t.setColor(Color.rgb(190,215,230));cn.drawText(String.format(Locale.US,"score %.0f | peak util %.1f%% | imbalance %.1f%% | complexity %.0f",q.score,q.peakUtil,q.imbalance,q.complexity),dp(28),y+dp(47),t);

        float left=dp(32),right=W-dp(32),tankY=y+dp(85);p.setColor(Color.rgb(74,84,96));cn.drawRoundRect(new RectF(left,tankY-dp(7),right,tankY+dp(7)),dp(7),dp(7),p);
        for(int a=0;a<q.actuators;a++){
          float gx=left+(right-left)*(a+.5f)/q.actuators;
          p.setColor(Color.rgb(247,207,77));cn.drawCircle(gx,tankY,dp(5),p);
          float py=tankY+dp(30);
          for(int l=1;l<=q.layers;l++){
            p.setColor(l==q.layers?Color.rgb(51,205,220):Color.rgb(110,145,170));cn.drawCircle(gx,py,dp(5),p);
            if(l<q.layers){p.setStrokeWidth(dp(2));p.setColor(Color.rgb(205,215,225));cn.drawLine(gx,py+dp(5),gx,py+dp(24),p);}
            py+=dp(28);
          }
          p.setColor(Color.rgb(55,132,238));cn.drawRoundRect(new RectF(gx-dp(12),py-dp(3),gx+dp(12),py+dp(28)),dp(5),dp(5),p);
        }
        t.setTextSize(dp(7));t.setColor(border);cn.drawText(k==0?"BEST WEIGHTED TRADE":"ALTERNATIVE",dp(28),y+cardH-dp(18),t);
      }
      float fy=top+n*(cardH+dp(18));t.setColor(Color.rgb(185,210,230));t.setTextSize(dp(7));
      cn.drawText("Trade score is a concept-screening aid; final architecture must still pass structural, hydraulic, control, safety and test-specific reviews.",dp(16),fy+dp(10),t);
    }
  }
}
