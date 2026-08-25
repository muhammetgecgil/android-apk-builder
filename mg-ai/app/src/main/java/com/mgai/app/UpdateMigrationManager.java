package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;

import java.io.File;

public final class UpdateMigrationManager {
    private static final String PREF="mg_ai_update_state";
    private static final String KEY_LAST_VERSION="last_version_code";

    private UpdateMigrationManager(){}

    public static void run(Context context){
        if(context==null)return;
        Context c=context.getApplicationContext();
        int current=currentVersionCode(c);
        SharedPreferences sp=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        int previous=sp.getInt(KEY_LAST_VERSION,0);

        // Android Package Manager replaces the old APK code/resources during an update.
        // We additionally clear stale runtime/code cache, while preserving models,
        // documents, chat memory, databases and user preferences.
        if(previous>0 && current>previous){
            deleteContents(c.getCacheDir());
            try{deleteContents(c.getCodeCacheDir());}catch(Throwable ignored){}
        }
        if(current>0)sp.edit().putInt(KEY_LAST_VERSION,current).apply();
    }

    private static int currentVersionCode(Context c){
        try{
            PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);
            if(android.os.Build.VERSION.SDK_INT>=28)return (int)Math.min(Integer.MAX_VALUE,p.getLongVersionCode());
            return p.versionCode;
        }catch(Throwable ignored){return 0;}
    }

    private static void deleteContents(File dir){
        if(dir==null||!dir.exists())return;
        File[] files=dir.listFiles();
        if(files==null)return;
        for(File f:files){
            if(f.isDirectory())deleteContents(f);
            try{f.delete();}catch(Throwable ignored){}
        }
    }
}
