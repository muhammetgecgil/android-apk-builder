package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@LooperMode(LooperMode.Mode.PAUSED)
public class PlaybackStateTest {
    @Test public void sleepFadePreservesUserVolumeAndOnlyFadesTheLastMinute() {
        assertEquals(.8f,SleepTimer.effectiveVolume(.8f,100_000,true,20_000),.001f);
        assertEquals(.4f,SleepTimer.effectiveVolume(.8f,100_000,true,70_000),.001f);
        assertEquals(0f,SleepTimer.effectiveVolume(.8f,100_000,true,100_001),.001f);
        assertEquals(.8f,SleepTimer.effectiveVolume(.8f,0,false,100_001),.001f);
    }

    @Test public void sleepDeadlineStopsWithoutAnOpenActivity() throws Exception {
        ServiceController<RadioService> controller=Robolectric.buildService(RadioService.class).create();
        RadioService service=controller.get();
        Handler worker=ReflectionHelpers.getField(service,"handler");
        service.getSharedPreferences("radio",0).edit().putLong("sleepDeadline",System.currentTimeMillis()+60_000).putBoolean("sleepFade",true).commit();
        ReflectionHelpers.setField(service,"userPaused",false);
        service.onStartCommand(new Intent(service,RadioService.class).setAction(RadioService.ACTION_SLEEP_TIMER),0,1);
        shadowOf(worker.getLooper()).idle();
        // Handler uptime is virtual in Robolectric; the persisted deadline uses wall time.
        // Simulate the next dispatch arriving after that deadline without a real sleep.
        service.getSharedPreferences("radio",0).edit().putLong("sleepDeadline",System.currentTimeMillis()-1000).commit();
        shadowOf(worker.getLooper()).idleFor(java.time.Duration.ofSeconds(2));
        assertEquals(0L,service.getSharedPreferences("radio",0).getLong("sleepDeadline",0));
        JSONObject state=new JSONObject(service.getSharedPreferences("radio",0).getString("telemetry","{}"));
        assertTrue(state.getBoolean("manualPause"));
        controller.destroy();shadowOf(worker.getLooper()).idle();
    }

    @Test public void expiredSleepAlarmDoesNotStartAnIdlePlaybackService() {
        Context app=RuntimeEnvironment.getApplication();
        RadioService.isRunning=false;
        app.getSharedPreferences("radio",0).edit().putLong("sleepDeadline",1).commit();
        new AlarmReceiver().onReceive(app,new Intent().setAction(RadioSchedule.SLEEP).putExtra("when",1L));
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        assertEquals(0L,app.getSharedPreferences("radio",0).getLong("sleepDeadline",0));
    }

    @Test public void voiceSearchPrefersExactNamesAndHandlesTurkishSpelling() throws Exception {
        org.json.JSONArray queue=new org.json.JSONArray("[{\"name\":\"Power Türk Akustik\",\"url\":\"https://a.test\"},{\"name\":\"Power Türk\",\"url\":\"https://b.test\"},{\"name\":\"İstanbul\",\"url\":\"https://c.test\"}]");
        assertEquals(1,StationSearch.find(queue,"  POWER TURK  "));
        assertEquals(2,StationSearch.find(queue,"istanbul"));
        assertEquals(0,StationSearch.find(queue,"akustik"));
    }

    @Test public void voiceSearchRejectsUnknownNamesAndStationsWithoutStreams() throws Exception {
        org.json.JSONArray queue=new org.json.JSONArray("[{\"name\":\"TRT FM\"},null]");
        assertEquals(-1,StationSearch.find(queue,"TRT FM"));
        assertEquals(-1,StationSearch.find(queue,""));
        assertEquals(-1,StationSearch.find(queue,"bilinmeyen"));
    }

