package com.mg.veinassist;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Range;
import android.util.Size;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    private static final int REQ_CAMERA = 42;
    private static final int CYAN = Color.rgb(25, 220, 220);
    private static final int CYAN_DARK = Color.rgb(0, 88, 94);
    private static final int PANEL = Color.rgb(5, 16, 26);
    private static final int BORDER = Color.rgb(54, 93, 114);
    private static final int WARN = Color.rgb(207, 150, 87);

    private TextureView textureView;
    private OverlayView overlayView;
    private ImageView effectView;
    private FrameLayout imageFrame;
    private TextView statusText;
    private TextView modeLabel;
    private Button sensitivityButton;
    private Button freezeButton;
    private Button liveButton;
    private Button atlasButton;
    private Button maskButton;
    private Button thermalButton;
    private Button autoButton;

    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private HandlerThread processingThread;
    private Handler processingHandler;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;
    private ImageReader imageReader;
    private CameraCharacteristics characteristics;
    private Size previewSize;
    private String cameraId;
    private int sensorOrientation = 90;

    private final AtomicBoolean processing = new AtomicBoolean(false);
    private long lastAnalysisMs = 0;
    private final VeinAnalyzer analyzer = new VeinAnalyzer();

    private int sensitivity = 21;
    private int viewMode = VeinAnalyzer.MODE_LIVE;
    private boolean atlasPrior = false;
    private boolean frozen = false;
    private String region = "AUTO";
    private final String[] regions = {"AUTO", "EL", "ONKOL", "DIRSEK", "UST KOL", "AYAK"};
    private int regionIndex = 0;

    private float zoomRatio = 1.0f;
    private float maxZoomRatio = 5.0f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private volatile float lastSharpness = 0f;
    private int focusAttempt = 0;
    private float bestFocusSharpness = 0f;
    private MeteringRectangle lastFocusRegion = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(Color.BLACK);
        buildUi();
        analyzer.setSensitivity(sensitivity);
        analyzer.setMode(viewMode);
        analyzer.setAtlasPrior(atlasPrior);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startThreads();
        if (textureView != null && textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else if (textureView != null) {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopThreads();
        super.onPause();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(2), dp(4), dp(2), dp(2));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(9), dp(8), dp(8), dp(7));
        header.setBackgroundColor(Color.rgb(2, 8, 13));

        TextView title = text("MG VEINASSIST CLINICAL RESEARCH", CYAN, 16, true);
        header.addView(title);

        statusText = text("BOLGE AUTO   GORUNTU --   CIZGI --   TEN --", Color.rgb(162, 179, 190), 10, false);
        header.addView(statusText);

        modeLabel = text("KLINISYEN DOGRULAMASI GEREKLI", WARN, 10, false);
        header.addView(modeLabel);
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(128)));

        imageFrame = new FrameLayout(this);
        imageFrame.setBackground(makeBackground(Color.rgb(0, 4, 8), CYAN, 1));

        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        imageFrame.addView(textureView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        effectView = new ImageView(this);
        effectView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        effectView.setVisibility(View.GONE);
        imageFrame.addView(effectView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlayView = new OverlayView();
        imageFrame.addView(overlayView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        imageLp.setMargins(0, dp(2), 0, dp(4));
        root.addView(imageFrame, imageLp);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(7), dp(3), dp(7), dp(3));
        controls.setBackgroundColor(Color.BLACK);

        liveButton = button("CANLI");
        atlasButton = button("ATLAS");
        maskButton = button("MASKE");
        controls.addView(row(liveButton, atlasButton, maskButton));

        Button minus = button("-");
        sensitivityButton = button("HASSASIYET " + sensitivity);
        Button plus = button("+");
        controls.addView(row(minus, sensitivityButton, plus));

        Button regionMinus = button("BOLGE -");
        autoButton = button("AUTO");
        Button regionPlus = button("BOLGE +");
        controls.addView(row(regionMinus, autoButton, regionPlus));

        freezeButton = button("DONDUR");
        thermalButton = button("TERMAL MOD");
        controls.addView(row(freezeButton, thermalButton));

        root.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(224)));

        TextView footer = text("EGITIM VE ARASTIRMA - TIBBI CIHAZ DEGIL", WARN, 9, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        setContentView(root);

        minus.setOnClickListener(v -> {
            if (sensitivity > 1) sensitivity--;
            updateSensitivity();
        });
        plus.setOnClickListener(v -> {
            if (sensitivity < 99) sensitivity++;
            updateSensitivity();
        });
        liveButton.setOnClickListener(v -> setMode(VeinAnalyzer.MODE_LIVE, false));
        atlasButton.setOnClickListener(v -> setMode(VeinAnalyzer.MODE_LIVE, true));
        maskButton.setOnClickListener(v -> setMode(VeinAnalyzer.MODE_MASK, atlasPrior));
        thermalButton.setOnClickListener(v -> setMode(VeinAnalyzer.MODE_THERMAL, false));

        regionMinus.setOnClickListener(v -> {
            regionIndex = (regionIndex - 1 + regions.length) % regions.length;
            region = regions[regionIndex];
            autoButton.setText(region);
            updateHeader(null);
        });
        regionPlus.setOnClickListener(v -> {
            regionIndex = (regionIndex + 1) % regions.length;
            region = regions[regionIndex];
            autoButton.setText(region);
            updateHeader(null);
        });
        autoButton.setOnClickListener(v -> {
            regionIndex = 0;
            region = "AUTO";
            autoButton.setText("AUTO");
            updateHeader(null);
        });
        freezeButton.setOnClickListener(v -> toggleFreeze());

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float next = zoomRatio * detector.getScaleFactor();
                setZoom(Math.max(1.0f, Math.min(maxZoomRatio, next)));
                return true;
            }
        });
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                triggerSmartFocus(e.getX(), e.getY());
                return true;
            }
        });
        imageFrame.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            return true;
        });

        updateButtonStates();
    }

    private void updateSensitivity() {
        sensitivityButton.setText("HASSASIYET " + sensitivity);
        analyzer.setSensitivity(sensitivity);
    }

    private void setMode(int mode, boolean atlas) {
        viewMode = mode;
        atlasPrior = atlas;
        analyzer.setMode(mode);
        analyzer.setAtlasPrior(atlas);
        overlayView.setAtlas(atlas);
        if (mode != VeinAnalyzer.MODE_THERMAL) effectView.setVisibility(View.GONE);
        updateButtonStates();
    }

    private void updateButtonStates() {
        select(liveButton, viewMode == VeinAnalyzer.MODE_LIVE && !atlasPrior);
        select(atlasButton, viewMode == VeinAnalyzer.MODE_LIVE && atlasPrior);
        select(maskButton, viewMode == VeinAnalyzer.MODE_MASK);
        select(thermalButton, viewMode == VeinAnalyzer.MODE_THERMAL);
    }

    private void toggleFreeze() {
        frozen = !frozen;
        freezeButton.setText(frozen ? "DEVAM" : "DONDUR");
        if (frozen) {
            Bitmap b = textureView.getBitmap();
            if (b != null) {
                effectView.setImageBitmap(b);
                effectView.setVisibility(View.VISIBLE);
            }
        } else {
            if (viewMode != VeinAnalyzer.MODE_THERMAL) effectView.setVisibility(View.GONE);
        }
    }

    private TextView text(String s, int color, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(200, 213, 224));
        b.setTextSize(12);
        b.setTypeface(Typeface.MONOSPACE);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setBackground(makeBackground(PANEL, BORDER, 1));
        return b;
    }

    private LinearLayout row(Button... buttons) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        r.setLayoutParams(rp);
        for (Button b : buttons) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            p.setMargins(dp(3), dp(3), dp(3), dp(3));
            r.addView(b, p);
        }
        return r;
    }

    private void select(Button b, boolean selected) {
        if (b == null) return;
        b.setBackground(makeBackground(selected ? CYAN_DARK : PANEL, selected ? CYAN : BORDER, selected ? 2 : 1));
        b.setTextColor(selected ? Color.WHITE : Color.rgb(200, 213, 224));
    }

    private GradientDrawable makeBackground(int fill, int stroke, int width) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setStroke(dp(width), stroke);
        return g;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void startThreads() {
        if (cameraThread == null) {
            cameraThread = new HandlerThread("VeinCamera");
            cameraThread.start();
            cameraHandler = new Handler(cameraThread.getLooper());
        }
        if (processingThread == null) {
            processingThread = new HandlerThread("VeinProcessing");
            processingThread.start();
            processingHandler = new Handler(processingThread.getLooper());
        }
    }

    private void stopThreads() {
        if (cameraThread != null) {
            cameraThread.quitSafely();
            try { cameraThread.join(); } catch (InterruptedException ignored) {}
            cameraThread = null;
            cameraHandler = null;
        }
        if (processingThread != null) {
            processingThread.quitSafely();
            try { processingThread.join(); } catch (InterruptedException ignored) {}
            processingThread = null;
            processingHandler = null;
        }
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }
        @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }
        @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }
        @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private void openCamera(int viewWidth, int viewHeight) {
        if (cameraDevice != null || cameraHandler == null) return;
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            chooseCamera(manager);
            if (cameraId == null || characteristics == null) return;
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return;
            previewSize = chooseSize(map.getOutputSizes(SurfaceTexture.class));
            sensorOrientation = valueOr(characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION), 90);

            if (Build.VERSION.SDK_INT >= 30) {
                Range<Float> range = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
                if (range != null) maxZoomRatio = Math.min(5.0f, range.getUpper());
            } else {
                Float max = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                if (max != null) maxZoomRatio = Math.min(5.0f, max);
            }

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), android.graphics.ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(this::onImageAvailable, cameraHandler);
            manager.openCamera(cameraId, cameraStateCallback, cameraHandler);
            configureTransform(viewWidth, viewHeight);
        } catch (Exception e) {
            Toast.makeText(this, "Kamera acilamadi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void chooseCamera(CameraManager manager) throws CameraAccessException {
        String best = null;
        int bestScore = -1;
        for (String id : manager.getCameraIdList()) {
            CameraCharacteristics c = manager.getCameraCharacteristics(id);
            Integer facing = c.get(CameraCharacteristics.LENS_FACING);
            if (facing == null || facing != CameraCharacteristics.LENS_FACING_BACK) continue;
            int score = 10;
            int[] caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            if (caps != null) {
                for (int cap : caps) if (cap == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) score += 100;
            }
            android.util.Size pixel = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
            if (pixel != null) score += Math.min(30, (pixel.getWidth() * pixel.getHeight()) / 10000000);
            if (score > bestScore) {
                bestScore = score;
                best = id;
                characteristics = c;
            }
        }
        cameraId = best;
    }

    private Size chooseSize(Size[] choices) {
        if (choices == null || choices.length == 0) return new Size(1280, 960);
        List<Size> list = new ArrayList<>(Arrays.asList(choices));
        list.sort(Comparator.comparingLong(s -> -(long) s.getWidth() * s.getHeight()));
        for (Size s : list) {
            float ar = (float) s.getWidth() / s.getHeight();
            if (s.getWidth() <= 1920 && s.getWidth() >= 1280 && Math.abs(ar - 4f / 3f) < 0.03f) return s;
        }
        for (Size s : list) if (s.getWidth() <= 1920 && s.getWidth() >= 1280) return s;
        return list.get(Math.min(list.size() - 1, 0));
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createSession();
        }
        @Override public void onDisconnected(CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }
        @Override public void onError(CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Kamera hata kodu: " + error, Toast.LENGTH_LONG).show());
        }
    };

    private void createSession() {
        if (cameraDevice == null || !textureView.isAvailable() || imageReader == null) return;
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(texture);
            Surface analysisSurface = imageReader.getSurface();

            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(previewSurface);
            previewBuilder.addTarget(analysisSurface);
            previewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            previewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            applyZoomToBuilder();

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, analysisSurface), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    captureSession = session;
                    try {
                        previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                        captureSession.setRepeatingRequest(previewBuilder.build(), null, cameraHandler);
                        cameraHandler.postDelayed(() -> triggerSmartFocus(-1, -1), 300);
                    } catch (CameraAccessException ignored) {}
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Kamera oturumu kurulamadi", Toast.LENGTH_LONG).show());
                }
            }, cameraHandler);
        } catch (Exception e) {
            Toast.makeText(this, "Kamera oturum hatasi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image == null) return;
        long now = SystemClock.elapsedRealtime();
        if (frozen || now - lastAnalysisMs < 110 || !processing.compareAndSet(false, true)) {
            image.close();
            return;
        }
        lastAnalysisMs = now;
        FrameData frame;
        try {
            frame = FrameData.copy(image);
        } finally {
            image.close();
        }
        if (processingHandler == null) {
            processing.set(false);
            return;
        }
        processingHandler.post(() -> {
            try {
                VeinAnalyzer.Result result = analyzer.process(frame, sensorOrientation);
                lastSharpness = result.sharpness;
                runOnUiThread(() -> applyResult(result));
            } catch (Throwable ignored) {
            } finally {
                processing.set(false);
            }
        });
    }

    private void applyResult(VeinAnalyzer.Result r) {
        if (!frozen) {
            overlayView.setMask(r.overlay);
            if (viewMode == VeinAnalyzer.MODE_THERMAL) {
                effectView.setImageBitmap(r.thermal);
                effectView.setVisibility(View.VISIBLE);
            } else if (!frozen) {
                effectView.setVisibility(View.GONE);
            }
        }
        updateHeader(r);
    }

    private void updateHeader(VeinAnalyzer.Result r) {
        if (statusText == null) return;
        if (r == null) {
            statusText.setText(String.format("BOLGE %s   ZOOM %.1fx", region, zoomRatio));
        } else {
            statusText.setText(String.format("BOLGE %s   NET %.0f   CIZGI %d   TEN %d%%   ZOOM %.1fx", region, r.sharpness, r.lineCount, r.skinPercent, zoomRatio));
        }
        if (atlasPrior) modeLabel.setText("ATLAS ONCELIGI - YAPAY DAMAR CIZMEZ");
        else modeLabel.setText("KLINISYEN DOGRULAMASI GEREKLI");
    }

    private void triggerSmartFocus(float viewX, float viewY) {
        if (cameraHandler == null) return;
        cameraHandler.post(() -> {
            lastFocusRegion = (viewX >= 0 && viewY >= 0) ? buildMeteringRegion(viewX, viewY) : null;
            focusAttempt = 0;
            bestFocusSharpness = lastSharpness;
            runFocusAttempt();
        });
    }

    private void runFocusAttempt() {
        if (captureSession == null || previewBuilder == null || cameraHandler == null) return;
        focusAttempt++;
        try {
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            if (lastFocusRegion != null) {
                Integer maxAf = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
                Integer maxAe = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
                if (maxAf != null && maxAf > 0) previewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{lastFocusRegion});
                if (maxAe != null && maxAe > 0) previewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{lastFocusRegion});
            }
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewBuilder.build(), null, cameraHandler);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            captureSession.capture(previewBuilder.build(), null, cameraHandler);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            captureSession.setRepeatingRequest(previewBuilder.build(), null, cameraHandler);
        } catch (CameraAccessException ignored) {}

        cameraHandler.postDelayed(() -> {
            float score = lastSharpness;
            boolean good = score >= 8.0f;
            boolean improved = score > bestFocusSharpness * 1.03f;
            if (score > bestFocusSharpness) bestFocusSharpness = score;
            if (!good && improved && focusAttempt < 3) {
                runFocusAttempt();
            } else {
                lockFocus();
            }
        }, 850);
    }

    private void lockFocus() {
        if (captureSession == null || previewBuilder == null) return;
        try {
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            captureSession.setRepeatingRequest(previewBuilder.build(), null, cameraHandler);
        } catch (CameraAccessException ignored) {}
    }

    private MeteringRectangle buildMeteringRegion(float x, float y) {
        if (characteristics == null || imageFrame.getWidth() == 0 || imageFrame.getHeight() == 0) return null;
        Rect active = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (active == null) return null;
        float nx = Math.max(0f, Math.min(1f, x / imageFrame.getWidth()));
        float ny = Math.max(0f, Math.min(1f, y / imageFrame.getHeight()));
        int sx;
        int sy;
        if (sensorOrientation == 90) {
            sx = active.left + (int) (ny * active.width());
            sy = active.top + (int) ((1f - nx) * active.height());
        } else if (sensorOrientation == 270) {
            sx = active.left + (int) ((1f - ny) * active.width());
            sy = active.top + (int) (nx * active.height());
        } else {
            sx = active.left + (int) (nx * active.width());
            sy = active.top + (int) (ny * active.height());
        }
        int rw = Math.max(80, active.width() / 8);
        int rh = Math.max(80, active.height() / 8);
        Rect r = new Rect(
                clamp(sx - rw / 2, active.left, active.right - rw),
                clamp(sy - rh / 2, active.top, active.bottom - rh),
                clamp(sx + rw / 2, active.left + rw, active.right),
                clamp(sy + rh / 2, active.top + rh, active.bottom));
        return new MeteringRectangle(r, MeteringRectangle.METERING_WEIGHT_MAX - 1);
    }

    private void setZoom(float zoom) {
        zoomRatio = zoom;
        if (cameraHandler != null) cameraHandler.post(this::applyRepeatingRequest);
        updateHeader(null);
    }

    private void applyRepeatingRequest() {
        if (captureSession == null || previewBuilder == null) return;
        try {
            applyZoomToBuilder();
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            captureSession.setRepeatingRequest(previewBuilder.build(), null, cameraHandler);
        } catch (Exception ignored) {}
    }

    private void applyZoomToBuilder() {
        if (previewBuilder == null || characteristics == null) return;
        if (Build.VERSION.SDK_INT >= 30) {
            Range<Float> range = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
            if (range != null) {
                float z = Math.max(range.getLower(), Math.min(Math.min(range.getUpper(), 5f), zoomRatio));
                previewBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, z);
                return;
            }
        }
        Float max = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Rect active = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (max != null && active != null) {
            float z = Math.max(1f, Math.min(Math.min(max, 5f), zoomRatio));
            int cw = (int) (active.width() / z);
            int ch = (int) (active.height() / z);
            int left = active.centerX() - cw / 2;
            int top = active.centerY() - ch / 2;
            previewBuilder.set(CaptureRequest.SCALER_CROP_REGION, new Rect(left, top, left + cw, top + ch));
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (textureView == null || previewSize == null) return;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect;
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            bufferRect = new RectF(0, 0, previewSize.getWidth(), previewSize.getHeight());
        } else {
            bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        }
        float cx = viewRect.centerX();
        float cy = viewRect.centerY();
        bufferRect.offset(cx - bufferRect.centerX(), cy - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        float scale = Math.max((float) viewHeight / bufferRect.height(), (float) viewWidth / bufferRect.width());
        matrix.postScale(scale, scale, cx, cy);
        int degrees = 0;
        if (rotation == Surface.ROTATION_0) degrees = 0;
        else if (rotation == Surface.ROTATION_90) degrees = 90;
        else if (rotation == Surface.ROTATION_180) degrees = 180;
        else if (rotation == Surface.ROTATION_270) degrees = 270;
        matrix.postRotate(degrees, cx, cy);
        textureView.setTransform(matrix);
    }

    private void closeCamera() {
        try { if (captureSession != null) captureSession.close(); } catch (Exception ignored) {}
        try { if (cameraDevice != null) cameraDevice.close(); } catch (Exception ignored) {}
        try { if (imageReader != null) imageReader.close(); } catch (Exception ignored) {}
        captureSession = null;
        cameraDevice = null;
        imageReader = null;
        previewBuilder = null;
        processing.set(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (textureView.isAvailable()) openCamera(textureView.getWidth(), textureView.getHeight());
        } else if (requestCode == REQ_CAMERA) {
            Toast.makeText(this, "Kamera izni gerekli", Toast.LENGTH_LONG).show();
        }
    }

    private int valueOr(Integer v, int fallback) { return v == null ? fallback : v; }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private class OverlayView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private Bitmap mask;
        private boolean atlas = false;

        OverlayView() { super(MainActivity.this); setWillNotDraw(false); }
        void setMask(Bitmap b) { mask = b; invalidate(); }
        void setAtlas(boolean a) { atlas = a; invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (viewMode == VeinAnalyzer.MODE_MASK) c.drawColor(Color.argb(205, 0, 0, 0));
            if (mask != null && viewMode != VeinAnalyzer.MODE_THERMAL) {
                Rect dst = new Rect(0, 0, getWidth(), getHeight());
                c.drawBitmap(mask, null, dst, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.argb(190, 44, 225, 215));
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            c.drawLine(cx - dp(22), cy, cx - dp(6), cy, paint);
            c.drawLine(cx + dp(6), cy, cx + dp(22), cy, paint);
            c.drawLine(cx, cy - dp(22), cx, cy - dp(6), paint);
            c.drawLine(cx, cy + dp(6), cx, cy + dp(22), paint);
            if (atlas) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(WARN);
                paint.setTextSize(dp(10));
                paint.setTypeface(Typeface.MONOSPACE);
                String label = "ATLAS ONCELIGI - YAPAY DAMAR CIZMEZ";
                float tw = paint.measureText(label);
                c.drawText(label, Math.max(dp(4), (getWidth() - tw) / 2f), dp(15), paint);
            }
        }
    }

    private static class FrameData {
        final int width, height;
        final byte[] y, u, v;
        final int yRowStride, uvRowStride, uvPixelStride;

        FrameData(int width, int height, byte[] y, byte[] u, byte[] v, int yRowStride, int uvRowStride, int uvPixelStride) {
            this.width = width; this.height = height; this.y = y; this.u = u; this.v = v;
            this.yRowStride = yRowStride; this.uvRowStride = uvRowStride; this.uvPixelStride = uvPixelStride;
        }

        static FrameData copy(Image image) {
            Image.Plane[] p = image.getPlanes();
            ByteBuffer yb = p[0].getBuffer().duplicate();
            ByteBuffer ub = p[1].getBuffer().duplicate();
            ByteBuffer vb = p[2].getBuffer().duplicate();
            byte[] y = new byte[yb.remaining()];
            byte[] u = new byte[ub.remaining()];
            byte[] v = new byte[vb.remaining()];
            yb.get(y); ub.get(u); vb.get(v);
            return new FrameData(image.getWidth(), image.getHeight(), y, u, v,
                    p[0].getRowStride(), p[1].getRowStride(), p[1].getPixelStride());
        }
    }

    private static class VeinAnalyzer {
        static final int MODE_LIVE = 0;
        static final int MODE_MASK = 1;
        static final int MODE_THERMAL = 2;
        private static final int W = 320;
        private static final int H = 240;
        private static final int N = W * H;

        private int sensitivity = 21;
        private int mode = MODE_LIVE;
        private boolean atlasPrior = false;
        private float[] temporal = new float[N];
        private byte[] persistence = new byte[N];

        void setSensitivity(int s) { sensitivity = Math.max(1, Math.min(99, s)); }
        void setMode(int m) { mode = m; }
        void setAtlasPrior(boolean b) { atlasPrior = b; }

        static class Result {
            Bitmap overlay;
            Bitmap thermal;
            float sharpness;
            int skinPercent;
            int lineCount;
        }

        Result process(FrameData f, int sensorOrientation) {
            float[] lum = new float[N];
            float[] green = new float[N];
            float[] cb = new float[N];
            float[] cr = new float[N];

            for (int y = 0; y < H; y++) {
                int sy = y * f.height / H;
                for (int x = 0; x < W; x++) {
                    int sx = x * f.width / W;
                    int yi = Math.min(f.y.length - 1, sy * f.yRowStride + sx);
                    int ui = Math.min(f.u.length - 1, (sy / 2) * f.uvRowStride + (sx / 2) * f.uvPixelStride);
                    int vi = Math.min(f.v.length - 1, (sy / 2) * f.uvRowStride + (sx / 2) * f.uvPixelStride);
                    int Y = f.y[yi] & 0xff;
                    int U = (f.u[ui] & 0xff) - 128;
                    int V = (f.v[vi] & 0xff) - 128;
                    int r = clamp255((int) (Y + 1.402f * V));
                    int g = clamp255((int) (Y - 0.344136f * U - 0.714136f * V));
                    int b = clamp255((int) (Y + 1.772f * U));
                    int i = y * W + x;
                    lum[i] = Y;
                    green[i] = g;
                    cb[i] = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b;
                    cr[i] = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b;
                }
            }

            float sharpness = computeSharpness(lum);
            boolean[] skin = buildSkinMask(lum, cb, cr);
            float[] skinFloat = new float[N];
            int skinCount = 0;
            for (int i = 0; i < N; i++) {
                if (skin[i]) { skinFloat[i] = 1f; skinCount++; }
            }
            float[] skinDensity = boxMean(skinFloat, W, H, 3);

            float[] m2 = boxMean(green, W, H, 2);
            float[] m5 = boxMean(green, W, H, 5);
            float[] m10 = boxMean(green, W, H, 10);
            float[] raw = new float[N];
            float threshold = 8.72f - sensitivity * 0.022f;

            for (int i = 0; i < N; i++) {
                if (!skin[i] || skinDensity[i] < 0.68f || lum[i] <= 58f) {
                    raw[i] = 0f;
                    continue;
                }
                float thin = Math.max(0f, m2[i] - green[i]);
                float mid = Math.max(0f, m5[i] - green[i]);
                float wide = Math.max(0f, m10[i] - green[i]);
                float ridge = Math.max(mid, wide * 0.86f) - thin * 0.46f;
                raw[i] = Math.max(0f, ridge);
            }

            float skinAxis = principalAxis(skin);
            float[] continuous = new float[N];
            byte[] bestDir = new byte[N];
            int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};
            for (int y = 5; y < H - 5; y++) {
                for (int x = 5; x < W - 5; x++) {
                    int i = y * W + x;
                    float base = raw[i];
                    if (base < threshold * 0.42f) continue;
                    float best = 0f;
                    int bestCount = 0;
                    int bestK = 0;
                    for (int k = 0; k < dirs.length; k++) {
                        int dx = dirs[k][0], dy = dirs[k][1];
                        float sum = 0f;
                        int count = 0;
                        for (int d : new int[]{2,4}) {
                            int i1 = (y + dy * d) * W + (x + dx * d);
                            int i2 = (y - dy * d) * W + (x - dx * d);
                            sum += raw[i1] + raw[i2];
                            if (raw[i1] > threshold * 0.30f) count++;
                            if (raw[i2] > threshold * 0.30f) count++;
                        }
                        float support = sum / 4f;
                        if (support > best) { best = support; bestCount = count; bestK = k; }
                    }
                    if (bestCount < 2) continue;
                    float factor = 0.72f + Math.min(0.55f, best / Math.max(2f, base) * 0.34f);
                    float score = base * factor;
                    if (atlasPrior) {
                        float angle = (float) Math.atan2(dirs[bestK][1], dirs[bestK][0]);
                        float diff = angleDiff(angle, skinAxis);
                        score *= diff < Math.toRadians(50) ? 1.08f : 0.97f;
                    }
                    continuous[i] = score;
                    bestDir[i] = (byte) bestK;
                }
            }

            boolean[] candidate = new boolean[N];
            for (int i = 0; i < N; i++) {
                float s = continuous[i];
                temporal[i] = temporal[i] * 0.70f + s * 0.30f;
                if (s > threshold * 0.72f) persistence[i] = (byte) Math.min(6, persistence[i] + 1);
                else persistence[i] = (byte) Math.max(0, persistence[i] - 1);
                candidate[i] = skin[i] && skinDensity[i] >= 0.68f && lum[i] > 58f && temporal[i] > threshold && persistence[i] >= 2;
            }

            ComponentResult filtered = filterContinuousComponents(candidate);
            boolean[] keep = filtered.keep;
            int[] pixels = new int[N];
            for (int i = 0; i < N; i++) {
                if (keep[i]) {
                    float a = Math.min(0.86f, 0.45f + temporal[i] / 35f);
                    pixels[i] = Color.argb((int) (255 * a), 0, 235, 225);
                } else pixels[i] = Color.TRANSPARENT;
            }
            Bitmap sensorMask = Bitmap.createBitmap(pixels, W, H, Bitmap.Config.ARGB_8888);
            Bitmap overlay = rotate(sensorMask, sensorOrientation == 270 ? 270 : 90);

            int[] thermalPx = new int[N];
            for (int i = 0; i < N; i++) thermalPx[i] = thermalColor((int) lum[i]);
            Bitmap thermal = rotate(Bitmap.createBitmap(thermalPx, W, H, Bitmap.Config.ARGB_8888), sensorOrientation == 270 ? 270 : 90);

            Result result = new Result();
            result.overlay = overlay;
            result.thermal = thermal;
            result.sharpness = sharpness;
            result.skinPercent = Math.round(100f * skinCount / N);
            result.lineCount = filtered.components;
            return result;
        }

        private boolean[] buildSkinMask(float[] lum, float[] cb, float[] cr) {
            float refCb = 0f, refCr = 0f;
            int refN = 0;
            for (int y = H * 35 / 100; y < H * 65 / 100; y++) {
                for (int x = W * 35 / 100; x < W * 65 / 100; x++) {
                    int i = y * W + x;
                    if (lum[i] > 35 && lum[i] < 245 && cb[i] > 68 && cb[i] < 155 && cr[i] > 115 && cr[i] < 198) {
                        refCb += cb[i]; refCr += cr[i]; refN++;
                    }
                }
            }
            if (refN > 0) { refCb /= refN; refCr /= refN; }

            float[] raw = new float[N];
            for (int i = 0; i < N; i++) {
                boolean broad = lum[i] > 28 && lum[i] < 250 && cb[i] > 65 && cb[i] < 158 && cr[i] > 112 && cr[i] < 202;
                boolean adaptive = true;
                if (refN > 20) {
                    float dc = cb[i] - refCb;
                    float dr = cr[i] - refCr;
                    adaptive = dc * dc + dr * dr < 42f * 42f;
                }
                raw[i] = (broad && adaptive) ? 1f : 0f;
            }
            float[] smooth = boxMean(raw, W, H, 2);
            boolean[] mask = new boolean[N];
            for (int i = 0; i < N; i++) mask[i] = smooth[i] >= 0.56f;
            return keepBestSkinComponent(mask);
        }

        private boolean[] keepBestSkinComponent(boolean[] src) {
            int[] label = new int[N];
            int[] queue = new int[N];
            int nextLabel = 0;
            int bestLabel = 0;
            double bestScore = 0;
            int cx = W / 2, cy = H / 2;
            for (int start = 0; start < N; start++) {
                if (!src[start] || label[start] != 0) continue;
                nextLabel++;
                int head = 0, tail = 0;
                queue[tail++] = start;
                label[start] = nextLabel;
                int size = 0;
                int minDist2 = Integer.MAX_VALUE;
                while (head < tail) {
                    int p = queue[head++];
                    size++;
                    int x = p % W, y = p / W;
                    int dx = x - cx, dy = y - cy;
                    minDist2 = Math.min(minDist2, dx * dx + dy * dy);
                    for (int oy = -1; oy <= 1; oy++) {
                        for (int ox = -1; ox <= 1; ox++) {
                            if (ox == 0 && oy == 0) continue;
                            int nx = x + ox, ny = y + oy;
                            if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                            int q = ny * W + nx;
                            if (src[q] && label[q] == 0) { label[q] = nextLabel; queue[tail++] = q; }
                        }
                    }
                }
                double centerBonus = 1.0 + 1.8 * Math.max(0, 1.0 - Math.sqrt(minDist2) / (Math.min(W, H) * 0.55));
                double score = size * centerBonus;
                if (score > bestScore) { bestScore = score; bestLabel = nextLabel; }
            }
            boolean[] out = new boolean[N];
            if (bestLabel == 0) return out;
            for (int i = 0; i < N; i++) out[i] = label[i] == bestLabel;
            return out;
        }

        private float principalAxis(boolean[] skin) {
            double sx = 0, sy = 0;
            int n = 0;
            for (int i = 0; i < N; i++) if (skin[i]) { sx += i % W; sy += i / W; n++; }
            if (n < 10) return 0f;
            double mx = sx / n, my = sy / n;
            double xx = 0, yy = 0, xy = 0;
            for (int i = 0; i < N; i++) if (skin[i]) {
                double dx = i % W - mx, dy = i / W - my;
                xx += dx * dx; yy += dy * dy; xy += dx * dy;
            }
            return (float) (0.5 * Math.atan2(2 * xy, xx - yy));
        }

        private ComponentResult filterContinuousComponents(boolean[] src) {
            int[] label = new int[N];
            int[] queue = new int[N];
            boolean[] keep = new boolean[N];
            int id = 0;
            int keptComponents = 0;
            for (int start = 0; start < N; start++) {
                if (!src[start] || label[start] != 0) continue;
                id++;
                int head = 0, tail = 0;
                queue[tail++] = start;
                label[start] = id;
                int minX = W, maxX = 0, minY = H, maxY = 0;
                while (head < tail) {
                    int p = queue[head++];
                    int x = p % W, y = p / W;
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    for (int oy = -1; oy <= 1; oy++) {
                        for (int ox = -1; ox <= 1; ox++) {
                            if (ox == 0 && oy == 0) continue;
                            int nx = x + ox, ny = y + oy;
                            if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                            int q = ny * W + nx;
                            if (src[q] && label[q] == 0) { label[q] = id; queue[tail++] = q; }
                        }
                    }
                }
                int size = tail;
                int bw = maxX - minX + 1, bh = maxY - minY + 1;
                float aspect = Math.max((float) bw / Math.max(1, bh), (float) bh / Math.max(1, bw));
                boolean accept = size >= 7 && (aspect >= 1.45f || size >= 30);
                if (accept) {
                    keptComponents++;
                    for (int k = 0; k < tail; k++) keep[queue[k]] = true;
                }
            }
            return new ComponentResult(keep, keptComponents);
        }

        private static class ComponentResult {
            final boolean[] keep;
            final int components;
            ComponentResult(boolean[] keep, int components) { this.keep = keep; this.components = components; }
        }

        private float computeSharpness(float[] lum) {
            double sum = 0;
            int count = 0;
            for (int y = 2; y < H - 2; y += 2) {
                for (int x = 2; x < W - 2; x += 2) {
                    int i = y * W + x;
                    float lap = 4f * lum[i] - lum[i - 1] - lum[i + 1] - lum[i - W] - lum[i + W];
                    sum += Math.abs(lap);
                    count++;
                }
            }
            return count == 0 ? 0f : (float) (sum / count);
        }

        private static float[] boxMean(float[] src, int w, int h, int r) {
            double[] ii = new double[(w + 1) * (h + 1)];
            for (int y = 1; y <= h; y++) {
                double row = 0;
                for (int x = 1; x <= w; x++) {
                    row += src[(y - 1) * w + (x - 1)];
                    ii[y * (w + 1) + x] = ii[(y - 1) * (w + 1) + x] + row;
                }
            }
            float[] out = new float[w * h];
            for (int y = 0; y < h; y++) {
                int y0 = Math.max(0, y - r), y1 = Math.min(h - 1, y + r);
                for (int x = 0; x < w; x++) {
                    int x0 = Math.max(0, x - r), x1 = Math.min(w - 1, x + r);
                    int a = y0 * (w + 1) + x0;
                    int b = y0 * (w + 1) + x1 + 1;
                    int c = (y1 + 1) * (w + 1) + x0;
                    int d = (y1 + 1) * (w + 1) + x1 + 1;
                    double sum = ii[d] - ii[b] - ii[c] + ii[a];
                    out[y * w + x] = (float) (sum / ((x1 - x0 + 1) * (y1 - y0 + 1)));
                }
            }
            return out;
        }

        private static float angleDiff(float a, float b) {
            float d = Math.abs(a - b);
            while (d > Math.PI) d -= Math.PI;
            if (d > Math.PI / 2) d = (float) Math.PI - d;
            return Math.abs(d);
        }

        private static Bitmap rotate(Bitmap b, int degrees) {
            Matrix m = new Matrix();
            m.postRotate(degrees);
            return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
        }

        private static int thermalColor(int y) {
            float t = Math.max(0f, Math.min(1f, y / 255f));
            int r, g, b;
            if (t < 0.25f) {
                float q = t / 0.25f; r = (int) (30 * q); g = 0; b = (int) (80 + 175 * q);
            } else if (t < 0.50f) {
                float q = (t - 0.25f) / 0.25f; r = (int) (30 + 225 * q); g = (int) (80 * q); b = (int) (255 - 155 * q);
            } else if (t < 0.75f) {
                float q = (t - 0.50f) / 0.25f; r = 255; g = (int) (80 + 175 * q); b = (int) (100 * (1 - q));
            } else {
                float q = (t - 0.75f) / 0.25f; r = 255; g = 255; b = (int) (255 * q);
            }
            return Color.rgb(clamp255(r), clamp255(g), clamp255(b));
        }

        private static int clamp255(int x) { return x < 0 ? 0 : (x > 255 ? 255 : x); }
    }
}
