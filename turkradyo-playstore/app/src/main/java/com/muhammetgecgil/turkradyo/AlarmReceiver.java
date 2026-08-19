package com.muhammetgecgil.turkradyo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Intent play = new Intent(context, RadioService.class).setAction(RadioService.ACTION_PLAY);
        play.putExtra("url", intent.getStringExtra("url"));
        play.putExtra("name", intent.getStringExtra("name"));
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(play);
        else context.startService(play);
    }
}
