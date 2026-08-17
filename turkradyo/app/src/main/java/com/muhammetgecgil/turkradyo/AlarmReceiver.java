package com.muhammetgecgil.turkradyo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra("sleep", false)) {
            context.startService(new Intent(context, RadioService.class).setAction(RadioService.ACTION_STOP));
            return;
        }
        String url = intent.getStringExtra("url");
        String name = intent.getStringExtra("name");
        if (url == null || url.isEmpty()) return;
        Intent service = new Intent(context, RadioService.class)
                .setAction(RadioService.ACTION_PLAY)
                .putExtra("url", url)
                .putExtra("name", name);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service); else context.startService(service);
    }
}
