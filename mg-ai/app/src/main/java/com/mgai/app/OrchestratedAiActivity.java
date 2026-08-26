package com.mgai.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class OrchestratedAiActivity extends Activity {
    private static final int VOICE=9401, DOC=9402;
    private TextView state,chat,route;
    private EditText input;
    private Button send;
    private long engine=0;
    private int ctx=0,threads=0;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(12));root.setBackgroundColor(Color.rgb(7,17,29));
        TextView title=new TextView(this);title.setText("MG-AI");title.setTextSize(30);title.setTextColor(Color.WHITE);title.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(title);
        TextView sub=new TextView(this);sub.setText("v0.59 • PHONE-NATIVE AI ORCHESTRATOR");sub.setTextSize(12);sub.setTextColor(Color.rgb(104,220,235));sub.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(sub);

        state=card();state.setText("Başlatılıyor…");root.addView(state);
        route=card();route.setText("ORCHESTRATOR • Hazır olduğunda rotayı otomatik seçecek");root.addView(route);

        chat=new TextView(this);chat.setTextSize(16);chat.setTextColor(Color.rgb(235,241,247));chat.setPadding(dp(12),dp(12),dp(12),dp(12));chat.setText("MG-AI: Merhaba. Yaz, konuş veya belge ekle; hangi motorun gerektiğini ben seçeceğim.");
        ScrollView sv=new ScrollView(this);sv.addView(chat);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f);sp.setMargins(0,dp(10),0,dp(10));root.addView(sv,sp);

        input=new EditText(this);input.setHint("MG-AI'ye yaz…");input.setHintTextColor(Color.rgb(130,145,160));input.setTextColor(Color.WHITE);input.setMinLines(2);input.setBackgroundColor(Color.rgb(18,34,51));input.setPadding(dp(12),dp(10),dp(12),dp(10));root.addView(input);
        send=button("Gönder");send.setOnClickListener(v->ask());root.addView(send);

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button voice=button("🎙 Konuş");voice.setOnClickListener(v->voice());row.addView(voice,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        Button doc=button("＋ Belge");doc.setOnClickListener(v->pickDoc());row.addView(doc,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        root.addView(row);
        Button advanced=button("Gelişmiş Sistem Merkezi");advanced.setOnClickListener(v->startActivity(new Intent(this,DashboardActivity.class)));root.addView(advanced);
        setContentView(root);
        refreshChat();ensureEngine();
    }

    private TextView card(){TextView t=new TextView(this);t.setTextSize(13);t.setTextColor(Color.rgb(208,222,233));t.setPadding(dp(10),dp(9),dp(10),dp(9));t.setBackgroundColor(Color.rgb(15,30,46));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.setMargins(0,dp(8),0,0);t.setLayoutParams(p);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}

    private void ensureEngine(){
        File model=LocalModelManager.activeModel(this);
        if(model==null){state.setText("MODEL GEREKLİ • İlk kurulumu açıyorum…");startActivity(new Intent(this,PhoneNativeActivity.class));return;}
        if(!LocalInferenceBridge.nativeAvailable()){state.setText("HATA • llama.cpp runtime yüklenemedi");return;}
        AdaptivePerformanceManager.Profile p=AdaptivePerformanceManager.choose(this);
        if(engine!=0&&ctx==p.contextSize&&threads==p.threads){state.setText("HAZIR • "+p.summary());return;}
        send.setEnabled(false);state.setText("MODEL YÜKLENİYOR • "+p.summary());
        new Thread(()->{try{if(engine!=0)LocalInferenceBridge.destroyEngine(engine);engine=LocalInferenceBridge.createEngine(model.getAbsolutePath(),p.contextSize,p.threads);ctx=p.contextSize;threads=p.threads;runOnUiThread(()->{send.setEnabled(engine!=0);state.setText(engine!=0?"HAZIR • "+p.summary():"Model yüklenemedi");});}catch(Throwable t){runOnUiThread(()->{send.setEnabled(true);state.setText("Motor hatası: "+t.getMessage());});}}).start();
    }

    private void ask(){
        String q=input.getText().toString().trim();if(q.isEmpty())return;
        if(engine==0){state.setText("Motor hazır değil; model hazırlanıyor…");ensureEngine();return;}
        AdaptivePerformanceManager.Profile perf=AdaptivePerformanceManager.choose(this);
        AiOrchestrator.Plan plan=AiOrchestrator.plan(this,q,perf);
        route.setText("ORCHESTRATOR → "+plan.summary());
        boolean remembered=LocalLongTermMemory.maybeRememberUserMessage(this,q);
        if(q.toLowerCase().startsWith("proje:"))LocalLongTermMemory.addProjectNote(this,q.substring(6).trim());
        LocalChatStore.add(this,"user",q);input.setText("");refreshChat();send.setEnabled(false);state.setText((remembered?"HAFIZAYA ALINDI • ":"")+plan.label()+" çalışıyor…");
        String full=AiOrchestrator.buildPrompt(this,q,plan);
        new Thread(()->{try{String ans=LocalInferenceBridge.generate(engine,full,plan.maxTokens,plan.temperature);SelfTuningManager.observe(this,perf,LocalInferenceBridge.lastMetrics());LocalChatStore.add(this,"assistant",ans);runOnUiThread(()->{send.setEnabled(true);state.setText("HAZIR • "+plan.label()+" tamamlandı");refreshChat();});}catch(Throwable t){runOnUiThread(()->{send.setEnabled(true);state.setText("Inference hatası: "+t.getMessage());});}}).start();
    }

    private void voice(){Intent i=LocalSpeechInput.intent();if(i.resolveActivity(getPackageManager())!=null)startActivityForResult(i,VOICE);else state.setText("Yerel Whisper ekranı bulunamadı");}
    private void pickDoc(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/pdf","image/jpeg","image/png","image/webp","text/plain","text/markdown","text/csv","application/json"});startActivityForResult(i,DOC);}

    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(c!=RESULT_OK||data==null)return;if(r==VOICE){ArrayList<String> xs=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(xs!=null&&!xs.isEmpty()){input.setText(xs.get(0));ask();}return;}if(r==DOC&&data.getData()!=null){Uri u=data.getData();String name=fileName(u),mime=getContentResolver().getType(u);state.setText("Belge telefonda işleniyor…");new Thread(()->{try{String info=LocalDocumentStore.importDocument(this,u,name,mime);runOnUiThread(()->{state.setText("BELGE HAZIR • "+info);route.setText("ORCHESTRATOR → Sonraki ilgili soruda RAG otomatik kullanılacak");});}catch(Exception e){runOnUiThread(()->state.setText("Belge hatası: "+e.getMessage()));}}).start();}}

    private String fileName(Uri u){String n="belge";try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int ix=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(ix>=0)n=c.getString(ix);}}catch(Exception ignored){}return n;}
    private void refreshChat(){String h=LocalChatStore.historyText(this);if(h==null||h.trim().isEmpty())chat.setText("MG-AI: Merhaba. Yaz, konuş veya belge ekle; hangi motorun gerektiğini ben seçeceğim.");else chat.setText(h);}
    @Override protected void onResume(){super.onResume();ensureEngine();}
    @Override protected void onDestroy(){if(engine!=0&&LocalInferenceBridge.nativeAvailable())try{LocalInferenceBridge.destroyEngine(engine);}catch(Throwable ignored){}super.onDestroy();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
