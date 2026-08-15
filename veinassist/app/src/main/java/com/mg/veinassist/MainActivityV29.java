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
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivityV29 extends Activity {

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
    private Button sensitivityButton, freezeButton, liveButton, atlasButton, maskButton, thermalButton, autoButton;

    private HandlerThread cameraThread, processingThread;
    private Handler cameraHandler, processingHandler;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;
    private ImageReader imageReader;
    private CameraCharacteristics characteristics;
    private Size previewSize, analysisSize;
    private String cameraId;
    private int sensorOrientation = 90;

    private final AtomicBoolean processing = new AtomicBoolean(false);
    private long lastAnalysisMs = 0;
    private final VeinAnalyzerV29 analyzer = new VeinAnalyzerV29();

    private int sensitivity = 21;
    private int viewMode = VeinAnalyzerV29.MODE_LIVE;
    private boolean atlasPrior = false;
    private volatile boolean frozen = false;
    private String region = "AUTO";
    private final String[] regions = {"AUTO", "EL", "ONKOL", "DIRSEK", "UST KOL", "AYAK"};
    private int regionIndex = 0;

    private float zoomRatio = 1.0f;
    private float maxZoomRatio = 5.0f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private volatile float lastSharpness = 0f;
    private volatile boolean focusSeeking = false;
    private volatile boolean focusLocked = false;
    private int focusAttempt = 0;
    private float focusStartSharpness = 0f;
    private MeteringRectangle lastFocusRegion = null;
    private Runnable zoomFocusRunnable;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarColor(Color.BLACK);
        buildUi();
        analyzer.setSensitivity(sensitivity);
        analyzer.setMode(viewMode);
        analyzer.setAtlasPrior(atlasPrior);
        analyzer.setRegion(region);

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        startThreads();
        if (textureView != null && textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else if (textureView != null) {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override protected void onPause() {
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

        header.addView(text("MG VEINASSIST CLINICAL RESEARCH", CYAN, 16, true));
        statusText = text("BOLGE AUTO   GORUNTU --   CIZGI --   TEN --", Color.rgb(162, 179, 190), 10, false);
        header.addView(statusText);
        modeLabel = text("KLINISYEN DOGRULAMASI GEREKLI", WARN, 10, false);
        header.addView(modeLabel);
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(128)));

        imageFrame = new FrameLayout(this);
        imageFrame.setBackground(makeBackground(Color.rgb(0, 4, 8), CYAN, 1));

        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        imageFrame.addView(textureView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        effectView = new ImageView(this);
        effectView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        effectView.setVisibility(View.GONE);
        imageFrame.addView(effectView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlayView = new OverlayView();
        imageFrame.addView(overlayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
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

        root.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(224)));

        TextView footer = text("EGITIM VE ARASTIRMA - TIBBI CIHAZ DEGIL", WARN, 9, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        setContentView(root);

        minus.setOnClickListener(v -> {
            if (sensitivity > 1) sensitivity--;
            updateSensitivity();
        });
        plus.setOnClickListener(v -> {
            if (sensitivity < 99) sensitivity++;
            updateSensitivity();
        });

        liveButton.setOnClickListener(v -> setMode(VeinAnalyzerV29.MODE_LIVE, false));
        atlasButton.setOnClickListener(v -> setMode(VeinAnalyzerV29.MODE_LIVE, true));
        maskButton.setOnClickListener(v -> setMode(VeinAnalyzerV29.MODE_MASK, atlasPrior));
        thermalButton.setOnClickListener(v -> setMode(VeinAnalyzerV29.MODE_THERMAL, false));

        regionMinus.setOnClickListener(v -> {
            regionIndex = (regionIndex - 1 + regions.length) % regions.length;
            region = regions[regionIndex];
            autoButton.setText(region);
            analyzer.setRegion(region);
            updateHeader(null);
        });
        regionPlus.setOnClickListener(v -> {
            regionIndex = (regionIndex + 1) % regions.length;
            region = regions[regionIndex];
            autoButton.setText(region);
            analyzer.setRegion(region);
            updateHeader(null);
        });
        autoButton.setOnClickListener(v -> {
            regionIndex = 0;
            region = "AUTO";
            autoButton.setText("AUTO");
            analyzer.setRegion(region);
            updateHeader(null);
        });

        freezeButton.setOnClickListener(v -> toggleFreeze());

        scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        float next = zoomRatio * detector.getScaleFactor();
                        setZoom(Math.max(1.0f, Math.min(maxZoomRatio, next)));
                        return true;
                    }
                });

        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onSingleTapUp(MotionEvent e) {
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
        if (mode != VeinAnalyzerV29.MODE_THERMAL && !frozen) effectView.setVisibility(View.GONE);
        updateButtonStates();
    }

    private void updateButtonStates() {
        select(liveButton, viewMode == VeinAnalyzerV29.MODE_LIVE && !atlasPrior);
        select(atlasButton, viewMode == VeinAnalyzerV29.MODE_LIVE && atlasPrior);
        select(maskButton, viewMode == VeinAnalyzerV29.MODE_MASK);
        select(thermalButton, viewMode == VeinAnalyzerV29.MODE_THERMAL);
    }

    private void toggleFreeze() {
        frozen = !frozen;
        freezeButton.setText(frozen ? "DEVAM" : "DONDUR");
        if (!frozen) {
            if (viewMode != VeinAnalyzerV29.MODE_THERMAL) effectView.setVisibility(View.GONE);
            return;
        }

        Handler h = processingHandler;
        if (h == null) return;
        h.post(() -> {
            Bitmap shot = null;
            try {
                int w = Math.min(1080, Math.max(540, textureView.getWidth()));
                int vh = Math.max(1, textureView.getHeight());
                int hgt = Math.max(1, (int)(w * (vh / (float)Math.max(1, textureView.getWidth()))));
                shot = textureView.getBitmap(w, hgt);
            } catch (Throwable ignored) {}
            final Bitmap finalShot = shot;
            runOnUiThread(() -> {
                if (frozen && finalShot != null) {
                    effectView.setImageBitmap(finalShot);
                    effectView.setVisibility(View.VISIBLE);
                }
            });
        });
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
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        r.setLayoutParams(rp);
        for (Button b : buttons) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            p.setMargins(dp(3), dp(3), dp(3), dp(3));
            r.addView(b, p);
        }
        return r;
    }

    private void select(Button b, boolean selected) {
        if (b == null) return;
        b.setBackground(makeBackground(selected ? CYAN_DARK : PANEL,
                selected ? CYAN : BORDER, selected ? 2 : 1));
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
        HandlerThread ct = cameraThread;
        HandlerThread pt = processingThread;
        cameraThread = null;
        processingThread = null;
        cameraHandler = null;
        processingHandler = null;
        if (ct != null) ct.quitSafely();
        if (pt != null) pt.quitSafely();
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
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
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;

        CameraManager manager = (CameraManager)getSystemService(CAMERA_SERVICE);
        try {
            chooseCamera(manager);
            if (cameraId == null || characteristics == null) return;
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return;

            previewSize = choosePreviewSize(map.getOutputSizes(SurfaceTexture.class));
            analysisSize = chooseAnalysisSize(map.getOutputSizes(android.graphics.ImageFormat.YUV_420_888));
            sensorOrientation = valueOr(characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION), 90);

            if (Build.VERSION.SDK_INT >= 30) {
                Range<Float> range = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
                if (range != null) maxZoomRatio = Math.min(5.0f, range.getUpper());
            } else {
                Float max = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                if (max != null) maxZoomRatio = Math.min(5.0f, max);
            }

            imageReader = ImageReader.newInstance(
                    analysisSize.getWidth(), analysisSize.getHeight(),
                    android.graphics.ImageFormat.YUV_420_888, 2);
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
                for (int cap : caps) {
                    if (cap == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
                        score += 100;
                }
            }
            Size pixel = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
            if (pixel != null) score += Math.min(30, (pixel.getWidth() * pixel.getHeight()) / 10000000);

            if (score > bestScore) {
                bestScore = score;
                best = id;
                characteristics = c;
            }
        }
        cameraId = best;
    }

    private Size choosePreviewSize(Size[] choices) {
        if (choices == null || choices.length == 0) return new Size(1920, 1440);
        List<Size> list = new ArrayList<>(Arrays.asList(choices));
        list.sort(Comparator.comparingLong(s -> -(long)s.getWidth() * s.getHeight()));

        for (Size s : list) {
            float ar = (float)s.getWidth() / s.getHeight();
            if (s.getWidth() <= 1920 && s.getWidth() >= 1280 &&
                    Math.abs(ar - 4f / 3f) < 0.035f) return s;
        }
        for (Size s : list) if (s.getWidth() <= 1920 && s.getWidth() >= 1280) return s;
        return list.get(list.size() - 1);
    }

    private Size chooseAnalysisSize(Size[] choices) {
        if (choices == null || choices.length == 0) return new Size(640, 480);
        Size best = null;
        long bestDiff = Long.MAX_VALUE;
        for (Size s : choices) {
            float ar = (float)s.getWidth() / s.getHeight();
            if (Math.abs(ar - 4f / 3f) > 0.05f) continue;
            long pixels = (long)s.getWidth() * s.getHeight();
            long diff = Math.abs(pixels - 640L * 480L);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = s;
            }
        }
        if (best != null) return best;
        Size smallest = choices[0];
        for (Size s : choices) {
            if ((long)s.getWidth() * s.getHeight() < (long)smallest.getWidth() * smallest.getHeight())
                smallest = s;
        }
        return smallest;
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
            runOnUiThread(() -> Toast.makeText(MainActivityV29.this,
                    "Kamera hata kodu: " + error, Toast.LENGTH_LONG).show());
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

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, analysisSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override public void onConfigured(CameraCaptureSession session) {
                            if (cameraDevice == null) return;
                            captureSession = session;
                            try {
                                previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                        CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                                captureSession.setRepeatingRequest(
                                        previewBuilder.build(), captureCallback, cameraHandler);
                                cameraHandler.postDelayed(() -> triggerSmartFocus(-1, -1), 350);
                            } catch (CameraAccessException ignored) {}
                        }
                        @Override public void onConfigureFailed(CameraCaptureSession session) {
                            runOnUiThread(() -> Toast.makeText(MainActivityV29.this,
                                    "Kamera oturumu kurulamadi", Toast.LENGTH_LONG).show());
                        }
                    }, cameraHandler);
        } catch (Exception e) {
            Toast.makeText(this, "Kamera oturum hatasi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override public void onCaptureCompleted(
                        CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    if (!focusSeeking) return;
                    Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (af == null) return;
                    if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        boolean hardwareFocused = af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED;
                        float score = lastSharpness;
                        boolean imageAcceptable = score >= 6.5f || score >= focusStartSharpness * 0.97f;

                        if (hardwareFocused && imageAcceptable) {
                            focusSeeking = false;
                            focusLocked = true;
                            applyRepeatingRequest();
                        } else if (focusAttempt < 2 && cameraHandler != null) {
                            cameraHandler.postDelayed(MainActivityV29.this::runFocusAttempt, 260);
                        } else {
                            focusSeeking = false;
                            focusLocked = hardwareFocused;
                            applyRepeatingRequest();
                        }
                    }
                }
            };

    private void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image == null) return;

        long now = SystemClock.elapsedRealtime();
        if (frozen || now - lastAnalysisMs < 150 || !processing.compareAndSet(false, true)) {
            image.close();
            return;
        }

        lastAnalysisMs = now;
        FrameDataV29 frame;
        try {
            frame = FrameDataV29.copy(image);
        } finally {
            image.close();
        }

        Handler h = processingHandler;
        if (h == null) {
            processing.set(false);
            return;
        }

        h.post(() -> {
            try {
                VeinAnalyzerV29.Result result = analyzer.process(frame, sensorOrientation);
                lastSharpness = result.sharpness;
                runOnUiThread(() -> applyResult(result));
            } catch (Throwable ignored) {
            } finally {
                processing.set(false);
            }
        });
    }

    private void applyResult(VeinAnalyzerV29.Result r) {
        if (!frozen) {
            overlayView.setMask(r.overlay);
            if (viewMode == VeinAnalyzerV29.MODE_THERMAL && r.thermal != null) {
                effectView.setImageBitmap(r.thermal);
                effectView.setVisibility(View.VISIBLE);
            } else if (viewMode != VeinAnalyzerV29.MODE_THERMAL) {
                effectView.setVisibility(View.GONE);
            }
        }
        updateHeader(r);
    }

    private void updateHeader(VeinAnalyzerV29.Result r) {
        if (statusText == null) return;
        String foc = focusSeeking ? "AF" : (focusLocked ? "KILIT" : "AF?");
        if (r == null) {
            statusText.setText(String.format("BOLGE %s   %s   ZOOM %.1fx", region, foc, zoomRatio));
        } else {
            statusText.setText(String.format(
                    "BOLGE %s   NET %.0f   CIZGI %d   TEN %d%%   %s   ZOOM %.1fx",
                    region, r.sharpness, r.lineCount, r.skinPercent, foc, zoomRatio));
        }
        if (atlasPrior)
            modeLabel.setText("ANATOMI ONCELIGI - SADECE GORUNTUDEKI ADAYI PUANLAR");
        else
            modeLabel.setText("KLINISYEN DOGRULAMASI GEREKLI");
    }

    private void triggerSmartFocus(float viewX, float viewY) {
        Handler h = cameraHandler;
        if (h == null) return;
        h.post(() -> {
            lastFocusRegion = (viewX >= 0 && viewY >= 0) ? buildMeteringRegion(viewX, viewY) : null;
            focusAttempt = 0;
            focusStartSharpness = lastSharpness;
            focusLocked = false;
            focusSeeking = true;
            runFocusAttempt();
        });
    }

    private void runFocusAttempt() {
        if (captureSession == null || previewBuilder == null || cameraHandler == null) return;
        focusAttempt++;
        focusSeeking = true;
        try {
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

            if (lastFocusRegion != null) {
                Integer maxAf = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
                Integer maxAe = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
                if (maxAf != null && maxAf > 0)
                    previewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,
                            new MeteringRectangle[]{lastFocusRegion});
                if (maxAe != null && maxAe > 0)
                    previewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS,
                            new MeteringRectangle[]{lastFocusRegion});
            }

            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            captureSession.capture(previewBuilder.build(), captureCallback, cameraHandler);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            captureSession.capture(previewBuilder.build(), captureCallback, cameraHandler);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            captureSession.setRepeatingRequest(previewBuilder.build(), captureCallback, cameraHandler);
        } catch (CameraAccessException ignored) {}
    }

    private MeteringRectangle buildMeteringRegion(float x, float y) {
        if (characteristics == null || imageFrame.getWidth() == 0 || imageFrame.getHeight() == 0)
            return null;
        Rect active = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (active == null) return null;

        float nx = Math.max(0f, Math.min(1f, x / imageFrame.getWidth()));
        float ny = Math.max(0f, Math.min(1f, y / imageFrame.getHeight()));
        int sx, sy;

        if (sensorOrientation == 90) {
            sx = active.left + (int)(ny * active.width());
            sy = active.top + (int)((1f - nx) * active.height());
        } else if (sensorOrientation == 270) {
            sx = active.left + (int)((1f - ny) * active.width());
            sy = active.top + (int)(nx * active.height());
        } else {
            sx = active.left + (int)(nx * active.width());
            sy = active.top + (int)(ny * active.height());
        }

        int rw = Math.max(100, active.width() / 10);
        int rh = Math.max(100, active.height() / 10);
        Rect r = new Rect(
                clamp(sx - rw / 2, active.left, active.right - rw),
                clamp(sy - rh / 2, active.top, active.bottom - rh),
                clamp(sx + rw / 2, active.left + rw, active.right),
                clamp(sy + rh / 2, active.top + rh, active.bottom));
        return new MeteringRectangle(r, MeteringRectangle.METERING_WEIGHT_MAX - 1);
    }

    private void setZoom(float zoom) {
        zoomRatio = zoom;
        Handler h = cameraHandler;
        if (h != null) {
            h.post(this::applyRepeatingRequest);
            if (zoomFocusRunnable != null) h.removeCallbacks(zoomFocusRunnable);
            zoomFocusRunnable = () -> triggerSmartFocus(-1, -1);
            h.postDelayed(zoomFocusRunnable, 260);
        }
        updateHeader(null);
    }

    private void applyRepeatingRequest() {
        if (captureSession == null || previewBuilder == null) return;
        try {
            applyZoomToBuilder();
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            captureSession.setRepeatingRequest(previewBuilder.build(), captureCallback, cameraHandler);
        } catch (Exception ignored) {}
    }

    private void applyZoomToBuilder() {
        if (previewBuilder == null || characteristics == null) return;
        if (Build.VERSION.SDK_INT >= 30) {
            Range<Float> range = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
            if (range != null) {
                float z = Math.max(range.getLower(),
                        Math.min(Math.min(range.getUpper(), 5f), zoomRatio));
                previewBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, z);
                return;
            }
        }

        Float max = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Rect active = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (max != null && active != null) {
            float z = Math.max(1f, Math.min(Math.min(max, 5f), zoomRatio));
            int cw = (int)(active.width() / z);
            int ch = (int)(active.height() / z);
            int left = active.centerX() - cw / 2;
            int top = active.centerY() - ch / 2;
            previewBuilder.set(CaptureRequest.SCALER_CROP_REGION,
                    new Rect(left, top, left + cw, top + ch));
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (textureView == null || previewSize == null) return;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect;
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
            bufferRect = new RectF(0, 0, previewSize.getWidth(), previewSize.getHeight());
        else
            bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());

        float cx = viewRect.centerX();
        float cy = viewRect.centerY();
        bufferRect.offset(cx - bufferRect.centerX(), cy - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        float scale = Math.max((float)viewHeight / bufferRect.height(),
                (float)viewWidth / bufferRect.width());
        matrix.postScale(scale, scale, cx, cy);

        int degrees = 0;
        if (rotation == Surface.ROTATION_90) degrees = 90;
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
        focusSeeking = false;
        focusLocked = false;
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

        OverlayView() {
            super(MainActivityV29.this);
            setWillNotDraw(false);
        }

        void setMask(Bitmap b) {
            mask = b;
            invalidate();
        }

        void setAtlas(boolean a) {
            atlas = a;
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (viewMode == VeinAnalyzerV29.MODE_MASK)
                c.drawColor(Color.argb(205, 0, 0, 0));

            if (mask != null && viewMode != VeinAnalyzerV29.MODE_THERMAL) {
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
                paint.setTextSize(dp(9));
                paint.setTypeface(Typeface.MONOSPACE);
                String label = "ANATOMI ONCELIGI - HASTAYA OZEL DEGIL";
                float tw = paint.measureText(label);
                c.drawText(label, Math.max(dp(4), (getWidth() - tw) / 2f), dp(15), paint);
            }
        }
    }
}
