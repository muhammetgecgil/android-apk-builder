package com.muhammet.habersafev5;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsReminderReceiver extends BroadcastReceiver {
    public static final String PREFS = "haber_safe_reminders";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_SELECTED = "selected_categories";
    public static final String KEY_HOUR = "hour";
    public static final String KEY_MINUTE = "minute";
    private static final String KEY_SEEN = "seen_links";
    private static final String CHANNEL_ID = "daily_news";
    private static final int NOTIFICATION_ID = 5101;
    private static final int ALARM_REQUEST = 5102;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            scheduleNext(context);
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ENABLED, false)) return;

        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                sendDailyNewsNotification(appContext);
            } finally {
                scheduleNext(appContext);
                pendingResult.finish();
            }
        }, "haber-safe-daily").start();
    }

    public static void scheduleNext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pi = alarmPendingIntent(context);
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            alarmManager.cancel(pi);
            return;
        }

        int hour = prefs.getInt(KEY_HOUR, 8);
        int minute = prefs.getInt(KEY_MINUTE, 0);
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) alarmManager.cancel(alarmPendingIntent(context));
    }

    private static PendingIntent alarmPendingIntent(Context context) {
        Intent intent = new Intent(context, NewsReminderReceiver.class);
        intent.setAction("com.muhammet.habersafev5.DAILY_NEWS");
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void sendDailyNewsNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> selected = new HashSet<>(prefs.getStringSet(KEY_SELECTED, new HashSet<>()));
        if (selected.isEmpty()) return;

        String query = NewsRepository.combinedQuery(selected);
        List<NewsRepository.NewsItem> latest = new ArrayList<>();
        try {
            latest = NewsRepository.fetchGoogleNews(query, 20);
        } catch (Exception ignored) {
            // Günlük hatırlatma yine gösterilir; uygulama açılınca kullanıcı yenileyebilir.
        }

        Set<String> seen = new HashSet<>(prefs.getStringSet(KEY_SEEN, new HashSet<>()));
        int newCount = 0;
        HashSet<String> currentLinks = new HashSet<>();
        for (NewsRepository.NewsItem item : latest) {
            currentLinks.add(item.link);
            if (!seen.contains(item.link)) newCount++;
        }
        if (!currentLinks.isEmpty()) prefs.edit().putStringSet(KEY_SEEN, currentLinks).apply();

        createChannel(context);

        Intent open = new Intent(context, MainActivity.class);
        open.putExtra("open_my_news", true);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                5103,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = newCount > 0
                ? "Haber SAFE • " + newCount + " yeni haber"
                : "Haber SAFE • Günlük haberlerin hazır";

        String categoryText = TextUtils.join(" • ", selected);
        Notification.InboxStyle style = new Notification.InboxStyle();
        int shown = Math.min(4, latest.size());
        for (int i = 0; i < shown; i++) {
            style.addLine(latest.get(i).title);
        }
        if (latest.isEmpty()) {
            style.addLine("Seçtiğin alanlardaki haberleri okumak için dokun.");
        }
        style.setSummaryText(categoryText);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        builder.setSmallIcon(R.drawable.ic_haber_safe_v5)
                .setContentTitle(title)
                .setContentText(latest.isEmpty() ? categoryText : latest.get(0).title)
                .setStyle(style)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setCategory(Notification.CATEGORY_RECOMMENDATION);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.notify(NOTIFICATION_ID, builder.build());
            } catch (SecurityException ignored) {
                // Android 13+ bildirim izni verilmediyse sessizce bekle.
            }
        }
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Günlük Haber Bildirimi",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Seçtiğin haber alanları için günlük doğrudan haber hatırlatması");
        manager.createNotificationChannel(channel);
    }
}
