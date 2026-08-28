package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class DistantLabelPolicyTest {
    @Test public void mapsCommonRoadObjectsToTurkish() {
        assertEquals("insan", DistantLabelPolicy.toTurkishObject("Pedestrian"));
        assertEquals("araç", DistantLabelPolicy.toTurkishObject("Car"));
        assertEquals("otobüs", DistantLabelPolicy.toTurkishObject("Bus"));
        assertEquals("bisiklet", DistantLabelPolicy.toTurkishObject("Bicycle"));
        assertEquals("trafik ışığı", DistantLabelPolicy.toTurkishObject("Traffic light"));
        assertEquals("bariyer", DistantLabelPolicy.toTurkishObject("Road barrier"));
    }

    @Test public void groundSafetySemanticsAreNotClaimedByFarLabeler() {
        assertEquals("", DistantLabelPolicy.toTurkishObject("Staircase"));
        assertEquals("", DistantLabelPolicy.toTurkishObject("Pothole"));
        assertEquals("", DistantLabelPolicy.toTurkishObject("Curb"));
    }

    @Test public void unknownGenericLabelIsSuppressed() {
        assertEquals("", DistantLabelPolicy.toTurkishObject("Outdoor scene"));
    }
}
