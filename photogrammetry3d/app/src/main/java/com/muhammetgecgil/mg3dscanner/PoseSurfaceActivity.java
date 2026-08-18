package com.muhammetgecgil.mg3dscanner;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.imgproc.Imgproc;

public class PoseSurfaceActivity extends Activity {
    static final int RV = 71, RI = 72;
    final ArrayList<Bitmap> frames = new ArrayList<>();
    TextView status;
    volatile boolean openCvReady = false;

    TextView tv(String s, int z) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(Color.WHITE); v.setTextSize(z); v.setPadding(18,12,18,12);
        return v;
    }
    Button bt(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(17); b.setMinHeight(62); return b;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(18,22,18,24); r.setBackgroundColor(Color.rgb(10,12,15));
        TextView h = tv("MG 3D • FEATURE POSE SURFACE v2.3",25); h.setGravity(Gravity.CENTER); r.addView(h);
        TextView d = tv("Video → nesne maskesi → ORB → Essential Matrix → gerçek göreli poz → 3B surface",13); d.setTextColor(Color.LTGRAY); d.setGravity(Gravity.CENTER); r.addView(d);
        Button v = bt("🎥 VİDEO SEÇ • ÖNERİLEN"); v.setOnClickListener(x -> pick(true)); r.addView(v);
        Button p = bt("📷 FOTOĞRAFLARI SEÇ"); p.setOnClickListener(x -> pick(false)); r.addView(p);
        Button c = bt("🗑 TEMİZLE"); c.setOnClickListener(x -> { frames.clear(); set("Hazır."); }); r.addView(c);
        status = tv("OpenCV başlatılıyor…",15); status.setBackgroundColor(Color.rgb(34,37,43)); r.addView(status);
        Button go = bt("🧊 FEATURE-POSE SURFACE OLUŞTUR"); go.setOnClickListener(x -> buildModel()); r.addView(go);
        TextView i = tv("Bu sürümde 'hata: null' kaldırıldı. Her aşama ayrı kontrol edilir. Beyaz/açık zemindeki koyu nesnelerde önce hızlı nesne maskesi kullanılır; gerekirse GrabCut devreye girer.",13); i.setTextColor(Color.LTGRAY); r.addView(i);
        setContentView(r);
        try { openCvReady = OpenCVLoader.initLocal(); }
        catch (Throwable t) { openCvReady = false; }
        set(openCvReady ? "Hazır • OpenCV aktif." : "OpenCV başlatılamadı. Uygulamayı yeniden açmayı dene.");
    }

    void pick(boolean video) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType(video ? "video/*" : "image/*");
        if (!video) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        startActivityForResult(i, video ? RV : RI);
    }

    @Override protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r,c,d); if (c != RESULT_OK || d == null) return;
        new Thread(() -> {
            try {
                frames.clear();
                if (r == RV) video(d.getData());
                else if (d.getClipData() != null) for (int k=0;k<d.getClipData().getItemCount();k++) add(d.getClipData().getItemAt(k).getUri());
                else add(d.getData());
                set(frames.size()+" kaliteli keyframe hazır. Şimdi Surface Oluştur'a bas.");
            } catch (Throwable e) { fail("Girdi", e); }
        }).start();
    }

    Bitmap safeBitmap(Bitmap b) {
        if (b == null) return null;
        Bitmap.Config cfg = b.getConfig();
        if (cfg == Bitmap.Config.ARGB_8888 && !b.isRecycled()) return b;
        Bitmap c = b.copy(Bitmap.Config.ARGB_8888,false);
        return c != null ? c : b;
    }

    void add(Uri u) throws Exception {
        if (u == null) return;
        Bitmap b = MediaStore.Images.Media.getBitmap(getContentResolver(),u);
        b = safeBitmap(b); if (b != null) frames.add(scale(b,900));
    }

    Bitmap scale(Bitmap b, int max) {
        b = safeBitmap(b); int w=b.getWidth(), h=b.getHeight();
        if (Math.max(w,h) <= max) return b;
        float s = max/(float)Math.max(w,h);
        return Bitmap.createScaledBitmap(b,Math.max(2,(int)(w*s)),Math.max(2,(int)(h*s)),true);
    }

    void video(Uri u) throws Exception {
        if (u == null) throw new IOException("Video seçilemedi.");
        MediaMetadataRetriever m = new MediaMetadataRetriever();
        try {
            m.setDataSource(this,u);
            String ds = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (ds == null) throw new IOException("Video süresi okunamadı.");
            long du = Long.parseLong(ds)*1000L;
            Bitmap last = null;
            for (int k=0;k<84;k++) {
                Bitmap b = m.getFrameAtTime((long)((k+.5)*du/84),MediaMetadataRetriever.OPTION_CLOSEST);
                if (b == null) continue;
                b = scale(safeBitmap(b),760);
                double sh = sharp(b), ch = last==null ? 1 : change(last,b);
                if (sh > 15 && (last==null || ch>.045 || frames.size()<5)) { frames.add(b); last=b; }
                if (frames.size() >= 28) break;
            }
        } finally { try { m.release(); } catch (Throwable ignored) {} }
        if (frames.size()<3) throw new IOException("Videodan yeterli net kare çıkarılamadı.");
    }

    double sharp(Bitmap b) {
        Mat src = new Mat(), gray = new Mat(), lap = new Mat();
        try {
            Utils.bitmapToMat(scale(b,180),src); Imgproc.cvtColor(src,gray,Imgproc.COLOR_RGBA2GRAY); Imgproc.Laplacian(gray,lap,CvType.CV_64F);
            MatOfDouble mean=new MatOfDouble(), sd=new MatOfDouble(); Core.meanStdDev(lap,mean,sd); double[] a=sd.toArray(); return a.length==0?0:a[0];
        } finally { src.release(); gray.release(); lap.release(); }
    }

    double change(Bitmap a, Bitmap b) {
        a=scale(a,120); b=scale(b,120); int w=Math.min(a.getWidth(),b.getWidth()), h=Math.min(a.getHeight(),b.getHeight()); long s=0; int n=0;
        for(int y=0;y<h;y+=3) for(int x=0;x<w;x+=3) { int A=a.getPixel(x,y), B=b.getPixel(x,y); s+=Math.abs(Color.red(A)-Color.red(B))+Math.abs(Color.green(A)-Color.green(B))+Math.abs(Color.blue(A)-Color.blue(B)); n+=3; }
        return s/(255.0*Math.max(1,n));
    }

    void set(String s) { runOnUiThread(() -> { if (status != null) status.setText(s); }); }
    void fail(String stage, Throwable e) {
        String msg = e==null ? "bilinmeyen hata" : e.getMessage();
        if (msg == null || msg.trim().isEmpty()) msg = e==null ? "bilinmeyen hata" : e.getClass().getSimpleName();
        String type = e==null ? "" : e.getClass().getSimpleName();
        set(stage+" hatası • "+type+" • "+msg);
    }

    void buildModel() {
        if (!openCvReady) { set("OpenCV aktif değil. Uygulamayı kapatıp yeniden aç."); return; }
        if (frames.size()<3) { set("En az 3 görüntü/keyframe gerekli."); return; }
        new Thread(() -> {
            try {
                set("1/5 • Nesne maskeleri ve ORB özellikleri hazırlanıyor…");
                ArrayList<ViewData> all = new ArrayList<>(); int ix=0;
                for (Bitmap b:frames) {
                    ix++; try { ViewData v=ViewData.make(b); if(v!=null) all.add(v); }
                    catch(Throwable one) { /* tek kötü kare tüm işlemi durdurmasın */ }
                    if(ix%4==0) set("1/5 • Kare "+ix+"/"+frames.size()+" • kabul: "+all.size());
                }
                if(all.size()<3) throw new IOException("Nesne üzerinde yeterli takip özelliği bulunamadı. Daha aydınlık ve daha yavaş video çek.");

                set("2/5 • Kareler arası gerçek göreli poz çözülüyor…");
                ArrayList<ViewData> good=new ArrayList<>(); Mat Rcum=Mat.eye(3,3,CvType.CV_64F); all.get(0).R=Rcum.clone(); good.add(all.get(0));
                for(int k=1;k<all.size();k++) {
                    Mat rel=Pose.solve(good.get(good.size()-1),all.get(k));
                    if(rel==null) continue;
                    Mat next=new Mat(); Core.gemm(rel,Rcum,1,new Mat(),0,next); Rcum.release(); Rcum=next; all.get(k).R=Rcum.clone(); good.add(all.get(k));
                }
                if(good.size()<3) throw new IOException("Poz eşleşmesi yetersiz. Nesneyi küçük açı adımlarıyla döndürerek tekrar video çek.");

                set("3/5 • "+good.size()+" poz ile 3B silhouette fusion…");
                Mesh mesh=Hull.build(good,54);
                if(mesh.f.size()<100) throw new IOException("Ortak 3B hacim yetersiz. Alt/üst ve yan açılarda daha fazla örtüşme gerekli.");

                set("4/5 • "+mesh.v.size()+" vertex • "+mesh.f.size()+" triangle • OBJ yazılıyor…");
                Uri uri=saveObj(mesh);
                set("TAMAMLANDI • FEATURE-POSE v2.3\n"+mesh.v.size()+" vertex • "+mesh.f.size()+" triangle\nPoz çözülen kare: "+good.size()+"/"+all.size()+"\nOBJ: Download/MG3DScanner\n"+uri);
            } catch(Throwable e) { fail("Feature-Pose",e); }
        }).start();
    }

    static class ViewData {
        Mat gray,desc,R; MatOfKeyPoint kp; Mask mask;
        static ViewData make(Bitmap b) {
            Mat rgba=new Mat(), rgb=new Mat(), g=new Mat();
            Utils.bitmapToMat(b,rgba); Imgproc.cvtColor(rgba,rgb,Imgproc.COLOR_RGBA2RGB);
            Mask mk=Mask.grabSafe(rgb); if(mk==null) { rgba.release(); rgb.release(); g.release(); return null; }
            Imgproc.cvtColor(rgb,g,Imgproc.COLOR_RGB2GRAY);
            ORB orb=ORB.create(4200); MatOfKeyPoint kp=new MatOfKeyPoint(); Mat d=new Mat(); orb.detectAndCompute(g,mk.raw,kp,d);
            if(d.empty() || kp.rows()<60) { rgba.release(); rgb.release(); g.release(); d.release(); return null; }
            ViewData v=new ViewData(); v.gray=g; v.kp=kp; v.desc=d; v.mask=mk; rgba.release(); rgb.release(); return v;
        }
    }

    static class Mask {
        Mat raw; org.opencv.core.Rect box;
        Mask(Mat m, org.opencv.core.Rect b){raw=m;box=b;}
        boolean sample(double x,double y){
            double sx=box.width, sy=box.height; int px=(int)Math.round(box.x+box.width*.5+x*sx*.5), py=(int)Math.round(box.y+box.height*.5-y*sy*.5);
            if(px<0||py<0||px>=raw.cols()||py>=raw.rows()) return false; double[] v=raw.get(py,px); return v!=null && v.length>0 && v[0]>0;
        }

        static Mask grabSafe(Mat rgb) {
            Mask m = darkObject(rgb); if(m!=null) return m;
            try { return grabCut(rgb); } catch(Throwable t) { return centerFallback(rgb); }
        }

        static Mask darkObject(Mat rgb) {
            try {
                Mat gray=new Mat(); Imgproc.cvtColor(rgb,gray,Imgproc.COLOR_RGB2GRAY);
                int w=gray.cols(), h=gray.rows(), q=Math.max(8,Math.min(w,h)/14); double sum=0; int n=0;
                int[][] cs={{0,0},{w-q,0},{0,h-q},{w-q,h-q}};
                for(int[] c:cs) for(int y=c[1];y<c[1]+q;y+=2) for(int x=c[0];x<c[0]+q;x+=2){ double[] v=gray.get(y,x); if(v!=null){sum+=v[0];n++;}}
                double bg=n==0?220:sum/n; if(bg<125){gray.release(); return null;}
                Mat bin=new Mat(); Imgproc.threshold(gray,bin,Math.max(55,bg-35),255,Imgproc.THRESH_BINARY_INV);
                Mat ker=Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,new Size(9,9)); Imgproc.morphologyEx(bin,bin,Imgproc.MORPH_CLOSE,ker); Imgproc.morphologyEx(bin,bin,Imgproc.MORPH_OPEN,ker);
                Mask out=largest(bin,rgb.rows()*rgb.cols()*.06); gray.release(); return out;
            } catch(Throwable t){ return null; }
        }

        static Mask grabCut(Mat rgb) {
            Mat gc=new Mat(rgb.rows(),rgb.cols(),CvType.CV_8UC1,new Scalar(Imgproc.GC_BGD)); int mx=Math.max(8,rgb.cols()/20), my=Math.max(8,rgb.rows()/20);
            org.opencv.core.Rect rect=new org.opencv.core.Rect(mx,my,Math.max(2,rgb.cols()-2*mx),Math.max(2,rgb.rows()-2*my)); Mat bg=new Mat(),fg=new Mat();
            Imgproc.grabCut(rgb,gc,rect,bg,fg,3,Imgproc.GC_INIT_WITH_RECT);
            Mat bin=Mat.zeros(gc.size(),CvType.CV_8UC1);
            for(int y=0;y<gc.rows();y++) for(int x=0;x<gc.cols();x++){ double[] z=gc.get(y,x); if(z==null)continue; int q=(int)z[0]; if(q==Imgproc.GC_FGD||q==Imgproc.GC_PR_FGD) bin.put(y,x,255); }
            Mat ker=Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,new Size(7,7)); Imgproc.morphologyEx(bin,bin,Imgproc.MORPH_CLOSE,ker);
            return largest(bin,rgb.rows()*rgb.cols()*.05);
        }

        static Mask centerFallback(Mat rgb) {
            Mat m=Mat.zeros(rgb.rows(),rgb.cols(),CvType.CV_8UC1); int mx=rgb.cols()/10,my=rgb.rows()/10;
            Imgproc.rectangle(m,new org.opencv.core.Point(mx,my),new org.opencv.core.Point(rgb.cols()-mx-1,rgb.rows()-my-1),new Scalar(255),-1);
            return new Mask(m,new org.opencv.core.Rect(mx,my,rgb.cols()-2*mx,rgb.rows()-2*my));
        }

        static Mask largest(Mat bin,double minArea) {
            ArrayList<MatOfPoint> cs=new ArrayList<>(); Mat tmp=bin.clone(); Imgproc.findContours(tmp,cs,new Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE); tmp.release();
            if(cs.isEmpty()) return null; MatOfPoint best=null; double area=0;
            for(MatOfPoint c:cs){ double a=Imgproc.contourArea(c); if(a>area){area=a;best=c;} }
            if(best==null||area<minArea) return null;
            Mat clean=Mat.zeros(bin.size(),CvType.CV_8UC1); Imgproc.drawContours(clean,Collections.singletonList(best),0,new Scalar(255),-1);
            return new Mask(clean,Imgproc.boundingRect(best));
        }
    }

    static class Pose {
        static Mat solve(ViewData a,ViewData b) {
            try {
                BFMatcher m=BFMatcher.create(Core.NORM_HAMMING,false); List<MatOfDMatch> knn=new ArrayList<>(); m.knnMatch(a.desc,b.desc,knn,2);
                KeyPoint[] ka=a.kp.toArray(),kb=b.kp.toArray(); ArrayList<org.opencv.core.Point> p1=new ArrayList<>(),p2=new ArrayList<>();
                for(MatOfDMatch mm:knn){ DMatch[] q=mm.toArray(); if(q.length>=2&&q[0].distance<.75*q[1].distance&&q[0].queryIdx<ka.length&&q[0].trainIdx<kb.length){p1.add(ka[q[0].queryIdx].pt);p2.add(kb[q[0].trainIdx].pt);} }
                if(p1.size()<28) return null;
                MatOfPoint2f A=new MatOfPoint2f();A.fromList(p1); MatOfPoint2f B=new MatOfPoint2f();B.fromList(p2);
                double w=a.gray.cols(),h=a.gray.rows(),f=.95*Math.max(w,h); Mat K=Mat.eye(3,3,CvType.CV_64F);K.put(0,0,f);K.put(1,1,f);K.put(0,2,w/2);K.put(1,2,h/2);
                Mat E=Calib3d.findEssentialMat(A,B,K,Calib3d.RANSAC,.999,1.8,1000); if(E==null||E.empty()) return null;
                Mat R=new Mat(),t=new Mat(),mask=Mat.ones(A.rows(),1,CvType.CV_8U); int in=Calib3d.recoverPose(E,A,B,K,R,t,mask); if(in<20||R.empty()) return null;
                double[] r00=R.get(0,0),r11=R.get(1,1),r22=R.get(2,2); if(r00==null||r11==null||r22==null)return null;
                double tr=r00[0]+r11[0]+r22[0], ang=Math.acos(Math.max(-1,Math.min(1,(tr-1)/2))); if(Double.isNaN(ang)||ang<.015||ang>.95)return null;
                return R;
            } catch(Throwable e){ return null; }
        }
    }

    static class Mesh { ArrayList<double[]> v=new ArrayList<>(); ArrayList<int[]> f=new ArrayList<>(); }

    static class Hull {
        static Mesh build(ArrayList<ViewData> vs,int R){
            boolean[][][] o=new boolean[R][R][R]; int need=Math.max(2,(int)Math.ceil(vs.size()*.48));
            for(int x=0;x<R;x++){ double X=-1+2.0*(x+.5)/R; for(int y=0;y<R;y++){ double Y=-1+2.0*(y+.5)/R; for(int z=0;z<R;z++){ double Z=-1+2.0*(z+.5)/R; int hit=0;
                for(ViewData v:vs){ double[] a=v.R.get(0,0),b=v.R.get(0,1),c=v.R.get(0,2),d=v.R.get(1,0),e=v.R.get(1,1),f=v.R.get(1,2); if(a==null||b==null||c==null||d==null||e==null||f==null)continue; double qx=a[0]*X+b[0]*Y+c[0]*Z, qy=d[0]*X+e[0]*Y+f[0]*Z; if(v.mask.sample(qx,qy))hit++; }
                o[x][y][z]=hit>=need;
            }}}
            return surface(o,R);
        }
        static Mesh surface(boolean[][][]o,int R){
            Mesh m=new Mesh(); HashMap<String,Integer> map=new HashMap<>(); int[][]ds={{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
            int[][][]corn={{{0,0,0},{0,1,0},{0,1,1},{0,0,1}},{{1,0,0},{1,0,1},{1,1,1},{1,1,0}},{{0,0,0},{0,0,1},{1,0,1},{1,0,0}},{{0,1,0},{1,1,0},{1,1,1},{0,1,1}},{{0,0,0},{1,0,0},{1,1,0},{0,1,0}},{{0,0,1},{0,1,1},{1,1,1},{1,0,1}}};
            for(int x=0;x<R;x++)for(int y=0;y<R;y++)for(int z=0;z<R;z++)if(o[x][y][z])for(int s=0;s<6;s++){int nx=x+ds[s][0],ny=y+ds[s][1],nz=z+ds[s][2];if(nx>=0&&ny>=0&&nz>=0&&nx<R&&ny<R&&nz<R&&o[nx][ny][nz])continue;int[]id=new int[4];for(int c=0;c<4;c++){int gx=x+corn[s][c][0],gy=y+corn[s][c][1],gz=z+corn[s][c][2];String key=gx+","+gy+","+gz;Integer q=map.get(key);if(q==null){q=m.v.size();map.put(key,q);m.v.add(new double[]{-1+2.0*gx/R,-1+2.0*gy/R,-1+2.0*gz/R});}id[c]=q;}m.f.add(new int[]{id[0],id[1],id[2]});m.f.add(new int[]{id[0],id[2],id[3]});}
            return m;
        }
    }

    Uri saveObj(Mesh m)throws Exception{
        String n="MG3D_SURFACE_POSE_v23_"+System.currentTimeMillis()+".obj"; ContentValues cv=new ContentValues(); cv.put(MediaStore.Downloads.DISPLAY_NAME,n);cv.put(MediaStore.Downloads.MIME_TYPE,"text/plain");cv.put(MediaStore.Downloads.RELATIVE_PATH,"Download/MG3DScanner");
        Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv); if(u==null)throw new IOException("Dosya oluşturulamadı");
        OutputStream os=getContentResolver().openOutputStream(u); if(os==null)throw new IOException("OBJ çıktı akışı açılamadı");
        try(Writer w=new BufferedWriter(new OutputStreamWriter(os))){w.write("# MG3D Feature Pose Surface v2.3\n");for(double[]p:m.v)w.write(String.format(Locale.US,"v %.6f %.6f %.6f\n",p[0],p[1],p[2]));for(int[]f:m.f)w.write("f "+(f[0]+1)+" "+(f[1]+1)+" "+(f[2]+1)+"\n");}
        return u;
    }
}
