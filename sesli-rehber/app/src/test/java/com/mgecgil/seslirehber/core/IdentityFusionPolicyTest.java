package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class IdentityFusionPolicyTest {
    @Test public void exactPixelEvidenceBoostsDetectorConfidence() {
        DeepLabIdentityMaskContext.Evidence e = new DeepLabIdentityMaskContext.Evidence(
                "koltuk", 0.42f, 0.48f, true, false, false, 1000L);
        IdentityFusionPolicy.Result result = IdentityFusionPolicy.fuse("koltuk", 0.70f, e);
        assertTrue(result.corroborated());
        assertFalse(result.conflicting());
        assertTrue(result.confidence() > 0.78f);
    }

    @Test public void strongContradictionDowngradesSingleHighConfidenceClaim() {
        DeepLabIdentityMaskContext.Evidence e = new DeepLabIdentityMaskContext.Evidence(
                "koltuk", 0.36f, 0.44f, false, false, true, 1000L);
        IdentityFusionPolicy.Result result = IdentityFusionPolicy.fuse("araç", 0.94f, e);
        assertTrue(result.conflicting());
        assertTrue(result.confidence() < 0.62f);
    }

    @Test public void compatibleFurnitureEvidenceOnlySlightlyBoosts() {
        DeepLabIdentityMaskContext.Evidence e = new DeepLabIdentityMaskContext.Evidence(
                "sandalye", 0.31f, 0.37f, false, true, false, 1000L);
        IdentityFusionPolicy.Result result = IdentityFusionPolicy.fuse("koltuk", 0.71f, e);
        assertTrue(result.familyCompatible());
        assertFalse(result.corroborated());
        assertTrue(result.confidence() > 0.71f);
        assertTrue(result.confidence() < 0.77f);
    }

    @Test public void absentPixelEvidenceLeavesUnrepresentedClassUntouched() {
        IdentityFusionPolicy.Result result = IdentityFusionPolicy.fuse(
                "telefon", 0.81f, DeepLabIdentityMaskContext.Evidence.none());
        assertEquals(0.81f, result.confidence(), 0.0001f);
        assertFalse(result.corroborated());
        assertFalse(result.conflicting());
    }
}
