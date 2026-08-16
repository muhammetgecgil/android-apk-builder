package com.muhammetgecgil.camera3d;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
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
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity implements GLSurfaceView.Renderer {
    private static final int REQ_CAMERA = 71;
    private GLSurfaceView glView;
    private Session session;
    private int cameraTextureId = -1;
    private volatile boolean scanning = false;
    private final ArrayList<float[]> points = new ArrayList<>();
    private final HashSet<Long> voxelKeys = new HashSet<>();
    private TextView status, countText, guideText;
    private ProgressBar progress;
    private Button scanButton, exportButton, clearButton;
    private long scanStarted = 0;
    private int targetPoints = 45000;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            setupAr();
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff050505);
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setPreserveEGLContextOnPause(true);
        glView.setRenderer(this);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(glView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(28, 28, 28, 22);
        top.setBackgroundColor(0xaa111111);
        TextView title = new TextView(this);
        title.setText("CAMERA 3D • CAD SCAN");
        title.setTextColor(0xffffffff);
        title.setTextSize(21);
        status = new TextView(this);
        status.setText("ARCore hazırlanıyor…");
        status.setTextColor(0xffdddddd);
        status.setTextSize(14);
        countText = new TextView(this);
        countText.setText("3D nokta: 0");
        countText.setTextColor(0xff9ee7ff);
        countText.setTextSize(17);
        guideText = new TextView(this);
        guideText.setText("Nesneyi ortada tut. Telefonu yavaşça nesnenin çevresinde 360° gezdir.");
        guideText.setTextColor(0xffffffff);
        guideText.setTextSize(14);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        top.addView(title); top.addView(status); top.addView(countText); top.addView(progress); top.addView(guideText);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        tp.setMargins(16, 35, 16, 0);
        root.addView(top, tp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(18, 18, 18, 24);
        bottom.setBackgroundColor(0xcc111111);
        LinearLayout row = new LinearLayout(this);
        scanButton = new Button(this);
        scanButton.setText("3D TARAMAYI BAŞLAT");
        scanButton.setOnClickListener(v -> toggleScan());
        clearButton = new Button(this);
        clearButton.setText("TEMİZLE");
        clearButton.setOnClickListener(v -> clearScan());
        row.addView(scanButton, new LinearLayout.LayoutParams(0, -2, 2f));
        row.addView(clearButton, new LinearLayout.LayoutParams(0, -2, 1f));
        bottom.addView(row);
        exportButton = new Button(this);
        exportButton.setText("CAD VERİSİ DIŞA AKTAR • PLY + OBJ");
        exportButton.setEnabled(false);
        exportButton.setOnClickListener(v -> exportCadData());
        bottom.addView(exportButton);
        TextView note = new TextView(this);
        note.setText("Bu sürüm gerçek ARCore 3D nokta bulutu üretir. PLY/OBJ mühendislik verisidir; STEP katı model sonraki yüzey-uydurma aşamasıdır.");
        note.setTextColor(0xffcccccc);
        note.setTextSize(12);
        bottom.addView(note);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        bp.setMargins(16, 0, 16, 38);
        root.addView(bottom, bp);
        setContentView(root);
    }

    private void setupAr() {
        try {
            ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
            if (availability.isUnsupported()) {
                status.setText("Bu cihaz ARCore desteklemiyor");
                return;
            }
            session = new Session(this);
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            session.configure(config);
            status.setText("ARCore hazır • 3D taramayı başlat");
        } catch (Throwable t) {
            status.setText("ARCore başlatılamadı: " + t.getClass().getSimpleName());
        }
    }

    private void toggleScan() {
        if (session == null) { setupAr(); if (session == null) return; }
        scanning = !scanning;
        if (scanning) {
            scanStarted = System.currentTimeMillis();
            scanButton.setText("TARAMAYI BİTİR");
            guideText.setText("Yavaş hareket et • Nesneyi her açıdan gör • Parlama ve hızlı dönüşten kaçın");
        } else {
            scanButton.setText("3D TARAMAYI BAŞLAT");
            guideText.setText("Tarama durdu. Nokta sayısı yeterliyse CAD verisini dışa aktar.");
            exportButton.setEnabled(points.size() > 100);
        }
    }

    private synchronized void clearScan() {
        scanning = false;
        points.clear();
        voxelKeys.clear();
        scanButton.setText("3D TARAMAYI BAŞLAT");
        exportButton.setEnabled(false);
        countText.setText("3D nokta: 0");
        progress.setProgress(0);
        guideText.setText("Nesneyi ortada tut. Telefonu yavaşça nesnenin çevresinde 360° gezdir.");
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        cameraTextureId = tex[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glClearColor(0.03f,0.03f,0.03f,1f);
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        if (session != null) session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(), width, height);
    }

    @Override public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Session s = session;
        if (s == null || cameraTextureId < 0) return;
        try {
            s.setCameraTextureName(cameraTextureId);
            Frame frame = s.update();
            Camera camera = frame.getCamera();
            if (camera.getTrackingState() != TrackingState.TRACKING) {
                runOnUiThread(() -> status.setText("Takip bekleniyor • telefonu yavaş hareket ettir"));
                return;
            }
            if (scanning) collectPointCloud(frame);
        } catch (Throwable t) {
            runOnUiThread(() -> status.setText("Tarama hatası: " + t.getClass().getSimpleName()));
        }
    }

    private void collectPointCloud(Frame frame) {
        try (PointCloud cloud = frame.acquirePointCloud()) {
            FloatBuffer fb = cloud.getPoints();
            int count = fb.remaining() / 4;
            int stride = Math.max(1, count / 700);
            synchronized (this) {
                for (int i=0;i<count;i+=stride) {
                    int p=i*4;
                    float x=fb.get(p), y=fb.get(p+1), z=fb.get(p+2), confidence=fb.get(p+3);
                    if (confidence < 0.35f) continue;
                    if (Math.abs(x)>5 || Math.abs(y)>5 || Math.abs(z)>5) continue;
                    long key = voxelKey(x,y,z,0.008f);
                    if (voxelKeys.add(key)) {
                        points.add(new float[]{x,y,z,confidence});
                        if (points.size() >= 120000) { scanning=false; break; }
                    }
                }
                int n=points.size();
                int pct=Math.min(100,(int)(100f*n/targetPoints));
                runOnUiThread(() -> {
                    countText.setText("3D nokta: " + n);
                    progress.setProgress(pct);
                    status.setText(pct<30?"Geometri toplanıyor…":pct<75?"İyi • diğer yüzleri tara":"Yoğun nokta bulutu • taramayı bitirebilirsin");
                    exportButton.setEnabled(n>100);
                });
            }
        }
    }

    private long voxelKey(float x,float y,float z,float cell) {
        long ix=(long)Math.floor(x/cell)+1048576;
        long iy=(long)Math.floor(y/cell)+1048576;
        long iz=(long)Math.floor(z/cell)+1048576;
        return ((ix & 0x1fffffL)<<42)|((iy & 0x1fffffL)<<21)|(iz & 0x1fffffL);
    }

    private void exportCadData() {
        final ArrayList<float[]> copy;
        synchronized (this) { copy = new ArrayList<>(points); }
        if (copy.size()<10) { Toast.makeText(this,"Önce tarama yap",Toast.LENGTH_SHORT).show(); return; }
        new Thread(() -> {
            try {
                String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
                Uri ply=writeDownload("Camera3D_"+stamp+".ply","application/octet-stream",buildPly(copy));
                Uri obj=writeDownload("Camera3D_"+stamp+".obj","text/plain",buildObj(copy));
                runOnUiThread(() -> Toast.makeText(this,"PLY + OBJ Downloads klasörüne kaydedildi",Toast.LENGTH_LONG).show());
            } catch (Throwable t) {
                runOnUiThread(() -> Toast.makeText(this,"Dışa aktarma hatası: "+t.getClass().getSimpleName(),Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private byte[] buildPly(ArrayList<float[]> pts) {
        StringBuilder sb=new StringBuilder(pts.size()*35);
        sb.append("ply\nformat ascii 1.0\nelement vertex ").append(pts.size()).append("\nproperty float x\nproperty float y\nproperty float z\nproperty float confidence\nend_header\n");
        for(float[] p:pts) sb.append(p[0]).append(' ').append(p[1]).append(' ').append(p[2]).append(' ').append(p[3]).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildObj(ArrayList<float[]> pts) {
        StringBuilder sb=new StringBuilder(pts.size()*30);
        sb.append("# Camera3D CAD Scan point cloud\n");
        for(float[] p:pts) sb.append("v ").append(p[0]).append(' ').append(p[1]).append(' ').append(p[2]).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Uri writeDownload(String name,String mime,byte[] data) throws Exception {
        ContentValues cv=new ContentValues();
        cv.put(MediaStore.Downloads.DISPLAY_NAME,name);
        cv.put(MediaStore.Downloads.MIME_TYPE,mime);
        cv.put(MediaStore.Downloads.RELATIVE_PATH,"Download/Camera3D");
        Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);
        if(uri==null) throw new IllegalStateException("MediaStore insert failed");
        try(OutputStream os=getContentResolver().openOutputStream(uri)){ if(os==null) throw new IllegalStateException("output null"); os.write(data); }
        return uri;
    }

    @Override protected void onResume() {
        super.onResume();
        if (session == null && checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) setupAr();
        if (session != null) {
            try { session.resume(); } catch (CameraNotAvailableException e) { status.setText("Kamera ARCore tarafından açılamadı"); }
        }
        if (glView != null) glView.onResume();
    }

    @Override protected void onPause() {
        if (glView != null) glView.onPause();
        if (session != null) session.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if(session!=null){session.close();session=null;}
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int rc,String[] p,int[] g){
        super.onRequestPermissionsResult(rc,p,g);
        if(rc==REQ_CAMERA && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED){setupAr();onResume();}
        else status.setText("Kamera izni gerekli");
    }
}
