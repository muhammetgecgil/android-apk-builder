package com.mg.bionavaviation;

import android.hardware.SensorManager;

public final class FusionEngine {
    public double northM, eastM, upM;
    public double vn, ve, vu;
    public double headingDeg, pitchDeg, rollDeg;
    public double pressureHpa = Double.NaN;
    public double baroAltitudeM = Double.NaN;
    public double relativeBaroAltitudeM = Double.NaN;
    public double verticalSpeedMps;
    public double magneticFieldUt = Double.NaN;
    public double horizontalSigmaM = 3.0;
    public double verticalSigmaM = 2.0;
    public double headingSigmaDeg = 8.0;
    public double accelBiasX, accelBiasY, accelBiasZ;
    public float accelConfidence, gyroConfidence, magConfidence, baroConfidence, attitudeConfidence;
    public String integrity = "ALIGNING";
    public String navMode = "INS / BIO-FUSION";
    public boolean stationary;

    private final float[] rotation = new float[9];
    private final float[] orientation = new float[3];
    private boolean hasAttitude;
    private long lastAccelNs, lastGyroNs, lastPressureNs;
    private double gyroNorm;
    private double recentLinearAccelNorm;
    private double pressureReferenceAlt = Double.NaN;
    private double lastBaroAlt = Double.NaN;
    private int stationarySamples;

    public void updateRotationVector(float[] values) {
        SensorManager.getRotationMatrixFromVector(rotation, values);
        SensorManager.getOrientation(rotation, orientation);
        headingDeg = wrap360(Math.toDegrees(orientation[0]));
        pitchDeg = Math.toDegrees(orientation[1]);
        rollDeg = Math.toDegrees(orientation[2]);
        hasAttitude = true;
        attitudeConfidence = 0.92f;
        headingSigmaDeg = Math.max(1.5, 9.0 - 7.0 * Math.max(magConfidence, 0.35f));
        updateIntegrity();
    }

    public void updateGyroscope(float gx, float gy, float gz, long timestampNs) {
        gyroNorm = Math.sqrt(gx * gx + gy * gy + gz * gz);
        if (lastGyroNs != 0L) {
            double dt = (timestampNs - lastGyroNs) * 1e-9;
            if (dt > 0 && dt < 0.2) {
                gyroConfidence = (float) clamp(0.97 - gyroNorm / 18.0, 0.45, 0.97);
            }
        }
        lastGyroNs = timestampNs;
        updateStationary();
    }

    public void updateMagneticField(float mx, float my, float mz) {
        magneticFieldUt = Math.sqrt(mx * mx + my * my + mz * mz);
        // Typical geomagnetic field at Earth's surface is roughly 25–65 uT.
        double deviation = magneticFieldUt < 25.0 ? 25.0 - magneticFieldUt :
                magneticFieldUt > 65.0 ? magneticFieldUt - 65.0 : 0.0;
        magConfidence = (float) clamp(0.95 - deviation / 35.0, 0.15, 0.95);
        headingSigmaDeg = Math.max(1.5, 9.0 - 7.0 * magConfidence);
        updateIntegrity();
    }

    public void updateLinearAcceleration(float ax, float ay, float az, long timestampNs) {
        recentLinearAccelNorm = Math.sqrt(ax * ax + ay * ay + az * az);
        updateStationary();
        if (!hasAttitude) return;
        if (lastAccelNs == 0L) {
            lastAccelNs = timestampNs;
            return;
        }
        double dt = (timestampNs - lastAccelNs) * 1e-9;
        lastAccelNs = timestampNs;
        if (dt <= 0 || dt > 0.10) return;

        // Slow bias learning only while the handset is confidently stationary.
        if (stationary) {
            double k = 0.004;
            accelBiasX = accelBiasX * (1.0 - k) + ax * k;
            accelBiasY = accelBiasY * (1.0 - k) + ay * k;
            accelBiasZ = accelBiasZ * (1.0 - k) + az * k;
        }
        double bx = ax - accelBiasX;
        double by = ay - accelBiasY;
        double bz = az - accelBiasZ;

        // Android rotation-vector matrix transforms device frame toward ENU world frame.
        double e = rotation[0] * bx + rotation[1] * by + rotation[2] * bz;
        double n = rotation[3] * bx + rotation[4] * by + rotation[5] * bz;
        double u = rotation[6] * bx + rotation[7] * by + rotation[8] * bz;

        // Strapdown dead-reckoning propagation. ZUPT is applied only when stationary.
        ve += e * dt;
        vn += n * dt;
        vu += u * dt;
        if (stationary) {
            ve *= 0.82;
            vn *= 0.82;
            vu *= 0.75;
            if (Math.abs(ve) < 0.025) ve = 0;
            if (Math.abs(vn) < 0.025) vn = 0;
            if (Math.abs(vu) < 0.025) vu = 0;
            horizontalSigmaM = Math.max(1.5, horizontalSigmaM * 0.997);
        } else {
            horizontalSigmaM += dt * (0.45 + 0.08 * groundSpeedMps());
            verticalSigmaM += dt * 0.20;
        }

        eastM += ve * dt;
        northM += vn * dt;
        upM += vu * dt;
        accelConfidence = (float) clamp(0.96 - recentLinearAccelNorm / 45.0, 0.35, 0.96);
        updateIntegrity();
    }

