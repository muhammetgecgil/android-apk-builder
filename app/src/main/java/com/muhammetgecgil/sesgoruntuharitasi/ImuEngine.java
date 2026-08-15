package com.muhammetgecgil.sesgoruntuharitasi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** Lightweight IMU fusion for camera-motion rejection and source-angle stabilization. */
public final class ImuEngine implements SensorEventListener {
    public static final class Snapshot {
        public final float yawDeg;
        public final float pitchDeg;
        public final float rollDeg;
        public final float angularSpeedRad;
        public final float linearAccelMs2;
        public final float motion01;
        public final long timestampNs;
        public final boolean hasRotation;

        Snapshot(float yawDeg, float pitchDeg, float rollDeg,
                 float angularSpeedRad, float linearAccelMs2,
                 float motion01, long timestampNs, boolean hasRotation) {
            this.yawDeg = yawDeg;
            this.pitchDeg = pitchDeg;
            this.rollDeg = rollDeg;
            this.angularSpeedRad = angularSpeedRad;
            this.linearAccelMs2 = linearAccelMs2;
            this.motion01 = motion01;
            this.timestampNs = timestampNs;
            this.hasRotation = hasRotation;
        }
    }

    private final SensorManager manager;
    private final Sensor rotation;
    private final Sensor gyro;
    private final Sensor linearAccel;
    private volatile Snapshot latest = new Snapshot(0f,0f,0f,0f,0f,0f,0L,false);
    private float yaw, pitch, roll, gyroMag, accelMag, motion;
    private boolean started = false;

    public ImuEngine(Context context) {
        manager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        rotation = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gyro = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        linearAccel = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void start() {
        if (started || manager == null) return;
        started = true;
        if (rotation != null) manager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
        if (gyro != null) manager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
        if (linearAccel != null) manager.registerListener(this, linearAccel, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        if (!started || manager == null) return;
        manager.unregisterListener(this);
        started = false;
    }

    public Snapshot getLatest() { return latest; }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null) return;
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] r = new float[9];
            float[] o = new float[3];
            try {
                SensorManager.getRotationMatrixFromVector(r, event.values);
                SensorManager.getOrientation(r, o);
                yaw = wrapDeg((float)Math.toDegrees(o[0]));
                pitch = wrapDeg((float)Math.toDegrees(o[1]));
                roll = wrapDeg((float)Math.toDegrees(o[2]));
            } catch (Exception ignored) {}
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && event.values.length >= 3) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            float raw = (float)Math.sqrt(x*x + y*y + z*z);
            gyroMag = 0.72f * gyroMag + 0.28f * raw;
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && event.values.length >= 3) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            float raw = (float)Math.sqrt(x*x + y*y + z*z);
            accelMag = 0.78f * accelMag + 0.22f * raw;
        }

        float instant = clamp01(0.70f * (gyroMag / 1.25f) + 0.30f * (accelMag / 3.0f));
        if (instant > motion) motion = 0.55f * motion + 0.45f * instant;
        else motion = 0.88f * motion + 0.12f * instant;
        latest = new Snapshot(yaw, pitch, roll, gyroMag, accelMag, motion,
                event.timestamp > 0 ? event.timestamp : System.nanoTime(), rotation != null);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private static float wrapDeg(float v) {
        while (v > 180f) v -= 360f;
        while (v < -180f) v += 360f;
        return v;
    }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
