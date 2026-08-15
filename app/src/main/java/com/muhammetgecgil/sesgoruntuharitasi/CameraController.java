package com.muhammetgecgil.sesgoruntuharitasi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.SizeF;
import android.view.Surface;
import android.view.TextureView;

import java.util.Collections;

public final class CameraController {
    private final Activity activity;
    private final TextureView textureView;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private boolean videoStabilizationSupported = false;
    private volatile float horizontalFovDeg = 70f;
    private volatile float verticalFovDeg = 52f;

    public CameraController(Activity activity, TextureView textureView) {
        this.activity = activity;
        this.textureView = textureView;
    }

    public void start() {
        if (cameraThread == null) {
            cameraThread = new HandlerThread("FusionCameraV7");
            cameraThread.start();
            cameraHandler = new Handler(cameraThread.getLooper());
        }
        if (textureView.isAvailable()) openCamera();
        else textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { openCamera(); }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
    }

    public void stop() {
        if (session != null) {
            try { session.close(); } catch (Exception ignored) {}
            session = null;
        }
        if (camera != null) {
            try { camera.close(); } catch (Exception ignored) {}
            camera = null;
        }
        if (cameraThread != null) {
            cameraThread.quitSafely();
            try { cameraThread.join(450); } catch (InterruptedException ignored) {}
            cameraThread = null;
            cameraHandler = null;
        }
    }

    public boolean isVideoStabilizationSupported() { return videoStabilizationSupported; }
    public float getHorizontalFovDeg() { return horizontalFovDeg; }
    public float getVerticalFovDeg() { return verticalFovDeg; }

    private void openCamera() {
        if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        try {
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            String chosen = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics c = manager.getCameraCharacteristics(id);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    chosen = id;
                    videoStabilizationSupported = supportsStabilization(c);
                    updateFov(c);
                    break;
                }
            }
            if (chosen == null && manager.getCameraIdList().length > 0) {
                chosen = manager.getCameraIdList()[0];
                CameraCharacteristics fallback = manager.getCameraCharacteristics(chosen);
                videoStabilizationSupported = supportsStabilization(fallback);
                updateFov(fallback);
            }
            if (chosen == null) return;
            manager.openCamera(chosen, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice device) { camera = device; createPreview(); }
                @Override public void onDisconnected(CameraDevice device) { device.close(); camera = null; }
                @Override public void onError(CameraDevice device, int error) { device.close(); camera = null; }
            }, cameraHandler);
        } catch (Exception ignored) {}
    }

    private void updateFov(CameraCharacteristics c) {
        try {
            SizeF size = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            float[] focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            if (size != null && focal != null && focal.length > 0 && focal[0] > 0f) {
                horizontalFovDeg = (float)Math.toDegrees(2.0 * Math.atan(size.getWidth() / (2.0 * focal[0])));
                verticalFovDeg = (float)Math.toDegrees(2.0 * Math.atan(size.getHeight() / (2.0 * focal[0])));
            }
        } catch (Throwable ignored) {}
    }

    private boolean supportsStabilization(CameraCharacteristics c) {
        try {
            int[] modes = c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
            if (modes != null) for (int m : modes) if (m == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private void createPreview() {
        if (camera == null || !textureView.isAvailable()) return;
        try {
            SurfaceTexture st = textureView.getSurfaceTexture();
            if (st == null) return;
            st.setDefaultBufferSize(1280, 720);
            Surface surface = new Surface(st);
            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            if (videoStabilizationSupported) {
                builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
            }
            camera.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession s) {
                    if (camera == null) return;
                    session = s;
                    try { session.setRepeatingRequest(builder.build(), null, cameraHandler); }
                    catch (Exception ignored) {}
                }
                @Override public void onConfigureFailed(CameraCaptureSession s) {}
            }, cameraHandler);
        } catch (Exception ignored) {}
    }
}
