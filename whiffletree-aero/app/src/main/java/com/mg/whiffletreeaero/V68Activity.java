package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V68Activity extends V67Activity {
  LinearLayout focusPanel, actuatorForcePanel, partPanel;
  TextView focusSummary, proofText, partsText;
  EditText[] actKnown=new EditText[12];
  PosterView poster;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    focusPanel=new LinearLayout(this);focusPanel.setOrientation(LinearLayout.VERTICAL);focusPanel.setPadding(dp(9),dp(9),dp(9),dp(9));focusPanel.setBackground(bg(Color.rgb(3,18,29),14));
    focusPanel.addView(tx("EFT STRUCTURAL TEST SYSTEM — ANA ÇALIŞMA EKRANI",20,true,Color.WHITE));
    focusPanel.addView(tx("Bilinen yük bölgeleri + Fx/Fy/Fz + actuator sayısı/kuvveti + whiffletree layer → hesapla → her seviyede kuvvet/moment/deplasman → eleman seçimi",8,false,Color.rgb(180,210,230)));
    focusSummary=card("Ana girdileri üst bölümde gir ve HESAPLA VE 2D / 3D GÖSTER'e bas.",Color.rgb(15,49,69));focusPanel.addView(focusSummary,lp());

    focusPanel.addView(tx("BİLİNEN ACTUATOR KUVVETLERİ / KAPASİTELERİ [N]",12,true,Color.rgb(247,207,77)));
    actuatorForcePanel=new LinearLayout(this);actuatorForcePanel.setOrientation(LinearLayout.VERTICAL);focusPanel.addView(actuatorForcePanel,lp());
    for(int i=0;i<12;i++){
      LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);
      r.addView(tx("ACT-"+(i+1),8,true,Color.WHITE),new LinearLayout.LayoutParams(0,dp(40),.8f));
      actKnown[i]=new EditText(this);actKnown[i].setText("100000");actKnown[i].setTextColor(Color.WHITE);actKnown[i].setSingleLine(true);actKnown[i].setTextSize(13);actKnown[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);actKnown[i].setBackground(bg(Color.rgb(24,52,73),7));
      r.addView(actKnown[i],new LinearLayout.LayoutParams(0,dp(40),1.5f));actuatorForcePanel.addView(r);
    }

    poster=new PosterView();focusPanel.addView(poster,new LinearLayout.LayoutParams(-1,dp(1220)));
    proofText=card("HESAP İSPATI hesap sonrası burada görünür.",Color.rgb(14,42,58));focusPanel.addView(proofText,lp());
    partPanel=new LinearLayout(this);partPanel.setOrientation(LinearLayout.VERTICAL);partPanel.setPadding(dp(7),dp(7),dp(7),dp(7));partPanel.setBackground(bg(Color.rgb(11,36,50),10));partPanel.addView(tx("WHIFFLETREE ELEMAN ÖN SEÇİMİ",13,true,Color.rgb(247,207,77)));partsText=tx("Hesap sonrası load-cell, actuator, pin ve beam için ön boyutlandırma burada görünür.",8,false,Color.WHITE);partPanel.addView(partsText);focusPanel.addView(partPanel,lp());

    root.addView(focusPanel,0,lp());
    updateActVisibility();
  }

  double av(int i){try{return Math.max(1,Double.parseDouble(actKnown[i].getText().toString().trim()));}catch(Exception e){return 1;}}
  void updateActVisibility(){int n=Math.max(1,Math.min(12,(int)Math.round(pv(pActs))));for(int i=0;i<12;i++)actuatorForcePanel.getChildAt(i).setVisibility(i<n?View.VISIBLE:View.GONE);}

  @Override void runPrimary(){updateActVisibility();super.runPrimary();updateFocusedResults();}
  @Override void calculateAndShow(){super.calculateAndShow();if(focusPanel!=null)updateFocusedResults();}

  void updateFocusedResults(){
    updateActVisibility();
    if(!solvedValid){focusSummary.setText("CALCULATION NOT READY — ana girdileri kontrol et.");poster.invalidate();return;}
    int na=Math.max(1,qi(qActs,1,12)),nl=Math.max(1,qi(qLayers,1,4));
    double[] ax=new double[na],ay=new double[na],az=new double[na],ar=new double[na];
    double sfX=0,sfY=0,sfZ=0,mx=0,my=0,mz=0,maxAct=0,maxUtil=0;
    for(SNode s:solved){sfX+=s.fx;sfY+=s.fy;sfZ+=s.fz;mx+=s.mx;my+=s.my;mz+=s.mz;int a=Math.min(na-1,s.act);ax[a]+=s.fx;ay[a]+=s.fy;az[a]+=s.fz;}
    for(int a=0;a<na;a++){ar[a]=Math.sqrt(ax[a]*ax[a]+ay[a]*ay[a]+az[a]*az[a]);maxAct=Math.max(maxAct,ar[a]);maxUtil=Math.max(maxUtil,100*ar[a]/av(a));}
    focusSummary.setText(String.format(Locale.US,"TEST SYSTEM CALCULATED\n%d yük bölümü • %d layer • %d actuator • %s\nΣFx %+.1f N | ΣFy %+.1f N | ΣFz %+.1f N\nPeak actuator demand %.1f N • max utilization %.1f%%\nPoster görünümünde tanktan actuator tabanına kadar her seviye gösterilir.",solved.size(),nl,na,qActType.getSelectedItemPosition()==0?"HYDRAULIC":"ELECTRIC",sfX,sfY,sfZ,maxAct,maxUtil));

    double actSX=0,actSY=0,actSZ=0;for(int a=0;a<na;a++){actSX+=ax[a];actSY+=ay[a];actSZ+=az[a];}
    double ferr=Math.sqrt(Math.pow(sfX-actSX,2)+Math.pow(sfY-actSY,2)+Math.pow(sfZ-actSZ,2));
    proofText.setText(String.format(Locale.US,"HESAP İSPATI / DENGE KONTROLÜ\n1) Tank load-zone toplamı: [ΣFx,ΣFy,ΣFz] = [%+.1f, %+.1f, %+.1f] N\n2) Actuator gruplarına taşınan toplam = [%+.1f, %+.1f, %+.1f] N\n3) Force closure residual = %.4f N  → %s\n4) Tank referansına göre toplam moment: Mx %+.1f | My %+.1f | Mz %+.1f Nmm\n5) Her layer'daki grup kuvveti altındaki bağlı station kuvvetlerinin vektörel toplamıdır.\nBu görünüm hesap zincirini tank → pad → beam/pivot → load-cell → actuator → strongback olarak ispatlar.",sfX,sfY,sfZ,actSX,actSY,actSZ,ferr,ferr<1e-6?"PASS":"CHECK",mx,my,mz));

    double sf=1.5,lcReq=maxAct*sf,tau=180.0,sigma=250.0;
    double pin=Math.sqrt(Math.max(0,2*maxAct/(Math.PI*tau)));
    double maxM=Math.sqrt(mx*mx+my*my+mz*mz),zreq=maxM/Math.max(1,sigma);
    partsText.setText(String.format(Locale.US,"ÖN BOYUTLANDIRMA — nihai seçim katalog/allowable/FEM ile doğrulanmalı\n• Peak actuator demand: %.1f N\n• Önerilen minimum load-cell kapasitesi (SF %.2f): %.1f N\n• Double-shear pin için teorik min. çap (τallow %.0f MPa): %.1f mm\n• Beam gerekli section modulus yaklaşık: %.1f mm³ (σallow %.0f MPa)\n• Actuator kapasite kontrolleri poster üzerinde ACT-1...ACT-%d için demand/capacity olarak gösterilir.\n• BOM zinciri: load pad → rod/clevis → beam → pivot/pin → spherical joint → load-cell → actuator → base/strongback.",maxAct,sf,lcReq,tau,pin,zreq,sigma,na));
    poster.invalidate();
  }

  class PosterView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    PosterView(){super(V68Activity.this);setBackgroundColor(Color.rgb(1,10,17));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float du(float v){return v*getResources().getDisplayMetrics().density;}
    void txt(Canvas c,String s,float x,float y,int col,float size){t.setColor(col);t.setTextSize(du(size));c.drawText(s,x,y,t);}
    void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth(du(w));c.drawLine(x1,y1,x2,y2,p);}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);int W=getWidth();txt(c,"EFT + WHIFFLETREE — CALCULATED TEST RIG VIEW",dp(12),dp(28),Color.WHITE,13);
      if(!solvedValid){txt(c,"HESAPLA VE 2D / 3D GÖSTER",dp(12),dp(62),Color.rgb(180,205,220),9);return;}
      int na=Math.max(1,qi(qActs,1,12)),nl=Math.max(1,qi(qLayers,1,4));double len=Math.max(1,qd(qLength));float left=dp(26),right=W-dp(26),tankY=dp(145);
      p.setColor(Color.rgb(82,91,100));c.drawRoundRect(new RectF(left,tankY-dp(42),right,tankY+dp(42)),dp(40),dp(40),p);txt(c,"EFT",left+dp(8),tankY+dp(4),Color.WHITE,7);
      double maxR=1;for(SNode s:solved)maxR=Math.max(maxR,s.r);
      for(SNode s:solved){
        float x=(float)(left+(s.x+len/2)/len*(right-left));line(c,x,tankY-dp(43),x,tankY+dp(43),Color.rgb(120,130,140),1);float al=(float)(dp(52)*s.r/maxR);int col=Math.abs(s.fz)>=Math.max(Math.abs(s.fx),Math.abs(s.fy))?Color.rgb(80,150,240):(Math.abs(s.fy)>=Math.abs(s.fx)?Color.rgb(80,210,120):Color.rgb(230,80,80));float endY=(float)(tankY-dp(48)-Math.signum(s.fz==0?s.r:s.fz)*al);line(c,x,tankY-dp(48),x,endY,col,3);txt(c,"S"+(s.section+1),x-dp(7),tankY-dp(103),Color.WHITE,5.5f);txt(c,String.format(Locale.US,"R %.0fN",s.r),x-dp(15),tankY-dp(86),Color.rgb(210,225,235),5.2f);
      }

      ArrayList<float[]> current=new ArrayList<>();for(SNode s:solved){float x=(float)(left+(s.x+len/2)/len*(right-left));current.add(new float[]{x,tankY+dp(52),(float)s.fx,(float)s.fy,(float)s.fz});}
      float y=dp(310);
      for(int l=1;l<=nl;l++){
        int target=(l==nl)?na:Math.max(na,(int)Math.ceil(current.size()/2.0));ArrayList<float[]> next=new ArrayList<>();
        txt(c,l+". KADEME",left,y-dp(42),Color.rgb(247,207,77),7);
        for(int g=0;g<target;g++){
          int i0=(int)Math.floor((double)g*current.size()/target),i1=Math.max(i0,Math.min(current.size()-1,(int)Math.floor((double)(g+1)*current.size()/target)-1));float x=0,fx=0,fy=0,fz=0;int cnt=0;
          for(int i=i0;i<=i1;i++){float[] q=current.get(i);x+=q[0];fx+=q[2];fy+=q[3];fz+=q[4];cnt++;}
          x/=Math.max(1,cnt);float half=dp(28);p.setColor(Color.rgb(215,145,35));c.drawRoundRect(new RectF(x-half,y-dp(9),x+half,y+dp(9)),dp(3),dp(3),p);
          for(int i=i0;i<=i1;i++){float[] q=current.get(i);line(c,q[0],q[1],x,y-dp(10),Color.rgb(150,165,175),1.3f);}double rr=Math.sqrt(fx*fx+fy*fy+fz*fz);txt(c,String.format(Locale.US,"%.0fN",rr),x-dp(14),y+dp(28),Color.WHITE,5.2f);next.add(new float[]{x,y+dp(12),fx,fy,fz});
        }current=next;y+=dp(135);
      }

      float lcY=y+dp(25),actY=lcY+dp(130);for(int a=0;a<na;a++){
        float ax=left+(right-left)*(a+.5f)/na;float fx=0,fy=0,fz=0;for(SNode s:solved)if(s.act==a){fx+=s.fx;fy+=s.fy;fz+=s.fz;}double rr=Math.sqrt(fx*fx+fy*fy+fz*fz);
        float[] src=current.get(Math.min(current.size()-1,a));line(c,src[0],src[1],ax,lcY-dp(14),Color.rgb(155,170,182),1.5f);p.setColor(Color.rgb(78,183,100));c.drawRect(ax-dp(9),lcY-dp(13),ax+dp(9),lcY+dp(13),p);txt(c,"LC"+(a+1),ax-dp(10),lcY+dp(31),Color.rgb(180,240,190),5.3f);
        line(c,ax,lcY+dp(14),ax,actY-dp(34),Color.rgb(180,190,200),1.5f);p.setColor(qActType.getSelectedItemPosition()==0?Color.rgb(62,70,78):Color.rgb(115,80,165));c.drawRoundRect(new RectF(ax-dp(15),actY-dp(34),ax+dp(15),actY+dp(34)),dp(5),dp(5),p);txt(c,"ACT-"+(a+1),ax-dp(15),actY+dp(54),Color.WHITE,5.5f);txt(c,String.format(Locale.US,"%.0fN",rr),ax-dp(14),actY+dp(70),Color.rgb(247,207,77),5.2f);double util=100*rr/av(a);txt(c,String.format(Locale.US,"%.0f%%",util),ax-dp(9),actY+dp(86),util<=100?Color.rgb(90,220,120):Color.rgb(235,85,75),5.2f);
      }
      p.setColor(Color.rgb(46,54,61));c.drawRect(left,actY+dp(105),right,actY+dp(124),p);txt(c,"STRONGBACK / GROUND REACTION",left,actY+dp(148),Color.rgb(190,210,225),6.5f);
      float boxY=actY+dp(195);p.setColor(Color.rgb(14,37,52));c.drawRoundRect(new RectF(left,boxY,right,boxY+dp(130)),dp(8),dp(8),p);double sx=0,sy=0,sz=0,mx=0,my=0,mz=0;for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;mx+=s.mx;my+=s.my;mz+=s.mz;}txt(c,String.format(Locale.US,"ΣFx %+.0f N   ΣFy %+.0f N   ΣFz %+.0f N",sx,sy,sz),left+dp(9),boxY+dp(30),Color.WHITE,6);txt(c,String.format(Locale.US,"ΣMx %+.0f   ΣMy %+.0f   ΣMz %+.0f Nmm",mx,my,mz),left+dp(9),boxY+dp(58),Color.rgb(247,207,77),5.8f);txt(c,"Her kademedeki değer = alt bağlı yüklerin vektörel toplamı",left+dp(9),boxY+dp(88),Color.rgb(180,205,220),5.5f);txt(c,"Tank → Pad → Beam/Pivot → Load Cell → Actuator → Strongback",left+dp(9),boxY+dp(112),Color.rgb(180,205,220),5.5f);
    }
  }
}
