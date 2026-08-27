package com.mgecgil.seslirehber;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
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
import com.mgecgil.seslirehber.core.AnnouncementGate;
import com.mgecgil.seslirehber.core.ArCoreDepthCapability;
import com.mgecgil.seslirehber.core.GuidanceModels.GroundObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.GuidanceDecision;
import com.mgecgil.seslirehber.core.GuidanceModels.MotionObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.ObjectObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.Risk;
import com.mgecgil.seslirehber.core.GuidanceSpeaker;
import com.mgecgil.seslirehber.core.OfflineIntentParser;
import com.mgecgil.seslirehber.core.SafetyGate;
import com.mgecgil.seslirehber.core.SensorFusionManager;
import com.mgecgil.seslirehber.core.VisionFusionAnalyzer;
import com.mgecgil.seslirehber.core.VoiceCommandController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends ComponentActivity {
    private PreviewView previewView;
    private TextView statusView;
    private TextView depthStatusView;
    private GuidanceSpeaker speaker;
    private VoiceCommandController voice;
    private SensorFusionManager sensors;
    private VisionFusionAnalyzer visionAnalyzer;
    private ExecutorService cameraExecutor;

    private final SafetyGate safetyGate = new SafetyGate();
    private final AnnouncementGate announcementGate = new AnnouncementGate();
    private final OfflineIntentParser intentParser = new OfflineIntentParser();

    private volatile boolean guidanceEnabled = true;
    private volatile ArCoreDepthCapability.Result depthCapability;
    private long lastVisionErrorMs;

    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean camera = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                if (camera) {
                    startCamera();
                } else {
                    guidanceEnabled = false;
                    updateStatus("Kamera izni olmadan çevre algısı çalışamaz.", true);
                    if (speaker != null) speaker.speak("Kamera izni gerekli. Rehberlik durduruldu.");
                }
            });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        speaker = new GuidanceSpeaker(this);
        sensors = new SensorFusionManager(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        voice = new VoiceCommandController(this, new VoiceCommandController.Listener() {
            @Override public void onVoiceText(String text) { handleVoice(text); }
            @Override public void onVoiceError(String message) {
                updateStatus(message, false);
                speaker.speak(message);
            }
        });

        setContentView(buildUi());
        probeDepthCapability();
        requestPermissionsAndStart();
        speaker.speak("Sesli Rehber sürüm sıfır nokta beş. Mevcut güvenli kamera rehberliği çalışırken ARCore derinlik yeteneği ayrıca doğrulanıyor. Canlı derinlik kararına geçmeden önce kamera sahipliği güvenli şekilde değiştirilecek.");
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        statusView = new TextView(this);
        statusView.setText("Başlatılıyor…");
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(20f);
        statusView.setGravity(Gravity.CENTER_VERTICAL);
        statusView.setMinHeight(dp(70));
        statusView.setContentDescription("Rehberlik durum bilgisi");
        root.addView(statusView, new LinearLayout.LayoutParams(-1, -2));

        depthStatusView = new TextView(this);
        depthStatusView.setText("Derinlik: kontrol ediliyor…");
        depthStatusView.setTextColor(Color.LTGRAY);
        depthStatusView.setTextSize(16f);
        depthStatusView.setMinHeight(dp(44));
        depthStatusView.setContentDescription("Derinlik sistemi durumu");
        root.addView(depthStatusView, new LinearLayout.LayoutParams(-1, -2));

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setContentDescription("Canlı kamera görüntüsü. Kullanmak için görsel etkileşim gerekmez.");
        root.addView(previewView, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button voiceButton = bigButton("Sesli Komut", "Bir kez sesli komut dinle");
        voiceButton.setOnClickListener(v -> voice.listenOnce());
        root.addView(voiceButton);

        Button toggleButton = bigButton("Rehberliği Durdur", "Çevre rehberliğini aç veya kapat");
        toggleButton.setOnClickListener(v -> {
            guidanceEnabled = !guidanceEnabled;
            announcementGate.reset();
            toggleButton.setText(guidanceEnabled ? "Rehberliği Durdur" : "Rehberliği Başlat");
            String message = guidanceEnabled ? "Çevre rehberliği açık." : "Çevre rehberliği kapalı.";
            updateStatus(message, false);
            speaker.speak(message);
        });
        root.addView(toggleButton);

        Button stopButton = bigButton("ACİL DUR", "Rehberliği hemen durdur ve güvenlik uyarısı ver");
        stopButton.setOnClickListener(v -> {
            guidanceEnabled = false;
            GuidanceDecision decision = new GuidanceDecision(
                    Risk.STOP,
                    com.mgecgil.seslirehber.core.GuidanceModels.Direction.UNKNOWN,
                    "Dur. Bastonla çevreni doğrula.",
                    1f);
            speaker.announce(decision);
            updateStatus(decision.speech(), true);
        });
        root.addView(stopButton);
        return root;
    }

    private Button bigButton(String text, String description) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(20f);
        button.setAllCaps(false);
        button.setMinHeight(dp(64));
        button.setMinWidth(dp(64));
        button.setContentDescription(description);
        button.setFocusable(true);
        return button;
    }

    private void probeDepthCapability() {
        ArCoreDepthCapability.probe(this, result -> {
            depthCapability = result;
            runOnUiThread(() -> {
                if (depthStatusView != null) {
                    depthStatusView.setText("Derinlik: " + result.status());
                    depthStatusView.setContentDescription("Derinlik sistemi: " + result.status());
                }
            });
        });
    }

    private void requestPermissionsAndStart() {
        boolean cameraOk = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean audioOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        if (cameraOk && audioOk) {
            startCamera();
        } else {
            permissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                int targetRotation = previewView.getDisplay() != null
                        ? previewView.getDisplay().getRotation()
                        : Surface.ROTATION_0;

                Preview preview = new Preview.Builder()
                        .setTargetRotation(targetRotation)
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetRotation(targetRotation)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                if (visionAnalyzer != null) visionAnalyzer.close();
                visionAnalyzer = new VisionFusionAnalyzer(new VisionFusionAnalyzer.Listener() {
                    @Override public void onMotion(MotionObservation observation) {
                        if (!guidanceEnabled) return;
                        handleDecision(safetyGate.evaluate(observation, sensors.stability()));
                    }

                    @Override public void onObject(ObjectObservation observation) {
                        if (!guidanceEnabled) return;
                        handleDecision(safetyGate.evaluateObject(observation, sensors.stability()));
                    }

                    @Override public void onGround(GroundObservation observation) {
                        if (!guidanceEnabled) return;
                        handleDecision(safetyGate.evaluateGround(observation, sensors.stability()));
                    }

                    @Override public void onVisionError(String message) {
                        long now = System.currentTimeMillis();
                        if (now - lastVisionErrorMs > 8000L) {
                            lastVisionErrorMs = now;
                            updateStatus(message, false);
                        }
                    }
                });
                analysis.setAnalyzer(cameraExecutor, visionAnalyzer);

                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
                updateStatus("Kamera aktif. Nesne yaklaşma + çarpışma koridoru + zemin sürekliliği çalışıyor.", false);
            } catch (Exception error) {
                guidanceEnabled = false;
                updateStatus("Kamera başlatılamadı: " + error.getClass().getSimpleName(), true);
                speaker.speak("Kamera başlatılamadı. Rehberlik kapatıldı.");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void handleDecision(GuidanceDecision decision) {
        long now = System.currentTimeMillis();
        if (!announcementGate.shouldAnnounce(decision, now)) return;

        runOnUiThread(() -> {
            boolean urgent = decision.risk() == Risk.STOP;
            updateStatus(
                    decision.speech() + "  Güven: " + Math.round(decision.confidence() * 100) + "%",
                    urgent);
            speaker.announce(decision);
        });
    }

    private void handleVoice(String text) {
        OfflineIntentParser.ParsedIntent parsed = intentParser.parse(text);
        switch (parsed.intent()) {
            case START_GUIDANCE -> {
                guidanceEnabled = true;
                announcementGate.reset();
                speaker.speak("Rehberlik açık.");
            }
            case STOP_GUIDANCE -> {
                guidanceEnabled = false;
                speaker.speak("Rehberlik durduruldu.");
            }
            case REPEAT -> speaker.repeat();
            case DESCRIBE_SCENE -> {
                String depthText;
                ArCoreDepthCapability.Result result = depthCapability;
                if (result != null && result.depthSupported()) {
                    depthText = " Bu cihazda ARCore derinlik desteği doğrulandı; canlı güvenlik füzyonu için kamera geçiş katmanı hazırlanıyor.";
                } else {
                    depthText = " Derinlik desteği canlı rehberliğe bağlı değil; mevcut kamera güvenlik kanalları çalışıyor.";
                }
                speaker.speak("Nesne yaklaşma ve ön zemin sürekliliği izleniyor." + depthText);
            }
            case HELP -> speaker.speak(
                    "Komutlar: rehberliği başlat, rehberliği durdur, tekrar et, çevremi anlat.");
            case UNKNOWN -> speaker.speak("Komutu anlayamadım.");
        }
        updateStatus("Komut: " + text, false);
    }

    private void updateStatus(String text, boolean urgent) {
        runOnUiThread(() -> {
            statusView.setText(text);
            statusView.setTextColor(urgent ? Color.rgb(255, 210, 80) : Color.WHITE);
            statusView.announceForAccessibility(text);
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sensors != null) sensors.start();
    }

    @Override
    protected void onStop() {
        if (sensors != null) sensors.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (visionAnalyzer != null) visionAnalyzer.close();
        if (voice != null) voice.destroy();
        if (speaker != null) speaker.shutdown();
        if (cameraExecutor != null) cameraExecutor.shutdownNow();
        super.onDestroy();
    }
}
