package com.mgecgil.seslirehber.core;

import android.media.Image;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

/** CameraX source feeding the same evidence cores used by the ARCore live path. */
public final class VisionFusionAnalyzer implements ImageAnalysis.Analyzer, AutoCloseable {
    public interface Listener {
        void onMotion(MotionObservation observation);
        void onObject(ObjectObservation observation);
        void onGround(GroundObservation observation);
        default void onSceneHealth(SceneHealthObservation observation) {}
        default void onTextRecognized(String text) {}
        default void onDistantObject(DistantObjectObservation observation) {
            SituationalAwarenessContext.noteDistant(observation);
            String text = DistantObjectSpeech.format(observation);
            if (!text.isEmpty()) onTextRecognized(text);
        }
        void onVisionError(String message);
    }

    private static final int GRID_W = 48;
    private static final int GRID_H = 72;
    private final byte[] current = new byte[GRID_W * GRID_H];
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final AtomicBoolean textScanRequested = new AtomicBoolean(false);
    private final Listener listener;
    private final ObjectDetector objectDetector;
    private final TextRecognizer textRecognizer;
    private final DistantObjectRecognizer distantRecognizer = new DistantObjectRecognizer();
    private final SemanticSegmentationEngine segmentationEngine = new SemanticSegmentationEngine();
    private final GridEvidenceEstimator gridEstimator = new GridEvidenceEstimator(GRID_W, GRID_H);
    private final ObjectObservationTracker objectTracker = new ObjectObservationTracker();

    public VisionFusionAnalyzer(Listener listener) {
        this.listener = listener;
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .build();
        objectDetector = ObjectDetection.getClient(options);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void requestTextScan() { textScanRequested.set(true); }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (!processing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }
        try {
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            long nowMs = System.currentTimeMillis();
            sampleLuma(imageProxy);
            GridEvidenceEstimator.Result evidence = gridEstimator.analyze(current, rotation, nowMs);
            listener.onSceneHealth(evidence.sceneHealth());
            listener.onMotion(evidence.motion());
            if (evidence.ground().viewConfidence() > 0.08f) listener.onGround(evidence.ground());

            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                processing.set(false);
                imageProxy.close();
                return;
            }

            segmentationEngine.maybeAnalyze(mediaImage, rotation, nowMs);

            int sourceWidth = imageProxy.getWidth();
            int sourceHeight = imageProxy.getHeight();
            int uprightWidth = (rotation == 90 || rotation == 270) ? sourceHeight : sourceWidth;
            int uprightHeight = (rotation == 90 || rotation == 270) ? sourceWidth : sourceHeight;
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, rotation);

            boolean doTextScan = textScanRequested.getAndSet(false)
                    || VisionTextContext.shouldAutoScan(nowMs);
            if (doTextScan) {
                textRecognizer.process(inputImage)
                        .addOnSuccessListener(result -> {
                            String text = result.getText() == null ? "" : result.getText().trim();
                            listener.onTextRecognized(
                                    VisionTextContext.enrichOcr(text, System.currentTimeMillis()));
                        })
                        .addOnFailureListener(error -> listener.onVisionError("Yazı okuma geçici olarak kullanılamıyor."))
                        .addOnCompleteListener(task -> {
                            processing.set(false);
                            imageProxy.close();
                        });
                return;
            }

            distantRecognizer.maybeAnalyze(
                    mediaImage,
                    rotation,
                    nowMs,
                    listener::onDistantObject);

            objectDetector.process(inputImage)
                    .addOnSuccessListener(objects -> {
                        List<ObjectObservation> observations = objectTracker.observeAll(
                                objects, uprightWidth, uprightHeight, nowMs);
                        for (ObjectObservation observation : observations) listener.onObject(observation);
                    })
                    .addOnFailureListener(error ->
                            listener.onVisionError("Nesne algılama geçici olarak kullanılamıyor."))
                    .addOnCompleteListener(task -> {
                        processing.set(false);
                        imageProxy.close();
                    });
        } catch (Throwable error) {
            processing.set(false);
            imageProxy.close();
            listener.onVisionError("Görüntü analizi geçici olarak kullanılamıyor.");
        }
    }

    private void sampleLuma(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        if (planes.length == 0) return;
        ImageProxy.PlaneProxy yPlane = planes[0];
        ByteBuffer buffer = yPlane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = yPlane.getRowStride();
        int pixelStride = yPlane.getPixelStride();
        for (int gy = 0; gy < GRID_H; gy++) {
            int sy = Math.min(height - 1, gy * height / GRID_H);
            for (int gx = 0; gx < GRID_W; gx++) {
                int sx = Math.min(width - 1, gx * width / GRID_W);
                int index = sy * rowStride + sx * pixelStride;
                current[gy * GRID_W + gx] = buffer.get(index);
            }
        }
    }

    static float[] rotateNormalized(float x, float y, int rotationDegrees) {
        return GridEvidenceEstimator.rotateNormalized(x, y, rotationDegrees);
    }

    @Override
    public void close() {
        objectDetector.close();
        textRecognizer.close();
        distantRecognizer.close();
        segmentationEngine.close();
        objectTracker.reset();
        gridEstimator.reset();
    }
}
