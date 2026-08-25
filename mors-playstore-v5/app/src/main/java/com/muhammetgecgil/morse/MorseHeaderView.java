package com.muhammetgecgil.morse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public final class MorseHeaderView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    public MorseHeaderView(Context context) { super(context); init(); }
    public MorseHeaderView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setContentDescription("MUHAMMET Mors sinyal tasarımı");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float r = Math.min(w, h) * 0.075f;
        rect.set(0, 0, w, h);
        paint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.rgb(2, 8, 17), Color.rgb(4, 20, 31), Color.rgb(2, 12, 16)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, r, r, paint);
        paint.setShader(null);

        int cyan = Color.rgb(32, 223, 244);
        int green = Color.rgb(130, 244, 91);
        int dim = Color.rgb(21, 82, 105);

        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.argb(50, 32, 223, 244));
        for (int i = 1; i < 12; i++) {
            float x = w * i / 12f;
            canvas.drawLine(x, h * .05f, x, h * .95f, paint);
        }
        for (int i = 1; i < 7; i++) {
            float y = h * i / 7f;
            canvas.drawLine(w * .03f, y, w * .97f, y, paint);
        }

        float cx = w * .5f;
        float cy = h * .29f;
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeCap(Paint.Cap.ROUND);
        glow.setColor(cyan);
        glow.setStrokeWidth(dp(3));
        glow.setShadowLayer(dp(9), 0, 0, Color.argb(150, 32, 223, 244));
        for (int i = 0; i < 3; i++) {
            float rr = h * (.11f + i * .065f);
            rect.set(cx - rr, cy - rr, cx + rr, cy + rr);
            canvas.drawArc(rect, 205, 130, false, glow);
            canvas.drawArc(rect, 25, 130, false, glow);
        }

        glow.setShadowLayer(dp(7), 0, 0, Color.argb(135, 32, 223, 244));
        glow.setStrokeWidth(dp(2.5f));
        path.reset();
        path.moveTo(cx, cy + h * .025f);
        path.lineTo(cx - w * .025f, cy + h * .18f);
        path.lineTo(cx + w * .025f, cy + h * .18f);
        path.close();
        canvas.drawPath(path, glow);
        canvas.drawCircle(cx, cy, h * .022f, glow);
        for (int i = 1; i <= 3; i++) {
            float yy = cy + h * (.055f + i * .033f);
            canvas.drawLine(cx - w * (.005f + i * .005f), yy,
                    cx + w * (.005f + i * .005f), yy, glow);
        }

        paint.setStrokeWidth(dp(1.4f));
        for (int i = 0; i < 40; i++) {
            float t = i / 39f;
            float xL = w * (.045f + t * .34f);
            float xR = w * (.615f + t * .34f);
            float amp = (float) (Math.abs(Math.sin(i * 1.7)) * h * .075f + dp(2));
            paint.setColor(Color.argb(145, 32, 223, 244));
            canvas.drawLine(xL, cy - amp / 2, xL, cy + amp / 2, paint);
            paint.setColor(Color.argb(145, 130, 244, 91));
            canvas.drawLine(xR, cy - amp / 2, xR, cy + amp / 2, paint);
        }

        float plateTop = h * .53f;
        float plateBottom = h * .78f;
        rect.set(w * .055f, plateTop, w * .945f, plateBottom);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(dp(1.6f));
        glow.setColor(cyan);
        glow.setShadowLayer(dp(7), 0, 0, Color.argb(125, 32, 223, 244));
        canvas.drawRoundRect(rect, dp(12), dp(12), glow);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(w * .2f, 0, w * .8f, 0, cyan, green, Shader.TileMode.CLAMP));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setTextSize(Math.min(w * .105f, h * .19f));
        paint.setShadowLayer(dp(8), 0, 0, Color.argb(150, 32, 223, 244));
        canvas.drawText("MUHAMMET", cx, plateTop + (plateBottom - plateTop) * .69f, paint);
        paint.clearShadowLayer();
        paint.setShader(null);

        String morse = "-- ..- .... .- -- -- . -";
        paint.setColor(Color.rgb(103, 240, 220));
        paint.setTextSize(Math.min(w * .042f, h * .072f));
        paint.setTypeface(android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD));
        canvas.drawText(morse, cx, h * .91f, paint);

        glow.clearShadowLayer();
        glow.setColor(dim);
        glow.setStrokeWidth(dp(1));
        glow.setStyle(Paint.Style.STROKE);
        rect.set(dp(1), dp(1), w - dp(1), h - dp(1));
        canvas.drawRoundRect(rect, r, r, glow);
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
