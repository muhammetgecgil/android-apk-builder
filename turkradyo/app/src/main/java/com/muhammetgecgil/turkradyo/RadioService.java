package com.muhammetgecgil.turkradyo;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.*;

public class RadioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    public static final String ACTION_PLAY="com.muhammetgecgil.turkradyo.PLAY";
    public static final String ACTION_PAUSE="com.muhammetgecgil.turkradyo.PAUSE";
    public static final String ACTION_RESUME="com.muhammetgecgil.turkradyo.RESUME";
    public static final String ACTION_STOP="com.muhammetgecgil.turkradyo.STOP";
    public static final String ACTION_VOLUME="com.muhammetgecgil.turkradyo.VOLUME";
    public static final String ACTION_GAIN="com.muhammetgecgil.turkradyo.GAIN";
    private static final int NOTIF_ID=1201;
    private static final String CHANNEL="radio_playback";
    private MediaPlayer player;
    private LoudnessEnhancer enhancer;
    private String stationName="Muhammet Türk Radyo";
    private float volume=1f;
    private int gainMb=0;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String a=intent.getAction();
        if (ACTION_PLAY.equals(a)) {
            String u=intent.getStringExtra("url"); String n=intent.getStringExtra("name");
            if (u != null && !u.isEmpty()) play(u,n);
        } else if (ACTION_PAUSE.equals(a)) pause();
        else if (ACTION_RESUME.equals(a)) resume();
        else if (ACTION_STOP.equals(a)) stopAll();
        else if (ACTION_VOLUME.equals(a)) { volume=Math.max(0f,Math.min(1f,intent.getFloatExtra("volume",1f))); if(player!=null) player.setVolume(volume,volume); }
        else if (ACTION_GAIN.equals(a)) { gainMb=intent.getIntExtra("gain",0); applyGain(); }
        return START_STICKY;
    }

    private void play(String url,String name) {
        stationName=(name==null||name.isEmpty())?"Türk Radyo":name;
        startForeground(NOTIF_ID,buildNotification("Bağlanıyor…",true));
        requestFocus();
        releasePlayer();
        try {
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            player.setOnPreparedListener(this); player.setOnErrorListener(this);
            player.setDataSource(this, Uri.parse(url));
            player.setVolume(volume,volume);
            player.prepareAsync();
        } catch(Exception e) {
            updateNotification("Yayın açılamadı",false);
        }
    }

    @Override public void onPrepared(MediaPlayer mp) {
        try { mp.start(); applyGain(); updateNotification("Canlı yayın",true); } catch(Exception ignored) { }
    }

    @Override public boolean onError(MediaPlayer mp,int what,int extra) {
        updateNotification("Bağlantı kesildi",false); return true;
    }

    private void pause(){ if(player!=null && player.isPlaying()){player.pause(); updateNotification("Duraklatıldı",false);} }
    private void resume(){ if(player!=null){try{player.start();updateNotification("Canlı yayın",true);}catch(Exception ignored){}} }
    private void stopAll(){ releasePlayer(); abandonFocus(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }

    private void applyGain(){
        if(player==null) return;
        try { if(enhancer!=null) enhancer.release(); enhancer=new LoudnessEnhancer(player.getAudioSessionId()); enhancer.setTargetGain(Math.max(0,Math.min(3000,gainMb))); enhancer.setEnabled(gainMb>0); } catch(Exception ignored){}
    }

    private void releasePlayer(){
        if(enhancer!=null){try{enhancer.release();}catch(Exception ignored){} enhancer=null;}
        if(player!=null){try{player.stop();}catch(Exception ignored){} try{player.release();}catch(Exception ignored){} player=null;}
    }

    private void requestFocus(){
        if(Build.VERSION.SDK_INT>=26){
            focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setOnAudioFocusChangeListener(change->{ if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause(); }).build();
            audioManager.requestAudioFocus(focusRequest);
        }
    }
    private void abandonFocus(){ if(Build.VERSION.SDK_INT>=26 && focusRequest!=null) audioManager.abandonAudioFocusRequest(focusRequest); }

    private PendingIntent svc(String action,int req){ return PendingIntent.getService(this,req,new Intent(this,RadioService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); }
    private Notification buildNotification(String state,boolean playing){
        Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_stat_radio).setContentTitle(stationName).setContentText(state).setContentIntent(content).setOngoing(playing).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PUBLIC)
          .addAction(new Notification.Action.Builder(null,playing?"Duraklat":"Oynat",svc(playing?ACTION_PAUSE:ACTION_RESUME,2)).build())
          .addAction(new Notification.Action.Builder(null,"Durdur",svc(ACTION_STOP,3)).build());
        if(Build.VERSION.SDK_INT>=21)b.setStyle(new Notification.MediaStyle());
        return b.build();
    }
    private void updateNotification(String s,boolean p){ ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIF_ID,buildNotification(s,p)); }
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel(CHANNEL,getString(R.string.notif_channel_name),NotificationManager.IMPORTANCE_LOW); c.setDescription(getString(R.string.notif_channel_desc)); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);} }
    @Override public void onDestroy(){ releasePlayer(); abandonFocus(); super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}
