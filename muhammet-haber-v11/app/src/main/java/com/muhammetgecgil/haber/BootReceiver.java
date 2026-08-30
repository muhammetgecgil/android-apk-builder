package com.muhammetgecgil.haber;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        AiNewsReceiver.schedule(context);
    }
}
