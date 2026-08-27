package com.mgecgil.seslirehber.navigation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.mgecgil.seslirehber.MainActivity;

/**
 * Foreground process/lifecycle anchor for an explicitly started pedestrian navigation session.
 * It is started only while the activity is visible and fine-location permission is already granted.
 * Camera and microphone are intentionally NOT owned by this service.
 */
public final class NavigationForegroundService extends Service {
    private static final String CHANNEL_ID = "pedestrian_navigation";
    private static final int NOTIFICATION_ID = 4107;
    private static final String ACTION_START = "com.mgecgil.seslirehber.NAV_START";
    private static final String ACTION_UPDATE = "com.mgecgil.seslirehber.NAV_UPDATE";
    private static final String ACTION_STOP = "com.mgecgil.seslirehber.NAV_STOP";
    private static final String EXTRA_STATUS = "status";

    public static boolean start(Context context, String status) {
        try {
            Intent intent = new Intent(context, NavigationForegroundService.class)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_STATUS, safeStatus(status));
            ContextCompat.startForegroundService(context, intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void update(Context context, String status) {
        try {
            Intent intent = new Intent(context, NavigationForegroundService.class)
                    .setAction(ACTION_UPDATE)
                    .putExtra(EXTRA_STATUS, safeStatus(status));
            context.startService(intent);
        } catch (Throwable ignored) {}
    }

    public static void stop(Context context) {
        try {
            Intent intent = new Intent(context, NavigationForegroundService.class).setAction(ACTION_STOP);
            context.startService(intent);
        } catch (Throwable ignored) {
            try { context.stopService(new Intent(context, NavigationForegroundService.class)); }
            catch (Throwable ignoredAgain) {}
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        String status = intent == null ? "Yaya navigasyonu aktif" : safeStatus(intent.getStringExtra(EXTRA_STATUS));
        Notification notification = buildNotification(status);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String status) {
        Intent launch = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                this,
                4107,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Sesli Rehber — yaya navigasyonu")
                .setContentText(status)
                .setStyle(new Notification.BigTextStyle().bigText(status))
                .setContentIntent(pending)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Yaya navigasyonu",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Ekran kapalıyken devam eden yaya rota takibi");
        manager.createNotificationChannel(channel);
    }

    private static String safeStatus(String value) {
        if (value == null || value.trim().isEmpty()) return "Yaya navigasyonu aktif";
        String clean = value.replaceAll("\\s+", " ").trim();
        return clean.length() > 180 ? clean.substring(0, 180) : clean;
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
