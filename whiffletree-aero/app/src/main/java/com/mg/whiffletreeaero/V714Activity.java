package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import java.util.*;

/** v7.14 — three-candidate whiffletree architecture study. */
public class V714Activity extends V713Activity {
  TextView alternativesSummary;

  static class AltDesign {
    String id,name,objective,status;
    int[] groupOf;
    double score,maxUtil,balance,compact,momentError,layerPenalty,pivotRisk;
    int requiredLayers,overloaded;
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    alternativesSummary=card("DESIGN A / B / C: hesap sonrası üç farklı whiffletree mimarisi karşılaştırılır.",Color.rgb(18,45,59));
    proHome.addView(alternativesSummary,Math.min(8,proHome.getChildCount()),lp());
  }

  @Override void calculateProfessional(){
    super.calculateProfessional();
    if(solvedValid) evaluateAlternatives();
  }

  void evaluateAlternatives(){
    ArrayList<SNode> s=new ArrayList<>(solved);
    Collections.sort(s,(a,b)->Double.compare(a.x,b.x));
    int n=s.size(), k=Math.min(Math.max(1,ival(hActs,1,12)),n), nl=Math.max(1,ival(hLayers,1,4));
    double cap=Math.max(1,val(hCap)), len=Math.max(1,val(hLen)*1000.0);

    AltDesign A=partitionCandidate("A","BALANCED ACTUATOR","Min actuator imbalance + overload",s,k,cap,len,nl,0);
    AltDesign B=partitionCandidate("B","COMPACT GEOMETRY","Min group span + lever complexity",s,k,cap,len,nl,1);
    AltDesign C=partitionCandidate("C","LOAD FIDELITY","Min equivalent root moment mismatch",s,k,cap,len,nl,2);
    AltDesign[] all=new AltDesign[]{A,B,C};
    Arrays.sort(all,(u,v)->Double.compare(v.score,u.score));
    AltDesign best=all[0];

    StringBuilder out=new StringBuilder();
    out.append("WHIFFLETREE DESIGN TRADE STUDY — RECOMMENDED: DESIGN ").append(best.id).append("\n");
    out.append("Her aday aynı hedef pad yüklerini korur; fark actuator partition, fiziksel kompaktlık ve 3D resultant/moment uyumundadır.\n\n");
    for(AltDesign d:all){
      out.append(String.format(Locale.US,
        "DESIGN %s — %s — %s\nScore %.1f/100 | Max actuator %.1f%% | overload %d | layer %d/%d\nBalance %.3f | compact %.3f | moment mismatch %.3f%% | pivot risk %.3f\nObjective: %s\nGroups: %s\n\n",
        d.id,d.name,d.status,d.score,100*d.maxUtil,d.overloaded,d.requiredLayers,nl,d.balance,d.compact,100*d.momentError,d.pivotRisk,d.objective,groupText(d.groupOf,s,k)));
    }
    out.append("SEÇİM MANTIĞI\n");
    out.append("• A: actuator kapasitelerini dengeler; genel amaçlı başlangıç tasarımı.\n");
    out.append("• B: daha kısa/kompakt beam grupları arar; fixture hacmi ve imalat için avantajlı olabilir.\n");
    out.append("• C: tek actuator kökünün grup kuvvet/moment eşdeğerini en iyi koruyan partition'u arar; karma X/Y/Z dağılımlarında değerlidir.\n");
    if(wtDirectionSpread>25)out.append("• Bu yük setinde vektör yön farkı yüksek: tek ortak tree yerine ayrı X/Y/Z tree veya çoklu-axis actuator mimarisi hâlâ öncelikli öneridir.\n");
    alternativesSummary.setText(out.toString());
  }

  AltDesign partitionCandidate(String id,String name,String objective,ArrayList<SNode> s,int k,double cap,double len,int nl,int mode){
    int n=s.size();double INF=1e100;
    double[][] dp=new double[k+1][n+1];int[][] prev=new int[k+1][n+1];
    for(double[] r:dp)Arrays.fill(r,INF);dp[0][0]=0;
    for(int g=1;g<=k;g++){
      for(int j=g;j<=n;j++){
        for(int i=g-1;i<j;i++){
          double c=segmentCost(s,i,j,cap,len,mode);
          if(dp[g-1][i]+c<dp[g][j]){dp[g][j]=dp[g-1][i]+c;prev[g][j]=i;}
        }
      }
    }
    int[] groups=new int[n];int j=n;
    for(int g=k;g>=1;g--){int i=prev[g][j];for(int q=i;q<j;q++)groups[q]=g-1;j=i;}
    AltDesign d=evaluatePartition(id,name,objective,s,groups,k,cap,len,nl);return d;
  }

  double segmentCost(ArrayList<SNode> s,int i,int j,double cap,double len,int mode){
    double fx=0,fy=0,fz=0,mx=0,my=0,mz=0,weight=0,xw=0;
    for(int q=i;q<j;q++){SNode n=s.get(q);fx+=n.fx;fy+=n.fy;fz+=n.fz;mx+=n.mx;my+=n.my;mz+=n.mz;double w=Math.max(1e-9,n.r);weight+=w;xw+=n.x*w;}
    double r=Math.sqrt(fx*fx+fy*fy+fz*fz), over=Math.max(0,r-cap)/cap;
    double span=(j-i<=1)?0:Math.abs(s.get(j-1).x-s.get(i).x)/len;
    double compact=span*span;
    double moment=equivalentMomentError(s,i,j,fx,fy,fz,mx,my,mz,weight,xw,len);
    if(mode==0){
      double target=Math.max(1,Math.sqrt(totalFx(s)*totalFx(s)+totalFy(s)*totalFy(s)+totalFz(s)*totalFz(s))/Math.max(1,ival(hActs,1,12)));
      double bal=(r-target)/target;return bal*bal+25*over*over+0.15*compact+2*moment*moment;
    }
    if(mode==1)return 4*compact+30*over*over+0.15*moment*moment+0.02*(j-i)*(j-i);
    return 12*moment*moment+18*over*over+0.7*compact;
  }

