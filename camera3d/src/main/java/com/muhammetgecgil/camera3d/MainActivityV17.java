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
import android.opengl.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

public class MainActivityV17 extends Activity implements GLSurfaceView.Renderer {
    static final int REQ = 71;
    static final String[] FACES = {"ÖN", "SAĞ", "ARKA", "SOL"};

    GLSurfaceView cameraGl;
    Session session;
    CameraBackground bg;
    TextView status, instruction, photoStatus, pointsText;
    TextView[] faceState = new TextView[4];
    Button shootBtn, approveBtn, retryBtn, buildBtn, newProjectBtn, viewerBtn;

    final ArrayList<float[]> points = new ArrayList<>();
    final HashSet<Long> voxels = new HashSet<>();
    final boolean[] captured = new boolean[4];
    final boolean[] approved = new boolean[4];
    final String[] photoNames = new String[4];
    final Pose[] photoPoses = new Pose[4];

    volatile boolean captureRequested = false;
    volatile int currentFace = 0;
    long lastCollect = 0;
    String projectId = "";
    boolean viewerOpen = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        newProjectId();
        buildCaptureUi();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ);
        } else setupAr();
    }

    void newProjectId() {
        projectId = "Scan_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setPadding(8, 5, 8, 5);
        return t;
    }

    void buildCaptureUi() {
        viewerOpen = false;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff080b10);
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);

        FrameLayout cameraBox = new FrameLayout(this);
        cameraGl = new GLSurfaceView(this);
        cameraGl.setEGLContextClientVersion(2);
        cameraGl.setPreserveEGLContextOnPause(true);
        cameraGl.setRenderer(this);
        cameraGl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        cameraBox.addView(cameraGl, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(16, 10, 16, 10);
        top.setBackgroundColor(0x99101820);
        top.addView(text("CAMERA 3D • FOTO → 3D • v1.7", 18, Color.WHITE));
        status = text("Kamera hazırlanıyor…", 13, 0xffd8e2ef);
        pointsText = text("3D destek noktası: " + points.size(), 12, 0xff7fdaff);
        top.addView(status); top.addView(pointsText);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        tp.setMargins(10, 10, 10, 0); cameraBox.addView(top, tp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(16, 10, 16, 12);
        bottom.setBackgroundColor(0xbb101820);
        instruction = text((currentFace + 1) + "/4 • " + FACES[currentFace] + " FOTOĞRAFI ÇEK", 18, Color.WHITE);
        photoStatus = text("Fotoğraf çekilmedi", 13, 0xffc7d3df);
        bottom.addView(instruction); bottom.addView(photoStatus);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        bp.setMargins(10, 0, 10, 10); cameraBox.addView(bottom, bp);

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(14, 14, 14, 14);
        side.setBackgroundColor(0xff121820);
        side.addView(text("4 FOTO → 3D", 22, Color.WHITE));
        side.addView(text("ÖN → SAĞ → ARKA → SOL. Her fotoğrafı sen ONAYLA demeden sonraki yüze geçmez.", 12, 0xffaebdca));
        for (int i = 0; i < 4; i++) {
            faceState[i] = text("○ " + FACES[i], 15, 0xffe6edf3);
            side.addView(faceState[i]);
        }

        shootBtn = new Button(this); shootBtn.setText("FOTOĞRAF ÇEK"); shootBtn.setOnClickListener(v -> requestCapture()); side.addView(shootBtn);
        approveBtn = new Button(this); approveBtn.setText("FOTOĞRAFI ONAYLA"); approveBtn.setOnClickListener(v -> approvePhoto()); side.addView(approveBtn);
        retryBtn = new Button(this); retryBtn.setText("TEKRAR ÇEK"); retryBtn.setOnClickListener(v -> retryPhoto()); side.addView(retryBtn);
        buildBtn = new Button(this); buildBtn.setText("4 FOTOĞRAFI 3D DATA YAP"); buildBtn.setOnClickListener(v -> build3D()); side.addView(buildBtn);
        viewerBtn = new Button(this); viewerBtn.setText("3D GÖRÜNTÜYÜ TAM EKRAN AÇ"); viewerBtn.setOnClickListener(v -> openViewerSnapshot()); side.addView(viewerBtn);
        newProjectBtn = new Button(this); newProjectBtn.setText("YENİ PROJE"); newProjectBtn.setOnClickListener(v -> resetProject()); side.addView(newProjectBtn);

        main.addView(cameraBox, new LinearLayout.LayoutParams(0, -1, 4.7f));
        main.addView(side, new LinearLayout.LayoutParams(0, -1, 1.8f));
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        refreshUi();
    }

    void setupAr() {
        try {
            if (session != null) return;
            if (ArCoreApk.getInstance().checkAvailability(this).isUnsupported()) {
                status.setText("ARCore desteklenmiyor"); return;
            }
            session = new Session(this);
            CameraConfigFilter filter = new CameraConfigFilter(session);
            filter.setFacingDirection(CameraConfig.FacingDirection.BACK);
            filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
            List<CameraConfig> cfgs = session.getSupportedCameraConfigs(filter);
            CameraConfig best = null; long area = -1;
            for (CameraConfig cc : cfgs) {
                long a = (long) cc.getTextureSize().getWidth() * cc.getTextureSize().getHeight();
                if (a > area) { area = a; best = cc; }
            }
            if (best != null) session.setCameraConfig(best);
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setFocusMode(Config.FocusMode.AUTO);
            session.configure(config);
            status.setText("Kamera hazır");
        } catch (Throwable t) {
            status.setText("ARCore: " + t.getClass().getSimpleName());
        }
    }

    void requestCapture() {
        if (session == null) { setupAr(); if (session == null) return; }
        if (captured[currentFace]) {
            Toast.makeText(this, "Önce ONAYLA veya TEKRAR ÇEK", Toast.LENGTH_SHORT).show(); return;
        }
        captureRequested = true;
        shootBtn.setEnabled(false);
        photoStatus.setText(FACES[currentFace] + " fotoğrafı çekiliyor… telefonu sabit tut");
    }

    void approvePhoto() {
        if (!captured[currentFace]) return;
        approved[currentFace] = true;
        if (currentFace < 3) {
            currentFace++;
            photoStatus.setText("Fotoğraf çekilmedi");
            instruction.setText((currentFace + 1) + "/4 • " + FACES[currentFace] + " FOTOĞRAFI ÇEK");
        } else {
            instruction.setText("4/4 TAMAM • 3D DATA OLUŞTUR");
            photoStatus.setText("Dört fotoğraf onaylandı");
        }
        refreshUi();
    }

    void retryPhoto() {
        captured[currentFace] = false;
        approved[currentFace] = false;
        photoNames[currentFace] = null;
        photoPoses[currentFace] = null;
        photoStatus.setText(FACES[currentFace] + " fotoğrafını tekrar çek");
        refreshUi();
    }

    void resetProject() {
        synchronized (this) { points.clear(); voxels.clear(); }
        for (int i = 0; i < 4; i++) {
            captured[i] = false; approved[i] = false; photoNames[i] = null; photoPoses[i] = null;
        }
        currentFace = 0; captureRequested = false; newProjectId();
        instruction.setText("1/4 • ÖN FOTOĞRAFI ÇEK");
        photoStatus.setText("Fotoğraf çekilmedi");
        pointsText.setText("3D destek noktası: 0");
        refreshUi();
    }

    void refreshUi() {
        for (int i = 0; i < 4; i++) {
            String p = approved[i] ? "✓ " : captured[i] ? "● " : (i == currentFace ? "▶ " : "○ ");
            faceState[i].setText(p + FACES[i]);
            faceState[i].setTextColor(approved[i] ? 0xff4bd37b : (i == currentFace ? 0xffffc857 : 0xffe6edf3));
        }
        boolean hasCurrent = captured[currentFace];
        shootBtn.setEnabled(!hasCurrent && !(approved[0] && approved[1] && approved[2] && approved[3]));
        approveBtn.setEnabled(hasCurrent && !approved[currentFace]);
        retryBtn.setEnabled(hasCurrent && !approved[currentFace]);
        boolean all = approved[0] && approved[1] && approved[2] && approved[3];
        buildBtn.setEnabled(all);
        viewerBtn.setEnabled(points.size() >= 20);
    }

    @Override public void onSurfaceCreated(GL10 g, EGLConfig e) {
        bg = new CameraBackground(); bg.create(); GLES20.glClearColor(0, 0, 0, 1);
    }

    @Override public void onSurfaceChanged(GL10 g, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        if (session != null) session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(), w, h);
    }

    @Override public void onDrawFrame(GL10 g) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (session == null || bg == null || viewerOpen) return;
        try {
            session.setCameraTextureName(bg.textureId);
            Frame f = session.update();
            bg.draw(f);
            com.google.ar.core.Camera cam = f.getCamera();
            if (cam.getTrackingState() != TrackingState.TRACKING) {
                uiStatus("Kamera açık • telefonu yavaş hareket ettir"); return;
            }
            collect3D(f);
            if (captureRequested) tryCapture(f, cam.getPose());
        } catch (Throwable t) {
            uiStatus("Kamera/AR hatası: " + t.getClass().getSimpleName());
        }
    }

    void collect3D(Frame f) {
        long now = System.currentTimeMillis();
        if (now - lastCollect < 80) return;
        lastCollect = now;
        try (PointCloud pc = f.acquirePointCloud()) {
            FloatBuffer b = pc.getPoints();
            int n = b.remaining() / 4, step = Math.max(1, n / 900);
            synchronized (this) {
                for (int i = 0; i < n; i += step) {
                    int q = i * 4;
                    float x = b.get(q), y = b.get(q + 1), z = b.get(q + 2), c = b.get(q + 3);
                    if (c < .25f || Math.abs(x) > 6 || Math.abs(y) > 6 || Math.abs(z) > 6) continue;
                    long k = key(x, y, z, .006f);
                    if (voxels.add(k)) points.add(new float[]{x, y, z, c});
                }
            }
            int count = points.size();
            runOnUiThread(() -> { pointsText.setText("3D destek noktası: " + count); viewerBtn.setEnabled(count >= 20); });
        } catch (Throwable ignored) {}
    }

    void tryCapture(Frame f, Pose pose) {
        Image img = null;
        try {
            img = f.acquireCameraImage();
            byte[] jpeg = yuv420ToJpeg(img, 94);
            String name = projectId + "_" + (currentFace + 1) + "_" + FACES[currentFace] + ".jpg";
            writeFile(name, "image/jpeg", jpeg, "Download/Camera3D/" + projectId);
            photoNames[currentFace] = name;
            photoPoses[currentFace] = pose;
            captured[currentFace] = true;
            captureRequested = false;
            int face = currentFace;
            runOnUiThread(() -> {
                photoStatus.setText(FACES[face] + " fotoğrafı çekildi • uygunsa ONAYLA");
                refreshUi();
            });
        } catch (Throwable t) {
            captureRequested = false;
            runOnUiThread(() -> { shootBtn.setEnabled(true); photoStatus.setText("Fotoğraf alınamadı • tekrar dene"); });
        } finally { if (img != null) img.close(); }
    }

    byte[] yuv420ToJpeg(Image image, int quality) throws Exception {
        int w = image.getWidth(), h = image.getHeight();
        Image.Plane[] p = image.getPlanes();
        byte[] nv21 = new byte[w * h * 3 / 2];
        ByteBuffer yb = p[0].getBuffer();
        int yr = p[0].getRowStride(), yp = p[0].getPixelStride();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int idx = y * yr + x * yp; if (idx < yb.limit()) nv21[y * w + x] = yb.get(idx);
        }
        ByteBuffer ub = p[1].getBuffer(), vb = p[2].getBuffer();
        int ur = p[1].getRowStride(), vr = p[2].getRowStride();
        int up = p[1].getPixelStride(), vp = p[2].getPixelStride();
        int off = w * h;
        for (int y = 0; y < h / 2; y++) for (int x = 0; x < w / 2; x++) {
            int vi = y * vr + x * vp, ui = y * ur + x * up;
            nv21[off++] = vb.get(vi); nv21[off++] = ub.get(ui);
        }
        YuvImage yi = new YuvImage(nv21, ImageFormat.NV21, w, h, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yi.compressToJpeg(new Rect(0, 0, w, h), quality, out);
        return out.toByteArray();
    }

    void build3D() {
        if (!(approved[0] && approved[1] && approved[2] && approved[3])) return;
        ArrayList<float[]> cp;
        synchronized (this) { cp = new ArrayList<>(points); }
        if (cp.size() < 20) {
            Toast.makeText(this, "3D nokta az • nesnenin çevresinde biraz daha hareket et", Toast.LENGTH_LONG).show(); return;
        }
        buildBtn.setEnabled(false);
        new Thread(() -> {
            try {
                String folder = "Download/Camera3D/" + projectId;
                writeFile(projectId + ".ply", "application/octet-stream", ply(cp), folder);
                writeFile(projectId + ".obj", "text/plain", obj(cp), folder);
                writeFile(projectId + "_poses.txt", "text/plain", posesText().getBytes(StandardCharsets.UTF_8), folder);
                runOnUiThread(() -> {
                    Toast.makeText(this, "3D data oluşturuldu • tam ekran görüntü açılıyor", Toast.LENGTH_LONG).show();
                    showViewer(cp);
                });
            } catch (Throwable e) {
                runOnUiThread(() -> { buildBtn.setEnabled(true); Toast.makeText(this, "3D kayıt hatası", Toast.LENGTH_LONG).show(); });
            }
        }).start();
    }

    void openViewerSnapshot() {
        ArrayList<float[]> cp;
        synchronized (this) { cp = new ArrayList<>(points); }
        if (cp.size() < 20) { Toast.makeText(this, "Henüz yeterli 3D veri yok", Toast.LENGTH_SHORT).show(); return; }
        showViewer(cp);
    }

    void showViewer(ArrayList<float[]> cp) {
        viewerOpen = true;
        if (session != null) try { session.pause(); } catch (Throwable ignored) {}
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        PointViewer view = new PointViewer(cp);
        root.addView(view, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(10, 8, 10, 8);
        bar.setBackgroundColor(0xaa000000);
        Button close = new Button(this); close.setText("← TARAMA"); close.setOnClickListener(v -> closeViewer());
        Button reset = new Button(this); reset.setText("MERKEZ"); reset.setOnClickListener(v -> view.resetView());
        Button front = new Button(this); front.setText("ÖN"); front.setOnClickListener(v -> view.setPreset(0));
        Button right = new Button(this); right.setText("SAĞ"); right.setOnClickListener(v -> view.setPreset(1));
        Button top = new Button(this); top.setText("ÜST"); top.setOnClickListener(v -> view.setPreset(2));
        bar.addView(close); bar.addView(reset); bar.addView(front); bar.addView(right); bar.addView(top);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        p.setMargins(10, 10, 10, 0); root.addView(bar, p);

        TextView hint = text("PARMAKLA DÖNDÜR • İKİ PARMAKLA ZOOM • X/Y/Z EKSENLERİNDE SERBEST İNCELE", 13, Color.WHITE);
        hint.setBackgroundColor(0xaa000000); hint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        hp.setMargins(30, 0, 30, 20); root.addView(hint, hp);
        setContentView(root);
    }

    void closeViewer() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        buildCaptureUi();
        viewerOpen = false;
        if (session != null) try { session.resume(); } catch (Throwable ignored) {}
        if (cameraGl != null) cameraGl.onResume();
    }

    String posesText() {
        StringBuilder s = new StringBuilder("Camera3D Photo Project\n");
        for (int i = 0; i < 4; i++) {
            Pose p = photoPoses[i];
            s.append(FACES[i]).append(" photo=").append(photoNames[i]).append('\n');
            if (p != null) {
                float[] t = p.getTranslation(), q = p.getRotationQuaternion();
                s.append("t ").append(t[0]).append(' ').append(t[1]).append(' ').append(t[2]).append('\n');
                s.append("q ").append(q[0]).append(' ').append(q[1]).append(' ').append(q[2]).append(' ').append(q[3]).append('\n');
            }
        }
        return s.toString();
    }

    byte[] ply(ArrayList<float[]> p) {
        StringBuilder s = new StringBuilder("ply\nformat ascii 1.0\nelement vertex " + p.size() + "\nproperty float x\nproperty float y\nproperty float z\nend_header\n");
        for (float[] v : p) s.append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');
        return s.toString().getBytes(StandardCharsets.UTF_8);
    }

    byte[] obj(ArrayList<float[]> p) {
        StringBuilder s = new StringBuilder("# Camera3D point data\n");
        for (float[] v : p) s.append("v ").append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');
        return s.toString().getBytes(StandardCharsets.UTF_8);
    }

    void writeFile(String name, String mime, byte[] data, String relative) throws Exception {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Downloads.DISPLAY_NAME, name);
        v.put(MediaStore.Downloads.MIME_TYPE, mime);
        v.put(MediaStore.Downloads.RELATIVE_PATH, relative);
        Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) throw new Exception("MediaStore insert failed");
        try (OutputStream o = getContentResolver().openOutputStream(u)) { if (o == null) throw new Exception("Output null"); o.write(data); }
    }

    long key(float x, float y, float z, float cell) {
        long a = (long)Math.floor(x / cell) + 1048576;
        long b = (long)Math.floor(y / cell) + 1048576;
        long c = (long)Math.floor(z / cell) + 1048576;
        return ((a & 0x1fffffL) << 42) | ((b & 0x1fffffL) << 21) | (c & 0x1fffffL);
    }

    void uiStatus(String s) { runOnUiThread(() -> { if (status != null) status.setText(s); }); }

    @Override protected void onResume() {
        super.onResume();
        if (!viewerOpen) {
            if (session == null && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) setupAr();
            if (session != null) try { session.resume(); } catch (CameraNotAvailableException e) { if (status != null) status.setText("Kamera açılamadı"); }
            if (cameraGl != null) cameraGl.onResume();
        }
    }

    @Override protected void onPause() {
        if (!viewerOpen) {
            if (cameraGl != null) cameraGl.onPause();
            if (session != null) try { session.pause(); } catch (Throwable ignored) {}
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (session != null) session.close();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (viewerOpen) closeViewer(); else super.onBackPressed();
    }

    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (r == REQ && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) setupAr();
    }

    static FloatBuffer floatBuffer(float[] a) {
        ByteBuffer b = ByteBuffer.allocateDirect(a.length * 4).order(ByteOrder.nativeOrder());
        FloatBuffer f = b.asFloatBuffer(); f.put(a).position(0); return f;
    }

    static int shader(int type, String src) {
        int s = GLES20.glCreateShader(type); GLES20.glShaderSource(s, src); GLES20.glCompileShader(s); return s;
    }

    class PointViewer extends GLSurfaceView implements GLSurfaceView.Renderer, View.OnTouchListener {
        final ArrayList<float[]> src;
        FloatBuffer verts;
        int count, program, aPos, uMvp, uColor;
        float rotX = -18f, rotY = 25f, zoom = 1.8f;
        float lastX, lastY;
        float centerX, centerY, centerZ, radius = 1f;
        ScaleGestureDetector scale;

        PointViewer(ArrayList<float[]> data) {
            super(MainActivityV17.this);
            src = data;
            setEGLContextClientVersion(2);
            setRenderer(this);
            setRenderMode(RENDERMODE_CONTINUOUSLY);
            setOnTouchListener(this);
            scale = new ScaleGestureDetector(MainActivityV17.this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    zoom /= detector.getScaleFactor();
                    zoom = Math.max(.35f, Math.min(8f, zoom));
                    return true;
                }
            });
            prepareData();
        }

        void prepareData() {
            if (src.isEmpty()) return;
            float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,minZ=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE,maxZ=-Float.MAX_VALUE;
            for (float[] p : src) {
                minX=Math.min(minX,p[0]); minY=Math.min(minY,p[1]); minZ=Math.min(minZ,p[2]);
                maxX=Math.max(maxX,p[0]); maxY=Math.max(maxY,p[1]); maxZ=Math.max(maxZ,p[2]);
            }
            centerX=(minX+maxX)/2f; centerY=(minY+maxY)/2f; centerZ=(minZ+maxZ)/2f;
            radius=Math.max(.01f, Math.max(maxX-minX, Math.max(maxY-minY,maxZ-minZ))/2f);
            float[] a = new float[src.size()*3]; int k=0;
            for(float[] p:src){a[k++]=(p[0]-centerX)/radius;a[k++]=(p[1]-centerY)/radius;a[k++]=(p[2]-centerZ)/radius;}
            verts=floatBuffer(a); count=src.size();
        }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
            GLES20.glClearColor(.015f,.018f,.025f,1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            String vs="uniform mat4 u;attribute vec3 a;void main(){gl_Position=u*vec4(a,1.0);gl_PointSize=4.0;}";
            String fs="precision mediump float;uniform vec4 c;void main(){gl_FragColor=c;}";
            program=GLES20.glCreateProgram();
            GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,vs));
            GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,fs));
            GLES20.glLinkProgram(program);
            aPos=GLES20.glGetAttribLocation(program,"a"); uMvp=GLES20.glGetUniformLocation(program,"u"); uColor=GLES20.glGetUniformLocation(program,"c");
        }

        @Override public void onSurfaceChanged(GL10 gl, int w, int h) { GLES20.glViewport(0,0,w,h); }

        @Override public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            if(verts==null||count==0)return;
            int w=getWidth(),h=Math.max(1,getHeight());
            float[] proj=new float[16],view=new float[16],model=new float[16],tmp=new float[16],mvp=new float[16];
            Matrix.perspectiveM(proj,0,45f,(float)w/h,.05f,50f);
            Matrix.setLookAtM(view,0,0,0,zoom,0,0,0,0,1,0);
            Matrix.setIdentityM(model,0);
            Matrix.rotateM(model,0,rotX,1,0,0); Matrix.rotateM(model,0,rotY,0,1,0);
            Matrix.multiplyMM(tmp,0,view,0,model,0); Matrix.multiplyMM(mvp,0,proj,0,tmp,0);
            GLES20.glUseProgram(program); GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0); GLES20.glUniform4f(uColor,.2f,.85f,1f,1f);
            verts.position(0); GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,verts); GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glDrawArrays(GLES20.GL_POINTS,0,count);
        }

        @Override public boolean onTouch(View v, MotionEvent e) {
            scale.onTouchEvent(e);
            if(e.getPointerCount()==1 && !scale.isInProgress()){
                if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-lastX,dy=e.getY()-lastY;rotY+=dx*.35f;rotX+=dy*.35f;lastX=e.getX();lastY=e.getY();return true;}
            }
            return true;
        }

        void resetView(){rotX=-18f;rotY=25f;zoom=1.8f;}
        void setPreset(int p){if(p==0){rotX=0;rotY=0;}else if(p==1){rotX=0;rotY=90;}else{rotX=-90;rotY=0;}zoom=1.8f;}
    }

    static class CameraBackground {
        int textureId, program, aPos, aUv, uTex;
        FloatBuffer vb=floatBuffer(new float[]{-1,-1,1,-1,-1,1,1,1});
        FloatBuffer tb=floatBuffer(new float[]{0,1,1,1,0,0,1,0});
        void create(){
            int[] t=new int[1]; GLES20.glGenTextures(1,t,0); textureId=t[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            String vs="attribute vec2 a;attribute vec2 b;varying vec2 v;void main(){gl_Position=vec4(a,0.,1.);v=b;}";
            String fs="#extension GL_OES_EGL_image_external : require\nprecision mediump float;uniform samplerExternalOES s;varying vec2 v;void main(){gl_FragColor=texture2D(s,v);}";
            program=GLES20.glCreateProgram(); GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,vs)); GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,fs)); GLES20.glLinkProgram(program);
            aPos=GLES20.glGetAttribLocation(program,"a");aUv=GLES20.glGetAttribLocation(program,"b");uTex=GLES20.glGetUniformLocation(program,"s");
        }
        void draw(Frame f){
            float[] in={0,0,1,0,0,1,1,1};FloatBuffer ib=floatBuffer(in),ob=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
            try{f.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,ib,Coordinates2d.TEXTURE_NORMALIZED,ob);ob.position(0);tb=ob;}catch(Throwable ignored){}
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);GLES20.glUseProgram(program);vb.position(0);tb.position(0);
            GLES20.glVertexAttribPointer(aPos,2,GLES20.GL_FLOAT,false,0,vb);GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glVertexAttribPointer(aUv,2,GLES20.GL_FLOAT,false,0,tb);GLES20.glEnableVertexAttribArray(aUv);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);GLES20.glUniform1i(uTex,0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        }
    }
}
