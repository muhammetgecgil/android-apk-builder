package com.mg.hafizadostum.v4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            MemoryStore.ensureDefaults(context);
            ReminderScheduler.scheduleAll(context);
        }
    }
}
