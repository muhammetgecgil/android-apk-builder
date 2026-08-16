package com.muhammetgecgil.nesnesayarai;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import java.util.*;

/** V114 - fiziksel govde oncelikli sayim.
 * ML adaylarini sayim sonucu olarak degil, fiziksel nesne hipotezi olarak kullanir.
 * Ayni govde icindeki halka, dugme, pervane, desen ve ic parca adaylarini tek nesnede toplar.
 */
public class MainActivityV9 extends MainActivityV8 {
 @Override void ui(){super.ui();status.setText("V114 Fiziksel Govde");}

 @Override List<Obj> solve(Bitmap src,List<RectF> raw,List<PointF> rv,int vw,int vh){
  final int W=src.getWidth(),H=src.getHeight();
  if(rv==null||rv.size()<3||raw==null||raw.isEmpty())return new ArrayList<>();
  List<RectF> a=new ArrayList<>();
  for(RectF z:raw){
   RectF r=new RectF(Math.max(0,z.left),Math.max(0,z.top),Math.min(W,z.right),Math.min(H,z.bottom));
   float af=area(r)/(W*(float)H);
   if(af<.00015f||af>.46f)continue;
   if(!centerInRoi(r,W,H,rv,vw,vh))continue;
   if(roiCoverage(r,W,H,rv,vw,vh)<.58f)continue;
   a.add(r);
  }
  // Tile tekrarlarini temizle; buyuk govde adayini kaybetme.
  a.sort((x,y)->Float.compare(area(y),area(x)));
  List<RectF> u=new ArrayList<>();
  for(RectF c:a){boolean dup=false;for(RectF q:u){
    float ov=iou(c,q), ct=Math.max(contain(c,q),contain(q,c));
    float d=(float)Math.hypot(c.centerX()-q.centerX(),c.centerY()-q.centerY());
    float s=.16f*(diag(c)+diag(q));
    float ar=Math.min(area(c),area(q))/Math.max(1f,Math.max(area(c),area(q)));
    if(ov>.62f||(d<s&&ar>.68f)){dup=true;break;}
   }if(!dup)u.add(c);
  }

  // Parent-child grafigi: ic bolgeler fiziksel govdenin ayri nesnesi degildir.
  boolean[] internal=new boolean[u.size()];
  for(int i=0;i<u.size();i++)for(int j=0;j<u.size();j++)if(i!=j){
   RectF child=u.get(i), parent=u.get(j);
   float ratio=area(child)/Math.max(1f,area(parent));
   if(ratio>=.03f&&ratio<=.72f&&contain(child,parent)>.86f){
    float dc=(float)Math.hypot(child.centerX()-parent.centerX(),child.centerY()-parent.centerY());
    // Merkeze yakin nested aday: halka/dugme/pervane/desen olma olasiligi yuksek.
    if(dc < .43f*diag(parent)) internal[i]=true;
   }
  }

  List<RectF> bodies=new ArrayList<>();for(int i=0;i<u.size();i++)if(!internal[i])bodies.add(u.get(i));
  // Bir fiziksel govde farkli tile'larda parcalandiysa yakin/ust uste adaylari birlestir.
  bodies.sort((x,y)->Float.compare(area(y),area(x)));
  List<RectF> merged=new ArrayList<>();
  for(RectF c:bodies){boolean done=false;for(int i=0;i<merged.size();i++){
    RectF q=merged.get(i);float ov=iou(c,q);
    float d=(float)Math.hypot(c.centerX()-q.centerX(),c.centerY()-q.centerY());
    float lim=.20f*(diag(c)+diag(q));
    if(ov>.38f||(d<lim&&Math.min(area(c),area(q))/Math.max(1f,Math.max(area(c),area(q)))>.42f)){
     RectF n=new RectF(Math.min(c.left,q.left),Math.min(c.top,q.top),Math.max(c.right,q.right),Math.max(c.bottom,q.bottom));
     if(area(n)<1.55f*Math.max(area(c),area(q)))merged.set(i,n);done=true;break;
    }
   }if(!done)merged.add(new RectF(c));
  }

  // Buyuk bir grup kutusu birden fazla ayri govdeyi kapsiyorsa grup kutusunu sayma.
  boolean[] drop=new boolean[merged.size()];
  for(int i=0;i<merged.size();i++){RectF big=merged.get(i);int kids=0;List<PointF> cc=new ArrayList<>();
   for(int j=0;j<merged.size();j++)if(i!=j){RectF sm=merged.get(j);float ratio=area(sm)/Math.max(1f,area(big));
    if(ratio>.025f&&ratio<.48f&&contain(sm,big)>.80f){kids++;cc.add(new PointF(sm.centerX(),sm.centerY()));}}
   if(kids>=2&&separated(cc,big))drop[i]=true;
  }

  List<Obj> out=new ArrayList<>();
  for(int i=0;i<merged.size();i++)if(!drop[i]){
   RectF r=merged.get(i);
   // Etiket tam fiziksel govde merkezinde.
   PointF c=new PointF(r.centerX(),r.centerY());
   out.add(new Obj(r,c,3));
  }
  out.sort((x,y)->{int z=Float.compare(x.center.y,y.center.y);return z!=0?z:Float.compare(x.center.x,y.center.x);});
  return out;
 }

 float roiCoverage(RectF r,int W,int H,List<PointF> rv,int vw,int vh){int in=0,n=0;for(int yy=1;yy<=5;yy++)for(int xx=1;xx<=5;xx++){float x=r.left+r.width()*xx/6f,y=r.top+r.height()*yy/6f;n++;if(pip(x,y,W,H,rv,vw,vh))in++;}return in/(float)n;}
 boolean separated(List<PointF> c,RectF big){float th=.24f*Math.min(big.width(),big.height());for(int i=0;i<c.size();i++)for(int j=i+1;j<c.size();j++)if(Math.hypot(c.get(i).x-c.get(j).x,c.get(i).y-c.get(j).y)>th)return true;return false;}
}