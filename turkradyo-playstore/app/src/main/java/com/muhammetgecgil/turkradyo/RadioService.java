package com.muhammetgecgil.turkradyo;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.audiofx.LoudnessEnhancer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.*;

public class RadioService extends Service {
    public static final String ACTION_PLAY = "turkradyo.PLAY";
    public static final String ACTION_PAUSE = "turkradyo.PAUSE";
    public static final String ACTION_RESUME = "turkradyo.RESUME";
    public static final String ACTION_STOP = "turkradyo.STOP";
    public static final String ACTION_VOLUME = "turkradyo.VOLUME";
    public static final String ACTION_GAIN = "turkradyo.GAIN";
    private static final String CHANNEL = "turkradyo_media";
    private static final int NOTIFY_ID = 2201;

    private MediaPlayer player;
    private MediaSession session;
    private LoudnessEnhancer enhancer;
    private float volume = 1f;
    private int gainMb = 0;
    private String station = "Türk Radyo";
    private String url = "";
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        session = new MediaSession(this, "MuhammetTurkRadyo");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { resume(); }
            @Override public void onPause() { pause(); }
            @Override public void onStop() { stopSelfSafely(); }
        });
        session.setActive(true);
        updateSession(PlaybackState.STATE_NONE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_STICKY;
        String a = intent.getAction();
        if (ACTION_PLAY.equals(a)) {
            station = safe(intent.getStringExtra("name"), "Türk Radyo");
            url = safe(intent.getStringExtra("url"), "");
            getSharedPreferences("radio", MODE_PRIVATE).edit().putString("last_name", station).putString("last_url", url).apply();
            startForeground(NOTIFY_ID, buildNotification("Bağlanıyor…", true));
            play(url);
        } else if (ACTION_PAUSE.equals(a)) pause();
        else if (ACTION_RESUME.equals(a)) resume();
        else if (ACTION_STOP.equals(a)) stopSelfSafely();
        else if (ACTION_VOLUME.equals(a)) {
            volume = clamp(intent.getFloatExtra("volume", 1f));
            if (player != null) player.setVolume(volume, volume);
        } else if (ACTION_GAIN.equals(a)) {
            gainMb = Math.max(0, Math.min(1200, intent.getIntExtra("gain_mb", 0)));
            applyEnhancer();
        }
        return START_STICKY;
    }

    private void play(String streamUrl) {
        releasePlayer();
        if (streamUrl == null || streamUrl.trim().isEmpty()) { stopSelfSafely(); return; }
        requestFocus();
        player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setVolume(volume, volume);
        player.setOnPreparedListener(mp -> {
            applyEnhancer();
            mp.start();
            updateSession(PlaybackState.STATE_PLAYING);
            notifyState("Canlı yayın", false);
        });
        player.setOnErrorListener((mp, what, extra) -> {
            updateSession(PlaybackState.STATE_ERROR);
            notifyState("Yayın bağlantısı kesildi", false);
            return true;
        });
        try {
            player.setDataSource(this, Uri.parse(streamUrl));
            player.prepareAsync();
        } catch (Exception e) {
            updateSession(PlaybackState.STATE_ERROR);
            notifyState("Yayın açılamadı", false);
        }
    }

    private void pause() {
        try { if (player != null && player.isPlaying()) player.pause(); } catch (Exception ignored) { }
        updateSession(PlaybackState.STATE_PAUSED);
        notifyState("Duraklatıldı", false);
    }

    private void resume() {
        try { if (player != null) { player.start(); updateSession(PlaybackState.STATE_PLAYING); notifyState("Canlı yayın", false); } } catch (Exception ignored) { }
    }

    private void stopSelfSafely() {
        releasePlayer();
        updateSession(PlaybackState.STATE_STOPPED);
        abandonFocus();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void releasePlayer() {
        if (enhancer != null) { try { enhancer.release(); } catch (Exception ignored) {} enhancer = null; }
        if (player != null) { try { player.stop(); } catch (Exception ignored) {} try { player.release(); } catch (Exception ignored) {} player = null; }
    }

    private void applyEnhancer() {
        if (player == null) return;
        try {
            if (enhancer != null) enhancer.release();
            enhancer = new LoudnessEnhancer(player.getAudioSessionId());
            enhancer.setTargetGain(gainMb);
            enhancer.setEnabled(gainMb > 0);
        } catch (Exception ignored) { enhancer = null; }
    }

    private void updateSession(int state) {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_PLAY_PAUSE;
        session.setPlaybackState(new PlaybackState.Builder().setActions(actions).setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f).build());
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, station).putString(MediaMetadata.METADATA_KEY_ARTIST, "Muhammet Türk Radyo").build());
    }

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Arka planda canlı radyo oynatma");
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification(String text, boolean connecting) {
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pause = servicePending(ACTION_PAUSE, 2);
        PendingIntent resume = servicePending(ACTION_RESUME, 3);
        PendingIntent stop = servicePending(ACTION_STOP, 4);
        Notification.Action playPause = new Notification.Action.Builder(android.R.drawable.ic_media_pause, "Duraklat", pause).build();
        if (player == null || (!connecting && !isPlaying())) playPause = new Notification.Action.Builder(android.R.drawable.ic_media_play, "Oynat", resume).build();
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_radio)
                .setContentTitle(station)
                .setContentText(text)
                .setContentIntent(content)
                .setOngoing(isPlaying() || connecting)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(playPause)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Durdur", stop).build())
                .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1))
                .build();
    }

    private PendingIntent servicePending(String action, int req) {
        Intent i = new Intent(this, RadioService.class).setAction(action);
        return PendingIntent.getService(this, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void notifyState(String text, boolean connecting) {
        getSystemService(NotificationManager.class).notify(NOTIFY_ID, buildNotification(text, connecting));
    }

    private boolean isPlaying() { try { return player != null && player.isPlaying(); } catch (Exception e) { return false; } }

    private void requestFocus() {
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setOnAudioFocusChangeListener(this::onFocusChange).build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(this::onFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void onFocusChange(int change) {
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause();
        else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK && player != null) player.setVolume(0.25f, 0.25f);
        else if (change == AudioManager.AUDIOFOCUS_GAIN && player != null) player.setVolume(volume, volume);
    }

    private void abandonFocus() {
        if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) audioManager.abandonAudioFocusRequest(focusRequest);
    }

    private static String safe(String s, String d) { return s == null || s.trim().isEmpty() ? d : s; }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override public void onDestroy() { releasePlayer(); if (session != null) session.release(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
