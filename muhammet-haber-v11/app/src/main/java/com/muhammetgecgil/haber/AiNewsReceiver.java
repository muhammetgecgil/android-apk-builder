package com.muhammetgecgil.haber;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

public class AiNewsReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "ai_daily_news";
    private static final int REQUEST_CODE = 22011;

    @Override public void onReceive(Context context, Intent intent) {
        PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                List<NewsUtils.Article> items = NewsUtils.fetch("yapay zeka AI artificial intelligence", 5);
                if (!items.isEmpty()) showNotification(context, items);
            } catch (Exception ignored) {
            } finally {
                pending.finish();
            }
        }).start();
    }

    public static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AiNewsReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 9);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 1);

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    private static void showNotification(Context context, List<NewsUtils.Article> items) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Günlük AI Haberleri", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Günde bir kez yapay zekâ haber özeti");
            nm.createNotificationChannel(channel);
        }

        StringBuilder body = new StringBuilder();
        for (int n = 0; n < Math.min(4, items.size()); n++) {
            if (n > 0) body.append("\n\n");
            body.append("• ").append(items.get(n).title);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.putExtra("open_ai", true);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context, 991, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Bugünün AI Haberleri")
                .setContentText(items.get(0).title)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(body.toString()))
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .setWhen(System.currentTimeMillis());
        nm.notify(1101, b.build());
    }
}
