package com.muhammetgecgil.nesnesayarai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivityV2 extends AppCompatActivity {
    private static final int REQ_CAMERA = 10;

    private PreviewView previewView;
    private Overlay overlay;
    private TextView resultText;
    private TextView statusText;
    private Button modeButton;
    private volatile Bitmap latestFrame;
    private volatile List<RectF> detections = new ArrayList<>();
    private boolean sameMode = false;
    private RectF sampleRect;

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService detectExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private final ObjectDetector detector = ObjectDetection.getClient(
            new ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .build());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        previewView = new PreviewView(this);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(previewView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlay = new Overlay(this);
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(14), 0, dp(14), 0);
        top.setBackgroundColor(Color.argb(190, 10, 18, 22));

        TextView title = new TextView(this);
        title.setText("Nesne Sayar AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(58), 1f));

        statusText = new TextView(this);
        statusText.setText("Hazır");
        statusText.setTextColor(Color.LTGRAY);
        statusText.setTextSize(12);
        statusText.setGravity(Gravity.CENTER);
        top.addView(statusText, new LinearLayout.LayoutParams(dp(120), dp(58)));

        resultText = new TextView(this);
        resultText.setText("0");
        resultText.setTextColor(Color.rgb(45, 245, 130));
        resultText.setTextSize(34);
        resultText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(resultText, new LinearLayout.LayoutParams(dp(72), dp(58)));

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58), Gravity.TOP);
        root.addView(top, topLp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setBackgroundColor(Color.argb(245, 15, 48, 58));

        Button sourceButton = makeButton("CANLI");
        modeButton = makeButton("FARKLI");
        Button countButton = makeButton("SAY");
        Button clearButton = makeButton("SİL");
        countButton.setBackgroundColor(Color.rgb(0, 135, 75));

        bottom.addView(sourceButton, new LinearLayout.LayoutParams(0, dp(60), 1f));
        bottom.addView(modeButton, new LinearLayout.LayoutParams(0, dp(60), 1f));
        bottom.addView(countButton, new LinearLayout.LayoutParams(0, dp(60), 1f));
        bottom.addView(clearButton, new LinearLayout.LayoutParams(0, dp(60), 1f));

        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60), Gravity.BOTTOM);
        bottomLp.bottomMargin = dp(8);
        root.addView(bottom, bottomLp);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topLp.topMargin = bars.top;
            bottomLp.bottomMargin = bars.bottom + dp(8);
            top.setLayoutParams(topLp);
            bottom.setLayoutParams(bottomLp);
            return insets;
        });

        setContentView(root);

        sourceButton.setOnClickListener(v -> Toast.makeText(this, "Canlı kamera aktif", Toast.LENGTH_SHORT).show());

        modeButton.setOnClickListener(v -> {
            sameMode = !sameMode;
            modeButton.setText(sameMode ? "AYNI" : "FARKLI");
            sampleRect = null;
            overlay.setSample(null);
            refreshCount();
        });

        countButton.setOnClickListener(v -> {
            Bitmap current = latestFrame;
            if (current == null || current.isRecycled()) {
                Toast.makeText(this, "Kamera görüntüsü hazırlanıyor", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap snapshot;
            try {
                snapshot = current.copy(Bitmap.Config.ARGB_8888, false);
            } catch (Throwable e) {
                Toast.makeText(this, "Yeni kamera karesi bekleniyor", Toast.LENGTH_SHORT).show();
                return;
            }
            analyze(snapshot);
        });

        clearButton.setOnClickListener(v -> {
            detections = new ArrayList<>();
            sampleRect = null;
            overlay.setBoxes(Collections.emptyList());
            overlay.setSample(null);
            resultText.setText("0");
            statusText.setText("Temizlendi");
        });
    }

    private Button makeButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.rgb(19, 58, 68));
        return b;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                analysis.setAnalyzer(cameraExecutor, image -> {
                    try {
                        Bitmap frame = rgbaToBitmap(image);
                        if (frame == null || frame.isRecycled()) return;
                        int rotation = image.getImageInfo().getRotationDegrees();
                        Bitmap corrected = rotation == 0 ? frame : rotate(frame, rotation);
                        if (corrected == null || corrected.isRecycled()) return;
                        latestFrame = corrected.copy(Bitmap.Config.ARGB_8888, false);
                    } catch (Throwable e) {
                        runOnUiThread(() -> statusText.setText("Kamera görüntü hatası"));
                    } finally {
                        image.close();
                    }
                });

                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Throwable e) {
                Toast.makeText(this, "Kamera başlatılamadı", Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap rgbaToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();
        int pixelStride = Math.max(1, plane.getPixelStride());
        int rowStride = plane.getRowStride();
        int paddedWidth = rowStride / pixelStride;
        Bitmap padded = Bitmap.createBitmap(paddedWidth, image.getHeight(), Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        if (paddedWidth == image.getWidth()) return padded;
        return Bitmap.createBitmap(padded, 0, 0, image.getWidth(), image.getHeight());
    }

    private Bitmap rotate(Bitmap src, int degrees) {
        if (src == null || src.isRecycled()) return null;
        if (degrees == 0) return src;
        Matrix m = new Matrix();
        m.postRotate(degrees);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
    }

    private void analyze(Bitmap source) {
        if (!busy.compareAndSet(false, true)) return;
        statusText.setText("AI tarıyor...");
        detectExecutor.execute(() -> {
            try {
                List<RectF> candidates = new ArrayList<>();
                detectOne(source, 0, 0, source.getWidth(), source.getHeight(), candidates);

                int grid = 4;
                int cellW = Math.max(1, source.getWidth() / grid);
                int cellH = Math.max(1, source.getHeight() / grid);
                int padX = Math.round(cellW * 0.18f);
                int padY = Math.round(cellH * 0.18f);

                for (int gy = 0; gy < grid; gy++) {
                    for (int gx = 0; gx < grid; gx++) {
                        int l = Math.max(0, gx * cellW - padX);
                        int t = Math.max(0, gy * cellH - padY);
                        int r = Math.min(source.getWidth(), (gx + 1) * cellW + padX);
                        int b = Math.min(source.getHeight(), (gy + 1) * cellH + padY);
                        if (r - l > 64 && b - t > 64) detectOne(source, l, t, r, b, candidates);
                    }
                }

                List<RectF> merged = nms(candidates, 0.40f);
                detections = merged;
                runOnUiThread(() -> {
                    overlay.setBoxes(merged);
                    refreshCount();
                    statusText.setText("Tamam: " + merged.size() + " aday");
                });
            } catch (Throwable e) {
                runOnUiThread(() -> {
                    statusText.setText("Algılama hatası");
                    Toast.makeText(this, e.getMessage() == null ? "Algılama hatası" : e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                busy.set(false);
            }
        });
    }

    private void detectOne(Bitmap source, int l, int t, int r, int b, List<RectF> out) throws Exception {
        Bitmap crop = Bitmap.createBitmap(source, l, t, r - l, b - t);
        List<DetectedObject> objects = Tasks.await(detector.process(InputImage.fromBitmap(crop, 0)));
        for (DetectedObject o : objects) {
            android.graphics.Rect rr = o.getBoundingBox();
            RectF g = new RectF(rr.left + l, rr.top + t, rr.right + l, rr.bottom + t);
            if (g.width() >= 8 && g.height() >= 8) out.add(g);
        }
    }

    private List<RectF> nms(List<RectF> src, float threshold) {
        List<RectF> keep = new ArrayList<>();
        src.sort((a, b) -> Float.compare(b.width() * b.height(), a.width() * a.height()));
        for (RectF c : src) {
            boolean dup = false;
            for (RectF k : keep) {
                if (iou(c, k) >= threshold) { dup = true; break; }
            }
            if (!dup) keep.add(c);
        }
        return keep;
    }

    private float iou(RectF a, RectF b) {
        float l = Math.max(a.left, b.left), t = Math.max(a.top, b.top);
        float r = Math.min(a.right, b.right), bot = Math.min(a.bottom, b.bottom);
        if (r <= l || bot <= t) return 0f;
        float inter = (r-l)*(bot-t);
        float union = a.width()*a.height() + b.width()*b.height() - inter;
        return union <= 0 ? 0f : inter / union;
    }

    private void refreshCount() {
        if (!sameMode || sampleRect == null) {
            resultText.setText(String.valueOf(detections.size()));
            return;
        }
        float sampleAspect = aspect(sampleRect);
        int count = 0;
        for (RectF r : detections) {
            float ratio = Math.min(r.width()*r.height(), sampleRect.width()*sampleRect.height()) /
                    Math.max(1f, Math.max(r.width()*r.height(), sampleRect.width()*sampleRect.height()));
            float ad = Math.abs(aspect(r) - sampleAspect);
            if (ratio > 0.35f && ad < 0.8f) count++;
        }
        resultText.setText(String.valueOf(count));
    }

    private float aspect(RectF r) {
        float a = r.width() / Math.max(1f, r.height());
        return Math.max(a, 1f / Math.max(0.001f, a));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detector.close();
        cameraExecutor.shutdownNow();
        detectExecutor.shutdownNow();
    }

    private class Overlay extends View {
        private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint samplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private List<RectF> boxes = new ArrayList<>();
        private RectF sample;

        Overlay(android.content.Context c) {
            super(c);
            setBackgroundColor(Color.TRANSPARENT);
            boxPaint.setColor(Color.rgb(45,245,130));
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(dp(3));
            circlePaint.setColor(Color.rgb(0,165,88));
            circlePaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(dp(17));
            textPaint.setFakeBoldText(true);
            samplePaint.setColor(Color.YELLOW);
            samplePaint.setStyle(Paint.Style.STROKE);
            samplePaint.setStrokeWidth(dp(4));
            setOnTouchListener((v, e) -> {
                if (!sameMode || e.getAction() != android.view.MotionEvent.ACTION_UP) return true;
                RectF best = null;
                float bestArea = Float.MAX_VALUE;
                for (RectF r : boxes) {
                    RectF vr = map(r);
                    if (vr.contains(e.getX(), e.getY())) {
                        float a = vr.width()*vr.height();
                        if (a < bestArea) { best = r; bestArea = a; }
                    }
                }
                if (best != null) {
                    sampleRect = new RectF(best);
                    setSample(sampleRect);
                    refreshCount();
                    statusText.setText("Örnek seçildi");
                }
                return true;
            });
        }

        void setBoxes(List<RectF> b) { boxes = new ArrayList<>(b); invalidate(); }
        void setSample(RectF s) { sample = s == null ? null : new RectF(s); invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            for (int i=0;i<boxes.size();i++) {
                RectF r = map(boxes.get(i));
                c.drawRoundRect(r, dp(8), dp(8), boxPaint);
                float cx = r.centerX();
                float cy = Math.max(dp(86), r.top + dp(2));
                c.drawCircle(cx, cy, dp(17), circlePaint);
                c.drawText(String.valueOf(i+1), cx, cy + dp(6), textPaint);
            }
            if (sample != null) c.drawRoundRect(map(sample), dp(10), dp(10), samplePaint);
        }

        private RectF map(RectF src) {
            Bitmap frame = latestFrame;
            if (frame == null) return new RectF(src);
            float vw = getWidth(), vh = getHeight();
            float sw = frame.getWidth(), sh = frame.getHeight();
            float scale = Math.max(vw/sw, vh/sh);
            float dx = (vw - sw*scale)/2f;
            float dy = (vh - sh*scale)/2f;
            return new RectF(src.left*scale+dx, src.top*scale+dy, src.right*scale+dx, src.bottom*scale+dy);
        }
    }
}
