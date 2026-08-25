package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V71Activity extends V70Activity {
  LinearLayout livePanel;
  TextView liveSummary, layerSummary;
  ForceVisibilityView forceView;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    livePanel=new LinearLayout(this);livePanel.setOrientation(LinearLayout.VERTICAL);livePanel.setPadding(dp(10),dp(10),dp(10),dp(10));livePanel.setBackground(bg(Color.rgb(2,17,28),16));
    livePanel.addView(tx("TEK VERİ SETİ — CANLI HESAP & KUVVET GÖRÜNÜRLÜĞÜ",20,true,Color.WHITE));
    livePanel.addView(tx("HESAPLA dediğinde ADIM 1–7 girişlerindeki değerler bütün hesap ekranlarına aktarılır. 2D/poster/load-cell/actuator/denge aynı aktif veri setini kullanır.",9,false,Color.rgb(180,210,230)));
    liveSummary=card("Henüz hesap yok. Girişleri doldurup HESAPLA VE 2D / 3D GÖSTER'e bas.",Color.rgb(14,45,64));livePanel.addView(liveSummary,lp());
    forceView=new ForceVisibilityView();livePanel.addView(forceView,new LinearLayout.LayoutParams(-1,dp(1320)));
    layerSummary=card("Layer ve actuator kuvvet özeti hesap sonrası burada görünür.",Color.rgb(13,39,54));livePanel.addView(layerSummary,lp());
    root.addView(livePanel,1,lp());
  }

  void syncGuidedInputs(){
    double L=gv(gLen),D=gv(gDia),fx=gv(gFx),fy=gv(gFy),fz=gv(gFz),cap=Math.max(1,gv(gActCap));
    int ns=gi(gSections,1,8),na=gi(gActs,1,12),nl=gi(gLayers,1,4);
    qLength.setText(String.format(Locale.US,"%.3f",L));qDiameter.setText(String.format(Locale.US,"%.3f",D));
    qSections.setText(String.valueOf(ns));pSections.setText(String.valueOf(ns));
    pFx.setText(String.format(Locale.US,"%.3f",fx));pFy.setText(String.format(Locale.US,"%.3f",fy));pFz.setText(String.format(Locale.US,"%.3f",fz));
    qFx.setText(String.format(Locale.US,"%.3f",fx));qFy.setText(String.format(Locale.US,"%.3f",fy));qFz.setText(String.format(Locale.US,"%.3f",fz));
    pActs.setText(String.valueOf(na));pLayers.setText(String.valueOf(nl));qActs.setText(String.valueOf(na));qLayers.setText(String.valueOf(nl));
    pActType.setSelection(gActType.getSelectedItemPosition());qActType.setSelection(gActType.getSelectedItemPosition());qPhase.setSelection(gPhase.getSelectedItemPosition());
    for(int i=0;i<actKnown.length;i++)actKnown[i].setText(String.format(Locale.US,"%.0f",cap));
    seedZones();
  }

  @Override void runGuided(){
    if(gv(gLen)<=0||gv(gDia)<=0){guideState.setText("GİRİŞ HATASI — Tank uzunluğu ve çapı sıfırdan büyük olmalı.");return;}
    syncGuidedInputs();
    super.runPrimary();
    guideState.setText(String.format(Locale.US,"TÜM GİRDİLER TEK MODELE AKTARILDI\nL %.3f m • D %.3f m • %d station • Fx %+.0f N • Fy %+.0f N • Fz %+.0f N\n%d actuator • %d layer • %s • %s\nAynı değerler tüm hesap ve görsel ekranlarında kullanılıyor.",gv(gLen),gv(gDia),gi(gSections,1,8),gv(gFx),gv(gFy),gv(gFz),gi(gActs,1,12),gi(gLayers,1,4),gActType.getSelectedItem().toString(),gPhase.getSelectedItem().toString()));
    refreshForceVisibility();
  }

  @Override void runPrimary(){
    super.runPrimary();
    refreshForceVisibility();
  }

  @Override void calculateAndShow(){
    super.calculateAndShow();
    refreshForceVisibility();
  }

  void refreshForceVisibility(){
    if(forceView==null)return;
    if(!solvedValid||solved==null||solved.isEmpty()){
      liveSummary.setText("HESAP HAZIR DEĞİL — giriş ekranındaki değerler henüz çözüme aktarılmadı.");
      layerSummary.setText("Layer kuvvetleri hesap sonrası görünür.");forceView.invalidate();return;
    }
    int na=Math.max(1,qi(qActs,1,12)),nl=Math.max(1,qi(qLayers,1,4));
    double sx=0,sy=0,sz=0,mx=0,my=0,mz=0,maxR=0;
    for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;mx+=s.mx;my+=s.my;mz+=s.mz;maxR=Math.max(maxR,s.r);}
    liveSummary.setText(String.format(Locale.US,"AKTİF HESAP VERİ SETİ\n%d station • %d layer • %d actuator • %s\nΣFx %+.1f N | ΣFy %+.1f N | ΣFz %+.1f N | Peak station %.1f N\nΣMx %+.1f | ΣMy %+.1f | ΣMz %+.1f Nmm\nBu değerler 2D, poster, load-cell, actuator ve hesap ispatında aynıdır.",solved.size(),nl,na,qActType.getSelectedItemPosition()==0?"HYDRAULIC":"ELECTRIC",sx,sy,sz,maxR,mx,my,mz));
    StringBuilder sb=new StringBuilder("ACTUATOR / LOAD-CELL VEKTÖR ÖZETİ\n");
    for(int a=0;a<na;a++){
      double fx=0,fy=0,fz=0;for(SNode s:solved)if(s.act==a){fx+=s.fx;fy+=s.fy;fz+=s.fz;}
      double r=Math.sqrt(fx*fx+fy*fy+fz*fz),u=100*r/av(a);
      sb.append(String.format(Locale.US,"ACT-%d / LC-%d : Fx %+.0f  Fy %+.0f  Fz %+.0f  R %.0f N  | util %.1f%%\n",a+1,a+1,fx,fy,fz,r,u));
    }
    sb.append("Her whiffletree beam üzerinde Fx/Fy/Fz ve resultant değerleri görselde yazılıdır.");
    layerSummary.setText(sb.toString());forceView.invalidate();
  }

  class ForceVisibilityView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    ForceVisibilityView(){super(V71Activity.this);setBackgroundColor(Color.rgb(1,9,16));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float px(float v){return v*getResources().getDisplayMetrics().density;}
    void text(Canvas c,String s,float x,float y,int col,float size){t.setColor(col);t.setTextSize(px(size));c.drawText(s,x,y,t);}
    void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth(px(w));c.drawLine(x1,y1,x2,y2,p);}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);int W=getWidth();float left=px(24),right=W-px(24);
      text(c,"WHIFFLETREE FORCE MAP — HER SEVİYEDE KUVVET",px(12),px(28),Color.WHITE,13);
      text(c,"Kırmızı Fx   Yeşil Fy   Mavi Fz   Beyaz R",px(12),px(48),Color.rgb(195,215,230),6.3f);
      if(!solvedValid||solved==null||solved.isEmpty()){text(c,"HESAPLA VE GÖSTER ile aktif modeli çöz.",px(12),px(82),Color.rgb(180,200,215),8);return;}
      int na=Math.max(1,qi(qActs,1,12)),nl=Math.max(1,qi(qLayers,1,4));double len=Math.max(1,qd(qLength));
      float tankY=px(145);p.setColor(Color.rgb(87,96,104));c.drawRoundRect(new RectF(left,tankY-px(38),right,tankY+px(38)),px(38),px(38),p);text(c,"EFT / LOAD APPLICATION",left+px(8),tankY+px(4),Color.WHITE,7);
      ArrayList<float[]> cur=new ArrayList<>();
      for(SNode s:solved){float x=(float)(left+(s.x+len/2.0)/len*(right-left));p.setColor(Color.rgb(92,210,115));c.drawRect(x-px(7),tankY+px(40),x+px(7),tankY+px(52),p);text(c,"S"+(s.section+1),x-px(6),tankY-px(58),Color.WHITE,5.3f);text(c,String.format(Locale.US,"%+.0f/%+.0f/%+.0f",s.fx,s.fy,s.fz),x-px(22),tankY-px(43),Color.rgb(190,220,245),4.3f);cur.add(new float[]{x,tankY+px(53),(float)s.fx,(float)s.fy,(float)s.fz});}
      float y=px(300);
      for(int l=1;l<=nl;l++){
        int target=(l==nl)?na:Math.max(na,(int)Math.ceil(cur.size()/2.0));ArrayList<float[]> next=new ArrayList<>();text(c,"LAYER "+l,left,y-px(38),Color.rgb(247,190,70),7);
        for(int g=0;g<target;g++){
          int i0=(int)Math.floor((double)g*cur.size()/target),i1=Math.max(i0,Math.min(cur.size()-1,(int)Math.floor((double)(g+1)*cur.size()/target)-1));float x=0,fx=0,fy=0,fz=0;int cnt=0;
          for(int i=i0;i<=i1;i++){float[] q=cur.get(i);x+=q[0];fx+=q[2];fy+=q[3];fz+=q[4];cnt++;}x/=Math.max(1,cnt);
          for(int i=i0;i<=i1;i++){float[] q=cur.get(i);line(c,q[0],q[1],x,y-px(12),Color.rgb(145,160,172),1.6f);}p.setColor(Color.rgb(222,147,34));c.drawRoundRect(new RectF(x-px(38),y-px(10),x+px(38),y+px(10)),px(3),px(3),p);p.setColor(Color.LTGRAY);c.drawCircle(x,y,px(4),p);
          double r=Math.sqrt(fx*fx+fy*fy+fz*fz);text(c,String.format(Locale.US,"Fx %+.0f",fx),x-px(32),y+px(28),Color.rgb(235,95,90),4.8f);text(c,String.format(Locale.US,"Fy %+.0f",fy),x-px(32),y+px(43),Color.rgb(95,225,125),4.8f);text(c,String.format(Locale.US,"Fz %+.0f",fz),x-px(32),y+px(58),Color.rgb(95,165,245),4.8f);text(c,String.format(Locale.US,"R %.0f N",r),x-px(32),y+px(73),Color.WHITE,4.8f);next.add(new float[]{x,y+px(13),fx,fy,fz});
        }cur=next;y+=px(165);
      }
      float lcY=y+px(18),actY=lcY+px(145);text(c,"LOAD CELL",left,lcY-px(38),Color.rgb(105,230,125),7);text(c,"ACTUATOR",left,actY-px(58),Color.rgb(220,225,230),7);
      for(int a=0;a<na;a++){
        float ax=left+(right-left)*(a+.5f)/na,fx=0,fy=0,fz=0;for(SNode s:solved)if(s.act==a){fx+=s.fx;fy+=s.fy;fz+=s.fz;}double r=Math.sqrt(fx*fx+fy*fy+fz*fz);float[] src=cur.get(Math.min(cur.size()-1,a));line(c,src[0],src[1],ax,lcY-px(14),Color.rgb(160,175,185),1.5f);p.setColor(Color.rgb(73,181,95));c.drawRect(ax-px(10),lcY-px(14),ax+px(10),lcY+px(14),p);text(c,"LC"+(a+1),ax-px(10),lcY+px(31),Color.WHITE,5);text(c,String.format(Locale.US,"R %.0f",r),ax-px(14),lcY+px(47),Color.WHITE,4.7f);line(c,ax,lcY+px(15),ax,actY-px(38),Color.rgb(170,180,188),1.5f);p.setColor(qActType.getSelectedItemPosition()==0?Color.rgb(64,72,79):Color.rgb(116,78,160));c.drawRoundRect(new RectF(ax-px(16),actY-px(38),ax+px(16),actY+px(38)),px(5),px(5),p);text(c,"ACT"+(a+1),ax-px(13),actY+px(55),Color.WHITE,5);text(c,String.format(Locale.US,"%.0fN",r),ax-px(13),actY+px(72),Color.rgb(247,207,77),4.8f);
      }
      p.setColor(Color.rgb(48,55,61));c.drawRect(left,actY+px(105),right,actY+px(128),p);text(c,"STRONGBACK / GROUND",left+px(8),actY+px(151),Color.rgb(205,215,225),6.3f);
    }
  }
}
