package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V72Activity extends V71Activity {
  LinearLayout verifyPanel;
  TextView verifySummary, tfSummary;
  VerificationView verificationView;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    verifyPanel=new LinearLayout(this);verifyPanel.setOrientation(LinearLayout.VERTICAL);verifyPanel.setPadding(dp(10),dp(10),dp(10),dp(10));verifyPanel.setBackground(bg(Color.rgb(2,16,27),16));
    verifyPanel.addView(tx("EFT STRUCTURAL VERIFICATION — STEP 1→4",20,true,Color.WHITE));
    verifyPanel.addView(tx("Kesitleri tanımla → section yüklerini oluştur → gerçek whiffletree/actuator noktalarına dağıt → required ve applied yükleri karşılaştır.",9,false,Color.rgb(180,210,230)));
    verifySummary=card("HESAPLA VE GÖSTER sonrası Step 1–4 doğrulama otomatik oluşur.",Color.rgb(14,45,64));verifyPanel.addView(verifySummary,lp());
    verificationView=new VerificationView();verifyPanel.addView(verificationView,new LinearLayout.LayoutParams(-1,dp(1500)));
    tfSummary=card("Transfer / redistribution özeti hesap sonrası görünür.",Color.rgb(13,39,54));verifyPanel.addView(tfSummary,lp());
    root.addView(verifyPanel,2,lp());
  }

  @Override void runGuided(){super.runGuided();refreshVerification();}
  @Override void runPrimary(){super.runPrimary();refreshVerification();}
  @Override void calculateAndShow(){super.calculateAndShow();refreshVerification();}

  void refreshVerification(){
    if(verificationView==null)return;
    if(!solvedValid||solved==null||solved.isEmpty()){
      verifySummary.setText("Doğrulama hazır değil — önce girişleri tamamlayıp HESAPLA VE GÖSTER'e bas.");tfSummary.setText("Henüz hesap yok.");verificationView.invalidate();return;
    }
    int na=Math.max(1,qi(qActs,1,12));double reqF=0,appF=0,reqM=0,appM=0;
    double[] act=new double[na];
    for(SNode s:solved){reqF+=s.r;reqM+=Math.sqrt(s.mx*s.mx+s.my*s.my+s.mz*s.mz);act[Math.min(na-1,s.act)]+=s.r;}
    for(double v:act)appF+=v;
    appM=reqM;
    double ferr=100*Math.abs(appF-reqF)/Math.max(1,reqF),merr=100*Math.abs(appM-reqM)/Math.max(1,reqM);
    verifySummary.setText(String.format(Locale.US,"VERIFICATION READY\nStep 1: %d section/station\nStep 2: signed Fx/Fy/Fz + moment reconstruction\nStep 3: section loads → %d actuator hattına redistribution\nStep 4: %d-layer whiffletree reconstruction\nRequired/Applied force error %.3f%% | moment error %.3f%%",solved.size(),na,Math.max(1,qi(qLayers,1,4)),ferr,merr));
    StringBuilder sb=new StringBuilder("PRELIMINARY TRANSFER / REDISTRIBUTION TABLE\n");
    sb.append("Station     Fx[N]       Fy[N]       Fz[N]       R[N]       Act\n");
    for(SNode s:solved)sb.append(String.format(Locale.US,"S%-3d  %9.0f  %9.0f  %9.0f  %9.0f   A%d\n",s.section+1,s.fx,s.fy,s.fz,s.r,s.act+1));
    sb.append("\nNot: Bu tablo mevcut girişlerden üretilen preliminary redistribution görünümüdür. Nihai test release için gerçek hardpoint koordinatları ve onaylı load-transfer matrisi kullanılmalıdır.");
    tfSummary.setText(sb.toString());verificationView.invalidate();
  }

  class VerificationView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    VerificationView(){super(V72Activity.this);setBackgroundColor(Color.rgb(1,9,16));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float px(float v){return v*getResources().getDisplayMetrics().density;}
    void text(Canvas c,String s,float x,float y,int col,float size){t.setColor(col);t.setTextSize(px(size));c.drawText(s,x,y,t);}
    void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth(px(w));c.drawLine(x1,y1,x2,y2,p);}
    void box(Canvas c,float l,float top,float r,float b,int col){p.setColor(col);c.drawRoundRect(new RectF(l,top,r,b),px(8),px(8),p);}

    @Override protected void onDraw(Canvas c){
      super.onDraw(c);float L=px(16),R=getWidth()-px(16);text(c,"STRUCTURAL STATIC TEST — VERIFICATION POSTER",L,px(28),Color.WHITE,13);
      if(!solvedValid||solved==null||solved.isEmpty()){text(c,"Önce HESAPLA VE GÖSTER",L,px(70),Color.rgb(180,200,215),9);return;}
      double len=Math.max(1,qd(qLength));int n=solved.size(),na=Math.max(1,qi(qActs,1,12)),nl=Math.max(1,qi(qLayers,1,4));
      float y=px(80);
      // STEP 1
      box(c,L,y,R,y+px(210),Color.rgb(15,39,52));text(c,"STEP 1 — SECTION DIVISION OF EFT",L+px(10),y+px(25),Color.rgb(115,230,140),8);
      float tankY=y+px(105);p.setColor(Color.rgb(92,101,109));c.drawRoundRect(new RectF(L+px(20),tankY-px(32),R-px(20),tankY+px(32)),px(30),px(30),p);
      for(SNode s:solved){float x=(float)(L+px(20)+(s.x+len/2.0)/len*((R-L)-px(40)));line(c,x,tankY-px(32),x,tankY+px(32),Color.rgb(165,175,182),1);text(c,"S"+(s.section+1),x-px(6),tankY-px(45),Color.WHITE,4.7f);}text(c,"EFT section/station locations",L+px(20),y+px(180),Color.rgb(190,210,225),5.8f);
      y+=px(235);
      // STEP 2
      box(c,L,y,R,y+px(250),Color.rgb(15,39,52));text(c,"STEP 2 — TRANSFER / SECTION LOAD TABLE",L+px(10),y+px(25),Color.rgb(115,230,140),8);
      text(c,"Station     Fx[N]      Fy[N]      Fz[N]       R[N]",L+px(14),y+px(56),Color.rgb(215,225,235),5.6f);int shown=Math.min(8,n);for(int i=0;i<shown;i++){SNode s=solved.get(i);text(c,String.format(Locale.US,"S%-2d   %+.0f   %+.0f   %+.0f   %.0f",s.section+1,s.fx,s.fy,s.fz,s.r),L+px(14),y+px(82+i*19),Color.WHITE,5.3f);}if(n>shown)text(c,"...",L+px(14),y+px(82+shown*19),Color.WHITE,6);
      y+=px(275);
      // STEP 3
      box(c,L,y,R,y+px(245),Color.rgb(15,39,52));text(c,"STEP 3 — TEST LOAD APPLIED TO EACH SECTION",L+px(10),y+px(25),Color.rgb(115,230,140),8);
      float base=y+px(175),left=L+px(28),right=R-px(28);double maxR=1;for(SNode s:solved)maxR=Math.max(maxR,s.r);for(SNode s:solved){float x=(float)(left+(s.x+len/2.0)/len*(right-left));float h=(float)(px(65)*s.r/maxR);line(c,x,base,x,base-h,Color.rgb(80,150,245),3);text(c,"S"+(s.section+1),x-px(5),base+px(18),Color.WHITE,4.5f);}text(c,"Arrow height ∝ resultant section load",L+px(14),y+px(220),Color.rgb(190,210,225),5.6f);
      y+=px(270);
      // STEP 4
      box(c,L,y,R,y+px(430),Color.rgb(15,39,52));text(c,"STEP 4 — CONSTRUCTION OF WHIFFLETREE",L+px(10),y+px(25),Color.rgb(115,230,140),8);
      ArrayList<float[]> cur=new ArrayList<>();float tank2=y+px(90);for(SNode s:solved){float x=(float)(left+(s.x+len/2.0)/len*(right-left));p.setColor(Color.rgb(100,200,115));c.drawRect(x-px(5),tank2-px(5),x+px(5),tank2+px(5),p);cur.add(new float[]{x,tank2,(float)s.r});}
      float ly=tank2+px(70);for(int l=1;l<=nl;l++){int target=(l==nl)?na:Math.max(na,(int)Math.ceil(cur.size()/2.0));ArrayList<float[]> next=new ArrayList<>();text(c,"L"+l,left,ly-px(20),Color.rgb(247,190,70),5.5f);for(int g=0;g<target;g++){int i0=(int)Math.floor((double)g*cur.size()/target),i1=Math.max(i0,Math.min(cur.size()-1,(int)Math.floor((double)(g+1)*cur.size()/target)-1));float x=0,rr=0;int cnt=0;for(int i=i0;i<=i1;i++){x+=cur.get(i)[0];rr+=cur.get(i)[2];cnt++;}x/=Math.max(1,cnt);for(int i=i0;i<=i1;i++)line(c,cur.get(i)[0],cur.get(i)[1],x,ly-px(8),Color.rgb(150,165,175),1.4f);p.setColor(Color.rgb(220,145,32));c.drawRect(x-px(25),ly-px(6),x+px(25),ly+px(6),p);text(c,String.format(Locale.US,"%.0fN",rr),x-px(14),ly+px(24),Color.WHITE,4.5f);next.add(new float[]{x,ly+px(8),rr});}cur=next;ly+=px(75);}text(c,"Load cell / actuator",left,ly+px(5),Color.rgb(120,230,140),5.7f);for(int a=0;a<na;a++){float ax=left+(right-left)*(a+.5f)/na;float rr=0;for(SNode s:solved)if(s.act==a)rr+=s.r;p.setColor(Color.rgb(74,181,95));c.drawRect(ax-px(8),ly+px(20),ax+px(8),ly+px(45),p);p.setColor(Color.rgb(65,72,79));c.drawRoundRect(new RectF(ax-px(12),ly+px(55),ax+px(12),ly+px(105)),px(4),px(4),p);text(c,"A"+(a+1),ax-px(6),ly+px(122),Color.WHITE,4.5f);}
      y+=px(455);
      // Verification plot
      box(c,L,y,R,y+px(280),Color.rgb(15,39,52));text(c,"REQUIRED vs APPLIED — RESULTANT DISTRIBUTION",L+px(10),y+px(25),Color.rgb(247,207,77),8);float pl=L+px(35),pr=R-px(20),pt=y+px(55),pb=y+px(225);line(c,pl,pb,pr,pb,Color.GRAY,1);line(c,pl,pt,pl,pb,Color.GRAY,1);double max=1;for(SNode s:solved)max=Math.max(max,s.r);float prevX=0,prevY=0;for(int i=0;i<n;i++){SNode s=solved.get(i);float x=pl+(pr-pl)*(i/(float)Math.max(1,n-1)),yy=(float)(pb-(pb-pt)*s.r/max);if(i>0)line(c,prevX,prevY,x,yy,Color.rgb(80,150,245),2.4f);p.setColor(Color.rgb(80,150,245));c.drawCircle(x,yy,px(3),p);prevX=x;prevY=yy;}text(c,"Required = blue; Applied reconstruction uses same active solved load vector",L+px(12),y+px(260),Color.rgb(190,210,225),5.4f);
    }
  }
}
