package com.mg.hafizadostum.v4;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public final class MemoryStore {
    private static final String PREF = "hafiza_dostum_v4";
    private static final String KEY_TASKS = "tasks";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_OBJECTS = "objects";

    private MemoryStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void ensureDefaults(Context c) {
        if (prefs(c).contains(KEY_TASKS)) return;
        JSONArray a = new JSONArray();
        a.put(task("door", "🚪 Kapıyı kilitledim", 8, 0, true, "1234567", "ev"));
        a.put(task("stove", "🔥 Ocağı kontrol ettim", 8, 5, true, "1234567", "ev"));
        a.put(task("carry", "🔑 Telefon • anahtar • cüzdan", 8, 10, false, "1234567", "cikis"));
        a.put(task("water", "💧 Su içtim", 11, 0, false, "1234567", "gunluk"));
        saveTasks(c, a);
        prefs(c).edit().putString(KEY_EVENTS, "[]").putString(KEY_OBJECTS, "{}").apply();
    }

    public static JSONObject task(String id, String name, int hour, int minute, boolean critical, String days, String category) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("hour", hour);
            o.put("minute", minute);
            o.put("critical", critical);
            o.put("days", days == null ? "1234567" : days);
            o.put("category", category == null ? "genel" : category);
            o.put("active", true);
            o.put("lastDone", 0L);
        } catch (JSONException ignored) {}
        return o;
    }

    public static String newId() {
        return "t_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    public static JSONArray getTasks(Context c) {
        ensureDefaults(c);
        try { return new JSONArray(prefs(c).getString(KEY_TASKS, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    public static void saveTasks(Context c, JSONArray a) {
        prefs(c).edit().putString(KEY_TASKS, a.toString()).apply();
    }

    public static JSONArray getEvents(Context c) {
        try { return new JSONArray(prefs(c).getString(KEY_EVENTS, "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    private static void saveEvents(Context c, JSONArray a) {
        prefs(c).edit().putString(KEY_EVENTS, a.toString()).apply();
    }

    public static JSONObject findTaskById(Context c, String id) {
        JSONArray a = getTasks(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }

    public static boolean markDone(Context c, String id, String source) {
        JSONArray tasks = getTasks(c);
        JSONObject found = null;
        long now = System.currentTimeMillis();
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject o = tasks.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) {
                found = o;
                try { o.put("lastDone", now); } catch (JSONException ignored) {}
                break;
            }
        }
        if (found == null) return false;
        saveTasks(c, tasks);

        JSONArray events = getEvents(c);
        JSONObject e = new JSONObject();
        try {
            e.put("id", UUID.randomUUID().toString());
            e.put("taskId", id);
            e.put("name", found.optString("name"));
            e.put("ts", now);
            e.put("source", source == null ? "app" : source);
            events.put(e);
        } catch (JSONException ignored) {}
        while (events.length() > 400) {
            JSONArray trimmed = new JSONArray();
            for (int i = 1; i < events.length(); i++) trimmed.put(events.opt(i));
            events = trimmed;
        }
        saveEvents(c, events);
        return true;
    }

    public static boolean undoLatest(Context c, String taskId) {
        JSONArray events = getEvents(c);
        int removeIndex = -1;
        for (int i = events.length() - 1; i >= 0; i--) {
            JSONObject e = events.optJSONObject(i);
            if (e != null && taskId.equals(e.optString("taskId"))) { removeIndex = i; break; }
        }
        if (removeIndex < 0) return false;
        JSONArray out = new JSONArray();
        for (int i = 0; i < events.length(); i++) if (i != removeIndex) out.put(events.opt(i));
        saveEvents(c, out);

        long previous = 0L;
        for (int i = out.length() - 1; i >= 0; i--) {
            JSONObject e = out.optJSONObject(i);
            if (e != null && taskId.equals(e.optString("taskId"))) { previous = e.optLong("ts", 0L); break; }
        }
        JSONArray tasks = getTasks(c);
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject t = tasks.optJSONObject(i);
            if (t != null && taskId.equals(t.optString("id"))) {
                try { t.put("lastDone", previous); } catch (JSONException ignored) {}
                break;
            }
        }
        saveTasks(c, tasks);
        return true;
    }

    public static boolean doneToday(JSONObject task) {
        long ts = task == null ? 0L : task.optLong("lastDone", 0L);
        if (ts <= 0L) return false;
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(ts);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean scheduledToday(JSONObject task) {
        if (task == null || !task.optBoolean("active", true)) return false;
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // Sun=1
        int mondayBased = ((dow + 5) % 7) + 1;
        return task.optString("days", "1234567").contains(String.valueOf(mondayBased));
    }

    public static String formatTime(long ts) {
        if (ts <= 0L) return "Kayıt yok";
        return new SimpleDateFormat("HH:mm", new Locale("tr", "TR")).format(new Date(ts));
    }

    public static String formatDateTime(long ts) {
        if (ts <= 0L) return "Kayıt yok";
        return new SimpleDateFormat("dd MMMM yyyy • HH:mm", new Locale("tr", "TR")).format(new Date(ts));
    }

    public static String taskStatus(JSONObject t) {
        if (doneToday(t)) return "✓ Bugün " + formatTime(t.optLong("lastDone")) + "'de yapıldı";
        if (!scheduledToday(t)) return "Bugün planlı değil";
        Calendar n = Calendar.getInstance();
        int now = n.get(Calendar.HOUR_OF_DAY) * 60 + n.get(Calendar.MINUTE);
        int due = t.optInt("hour", 0) * 60 + t.optInt("minute", 0);
        if (now > due) return t.optBoolean("critical") ? "⚠ GECİKTİ • kritik" : "Gecikti";
        int d = due - now;
        if (d <= 60) return "Yaklaşıyor • " + d + " dk";
        return String.format(Locale.getDefault(), "%02d:%02d", t.optInt("hour"), t.optInt("minute"));
    }

    public static String smartNow(Context c) {
        JSONArray a = getTasks(c);
        Calendar n = Calendar.getInstance();
        int now = n.get(Calendar.HOUR_OF_DAY) * 60 + n.get(Calendar.MINUTE);
        JSONObject bestCritical = null, bestDue = null, bestNext = null;
        int bestNextDelta = Integer.MAX_VALUE;
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t == null || !scheduledToday(t) || doneToday(t)) continue;
            int due = t.optInt("hour") * 60 + t.optInt("minute");
            int delta = due - now;
            if (delta < 0 && t.optBoolean("critical")) {
                if (bestCritical == null || due < bestCritical.optInt("hour") * 60 + bestCritical.optInt("minute")) bestCritical = t;
            } else if (delta <= 0 && bestDue == null) {
                bestDue = t;
            } else if (delta > 0 && delta < bestNextDelta) {
                bestNextDelta = delta;
                bestNext = t;
            }
        }
        if (bestCritical != null) return "ÖNCE BUNU YAP\n" + bestCritical.optString("name") + "\n" + taskStatus(bestCritical);
        if (bestDue != null) return "ŞİMDİ UYGUN\n" + bestDue.optString("name") + "\n" + taskStatus(bestDue);
        if (bestNext != null && bestNextDelta <= 120) return "SIRADAKİ\n" + bestNext.optString("name") + "\n" + bestNextDelta + " dakika sonra";
        if (bestNext != null) return "Şimdilik kritik eksiğin görünmüyor.\nSıradaki: " + bestNext.optString("name") + " • " + String.format(Locale.getDefault(), "%02d:%02d", bestNext.optInt("hour"), bestNext.optInt("minute"));
        return "Bugünün planlı işleri tamamlanmış görünüyor. ✓";
    }

    public static String missedToday(Context c) {
        JSONArray a = getTasks(c);
        Calendar n = Calendar.getInstance();
        int now = n.get(Calendar.HOUR_OF_DAY) * 60 + n.get(Calendar.MINUTE);
        StringBuilder critical = new StringBuilder();
        StringBuilder normal = new StringBuilder();
        int count = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t == null || !scheduledToday(t) || doneToday(t)) continue;
            int due = t.optInt("hour") * 60 + t.optInt("minute");
            if (now < due) continue;
            count++;
            if (t.optBoolean("critical")) critical.append("• ").append(t.optString("name")).append("\n");
            else normal.append("• ").append(t.optString("name")).append("\n");
        }
        if (count == 0) return "Şu ana kadar gecikmiş bir iş görünmüyor.";
        StringBuilder out = new StringBuilder();
        if (critical.length() > 0) out.append("Kritik:\n").append(critical).append("\n");
        if (normal.length() > 0) out.append("Diğer:\n").append(normal);
        return out.toString().trim();
    }

    public static int[] progressToday(Context c) {
        JSONArray a = getTasks(c);
        int total = 0, done = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t != null && scheduledToday(t)) {
                total++;
                if (doneToday(t)) done++;
            }
        }
        return new int[]{done, total};
    }

    public static JSONObject findTaskByQuery(Context c, String query) {
        String q = normalize(query);
        JSONArray a = getTasks(c);
        JSONObject best = null;
        int scoreBest = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t == null) continue;
            String n = normalize(t.optString("name"));
            int score = 0;
            if (q.contains(n) || n.contains(q)) score += 10;
            String[] words = q.split("\\s+");
            for (String w : words) if (w.length() >= 3 && n.contains(w)) score += 2;
            if (score > scoreBest) { scoreBest = score; best = t; }
        }
        return scoreBest >= 2 ? best : null;
    }

    public static String answerDidI(Context c, String query) {
        String q = normalize(query);
        if (q.contains("bugun") && (q.contains("unut") || q.contains("eksik"))) return missedToday(c);
        if (q.contains("simdi") && (q.contains("ne") || q.contains("yap"))) return smartNow(c);
        JSONObject t = findTaskByQuery(c, query);
        if (t == null) return "Bununla eşleşen bir rutin bulamadım. İstersen yeni görev olarak ekleyebilirsin.";
        long ts = t.optLong("lastDone", 0L);
        if (ts <= 0L) return t.optString("name") + " için henüz bir 'yaptım' kaydı yok.";
        if (doneToday(t)) return "Evet. " + t.optString("name") + " bugün saat " + formatTime(ts) + "'de yapıldı olarak kaydedildi.";
        return "Bugün yapılmış kaydı görünmüyor. En son " + formatDateTime(ts) + " tarihinde kaydedilmiş.";
    }

    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(new Locale("tr", "TR"))
                .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
                .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
                .replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static JSONObject getObjects(Context c) {
        try { return new JSONObject(prefs(c).getString(KEY_OBJECTS, "{}")); }
        catch (JSONException e) { return new JSONObject(); }
    }

    public static void rememberObject(Context c, String item, String location) {
        JSONObject all = getObjects(c);
        JSONObject o = new JSONObject();
        try {
            o.put("item", item);
            o.put("location", location);
            o.put("ts", System.currentTimeMillis());
            all.put(normalize(item), o);
            prefs(c).edit().putString(KEY_OBJECTS, all.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public static String findObject(Context c, String query) {
        JSONObject all = getObjects(c);
        String q = normalize(query);
        java.util.Iterator<String> keys = all.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            if (k.contains(q) || q.contains(k)) {
                JSONObject o = all.optJSONObject(k);
                if (o != null) return o.optString("item") + " → " + o.optString("location") + "\nKaydedildi: " + formatDateTime(o.optLong("ts"));
            }
        }
        return "Bu eşya için yer kaydı bulamadım.";
    }

    public static void addIfMissing(Context c, JSONObject newTask) {
        JSONArray a = getTasks(c);
        String name = normalize(newTask.optString("name"));
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t != null && normalize(t.optString("name")).equals(name)) return;
        }
        a.put(newTask);
        saveTasks(c, a);
    }

    public static void removeTask(Context c, String id) {
        JSONArray a = getTasks(c), out = new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t != null && !id.equals(t.optString("id"))) out.put(t);
        }
        saveTasks(c, out);
    }

    public static String dailyShareText(Context c) {
        int[] p = progressToday(c);
        StringBuilder s = new StringBuilder("Hafıza Dostum • Günlük özet\n");
        s.append("Tamamlanan: ").append(p[0]).append("/").append(p[1]).append("\n\n");
        JSONArray a = getTasks(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject t = a.optJSONObject(i);
            if (t == null || !scheduledToday(t)) continue;
            s.append(doneToday(t) ? "✓ " : "○ ").append(t.optString("name"));
            if (doneToday(t)) s.append(" • ").append(formatTime(t.optLong("lastDone")));
            s.append("\n");
        }
        return s.toString();
    }
}
