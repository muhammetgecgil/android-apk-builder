package com.mgecgil.seslirehber.navigation;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.navigation.NavigationModels.*;

public class NavigationProgressEngineTest {
    @Test public void poorGpsHoldsManeuverGuidance() {
        NavigationProgressEngine engine = new NavigationProgressEngine();
        engine.setRoute(route());
        List<NavigationEvent> events = engine.update(fix(point(2), 48f));
        assertEquals(1, events.size());
        assertEquals(EventType.LOCATION_UNCERTAIN, events.get(0).type());
    }

    @Test public void persistentOffRouteRequestsRerouteOnlyAfterThreeSamples() {
        NavigationProgressEngine engine = new NavigationProgressEngine();
        engine.setRoute(route());
        GeoPoint far = new GeoPoint(41.00018, 29.0010);
        assertTrue(engine.update(fix(far, 6f)).isEmpty());
        assertTrue(engine.update(fix(far, 6f)).isEmpty());
        List<NavigationEvent> third = engine.update(fix(far, 6f));
        assertTrue(third.stream().anyMatch(e -> e.type() == EventType.REROUTE_REQUEST));
    }

    @Test public void turnGetsPrepareThenManeuver() {
        NavigationProgressEngine engine = new NavigationProgressEngine();
        engine.setRoute(route());
        List<NavigationEvent> prepare = engine.update(fix(point(2), 5f));
        assertTrue(prepare.stream().anyMatch(e -> e.type() == EventType.PREPARE));
        List<NavigationEvent> now = engine.update(fix(point(5), 5f));
        assertTrue(now.stream().anyMatch(e -> e.type() == EventType.MANEUVER));
    }

    @Test public void finalApproachIsAnnouncedOnceAndDoesNotClaimEntrance() {
        NavigationProgressEngine engine = new NavigationProgressEngine();
        engine.setRoute(route());
        List<NavigationEvent> first = engine.update(fix(point(5), 5f));
        NavigationEvent finalApproach = first.stream()
                .filter(e -> e.type() == EventType.FINAL_APPROACH).findFirst().orElseThrow();
        assertTrue(finalApproach.speech().toLowerCase().contains("gps"));
        assertTrue(finalApproach.speech().toLowerCase().contains("doğrula"));
        assertFalse(finalApproach.speech().toLowerCase().contains("giriş burası"));
        List<NavigationEvent> second = engine.update(fix(point(6), 5f));
        assertFalse(second.stream().anyMatch(e -> e.type() == EventType.FINAL_APPROACH));
    }

    @Test public void arrivalRequiresCloseAccurateFixAndUsesVerificationLanguage() {
        NavigationProgressEngine engine = new NavigationProgressEngine();
        engine.setRoute(route());
        List<NavigationEvent> events = engine.update(fix(point(10), 5f));
        NavigationEvent arrival = events.stream()
                .filter(e -> e.type() == EventType.ARRIVED).findFirst().orElseThrow();
        assertTrue(arrival.speech().toLowerCase().contains("doğrula"));
        assertFalse(arrival.speech().toLowerCase().contains("güvenli"));
    }

    @Test public void maneuverLanguageNeverClaimsSafeCrossing() {
        int[] types = {8,9,10,11,12,13,14,15,16,26,27,39,40,41,42,43};
        for (int type : types) {
            String speech = ManeuverSpeechFormatter.now(new Maneuver(type, 1, 2, "Test Sokak", ""));
            String lower = speech.toLowerCase();
            assertFalse(lower.contains("karşıya geçmek güvenli"));
            assertFalse(lower.contains("güvenli geç"));
        }
    }

    private static Route route() {
        ArrayList<GeoPoint> shape = new ArrayList<>();
        for (int i = 0; i <= 10; i++) shape.add(point(i));
        List<Maneuver> maneuvers = List.of(
                new Maneuver(1, 0, 1, "Başlangıç", ""),
                new Maneuver(10, 5, 6, "Sağ Sokak", ""),
                new Maneuver(4, 10, 10, "", ""));
        return new Route("Test hedef", point(10), shape, maneuvers, 100d, 80d);
    }

    private static GeoPoint point(int i) {
        return new GeoPoint(41.0 + i * 0.00009, 29.0);
    }

    private static LocationFix fix(GeoPoint point, float accuracy) {
        return new LocationFix(point, accuracy, Float.NaN, 1f, System.currentTimeMillis());
    }
}
