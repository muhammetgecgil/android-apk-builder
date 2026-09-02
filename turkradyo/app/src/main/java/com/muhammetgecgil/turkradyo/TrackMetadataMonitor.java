package com.muhammetgecgil.turkradyo;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TrackMetadataMonitor {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Pattern TITLE_QUOTED = Pattern.compile("StreamTitle\\s*=\\s*['\\\"](.*?)['\\\"]\\s*;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TITLE_BARE = Pattern.compile("StreamTitle\\s*=\\s*([^;]+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final int MAX_TRACKS = 500;
    private static final long POLL_MS = 10_000L;

    private static Context app;
    private static String station = "";
    private static String url = "";
    private static String lastTitle = "";
    private static int generation = 0;
    private static boolean busy = false;

    private TrackMetadataMonitor() {}

    static synchronized void start(Context context, String stationName, String streamUrl) {
        if (context == null) return;
        String nextUrl = clean(streamUrl);
        String nextStation = clean(stationName);
        if (nextUrl.isEmpty()) {
            stop();
            return;
        }
        app = context.getApplicationContext();
        boolean changed = !nextUrl.equals(url) || !nextStation.equals(station);
        station = nextStation.isEmpty() ? "Türk Radyo" : nextStation;
        url = nextUrl;
        if (changed) lastTitle = "";
        generation++;
        int token = generation;
        MAIN.removeCallbacksAndMessages(TrackMetadataMonitor.class);
        schedule(token, changed ? 1200L : 2500L);
    }

    static synchronized void stop() {
        generation++;
        url = "";
        station = "";
        lastTitle = "";
        busy = false;
        MAIN.removeCallbacksAndMessages(TrackMetadataMonitor.class);
    }

    private static void schedule(final int token, long delay) {
        MAIN.postAtTime(() -> poll(token), TrackMetadataMonitor.class, android.os.SystemClock.uptimeMillis() + delay);
    }

    private static void poll(final int token) {
        final Context c;
        final String u;
        final String s;
        synchronized (TrackMetadataMonitor.class) {
            if (token != generation || app == null || url.isEmpty()) return;
            if (busy) {
                schedule(token, 1500L);
                return;
            }
            c = app;
            u = url;
            s = station;
            if (!isAudioActive(c)) {
                writeStatus(c, s, "PAUSED", false, "");
                schedule(token, 3500L);
                return;
            }
            busy = true;
        }
        IO.execute(() -> {
            String title = "";
            String error = "";
            boolean supported = false;
            try {
                MetaResult r = readIcy(u);
                title = r.title;
                supported = r.supported;
            } catch (Exception e) {
                error = e.getClass().getSimpleName();
            }
            final String found = cleanTitle(title);
            final boolean metaSupported = supported;
            final String err = error;
            MAIN.post(() -> {
                synchronized (TrackMetadataMonitor.class) {
                    busy = false;
                    if (token != generation || !u.equals(url)) return;
                    if (!found.isEmpty()) {
                        if (!sameTitle(lastTitle, found)) {
                            lastTitle = found;
                            record(c, s, found, "ICY");
                        }
                        writeStatus(c, s, "LIVE", true, found);
                    } else {
                        writeStatus(c, s, metaSupported ? "WAITING_TITLE" : "NO_METADATA", metaSupported, err);
                    }
                    schedule(token, POLL_MS);
                }
            });
        });
    }

    private static boolean isAudioActive(Context c) {
        try {
            AudioManager a = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            return a != null && a.isMusicActive();
        } catch (Exception e) {
            return true;
        }
    }

    private static MetaResult readIcy(String streamUrl) throws Exception {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL u = new URL(streamUrl);
            String scheme = u.getProtocol();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return new MetaResult("", false);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(5500);
            conn.setReadTimeout(6500);
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Icy-MetaData", "1");
            conn.setRequestProperty("User-Agent", "TurkRadyo/2.7.3 Android");
            conn.setRequestProperty("Accept", "*/*");
            conn.connect();
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) return new MetaResult("", false);
            int metaInt = parseInt(header(conn, "icy-metaint"));
            if (metaInt <= 0 || metaInt > 1_048_576) return new MetaResult("", false);
            in = conn.getInputStream();
            if (!discardExactly(in, metaInt)) return new MetaResult("", true);
            int blocks = in.read();
            if (blocks < 0) return new MetaResult("", true);
            int len = blocks * 16;
            if (len <= 0) return new MetaResult("", true);
            ByteArrayOutputStream out = new ByteArrayOutputStream(len);
            byte[] buf = new byte[Math.min(4096, len)];
            int left = len;
            while (left > 0) {
                int n = in.read(buf, 0, Math.min(buf.length, left));
                if (n < 0) break;
                out.write(buf, 0, n);
                left -= n;
            }
            byte[] raw = out.toByteArray();
            String meta = decode(raw).replace("\u0000", "").trim();
            Matcher m = TITLE_QUOTED.matcher(meta);
            if (m.find()) return new MetaResult(m.group(1), true);
            m = TITLE_BARE.matcher(meta);
            if (m.find()) return new MetaResult(m.group(1), true);
            return new MetaResult("", true);
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    private static boolean discardExactly(InputStream in, int count) throws Exception {
        byte[] b = new byte[8192];
        int left = count;
        while (left > 0) {
            int n = in.read(b, 0, Math.min(b.length, left));
            if (n < 0) return false;
            left -= n;
        }
        return true;
    }

    private static String header(HttpURLConnection c, String name) {
        try {
            String v = c.getHeaderField(name);
            if (v != null) return v;
            for (Map.Entry<String, List<String>> e : c.getHeaderFields().entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name) && e.getValue() != null && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s == null ? "" : s.trim()); } catch (Exception e) { return -1; }
    }

    private static String decode(byte[] raw) {
        try {
            String u = new String(raw, StandardCharsets.UTF_8);
            if (!u.contains("\uFFFD")) return u;
        } catch (Exception ignored) {}
        try { return new String(raw, "ISO-8859-1"); } catch (Exception e) { return ""; }
    }

    private static String cleanTitle(String t) {
        String x = clean(t).replaceAll("\\s+", " ").trim();
        x = x.replaceAll("^[\\-–—|•\\s]+|[\\-–—|•\\s]+$", "").trim();
        if (x.length() > 240) x = x.substring(0, 240).trim();
        if (x.equalsIgnoreCase("unknown") || x.equalsIgnoreCase("null") || x.equals("-")) return "";
        return x;
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean sameTitle(String a, String b) {
        return clean(a).equalsIgnoreCase(clean(b));
    }

    private static void record(Context c, String stationName, String title, String source) {
        try {
            SharedPreferences p = c.getSharedPreferences("radio", Context.MODE_PRIVATE);
            JSONArray old;
            try { old = new JSONArray(p.getString("tracks", "[]")); } catch (Exception e) { old = new JSONArray(); }
            JSONArray out = new JSONArray();
            JSONObject n = new JSONObject();
            n.put("title", title);
            n.put("station", clean(stationName).isEmpty() ? "Türk Radyo" : clean(stationName));
            n.put("time", System.currentTimeMillis());
            n.put("source", source);
            out.put(n);
            for (int i = 0; i < old.length() && out.length() < MAX_TRACKS; i++) {
                JSONObject x = old.optJSONObject(i);
                if (x == null) continue;
                String xt = x.optString("title", "");
                String xs = x.optString("station", "");
                long tm = x.optLong("time", 0);
                if (i == 0 && sameTitle(xt, title) && sameTitle(xs, stationName) && System.currentTimeMillis() - tm < 90_000L) continue;
                out.put(x);
            }
            p.edit().putString("tracks", out.toString()).putString("nowTitle", title).apply();
        } catch (Exception ignored) {}
    }

    private static void writeStatus(Context c, String stationName, String state, boolean supported, String detail) {
        try {
            JSONObject o = new JSONObject();
            o.put("station", stationName);
            o.put("state", state);
            o.put("metadataSupported", supported);
            o.put("detail", detail == null ? "" : detail);
            o.put("time", System.currentTimeMillis());
            c.getSharedPreferences("radio", Context.MODE_PRIVATE).edit().putString("trackMetaStatus", o.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static final class MetaResult {
        final String title;
        final boolean supported;
        MetaResult(String title, boolean supported) { this.title = title == null ? "" : title; this.supported = supported; }
    }
}
