package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class GroundDepthSynchronizerTest {
    private GroundObservation ground(long ts) {
        return new GroundObservation(0.8f, 0.7f, 0.6f, 0.6f, 0.8f, 0.9f, 0.65f, ts);
    }

    private DepthObservation depth(long ts) {
        return new DepthObservation(0.9f, 2400f, 1700f, 5200f, 3500f, 0.9f, 0.9f, ts);
    }

    @Test public void closeTimestampsCanFuse() {
        GroundDepthSynchronizer sync = new GroundDepthSynchronizer();
        assertNull(sync.offerGround(ground(1000L)));
        GroundDepthEvidence evidence = sync.offerDepth(depth(1190L));
        assertNotNull(evidence);
        assertEquals(190L, evidence.timestampSkewMs());
    }

    @Test public void staleDepthCannotFuseWithNewGround() {
        GroundDepthSynchronizer sync = new GroundDepthSynchronizer();
        assertNull(sync.offerDepth(depth(1000L)));
        assertNull(sync.offerGround(ground(1500L)));
    }

    @Test public void samePairIsNotEmittedTwice() {
        GroundDepthSynchronizer sync = new GroundDepthSynchronizer();
        sync.offerGround(ground(1000L));
        assertNotNull(sync.offerDepth(depth(1100L)));
        assertNull(sync.offerDepth(depth(1100L)));
    }
}
