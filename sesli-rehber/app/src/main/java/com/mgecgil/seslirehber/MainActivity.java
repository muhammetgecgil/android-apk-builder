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
import com.mgecgil.seslirehber.core.ArCoreLiveVisionEngine;
import com.mgecgil.seslirehber.core.GroundDepthSynchronizer;
import com.mgecgil.seslirehber.core.GuidanceModels.DepthObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.GroundDepthEvidence;
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
    private enum VisionMode { STARTING, CAMERAX, ARCORE }

    private PreviewView previewView;
    private TextView statusView;
    private TextView depthStatusView;
    private GuidanceSpeaker speaker;
    private VoiceCommandController voice;
    private SensorFusionManager sensors;
    private VisionFusionAnalyzer visionAnalyzer;
    private ArCoreLiveVisionEngine arCoreEngine;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;

    private final SafetyGate safetyGate = new SafetyGate();
    private final AnnouncementGate announcementGate = new AnnouncementGate();
    private final OfflineIntentParser intentParser = new OfflineIntentParser();
    private final GroundDepthSynchronizer groundDepthSynchronizer = new GroundDepthSynchronizer();

    private volatile boolean guidanceEnabled = true;
    private volatile ArCoreDepthCapability.Result depthCapability;
    private volatile VisionMode visionMode = VisionMode.STARTING;
    private volatile boolean arCoreSwitchAttempted;
    private volatile boolean destroyed;
    private long lastVisionErrorMs;

    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean camera = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                if (camera) {
                    startCameraX();
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
        speaker.speak("Sesli Rehber sürüm sıfır nokta altı. Güvenli kamera rehberliği başlıyor; destek varsa canlı ARCore derinlik moduna geçilecek ve sorun olursa CameraX geri dönüşü yapılacak.");
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
        previewView.setContentDescription("Canlı kamera görüntüsü. ARCore derinlik modunda görsel önizleme enerji tasarrufu için kapatılabilir.");
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
                if (destroyed) return;
                depthStatusView.setText("Derinlik: " + result.status());
                depthStatusView.setContentDescription("Derinlik sistemi: " + result.status());
                maybeSwitchToArCore();
            });
        });
    }

    private void requestPermissionsAndStart() {
        boolean cameraOk = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean audioOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        if (cameraOk && audioOk) {
            startCameraX();
        } else {
            permissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        }
    }

    private boolean cameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCameraX() {
        if (destroyed || !cameraPermissionGranted()) return;
        visionMode = VisionMode.STARTING;
        previewView.setVisibility(View.VISIBLE);
        groundDepthSynchronizer.reset();

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                if (destroyed || visionMode == VisionMode.ARCORE) return;
                ProcessCameraProvider provider = future.get();
                cameraProvider = provider;
                int targetRotation = currentDisplayRotation();

                Preview preview = new Preview.Builder()
                        .setTargetRotation(targetRotation)
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetRotation(targetRotation)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                closeCameraXAnalyzer();
                visionAnalyzer = new VisionFusionAnalyzer(new VisionFusionAnalyzer.Listener() {
                    @Override public void onMotion(MotionObservation observation) { handleMotion(observation); }
                    @Override public void onObject(ObjectObservation observation) { handleObject(observation); }
                    @Override public void onGround(GroundObservation observation) { handleGround(observation); }
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
                visionMode = VisionMode.CAMERAX;
                updateStatus("CameraX güvenli mod aktif. Nesne + çarpışma koridoru + zemin sürekliliği çalışıyor.", false);
                maybeSwitchToArCore();
            } catch (Exception error) {
                guidanceEnabled = false;
                updateStatus("Kamera başlatılamadı: " + error.getClass().getSimpleName(), true);
                speaker.speak("Kamera başlatılamadı. Rehberlik kapatıldı.");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void maybeSwitchToArCore() {
        if (destroyed || arCoreSwitchAttempted || visionMode != VisionMode.CAMERAX || !cameraPermissionGranted()) return;
        ArCoreDepthCapability.Result capability = depthCapability;
        if (capability == null || !capability.depthSupported() || !capability.hardwareBufferModeSupported()) return;

        arCoreSwitchAttempted = true;
        visionMode = VisionMode.STARTING;
        groundDepthSynchronizer.reset();
        if (cameraProvider != null) cameraProvider.unbindAll();
        closeCameraXAnalyzer();
        updateStatus("Canlı ARCore Depth16 moduna geçiliyor…", false);
        depthStatusView.setText("Derinlik: kamera ARCore'a devrediliyor…");

        // CameraX unbind is synchronous at the use-case layer but the camera device can need a short
        // release interval before ARCore Session.resume(). A small deterministic handoff reduces
        // spurious CameraNotAvailable fallbacks without hiding a persistent failure.
        previewView.postDelayed(() -> startArCore(), 220L);
    }

    private void startArCore() {
        if (destroyed) return;
        if (arCoreEngine != null) arCoreEngine.close();
        arCoreEngine = new ArCoreLiveVisionEngine(this, new ArCoreLiveVisionEngine.Listener() {
            @Override public void onMotion(MotionObservation observation) { handleMotion(observation); }
            @Override public void onObject(ObjectObservation observation) { handleObject(observation); }
            @Override public void onGround(GroundObservation observation) { handleGround(observation); }
            @Override public void onDepth(DepthObservation observation) { handleDepth(observation); }

            @Override public void onStatus(String status) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    visionMode = VisionMode.ARCORE;
                    previewView.setVisibility(View.GONE);
                    depthStatusView.setText("Derinlik: canlı Depth16 aktif");
                    depthStatusView.setContentDescription("Derinlik sistemi: canlı Depth16 aktif");
                    updateStatus(status, false);
                });
            }

            @Override public void onFatal(String message) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    visionMode = VisionMode.STARTING;
                    groundDepthSynchronizer.reset();
                    if (arCoreEngine != null) {
                        arCoreEngine.close();
                        arCoreEngine = null;
                    }
                    previewView.setVisibility(View.VISIBLE);
                    depthStatusView.setText("Derinlik: canlı mod kapandı; CameraX yedek aktif");
                    updateStatus(message, true);
                    speaker.speak("Derinlik modu kapandı. Kamera rehberliği devam ediyor.");
                    startCameraX();
                });
            }
        });
        if (!arCoreEngine.start(currentDisplayRotation())) {
            previewView.setVisibility(View.VISIBLE);
            startCameraX();
        }
    }

    private void handleMotion(MotionObservation observation) {
        if (!guidanceEnabled) return;
        handleDecision(safetyGate.evaluate(observation, sensors.stability()));
    }

    private void handleObject(ObjectObservation observation) {
        if (!guidanceEnabled) return;
        handleDecision(safetyGate.evaluateObject(observation, sensors.stability()));
    }

    private void handleGround(GroundObservation observation) {
        if (!guidanceEnabled) return;
        handleDecision(safetyGate.evaluateGround(observation, sensors.stability()));
        GroundDepthEvidence evidence = groundDepthSynchronizer.offerGround(observation);
        if (evidence != null) {
            handleDecision(safetyGate.evaluateGroundWithDepth(
                    evidence.ground(), evidence.depth(), sensors.stability()));
        }
    }

    private void handleDepth(DepthObservation observation) {
        if (!guidanceEnabled) return;
        handleDecision(safetyGate.evaluateDepth(observation, sensors.stability()));
        GroundDepthEvidence evidence = groundDepthSynchronizer.offerDepth(observation);
        if (evidence != null) {
            handleDecision(safetyGate.evaluateGroundWithDepth(
                    evidence.ground(), evidence.depth(), sensors.stability()));
        }
    }

    private void handleDecision(GuidanceDecision decision) {
        long now = System.currentTimeMillis();
        if (!announcementGate.shouldAnnounce(decision, now)) return;
        runOnUiThread(() -> {
            boolean urgent = decision.risk() == Risk.STOP;
            updateStatus(decision.speech() + "  Güven: " + Math.round(decision.confidence() * 100) + "%", urgent);
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
                String modeText = switch (visionMode) {
                    case ARCORE -> " Canlı ARCore derinlik, zemin, nesne ve hareket kanalları birlikte çalışıyor.";
                    case CAMERAX -> " CameraX zemin, nesne ve hareket kanalları çalışıyor; canlı derinlik aktif değil.";
                    default -> " Görüş sistemi başlatılıyor.";
                };
                speaker.speak("Ön çevre güvenlik kanalları izleniyor." + modeText
                        + " Çukur veya kaldırım adı saha doğrulaması olmadan kesin söylenmez.");
            }
            case HELP -> speaker.speak("Komutlar: rehberliği başlat, rehberliği durdur, tekrar et, çevremi anlat.");
            case UNKNOWN -> speaker.speak("Komutu anlayamadım.");
        }
        updateStatus("Komut: " + text, false);
    }

    private void updateStatus(String text, boolean urgent) {
        runOnUiThread(() -> {
            if (destroyed || statusView == null) return;
            statusView.setText(text);
            statusView.setTextColor(urgent ? Color.rgb(255, 210, 80) : Color.WHITE);
            statusView.announceForAccessibility(text);
        });
    }

    private int currentDisplayRotation() {
        return previewView != null && previewView.getDisplay() != null
                ? previewView.getDisplay().getRotation()
                : Surface.ROTATION_0;
    }

    private void closeCameraXAnalyzer() {
        if (visionAnalyzer != null) {
            visionAnalyzer.close();
            visionAnalyzer = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sensors != null) sensors.start();
        if (visionMode == VisionMode.ARCORE && arCoreEngine != null && !arCoreEngine.isRunning()) {
            arCoreEngine.start(currentDisplayRotation());
        }
    }

    @Override
    protected void onStop() {
        if (arCoreEngine != null) arCoreEngine.stop();
        if (sensors != null) sensors.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (cameraProvider != null) cameraProvider.unbindAll();
        closeCameraXAnalyzer();
        if (arCoreEngine != null) {
            arCoreEngine.close();
            arCoreEngine = null;
        }
        if (voice != null) voice.destroy();
        if (speaker != null) speaker.shutdown();
        if (cameraExecutor != null) cameraExecutor.shutdownNow();
        super.onDestroy();
    }
}
