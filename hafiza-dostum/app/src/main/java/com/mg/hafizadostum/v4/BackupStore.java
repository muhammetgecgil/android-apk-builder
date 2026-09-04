package com.mg.hafizadostum.v4;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

public final class BackupStore {
    private static final int SCHEMA = 1;
    private static final String[] PREFS = {
            "hafiza_dostum_v4",
            "hafiza_dostum_profile",
            "hafiza_dostum_ui"
    };

    private BackupStore() {}

    public static String exportJson(Context c) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schema", SCHEMA);
        root.put("app", "Hafıza Dostum");
        root.put("version", "5.0");
        root.put("exportedAt", System.currentTimeMillis());
        JSONObject prefs = new JSONObject();
        for (String name : PREFS) prefs.put(name, encodePrefs(c.getSharedPreferences(name, Context.MODE_PRIVATE)));
        root.put("preferences", prefs);
        root.put("archive", ArchiveStore.getAll(c));
        return root.toString(2);
    }

    public static void importJson(Context c, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (root.optInt("schema", -1) != SCHEMA) throw new IllegalArgumentException("Desteklenmeyen yedek sürümü");
        JSONObject prefs = root.optJSONObject("preferences");
        if (prefs == null) throw new IllegalArgumentException("Yedek içeriği eksik");

        ReminderScheduler.cancelAll(c);
        for (String name : PREFS) {
            JSONObject data = prefs.optJSONObject(name);
            if (data != null) decodePrefs(c.getSharedPreferences(name, Context.MODE_PRIVATE), data);
        }
        ArchiveStore.replaceAll(c, root.optJSONArray("archive"));
        if (ProfileEngine.isSaved(c)) ProfileEngine.applySaved(c);
        ReminderScheduler.scheduleAll(c);
    }

    public static void clearAll(Context c) {
        ReminderScheduler.cancelAll(c);
        for (String name : PREFS) c.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit();
        ArchiveStore.clear(c);
    }

    private static JSONObject encodePrefs(SharedPreferences p) throws Exception {
        JSONObject out = new JSONObject();
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            Object value = e.getValue();
            JSONObject item = new JSONObject();
            if (value instanceof String) { item.put("type", "string"); item.put("value", value); }
            else if (value instanceof Boolean) { item.put("type", "boolean"); item.put("value", value); }
            else if (value instanceof Integer) { item.put("type", "int"); item.put("value", value); }
            else if (value instanceof Long) { item.put("type", "long"); item.put("value", value); }
            else if (value instanceof Float) { item.put("type", "float"); item.put("value", ((Float)value).doubleValue()); }
            else if (value instanceof Set) {
                item.put("type", "set");
                JSONArray a = new JSONArray();
                for (Object x : (Set<?>)value) a.put(String.valueOf(x));
                item.put("value", a);
            } else continue;
            out.put(e.getKey(), item);
        }
        return out;
    }

    private static void decodePrefs(SharedPreferences p, JSONObject data) throws Exception {
        SharedPreferences.Editor ed = p.edit().clear();
        java.util.Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject item = data.optJSONObject(key);
            if (item == null) continue;
            String type = item.optString("type");
            switch (type) {
                case "string": ed.putString(key, item.optString("value", "")); break;
                case "boolean": ed.putBoolean(key, item.optBoolean("value", false)); break;
                case "int": ed.putInt(key, item.optInt("value", 0)); break;
                case "long": ed.putLong(key, item.optLong("value", 0L)); break;
                case "float": ed.putFloat(key, (float)item.optDouble("value", 0)); break;
                case "set":
                    JSONArray a = item.optJSONArray("value");
                    java.util.HashSet<String> set = new java.util.HashSet<>();
                    if (a != null) for (int i = 0; i < a.length(); i++) set.add(a.optString(i));
                    ed.putStringSet(key, set); break;
            }
        }
        if (!ed.commit()) throw new IllegalStateException("Yedek geri yüklenemedi");
    }
}
