package com.mgecgil.seslirehber.navigation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.core.content.ContextCompat;
import java.lang.ref.WeakReference;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import static com.mgecgil.seslirehber.navigation.NavigationModels.*;

/**
 * M3 pedestrian-navigation coordinator: geocode -> spoken confirmation -> precise location ->
 * pedestrian route -> maneuver progress -> persistent off-route reroute.
 */
public final class NavigationCoordinator implements AutoCloseable {
    public interface Output {
        void speakSystem(String text);
        void speakNavigation(String text);
    }

    private static final int LOCATION_REQUEST_CODE = 4107;
    private static final long REROUTE_COOLDOWN_MS = 45_000L;

    private final Context appContext;
    private final WeakReference<Activity> activityRef;
    private final Output output;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AndroidGeocodeResolver geocoder;
    private final ValhallaPedestrianRouteClient routeClient = new ValhallaPedestrianRouteClient();
    private final NavigationProgressEngine progress = new NavigationProgressEngine();
    private final LocationTracker locationTracker;
    private final NavigationVoiceBridge.Handler voiceHandler = this::handleNavigationVoice;

    private GeocodeCandidate confirmedDestination;
    private GeocodeCandidate pendingCandidate;
    private LocationFix lastFix;
    private Route currentRoute;
    private boolean routeRequestInFlight;
    private boolean closed;
    private int generation;
    private long lastRouteRequestElapsed;

    public NavigationCoordinator(Context context, Output output) {
        appContext = context.getApplicationContext();
        activityRef = new WeakReference<>(context instanceof Activity ? (Activity) context : null);
        this.output = output;
        geocoder = new AndroidGeocodeResolver(appContext);
        locationTracker = new LocationTracker(appContext, new LocationTracker.Listener() {
            @Override public void onLocation(LocationFix fix) { onLocationFix(fix); }
            @Override public void onProviderProblem(String message) { output.speakSystem(message); }
        });
    }

    public void requestDestination(String rawTarget) {
        if (closed) return;
        final String target = rawTarget == null ? "" : rawTarget.trim();
        if (target.length() < 2) {
            output.speakSystem("Hedefi anlayamadım. Adresi veya yer adını tekrar söyle.");
            return;
        }
        final int requestGeneration = ++generation;
        stopRouteOnly();
        pendingCandidate = null;
        confirmedDestination = null;
        NavigationVoiceBridge.clear(voiceHandler);
        output.speakSystem(target + " hedefini arıyorum.");
        geocoder.resolve(target, new AndroidGeocodeResolver.Listener() {
            @Override public void onCandidates(List<GeocodeCandidate> candidates) {
                mainHandler.post(() -> {
                    if (closed || requestGeneration != generation || candidates == null || candidates.isEmpty()) return;
                    pendingCandidate = candidates.get(0);
                    NavigationVoiceBridge.install(voiceHandler);
                    output.speakSystem("Hedef olarak " + spokenLabel(pendingCandidate.label())
                            + " buldum. Bu hedef doğru mu? Evet veya hayır de.");
                });
            }
            @Override public void onError(String message) {
                mainHandler.post(() -> {
                    if (requestGeneration != generation || closed) return;
                    NavigationVoiceBridge.clear(voiceHandler);
                    output.speakSystem(message);
                });
            }
        });
    }

    private boolean handleNavigationVoice(String rawText) {
        if (closed) return false;
        String normalized = normalize(rawText);
        if (pendingCandidate != null) {
            if (equalsAny(normalized, "evet", "dogru", "evet dogru", "onayliyorum", "baslat")) {
                confirmedDestination = pendingCandidate;
                pendingCandidate = null;
                // Keep the bridge installed while navigation is active so explicit route-stop
                // commands remain available hands-free. Unrelated commands fall through to M2.
                mainHandler.post(this::ensureLocationPermissionAndStart);
                return true;
            }
            if (equalsAny(normalized, "hayir", "yanlis", "iptal", "vazgec", "vazgectim")) {
                cancelAll("Hedef iptal edildi.");
                return true;
            }
        }

        if (equalsAny(normalized,
                "navigasyonu durdur", "rotayi durdur", "rotayi iptal", "navigasyonu iptal")) {
            cancelAll("Yaya navigasyonu durduruldu.");
            return true;
        }

        // A global guidance-stop command must also stop navigation, but return false so the main
        // offline intent parser can disable the camera guidance in the same utterance.
        if (equalsAny(normalized,
                "rehberligi durdur", "yonlendirmeyi durdur", "yurumeyi durdur", "dur")) {
            cancelAll(null);
            return false;
        }
        return false;
    }

