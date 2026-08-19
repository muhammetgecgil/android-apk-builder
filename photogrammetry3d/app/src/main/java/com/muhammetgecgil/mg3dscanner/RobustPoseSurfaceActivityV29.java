package com.muhammetgecgil.mg3dscanner;

import android.net.Uri;
import java.io.*;
import java.util.*;
import org.opencv.core.*;

public class RobustPoseSurfaceActivityV29 extends RobustPoseSurfaceActivityV251 {
 static final int FRONT=0,RIGHT=1,BACK=2,LEFT=3,TOP=4,BOTTOM=5;
 static class Cand{ViewData v;double score;Cand(ViewData a,double s){v=a;score=s;}}

 @Override void build(){
  if(!cv){set("OpenCV aktif değil.");return;}
  if(frames.isEmpty()){set("Önce en az bir video veya fotoğraf seç.");return;}
  new Thread(()->{
   try{
    set("1/6 • Kare maskeleri çıkarılıyor ve bozuk kareler eleniyor…");
    ArrayList<ViewData> all=new ArrayList<>();
    ArrayList<Double> areas=new ArrayList<>();
    for(BitmapWrap bw:wrapFrames()){
     ViewData v=null;
     try{v=looseView(bw.b);}catch(Throwable ignored){}
     if(v==null)continue;
     double ar=maskArea(v);
     if(ar>0){all.add(v);areas.add(ar);}
    }
    if(all.isEmpty())throw new IOException("Videodan kullanılabilir nesne maskesi çıkarılamadı");
    double med=median(areas);
    ArrayList<ViewData> clean=new ArrayList<>();
    for(ViewData v:all){double a=maskArea(v);if(a>=med*.42&&a<=med*2.25&&!touchesEdge(v))clean.add(v);}
    if(clean.size()<4)clean=all;

    set("2/6 • Göreli kamera pozları çözülüyor…");
    ArrayList<ViewData> posed=new ArrayList<>();
    Mat cum=Mat.eye(3,3,CvType.CV_64F);
    clean.get(0).R=cum.clone();posed.add(clean.get(0));
    int solved=0;
    for(int k=1;k<clean.size();k++){
     PoseResult pr=null;
     try{pr=Pose.solve(posed.get(posed.size()-1),clean.get(k),true);}catch(Throwable ignored){}
     if(pr==null)continue;
     Mat nx=new Mat();Core.gemm(pr.R,cum,1,new Mat(),0,nx);cum.release();cum=nx;
     clean.get(k).R=cum.clone();posed.add(clean.get(k));solved++;
    }

    set("3/6 • Kareler ÖN/SAĞ/ARKA/SOL/ÜST/ALT yönlerine sınıflandırılıyor…");
    HashMap<Integer,Cand> bins=new HashMap<>();
    if(posed.size()>=3){
     for(ViewData v:posed){int b=classify(v.R);double sc=quality(v,med);Cand old=bins.get(b);if(old==null||sc>old.score)bins.put(b,new Cand(v,sc));}
    }
    ArrayList<ViewData> use=new ArrayList<>();
    boolean fallback=false;
    if(horizontalCount(bins)>=3){
     int[] order={FRONT,RIGHT,BACK,LEFT,TOP,BOTTOM};
     for(int b:order){Cand c=bins.get(b);if(c!=null){c.v.R=canonical(b);use.add(c.v);}}
    }else{
     fallback=true;
     set("3/6 • Poz sınıflaması zayıf; kararlı 4-yön video orbit yedeği kullanılıyor…");
     use=sequenceFour(clean);
    }
    if(use.size()<3){fallback=true;use.clear();use.addAll(clean);for(int k=0;k<use.size();k++)use.get(k).R=yaw(2*Math.PI*k/Math.max(1,use.size()));}

    set("4/6 • "+use.size()+" ana yön ile temiz 3B hull oluşturuluyor…");
    Mesh mesh=Hull.build(use,72);
    if(mesh==null||mesh.f.size()<180){
     fallback=true;
     set("4/6 • Ortak hacim zayıf; en iyi ana siluetten kontrollü kapalı surface…");
     mesh=extrude(bestMask(use),72);
    }
    mesh=MeshOps.smooth(mesh,1,.07);
    set("5/6 • "+mesh.v.size()+" vertex • "+mesh.f.size()+" triangle • OBJ yazılıyor…");
    Uri u=save25(mesh);
    set("6/6 • TAMAMLANDI • VIDEO VIEW CLASSIFY v2.9\n"+
      "Kullanılan ana yön: "+use.size()+" • Poz çözülen: "+solved+"/"+clean.size()+
      (fallback?"\nMod: KARARLI YEDEK / tahmini":"\nMod: OTOMATİK YÖN SINIFLANDIRMA")+
      "\n"+mesh.v.size()+" vertex • "+mesh.f.size()+" triangle\nCAD Viewer açılıyor…");
   }catch(Throwable e){fail("v2.9 Video sınıflandırma",e);}
  }).start();
 }

