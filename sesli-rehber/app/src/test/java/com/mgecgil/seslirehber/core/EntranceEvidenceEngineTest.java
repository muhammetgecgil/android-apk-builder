package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class EntranceEvidenceEngineTest {
    @Test public void singleDoorNumberReadingDoesNotClaimEntrance() {
        EntranceEvidenceEngine engine = new EntranceEvidenceEngine();
        engine.activate("Test Sokak No 42", 1000L);
        assertEquals("", engine.observeOcr("42", 2000L));
    }

    @Test public void repeatedMatchingDoorNumberProducesAdvisoryOnly() {
        EntranceEvidenceEngine engine = new EntranceEvidenceEngine();
        engine.activate("Test Sokak No 42", 1000L);
        assertEquals("", engine.observeOcr("Kapı No 42", 2000L));
        String speech = engine.observeOcr("Giriş 42", 6500L);
        assertFalse(speech.isEmpty());
        assertTrue(speech.toLowerCase().contains("aday"));
        assertTrue(speech.toLowerCase().contains("doğrula"));
        assertFalse(speech.toLowerCase().contains("doğru kapı budur"));
    }

    @Test public void unrelatedNumberDoesNotMatchDestination() {
        EntranceEvidenceEngine engine = new EntranceEvidenceEngine();
        engine.activate("Test Sokak No 42", 1000L);
        assertEquals("", engine.observeOcr("No 18", 2000L));
        assertEquals("", engine.observeOcr("No 18", 5000L));
    }

    @Test public void automaticScanIsRateLimitedAndExpires() {
        EntranceEvidenceEngine engine = new EntranceEvidenceEngine();
        engine.activate("Test Sokak 42", 1000L);
        assertFalse(engine.consumeAutoScanPermit(1500L));
        assertTrue(engine.consumeAutoScanPermit(2000L));
        assertFalse(engine.consumeAutoScanPermit(2500L));
        assertFalse(engine.isActive(300_000L));
    }
}
