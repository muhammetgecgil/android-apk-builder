package com.mgecgil.seslirehber.core;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public final class HudPerceptionContextTest {
    @Before public void reset() { HudPerceptionContext.reset(); }

    @Test public void keepsAtMostEightTracks() {
        for (int i = 0; i < 10; i++) {
            HudPerceptionContext.noteObject(new ObjectObservation(
                    0.5f, 0.5f, 0.7f, 0.08f, 0f, 0f, i, 0.8f, 1000L + i));
        }
        assertEquals(8, HudPerceptionContext.snapshot(1500L).objects().size());
    }

    @Test public void staleTracksExpire() {
        HudPerceptionContext.noteObject(new ObjectObservation(
                0.5f, 0.5f, 0.7f, 0.08f, 0f, 0f, 7, 0.8f, 1000L));
        assertEquals(1, HudPerceptionContext.snapshot(2000L).objects().size());
        assertEquals(0, HudPerceptionContext.snapshot(4000L).objects().size());
    }

    @Test public void sourceAspectIsRetained() {
        HudPerceptionContext.noteSourceAspect(0.5625f);
        assertEquals(0.5625f, HudPerceptionContext.snapshot(1000L).sourceAspect(), 0.0001f);
    }
}
