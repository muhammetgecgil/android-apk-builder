package com.mgecgil.seslirehber.core;

import android.content.Context;
import android.os.Build;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;

/**
 * Optional ARCore/Depth capability probe. It never resumes a Session and therefore does not take
 * camera ownership away from the active CameraX safety pipeline.
 */
public final class ArCoreDepthCapability {
    public record Result(
            boolean arCoreSupported,
            boolean arCoreInstalled,
            boolean depthSupported,
            boolean hardwareBufferModeSupported,
            String status) {}

    public interface Listener {
        void onResult(Result result);
    }

    private ArCoreDepthCapability() {}

    public static void probe(Context context, Listener listener) {
        Context app = context.getApplicationContext();
        ArCoreApk.getInstance().checkAvailabilityAsync(app, availability -> {
            if (availability.isUnsupported()) {
                listener.onResult(new Result(false, false, false, false,
                        "ARCore desteklenmiyor; CameraX güvenli modu aktif."));
                return;
            }
            if (availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED) {
                listener.onResult(new Result(true, false, false, false,
                        "ARCore destekli ancak Google Play Hizmetleri AR yüklü değil."));
                return;
            }
            if (availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD) {
                listener.onResult(new Result(true, false, false, false,
                        "ARCore güncellemesi gerekiyor; CameraX güvenli modu aktif."));
                return;
            }
            if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                listener.onResult(new Result(false, false, false, false,
                        "ARCore durumu henüz doğrulanamadı; CameraX güvenli modu aktif."));
                return;
            }

            Thread probeThread = new Thread(() -> probeInstalled(app, listener), "rehber-arcore-probe");
            probeThread.setDaemon(true);
            probeThread.start();
        });
    }

    private static void probeInstalled(Context context, Listener listener) {
        Session session = null;
        try {
            session = new Session(context);
            boolean depth = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
            boolean hardwareBuffer = false;

            if (depth) {
                Config config = session.getConfig();
                config.setDepthMode(Config.DepthMode.AUTOMATIC);
                if (Build.VERSION.SDK_INT >= 27) {
                    try {
                        config.setTextureUpdateMode(Config.TextureUpdateMode.EXPOSE_HARDWARE_BUFFER);
                        session.configure(config);
                        hardwareBuffer = true;
                    } catch (Throwable unsupportedBufferMode) {
                        config = session.getConfig();
                        config.setDepthMode(Config.DepthMode.AUTOMATIC);
                        session.configure(config);
                    }
                } else {
                    session.configure(config);
                }
            }

            String status;
            if (!depth) {
                status = "ARCore var, Depth API bu cihaz/kamera yapılandırmasında yok.";
            } else if (hardwareBuffer) {
                status = "ARCore Depth API hazır; canlı derinlik motoru için donanım tampon yolu destekli.";
            } else {
                status = "ARCore Depth API hazır; canlı motor GPU kamera yolu gerektiriyor.";
            }
            listener.onResult(new Result(true, true, depth, hardwareBuffer, status));
        } catch (Throwable error) {
            listener.onResult(new Result(true, true, false, false,
                    "ARCore oturumu doğrulanamadı; CameraX güvenli modu korunuyor."));
        } finally {
            if (session != null) session.close();
        }
    }
}
