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
        controller.destroy();
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
