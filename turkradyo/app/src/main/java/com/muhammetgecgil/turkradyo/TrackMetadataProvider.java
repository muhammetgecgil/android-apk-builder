package com.muhammetgecgil.turkradyo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

public final class TrackMetadataProvider extends ContentProvider {
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable sync = this::syncMonitor;

    @Override public boolean onCreate() {
        Context c = getContext();
        if (c == null) return false;
        prefs = c.getSharedPreferences("radio", Context.MODE_PRIVATE);
        listener = (sp, key) -> {
            if ("url".equals(key) || "resolvedUrl".equals(key) || "name".equals(key)) {
                main.removeCallbacks(sync);
                main.postDelayed(sync, 450L);
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
        main.postDelayed(sync, 1200L);
        return true;
    }

    private void syncMonitor() {
        Context c = getContext();
        if (c == null || prefs == null) return;
        String name = prefs.getString("name", "Türk Radyo");
        String resolved = prefs.getString("resolvedUrl", "");
        String primary = prefs.getString("url", "");
        TrackMetadataMonitor.start(c, name, resolved == null || resolved.isEmpty() ? primary : resolved);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
