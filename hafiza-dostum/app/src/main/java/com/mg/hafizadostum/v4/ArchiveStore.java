package com.mg.hafizadostum.v4;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.UUID;

public final class ArchiveStore {
    private static final String PREF = "hafiza_dostum_archive_v42";
    private static final String KEY = "events";
    private static final int MAX_EVENTS = 10000;

    private ArchiveStore() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static JSONArray getAll(Context c) {
        try { return new JSONArray(p(c).getString(KEY, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    private static void save(Context c, JSONArray a) {
        p(c).edit().putString(KEY, a.toString()).apply();
    }

    public static void record(Context c, String taskId, String name, long ts, String source) {
        if (ts <= 0L) ts = System.currentTimeMillis();
        JSONArray a = getAll(c);
        String signature = (taskId == null ? "" : taskId) + "|" + ts;
        for (int i = a.length() - 1; i >= 0 && i >= a.length() - 30; i--) {
            JSONObject x = a.optJSONObject(i);
            if (x != null && signature.equals(x.optString("sig"))) return;
        }
        JSONObject e = new JSONObject();
        try {
            e.put("id", UUID.randomUUID().toString());
            e.put("taskId", taskId == null ? "" : taskId);
            e.put("name", name == null ? "Kayıt" : name);
            e.put("ts", ts);
            e.put("source", source == null ? "app" : source);
            e.put("sig", signature);
            a.put(e);
        } catch (JSONException ignored) {}
        save(c, prune(a));
    }

    public static void importLegacy(Context c, JSONArray legacy) {
        if (legacy == null || legacy.length() == 0) {
            save(c, prune(getAll(c)));
            return;
        }
        JSONArray a = getAll(c);
        for (int i = 0; i < legacy.length(); i++) {
            JSONObject old = legacy.optJSONObject(i);
            if (old == null) continue;
            long ts = old.optLong("ts", 0L);
            if (ts < cutoff()) continue;
            String taskId = old.optString("taskId", "");
            String sig = taskId + "|" + ts;
            boolean exists = false;
            for (int j = a.length() - 1; j >= 0; j--) {
                JSONObject x = a.optJSONObject(j);
                if (x != null && sig.equals(x.optString("sig"))) { exists = true; break; }
            }
            if (exists) continue;
            JSONObject e = new JSONObject();
            try {
                e.put("id", old.optString("id", UUID.randomUUID().toString()));
                e.put("taskId", taskId);
                e.put("name", old.optString("name", "Kayıt"));
                e.put("ts", ts);
                e.put("source", old.optString("source", "legacy"));
                e.put("sig", sig);
                a.put(e);
            } catch (JSONException ignored) {}
        }
        save(c, prune(a));
    }

    public static boolean undoLatest(Context c, String taskId) {
        JSONArray a = getAll(c);
        int remove = -1;
        for (int i = a.length() - 1; i >= 0; i--) {
            JSONObject x = a.optJSONObject(i);
            if (x != null && taskId.equals(x.optString("taskId"))) { remove = i; break; }
        }
        if (remove < 0) return false;
        JSONArray out = new JSONArray();
        for (int i = 0; i < a.length(); i++) if (i != remove) out.put(a.opt(i));
        save(c, out);
        return true;
    }

    public static JSONArray forDay(Context c, long dayMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(dayMillis);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.DAY_OF_YEAR, 1);
        JSONArray all = getAll(c), out = new JSONArray();
        for (int i = 0; i < all.length(); i++) {
            JSONObject x = all.optJSONObject(i);
            if (x == null) continue;
            long ts = x.optLong("ts", 0L);
            if (ts >= start.getTimeInMillis() && ts < end.getTimeInMillis()) out.put(x);
        }
        return out;
    }

    public static int countMonth(Context c, long dayMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(dayMillis);
        start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.MONTH, 1);
        JSONArray all = getAll(c);
        int count = 0;
        for (int i = 0; i < all.length(); i++) {
            JSONObject x = all.optJSONObject(i);
            if (x == null) continue;
            long ts = x.optLong("ts", 0L);
            if (ts >= start.getTimeInMillis() && ts < end.getTimeInMillis()) count++;
        }
        return count;
    }

    public static long minDate() { return cutoff(); }

    private static long cutoff() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -6);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static JSONArray prune(JSONArray in) {
        JSONArray recent = new JSONArray();
        long cut = cutoff();
        int start = Math.max(0, in.length() - MAX_EVENTS);
        for (int i = start; i < in.length(); i++) {
            JSONObject x = in.optJSONObject(i);
            if (x != null && x.optLong("ts", 0L) >= cut) recent.put(x);
        }
        return recent;
    }
}
