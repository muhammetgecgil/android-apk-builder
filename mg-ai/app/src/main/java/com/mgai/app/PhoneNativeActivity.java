package com.mgai.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

public class PhoneNativeActivity extends Activity {
    private static final int PICK_GGUF=9101;
    private TextView status,output,history;
    private EditText prompt;
    private ProgressBar progress;
    private Button downloadBtn,cancelBtn,askBtn;
    private CheckBox wifiOnly;
    private long engine=0;
    private boolean downloading=false;
    private String pendingPrompt="";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(24),dp(18),dp(18));root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this);title.setText("MG-AI");title.setTextSize(30);title.setTextColor(Color.rgb(20,24,32));root.addView(title);
        TextView desc=new TextView(this);desc.setText("v0.22 • Offline sohbet + yerel hafıza");desc.setTextSize(13);desc.setTextColor(Color.DKGRAY);root.addView(desc);
        status=new TextView(this);status.setPadding(0,dp(14),0,dp(8));root.addView(status);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setVisibility(ProgressBar.GONE);root.addView(progress,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(18)));

        history=new TextView(this);history.setTextSize(15);history.setTextColor(Color.rgb(40,44,52));history.setPadding(dp(10),dp(10),dp(10),dp(10));history.setBackgroundColor(Color.WHITE);
        ScrollView hs=new ScrollView(this);hs.addView(history);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(220));hp.setMargins(0,dp(8),0,dp(8));root.addView(hs,hp);
        renderHistory();

        prompt=new EditText(this);prompt.setHint("Bir şey sor...");prompt.setMinLines(2);prompt.setPadding(dp(10),dp(12),dp(10),dp(12));root.addView(prompt);
        askBtn=new Button(this);askBtn.setText("Gönder");askBtn.setAllCaps(false);askBtn.setOnClickListener(v->runLocal());root.addView(askBtn);
        output=new TextView(this);output.setTextSize(15);output.setTextColor(Color.rgb(30,34,42));output.setPadding(0,dp(12),0,dp(8));root.addView(output);

        Button clear=new Button(this);clear.setText("Sohbet Geçmişini Temizle");clear.setAllCaps(false);clear.setOnClickListener(v->{LocalChatStore.clear(this);renderHistory();output.setText("Yerel sohbet geçmişi temizlendi.");});root.addView(clear);
        wifiOnly=new CheckBox(this);wifiOnly.setText("İlk model kurulumunu yalnız Wi-Fi ile yap");wifiOnly.setChecked(LocalModelManager.wifiOnly(this));wifiOnly.setOnCheckedChangeListener((b1,v)->LocalModelManager.setWifiOnly(this,v));root.addView(wifiOnly);
        downloadBtn=new Button(this);downloadBtn.setText("Modeli İndir / Devam Et");downloadBtn.setAllCaps(false);downloadBtn.setOnClickListener(v->autoInstallDefault());root.addView(downloadBtn);
        cancelBtn=new Button(this);cancelBtn.setText("İndirmeyi Durdur");cancelBtn.setAllCaps(false);cancelBtn.setEnabled(false);cancelBtn.setOnClickListener(v->{LocalModelManager.cancelDownload();output.setText("İndirme durduruluyor; yarım dosya korunacak.");});root.addView(cancelBtn);
        Button pick=new Button(this);pick.setText("Gelişmiş: Başka GGUF Model Seç");pick.setAllCaps(false);pick.setOnClickListener(v->pickModel());root.addView(pick);
        Button advanced=new Button(this);advanced.setText("Gelişmiş / Sistem Merkezi");advanced.setAllCaps(false);advanced.setOnClickListener(v->startActivity(new Intent(this,DashboardActivity.class)));root.addView(advanced);

        setContentView(root);updateStatus();
        if(LocalModelManager.activeModel(this)==null)autoInstallDefault();else loadModel();
    }

    private void renderHistory(){String h=LocalChatStore.historyText(this);history.setText(h.isEmpty()?"Henüz konuşma yok.":h);}
    private void setDownloading(boolean v){downloading=v;downloadBtn.setEnabled(!v);cancelBtn.setEnabled(v);wifiOnly.setEnabled(!v);}
    private void autoInstallDefault(){
        if(downloading)return;setDownloading(true);progress.setVisibility(ProgressBar.VISIBLE);progress.setIndeterminate(true);
        long partial=LocalModelManager.partialBytes(this);output.setText((partial>0?"Model indirmesine kaldığı yerden devam ediliyor…":"MG-AI ilk kullanım için modelini hazırlıyor…")+"\nQwen2.5 1.5B Instruct • Q4_K_M • yaklaşık 1.12 GB");
        LocalModelManager.downloadDefaultModel(this,new LocalModelManager.DownloadListener(){
            @Override public void onProgress(long done,long total,int pct){runOnUiThread(()->{progress.setIndeterminate(pct<0);if(pct>=0)progress.setProgress(pct);String d=String.format(Locale.US,"%.0f MB",done/1048576.0);String t=total>0?String.format(Locale.US," / %.0f MB",total/1048576.0):"";output.setText("Model indiriliyor: "+(pct>=0?pct+"%":"…")+"\n"+d+t);updateStatus();});}
            @Override public void onComplete(File model){runOnUiThread(()->{setDownloading(false);progress.setProgress(100);output.setText("Model doğrulandı. MG-AI başlatılıyor…");updateStatus();loadModel();});}
            @Override public void onCancelled(long done){runOnUiThread(()->{setDownloading(false);progress.setVisibility(ProgressBar.GONE);output.setText("İndirme durduruldu. "+String.format(Locale.US,"%.0f MB",done/1048576.0)+" saklandı; sonra devam edebilirsin.");updateStatus();});}
            @Override public void onError(String message){runOnUiThread(()->{setDownloading(false);progress.setVisibility(ProgressBar.GONE);output.setText("Model kurulumu: "+message+"\nYarım dosya varsa sonraki denemede devam edilir.");updateStatus();});}
        });
    }

    private void pickModel(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");startActivityForResult(i,PICK_GGUF);}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r!=PICK_GGUF||c!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();String name=fileName(uri);new Thread(()->{try{File f=LocalModelManager.importGguf(this,uri,name);runOnUiThread(()->{output.setText("Model kuruldu: "+f.getName());updateStatus();loadModel();});}catch(Exception e){runOnUiThread(()->output.setText("Model import hatası: "+e.getMessage()));}}).start();}
    private void loadModel(){
        File f=LocalModelManager.activeModel(this);if(f==null){output.setText("Model hazırlanıyor. İlk kullanımda internet gerekir.");return;}
        if(!LocalInferenceBridge.nativeAvailable()){output.setText("Native llama.cpp runtime yüklenemedi.");updateStatus();return;}
        output.setText("MG-AI başlatılıyor…");askBtn.setEnabled(false);
        new Thread(()->{try{if(engine!=0)LocalInferenceBridge.destroyEngine(engine);engine=LocalInferenceBridge.createEngine(f.getAbsolutePath(),4096,Math.max(2,Runtime.getRuntime().availableProcessors()-2));runOnUiThread(()->{progress.setVisibility(ProgressBar.GONE);askBtn.setEnabled(true);output.setText(engine!=0?"Hazır. İnternet olmadan soru sorabilirsin.":"Model yüklenemedi.");updateStatus();if(engine!=0&&!pendingPrompt.isEmpty()){String q=pendingPrompt;pendingPrompt="";prompt.setText(q);runLocal();}});}catch(Throwable t){runOnUiThread(()->{askBtn.setEnabled(true);output.setText("Native yükleme hatası: "+t.getMessage());});}}).start();
    }
    private void runLocal(){
        String p=prompt.getText().toString().trim();if(p.isEmpty())return;
        if(engine==0){pendingPrompt=p;prompt.setText("");output.setText("Sorun kuyruğa alındı. Model hazır olduğunda otomatik cevaplanacak.");if(LocalModelManager.activeModel(this)==null&&!downloading)autoInstallDefault();return;}
        LocalChatStore.add(this,"user",p);renderHistory();prompt.setText("");askBtn.setEnabled(false);output.setText("Düşünüyor…");
        String ctx=LocalChatStore.transcript(this,6);
        String full="Sen MG-AI adlı yardımcı yapay zekasın. Türkçe sorulara Türkçe cevap ver. Aşağıdaki konuşma bağlamını kullan.\n\n"+ctx+"\nMG-AI:";
        new Thread(()->{try{String ans=LocalInferenceBridge.generate(engine,full,512,0.7f);LocalChatStore.add(this,"assistant",ans);runOnUiThread(()->{askBtn.setEnabled(true);output.setText(ans);renderHistory();});}catch(Throwable t){runOnUiThread(()->{askBtn.setEnabled(true);output.setText("Inference hatası: "+t.getMessage());});}}).start();
    }
    private String fileName(Uri u){String n="mg-ai.gguf";try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int ix=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(ix>=0)n=c.getString(ix);}}catch(Exception ignored){}return n;}
    private void updateStatus(){status.setText("Model: "+LocalModelManager.status(this)+"\nMotor: "+(LocalInferenceBridge.nativeAvailable()?"HAZIR":"HATA")+" • Offline: "+(LocalModelManager.activeModel(this)!=null?"EVET":"KURULUYOR"));}
    @Override protected void onDestroy(){if(downloading)LocalModelManager.cancelDownload();if(engine!=0&&LocalInferenceBridge.nativeAvailable()){try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}}super.onDestroy();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
