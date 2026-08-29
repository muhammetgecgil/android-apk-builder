package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class WideObjectPolicyTest {
    @Test public void mapsRequestedHomeOfficeMarketStreetObjects() {
        assertEquals("bıçak", WideObjectPolicy.toTurkish("knife"));
        assertEquals("bardak", WideObjectPolicy.toTurkish("cup"));
        assertEquals("televizyon", WideObjectPolicy.toTurkish("tv"));
        assertEquals("saksı", WideObjectPolicy.toTurkish("potted plant"));
        assertEquals("laptop", WideObjectPolicy.toTurkish("laptop"));
        assertEquals("çanta", WideObjectPolicy.toTurkish("handbag"));
        assertEquals("otobüs", WideObjectPolicy.toTurkish("bus"));
        assertEquals("trafik ışığı", WideObjectPolicy.toTurkish("traffic light"));
    }

    @Test public void cropFallbackAddsDoorWindowPenAndGlass() {
        assertEquals("kapı", DistantLabelPolicy.toTurkishObject("Door"));
        assertEquals("pencere", DistantLabelPolicy.toTurkishObject("Window"));
        assertEquals("kalem", DistantLabelPolicy.toTurkishObject("Ballpoint pen"));
        assertEquals("su bardağı", DistantLabelPolicy.toTurkishObject("Drinking glass"));
    }

    @Test public void cuttingAndTrafficObjectsAreImportantButNotSafetyTruth() {
        assertTrue(WideObjectPolicy.important("bıçak"));
        assertTrue(WideObjectPolicy.important("araç"));
        assertFalse(WideObjectPolicy.important("koltuk"));
    }
}
