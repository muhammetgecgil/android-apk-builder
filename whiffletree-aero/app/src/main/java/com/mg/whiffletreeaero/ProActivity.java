package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ProActivity extends MainActivity {
  ProTopologyView topology;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    topology=new ProTopologyView();
    LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(650));
    pp.setMargins(0,dp(5),0,dp(5));
    root.addView(topology,4,pp);
    TextWatcher refresh=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int a,int b,int c){}
      public void onTextChanged(CharSequence s,int a,int b,int c){if(topology!=null)topology.invalidate();}
      public void afterTextChanged(Editable e){}
    };
    for(EditText e:new EditText[]{F,M,L,D,stations,layers,actCount,actCapacity,pressure,sf,padW,padL,deflection,linkLength,beamSpan,beamH,beamB,pinAllow,bearingAllow})e.addTextChangedListener(refresh);
  }

  class Node { float x,y; double f; Node(float X,float Y,double F){x=X;y=Y;f=F;} }

  class ProTopologyView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG), t=new Paint(Paint.ANTI_ALIAS_FLAG);
    int[] gc={Color.rgb(55,132,238),Color.rgb(51,205,220),Color.rgb(245,164,47),Color.rgb(160,110,230),Color.rgb(67,190,113),Color.rgb(229,82,74),Color.rgb(247,207,77),Color.rgb(120,180,220)};
    ProTopologyView(){super(ProActivity.this);setBackgroundColor(Color.rgb(5,17,29));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}

    protected void onDraw(Canvas cn){
      super.onDraw(cn);
      Calc c; try{c=compute(false);}catch(Exception ex){return;}
      int W=getWidth(), H=getHeight();
      title(cn,c,W);
      float l=dp(34), r=W-dp(18), tankY=dp(112);
      drawTank(cn,l,r,tankY);
      drawStationGroups(cn,c,l,r,tankY);
      drawLayerGuides(cn,c,W,tankY);
      drawForest(cn,c,l,r,H);
      drawLegend(cn,c,H);
    }

    void title(Canvas cn,Calc c,int W){
      t.setColor(Color.WHITE);t.setTextSize(dp(14));cn.drawText("PROFESSIONAL 2D LOAD-PATH MAP",dp(12),dp(24),t);
      t.setTextSize(dp(9));t.setColor(Color.rgb(185,210,230));cn.drawText("Station grouping → whiffletree layers → actuator channels",dp(12),dp(43),t);
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(12,54,78));RectF b=new RectF(W-dp(184),dp(8),W-dp(8),dp(78));cn.drawRoundRect(b,dp(8),dp(8),p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(51,205,220));cn.drawRoundRect(b,dp(8),dp(8),p);
      t.setColor(Color.rgb(51,205,220));t.setTextSize(dp(8));cn.drawText("WHIFFLETREE CONFIG",W-dp(174),dp(27),t);
      t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText(c.layers+" LAYERS",W-dp(174),dp(49),t);cn.drawText(c.nAct+" ACTUATORS",W-dp(174),dp(69),t);
    }

    void drawTank(Canvas cn,float l,float r,float y){
      p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(50,63,76));cn.drawRoundRect(new RectF(l,y-dp(20),r,y+dp(20)),dp(20),dp(20),p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(160,180,198));cn.drawRoundRect(new RectF(l,y-dp(20),r,y+dp(20)),dp(20),dp(20),p);
    }

    void drawStationGroups(Canvas cn,Calc c,float l,float r,float y){
      for(int a=0;a<c.nAct;a++){
        int s=(int)Math.floor(a*c.n/(double)c.nAct), e=(int)Math.floor((a+1)*c.n/(double)c.nAct);
        if(e<=s)e=Math.min(c.n,s+1);
        float x1=l+(float)(c.x[s]/c.L)*(r-l)-dp(12), x2=l+(float)(c.x[e-1]/c.L)*(r-l)+dp(12);
        p.setStyle(Paint.Style.FILL);p.setColor(withAlpha(gc[a%gc.length],42));cn.drawRoundRect(new RectF(x1,y-dp(34),x2,y+dp(34)),dp(8),dp(8),p);
        t.setColor(gc[a%gc.length]);t.setTextSize(dp(8));cn.drawText("A"+(a+1)+" GROUP",x1+dp(4),y-dp(42),t);
      }
      for(int i=0;i<c.n;i++){
        float x=l+(float)(c.x[i]/c.L)*(r-l);
        int a=Math.min(c.nAct-1,(int)Math.floor(i*c.nAct/(double)c.n));
        p.setColor(gc[a%gc.length]);p.setStrokeWidth(2);cn.drawLine(x,y-dp(20),x,y+dp(22),p);
        p.setStyle(Paint.Style.FILL);cn.drawCircle(x,y,dp(4),p);
        t.setColor(Color.WHITE);t.setTextSize(dp(7));cn.drawText("S"+(i+1),x-dp(6),y+dp(33),t);
        t.setColor(Color.rgb(200,220,235));cn.drawText(String.format(Locale.US,"%.1f",c.fi[i]),x-dp(8),y-dp(27),t);
      }
    }

    void drawLayerGuides(Canvas cn,Calc c,int W,float tankY){
      float base=tankY+dp(78);
      for(int lev=1;lev<=c.layers;lev++){
        float yy=base+dp(68)*(lev-1);
        p.setStyle(Paint.Style.FILL);p.setColor(lev==1?Color.rgb(245,164,47):(lev==2?Color.rgb(247,207,77):Color.rgb(51,205,220)));
        cn.drawRoundRect(new RectF(dp(4),yy-dp(13),dp(72),yy+dp(13)),dp(5),dp(5),p);
        t.setColor(Color.rgb(7,20,34));t.setTextSize(dp(8));cn.drawText("LAYER "+lev,dp(10),yy+dp(4),t);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(Color.rgb(44,69,88));cn.drawLine(dp(78),yy,W-dp(8),yy,p);
      }
    }

    void drawForest(Canvas cn,Calc c,float l,float r,int H){
      float base=dp(190);
      int capacity=(int)Math.pow(2,c.layers);
      double cap=Math.max(1,d(actCapacity));
      for(int a=0;a<c.nAct;a++){
        int s=(int)Math.floor(a*c.n/(double)c.nAct), e=(int)Math.floor((a+1)*c.n/(double)c.nAct); if(e<=s)e=Math.min(c.n,s+1);
        ArrayList<Node> nodes=new ArrayList<>();
        for(int i=s;i<e;i++){float x=l+(float)(c.x[i]/c.L)*(r-l);nodes.add(new Node(x,dp(145),c.fi[i]*c.sf));}
        int beamNo=1;
        for(int lev=1;lev<=c.layers && nodes.size()>1;lev++){
          float yy=base+dp(68)*(lev-1);ArrayList<Node> next=new ArrayList<>();
          for(int i=0;i<nodes.size();i+=2){
            if(i+1<nodes.size()){
              Node A=nodes.get(i),B=nodes.get(i+1);double sum=A.f+B.f;float px=(float)((A.x*B.f+B.x*A.f)/Math.max(.0001,sum));
              p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(gc[a%gc.length]);cn.drawLine(A.x,yy,B.x,yy,p);
              p.setStrokeWidth(1.5f);cn.drawLine(A.x,A.y,A.x,yy,p);cn.drawLine(B.x,B.y,B.x,yy,p);
              p.setStyle(Paint.Style.FILL);cn.drawCircle(px,yy,dp(4),p);
              double leftPct=100*B.f/sum,rightPct=100*A.f/sum;
              t.setColor(Color.WHITE);t.setTextSize(dp(6));cn.drawText(String.format(Locale.US,"B%d %.0fkN  %.0f/%.0f%%",beamNo++,sum,leftPct,rightPct),Math.min(A.x,B.x),yy-dp(7),t);
              next.add(new Node(px,yy,sum));
            }else next.add(nodes.get(i));
          }
          nodes=next;
        }
        float actY=base+dp(68)*c.layers+dp(72);
        double groupF=0;for(int i=s;i<e;i++)groupF+=c.fi[i]*c.sf;
        float ax=0;for(Node n:nodes)ax+=n.x;ax/=Math.max(1,nodes.size());
        boolean covered=(e-s)<=capacity; boolean overload=groupF>cap;
        for(Node n:nodes){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(covered?2:4);p.setColor(covered?gc[a%gc.length]:Color.rgb(229,82,74));cn.drawLine(n.x,n.y,ax,actY-dp(47),p);}
        p.setStyle(Paint.Style.FILL);p.setColor(overload?Color.rgb(150,46,45):Color.rgb(46,82,70));RectF ar=new RectF(ax-dp(28),actY-dp(47),ax+dp(28),actY);cn.drawRoundRect(ar,dp(6),dp(6),p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(gc[a%gc.length]);cn.drawRoundRect(ar,dp(6),dp(6),p);
        t.setColor(Color.WHITE);t.setTextSize(dp(8));cn.drawText("ACT "+(a+1),ax-dp(17),actY-dp(29),t);t.setTextSize(dp(7));cn.drawText(String.format(Locale.US,"%.0f kN",groupF),ax-dp(15),actY-dp(14),t);
        float util=(float)(100*groupF/cap);t.setColor(util>100?Color.rgb(255,130,120):Color.rgb(130,230,175));cn.drawText(String.format(Locale.US,"%.0f%% cap",util),ax-dp(17),actY+dp(13),t);
        t.setColor(gc[a%gc.length]);cn.drawText("S"+(s+1)+"–S"+e,ax-dp(16),actY+dp(28),t);
        if(!covered){t.setColor(Color.rgb(255,120,110));t.setTextSize(dp(7));cn.drawText("NEED MORE LAYERS/ACTUATORS",ax-dp(52),actY+dp(43),t);}
      }
    }

    void drawLegend(Canvas cn,Calc c,int H){
      float y=H-dp(46);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(13,35,51));cn.drawRoundRect(new RectF(dp(8),y,getWidth()-dp(8),H-dp(8)),dp(7),dp(7),p);
      t.setTextSize(dp(7));t.setColor(Color.rgb(200,220,235));cn.drawText("Beam label: design kN + pivot L/R %   •   Actuator: grouped design load + capacity utilization   •   Red link: topology coverage warning",dp(15),y+dp(23),t);
    }

    int withAlpha(int color,int alpha){return Color.argb(alpha,Color.red(color),Color.green(color),Color.blue(color));}
  }
}
