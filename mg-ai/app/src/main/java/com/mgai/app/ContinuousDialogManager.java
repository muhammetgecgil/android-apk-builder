package com.mgai.app;

import android.content.Context;
import android.content.Intent;

public final class ContinuousDialogManager {
    private static final String PREFS="mg_continuous_dialog";
    private static final String KEY_ENABLED="enabled";
    private static volatile boolean suppressNext=false;

    private ContinuousDialogManager(){}

    public static boolean enabled(Context c){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_ENABLED,false);
    }

    public static void setEnabled(Context c,boolean enabled){
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED,enabled).apply();
        if(!enabled) suppressNext=true;
    }

    public static void suppressNextAutoListen(){ suppressNext=true; }

    public static void onSpeechDone(Context context){
        if(context==null)return;
        if(suppressNext){suppressNext=false;return;}
        if(!enabled(context))return;
        Intent i=LocalSpeechInput.intent();
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try{context.getApplicationContext().startActivity(i);}catch(Throwable ignored){}
    }
}
