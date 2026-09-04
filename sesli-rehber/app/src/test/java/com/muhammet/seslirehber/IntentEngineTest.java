package com.muhammet.seslirehber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Test;

public class IntentEngineTest {
    @After
    public void clearConversation() {
        IntentEngine.cancelPending();
    }

    @Test
    public void understandsNaturalTimeQuestion() {
        assertEquals("TIME", IntentEngine.understand("Saat kaç oldu?").intent);
    }

    @Test
    public void understandsSafeWalkAliases() {
        assertEquals("SCENE", IntentEngine.understand("Güvenli yürüyüşü aç").intent);
        assertEquals("SCENE", IntentEngine.understand("Hareket Görüş").intent);
    }

    @Test
    public void completesMissingNavigationDestination() {
        IntentEngine.Result first = IntentEngine.understand("Beni götür");
        assertNotNull(first.clarification);
        IntentEngine.Result second = IntentEngine.understand("Kadıköy iskelesi");
        assertEquals("NAVIGATE", second.intent);
        assertEquals("kadikoy iskelesi yol tarifi", second.command);
    }
}
