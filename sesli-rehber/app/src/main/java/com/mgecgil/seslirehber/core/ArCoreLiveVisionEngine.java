package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.view.Surface;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/** Headless ARCore camera owner with aligned Depth16 plus advisory general and urban segmentation. */
public final class ArCoreLiveVisionEngine implements AutoCloseable {
    public interface Listener {
        void onMotion(MotionObservation observation);
        void onObject(ObjectObservation observation);
        void onGround(GroundObservation observation);
        default void onSceneHealth(SceneHealthObservation observation) {}
        void onDepth(DepthObservation observation);
        default void onWalkable(WalkableCorridorObservation observation) {}
        default void onTextRecognized(String text) {}
        default void onDistantObject(DistantObjectObservation observation) {
            SituationalAwarenessContext.noteDistant(observation);
            String text = DistantObjectSpeech.format(observation);
            if (!text.isEmpty()) onTextRecognized(text);
        }
        void onStatus(String status);
        void onFatal(String message);
    }

    private static final int GRID_W = 48;
    private static final int GRID_H = 72;
    private static final long VISUAL_FRAME_INTERVAL_MS = 180L;
    private final Context context;
    private final Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean detectorBusy = new AtomicBoolean(false);
    private final AtomicBoolean textScanRequested = new AtomicBoolean(false);
    private final byte[] lumaGrid = new byte[GRID_W * GRID_H];
    private final GridEvidenceEstimator gridEstimator = new GridEvidenceEstimator(GRID_W, GRID_H);
    private final ObjectObservationTracker objectTracker = new ObjectObservationTracker();
    private final DepthImageAdapter depthAdapter = new DepthImageAdapter();
    private final DistantObjectRecognizer distantRecognizer = new DistantObjectRecognizer();
    private final SemanticSegmentationEngine segmentationEngine = new SemanticSegmentationEngine();
    private final UrbanSegmentationEngine urbanSegmentationEngine = new UrbanSegmentationEngine();
    private final ObjectDetector objectDetector;
    private final TextRecognizer textRecognizer;
    private volatile Session session;
    private volatile int displayRotation = Surface.ROTATION_0;
    private long lastVisualFrameMs;

