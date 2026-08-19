package com.muhammetgecgil.mg3dscanner;

import android.net.Uri;
import java.io.*;import java.util.*;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.features2d.*;

/** v3.0: real feature triangulation path. No silhouette extrusion as primary reconstruction. */
public class SfmSurfaceActivityV30 extends RobustPoseSurfaceActivityV251 {
 static class Cam { Mat R,t; Cam(Mat r,Mat tt){R=r;t=tt;} }
 static class Pair { Mat R,t; ArrayList<org.opencv.core.Point> a=new ArrayList<>(),b=new ArrayList<>(); int inliers; }
 static class P3 { double x,y,z; P3(double X,double Y,double Z){x=X;y=Y;z=Z;} }

 @Override void build(){
  if(!cv){set("OpenCV aktif değil.");return;}
  if(frames.size()<3){set("En az 3 görüntü/video keyframe gerekli.");return;}
  new Thread(()->{try{
   set("1/7 • Nesne özellikleri hazırlanıyor…");
   ArrayList<ViewData> v=new ArrayList<>();
   for(int i=0;i<frames.size();i++){
    ViewData d=null; try{d=looseView(frames.get(i));}catch(Throwable ignored){}
    if(d!=null && d.objDesc!=null && !d.objDesc.empty() && d.objKp!=null && d.objKp.rows()>=45) v.add(d);
    if(i%4==0)set("1/7 • "+(i+1)+"/"+frames.size()+" • kabul "+v.size());
   }
   if(v.size()<3)throw new IOException("Yeterli ortak nesne özelliği yok; daha yakın ve örtüşmeli çekim gerekli");

   set("2/7 • Kamera R+t pozları RANSAC ile çözülüyor…");
   ArrayList<Cam> cams=new ArrayList<>();
   cams.add(new Cam(Mat.eye(3,3,CvType.CV_64F),Mat.zeros(3,1,CvType.CV_64F)));
   ArrayList<P3> cloud=new ArrayList<>(); int acceptedPairs=0,totalIn=0;
   for(int i=1;i<v.size();i++){
    Pair p=solvePair(v.get(i-1),v.get(i));
    if(p==null || p.inliers<24) continue;
    Cam prev=cams.get(cams.size()-1); Cam cur=compose(prev,p);
    cams.add(cur);
    ArrayList<P3> pts=triangulate(p,prev,cur,v.get(i-1).gray.cols(),v.get(i-1).gray.rows());
    cloud.addAll(pts); acceptedPairs++; totalIn+=p.inliers;
    set("2/7 • çift "+acceptedPairs+" • inlier "+totalIn+" • 3B nokta "+cloud.size());
   }
   if(acceptedPairs<2 || cloud.size()<80)throw new IOException("Gerçek 3B triangülasyon için ortak eşleşme yetersiz");

   set("3/7 • 3B aykırı noktalar temizleniyor…");
   cloud=filterCloud(cloud);
   if(cloud.size()<60)throw new IOException("Filtre sonrası yeterli tutarlı 3B nokta kalmadı");

   set("4/7 • Sparse cloud yüzeye yoğunlaştırılıyor…");
   Mesh mesh=cloudToMesh(cloud,86);
   if(mesh==null || mesh.f.size()<250)throw new IOException("Nokta bulutundan tutarlı yüzey üretilemedi");

   set("5/7 • En büyük bağlı yüzey + hafif smoothing…");
   mesh=MeshOps.smooth(mesh,2,.10);
   set("6/7 • "+mesh.v.size()+" vertex • "+mesh.f.size()+" triangle • OBJ yazılıyor…");
   Uri u=save25(mesh);
   set("7/7 • TAMAMLANDI • SfM v3.0\nGerçek triangülasyon: "+cloud.size()+" 3B nokta\nKabul edilen kamera çifti: "+acceptedPairs+" • RANSAC inlier: "+totalIn+"\n"+mesh.v.size()+" vertex • "+mesh.f.size()+" triangle\nCAD Viewer açılıyor…\n"+u);
  }catch(Throwable e){fail("SfM v3.0",e);}}).start();
 }

