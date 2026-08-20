package com.mg.stonefluorescence;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 1001;

    private TextureView textureView;
    private GlowOverlay overlay;
    private TextView scoreText;
    private TextView detailText;
    private TextView modeText;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private CaptureRequest.Builder previewBuilder;
    private CameraCharacteristics cameraCharacteristics;
    private Rect activeArray;

    private double baseBrightness = -1;
    private double baseSaturation = -1;
    private double baseRedGreen = 0;
    private double baseHot = -1;
    private int warmupFrames = 0;
    private long lastUiMs = 0;

    private String currentMode = "NORMAL";

    static class Stat {
        double brightness;
        double saturation;
        double redGreen;
        double hotRatio;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraFlow();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        textureView = new TextureView(this);
        textureView.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                focusAt(e.getX(), e.getY());
                overlay.showFocus(e.getX(), e.getY());
                return true;
            }
            return true;
        });
        root.addView(textureView, new FrameLayout.LayoutParams(-1, -1));

        overlay = new GlowOverlay(this);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(14), dp(16), dp(14), dp(10));
        top.setBackgroundColor(0x88000000);

        TextView title = label("STONE GLOW ANALYZER • LIVE FILTER", 20, true, Color.WHITE);
        top.addView(title);

        modeText = label("MOD: NORMAL", 13, true, Color.rgb(180, 210, 255));
        modeText.setPadding(0, dp(4), 0, dp(4));
        top.addView(modeText);

        TextView helper = label("Taşı merkez çerçevede tut • Netlik için ekrana dokun", 12, false, Color.rgb(215, 220, 228));
        top.addView(helper);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"NORMAL", "UV+", "FOSFOR", "KONTRAST+", "YEŞİL↔KIRMIZI", "ISI"};
        for (String n : names) {
            Button b = new Button(this);
            b.setText(n);
            b.setTextSize(11);
            b.setAllCaps(false);
            b.setOnClickListener(v -> applyFilter(n));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(112), dp(46));
            lp.setMargins(dp(3), dp(4), dp(3), 0);
            filters.addView(b, lp);
        }
        scroll.addView(filters);
        top.addView(scroll, new LinearLayout.LayoutParams(-1, dp(54)));

        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        root.addView(top, tp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(14), dp(10), dp(14), dp(16));
        bottom.setBackgroundColor(0xB0000000);

        scoreText = label("Kamera hazırlanıyor…", 21, true, Color.WHITE);
        scoreText.setGravity(Gravity.CENTER_HORIZONTAL);
        bottom.addView(scoreText);

        detailText = label("Canlı analiz, görünür kameradaki sıra dışı parlaklık ve renk değişimlerini vurgular.", 12, false, Color.rgb(220,225,232));
        detailText.setPadding(0, dp(6), 0, 0);
        bottom.addView(detailText);

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(bottom, bp);
        return root;
    }

    private TextView label(String text, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private void applyFilter(String mode) {
        currentMode = mode;
        modeText.setText("MOD: " + mode);
        Paint p = new Paint();
        ColorMatrix m = new ColorMatrix();

        if ("NORMAL".equals(mode)) {
            textureView.setLayerType(View.LAYER_TYPE_NONE, null);
            overlay.setTint(0x00000000);
            return;
        }

        if ("UV+".equals(mode)) {
            m.set(new float[]{
                    0.85f, 0.10f, 0.25f, 0, 0,
                    0.05f, 1.15f, 0.55f, 0, 0,
                    0.15f, 0.25f, 1.45f, 0, 10,
                    0,0,0,1,0
            });
            overlay.setTint(0x2A5D2DFF);
        } else if ("FOSFOR".equals(mode)) {
            m.set(new float[]{
                    0.45f, 0.15f, 0.05f, 0, -10,
                    0.15f, 1.75f, 0.20f, 0, 0,
                    0.05f, 0.25f, 0.35f, 0, -20,
                    0,0,0,1,0
            });
            overlay.setTint(0x1800FF55);
        } else if ("KONTRAST+".equals(mode)) {
            float c = 1.55f;
            float t = 128f * (1f - c);
            m.set(new float[]{
                    c,0,0,0,t,
                    0,c,0,0,t,
                    0,0,c,0,t,
                    0,0,0,1,0
            });
            overlay.setTint(0x00000000);
        } else if ("YEŞİL↔KIRMIZI".equals(mode)) {
            m.set(new float[]{
                    1.55f,-0.45f,0,0,0,
                    -0.40f,1.55f,0,0,0,
                    0.05f,0.05f,0.70f,0,0,
                    0,0,0,1,0
            });
            overlay.setTint(0x10000000);
        } else {
            m.set(new float[]{
                    1.75f,0.10f,-0.35f,0,0,
                    -0.20f,1.25f,0.10f,0,0,
                    -0.70f,0.35f,1.45f,0,0,
                    0,0,0,1,0
            });
            overlay.setTint(0x180000FF);
        }

        p.setColorFilter(new ColorMatrixColorFilter(m));
        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, p);
        textureView.invalidate();
    }

    private void startCameraFlow() {
        startBackgroundThread();
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { openCamera(); }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
        if (textureView.isAvailable()) openCamera();
    }

    private void startBackgroundThread() {
        if (cameraThread != null) return;
        cameraThread = new HandlerThread("StoneGlowCamera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String selectedId = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    selectedId = id;
                    cameraCharacteristics = cc;
                    break;
                }
            }
            if (selectedId == null && manager.getCameraIdList().length > 0) {
                selectedId = manager.getCameraIdList()[0];
                cameraCharacteristics = manager.getCameraCharacteristics(selectedId);
            }
            if (selectedId == null) throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Kamera bulunamadı");
            activeArray = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            imageReader = ImageReader.newInstance(640, 480, android.graphics.ImageFormat.YUV_420_888, 3);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image == null) return;
                try { analyzeImage(image); } finally { image.close(); }
            }, cameraHandler);

            manager.openCamera(selectedId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice camera) { cameraDevice = camera; createSession(); }
                @Override public void onDisconnected(CameraDevice camera) { camera.close(); cameraDevice = null; }
                @Override public void onError(CameraDevice camera, int error) {
                    camera.close(); cameraDevice = null;
                    runOnUiThread(() -> scoreText.setText("Kamera açılamadı"));
                }
            }, cameraHandler);
        } catch (Exception e) {
            runOnUiThread(() -> {
                scoreText.setText("Kamera hatası");
                detailText.setText(e.getMessage() == null ? "Kamera başlatılamadı." : e.getMessage());
            });
        }
    }

    private void createSession() {
        if (cameraDevice == null || !textureView.isAvailable() || imageReader == null) return;
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;
            texture.setDefaultBufferSize(1280, 720);
            Surface previewSurface = new Surface(texture);
            Surface analysisSurface = imageReader.getSurface();

            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(previewSurface);
            previewBuilder.addTarget(analysisSurface);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            previewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, analysisSurface), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        session.setRepeatingRequest(previewBuilder.build(), null, cameraHandler);
                        runOnUiThread(() -> scoreText.setText("CANLI ANALİZ • KALİBRASYON"));
                    } catch (CameraAccessException e) {
                        runOnUiThread(() -> scoreText.setText("Kamera akışı başlatılamadı"));
                    }
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {
                    runOnUiThread(() -> scoreText.setText("Kamera oturumu kurulamadı"));
                }
            }, cameraHandler);
        } catch (CameraAccessException e) {
            runOnUiThread(() -> scoreText.setText("Kamera yapılandırma hatası"));
        }
    }

    private void focusAt(float x, float y) {
        if (captureSession == null || previewBuilder == null) return;
        try {
            if (activeArray != null) {
                float nx = x / Math.max(1f, textureView.getWidth());
                float ny = y / Math.max(1f, textureView.getHeight());
                int sx = activeArray.left + (int)(nx * activeArray.width());
                int sy = activeArray.top + (int)(ny * activeArray.height());
                int half = Math.max(80, Math.min(activeArray.width(), activeArray.height()) / 18);
                Rect r = new Rect(
                        Math.max(activeArray.left, sx - half),
                        Math.max(activeArray.top, sy - half),
                        Math.min(activeArray.right, sx + half),
                        Math.min(activeArray.bottom, sy + half));
                MeteringRectangle mr = new MeteringRectangle(r, MeteringRectangle.METERING_WEIGHT_MAX);
                Integer afRegions = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
                Integer aeRegions = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
                if (afRegions != null && afRegions > 0) previewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{mr});
                if (aeRegions != null && aeRegions > 0) previewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{mr});
            }
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            captureSession.capture(previewBuilder.build(), null, cameraHandler);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureSession.setRepeatingRequest(previewBuilder.build(), null, cameraHandler);
            runOnUiThread(() -> detailText.setText("Dokunulan noktada otomatik netlik ve pozlama ayarlandı."));
        } catch (Exception ignored) {}
    }

    private void analyzeImage(Image image) {
        Stat s = statFromYuv(image);
        if (s == null) return;

        if (baseBrightness < 0) {
            baseBrightness = s.brightness;
            baseSaturation = s.saturation;
            baseRedGreen = s.redGreen;
            baseHot = s.hotRatio;
            warmupFrames = 1;
            return;
        }

        if (warmupFrames < 20) {
            baseBrightness = ema(baseBrightness, s.brightness, 0.18);
            baseSaturation = ema(baseSaturation, s.saturation, 0.18);
            baseRedGreen = ema(baseRedGreen, s.redGreen, 0.18);
            baseHot = ema(baseHot, s.hotRatio, 0.18);
            warmupFrames++;
            if (warmupFrames == 20) runOnUiThread(() -> scoreText.setText("CANLI ANALİZ AKTİF"));
            return;
        }

        double bChange = Math.abs(s.brightness - baseBrightness) / (baseBrightness + 0.05);
        double satChange = Math.max(0, s.saturation - baseSaturation) / (baseSaturation + 0.05);
        double rgShift = Math.abs(s.redGreen - baseRedGreen);
        double hotRise = Math.max(0, s.hotRatio - baseHot) / (baseHot + 0.01);
        double score = 100.0 * clamp(0.34 * clamp(bChange / 0.35)
                + 0.20 * clamp(satChange / 0.45)
                + 0.28 * clamp(rgShift / 0.28)
                + 0.18 * clamp(hotRise / 1.5));

        String colorShift;
        double delta = s.redGreen - baseRedGreen;
        if (delta > 0.08) colorShift = "Kırmızıya kayma";
        else if (delta < -0.08) colorShift = "Yeşile kayma";
        else colorShift = "Belirgin renk yönü yok";

        int level;
        String label;
        if (score >= 70) { level = 3; label = "GÜÇLÜ OPTİK ANOMALİ"; }
        else if (score >= 45) { level = 2; label = "BELİRGİN ANOMALİ"; }
        else if (score >= 25) { level = 1; label = "HAFİF ANOMALİ"; }
        else { level = 0; label = "NORMAL / ZAYIF TEPKİ"; }

        long now = System.currentTimeMillis();
        if (now - lastUiMs > 120) {
            lastUiMs = now;
            final double finalScore = score;
            final int finalLevel = level;
            final String finalLabel = label;
            final String finalColorShift = colorShift;
            final double finalB = bChange;
            final double finalHot = hotRise;
            runOnUiThread(() -> {
                scoreText.setText(String.format(Locale.US, "%s  %.0f/100", finalLabel, finalScore));
                scoreText.setTextColor(finalLevel >= 2 ? Color.rgb(255,105,95) : (finalLevel == 1 ? Color.rgb(255,215,110) : Color.rgb(120,230,180)));
                detailText.setText(String.format(Locale.US,
                        "%s • Parlaklık farkı %.0f%% • Lokal parlama %.0f%% • %s",
                        finalColorShift, finalB * 100, clamp(finalHot) * 100, currentMode));
                overlay.setLevel(finalLevel, (float) finalScore);
            });
        }

        if (score < 22) {
            baseBrightness = ema(baseBrightness, s.brightness, 0.015);
            baseSaturation = ema(baseSaturation, s.saturation, 0.015);
            baseRedGreen = ema(baseRedGreen, s.redGreen, 0.015);
            baseHot = ema(baseHot, s.hotRatio, 0.015);
        }
    }

    private Stat statFromYuv(Image image) {
        if (image.getPlanes().length < 3) return null;
        Image.Plane yP = image.getPlanes()[0], uP = image.getPlanes()[1], vP = image.getPlanes()[2];
        ByteBuffer yB = yP.getBuffer(), uB = uP.getBuffer(), vB = vP.getBuffer();
        int w = image.getWidth(), h = image.getHeight();
        int x0 = (int)(w * 0.22), x1 = (int)(w * 0.78), y0 = (int)(h * 0.22), y1 = (int)(h * 0.78);
        int step = 6;
        double sumV = 0, sumSat = 0, sumRG = 0;
        int hot = 0, n = 0;
        int yRow = yP.getRowStride(), yPix = yP.getPixelStride();
        int uRow = uP.getRowStride(), uPix = uP.getPixelStride();
        int vRow = vP.getRowStride(), vPix = vP.getPixelStride();

        for (int yy = y0; yy < y1; yy += step) {
            for (int xx = x0; xx < x1; xx += step) {
                int yi = yy * yRow + xx * yPix;
                int ui = (yy / 2) * uRow + (xx / 2) * uPix;
                int vi = (yy / 2) * vRow + (xx / 2) * vPix;
                if (yi >= yB.limit() || ui >= uB.limit() || vi >= vB.limit()) continue;
                int Y = yB.get(yi) & 0xff;
                int U = (uB.get(ui) & 0xff) - 128;
                int V = (vB.get(vi) & 0xff) - 128;
                int r = clamp255((int)(Y + 1.402 * V));
                int g = clamp255((int)(Y - 0.344136 * U - 0.714136 * V));
                int b = clamp255((int)(Y + 1.772 * U));
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                double val = max / 255.0;
                double sat = max == 0 ? 0 : (max - min) / (double) max;
                sumV += val;
                sumSat += sat;
                sumRG += (r - g) / 255.0;
                if (val > 0.86 && sat > 0.28) hot++;
                n++;
            }
        }
        if (n == 0) return null;
        Stat s = new Stat();
        s.brightness = sumV / n;
        s.saturation = sumSat / n;
        s.redGreen = sumRG / n;
        s.hotRatio = hot / (double) n;
        return s;
    }

    private double ema(double oldV, double newV, double a) { return oldV * (1.0 - a) + newV * a; }
    private double clamp(double x) { return Math.max(0, Math.min(1, x)); }
    private int clamp255(int x) { return Math.max(0, Math.min(255, x)); }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startCameraFlow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (captureSession != null) captureSession.close(); } catch (Exception ignored) {}
        try { if (cameraDevice != null) cameraDevice.close(); } catch (Exception ignored) {}
        try { if (imageReader != null) imageReader.close(); } catch (Exception ignored) {}
        if (cameraThread != null) {
            cameraThread.quitSafely();
            cameraThread = null;
            cameraHandler = null;
        }
    }

    static class GlowOverlay extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int level = 0;
        private float score = 0;
        private int tint = 0x00000000;
        private float focusX = -1, focusY = -1;
        private long focusUntil = 0;

        GlowOverlay(Context c) { super(c); setWillNotDraw(false); }

        void setLevel(int l, float s) { level = l; score = s; invalidate(); }
        void setTint(int c) { tint = c; invalidate(); }
        void showFocus(float x, float y) { focusX = x; focusY = y; focusUntil = System.currentTimeMillis() + 1200; invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            if ((tint >>> 24) != 0) c.drawColor(tint);

            float w = getWidth(), h = getHeight();
            float left = w * 0.18f, right = w * 0.82f, top = h * 0.24f, bottom = h * 0.73f;
            int color = level >= 2 ? Color.rgb(255,95,90) : (level == 1 ? Color.rgb(255,215,95) : Color.rgb(80,235,145));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(6f);
            p.setColor(color);
            float len = Math.min(w,h) * 0.08f;
            c.drawLine(left, top, left+len, top, p); c.drawLine(left, top, left, top+len, p);
            c.drawLine(right, top, right-len, top, p); c.drawLine(right, top, right, top+len, p);
            c.drawLine(left, bottom, left+len, bottom, p); c.drawLine(left, bottom, left, bottom-len, p);
            c.drawLine(right, bottom, right-len, bottom, p); c.drawLine(right, bottom, right, bottom-len, p);

            if (level >= 2) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(level == 3 ? 0x28FF4040 : 0x20FFD060);
                c.drawRoundRect(new RectF(left, top, right, bottom), 24, 24, p);
            }

            p.setStyle(Paint.Style.FILL);
            p.setColor(0xCC000000);
            c.drawRoundRect(new RectF(w*0.34f, h*0.75f, w*0.66f, h*0.83f), 20, 20, p);
            p.setColor(color);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Math.max(34f, w*0.065f));
            p.setFakeBoldText(true);
            c.drawText(String.format(Locale.US, "%.0f/100", score), w/2f, h*0.805f, p);

            if (focusX >= 0 && System.currentTimeMillis() < focusUntil) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(4f);
                p.setColor(Color.WHITE);
                c.drawCircle(focusX, focusY, 42f, p);
                c.drawLine(focusX-58, focusY, focusX-20, focusY, p);
                c.drawLine(focusX+20, focusY, focusX+58, focusY, p);
                c.drawLine(focusX, focusY-58, focusX, focusY-20, p);
                c.drawLine(focusX, focusY+20, focusX, focusY+58, p);
                postInvalidateDelayed(60);
            }
        }
    }
}
