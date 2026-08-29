package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;
import static org.junit.Assert.*;

public class ObjectSemanticSpeechTest {
    @Test public void definiteIdentityUsesDirectThereIsWording() {
        ObjectSemanticObservation o = new ObjectSemanticObservation(
                1, "koltuk", 0.84f, true, Direction.CENTER, 2, 1000L);
        String speech = ObjectSemanticSpeech.format(o);
        assertEquals("Önde koltuk var.", speech);
        assertFalse(speech.contains("olabilir"));
    }

    @Test public void candidateIdentityKeepsUncertaintyAndConfidence() {
        ObjectSemanticObservation o = new ObjectSemanticObservation(
                2, "sandalye", 0.64f, false, Direction.LEFT, 2, 1000L);
        String speech = ObjectSemanticSpeech.format(o);
        assertTrue(speech.contains("Solda sandalye olabilir"));
        assertTrue(speech.contains("64"));
    }
}