    public ArCoreLiveVisionEngine(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .build();
        objectDetector = ObjectDetection.getClient(options);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public boolean start(int displayRotation) {
        if (!running.compareAndSet(false, true)) return false;
        this.displayRotation = displayRotation;
        executor.execute(this::runLoop);
        return true;
    }

    public boolean isRunning() { return running.get(); }
    public void requestTextScan() { textScanRequested.set(true); }

    private void runLoop() {
        Session localSession = null;
        String fatalMessage = null;
        try {
            localSession = new Session(context);
            if (!localSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                throw new IllegalStateException("Depth API unsupported");
            }

            Config config = localSession.getConfig();
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setTextureUpdateMode(Config.TextureUpdateMode.EXPOSE_HARDWARE_BUFFER);
            localSession.configure(config);
            localSession.resume();
            session = localSession;

            int imageRotationDegrees = resolveImageRotation(localSession, displayRotation);
            listener.onStatus("ARCore canlı derinlik modu aktif. Yakın güvenlik, şehir segmentasyonu, çoklu nesne ve uzak görüş birlikte izleniyor.");

            long lastFrameTimestampNs = Long.MIN_VALUE;
            while (running.get()) {
                Frame frame = localSession.update();
                long frameTimestampNs = frame.getTimestamp();
                if (frameTimestampNs == lastFrameTimestampNs) {
                    sleepQuietly(4L);
                    continue;
                }
                lastFrameTimestampNs = frameTimestampNs;
                processFrame(frame, imageRotationDegrees, System.currentTimeMillis());
            }
        } catch (Throwable error) {
            if (running.get()) fatalMessage = "ARCore canlı derinlik modu kullanılamadı. CameraX güvenli moda dönülüyor.";
        } finally {
            running.set(false);
            session = null;
            ArCoreVisualFrameContext.reset();
            if (localSession != null) {
                try { localSession.pause(); } catch (Throwable ignored) {}
                try { localSession.close(); } catch (Throwable ignored) {}
            }
            if (fatalMessage != null) listener.onFatal(fatalMessage);
        }
    }

    private void processFrame(Frame frame, int rotationDegrees, long nowMs) {
        Image cameraImage = null;
        Image depthImage = null;
        try {
            cameraImage = frame.acquireCameraImage();
            sampleLuma(cameraImage, lumaGrid);
            int uprightWidth = (rotationDegrees == 90 || rotationDegrees == 270)
                    ? cameraImage.getHeight() : cameraImage.getWidth();
            int uprightHeight = (rotationDegrees == 90 || rotationDegrees == 270)
                    ? cameraImage.getWidth() : cameraImage.getHeight();
            float sourceAspect = uprightHeight <= 0 ? 9f / 16f : uprightWidth / (float) uprightHeight;
            HudPerceptionContext.noteSourceAspect(sourceAspect);

            GridEvidenceEstimator.Result evidence = gridEstimator.analyze(lumaGrid, rotationDegrees, nowMs);
            listener.onSceneHealth(evidence.sceneHealth());
            listener.onMotion(evidence.motion());
            if (evidence.ground().viewConfidence() > 0.08f) listener.onGround(evidence.ground());

            segmentationEngine.maybeAnalyze(cameraImage, rotationDegrees, nowMs);
            urbanSegmentationEngine.maybeAnalyze(cameraImage, rotationDegrees, nowMs);

            if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
                try {
                    depthImage = frame.acquireDepthImage16Bits();
                    DepthImageAdapter.AlignedEvidence depthEvidence = depthAdapter.analyzeAlignedEvidence(
                            frame,
                            depthImage,
                            cameraImage.getWidth(),
                            cameraImage.getHeight(),
                            rotationDegrees,
                            nowMs);
                    if (depthEvidence.depth().validRatio() > 0.04f) listener.onDepth(depthEvidence.depth());
                    if (depthEvidence.walkable().confidence() > 0.20f) listener.onWalkable(depthEvidence.walkable());
                } catch (NotYetAvailableException ignored) {
                    // Normal at startup, low motion, weak texture or temporary tracking loss.
                }
            }

            // Visual-only preview is intentionally generated after Depth evidence so HUD rendering can
            // never outrank the safety path. Low resolution and rate keep the extra work bounded.
            if (nowMs - lastVisualFrameMs >= VISUAL_FRAME_INTERVAL_MS) {
                lastVisualFrameMs = nowMs;
                Bitmap visual = null;
                try {
                    boolean portrait = rotationDegrees == 90 || rotationDegrees == 270;
                    visual = UprightYuvBitmapExtractor.extract(
                            cameraImage,
                            rotationDegrees,
                            portrait ? 270 : 480,
                            portrait ? 480 : 270);
                    if (visual != null) ArCoreVisualFrameContext.publish(visual, sourceAspect, nowMs);
                } catch (Throwable ignored) {
                    // Missing HUD preview is never a safety failure.
                } finally {
                    if (visual != null) try { visual.recycle(); } catch (Throwable ignored) {}
                }
            }

            if (!detectorBusy.compareAndSet(false, true)) return;
            final Image heldImage = cameraImage;
            cameraImage = null;
            int sourceWidth = heldImage.getWidth();
            int sourceHeight = heldImage.getHeight();
            int objectUprightWidth = (rotationDegrees == 90 || rotationDegrees == 270) ? sourceHeight : sourceWidth;
            int objectUprightHeight = (rotationDegrees == 90 || rotationDegrees == 270) ? sourceWidth : sourceHeight;
            InputImage input = InputImage.fromMediaImage(heldImage, rotationDegrees);

            boolean doTextScan = textScanRequested.getAndSet(false)
                    || VisionTextContext.shouldAutoScan(nowMs);
            if (doTextScan) {
                textRecognizer.process(input)
                        .addOnSuccessListener(result -> {
                            String text = result.getText() == null ? "" : result.getText().trim();
                            listener.onTextRecognized(VisionTextContext.enrichOcr(text, System.currentTimeMillis()));
                        })
                        .addOnCompleteListener(task -> {
                            heldImage.close();
                            detectorBusy.set(false);
                        });
                return;
            }

            distantRecognizer.maybeAnalyze(heldImage, rotationDegrees, nowMs, listener::onDistantObject);

            objectDetector.process(input)
                    .addOnSuccessListener(objects -> {
                        List<ObjectObservation> observations = objectTracker.observeAll(
                                objects, objectUprightWidth, objectUprightHeight, nowMs);
                        for (ObjectObservation observation : observations) listener.onObject(observation);
                    })
                    .addOnCompleteListener(task -> {
                        heldImage.close();
                        detectorBusy.set(false);
                    });
        } catch (NotYetAvailableException ignored) {
            // CPU camera image may not be ready on every ARCore frame.
        } catch (Throwable ignored) {
            // Transient frame acquisition loss does not justify abandoning ARCore immediately.
        } finally {
            if (depthImage != null) depthImage.close();
            if (cameraImage != null) cameraImage.close();
        }
    }

    private static void sampleLuma(Image image, byte[] output) {
        if (image.getPlanes().length == 0) return;
        Image.Plane yPlane = image.getPlanes()[0];
        ByteBuffer buffer = yPlane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = yPlane.getRowStride();
        int pixelStride = yPlane.getPixelStride();
        for (int gy = 0; gy < GRID_H; gy++) {
            int sy = Math.min(height - 1, gy * height / GRID_H);
            for (int gx = 0; gx < GRID_W; gx++) {
                int sx = Math.min(width - 1, gx * width / GRID_W);
                int offset = sy * rowStride + sx * pixelStride;
                output[gy * GRID_W + gx] = buffer.get(offset);
            }
        }
    }

    private int resolveImageRotation(Session session, int displayRotation) {
        try {
            String cameraId = session.getCameraConfig().getCameraId();
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            Integer sensorOrientation = manager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SENSOR_ORIENTATION);
            int sensor = sensorOrientation == null ? 90 : sensorOrientation;
            int displayDegrees = switch (displayRotation) {
                case Surface.ROTATION_90 -> 90;
                case Surface.ROTATION_180 -> 180;
                case Surface.ROTATION_270 -> 270;
                default -> 0;
            };
            return (sensor - displayDegrees + 360) % 360;
        } catch (Throwable ignored) {
            return 90;
        }
    }

    public void stop() { running.set(false); }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }

    @Override
    public void close() {
        stop();
        executor.shutdownNow();
        objectTracker.reset();
        gridEstimator.reset();
        depthAdapter.reset();
        distantRecognizer.close();
        segmentationEngine.close();
        urbanSegmentationEngine.close();
        ArCoreVisualFrameContext.reset();
        try { objectDetector.close(); } catch (Throwable ignored) {}
        try { textRecognizer.close(); } catch (Throwable ignored) {}
    }
}
