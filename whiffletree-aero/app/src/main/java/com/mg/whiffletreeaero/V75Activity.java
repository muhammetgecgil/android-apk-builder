package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V75Activity extends V74Activity {
  LinearLayout matrixPanel;
  TextView matrixSummary, leverSummary, errorSummary;
  MatrixProofView matrixView;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    matrixPanel=new LinearLayout(this);matrixPanel.setOrientation(LinearLayout.VERTICAL);matrixPanel.setPadding(dp(10),dp(10),dp(10),dp(10));matrixPanel.setBackground(bg(Color.rgb(2,15,25),16));
    matrixPanel.addView(tx("TRANSFER MATRIX + TRUE LEVER-ARM SOLVER",20,true,Color.WHITE));
    matrixPanel.addView(tx("Section loads → actual pad/load path → whiffletree beam equilibrium → required/applied shear & moment error map",9,false,Color.rgb(180,210,230)));
    matrixSummary=card("HESAPLA VE GÖSTER sonrası transfer matrix ve load reconstruction burada oluşur.",Color.rgb(14,45,64));matrixPanel.addView(matrixSummary,lp());
    leverSummary=card("Beam lever-arm equilibrium hesap sonrası görünür.",Color.rgb(13,39,54));matrixPanel.addView(leverSummary,lp());
    matrixView=new MatrixProofView();matrixPanel.addView(matrixView,new LinearLayout.LayoutParams(-1,dp(1700)));
    errorSummary=card("Required / applied error map hesap sonrası görünür.",Color.rgb(13,39,54));matrixPanel.addView(errorSummary,lp());
    root.addView(matrixPanel,5,lp());
  }

  @Override void runGuided(){super.runGuided();refreshMatrixSolver();}
  @Override void runPrimary(){super.runPrimary();refreshMatrixSolver();}
  @Override void calculateAndShow(){super.calculateAndShow();refreshMatrixSolver();}

  void refreshMatrixSolver(){
    if(matrixSummary==null)return;
    if(!solvedValid||solved==null||solved.isEmpty()){
      matrixSummary.setText("Doğrulama hazır değil — önce HESAPLA VE GÖSTER.");leverSummary.setText("Henüz beam dengesi yok.");errorSummary.setText("Henüz error map yok.");matrixView.invalidate();return;
    }
    int n=solved.size(),nl=Math.max(1,qi(qLayers,1,4));
    double[][] T=buildTransferMatrix();
    double maxRowErr=0;for(int i=0;i<n;i++){double s=0;for(int j=0;j<n;j++)s+=T[i][j];maxRowErr=Math.max(maxRowErr,Math.abs(1-s));}
    matrixSummary.setText(String.format(Locale.US,"TRANSFER MATRIX READY\n%d section • %d×%d influence matrix • row-sum residual %.6f\nMatrix, section resultant loadlarını actual application / grouping noktalarına taşımak için normalized influence weights kullanır.\nFinal release için gerçek hardpoint koordinatları ile project-specific transfer matrix girilmelidir.",n,n,n,maxRowErr));

    StringBuilder ls=new StringBuilder("TRUE LEVER-ARM BEAM CHECK\n");
    ArrayList<double[]> beams=buildLeverBeams();double worst=0;int k=1;
    for(double[] b:beams){double fl=b[0],fr=b[1],a=b[2],bb=b[3],rp=b[4],mr=b[5];worst=Math.max(worst,Math.abs(mr));ls.append(String.format(Locale.US,"B%02d  FL %.0f N  FR %.0f N  a %.1f mm  b %.1f mm  Rp %.0f N  ΣMres %.3f Nmm\n",k++,fl,fr,a,bb,rp,mr));}
    ls.append(String.format(Locale.US,"Worst beam moment residual %.6f Nmm → %s",worst,worst<1e-3?"PASS":"CHECK"));leverSummary.setText(ls.toString());

    double[][] req=requiredCurves(),app=appliedCurves(T);double maxShearErr=0,maxMomentErr=0;int worstS=0,worstM=0;
    for(int i=0;i<n;i++){
      double es=Math.abs(app[0][i]-req[0][i])/Math.max(1,Math.abs(req[0][i]));double em=Math.abs(app[1][i]-req[1][i])/Math.max(1,Math.abs(req[1][i]));
      if(es>maxShearErr){maxShearErr=es;worstS=i;}if(em>maxMomentErr){maxMomentErr=em;worstM=i;}
    }
    errorSummary.setText(String.format(Locale.US,"REQUIRED vs APPLIED ERROR MAP\nMax shear error %.3f%% @ S%d\nMax moment error %.3f%% @ S%d\nTarget for preliminary reconstruction: minimize both errors while satisfying actuator capacity, stroke and joint geometry.\nGreen <2%% | Yellow 2–5%% | Red >5%%",100*maxShearErr,worstS+1,100*maxMomentErr,worstM+1));matrixView.invalidate();
  }

  double[][] buildTransferMatrix(){
    int n=solved.size();double[][] T=new double[n][n];
    double sigma=Math.max(1,qd(qLength)*1000.0/Math.max(2,n));
    for(int i=0;i<n;i++){
      double sum=0;for(int j=0;j<n;j++){double dx=solved.get(i).x-solved.get(j).x;double w=Math.exp(-0.5*(dx*dx)/(sigma*sigma));T[i][j]=w;sum+=w;}
      for(int j=0;j<n;j++)T[i][j]/=Math.max(1e-12,sum);
    }return T;
  }

  ArrayList<double[]> buildLeverBeams(){
    ArrayList<double[]> out=new ArrayList<>();ArrayList<Double> q=new ArrayList<>();for(SNode s:solved)q.add(s.r);
    double baseArm=Math.max(50,qd(qLength)*1000.0/Math.max(2,solved.size()));
    while(q.size()>1){ArrayList<Double> next=new ArrayList<>();for(int i=0;i<q.size();i+=2){double fl=q.get(i),fr=(i+1<q.size()?q.get(i+1):0);double a=baseArm,b=baseArm;double rp=fl+fr;double pivot=(rp==0?0:(fr*b-fl*a)/Math.max(1e-9,rp));a=Math.max(10,a+pivot);b=Math.max(10,b-pivot);double mr=fl*a-fr*b;out.add(new double[]{fl,fr,a,b,rp,mr});next.add(rp);}q=next;baseArm*=1.6;if(out.size()>30)break;}return out;
  }

  double[][] requiredCurves(){
    int n=solved.size();double[] shear=new double[n],moment=new double[n];double q=0,m=0;double dx=Math.max(1,qd(qLength)*1000.0/Math.max(1,n-1));
    for(int i=n-1;i>=0;i--){q+=solved.get(i).fz;shear[i]=q;if(i<n-1)m+=shear[i+1]*dx;moment[i]=m;}return new double[][]{shear,moment};
  }

  double[][] appliedCurves(double[][] T){
    int n=solved.size();double[] f=new double[n];for(int i=0;i<n;i++){double v=0;for(int j=0;j<n;j++)v+=T[i][j]*solved.get(j).fz;f[i]=v;}
    double[] shear=new double[n],moment=new double[n];double q=0,m=0;double dx=Math.max(1,qd(qLength)*1000.0/Math.max(1,n-1));
    for(int i=n-1;i>=0;i--){q+=f[i];shear[i]=q;if(i<n-1)m+=shear[i+1]*dx;moment[i]=m;}return new double[][]{shear,moment};
  }

  class MatrixProofView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    MatrixProofView(){super(V75Activity.this);setBackgroundColor(Color.rgb(1,9,16));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    float px(float v){return v*getResources().getDisplayMetrics().density;}
    void text(Canvas c,String s,float x,float y,int col,float size){t.setColor(col);t.setTextSize(px(size));c.drawText(s,x,y,t);}void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth(px(w));c.drawLine(x1,y1,x2,y2,p);}void box(Canvas c,float l,float top,float r,float b,int col){p.setColor(col);c.drawRoundRect(new RectF(l,top,r,b),px(8),px(8),p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float L=px(14),R=getWidth()-px(14);text(c,"MATRIX / LEVER-ARM / ERROR PROOF",L,px(28),Color.WHITE,13);if(!solvedValid||solved==null||solved.isEmpty()){text(c,"Önce HESAPLA VE GÖSTER",L,px(70),Color.LTGRAY,8);return;}
      int n=solved.size();double[][] T=buildTransferMatrix();float y=px(65);
      box(c,L,y,R,y+px(330),Color.rgb(15,39,52));text(c,"1 — NORMALIZED TRANSFER MATRIX",L+px(10),y+px(24),Color.rgb(115,230,140),8);int show=Math.min(8,n);float cell=(R-L-px(45))/Math.max(1,show);for(int i=0;i<show;i++)for(int j=0;j<show;j++){double v=T[i][j];int col=Color.rgb((int)(30+190*v),(int)(55+120*v),(int)(85+80*v));p.setColor(col);float x=L+px(35)+j*cell,yy=y+px(55)+i*cell;c.drawRect(x,yy,x+cell-1,yy+cell-1,p);}for(int i=0;i<show;i++){text(c,"S"+(i+1),L+px(5),y+px(70)+i*cell,Color.WHITE,4.4f);text(c,"S"+(i+1),L+px(36)+i*cell,y+px(50),Color.WHITE,4.4f);}text(c,"Darker / brighter cell = larger influence weight",L+px(10),y+px(312),Color.rgb(190,210,225),5.3f);
      y+=px(355);
      box(c,L,y,R,y+px(430),Color.rgb(15,39,52));text(c,"2 — TRUE LEVER-ARM WHIFFLETREE CHECK",L+px(10),y+px(24),Color.rgb(247,190,70),8);ArrayList<double[]> beams=buildLeverBeams();float yy=y+px(65);int shown=Math.min(10,beams.size());for(int i=0;i<shown;i++){double[] b=beams.get(i);float cx=(L+R)/2;float span=(R-L)*0.34f;line(c,cx-span,yy,cx+span,yy,Color.rgb(220,145,32),5);p.setColor(Color.LTGRAY);c.drawCircle(cx,yy,px(5),p);text(c,String.format(Locale.US,"B%02d  FL %.0fN  a %.0fmm",i+1,b[0],b[2]),L+px(8),yy-px(9),Color.WHITE,4.8f);text(c,String.format(Locale.US,"FR %.0fN  b %.0fmm  ΣM %.2f",b[1],b[3],b[5]),cx+px(8),yy+px(18),Color.WHITE,4.8f);yy+=px(34);}text(c,"Each beam solved from force balance + pivot moment balance",L+px(10),y+px(410),Color.rgb(190,210,225),5.3f);
      y+=px(455);
      box(c,L,y,R,y+px(430),Color.rgb(15,39,52));text(c,"3 — REQUIRED vs APPLIED SHEAR / MOMENT",L+px(10),y+px(24),Color.rgb(115,230,140),8);double[][] req=requiredCurves(),app=appliedCurves(T);drawCurve(c,L+px(35),R-px(18),y+px(65),y+px(185),req[0],app[0],"SHEAR");drawCurve(c,L+px(35),R-px(18),y+px(235),y+px(355),req[1],app[1],"MOMENT");text(c,"Blue = required   White = applied reconstruction",L+px(10),y+px(404),Color.rgb(190,210,225),5.3f);
      y+=px(455);
      box(c,L,y,R,y+px(260),Color.rgb(15,39,52));text(c,"4 — STATION ERROR MAP",L+px(10),y+px(24),Color.rgb(247,207,77),8);float left=L+px(25),right=R-px(20);for(int i=0;i<n;i++){double es=Math.abs(app[0][i]-req[0][i])/Math.max(1,Math.abs(req[0][i]));double em=Math.abs(app[1][i]-req[1][i])/Math.max(1,Math.abs(req[1][i]));double e=Math.max(es,em);int col=e<.02?Color.rgb(75,190,95):(e<.05?Color.rgb(235,185,55):Color.rgb(225,75,70));float x=left+(right-left)*(i+.5f)/n;p.setColor(col);c.drawRect(x-px(8),y+px(70),x+px(8),y+px(135),p);text(c,"S"+(i+1),x-px(6),y+px(157),Color.WHITE,4.4f);text(c,String.format(Locale.US,"%.1f%%",100*e),x-px(9),y+px(177),Color.WHITE,4.3f);}text(c,"Green <2% | Yellow 2–5% | Red >5%",L+px(10),y+px(225),Color.rgb(190,210,225),5.4f);
    }
    void drawCurve(Canvas c,float l,float r,float top,float bot,double[] a,double[] b,String title){text(c,title,l,top-px(10),Color.WHITE,5.5f);line(c,l,(top+bot)/2,r,(top+bot)/2,Color.GRAY,1);double max=1;for(double v:a)max=Math.max(max,Math.abs(v));for(double v:b)max=Math.max(max,Math.abs(v));float px0=0,py0=0,qx0=0,qy0=0;for(int i=0;i<a.length;i++){float x=l+(r-l)*(i/(float)Math.max(1,a.length-1));float ya=(float)((top+bot)/2-(bot-top)*.42*a[i]/max);float yb=(float)((top+bot)/2-(bot-top)*.42*b[i]/max);if(i>0){line(c,px0,py0,x,ya,Color.rgb(80,150,245),2.3f);line(c,qx0,qy0,x,yb,Color.WHITE,1.8f);}px0=x;py0=ya;qx0=x;qy0=yb;}}
  }
}
