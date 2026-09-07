package com.muhammetgecgil.morse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MorseCodecTest {
    @Test public void encodesSos() {
        assertEquals("... --- ...", MorseCodec.toMorse("SOS"));
    }

    @Test public void roundTripsBasicText() {
        String code = MorseCodec.toMorse("MERHABA 123");
        assertEquals("MERHABA 123", MorseCodec.fromMorse(code));
    }

    @Test public void supportsTurkishLetters() {
        String code = MorseCodec.toMorse("ÇĞİÖŞÜ");
        assertEquals("ÇĞİÖŞÜ", MorseCodec.fromMorse(code));
    }

    @Test public void acceptsUnicodeMorseMarks() {
        assertTrue(MorseCodec.looksLikeMorse("••• ——— •••"));
    }
}
