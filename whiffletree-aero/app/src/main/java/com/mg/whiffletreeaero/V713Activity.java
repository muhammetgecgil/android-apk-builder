package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import java.util.*;

/** v7.13 — engineering topology optimizer on top of unequal-load pivot solver. */
public class V713Activity extends V712Activity {
  TextView optimizerSummary;
  double topologyScore, capacityPenalty, geometryPenalty, balancePenalty;
  String topologyStatus="-";

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    optimizerSummary=card("TOPOLOGY OPTIMIZER: HESAPLA VE GÖSTER sonrası actuator grupları, layer yeterliliği, kapasite ve beam geometrisi puanlanır.",Color.rgb(20,48,62));
    proHome.addView(optimizerSummary,Math.min(7,proHome.getChildCount()),lp());
  }

  @Override void calculateProfessional(){
    super.calculateProfessional();
    if(solvedValid){ optimizeTopology(); designAutomaticWhiffletree(); evaluateDesignedTree(); }
  }

  /** Contiguous dynamic-programming partition: minimizes actuator resultant imbalance and overload. */
  void optimizeTopology(){
    if(solved==null||solved.isEmpty())return;
    int n=solved.size(), k=Math.min(Math.max(1,ival(hActs,1,12)),n);
    double cap=Math.max(1,val(hCap));
    ArrayList<SNode> s=new ArrayList<>(solved);Collections.sort(s,(a,b)->Double.compare(a.x,b.x));
    double[] px=new double[n+1],py=new double[n+1],pz=new double[n+1];
    for(int i=0;i<n;i++){px[i+1]=px[i]+s.get(i).fx;py[i+1]=py[i]+s.get(i).fy;pz[i+1]=pz[i]+s.get(i).fz;}
    double totalR=Math.sqrt(px[n]*px[n]+py[n]*py[n]+pz[n]*pz[n]);double target=totalR/k;
    double INF=1e100;double[][] dp=new double[k+1][n+1];int[][] prev=new int[k+1][n+1];
    for(double[] row:dp)Arrays.fill(row,INF);dp[0][0]=0;
    for(int g=1;g<=k;g++)for(int j=g;j<=n;j++)for(int i=g-1;i<j;i++){
      double fx=px[j]-px[i],fy=py[j]-py[i],fz=pz[j]-pz[i],r=Math.sqrt(fx*fx+fy*fy+fz*fz);
      double imbalance=(r-target)*(r-target)/Math.max(1,target*target);
      double over=Math.max(0,r-cap)/cap;double cost=dp[g-1][i]+imbalance+25*over*over;
      if(cost<dp[g][j]){dp[g][j]=cost;prev[g][j]=i;}
    }
    int[] start=new int[k],end=new int[k];int j=n;for(int g=k;g>=1;g--){int i=prev[g][j];start[g-1]=i;end[g-1]=j;j=i;}
    for(int g=0;g<k;g++)for(int i=start[g];i<end[g];i++)s.get(i).act=g;
    // If user requested more actuators than stations, remaining actuators stay intentionally unused.
    balancePenalty=dp[k][n];
  }

  void evaluateDesignedTree(){
    int nl=Math.max(1,ival(hLayers,1,4));double cap=Math.max(1,val(hCap));
    capacityPenalty=0;geometryPenalty=0;double maxUtil=0,worstPivot=0;int overloaded=0,badGeometry=0;
    for(WtRoot r:wtRoots){double u=r.r/cap;maxUtil=Math.max(maxUtil,u);if(u>1){capacityPenalty+=(u-1)*(u-1);overloaded++;}}
    for(WtBeam b:wtBeams){double minArm=Math.min(b.leftArm,b.rightArm),span=b.leftArm+b.rightArm;double edge=span>0?minArm/span:0;worstPivot=Math.max(worstPivot,Math.abs(.5-b.pivotRatio));if(minArm<20||edge<.08){geometryPenalty+=1+Math.max(0,(20-minArm)/20);badGeometry++;}}
    int requiredLayers=1;int maxLeavesPerAct=0;for(WtRoot r:wtRoots){int c=0;for(WtLeaf l:wtLeaves)if(l.act==r.act)c++;maxLeavesPerAct=Math.max(maxLeavesPerAct,c);}while((1<<requiredLayers)<Math.max(1,maxLeavesPerAct))requiredLayers++;
    double layerPenalty=Math.max(0,requiredLayers-nl);geometryPenalty+=4*layerPenalty*layerPenalty;
    double dirPenalty=wtDirectionSpread<=8?0:(wtDirectionSpread<=25?Math.pow((wtDirectionSpread-8)/17.0,2):4+Math.pow((wtDirectionSpread-25)/20.0,2));
    topologyScore=100.0/(1.0+balancePenalty+12*capacityPenalty+3*geometryPenalty+5*dirPenalty);
    topologyStatus=topologyScore>=85?"GOOD":(topologyScore>=65?"USABLE / REVIEW":"REDESIGN RECOMMENDED");
    optimizerSummary.setText(String.format(Locale.US,
      "AUTO TOPOLOGY OPTIMIZER — %s\nScore %.1f / 100\nActuator partition penalty %.4f • capacity penalty %.4f • geometry/layer penalty %.4f\nMax actuator utilization %.1f%% • overloaded actuator %d\nRequired binary depth ≈ %d layer • available %d layer\nWorst pivot eccentricity %.1f%% • geometry warnings %d\nArchitecture: %s\n\nOptimizer objective: target pad loads are preserved while actuator groups, pivot ratios and available layer depth are chosen/checked against physical feasibility. Low score means add actuator/layer, increase capacity, or separate X/Y/Z load trees.",
      topologyStatus,topologyScore,balancePenalty,capacityPenalty,geometryPenalty,100*maxUtil,overloaded,requiredLayers,nl,100*worstPivot,badGeometry,wtArchitecture));
  }
}
