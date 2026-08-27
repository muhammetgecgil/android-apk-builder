package com.mgecgil.seslirehber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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
import androidx.core.content.FileProvider;
import com.google.common.util.concurrent.ListenableFuture;
import com.mgecgil.seslirehber.core.AnnouncementGate;
import com.mgecgil.seslirehber.core.ArCoreDepthCapability;
import com.mgecgil.seslirehber.core.ArCoreLiveVisionEngine;
import com.mgecgil.seslirehber.core.GateCRecorder;
import com.mgecgil.seslirehber.core.GroundDepthSynchronizer;
import com.mgecgil.seslirehber.core.GuidanceModels.DepthObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.GroundDepthEvidence;
import com.mgecgil.seslirehber.core.GuidanceModels.GroundObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.GuidanceDecision;
import com.mgecgil.seslirehber.core.GuidanceModels.MotionObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.ObjectObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.Risk;
import com.mgecgil.seslirehber.core.GuidanceModels.SceneHealthObservation;
import com.mgecgil.seslirehber.core.GuidanceModels.WalkableCorridorObservation;
import com.mgecgil.seslirehber.core.GuidancePriorityArbiter;
import com.mgecgil.seslirehber.core.GuidanceSpeaker;
import com.mgecgil.seslirehber.core.OfflineIntentParser;
import com.mgecgil.seslirehber.core.SafetyGate;
import com.mgecgil.seslirehber.core.SceneSummaryState;
import com.mgecgil.seslirehber.core.SensorFusionManager;
import com.mgecgil.seslirehber.core.VisionFusionAnalyzer;
import com.mgecgil.seslirehber.core.VisionHealthWatchdog;
import com.mgecgil.seslirehber.core.VoiceCommandController;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends ComponentActivity {
    private enum VisionMode { STARTING, CAMERAX, ARCORE }
    private enum PendingAudioAction { NONE, ONE_SHOT, HANDS_FREE }

    private PreviewView previewView;
    private TextView statusView;
    private TextView depthStatusView;
    private TextView voiceStatusView;
    private TextView gateCStatusView;
    private Button wakeButton;
    private Button gateCButton;
    private Button shareReportButton;
    private GuidanceSpeaker speaker;
    private VoiceCommandController voice;
    private SensorFusionManager sensors;
    private VisionFusionAnalyzer visionAnalyzer;
    private ArCoreLiveVisionEngine arCoreEngine;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;
    private GateCRecorder gateCRecorder;

    private final SafetyGate safetyGate = new SafetyGate();
    private final AnnouncementGate announcementGate = new AnnouncementGate();
    private final GuidancePriorityArbiter priorityArbiter = new GuidancePriorityArbiter();
    private final OfflineIntentParser intentParser = new OfflineIntentParser();
    private final SceneSummaryState sceneSummary = new SceneSummaryState();
    private final GroundDepthSynchronizer groundDepthSynchronizer = new GroundDepthSynchronizer();
    private final VisionHealthWatchdog visionWatchdog = new VisionHealthWatchdog();
    private final Handler healthHandler = new Handler(Looper.getMainLooper());

    private volatile boolean guidanceEnabled = true;
    private volatile ArCoreDepthCapability.Result depthCapability;
    private volatile VisionMode visionMode = VisionMode.STARTING;
    private volatile boolean arCoreSwitchAttempted;
    private volatile boolean destroyed;
    private boolean watchdogRecoveryInProgress;
    private VisionHealthWatchdog.Health lastWatchdogHealth = VisionHealthWatchdog.Health.STARTING;
    private PendingAudioAction pendingAudioAction = PendingAudioAction.NONE;
    private long lastVisionErrorMs;
    private String pendingDestination = "";

    private final Runnable healthTick = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            checkVisionHealth();
            healthHandler.postDelayed(this, 500L);
        }
    };

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    startCameraX();
                } else {
                    guidanceEnabled = false;
                    updateStatus("Kamera izni olmadan çevre algısı çalışamaz.", true);
                    if (speaker != null) speaker.speak("Kamera izni gerekli. Rehberlik durduruldu.");
                }
            });

    private final ActivityResultLauncher<String> audioPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                PendingAudioAction action = pendingAudioAction;
                pendingAudioAction = PendingAudioAction.NONE;
                if (Boolean.TRUE.equals(granted)) {
                    if (action == PendingAudioAction.HANDS_FREE) enableHandsFreeNow();
                    else if (voice != null) voice.listenOnce();
                } else if (speaker != null) {
                    speaker.speak("Sesli komut için mikrofon izni gerekli. Kamera rehberliği çalışmaya devam ediyor.");
                    updateVoiceUi();
                }
            });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        speaker = new GuidanceSpeaker(this);
        sensors = new SensorFusionManager(this);
        gateCRecorder = new GateCRecorder(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        voice = new VoiceCommandController(this, new VoiceCommandController.Listener() {
            @Override public void onVoiceText(String text) { handleVoice(text); }

            @Override public void onVoiceError(String message) {
                updateStatus(message, false);
                speaker.speak(message);
            }

            @Override public void onVoiceState(String message) {
                runOnUiThread(() -> {
                    if (voiceStatusView != null) voiceStatusView.setText("Ses: " + message);
                });
                if ("Dinliyorum.".equals(message)) speaker.speak(message);
            }

            @Override public void onWakeModeChanged(boolean enabled, boolean onDevice) {
                updateVoiceUi();
            }
        });
        voice.setSpeechBusySupplier(speaker::isSpeaking);

        setContentView(buildUi());
        updateVoiceUi();
        probeDepthCapability();
        requestCameraAndStart();
        speaker.speak("Sesli Rehber sürüm sıfır nokta dokuz. Eller serbest ses, yazı okuma ve canlı çevre özeti hazırlanıyor. Kamera güvenliği çalışmaya devam ediyor.");
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

        voiceStatusView = new TextView(this);
        voiceStatusView.setText("Ses: kontrol ediliyor…");
        voiceStatusView.setTextColor(Color.LTGRAY);
        voiceStatusView.setTextSize(16f);
        voiceStatusView.setMinHeight(dp(44));
        voiceStatusView.setContentDescription("Hey Rehber ve ses tanıma durumu");
        root.addView(voiceStatusView, new LinearLayout.LayoutParams(-1, -2));

        gateCStatusView = new TextView(this);
        gateCStatusView.setText("Gate C: kayıt kapalı");
        gateCStatusView.setTextColor(Color.LTGRAY);
        gateCStatusView.setTextSize(15f);
        gateCStatusView.setMinHeight(dp(40));
        gateCStatusView.setContentDescription("Gate C cihaz doğrulama durumu");
        root.addView(gateCStatusView, new LinearLayout.LayoutParams(-1, -2));

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setContentDescription("Canlı kamera görüntüsü. ARCore derinlik modunda görsel önizleme enerji tasarrufu için kapatılabilir.");
        root.addView(previewView, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button voiceButton = bigButton("Sesli Komut", "Bir kez sesli komut dinle");
        voiceButton.setOnClickListener(v -> startVoiceCommand());
        root.addView(voiceButton);

        wakeButton = bigButton("Hey Rehber'i Aç", "Eller serbest Hey Rehber dinlemeyi aç veya kapat");
        wakeButton.setOnClickListener(v -> toggleHandsFree());
        root.addView(wakeButton);

        Button toggleButton = bigButton("Rehberliği Durdur", "Çevre rehberliğini aç veya kapat");
        toggleButton.setOnClickListener(v -> {
            guidanceEnabled = !guidanceEnabled;
            announcementGate.reset();
            priorityArbiter.reset();
            toggleButton.setText(guidanceEnabled ? "Rehberliği Durdur" : "Rehberliği Başlat");
            String message = guidanceEnabled ? "Çevre rehberliği açık." : "Çevre rehberliği kapalı.";
            updateStatus(message, false);
            speaker.speak(message);
        });
        root.addView(toggleButton);

        gateCButton = bigButton("Gate C Testini Başlat", "Cihaz doğrulama ölçüm kaydını başlat veya durdur");
        gateCButton.setOnClickListener(v -> toggleGateCTest());
        root.addView(gateCButton);

        shareReportButton = bigButton("Gate C Raporunu Paylaş", "Son Gate C CSV raporunu paylaş");
        shareReportButton.setEnabled(false);
        shareReportButton.setOnClickListener(v -> shareGateCReport());
        root.addView(shareReportButton);

        Button stopButton = bigButton("ACİL DUR", "Rehberliği hemen durdur ve güvenlik uyarısı ver");
        stopButton.setOnClickListener(v -> {
            guidanceEnabled = false;
            GuidanceDecision decision = new GuidanceDecision(
                    Risk.STOP,
                    com.mgecgil.seslirehber.core.GuidanceModels.Direction.UNKNOWN,
                    "Dur. Bastonla çevreni doğrula.",
                    1f);
            recordAndAnnounceDecision(decision, "MANUAL", System.currentTimeMillis());
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

    private void startVoiceCommand() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            voice.listenOnce();
        } else {
            pendingAudioAction = PendingAudioAction.ONE_SHOT;
            audioPermission.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void toggleHandsFree() {
        if (voice == null) return;
        if (voice.isHandsFreeEnabled()) {
            voice.setHandsFreeEnabled(false);
            updateVoiceUi();
            speaker.speak("Hey Rehber kapalı.");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pendingAudioAction = PendingAudioAction.HANDS_FREE;
            audioPermission.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        enableHandsFreeNow();
    }

    private void enableHandsFreeNow() {
        boolean enabled = voice != null && voice.setHandsFreeEnabled(true);
        updateVoiceUi();
        if (enabled) {
            speaker.speak("Hey Rehber açık. Uygulama ekrandayken yerel ses tanıma ile dinliyorum.");
        } else {
            speaker.speak("Bu cihazda yerel eller serbest ses tanıma hazır değil. Sesli Komut düğmesi kullanılabilir.");
        }
    }

    private void updateVoiceUi() {
        runOnUiThread(() -> {
            if (destroyed || voice == null) return;
            if (voiceStatusView != null) voiceStatusView.setText("Ses: " + voice.modeDescription());
            if (wakeButton != null) {
                wakeButton.setText(voice.isHandsFreeEnabled() ? "Hey Rehber'i Kapat" : "Hey Rehber'i Aç");
            }
        });
    }

    private void toggleGateCTest() {
        if (gateCRecorder.isActive()) {
            String summary = gateCRecorder.stop();
            gateCButton.setText("Gate C Testini Başlat");
            gateCStatusView.setText(summary);
            shareReportButton.setEnabled(gateCRecorder.lastReportFile() != null);
            speaker.speak("Gate C testi durduruldu. " + summary);
            return;
        }

        boolean started = gateCRecorder.start("0.9.0", modeName());
        if (!started) {
            gateCStatusView.setText("Gate C: kayıt dosyası açılamadı");
            speaker.speak("Gate C kayıt dosyası açılamadı.");
            return;
        }
        gateCButton.setText("Gate C Testini Durdur");
        shareReportButton.setEnabled(false);
        gateCStatusView.setText("Gate C: kayıt aktif — kontrollü test alanında kullan");
        gateCRecorder.recordMode(modeName(), "test_started");
        speaker.speak("Gate C cihaz doğrulama kaydı başladı.");
    }

    private void shareGateCReport() {
        if (gateCRecorder.isActive()) {
            String summary = gateCRecorder.stop();
            gateCButton.setText("Gate C Testini Başlat");
            gateCStatusView.setText(summary);
        }
        File file = gateCRecorder.lastReportFile();
        if (file == null) {
            speaker.speak("Paylaşılacak Gate C raporu yok.");
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/csv");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, "Sesli Rehber Gate C cihaz doğrulama raporu");
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Gate C raporunu paylaş"));
        } catch (Throwable error) {
            speaker.speak("Gate C raporu paylaşılamadı.");
        }
    }

    private void probeDepthCapability() {
        ArCoreDepthCapability.probe(this, result -> {
            depthCapability = result;
            runOnUiThread(() -> {
                if (destroyed) return;
                depthStatusView.setText("Derinlik: " + result.status());
                depthStatusView.setContentDescription("Derinlik sistemi: " + result.status());
                if (gateCRecorder.isActive()) gateCRecorder.recordMode(modeName(), "depth_capability=" + result.status());
                maybeSwitchToArCore();
            });
        });
    }

    private void requestCameraAndStart() {
        if (cameraPermissionGranted()) startCameraX();
        else cameraPermission.launch(Manifest.permission.CAMERA);
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
        sceneSummary.reset();
        visionWatchdog.beginMode(SystemClock.elapsedRealtime(), false);
        lastWatchdogHealth = VisionHealthWatchdog.Health.STARTING;
        if (gateCRecorder.isActive()) gateCRecorder.recordMode("STARTING", "CameraX_start_requested");

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
                    @Override public void onSceneHealth(SceneHealthObservation observation) { handleSceneHealth(observation); }
                    @Override public void onTextRecognized(String text) { handleTextRecognized(text); }
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
                watchdogRecoveryInProgress = false;
                visionWatchdog.beginMode(SystemClock.elapsedRealtime(), false);
                if (gateCRecorder.isActive()) gateCRecorder.recordMode("CAMERAX", "camera_bound");
                updateStatus("CameraX güvenli mod aktif. Kamera sağlığı + nesne + çarpışma koridoru + zemin sürekliliği çalışıyor.", false);
                maybeSwitchToArCore();
            } catch (Exception error) {
                guidanceEnabled = false;
                if (gateCRecorder.isActive()) gateCRecorder.recordWatchdog("CAMERAX", "camera_start_failed=" + error.getClass().getSimpleName(), -1L, sensors.stability());
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
        sceneSummary.reset();
        if (cameraProvider != null) cameraProvider.unbindAll();
        closeCameraXAnalyzer();
        visionWatchdog.beginMode(SystemClock.elapsedRealtime(), true);
        if (gateCRecorder.isActive()) gateCRecorder.recordMode("HANDOFF", "CameraX_to_ARCore");
        updateStatus("Canlı ARCore Depth16 moduna geçiliyor…", false);
        depthStatusView.setText("Derinlik: kamera ARCore'a devrediliyor…");
        previewView.postDelayed(this::startArCore, 220L);
    }

    private void startArCore() {
        if (destroyed) return;
        if (arCoreEngine != null) arCoreEngine.close();
        visionWatchdog.beginMode(SystemClock.elapsedRealtime(), true);
        lastWatchdogHealth = VisionHealthWatchdog.Health.STARTING;
        arCoreEngine = new ArCoreLiveVisionEngine(this, new ArCoreLiveVisionEngine.Listener() {
            @Override public void onMotion(MotionObservation observation) { handleMotion(observation); }
            @Override public void onObject(ObjectObservation observation) { handleObject(observation); }
            @Override public void onGround(GroundObservation observation) { handleGround(observation); }
            @Override public void onSceneHealth(SceneHealthObservation observation) { handleSceneHealth(observation); }
            @Override public void onDepth(DepthObservation observation) { handleDepth(observation); }
            @Override public void onWalkable(WalkableCorridorObservation observation) { handleWalkable(observation); }
            @Override public void onTextRecognized(String text) { handleTextRecognized(text); }

            @Override public void onStatus(String status) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    visionMode = VisionMode.ARCORE;
                    watchdogRecoveryInProgress = false;
                    previewView.setVisibility(View.GONE);
                    depthStatusView.setText("Derinlik: canlı Depth16 + yürüyüş koridoru aktif");
                    depthStatusView.setContentDescription("Derinlik sistemi: canlı Depth16 ve göreli yürüyüş koridoru aktif");
                    if (gateCRecorder.isActive()) gateCRecorder.recordMode("ARCORE", "live_depth_walkable_active");
                    updateStatus(status, false);
                });
            }

            @Override public void onFatal(String message) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    if (gateCRecorder.isActive()) gateCRecorder.recordFallback("ARCORE", "engine_fatal");
                    fallbackToCameraX(message, true);
                });
            }
        });
        if (!arCoreEngine.start(currentDisplayRotation())) {
            if (gateCRecorder.isActive()) gateCRecorder.recordFallback("ARCORE", "start_rejected");
            fallbackToCameraX("ARCore canlı mod başlatılamadı. CameraX güvenli moda dönülüyor.", true);
        }
    }

    private void handleMotion(MotionObservation observation) {
        sceneSummary.update(observation);
        long elapsed = SystemClock.elapsedRealtime();
        visionWatchdog.noteVision(elapsed);
        float stability = sensors.stability();
        if (gateCRecorder.isActive()) gateCRecorder.recordMotion(modeName(), observation, stability);
        if (!guidanceEnabled) return;
        recordAndAnnounceDecision(safetyGate.evaluate(observation, stability), "MOTION", observation.timestampMs());
    }

    private void handleObject(ObjectObservation observation) {
        sceneSummary.update(observation);
        float stability = sensors.stability();
        if (gateCRecorder.isActive()) gateCRecorder.recordObject(modeName(), observation, stability);
        if (!guidanceEnabled) return;
        recordAndAnnounceDecision(safetyGate.evaluateObject(observation, stability), "OBJECT", observation.timestampMs());
    }

    private void handleGround(GroundObservation observation) {
        sceneSummary.update(observation);
        float stability = sensors.stability();
        if (gateCRecorder.isActive()) gateCRecorder.recordGround(modeName(), observation, stability);
        if (!guidanceEnabled) return;
        recordAndAnnounceDecision(safetyGate.evaluateGround(observation, stability), "GROUND", observation.timestampMs());
        GroundDepthEvidence evidence = groundDepthSynchronizer.offerGround(observation);
        if (evidence != null) {
            long sourceTs = Math.max(evidence.ground().timestampMs(), evidence.depth().timestampMs());
            recordAndAnnounceDecision(
                    safetyGate.evaluateGroundWithDepth(evidence.ground(), evidence.depth(), stability),
                    "GROUND_DEPTH", sourceTs);
        }
    }

    private void handleSceneHealth(SceneHealthObservation observation) {
        sceneSummary.update(observation);
        if (!guidanceEnabled) return;
        GuidanceDecision decision = safetyGate.evaluateSceneHealth(observation, sensors.stability());
        recordAndAnnounceDecision(decision, "SCENE_HEALTH", observation.timestampMs());
    }

    private void handleDepth(DepthObservation observation) {
        sceneSummary.update(observation);
        visionWatchdog.noteDepth(SystemClock.elapsedRealtime());
        float stability = sensors.stability();
        if (gateCRecorder.isActive()) gateCRecorder.recordDepth(modeName(), observation, stability);
        if (!guidanceEnabled) return;
        recordAndAnnounceDecision(safetyGate.evaluateDepth(observation, stability), "DEPTH", observation.timestampMs());
        GroundDepthEvidence evidence = groundDepthSynchronizer.offerDepth(observation);
        if (evidence != null) {
            long sourceTs = Math.max(evidence.ground().timestampMs(), evidence.depth().timestampMs());
            recordAndAnnounceDecision(
                    safetyGate.evaluateGroundWithDepth(evidence.ground(), evidence.depth(), stability),
                    "GROUND_DEPTH", sourceTs);
        }
    }

    private void handleWalkable(WalkableCorridorObservation observation) {
        sceneSummary.update(observation);
        if (!guidanceEnabled) return;
        GuidanceDecision decision = safetyGate.evaluateWalkable(observation, sensors.stability());
        recordAndAnnounceDecision(decision, "WALKABLE", observation.timestampMs());
    }

    private void handleTextRecognized(String rawText) {
        String text = rawText == null ? "" : rawText.replaceAll("\\s+", " ").trim();
        if (text.length() > 420) text = text.substring(0, 420) + ". Devamı var.";
        if (text.isEmpty()) text = "Okunabilir yazı bulamadım.";
        deliverNonSafety(GuidancePriorityArbiter.Channel.SCENE, text);
    }

    private void requestTextScan() {
        if (visionMode == VisionMode.ARCORE && arCoreEngine != null) {
            arCoreEngine.requestTextScan();
            deliverNonSafety(GuidancePriorityArbiter.Channel.SCENE, "Yazıyı okuyorum.");
        } else if (visionMode == VisionMode.CAMERAX && visionAnalyzer != null) {
            visionAnalyzer.requestTextScan();
            deliverNonSafety(GuidancePriorityArbiter.Channel.SCENE, "Yazıyı okuyorum.");
        } else {
            deliverNonSafety(GuidancePriorityArbiter.Channel.SYSTEM, "Kamera henüz hazır değil.");
        }
    }

    private void recordAndAnnounceDecision(GuidanceDecision decision, String source, long sourceTimestampMs) {
        float stability = sensors != null ? sensors.stability() : 0f;
        if (gateCRecorder != null && gateCRecorder.isActive()) {
            gateCRecorder.recordDecision(source, modeName(), decision, sourceTimestampMs, stability);
        }
        long now = System.currentTimeMillis();
        if (!priorityArbiter.shouldDeliver(GuidancePriorityArbiter.Channel.SAFETY, decision, now)) return;
        if (!announcementGate.shouldAnnounce(decision, now)) return;
        runOnUiThread(() -> {
            boolean urgent = decision.risk() == Risk.STOP;
            updateStatus(decision.speech() + "  Güven: " + Math.round(decision.confidence() * 100) + "%", urgent);
            speaker.announce(decision);
        });
    }

    private void deliverNonSafety(GuidancePriorityArbiter.Channel channel, String text) {
        if (text == null || text.trim().isEmpty()) return;
        GuidanceDecision message = new GuidanceDecision(
                Risk.INFO,
                com.mgecgil.seslirehber.core.GuidanceModels.Direction.UNKNOWN,
                text,
                1f);
        long now = System.currentTimeMillis();
        if (!priorityArbiter.shouldDeliver(channel, message, now)) {
            updateStatus("Güvenlik uyarısı öncelikli; istek geçici olarak bastırıldı.", true);
            return;
        }
        updateStatus(text, false);
        speaker.speak(text);
    }

    private void checkVisionHealth() {
        if (!guidanceEnabled || visionMode == VisionMode.STARTING || destroyed) return;
        VisionHealthWatchdog.Snapshot snapshot = visionWatchdog.snapshot(SystemClock.elapsedRealtime());
        if (snapshot.health() == VisionHealthWatchdog.Health.HEALTHY) {
            lastWatchdogHealth = snapshot.health();
            return;
        }
        if (snapshot.health() == VisionHealthWatchdog.Health.STARTING) return;

        boolean firstIncident = snapshot.health() != lastWatchdogHealth;
        lastWatchdogHealth = snapshot.health();
        if (firstIncident && gateCRecorder.isActive()) {
            long age = snapshot.health() == VisionHealthWatchdog.Health.VISION_STALE
                    ? snapshot.visionAgeMs() : snapshot.depthAgeMs();
            gateCRecorder.recordWatchdog(modeName(), snapshot.health().name(), age, sensors.stability());
        }

        if (snapshot.health() == VisionHealthWatchdog.Health.VISION_STALE && firstIncident) {
            GuidanceDecision decision = new GuidanceDecision(
                    Risk.STOP,
                    com.mgecgil.seslirehber.core.GuidanceModels.Direction.UNKNOWN,
                    "Dur. Görüntü akışı kesildi. Bastonla doğrula.",
                    1f);
            recordAndAnnounceDecision(decision, "WATCHDOG", System.currentTimeMillis());
        }

        if (snapshot.health() == VisionHealthWatchdog.Health.DEPTH_STALE && firstIncident) {
            depthStatusView.setText("Derinlik: veri geçici olarak kesildi");
            updateStatus("Derinlik verisi güncel değil; kamera tabanlı rehberlik sürüyor.", false);
        }

        if (snapshot.failoverRecommended() && !watchdogRecoveryInProgress) {
            watchdogRecoveryInProgress = true;
            if (gateCRecorder.isActive()) gateCRecorder.recordFallback(modeName(), "watchdog_" + snapshot.health().name());
            fallbackToCameraX("Algı akışı sağlık denetimi CameraX güvenli moda dönüyor.", snapshot.health() == VisionHealthWatchdog.Health.VISION_STALE);
        }
    }

    private void fallbackToCameraX(String message, boolean urgent) {
        if (destroyed) return;
        visionMode = VisionMode.STARTING;
        groundDepthSynchronizer.reset();
        sceneSummary.reset();
        if (arCoreEngine != null) {
            arCoreEngine.close();
            arCoreEngine = null;
        }
        previewView.setVisibility(View.VISIBLE);
        depthStatusView.setText("Derinlik: CameraX yedek mod");
        updateStatus(message, urgent);
        if (urgent) speaker.speak("Güvenli kamera moduna dönülüyor.");
        startCameraX();
    }

    private void handleVoice(String text) {
        OfflineIntentParser.ParsedIntent parsed = intentParser.parse(text);
        switch (parsed.intent()) {
            case START_GUIDANCE -> {
                guidanceEnabled = true;
                announcementGate.reset();
                priorityArbiter.reset();
                speaker.speak("Rehberlik açık.");
            }
            case STOP_GUIDANCE -> {
                guidanceEnabled = false;
                speaker.speak("Rehberlik durduruldu.");
            }
            case WAKE_MODE_ON -> {
                if (!voice.isHandsFreeEnabled()) toggleHandsFree();
                else speaker.speak("Hey Rehber zaten açık.");
            }
            case WAKE_MODE_OFF -> {
                if (voice.isHandsFreeEnabled()) {
                    voice.setHandsFreeEnabled(false);
                    updateVoiceUi();
                    speaker.speak("Hey Rehber kapalı.");
                } else speaker.speak("Hey Rehber zaten kapalı.");
            }
            case REPEAT -> speaker.repeat();
            case READ_TEXT -> requestTextScan();
            case DESCRIBE_SCENE -> deliverNonSafety(
                    GuidancePriorityArbiter.Channel.SCENE,
                    sceneSummary.summarize(System.currentTimeMillis()));
            case NAVIGATE_TO -> {
                pendingDestination = parsed.argument();
                deliverNonSafety(
                        GuidancePriorityArbiter.Channel.NAVIGATION,
                        "Hedef algılandı: " + pendingDestination
                                + ". Rota motoru henüz bağlı değil; yönlendirme başlatılmadı.");
            }
            case HELP -> speaker.speak(
                    "Komutlar: Hey Rehber çevremi anlat, yazıyı oku, beni bir adrese götür, "
                            + "rehberliği başlat, rehberliği durdur ve tekrar et. "
                            + "Hey Rehber modu yalnız cihazda yerel tanıma varsa eller serbest açılır.");
            case UNKNOWN -> speaker.speak("Komutu anlayamadım.");
        }
        updateStatus("Komut: " + text, false);
    }

    private String modeName() { return visionMode.name(); }

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
        if (voice != null) voice.onHostStart();
        healthHandler.removeCallbacks(healthTick);
        healthHandler.postDelayed(healthTick, 500L);
        if (visionMode == VisionMode.ARCORE && arCoreEngine != null && !arCoreEngine.isRunning()) {
            visionWatchdog.beginMode(SystemClock.elapsedRealtime(), true);
            arCoreEngine.start(currentDisplayRotation());
        }
    }

    @Override
    protected void onStop() {
        healthHandler.removeCallbacks(healthTick);
        if (voice != null) voice.onHostStop();
        if (arCoreEngine != null) arCoreEngine.stop();
        if (sensors != null) sensors.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        healthHandler.removeCallbacksAndMessages(null);
        if (cameraProvider != null) cameraProvider.unbindAll();
        closeCameraXAnalyzer();
        if (arCoreEngine != null) {
            arCoreEngine.close();
            arCoreEngine = null;
        }
        if (gateCRecorder != null) gateCRecorder.close();
        if (voice != null) voice.destroy();
        if (speaker != null) speaker.shutdown();
        if (cameraExecutor != null) cameraExecutor.shutdownNow();
        super.onDestroy();
    }
}
