package com.mg.bionavaviation;

import android.hardware.SensorManager;

public final class FusionEngine {
    public double northM, eastM, upM;
    public double vn, ve, vu;
    public double headingDeg;
    public double pressureHpa = Double.NaN;
    public double baroAltitudeM = Double.NaN;
    public float accelConfidence, gyroConfidence, magConfidence, baroConfidence;
    public String integrity = "INIT";

    private final float[] rotation = new float[9];
    private final float[] orientation = new float[3];
    private boolean hasAttitude;
    private long lastAccelNs;

    public void updateRotationVector(float[] values) {
        SensorManager.getRotationMatrixFromVector(rotation, values);
        SensorManager.getOrientation(rotation, orientation);
        double hdg = Math.toDegrees(orientation[0]);
        if (hdg < 0) hdg += 360.0;
        headingDeg = hdg;
        hasAttitude = true;
        gyroConfidence = 0.85f;
        magConfidence = 0.80f;
        updateIntegrity();
    }

    public void updateLinearAcceleration(float ax, float ay, float az, long timestampNs) {
        if (!hasAttitude) return;
        if (lastAccelNs == 0L) {
            lastAccelNs = timestampNs;
            return;
        }
        double dt = (timestampNs - lastAccelNs) * 1e-9;
        lastAccelNs = timestampNs;
        if (dt <= 0 || dt > 0.10) return;

        // Android world frame from rotation vector is approximately East/North/Up.
        double e = rotation[0] * ax + rotation[1] * ay + rotation[2] * az;
        double n = rotation[3] * ax + rotation[4] * ay + rotation[5] * az;
        double u = rotation[6] * ax + rotation[7] * ay + rotation[8] * az;

        // Conservative damping prevents an unbounded demo display, but does not make
        // phone-grade inertial navigation suitable for real flight navigation.
        double damping = 0.9992;
        ve = ve * damping + e * dt;
        vn = vn * damping + n * dt;
        vu = vu * damping + u * dt;

        eastM += ve * dt;
        northM += vn * dt;
        upM += vu * dt;

        double a = Math.sqrt(ax * ax + ay * ay + az * az);
        accelConfidence = (float) Math.max(0.25, Math.min(0.90, 0.90 - a / 35.0));
        updateIntegrity();
    }

    public void updatePressure(float hPa) {
        pressureHpa = hPa;
        // ISA pressure altitude using standard sea-level pressure. User calibration is
        // intentionally left for a later version.
        baroAltitudeM = 44330.0 * (1.0 - Math.pow(hPa / 1013.25, 0.190294957));
        baroConfidence = 0.82f;
        updateIntegrity();
    }

    public double groundSpeedMps() {
        return Math.sqrt(vn * vn + ve * ve);
    }

    public double distanceFromOriginM() {
        return Math.sqrt(northM * northM + eastM * eastM);
    }

    public double bearingToOriginDeg() {
        if (Math.abs(northM) < 0.001 && Math.abs(eastM) < 0.001) return 0.0;
        double b = Math.toDegrees(Math.atan2(-eastM, -northM));
        return b < 0 ? b + 360.0 : b;
    }

    public void reset() {
        northM = eastM = upM = 0;
        vn = ve = vu = 0;
        lastAccelNs = 0L;
    }

    private void updateIntegrity() {
        int sources = 0;
        if (accelConfidence > 0.1f) sources++;
        if (gyroConfidence > 0.1f) sources++;
        if (magConfidence > 0.1f) sources++;
        if (baroConfidence > 0.1f) sources++;
        integrity = sources >= 4 ? "DEMO GOOD" : sources >= 2 ? "DEMO DEGRADED" : "INSUFFICIENT";
    }
}
