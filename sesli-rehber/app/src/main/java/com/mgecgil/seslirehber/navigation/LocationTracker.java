package com.mgecgil.seslirehber.navigation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import java.util.HashSet;
import java.util.Set;
import static com.mgecgil.seslirehber.navigation.NavigationModels.*;

/** Precise foreground pedestrian location updates. Coarse-only location is not used for turns. */
public final class LocationTracker implements AutoCloseable {
    public interface Listener {
        void onLocation(LocationFix fix);
        void onProviderProblem(String message);
    }

    private final Context context;
    private final LocationManager manager;
    private final Listener listener;
    private final Set<String> registeredProviders = new HashSet<>();
    private boolean running;

    private final LocationListener locationListener = new LocationListener() {
        @Override public void onLocationChanged(Location location) {
            if (location == null) return;
            listener.onLocation(new LocationFix(
                    new GeoPoint(location.getLatitude(), location.getLongitude()),
                    location.hasAccuracy() ? location.getAccuracy() : 999f,
                    location.hasBearing() ? location.getBearing() : Float.NaN,
                    location.hasSpeed() ? location.getSpeed() : 0f,
                    location.getTime()));
        }
        @Override public void onProviderDisabled(String provider) {
            listener.onProviderProblem("Konum sağlayıcısı kapalı: " + provider);
        }
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    public LocationTracker(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        manager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean hasFinePermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean start() {
        if (running) return true;
        if (!hasFinePermission() || manager == null) return false;
        registeredProviders.clear();
        registerIfAvailable(LocationManager.GPS_PROVIDER);
        registerIfAvailable(LocationManager.NETWORK_PROVIDER);
        if (registeredProviders.isEmpty()) {
            listener.onProviderProblem("Konum sağlayıcısı kullanılamıyor. GPS ve konumu aç.");
            return false;
        }
        running = true;
        deliverBestLastKnown();
        return true;
    }

    private void registerIfAvailable(String provider) {
        try {
            if (manager.getProvider(provider) == null || !manager.isProviderEnabled(provider)) return;
            manager.requestLocationUpdates(provider, 1000L, 1f, locationListener, Looper.getMainLooper());
            registeredProviders.add(provider);
        } catch (SecurityException ignored) {
        } catch (Throwable ignored) {
        }
    }

    private void deliverBestLastKnown() {
        Location best = null;
        for (String provider : registeredProviders) {
            try {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate == null) continue;
                if (best == null
                        || candidate.getTime() > best.getTime()
                        || (candidate.hasAccuracy() && best.hasAccuracy()
                        && candidate.getAccuracy() < best.getAccuracy() * 0.65f)) {
                    best = candidate;
                }
            } catch (SecurityException ignored) {}
        }
        if (best != null && System.currentTimeMillis() - best.getTime() <= 120_000L) {
            locationListener.onLocationChanged(best);
        }
    }

    public void stop() {
        if (manager != null) {
            try { manager.removeUpdates(locationListener); } catch (Throwable ignored) {}
        }
        registeredProviders.clear();
        running = false;
    }

    public boolean isRunning() { return running; }
    @Override public void close() { stop(); }
}
