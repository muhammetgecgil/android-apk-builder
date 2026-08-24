package com.mgai.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ModelClient {
    interface Callback {
        void onSuccess(String text);
        void onError(String message);
    }

    private ModelClient() {}

    static void chat(String endpoint, String model, String apiKey, JSONArray conversation, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                if (endpoint == null || endpoint.trim().isEmpty()) {
                    callback.onError("MG-Core endpoint ayarlanmamış.");
                    return;
                }
                if (model == null || model.trim().isEmpty()) {
                    callback.onError("Model adı ayarlanmamış.");
                    return;
                }

                URL url = new URL(endpoint.trim());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(120000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json");
                addAuth(connection, apiKey);

                JSONArray messages = new JSONArray();
                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content", "Sen MG-AI çekirdeğisin. Doğru, açık ve kanıta dayalı cevap ver. Bilmediğin şeyi uydurma; belirsizliği belirt. Fiziksel robot eylemlerinde yalnız yüksek seviyeli plan üret; güvenlik denetçisini veya gerçek zamanlı kontrol katmanını atlama.");
                messages.put(system);
                for (int i = 0; i < conversation.length(); i++) messages.put(conversation.getJSONObject(i));

                JSONObject body = new JSONObject();
                body.put("model", model.trim());
                body.put("messages", messages);
                body.put("temperature", 0.35);
                body.put("stream", false);

                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = connection.getOutputStream()) { os.write(bytes); }

                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = readAll(stream);
                if (status < 200 || status >= 300) {
                    callback.onError("HTTP " + status + (response.isEmpty() ? "" : ": " + shorten(response, 500)));
                    return;
                }

                JSONObject json = new JSONObject(response);
                JSONArray choices = json.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    callback.onError("Model cevabında 'choices' alanı yok.");
                    return;
                }
                JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                String content = message == null ? "" : message.optString("content", "").trim();
                if (content.isEmpty()) {
                    callback.onError("Model boş cevap döndürdü.");
                    return;
                }
                callback.onSuccess(content);
            } catch (Exception e) {
                String msg = e.getMessage();
                callback.onError(e.getClass().getSimpleName() + (msg == null ? "" : ": " + msg));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "mg-core-chat").start();
    }

    static void testConnection(String chatEndpoint, String expectedModel, String apiKey, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String modelsEndpoint = deriveModelsEndpoint(chatEndpoint);
                URL url = new URL(modelsEndpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("Accept", "application/json");
                addAuth(connection, apiKey);

                long started = System.nanoTime();
                int status = connection.getResponseCode();
                long latencyMs = (System.nanoTime() - started) / 1_000_000L;
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = readAll(stream);
                if (status < 200 || status >= 300) {
                    callback.onError("MG-Core erişilemiyor: HTTP " + status + (response.isEmpty() ? "" : " • " + shorten(response, 300)));
                    return;
                }

                JSONObject json = new JSONObject(response);
                JSONArray data = json.optJSONArray("data");
                if (data == null) {
                    callback.onError("Sunucu cevap verdi fakat /v1/models OpenAI-compatible biçimde değil.");
                    return;
                }
                boolean found = false;
                StringBuilder models = new StringBuilder();
                for (int i = 0; i < data.length(); i++) {
                    String id = data.getJSONObject(i).optString("id", "");
                    if (id.isEmpty()) continue;
                    if (models.length() > 0) models.append(", ");
                    models.append(id);
                    if (expectedModel != null && !expectedModel.trim().isEmpty() && id.equals(expectedModel.trim())) found = true;
                }
                if (expectedModel != null && !expectedModel.trim().isEmpty() && !found) {
                    callback.onError("Sunucu sağlıklı (" + latencyMs + " ms) fakat beklenen model bulunamadı: " + expectedModel + ". Sunucudaki modeller: " + models);
                    return;
                }
                callback.onSuccess("MG-Core bağlantısı başarılı • " + latencyMs + " ms • model: " + (expectedModel == null || expectedModel.trim().isEmpty() ? models : expectedModel.trim()));
            } catch (Exception e) {
                String msg = e.getMessage();
                callback.onError(e.getClass().getSimpleName() + (msg == null ? "" : ": " + msg));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "mg-core-health").start();
    }

    private static String deriveModelsEndpoint(String chatEndpoint) throws Exception {
        if (chatEndpoint == null || chatEndpoint.trim().isEmpty()) throw new IllegalArgumentException("MG-Core endpoint ayarlanmamış.");
        URL u = new URL(chatEndpoint.trim());
        String path = u.getPath();
        int v1 = path.indexOf("/v1/");
        String basePath = v1 >= 0 ? path.substring(0, v1) : "";
        return new URL(u.getProtocol(), u.getHost(), u.getPort(), basePath + "/v1/models").toString();
    }

    private static void addAuth(HttpURLConnection connection, String apiKey) {
        if (apiKey != null && !apiKey.trim().isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
        }
        return out.toString().trim();
    }

    private static String shorten(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