 Pair solvePair(ViewData A,ViewData B){try{
  BFMatcher m=BFMatcher.create(Core.NORM_HAMMING,false); List<MatOfDMatch> knn=new ArrayList<>(); m.knnMatch(A.objDesc,B.objDesc,knn,2);
  KeyPoint[] ka=A.objKp.toArray(),kb=B.objKp.toArray(); ArrayList<org.opencv.core.Point> p1=new ArrayList<>(),p2=new ArrayList<>();
  for(MatOfDMatch mm:knn){DMatch[] q=mm.toArray(); if(q.length<2||q[0].distance>=.72*q[1].distance)continue; if(q[0].queryIdx<0||q[0].queryIdx>=ka.length||q[0].trainIdx<0||q[0].trainIdx>=kb.length)continue; p1.add(ka[q[0].queryIdx].pt);p2.add(kb[q[0].trainIdx].pt);}
  if(p1.size()<30)return null;
  MatOfPoint2f a=new MatOfPoint2f();a.fromList(p1);MatOfPoint2f b=new MatOfPoint2f();b.fromList(p2);
  double w=A.gray.cols(),h=A.gray.rows(),f=.92*Math.max(w,h); org.opencv.core.Point pp=new org.opencv.core.Point(w*.5,h*.5); Mat mask=new Mat();
  Mat E=Calib3d.findEssentialMat(a,b,f,pp,Calib3d.RANSAC,.999,1.25,mask); if(E==null||E.empty())return null;
  Mat R=new Mat(),t=new Mat(); int in=Calib3d.recoverPose(E,a,b,R,t,f,pp,mask); if(in<24)return null;
  Pair out=new Pair(); out.R=R;out.t=t;out.inliers=in;
  byte[] mk=new byte[(int)mask.total()]; if(mk.length>0)mask.get(0,0,mk);
  for(int i=0;i<p1.size();i++){if(i<mk.length && mk[i]!=0){out.a.add(p1.get(i));out.b.add(p2.get(i));}}
  return out;
 }catch(Throwable e){return null;}}

 Cam compose(Cam prev,Pair p){
  Mat R=new Mat();Core.gemm(p.R,prev.R,1,new Mat(),0,R);
  Mat rt=new Mat();Core.gemm(p.R,prev.t,1,new Mat(),0,rt);Core.add(rt,p.t,rt);
  return new Cam(R,rt);
 }

