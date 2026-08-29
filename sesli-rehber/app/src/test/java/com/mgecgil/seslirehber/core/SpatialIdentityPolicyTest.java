package com.mgecgil.seslirehber.core;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

public class SpatialIdentityPolicyTest {
    @Test public void overlapAndDistanceRecognizeSamePhysicalRegion() {
        assertTrue(SpatialIdentityPolicy.samePhysicalRegion(
                true, 0.20f, 0.20f, 0.60f, 0.70f,
                0.23f, 0.22f, 0.63f, 0.72f));
        assertFalse(SpatialIdentityPolicy.samePhysicalRegion(
                true, 0.05f, 0.10f, 0.25f, 0.35f,
                0.65f, 0.10f, 0.85f, 0.35f));
    }

    @Test public void homeContextRejectsMediumConfidenceStreetPhantom() {
        assertFalse(SpatialIdentityPolicy.allowDistant(
                "araç", 0.84f, WideObjectContext.Environment.HOME_OFFICE));
        assertTrue(SpatialIdentityPolicy.allowDistant(
                "araç", 0.94f, WideObjectContext.Environment.HOME_OFFICE));
        assertTrue(SpatialIdentityPolicy.allowDistant(
                "araç", 0.74f, WideObjectContext.Environment.STREET));
    }

    @Test public void unknownContextRequiresExtraEvidenceForStreetLabel() {
        assertFalse(SpatialIdentityPolicy.allowDistant(
                "araç", 0.70f, WideObjectContext.Environment.UNKNOWN));
        assertTrue(SpatialIdentityPolicy.allowDistant(
                "araç", 0.78f, WideObjectContext.Environment.UNKNOWN));
    }

    @Test public void oversizedPillowNeedsVeryHighConfidence() {
        assertFalse(SpatialIdentityPolicy.allowSupplementalCrop(
                "yastık", 0.85f, 0.32f, 1.8f, WideObjectContext.Environment.HOME_OFFICE));
        assertTrue(SpatialIdentityPolicy.allowSupplementalCrop(
                "yastık", 0.95f, 0.32f, 1.8f, WideObjectContext.Environment.HOME_OFFICE));
    }

    @Test public void cropYieldsWhenWideDetectorOwnsSameBox() {
        WideObjectObservation couch = new WideObjectObservation(
                "koltuk", 0.87f, 0.18f, 0.42f, 0.82f, 0.82f,
                Direction.CENTER, true, false, 1000L);
        assertTrue(SpatialIdentityPolicy.cropShouldYieldToWide(
                "yastık", 0.20f, 0.44f, 0.80f, 0.81f, List.of(couch)));
        assertFalse(SpatialIdentityPolicy.cropShouldYieldToWide(
                "yastık", 0.02f, 0.05f, 0.15f, 0.16f, List.of(couch)));
    }
}
