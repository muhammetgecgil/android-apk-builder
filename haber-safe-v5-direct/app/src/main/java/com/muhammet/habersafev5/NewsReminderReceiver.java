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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class NewsReminderReceiver extends BroadcastReceiver {
    public static final String PREFS = "haber_safe_reminders";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_SELECTED = "selected_categories";
    public static final String KEY_HOUR = "hour";
    public static final String KEY_MINUTE = "minute";
    public static final String KEY_ONLY_NEW = "only_new";
    private static final String KEY_SEEN = "seen_links_v6";
    private static final String KEY_LAST_RUN = "last_notification_run";
    private static final String CHANNEL_ID = "daily_news_v2";
    private static final int NOTIFICATION_ID = 6101;
    private static final int ALARM_REQUEST = 6102;

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
                sendDailyNewsNotification(appContext, false);
            } finally {
                scheduleNext(appContext);
                pendingResult.finish();
            }
        }, "haber-safe-daily-v6").start();
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
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 1);

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) alarmManager.cancel(alarmPendingIntent(context));
    }

    public static void sendNow(Context context) {
        Context appContext = context.getApplicationContext();
        new Thread(() -> sendDailyNewsNotification(appContext, true), "haber-safe-test-v6").start();
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

    private static void sendDailyNewsNotification(Context context, boolean force) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> selected = new HashSet<>(prefs.getStringSet(KEY_SELECTED, new HashSet<>()));
        if (selected.isEmpty()) return;

        List<NewsRepository.NewsItem> latest = new ArrayList<>();
        String cacheKey = NewsRepository.selectionCacheKey(selected);
        try {
            latest = NewsRepository.fetchSelectedCategories(selected, 7, 28);
            AppStore.saveCache(context, cacheKey, latest);
        } catch (Exception ignored) {
            latest = AppStore.loadCache(context, cacheKey);
        }

        Set<String> seen = new HashSet<>(prefs.getStringSet(KEY_SEEN, new HashSet<>()));
        int newCount = 0;
        HashSet<String> updatedSeen = new HashSet<>(seen);
        for (NewsRepository.NewsItem item : latest) {
            if (!seen.contains(item.link)) newCount++;
            updatedSeen.add(item.link);
        }
        if (updatedSeen.size() > 800) {
            updatedSeen.clear();
            for (NewsRepository.NewsItem item : latest) updatedSeen.add(item.link);
        }
        prefs.edit()
                .putStringSet(KEY_SEEN, updatedSeen)
                .putLong(KEY_LAST_RUN, System.currentTimeMillis())
                .apply();

        boolean onlyNew = prefs.getBoolean(KEY_ONLY_NEW, false);
        if (!force && onlyNew && newCount == 0) return;

        createChannel(context);

        Intent open = new Intent(context, MainActivity.class);
        open.putExtra("open_my_news", true);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                6103,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String categoryText = categoryText(selected);
        String title;
        if (force) title = "Haber SAFE • Bildirim testi";
        else if (newCount > 0) title = "Haber SAFE • " + newCount + " yeni haber";
        else title = "Haber SAFE • Günlük haberlerin hazır";

        Notification.InboxStyle style = new Notification.InboxStyle();
        int shown = Math.min(5, latest.size());
        for (int i = 0; i < shown; i++) style.addLine(latest.get(i).title);
        if (latest.isEmpty()) style.addLine("Seçtiğin alanlardaki haberleri okumak için dokun.");
        style.setSummaryText(categoryText);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        builder.setSmallIcon(R.drawable.ic_haber_safe_v5)
                .setContentTitle(title)
                .setContentText(latest.isEmpty() ? categoryText : latest.get(0).title)
                .setSubText("Doğrudan kaynak")
                .setStyle(style)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setCategory(Notification.CATEGORY_RECOMMENDATION);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            try { manager.notify(NOTIFICATION_ID, builder.build()); }
            catch (SecurityException ignored) {}
        }
    }

    private static String categoryText(Set<String> selected) {
        LinkedHashMap<String, String> all = NewsRepository.categories();
        ArrayList<String> ordered = new ArrayList<>();
        for (String name : all.keySet()) if (selected.contains(name)) ordered.add(name);
        return TextUtils.join(" • ", ordered);
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Haber SAFE Günlük",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Seçtiğin alanlar için günlük doğrudan haber bildirimi");
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }
}
