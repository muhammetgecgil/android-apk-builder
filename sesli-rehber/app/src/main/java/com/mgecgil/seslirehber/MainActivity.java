package com.mgecgil.seslirehber;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.mgecgil.seslirehber.core.GuidanceModels.GuidanceDecision;
import com.mgecgil.seslirehber.core.GuidanceSpeaker;
import com.mgecgil.seslirehber.core.OfflineIntentParser;
import com.mgecgil.seslirehber.core.SafetyGate;
import com.mgecgil.seslirehber.core.SensorFusionManager;
import com.mgecgil.seslirehber.core.VisionMotionAnalyzer;
import com.mgecgil.seslirehber.core.VoiceCommandController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends ComponentActivity {
    private PreviewView previewView; private TextView statusView; private GuidanceSpeaker speaker; private VoiceCommandController voice; private SensorFusionManager sensors; private final SafetyGate safetyGate=new SafetyGate(); private final OfflineIntentParser intentParser=new OfflineIntentParser(); private ExecutorService cameraExecutor; private volatile boolean guidanceEnabled=true; private long lastAnnouncementMs;
    private final ActivityResultLauncher<String[]> permissions=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),result->{boolean camera=Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));if(camera)startCamera();else updateStatus("Kamera izni olmadan çevre algısı çalışamaz.",true);});
    @Override protected void onCreate(Bundle state){super.onCreate(state);speaker=new GuidanceSpeaker(this);sensors=new SensorFusionManager(this);cameraExecutor=Executors.newSingleThreadExecutor();voice=new VoiceCommandController(this,new VoiceCommandController.Listener(){@Override public void onVoiceText(String text){handleVoice(text);}@Override public void onVoiceError(String message){updateStatus(message,false);speaker.speak(message);}});setContentView(buildUi());requestPermissionsAndStart();speaker.speak("Sesli Rehber açıldı. Bu ilk sürüm hareket algılar; sabit engeller için bastonunuzu kullanmaya devam edin.");}
    private View buildUi(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);root.setPadding(dp(12),dp(12),dp(12),dp(12));statusView=new TextView(this);statusView.setText("Başlatılıyor…");statusView.setTextColor(Color.WHITE);statusView.setTextSize(20f);statusView.setGravity(Gravity.CENTER_VERTICAL);statusView.setMinHeight(dp(70));statusView.setContentDescription("Durum bilgisi");root.addView(statusView,new LinearLayout.LayoutParams(-1,-2));previewView=new PreviewView(this);previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);previewView.setContentDescription("Canlı kamera görüntüsü");root.addView(previewView,new LinearLayout.LayoutParams(-1,0,1f));Button voiceButton=bigButton("Sesli Komut","Bir kez sesli komut dinle");voiceButton.setOnClickListener(v->voice.listenOnce());root.addView(voiceButton);Button toggleButton=bigButton("Rehberliği Durdur","Hareket uyarılarını aç veya kapat");toggleButton.setOnClickListener(v->{guidanceEnabled=!guidanceEnabled;toggleButton.setText(guidanceEnabled?"Rehberliği Durdur":"Rehberliği Başlat");String msg=guidanceEnabled?"Hareket uyarıları açık.":"Hareket uyarıları kapalı.";updateStatus(msg,false);speaker.speak(msg);});root.addView(toggleButton);Button stopButton=bigButton("ACİL DUR","Acil dur uyarısı");stopButton.setOnClickListener(v->{guidanceEnabled=false;GuidanceDecision decision=new GuidanceDecision(com.mgecgil.seslirehber.core.GuidanceModels.Risk.STOP,com.mgecgil.seslirehber.core.GuidanceModels.Direction.UNKNOWN,"Dur. Bastonla çevreni doğrula.",1f);speaker.announce(decision);updateStatus(decision.speech(),true);});root.addView(stopButton);return root;}
    private Button bigButton(String text,String description){Button b=new Button(this);b.setText(text);b.setTextSize(20f);b.setAllCaps(false);b.setMinHeight(dp(64));b.setContentDescription(description);b.setFocusable(true);return b;}
    private void requestPermissionsAndStart(){boolean cameraOk=ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED;boolean audioOk=ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;if(cameraOk&&audioOk)startCamera();else permissions.launch(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO});}
    private void startCamera(){ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);future.addListener(()->{try{ProcessCameraProvider provider=future.get();Preview preview=new Preview.Builder().build();preview.setSurfaceProvider(previewView.getSurfaceProvider());ImageAnalysis analysis=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();analysis.setAnalyzer(cameraExecutor,new VisionMotionAnalyzer(observation->{if(!guidanceEnabled)return;GuidanceDecision decision=safetyGate.evaluate(observation,sensors.stability());long now=System.currentTimeMillis();long cooldown=decision.risk()==com.mgecgil.seslirehber.core.GuidanceModels.Risk.STOP?1300:2200;if(!decision.speech().isBlank()&&now-lastAnnouncementMs>cooldown){lastAnnouncementMs=now;runOnUiThread(()->{updateStatus(decision.speech()+"  Güven: "+Math.round(decision.confidence()*100)+"%",decision.risk()==com.mgecgil.seslirehber.core.GuidanceModels.Risk.STOP);speaker.announce(decision);});}}));provider.unbindAll();provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis);updateStatus("Kamera aktif. Hareket katmanı çalışıyor.",false);}catch(Exception e){updateStatus("Kamera başlatılamadı: "+e.getClass().getSimpleName(),true);speaker.speak("Kamera başlatılamadı. Rehberlik kapatıldı.");guidanceEnabled=false;}},ContextCompat.getMainExecutor(this));}
    private void handleVoice(String text){OfflineIntentParser.ParsedIntent parsed=intentParser.parse(text);switch(parsed.intent()){case START_GUIDANCE->{guidanceEnabled=true;speaker.speak("Rehberlik açık.");}case STOP_GUIDANCE->{guidanceEnabled=false;speaker.speak("Rehberlik durduruldu.");}case REPEAT->speaker.repeat();case DESCRIBE_SCENE->speaker.speak("Sahne anlatımı için nesne ve derinlik modeli henüz bağlanmadı. Hareket katmanı çalışıyor.");case HELP->speaker.speak("Komutlar: rehberliği başlat, rehberliği durdur, tekrar et, çevremi anlat.");case UNKNOWN->speaker.speak("Komutu anlayamadım.");}updateStatus("Komut: "+text,false);}
    private void updateStatus(String text,boolean urgent){runOnUiThread(()->{statusView.setText(text);statusView.setTextColor(urgent?Color.rgb(255,210,80):Color.WHITE);statusView.announceForAccessibility(text);});}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onStart(){super.onStart();if(sensors!=null)sensors.start();}
    @Override protected void onStop(){if(sensors!=null)sensors.stop();super.onStop();}
    @Override protected void onDestroy(){if(voice!=null)voice.destroy();if(speaker!=null)speaker.shutdown();if(cameraExecutor!=null)cameraExecutor.shutdownNow();super.onDestroy();}
}
