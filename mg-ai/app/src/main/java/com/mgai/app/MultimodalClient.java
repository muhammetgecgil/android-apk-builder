package com.mgai.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class MultimodalClient {
    interface Callback { void onSuccess(String value); void onError(String error); }
    private MultimodalClient() {}

    static void health(String baseEndpoint, Callback cb) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                String base = baseEndpoint == null ? "" : baseEndpoint.trim();
                if (base.endsWith("/")) base = base.substring(0, base.length()-1);
                c = (HttpURLConnection) new URL(base + "/health").openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(12000);
                c.setReadTimeout(12000);
                int status = c.getResponseCode();
                InputStream s = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
                StringBuilder b = new StringBuilder();
                if (s != null) try (BufferedReader r = new BufferedReader(new InputStreamReader(s, StandardCharsets.UTF_8))) {
                    String line; while ((line = r.readLine()) != null) b.append(line).append('\n');
                }
                String text = b.toString().trim();
                if (status >= 200 && status < 300) cb.onSuccess(text); else cb.onError("HTTP " + status + ": " + text);
            } catch (Exception e) { cb.onError(e.getClass().getSimpleName()+": "+(e.getMessage()==null?"":e.getMessage())); }
            finally { if (c != null) c.disconnect(); }
        }, "mg-multimodal-health").start();
    }
}
