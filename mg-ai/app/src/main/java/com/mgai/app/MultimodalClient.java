package com.mgai.app;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class MultimodalClient {
    interface Callback { void onSuccess(String value); void onError(String error); }
    private MultimodalClient() {}

    static void health(String baseEndpoint, Callback cb) { request(baseEndpoint, "/health", "GET", null, cb); }

    static void postEvent(String baseEndpoint, String modality, String source, String mimeType,
                          String base64, long capturedAtMs, Callback cb) {
        try {
            JSONObject payload = new JSONObject()
                    .put("mime_type", mimeType)
                    .put("encoding", "base64")
                    .put("data", base64)
                    .put("captured_at_ms", capturedAtMs);
            JSONObject body = new JSONObject()
                    .put("modality", modality)
                    .put("source", source)
                    .put("payload", payload)
                    .put("confidence", 1.0)
                    .put("freshness_ms", 0)
                    .put("calibration_state", "device-capture")
                    .put("provenance", new JSONObject()
                            .put("origin", "android-device")
                            .put("captured_at_ms", capturedAtMs));
            request(baseEndpoint, "/v1/perception/event", "POST", body, cb);
        } catch (Exception e) { cb.onError(e.getClass().getSimpleName()+": "+e.getMessage()); }
    }

    private static void request(String baseEndpoint, String path, String method, JSONObject body, Callback cb) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                String base = baseEndpoint == null ? "" : baseEndpoint.trim();
                if (base.endsWith("/")) base = base.substring(0, base.length()-1);
                c = (HttpURLConnection) new URL(base + path).openConnection();
                c.setRequestMethod(method);
                c.setConnectTimeout(15000);
                c.setReadTimeout(120000);
                if (body != null) {
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
                }
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
        }, "mg-multimodal-network").start();
    }
}
