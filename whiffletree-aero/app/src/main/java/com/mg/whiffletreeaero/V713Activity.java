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
    if(solvedValid){ optimizeTopology(); rebuildOptimizedTree(); evaluateDesignedTree(); }
  }

  /** Contiguous dynamic-programming partition: minimizes actuator resultant imbalance and overload. */
  void optimizeTopology(){
    if(solved==null||solved.isEmpty())return;
    int n=solved.size(), k=Math.min(Math.max(1,ival(hActs,1,12)),n);
    double cap=Math.max(1,val(hCap));
    ArrayList<SNode> s=new ArrayList<>(solved);Collections.sort(s,(a,b)->Double.compare(a.x,b.x));
    double[] px=new double[n+1],py=new double[n+1],pz=new double[n+1];
    for(int i=0;i<n;i++){px[i+1]=px[i]+s.get(i).fx;py[i+1]=py[i]+s.get(i).fy;pz[i+1]=pz[i]+s.get(i).fz;}
    double totalR=Math.sqrt(px[n]*px[n]+py[n]*py[n]+pz[n]*pz[n]);double target=Math.max(1,totalR/k);
    double INF=1e100;double[][] dp=new double[k+1][n+1];int[][] prev=new int[k+1][n+1];
    for(double[] row:dp)Arrays.fill(row,INF);dp[0][0]=0;
    for(int g=1;g<=k;g++)for(int j=g;j<=n;j++)for(int i=g-1;i<j;i++){
      if(dp[g-1][i]>=INF/2)continue;
      double fx=px[j]-px[i],fy=py[j]-py[i],fz=pz[j]-pz[i],r=Math.sqrt(fx*fx+fy*fy+fz*fz);
      double imbalance=(r-target)*(r-target)/(target*target);
      double over=Math.max(0,r-cap)/cap;
      double countPenalty=.03*Math.pow((j-i)-(double)n/k,2);
      double cost=dp[g-1][i]+imbalance+25*over*over+countPenalty;
      if(cost<dp[g][j]){dp[g][j]=cost;prev[g][j]=i;}
    }
    int[] start=new int[k],end=new int[k];int j=n;
    for(int g=k;g>=1;g--){int i=prev[g][j];start[g-1]=i;end[g-1]=j;j=i;}
    for(int g=0;g<k;g++)for(int i=start[g];i<end[g];i++)s.get(i).act=g;
    balancePenalty=dp[k][n];
  }

  /** Rebuild unequal-load tree using optimized station→actuator assignment, without re-seeding equal groups. */
  void rebuildOptimizedTree(){
    wtBeams.clear();wtLeaves.clear();wtRoots.clear();wtForceResidual=0;wtMomentResidual=0;
    if(solved==null||solved.isEmpty())return;
    int na=Math.max(1,ival(hActs,1,12)),maxLayers=Math.max(1,ival(hLayers,1,4));

    double sx=0,sy=0,sz=0;for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;}double sr=Math.sqrt(sx*sx+sy*sy+sz*sz);
    double ux=sr>1e-9?sx/sr:0,uy=sr>1e-9?sy/sr:0,uz=sr>1e-9?sz/sr:1,maxAng=0;
    for(SNode s:solved){if(s.r<1e-9)continue;double dot=(s.fx*ux+s.fy*uy+s.fz*uz)/s.r;dot=Math.max(-1,Math.min(1,dot));maxAng=Math.max(maxAng,Math.toDegrees(Math.acos(dot)));}
    wtDirectionSpread=maxAng;
    wtArchitecture=maxAng<=8?"COMMON 3D RESULTANT TREE":(maxAng<=25?"MULTI-AXIS TREE / CHECK GEOMETRY":"SEPARATE X/Y/Z LOAD TREES RECOMMENDED");

    for(SNode s:solved){WtLeaf l=new WtLeaf();l.station=s.section;l.act=s.act;l.x=s.x;l.fx=s.fx;l.fy=s.fy;l.fz=s.fz;l.r=s.r;l.id="P"+(s.section+1);wtLeaves.add(l);}
    Collections.sort(wtLeaves,(a,b)->Double.compare(a.x,b.x));

    for(int a=0;a<na;a++){
      ArrayList<WtNode> nodes=new ArrayList<>();for(WtLeaf l:wtLeaves)if(l.act==a)nodes.add(new WtNode(l.id,0,a,l.x,l.fx,l.fy,l.fz));
      Collections.sort(nodes,(u,v)->Double.compare(u.x,v.x));
      if(nodes.isEmpty()){WtRoot r=new WtRoot();r.act=a;r.id="ACT"+(a+1);r.x=0;wtRoots.add(r);continue;}
      int level=1,bn=1;
      while(nodes.size()>1 && level<=maxLayers){
        ArrayList<WtNode> next=new ArrayList<>();
        for(int i=0;i<nodes.size();i+=2){
          if(i+1>=nodes.size()){next.add(nodes.get(i));continue;}
          WtNode L=nodes.get(i),R=nodes.get(i+1);double fl=Math.max(1e-9,L.r),fr=Math.max(1e-9,R.r),span=Math.max(40,Math.abs(R.x-L.x));
          double leftArm=span*fr/(fl+fr),rightArm=span*fl/(fl+fr),pivotX=L.x+leftArm;
          WtBeam b=new WtBeam();b.id="A"+(a+1)+"-L"+level+"-B"+(bn++);b.leftId=L.id;b.rightId=R.id;b.level=level;b.act=a;b.leftX=L.x;b.rightX=R.x;b.x=pivotX;b.fl=L.r;b.fr=R.r;b.total=L.r+R.r;b.leftArm=leftArm;b.rightArm=rightArm;b.pivotRatio=leftArm/span;b.momentResidual=fl*leftArm-fr*rightArm;b.fx=L.fx+R.fx;b.fy=L.fy+R.fy;b.fz=L.fz+R.fz;wtBeams.add(b);wtMomentResidual=Math.max(wtMomentResidual,Math.abs(b.momentResidual));
          next.add(new WtNode(b.id,level,a,pivotX,b.fx,b.fy,b.fz));
        }
        nodes=next;level++;
      }
      if(nodes.size()>1)wtArchitecture+=" • LAYER LIMIT INSUFFICIENT";
      double rfx=0,rfy=0,rfz=0,rx=0,rw=0;for(WtNode n:nodes){rfx+=n.fx;rfy+=n.fy;rfz+=n.fz;double w=Math.max(1e-9,n.r);rx+=n.x*w;rw+=w;}
      WtRoot rt=new WtRoot();rt.act=a;rt.id="ACT"+(a+1);rt.fx=rfx;rt.fy=rfy;rt.fz=rfz;rt.r=Math.sqrt(rfx*rfx+rfy*rfy+rfz*rfz);rt.x=rw>0?rx/rw:0;wtRoots.add(rt);
    }

    double tx=0,ty=0,tz=0,ax=0,ay=0,az=0;for(SNode s:solved){tx+=s.fx;ty+=s.fy;tz+=s.fz;}for(WtRoot r:wtRoots){ax+=r.fx;ay+=r.fy;az+=r.fz;}
    wtForceResidual=Math.sqrt((tx-ax)*(tx-ax)+(ty-ay)*(ty-ay)+(tz-az)*(tz-az));

    StringBuilder sb=new StringBuilder("AUTO WHIFFLETREE DESIGN — ").append(wtArchitecture).append("\n");
    sb.append(String.format(Locale.US,"%d pad • %d actuator root • %d beam/pivot • max %d layer\n",wtLeaves.size(),wtRoots.size(),wtBeams.size(),maxLayers));
    sb.append(String.format(Locale.US,"Direction spread %.1f° • force closure %.6f N • worst ΣM residual %.6f Nmm\n",wtDirectionSpread,wtForceResidual,wtMomentResidual));
    sb.append("Actuator grouping is optimized; pivot is solved from FL·a = FR·b, not fixed at beam center.\n");
    int show=Math.min(8,wtBeams.size());for(int i=0;i<show;i++){WtBeam b=wtBeams.get(i);sb.append(String.format(Locale.US,"%s: %.0f/%.0f N  a=%.1f  b=%.1f mm  pivot=%.1f%%\n",b.id,b.fl,b.fr,b.leftArm,b.rightArm,100*b.pivotRatio));}
    autoWtSummary.setText(sb.toString());if(autoWtView!=null)autoWtView.invalidate();
  }

  void evaluateDesignedTree(){
    int nl=Math.max(1,ival(hLayers,1,4));double cap=Math.max(1,val(hCap));
    capacityPenalty=0;geometryPenalty=0;double maxUtil=0,worstPivot=0;int overloaded=0,badGeometry=0;
    for(WtRoot r:wtRoots){double u=r.r/cap;maxUtil=Math.max(maxUtil,u);if(u>1){capacityPenalty+=(u-1)*(u-1);overloaded++;}}
    for(WtBeam b:wtBeams){double minArm=Math.min(b.leftArm,b.rightArm),span=b.leftArm+b.rightArm;double edge=span>0?minArm/span:0;worstPivot=Math.max(worstPivot,Math.abs(.5-b.pivotRatio));if(minArm<20||edge<.08){geometryPenalty+=1+Math.max(0,(20-minArm)/20);badGeometry++;}}
    int requiredLayers=1,maxLeavesPerAct=0;for(WtRoot r:wtRoots){int c=0;for(WtLeaf l:wtLeaves)if(l.act==r.act)c++;maxLeavesPerAct=Math.max(maxLeavesPerAct,c);}while((1<<requiredLayers)<Math.max(1,maxLeavesPerAct))requiredLayers++;
    double layerPenalty=Math.max(0,requiredLayers-nl);geometryPenalty+=4*layerPenalty*layerPenalty;
    double dirPenalty=wtDirectionSpread<=8?0:(wtDirectionSpread<=25?Math.pow((wtDirectionSpread-8)/17.0,2):4+Math.pow((wtDirectionSpread-25)/20.0,2));
    topologyScore=100.0/(1.0+balancePenalty+12*capacityPenalty+3*geometryPenalty+5*dirPenalty);
    topologyStatus=topologyScore>=85?"GOOD":(topologyScore>=65?"USABLE / REVIEW":"REDESIGN RECOMMENDED");
    optimizerSummary.setText(String.format(Locale.US,
      "AUTO TOPOLOGY OPTIMIZER — %s\nScore %.1f / 100\nActuator grouping penalty %.4f • capacity penalty %.4f • geometry/layer penalty %.4f\nMax actuator utilization %.1f%% • overloaded actuator %d\nRequired binary depth ≈ %d layer • available %d layer\nWorst pivot eccentricity %.1f%% • geometry warnings %d\nArchitecture: %s\n\nLow score ⇒ add actuator/layer, increase actuator capacity, move load pads, or use separate X/Y/Z trees.",
      topologyStatus,topologyScore,balancePenalty,capacityPenalty,geometryPenalty,100*maxUtil,overloaded,requiredLayers,nl,100*worstPivot,badGeometry,wtArchitecture));
  }
}
