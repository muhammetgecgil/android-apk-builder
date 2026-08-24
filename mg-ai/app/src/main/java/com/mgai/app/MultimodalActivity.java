package com.mgai.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

public class MultimodalActivity extends Activity {
    private static final int REQ_CAMERA = 4101;
    private static final int REQ_AUDIO_PERMISSION = 4102;
    private EditText endpoint, instruction;
    private CheckBox ocr;
    private TextView out;
    private MediaRecorder recorder;
    private File audioFile;
    private boolean recording = false;
    private Button audioButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(244,246,248));

        TextView title = new TextView(this); title.setText("Görsel & Ses Analizi"); title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD); root.addView(title);
        TextView info = new TextView(this);
        info.setText("v0.10: kamera ve mikrofon verisi provenance ile alınır; yapılandırılmış vision/audio/OCR adaptörüne gönderilir. Model yoksa sahte analiz yapılmaz.");
        info.setTextSize(14); info.setPadding(0, dp(12), 0, dp(16)); root.addView(info);

        endpoint = new EditText(this); endpoint.setHint("Multimodal API endpoint"); endpoint.setSingleLine(true); root.addView(endpoint);
        instruction = new EditText(this); instruction.setHint("Analiz talimatı (örn. parçaları, yazıları ve konumları incele)"); root.addView(instruction);
        ocr = new CheckBox(this); ocr.setText("Görüntüde OCR da çalıştır"); ocr.setChecked(true); root.addView(ocr);

        Button health = button("Multimodal Durum / Model Adaptörleri"); root.addView(health);
        Button camera = button("Kamera ile Fotoğraf Çek ve Analiz Et"); root.addView(camera);
        audioButton = button("Mikrofon Kaydını Başlat"); root.addView(audioButton);
        out = new TextView(this); out.setText("Hazır."); out.setPadding(0, dp(14), 0, 0); root.addView(out);

        health.setOnClickListener(v -> { String base=base(); if(base==null)return; out.setText("Bağlantı testi..."); MultimodalClient.health(base, callback()); });
        camera.setOnClickListener(v -> {
            if (base() == null) return;
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (i.resolveActivity(getPackageManager()) != null) startActivityForResult(i, REQ_CAMERA);
            else out.setText("Kamera uygulaması bulunamadı.");
        });
        audioButton.setOnClickListener(v -> {
            if (recording) stopAudioAndAnalyze();
            else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO_PERMISSION);
            else startAudio();
        });
        setContentView(root);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK && data != null && data.getExtras() != null) {
            Object obj = data.getExtras().get("data");
            if (!(obj instanceof Bitmap)) { out.setText("Kamera görüntüsü alınamadı."); return; }
            Bitmap bmp = (Bitmap)obj;
            ByteArrayOutputStream bos = new ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            long ts = System.currentTimeMillis(); out.setText("Görüntü analiz ediliyor...");
            MultimodalClient.analyze(base(), "image", "android-camera", "image/jpeg", b64, ts,
                    instruction.getText().toString().trim(), ocr.isChecked(), callback());
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startAudio();
            else out.setText("Mikrofon izni verilmedi.");
        }
    }

    private void startAudio() {
        try {
            audioFile = new File(getCacheDir(), "mg_audio_"+System.currentTimeMillis()+".m4a");
            recorder = new MediaRecorder(); recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100); recorder.setAudioEncodingBitRate(128000);
            recorder.setOutputFile(audioFile.getAbsolutePath()); recorder.prepare(); recorder.start();
            recording = true; audioButton.setText("Kaydı Durdur ve Analiz Et"); out.setText("Mikrofon kaydı devam ediyor...");
        } catch (Exception e) { recording=false; out.setText("Ses kayıt hatası: "+e.getMessage()); releaseRecorder(); }
    }

    private void stopAudioAndAnalyze() {
        try {
            recorder.stop(); releaseRecorder(); recording=false; audioButton.setText("Mikrofon Kaydını Başlat");
            byte[] bytes = Files.readAllBytes(audioFile.toPath()); String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            long ts=System.currentTimeMillis(); out.setText("Ses analiz ediliyor...");
            MultimodalClient.analyze(base(), "audio", "android-microphone", "audio/mp4", b64, ts,
                    instruction.getText().toString().trim(), false, callback());
        } catch (Exception e) { out.setText("Ses analiz hatası: "+e.getMessage()); releaseRecorder(); recording=false; }
    }

    private void releaseRecorder() { if (recorder != null) { try { recorder.release(); } catch(Exception ignored){} recorder=null; } }
    @Override protected void onDestroy(){ if(recording){ try{recorder.stop();}catch(Exception ignored){} } releaseRecorder(); super.onDestroy(); }
    private String base(){ String b=endpoint.getText().toString().trim(); if(b.isEmpty()){ out.setText("Endpoint gir."); return null;} return b; }
    private MultimodalClient.Callback callback(){ return new MultimodalClient.Callback(){
        public void onSuccess(String value){ runOnUiThread(() -> out.setText(value)); }
        public void onError(String error){ runOnUiThread(() -> out.setText("Hata: "+error)); }
    };}
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private int dp(int v){ return Math.round(v * getResources().getDisplayMetrics().density); }
}
