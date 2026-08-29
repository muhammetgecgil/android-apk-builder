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

    @Test public void mapsIndoorObjectsUsedByCloseSemanticRecognizer() {
        assertEquals("koltuk", DistantLabelPolicy.toTurkishObject("Sofa"));
        assertEquals("koltuk", DistantLabelPolicy.toTurkishObject("Couch"));
        assertEquals("sandalye", DistantLabelPolicy.toTurkishObject("Chair"));
        assertEquals("yastık", DistantLabelPolicy.toTurkishObject("Pillow"));
        assertEquals("halı", DistantLabelPolicy.toTurkishObject("Rug"));
        assertEquals("dolap", DistantLabelPolicy.toTurkishObject("Cabinet"));
        assertEquals("televizyon", DistantLabelPolicy.toTurkishObject("Television"));
        assertEquals("saat", DistantLabelPolicy.toTurkishObject("Wall clock"));
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
