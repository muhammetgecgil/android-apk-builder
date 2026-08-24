package com.mgai.app;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class MemoryClient {
    interface Callback {
        void onSuccess(JSONObject value);
        void onError(String message);
    }

    private MemoryClient() {}

    static void ingest(String baseEndpoint, String documentId, String text, Callback callback) {
        tryPost(baseEndpoint, "/v1/memory/ingest", new JSONObject()
                .put("document_id", documentId)
                .put("text", text)
                .put("memory_type", "semantic")
                .put("importance", 0.7)
                .put("confidence", 0.7)
                .put("provenance", new JSONObject().put("source", "android-user")), callback);
    }

    static void query(String baseEndpoint, String query, Callback callback) {
        tryPost(baseEndpoint, "/v1/memory/query", new JSONObject()
                .put("query", query)
                .put("top_k", 5), callback);
    }

    private static void tryPost(String baseEndpoint, String path, JSONObject body, Callback callback) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                String base = baseEndpoint == null ? "" : baseEndpoint.trim();
                if (base.endsWith("/")) base = base.substring(0, base.length()-1);
                URL u = new URL(base + path);
                c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(30000);
                c.setReadTimeout(120000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
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
        }, "mg-memory-network").start();
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
