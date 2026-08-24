package com.mgai.app;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ResearchClient {
    interface Callback {
        void onSuccess(JSONObject packet);
        void onError(String message);
    }
    private ResearchClient() {}

    static void research(String endpoint, String query, Callback callback) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                URL u = new URL(endpoint);
                c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(30000);
                c.setReadTimeout(120000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                JSONObject body = new JSONObject();
                body.put("query", query);
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
                int status = c.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
                String text = readAll(stream);
                if (status < 200 || status >= 300) {
                    callback.onError("HTTP " + status + (text.isEmpty() ? "" : ": " + text));
                    return;
                }
                callback.onSuccess(new JSONObject(text));
            } catch (Exception e) {
                callback.onError(e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "" : e.getMessage()));
            } finally {
                if (c != null) c.disconnect();
            }
        }, "mg-research-network").start();
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder b = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) b.append(line).append('\n');
        }
        return b.toString().trim();
    }
}
