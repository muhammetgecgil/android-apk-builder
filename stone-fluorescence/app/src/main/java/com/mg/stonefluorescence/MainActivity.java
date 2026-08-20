package com.mg.stonefluorescence;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
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
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private double baseBrightness = -1;
    private double baseSaturation = -1;
    private double baseRedGreen = 0;
    private double baseHot = -1;
    private int warmupFrames = 0;
    private long lastUiMs = 0;

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
        root.addView(textureView, new FrameLayout.LayoutParams(-1, -1));

        overlay = new GlowOverlay(this);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(16), dp(18), dp(16), dp(12));
        top.setBackgroundColor(0x99000000);

        TextView title = new TextView(this);
        title.setText("STONE GLOW ANALYZER • LIVE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Taşı merkez çerçevede tut. Kamera canlı olarak renk/parlaklık anomalisi arar.");
        sub.setTextColor(Color.rgb(210,220,230));
        sub.setTextSize(13);
        sub.setPadding(0, dp(5), 0, 0);
        top.addView(sub);

        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        root.addView(top, tp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(16), dp(12), dp(16), dp(18));
        bottom.setBackgroundColor(0xB0000000);

        scoreText = new TextView(this);
        scoreText.setText("Kamera hazırlanıyor…");
        scoreText.setTextColor(Color.WHITE);
        scoreText.setTextSize(22);
        scoreText.setTypeface(null, android.graphics.Typeface.BOLD);
        scoreText.setGravity(Gravity.CENTER_HORIZONTAL);
        bottom.addView(scoreText);

        detailText = new TextView(this);
        detailText.setText("Not: Bu uygulama gerçek UV dalga boyunu ölçmez; görünür kameradaki sıra dışı optik davranışı işaretler.");
        detailText.setTextColor(Color.rgb(220,225,232));
        detailText.setTextSize(13);
        detailText.setPadding(0, dp(7), 0, 0);
        bottom.addView(detailText);

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(bottom, bp);
        return root;
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
                Integer facing = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) { selectedId = id; break; }
            }
            if (selectedId == null && manager.getCameraIdList().length > 0) selectedId = manager.getCameraIdList()[0];
            if (selectedId == null) throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Kamera bulunamadı");

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

            final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(previewSurface);
            builder.addTarget(analysisSurface);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, analysisSurface), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        session.setRepeatingRequest(builder.build(), null, cameraHandler);
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
                        "%s • Parlaklık farkı %.0f%% • Lokal parlama %.0f%%\nTaşı merkez çerçevede sabit tut. Güçlü skor birkaç kare boyunca sürerse daha anlamlıdır.",
                        finalColorShift, finalB * 100, clamp(finalHot) * 100));
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
        if (requestCode == REQ_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCameraFlow();
        } else {
            scoreText.setText("Kamera izni gerekli");
            detailText.setText("Canlı analiz için kamera iznini etkinleştir.");
        }
    }

    @Override protected void onPause() { super.onPause(); closeCamera(); }
    @Override protected void onResume() {
        super.onResume();
        if (textureView != null && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && cameraDevice == null) startCameraFlow();
    }
    @Override protected void onDestroy() { super.onDestroy(); closeCamera(); stopBackgroundThread(); }

    private void closeCamera() {
        try { if (captureSession != null) captureSession.close(); } catch (Exception ignored) {}
        captureSession = null;
        try { if (cameraDevice != null) cameraDevice.close(); } catch (Exception ignored) {}
        cameraDevice = null;
        try { if (imageReader != null) imageReader.close(); } catch (Exception ignored) {}
        imageReader = null;
    }

    private void stopBackgroundThread() {
        if (cameraThread != null) {
            cameraThread.quitSafely();
            try { cameraThread.join(); } catch (InterruptedException ignored) {}
        }
        cameraThread = null;
        cameraHandler = null;
    }

    public static class GlowOverlay extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int level = 0;
        private float score = 0;
        public GlowOverlay(Context context) { super(context); setWillNotDraw(false); }
        public void setLevel(int level, float score) { this.level = level; this.score = score; invalidate(); }
        @Override protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            float rw = w * 0.68f, rh = h * 0.42f;
            RectF r = new RectF((w-rw)/2f, (h-rh)/2f, (w+rw)/2f, (h+rh)/2f);
            int c = level >= 2 ? Color.rgb(255,75,75) : (level == 1 ? Color.rgb(255,205,80) : Color.rgb(80,230,150));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(level >= 2 ? 10f : 5f);
            paint.setColor(c);
            paint.setAlpha(level >= 2 ? 245 : 190);
            canvas.drawRoundRect(r, 28f, 28f, paint);
            if (level >= 2) {
                paint.setStrokeWidth(24f);
                paint.setAlpha((int)Math.min(120, 35 + score));
                canvas.drawRoundRect(r, 28f, 28f, paint);
            }
        }
    }
}
