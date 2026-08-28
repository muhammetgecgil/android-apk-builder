package com.mg.whiffletreeaero;

import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
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
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();
        JSONObject j=new JSONObject(sb.toString());
        int remoteCode=j.getInt("versionCode");String remoteName=j.optString("versionName",String.valueOf(remoteCode));String url=j.getString("url");String encoding=j.optString("encoding","raw");
        int local=getPackageManager().getPackageInfo(getPackageName(),0).versionCode;
        runOnUiThread(()->{
          if(remoteCode>local){
            updateCard.setText("YENİ SÜRÜM: "+remoteName+" hazır. İndiriliyor; Android kurulum onayı açılacak.");
            downloadAndInstall(url,encoding,remoteName);
          } else updateCard.setText("GÜNCELLEME: Bu cihazda en güncel sürüm kurulu.");
        });
      }catch(Exception e){runOnUiThread(()->{if(updateCard!=null)updateCard.setText("GÜNCELLEME: İnternet yok veya sürüm bilgisi alınamadı. Uygulama normal çalışmaya devam eder.");});}
    }).start();
  }

  void downloadAndInstall(String url,String encoding,String ver){
    if(!BuildConfig.ALLOW_SIDELOAD_UPDATE)return;
    new Thread(()->{
      try{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setUseCaches(false);
        ByteArrayOutputStream bos=new ByteArrayOutputStream();InputStream in=c.getInputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)bos.write(buf,0,n);in.close();
        byte[] data=bos.toByteArray();if("base64".equalsIgnoreCase(encoding))data=Base64.decode(new String(data,"UTF-8").replaceAll("\\s",""),Base64.DEFAULT);
        File f=new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"Whiffletree-Aero-update.apk");FileOutputStream out=new FileOutputStream(f);out.write(data);out.close();
        runOnUiThread(()->launchInstaller(f,ver));
      }catch(Exception e){runOnUiThread(()->{if(updateCard!=null)updateCard.setText("GÜNCELLEME İNDİRİLEMEDİ: "+e.getClass().getSimpleName());});}
    }).start();
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
