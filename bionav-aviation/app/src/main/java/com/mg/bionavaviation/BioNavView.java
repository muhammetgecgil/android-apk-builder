package com.mg.bionavaviation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.Locale;

public class BioNavView extends View {
    private final FusionEngine f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BioNavView(Context context, FusionEngine fusion) {
        super(context);
        this.f = fusion;
        p.setTypeface(android.graphics.Typeface.MONOSPACE);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        c.drawColor(0xFF08111A);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(0xFF28465F);
        for (int i = 1; i < 6; i++) {
            float x = w * i / 6f;
            c.drawLine(x, 0, x, h * 0.48f, p);
        }
        for (int i = 1; i < 5; i++) {
            float y = h * 0.48f * i / 5f;
            c.drawLine(0, y, w, y, p);
        }

        float cx = w / 2f;
        float cy = h * 0.24f;
        float scale = 1.8f;
        float px = cx + (float) f.eastM * scale;
        float py = cy - (float) f.northM * scale;

        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF63D8FF);
        c.drawCircle(cx, cy, 6f, p);
        p.setColor(0xFFFFC857);
        c.drawCircle(px, py, 9f, p);

        double rad = Math.toRadians(f.headingDeg - 90.0);
        Path aircraft = new Path();
        float len = 34f;
        float tipX = px + (float) Math.cos(rad) * len;
        float tipY = py + (float) Math.sin(rad) * len;
        aircraft.moveTo(tipX, tipY);
        aircraft.lineTo(px - (float) Math.sin(rad) * 12f, py + (float) Math.cos(rad) * 12f);
        aircraft.lineTo(px + (float) Math.sin(rad) * 12f, py - (float) Math.cos(rad) * 12f);
        aircraft.close();
        p.setColor(0xFF57E389);
        c.drawPath(aircraft, p);

        p.setTextSize(23f);
        p.setColor(0xFFF4F7FA);
        float y0 = h * 0.55f;
        line(c, 22, y0, "HDG", fmt("%06.1f°", f.headingDeg));
        line(c, 22, y0 + 38, "N / E", fmt("%+.1f / %+.1f m", f.northM, f.eastM));
        line(c, 22, y0 + 76, "GS", fmt("%.2f m/s", f.groundSpeedMps()));
        line(c, 22, y0 + 114, "ORIGIN", fmt("%.1f m @ %03.0f°", f.distanceFromOriginM(), f.bearingToOriginDeg()));
        String alt = Double.isNaN(f.baroAltitudeM) ? "N/A" : fmt("%.1f m PA", f.baroAltitudeM);
        line(c, 22, y0 + 152, "BARO ALT", alt);

        p.setTextSize(18f);
        p.setColor("DEMO GOOD".equals(f.integrity) ? 0xFF57E389 : 0xFFFFC857);
        c.drawText("INTEGRITY: " + f.integrity, 22, y0 + 202, p);

        p.setTextSize(16f);
        p.setColor(0xFFB8C7D4);
        c.drawText(fmt("IMU %.0f%%   ATT %.0f%%   MAG %.0f%%   BARO %.0f%%",
                f.accelConfidence * 100f,
                f.gyroConfidence * 100f,
                f.magConfidence * 100f,
                f.baroConfidence * 100f), 22, y0 + 235, p);

        p.setTextSize(14f);
        p.setColor(0xFF8196A8);
        c.drawText("No GNSS/location permission. Relative inertial demo only.", 22, h - 22, p);
    }

    private void line(Canvas c, float x, float y, String label, String value) {
        p.setColor(0xFF8196A8);
        c.drawText(label, x, y, p);
        p.setColor(0xFFF4F7FA);
        c.drawText(value, x + 155, y, p);
    }

    private String fmt(String pattern, Object... args) {
        return String.format(Locale.US, pattern, args);
    }
}
