package com.muhammetgecgil.nesnesayarai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_CAMERA = 10;
    private static final int REQ_IMAGE = 20;
    private static final int REQ_VIDEO = 21;

    enum SourceMode { VIDEO, FOTO, CANLI }
    enum CountMode { AYNI, FARKLI }

    private PreviewView previewView;
    private DetectionOverlay overlay;
    private TextView resultText;
    private TextView statusText;
    private Button sourceButton;
    private Button modeButton;

    private SourceMode sourceMode = SourceMode.VIDEO;
    private CountMode countMode = CountMode.FARKLI;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService detectExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private volatile Bitmap latestBitmap;
    private volatile List<Box> allBoxes = new ArrayList<>();
    private RectF sampleRect;
    private Descriptor sampleDescriptor;

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

        overlay = new DetectionOverlay();
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(14), 0, dp(14), 0);
        top.setBackgroundColor(Color.argb(185, 18, 18, 18));

        TextView title = new TextView(this);
        title.setText("Nesne Sayar AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1f));

        statusText = new TextView(this);
        statusText.setText("Hazır");
        statusText.setTextColor(Color.LTGRAY);
        statusText.setTextSize(12);
        statusText.setGravity(Gravity.CENTER);
        top.addView(statusText, new LinearLayout.LayoutParams(dp(110), dp(54)));

        resultText = new TextView(this);
        resultText.setText("0");
        resultText.setTextColor(Color.rgb(50, 245, 135));
        resultText.setTextSize(34);
        resultText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(resultText, new LinearLayout.LayoutParams(dp(72), dp(54)));

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54), Gravity.TOP);
        root.addView(top, topLp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setBackgroundColor(Color.argb(235, 18, 47, 56));
        sourceButton = makeButton("VIDEO");
        modeButton = makeButton("FARKLI");
        Button countButton = makeButton("SAY");
        Button clearButton = makeButton("SİL");
        countButton.setBackgroundColor(Color.rgb(0, 137, 76));

        bottom.addView(sourceButton, new LinearLayout.LayoutParams(0, dp(58), 1f));
        bottom.addView(modeButton, new LinearLayout.LayoutParams(0, dp(58), 1f));
        bottom.addView(countButton, new LinearLayout.LayoutParams(0, dp(58), 1f));
        bottom.addView(clearButton, new LinearLayout.LayoutParams(0, dp(58), 1f));
        root.addView(bottom, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58), Gravity.BOTTOM));

        setContentView(root);

        sourceButton.setOnClickListener(v -> {
            if (sourceMode == SourceMode.VIDEO) {
                sourceMode = SourceMode.FOTO;
                sourceButton.setText("FOTO");
                chooseImage();
            } else if (sourceMode == SourceMode.FOTO) {
                sourceMode = SourceMode.CANLI;
                sourceButton.setText("CANLI");
                status("Canlı kamera");
            } else {
                sourceMode = SourceMode.VIDEO;
                sourceButton.setText("VIDEO");
                chooseVideo();
            }
        });

        modeButton.setOnClickListener(v -> {
            countMode = countMode == CountMode.FARKLI ? CountMode.AYNI : CountMode.FARKLI;
            modeButton.setText(countMode.name());
            refreshVisible();
        });

        countButton.setOnClickListener(v -> {
            Bitmap b = latestBitmap;
            if (b == null) {
                Toast.makeText(this, "Görüntü henüz hazır değil", Toast.LENGTH_SHORT).show();
                return;
            }
            analyze(b.copy(Bitmap.Config.ARGB_8888, false));
        });

        clearButton.setOnClickListener(v -> {
            allBoxes = new ArrayList<>();
            sampleRect = null;
            sampleDescriptor = null;
            overlay.clearAll();
            resultText.setText("0");
            status("Temizlendi");
        });
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.rgb(20, 58, 68));
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
                        Bitmap b = rgbaToBitmap(image);
                        int rot = image.getImageInfo().getRotationDegrees();
                        if (rot != 0) b = rotate(b, rot);
                        latestBitmap = b;
                        int w = b.getWidth(), h = b.getHeight();
                        overlay.post(() -> overlay.setSourceSize(w, h));
                    } catch (Throwable ignored) {
                    } finally {
                        image.close();
                    }
                });

                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception e) {
                Toast.makeText(this, "Kamera başlatılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap rgbaToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int paddedWidth = rowStride / Math.max(1, pixelStride);
        Bitmap padded = Bitmap.createBitmap(paddedWidth, image.getHeight(), Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        if (paddedWidth == image.getWidth()) return padded;
        Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, image.getWidth(), image.getHeight());
        padded.recycle();
        return cropped;
    }

    private Bitmap rotate(Bitmap src, int degrees) {
        Matrix m = new Matrix();
        m.postRotate(degrees);
        Bitmap out = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        if (out != src) src.recycle();
        return out;
    }

    private void chooseImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, REQ_IMAGE);
    }

    private void chooseVideo() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("video/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, REQ_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            Bitmap b;
            if (requestCode == REQ_IMAGE) {
                b = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                sourceMode = SourceMode.FOTO;
                sourceButton.setText("FOTO");
            } else if (requestCode == REQ_VIDEO) {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this, uri);
                b = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                mmr.release();
                sourceMode = SourceMode.VIDEO;
                sourceButton.setText("VIDEO");
            } else return;
            if (b != null) {
                latestBitmap = b.copy(Bitmap.Config.ARGB_8888, false);
                overlay.setSourceSize(b.getWidth(), b.getHeight());
                status("Kaynak hazır");
                analyze(latestBitmap.copy(Bitmap.Config.ARGB_8888, false));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Dosya açılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void analyze(Bitmap source) {
        if (!busy.compareAndSet(false, true)) return;
        status("AI tarıyor 0/90");
        detectExecutor.execute(() -> {
            try {
                List<Box> candidates = new ArrayList<>();
                int done = 0;
                candidates.addAll(detectCrop(source, new Rect(0, 0, source.getWidth(), source.getHeight()), 3f));
                done++;
                progress(done);
                done = scanGrid(source, 5, 0.30f, candidates, done);
                done = scanGrid(source, 8, 0.30f, candidates, done);

                List<Box> merged = nms(candidates, 0.42f);
                List<Box> roiBoxes = new ArrayList<>();
                for (Box b : merged) if (overlay.isRectInsideRoi(b.rect)) roiBoxes.add(b);
                allBoxes = roiBoxes;
                runOnUiThread(() -> {
                    refreshVisible();
                    status("Tamam: " + roiBoxes.size() + " aday");
                });
            } catch (Throwable e) {
                runOnUiThread(() -> {
                    status("Algılama hatası");
                    Toast.makeText(this, String.valueOf(e.getMessage()), Toast.LENGTH_LONG).show();
                });
            } finally {
                busy.set(false);
                source.recycle();
            }
        });
    }

    private int scanGrid(Bitmap source, int grid, float overlap, List<Box> out, int done) throws Exception {
        int w = source.getWidth(), h = source.getHeight();
        float cellW = w / (float) grid, cellH = h / (float) grid;
        float tileW = cellW * (1f + overlap), tileH = cellH * (1f + overlap);
        for (int gy = 0; gy < grid; gy++) {
            for (int gx = 0; gx < grid; gx++) {
                float cx = (gx + .5f) * cellW;
                float cy = (gy + .5f) * cellH;
                int l = Math.max(0, Math.round(cx - tileW / 2));
                int t = Math.max(0, Math.round(cy - tileH / 2));
                int r = Math.min(w, Math.round(cx + tileW / 2));
                int b = Math.min(h, Math.round(cy + tileH / 2));
                if (r - l >= 64 && b - t >= 64) out.addAll(detectCrop(source, new Rect(l, t, r, b), grid == 8 ? 1f : 2f));
                done++;
                if (done % 5 == 0 || done == 90) progress(done);
            }
        }
        return done;
    }

    private void progress(int n) {
        runOnUiThread(() -> status("AI tarıyor " + Math.min(90, n) + "/90"));
    }

    private List<Box> detectCrop(Bitmap source, Rect cropRect, float scaleScore) throws Exception {
        Bitmap crop = Bitmap.createBitmap(source, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
        List<DetectedObject> objects = Tasks.await(detector.process(InputImage.fromBitmap(crop, 0)));
        List<Box> out = new ArrayList<>();
        for (DetectedObject obj : objects) {
            Rect r = obj.getBoundingBox();
            RectF g = new RectF(r.left + cropRect.left, r.top + cropRect.top, r.right + cropRect.left, r.bottom + cropRect.top);
            if (g.width() < 8 || g.height() < 8) continue;
            float a = g.width() * g.height();
            float whole = source.getWidth() * source.getHeight();
            if (a < whole * 0.00002f) continue;
            out.add(new Box(g, scaleScore));
        }
        crop.recycle();
        return out;
    }

    private List<Box> nms(List<Box> src, float threshold) {
        List<Box> sorted = new ArrayList<>(src);
        Collections.sort(sorted, (a, b) -> Float.compare(b.quality, a.quality));
        List<Box> keep = new ArrayList<>();
        for (Box candidate : sorted) {
            boolean dup = false;
            for (Box k : keep) {
                float iou = iou(k.rect, candidate.rect);
                float ar = areaRatio(k.rect, candidate.rect);
                float dx = k.rect.centerX() - candidate.rect.centerX();
                float dy = k.rect.centerY() - candidate.rect.centerY();
                float centerDist = (float) Math.hypot(dx, dy);
                float small = Math.min(Math.max(k.rect.width(), k.rect.height()), Math.max(candidate.rect.width(), candidate.rect.height()));
                if (iou >= threshold || (ar > 0.45f && centerDist < small * 0.28f)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) keep.add(candidate);
        }
        return keep;
    }

    private float iou(RectF a, RectF b) {
        float l = Math.max(a.left, b.left), t = Math.max(a.top, b.top);
        float r = Math.min(a.right, b.right), bot = Math.min(a.bottom, b.bottom);
        if (r <= l || bot <= t) return 0;
        float inter = (r - l) * (bot - t);
        float union = a.width() * a.height() + b.width() * b.height() - inter;
        return union <= 0 ? 0 : inter / union;
    }

    private float areaRatio(RectF a, RectF b) {
        float aa = a.width() * a.height(), bb = b.width() * b.height();
        return Math.min(aa, bb) / Math.max(1f, Math.max(aa, bb));
    }

    private void refreshVisible() {
        Bitmap b = latestBitmap;
        List<Box> visible = new ArrayList<>();
        if (countMode == CountMode.FARKLI || sampleDescriptor == null || b == null) {
            visible.addAll(allBoxes);
        } else {
            for (Box box : allBoxes) {
                Descriptor d = descriptor(b, box.rect);
                if (similarity(sampleDescriptor, d) >= 0.58) visible.add(box);
            }
        }
        resultText.setText(String.valueOf(visible.size()));
        overlay.setBoxes(visible, countMode == CountMode.AYNI ? sampleRect : null);
    }

    private Descriptor descriptor(Bitmap bmp, RectF rf) {
        int l = clamp((int) rf.left, 0, bmp.getWidth() - 1);
        int t = clamp((int) rf.top, 0, bmp.getHeight() - 1);
        int r = clamp((int) rf.right, l + 1, bmp.getWidth());
        int bot = clamp((int) rf.bottom, t + 1, bmp.getHeight());
        double[] hist = new double[12];
        double rs = 0, gs = 0, bs = 0, edges = 0;
        int n = 0;
        int step = Math.max(2, Math.min(r - l, bot - t) / 36);
        for (int y = t; y < bot; y += step) {
            for (int x = l; x < r; x += step) {
                int c = bmp.getPixel(x, y);
                int rr = Color.red(c), gg = Color.green(c), bb = Color.blue(c);
                rs += rr; gs += gg; bs += bb;
                int lum = (rr + gg + bb) / 3;
                hist[Math.min(11, lum / 22)]++;
                if (x + step < r) {
                    int c2 = bmp.getPixel(x + step, y);
                    int lum2 = (Color.red(c2) + Color.green(c2) + Color.blue(c2)) / 3;
                    if (Math.abs(lum - lum2) > 28) edges++;
                }
                n++;
            }
        }
        if (n == 0) n = 1;
        for (int i = 0; i < hist.length; i++) hist[i] /= n;
        double aspect = (r - l) / (double) Math.max(1, bot - t);
        aspect = Math.max(aspect, 1.0 / Math.max(.001, aspect));
        return new Descriptor(rs / n, gs / n, bs / n, aspect, edges / n, hist);
    }

    private double similarity(Descriptor a, Descriptor b) {
        double cd = Math.sqrt(sq(a.r - b.r) + sq(a.g - b.g) + sq(a.b - b.b));
        double color = 1.0 - Math.min(1.0, cd / 230.0);
        double aspect = 1.0 - Math.min(1.0, Math.abs(a.aspect - b.aspect) / 2.4);
        double edge = 1.0 - Math.min(1.0, Math.abs(a.edge - b.edge) / 0.40);
        double hd = 0;
        for (int i = 0; i < a.hist.length; i++) hd += Math.abs(a.hist[i] - b.hist[i]);
        double hist = 1.0 - Math.min(1.0, hd / 1.6);
        return color * .28 + aspect * .22 + edge * .20 + hist * .30;
    }

    private double sq(double x) { return x * x; }
    private int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private void status(String s) { runOnUiThread(() -> statusText.setText(s)); }

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

    static class Box {
        final RectF rect;
        final float quality;
        Box(RectF rect, float quality) { this.rect = rect; this.quality = quality; }
    }

    static class Descriptor {
        final double r, g, b, aspect, edge;
        final double[] hist;
        Descriptor(double r, double g, double b, double aspect, double edge, double[] hist) {
            this.r = r; this.g = g; this.b = b; this.aspect = aspect; this.edge = edge; this.hist = hist;
        }
    }

    private class DetectionOverlay extends View {
        private final Paint roiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint samplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<PointF> roi = new ArrayList<>();
        private final List<PointF> active = new ArrayList<>();
        private List<Box> boxes = new ArrayList<>();
        private RectF sample;
        private int sourceW = 1, sourceH = 1;
        private float downX, downY;
        private boolean moved;

        DetectionOverlay() {
            super(MainActivity.this);
            setBackgroundColor(Color.TRANSPARENT);
            roiPaint.setColor(Color.CYAN); roiPaint.setStyle(Paint.Style.STROKE); roiPaint.setStrokeWidth(dp(4));
            boxPaint.setColor(Color.rgb(45, 245, 140)); boxPaint.setStyle(Paint.Style.STROKE); boxPaint.setStrokeWidth(dp(3));
            samplePaint.setColor(Color.YELLOW); samplePaint.setStyle(Paint.Style.STROKE); samplePaint.setStrokeWidth(dp(4));
            circlePaint.setColor(Color.rgb(0, 165, 88)); circlePaint.setStyle(Paint.Style.FILL);
            numberPaint.setColor(Color.WHITE); numberPaint.setTextAlign(Paint.Align.CENTER); numberPaint.setTextSize(dp(18)); numberPaint.setFakeBoldText(true);
        }

        void setSourceSize(int w, int h) { sourceW = Math.max(1, w); sourceH = Math.max(1, h); invalidate(); }
        void setBoxes(List<Box> b, RectF s) { boxes = new ArrayList<>(b); sample = s == null ? null : new RectF(s); invalidate(); }
        void clearAll() { roi.clear(); active.clear(); boxes.clear(); sample = null; invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawPoly(canvas, roi, roiPaint, true);
            drawPoly(canvas, active, samplePaint, false);
            for (int i = 0; i < boxes.size(); i++) {
                RectF r = sourceToView(boxes.get(i).rect);
                canvas.drawRoundRect(r, dp(8), dp(8), boxPaint);
                float cx = r.centerX();
                float cy = Math.max(dp(74), r.top + dp(2));
                canvas.drawCircle(cx, cy, dp(18), circlePaint);
                canvas.drawText(String.valueOf(i + 1), cx, cy + dp(6), numberPaint);
            }
            if (sample != null) canvas.drawRoundRect(sourceToView(sample), dp(10), dp(10), samplePaint);
        }

        private void drawPoly(Canvas c, List<PointF> pts, Paint p, boolean close) {
            if (pts.size() < 2) return;
            Path path = new Path(); path.moveTo(pts.get(0).x, pts.get(0).y);
            for (int i = 1; i < pts.size(); i++) path.lineTo(pts.get(i).x, pts.get(i).y);
            if (close && pts.size() > 2) path.close();
            c.drawPath(path, p);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX(); downY = e.getY(); moved = false;
                    active.clear(); active.add(new PointF(downX, downY));
                    return true;
                case MotionEvent.ACTION_MOVE:
                    active.add(new PointF(e.getX(), e.getY()));
                    if (Math.hypot(e.getX() - downX, e.getY() - downY) > dp(20)) moved = true;
                    invalidate(); return true;
                case MotionEvent.ACTION_UP:
                    if (moved && active.size() > 5) {
                        roi.clear();
                        int step = Math.max(1, active.size() / 40);
                        for (int i = 0; i < active.size(); i += step) roi.add(active.get(i));
                        status("ROI hazır");
                    } else if (countMode == CountMode.AYNI) {
                        selectSample(e.getX(), e.getY());
                    }
                    active.clear(); invalidate(); return true;
            }
            return true;
        }

        private void selectSample(float vx, float vy) {
            Box best = null; float bestArea = Float.MAX_VALUE;
            for (Box b : allBoxes) {
                RectF vr = sourceToView(b.rect);
                if (vr.contains(vx, vy)) {
                    float a = vr.width() * vr.height();
                    if (a < bestArea) { best = b; bestArea = a; }
                }
            }
            if (best != null && latestBitmap != null) {
                sampleRect = new RectF(best.rect);
                sampleDescriptor = descriptor(latestBitmap, best.rect);
                status("Örnek seçildi");
                refreshVisible();
            }
        }

        boolean isRectInsideRoi(RectF sourceRect) {
            if (roi.size() < 3) return true;
            RectF v = sourceToView(sourceRect);
            int inside = 0;
            float[] f = {.2f, .5f, .8f};
            for (float fy : f) for (float fx : f) {
                float x = v.left + v.width() * fx, y = v.top + v.height() * fy;
                if (pointInPoly(x, y)) inside++;
            }
            return inside >= 7;
        }

        private boolean pointInPoly(float x, float y) {
            boolean inside = false;
            for (int i = 0, j = roi.size() - 1; i < roi.size(); j = i++) {
                PointF pi = roi.get(i), pj = roi.get(j);
                boolean cross = ((pi.y > y) != (pj.y > y)) &&
                        (x < (pj.x - pi.x) * (y - pi.y) / ((pj.y - pi.y) + 0.00001f) + pi.x);
                if (cross) inside = !inside;
            }
            return inside;
        }

        private RectF sourceToView(RectF r) {
            float vw = Math.max(1, getWidth()), vh = Math.max(1, getHeight());
            float scale = Math.max(vw / sourceW, vh / sourceH);
            float dx = (vw - sourceW * scale) / 2f;
            float dy = (vh - sourceH * scale) / 2f;
            return new RectF(r.left * scale + dx, r.top * scale + dy, r.right * scale + dx, r.bottom * scale + dy);
        }
    }
}
