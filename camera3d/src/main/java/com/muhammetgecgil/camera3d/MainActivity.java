package com.muhammetgecgil.camera3d;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
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
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

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
    static final int REQ = 71;
    static final String[] FACE_NAMES = {"ÖN", "SAĞ", "ARKA", "SOL"};

    GLSurfaceView gl;
    Session session;
    BackgroundRenderer bg;
    TextView title, status, guide, pointText, selectedText, reviewText;
    ProgressBar faceProgress, totalProgress;
    Button scanBtn, finishFaceBtn, rescanBtn, exportBtn;
    Button[] faceBtns = new Button[4];

    final ArrayList<float[]> pts = new ArrayList<>();
    final HashSet<Long> vox = new HashSet<>();
    final boolean[] done = new boolean[4];
    final int[] faceFrames = new int[4];
    final int[] faceNewPoints = new int[4];

    volatile boolean scanning = false;
    volatile boolean reviewMode = false;
    int selectedFace = 0;
    long lastUi = 0;
    String cameraInfo = "";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        buildUi();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ);
        else setupAr();
    }

    private TextView label(String text, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(sp); t.setTextColor(color);
        return t;
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);

        FrameLayout cameraArea = new FrameLayout(this);
        gl = new GLSurfaceView(this);
        gl.setEGLContextClientVersion(2);
        gl.setPreserveEGLContextOnPause(true);
        gl.setRenderer(this);
        gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        cameraArea.addView(gl, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout cameraTop = new LinearLayout(this);
        cameraTop.setOrientation(LinearLayout.VERTICAL);
        cameraTop.setPadding(18, 10, 18, 10);
        cameraTop.setBackgroundColor(0x99101820);
        title = label("CAD SCAN • HQ", 18, Color.WHITE);
        status = label("Kamera hazırlanıyor…", 13, 0xffd8e2ef);
        pointText = label("3D nokta: 0", 13, 0xff7fdaff);
        cameraTop.addView(title); cameraTop.addView(status); cameraTop.addView(pointText);
        FrameLayout.LayoutParams ctp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        ctp.setMargins(10, 10, 10, 0);
        cameraArea.addView(cameraTop, ctp);

        LinearLayout cameraBottom = new LinearLayout(this);
        cameraBottom.setOrientation(LinearLayout.VERTICAL);
        cameraBottom.setPadding(14, 10, 14, 12);
        cameraBottom.setBackgroundColor(0xaa101820);
        selectedText = label("SEÇİLİ YÜZ: ÖN", 16, Color.WHITE);
        guide = label("ÖN yüzü seçili. Taramayı başlat ve telefonu yavaş hareket ettir.", 13, 0xffd8e2ef);
        faceProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        faceProgress.setMax(100);
        cameraBottom.addView(selectedText); cameraBottom.addView(guide); cameraBottom.addView(faceProgress);
        FrameLayout.LayoutParams cbp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        cbp.setMargins(10, 0, 10, 10);
        cameraArea.addView(cameraBottom, cbp);

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(14, 14, 14, 14);
        side.setBackgroundColor(0xff121820);

        TextView faceTitle = label("YÜZ SEÇ", 20, Color.WHITE);
        TextView faceHelp = label("İstediğin yüzü kendin seç. Uygulama otomatik geçmez.", 12, 0xffaebdca);
        side.addView(faceTitle); side.addView(faceHelp);

        for (int i=0;i<4;i++) {
            final int fi=i;
            faceBtns[i] = new Button(this);
            faceBtns[i].setText(FACE_NAMES[i]);
            faceBtns[i].setAllCaps(false);
            faceBtns[i].setOnClickListener(v -> selectFace(fi));
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, 0, 1f);
            fp.setMargins(0, 6, 0, 6);
            side.addView(faceBtns[i], fp);
        }

        reviewText = label("Durum: ÖN bekliyor", 13, 0xffd8e2ef);
        side.addView(reviewText);

        totalProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        totalProgress.setMax(100);
        side.addView(totalProgress);

        scanBtn = new Button(this);
        scanBtn.setText("TARAMAYI BAŞLAT");
        scanBtn.setOnClickListener(v -> toggleScan());
        side.addView(scanBtn);

        finishFaceBtn = new Button(this);
        finishFaceBtn.setText("YÜZÜ TAMAMLA • KONTROL ET");
        finishFaceBtn.setEnabled(false);
        finishFaceBtn.setOnClickListener(v -> finishSelectedFace());
        side.addView(finishFaceBtn);

        rescanBtn = new Button(this);
        rescanBtn.setText("BU YÜZÜ TEKRAR TARA");
        rescanBtn.setVisibility(View.GONE);
        rescanBtn.setOnClickListener(v -> rescanSelectedFace());
        side.addView(rescanBtn);

        exportBtn = new Button(this);
        exportBtn.setText("CAD VERİSİ • PLY + OBJ");
        exportBtn.setEnabled(false);
        exportBtn.setOnClickListener(v -> save());
        side.addView(exportBtn);

        main.addView(cameraArea, new LinearLayout.LayoutParams(0, -1, 4.4f));
        main.addView(side, new LinearLayout.LayoutParams(0, -1, 1.6f));
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        refreshFaceButtons();
    }

    private void selectFace(int face) {
        scanning = false;
        reviewMode = done[face];
        selectedFace = face;
        scanBtn.setText(done[face] ? "YÜZÜ İNCELE" : "TARAMAYI BAŞLAT");
        finishFaceBtn.setEnabled(!done[face] && faceFrames[face] > 10);
        rescanBtn.setVisibility(done[face] ? View.VISIBLE : View.GONE);
        selectedText.setText("SEÇİLİ YÜZ: " + FACE_NAMES[face]);
        if (done[face]) {
            guide.setText(FACE_NAMES[face] + " tamamlandı. Kamera üzerinde kontrol et. Beğenmezsen TEKRAR TARA.");
            reviewText.setText("Durum: " + FACE_NAMES[face] + " tamamlandı • kontrol modu");
        } else {
            guide.setText(FACE_NAMES[face] + " yüzü seçili. Taramayı başlat ve yalnız bu yüzü göster.");
            reviewText.setText("Durum: " + FACE_NAMES[face] + " bekliyor");
        }
        updateFaceProgressUi();
        refreshFaceButtons();
    }

    private void toggleScan() {
        if (session == null) { setupAr(); if (session == null) return; }
        if (done[selectedFace]) {
            reviewMode = true;
            scanning = false;
            guide.setText(FACE_NAMES[selectedFace] + " tamamlandı. Canlı görüntüde kontrol ediyorsun.");
            return;
        }
        reviewMode = false;
        scanning = !scanning;
        scanBtn.setText(scanning ? "TARAMAYI DURAKLAT" : "TARAMAYA DEVAM");
        guide.setText(scanning ? FACE_NAMES[selectedFace] + " taranıyor • yavaşça sağa/sola hareket et" : "Tarama duraklatıldı • görüntüyü kontrol et");
        reviewText.setText(scanning ? "Durum: veri toplanıyor" : "Durum: duraklatıldı");
    }

    private void finishSelectedFace() {
        scanning = false;
        reviewMode = true;
        done[selectedFace] = true;
        scanBtn.setText("YÜZÜ İNCELE");
        finishFaceBtn.setEnabled(false);
        rescanBtn.setVisibility(View.VISIBLE);
        guide.setText(FACE_NAMES[selectedFace] + " TAMAMLANDI. Sonraki yüzeye geçmiyorum; önce kontrol et.");
        reviewText.setText("Durum: " + FACE_NAMES[selectedFace] + " tamamlandı • KONTROL ET");
        refreshFaceButtons();
        updateTotalProgressUi();
        exportBtn.setEnabled(doneCount() >= 2 && pts.size() > 100);
    }

    private void rescanSelectedFace() {
        done[selectedFace] = false;
        faceFrames[selectedFace] = 0;
        faceNewPoints[selectedFace] = 0;
        scanning = false;
        reviewMode = false;
        rescanBtn.setVisibility(View.GONE);
        scanBtn.setText("TARAMAYI BAŞLAT");
        finishFaceBtn.setEnabled(false);
        guide.setText(FACE_NAMES[selectedFace] + " sıfırlandı. Aynı yüzü yeniden tara.");
        reviewText.setText("Durum: yeniden tarama bekliyor");
        refreshFaceButtons();
        updateFaceProgressUi(); updateTotalProgressUi();
    }

    private void refreshFaceButtons() {
        for (int i=0;i<4;i++) {
            String prefix = done[i] ? "✓ " : (i==selectedFace ? "▶ " : "");
            faceBtns[i].setText(prefix + FACE_NAMES[i]);
            faceBtns[i].setAlpha(i==selectedFace ? 1f : 0.82f);
        }
    }

    private int doneCount(){ int n=0; for(boolean b:done) if(b)n++; return n; }

    private void updateFaceProgressUi() {
        int p = Math.min(100, faceFrames[selectedFace] * 2);
        faceProgress.setProgress(done[selectedFace] ? 100 : p);
    }

    private void updateTotalProgressUi() {
        int partial = done[selectedFace] ? 0 : Math.min(25, faceFrames[selectedFace]/2);
        totalProgress.setProgress(Math.min(100, doneCount()*25 + partial));
    }

    private void setupAr() {
        try {
            if (session != null) return;
            if (ArCoreApk.getInstance().checkAvailability(this).isUnsupported()) {
                status.setText("ARCore desteklenmiyor"); return;
            }
            session = new Session(this);
            CameraConfigFilter filter = new CameraConfigFilter(session);
            filter.setFacingDirection(CameraConfig.FacingDirection.BACK);
            filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
            List<CameraConfig> configs = session.getSupportedCameraConfigs(filter);
            CameraConfig best = null; long bestArea = -1;
            for (CameraConfig cc : configs) {
                long a = (long)cc.getTextureSize().getWidth()*cc.getTextureSize().getHeight();
                if (a > bestArea) { bestArea = a; best = cc; }
            }
            if (best != null) {
                session.setCameraConfig(best);
                cameraInfo = best.getTextureSize().getWidth()+"×"+best.getTextureSize().getHeight();
            }
            Config c = new Config(session);
            c.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            c.setFocusMode(Config.FocusMode.AUTO);
            session.configure(c);
            status.setText("HQ kamera hazır" + (cameraInfo.isEmpty()?"":" • "+cameraInfo));
        } catch (Throwable t) { status.setText("ARCore: "+t.getClass().getSimpleName()); }
    }

    @Override public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        bg = new BackgroundRenderer(); bg.create(); GLES20.glClearColor(0,0,0,1);
    }

    @Override public void onSurfaceChanged(GL10 gl10, int w, int h) {
        GLES20.glViewport(0,0,w,h);
        if(session!=null) session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(),w,h);
    }

    @Override public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if(session==null || bg==null) return;
        try {
            session.setCameraTextureName(bg.textureId);
            Frame f=session.update(); bg.draw(f);
            com.google.ar.core.Camera cam=f.getCamera();
            if(cam.getTrackingState()!=TrackingState.TRACKING){uiStatus("Kamera açık • telefonu yavaş hareket ettir");return;}
            if(scanning) collect(f);
            else if(reviewMode) uiStatus("Kontrol modu • "+FACE_NAMES[selectedFace]+" yüzünü incele");
            else uiStatus("HQ canlı görüntü"+(cameraInfo.isEmpty()?"":" • "+cameraInfo));
        } catch(Throwable t){ uiStatus("Kamera/AR hatası: "+t.getClass().getSimpleName()); }
    }

    private void collect(Frame f) {
        int added=0;
        try(PointCloud pc=f.acquirePointCloud()) {
            FloatBuffer b=pc.getPoints(); int n=b.remaining()/4, step=Math.max(1,n/1000);
            synchronized(this) {
                for(int i=0;i<n;i+=step){
                    int q=i*4; float X=b.get(q),Y=b.get(q+1),Z=b.get(q+2),C=b.get(q+3);
                    if(C<.22f || Math.abs(X)>6 || Math.abs(Y)>6 || Math.abs(Z)>6) continue;
                    long k=key(X,Y,Z,.006f);
                    if(vox.add(k)){pts.add(new float[]{X,Y,Z,C,selectedFace});added++;}
                }
                faceFrames[selectedFace]++;
                faceNewPoints[selectedFace]+=added;
            }
            updateLiveUi();
        } catch(Throwable ignored){}
    }

    private void updateLiveUi(){
        long now=System.currentTimeMillis(); if(now-lastUi<250)return; lastUi=now;
        int face=selectedFace, frames=faceFrames[face], pn=pts.size(), added=faceNewPoints[face];
        runOnUiThread(() -> {
            int fp=Math.min(100,frames*2); faceProgress.setProgress(fp);
            finishFaceBtn.setEnabled(frames>=12);
            pointText.setText("3D nokta: "+pn+" • bu yüz: "+added);
            reviewText.setText("Durum: "+FACE_NAMES[face]+" taranıyor • %"+fp);
            updateTotalProgressUi();
        });
    }

    private long key(float x,float y,float z,float c){
        long a=(long)Math.floor(x/c)+1048576,b=(long)Math.floor(y/c)+1048576,d=(long)Math.floor(z/c)+1048576;
        return((a&0x1fffffL)<<42)|((b&0x1fffffL)<<21)|(d&0x1fffffL);
    }

    private void uiStatus(String s){
        long n=System.currentTimeMillis(); if(n-lastUi>800){lastUi=n;runOnUiThread(()->status.setText(s));}
    }

    private void save(){
        ArrayList<float[]> cp; synchronized(this){cp=new ArrayList<>(pts);} if(cp.size()<10)return;
        new Thread(()->{
            try{
                String st=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
                write("CADScan_"+st+".ply","application/octet-stream",ply(cp));
                write("CADScan_"+st+".obj","text/plain",obj(cp));
                runOnUiThread(()->Toast.makeText(this,"PLY + OBJ kaydedildi",Toast.LENGTH_LONG).show());
            }catch(Throwable e){runOnUiThread(()->Toast.makeText(this,"Kayıt hatası",Toast.LENGTH_LONG).show());}
        }).start();
    }

    private byte[] ply(ArrayList<float[]>p){
        StringBuilder s=new StringBuilder("ply\nformat ascii 1.0\nelement vertex "+p.size()+"\nproperty float x\nproperty float y\nproperty float z\nend_header\n");
        for(float[]v:p)s.append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');
        return s.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] obj(ArrayList<float[]>p){
        StringBuilder s=new StringBuilder("# Camera3D CADScan\n");
        for(float[]v:p)s.append("v ").append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');
        return s.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void write(String n,String m,byte[]d)throws Exception{
        ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,n);v.put(MediaStore.Downloads.MIME_TYPE,m);v.put(MediaStore.Downloads.RELATIVE_PATH,"Download/Camera3D");
        Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
        try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(d);}
    }

    @Override protected void onResume(){super.onResume();if(session==null&&checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)setupAr();if(session!=null)try{session.resume();}catch(CameraNotAvailableException e){status.setText("Kamera açılamadı");}if(gl!=null)gl.onResume();}
    @Override protected void onPause(){if(gl!=null)gl.onPause();if(session!=null)session.pause();super.onPause();}
    @Override protected void onDestroy(){if(session!=null)session.close();super.onDestroy();}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){setupAr();try{session.resume();}catch(Throwable ignored){}}}

    static class BackgroundRenderer {
        int textureId=-1,program,pos,tex,coord; FloatBuffer vb,tb;
        final float[] ndc={-1,-1,1,-1,-1,1,1,1}; final float[] uv=new float[8];
        void create(){
            int[]t=new int[1];GLES20.glGenTextures(1,t,0);textureId=t[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            vb=buf(ndc);tb=buf(new float[]{0,1,1,1,0,0,1,0});
            String vs="attribute vec2 a;attribute vec2 b;varying vec2 v;void main(){gl_Position=vec4(a,0.,1.);v=b;}";
            String fs="#extension GL_OES_EGL_image_external : require\nprecision highp float;uniform samplerExternalOES s;varying vec2 v;void main(){gl_FragColor=texture2D(s,v);}";
            program=GLES20.glCreateProgram();int a=sh(GLES20.GL_VERTEX_SHADER,vs),b=sh(GLES20.GL_FRAGMENT_SHADER,fs);
            GLES20.glAttachShader(program,a);GLES20.glAttachShader(program,b);GLES20.glLinkProgram(program);
            pos=GLES20.glGetAttribLocation(program,"a");tex=GLES20.glGetAttribLocation(program,"b");coord=GLES20.glGetUniformLocation(program,"s");
        }
        void draw(Frame f){
            f.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,ndc,Coordinates2d.TEXTURE_NORMALIZED,uv);
            tb.position(0);tb.put(uv);tb.position(0);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);GLES20.glUseProgram(program);vb.position(0);
            GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,0,vb);GLES20.glEnableVertexAttribArray(pos);
            GLES20.glVertexAttribPointer(tex,2,GLES20.GL_FLOAT,false,0,tb);GLES20.glEnableVertexAttribArray(tex);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glUniform1i(coord,0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        }
        static FloatBuffer buf(float[]a){ByteBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder());FloatBuffer f=b.asFloatBuffer();f.put(a).position(0);return f;}
        static int sh(int t,String s){int x=GLES20.glCreateShader(t);GLES20.glShaderSource(x,s);GLES20.glCompileShader(x);return x;}
    }
}
