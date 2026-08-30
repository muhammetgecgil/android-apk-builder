package com.mg.hafizadostum.v4;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

public final class UiUtil {
    private UiUtil() {}

    public static void prepareWindow(Activity activity) {
        Window w = activity.getWindow();
        if (Build.VERSION.SDK_INT >= 21) {
            w.setStatusBarColor(Color.rgb(13, 53, 86));
            w.setNavigationBarColor(Color.rgb(13, 53, 86));
        }
    }

    public static void applyInsets(View root, int left, int top, int right, int bottom) {
        final float d = root.getResources().getDisplayMetrics().density;
        final int l0 = (int)(left * d + .5f);
        final int t0 = (int)(top * d + .5f);
        final int r0 = (int)(right * d + .5f);
        final int b0 = (int)(bottom * d + .5f);
        if (Build.VERSION.SDK_INT >= 21) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                int l = insets.getSystemWindowInsetLeft();
                int t = insets.getSystemWindowInsetTop();
                int r = insets.getSystemWindowInsetRight();
                int b = insets.getSystemWindowInsetBottom();
                v.setPadding(l0 + l, t0 + t, r0 + r, b0 + b);
                return insets;
            });
            root.requestApplyInsets();
        } else {
            root.setPadding(l0, t0, r0, b0);
        }
    }
}