  AltDesign evaluatePartition(String id,String name,String objective,ArrayList<SNode> s,int[] groups,int k,double cap,double len,int nl){
    AltDesign d=new AltDesign();d.id=id;d.name=name;d.objective=objective;d.groupOf=groups.clone();
    double[] fx=new double[k],fy=new double[k],fz=new double[k],minX=new double[k],maxX=new double[k];int[] cnt=new int[k];
    Arrays.fill(minX,Double.POSITIVE_INFINITY);Arrays.fill(maxX,Double.NEGATIVE_INFINITY);
    for(int i=0;i<s.size();i++){int g=groups[i];SNode n=s.get(i);fx[g]+=n.fx;fy[g]+=n.fy;fz[g]+=n.fz;minX[g]=Math.min(minX[g],n.x);maxX[g]=Math.max(maxX[g],n.x);cnt[g]++;}
    double totalR=Math.sqrt(totalFx(s)*totalFx(s)+totalFy(s)*totalFy(s)+totalFz(s)*totalFz(s));double target=totalR/k;
    double bal=0,compact=0,moment=0,pivotRisk=0;int overload=0,maxLeaves=0;double maxUtil=0;
    for(int g=0;g<k;g++){
      double r=Math.sqrt(fx[g]*fx[g]+fy[g]*fy[g]+fz[g]*fz[g]);double u=r/cap;maxUtil=Math.max(maxUtil,u);if(u>1)overload++;
      bal+=Math.pow((r-target)/Math.max(1,target),2);
      if(cnt[g]>1)compact+=Math.pow((maxX[g]-minX[g])/len,2);
      maxLeaves=Math.max(maxLeaves,cnt[g]);
      int first=-1,last=-1;for(int i=0;i<groups.length;i++)if(groups[i]==g){if(first<0)first=i;last=i;}
      if(first>=0){double mx=0,my=0,mz=0,w=0,xw=0;for(int i=first;i<=last;i++){SNode n=s.get(i);mx+=n.mx;my+=n.my;mz+=n.mz;double ww=Math.max(1e-9,n.r);w+=ww;xw+=n.x*ww;}moment+=equivalentMomentError(s,first,last+1,fx[g],fy[g],fz[g],mx,my,mz,w,xw,len);}
      // coarse pivot manufacturability estimate from adjacent resultant imbalance inside each group
      for(int i=0;i<groups.length-1;i++)if(groups[i]==g&&groups[i+1]==g){double a=Math.max(1e-9,s.get(i).r),b=Math.max(1e-9,s.get(i+1).r);double pr=b/(a+b);pivotRisk=Math.max(pivotRisk,Math.max(0,.10-Math.min(pr,1-pr))/.10);}
    }
    int req=0,p=1;while(p<Math.max(1,maxLeaves)){p*=2;req++;}req=Math.max(1,req);
    double layerPenalty=Math.max(0,req-nl);
    d.maxUtil=maxUtil;d.overloaded=overload;d.balance=bal/k;d.compact=compact/k;d.momentError=moment/k;d.pivotRisk=pivotRisk;d.requiredLayers=req;d.layerPenalty=layerPenalty;
    double penalty=2.2*d.balance+2.4*d.compact+8*d.momentError*d.momentError+8*Math.max(0,maxUtil-1)*Math.max(0,maxUtil-1)+2*pivotRisk+3*layerPenalty*layerPenalty;
    d.score=100/(1+penalty);
    d.status=(overload==0&&layerPenalty==0&&d.score>=80)?"GOOD":((overload==0&&d.score>=60)?"REVIEW":"REDESIGN");
    return d;
  }

  double equivalentMomentError(ArrayList<SNode> s,int i,int j,double fx,double fy,double fz,double mx,double my,double mz,double weight,double xw,double len){
    double y=0,z=0;for(int q=i;q<j;q++){y+=s.get(q).y;z+=s.get(q).z;}int c=Math.max(1,j-i);y/=c;z/=c;
    double denom=fz*fz+fy*fy;double x=(weight>0?xw/weight:0);
    if(denom>1e-12){double b1=my-z*fx,b2=mz+y*fx;x=((-fz)*b1+fy*b2)/denom;}
    double pmx=y*fz-z*fy,pmy=z*fx-x*fz,pmz=x*fy-y*fx;
    double er=Math.sqrt((pmx-mx)*(pmx-mx)+(pmy-my)*(pmy-my)+(pmz-mz)*(pmz-mz));
    double ref=Math.max(1,Math.sqrt(mx*mx+my*my+mz*mz)+Math.sqrt(fx*fx+fy*fy+fz*fz)*Math.max(1,len*.05));
    return er/ref;
  }

  double totalFx(ArrayList<SNode> s){double v=0;for(SNode n:s)v+=n.fx;return v;}
  double totalFy(ArrayList<SNode> s){double v=0;for(SNode n:s)v+=n.fy;return v;}
  double totalFz(ArrayList<SNode> s){double v=0;for(SNode n:s)v+=n.fz;return v;}

  String groupText(int[] groups,ArrayList<SNode> s,int k){
    StringBuilder b=new StringBuilder();for(int g=0;g<k;g++){if(g>0)b.append(" | ");b.append("A").append(g+1).append(":");boolean first=true;for(int i=0;i<groups.length;i++)if(groups[i]==g){if(!first)b.append(",");b.append("S").append(s.get(i).section+1);first=false;}}return b.toString();
  }
}
