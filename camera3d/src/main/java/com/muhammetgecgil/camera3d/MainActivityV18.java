package com.muhammetgecgil.camera3d;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivityV18 extends Activity implements GLSurfaceView.Renderer {
    static final int REQ = 71;
    static final String[] FACES = {"ÖN", "SAĞ", "ARKA", "SOL"};
    static final int MASK_W = 128, MASK_H = 128, GRID = 64;

    GLSurfaceView cameraGl;
    Session session;
    CameraBackground bg;
    TextView status, instruction, photoStatus, modelInfo;
    ProgressBar progress;
    TextView[] faceState = new TextView[4];
    Button shootBtn, approveBtn, retryBtn, buildBtn, newProjectBtn, viewerBtn;

    final boolean[] captured = new boolean[4];
    final boolean[] approved = new boolean[4];
    final byte[][] photos = new byte[4][];
    final Pose[] photoPoses = new Pose[4];
    final String[] photoNames = new String[4];
    volatile int currentFace = 0;
    volatile boolean captureRequested = false;
    volatile boolean viewerOpen = false;
    String projectId = "";
    ArrayList<float[]> modelPoints = new ArrayList<>();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        newProjectId();
        buildCaptureUi();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ);
        else setupAr();
    }

    void newProjectId(){ projectId = "Scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()); }

    TextView tv(String s, float sp, int color){ TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setPadding(8,5,8,5); return t; }

    void buildCaptureUi(){
        viewerOpen = false;
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(0xff080b10);
        LinearLayout main = new LinearLayout(this); main.setOrientation(LinearLayout.HORIZONTAL);

        FrameLayout camBox = new FrameLayout(this);
        cameraGl = new GLSurfaceView(this); cameraGl.setEGLContextClientVersion(2); cameraGl.setPreserveEGLContextOnPause(true); cameraGl.setRenderer(this); cameraGl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        camBox.addView(cameraGl, new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.VERTICAL); top.setPadding(16,10,16,10); top.setBackgroundColor(0x99101820);
        top.addView(tv("CAMERA 3D • 4 FOTO GERÇEK 3D • v1.8",18,Color.WHITE));
        status = tv("Kamera hazırlanıyor…",13,0xffd8e2ef);
        modelInfo = tv("3D model: henüz oluşturulmadı",12,0xff7fdaff);
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100); progress.setProgress(0);
        top.addView(status); top.addView(modelInfo); top.addView(progress);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1,-2,Gravity.TOP); tp.setMargins(10,10,10,0); camBox.addView(top,tp);

        LinearLayout bottom = new LinearLayout(this); bottom.setOrientation(LinearLayout.VERTICAL); bottom.setPadding(16,10,16,12); bottom.setBackgroundColor(0xbb101820);
        instruction = tv("1/4 • ÖN FOTOĞRAFI ÇEK",18,Color.WHITE);
        photoStatus = tv("Nesneyi kadrajda aynı büyüklükte tut. Arka plan mümkünse düz renk olsun.",13,0xffc7d3df);
        bottom.addView(instruction); bottom.addView(photoStatus);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); bp.setMargins(10,0,10,10); camBox.addView(bottom,bp);

        LinearLayout side = new LinearLayout(this); side.setOrientation(LinearLayout.VERTICAL); side.setPadding(14,14,14,14); side.setBackgroundColor(0xff121820);
        side.addView(tv("4 FOTO → 3D HACİM",21,Color.WHITE));
        side.addView(tv("ÖN → SAĞ → ARKA → SOL. Her fotoğrafı sen onaylarsın. Sonra 4 siluet birleştirilip 3D yüzey çıkarılır.",12,0xffaebdca));
        for(int i=0;i<4;i++){ faceState[i]=tv("○ "+FACES[i],15,0xffe6edf3); side.addView(faceState[i]); }
        shootBtn = new Button(this); shootBtn.setText("FOTOĞRAF ÇEK"); shootBtn.setOnClickListener(v->requestCapture()); side.addView(shootBtn);
        approveBtn = new Button(this); approveBtn.setText("FOTOĞRAFI ONAYLA"); approveBtn.setOnClickListener(v->approvePhoto()); side.addView(approveBtn);
        retryBtn = new Button(this); retryBtn.setText("TEKRAR ÇEK"); retryBtn.setOnClickListener(v->retryPhoto()); side.addView(retryBtn);
        buildBtn = new Button(this); buildBtn.setText("4 FOTOĞRAFI 3D OLUŞTUR"); buildBtn.setOnClickListener(v->build3D()); side.addView(buildBtn);
        viewerBtn = new Button(this); viewerBtn.setText("3D MODELİ TAM EKRAN AÇ"); viewerBtn.setOnClickListener(v->openViewer()); side.addView(viewerBtn);
        newProjectBtn = new Button(this); newProjectBtn.setText("YENİ PROJE"); newProjectBtn.setOnClickListener(v->resetProject()); side.addView(newProjectBtn);

        main.addView(camBox,new LinearLayout.LayoutParams(0,-1,4.8f)); main.addView(side,new LinearLayout.LayoutParams(0,-1,1.8f)); root.addView(main,new FrameLayout.LayoutParams(-1,-1)); setContentView(root); refreshUi();
    }

    void setupAr(){
        try{
            if(session!=null)return;
            if(ArCoreApk.getInstance().checkAvailability(this).isUnsupported()){status.setText("ARCore desteklenmiyor");return;}
            session = new Session(this);
            CameraConfigFilter f = new CameraConfigFilter(session); f.setFacingDirection(CameraConfig.FacingDirection.BACK); f.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
            List<CameraConfig> configs = session.getSupportedCameraConfigs(f); CameraConfig best=null; long area=-1;
            for(CameraConfig c:configs){ long a=(long)c.getTextureSize().getWidth()*c.getTextureSize().getHeight(); if(a>area){area=a;best=c;} }
            if(best!=null) session.setCameraConfig(best);
            Config c = new Config(session); c.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE); c.setFocusMode(Config.FocusMode.AUTO); session.configure(c);
            status.setText("Kamera hazır • nesneyi sabit tut");
        }catch(Throwable t){ status.setText("ARCore: "+t.getClass().getSimpleName()); }
    }

    void requestCapture(){
        if(session==null){setupAr(); if(session==null)return;}
        if(captured[currentFace]){Toast.makeText(this,"Önce ONAYLA veya TEKRAR ÇEK",Toast.LENGTH_SHORT).show();return;}
        captureRequested=true; shootBtn.setEnabled(false); photoStatus.setText(FACES[currentFace]+" çekiliyor… telefonu sabit tut");
    }

    void approvePhoto(){
        if(!captured[currentFace])return;
        approved[currentFace]=true;
        if(currentFace<3){ currentFace++; instruction.setText((currentFace+1)+"/4 • "+FACES[currentFace]+" FOTOĞRAFI ÇEK"); photoStatus.setText("Aynı mesafe ve benzer kadrajla çek"); }
        else { instruction.setText("4/4 TAMAM • 3D OLUŞTUR"); photoStatus.setText("Dört fotoğraf hazır"); }
        refreshUi();
    }

    void retryPhoto(){ captured[currentFace]=false; approved[currentFace]=false; photos[currentFace]=null; photoPoses[currentFace]=null; photoNames[currentFace]=null; photoStatus.setText(FACES[currentFace]+" fotoğrafını tekrar çek"); refreshUi(); }

    void resetProject(){
        Arrays.fill(captured,false); Arrays.fill(approved,false); Arrays.fill(photos,null); Arrays.fill(photoPoses,null); Arrays.fill(photoNames,null); modelPoints.clear(); currentFace=0; captureRequested=false; newProjectId(); progress.setProgress(0); modelInfo.setText("3D model: henüz oluşturulmadı"); instruction.setText("1/4 • ÖN FOTOĞRAFI ÇEK"); photoStatus.setText("Nesneyi kadrajda aynı büyüklükte tut"); refreshUi();
    }

    void refreshUi(){
        for(int i=0;i<4;i++){ String s=approved[i]?"✓ ":captured[i]?"● ":i==currentFace?"▶ ":"○ "; faceState[i].setText(s+FACES[i]); faceState[i].setTextColor(approved[i]?0xff4bd37b:(i==currentFace?0xffffc857:0xffe6edf3)); }
        boolean all=approved[0]&&approved[1]&&approved[2]&&approved[3]; boolean hc=captured[currentFace]; shootBtn.setEnabled(!hc&&!all); approveBtn.setEnabled(hc&&!approved[currentFace]); retryBtn.setEnabled(hc&&!approved[currentFace]); buildBtn.setEnabled(all); viewerBtn.setEnabled(modelPoints.size()>100);
    }

    @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){ bg=new CameraBackground(); bg.create(); GLES20.glClearColor(0,0,0,1); }
    @Override public void onSurfaceChanged(GL10 gl,int w,int h){ GLES20.glViewport(0,0,w,h); if(session!=null)session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(),w,h); }
    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT); if(session==null||bg==null||viewerOpen)return;
        try{ session.setCameraTextureName(bg.textureId); Frame frame=session.update(); bg.draw(frame); com.google.ar.core.Camera cam=frame.getCamera(); if(cam.getTrackingState()!=TrackingState.TRACKING){ui("Kamera açık • telefonu yavaşça hareket ettir");return;} if(captureRequested)tryCapture(frame,cam.getPose()); }
        catch(Throwable t){ui("Kamera/AR hatası: "+t.getClass().getSimpleName());}
    }

    void tryCapture(Frame f,Pose pose){
        Image img=null;
        try{
            img=f.acquireCameraImage(); byte[] jpeg=yuv420ToJpeg(img,96); photos[currentFace]=jpeg; photoPoses[currentFace]=pose; captured[currentFace]=true; captureRequested=false;
            String name=projectId+"_"+(currentFace+1)+"_"+FACES[currentFace]+".jpg"; photoNames[currentFace]=name; writeFile(name,"image/jpeg",jpeg,"Download/Camera3D/"+projectId);
            int face=currentFace; runOnUiThread(()->{ photoStatus.setText(FACES[face]+" çekildi • uygunsa ONAYLA"); refreshUi(); });
        }catch(Throwable t){captureRequested=false;runOnUiThread(()->{shootBtn.setEnabled(true);photoStatus.setText("Fotoğraf alınamadı • tekrar dene");});}
        finally{if(img!=null)img.close();}
    }

    byte[] yuv420ToJpeg(Image image,int quality)throws Exception{
        int w=image.getWidth(),h=image.getHeight(); Image.Plane[] p=image.getPlanes(); byte[] nv21=new byte[w*h*3/2]; ByteBuffer yb=p[0].getBuffer(); int yr=p[0].getRowStride(),yp=p[0].getPixelStride();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int idx=y*yr+x*yp;if(idx<yb.limit())nv21[y*w+x]=yb.get(idx);} ByteBuffer ub=p[1].getBuffer(),vb=p[2].getBuffer(); int ur=p[1].getRowStride(),vr=p[2].getRowStride(),up=p[1].getPixelStride(),vp=p[2].getPixelStride(),off=w*h;
        for(int y=0;y<h/2;y++)for(int x=0;x<w/2;x++){int vi=y*vr+x*vp,ui=y*ur+x*up;nv21[off++]=vb.get(vi);nv21[off++]=ub.get(ui);} YuvImage yi=new YuvImage(nv21,ImageFormat.NV21,w,h,null); ByteArrayOutputStream out=new ByteArrayOutputStream(); yi.compressToJpeg(new Rect(0,0,w,h),quality,out); return out.toByteArray();
    }

    void build3D(){
        if(!(approved[0]&&approved[1]&&approved[2]&&approved[3]))return;
        buildBtn.setEnabled(false); viewerBtn.setEnabled(false); progress.setProgress(2); modelInfo.setText("3D: fotoğraflar analiz ediliyor…");
        new Thread(()->{
            try{
                boolean[][] masks=new boolean[4][];
                for(int i=0;i<4;i++){ final int pct=10+i*12; final int fi=i; runOnUiThread(()->{progress.setProgress(pct);modelInfo.setText("3D: "+FACES[fi]+" silueti çıkarılıyor…");}); masks[i]=makeMask(photos[i]); }
                runOnUiThread(()->{progress.setProgress(62);modelInfo.setText("3D: dört görünüş birleştiriliyor…");});
                ArrayList<float[]> pts=visualHull(masks);
                if(pts.size()<300) throw new Exception("Siluet eşleşmesi yetersiz");
                modelPoints=pts;
                runOnUiThread(()->{progress.setProgress(84);modelInfo.setText("3D: yüzey hazırlanıyor • "+pts.size()+" nokta");});
                String folder="Download/Camera3D/"+projectId; writeFile(projectId+"_visual_hull.ply","application/octet-stream",ply(pts),folder); writeFile(projectId+"_visual_hull.obj","text/plain",obj(pts),folder);
                runOnUiThread(()->{progress.setProgress(100);modelInfo.setText("3D MODEL HAZIR • "+pts.size()+" yüzey noktası");viewerBtn.setEnabled(true);buildBtn.setEnabled(true);Toast.makeText(this,"4 fotoğraftan 3D model oluşturuldu",Toast.LENGTH_LONG).show();showViewer(pts);});
            }catch(Throwable e){ runOnUiThread(()->{progress.setProgress(0);modelInfo.setText("3D oluşturulamadı • arka planı sadeleştirip tekrar çek");buildBtn.setEnabled(true);Toast.makeText(this,"3D model üretilemedi: "+e.getMessage(),Toast.LENGTH_LONG).show();}); }
        }).start();
    }

    boolean[] makeMask(byte[] jpeg)throws Exception{
        Bitmap src=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length); if(src==null)throw new Exception("Foto decode");
        int side=Math.min(src.getWidth(),src.getHeight()); int sx=(src.getWidth()-side)/2, sy=(src.getHeight()-side)/2; Bitmap crop=Bitmap.createBitmap(src,sx,sy,side,side); Bitmap b=Bitmap.createScaledBitmap(crop,MASK_W,MASK_H,true); if(crop!=src)crop.recycle(); if(src!=b)src.recycle();
        long sr=0,sg=0,sb=0,sr2=0,sg2=0,sb2=0; int n=0, border=10;
        for(int y=0;y<MASK_H;y++)for(int x=0;x<MASK_W;x++){if(x<border||x>=MASK_W-border||y<border||y>=MASK_H-border){int c=b.getPixel(x,y);int r=Color.red(c),g=Color.green(c),bl=Color.blue(c);sr+=r;sg+=g;sb+=bl;sr2+=(long)r*r;sg2+=(long)g*g;sb2+=(long)bl*bl;n++;}}
        float br=sr/(float)n,bg=sg/(float)n,bb=sb/(float)n; float vr=sr2/(float)n-br*br,vg=sg2/(float)n-bg*bg,vb=sb2/(float)n-bb*bb; float sigma=(float)Math.sqrt(Math.max(1,(vr+vg+vb)/3f)); float th=Math.max(38f,Math.min(95f,38f+sigma*1.7f));
        boolean[] raw=new boolean[MASK_W*MASK_H];
        for(int y=0;y<MASK_H;y++)for(int x=0;x<MASK_W;x++){int c=b.getPixel(x,y);float dr=Color.red(c)-br,dg=Color.green(c)-bg,db=Color.blue(c)-bb;float d=(float)Math.sqrt(dr*dr+dg*dg+db*db);raw[y*MASK_W+x]=d>th;}
        b.recycle(); raw=morph(raw); return largestComponent(raw);
    }

    boolean[] morph(boolean[] m){
        boolean[] a=m.clone();
        for(int pass=0;pass<2;pass++){boolean[] o=a.clone();for(int y=1;y<MASK_H-1;y++)for(int x=1;x<MASK_W-1;x++){int cnt=0;for(int yy=-1;yy<=1;yy++)for(int xx=-1;xx<=1;xx++)if(a[(y+yy)*MASK_W+x+xx])cnt++;o[y*MASK_W+x]=cnt>=5;}a=o;}return a;
    }

    boolean[] largestComponent(boolean[] m){
        boolean[] seen=new boolean[m.length],best=new boolean[m.length];int bestN=0;int[] q=new int[m.length];
        for(int i=0;i<m.length;i++){if(!m[i]||seen[i])continue;int head=0,tail=0;q[tail++]=i;seen[i]=true;ArrayList<Integer> comp=new ArrayList<>();while(head<tail){int p=q[head++];comp.add(p);int x=p%MASK_W,y=p/MASK_W;int[] ns={p-1,p+1,p-MASK_W,p+MASK_W};for(int k=0;k<4;k++){int np=ns[k];if(np<0||np>=m.length)continue;int nx=np%MASK_W,ny=np/MASK_W;if(Math.abs(nx-x)+Math.abs(ny-y)!=1)continue;if(m[np]&&!seen[np]){seen[np]=true;q[tail++]=np;}}}if(comp.size()>bestN){Arrays.fill(best,false);for(int p:comp)best[p]=true;bestN=comp.size();}}
        return best;
    }

    ArrayList<float[]> visualHull(boolean[][] m){
        boolean[] occ=new boolean[GRID*GRID*GRID]; int inside=0;
        for(int y=0;y<GRID;y++){int my=Math.min(MASK_H-1,Math.round(y*(MASK_H-1f)/(GRID-1f)));for(int x=0;x<GRID;x++){int mx=Math.min(MASK_W-1,Math.round(x*(MASK_W-1f)/(GRID-1f)));int bx=MASK_W-1-mx;for(int z=0;z<GRID;z++){int mz=Math.min(MASK_W-1,Math.round(z*(MASK_W-1f)/(GRID-1f)));int bz=MASK_W-1-mz;boolean ok=m[0][my*MASK_W+mx]&&m[2][my*MASK_W+bx]&&m[1][my*MASK_W+mz]&&m[3][my*MASK_W+bz];if(ok){occ[(y*GRID+x)*GRID+z]=true;inside++;}}}}
        ArrayList<float[]> pts=new ArrayList<>();
        for(int y=0;y<GRID;y++)for(int x=0;x<GRID;x++)for(int z=0;z<GRID;z++){int idx=(y*GRID+x)*GRID+z;if(!occ[idx])continue;boolean surf=false;int[][] d={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};for(int[]v:d){int yy=y+v[0],xx=x+v[1],zz=z+v[2];if(yy<0||yy>=GRID||xx<0||xx>=GRID||zz<0||zz>=GRID||!occ[(yy*GRID+xx)*GRID+zz]){surf=true;break;}}if(surf){float fx=(x-(GRID-1)/2f)/((GRID-1)/2f);float fy=((GRID-1)/2f-y)/((GRID-1)/2f);float fz=(z-(GRID-1)/2f)/((GRID-1)/2f);pts.add(new float[]{fx,fy,fz,1f});}}
        return pts;
    }

    void openViewer(){ if(modelPoints.size()<100){Toast.makeText(this,"Önce 3D modeli oluştur",Toast.LENGTH_SHORT).show();return;} showViewer(new ArrayList<>(modelPoints)); }

    void showViewer(ArrayList<float[]> pts){
        viewerOpen=true;if(session!=null)try{session.pause();}catch(Throwable ignored){}getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);PointViewer view=new PointViewer(pts);root.addView(view,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setPadding(8,6,8,6);bar.setBackgroundColor(0xaa000000);
        Button close=new Button(this);close.setText("← TARAMA");close.setOnClickListener(v->closeViewer());Button reset=new Button(this);reset.setText("MERKEZ");reset.setOnClickListener(v->view.resetView());Button front=new Button(this);front.setText("ÖN");front.setOnClickListener(v->view.setPreset(0));Button right=new Button(this);right.setText("SAĞ");right.setOnClickListener(v->view.setPreset(1));Button top=new Button(this);top.setText("ÜST");top.setOnClickListener(v->view.setPreset(2));bar.addView(close);bar.addView(reset);bar.addView(front);bar.addView(right);bar.addView(top);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);p.setMargins(8,8,8,0);root.addView(bar,p);
        TextView hint=tv("PARMAKLA 360° DÖNDÜR • İKİ PARMAKLA ZOOM • 4 FOTOĞRAFTAN ÜRETİLEN 3D YÜZEY",13,Color.WHITE);hint.setBackgroundColor(0xaa000000);hint.setGravity(Gravity.CENTER);FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);hp.setMargins(25,0,25,18);root.addView(hint,hp);setContentView(root);
    }

    void closeViewer(){getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);buildCaptureUi();viewerOpen=false;if(session!=null)try{session.resume();}catch(Throwable ignored){}if(cameraGl!=null)cameraGl.onResume();}

    byte[] ply(ArrayList<float[]>p){StringBuilder s=new StringBuilder("ply\nformat ascii 1.0\nelement vertex "+p.size()+"\nproperty float x\nproperty float y\nproperty float z\nend_header\n");for(float[]v:p)s.append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');return s.toString().getBytes(StandardCharsets.UTF_8);}
    byte[] obj(ArrayList<float[]>p){StringBuilder s=new StringBuilder("# Camera3D visual hull\n");for(float[]v:p)s.append("v ").append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');return s.toString().getBytes(StandardCharsets.UTF_8);}
    void writeFile(String name,String mime,byte[]data,String relative)throws Exception{ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,name);v.put(MediaStore.Downloads.MIME_TYPE,mime);v.put(MediaStore.Downloads.RELATIVE_PATH,relative);Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)throw new Exception("MediaStore");try(OutputStream o=getContentResolver().openOutputStream(u)){if(o==null)throw new Exception("Output");o.write(data);}}
    void ui(String s){runOnUiThread(()->{if(status!=null)status.setText(s);});}

    @Override protected void onResume(){super.onResume();if(!viewerOpen){if(session==null&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)setupAr();if(session!=null)try{session.resume();}catch(CameraNotAvailableException e){if(status!=null)status.setText("Kamera açılamadı");}if(cameraGl!=null)cameraGl.onResume();}}
    @Override protected void onPause(){if(!viewerOpen){if(cameraGl!=null)cameraGl.onPause();if(session!=null)try{session.pause();}catch(Throwable ignored){}}super.onPause();}
    @Override protected void onDestroy(){if(session!=null)session.close();super.onDestroy();}
    @Override public void onBackPressed(){if(viewerOpen)closeViewer();else super.onBackPressed();}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)setupAr();}

    static FloatBuffer fb(float[]a){ByteBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder());FloatBuffer f=b.asFloatBuffer();f.put(a).position(0);return f;}
    static int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}

    class PointViewer extends GLSurfaceView implements GLSurfaceView.Renderer,View.OnTouchListener{
        FloatBuffer verts;int count,program,aPos,uMvp,uColor;float rotX=-18f,rotY=25f,zoom=3.1f,lastX,lastY;ScaleGestureDetector scale;
        PointViewer(ArrayList<float[]>data){super(MainActivityV18.this);setEGLContextClientVersion(2);float[]a=new float[data.size()*3];int k=0;for(float[]p:data){a[k++]=p[0];a[k++]=p[1];a[k++]=p[2];}verts=fb(a);count=data.size();setRenderer(this);setRenderMode(RENDERMODE_CONTINUOUSLY);setOnTouchListener(this);scale=new ScaleGestureDetector(MainActivityV18.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoom/=d.getScaleFactor();zoom=Math.max(1.6f,Math.min(8f,zoom));return true;}});}
        public void onSurfaceCreated(GL10 g,EGLConfig c){GLES20.glClearColor(.015f,.018f,.025f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);String vs="uniform mat4 u;attribute vec3 a;void main(){gl_Position=u*vec4(a,1.0);gl_PointSize=5.0;}";String fs="precision mediump float;uniform vec4 c;void main(){gl_FragColor=c;}";program=GLES20.glCreateProgram();GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(program);aPos=GLES20.glGetAttribLocation(program,"a");uMvp=GLES20.glGetUniformLocation(program,"u");uColor=GLES20.glGetUniformLocation(program,"c");}
        public void onSurfaceChanged(GL10 g,int w,int h){GLES20.glViewport(0,0,w,h);}public void onDrawFrame(GL10 g){GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);if(count==0)return;float[]pr=new float[16],vw=new float[16],md=new float[16],tmp=new float[16],mvp=new float[16];Matrix.perspectiveM(pr,0,45f,(float)getWidth()/Math.max(1,getHeight()),.05f,50);Matrix.setLookAtM(vw,0,0,0,zoom,0,0,0,0,1,0);Matrix.setIdentityM(md,0);Matrix.rotateM(md,0,rotX,1,0,0);Matrix.rotateM(md,0,rotY,0,1,0);Matrix.multiplyMM(tmp,0,vw,0,md,0);Matrix.multiplyMM(mvp,0,pr,0,tmp,0);GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniform4f(uColor,.18f,.86f,1f,1);verts.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,verts);GLES20.glEnableVertexAttribArray(aPos);GLES20.glDrawArrays(GLES20.GL_POINTS,0,count);}
        public boolean onTouch(View v,MotionEvent e){scale.onTouchEvent(e);if(e.getPointerCount()==1&&!scale.isInProgress()){if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){rotY+=(e.getX()-lastX)*.35f;rotX+=(e.getY()-lastY)*.35f;lastX=e.getX();lastY=e.getY();return true;}}return true;}void resetView(){rotX=-18;rotY=25;zoom=3.1f;}void setPreset(int p){if(p==0){rotX=0;rotY=0;}else if(p==1){rotX=0;rotY=90;}else{rotX=-90;rotY=0;}zoom=3.1f;}
    }

    static class CameraBackground{
        int textureId,program,aPos,aUv,uTex;FloatBuffer vb=fb(new float[]{-1,-1,1,-1,-1,1,1,1}),tb=fb(new float[]{0,1,1,1,0,0,1,0});
        void create(){int[]t=new int[1];GLES20.glGenTextures(1,t,0);textureId=t[0];GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);String vs="attribute vec2 a;attribute vec2 b;varying vec2 v;void main(){gl_Position=vec4(a,0.,1.);v=b;}";String fs="#extension GL_OES_EGL_image_external : require\nprecision mediump float;uniform samplerExternalOES s;varying vec2 v;void main(){gl_FragColor=texture2D(s,v);}";program=GLES20.glCreateProgram();GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(program);aPos=GLES20.glGetAttribLocation(program,"a");aUv=GLES20.glGetAttribLocation(program,"b");uTex=GLES20.glGetUniformLocation(program,"s");}
        void draw(Frame f){float[]in={-1,-1,1,-1,-1,1,1,1},out=new float[8];try{f.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,in,Coordinates2d.TEXTURE_NORMALIZED,out);tb=fb(out);}catch(Throwable ignored){}GLES20.glDisable(GLES20.GL_DEPTH_TEST);GLES20.glUseProgram(program);vb.position(0);tb.position(0);GLES20.glVertexAttribPointer(aPos,2,GLES20.GL_FLOAT,false,0,vb);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aUv,2,GLES20.GL_FLOAT,false,0,tb);GLES20.glEnableVertexAttribArray(aUv);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glUniform1i(uTex,0);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);}
    }
}
