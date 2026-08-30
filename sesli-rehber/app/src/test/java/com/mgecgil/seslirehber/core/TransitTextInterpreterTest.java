package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class TransitTextInterpreterTest {
    @Test public void recognizesTypicalIettLineCandidate() {
        String hint = TransitTextInterpreter.interpret("İETT DURAK 16S");
        assertTrue(hint.contains("16S"));
        assertTrue(hint.toLowerCase().contains("doğrula"));
        assertFalse(hint.toLowerCase().contains("doğru otobüs"));
    }

    @Test public void recognizesLetterNumberRouteCandidate() {
        String hint = TransitTextInterpreter.interpret("E-10 Sabiha Gökçen");
        assertTrue(hint.contains("E-10"));
        assertTrue(hint.toLowerCase().contains("olabilecek"));
    }

    @Test public void plainUnrelatedTextStaysQuiet() {
        assertEquals("", TransitTextInterpreter.interpret("MARKET AÇIK 24 SAAT"));
    }
}
