package com.mgai.app;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public final class WhisperModelManager {
    public static final String FILE_NAME = "ggml-tiny.bin";
    public static final String MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin?download=true";
    public static final String SHA256 = "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21";
    private WhisperModelManager(){}

    public interface Listener {
        void onProgress(long done,long total,int pct);
        void onComplete(File model);
        void onError(String message);
    }

    public static File modelFile(Context c){
        File dir=new File(c.getFilesDir(),"models/whisper");
        if(!dir.exists())dir.mkdirs();
        return new File(dir,FILE_NAME);
    }

    public static boolean ready(Context c){
        File f=modelFile(c);
        return f.isFile() && f.length()>70L*1024L*1024L;
    }

    public static void ensure(Context c,Listener l){
        new Thread(()->{
            try{
                File f=modelFile(c);
                if(f.isFile() && SHA256.equalsIgnoreCase(sha256(f))){l.onComplete(f);return;}
                File part=new File(f.getAbsolutePath()+".part");
                long existing=part.isFile()?part.length():0;
                HttpURLConnection con=(HttpURLConnection)new URL(MODEL_URL).openConnection();
                con.setConnectTimeout(20000);con.setReadTimeout(60000);con.setInstanceFollowRedirects(true);
                if(existing>0)con.setRequestProperty("Range","bytes="+existing+"-");
                con.connect();
                int code=con.getResponseCode();
                boolean append=existing>0 && code==206;
                if(existing>0 && !append){existing=0;if(part.exists())part.delete();}
                long len=con.getContentLengthLong();
                long total=len>0?existing+len:-1;
                try(java.io.InputStream in=con.getInputStream(); FileOutputStream out=new FileOutputStream(part,append)){
                    byte[] buf=new byte[256*1024]; long done=existing; int n;
                    while((n=in.read(buf))>0){out.write(buf,0,n);done+=n;int pct=total>0?(int)Math.min(100,(done*100)/total):-1;l.onProgress(done,total,pct);}
                } finally {con.disconnect();}
                if(!SHA256.equalsIgnoreCase(sha256(part))){part.delete();throw new IllegalStateException("whisper_model_sha256_mismatch");}
                if(f.exists())f.delete();
                if(!part.renameTo(f))throw new IllegalStateException("whisper_model_move_failed");
                l.onComplete(f);
            }catch(Exception e){l.onError(e.getMessage()==null?e.toString():e.getMessage());}
        },"mg-ai-whisper-model").start();
    }

    private static String sha256(File f)throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[1024*1024];int n;while((n=in.read(b))>0)md.update(b,0,n);}StringBuilder sb=new StringBuilder();for(byte x:md.digest())sb.append(String.format("%02x",x));return sb.toString();
    }
}
