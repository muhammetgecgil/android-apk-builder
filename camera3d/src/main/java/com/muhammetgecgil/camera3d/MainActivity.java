package com.muhammetgecgil.camera3d;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Arrays;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int REQ_CAMERA = 70;

    private TextureView baseView;
    private TextureView depthView;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private float basePitch;
    private float baseRoll;
    private boolean orientationReady = false;
    private boolean mode3d = true;
    private boolean openingCamera = false;
    private float depthPx = 12f;
    private TextView status;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
            }
        }

        buildUi();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationSensor = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                : null;

        startCameraThread();

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            tryOpenWhenReady();
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        baseView = new TextureView(this);
        depthView = new TextureView(this);
        depthView.setAlpha(0.34f);

        root.addView(baseView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(depthView, new FrameLayout.LayoutParams(-1, -1));

        TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture s, int w, int h) {
                tryOpenWhenReady();
            }

            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) {}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s) { return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture s) {}
        };

        baseView.setSurfaceTextureListener(listener);
        depthView.setSurfaceTextureListener(listener);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(22, 18, 22, 18);
        top.setBackgroundColor(0x66000000);

        TextView title = new TextView(this);
        title.setText("CAMERA 3D • LIVE");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);

        status = new TextView(this);
        status.setText("Kamera hazırlanıyor…");
        status.setTextSize(13);
        status.setTextColor(0xffdddddd);

        top.addView(title);
        top.addView(status);

        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        tp.setMargins(16, 16, 16, 0);
        root.addView(top, tp);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(22, 18, 22, 22);
        controls.setBackgroundColor(0xaa111111);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button toggle = new Button(this);
        toggle.setText("3D AÇIK");
        toggle.setOnClickListener(v -> {
            mode3d = !mode3d;
            toggle.setText(mode3d ? "3D AÇIK" : "NORMAL");
            depthView.setVisibility(mode3d ? View.VISIBLE : View.GONE);
            if (!mode3d) {
                depthView.setTranslationX(0f);
                depthView.setTranslationY(0f);
            }
        });

        Button reset = new Button(this);
        reset.setText("MERKEZLE");
        reset.setOnClickListener(v -> {
            orientationReady = false;
            depthView.setTranslationX(depthPx * 0.55f);
            depthView.setTranslationY(0f);
        });

        row.addView(toggle, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(reset, new LinearLayout.LayoutParams(0, -2, 1f));
        controls.addView(row);

        TextView depthTitle = new TextView(this);
        depthTitle.setText("3D DERİNLİK");
        depthTitle.setTextColor(Color.WHITE);
        depthTitle.setPadding(8, 10, 8, 0);
        controls.addView(depthTitle);

        SeekBar depth = new SeekBar(this);
        depth.setMax(40);
        depth.setProgress(12);
        depth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                depthPx = p;
                if (mode3d && !orientationReady) {
                    depthView.setTranslationX(depthPx * 0.55f);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        controls.addView(depth);

        TextView hint = new TextView(this);
        hint.setText("Telefonu hafifçe sağa-sola hareket ettir. İkinci kamera katmanı jiroskopa göre kayarak 3D parallax oluşturur.");
        hint.setTextColor(0xffcccccc);
        hint.setTextSize(12);
        controls.addView(hint);

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        cp.setMargins(16, 0, 16, 22);
        root.addView(controls, cp);

        setContentView(root);
    }

    private void startCameraThread() {
        if (cameraThread != null) return;
        cameraThread = new HandlerThread("camera3d");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void tryOpenWhenReady() {
        if (isFinishing() || isDestroyed()) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        if (baseView == null || depthView == null) return;
        if (!baseView.isAvailable() || !depthView.isAvailable()) return;
        if (camera != null || openingCamera) return;
        if (cameraHandler == null) startCameraThread();
        openBackCamera();
    }

    private void openBackCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            status.setText("Kamera servisi bulunamadı");
            return;
        }

        try {
            String[] ids = manager.getCameraIdList();
            if (ids.length == 0) {
                status.setText("Kamera bulunamadı");
                return;
            }

            String selected = null;
            for (String id : ids) {
                Integer facing = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    selected = id;
                    break;
                }
            }
            if (selected == null) selected = ids[0];

            openingCamera = true;
            status.setText("Arka kamera açılıyor…");

            manager.openCamera(selected, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice c) {
                    openingCamera = false;
                    camera = c;
                    createPreview();
                }

                @Override
                public void onDisconnected(CameraDevice c) {
                    openingCamera = false;
                    c.close();
                    camera = null;
                    runOnUiThread(() -> status.setText("Kamera bağlantısı kesildi"));
                }

                @Override
                public void onError(CameraDevice c, int error) {
                    openingCamera = false;
                    c.close();
                    camera = null;
                    runOnUiThread(() -> status.setText("Kamera hatası: " + error));
                }
            }, cameraHandler);

        } catch (SecurityException e) {
            openingCamera = false;
            status.setText("Kamera izni gerekli");
        } catch (Exception e) {
            openingCamera = false;
            status.setText("Kamera açılamadı");
        }
    }

    private void createPreview() {
        CameraDevice currentCamera = camera;
        if (currentCamera == null) return;

        try {
            SurfaceTexture t1 = baseView.getSurfaceTexture();
            SurfaceTexture t2 = depthView.getSurfaceTexture();
            if (t1 == null || t2 == null) {
                runOnUiThread(() -> status.setText("Kamera yüzeyi hazır değil"));
                return;
            }

            t1.setDefaultBufferSize(1280, 720);
            t2.setDefaultBufferSize(1280, 720);

            Surface s1 = new Surface(t1);
            Surface s2 = new Surface(t2);

            CaptureRequest.Builder req = currentCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            req.addTarget(s1);
            req.addTarget(s2);
            req.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            req.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            currentCamera.createCaptureSession(Arrays.asList(s1, s2), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cs) {
                    if (camera == null) {
                        cs.close();
                        return;
                    }
                    session = cs;
                    try {
                        cs.setRepeatingRequest(req.build(), null, cameraHandler);
                        runOnUiThread(() -> {
                            depthView.setTranslationX(depthPx * 0.55f);
                            status.setText("CANLI • 3D PARALLAX AKTİF");
                        });
                    } catch (CameraAccessException | IllegalStateException e) {
                        runOnUiThread(() -> status.setText("Önizleme başlatılamadı"));
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cs) {
                    runOnUiThread(() -> status.setText("Çift kamera yüzeyi desteklenmedi"));
                }
            }, cameraHandler);

        } catch (CameraAccessException | IllegalStateException e) {
            runOnUiThread(() -> status.setText("3D önizleme kurulamadı"));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;

        float[] r = new float[9];
        float[] o = new float[3];
        SensorManager.getRotationMatrixFromVector(r, e.values);
        SensorManager.getOrientation(r, o);

        if (!orientationReady) {
            basePitch = o[1];
            baseRoll = o[2];
            orientationReady = true;
            return;
        }

        float dx = clampAngle(o[2] - baseRoll);
        float dy = clampAngle(o[1] - basePitch);
        updateParallax(dx, dy);
    }

    private float clampAngle(float a) {
        while (a > Math.PI) a -= (float) (2 * Math.PI);
        while (a < -Math.PI) a += (float) (2 * Math.PI);
        return a;
    }

    private void updateParallax(float roll, float pitch) {
        if (!mode3d || depthView == null) return;

        float x = Math.max(-1f, Math.min(1f, roll * 2.2f));
        float y = Math.max(-1f, Math.min(1f, pitch * 1.8f));

        depthView.setTranslationX(depthPx * (0.55f + x));
        depthView.setTranslationY(depthPx * y * 0.45f);
        float scale = 1.01f + depthPx / 6000f;
        depthView.setScaleX(scale);
        depthView.setScaleY(scale);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (cameraThread == null) startCameraThread();
        tryOpenWhenReady();
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        closeCamera();
        super.onPause();
    }

    private void closeCamera() {
        openingCamera = false;
        if (session != null) {
            try { session.close(); } catch (Exception ignored) {}
            session = null;
        }
        if (camera != null) {
            try { camera.close(); } catch (Exception ignored) {}
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        closeCamera();
        if (cameraThread != null) {
            cameraThread.quitSafely();
            cameraThread = null;
            cameraHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == REQ_CAMERA && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            tryOpenWhenReady();
        } else if (status != null) {
            status.setText("Kamera izni gerekli");
        }
    }
}
