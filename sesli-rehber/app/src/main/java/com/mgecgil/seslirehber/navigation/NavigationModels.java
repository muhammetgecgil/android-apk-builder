package com.mgecgil.seslirehber.navigation;

import java.util.List;

/** Pure navigation data models; no Android dependency so route progress can be unit-tested. */
public final class NavigationModels {
    private NavigationModels() {}

    public record GeoPoint(double latitude, double longitude) {}

    public record GeocodeCandidate(String label, GeoPoint point) {}

    public record Maneuver(
            int type,
            int beginShapeIndex,
            int endShapeIndex,
            String streetName,
            String providerInstruction) {}

    public record Route(
            String destinationLabel,
            GeoPoint destination,
            List<GeoPoint> shape,
            List<Maneuver> maneuvers,
            double distanceMeters,
            double durationSeconds) {}

    public record LocationFix(
            GeoPoint point,
            float accuracyMeters,
            float bearingDegrees,
            float speedMetersPerSecond,
            long timestampMs) {}

    public enum EventType {
        LOCATION_UNCERTAIN,
        PREPARE,
        MANEUVER,
        OFF_ROUTE,
        REROUTE_REQUEST,
        FINAL_APPROACH,
        ARRIVED
    }

    public record NavigationEvent(
            EventType type,
            String speech,
            double distanceMeters,
            int maneuverIndex) {}
}
