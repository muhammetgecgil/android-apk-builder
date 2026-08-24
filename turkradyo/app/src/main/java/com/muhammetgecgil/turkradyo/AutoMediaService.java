package com.muhammetgecgil.turkradyo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AutoMediaService extends MediaBrowserService {
    private MediaSession session;

    @Override public void onCreate() {
        super.onCreate();
        session = new MediaSession(this, "TurkRadyoAuto");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { playLast(); }
            @Override public void onPlayFromMediaId(String mediaId, Bundle extras) { playMediaId(mediaId); }
            @Override public void onPause() { send(RadioService.ACTION_PAUSE); }
            @Override public void onStop() { send(RadioService.ACTION_STOP); }
            @Override public void onSkipToNext() { send(RadioService.ACTION_NEXT); }
            @Override public void onSkipToPrevious() { send(RadioService.ACTION_PREV); }
        });
        session.setActive(true);
        setSessionToken(session.getSessionToken());
    }

    private SharedPreferences prefs(){ return getSharedPreferences("radio", MODE_PRIVATE); }
    private void send(String action){ startService(new Intent(this, RadioService.class).setAction(action)); }

    private void playLast() {
        SharedPreferences p = prefs();
        String url = p.getString("url", "");
        String name = p.getString("name", "Son dinlenen radyo");
        if (url.isEmpty()) {
            try {
                JSONArray q = new JSONArray(p.getString("queue", "[]"));
                if(q.length()>0){
                    int i=Math.max(0,Math.min(p.getInt("queueIndex",0),q.length()-1));
                    JSONObject o=q.optJSONObject(i);
                    if(o!=null){url=o.optString("url","");name=o.optString("name","Türk Radyo");}
                }
            } catch(Exception ignored) {}
        }
        play(name,url);
    }

    private void playMediaId(String mediaId){
        if(mediaId==null)return;
        if("last".equals(mediaId)){ playLast(); return; }
        if(mediaId.startsWith("station:")){
            try{
                int idx=Integer.parseInt(mediaId.substring(8));
                SharedPreferences p=prefs();
                JSONArray q=new JSONArray(p.getString("queue","[]"));
                if(idx<0||idx>=q.length())return;
                JSONObject o=q.optJSONObject(idx); if(o==null)return;
                String u=o.optString("url",""); String n=o.optString("name","Türk Radyo");
                if(u.isEmpty())return;
                p.edit().putInt("queueIndex",idx).apply();
                play(n,u);
            }catch(Exception ignored){}
        }
    }

    private void play(String name,String url){
        if(url==null||url.isEmpty())return;
        Intent i=new Intent(this,RadioService.class).setAction(RadioService.ACTION_PLAY).putExtra("url",url).putExtra("name",name);
        if(android.os.Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        Bundle extras=new Bundle();
        extras.putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED",true);
        extras.putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT",1);
        extras.putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT",1);
        return new BrowserRoot("root", extras);
    }

    @Override public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        List<MediaBrowser.MediaItem> list = new ArrayList<>();
        SharedPreferences p=prefs();
        if ("root".equals(parentId)) {
            String name=p.getString("name","Son dinlenen radyo");
            MediaDescription last=new MediaDescription.Builder().setMediaId("last").setTitle("Son Dinlenen").setSubtitle(name).build();
            list.add(new MediaBrowser.MediaItem(last,MediaBrowser.MediaItem.FLAG_PLAYABLE));
            MediaDescription main80=new MediaDescription.Builder().setMediaId("main80").setTitle("Ana 80 Radyo").setSubtitle("Türk Radyo • 80 istasyon").build();
            list.add(new MediaBrowser.MediaItem(main80,MediaBrowser.MediaItem.FLAG_BROWSABLE));
        } else if("main80".equals(parentId)) {
            try{
                JSONArray q=new JSONArray(p.getString("queue","[]"));
                for(int i=0;i<q.length()&&i<80;i++){
                    JSONObject o=q.optJSONObject(i); if(o==null)continue;
                    String n=o.optString("name","Türk Radyo"); String u=o.optString("url",""); if(u.isEmpty())continue;
                    MediaDescription d=new MediaDescription.Builder().setMediaId("station:"+i).setTitle(n).setSubtitle((i+1)+" / "+Math.min(80,q.length())).build();
                    list.add(new MediaBrowser.MediaItem(d,MediaBrowser.MediaItem.FLAG_PLAYABLE));
                }
            }catch(Exception ignored){}
        }
        result.sendResult(list);
    }

    @Override public void onDestroy() {
        if (session != null) session.release();
        super.onDestroy();
    }
}
