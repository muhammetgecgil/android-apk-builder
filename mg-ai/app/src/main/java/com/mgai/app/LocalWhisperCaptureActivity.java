package com.mgai.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class LocalWhisperCaptureActivity extends Activity {
    private static final int REQ_AUDIO=7001;
    private TextView status;
    private ProgressBar progress;
    private Button action;
    private LocalAudioRecorder recorder;
    private boolean recording=false;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(28),dp(22),dp(22));root.setBackgroundColor(Color.rgb(244,246,248));
        TextView title=new TextView(this);title.setText("MG-AI Yerel Ses");title.setTextSize(26);title.setTextColor(Color.rgb(20,24,32));root.addView(title);
        status=new TextView(this);status.setText("Whisper hazırlanıyor…");status.setTextSize(15);status.setPadding(0,dp(18),0,dp(12));root.addView(status);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(18)));
        action=new Button(this);action.setText("Konuşmaya Başla");action.setAllCaps(false);action.setOnClickListener(v->{if(recording)stopAndTranscribe();else prepareAndStart();});root.addView(action);
        setContentView(root);
    }

    private void prepareAndStart(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}
        if(VoiceSessionStateManager.is(VoiceSessionStateManager.State.TRANSCRIBING)||VoiceSessionStateManager.is(VoiceSessionStateManager.State.THINKING)){
            status.setText("Ses oturumu meşgul: "+VoiceSessionStateManager.summary());return;
        }
        action.setEnabled(false);status.setText(LocalWhisperModelManager.isReady(this)?"Whisper modeli hazır.":"Whisper modeli ilk kullanım için indiriliyor…");
        new Thread(()->{try{
            LocalWhisperModelManager.ensure(this,(done,total)->runOnUiThread(()->{int pct=total>0?(int)Math.min(100,done*100/total):0;progress.setProgress(pct);status.setText("Whisper modeli hazırlanıyor: %"+pct); }));
            runOnUiThread(this::startRecording);
        }catch(Exception e){VoiceSessionStateManager.reset();runOnUiThread(()->{action.setEnabled(true);status.setText("Whisper model hatası: "+e.getMessage());});}}).start();
    }

    private void startRecording(){
        try{
            VoiceSessionStateManager.set(VoiceSessionStateManager.State.LISTENING);
            recorder=new LocalAudioRecorder();recorder.start();recording=true;progress.setProgress(100);status.setText("Dinliyorum… Bitince düğmeye tekrar bas.");action.setText("Durdur ve Sor");action.setEnabled(true);
        }catch(Exception e){VoiceSessionStateManager.reset();action.setEnabled(true);status.setText("Mikrofon başlatılamadı: "+e.getMessage());}
    }

    private void stopAndTranscribe(){
        action.setEnabled(false);status.setText("Ses telefonda Whisper ile yazıya çevriliyor…");recording=false;VoiceSessionStateManager.set(VoiceSessionStateManager.State.TRANSCRIBING);
        new Thread(()->{long handle=0;File wav=new File(getCacheDir(),"mg_voice.wav");try{
            recorder.stopToWav(wav);
            if(!WhisperInferenceBridge.nativeAvailable())throw new IllegalStateException("libmgwhisper_yuklenemedi");
            File model=LocalWhisperModelManager.ensure(this,null);
            handle=WhisperInferenceBridge.create(model.getAbsolutePath());
            if(handle==0)throw new IllegalStateException("whisper_model_acilamadi");
            String text=WhisperInferenceBridge.transcribe(handle,wav.getAbsolutePath(),"tr");
            if(text==null||text.trim().isEmpty())throw new IllegalStateException("konusma_algilanamadi");
            ArrayList<String> out=new ArrayList<>();out.add(text.trim());
            Intent data=new Intent();data.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,out);
            VoiceSessionStateManager.set(VoiceSessionStateManager.State.IDLE);
            setResult(RESULT_OK,data);runOnUiThread(this::finish);
        }catch(Exception e){VoiceSessionStateManager.reset();runOnUiThread(()->{action.setEnabled(true);action.setText("Tekrar Dene");status.setText("Yerel ses tanıma hatası: "+e.getMessage());});}
        finally{if(handle!=0)try{WhisperInferenceBridge.destroy(handle);}catch(Throwable ignored){}if(wav.exists())wav.delete();}}).start();
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_AUDIO&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)prepareAndStart();else status.setText("Mikrofon izni gerekli.");}
    @Override protected void onDestroy(){if(recording&&recorder!=null){try{recorder.stopToWav(new File(getCacheDir(),"discard.wav"));}catch(Exception ignored){}}if(VoiceSessionStateManager.is(VoiceSessionStateManager.State.LISTENING)||VoiceSessionStateManager.is(VoiceSessionStateManager.State.TRANSCRIBING))VoiceSessionStateManager.reset();super.onDestroy();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
