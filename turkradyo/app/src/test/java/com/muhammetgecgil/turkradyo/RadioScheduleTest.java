package com.muhammetgecgil.turkradyo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.util.ReflectionHelpers;
import java.util.Calendar;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@LooperMode(LooperMode.Mode.PAUSED)
public class RadioScheduleTest {
    Context app;SharedPreferences p;AlarmManager alarms;
    @Before public void reset(){app=RuntimeEnvironment.getApplication();p=RadioSchedule.prefs(app);p.edit().clear().commit();RadioService.isRunning=false;alarms=(AlarmManager)app.getSystemService(Context.ALARM_SERVICE);}
    private Intent due(boolean daily){
        RadioSchedule.setWake(app,6,42,"https://stream.test/wake","Alarm radyosu",daily);
        long when=System.currentTimeMillis()-1000;p.edit().putLong("wakeWhen",when).commit();
        return new Intent(app,AlarmReceiver.class).setAction(RadioSchedule.WAKE).putExtra("when",when);
    }
    @Test public void remembersLastTimeAndStationAcrossCancelAndReopen() throws Exception {
        JSONObject set=new JSONObject(RadioSchedule.setWake(app,8,35,"https://stream.test/a","TRT FM",false));
        assertTrue(set.getBoolean("scheduled"));assertEquals("08:35",set.getString("time"));
        assertEquals(set.getLong("when"),alarms.getNextAlarmClock().getTriggerTime());
        RadioSchedule.cancelWake(app);
        JSONObject reopened=new JSONObject(RadioSchedule.wakeJson(app));
        assertEquals("08:35",reopened.getString("time"));assertEquals("TRT FM",reopened.getString("name"));assertFalse(reopened.getBoolean("enabled"));assertNull(alarms.getNextAlarmClock());
    }
    @Test public void coldAlarmStartsForegroundPlaybackAndClearsEveningTimer() throws Exception {
        Intent fire=due(false);RadioSchedule.setSleep(app,System.currentTimeMillis()+60_000,true);
        PlaybackGuardian.manualPause(app);
        new AlarmReceiver().onReceive(app,fire);
        Intent started=shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService();
        assertNotNull(started);assertEquals(RadioService.ACTION_PLAY,started.getAction());assertEquals("https://stream.test/wake",started.getStringExtra("url"));
        assertEquals(0,p.getLong("sleepDeadline",0));assertEquals("fired",new JSONObject(RadioSchedule.wakeJson(app)).getString("status"));
        new AlarmReceiver().onReceive(app,fire);assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    @Test public void alarmPlayOverridesPausedPlaybackWithoutAnActivity(){
        Intent play=RadioSchedule.consumeWake(app,due(false));
        ServiceController<RadioService> controller=Robolectric.buildService(RadioService.class).create();RadioService service=controller.get();Handler worker=ReflectionHelpers.getField(service,"handler");
        try{assertTrue((boolean)ReflectionHelpers.getField(service,"userPaused"));service.onStartCommand(play,0,1);shadowOf(worker.getLooper()).idle();assertFalse((boolean)ReflectionHelpers.getField(service,"userPaused"));assertEquals("https://stream.test/wake",ReflectionHelpers.getField(service,"primaryUrl"));}
        finally{controller.destroy();shadowOf(worker.getLooper()).idle();}
    }
    @Test public void replacedAndCancelledAlarmsCannotWakePlayback(){
        Intent old=due(false);RadioSchedule.setWake(app,9,12,"https://stream.test/new","Yeni radyo",false);
        assertNull(RadioSchedule.consumeWake(app,old));
        Intent current=due(false);RadioSchedule.cancelWake(app);assertNull(RadioSchedule.consumeWake(app,current));
    }
    @Test public void dailyWakeRearmsLastTimeAndIgnoresDuplicateDelivery() throws Exception {
        Intent fire=due(true);assertNotNull(RadioSchedule.consumeWake(app,fire));JSONObject next=new JSONObject(RadioSchedule.wakeJson(app));
        assertTrue(next.getBoolean("scheduled"));assertTrue(next.getLong("when")>System.currentTimeMillis());assertEquals("06:42",next.getString("time"));assertNull(RadioSchedule.consumeWake(app,fire));
    }
    @Test @Config(sdk=31) public void deniedExactPermissionDoesNotClaimAlarmIsArmedAndGrantRestoresIt() throws Exception {
        shadowOf(alarms).setCanScheduleExactAlarms(false);
        JSONObject denied=new JSONObject(RadioSchedule.setWake(app,7,45,"https://stream.test/a","Radyo",false));
        assertFalse(denied.getBoolean("scheduled"));assertTrue(denied.getBoolean("enabled"));assertEquals("permission",denied.getString("status"));assertNull(alarms.getNextAlarmClock());
        shadowOf(alarms).setCanScheduleExactAlarms(true);new ScheduleRestoreReceiver().onReceive(app,new Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED));
        assertTrue(new JSONObject(RadioSchedule.wakeJson(app)).getBoolean("scheduled"));assertNotNull(alarms.getNextAlarmClock());
    }
    @Test public void bootReschedulesFutureWakeWithoutStartingMedia(){
        RadioSchedule.setWake(app,7,15,"https://stream.test/a","Radyo",false);long when=p.getLong("wakeWhen",0);
        PendingIntent intent=PendingIntent.getBroadcast(app,8201,new Intent(app,AlarmReceiver.class).setAction(RadioSchedule.WAKE),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);alarms.cancel(intent);
        assertNull(alarms.getNextAlarmClock());new ScheduleRestoreReceiver().onReceive(app,new Intent(Intent.ACTION_BOOT_COMPLETED));
        assertEquals(when,alarms.getNextAlarmClock().getTriggerTime());assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    @Test public void missedOneShotStaysOffAfterRebootButRetainsTime() throws Exception {
        due(false);new ScheduleRestoreReceiver().onReceive(app,new Intent(Intent.ACTION_BOOT_COMPLETED));JSONObject state=new JSONObject(RadioSchedule.wakeJson(app));
        assertFalse(state.getBoolean("enabled"));assertEquals("missed",state.getString("status"));assertEquals("06:42",state.getString("time"));assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    @Test public void pastClockTimeRollsToTomorrow(){
        Calendar now=Calendar.getInstance();now.set(2026,8,21,23,59,30);now.set(Calendar.MILLISECOND,0);
        Calendar next=Calendar.getInstance();next.setTimeInMillis(RadioSchedule.nextTime(0,10,now.getTimeInMillis()));
        assertEquals(22,next.get(Calendar.DAY_OF_MONTH));assertEquals(0,next.get(Calendar.HOUR_OF_DAY));assertEquals(10,next.get(Calendar.MINUTE));
    }
    @Test public void sleepUsesMonotonicTimeAndRejectsOldOrCancelledDelivery() throws Exception {
        RadioSchedule.setSleep(app,System.currentTimeMillis()+60_000,true);long old=p.getLong("sleepDeadline",0);
        // A wall-clock adjustment cannot shorten a duration timer.
        p.edit().putLong("sleepDeadline",System.currentTimeMillis()-3600_000).commit();assertTrue(SleepTimer.remaining(p)>50_000);
        RadioSchedule.restore(app,Intent.ACTION_TIME_CHANGED);assertTrue(p.getLong("sleepDeadline",0)>System.currentTimeMillis()+50_000);
        RadioSchedule.setSleep(app,System.currentTimeMillis()+120_000,false);long current=p.getLong("sleepDeadline",0);
        new AlarmReceiver().onReceive(app,new Intent().setAction(RadioSchedule.SLEEP).putExtra("when",old));assertEquals(current,p.getLong("sleepDeadline",0));
        RadioSchedule.cancelSleep(app,true);new AlarmReceiver().onReceive(app,new Intent().setAction(RadioSchedule.SLEEP).putExtra("when",current));
        assertEquals(0,new JSONObject(RadioSchedule.sleepJson(app)).getLong("when"));assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    @Test public void sleepReceiverStopsAnActiveServiceButCannotStopALaterTimer(){
        ServiceController<RadioService> controller=Robolectric.buildService(RadioService.class).create();RadioService service=controller.get();Handler worker=ReflectionHelpers.getField(service,"handler");
        try{
            // Drain startup work before arranging a receiver delivery; otherwise the
            // service's independent deadline check can legitimately consume it first.
            shadowOf(worker.getLooper()).idle();
            long deadline=System.currentTimeMillis()-1;p.edit().putLong("sleepDeadline",deadline).putLong("sleepElapsedDeadline",SystemClock.elapsedRealtime()-1).commit();
            new AlarmReceiver().onReceive(app,new Intent().setAction(RadioSchedule.SLEEP).putExtra("when",deadline));Intent check=shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService();assertNotNull("running="+RadioService.isRunning+", remaining="+SleepTimer.remaining(p)+", deadline="+p.getLong("sleepDeadline",0),check);
            // This queued delivery arrives after the user replaced the timer.
            RadioSchedule.setSleep(app,System.currentTimeMillis()+120_000,true);service.onStartCommand(check,0,1);shadowOf(worker.getLooper()).idle();assertTrue(SleepTimer.remaining(p)>0);
            p.edit().putLong("sleepElapsedDeadline",SystemClock.elapsedRealtime()-1).commit();service.onStartCommand(check,0,2);shadowOf(worker.getLooper()).idle();assertEquals(0,p.getLong("sleepDeadline",0));
        }finally{controller.destroy();shadowOf(worker.getLooper()).idle();}
    }
}
