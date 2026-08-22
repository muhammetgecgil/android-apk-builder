package com.muhammetgecgil.mg3dscanner;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/**
 * v3.1 mechanical hybrid reconstruction.
 * Root fixes over v2.8/v3.0:
 *  - keeps holes and separated projected components instead of filling one outer contour
 *  - preserves each engineering view's aspect ratio (no square stretching)
 *  - intersects six labelled projections in one common volume
 *  - rejects collapsed/degenerate meshes instead of presenting them as successful CAD
 */
public class MechanicalHybridActivityV31 extends OrthographicCadActivityV26 {

 static class AspectSil extends Sil {
  final double cx,cy,scale;
  AspectSil(Mat m, org.opencv.core.Rect b){
   super(m,b); cx=b.x+b.width*.5; cy=b.y+b.height*.5; scale=Math.max(b.width,b.height);
  }
  @Override boolean at(double x,double y){
   int px=(int)Math.round(cx+x*.5*scale);
   int py=(int)Math.round(cy-y*.5*scale);
   if(px<0||py<0||px>=m.cols()||py>=m.rows())return false;
   double[] q=m.get(py,px); return q!=null&&q.length>0&&q[0]>0;
  }
 }

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  try{brand(getWindow().getDecorView());}catch(Throwable ignored){}
 }

 void brand(View v){
  if(v instanceof TextView){
   TextView t=(TextView)v; String s=String.valueOf(t.getText());
   s=s.replace("MG 3D • ORTHO CAD v2.8","MG 3D • HYBRID CAD v3.1")
      .replace("🧊 ORTHO CAD SURFACE OLUŞTUR","🧠 HYBRID 6-VIEW CAD OLUŞTUR")
      .replace("Ana geometriyi altı yön görünüşü belirler.","v3.1: Altı yönün TAM maskesi, boşlukları/delikleri ve gerçek en-boy oranını korur. Ana geometriyi altı yön görünüşü belirler.");
   t.setText(s);
  }
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)brand(g.getChildAt(i));}
 }

 /** Preserve the complete object projection. Never collapse it to one filled external contour. */
 @Override Sil outer(Bitmap b){
  Mat rgba=new Mat(),rgb=new Mat();
  Utils.bitmapToMat(b,rgba); Imgproc.cvtColor(rgba,rgb,Imgproc.COLOR_RGBA2RGB);
  Mask mm=Mask.auto(rgb); rgba.release(); rgb.release();
  if(mm==null||mm.raw==null||mm.raw.empty())return null;
  Mat raw=mm.raw.clone();
  Imgproc.threshold(raw,raw,1,255,Imgproc.THRESH_BINARY);
  // Only tiny speckle cleanup. Large slots/holes must stay open.
  Mat k3=Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,new Size(3,3));
  Imgproc.morphologyEx(raw,raw,Imgproc.MORPH_OPEN,k3);

  ArrayList<MatOfPoint> cs=new ArrayList<>();
  Imgproc.findContours(raw.clone(),cs,new Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
  if(cs.isEmpty()){raw.release();return null;}
  double maxA=0; for(MatOfPoint q:cs)maxA=Math.max(maxA,Imgproc.contourArea(q));
  double minA=Math.max(18.0,maxA*.0012); // keep rods, bolts, pulleys; drop isolated pixel noise
  Mat clean=Mat.zeros(raw.size(),CvType.CV_8UC1);
  org.opencv.core.Rect ub=null;
  for(MatOfPoint q:cs){
   double a=Imgproc.contourArea(q); if(a<minA)continue;
   org.opencv.core.Rect r=Imgproc.boundingRect(q);
   boolean border=r.x<=1||r.y<=1||r.x+r.width>=raw.cols()-1||r.y+r.height>=raw.rows()-1;
   if(border&&a<maxA*.12)continue;
   Mat region=Mat.zeros(raw.size(),CvType.CV_8UC1);
   Imgproc.drawContours(region,Collections.singletonList(q),-1,new Scalar(255),-1);
   Core.bitwise_and(region,raw,region); // restores all internal holes from original mask
   Core.bitwise_or(clean,region,clean); region.release();
   if(ub==null)ub=r; else {
    int x=Math.min(ub.x,r.x),y=Math.min(ub.y,r.y);
    int x2=Math.max(ub.x+ub.width,r.x+r.width),y2=Math.max(ub.y+ub.height,r.y+r.height);
    ub=new org.opencv.core.Rect(x,y,x2-x,y2-y);
   }
  }
  raw.release();
  if(ub==null||ub.width<10||ub.height<10){clean.release();return null;}
  return new AspectSil(clean,ub);
 }

 @Override void make(){
  if(front==null||back==null||left==null||right==null||top==null||bottom==null){
   st.setText("Başarılı geometri için ÖN + ARKA + SOL + SAĞ + ÜST + ALT görünüşlerinin altısı da gerekli.");return;
  }
  new Thread(()->{try{
   setS("1/7 • v3.1 tam projeksiyon maskeleri: delikler + ayrı parçalar korunuyor…");
   Sil ff=outer(front),fb=outer(back),sl=outer(left),sr=outer(right),tt=outer(top),tb=outer(bottom);
   if(ff==null||fb==null||sl==null||sr==null||tt==null||tb==null)throw new IOException("Bir ana görünüşte güvenilir nesne maskesi çıkarılamadı");
   setS("2/7 • En-boy oranları korunarak ÖN/ARKA, SOL/SAĞ, ÜST/ALT kaydediliyor…");
   Sil f=merge(ff,fb),s=merge(sl,sr),t=mergeTop(tt,tb);
   setS("3/7 • 112³ ortak 3B visual-hull kesişimi hesaplanıyor…");
   Mesh m=ortho(f,s,t,112);
   if(m==null||m.f.size()<500)throw new IOException("Altı görünüş ortak 3B hacimde yeterince uyuşmuyor");
   double[] ext=extent(m); double mx=Math.max(ext[0],Math.max(ext[1],ext[2]));
   double mn=Math.min(ext[0],Math.min(ext[1],ext[2]));
   if(mx<=1e-8||mn/mx<.035)throw new IOException("3B çözüm çöktü/levha biçiminde; görünüş yönlerini kontrol et");
   setS("4/7 • "+others.size()+" çapraz foto + "+videos.size()+" video kalite referansı okunuyor…");
   int vf=collectVideoFrames();
   // Gentle smoothing only: keep cylinders, holes, thin rods and base edges.
   m=MeshOps.smooth(m,1,.035);
   setS("5/7 • Geometri kalite kontrolü: X="+fmt(ext[0])+" Y="+fmt(ext[1])+" Z="+fmt(ext[2])+" • video kare "+vf);
   setS("6/7 • "+m.v.size()+" vertex • "+m.f.size()+" triangle • OBJ yazılıyor…");
   Uri u=save31(m);
   setS("7/7 • TAMAMLANDI • HYBRID CAD v3.1 • Viewer açılıyor…");
   runOnUiThread(()->{Intent i=new Intent(this,AutoObjCadSurfaceActivity.class);i.setData(u);i.putExtra("force_format","obj");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);});
  }catch(Throwable e){setS("v3.1 kalite kapısı: "+e.getClass().getSimpleName()+" • "+e.getMessage()+"\nYanlış/uydurma OBJ oluşturulmadı.");}}).start();
 }

 double[] extent(Mesh m){
  double ax=1e9,ay=1e9,az=1e9,bx=-1e9,by=-1e9,bz=-1e9;
  for(double[]p:m.v){ax=Math.min(ax,p[0]);ay=Math.min(ay,p[1]);az=Math.min(az,p[2]);bx=Math.max(bx,p[0]);by=Math.max(by,p[1]);bz=Math.max(bz,p[2]);}
  return new double[]{bx-ax,by-ay,bz-az};
 }
 String fmt(double x){return String.format(Locale.US,"%.3f",x);}

 Uri save31(Mesh m)throws Exception{
  String n="MG3D_HYBRID_CAD_v31_"+System.currentTimeMillis()+".obj";
  ContentValues c=new ContentValues();c.put(MediaStore.Downloads.DISPLAY_NAME,n);c.put(MediaStore.Downloads.MIME_TYPE,"model/obj");c.put(MediaStore.Downloads.RELATIVE_PATH,"Download/MG3DScanner");
  Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,c);if(u==null)throw new IOException("OBJ oluşturulamadı");
  try(Writer w=new BufferedWriter(new OutputStreamWriter(getContentResolver().openOutputStream(u)))){
   w.write("# MG3D Hybrid CAD v3.1 - six-view aspect-correct full-mask visual hull\n");
   for(double[]p:m.v)w.write(String.format(Locale.US,"v %.6f %.6f %.6f\n",p[0],p[1],p[2]));
   for(int[]f:m.f)w.write("f "+(f[0]+1)+" "+(f[1]+1)+" "+(f[2]+1)+"\n");
  }
  return u;
 }
}