    public void updatePressure(float hPa, long timestampNs) {
        pressureHpa = hPa;
        baroAltitudeM = 44330.0 * (1.0 - Math.pow(hPa / 1013.25, 0.190294957));
        if (Double.isNaN(pressureReferenceAlt)) pressureReferenceAlt = baroAltitudeM;
        relativeBaroAltitudeM = baroAltitudeM - pressureReferenceAlt;
        if (!Double.isNaN(lastBaroAlt) && lastPressureNs != 0L) {
            double dt = (timestampNs - lastPressureNs) * 1e-9;
            if (dt > 0.05 && dt < 2.0) {
                double rawVs = (baroAltitudeM - lastBaroAlt) / dt;
                verticalSpeedMps = verticalSpeedMps * 0.88 + rawVs * 0.12;
            }
        }
        lastBaroAlt = baroAltitudeM;
        lastPressureNs = timestampNs;
        baroConfidence = 0.93f;
        verticalSigmaM = Math.max(1.0, verticalSigmaM * 0.985);
        updateIntegrity();
    }

    public void calibrateBaroOrigin() {
        if (!Double.isNaN(baroAltitudeM)) {
            pressureReferenceAlt = baroAltitudeM;
            relativeBaroAltitudeM = 0.0;
            verticalSpeedMps = 0.0;
        }
    }

    public void markSensorAccuracy(int sensorType, int accuracy) {
        float factor = accuracy >= 3 ? 1.0f : accuracy == 2 ? 0.78f : accuracy == 1 ? 0.5f : 0.25f;
        if (sensorType == android.hardware.Sensor.TYPE_MAGNETIC_FIELD) magConfidence *= factor;
        if (sensorType == android.hardware.Sensor.TYPE_GYROSCOPE) gyroConfidence *= factor;
        updateIntegrity();
    }

    public double groundSpeedMps() { return Math.sqrt(vn * vn + ve * ve); }
    public double trackDeg() { return groundSpeedMps() < 0.05 ? headingDeg : wrap360(Math.toDegrees(Math.atan2(ve, vn))); }
    public double distanceFromOriginM() { return Math.sqrt(northM * northM + eastM * eastM); }
    public double bearingToOriginDeg() {
        if (distanceFromOriginM() < 0.001) return 0.0;
        return wrap360(Math.toDegrees(Math.atan2(-eastM, -northM)));
    }

    public void reset() {
        northM = eastM = upM = 0;
        vn = ve = vu = 0;
        horizontalSigmaM = 3.0;
        verticalSigmaM = 2.0;
        lastAccelNs = 0L;
        calibrateBaroOrigin();
    }

    private void updateStationary() {
        boolean candidate = recentLinearAccelNorm < 0.18 && gyroNorm < 0.035;
        stationarySamples = candidate ? Math.min(200, stationarySamples + 1) : Math.max(0, stationarySamples - 4);
        stationary = stationarySamples > 25;
    }

    private void updateIntegrity() {
        double weighted = accelConfidence * 0.23 + gyroConfidence * 0.22 + attitudeConfidence * 0.23 +
                magConfidence * 0.17 + baroConfidence * 0.15;
        if (!hasAttitude) {
            integrity = "ALIGNING";
        } else if (weighted >= 0.78 && horizontalSigmaM < 80.0) {
            integrity = "NAV GOOD";
        } else if (weighted >= 0.52 && horizontalSigmaM < 250.0) {
            integrity = "NAV DEGRADED";
        } else {
            integrity = "NAV UNRELIABLE";
        }
    }

    private static double wrap360(double v) {
        v %= 360.0;
        return v < 0 ? v + 360.0 : v;
    }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
