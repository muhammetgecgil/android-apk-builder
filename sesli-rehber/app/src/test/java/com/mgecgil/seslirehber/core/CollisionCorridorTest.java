package com.mgecgil.seslirehber.core;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public class CollisionCorridorTest {
    @Test public void centeredApproachingObjectScoresHigherThanSmallSideObject() {
        CollisionCorridor corridor = new CollisionCorridor();
        ObjectObservation center = new ObjectObservation(
                0.50f, 0.52f, 0.84f, 0.18f, 0.18f, 0.00f, 1, 0.86f, 1000L);
        ObjectObservation side = new ObjectObservation(
                0.12f, 0.52f, 0.70f, 0.05f, 0.00f, 0.00f, 2, 0.86f, 1000L);
        CorridorAssessment centerAssessment = corridor.assess(center, 0.90f);
        CorridorAssessment sideAssessment = corridor.assess(side, 0.90f);
        assertTrue(centerAssessment.inCorridor());
        assertFalse(sideAssessment.inCorridor());
        assertTrue(centerAssessment.hazardScore() > sideAssessment.hazardScore());
    }

    @Test public void sideObjectMovingTowardCenterIsMarkedCrossing() {
        CollisionCorridor corridor = new CollisionCorridor();
        ObjectObservation object = new ObjectObservation(
                0.22f, 0.48f, 0.78f, 0.10f, 0.02f, 0.20f, 3, 0.84f, 1000L);
        CorridorAssessment assessment = corridor.assess(object, 0.92f);
        assertTrue(assessment.crossingIntoCorridor());
    }

    @Test public void lowerStabilityWidensCorridorBeforeFailSafeThreshold() {
        CollisionCorridor corridor = new CollisionCorridor();
        ObjectObservation object = new ObjectObservation(
                0.50f, 0.50f, 0.75f, 0.10f, 0.03f, 0.00f, 4, 0.82f, 1000L);
        float stable = corridor.assess(object, 0.95f).corridorHalfWidth();
        float lessStable = corridor.assess(object, 0.45f).corridorHalfWidth();
        assertTrue(lessStable > stable);
    }
}
