package com.mg.hafizadostum.v4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.UUID;

public final class ArchiveStore {
    private static final String DB_NAME = "hafiza_archive.db";
    private static final int DB_VERSION = 1;

    private ArchiveStore() {}

    private static final class Db extends SQLiteOpenHelper {
        Db(Context c) { super(c, DB_NAME, null, DB_VERSION); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE archive_events (id TEXT PRIMARY KEY, taskId TEXT, name TEXT NOT NULL, ts INTEGER NOT NULL, source TEXT, sig TEXT UNIQUE)");
            db.execSQL("CREATE INDEX idx_archive_ts ON archive_events(ts)");
            db.execSQL("CREATE INDEX idx_archive_task ON archive_events(taskId, ts)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    }

    private static SQLiteDatabase rw(Context c) { return new Db(c.getApplicationContext()).getWritableDatabase(); }

    public static JSONArray getAll(Context c) {
        prune(c);
        return query(c, "ts>=?", new String[]{String.valueOf(cutoff())}, "ts ASC");
    }

    public static void record(Context c, String taskId, String name, long ts, String source) {
        if (ts <= 0L) ts = System.currentTimeMillis();
        if (ts < cutoff()) return;
        String id = UUID.randomUUID().toString();
        String safeTask = taskId == null ? "" : taskId;
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("taskId", safeTask);
        v.put("name", name == null ? "Kayıt" : name);
        v.put("ts", ts);
        v.put("source", source == null ? "app" : source);
        v.put("sig", safeTask + "|" + ts);
        rw(c).insertWithOnConflict("archive_events", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        prune(c);
    }

    public static void importLegacy(Context c, JSONArray legacy) {
        if (legacy == null) return;
        SQLiteDatabase db = rw(c);
        db.beginTransaction();
        try {
            for (int i = 0; i < legacy.length(); i++) {
                JSONObject e = legacy.optJSONObject(i);
                if (e == null) continue;
                long ts = e.optLong("ts", 0L);
                if (ts < cutoff()) continue;
                String taskId = e.optString("taskId", "");
                ContentValues v = new ContentValues();
                v.put("id", e.optString("id", UUID.randomUUID().toString()));
                v.put("taskId", taskId);
                v.put("name", e.optString("name", "Kayıt"));
                v.put("ts", ts);
                v.put("source", e.optString("source", "legacy"));
                v.put("sig", taskId + "|" + ts);
                db.insertWithOnConflict("archive_events", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        prune(c);
    }

    public static void replaceAll(Context c, JSONArray events) {
        SQLiteDatabase db = rw(c);
        db.beginTransaction();
        try {
            db.delete("archive_events", null, null);
            if (events != null) {
                for (int i = 0; i < events.length(); i++) {
                    JSONObject e = events.optJSONObject(i);
                    if (e == null) continue;
                    long ts = e.optLong("ts", 0L);
                    if (ts < cutoff()) continue;
                    String taskId = e.optString("taskId", "");
                    ContentValues v = new ContentValues();
                    v.put("id", e.optString("id", UUID.randomUUID().toString()));
                    v.put("taskId", taskId);
                    v.put("name", e.optString("name", "Kayıt"));
                    v.put("ts", ts);
                    v.put("source", e.optString("source", "backup"));
                    v.put("sig", taskId + "|" + ts);
                    db.insertWithOnConflict("archive_events", null, v, SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static boolean undoLatest(Context c, String taskId) {
        SQLiteDatabase db = rw(c);
        Cursor cur = db.query("archive_events", new String[]{"id"}, "taskId=?", new String[]{taskId}, null, null, "ts DESC", "1");
        String id = null;
        if (cur.moveToFirst()) id = cur.getString(0);
        cur.close();
        return id != null && db.delete("archive_events", "id=?", new String[]{id}) > 0;
    }

    public static JSONArray forDay(Context c, long dayMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(dayMillis);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.DAY_OF_YEAR, 1);
        return query(c, "ts>=? AND ts<?", new String[]{String.valueOf(start.getTimeInMillis()), String.valueOf(end.getTimeInMillis())}, "ts ASC");
    }

    public static int countMonth(Context c, long dayMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(dayMillis);
        start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.MONTH, 1);
        Cursor cur = rw(c).rawQuery("SELECT COUNT(*) FROM archive_events WHERE ts>=? AND ts<?", new String[]{String.valueOf(start.getTimeInMillis()), String.valueOf(end.getTimeInMillis())});
        int count = cur.moveToFirst() ? cur.getInt(0) : 0;
        cur.close();
        return count;
    }

    public static void clear(Context c) { rw(c).delete("archive_events", null, null); }
    public static long minDate() { return cutoff(); }

    private static JSONArray query(Context c, String selection, String[] args, String order) {
        JSONArray out = new JSONArray();
        Cursor cur = rw(c).query("archive_events", new String[]{"id","taskId","name","ts","source"}, selection, args, null, null, order);
        while (cur.moveToNext()) {
            JSONObject e = new JSONObject();
            try {
                e.put("id", cur.getString(0));
                e.put("taskId", cur.getString(1));
                e.put("name", cur.getString(2));
                e.put("ts", cur.getLong(3));
                e.put("source", cur.getString(4));
                out.put(e);
            } catch (Exception ignored) {}
        }
        cur.close();
        return out;
    }

    private static void prune(Context c) {
        rw(c).delete("archive_events", "ts<?", new String[]{String.valueOf(cutoff())});
    }

    private static long cutoff() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -6);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
