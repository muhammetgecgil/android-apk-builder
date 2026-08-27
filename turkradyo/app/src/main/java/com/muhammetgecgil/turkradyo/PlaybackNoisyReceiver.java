package com.muhammetgecgil.turkradyo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

/** Prevents radio audio from unexpectedly jumping to the phone speaker when a wired/Bluetooth route disappears. */
public class PlaybackNoisyReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) return;
        try {
            context.getSharedPreferences("radio", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("lastNoisyPause", System.currentTimeMillis())
                    .putString("lastPauseReason", "audio_route_lost")
                    .apply();
            Intent pause = new Intent(context, RadioService.class).setAction(RadioService.ACTION_PAUSE);
            context.startService(pause);
        } catch (Exception ignored) {}
    }
}
