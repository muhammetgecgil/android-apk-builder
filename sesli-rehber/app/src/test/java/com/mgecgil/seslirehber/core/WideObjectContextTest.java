package com.mgecgil.seslirehber.core;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

public class WideObjectContextTest {
    @After public void reset() { WideObjectContext.reset(); }

    @Test public void infersHomeOfficeFromIndoorInventory() {
        WideObjectContext.note(o("koltuk", Direction.LEFT, 1000));
        WideObjectContext.note(o("televizyon", Direction.CENTER, 1001));
        assertEquals(WideObjectContext.Environment.HOME_OFFICE, WideObjectContext.environment(1200));
    }

    @Test public void infersStreetFromTrafficInventory() {
        WideObjectContext.note(o("araç", Direction.LEFT, 1000));
        WideObjectContext.note(o("trafik ışığı", Direction.CENTER, 1001));
        assertEquals(WideObjectContext.Environment.STREET, WideObjectContext.environment(1200));
    }

    @Test public void inventorySummaryNamesLocations() {
        WideObjectContext.note(o("bıçak", Direction.RIGHT, 1000));
        WideObjectContext.note(o("bardak", Direction.CENTER, 1001));
        String s = WideObjectContext.inventorySummary(1200);
        assertTrue(s.contains("sağda bıçak"));
        assertTrue(s.contains("önde bardak"));
    }

    @Test public void staleInventoryDisappears() {
        WideObjectContext.note(o("laptop", Direction.CENTER, 1000));
        assertTrue(WideObjectContext.inventorySummary(5000).isEmpty());
    }

    private static WideObjectObservation o(String label, Direction d, long t) {
        float l = d == Direction.LEFT ? 0.05f : d == Direction.RIGHT ? 0.70f : 0.40f;
        return new WideObjectObservation(label, 0.88f, l, 0.2f, l + 0.2f, 0.6f, d, true, false, t);
    }
}
