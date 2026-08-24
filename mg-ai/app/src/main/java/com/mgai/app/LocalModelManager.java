package com.mgai.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.StatFs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LocalModelManager {
    private static final String PREFS="mg_phone_native";
    private static final String KEY_MODEL_PATH="local_model_path";
    private static final String KEY_MODEL_NAME="local_model_name";
    private static final String KEY_WIFI_ONLY="wifi_only";
    private static final AtomicBoolean CANCEL=new AtomicBoolean(false);

    public static final String DEFAULT_MODEL_VERSION="qwen2.5-1.5b-instruct-q4_k_m-v1";
    public static final String DEFAULT_MODEL_NAME="qwen2.5-1.5b-instruct-q4_k_m.gguf";
    public static final String DEFAULT_MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf?download=true";
    public static final String DEFAULT_MODEL_SHA256="6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e";
    private static final long MIN_FREE_BYTES=1500L*1024L*1024L;

    public interface DownloadListener {
        void onProgress(long downloaded,long total,int percent);
        void onComplete(File model);
        void onError(String message);
        default void onCancelled(long downloaded) {}
    }

    private LocalModelManager() {}
    public static File modelDir(Context c){File d=new File(c.getFilesDir(),"models");if(!d.exists())d.mkdirs();return d;}
    public static boolean hasEnoughSpace(Context c){return new StatFs(modelDir(c).getAbsolutePath()).getAvailableBytes()>=MIN_FREE_BYTES;}
    public static boolean wifiOnly(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_WIFI_ONLY,true);}
    public static void setWifiOnly(Context c,boolean v){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY_WIFI_ONLY,v).apply();}
    public static boolean isOnWifi(Context c){
        ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE); if(cm==null)return false;
        Network n=cm.getActiveNetwork(); if(n==null)return false; NetworkCapabilities nc=cm.getNetworkCapabilities(n);
        return nc!=null&&nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
    public static long partialBytes(Context c){File f=new File(modelDir(c),DEFAULT_MODEL_NAME+".part");return f.isFile()?f.length():0;}
    public static void cancelDownload(){CANCEL.set(true);}

    public static File importGguf(Context c,Uri uri,String displayName)throws Exception{
        String safe=(displayName==null||displayName.trim().isEmpty())?"mg-ai.gguf":displayName.replaceAll("[^A-Za-z0-9._-]","_");if(!safe.toLowerCase().endsWith(".gguf"))safe+=".gguf";
        File out=new File(modelDir(c),safe);
        try(InputStream in=c.getContentResolver().openInputStream(uri);FileOutputStream fos=new FileOutputStream(out)){if(in==null)throw new IllegalArgumentException("model_stream_unavailable");byte[] buf=new byte[1024*1024];int n;long total=0;while((n=in.read(buf))>0){fos.write(buf,0,n);total+=n;}fos.flush();if(total<1024*1024)throw new IllegalArgumentException("gguf_file_too_small");}
        activate(c,out,safe);return out;
    }

    public static void downloadDefaultModel(Context context,DownloadListener listener){
        final Context c=context.getApplicationContext(); CANCEL.set(false);
        new Thread(()->{
            File part=new File(modelDir(c),DEFAULT_MODEL_NAME+".part"); File out=new File(modelDir(c),DEFAULT_MODEL_NAME);
            try{
                if(out.isFile()&&DEFAULT_MODEL_SHA256.equalsIgnoreCase(sha256(out))){activate(c,out,DEFAULT_MODEL_NAME);listener.onComplete(out);return;}
                if(wifiOnly(c)&&!isOnWifi(c))throw new IllegalStateException("Wi-Fi-only açık. Wi-Fi ağına bağlan veya bu seçeneği kapat.");
                if(!hasEnoughSpace(c)&&!part.isFile())throw new IllegalStateException("En az 1.5 GB boş alan gerekli.");
                long resume=part.isFile()?part.length():0;
                HttpURLConnection conn=(HttpURLConnection)new URL(DEFAULT_MODEL_URL).openConnection();conn.setConnectTimeout(20000);conn.setReadTimeout(30000);conn.setInstanceFollowRedirects(true);conn.setRequestProperty("User-Agent","MG-AI-Android/0.20");if(resume>0)conn.setRequestProperty("Range","bytes="+resume+"-");conn.connect();
                int code=conn.getResponseCode(); boolean resumed=resume>0&&code==206; if(code<200||code>=300)throw new IllegalStateException("Model indirme HTTP "+code); if(resume>0&&!resumed){resume=0;if(part.exists()&&!part.delete())throw new IllegalStateException("Yarım indirme sıfırlanamadı.");}
                long remaining=conn.getContentLengthLong(); long total=remaining>0?resume+remaining:-1; long done=resume;
                try(InputStream in=conn.getInputStream();FileOutputStream fos=new FileOutputStream(part,resume>0&&resumed)){byte[] buf=new byte[1024*1024];int n;int last=-1;while((n=in.read(buf))>0){if(CANCEL.get()){fos.flush();listener.onCancelled(done);return;}fos.write(buf,0,n);done+=n;int pct=total>0?(int)Math.min(100,(done*100L)/total):-1;if(pct!=last){last=pct;listener.onProgress(done,total,pct);}}fos.flush();}finally{conn.disconnect();}
                String actual=sha256(part);if(!DEFAULT_MODEL_SHA256.equalsIgnoreCase(actual)){part.delete();throw new IllegalStateException("Model SHA-256 doğrulaması başarısız; yarım dosya temizlendi.");}
                if(out.exists()&&!out.delete())throw new IllegalStateException("Eski model silinemedi.");if(!part.renameTo(out))throw new IllegalStateException("Model kurulumuna taşınamadı.");activate(c,out,DEFAULT_MODEL_NAME);listener.onComplete(out);
            }catch(Throwable t){listener.onError(t.getMessage()==null?t.getClass().getSimpleName():t.getMessage());}
        },"mg-model-download").start();
    }

    private static void activate(Context c,File out,String name){SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);p.edit().putString(KEY_MODEL_PATH,out.getAbsolutePath()).putString(KEY_MODEL_NAME,name).apply();}
    public static String sha256(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[1024*1024];int n;while((n=in.read(b))>0)md.update(b,0,n);}StringBuilder sb=new StringBuilder();for(byte x:md.digest())sb.append(String.format(Locale.US,"%02x",x));return sb.toString();}
    public static File activeModel(Context c){String path=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_MODEL_PATH,"");if(path.isEmpty())return null;File f=new File(path);return f.isFile()?f:null;}
    public static String activeModelName(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_MODEL_NAME,"");}
    public static String status(Context c){File f=activeModel(c);if(f==null){long p=partialBytes(c);return p>0?"Yarım indirme: "+String.format(Locale.US,"%.0f MB",p/1048576.0):"Yerel model kurulmadı";}return activeModelName(c)+" • "+String.format(Locale.US,"%.2f GB",f.length()/1073741824.0);}
}
