package com.mg.hafizadostum.v4;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public final class ReminderScheduler {
    public static final String ACTION_REMIND = "com.mg.hafizadostum.v4.REMIND";
    public static final String ACTION_DONE = "com.mg.hafizadostum.v4.DONE";
    public static final String ACTION_SNOOZE = "com.mg.hafizadostum.v4.SNOOZE";

    private ReminderScheduler() {}

    public static void scheduleAll(Context c) {
        JSONArray a = MemoryStore.getTasks(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t != null && t.optBoolean("active", true)) scheduleTask(c, t);
        }
    }

    public static void scheduleTask(Context c, JSONObject t) {
        if (t == null || !t.optBoolean("active", true)) return;
        long when = nextTime(t);
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_REMIND);
        i.putExtra("taskId", t.optString("id"));
        PendingIntent pi = PendingIntent.getBroadcast(c, requestCode(t.optString("id")), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        else am.set(AlarmManager.RTC_WAKEUP, when, pi);
    }

    public static void snooze(Context c, String taskId, int minutes) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_REMIND);
        i.putExtra("taskId", taskId);
        PendingIntent pi = PendingIntent.getBroadcast(c, requestCode(taskId), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long when = System.currentTimeMillis() + minutes * 60_000L;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        else am.set(AlarmManager.RTC_WAKEUP, when, pi);
    }

    private static long nextTime(JSONObject t) {
        Calendar now = Calendar.getInstance();
        String days = t.optString("days", "1234567");
        for (int add = 0; add <= 7; add++) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, add);
            c.set(Calendar.HOUR_OF_DAY, t.optInt("hour", 9));
            c.set(Calendar.MINUTE, t.optInt("minute", 0));
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            int dow = c.get(Calendar.DAY_OF_WEEK);
            int mondayBased = ((dow + 5) % 7) + 1;
            if (!days.contains(String.valueOf(mondayBased))) continue;
            if (c.getTimeInMillis() > now.getTimeInMillis() + 10_000L) return c.getTimeInMillis();
        }
        return System.currentTimeMillis() + 24L * 60L * 60L * 1000L;
    }

    private static int requestCode(String id) {
        return 10_000 + Math.abs(id == null ? 0 : id.hashCode() % 20_000);
    }
}
