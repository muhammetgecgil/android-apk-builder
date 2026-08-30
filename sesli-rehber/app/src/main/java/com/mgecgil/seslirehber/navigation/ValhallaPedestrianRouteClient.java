package com.mgecgil.seslirehber.navigation;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static com.mgecgil.seslirehber.navigation.NavigationModels.*;

/**
 * Development pedestrian-route provider. Default endpoint is the FOSSGIS Valhalla public demo;
 * it is rate-limited and must be replaced by a controlled/self-hosted endpoint before production.
 */
public final class ValhallaPedestrianRouteClient implements AutoCloseable {
    public interface Listener {
        void onRoute(Route route);
        void onError(String message);
    }

    public static final String DEFAULT_DEMO_ENDPOINT = "https://valhalla.openstreetmap.de/route";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String endpoint;

    public ValhallaPedestrianRouteClient() { this(DEFAULT_DEMO_ENDPOINT); }
    public ValhallaPedestrianRouteClient(String endpoint) {
        this.endpoint = endpoint == null || endpoint.trim().isEmpty() ? DEFAULT_DEMO_ENDPOINT : endpoint;
    }

    public void request(GeoPoint start, GeocodeCandidate destination, Listener listener) {
        executor.execute(() -> {
            try {
                listener.onRoute(fetch(start, destination));
            } catch (Throwable error) {
                listener.onError("Yaya rotası alınamadı. İnternet veya rota servisini kontrol et.");
            }
        });
    }

    Route fetch(GeoPoint start, GeocodeCandidate destination) throws Exception {
        JSONObject body = new JSONObject();
        JSONArray locations = new JSONArray();
        locations.put(new JSONObject()
                .put("lat", start.latitude())
                .put("lon", start.longitude())
                .put("type", "break"));
        locations.put(new JSONObject()
                .put("lat", destination.point().latitude())
                .put("lon", destination.point().longitude())
                .put("type", "break")
                .put("name", destination.label()));
        body.put("locations", locations);
        body.put("costing", "pedestrian");
        body.put("units", "kilometers");
        body.put("language", "tr-TR");
        body.put("directions_type", "instructions");

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(9000);
        connection.setReadTimeout(14000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        // Requested by the public demo policy for end-user clients; also useful for self-host logs.
        connection.setRequestProperty("X-Client-Id", "sesli-rehber-dev-android");
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();
        if (code < 200 || code >= 300) throw new IllegalStateException("route http " + code);
        return parse(response, destination);
    }

    static Route parse(String response, GeocodeCandidate destination) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONObject trip = root.getJSONObject("trip");
        JSONObject summary = trip.getJSONObject("summary");
        JSONArray legs = trip.getJSONArray("legs");
        if (legs.length() == 0) throw new IllegalStateException("empty route");
        JSONObject leg = legs.getJSONObject(0);
        List<GeoPoint> shape = Polyline6.decode(leg.getString("shape"));
        if (shape.size() < 2) throw new IllegalStateException("empty shape");

        ArrayList<Maneuver> maneuvers = new ArrayList<>();
        JSONArray items = leg.optJSONArray("maneuvers");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject m = items.getJSONObject(i);
                String street = "";
                JSONArray streets = m.optJSONArray("street_names");
                if (streets != null && streets.length() > 0) street = streets.optString(0, "");
                maneuvers.add(new Maneuver(
                        m.optInt("type", 0),
                        m.optInt("begin_shape_index", 0),
                        m.optInt("end_shape_index", 0),
                        street,
                        m.optString("instruction", "")));
            }
        }
        double distanceMeters = summary.optDouble("length", 0d) * 1000d;
        double durationSeconds = summary.optDouble("time", 0d);
        return new Route(destination.label(), destination.point(), shape, maneuvers,
                distanceMeters, durationSeconds);
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    @Override public void close() { executor.shutdownNow(); }
}
