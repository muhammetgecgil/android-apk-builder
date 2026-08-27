package com.mgecgil.seslirehber.navigation;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static com.mgecgil.seslirehber.navigation.NavigationModels.*;

/** Android Geocoder wrapper. A candidate is never treated as confirmed until the user approves it. */
public final class AndroidGeocodeResolver implements AutoCloseable {
    public interface Listener {
        void onCandidates(List<GeocodeCandidate> candidates);
        void onError(String message);
    }

    private final Geocoder geocoder;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AndroidGeocodeResolver(Context context) {
        geocoder = new Geocoder(context.getApplicationContext(), new Locale("tr", "TR"));
    }

    public void resolve(String query, Listener listener) {
        if (query == null || query.trim().length() < 2) {
            listener.onError("Hedef adı çok kısa.");
            return;
        }
        if (!Geocoder.isPresent()) {
            listener.onError("Bu cihazda adres çözümleme servisi bulunamadı.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                geocoder.getFromLocationName(query.trim(), 3, new Geocoder.GeocodeListener() {
                    @Override public void onGeocode(List<Address> addresses) {
                        deliver(addresses, listener);
                    }
                    @Override public void onError(String errorMessage) {
                        listener.onError("Hedef adres çözümlenemedi.");
                    }
                });
            } catch (Throwable error) {
                listener.onError("Hedef adres çözümlenemedi.");
            }
        } else {
            executor.execute(() -> {
                try { deliver(geocoder.getFromLocationName(query.trim(), 3), listener); }
                catch (Throwable error) { listener.onError("Hedef adres çözümlenemedi."); }
            });
        }
    }

    private static void deliver(List<Address> addresses, Listener listener) {
        ArrayList<GeocodeCandidate> out = new ArrayList<>();
        if (addresses != null) {
            for (Address a : addresses) {
                if (a == null || !a.hasLatitude() || !a.hasLongitude()) continue;
                String label = a.getAddressLine(0);
                if (label == null || label.trim().isEmpty()) label = a.getFeatureName();
                if (label == null || label.trim().isEmpty()) label = a.getLocality();
                if (label == null || label.trim().isEmpty()) label = "Seçilen hedef";
                out.add(new GeocodeCandidate(label.replaceAll("\\s+", " ").trim(),
                        new GeoPoint(a.getLatitude(), a.getLongitude())));
            }
        }
        if (out.isEmpty()) listener.onError("Bu ad için güvenilir bir hedef bulunamadı.");
        else listener.onCandidates(out);
    }

    @Override public void close() { executor.shutdownNow(); }
}
