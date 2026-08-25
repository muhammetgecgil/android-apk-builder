package com.mgai.app;

import android.app.Application;

public class MgAiApplication extends Application {
    @Override public void onCreate(){
        super.onCreate();
        UpdateMigrationManager.run(this);
    }
}