    private void ensureLocationPermissionAndStart() {
        if (hasFineLocation()) {
            startLocationTracking();
            return;
        }
        Activity activity = activityRef.get();
        if (activity == null) {
            output.speakSystem("Yaya navigasyonu için hassas konum izni gerekli.");
            return;
        }
        output.speakSystem("Yaya navigasyonu için hassas konum iznini onayla.");
        activity.requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_REQUEST_CODE);
        pollPermission(0);
    }

    private void pollPermission(int attempt) {
        if (closed || confirmedDestination == null) return;
        if (hasFineLocation()) {
            startLocationTracking();
            return;
        }
        if (attempt >= 24) {
            output.speakSystem("Hassas konum izni verilmedi. Yaya rotası başlatılmadı.");
            cancelAll(null);
            return;
        }
        mainHandler.postDelayed(() -> pollPermission(attempt + 1), 500L);
    }

    private boolean hasFineLocation() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationTracking() {
        if (confirmedDestination == null || closed) return;
        if (!locationTracker.start()) {
            output.speakSystem("Hassas konum alınamadı. GPS ve konum hizmetlerini aç.");
            return;
        }
        output.speakSystem("Konum alınıyor. Rota, yeterli GPS doğruluğu oluşunca hesaplanacak.");
        if (lastFix != null) maybeRequestRoute(lastFix, false);
    }

    private void onLocationFix(LocationFix fix) {
        if (closed || fix == null) return;
        lastFix = fix;
        if (confirmedDestination == null) return;
        if (currentRoute == null) {
            maybeRequestRoute(fix, false);
            return;
        }
        for (NavigationEvent event : progress.update(fix)) {
            switch (event.type()) {
                case LOCATION_UNCERTAIN -> output.speakNavigation(event.speech());
                case PREPARE, MANEUVER -> output.speakNavigation(event.speech());
                case OFF_ROUTE -> output.speakNavigation(event.speech());
                case REROUTE_REQUEST -> maybeRequestRoute(fix, true);
                case ARRIVED -> {
                    output.speakNavigation(event.speech());
                    stopRouteOnly();
                    confirmedDestination = null;
                    NavigationVoiceBridge.clear(voiceHandler);
                }
            }
        }
    }

    private void maybeRequestRoute(LocationFix fix, boolean reroute) {
        if (closed || fix == null || confirmedDestination == null || routeRequestInFlight) return;
        if (fix.accuracyMeters() <= 0f || fix.accuracyMeters() > 38f) return;
        long now = SystemClock.elapsedRealtime();
        if (reroute && now - lastRouteRequestElapsed < REROUTE_COOLDOWN_MS) return;
        routeRequestInFlight = true;
        lastRouteRequestElapsed = now;
        if (reroute) output.speakNavigation("Yeni yaya rotası hesaplanıyor.");
        routeClient.request(fix.point(), confirmedDestination, new ValhallaPedestrianRouteClient.Listener() {
            @Override public void onRoute(Route route) {
                mainHandler.post(() -> {
                    routeRequestInFlight = false;
                    if (closed || confirmedDestination == null) return;
                    currentRoute = route;
                    progress.setRoute(route);
                    int meters = (int) Math.round(route.distanceMeters());
                    int minutes = Math.max(1, (int) Math.round(route.durationSeconds() / 60d));
                    output.speakNavigation((reroute ? "Yeni rota hazır. " : "Yaya rotası hazır. ")
                            + "Yaklaşık " + readableDistance(meters) + ", " + minutes + " dakika. "
                            + "Engel ve zemin uyarıları rota talimatlarından önceliklidir.");
                    LocationFix latest = lastFix;
                    if (latest != null) onLocationFix(latest);
                });
            }
            @Override public void onError(String message) {
                mainHandler.post(() -> {
                    routeRequestInFlight = false;
                    if (!closed) output.speakSystem(message);
                });
            }
        });
    }

    public void stopNavigation() { cancelAll("Yaya navigasyonu durduruldu."); }

    private void cancelAll(String speech) {
        generation++;
        NavigationVoiceBridge.clear(voiceHandler);
        pendingCandidate = null;
        confirmedDestination = null;
        stopRouteOnly();
        if (speech != null && !speech.isEmpty()) output.speakSystem(speech);
    }

    private void stopRouteOnly() {
        locationTracker.stop();
        currentRoute = null;
        routeRequestInFlight = false;
        progress.setRoute(null);
    }

    private static String readableDistance(int meters) {
        if (meters < 1000) return meters + " metre";
        double km = meters / 1000d;
        return String.format(new Locale("tr", "TR"), "%.1f kilometre", km);
    }

    private static String spokenLabel(String label) {
        if (label == null) return "seçilen hedef";
        String clean = label.replaceAll("\\s+", " ").trim();
        return clean.length() > 140 ? clean.substring(0, 140) : clean;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        String s = input.toLowerCase(new Locale("tr", "TR"))
                .replace('ı','i').replace('ğ','g').replace('ü','u')
                .replace('ş','s').replace('ö','o').replace('ç','c');
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static boolean equalsAny(String value, String... choices) {
        for (String c : choices) if (value.equals(c)) return true;
        return false;
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        generation++;
        NavigationVoiceBridge.clear(voiceHandler);
        mainHandler.removeCallbacksAndMessages(null);
        locationTracker.close();
        routeClient.close();
        geocoder.close();
    }
}
