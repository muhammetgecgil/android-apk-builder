package com.muhammetgecgil.turkradyo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent in){
        if(in==null)return;
        if(RadioSchedule.SLEEP.equals(in.getAction())){RadioSchedule.deliverSleep(c,in);return;}
        if(!RadioSchedule.WAKE.equals(in.getAction()))return;
        Intent play=RadioSchedule.consumeWake(c,in);if(play==null)return;
        try{c.startForegroundService(play);}
        catch(RuntimeException e){RadioSchedule.prefs(c).edit().putString("wakeError","Android yayını başlatamadı. Alarm ve pil ayarlarını kontrol et.").apply();}
    }
}
