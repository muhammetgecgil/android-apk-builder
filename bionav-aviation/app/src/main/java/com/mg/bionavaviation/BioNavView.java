package com.mg.bionavaviation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import java.util.Locale;

public class BioNavView extends View {
    private final FusionEngine f;
    private final FlightRecorder recorder;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BioNavView(Context context, FusionEngine fusion, FlightRecorder recorder) {
        super(context);
        this.f = fusion;
        this.recorder = recorder;
        p.setTypeface(android.graphics.Typeface.MONOSPACE);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        c.drawColor(0xFF071018);

        float mapH = h * 0.43f;
        drawGrid(c, w, mapH);

        float cx = w / 2f;
        float cy = mapH / 2f;
        float usable = Math.min(w, mapH) * 0.36f;
        double radiusM = Math.max(50.0, f.distanceFromOriginM() * 1.25 + f.horizontalSigmaM * 2.0);
        float scale = (float) (usable / radiusM);
        float px = cx + (float) f.eastM * scale;
        float py = cy - (float) f.northM * scale;

        // Horizontal uncertainty ellipse (isotropic in v2 phone demo).
        float sigmaPx = (float) Math.min(usable, f.horizontalSigmaM * scale);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(0x6657E389);
        c.drawOval(new RectF(px - sigmaPx, py - sigmaPx, px + sigmaPx, py + sigmaPx), p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF63D8FF);
        c.drawCircle(cx, cy, 6f, p);
        p.setColor(0xFFFFC857);
        c.drawCircle(px, py, 8f, p);

        double rad = Math.toRadians(f.headingDeg - 90.0);
        Path aircraft = new Path();
        float len = 30f;
        float tipX = px + (float) Math.cos(rad) * len;
        float tipY = py + (float) Math.sin(rad) * len;
        aircraft.moveTo(tipX, tipY);
        aircraft.lineTo(px - (float) Math.sin(rad) * 11f, py + (float) Math.cos(rad) * 11f);
        aircraft.lineTo(px + (float) Math.sin(rad) * 11f, py - (float) Math.cos(rad) * 11f);
        aircraft.close();
        p.setColor(0xFF57E389);
        c.drawPath(aircraft, p);

        p.setTextSize(14f);
        p.setColor(0xFF8196A8);
        c.drawText(fmt("MAP ±%.0f m", radiusM), 16, mapH - 12, p);
        c.drawText("N", cx - 5, 18, p);

        float y0 = mapH + 30f;
        p.setTextSize(20f);
        line(c, 18, y0, "HDG / TRK", fmt("%05.1f° / %05.1f°", f.headingDeg, f.trackDeg()));
        line(c, 18, y0 + 34, "ATT", fmt("P%+05.1f° R%+05.1f°", f.pitchDeg, f.rollDeg));
        line(c, 18, y0 + 68, "N / E", fmt("%+.1f / %+.1f m", f.northM, f.eastM));
        line(c, 18, y0 + 102, "GS", fmt("%.2f m/s", f.groundSpeedMps()));
        line(c, 18, y0 + 136, "HOME", fmt("%.1f m @ %03.0f°", f.distanceFromOriginM(), f.bearingToOriginDeg()));
        String alt = Double.isNaN(f.relativeBaroAltitudeM) ? "N/A" : fmt("%+.1f m  VS %+.2f m/s", f.relativeBaroAltitudeM, f.verticalSpeedMps);
        line(c, 18, y0 + 170, "BARO REL", alt);

        p.setTextSize(17f);
        p.setColor(integrityColor());
        c.drawText("INTEGRITY: " + f.integrity, 18, y0 + 213, p);
        p.setColor(0xFFB8C7D4);
        c.drawText(fmt("H-UNC ±%.1f m   V-UNC ±%.1f m   HDG ±%.1f°", f.horizontalSigmaM, f.verticalSigmaM, f.headingSigmaDeg), 18, y0 + 241, p);

        p.setTextSize(14f);
        p.setColor(0xFFB8C7D4);
        c.drawText(fmt("ACC %02.0f  GYR %02.0f  ATT %02.0f  MAG %02.0f  BARO %02.0f",
                f.accelConfidence * 100f, f.gyroConfidence * 100f, f.attitudeConfidence * 100f,
                f.magConfidence * 100f, f.baroConfidence * 100f), 18, y0 + 270, p);
        String mag = Double.isNaN(f.magneticFieldUt) ? "N/A" : fmt("%.1f uT", f.magneticFieldUt);
        c.drawText("MAG FIELD " + mag + "   ZUPT " + (f.stationary ? "ACTIVE" : "OFF"), 18, y0 + 294, p);

        p.setTextSize(14f);
        p.setColor(recorder.isRecording() ? 0xFFFF6B6B : 0xFF8196A8);
        c.drawText((recorder.isRecording() ? "● REC  " : "○ REC  ") + recorder.size() + " samples", 18, y0 + 320, p);

        p.setTextSize(12f);
        p.setColor(0xFF6F8496);
        c.drawText("GNSS permission: NONE | Relative inertial research solution", 18, h - 8, p);
    }

    private void drawGrid(Canvas c, float w, float mapH) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        p.setColor(0xFF1E364A);
        for (int i = 1; i < 7; i++) {
            float x = w * i / 7f;
            c.drawLine(x, 0, x, mapH, p);
        }
        for (int i = 1; i < 6; i++) {
            float y = mapH * i / 6f;
            c.drawLine(0, y, w, y, p);
        }
    }

    private int integrityColor() {
        if ("NAV GOOD".equals(f.integrity)) return 0xFF57E389;
        if ("NAV DEGRADED".equals(f.integrity) || "ALIGNING".equals(f.integrity)) return 0xFFFFC857;
        return 0xFFFF6B6B;
    }

    private void line(Canvas c, float x, float y, String label, String value) {
        p.setColor(0xFF8196A8);
        c.drawText(label, x, y, p);
        p.setColor(0xFFF4F7FA);
        c.drawText(value, x + 150, y, p);
    }

    private String fmt(String pattern, Object... args) {
        return String.format(Locale.US, pattern, args);
    }
}
