package com.mgecgil.seslirehber.core;

import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.Direction;

public class DistantObjectSpeechTest {
    @Test public void farRecognitionSpeechNeverBecomesSafetyInstruction() {
        DistantObjectObservation observation = new DistantObjectObservation(
                "araç", Direction.CENTER, 0.82f, 0.75f, 2.5f, 0.40f, 1000L);
        String speech = DistantObjectSpeech.format(observation);
        String lower = speech.toLowerCase(new Locale("tr", "TR"));
        assertTrue(lower.contains("uzak görüş"));
        assertTrue(lower.contains("olabilecek"));
        assertFalse(lower.contains("dur."));
        assertFalse(lower.contains("güvenli"));
        assertFalse(lower.contains("metre"));
    }
}
