package com.muhammetgecgil.turkradyo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Android Auto media browser: safe, large-target browsing with Ana 80 + recent stations. */
public class AutoMediaService extends MediaBrowserService {
    private static final String ROOT="root", MAIN="main80", RECENT="recent";
    private MediaSession session;

    @Override public void onCreate(){
        super.onCreate();
        session=new MediaSession(this,"TurkRadyoAuto");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback(){
            @Override public void onPlay(){playLast();}
            @Override public void onPause(){send(RadioService.ACTION_PAUSE);setState(PlaybackState.STATE_PAUSED);}
            @Override public void onStop(){send(RadioService.ACTION_STOP);setState(PlaybackState.STATE_STOPPED);}
            @Override public void onSkipToNext(){send(RadioService.ACTION_NEXT);setState(PlaybackState.STATE_PLAYING);}
            @Override public void onSkipToPrevious(){send(RadioService.ACTION_PREV);setState(PlaybackState.STATE_PLAYING);}
            @Override public void onPlayFromMediaId(String mediaId,Bundle extras){playMediaId(mediaId);}
        });
        session.setActive(true);setState(PlaybackState.STATE_PAUSED);setSessionToken(session.getSessionToken());
    }

    private void setState(int state){
        long actions=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_PLAY_FROM_MEDIA_ID;
        session.setPlaybackState(new PlaybackState.Builder().setActions(actions).setState(state,PlaybackState.PLAYBACK_POSITION_UNKNOWN,state==PlaybackState.STATE_PLAYING?1f:0f).build());
    }

    private void send(String action){startService(new Intent(this,RadioService.class).setAction(action));}
    private void play(String url,String name){
        if(url==null||url.isEmpty())return;
        Intent i=new Intent(this,RadioService.class).setAction(RadioService.ACTION_PLAY).putExtra("url",url).putExtra("name",name);
        if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);
        session.setMetadata(new android.media.MediaMetadata.Builder().putString(android.media.MediaMetadata.METADATA_KEY_TITLE,name).putString(android.media.MediaMetadata.METADATA_KEY_ARTIST,"Canlı Radyo").putString(android.media.MediaMetadata.METADATA_KEY_ALBUM,"Türk Radyo").build());
        setState(PlaybackState.STATE_PLAYING);
    }

    private void playLast(){SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);play(p.getString("url",""),p.getString("name","Son dinlenen radyo"));}

    private void playMediaId(String id){
        try{
            SharedPreferences p=getSharedPreferences("radio",MODE_PRIVATE);
            if(id.startsWith("q:")){int n=Integer.parseInt(id.substring(2));JSONArray a=new JSONArray(p.getString("queue","[]"));JSONObject o=a.optJSONObject(n);if(o!=null){p.edit().putInt("queueIndex",n).apply();play(o.optString("url"),o.optString("name","Türk Radyo"));}}
            else if(id.startsWith("recent:")){int n=Integer.parseInt(id.substring(7));JSONArray a=new JSONArray(p.getString("recentStations","[]"));JSONObject o=a.optJSONObject(n);if(o!=null)play(o.optString("url"),o.optString("name","Türk Radyo"));}
        }catch(Exception ignored){}
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName,int clientUid,Bundle rootHints){return new BrowserRoot(ROOT,null);}

    @Override public void onLoadChildren(String parentId,Result<List<MediaBrowser.MediaItem>> result){
        List<MediaBrowser.MediaItem> out=new ArrayList<>();
        if(ROOT.equals(parentId)){
            out.add(folder(MAIN,"Ana 80","Kayıtlı ana radyo listeniz"));
            out.add(folder(RECENT,"Son Dinlenenler","Son açılan radyolar"));
        }else if(MAIN.equals(parentId))loadArray(out,getSharedPreferences("radio",MODE_PRIVATE).getString("queue","[]"),"q:",80);
        else if(RECENT.equals(parentId))loadArray(out,getSharedPreferences("radio",MODE_PRIVATE).getString("recentStations","[]"),"recent:",20);
        result.sendResult(out);
    }

    private MediaBrowser.MediaItem folder(String id,String title,String subtitle){MediaDescription d=new MediaDescription.Builder().setMediaId(id).setTitle(title).setSubtitle(subtitle).build();return new MediaBrowser.MediaItem(d,MediaBrowser.MediaItem.FLAG_BROWSABLE);}
    private void loadArray(List<MediaBrowser.MediaItem> out,String json,String prefix,int max){
        try{JSONArray a=new JSONArray(json);for(int i=0;i<a.length()&&i<max;i++){JSONObject o=a.optJSONObject(i);if(o==null||o.optString("url").isEmpty())continue;MediaDescription d=new MediaDescription.Builder().setMediaId(prefix+i).setTitle(o.optString("name","Türk Radyo")).setSubtitle("Canlı yayın").build();out.add(new MediaBrowser.MediaItem(d,MediaBrowser.MediaItem.FLAG_PLAYABLE));}}catch(Exception ignored){}
    }

    @Override public void onDestroy(){if(session!=null)session.release();super.onDestroy();}
}
