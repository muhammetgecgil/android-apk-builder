package com.muhammetgecgil.camera3d;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity implements GLSurfaceView.Renderer {
    static final int REQ=71;
    static final String[] FACES={"ÖN","SAĞ","ARKA","SOL"};

    GLSurfaceView gl; Session session; BackgroundRenderer bg;
    TextView title,status,instruction,photoStatus,pointsText;
    Button shootBtn,approveBtn,retryBtn,buildBtn,newProjectBtn;
    TextView[] faceState=new TextView[4];

    final ArrayList<float[]> points=new ArrayList<>();
    final HashSet<Long> voxels=new HashSet<>();
    final boolean[] captured=new boolean[4];
    final boolean[] approved=new boolean[4];
    final String[] photoNames=new String[4];
    final Pose[] photoPoses=new Pose[4];

    volatile boolean captureRequested=false;
    volatile int currentFace=0;
    long lastCollect=0;
    String projectId="";
    String cameraInfo="";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        newProjectId(); buildUi();
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.CAMERA},REQ); else setupAr();
    }

    void newProjectId(){projectId="Scan_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());}

    TextView text(String s,float sp,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);t.setPadding(8,5,8,5);return t;}

    void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff080b10);
        LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout cameraBox=new FrameLayout(this);
        gl=new GLSurfaceView(this);gl.setEGLContextClientVersion(2);gl.setPreserveEGLContextOnPause(true);gl.setRenderer(this);gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);cameraBox.addView(gl,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(16,10,16,10);top.setBackgroundColor(0x99101820);
        title=text("CAMERA 3D • FOTOĞRAFLI TARAMA 1.6",18,Color.WHITE);
        status=text("Kamera hazırlanıyor…",13,0xffd8e2ef);pointsText=text("3D destek noktası: 0",12,0xff7fdaff);
        top.addView(title);top.addView(status);top.addView(pointsText);
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);tp.setMargins(10,10,10,0);cameraBox.addView(top,tp);

        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(16,10,16,12);bottom.setBackgroundColor(0xbb101820);
        instruction=text("1/4 • ÖN FOTOĞRAFI ÇEK",18,Color.WHITE);photoStatus=text("Fotoğraf çekilmedi",13,0xffc7d3df);
        bottom.addView(instruction);bottom.addView(photoStatus);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);bp.setMargins(10,0,10,10);cameraBox.addView(bottom,bp);

        LinearLayout side=new LinearLayout(this);side.setOrientation(LinearLayout.VERTICAL);side.setPadding(14,14,14,14);side.setBackgroundColor(0xff121820);
        side.addView(text("4 FOTO → 3D",22,Color.WHITE));
        side.addView(text("Sıra: ÖN → SAĞ → ARKA → SOL. Fotoğrafı çektikten sonra sen ONAYLA demeden diğer yüze geçmez.",12,0xffaebdca));
        for(int i=0;i<4;i++){faceState[i]=text("○ "+FACES[i],15,0xffe6edf3);side.addView(faceState[i]);}

        shootBtn=new Button(this);shootBtn.setText("FOTOĞRAF ÇEK");shootBtn.setOnClickListener(v->requestCapture());side.addView(shootBtn);
        approveBtn=new Button(this);approveBtn.setText("FOTOĞRAFI ONAYLA");approveBtn.setEnabled(false);approveBtn.setOnClickListener(v->approvePhoto());side.addView(approveBtn);
        retryBtn=new Button(this);retryBtn.setText("TEKRAR ÇEK");retryBtn.setEnabled(false);retryBtn.setOnClickListener(v->retryPhoto());side.addView(retryBtn);
        buildBtn=new Button(this);buildBtn.setText("4 FOTOĞRAFI 3D DATA YAP");buildBtn.setEnabled(false);buildBtn.setOnClickListener(v->build3D());side.addView(buildBtn);
        newProjectBtn=new Button(this);newProjectBtn.setText("YENİ PROJE");newProjectBtn.setOnClickListener(v->resetProject());side.addView(newProjectBtn);
        side.addView(text("Çıktı: 4 kaynak JPEG + kamera pozları + PLY/OBJ 3D nokta verisi. Bu sürüm kaba 3D prototiptir; hassas CAD mesh sonraki aşamadır.",11,0xff8fa1b3));

        main.addView(cameraBox,new LinearLayout.LayoutParams(0,-1,4.5f));main.addView(side,new LinearLayout.LayoutParams(0,-1,1.7f));root.addView(main,new FrameLayout.LayoutParams(-1,-1));setContentView(root);refreshUi();
    }

    void setupAr(){
        try{
            if(session!=null)return;
            if(ArCoreApk.getInstance().checkAvailability(this).isUnsupported()){status.setText("ARCore desteklenmiyor");return;}
            session=new Session(this);
            CameraConfigFilter filter=new CameraConfigFilter(session);filter.setFacingDirection(CameraConfig.FacingDirection.BACK);filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
            List<CameraConfig> cfgs=session.getSupportedCameraConfigs(filter);CameraConfig best=null;long area=-1;
            for(CameraConfig c:cfgs){long a=(long)c.getCpuImageSize().getWidth()*c.getCpuImageSize().getHeight();if(a>area){area=a;best=c;}}
            if(best!=null){session.setCameraConfig(best);cameraInfo=best.getCpuImageSize().getWidth()+"×"+best.getCpuImageSize().getHeight();}
            Config c=new Config(session);c.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);c.setFocusMode(Config.FocusMode.AUTO);session.configure(c);
            status.setText("Kamera hazır"+(cameraInfo.isEmpty()?"":" • foto "+cameraInfo));
        }catch(Throwable t){status.setText("ARCore: "+t.getClass().getSimpleName());}
    }

    void requestCapture(){
        if(session==null){setupAr();if(session==null)return;}
        if(captured[currentFace]){Toast.makeText(this,"Önce ONAYLA veya TEKRAR ÇEK",Toast.LENGTH_SHORT).show();return;}
        captureRequested=true;shootBtn.setEnabled(false);photoStatus.setText(FACES[currentFace]+" fotoğrafı çekiliyor… telefonu sabit tut");
    }

    void approvePhoto(){
        if(!captured[currentFace])return;approved[currentFace]=true;
        if(currentFace<3){currentFace++;approveBtn.setEnabled(false);retryBtn.setEnabled(false);shootBtn.setEnabled(true);photoStatus.setText("Fotoğraf çekilmedi");instruction.setText((currentFace+1)+"/4 • "+FACES[currentFace]+" FOTOĞRAFI ÇEK");}
        else {approveBtn.setEnabled(false);retryBtn.setEnabled(false);shootBtn.setEnabled(false);instruction.setText("4/4 TAMAM • 3D DATA OLUŞTUR");photoStatus.setText("Dört fotoğraf onaylandı");buildBtn.setEnabled(true);}
        refreshUi();
    }

    void retryPhoto(){captured[currentFace]=false;approved[currentFace]=false;photoNames[currentFace]=null;photoPoses[currentFace]=null;approveBtn.setEnabled(false);retryBtn.setEnabled(false);shootBtn.setEnabled(true);photoStatus.setText(FACES[currentFace]+" fotoğrafını tekrar çek");refreshUi();}

    void resetProject(){
        synchronized(this){points.clear();voxels.clear();}
        for(int i=0;i<4;i++){captured[i]=false;approved[i]=false;photoNames[i]=null;photoPoses[i]=null;}
        currentFace=0;captureRequested=false;newProjectId();shootBtn.setEnabled(true);approveBtn.setEnabled(false);retryBtn.setEnabled(false);buildBtn.setEnabled(false);instruction.setText("1/4 • ÖN FOTOĞRAFI ÇEK");photoStatus.setText("Fotoğraf çekilmedi");refreshUi();
    }

    void refreshUi(){for(int i=0;i<4;i++){String p=approved[i]?"✓ ":captured[i]?"● ":(i==currentFace?"▶ ":"○ ");faceState[i].setText(p+FACES[i]);faceState[i].setTextColor(approved[i]?0xff4bd37b:(i==currentFace?0xffffc857:0xffe6edf3));}}

    @Override public void onSurfaceCreated(GL10 g,EGLConfig e){bg=new BackgroundRenderer();bg.create();GLES20.glClearColor(0,0,0,1);}
    @Override public void onSurfaceChanged(GL10 g,int w,int h){GLES20.glViewport(0,0,w,h);if(session!=null)session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(),w,h);}
    @Override public void onDrawFrame(GL10 g){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);if(session==null||bg==null)return;
        try{
            session.setCameraTextureName(bg.textureId);Frame f=session.update();bg.draw(f);com.google.ar.core.Camera cam=f.getCamera();
            if(cam.getTrackingState()!=TrackingState.TRACKING){uiStatus("Kamera açık • AR takibi için telefonu biraz hareket ettir");return;}
            collect3D(f);
            if(captureRequested){tryCapture(f,cam.getPose());}
        }catch(Throwable t){uiStatus("Kamera/AR hatası: "+t.getClass().getSimpleName());}
    }

    void collect3D(Frame f){long now=System.currentTimeMillis();if(now-lastCollect<80)return;lastCollect=now;try(PointCloud pc=f.acquirePointCloud()){FloatBuffer b=pc.getPoints();int n=b.remaining()/4,step=Math.max(1,n/900);synchronized(this){for(int i=0;i<n;i+=step){int q=i*4;float x=b.get(q),y=b.get(q+1),z=b.get(q+2),c=b.get(q+3);if(c<.25f||Math.abs(x)>6||Math.abs(y)>6||Math.abs(z)>6)continue;long k=key(x,y,z,.006f);if(voxels.add(k))points.add(new float[]{x,y,z,c});}}int count=points.size();runOnUiThread(()->pointsText.setText("3D destek noktası: "+count));}catch(Throwable ignored){}
    }

    void tryCapture(Frame f,Pose pose){
        Image img=null;
        try{
            img=f.acquireCameraImage();byte[] jpeg=yuv420ToJpeg(img,94);String name=projectId+"_"+(currentFace+1)+"_"+FACES[currentFace]+".jpg";writeFile(name,"image/jpeg",jpeg,"Download/Camera3D/"+projectId);
            photoNames[currentFace]=name;photoPoses[currentFace]=pose;captured[currentFace]=true;captureRequested=false;
            int face=currentFace;runOnUiThread(()->{shootBtn.setEnabled(false);approveBtn.setEnabled(true);retryBtn.setEnabled(true);photoStatus.setText(FACES[face]+" fotoğrafı çekildi • kontrol et, uygunsa ONAYLA");refreshUi();});
        }catch(Throwable t){captureRequested=false;runOnUiThread(()->{shootBtn.setEnabled(true);photoStatus.setText("Fotoğraf alınamadı • tekrar dene");});}
        finally{if(img!=null)img.close();}
    }

    byte[] yuv420ToJpeg(Image image,int quality)throws Exception{
        int w=image.getWidth(),h=image.getHeight();Image.Plane[] p=image.getPlanes();byte[] nv21=new byte[w*h*3/2];
        copyPlane(p[0],w,h,nv21,0,1);
        ByteBuffer ub=p[1].getBuffer(),vb=p[2].getBuffer();int ur=p[1].getRowStride(),vr=p[2].getRowStride(),up=p[1].getPixelStride(),vp=p[2].getPixelStride();int off=w*h;
        for(int y=0;y<h/2;y++)for(int x=0;x<w/2;x++){int vi=y*vr+x*vp,ui=y*ur+x*up;nv21[off++]=vb.get(vi);nv21[off++]=ub.get(ui);}
        YuvImage yi=new YuvImage(nv21,ImageFormat.NV21,w,h,null);ByteArrayOutputStream out=new ByteArrayOutputStream();yi.compressToJpeg(new Rect(0,0,w,h),quality,out);return out.toByteArray();
    }

    void copyPlane(Image.Plane plane,int width,int height,byte[] out,int offset,int outPixelStride){ByteBuffer b=plane.getBuffer();int row=plane.getRowStride(),pix=plane.getPixelStride();for(int y=0;y<height;y++)for(int x=0;x<width;x++){int idx=y*row+x*pix;if(idx<b.limit())out[offset+(y*width+x)*outPixelStride]=b.get(idx);}}

    void build3D(){
        if(!(approved[0]&&approved[1]&&approved[2]&&approved[3]))return;
        ArrayList<float[]> cp; synchronized(this){cp=new ArrayList<>(points);}if(cp.size()<20){Toast.makeText(this,"3D nokta az • nesnenin çevresinde biraz daha hareket et",Toast.LENGTH_LONG).show();return;}
        new Thread(()->{try{String folder="Download/Camera3D/"+projectId;writeFile(projectId+".ply","application/octet-stream",ply(cp),folder);writeFile(projectId+".obj","text/plain",obj(cp),folder);writeFile(projectId+"_poses.txt","text/plain",posesText().getBytes(StandardCharsets.UTF_8),folder);runOnUiThread(()->Toast.makeText(this,"4 foto + 3D data kaydedildi: "+projectId,Toast.LENGTH_LONG).show());}catch(Throwable e){runOnUiThread(()->Toast.makeText(this,"3D kayıt hatası",Toast.LENGTH_LONG).show());}}).start();
    }

    String posesText(){StringBuilder s=new StringBuilder("Camera3D Photo Project\n");for(int i=0;i<4;i++){Pose p=photoPoses[i];s.append(FACES[i]).append(" photo=").append(photoNames[i]).append('\n');if(p!=null){float[] t=p.getTranslation(),q=p.getRotationQuaternion();s.append("t ").append(t[0]).append(' ').append(t[1]).append(' ').append(t[2]).append('\n');s.append("q ").append(q[0]).append(' ').append(q[1]).append(' ').append(q[2]).append(' ').append(q[3]).append('\n');}}return s.toString();}
    byte[] ply(ArrayList<float[]>p){StringBuilder s=new StringBuilder("ply\nformat ascii 1.0\nelement vertex "+p.size()+"\nproperty float x\nproperty float y\nproperty float z\nend_header\n");for(float[]v:p)s.append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');return s.toString().getBytes(StandardCharsets.UTF_8);}
    byte[] obj(ArrayList<float[]>p){StringBuilder s=new StringBuilder("# Camera3D 4-photo point data\n");for(float[]v:p)s.append("v ").append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');return s.toString().getBytes(StandardCharsets.UTF_8);}
    void writeFile(String name,String mime,byte[] data,String path)throws Exception{ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,name);v.put(MediaStore.Downloads.MIME_TYPE,mime);v.put(MediaStore.Downloads.RELATIVE_PATH,path);Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(data);}}
    long key(float x,float y,float z,float c){long a=(long)Math.floor(x/c)+1048576,b=(long)Math.floor(y/c)+1048576,d=(long)Math.floor(z/c)+1048576;return((a&0x1fffffL)<<42)|((b&0x1fffffL)<<21)|(d&0x1fffffL);}
    void uiStatus(String s){runOnUiThread(()->status.setText(s+(cameraInfo.isEmpty()?"":" • "+cameraInfo)));}

    @Override protected void onResume(){super.onResume();if(session==null&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)setupAr();if(session!=null)try{session.resume();}catch(CameraNotAvailableException ignored){}if(gl!=null)gl.onResume();}
    @Override protected void onPause(){if(gl!=null)gl.onPause();if(session!=null)session.pause();super.onPause();}
    @Override protected void onDestroy(){if(session!=null)session.close();super.onDestroy();}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){setupAr();try{session.resume();}catch(Throwable ignored){}}}

    static class BackgroundRenderer{
        int textureId=-1,program,pos,tex,uni;FloatBuffer vb,tb;final float[] ndc={-1,-1,1,-1,-1,1,1,1};final float[] uv=new float[8];
        void create(){int[]t=new int[1];GLES20.glGenTextures(1,t,0);textureId=t[0];GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);vb=buf(ndc);tb=buf(new float[]{0,1,1,1,0,0,1,0});String vs="attribute vec2 a;attribute vec2 b;varying vec2 v;void main(){gl_Position=vec4(a,0.,1.);v=b;}";String fs="#extension GL_OES_EGL_image_external : require\nprecision highp float;uniform samplerExternalOES s;varying vec2 v;void main(){gl_FragColor=texture2D(s,v);}";program=GLES20.glCreateProgram();int a=shader(GLES20.GL_VERTEX_SHADER,vs),b=shader(GLES20.GL_FRAGMENT_SHADER,fs);GLES20.glAttachShader(program,a);GLES20.glAttachShader(program,b);GLES20.glLinkProgram(program);pos=GLES20.glGetAttribLocation(program,"a");tex=GLES20.glGetAttribLocation(program,"b");uni=GLES20.glGetUniformLocation(program,"s");}
        void draw(Frame f){f.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,ndc,Coordinates2d.TEXTURE_NORMALIZED,uv);tb.position(0);tb.put(uv);tb.position(0);GLES20.glDisable(GLES20.GL_DEPTH_TEST);GLES20.glUseProgram(program);vb.position(0);GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,0,vb);GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(tex,2,GLES20.GL_FLOAT,false,0,tb);GLES20.glEnableVertexAttribArray(tex);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glUniform1i(uni,0);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);}
        static FloatBuffer buf(float[]a){ByteBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder());FloatBuffer f=b.asFloatBuffer();f.put(a).position(0);return f;}
        static int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
    }
}