 static class BitmapWrap{android.graphics.Bitmap b;BitmapWrap(android.graphics.Bitmap x){b=x;}}
 ArrayList<BitmapWrap> wrapFrames(){ArrayList<BitmapWrap>a=new ArrayList<>();for(android.graphics.Bitmap b:frames)if(b!=null)a.add(new BitmapWrap(b));return a;}
 double maskArea(ViewData v){if(v==null||v.mask==null||v.mask.raw==null)return 0;return Core.countNonZero(v.mask.raw)/(double)Math.max(1,v.mask.raw.rows()*v.mask.raw.cols());}
 boolean touchesEdge(ViewData v){if(v==null||v.mask==null)return true;org.opencv.core.Rect b=v.mask.box;int w=v.mask.raw.cols(),h=v.mask.raw.rows();int m=Math.max(2,Math.min(w,h)/100);return b.x<=m||b.y<=m||b.x+b.width>=w-m||b.y+b.height>=h-m;}
 double median(ArrayList<Double>a){ArrayList<Double>b=new ArrayList<>(a);Collections.sort(b);return b.isEmpty()?0:b.get(b.size()/2);}
 double quality(ViewData v,double med){double a=maskArea(v),areaScore=1.0-Math.min(1,Math.abs(a-med)/Math.max(.001,med));double feat=0;try{feat=(v.objKp==null?0:v.objKp.rows())+(v.bgKp==null?0:v.bgKp.rows())*.35;}catch(Throwable ignored){}return areaScore*1000+Math.min(1200,feat);}
 int horizontalCount(HashMap<Integer,Cand>b){int n=0;if(b.containsKey(FRONT))n++;if(b.containsKey(RIGHT))n++;if(b.containsKey(BACK))n++;if(b.containsKey(LEFT))n++;return n;}
 int classify(Mat r){
  if(r==null||r.empty())return FRONT;
  double fx=g(r,0,2),fy=g(r,1,2),fz=g(r,2,2);
  double n=Math.sqrt(fx*fx+fy*fy+fz*fz);if(n<1e-8)return FRONT;fx/=n;fy/=n;fz/=n;
  if(Math.abs(fy)>.72)return fy>0?TOP:BOTTOM;
  double a=Math.atan2(fx,fz);int q=(int)Math.round(a/(Math.PI/2));q=((q%4)+4)%4;
  if(q==0)return FRONT;if(q==1)return RIGHT;if(q==2)return BACK;return LEFT;
 }
 double g(Mat m,int r,int c){double[]v=m.get(r,c);return v==null||v.length==0?0:v[0];}
 Mat canonical(int b){if(b==FRONT)return yaw(0);if(b==RIGHT)return yaw(Math.PI/2);if(b==BACK)return yaw(Math.PI);if(b==LEFT)return yaw(-Math.PI/2);return pitch(b==TOP?-Math.PI/2:Math.PI/2);}
 Mat pitch(double a){Mat r=Mat.eye(3,3,CvType.CV_64F);double c=Math.cos(a),s=Math.sin(a);r.put(1,1,c);r.put(1,2,-s);r.put(2,1,s);r.put(2,2,c);return r;}
 ArrayList<ViewData> sequenceFour(ArrayList<ViewData>a){ArrayList<ViewData>u=new ArrayList<>();if(a.isEmpty())return u;int n=a.size();int[]idx={0,n/4,n/2,(3*n)/4};double[]ang={0,Math.PI/2,Math.PI,-Math.PI/2};HashSet<Integer>seen=new HashSet<>();for(int k=0;k<4;k++){int i=Math.min(n-1,idx[k]);if(seen.add(i)){ViewData v=a.get(i);v.R=yaw(ang[k]);u.add(v);}}return u;}
}
