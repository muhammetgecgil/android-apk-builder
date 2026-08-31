package com.muhammet.habersafev5;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AppStore {
    private static final String PREFS = "haber_safe_product_store";
    private static final String KEY_READ = "read_links";
    private static final String KEY_FAVORITES = "favorites_json";
    private static final String CACHE_PREFIX = "cache_";
    private static final String CACHE_TIME_PREFIX = "cache_time_";

    private AppStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isRead(Context context, String link) {
        if (link == null) return false;
        return prefs(context).getStringSet(KEY_READ, new HashSet<>()).contains(link);
    }

    public static void setRead(Context context, String link, boolean read) {
        if (link == null || link.isEmpty()) return;
        SharedPreferences p = prefs(context);
        Set<String> set = new HashSet<>(p.getStringSet(KEY_READ, new HashSet<>()));
        if (read) set.add(link); else set.remove(link);
        if (set.size() > 1200) set.clear();
        p.edit().putStringSet(KEY_READ, set).apply();
    }

    public static void clearRead(Context context) {
        prefs(context).edit().remove(KEY_READ).apply();
    }

    public static boolean isFavorite(Context context, String link) {
        if (link == null) return false;
        for (NewsRepository.NewsItem item : getFavorites(context)) {
            if (link.equals(item.link)) return true;
        }
        return false;
    }

    public static boolean toggleFavorite(Context context, NewsRepository.NewsItem item) {
        List<NewsRepository.NewsItem> list = getFavorites(context);
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (item.link.equals(list.get(i).link)) {
                list.remove(i);
                removed = true;
            }
        }
        if (!removed) list.add(0, item);
        saveFavorites(context, list);
        return !removed;
    }

    public static List<NewsRepository.NewsItem> getFavorites(Context context) {
        ArrayList<NewsRepository.NewsItem> out = new ArrayList<>();
        String raw = prefs(context).getString(KEY_FAVORITES, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                NewsRepository.NewsItem item = fromJson(arr.optJSONObject(i));
                if (item != null) out.add(item);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static void saveFavorites(Context context, List<NewsRepository.NewsItem> list) {
        JSONArray arr = new JSONArray();
        int max = Math.min(250, list.size());
        for (int i = 0; i < max; i++) arr.put(toJson(list.get(i)));
        prefs(context).edit().putString(KEY_FAVORITES, arr.toString()).apply();
    }

    public static void saveCache(Context context, String key, List<NewsRepository.NewsItem> list) {
        if (key == null || key.isEmpty() || list == null) return;
        JSONArray arr = new JSONArray();
        int max = Math.min(60, list.size());
        for (int i = 0; i < max; i++) arr.put(toJson(list.get(i)));
        String suffix = cacheSuffix(key);
        prefs(context).edit()
                .putString(CACHE_PREFIX + suffix, arr.toString())
                .putLong(CACHE_TIME_PREFIX + suffix, System.currentTimeMillis())
                .apply();
    }

    public static List<NewsRepository.NewsItem> loadCache(Context context, String key) {
        ArrayList<NewsRepository.NewsItem> out = new ArrayList<>();
        if (key == null || key.isEmpty()) return out;
        String raw = prefs(context).getString(CACHE_PREFIX + cacheSuffix(key), "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                NewsRepository.NewsItem item = fromJson(arr.optJSONObject(i));
                if (item != null) out.add(item);
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static long cacheTime(Context context, String key) {
        if (key == null || key.isEmpty()) return 0L;
        return prefs(context).getLong(CACHE_TIME_PREFIX + cacheSuffix(key), 0L);
    }

    public static void clearCache(Context context) {
        SharedPreferences p = prefs(context);
        SharedPreferences.Editor editor = p.edit();
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            if (e.getKey().startsWith(CACHE_PREFIX) || e.getKey().startsWith(CACHE_TIME_PREFIX)) {
                editor.remove(e.getKey());
            }
        }
        editor.apply();
    }

    private static String cacheSuffix(String key) {
        return Integer.toHexString(key.hashCode());
    }

    private static JSONObject toJson(NewsRepository.NewsItem item) {
        JSONObject o = new JSONObject();
        try {
            o.put("title", item.title);
            o.put("link", item.link);
            o.put("source", item.source);
            o.put("date", item.date);
            o.put("publishedAt", item.publishedAt);
        } catch (Exception ignored) {}
        return o;
    }

    private static NewsRepository.NewsItem fromJson(JSONObject o) {
        if (o == null) return null;
        String title = o.optString("title", "");
        String link = o.optString("link", "");
        if (title.isEmpty() || link.isEmpty()) return null;
        return new NewsRepository.NewsItem(
                title,
                link,
                o.optString("source", ""),
                o.optString("date", ""),
                o.optLong("publishedAt", 0L)
        );
    }
}
