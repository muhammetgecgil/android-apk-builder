package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Per-station self-healing stream resolver.
 * Keeps a verified preferred stream until it fails, quarantines bad URLs,
 * then discovers and validates alternate URLs for the SAME station.
 */
public final class StreamFallbackManager {
    private static final String PREFS = "stream_health_v3";
    private static final String[] MIRRORS = {
            "https://de1.api.radio-browser.info",
            "https://fi1.api.radio-browser.info",
            "https://nl1.api.radio-browser.info"
    };
    private StreamFallbackManager() {}

    public interface Callback { void onResult(String url); }

    private static String sid(String name) {
        String s = name == null ? "turk_radyo" : name.toLowerCase(new Locale("tr", "TR"));
        s = s.replaceAll("[^a-z0-9çğıöşü]+", "_");
        return s.length() > 80 ? s.substring(0, 80) : s;
    }

    private static String preferredKey(String name) { return "preferred_" + sid(name); }
    private static String goodKey(String name) { return "good_" + sid(name); }
    private static String badKey(String name, String url) { return "bad_" + sid(name) + "_" + Integer.toHexString(url.hashCode()); }

    public static String getPreferred(Context c, String name, String primary) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String u = p.getString(preferredKey(name), "");
        if (!u.isEmpty() && p.getLong(badKey(name, u), 0) < System.currentTimeMillis()) return u;
        return primary;
    }

    public static void markGood(Context c, String name, String url, long startupMs) {
        if (url == null || url.isEmpty()) return;
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray a;
            try { a = new JSONArray(p.getString(goodKey(name), "[]")); } catch (Exception e) { a = new JSONArray(); }
            JSONArray out = new JSONArray();
            JSONObject n = new JSONObject();
            n.put("url", url); n.put("startupMs", startupMs); n.put("lastOk", System.currentTimeMillis());
            out.put(n);
            for (int i = 0; i < a.length() && out.length() < 8; i++) {
                JSONObject x = a.optJSONObject(i);
                if (x != null && !url.equals(x.optString("url"))) out.put(x);
            }
            p.edit().putString(preferredKey(name), url).putString(goodKey(name), out.toString()).remove(badKey(name, url)).apply();
        } catch (Exception ignored) {}
    }

    public static void markBad(Context c, String name, String url, long quarantineMs) {
        if (url == null || url.isEmpty()) return;
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long until = System.currentTimeMillis() + Math.max(60_000L, quarantineMs);
        SharedPreferences.Editor e = p.edit().putLong(badKey(name, url), until);
        if (url.equals(p.getString(preferredKey(name), ""))) e.remove(preferredKey(name));
        e.apply();
    }

    public static List<String> cachedCandidates(Context c, String name, String primary) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String pref = p.getString(preferredKey(name), "");
        addIfHealthy(p, name, pref, out, seen);
        try {
            JSONArray a = new JSONArray(p.getString(goodKey(name), "[]"));
            for (int i = 0; i < a.length(); i++) addIfHealthy(p, name, a.optJSONObject(i) == null ? "" : a.optJSONObject(i).optString("url"), out, seen);
        } catch (Exception ignored) {}
        addIfHealthy(p, name, primary, out, seen);
        return out;
    }

    private static void addIfHealthy(SharedPreferences p, String name, String u, List<String> out, Set<String> seen) {
        if (u == null || u.isEmpty() || !seen.add(u)) return;
        if (p.getLong(badKey(name, u), 0) < System.currentTimeMillis()) out.add(u);
    }

    public static void discoverBestAsync(Context c, String name, String primary, Callback cb) {
        new Thread(() -> {
            String best = null;
            long bestMs = Long.MAX_VALUE;
            for (String u : cachedCandidates(c, name, primary)) {
                long ms = probe(u);
                if (ms >= 0 && ms < bestMs) { best = u; bestMs = ms; }
            }
            if (best == null) {
                for (Candidate x : searchDirectory(name)) {
                    if (sameUrl(primary, x.url)) continue;
                    long ms = probe(x.url);
                    if (ms >= 0 && ms < bestMs) { best = x.url; bestMs = ms; }
                    if (bestMs < 850) break;
                }
            }
            final String result = best;
            final long latency = bestMs;
            if (result != null) markGood(c, name, result, latency);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onResult(result));
        }, "stream-repair").start();
    }

    private static boolean sameUrl(String a, String b) { return a != null && a.equals(b); }

    private static final class Candidate {
        String url; int score;
        Candidate(String u, int s) { url = u; score = s; }
    }

    private static List<Candidate> searchDirectory(String name) {
        ArrayList<Candidate> out = new ArrayList<>();
        String enc;
        try { enc = URLEncoder.encode(name == null ? "" : name, "UTF-8"); } catch (Exception e) { return out; }
        for (String base : MIRRORS) {
            HttpURLConnection h = null;
            try {
                URL u = new URL(base + "/json/stations/search?countrycode=TR&hidebroken=true&limit=80&order=clickcount&reverse=true&name=" + enc);
                h = (HttpURLConnection) u.openConnection();
                h.setConnectTimeout(3500); h.setReadTimeout(5000); h.setRequestProperty("User-Agent", "TurkRadyo/2.0.7");
                if (h.getResponseCode() / 100 != 2) continue;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), StandardCharsets.UTF_8))) {
                    String line; while ((line = r.readLine()) != null && sb.length() < 1_500_000) sb.append(line);
                }
                JSONArray a = new JSONArray(sb.toString());
                String target = norm(name);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.optJSONObject(i); if (o == null) continue;
                    String n = o.optString("name"), url = o.optString("url_resolved", o.optString("url"));
                    if (url.isEmpty()) continue;
                    int sim = similarity(target, norm(n)); if (sim < 65) continue;
                    int score = sim * 2 + (o.optInt("lastcheckok") == 1 ? 50 : 0) + Math.min(30, o.optInt("bitrate") / 8) + Math.min(20, o.optInt("votes") / 10);
                    out.add(new Candidate(url, score));
                }
                if (!out.isEmpty()) break;
            } catch (Exception ignored) {} finally { if (h != null) h.disconnect(); }
        }
        Collections.sort(out, (a,b) -> Integer.compare(b.score, a.score));
        return out.size() > 16 ? new ArrayList<>(out.subList(0, 16)) : out;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase(new Locale("tr","TR"))
                .replace(" radyo ", " ").replace(" radio ", " ").replace(" fm ", " ")
                .replaceAll("[^a-z0-9çğıöşü]+", " ").trim();
    }

    private static int similarity(String a, String b) {
        if (a.equals(b)) return 100;
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.contains(b) || b.contains(a)) return 88;
        Set<String> A = new HashSet<>(), B = new HashSet<>();
        for (String x : a.split(" ")) if (x.length() > 2) A.add(x);
        for (String x : b.split(" ")) if (x.length() > 2) B.add(x);
        int hit = 0; for (String x : A) if (B.contains(x)) hit++;
        return (int) (70.0 * hit / Math.max(1, Math.max(A.size(), B.size())));
    }

    /** Returns first-byte/segment latency, or -1 if the stream is not actually readable. */
    private static long probe(String raw) {
        HttpURLConnection h = null; InputStream in = null;
        try {
            long t0 = System.currentTimeMillis();
            h = (HttpURLConnection) new URL(raw).openConnection();
            h.setInstanceFollowRedirects(true); h.setConnectTimeout(2800); h.setReadTimeout(3800);
            h.setRequestProperty("User-Agent", "TurkRadyo/2.0.7"); h.setRequestProperty("Icy-MetaData", "0");
            int code = h.getResponseCode(); if (code < 200 || code >= 400) return -1;
            String ct = String.valueOf(h.getContentType()).toLowerCase(Locale.ROOT);
            in = new BufferedInputStream(h.getInputStream(), 8192);
            byte[] buf = new byte[8192]; int total = 0, n;
            while (total < 8192 && (n = in.read(buf, 0, Math.min(buf.length, 8192-total))) > 0) total += n;
            long ms = System.currentTimeMillis() - t0;
            if (total < 64) return -1;
            if (raw.toLowerCase(Locale.ROOT).contains(".m3u8") || ct.contains("mpegurl")) return total >= 128 ? ms : -1;
            return total >= 2048 ? ms : -1;
        } catch (Exception e) { return -1; }
        finally { try { if (in != null) in.close(); } catch (Exception ignored) {} if (h != null) h.disconnect(); }
    }
}
