package com.mgecgil.seslirehber.core;

import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class DeepLabIdentityMaskContextTest {
    @After public void cleanup() { DeepLabIdentityMaskContext.reset(); }

    @Test public void sofaPixelsCorroborateCouchBox() {
        byte[] mask = new byte[100];
        for (int y = 2; y < 8; y++) {
            for (int x = 2; x < 8; x++) mask[y * 10 + x] = 18; // sofa
        }
        DeepLabIdentityMaskContext.publish(ByteBuffer.wrap(mask), 10, 10, 1000L);
        DeepLabIdentityMaskContext.Evidence e = DeepLabIdentityMaskContext.evidenceFor(
                "koltuk", 0.18f, 0.18f, 0.82f, 0.82f, 1200L);
        assertTrue(e.usable());
        assertEquals("koltuk", e.bestLabel());
        assertTrue(e.exact());
        assertFalse(e.conflicting());
        assertTrue(e.bestShare() > 0.55f);
    }

    @Test public void sofaPixelsConflictWithCarClaim() {
        byte[] mask = new byte[100];
        for (int y = 1; y < 9; y++) {
            for (int x = 1; x < 9; x++) mask[y * 10 + x] = 18;
        }
        DeepLabIdentityMaskContext.publish(ByteBuffer.wrap(mask), 10, 10, 2000L);
        DeepLabIdentityMaskContext.Evidence e = DeepLabIdentityMaskContext.evidenceFor(
                "araç", 0.10f, 0.10f, 0.90f, 0.90f, 2200L);
        assertEquals("koltuk", e.bestLabel());
        assertTrue(e.conflicting());
        assertFalse(e.familyCompatible());
    }

    @Test public void chairAndCouchAreCompatibleFurnitureEvidence() {
        byte[] mask = new byte[64];
        for (int i = 0; i < mask.length; i++) mask[i] = 9; // chair
        DeepLabIdentityMaskContext.publish(ByteBuffer.wrap(mask), 8, 8, 3000L);
        DeepLabIdentityMaskContext.Evidence e = DeepLabIdentityMaskContext.evidenceFor(
                "koltuk", 0f, 0f, 1f, 1f, 3100L);
        assertTrue(e.familyCompatible());
        assertFalse(e.conflicting());
    }

    @Test public void staleMaskCannotInfluenceIdentity() {
        byte[] mask = new byte[64];
        for (int i = 0; i < mask.length; i++) mask[i] = 7;
        DeepLabIdentityMaskContext.publish(ByteBuffer.wrap(mask), 8, 8, 4000L);
        assertFalse(DeepLabIdentityMaskContext.evidenceFor(
                "araç", 0f, 0f, 1f, 1f, 6000L).usable());
    }
}
