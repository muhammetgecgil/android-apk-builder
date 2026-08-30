package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

public final class SensorFusionManager implements SensorEventListener {
    private static final long SENSOR_STALE_MS = 1800L;
    private static final long STARTUP_GRACE_MS = 2200L;

    private final SensorManager manager;
    private final Sensor accelerometer;
    private final Sensor gyroscope;
    private volatile float gyroMagnitude;
    private volatile float accelDelta;
    private volatile long lastAccelElapsedMs;
    private volatile long lastGyroElapsedMs;
    private volatile long startedElapsedMs;

    public SensorFusionManager(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void start() {
        startedElapsedMs = SystemClock.elapsedRealtime();
        lastAccelElapsedMs = 0L;
        lastGyroElapsedMs = 0L;
        if (accelerometer != null) {
            manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscope != null) {
            manager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stop() {
        manager.unregisterListener(this);
    }

    public float stability() {
        long now = SystemClock.elapsedRealtime();
        long sinceStart = Math.max(0L, now - startedElapsedMs);

        if (accelerometer == null || gyroscope == null) {
            return 0.24f;
        }

        boolean accelFresh = lastAccelElapsedMs > 0L && now - lastAccelElapsedMs <= SENSOR_STALE_MS;
        boolean gyroFresh = lastGyroElapsedMs > 0L && now - lastGyroElapsedMs <= SENSOR_STALE_MS;
        if (!accelFresh || !gyroFresh) {
            // Avoid a false hard stop during the very first sensor callbacks, but never report full
            // confidence while health is unknown.
            return sinceStart <= STARTUP_GRACE_MS ? 0.44f : 0.24f;
        }

        float gyroPenalty = clamp01(gyroMagnitude / 2.5f);
        float accelPenalty = clamp01(accelDelta / 4.0f);
        return clamp01(1f - (0.72f * gyroPenalty + 0.28f * accelPenalty));
    }

    public boolean sensorsHealthy() {
        long now = SystemClock.elapsedRealtime();
        return accelerometer != null
                && gyroscope != null
                && lastAccelElapsedMs > 0L
                && lastGyroElapsedMs > 0L
                && now - lastAccelElapsedMs <= SENSOR_STALE_MS
                && now - lastGyroElapsedMs <= SENSOR_STALE_MS;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = SystemClock.elapsedRealtime();
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float raw = (float) Math.sqrt(x * x + y * y + z * z);
            gyroMagnitude = ema(gyroMagnitude, raw, 0.28f);
            lastGyroElapsedMs = now;
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
            float rawDelta = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
            accelDelta = ema(accelDelta, rawDelta, 0.24f);
            lastAccelElapsedMs = now;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private static float ema(float previous, float current, float alpha) {
        return previous * (1f - alpha) + current * alpha;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
