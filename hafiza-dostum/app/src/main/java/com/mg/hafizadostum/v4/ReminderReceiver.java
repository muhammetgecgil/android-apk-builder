package com.mg.hafizadostum.v4;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import org.json.JSONObject;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "hafiza_hatirlatmalar";

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String id = intent.getStringExtra("taskId");
        if (id == null) return;
        JSONObject task = MemoryStore.findTaskById(context, id);
        if (task == null) return;

        if (ReminderScheduler.ACTION_DONE.equals(action)) {
            long last = task.optLong("lastDone", 0L);
            if (task.optBoolean("critical") && last > 0L && System.currentTimeMillis() - last < 30L * 60L * 1000L) {
                cancel(context, id);
                ReminderScheduler.scheduleTask(context, task);
                Toast.makeText(context,
                        "Bu kritik rutin az önce kaydedilmiş. İkinci kez işaretlenmedi.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            MemoryStore.markDone(context, id, "notification");
            cancel(context, id);
            JSONObject fresh = MemoryStore.findTaskById(context, id);
            ReminderScheduler.scheduleTask(context, fresh == null ? task : fresh);
            Toast.makeText(context, "Kaydedildi: " + task.optString("name"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (ReminderScheduler.ACTION_SNOOZE.equals(action)) {
            cancel(context, id);
            ReminderScheduler.snooze(context, id, 10);
            Toast.makeText(context, "10 dakika sonra tekrar hatırlatacağım.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (MemoryStore.doneToday(task)) {
            ReminderScheduler.scheduleTask(context, task);
            return;
        }
        show(context, task);
        ReminderScheduler.scheduleTask(context, task);
    }

    private void show(Context c, JSONObject task) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Hafıza Dostum hatırlatmaları", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Planlı rutin ve kritik görev hatırlatmaları");
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }

        String id = task.optString("id");
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(c, Math.abs(id.hashCode()), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent done = new Intent(c, ReminderReceiver.class).setAction(ReminderScheduler.ACTION_DONE).putExtra("taskId", id);
        PendingIntent donePi = PendingIntent.getBroadcast(c, 30000 + Math.abs(id.hashCode() % 10000), done, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snooze = new Intent(c, ReminderReceiver.class).setAction(ReminderScheduler.ACTION_SNOOZE).putExtra("taskId", id);
        PendingIntent snoozePi = PendingIntent.getBroadcast(c, 40000 + Math.abs(id.hashCode() % 10000), snooze, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = task.optBoolean("critical") ? "⚠ Unutma: kritik rutin" : "Hafıza Dostum";
        String text = task.optString("name");
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text + "\nYaptıysan tek dokunuşla tarih-saat kaydı oluştur."))
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(new Notification.Action.Builder(android.R.drawable.checkbox_on_background, "YAPTIM", donePi).build())
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_lock_idle_alarm, "10 DK SONRA", snoozePi).build());
        nm.notify(notificationId(id), b.build());
    }

    private void cancel(Context c, String id) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notificationId(id));
    }

    private int notificationId(String id) {
        return 5000 + Math.abs(id.hashCode() % 20000);
    }
}
