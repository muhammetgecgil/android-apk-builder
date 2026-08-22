package com.mg.fixturecockpitsim;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * V17 compatibility shell.
 * The old procedural fighter is removed from the live display path.
 * Existing cockpit/demo/Bluetooth code still instantiates Jet3DViewV7,
 * so this class now hosts the embedded textured F-22 renderer.
 */
public final class Jet3DViewV7 extends FrameLayout {
    private final F22InternetModelView f22;

    public Jet3DViewV7(Context context) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);
        f22 = new F22InternetModelView(context);
        addView(f22, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setTelemetry(float roll, float pitch, float yaw, float thr,
                             float hz, int drops, boolean live) {
        f22.setTelemetry(roll, pitch, yaw, thr, hz, drops, live);
    }

    @Override protected void onDetachedFromWindow() {
        try { f22.onPause(); } catch (Exception ignored) {}
        super.onDetachedFromWindow();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try { f22.onResume(); } catch (Exception ignored) {}
    }
}
