package com.mgecgil.seslirehber.navigation;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.navigation.NavigationModels.GeoPoint;

public class Polyline6Test {
    @Test public void decodesSixDigitPolylineWithoutCoordinateDrift() {
        List<GeoPoint> input = List.of(
                new GeoPoint(40.992345, 29.124567),
                new GeoPoint(40.992421, 29.124701),
                new GeoPoint(40.992502, 29.124812));
        String encoded = encode(input);
        List<GeoPoint> decoded = Polyline6.decode(encoded);
        assertEquals(input.size(), decoded.size());
        for (int i = 0; i < input.size(); i++) {
            assertEquals(input.get(i).latitude(), decoded.get(i).latitude(), 0.0000011);
            assertEquals(input.get(i).longitude(), decoded.get(i).longitude(), 0.0000011);
        }
    }

    private static String encode(List<GeoPoint> points) {
        StringBuilder out = new StringBuilder();
        long lastLat = 0, lastLon = 0;
        for (GeoPoint p : points) {
            long lat = Math.round(p.latitude() * 1_000_000d);
            long lon = Math.round(p.longitude() * 1_000_000d);
            encodeDelta(lat - lastLat, out);
            encodeDelta(lon - lastLon, out);
            lastLat = lat;
            lastLon = lon;
        }
        return out.toString();
    }

    private static void encodeDelta(long delta, StringBuilder out) {
        long value = delta < 0 ? ~(delta << 1) : (delta << 1);
        while (value >= 0x20) {
            out.append((char) ((0x20 | (value & 0x1f)) + 63));
            value >>= 5;
        }
        out.append((char) (value + 63));
    }
}
