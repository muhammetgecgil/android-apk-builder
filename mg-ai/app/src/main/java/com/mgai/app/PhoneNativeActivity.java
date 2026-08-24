package com.mgai.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class PhoneNativeActivity extends Activity {
    private static final int PICK_GGUF=9101;
    private TextView status;
    private EditText prompt;
    private TextView output;
    private long engine=0;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(24),dp(18),dp(18)); root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this); title.setText("Phone-Native MG-AI"); title.setTextSize(28); title.setTextColor(Color.rgb(20,24,32)); root.addView(title);
        TextView desc=new TextView(this); desc.setText("Sunucusuz çalışma modu • GGUF model telefon içinde"); desc.setTextSize(13); desc.setTextColor(Color.DKGRAY); root.addView(desc);
        status=new TextView(this); status.setPadding(0,dp(16),0,dp(12)); root.addView(status);
        Button pick=new Button(this); pick.setText("GGUF Model Seç / Kur"); pick.setAllCaps(false); pick.setOnClickListener(v->pickModel()); root.addView(pick);
        Button load=new Button(this); load.setText("Yerel Modeli Yükle"); load.setAllCaps(false); load.setOnClickListener(v->loadModel()); root.addView(load);
        prompt=new EditText(this); prompt.setHint("Offline soru yaz..."); prompt.setMinLines(3); prompt.setPadding(dp(10),dp(14),dp(10),dp(14)); root.addView(prompt);
        Button ask=new Button(this); ask.setText("Telefonda Çalıştır"); ask.setAllCaps(false); ask.setOnClickListener(v->runLocal()); root.addView(ask);
        output=new TextView(this); output.setTextSize(16); output.setTextColor(Color.rgb(30,34,42)); output.setPadding(0,dp(18),0,0); root.addView(output);
        updateStatus(); setContentView(root);
    }

    private void pickModel(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/octet-stream"); startActivityForResult(i,PICK_GGUF);
    }
    @Override protected void onActivityResult(int r,int c,Intent data){
        super.onActivityResult(r,c,data); if(r!=PICK_GGUF||c!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData(); String name=fileName(uri);
        new Thread(()->{try{File f=LocalModelManager.importGguf(this,uri,name);runOnUiThread(()->{output.setText("Model telefona kopyalandı: "+f.getName());updateStatus();});}catch(Exception e){runOnUiThread(()->output.setText("Model import hatası: "+e.getMessage()));}}).start();
    }
    private void loadModel(){
        File f=LocalModelManager.activeModel(this); if(f==null){output.setText("Önce GGUF model seç.");return;}
        if(!LocalInferenceBridge.nativeAvailable()){output.setText("GGUF hazır. Native llama.cpp motoru henüz bu build'e gömülü değil; sonraki build bunu etkinleştirecek.");updateStatus();return;}
        new Thread(()->{try{if(engine!=0)LocalInferenceBridge.destroyEngine(engine);engine=LocalInferenceBridge.createEngine(f.getAbsolutePath(),4096,Math.max(2,Runtime.getRuntime().availableProcessors()-2));runOnUiThread(()->{output.setText(engine!=0?"Yerel model yüklendi.":"Model yüklenemedi.");updateStatus();});}catch(Throwable t){runOnUiThread(()->output.setText("Native yükleme hatası: "+t.getMessage()));}}).start();
    }
    private void runLocal(){
        String p=prompt.getText().toString().trim(); if(p.isEmpty())return; if(engine==0){output.setText("Önce yerel modeli yükle.");return;}
        output.setText("Telefon üzerinde üretiliyor..."); new Thread(()->{try{String ans=LocalInferenceBridge.generate(engine,p,512,0.7f);runOnUiThread(()->output.setText(ans));}catch(Throwable t){runOnUiThread(()->output.setText("Inference hatası: "+t.getMessage()));}}).start();
    }
    private String fileName(Uri u){String n="mg-ai.gguf";try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int ix=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(ix>=0)n=c.getString(ix);}}catch(Exception ignored){}return n;}
    private void updateStatus(){status.setText("Model: "+LocalModelManager.status(this)+"\nNative runtime: "+(LocalInferenceBridge.nativeAvailable()?"HAZIR":"BEKLENİYOR")+"\nInternet gereksinimi: YOK");}
    @Override protected void onDestroy(){if(engine!=0&&LocalInferenceBridge.nativeAvailable()){try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}}super.onDestroy();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
