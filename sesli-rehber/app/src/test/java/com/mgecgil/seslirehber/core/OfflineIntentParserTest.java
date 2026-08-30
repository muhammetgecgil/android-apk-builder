package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OfflineIntentParserTest {
    private final OfflineIntentParser parser = new OfflineIntentParser();

    @Test public void parsesWakeWordAndDescribe() {
        assertEquals(OfflineIntentParser.Intent.DESCRIBE_SCENE,
                parser.parse("Hey Rehber önümde ne var?").intent());
    }

    @Test public void parsesSituationalAwarenessQuestion() {
        assertEquals(OfflineIntentParser.Intent.DESCRIBE_SCENE,
                parser.parse("Hey Rehber durum ne?").intent());
        assertEquals(OfflineIntentParser.Intent.DESCRIBE_SCENE,
                parser.parse("Çevrede ne var?").intent());
    }

    @Test public void parsesStop() {
        assertEquals(OfflineIntentParser.Intent.STOP_GUIDANCE,
                parser.parse("Rehberliği durdur").intent());
    }

    @Test public void busStopDoesNotAccidentallyStopGuidance() {
        assertFalse(parser.parse("Otobüs durağına git").intent()
                == OfflineIntentParser.Intent.STOP_GUIDANCE);
    }

    @Test public void parsesReadText() {
        assertEquals(OfflineIntentParser.Intent.READ_TEXT,
                parser.parse("Hey Rehber tabelayı oku").intent());
    }

    @Test public void extractsDestinationFromTakeMeCommand() {
        OfflineIntentParser.ParsedIntent result = parser.parse("Hey Rehber beni Taksim Meydanı'na götür");
        assertEquals(OfflineIntentParser.Intent.NAVIGATE_TO, result.intent());
        assertTrue(result.hasArgument());
        assertTrue(result.argument().contains("taksim"));
        assertTrue(result.argument().contains("meydani"));
    }

    @Test public void extractsDestinationFromAddressCommand() {
        OfflineIntentParser.ParsedIntent result = parser.parse("Sabiha Gökçen Havalimanı adresine götür");
        assertEquals(OfflineIntentParser.Intent.NAVIGATE_TO, result.intent());
        assertTrue(result.argument().contains("sabiha"));
        assertTrue(result.argument().contains("gokcen"));
    }

    @Test public void parsesWakeModeControl() {
        assertEquals(OfflineIntentParser.Intent.WAKE_MODE_ON,
                parser.parse("Hey Rehber eller serbest modu aç").intent());
        assertEquals(OfflineIntentParser.Intent.WAKE_MODE_OFF,
                parser.parse("eller serbest modu kapat").intent());
    }
}
