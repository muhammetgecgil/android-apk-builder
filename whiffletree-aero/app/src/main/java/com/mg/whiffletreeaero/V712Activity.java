package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** v7.12 — automatic unequal-load whiffletree topology + pivot solver. */
public class V712Activity extends V711Activity {
  TextView autoWtSummary;
  AutoWtView autoWtView;
  ArrayList<WtBeam> wtBeams=new ArrayList<>();
  ArrayList<WtLeaf> wtLeaves=new ArrayList<>();
  ArrayList<WtRoot> wtRoots=new ArrayList<>();
  double wtForceResidual=0, wtMomentResidual=0, wtDirectionSpread=0;
  String wtArchitecture="-";
  int[] forcedActuatorGroups=null;

  static class WtLeaf { int station,act; double x,fx,fy,fz,r; String id; }
  static class WtRoot { int act; double x,fx,fy,fz,r; String id; }
  static class WtBeam {
    String id,leftId,rightId; int level,act; double x,leftX,rightX;
    double fl,fr,total,leftArm,rightArm,pivotRatio,momentResidual;
    double fx,fy,fz;
  }
  static class WtNode {
    String id; int level,act; double x,fx,fy,fz,r;
    WtNode(String i,int l,int a,double xx,double xF,double yF,double zF){id=i;level=l;act=a;x=xx;fx=xF;fy=yF;fz=zF;r=Math.sqrt(fx*fx+fy*fy+fz*fz);}
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    autoWtSummary=card("AUTO WHIFFLETREE DESIGN: hesap sonrası hedef station yüklerinden topoloji ve pivot oranları otomatik oluşturulur.",Color.rgb(21,55,64));
    proHome.addView(autoWtSummary,Math.min(5,proHome.getChildCount()),lp());
    autoWtView=new AutoWtView();
    proHome.addView(autoWtView,Math.min(6,proHome.getChildCount()),new LinearLayout.LayoutParams(-1,dp(720)));
  }

  @Override void calculateProfessional(){
    forcedActuatorGroups=null;
    super.calculateProfessional();
    if(solvedValid) designAutomaticWhiffletree();
  }

