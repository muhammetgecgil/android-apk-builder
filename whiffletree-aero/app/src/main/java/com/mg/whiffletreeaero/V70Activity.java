package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V70Activity extends V69Activity {
  LinearLayout visualGuide;
  TextView visualGuideSummary;
  RigPoster70 rigPoster70;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    visualGuide=new LinearLayout(this);visualGuide.setOrientation(LinearLayout.VERTICAL);visualGuide.setPadding(dp(10),dp(10),dp(10),dp(10));visualGuide.setBackground(bg(Color.rgb(2,14,24),16));
    visualGuide.addView(tx("TEST RIG GÖRSEL REHBERİ — NE NEREDE?",20,true,Color.WHITE));
    visualGuide.addView(tx("Poster mantığı: EFT en üstte; yük uygulama bölgeleri tank üzerinde; load pad ve bağlantılar altında; turuncu whiffletree kademeleri; load-cell; actuator; strongback en altta.",9,false,Color.rgb(180,210,230)));
    visualGuideSummary=card("HESAPLA VE GÖSTER sonrası her elemanın adı ve taşıdığı kuvvet doğrudan poster üzerinde görünür.",Color.rgb(14,45,64));visualGuide.addView(visualGuideSummary,lp());
    rigPoster70=new RigPoster70();visualGuide.addView(rigPoster70,new LinearLayout.LayoutParams(-1,dp(1380)));
    root.addView(visualGuide,1,lp());
  }

  @Override void runGuided(){super.runGuided();refreshVisualGuide();}
  @Override void runPrimary(){super.runPrimary();refreshVisualGuide();}
  @Override void calculateAndShow(){super.calculateAndShow();refreshVisualGuide();}

  void refreshVisualGuide(){
    if(rigPoster70==null)return;
    if(!solvedValid){visualGuideSummary.setText("Hesap hazır değil. ADIM 1→7 sırasını tamamla ve HESAPLA VE 2D / 3D GÖSTER'e bas.");rigPoster70.invalidate();return;}
    visualGuideSummary.setText(String.format(Locale.US,"GÖRSEL HAZIR — %d yük bölgesi • %d layer • %d actuator\nMavi oklar: yük uygulama bölgeleri | Yeşil: load pad/load-cell | Turuncu: whiffletree beam | Koyu gövde: actuator | Gri taban: strongback",solved.size(),Math.max(1,qi(qLayers,1,4)),Math.max(1,qi(qActs,1,12))));
    rigPoster70.invalidate();
  }

  class RigPoster70 extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    RigPoster70(){super(V70Activity.this);setBackgroundColor(Color.rgb(1,9,16));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float px(float v){return v*getResources().getDisplayMetrics().density;}
    void text(Canvas c,String s,float x,float y,int col,float size){t.setColor(col);t.setTextSize(px(size));c.drawText(s,x,y,t);}
    void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth(px(w));c.drawLine(x1,y1,x2,y2,p);}
    void arrow(Canvas c,float x,float y,float dy,int col){line(c,x,y,x,y+dy,col,2.8f);float yy=y+dy;line(c,x,yy,x-px(5),yy-(dy>0?px(8):-px(8)),col,2.2f);line(c,x,yy,x+px(5),yy-(dy>0?px(8):-px(8)),col,2.2f);}

    @Override protected void onDraw(Canvas c){
      super.onDraw(c);int W=getWidth();float left=px(28),right=W-px(28);
      text(c,"EFT STRUCTURAL TEST RIG — POSTER VIEW",px(14),px(28),Color.WHITE,14);
      text(c,"YÜK UYGULAMA → LOAD PAD → WHIFFLETREE → LOAD CELL → ACTUATOR → STRONGBACK",px(14),px(50),Color.rgb(190,215,230),6.5f);
      if(!solvedValid){text(c,"Önce ADIM 1–7 girişlerini tamamla.",px(14),px(84),Color.rgb(180,200,215),9);return;}
      int na=Math.max(1,qi(qActs,1,12)),nl=Math.max(1,qi(qLayers,1,4));double len=Math.max(1,qd(qLength));

      float tankY=px(165);p.setColor(Color.rgb(95,104,112));c.drawRoundRect(new RectF(left,tankY-px(45),right,tankY+px(45)),px(45),px(45),p);
      text(c,"EFT TANK",left+px(10),tankY+px(4),Color.WHITE,8);
      text(c,"YÜK UYGULAMA BÖLGELERİ / STATIONS",left,tankY-px(88),Color.rgb(100,170,255),7);
      double maxR=1;for(SNode s:solved)maxR=Math.max(maxR,s.r);
      ArrayList<float[]> cur=new ArrayList<>();
      for(SNode s:solved){
        float x=(float)(left+(s.x+len/2.0)/len*(right-left));
        line(c,x,tankY-px(44),x,tankY+px(44),Color.rgb(130,138,145),1);
        float al=(float)(px(42)*(s.r/maxR));double sign=(Math.abs(s.fz)>1e-9?s.fz:(Math.abs(s.fy)>1e-9?s.fy:s.fx));
        arrow(c,x,tankY-px(52),(float)(-Math.signum(sign)*al),Color.rgb(70,145,245));
        text(c,"S"+(s.section+1),x-px(7),tankY-px(104),Color.WHITE,5.2f);
        text(c,String.format(Locale.US,"%+.0f N",s.r*Math.signum(sign)),x-px(17),tankY-px(88),Color.rgb(180,215,255),4.9f);
        p.setColor(Color.rgb(90,200,110));c.drawRect(x-px(8),tankY+px(48),x+px(8),tankY+px(61),p);
        text(c,"PAD",x-px(9),tankY+px(78),Color.rgb(150,240,165),4.8f);
        cur.add(new float[]{x,tankY+px(62),(float)s.fx,(float)s.fy,(float)s.fz});
      }

      float y=px(340);
      for(int l=1;l<=nl;l++){
        int target=(l==nl)?na:Math.max(na,(int)Math.ceil(cur.size()/2.0));ArrayList<float[]> next=new ArrayList<>();
        text(c,l+". KADEME — WHIFFLETREE BEAM",left,y-px(34),Color.rgb(247,178,55),7);
        for(int g=0;g<target;g++){
          int i0=(int)Math.floor((double)g*cur.size()/target);int i1=Math.max(i0,Math.min(cur.size()-1,(int)Math.floor((double)(g+1)*cur.size()/target)-1));
          float x=0,fx=0,fy=0,fz=0;int cnt=0;for(int i=i0;i<=i1;i++){float[] q=cur.get(i);x+=q[0];fx+=q[2];fy+=q[3];fz+=q[4];cnt++;}x/=Math.max(1,cnt);
          for(int i=i0;i<=i1;i++){float[] q=cur.get(i);line(c,q[0],q[1],x,y-px(10),Color.rgb(165,175,182),1.3f);}
          float half=px(34);p.setColor(Color.rgb(220,145,32));c.drawRoundRect(new RectF(x-half,y-px(9),x+half,y+px(9)),px(3),px(3),p);
          p.setColor(Color.rgb(210,215,220));c.drawCircle(x,y,px(4),p);text(c,"PIVOT",x-px(12),y-px(15),Color.rgb(220,225,230),4.6f);
          double rr=Math.sqrt(fx*fx+fy*fy+fz*fz);text(c,String.format(Locale.US,"R %.0f N",rr),x-px(18),y+px(28),Color.WHITE,5);
          next.add(new float[]{x,y+px(14),fx,fy,fz});
        }
        cur=next;y+=px(145);
      }

      float lcY=y+px(20),actY=lcY+px(145);
      text(c,"LOAD CELL SEVİYESİ",left,lcY-px(38),Color.rgb(120,230,135),7);
      text(c,"ACTUATOR / SİLİNDİR SEVİYESİ",left,actY-px(58),Color.rgb(220,225,230),7);
      for(int a=0;a<na;a++){
        float ax=left+(right-left)*(a+.5f)/na;float fx=0,fy=0,fz=0;for(SNode s:solved)if(s.act==a){fx+=s.fx;fy+=s.fy;fz+=s.fz;}double rr=Math.sqrt(fx*fx+fy*fy+fz*fz);
        float[] src=cur.get(Math.min(cur.size()-1,a));line(c,src[0],src[1],ax,lcY-px(16),Color.rgb(170,180,188),1.4f);
        p.setColor(Color.rgb(75,180,95));c.drawRoundRect(new RectF(ax-px(11),lcY-px(16),ax+px(11),lcY+px(16)),px(4),px(4),p);text(c,"LC-"+(a+1),ax-px(12),lcY+px(34),Color.rgb(170,245,185),5.2f);text(c,String.format(Locale.US,"%.0fN",rr),ax-px(14),lcY+px(50),Color.WHITE,4.8f);
        line(c,ax,lcY+px(17),ax,actY-px(42),Color.rgb(175,185,192),1.4f);
        int body=qActType.getSelectedItemPosition()==0?Color.rgb(70,78,85):Color.rgb(120,82,165);p.setColor(body);c.drawRoundRect(new RectF(ax-px(18),actY-px(42),ax+px(18),actY+px(42)),px(5),px(5),p);p.setColor(Color.rgb(190,195,200));c.drawRect(ax-px(4),actY-px(67),ax+px(4),actY-px(42),p);
        text(c,"ACT-"+(a+1),ax-px(16),actY+px(61),Color.WHITE,5.3f);double util=100*rr/av(a);text(c,String.format(Locale.US,"Demand %.0fN",rr),ax-px(24),actY+px(78),Color.rgb(247,207,77),4.7f);text(c,String.format(Locale.US,"Util %.0f%%",util),ax-px(17),actY+px(95),util<=100?Color.rgb(100,225,120):Color.rgb(240,85,75),4.7f);
      }

      p.setColor(Color.rgb(48,55,61));c.drawRect(left,actY+px(125),right,actY+px(149),p);text(c,"STRONGBACK / GROUND REACTION",left+px(8),actY+px(174),Color.rgb(205,215,225),6.4f);

      float lgY=actY+px(220);p.setColor(Color.rgb(13,34,48));c.drawRoundRect(new RectF(left,lgY,right,lgY+px(180)),px(8),px(8),p);
      text(c,"GÖRSEL ANAHTAR",left+px(10),lgY+px(24),Color.WHITE,7);
      p.setColor(Color.rgb(70,145,245));c.drawRect(left+px(12),lgY+px(39),left+px(28),lgY+px(51),p);text(c,"Yük uygulama oku / station",left+px(36),lgY+px(50),Color.rgb(200,220,240),5.5f);
      p.setColor(Color.rgb(90,200,110));c.drawRect(left+px(12),lgY+px(63),left+px(28),lgY+px(75),p);text(c,"Load pad / load cell",left+px(36),lgY+px(74),Color.rgb(200,220,240),5.5f);
      p.setColor(Color.rgb(220,145,32));c.drawRect(left+px(12),lgY+px(87),left+px(28),lgY+px(99),p);text(c,"Whiffletree beam / kademe",left+px(36),lgY+px(98),Color.rgb(200,220,240),5.5f);
      p.setColor(Color.rgb(75,80,86));c.drawRect(left+px(12),lgY+px(111),left+px(28),lgY+px(123),p);text(c,"Actuator / silindir",left+px(36),lgY+px(122),Color.rgb(200,220,240),5.5f);
      p.setColor(Color.rgb(48,55,61));c.drawRect(left+px(12),lgY+px(135),left+px(28),lgY+px(147),p);text(c,"Strongback / ground",left+px(36),lgY+px(146),Color.rgb(200,220,240),5.5f);
    }
  }
}
