package com.mgecgil.seslirehber.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import com.mgecgil.seslirehber.core.HudPerceptionContext;
import com.mgecgil.seslirehber.core.ObjectSemanticContext;
import com.mgecgil.seslirehber.core.ObjectSemanticObservation;
import com.mgecgil.seslirehber.core.SituationalAwarenessContext;
import com.mgecgil.seslirehber.core.SituationalAwarenessEngine;
import com.mgecgil.seslirehber.core.UrbanHudMaskContext;
import java.util.List;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/**
 * Visual-only situational-awareness overlay. This view never feeds SafetyGate and never converts
 * its drawing into a navigation/safety decision. It exists for the sighted tester/caregiver while
 * spoken and haptic guidance remain the blind user's primary interface.
 */
public final class AwarenessHudView extends View {
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint objectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint approachingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint openPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private HudPerceptionContext.Snapshot perception;
    private SituationalAwarenessEngine.Snapshot world;
    private Bitmap maskBitmap;
    private long maskTimestampMs;
    private float sourceAspect = 9f / 16f;

    public AwarenessHudView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1f));
        gridPaint.setColor(Color.argb(92, 225, 240, 255));

        objectPaint.setStyle(Paint.Style.STROKE);
        objectPaint.setStrokeWidth(dp(2f));
        objectPaint.setColor(Color.argb(220, 90, 220, 255));

        approachingPaint.setStyle(Paint.Style.STROKE);
        approachingPaint.setStrokeWidth(dp(3f));
        approachingPaint.setColor(Color.argb(235, 255, 190, 50));

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(dp(12f));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(dp(3f), 0f, dp(1f), Color.BLACK);

        bandPaint.setStyle(Paint.Style.FILL);
        groundPaint.setStyle(Paint.Style.STROKE);
        groundPaint.setStrokeWidth(dp(3f));
        groundPaint.setColor(Color.argb(230, 255, 210, 40));

        openPaint.setStyle(Paint.Style.STROKE);
        openPaint.setStrokeWidth(dp(3f));
        openPaint.setColor(Color.argb(220, 80, 255, 170));
        maskPaint.setAlpha(105);
    }

    public void refresh(long nowMs) {
        perception = HudPerceptionContext.snapshot(nowMs);
        world = SituationalAwarenessContext.snapshot(nowMs);
        UrbanHudMaskContext.Frame mask = UrbanHudMaskContext.latest(nowMs);
        if (mask != null) {
            sourceAspect = mask.sourceAspect();
            if (mask.timestampMs() != maskTimestampMs) rebuildMask(mask);
        } else if (perception != null && perception.sourceAspect() > 0f) {
            sourceAspect = perception.sourceAspect();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        RectF camera = centerCropRect(getWidth(), getHeight(), sourceAspect);

        if (maskBitmap != null) canvas.drawBitmap(maskBitmap, null, camera, maskPaint);
        drawOccupancy(canvas, camera);
        drawGrid(canvas, camera);
        drawGround(canvas, camera);
        drawObjects(canvas, camera);
        drawWorldLabels(canvas, camera);
        drawOpenDirection(canvas, camera);
        drawTelemetry(canvas, camera);
    }

    private void drawGrid(Canvas canvas, RectF r) {
        float x1 = r.left + r.width() / 3f;
        float x2 = r.left + r.width() * 2f / 3f;
        float y1 = r.top + r.height() / 3f;
        float y2 = r.top + r.height() * 2f / 3f;
        canvas.drawLine(x1, r.top, x1, r.bottom, gridPaint);
        canvas.drawLine(x2, r.top, x2, r.bottom, gridPaint);
        canvas.drawLine(r.left, y1, r.right, y1, gridPaint);
        canvas.drawLine(r.left, y2, r.right, y2, gridPaint);
    }

    private void drawOccupancy(Canvas canvas, RectF r) {
        if (world == null) return;
        drawSectorBands(canvas, r, 0, world.left());
        drawSectorBands(canvas, r, 1, world.center());
        drawSectorBands(canvas, r, 2, world.right());
    }

    private void drawSectorBands(
            Canvas canvas,
            RectF r,
            int sector,
            SituationalAwarenessEngine.SectorSnapshot s) {
        float x0 = r.left + r.width() * sector / 3f;
        float x1 = r.left + r.width() * (sector + 1) / 3f;
        drawBand(canvas, x0, x1, r.top, r.top + r.height() / 3f, s.farOccupancy(), 48);
        drawBand(canvas, x0, x1, r.top + r.height() / 3f, r.top + r.height() * 2f / 3f, s.midOccupancy(), 66);
        drawBand(canvas, x0, x1, r.top + r.height() * 2f / 3f, r.bottom, s.nearOccupancy(), 86);
    }

    private void drawBand(Canvas canvas, float l, float rr, float t, float b, float score, int maxAlpha) {
        if (score < 0.20f) return;
        int alpha = Math.min(maxAlpha, Math.round(maxAlpha * score));
        bandPaint.setColor(Color.argb(alpha, 255, 105, 70));
        canvas.drawRect(l, t, rr, b, bandPaint);
    }

    private void drawGround(Canvas canvas, RectF r) {
        if (perception == null || perception.ground() == null) return;
        GroundObservation g = perception.ground();
        if (g.viewConfidence() < 0.25f || g.boundaryY() <= 0f || g.boundaryY() >= 1f) return;
        float y = r.top + g.boundaryY() * r.height();
        groundPaint.setAlpha(g.persistentAnomaly() ? 245 : 150);
        canvas.drawLine(r.left + r.width() * 0.18f, y, r.right - r.width() * 0.18f, y, groundPaint);
    }

    private void drawObjects(Canvas canvas, RectF r) {
        if (perception == null) return;
        List<ObjectObservation> objects = perception.objects();
        long nowMs = perception.timestampMs();
        for (ObjectObservation o : objects) {
            float hNorm = clamp(2f * Math.max(0.025f, o.bottomY() - o.centerY()), 0.06f, 0.88f);
            float wNorm = clamp(o.areaRatio() / Math.max(0.035f, hNorm), 0.06f, 0.82f);
            float cx = mapX(r, o.centerX());
            float cy = mapY(r, o.centerY());
            float halfW = r.width() * wNorm * 0.5f;
            float halfH = r.height() * hNorm * 0.5f;
            RectF box = new RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
            Paint p = o.isApproaching() ? approachingPaint : objectPaint;
            canvas.drawRoundRect(box, dp(7f), dp(7f), p);

            ObjectSemanticObservation semantic = o.trackingId() >= 0
                    ? ObjectSemanticContext.forTrackingId(o.trackingId(), nowMs)
                    : null;
            String id;
            if (semantic != null && semantic.usable()) {
                int pct = Math.max(0, Math.min(100, Math.round(semantic.confidence() * 100f)));
                id = semantic.label().toUpperCase() + (semantic.definite() ? " " : "? ") + pct + "%";
            } else {
                id = o.trackingId() >= 0 ? "NESNE #" + o.trackingId() : "NESNE";
            }
            if (o.isApproaching()) id += "  YAKLAŞIYOR";
            else if (Math.abs(o.centerVelocityX()) > 0.04f) id += "  HAREKET";
            canvas.drawText(id, box.left + dp(5f), Math.max(r.top + dp(15f), box.top - dp(5f)), textPaint);
        }
    }

    private void drawWorldLabels(Canvas canvas, RectF r) {
        if (world == null) return;
        drawSectorLabel(canvas, r, 0, world.left());
        drawSectorLabel(canvas, r, 1, world.center());
        drawSectorLabel(canvas, r, 2, world.right());
    }

    private void drawSectorLabel(
            Canvas canvas,
            RectF r,
            int sector,
            SituationalAwarenessEngine.SectorSnapshot s) {
        String label = s.farSemanticLabel();
        if (label == null || label.isBlank() || s.farSemanticConfidence() < 0.35f) return;
        float x = r.left + r.width() * (sector + 0.5f) / 3f;
        float y = r.top + r.height() * 0.18f;
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label.toUpperCase(), x, y, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawOpenDirection(Canvas canvas, RectF r) {
        if (world == null || world.moreOpenDirection() == Direction.UNKNOWN) return;
        int sector = world.moreOpenDirection() == Direction.LEFT ? 0
                : world.moreOpenDirection() == Direction.RIGHT ? 2 : 1;
        float l = r.left + r.width() * sector / 3f + dp(8f);
        float rr = r.left + r.width() * (sector + 1) / 3f - dp(8f);
        float t = r.top + r.height() * 0.68f;
        float b = r.bottom - dp(84f);
        canvas.drawRoundRect(new RectF(l, t, rr, b), dp(14f), dp(14f), openPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GÖRELİ AÇIK ADAY", (l + rr) * 0.5f, Math.max(t + dp(18f), b - dp(8f)), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawTelemetry(Canvas canvas, RectF r) {
        if (world == null) return;
        String text = "WORLD " + Math.round(world.awarenessConfidence() * 100f)
                + "%  •  COMPLEX " + Math.round(world.environmentComplexity() * 100f) + "%";
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(text, r.right - dp(10f), r.top + dp(22f), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void rebuildMask(UrbanHudMaskContext.Frame frame) {
        byte[] labels = frame.labels();
        int pixels = frame.width() * frame.height();
        if (labels.length < pixels) return;
        int[] argb = new int[pixels];
        for (int i = 0; i < pixels; i++) argb[i] = colorFor(labels[i] & 0xff);
        Bitmap next = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
        next.setPixels(argb, 0, frame.width(), 0, 0, frame.width(), frame.height());
        if (maskBitmap != null) try { maskBitmap.recycle(); } catch (Throwable ignored) {}
        maskBitmap = next;
        maskTimestampMs = frame.timestampMs();
    }

    private static int colorFor(int label) {
        return switch (label) {
            case 0 -> Color.argb(90, 65, 110, 245);       // road
            case 1 -> Color.argb(105, 50, 220, 235);     // sidewalk
            case 2, 3 -> Color.argb(72, 190, 200, 215);  // building/wall
            case 4, 5 -> Color.argb(110, 245, 145, 55);  // fence/pole
            case 6, 7 -> Color.argb(145, 255, 225, 45);  // traffic control
            case 8, 9 -> Color.argb(72, 80, 210, 110);   // vegetation/terrain
            case 10 -> Color.TRANSPARENT;                // sky
            case 11, 12 -> Color.argb(145, 255, 195, 55);// person/rider
            case 13, 14, 15, 16 -> Color.argb(135, 255, 80, 95); // vehicles
            case 17, 18 -> Color.argb(135, 230, 85, 255);// two-wheelers
            default -> Color.TRANSPARENT;
        };
    }

    private static RectF centerCropRect(int width, int height, float sourceAspect) {
        float viewAspect = width / (float) Math.max(1, height);
        if (sourceAspect > viewAspect) {
            float drawW = height * sourceAspect;
            float left = (width - drawW) * 0.5f;
            return new RectF(left, 0f, left + drawW, height);
        }
        float drawH = width / Math.max(0.01f, sourceAspect);
        float top = (height - drawH) * 0.5f;
        return new RectF(0f, top, width, top + drawH);
    }

    private static float mapX(RectF r, float x) { return r.left + clamp(x, 0f, 1f) * r.width(); }
    private static float mapY(RectF r, float y) { return r.top + clamp(y, 0f, 1f) * r.height(); }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    @Override protected void onDetachedFromWindow() {
        if (maskBitmap != null) {
            try { maskBitmap.recycle(); } catch (Throwable ignored) {}
            maskBitmap = null;
        }
        super.onDetachedFromWindow();
    }
}
