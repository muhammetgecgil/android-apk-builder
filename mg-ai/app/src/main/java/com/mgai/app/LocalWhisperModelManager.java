package com.mgai.app;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public final class LocalWhisperModelManager {
    private static final String URL_STR="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin?download=true";
    private static final String NAME="ggml-tiny.bin";
    private static final long EXPECTED_BYTES=77691713L;
    private static final String SHA256="be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21";
    private LocalWhisperModelManager(){}

    public interface Progress { void onProgress(long done,long total); }

    public static File modelFile(Context c){
        File d=new File(c.getFilesDir(),"whisper");
        if(!d.exists())d.mkdirs();
        return new File(d,NAME);
    }

    public static boolean isReady(Context c){
        File f=modelFile(c);
        return f.isFile()&&f.length()==EXPECTED_BYTES;
    }

    public static File ensure(Context c,Progress progress) throws Exception {
        File dst=modelFile(c);
        if(dst.isFile()&&dst.length()==EXPECTED_BYTES&&SHA256.equalsIgnoreCase(sha256(dst)))return dst;
        if(dst.exists())dst.delete();
        File part=new File(dst.getParentFile(),NAME+".part");
        long have=part.exists()?part.length():0L;
        if(have>EXPECTED_BYTES){part.delete();have=0L;}

        HttpURLConnection con=(HttpURLConnection)new URL(URL_STR).openConnection();
        con.setConnectTimeout(20000);con.setReadTimeout(60000);con.setInstanceFollowRedirects(true);
        if(have>0)con.setRequestProperty("Range","bytes="+have+"-");
        int code=con.getResponseCode();
        if(code!=200&&code!=206)throw new IllegalStateException("whisper_download_http_"+code);
        if(have>0&&code==200){part.delete();have=0L;}
        long total=EXPECTED_BYTES;
        try(InputStream in=con.getInputStream();FileOutputStream out=new FileOutputStream(part,have>0)){
            byte[] buf=new byte[256*1024];int n;long done=have;
            while((n=in.read(buf))>0){out.write(buf,0,n);done+=n;if(progress!=null)progress.onProgress(done,total);}
        } finally { con.disconnect(); }
        if(part.length()!=EXPECTED_BYTES)throw new IllegalStateException("whisper_size_mismatch_"+part.length());
        String got=sha256(part);
        if(!SHA256.equalsIgnoreCase(got)){part.delete();throw new IllegalStateException("whisper_sha256_mismatch");}
        if(!part.renameTo(dst)){
            try(FileInputStream in=new FileInputStream(part);FileOutputStream out=new FileOutputStream(dst)){
                byte[] b=new byte[256*1024];int n;while((n=in.read(b))>0)out.write(b,0,n);
            }
            part.delete();
        }
        return dst;
    }

    private static String sha256(File f) throws Exception {
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[256*1024];int n;while((n=in.read(b))>0)md.update(b,0,n);}
        StringBuilder s=new StringBuilder();for(byte x:md.digest())s.append(String.format("%02x",x));return s.toString();
    }
}
