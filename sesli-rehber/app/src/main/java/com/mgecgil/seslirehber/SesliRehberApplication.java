package com.mgecgil.seslirehber;

import android.app.Application;
import android.content.Context;

/** Process application used only to expose a safe application context to long-lived ML cores. */
public final class SesliRehberApplication extends Application {
    private static volatile Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context appContext() {
        Context value = appContext;
        if (value == null) throw new IllegalStateException("Application context is not ready");
        return value;
    }
}