 ArrayList<P3> triangulate(Pair p,Cam c1,Cam c2,int w,int h){ArrayList<P3> out=new ArrayList<>();try{
  double f=.92*Math.max(w,h),cx=w*.5,cy=h*.5; Mat K=new Mat(3,3,CvType.CV_64F);K.put(0,0,f,0,cx,0,f,cy,0,0,1);
  Mat P1=proj(c1),P2=proj(c2),KP1=new Mat(),KP2=new Mat();Core.gemm(K,P1,1,new Mat(),0,KP1);Core.gemm(K,P2,1,new Mat(),0,KP2);
  MatOfPoint2f a=new MatOfPoint2f();a.fromList(p.a);MatOfPoint2f b=new MatOfPoint2f();b.fromList(p.b);Mat X=new Mat();Calib3d.triangulatePoints(KP1,KP2,a,b,X);
  for(int i=0;i<X.cols();i++){double[] q0=X.get(0,i),q1=X.get(1,i),q2=X.get(2,i),q3=X.get(3,i);if(q0==null||q3==null||Math.abs(q3[0])<1e-9)continue;double x=q0[0]/q3[0],y=q1[0]/q3[0],z=q2[0]/q3[0];if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))continue;if(Math.abs(x)>50||Math.abs(y)>50||Math.abs(z)>50)continue;out.add(new P3(x,y,z));}
  K.release();P1.release();P2.release();KP1.release();KP2.release();X.release();
 }catch(Throwable ignored){}return out;}

 Mat proj(Cam c){Mat P=new Mat(3,4,CvType.CV_64F);for(int r=0;r<3;r++){for(int k=0;k<3;k++)P.put(r,k,c.R.get(r,k)[0]);P.put(r,3,c.t.get(r,0)[0]);}return P;}

 ArrayList<P3> filterCloud(ArrayList<P3> a){
  if(a.size()<20)return a; double[] xs=new double[a.size()],ys=new double[a.size()],zs=new double[a.size()];for(int i=0;i<a.size();i++){xs[i]=a.get(i).x;ys[i]=a.get(i).y;zs[i]=a.get(i).z;}
  double mx=med(xs),my=med(ys),mz=med(zs);double[] rr=new double[a.size()];for(int i=0;i<a.size();i++){P3 p=a.get(i);rr[i]=Math.sqrt(sq(p.x-mx)+sq(p.y-my)+sq(p.z-mz));}double mr=med(rr);ArrayList<P3> o=new ArrayList<>();for(int i=0;i<a.size();i++)if(rr[i]<=Math.max(1e-6,mr*2.8))o.add(a.get(i));return o;
 }
 double sq(double x){return x*x;} double med(double[]a){double[]b=a.clone();Arrays.sort(b);return b[b.length/2];}

 Mesh cloudToMesh(ArrayList<P3>a,int R){
  double minx=1e9,miny=1e9,minz=1e9,maxx=-1e9,maxy=-1e9,maxz=-1e9;for(P3 p:a){minx=Math.min(minx,p.x);miny=Math.min(miny,p.y);minz=Math.min(minz,p.z);maxx=Math.max(maxx,p.x);maxy=Math.max(maxy,p.y);maxz=Math.max(maxz,p.z);}double sx=maxx-minx,sy=maxy-miny,sz=maxz-minz,s=Math.max(sx,Math.max(sy,sz));if(s<1e-9)return null;
  boolean[][][] o=new boolean[R][R][R];int rad=2;
  for(P3 p:a){int x=(int)Math.round((p.x-minx)/s*(R-8))+4,y=(int)Math.round((p.y-miny)/s*(R-8))+4,z=(int)Math.round((p.z-minz)/s*(R-8))+4;for(int dx=-rad;dx<=rad;dx++)for(int dy=-rad;dy<=rad;dy++)for(int dz=-rad;dz<=rad;dz++){if(dx*dx+dy*dy+dz*dz>rad*rad)continue;int X=x+dx,Y=y+dy,Z=z+dz;if(X>=0&&Y>=0&&Z>=0&&X<R&&Y<R&&Z<R)o[X][Y][Z]=true;}}
  // close tiny gaps without creating long plates
  for(int pass=0;pass<2;pass++){boolean[][][] n=new boolean[R][R][R];for(int x=1;x<R-1;x++)for(int y=1;y<R-1;y++)for(int z=1;z<R-1;z++){int c=0;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)if(o[x+dx][y+dy][z+dz])c++;n[x][y][z]=o[x][y][z]||c>=5;}o=n;}
  o=largest(o,R);return Hull.surface(o,R);
 }

 boolean[][][] largest(boolean[][][]o,int R){boolean[][][]seen=new boolean[R][R][R],best=new boolean[R][R][R];int bestN=0;int[]dx={1,-1,0,0,0,0},dy={0,0,1,-1,0,0},dz={0,0,0,0,1,-1};for(int x=0;x<R;x++)for(int y=0;y<R;y++)for(int z=0;z<R;z++)if(o[x][y][z]&&!seen[x][y][z]){ArrayDeque<int[]>q=new ArrayDeque<>();ArrayList<int[]>c=new ArrayList<>();q.add(new int[]{x,y,z});seen[x][y][z]=true;while(!q.isEmpty()){int[]p=q.remove();c.add(p);for(int k=0;k<6;k++){int X=p[0]+dx[k],Y=p[1]+dy[k],Z=p[2]+dz[k];if(X<0||Y<0||Z<0||X>=R||Y>=R||Z>=R||seen[X][Y][Z]||!o[X][Y][Z])continue;seen[X][Y][Z]=true;q.add(new int[]{X,Y,Z});}}if(c.size()>bestN){bestN=c.size();best=new boolean[R][R][R];for(int[]p:c)best[p[0]][p[1]][p[2]]=true;}}return best;}
}