  void designAutomaticWhiffletree(){
    wtBeams.clear();wtLeaves.clear();wtRoots.clear();wtForceResidual=0;wtMomentResidual=0;
    if(solved==null||solved.isEmpty()){autoWtSummary.setText("AUTO WHIFFLETREE: çözüm için önce yük hesabı gerekli.");return;}
    int na=Math.max(1,ival(hActs,1,12));int maxLayers=Math.max(1,ival(hLayers,1,4));

    double sx=0,sy=0,sz=0;for(SNode s:solved){sx+=s.fx;sy+=s.fy;sz+=s.fz;}double sr=Math.sqrt(sx*sx+sy*sy+sz*sz);
    double ux=sr>1e-9?sx/sr:0,uy=sr>1e-9?sy/sr:0,uz=sr>1e-9?sz/sr:1;
    double maxAng=0;
    for(SNode s:solved){if(s.r<1e-9)continue;double dot=(s.fx*ux+s.fy*uy+s.fz*uz)/s.r;dot=Math.max(-1,Math.min(1,dot));maxAng=Math.max(maxAng,Math.toDegrees(Math.acos(dot)));}
    wtDirectionSpread=maxAng;
    wtArchitecture=maxAng<=8?"COMMON 3D RESULTANT TREE":(maxAng<=25?"MULTI-AXIS TREE / CHECK GEOMETRY":"SEPARATE X/Y/Z LOAD TREES RECOMMENDED");

    ArrayList<SNode> ss=new ArrayList<>(solved);Collections.sort(ss,(a,b)->Double.compare(a.x,b.x));
    boolean useForced=forcedActuatorGroups!=null && forcedActuatorGroups.length==ss.size();
    if(useForced){
      for(int i=0;i<ss.size();i++)ss.get(i).act=Math.max(0,Math.min(na-1,forcedActuatorGroups[i]));
    }else{
      double totalWeight=0;for(SNode s:ss)totalWeight+=Math.max(1e-9,s.r);double target=totalWeight/na,cum=0;int ai=0;
      for(int i=0;i<ss.size();i++){SNode s=ss.get(i);if(ai<na-1 && cum>0 && cum+s.r>target*(ai+1))ai++;s.act=ai;cum+=Math.max(1e-9,s.r);}
    }
    for(SNode s:ss){WtLeaf lf=new WtLeaf();lf.station=s.section;lf.act=s.act;lf.x=s.x;lf.fx=s.fx;lf.fy=s.fy;lf.fz=s.fz;lf.r=s.r;lf.id="P"+(s.section+1);wtLeaves.add(lf);}

    for(int a=0;a<na;a++){
      ArrayList<WtNode> nodes=new ArrayList<>();for(WtLeaf l:wtLeaves)if(l.act==a)nodes.add(new WtNode(l.id,0,a,l.x,l.fx,l.fy,l.fz));
      if(nodes.isEmpty()){WtRoot r=new WtRoot();r.act=a;r.id="ACT"+(a+1);r.x=0;wtRoots.add(r);continue;}
      Collections.sort(nodes,(u,v)->Double.compare(u.x,v.x));int level=1,bn=1;
      while(nodes.size()>1 && level<=maxLayers){
        ArrayList<WtNode> next=new ArrayList<>();
        for(int i=0;i<nodes.size();i+=2){
          if(i+1>=nodes.size()){next.add(nodes.get(i));continue;}
          WtNode L=nodes.get(i),R=nodes.get(i+1);double fl=Math.max(1e-9,L.r),fr=Math.max(1e-9,R.r),span=Math.max(40,Math.abs(R.x-L.x));
          double leftArm=span*fr/(fl+fr),rightArm=span*fl/(fl+fr);double pivotX=L.x+leftArm;
          WtBeam b=new WtBeam();b.id="A"+(a+1)+"-L"+level+"-B"+(bn++);b.leftId=L.id;b.rightId=R.id;b.level=level;b.act=a;b.leftX=L.x;b.rightX=R.x;b.x=pivotX;b.fl=L.r;b.fr=R.r;b.total=L.r+R.r;b.leftArm=leftArm;b.rightArm=rightArm;b.pivotRatio=leftArm/span;b.momentResidual=fl*leftArm-fr*rightArm;b.fx=L.fx+R.fx;b.fy=L.fy+R.fy;b.fz=L.fz+R.fz;wtBeams.add(b);wtMomentResidual=Math.max(wtMomentResidual,Math.abs(b.momentResidual));
          next.add(new WtNode(b.id,level,a,pivotX,b.fx,b.fy,b.fz));
        }
        nodes=next;level++;
      }
      if(nodes.size()>1)wtArchitecture += " • LAYER LIMIT INSUFFICIENT";
      double rfx=0,rfy=0,rfz=0,rx=0,rrw=0;for(WtNode n:nodes){rfx+=n.fx;rfy+=n.fy;rfz+=n.fz;rx+=n.x*Math.max(1e-9,n.r);rrw+=Math.max(1e-9,n.r);}WtRoot rt=new WtRoot();rt.act=a;rt.id="ACT"+(a+1);rt.fx=rfx;rt.fy=rfy;rt.fz=rfz;rt.r=Math.sqrt(rfx*rfx+rfy*rfy+rfz*rfz);rt.x=rrw>0?rx/rrw:0;wtRoots.add(rt);
    }

    double tx=0,ty=0,tz=0,ax=0,ay=0,az=0;for(SNode s:solved){tx+=s.fx;ty+=s.fy;tz+=s.fz;}for(WtRoot r:wtRoots){ax+=r.fx;ay+=r.fy;az+=r.fz;}
    wtForceResidual=Math.sqrt((tx-ax)*(tx-ax)+(ty-ay)*(ty-ay)+(tz-az)*(tz-az));

    StringBuilder sb=new StringBuilder();
    sb.append("AUTO WHIFFLETREE DESIGN — ").append(wtArchitecture).append("\n");
    if(useForced)sb.append("Optimizer-selected actuator partition ACTIVE\n");
    sb.append(String.format(Locale.US,"%d pad • %d actuator subtree • %d calculated beam/pivot • max %d layer\n",wtLeaves.size(),wtRoots.size(),wtBeams.size(),maxLayers));
    sb.append(String.format(Locale.US,"Vector direction spread %.1f° • force closure %.6f N • worst beam ΣM residual %.6f Nmm\n",wtDirectionSpread,wtForceResidual,wtMomentResidual));
    sb.append("Pivotlar orta noktaya sabitlenmez: her beam için FL·a = FR·b denklemiyle otomatik yerleştirilir.\n");
    int show=Math.min(8,wtBeams.size());for(int i=0;i<show;i++){WtBeam b=wtBeams.get(i);sb.append(String.format(Locale.US,"%s: %.0f / %.0f N → a %.1f mm, b %.1f mm, pivot %.1f%%\n",b.id,b.fl,b.fr,b.leftArm,b.rightArm,100*b.pivotRatio));}
    if(wtBeams.size()>show)sb.append("… diğer beam/pivot sonuçları 2D tasarım görünümünde.\n");
    if(wtDirectionSpread>25)sb.append("UYARI: station yük vektörleri ortak doğrultuda değil; tek mekanik ağaç X/Y/Z dağılımlarını bağımsız olarak sağlayamaz. Ayrı eksen ağaçları / actuator setleri önerildi.");
    autoWtSummary.setText(sb.toString());autoWtView.invalidate();
  }

