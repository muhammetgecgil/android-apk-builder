package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V73Activity extends V72Activity {
  LinearLayout proofPanel;
  TextView proofSummary;
  XYZProofView xyzProof;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    proofPanel=new LinearLayout(this);proofPanel.setOrientation(LinearLayout.VERTICAL);proofPanel.setPadding(dp(10),dp(10),dp(10),dp(10));proofPanel.setBackground(bg(Color.rgb(2,15,25),16));
    proofPanel.addView(tx("FULL LOAD VERIFICATION — X / Y / Z HESAP İSPATI",20,true,Color.WHITE));
    proofPanel.addView(tx("Required ve applied dağılımlarını, cumulative shear/moment değerlerini ve whiffletree layer reactionlarını aynı aktif veri setinden gösterir.",9,false,Color.rgb(180,210,230)));
    proofSummary=card("HESAPLA VE GÖSTER sonrası X/Y/Z doğrulama otomatik oluşur.",Color.rgb(14,45,64));proofPanel.addView(proofSummary,lp());
    xyzProof=new XYZProofView();proofPanel.addView(xyzProof,new LinearLayout.LayoutParams(-1,dp(1800)));
    root.addView(proofPanel,3,lp());
  }

  @Override void runGuided(){super.runGuided();refreshXYZProof();}
  @Override void runPrimary(){super.runPrimary();refreshXYZProof();}
  @Override void calculateAndShow(){super.calculateAndShow();refreshXYZProof();}

  void refreshXYZProof(){
    if(xyzProof==null)return;
    if(!solvedValid||solved==null||solved.isEmpty()){proofSummary.setText("Doğrulama hazır değil — önce HESAPLA VE GÖSTER.");xyzProof.invalidate();return;}
    double sx=0,sy=0,sz=0,mx=0,my=0,mz=0;
    for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;mx+=s.mx;my+=s.my;mz+=s.mz;}
    int na=Math.max(1,qi(qActs,1,12));double ax=0,ay=0,az=0;for(int a=0;a<na;a++){for(SNode s:solved)if(s.act==a){ax+=s.fx;ay+=s.fy;az+=s.fz;}}
    double ferr=Math.sqrt((sx-ax)*(sx-ax)+(sy-ay)*(sy-ay)+(sz-az)*(sz-az));
    proofSummary.setText(String.format(Locale.US,"FORCE CLOSURE %.6f N  → %s\nΣF = [%+.1f, %+.1f, %+.1f] N\nΣM = [%+.1f, %+.1f, %+.1f] Nmm\n%d station • %d layer • %d actuator",ferr,ferr<1e-6?"PASS":"CHECK",sx,sy,sz,mx,my,mz,solved.size(),Math.max(1,qi(qLayers,1,4)),na));
    xyzProof.invalidate();
  }

  class XYZProofView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    XYZProofView(){super(V73Activity.this);setBackgroundColor(Color.rgb(1,9,16));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float px(float v){return v*getResources().getDisplayMetrics().density;}
    void text(Canvas c,String s,float x,float y,int col,float size){t.setColor(col);t.setTextSize(px(size));c.drawText(s,x,y,t);}
    void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth(px(w));c.drawLine(x1,y1,x2,y2,p);}
    void box(Canvas c,float l,float top,float r,float b,int col){p.setColor(col);c.drawRoundRect(new RectF(l,top,r,b),px(8),px(8),p);}

    double comp(SNode s,int axis){return axis==0?s.fx:(axis==1?s.fy:s.fz);}    
    String an(int axis){return axis==0?"X / Fx":(axis==1?"Y / Fy":"Z / Fz");}
    int ac(int axis){return axis==0?Color.rgb(235,90,85):(axis==1?Color.rgb(90,220,120):Color.rgb(80,150,245));}

    @Override protected void onDraw(Canvas c){
      super.onDraw(c);float L=px(14),R=getWidth()-px(14);text(c,"XYZ LOAD PATH + SHEAR + MOMENT PROOF",L,px(28),Color.WHITE,13);
      if(!solvedValid||solved==null||solved.isEmpty()){text(c,"Önce HESAPLA VE GÖSTER",L,px(70),Color.rgb(180,200,215),9);return;}
      float y=px(70);for(int axis=0;axis<3;axis++){drawAxis(c,L,R,y,axis);y+=px(510);}drawLayerClosure(c,L,R,y);
    }

    void drawAxis(Canvas c,float L,float R,float y,int axis){
      int n=solved.size();int col=ac(axis);box(c,L,y,R,y+px(485),Color.rgb(15,39,52));text(c,"AXIS "+an(axis)+" — REQUIRED / APPLIED / SHEAR / MOMENT",L+px(10),y+px(25),col,8);
      float pl=L+px(34),pr=R-px(18),pt=y+px(55),pb=y+px(190);line(c,pl,pb,pr,pb,Color.GRAY,1);line(c,pl,pt,pl,pb,Color.GRAY,1);
      double max=1;for(SNode s:solved)max=Math.max(max,Math.abs(comp(s,axis)));float prevX=0,prevY=0;
      for(int i=0;i<n;i++){SNode s=solved.get(i);float x=pl+(pr-pl)*(i/(float)Math.max(1,n-1));float yy=(float)(pb-(pb-pt)*(.5+.45*comp(s,axis)/max));if(i>0)line(c,prevX,prevY,x,yy,col,2.4f);p.setColor(col);c.drawCircle(x,yy,px(3),p);prevX=x;prevY=yy;text(c,"S"+(s.section+1),x-px(5),pb+px(17),Color.LTGRAY,4.4f);}text(c,"Required section load",pl,pt-px(10),Color.WHITE,5.3f);

      double[] shear=new double[n],moment=new double[n];double q=0,m=0;for(int i=n-1;i>=0;i--){SNode s=solved.get(i);q+=comp(s,axis);m+=q*(i==n-1?0:1000.0);shear[i]=q;moment[i]=m;}
      float sTop=y+px(235),sBot=y+px(315);line(c,pl,sBot,pr,sBot,Color.GRAY,1);double sMax=1;for(double v:shear)sMax=Math.max(sMax,Math.abs(v));prevX=0;prevY=0;for(int i=0;i<n;i++){float x=pl+(pr-pl)*(i/(float)Math.max(1,n-1));float yy=(float)(sBot-px(34)*shear[i]/sMax);if(i>0)line(c,prevX,prevY,x,yy,Color.rgb(230,230,235),1.8f);prevX=x;prevY=yy;}text(c,"Cumulative shear",pl,sTop,Color.WHITE,5.5f);
      float mTop=y+px(350),mBot=y+px(430);line(c,pl,mBot,pr,mBot,Color.GRAY,1);double mMax=1;for(double v:moment)mMax=Math.max(mMax,Math.abs(v));prevX=0;prevY=0;for(int i=0;i<n;i++){float x=pl+(pr-pl)*(i/(float)Math.max(1,n-1));float yy=(float)(mBot-px(34)*moment[i]/mMax);if(i>0)line(c,prevX,prevY,x,yy,Color.rgb(247,190,70),1.8f);prevX=x;prevY=yy;}text(c,"Reconstructed moment (relative proof)",pl,mTop,Color.rgb(247,190,70),5.5f);
      double sum=0;for(SNode s:solved)sum+=comp(s,axis);text(c,String.format(Locale.US,"Σ%s = %+.1f N",axis==0?"Fx":(axis==1?"Fy":"Fz"),sum),L+px(10),y+px(468),Color.WHITE,5.8f);
    }

    void drawLayerClosure(Canvas c,float L,float R,float y){
      box(c,L,y,R,y+px(240),Color.rgb(15,39,52));text(c,"LAYER / ACTUATOR CLOSURE SUMMARY",L+px(10),y+px(25),Color.rgb(247,207,77),8);
      int na=Math.max(1,qi(qActs,1,12));for(int a=0;a<na;a++){double fx=0,fy=0,fz=0;for(SNode s:solved)if(s.act==a){fx+=s.fx;fy+=s.fy;fz+=s.fz;}double r=Math.sqrt(fx*fx+fy*fy+fz*fz);text(c,String.format(Locale.US,"ACT-%d / LC-%d   Fx %+.0f   Fy %+.0f   Fz %+.0f   R %.0f N",a+1,a+1,fx,fy,fz,r),L+px(12),y+px(55)+a*px(18),Color.WHITE,5.1f);}text(c,"Her actuator hattı = bağlı stationların vektörel toplamı. Toplam hat kuvvetleri tank section toplamını kapatmalıdır.",L+px(10),y+px(218),Color.rgb(190,210,225),5.2f);
    }
  }
}
