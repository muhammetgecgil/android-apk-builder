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

public class MainActivityV19 extends Activity implements GLSurfaceView.Renderer {
    static final int REQ = 71;
    static final String[] FACES = {"ÖN", "SAĞ", "ARKA", "SOL"};
    static final int MW = 96, MH = 96, GRID = 48;

    GLSurfaceView cameraGl;
    Session session;
    CameraBackground bg;
    TextView status, instruction, photoStatus, modelInfo, videoInfo;
    TextView[] faceState = new TextView[4];
    ProgressBar progress;
    Button photoModeBtn, videoModeBtn, shootBtn, approveBtn, retryBtn, buildBtn, viewerBtn, newProjectBtn, videoStartBtn;

    final byte[][] photos = new byte[4][];
    final Pose[] photoPoses = new Pose[4];
    final boolean[] captured = new boolean[4];
    final boolean[] approved = new boolean[4];
    final float[] videoBestError = {999,999,999,999};
    int currentFace = 0;
    boolean captureRequested = false, viewerOpen = false, videoMode = false, videoScanning = false;
    long lastVideoFrame = 0;
    float videoYaw0 = 0;
    int videoFrames = 0;
    String projectId;
    Mesh mesh;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        newProject();
        buildUi();
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.CAMERA},REQ); else setupAr();
    }

    void newProject(){ projectId="Scan_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date()); }
    TextView tv(String s,float sp,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setPadding(8,5,8,5);return t;}

    void buildUi(){
        viewerOpen=false;
        FrameLayout root=new FrameLayout(this); root.setBackgroundColor(0xff070a0f);
        LinearLayout main=new LinearLayout(this); main.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout camBox=new FrameLayout(this);
        cameraGl=new GLSurfaceView(this); cameraGl.setEGLContextClientVersion(2); cameraGl.setPreserveEGLContextOnPause(true); cameraGl.setRenderer(this); cameraGl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); camBox.addView(cameraGl,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.VERTICAL); top.setPadding(16,10,16,10); top.setBackgroundColor(0xaa0b1119);
        top.addView(tv("CAMERA 3D • SOLID MESH + VIDEO • v1.9",18,Color.WHITE));
        status=tv("Kamera hazırlanıyor…",13,0xffdce7f2); modelInfo=tv("3D model: hazır değil",12,0xff83ddff); videoInfo=tv("",12,0xffffcc66);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100);
        top.addView(status); top.addView(modelInfo); top.addView(videoInfo); top.addView(progress);
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP); tp.setMargins(10,10,10,0); camBox.addView(top,tp);

        LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.VERTICAL); bottom.setPadding(16,10,16,12); bottom.setBackgroundColor(0xbb0b1119);
        instruction=tv("ÖN FOTOĞRAFI ÇEK",18,Color.WHITE); photoStatus=tv("Nesneyi açık ve sade bir arka planda kadraja al.",13,0xffcbd6e1); bottom.addView(instruction); bottom.addView(photoStatus);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); bp.setMargins(10,0,10,10); camBox.addView(bottom,bp);

        LinearLayout side=new LinearLayout(this); side.setOrientation(LinearLayout.VERTICAL); side.setPadding(12,12,12,12); side.setBackgroundColor(0xff111821);
        LinearLayout modes=new LinearLayout(this); photoModeBtn=new Button(this); photoModeBtn.setText("4 FOTO"); videoModeBtn=new Button(this); videoModeBtn.setText("VİDEO TARAMA"); modes.addView(photoModeBtn,new LinearLayout.LayoutParams(0,-2,1)); modes.addView(videoModeBtn,new LinearLayout.LayoutParams(0,-2,1)); side.addView(modes);
        photoModeBtn.setOnClickListener(v->setMode(false)); videoModeBtn.setOnClickListener(v->setMode(true));
        side.addView(tv("ÇIKTI: DOLU ÜÇGEN YÜZEY (MESH)",15,Color.WHITE));
        for(int i=0;i<4;i++){faceState[i]=tv("○ "+FACES[i],14,0xffe7edf3);side.addView(faceState[i]);}
        shootBtn=new Button(this);shootBtn.setText("FOTOĞRAF ÇEK");shootBtn.setOnClickListener(v->requestCapture());side.addView(shootBtn);
        approveBtn=new Button(this);approveBtn.setText("FOTOĞRAFI ONAYLA");approveBtn.setOnClickListener(v->approve());side.addView(approveBtn);
        retryBtn=new Button(this);retryBtn.setText("TEKRAR ÇEK");retryBtn.setOnClickListener(v->retry());side.addView(retryBtn);
        videoStartBtn=new Button(this);videoStartBtn.setText("VİDEO TARAMAYI BAŞLAT");videoStartBtn.setOnClickListener(v->toggleVideo());side.addView(videoStartBtn);
        buildBtn=new Button(this);buildBtn.setText("SOLID 3D MESH OLUŞTUR");buildBtn.setOnClickListener(v->buildMesh());side.addView(buildBtn);
        viewerBtn=new Button(this);viewerBtn.setText("3D YÜZEYİ TAM EKRAN AÇ");viewerBtn.setOnClickListener(v->openViewer());side.addView(viewerBtn);
        newProjectBtn=new Button(this);newProjectBtn.setText("YENİ PROJE");newProjectBtn.setOnClickListener(v->reset());side.addView(newProjectBtn);

        main.addView(camBox,new LinearLayout.LayoutParams(0,-1,4.8f)); main.addView(side,new LinearLayout.LayoutParams(0,-1,1.9f)); root.addView(main,new FrameLayout.LayoutParams(-1,-1)); setContentView(root); setMode(false); refresh();
    }

    void setMode(boolean video){
        videoMode=video;
        if(videoScanning) videoScanning=false;
        shootBtn.setVisibility(video?View.GONE:View.VISIBLE); approveBtn.setVisibility(video?View.GONE:View.VISIBLE); retryBtn.setVisibility(video?View.GONE:View.VISIBLE); videoStartBtn.setVisibility(video?View.VISIBLE:View.GONE);
        instruction.setText(video?"VİDEO: NESNENİN ETRAFINDA YAVAŞÇA DOLAŞ":"1/4 • ÖN FOTOĞRAFI ÇEK");
        photoStatus.setText(video?"Uygulama 0°/90°/180°/270° yönlerine en yakın kareleri otomatik seçecek.":"Her yüzü çek ve sen onayla.");
        videoInfo.setText(video?"Video sektörleri: 0/4":""); refresh();
    }

    void setupAr(){
        try{
            if(session!=null)return;
            if(ArCoreApk.getInstance().checkAvailability(this).isUnsupported()){status.setText("ARCore desteklenmiyor");return;}
            session=new Session(this);
            CameraConfigFilter f=new CameraConfigFilter(session);f.setFacingDirection(CameraConfig.FacingDirection.BACK);f.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
            List<CameraConfig> cs=session.getSupportedCameraConfigs(f);CameraConfig best=null;long area=-1;for(CameraConfig c:cs){long a=(long)c.getTextureSize().getWidth()*c.getTextureSize().getHeight();if(a>area){area=a;best=c;}}if(best!=null)session.setCameraConfig(best);
            Config c=new Config(session);c.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);c.setFocusMode(Config.FocusMode.AUTO);session.configure(c);status.setText("Kamera hazır");
        }catch(Throwable t){status.setText("ARCore: "+t.getClass().getSimpleName());}
    }

    void requestCapture(){if(captured[currentFace]){Toast.makeText(this,"Önce ONAYLA veya TEKRAR ÇEK",Toast.LENGTH_SHORT).show();return;}captureRequested=true;shootBtn.setEnabled(false);photoStatus.setText(FACES[currentFace]+" çekiliyor…");}
    void approve(){if(!captured[currentFace])return;approved[currentFace]=true;if(currentFace<3){currentFace++;instruction.setText((currentFace+1)+"/4 • "+FACES[currentFace]+" FOTOĞRAFI ÇEK");}else{instruction.setText("4/4 TAMAM • SOLID MESH OLUŞTUR");}refresh();}
    void retry(){captured[currentFace]=false;approved[currentFace]=false;photos[currentFace]=null;photoPoses[currentFace]=null;photoStatus.setText(FACES[currentFace]+" tekrar çekilecek");refresh();}

    void toggleVideo(){
        if(!videoScanning){Arrays.fill(videoBestError,999);Arrays.fill(captured,false);Arrays.fill(approved,false);Arrays.fill(photos,null);videoFrames=0;videoScanning=true;videoYaw0=Float.NaN;videoStartBtn.setText("VİDEO TARAMAYI BİTİR");instruction.setText("YAVAŞÇA 360° DOLAŞ • NESNEYİ ORTADA TUT");}
        else{videoScanning=false;videoStartBtn.setText("VİDEO TARAMAYI BAŞLAT");int n=countCaptured();if(n==4){Arrays.fill(approved,true);instruction.setText("4 YÖN TAMAM • SOLID MESH OLUŞTUR");Toast.makeText(this,"Video taraması 4 yönü tamamladı",Toast.LENGTH_LONG).show();}else Toast.makeText(this,"Eksik yön var: "+n+"/4 • tekrar video tara",Toast.LENGTH_LONG).show();refresh();}
    }

    int countCaptured(){int n=0;for(boolean b:captured)if(b)n++;return n;}

    @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){bg=new CameraBackground();bg.create();GLES20.glClearColor(0,0,0,1);}
    @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);if(session!=null)session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(),w,h);}
    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);if(session==null||bg==null||viewerOpen)return;
        try{session.setCameraTextureName(bg.textureId);Frame f=session.update();bg.draw(f);com.google.ar.core.Camera cam=f.getCamera();if(cam.getTrackingState()!=TrackingState.TRACKING){ui("Takip hazırlanıyor • telefonu yavaş hareket ettir");return;}if(captureRequested)captureOne(f,cam.getPose(),currentFace,false);if(videoScanning)videoSample(f,cam.getPose());}
        catch(Throwable t){ui("Kamera/AR hatası: "+t.getClass().getSimpleName());}
    }

    void videoSample(Frame f,Pose p){
        long now=System.currentTimeMillis();if(now-lastVideoFrame<550)return;lastVideoFrame=now;
        float yaw=yawDeg(p);if(Float.isNaN(videoYaw0))videoYaw0=yaw;float rel=wrap(yaw-videoYaw0);int sector=((int)Math.floor((rel+45f)/90f))&3;float center=sector*90f;float err=Math.abs(angleDiff(rel,center));
        if(err<videoBestError[sector] && err<34f){captureOne(f,p,sector,true);videoBestError[sector]=err;videoFrames++;runOnUiThread(()->{videoInfo.setText("Video kare: "+videoFrames+" • yön: "+countCaptured()+"/4");refresh();});}
    }

    float yawDeg(Pose p){float[] z=p.getZAxis();return (float)Math.toDegrees(Math.atan2(-z[0],-z[2]));}
    float wrap(float a){while(a<0)a+=360;while(a>=360)a-=360;return a;}
    float angleDiff(float a,float b){float d=wrap(a)-wrap(b);while(d>180)d-=360;while(d<-180)d+=360;return d;}

    void captureOne(Frame f,Pose pose,int face,boolean fromVideo){Image img=null;try{img=f.acquireCameraImage();byte[] jpeg=yuv420ToJpeg(img,94);photos[face]=jpeg;photoPoses[face]=pose;captured[face]=true;if(!fromVideo)captureRequested=false;String name=projectId+"_"+FACES[face]+(fromVideo?"_VIDEO":"")+".jpg";writeFile(name,"image/jpeg",jpeg,"Download/Camera3D/"+projectId);runOnUiThread(()->{if(!fromVideo){shootBtn.setEnabled(false);photoStatus.setText(FACES[face]+" çekildi • uygunsa ONAYLA");}refresh();});}catch(Throwable t){if(!fromVideo){captureRequested=false;runOnUiThread(()->shootBtn.setEnabled(true));}}finally{if(img!=null)img.close();}}

    byte[] yuv420ToJpeg(Image image,int q)throws Exception{int w=image.getWidth(),h=image.getHeight();Image.Plane[] p=image.getPlanes();byte[] nv21=new byte[w*h*3/2];ByteBuffer yb=p[0].getBuffer();int yr=p[0].getRowStride(),yp=p[0].getPixelStride();for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=y*yr+x*yp;if(i<yb.limit())nv21[y*w+x]=yb.get(i);}ByteBuffer ub=p[1].getBuffer(),vb=p[2].getBuffer();int ur=p[1].getRowStride(),vr=p[2].getRowStride(),up=p[1].getPixelStride(),vp=p[2].getPixelStride(),o=w*h;for(int y=0;y<h/2;y++)for(int x=0;x<w/2;x++){int vi=y*vr+x*vp,ui=y*ur+x*up;nv21[o++]=vb.get(vi);nv21[o++]=ub.get(ui);}YuvImage yi=new YuvImage(nv21,ImageFormat.NV21,w,h,null);ByteArrayOutputStream out=new ByteArrayOutputStream();yi.compressToJpeg(new Rect(0,0,w,h),q,out);return out.toByteArray();}

    void buildMesh(){
        if(!(captured[0]&&captured[1]&&captured[2]&&captured[3])){Toast.makeText(this,"ÖN/SAĞ/ARKA/SOL dört görünüş gerekli",Toast.LENGTH_LONG).show();return;}
        buildBtn.setEnabled(false);progress.setProgress(3);modelInfo.setText("Siluetler çıkarılıyor…");
        new Thread(()->{try{boolean[][] masks=new boolean[4][];for(int i=0;i<4;i++){final int ii=i;runOnUiThread(()->{progress.setProgress(10+ii*10);modelInfo.setText(FACES[ii]+" arka plan ayrılıyor…");});masks[i]=mask(photos[i]);}runOnUiThread(()->{progress.setProgress(55);modelInfo.setText("3D hacim hesaplanıyor…");});boolean[] vox=volume(masks);runOnUiThread(()->{progress.setProgress(72);modelInfo.setText("Noktalar üçgen yüzeye çevriliyor…");});Mesh m=meshFromVoxels(vox);if(m.triangles<20)throw new Exception("yüzey çok az");mesh=m;String folder="Download/Camera3D/"+projectId;writeFile(projectId+"_SOLID_MESH.obj","text/plain",obj(m),folder);writeFile(projectId+"_SOLID_MESH.ply","application/octet-stream",ply(m),folder);runOnUiThread(()->{progress.setProgress(100);modelInfo.setText("SOLID MESH HAZIR • "+m.triangles+" üçgen");buildBtn.setEnabled(true);viewerBtn.setEnabled(true);Toast.makeText(this,"Dolu 3D yüzey oluşturuldu",Toast.LENGTH_LONG).show();showViewer(m);});}catch(Throwable e){runOnUiThread(()->{progress.setProgress(0);modelInfo.setText("Mesh üretilemedi • arka plan/çekim kontrol et");buildBtn.setEnabled(true);Toast.makeText(this,"Mesh hatası: "+e.getMessage(),Toast.LENGTH_LONG).show();});}}).start();
    }

    boolean[] mask(byte[] jpeg)throws Exception{
        Bitmap src=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length);if(src==null)throw new Exception("foto okunamadı");Bitmap b=Bitmap.createScaledBitmap(src,MW,MH,true);int[] pix=new int[MW*MH];b.getPixels(pix,0,MW,0,0,MW,MH);src.recycle();b.recycle();
        long sr=0,sg=0,sb=0;int n=0;for(int y=0;y<MH;y++)for(int x=0;x<MW;x++)if(x<6||x>=MW-6||y<6||y>=MH-6){int c=pix[y*MW+x];sr+=Color.red(c);sg+=Color.green(c);sb+=Color.blue(c);n++;}float br=sr/(float)n,bg=sg/(float)n,bb=sb/(float)n;
        double var=0;for(int y=0;y<MH;y++)for(int x=0;x<MW;x++)if(x<6||x>=MW-6||y<6||y>=MH-6){int c=pix[y*MW+x];float dr=Color.red(c)-br,dg=Color.green(c)-bg,db=Color.blue(c)-bb;var+=dr*dr+dg*dg+db*db;}float th=(float)Math.max(28,Math.min(75,Math.sqrt(var/n)*2.1+24));
        boolean[] raw=new boolean[MW*MH];for(int i=0;i<raw.length;i++){int c=pix[i];float dr=Color.red(c)-br,dg=Color.green(c)-bg,db=Color.blue(c)-bb;raw[i]=Math.sqrt(dr*dr+dg*dg+db*db)>th;}
        raw=majority(raw);raw=majority(raw);return largest(raw);
    }

    boolean[] majority(boolean[] a){boolean[] o=new boolean[a.length];for(int y=1;y<MH-1;y++)for(int x=1;x<MW-1;x++){int c=0;for(int yy=-1;yy<=1;yy++)for(int xx=-1;xx<=1;xx++)if(a[(y+yy)*MW+x+xx])c++;o[y*MW+x]=c>=4;}return o;}
    boolean[] largest(boolean[] a){boolean[] seen=new boolean[a.length],best=new boolean[a.length];int bestN=0;int[] q=new int[a.length];for(int s=0;s<a.length;s++){if(!a[s]||seen[s])continue;int h=0,t=0;q[t++]=s;seen[s]=true;while(h<t){int v=q[h++],x=v%MW,y=v/MW;int[] ns={v-1,v+1,v-MW,v+MW};for(int k=0;k<4;k++){int u=ns[k];if(u<0||u>=a.length||seen[u]||!a[u])continue;int ux=u%MW,uy=u/MW;if(Math.abs(ux-x)+Math.abs(uy-y)!=1)continue;seen[u]=true;q[t++]=u;}}if(t>bestN){Arrays.fill(best,false);for(int i=0;i<t;i++)best[q[i]]=true;bestN=t;}}return best;}

    boolean[] volume(boolean[][] m){boolean[] v=new boolean[GRID*GRID*GRID];for(int y=0;y<GRID;y++){float fy=1f-((y+.5f)/GRID);int py=clamp((int)(fy*MH),0,MH-1);for(int z=0;z<GRID;z++){float fz=(z+.5f)/GRID;for(int x=0;x<GRID;x++){float fx=(x+.5f)/GRID;int pf=clamp((int)(fx*MW),0,MW-1),pb=clamp((int)((1-fx)*MW),0,MW-1),pr=clamp((int)((1-fz)*MW),0,MW-1),pl=clamp((int)(fz*MW),0,MW-1);if(m[0][py*MW+pf]&&m[2][py*MW+pb]&&m[1][py*MW+pr]&&m[3][py*MW+pl])v[(y*GRID+z)*GRID+x]=true;}}}return v;}
    int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}

    Mesh meshFromVoxels(boolean[] vox){ArrayList<Float> p=new ArrayList<>(),n=new ArrayList<>();int[][] dirs={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};for(int y=0;y<GRID;y++)for(int z=0;z<GRID;z++)for(int x=0;x<GRID;x++){if(!vox[(y*GRID+z)*GRID+x])continue;for(int f=0;f<6;f++){int nx=x+dirs[f][0],ny=y+dirs[f][1],nz=z+dirs[f][2];boolean out=nx<0||ny<0||nz<0||nx>=GRID||ny>=GRID||nz>=GRID||!vox[(ny*GRID+nz)*GRID+nx];if(out)addFace(p,n,x,y,z,f);}}float[] pp=new float[p.size()],nn=new float[n.size()];for(int i=0;i<pp.length;i++)pp[i]=p.get(i);for(int i=0;i<nn.length;i++)nn[i]=n.get(i);return new Mesh(pp,nn,pp.length/9);}

    void addFace(ArrayList<Float> p,ArrayList<Float> n,int x,int y,int z,int f){float a=-1f+2f*x/GRID,b=-1f+2f*(x+1)/GRID,c=1f-2f*y/GRID,d=1f-2f*(y+1)/GRID,e=-1f+2f*z/GRID,g=-1f+2f*(z+1)/GRID;float[][] q;float[] no;if(f==0){q=new float[][]{{b,c,e},{b,d,e},{b,d,g},{b,c,g}};no=new float[]{1,0,0};}else if(f==1){q=new float[][]{{a,c,g},{a,d,g},{a,d,e},{a,c,e}};no=new float[]{-1,0,0};}else if(f==2){q=new float[][]{{a,c,e},{a,c,g},{b,c,g},{b,c,e}};no=new float[]{0,1,0};}else if(f==3){q=new float[][]{{a,d,g},{a,d,e},{b,d,e},{b,d,g}};no=new float[]{0,-1,0};}else if(f==4){q=new float[][]{{b,c,g},{b,d,g},{a,d,g},{a,c,g}};no=new float[]{0,0,1};}else{q=new float[][]{{a,c,e},{a,d,e},{b,d,e},{b,c,e}};no=new float[]{0,0,-1};}int[] id={0,1,2,0,2,3};for(int k:id){for(float v:q[k])p.add(v);for(float v:no)n.add(v);}}

    byte[] obj(Mesh m){StringBuilder s=new StringBuilder("# Camera3D Solid Mesh v1.9\n");for(int i=0;i<m.pos.length;i+=3)s.append("v ").append(m.pos[i]).append(' ').append(m.pos[i+1]).append(' ').append(m.pos[i+2]).append('\n');for(int i=0;i<m.triangles;i++){int a=i*3+1;s.append("f ").append(a).append(' ').append(a+1).append(' ').append(a+2).append('\n');}return s.toString().getBytes(StandardCharsets.UTF_8);}
    byte[] ply(Mesh m){int vc=m.pos.length/3;StringBuilder s=new StringBuilder("ply\nformat ascii 1.0\nelement vertex "+vc+"\nproperty float x\nproperty float y\nproperty float z\nelement face "+m.triangles+"\nproperty list uchar int vertex_indices\nend_header\n");for(int i=0;i<m.pos.length;i+=3)s.append(m.pos[i]).append(' ').append(m.pos[i+1]).append(' ').append(m.pos[i+2]).append('\n');for(int i=0;i<m.triangles;i++){int a=i*3;s.append("3 ").append(a).append(' ').append(a+1).append(' ').append(a+2).append('\n');}return s.toString().getBytes(StandardCharsets.UTF_8);}

    void openViewer(){if(mesh==null){Toast.makeText(this,"Önce SOLID MESH oluştur",Toast.LENGTH_SHORT).show();return;}showViewer(mesh);}
    void showViewer(Mesh m){viewerOpen=true;if(session!=null)try{session.pause();}catch(Throwable ignored){}getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);MeshView view=new MeshView(m);root.addView(view,new FrameLayout.LayoutParams(-1,-1));LinearLayout bar=new LinearLayout(this);Button close=new Button(this);close.setText("← TARAMA");close.setOnClickListener(v->closeViewer());Button center=new Button(this);center.setText("MERKEZ");center.setOnClickListener(v->view.reset());Button surf=new Button(this);surf.setText("YÜZEY");surf.setOnClickListener(v->view.mode=0);Button point=new Button(this);point.setText("NOKTA");point.setOnClickListener(v->view.mode=1);Button front=new Button(this);front.setText("ÖN");front.setOnClickListener(v->view.preset(0));Button right=new Button(this);right.setText("SAĞ");right.setOnClickListener(v->view.preset(1));Button top=new Button(this);top.setText("ÜST");top.setOnClickListener(v->view.preset(2));bar.addView(close);bar.addView(center);bar.addView(surf);bar.addView(point);bar.addView(front);bar.addView(right);bar.addView(top);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);bp.setMargins(8,8,8,0);root.addView(bar,bp);TextView hint=tv("DOLU ÜÇGEN YÜZEY • PARMAKLA 360° DÖNDÜR • İKİ PARMAKLA ZOOM",13,Color.WHITE);hint.setGravity(Gravity.CENTER);hint.setBackgroundColor(0xaa000000);FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);hp.setMargins(20,0,20,16);root.addView(hint,hp);setContentView(root);}
    void closeViewer(){getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);buildUi();viewerOpen=false;if(session!=null)try{session.resume();}catch(Throwable ignored){}if(cameraGl!=null)cameraGl.onResume();}

    void reset(){Arrays.fill(photos,null);Arrays.fill(photoPoses,null);Arrays.fill(captured,false);Arrays.fill(approved,false);Arrays.fill(videoBestError,999);mesh=null;currentFace=0;captureRequested=false;videoScanning=false;newProject();progress.setProgress(0);modelInfo.setText("3D model: hazır değil");setMode(videoMode);refresh();}
    void refresh(){if(faceState==null||faceState[0]==null)return;for(int i=0;i<4;i++){faceState[i].setText((approved[i]?"✓ ":captured[i]?"● ":"○ ")+FACES[i]);faceState[i].setTextColor(captured[i]?0xff4bd37b:0xffe7edf3);}boolean all=captured[0]&&captured[1]&&captured[2]&&captured[3];if(!videoMode){boolean hc=captured[currentFace];shootBtn.setEnabled(!hc);approveBtn.setEnabled(hc&&!approved[currentFace]);retryBtn.setEnabled(hc&&!approved[currentFace]);}buildBtn.setEnabled(all);viewerBtn.setEnabled(mesh!=null);if(videoMode)videoInfo.setText("Video sektörleri: "+countCaptured()+"/4"+(videoScanning?" • KAYIT AKTİF":""));}

    void writeFile(String name,String mime,byte[] data,String folder)throws Exception{ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,name);v.put(MediaStore.Downloads.MIME_TYPE,mime);v.put(MediaStore.Downloads.RELATIVE_PATH,folder);Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)throw new Exception("dosya açılamadı");try(OutputStream o=getContentResolver().openOutputStream(u)){if(o==null)throw new Exception("dosya yazılamadı");o.write(data);}}
    void ui(String s){runOnUiThread(()->{if(status!=null)status.setText(s);});}

    @Override protected void onResume(){super.onResume();if(!viewerOpen){if(session==null&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)setupAr();if(session!=null)try{session.resume();}catch(CameraNotAvailableException e){if(status!=null)status.setText("Kamera açılamadı");}if(cameraGl!=null)cameraGl.onResume();}}
    @Override protected void onPause(){if(!viewerOpen){if(cameraGl!=null)cameraGl.onPause();if(session!=null)try{session.pause();}catch(Throwable ignored){}}super.onPause();}
    @Override protected void onDestroy(){if(session!=null)session.close();super.onDestroy();}
    @Override public void onBackPressed(){if(viewerOpen)closeViewer();else super.onBackPressed();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)setupAr();}

    static class Mesh{final float[] pos,nor;final int triangles;Mesh(float[] p,float[] n,int t){pos=p;nor=n;triangles=t;}}

    class MeshView extends GLSurfaceView implements GLSurfaceView.Renderer,View.OnTouchListener{
        Mesh m;FloatBuffer pb,nb;int prog,aP,aN,uMvp,uModel,uMode;float rx=-18,ry=25,zoom=3.1f,lx,ly;ScaleGestureDetector scale;volatile int mode=0;
        MeshView(Mesh mm){super(MainActivityV19.this);m=mm;setEGLContextClientVersion(2);setRenderer(this);setRenderMode(RENDERMODE_CONTINUOUSLY);setOnTouchListener(this);pb=fb(m.pos);nb=fb(m.nor);scale=new ScaleGestureDetector(MainActivityV19.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoom/=d.getScaleFactor();zoom=Math.max(1.4f,Math.min(8f,zoom));return true;}});}
        @Override public void onSurfaceCreated(GL10 g,EGLConfig c){GLES20.glClearColor(.015f,.018f,.023f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_CULL_FACE);String vs="uniform mat4 uMvp;uniform mat4 uModel;attribute vec3 aP;attribute vec3 aN;varying vec3 n;varying vec3 p;void main(){vec4 wp=uModel*vec4(aP,1.0);p=wp.xyz;n=normalize(mat3(uModel)*aN);gl_Position=uMvp*vec4(aP,1.0);gl_PointSize=3.0;}";String fs="precision mediump float;varying vec3 n;varying vec3 p;uniform float uMode;void main(){vec3 L=normalize(vec3(.45,.7,1.0));float d=max(.15,dot(normalize(n),L));vec3 base=vec3(.15,.62,.82);if(uMode>0.5)base=vec3(.15,.85,1.0);gl_FragColor=vec4(base*(.35+.8*d),1.0);}";prog=GLES20.glCreateProgram();GLES20.glAttachShader(prog,sh(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(prog,sh(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(prog);aP=GLES20.glGetAttribLocation(prog,"aP");aN=GLES20.glGetAttribLocation(prog,"aN");uMvp=GLES20.glGetUniformLocation(prog,"uMvp");uModel=GLES20.glGetUniformLocation(prog,"uModel");uMode=GLES20.glGetUniformLocation(prog,"uMode");}
        @Override public void onSurfaceChanged(GL10 g,int w,int h){GLES20.glViewport(0,0,w,h);}
        @Override public void onDrawFrame(GL10 g){GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);int w=getWidth(),h=Math.max(1,getHeight());float[] pr=new float[16],vw=new float[16],mo=new float[16],tmp=new float[16],mvp=new float[16];Matrix.perspectiveM(pr,0,42,(float)w/h,.1f,50);Matrix.setLookAtM(vw,0,0,0,zoom,0,0,0,0,1,0);Matrix.setIdentityM(mo,0);Matrix.rotateM(mo,0,rx,1,0,0);Matrix.rotateM(mo,0,ry,0,1,0);Matrix.multiplyMM(tmp,0,vw,0,mo,0);Matrix.multiplyMM(mvp,0,pr,0,tmp,0);GLES20.glUseProgram(prog);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,mo,0);GLES20.glUniform1f(uMode,mode);pb.position(0);nb.position(0);GLES20.glVertexAttribPointer(aP,3,GLES20.GL_FLOAT,false,0,pb);GLES20.glEnableVertexAttribArray(aP);GLES20.glVertexAttribPointer(aN,3,GLES20.GL_FLOAT,false,0,nb);GLES20.glEnableVertexAttribArray(aN);if(mode==0)GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,m.pos.length/3);else GLES20.glDrawArrays(GLES20.GL_POINTS,0,m.pos.length/3);}
        @Override public boolean onTouch(View v,MotionEvent e){scale.onTouchEvent(e);if(e.getPointerCount()==1&&!scale.isInProgress()){if(e.getAction()==MotionEvent.ACTION_DOWN){lx=e.getX();ly=e.getY();}else if(e.getAction()==MotionEvent.ACTION_MOVE){ry+=(e.getX()-lx)*.35f;rx+=(e.getY()-ly)*.35f;lx=e.getX();ly=e.getY();}}return true;}
        void reset(){rx=-18;ry=25;zoom=3.1f;}void preset(int p){if(p==0){rx=0;ry=0;}else if(p==1){rx=0;ry=90;}else{rx=-90;ry=0;}zoom=3.1f;}
    }

    static FloatBuffer fb(float[] a){ByteBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder());FloatBuffer f=b.asFloatBuffer();f.put(a).position(0);return f;}
    static int sh(int t,String s){int x=GLES20.glCreateShader(t);GLES20.glShaderSource(x,s);GLES20.glCompileShader(x);return x;}

    static class CameraBackground{
        int textureId,prog,aP,aU,uT;FloatBuffer vb=fb(new float[]{-1,-1,1,-1,-1,1,1,1}),tb=fb(new float[]{0,1,1,1,0,0,1,0});
        void create(){int[] t=new int[1];GLES20.glGenTextures(1,t,0);textureId=t[0];GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);String vs="attribute vec2 a;attribute vec2 b;varying vec2 v;void main(){gl_Position=vec4(a,0.,1.);v=b;}";String fs="#extension GL_OES_EGL_image_external : require\nprecision mediump float;uniform samplerExternalOES s;varying vec2 v;void main(){gl_FragColor=texture2D(s,v);}";prog=GLES20.glCreateProgram();GLES20.glAttachShader(prog,sh(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(prog,sh(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(prog);aP=GLES20.glGetAttribLocation(prog,"a");aU=GLES20.glGetAttribLocation(prog,"b");uT=GLES20.glGetUniformLocation(prog,"s");}
        void draw(Frame f){float[] in={-1,-1,1,-1,-1,1,1,1},out=new float[8];try{f.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,in,Coordinates2d.TEXTURE_NORMALIZED,out);tb=fb(out);}catch(Throwable ignored){}GLES20.glDisable(GLES20.GL_DEPTH_TEST);GLES20.glUseProgram(prog);vb.position(0);tb.position(0);GLES20.glVertexAttribPointer(aP,2,GLES20.GL_FLOAT,false,0,vb);GLES20.glEnableVertexAttribArray(aP);GLES20.glVertexAttribPointer(aU,2,GLES20.GL_FLOAT,false,0,tb);GLES20.glEnableVertexAttribArray(aU);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glUniform1i(uT,0);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);}
    }
}
