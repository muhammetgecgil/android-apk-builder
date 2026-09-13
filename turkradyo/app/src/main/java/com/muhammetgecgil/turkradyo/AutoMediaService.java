package com.muhammetgecgil.turkradyo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.media.MediaBrowserService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Android Auto media browser for TurkRadyo v2.8.7.
 *
 * The car UI owns browsing and controls while RadioService remains the single
 * playback engine. This service mirrors RadioService state/metadata from the
 * shared radio store so the car display stays in sync without creating a
 * second audio player.
 */
public class AutoMediaService extends MediaBrowserService {
    private static final String ROOT="root";
    private static final String LAST="last";
    private static final String MAIN="main80";
    private static final String RECENT="recent";
    private static final long SYNC_MS=1000L;

    private MediaSession session;
    private SharedPreferences prefs;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Runnable syncTask;
    private String lastMetaSignature="";
    private int lastState=-1;

    @Override public void onCreate(){
        super.onCreate();
        prefs=getSharedPreferences("radio",MODE_PRIVATE);
        session=new MediaSession(this,"TurkRadyoAuto");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback(){
            @Override public void onPlay(){playLast();}
            @Override public void onPause(){send(RadioService.ACTION_PAUSE,false);setState(PlaybackState.STATE_PAUSED);}
            @Override public void onStop(){send(RadioService.ACTION_STOP,false);setState(PlaybackState.STATE_STOPPED);}
            @Override public void onSkipToNext(){send(RadioService.ACTION_NEXT,false);setState(PlaybackState.STATE_CONNECTING);syncSoon();}
            @Override public void onSkipToPrevious(){send(RadioService.ACTION_PREV,false);setState(PlaybackState.STATE_CONNECTING);syncSoon();}
            @Override public void onPlayFromMediaId(String mediaId,Bundle extras){playMediaId(mediaId);}
        });
        session.setActive(true);
        setSessionToken(session.getSessionToken());
        syncFromPlayback();
        syncTask=new Runnable(){@Override public void run(){syncFromPlayback();handler.postDelayed(this,SYNC_MS);}};
        handler.postDelayed(syncTask,SYNC_MS);
    }

    private Uri iconUri(){return Uri.parse("android.resource://"+getPackageName()+"/drawable/ic_launcher_selected");}

    private long actions(){
        return PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|
                PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID;
    }

    private void setState(int state){
        if(session==null)return;
        lastState=state;
        float speed=state==PlaybackState.STATE_PLAYING?1f:0f;
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions())
                .setState(state,PlaybackState.PLAYBACK_POSITION_UNKNOWN,speed)
                .build());
    }

    private void send(String action,boolean foreground){
        Intent i=new Intent(this,RadioService.class).setAction(action);
        if(foreground&&android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
    }

    private void play(String url,String name){
        if(url==null||url.trim().isEmpty())return;
        Intent i=new Intent(this,RadioService.class)
                .setAction(RadioService.ACTION_PLAY)
                .putExtra("url",url)
                .putExtra("name",name==null||name.trim().isEmpty()?"Türk Radyo":name);
        if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
        setState(PlaybackState.STATE_CONNECTING);
        syncSoon();
    }

    private void syncSoon(){handler.postDelayed(this::syncFromPlayback,350L);}

    private void playLast(){
        String url=prefs.getString("url","");
        String name=prefs.getString("name","Son dinlenen radyo");
        if(url!=null&&!url.isEmpty()){play(url,name);return;}
        playFirstQueueItem();
    }

    private void playFirstQueueItem(){
        try{
            JSONArray q=new JSONArray(prefs.getString("queue","[]"));
            for(int i=0;i<q.length();i++){
                JSONObject o=q.optJSONObject(i);
                if(o==null)continue;
                String u=o.optString("url","");
                if(u.isEmpty())continue;
                prefs.edit().putInt("queueIndex",i).apply();
                play(u,o.optString("name","Türk Radyo"));
                return;
            }
        }catch(Exception ignored){}
    }

    private void playMediaId(String id){
        if(id==null)return;
        try{
            if(LAST.equals(id)){playLast();return;}
            if(id.startsWith("q:")){
                int n=Integer.parseInt(id.substring(2));
                JSONArray a=new JSONArray(prefs.getString("queue","[]"));
                JSONObject o=a.optJSONObject(n);
                if(o!=null){prefs.edit().putInt("queueIndex",n).apply();play(o.optString("url"),o.optString("name","Türk Radyo"));}
                return;
            }
            if(id.startsWith("recent:")){
                int n=Integer.parseInt(id.substring(7));
                JSONArray a=new JSONArray(prefs.getString("recentStations","[]"));
                JSONObject o=a.optJSONObject(n);
                if(o!=null)play(o.optString("url"),o.optString("name","Türk Radyo"));
            }
        }catch(Exception ignored){}
    }

    private void syncFromPlayback(){
        if(session==null||prefs==null)return;
        try{
            String station=prefs.getString("name","Türk Radyo");
            if(station==null||station.trim().isEmpty())station="Türk Radyo";
            String track=prefs.getString("nowTitle","");
            if(track==null)track="";
            String telemetry=prefs.getString("telemetry","{}");
            JSONObject t;
            try{t=new JSONObject(telemetry==null?"{}":telemetry);}catch(Exception e){t=new JSONObject();}
            boolean playing=t.optBoolean("isPlaying",false);
            boolean buffering=t.optBoolean("buffering",false);
            int playerState=t.optInt("playerState",0);
            String url=prefs.getString("url","");

            int state;
            if(playing)state=PlaybackState.STATE_PLAYING;
            else if(buffering||playerState==2)state=PlaybackState.STATE_CONNECTING;
            else if(url!=null&&!url.isEmpty())state=PlaybackState.STATE_PAUSED;
            else state=PlaybackState.STATE_STOPPED;

            String sig=station+"\n"+track;
            if(!sig.equals(lastMetaSignature)){
                lastMetaSignature=sig;
                String artist=track.isEmpty()?"Canlı Radyo":track;
                session.setMetadata(new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID,LAST)
                        .putString(MediaMetadata.METADATA_KEY_TITLE,station)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST,artist)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM,"Türk Radyo")
                        .build());
            }
            if(state!=lastState)setState(state);
            if(!session.isActive())session.setActive(true);
        }catch(Exception ignored){}
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName,int clientUid,Bundle rootHints){
        return new BrowserRoot(ROOT,null);
    }

    @Override public void onLoadChildren(String parentId,Result<List<MediaBrowser.MediaItem>> result){
        List<MediaBrowser.MediaItem> out=new ArrayList<>();
        if(ROOT.equals(parentId)){
            String lastUrl=prefs.getString("url","");
            if(lastUrl!=null&&!lastUrl.isEmpty()){
                out.add(playable(LAST,prefs.getString("name","Son dinlenen radyo"),"Son dinlenen • Canlı yayın"));
            }
            out.add(folder(MAIN,"Ana 80","Radyo listeniz"));
            out.add(folder(RECENT,"Son Dinlenenler","Yakın zamanda açılan radyolar"));
        }else if(MAIN.equals(parentId)){
            loadArray(out,prefs.getString("queue","[]"),"q:",80);
            if(out.isEmpty()){
                String lastUrl=prefs.getString("url","");
                if(lastUrl!=null&&!lastUrl.isEmpty())out.add(playable(LAST,prefs.getString("name","Son dinlenen radyo"),"Canlı yayın"));
            }
        }else if(RECENT.equals(parentId)){
            loadArray(out,prefs.getString("recentStations","[]"),"recent:",50);
        }
        result.sendResult(out);
    }

    private MediaBrowser.MediaItem folder(String id,String title,String subtitle){
        MediaDescription d=new MediaDescription.Builder()
                .setMediaId(id).setTitle(title).setSubtitle(subtitle).setIconUri(iconUri()).build();
        return new MediaBrowser.MediaItem(d,MediaBrowser.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowser.MediaItem playable(String id,String title,String subtitle){
        MediaDescription d=new MediaDescription.Builder()
                .setMediaId(id).setTitle(title).setSubtitle(subtitle).setIconUri(iconUri()).build();
        return new MediaBrowser.MediaItem(d,MediaBrowser.MediaItem.FLAG_PLAYABLE);
    }

    private void loadArray(List<MediaBrowser.MediaItem> out,String json,String prefix,int max){
        try{
            JSONArray a=new JSONArray(json==null?"[]":json);
            for(int i=0;i<a.length()&&out.size()<max;i++){
                JSONObject o=a.optJSONObject(i);
                if(o==null||o.optString("url","").isEmpty())continue;
                out.add(playable(prefix+i,o.optString("name","Türk Radyo"),"Canlı yayın"));
            }
        }catch(Exception ignored){}
    }

    @Override public void onDestroy(){
        if(syncTask!=null)handler.removeCallbacks(syncTask);
        handler.removeCallbacksAndMessages(null);
        if(session!=null){try{session.release();}catch(Exception ignored){}}
        super.onDestroy();
    }
}