    @Test public void pausePublishesFreshStateAndCancelsPendingRecovery() throws Exception {
        ServiceController<RadioService> controller=Robolectric.buildService(RadioService.class).create();
        RadioService service=controller.get();
        Handler worker=ReflectionHelpers.getField(service,"handler");
        assertNotSame(Looper.getMainLooper(),worker.getLooper());
        java.util.concurrent.FutureTask<Void> repair=new java.util.concurrent.FutureTask<>(()->null);
        ReflectionHelpers.setField(service,"repairFuture",repair);
        service.getSharedPreferences("radio",0).edit().putString("telemetry","{\"isPlaying\":true,\"buffering\":true}").commit();
        service.onStartCommand(new Intent(service,RadioService.class).setAction(RadioService.ACTION_PAUSE),0,1);
        shadowOf(worker.getLooper()).idle();
        JSONObject state=new JSONObject(service.getSharedPreferences("radio",0).getString("telemetry","{}"));
        assertFalse(state.getBoolean("isPlaying"));
        assertFalse(state.getBoolean("buffering"));
        assertTrue(state.getBoolean("manualPause"));
        assertTrue(state.getBoolean("nativeRecovery"));
        assertTrue(repair.isCancelled());
        android.media.session.MediaSession session=ReflectionHelpers.getField(service,"mediaSession");
        assertVoiceSearchPublished(session);
        controller.destroy();
        shadowOf(worker.getLooper()).idle();
    }

    @Test public void gainAndEqSurviveServiceRecreation() {
        ServiceController<RadioService> controller=Robolectric.buildService(RadioService.class).create();
        RadioService service=controller.get();
        Handler worker=ReflectionHelpers.getField(service,"handler");
        service.onStartCommand(new Intent(service,RadioService.class).setAction(RadioService.ACTION_GAIN).putExtra("gain",450),0,1);
        service.onStartCommand(new Intent(service,RadioService.class).setAction(RadioService.ACTION_EQ).putExtra("band",2).putExtra("level",600),0,2);
        shadowOf(worker.getLooper()).idle();
        controller.destroy();shadowOf(worker.getLooper()).idle();
        controller=Robolectric.buildService(RadioService.class).create();service=controller.get();
        assertEquals(450,(int)ReflectionHelpers.getField(service,"gainMb"));
        short[] levels=ReflectionHelpers.getField(service,"eqLevels");assertEquals(600,levels[2]);
        worker=ReflectionHelpers.getField(service,"handler");
        controller.destroy();shadowOf(worker.getLooper()).idle();
    }

    @Test public void carPauseWinsOverStalePlayingTelemetry() {
        Context app=RuntimeEnvironment.getApplication();
        app.getSharedPreferences("radio",0).edit().putString("telemetry","{\"isPlaying\":true}").commit();
        PlaybackGuardian.manualPause(app);
        ServiceController<AutoMediaService> controller=Robolectric.buildService(AutoMediaService.class).create();
        int state=ReflectionHelpers.getField(controller.get(),"lastState");
        assertEquals(android.media.session.PlaybackState.STATE_PAUSED,state);
        android.media.session.MediaSession session=ReflectionHelpers.getField(controller.get(),"session");
        assertVoiceSearchPublished(session);
        controller.destroy();
    }

    private static void assertVoiceSearchPublished(android.media.session.MediaSession session) {
        // Robolectric does not relay session state through the system media Binder.
        // Inspect the state retained by the real API 28 MediaSession setter instead.
        android.media.session.PlaybackState state=ReflectionHelpers.getField(session,"mPlaybackState");
        assertNotNull(state);
        assertTrue((state.getActions()&android.media.session.PlaybackState.ACTION_PLAY_FROM_SEARCH)!=0);
    }

    @Test public void healthyPlaybackDoesNotConsumeAutoResumeBudget() throws Exception {
        Context app=RuntimeEnvironment.getApplication();
        PlaybackGuardian.manualPlay(app);
        PlaybackGuardian.interrupted(app,"network");
        PlaybackGuardian.recoveredIfInterrupted(app);
        PlaybackGuardian.recoveredIfInterrupted(app);
        JSONObject status=new JSONObject(PlaybackGuardian.statusJson(app));
        assertEquals(1,status.getInt("recoveries"));
        assertEquals(0,status.getInt("windowAttempts"));
        assertFalse(status.getBoolean("interrupted"));
        PlaybackGuardian.manualPause(app);
        PlaybackGuardian.interrupted(app,"network");
        assertFalse(PlaybackGuardian.mayAutoResume(app));
    }
}
