package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class LocalModelManager {
    private static final String PREFS="mg_phone_native";
    private static final String KEY_MODEL_PATH="local_model_path";
    private static final String KEY_MODEL_NAME="local_model_name";

    private LocalModelManager() {}

    public static File modelDir(Context c){
        File d=new File(c.getFilesDir(),"models");
        if(!d.exists()) d.mkdirs();
        return d;
    }

    public static File importGguf(Context c, Uri uri, String displayName) throws Exception {
        String safe=(displayName==null||displayName.trim().isEmpty())?"mg-ai.gguf":displayName.replaceAll("[^A-Za-z0-9._-]","_");
        if(!safe.toLowerCase().endsWith(".gguf")) safe += ".gguf";
        File out=new File(modelDir(c),safe);
        try(InputStream in=c.getContentResolver().openInputStream(uri); FileOutputStream fos=new FileOutputStream(out)){
            if(in==null) throw new IllegalArgumentException("model_stream_unavailable");
            byte[] buf=new byte[1024*1024]; int n; long total=0;
            while((n=in.read(buf))>0){fos.write(buf,0,n); total+=n;}
            fos.flush();
            if(total < 1024*1024) throw new IllegalArgumentException("gguf_file_too_small");
        }
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        p.edit().putString(KEY_MODEL_PATH,out.getAbsolutePath()).putString(KEY_MODEL_NAME,safe).apply();
        return out;
    }

    public static File activeModel(Context c){
        String path=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_MODEL_PATH,"");
        if(path.isEmpty()) return null;
        File f=new File(path); return f.isFile()?f:null;
    }

    public static String activeModelName(Context c){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_MODEL_NAME,"");
    }

    public static String status(Context c){
        File f=activeModel(c);
        if(f==null) return "Yerel model kurulmadı";
        return activeModelName(c)+" • "+String.format(java.util.Locale.US,"%.2f GB",f.length()/1073741824.0);
    }
}
