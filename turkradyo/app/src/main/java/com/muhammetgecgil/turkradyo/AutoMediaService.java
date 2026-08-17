package com.muhammetgecgil.turkradyo;

import android.content.Intent;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import java.util.ArrayList;
import java.util.List;

public class AutoMediaService extends MediaBrowserService {
    private MediaSession session;

    @Override public void onCreate() {
        super.onCreate();
        session = new MediaSession(this, "TurkRadyoAuto");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { playLast(); }
            @Override public void onPause() { startService(new Intent(AutoMediaService.this, RadioService.class).setAction(RadioService.ACTION_PAUSE)); }
            @Override public void onStop() { startService(new Intent(AutoMediaService.this, RadioService.class).setAction(RadioService.ACTION_STOP)); }
        });
        session.setActive(true);
        setSessionToken(session.getSessionToken());
    }

    private void playLast() {
        android.content.SharedPreferences p = getSharedPreferences("radio", MODE_PRIVATE);
        String url = p.getString("url", "");
        String name = p.getString("name", "Son dinlenen radyo");
        if (url.isEmpty()) return;
        Intent i = new Intent(this, RadioService.class).setAction(RadioService.ACTION_PLAY).putExtra("url", url).putExtra("name", name);
        startForegroundService(i);
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        List<MediaBrowser.MediaItem> list = new ArrayList<>();
        if ("root".equals(parentId)) {
            String name = getSharedPreferences("radio", MODE_PRIVATE).getString("name", "Son dinlenen radyo");
            MediaDescription d = new MediaDescription.Builder().setMediaId("last").setTitle(name).setSubtitle("Muhammet Türk Radyo").build();
            list.add(new MediaBrowser.MediaItem(d, MediaBrowser.MediaItem.FLAG_PLAYABLE));
        }
        result.sendResult(list);
    }

    @Override public void onDestroy() {
        if (session != null) session.release();
        super.onDestroy();
    }
}
