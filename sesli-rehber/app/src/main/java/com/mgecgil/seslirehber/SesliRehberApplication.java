package com.mgecgil.seslirehber;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import com.mgecgil.seslirehber.ui.HudLayoutInstaller;

/** Process application used to expose app context and install the camera-first HUD shell. */
public final class SesliRehberApplication extends Application {
    private static volatile Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle state) {
                if (activity != null && activity.getClass().getName().equals("com.mgecgil.seslirehber.MainActivity")) {
                    activity.getWindow().getDecorView().post(() -> HudLayoutInstaller.install(activity));
                }
            }
            @Override public void onActivityDestroyed(Activity activity) { HudLayoutInstaller.detach(activity); }
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        });
    }

    public static Context appContext() {
        Context value = appContext;
        if (value == null) throw new IllegalStateException("Application context is not ready");
        return value;
    }
}
