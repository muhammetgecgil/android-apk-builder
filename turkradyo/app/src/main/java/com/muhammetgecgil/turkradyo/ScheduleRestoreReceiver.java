package com.muhammetgecgil.turkradyo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class ScheduleRestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent in){if(in!=null)RadioSchedule.restore(c,in.getAction());}
}
