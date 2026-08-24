package com.mgai.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhoneNativeActivity extends Activity {
    private static final int PICK_GGUF=9101;
    private static final int PICK_DOC=9102;
    private static final int TAKE_PHOTO=9103;
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
        TextView desc=new TextView(this);desc.setText("v0.27 • Offline AI + hafıza + PDF/RAG + kamera OCR");desc.setTextSize(13);desc.setTextColor(Color.DKGRAY);root.addView(desc);
        status=new TextView(this);status.setPadding(0,dp(14),0,dp(8));root.addView(status);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setVisibility(ProgressBar.GONE);root.addView(progress,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(18)));

        history=new TextView(this);history.setTextSize(15);history.setTextColor(Color.rgb(40,44,52));history.setPadding(dp(10),dp(10),dp(10),dp(10));history.setBackgroundColor(Color.WHITE);
        ScrollView hs=new ScrollView(this);hs.addView(history);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(220));hp.setMargins(0,dp(8),0,dp(8));root.addView(hs,hp);
        renderHistory();

        prompt=new EditText(this);prompt.setHint("Bir şey sor... Belge veya kamerayla gösterip sorabilirsin.");prompt.setMinLines(2);prompt.setPadding(dp(10),dp(12),dp(10),dp(12));root.addView(prompt);
        askBtn=new Button(this);askBtn.setText("Gönder");askBtn.setAllCaps(false);askBtn.setOnClickListener(v->runLocal());root.addView(askBtn);
        output=new TextView(this);output.setTextSize(15);output.setTextColor(Color.rgb(30,34,42));output.setPadding(0,dp(12),0,dp(8));root.addView(output);

        Button camera=new Button(this);camera.setText("Kamerayla Tara → OCR → Sor");camera.setAllCaps(false);camera.setOnClickListener(v->takePhoto());root.addView(camera);
        Button addDoc=new Button(this);addDoc.setText("Belge / Foto Ekle (PDF / JPG / PNG / TXT…)");addDoc.setAllCaps(false);addDoc.setOnClickListener(v->pickDocument());root.addView(addDoc);
        Button showDocs=new Button(this);showDocs.setText("Yerel Belgeleri Göster");showDocs.setAllCaps(false);showDocs.setOnClickListener(v->output.setText(LocalDocumentStore.summary(this)));root.addView(showDocs);
        Button clearDocs=new Button(this);clearDocs.setText("Yerel Belgeleri Temizle");clearDocs.setAllCaps(false);clearDocs.setOnClickListener(v->{LocalDocumentStore.clear(this);output.setText("Yerel belge bilgi tabanı temizlendi.");updateStatus();});root.addView(clearDocs);

        Button memory=new Button(this);memory.setText("Uzun Süreli Hafızayı Göster");memory.setAllCaps(false);memory.setOnClickListener(v->{String m=LocalLongTermMemory.allText(this);output.setText(m.isEmpty()?"Uzun süreli hafıza henüz boş.":m);});root.addView(memory);
        Button clearMemory=new Button(this);clearMemory.setText("Uzun Süreli Hafızayı Temizle");clearMemory.setAllCaps(false);clearMemory.setOnClickListener(v->{LocalLongTermMemory.clear(this);output.setText("Uzun süreli yerel hafıza temizlendi.");updateStatus();});root.addView(clearMemory);
        Button clear=new Button(this);clear.setText("Sohbet Geçmişini Temizle");clear.setAllCaps(false);clear.setOnClickListener(v->{LocalChatStore.clear(this);renderHistory();output.setText("Kısa süreli sohbet geçmişi temizlendi. Uzun hafıza ve belgeler korunuyor.");});root.addView(clear);
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
    private void pickDocument(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/pdf","image/jpeg","image/png","image/webp","text/plain","text/markdown","text/csv","application/json"});startActivityForResult(i,PICK_DOC);}
    private void takePhoto(){Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);if(i.resolveActivity(getPackageManager())!=null)startActivityForResult(i,TAKE_PHOTO);else output.setText("Kamera uygulaması bulunamadı.");}

    @Override protected void onActivityResult(int r,int c,Intent data){
        super.onActivityResult(r,c,data);if(c!=RESULT_OK)return;
        if(r==TAKE_PHOTO){
            Bitmap bmp=data!=null&&data.getExtras()!=null?(Bitmap)data.getExtras().get("data"):null;
            if(bmp==null){output.setText("Kamera görüntüsü alınamadı.");return;}
            output.setText("Kamera görüntüsü telefonda OCR ile okunuyor…");
            new Thread(()->{try{
                String text=LocalOcrEngine.recognizeBitmap(bmp).trim();
                String name="Kamera "+new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date());
                String info=LocalDocumentStore.importCameraOcr(this,text,name);
                runOnUiThread(()->{output.setText("Kamera OCR yerel bilgi tabanına eklendi:\n"+info+"\nŞimdi bu görüntü hakkında soru sorabilirsin.");updateStatus();});
            }catch(Exception e){runOnUiThread(()->output.setText("Kamera OCR hatası: "+e.getMessage()));}}).start();
            return;
        }
        if(data==null||data.getData()==null)return;Uri uri=data.getData();String name=fileName(uri);
        if(r==PICK_GGUF){new Thread(()->{try{File f=LocalModelManager.importGguf(this,uri,name);runOnUiThread(()->{output.setText("Model kuruldu: "+f.getName());updateStatus();loadModel();});}catch(Exception e){runOnUiThread(()->output.setText("Model import hatası: "+e.getMessage()));}}).start();}
        else if(r==PICK_DOC){String mime=getContentResolver().getType(uri);output.setText("Belge/foto telefonda işleniyor…\nGörüntü veya taranmış PDF ise OCR yerel olarak çalışacak.");new Thread(()->{try{String info=LocalDocumentStore.importDocument(this,uri,name,mime);runOnUiThread(()->{output.setText("Yerel bilgi tabanına eklendi:\n"+info);updateStatus();});}catch(Exception e){runOnUiThread(()->output.setText("Belge/OCR import hatası: "+e.getMessage()));}}).start();}
    }
    private void loadModel(){
        File f=LocalModelManager.activeModel(this);if(f==null){output.setText("Model hazırlanıyor. İlk kullanımda internet gerekir.");return;}
        if(!LocalInferenceBridge.nativeAvailable()){output.setText("Native llama.cpp runtime yüklenemedi.");updateStatus();return;}
        output.setText("MG-AI başlatılıyor…");askBtn.setEnabled(false);
        new Thread(()->{try{if(engine!=0)LocalInferenceBridge.destroyEngine(engine);engine=LocalInferenceBridge.createEngine(f.getAbsolutePath(),4096,Math.max(2,Runtime.getRuntime().availableProcessors()-2));runOnUiThread(()->{progress.setVisibility(ProgressBar.GONE);askBtn.setEnabled(true);output.setText(engine!=0?"Hazır. İnternet olmadan sohbet, PDF, kamera OCR ve yerel RAG kullanılabilir.":"Model yüklenemedi.");updateStatus();if(engine!=0&&!pendingPrompt.isEmpty()){String q=pendingPrompt;pendingPrompt="";prompt.setText(q);runLocal();}});}catch(Throwable t){runOnUiThread(()->{askBtn.setEnabled(true);output.setText("Native yükleme hatası: "+t.getMessage());});}}).start();
    }
    private void runLocal(){
        String p=prompt.getText().toString().trim();if(p.isEmpty())return;
        if(engine==0){pendingPrompt=p;prompt.setText("");output.setText("Sorun kuyruğa alındı. Model hazır olduğunda otomatik cevaplanacak.");if(LocalModelManager.activeModel(this)==null&&!downloading)autoInstallDefault();return;}
        boolean remembered=LocalLongTermMemory.maybeRememberUserMessage(this,p);
        if(p.toLowerCase(new Locale("tr","TR")).startsWith("proje:"))LocalLongTermMemory.addProjectNote(this,p.substring(6).trim());
        LocalChatStore.add(this,"user",p);renderHistory();prompt.setText("");askBtn.setEnabled(false);output.setText(remembered?"Hafızaya kaydedildi. Düşünüyor…":"Düşünüyor…");
        String ctx=LocalChatStore.transcript(this,6);
        String mem=LocalLongTermMemory.relevantContext(this,p,6);
        String docs=LocalDocumentStore.retrieve(this,p,3);
        String full="Sen MG-AI adlı yardımcı yapay zekasın. Türkçe sorulara Türkçe cevap ver. Kullanıcıya ait uzun süreli hafıza ve yerel belge/OCR parçaları varsa yalnız ilgili olduğu ölçüde kullan. Belgelerde olmayan bir bilgiyi belge varmış gibi uydurma.\n\nUZUN SÜRELİ HAFIZA:\n"+(mem.isEmpty()?"(ilgili kayıt yok)":mem)+"\n\nYEREL BELGE/OCR BAĞLAMI:\n"+(docs.isEmpty()?"(ilgili belge parçası yok)":docs)+"\n\nSON KONUŞMA:\n"+ctx+"\nMG-AI:";
        new Thread(()->{try{String ans=LocalInferenceBridge.generate(engine,full,512,0.7f);LocalChatStore.add(this,"assistant",ans);runOnUiThread(()->{askBtn.setEnabled(true);output.setText(ans);renderHistory();updateStatus();});}catch(Throwable t){runOnUiThread(()->{askBtn.setEnabled(true);output.setText("Inference hatası: "+t.getMessage());});}}).start();
    }
    private String fileName(Uri u){String n="belge";try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int ix=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(ix>=0)n=c.getString(ix);}}catch(Exception ignored){}return n;}
    private void updateStatus(){status.setText("Model: "+LocalModelManager.status(this)+"\nMotor: "+(LocalInferenceBridge.nativeAvailable()?"HAZIR":"HATA")+" • Offline: "+(LocalModelManager.activeModel(this)!=null?"EVET":"KURULUYOR")+" • Uzun hafıza: "+LocalLongTermMemory.count(this)+" kayıt\nBelgeler/OCR: "+LocalDocumentStore.summary(this).replace('\n',' '));}
    @Override protected void onDestroy(){if(downloading)LocalModelManager.cancelDownload();if(engine!=0&&LocalInferenceBridge.nativeAvailable()){try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}}super.onDestroy();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
