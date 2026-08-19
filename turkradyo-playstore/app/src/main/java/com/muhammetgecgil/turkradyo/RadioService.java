package com.muhammetgecgil.turkradyo;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.audiofx.LoudnessEnhancer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class RadioService extends Service {
    public static final String ACTION_PLAY = "turkradyo.PLAY";
    public static final String ACTION_PAUSE = "turkradyo.PAUSE";
    public static final String ACTION_RESUME = "turkradyo.RESUME";
    public static final String ACTION_STOP = "turkradyo.STOP";
    public static final String ACTION_PREVIOUS = "turkradyo.PREVIOUS";
    public static final String ACTION_NEXT = "turkradyo.NEXT";
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
            @Override public void onPlay() { resumeOrPlayLast(); }
            @Override public void onPlayFromSearch(String query, Bundle extras) { resumeOrPlayLast(); }
            @Override public void onPlayFromMediaId(String mediaId, Bundle extras) { resumeOrPlayLast(); }
            @Override public void onPause() { pause(); }
            @Override public void onStop() { stopSelfSafely(); }
            @Override public void onSkipToPrevious() { cycleStation(-1); }
            @Override public void onSkipToNext() { cycleStation(1); }
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
            syncQueueIndexToCurrent();
            startForeground(NOTIFY_ID, buildNotification("Bağlanıyor…", true));
            play(url);
        } else if (ACTION_PAUSE.equals(a)) pause();
        else if (ACTION_RESUME.equals(a)) resumeOrPlayLast();
        else if (ACTION_STOP.equals(a)) stopSelfSafely();
        else if (ACTION_PREVIOUS.equals(a)) cycleStation(-1);
        else if (ACTION_NEXT.equals(a)) cycleStation(1);
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

    private void cycleStation(int direction) {
        try {
            android.content.SharedPreferences p = getSharedPreferences("radio", MODE_PRIVATE);
            String raw = p.getString("queue_json", "");
            if (raw == null || raw.trim().isEmpty()) return;
            JSONObject root = new JSONObject(raw);
            JSONArray list = root.optJSONArray("s");
            if (list == null || list.length() == 0) return;
            int n = list.length();
            int idx = p.getInt("queue_index", root.optInt("i", 0));
            for (int tries = 0; tries < n; tries++) {
                idx = (idx + direction + n) % n;
                JSONObject item = list.optJSONObject(idx);
                if (item == null) continue;
                String nextUrl = safe(item.optString("url", ""), "");
                if (nextUrl.isEmpty()) continue;
                String nextName = safe(item.optString("name", "Türk Radyo"), "Türk Radyo");
                station = nextName;
                url = nextUrl;
                p.edit().putInt("queue_index", idx).putString("last_name", station).putString("last_url", url).apply();
                startForeground(NOTIFY_ID, buildNotification("Bağlanıyor…", true));
                play(url);
                sendBroadcast(new Intent("com.muhammetgecgil.turkradyo.STATION_CHANGED").setPackage(getPackageName()).putExtra("index", idx).putExtra("name", station));
                return;
            }
        } catch (Exception ignored) { }
    }

    private void syncQueueIndexToCurrent() {
        try {
            android.content.SharedPreferences p = getSharedPreferences("radio", MODE_PRIVATE);
            String raw = p.getString("queue_json", "");
            if (raw == null || raw.isEmpty()) return;
            JSONObject root = new JSONObject(raw);
            JSONArray list = root.optJSONArray("s");
            if (list == null) return;
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                if (item == null) continue;
                if (url.equals(item.optString("url", "")) || station.equals(item.optString("name", ""))) {
                    p.edit().putInt("queue_index", i).apply();
                    return;
                }
            }
        } catch (Exception ignored) { }
    }

    private void pause() {
        try { if (player != null && player.isPlaying()) player.pause(); } catch (Exception ignored) { }
        updateSession(PlaybackState.STATE_PAUSED);
        notifyState("Duraklatıldı", false);
    }

    private void resumeOrPlayLast() {
        try {
            if (player != null) {
                player.start();
                updateSession(PlaybackState.STATE_PLAYING);
                notifyState("Canlı yayın", false);
                return;
            }
        } catch (Exception ignored) { }
        String lastUrl = getSharedPreferences("radio", MODE_PRIVATE).getString("last_url", "");
        String lastName = getSharedPreferences("radio", MODE_PRIVATE).getString("last_name", "Türk Radyo");
        if (lastUrl != null && !lastUrl.trim().isEmpty()) {
            station = lastName;
            url = lastUrl;
            startForeground(NOTIFY_ID, buildNotification("Bağlanıyor…", true));
            play(lastUrl);
        }
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
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP |
                PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PLAY_FROM_SEARCH;
        session.setPlaybackState(new PlaybackState.Builder().setActions(actions).setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f).build());
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, station)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Muhammet Türk Radyo")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "Canlı Radyo")
                .build());
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
        PendingIntent previous = servicePending(ACTION_PREVIOUS, 2);
        PendingIntent pause = servicePending(ACTION_PAUSE, 3);
        PendingIntent resume = servicePending(ACTION_RESUME, 4);
        PendingIntent next = servicePending(ACTION_NEXT, 5);
        PendingIntent stop = servicePending(ACTION_STOP, 6);

        Notification.Action prevAction = new Notification.Action.Builder(android.R.drawable.ic_media_previous, "Önceki radyo", previous).build();
        Notification.Action playPause = new Notification.Action.Builder(android.R.drawable.ic_media_pause, "Duraklat", pause).build();
        if (player == null || (!connecting && !isPlaying())) playPause = new Notification.Action.Builder(android.R.drawable.ic_media_play, "Oynat", resume).build();
        Notification.Action nextAction = new Notification.Action.Builder(android.R.drawable.ic_media_next, "Sonraki radyo", next).build();
        Notification.Action stopAction = new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Durdur", stop).build();

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_radio)
                .setContentTitle(station)
                .setContentText(text)
                .setSubText("Muhammet Türk Radyo")
                .setContentIntent(content)
                .setOngoing(isPlaying() || connecting)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .addAction(prevAction)
                .addAction(playPause)
                .addAction(nextAction)
                .addAction(stopAction)
                .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2))
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