  class AutoWtView extends View{
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),t=new Paint(Paint.ANTI_ALIAS_FLAG);
    AutoWtView(){super(V712Activity.this);setBackgroundColor(Color.rgb(2,12,19));t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
    void text(Canvas c,String s,float x,float y,int col,float sz){t.setColor(col);t.setTextSize((float)dp((int)Math.max(1,Math.round(sz))));c.drawText(s,x,y,t);}
    void line(Canvas c,float x1,float y1,float x2,float y2,int col,float w){p.setColor(col);p.setStrokeWidth((float)dp((int)Math.max(1,Math.round(w))));c.drawLine(x1,y1,x2,y2,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int W=getWidth();text(c,"AUTO UNEQUAL-LOAD WHIFFLETREE",dp(12),dp(28),Color.WHITE,13);if(wtLeaves.isEmpty()){text(c,"HESAPLA VE GÖSTER sonrası otomatik topoloji burada oluşur.",dp(12),dp(60),Color.LTGRAY,7);return;}double len=Math.max(1,val(hLen)*1000.0);float left=dp(28),right=W-dp(28),tankY=dp(110);p.setColor(Color.rgb(68,80,92));c.drawRoundRect(new RectF(left,tankY-dp(28),right,tankY+dp(28)),dp(26),dp(26),p);
      HashMap<String,float[]> pos=new HashMap<>();for(WtLeaf l:wtLeaves){float x=(float)(left+(l.x+len/2)/len*(right-left));pos.put(l.id,new float[]{x,tankY});p.setColor(Color.WHITE);c.drawCircle(x,tankY,dp(4),p);text(c,l.id,x-dp(7),tankY-dp(38),Color.WHITE,5);text(c,String.format(Locale.US,"%.0fN",l.r),x-dp(10),tankY-dp(24),Color.rgb(205,220,230),5);}
      ArrayList<WtBeam> bs=new ArrayList<>(wtBeams);Collections.sort(bs,(a,b)->Integer.compare(a.level,b.level));for(WtBeam b:bs){float[] pl=pos.get(b.leftId),pr=pos.get(b.rightId);if(pl==null||pr==null)continue;float x=(float)(left+(b.x+len/2)/len*(right-left));float y=tankY+dp(95*b.level);line(c,pl[0],pl[1],x,y,Color.rgb(145,165,180),2);line(c,pr[0],pr[1],x,y,Color.rgb(145,165,180),2);p.setColor(Color.rgb(220,145,35));c.drawRoundRect(new RectF(Math.min(pl[0],pr[0]),y-dp(5),Math.max(pl[0],pr[0]),y+dp(5)),dp(2),dp(2),p);p.setColor(Color.rgb(247,207,77));c.drawCircle(x,y,dp(5),p);text(c,String.format(Locale.US,"%.0f/%.0f",b.fl,b.fr),x-dp(16),y+dp(20),Color.WHITE,5);text(c,String.format(Locale.US,"P %.0f%%",100*b.pivotRatio),x-dp(14),y+dp(34),Color.rgb(247,207,77),5);pos.put(b.id,new float[]{x,y});}
      float base=dp(590);for(WtRoot r:wtRoots){float x=(float)(left+(r.x+len/2)/len*(right-left));p.setColor(Color.rgb(67,190,113));c.drawRoundRect(new RectF(x-dp(14),base-dp(38),x+dp(14),base+dp(20)),dp(5),dp(5),p);text(c,r.id,x-dp(13),base+dp(40),Color.WHITE,5);text(c,String.format(Locale.US,"%.0fN",r.r),x-dp(13),base+dp(54),Color.rgb(247,207,77),5);}
      text(c,wtArchitecture,dp(12),dp(685),wtDirectionSpread<=8?Color.rgb(90,220,120):(wtDirectionSpread<=25?Color.rgb(247,207,77):Color.rgb(235,90,80)),6);
    }
  }
}
