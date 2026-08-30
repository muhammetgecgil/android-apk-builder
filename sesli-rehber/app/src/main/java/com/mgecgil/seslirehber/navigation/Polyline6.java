package com.mgecgil.seslirehber.navigation;

import java.util.ArrayList;
import java.util.List;
import static com.mgecgil.seslirehber.navigation.NavigationModels.GeoPoint;

/** Decoder for Valhalla's encoded polyline with six decimal digits of precision. */
public final class Polyline6 {
    private Polyline6() {}

    public static List<GeoPoint> decode(String encoded) {
        ArrayList<GeoPoint> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return points;
        int index = 0;
        long lat = 0;
        long lon = 0;
        while (index < encoded.length()) {
            DecodeResult a = decodeValue(encoded, index);
            index = a.nextIndex;
            if (index > encoded.length()) break;
            DecodeResult b = decodeValue(encoded, index);
            index = b.nextIndex;
            lat += a.delta;
            lon += b.delta;
            points.add(new GeoPoint(lat / 1_000_000d, lon / 1_000_000d));
        }
        return points;
    }

    private static DecodeResult decodeValue(String encoded, int start) {
        long result = 0;
        int shift = 0;
        int index = start;
        int b;
        do {
            if (index >= encoded.length()) return new DecodeResult(0, encoded.length() + 1);
            b = encoded.charAt(index++) - 63;
            result |= (long) (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20 && shift < 60);
        long delta = (result & 1L) != 0 ? ~(result >> 1) : (result >> 1);
        return new DecodeResult(delta, index);
    }

    private record DecodeResult(long delta, int nextIndex) {}
}
