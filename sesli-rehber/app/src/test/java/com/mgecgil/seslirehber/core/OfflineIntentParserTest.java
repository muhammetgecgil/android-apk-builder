package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class OfflineIntentParserTest {
    private final OfflineIntentParser parser=new OfflineIntentParser();
    @Test public void parsesWakeWordAndDescribe(){assertEquals(OfflineIntentParser.Intent.DESCRIBE_SCENE,parser.parse("Hey Rehber önümde ne var?").intent());}
    @Test public void parsesStop(){assertEquals(OfflineIntentParser.Intent.STOP_GUIDANCE,parser.parse("Rehberliği durdur").intent());}
}
