package com.muhammetgecgil.turkradyo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SleepReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, RadioService.class));
    }
}
