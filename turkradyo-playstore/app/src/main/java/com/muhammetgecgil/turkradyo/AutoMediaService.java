package com.muhammetgecgil.turkradyo;

import android.content.Intent;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import java.util.Collections;
import java.util.List;

public class AutoMediaService extends MediaBrowserService {
    private MediaSession session;
    private static final String ROOT = "root";
    private static final String LAST = "last";

    @Override public void onCreate() {
        super.onCreate();
        session = new MediaSession(this, "TurkRadyoAuto");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { playLast(); }
            @Override public void onPlayFromMediaId(String mediaId, Bundle extras) { if (LAST.equals(mediaId)) playLast(); }
            @Override public void onPause() {
                startService(new Intent(AutoMediaService.this, RadioService.class).setAction(RadioService.ACTION_PAUSE));
                updateState(PlaybackState.STATE_PAUSED);
            }
            @Override public void onStop() {
                startService(new Intent(AutoMediaService.this, RadioService.class).setAction(RadioService.ACTION_STOP));
                updateState(PlaybackState.STATE_STOPPED);
            }
        });
        session.setActive(true);
        setSessionToken(session.getSessionToken());
        updateState(PlaybackState.STATE_NONE);
    }

    private void playLast() {
        String url = getSharedPreferences("radio", MODE_PRIVATE).getString("last_url", "");
        String name = getSharedPreferences("radio", MODE_PRIVATE).getString("last_name", "Türk Radyo");
        if (url == null || url.trim().isEmpty()) return;
        Intent i = new Intent(this, RadioService.class).setAction(RadioService.ACTION_PLAY).putExtra("url", url).putExtra("name", name);
        startForegroundService(i);
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, name)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Muhammet Türk Radyo")
                .build());
        updateState(PlaybackState.STATE_PLAYING);
    }

    private void updateState(int state) {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID;
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build());
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(ROOT, null);
    }

    @Override public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        if (!ROOT.equals(parentId)) {
            result.sendResult(Collections.emptyList());
            return;
        }
        String name = getSharedPreferences("radio", MODE_PRIVATE).getString("last_name", "Son dinlenen radyo");
        MediaDescription desc = new MediaDescription.Builder()
                .setMediaId(LAST)
                .setTitle(name)
                .setSubtitle("Son dinlenen istasyonu aç")
                .build();
        result.sendResult(Collections.singletonList(new MediaBrowser.MediaItem(desc, MediaBrowser.MediaItem.FLAG_PLAYABLE)));
    }

    @Override public void onDestroy() {
        if (session != null) session.release();
        super.onDestroy();
    }
}
