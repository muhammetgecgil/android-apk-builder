package com.muhammetgecgil.pcbzekapro;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView statusText;
    private TextView resultText;
    private TextView zoomText;
    private SeekBar zoomBar;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private final AtomicBoolean scanRequested = new AtomicBoolean(false);
    private final AtomicBoolean ocrBusy = new AtomicBoolean(false);
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private ScaleGestureDetector scaleDetector;
    private boolean torchOn;

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else statusText.setText("Kamera izni gerekli — Ayarlar > İzinler > Kamera");
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        cameraExecutor = Executors.newSingleThreadExecutor();
        buildUi();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(7, 16, 23));
        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(previewView, new FrameLayout.LayoutParams(-1, -1));

        TextView title = label("PCB ZEKÂ PRO • S24 ULTRA", 17, Color.WHITE);
        title.setBackgroundColor(0xC8071017);
        title.setPadding(dp(16), dp(14), dp(16), dp(14));
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        root.addView(title, titleLp);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(14));
        panel.setBackgroundColor(0xE6071017);

        statusText = label("Kamera hazırlanıyor…", 13, 0xFF75FFF7);
        panel.addView(statusText);
        zoomText = label("Zoom 1.0× • ekrana dokun: netleştir", 13, Color.WHITE);
        panel.addView(zoomText);
        zoomBar = new SeekBar(this);
        zoomBar.setMax(100);
        panel.addView(zoomBar, new LinearLayout.LayoutParams(-1, dp(38)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        Button scan = button("YAZILARI OKU");
        Button torch = button("FLAŞ");
        buttons.addView(scan, new LinearLayout.LayoutParams(0, dp(48), 2));
        buttons.addView(torch, new LinearLayout.LayoutParams(0, dp(48), 1));
        panel.addView(buttons);

        resultText = label("OCR sonucu burada görünecek. Entegre üzerindeki kodu kadraja yaklaştırın.", 14, Color.WHITE);
        resultText.setMaxLines(6);
        resultText.setPadding(0, dp(8), 0, 0);
        panel.addView(resultText);
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        panelLp.setMargins(dp(8), dp(8), dp(8), dp(8));
        root.addView(panel, panelLp);
        setContentView(root);

        scan.setOnClickListener(v -> {
            scanRequested.set(true);
            statusText.setText("OCR: sıradaki net kare analiz ediliyor…");
        });
        torch.setOnClickListener(v -> {
            if (camera == null || !camera.getCameraInfo().hasFlashUnit()) return;
            torchOn = !torchOn;
            camera.getCameraControl().enableTorch(torchOn);
            torch.setText(torchOn ? "FLAŞ AÇIK" : "FLAŞ");
        });
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) { if (user) setLinearZoom(p / 100f); }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                if (camera == null) return false;
                float current = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                camera.getCameraControl().setZoomRatio(current * detector.getScaleFactor());
                return true;
            }
        });
        previewView.setOnTouchListener(this::handlePreviewTouch);
    }

    private boolean handlePreviewTouch(View view, MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP && camera != null && !scaleDetector.isInProgress()) {
            MeteringPoint point = previewView.getMeteringPointFactory().createPoint(event.getX(), event.getY());
            FocusMeteringAction action = new FocusMeteringAction.Builder(point,
                    FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                    .setAutoCancelDuration(4, TimeUnit.SECONDS).build();
            camera.getCameraControl().startFocusAndMetering(action);
            statusText.setText("Netlik kilitlendi");
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().setTargetResolution(new Size(1920, 1080)).build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1920, 1080))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);
                provider.unbindAll();
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
                camera.getCameraControl().setLinearZoom(0f);
                runOnUiThread(() -> statusText.setText("Kamera açık • OCR hazır • dokunarak netleştir"));
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("Kamera açılamadı: " + e.getMessage()));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeFrame(@NonNull ImageProxy proxy) {
        if (!scanRequested.compareAndSet(true, false) || !ocrBusy.compareAndSet(false, true)) {
            proxy.close(); return;
        }
        if (proxy.getImage() == null) { ocrBusy.set(false); proxy.close(); return; }
        InputImage image = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());
        recognizer.process(image)
                .addOnSuccessListener(this::showText)
                .addOnFailureListener(e -> runOnUiThread(() -> statusText.setText("OCR hatası: " + e.getMessage())))
                .addOnCompleteListener(task -> { ocrBusy.set(false); proxy.close(); });
    }

    private void showText(Text text) {
        String value = text.getText().trim();
        runOnUiThread(() -> {
            statusText.setText(value.isEmpty() ? "OCR tamamlandı • yazı bulunamadı" : "OCR tamamlandı • " + text.getTextBlocks().size() + " alan");
            resultText.setText(value.isEmpty() ? "Yazı bulunamadı. Yaklaştırın, dokunarak netleştirin ve tekrar deneyin." : value);
        });
    }

    private void setLinearZoom(float linear) {
        if (camera == null) return;
        camera.getCameraControl().setLinearZoom(Math.max(0f, Math.min(1f, linear)));
        Float ratio = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
        zoomText.setText(String.format(Locale.US, "Zoom %.1f× • ekrana dokun: netleştir", ratio));
    }

    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(13); return b;
    }
    private TextView label(String text, int size, int color) {
        TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color); return v;
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() {
        recognizer.close();
        cameraExecutor.shutdown();
        super.onDestroy();
    }
}
