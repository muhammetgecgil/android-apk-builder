package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public final class SensorFusionManager implements SensorEventListener {
    private final SensorManager manager;
    private final Sensor accelerometer;
    private final Sensor gyroscope;
    private volatile float gyroMagnitude;
    private volatile float accelDelta;
    public SensorFusionManager(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }
    public void start() { if (accelerometer != null) manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME); if (gyroscope != null) manager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME); }
    public void stop() { manager.unregisterListener(this); }
    public float stability() {
        float gyroPenalty = clamp01(gyroMagnitude / 2.5f);
        float accelPenalty = clamp01(accelDelta / 4.0f);
        return clamp01(1f - (0.72f * gyroPenalty + 0.28f * accelPenalty));
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x=event.values[0], y=event.values[1], z=event.values[2]; gyroMagnitude=(float)Math.sqrt(x*x+y*y+z*z);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x=event.values[0], y=event.values[1], z=event.values[2]; float mag=(float)Math.sqrt(x*x+y*y+z*z); accelDelta=Math.abs(mag-SensorManager.GRAVITY_EARTH);
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    private static float clamp01(float x) { return Math.max(0f, Math.min(1f, x)); }
}
