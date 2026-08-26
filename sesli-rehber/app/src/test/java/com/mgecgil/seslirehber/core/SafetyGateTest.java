package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class SafetyGateTest {
    @Test public void unstablePhoneForcesStop(){SafetyGate gate=new SafetyGate();MotionObservation o=new MotionObservation(0.2f,0.5f,0.5f,0.9f,1L);assertEquals(Risk.STOP,gate.evaluate(o,0.2f).risk());}
}
