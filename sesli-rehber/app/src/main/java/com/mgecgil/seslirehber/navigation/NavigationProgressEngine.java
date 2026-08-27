package com.mgecgil.seslirehber.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static com.mgecgil.seslirehber.navigation.NavigationModels.*;

/**
 * GPS route progress state machine. It intentionally treats poor GPS as uncertainty, not as proof
 * that the user is on the route. Off-route requires persistence before a reroute request.
 */
public final class NavigationProgressEngine {
    private static final float MAX_GUIDANCE_ACCURACY_M = 32f;
    private static final double ARRIVAL_M = 14d;
    private static final double PREPARE_M = 38d;
    private static final double MANEUVER_M = 10d;
    private static final int OFF_ROUTE_PERSISTENCE = 3;

    private Route route;
    private double[] cumulativeMeters = new double[0];
    private int nextManeuver;
    private int lastNearestShapeIndex;
    private int preparedManeuver = -1;
    private int firedManeuver = -1;
    private int offRouteStreak;
    private boolean uncertainReported;
    private boolean arrived;

    public void setRoute(Route route) {
        this.route = route;
        this.cumulativeMeters = cumulative(route == null ? List.of() : route.shape());
        this.nextManeuver = 0;
        this.lastNearestShapeIndex = 0;
        this.preparedManeuver = -1;
        this.firedManeuver = -1;
        this.offRouteStreak = 0;
        this.uncertainReported = false;
        this.arrived = false;
        skipStartManeuvers();
    }

    public boolean hasRoute() { return route != null && route.shape() != null && !route.shape().isEmpty(); }
    public boolean isArrived() { return arrived; }

    public List<NavigationEvent> update(LocationFix fix) {
        if (!hasRoute() || fix == null || fix.point() == null || arrived) return Collections.emptyList();
        ArrayList<NavigationEvent> events = new ArrayList<>();

        if (fix.accuracyMeters() <= 0f || fix.accuracyMeters() > MAX_GUIDANCE_ACCURACY_M) {
            if (!uncertainReported) {
                uncertainReported = true;
                events.add(new NavigationEvent(
                        EventType.LOCATION_UNCERTAIN,
                        "Konum doğruluğu yetersiz. Rota talimatını bekletiyorum; çevre güvenliği çalışmaya devam ediyor.",
                        fix.accuracyMeters(), -1));
            }
            return events;
        }
        uncertainReported = false;

        double destinationDistance = distanceMeters(fix.point(), route.destination());
        if (destinationDistance <= Math.max(ARRIVAL_M, fix.accuracyMeters() * 0.75d)) {
            arrived = true;
            events.add(new NavigationEvent(
                    EventType.ARRIVED,
                    "Hedef konumuna çok yaklaştın. Giriş noktasını kamera ve bastonla doğrula.",
                    destinationDistance, -1));
            return events;
        }

        Nearest nearest = nearestRoute(fix.point());
        lastNearestShapeIndex = Math.max(lastNearestShapeIndex - 8, nearest.index());
        double offRouteThreshold = Math.max(32d, fix.accuracyMeters() * 1.65d);
        if (nearest.distanceMeters() > offRouteThreshold) {
            offRouteStreak++;
            if (offRouteStreak == OFF_ROUTE_PERSISTENCE) {
                events.add(new NavigationEvent(
                        EventType.OFF_ROUTE,
                        "Rotadan uzaklaşmış olabilirsin. Yeni rota hesaplanıyor.",
                        nearest.distanceMeters(), -1));
                events.add(new NavigationEvent(EventType.REROUTE_REQUEST, "", nearest.distanceMeters(), -1));
            }
            return events;
        }
        offRouteStreak = 0;

        advancePassedManeuvers(nearest.index());
        if (nextManeuver >= route.maneuvers().size()) return events;
        Maneuver maneuver = route.maneuvers().get(nextManeuver);
        int targetIndex = clampIndex(maneuver.beginShapeIndex());
        double distanceToManeuver = alongRouteDistance(nearest.index(), targetIndex);

        if (distanceToManeuver <= PREPARE_M
                && distanceToManeuver > MANEUVER_M
                && preparedManeuver != nextManeuver) {
            preparedManeuver = nextManeuver;
            events.add(new NavigationEvent(
                    EventType.PREPARE,
                    ManeuverSpeechFormatter.prepare(maneuver, distanceToManeuver),
                    distanceToManeuver,
                    nextManeuver));
        }

        if (distanceToManeuver <= MANEUVER_M && firedManeuver != nextManeuver) {
            firedManeuver = nextManeuver;
            events.add(new NavigationEvent(
                    EventType.MANEUVER,
                    ManeuverSpeechFormatter.now(maneuver),
                    distanceToManeuver,
                    nextManeuver));
            nextManeuver++;
            preparedManeuver = -1;
            skipStartManeuvers();
        }
        return events;
    }

    private void skipStartManeuvers() {
        if (route == null) return;
        while (nextManeuver < route.maneuvers().size()) {
            int type = route.maneuvers().get(nextManeuver).type();
            if (type != 1 && type != 2 && type != 3) break;
            if (route.maneuvers().get(nextManeuver).beginShapeIndex() > 2) break;
            nextManeuver++;
        }
    }

    private void advancePassedManeuvers(int nearestIndex) {
        while (nextManeuver < route.maneuvers().size()) {
            Maneuver m = route.maneuvers().get(nextManeuver);
            if (m.type() >= 4 && m.type() <= 6) break;
            if (nearestIndex <= m.endShapeIndex() + 2) break;
            nextManeuver++;
            preparedManeuver = -1;
        }
    }

    private Nearest nearestRoute(GeoPoint p) {
        List<GeoPoint> shape = route.shape();
        int start = Math.max(0, lastNearestShapeIndex - 20);
        double best = Double.MAX_VALUE;
        int bestIndex = start;
        for (int i = start; i < shape.size(); i++) {
            double d = distanceMeters(p, shape.get(i));
            if (d < best) {
                best = d;
                bestIndex = i;
            }
            if (i > bestIndex + 180 && best < 18d) break;
        }
        return new Nearest(bestIndex, best);
    }

    private int clampIndex(int index) {
        if (cumulativeMeters.length == 0) return 0;
        return Math.max(0, Math.min(cumulativeMeters.length - 1, index));
    }

    private double alongRouteDistance(int from, int to) {
        if (cumulativeMeters.length == 0) return Double.MAX_VALUE;
        int a = clampIndex(from);
        int b = clampIndex(to);
        if (b <= a) return 0d;
        return cumulativeMeters[b] - cumulativeMeters[a];
    }

    private static double[] cumulative(List<GeoPoint> points) {
        double[] out = new double[points.size()];
        for (int i = 1; i < points.size(); i++) {
            out[i] = out[i - 1] + distanceMeters(points.get(i - 1), points.get(i));
        }
        return out;
    }

    public static double distanceMeters(GeoPoint a, GeoPoint b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        return 6_371_000d * 2d * Math.atan2(Math.sqrt(h), Math.sqrt(Math.max(0d, 1d - h)));
    }

    private record Nearest(int index, double distanceMeters) {}
}
