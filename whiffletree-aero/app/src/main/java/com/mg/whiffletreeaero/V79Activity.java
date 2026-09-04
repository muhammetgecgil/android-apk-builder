package com.mg.whiffletreeaero;

import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Locale;
import android.util.Base64;

public class V79Activity extends V78Activity {
  static final String META_URL="https://raw.githubusercontent.com/muhammetgecgil/android-apk-builder/main/whiffletree-aero/update/latest.json";
  TextView updateCard;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    if(BuildConfig.ALLOW_SIDELOAD_UPDATE){
      updateCard=card("GÜNCELLEME: kontrol ediliyor…", Color.rgb(11,45,61));
      proHome.addView(updateCard,2,lp());
      checkForUpdate(false);
    }
  }

  void checkForUpdate(boolean manual){
    if(!BuildConfig.ALLOW_SIDELOAD_UPDATE)return;
    new Thread(()->{
      try{
        HttpURLConnection c=(HttpURLConnection)new URL(META_URL).openConnection();
        c.setConnectTimeout(6000);c.setReadTimeout(6000);c.setUseCaches(false);
        if(c.getResponseCode()/100!=2)throw new IOException("HTTP "+c.getResponseCode());
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();
        JSONObject j=new JSONObject(sb.toString());
        int remoteCode=j.getInt("versionCode");
        String remoteName=j.optString("versionName",String.valueOf(remoteCode));
        String url=j.getString("url");
        String encoding=j.optString("encoding","raw");
        String sha256=j.optString("sha256","");
        int local=getPackageManager().getPackageInfo(getPackageName(),0).versionCode;
        runOnUiThread(()->{
          if(remoteCode>local){
            updateCard.setText("YENİ SÜRÜM: "+remoteName+" hazır. Güvenli paket indiriliyor…");
            downloadAndInstall(url,encoding,remoteName,sha256);
          } else updateCard.setText("GÜNCELLEME: Bu cihazda en güncel sürüm kurulu.");
        });
      }catch(Exception e){runOnUiThread(()->{if(updateCard!=null)updateCard.setText("GÜNCELLEME: İnternet yok veya sürüm bilgisi alınamadı. Uygulama normal çalışmaya devam eder.");});}
    }).start();
  }

  void downloadAndInstall(String url,String encoding,String ver,String expectedSha256){
    if(!BuildConfig.ALLOW_SIDELOAD_UPDATE)return;
    new Thread(()->{
      try{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(30000);c.setUseCaches(false);
        if(c.getResponseCode()/100!=2)throw new IOException("HTTP "+c.getResponseCode());
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        InputStream in=c.getInputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)bos.write(buf,0,n);in.close();
        byte[] downloaded=bos.toByteArray();
        final byte[] apkData="base64".equalsIgnoreCase(encoding)?Base64.decode(new String(downloaded,"UTF-8").replaceAll("\\s",""),Base64.DEFAULT):downloaded;
        if(expectedSha256!=null&&!expectedSha256.trim().isEmpty()){
          String actual=sha256(apkData);
          if(!actual.equalsIgnoreCase(expectedSha256.trim()))throw new SecurityException("SHA256 mismatch");
        }
        File f=new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"Whiffletree-Aero-update.apk");
        FileOutputStream out=new FileOutputStream(f);out.write(apkData);out.close();
        if(!sameSignerAsInstalled(f)){
          runOnUiThread(()->updateCard.setText("GÜNCELLEME ENGELLENDİ: APK imzası mevcut uygulamayla aynı değil. Kararlı imza taban sürümü gerekir."));
          return;
        }
        runOnUiThread(()->launchInstaller(f,ver));
      }catch(SecurityException e){runOnUiThread(()->{if(updateCard!=null)updateCard.setText("GÜNCELLEME GÜVENLİK KONTROLÜ BAŞARISIZ: paket doğrulanamadı.");});
      }catch(Exception e){runOnUiThread(()->{if(updateCard!=null)updateCard.setText("GÜNCELLEME İNDİRİLEMEDİ: "+e.getClass().getSimpleName());});}
    }).start();
  }

  String sha256(byte[] data)throws Exception{
    MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] d=md.digest(data);StringBuilder s=new StringBuilder();
    for(byte b:d)s.append(String.format(Locale.US,"%02x",b&0xff));return s.toString();
  }

  boolean sameSignerAsInstalled(File apk){
    try{
      PackageManager pm=getPackageManager();
      int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
      PackageInfo installed=pm.getPackageInfo(getPackageName(),flags);
      PackageInfo candidate=pm.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
      if(candidate==null||!getPackageName().equals(candidate.packageName))return false;
      byte[] a=firstSigner(installed),b=firstSigner(candidate);
      return a!=null&&b!=null&&MessageDigest.isEqual(a,b);
    }catch(Exception e){return false;}
  }

  byte[] firstSigner(PackageInfo p){
    if(Build.VERSION.SDK_INT>=28){
      if(p.signingInfo==null)return null;
      android.content.pm.Signature[] s=p.signingInfo.getApkContentsSigners();
      return s!=null&&s.length>0?s[0].toByteArray():null;
    }
    android.content.pm.Signature[] s=p.signatures;
    return s!=null&&s.length>0?s[0].toByteArray():null;
  }

  void launchInstaller(File f,String ver){
    if(!BuildConfig.ALLOW_SIDELOAD_UPDATE)return;
    if(Build.VERSION.SDK_INT>=26 && !getPackageManager().canRequestPackageInstalls()){
      updateCard.setText("GÜNCELLEME "+ver+" indirildi. 'Bilinmeyen uygulamaları yükle' iznini bir kez aç; sonra uygulamaya dön.");
      Intent s=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:"+getPackageName()));startActivity(s);return;
    }
    try{
      Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);
      Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,"application/vnd.android.package-archive");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);
      updateCard.setText("GÜNCELLEME "+ver+" hazır — Android kurulum ekranında Güncelle'ye bas.");
    }catch(Exception e){updateCard.setText("GÜNCELLEME KURULUMU AÇILAMADI: "+e.getClass().getSimpleName());}
  }

  @Override protected void onResume(){
    super.onResume();
    if(BuildConfig.ALLOW_SIDELOAD_UPDATE && updateCard!=null && Build.VERSION.SDK_INT>=26 && getPackageManager().canRequestPackageInstalls() && updateCard.getText().toString().contains("Bilinmeyen"))checkForUpdate(false);
  }
}
