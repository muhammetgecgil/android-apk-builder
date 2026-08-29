package com.mgecgil.seslirehber.core;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class UrbanGateAcceptanceTest {
    @Test public void goodGpuSessionPasses() {
        UrbanGateAcceptance.Result r = UrbanGateAcceptance.evaluate(input(
                UrbanValidationTelemetry.Backend.GPU,
                70, 1, 900, 39.5f, 1,
                0.80f, 0.80f, 0.75f, 0.60f, 0.50f, 0.70f, 0.50f));
        assertEquals(UrbanGateAcceptance.Verdict.PASS, r.verdict());
        assertTrue(r.shortText().contains("PASS"));
    }

    @Test public void cpuFallbackForcesReviewEvenWhenEvidenceIsGood() {
        UrbanGateAcceptance.Result r = UrbanGateAcceptance.evaluate(input(
                UrbanValidationTelemetry.Backend.CPU,
                70, 1, 2500, 40f, 1,
                0.80f, 0.80f, 0.75f, 0.60f, 0.50f, 0.70f, 0.50f));
        assertEquals(UrbanGateAcceptance.Verdict.REVIEW, r.verdict());
        assertTrue(r.shortText().contains("CPU"));
    }

    @Test public void missingRequiredScenarioFails() {
        List<UrbanGateAcceptance.ScenarioMetric> metrics = goodMetrics(
                0.80f, 0.80f, 0.75f, 0.60f, 0.50f, 0.70f, 0.50f);
        metrics.removeIf(m -> m.scenario() == UrbanValidationTelemetry.Scenario.TRAFFIC_CONTROL);
        UrbanGateAcceptance.Result r = UrbanGateAcceptance.evaluate(new UrbanGateAcceptance.Input(
                UrbanValidationTelemetry.Backend.GPU, 60, 0, 900, 39f, 1, metrics));
        assertEquals(UrbanGateAcceptance.Verdict.FAIL, r.verdict());
        assertTrue(r.shortText().contains("Trafik"));
    }

    @Test public void severeHeatFailsRegardlessOfSemanticEvidence() {
        UrbanGateAcceptance.Result r = UrbanGateAcceptance.evaluate(input(
                UrbanValidationTelemetry.Backend.GPU,
                70, 0, 900, 45.2f, 4,
                0.80f, 0.80f, 0.75f, 0.60f, 0.50f, 0.70f, 0.50f));
        assertEquals(UrbanGateAcceptance.Verdict.FAIL, r.verdict());
        assertTrue(r.shortText().contains("batarya"));
        assertTrue(r.shortText().contains("thermal"));
    }

    @Test public void excessiveGpuLatencyFails() {
        UrbanGateAcceptance.Result r = UrbanGateAcceptance.evaluate(input(
                UrbanValidationTelemetry.Backend.GPU,
                70, 0, 3000, 39f, 1,
                0.80f, 0.80f, 0.75f, 0.60f, 0.50f, 0.70f, 0.50f));
        assertEquals(UrbanGateAcceptance.Verdict.FAIL, r.verdict());
        assertTrue(r.shortText().contains("p95"));
    }

    @Test public void weakTrafficControlEvidenceFailsNotPasses() {
        UrbanGateAcceptance.Result r = UrbanGateAcceptance.evaluate(input(
                UrbanValidationTelemetry.Backend.GPU,
                70, 0, 900, 39f, 1,
                0.80f, 0.80f, 0.75f, 0.60f, 0.05f, 0.70f, 0.50f));
        assertEquals(UrbanGateAcceptance.Verdict.FAIL, r.verdict());
        assertTrue(r.shortText().contains("Trafik"));
    }

    private static UrbanGateAcceptance.Input input(
            UrbanValidationTelemetry.Backend backend,
            long successes,
            long failures,
            long p95,
            float battery,
            int thermal,
            float sidewalk,
            float road,
            float building,
            float pole,
            float traffic,
            float personVehicle,
            float lowLight) {
        return new UrbanGateAcceptance.Input(
                backend, successes, failures, p95, battery, thermal,
                goodMetrics(sidewalk, road, building, pole, traffic, personVehicle, lowLight));
    }

    private static List<UrbanGateAcceptance.ScenarioMetric> goodMetrics(
            float sidewalk,
            float road,
            float building,
            float pole,
            float traffic,
            float personVehicle,
            float lowLight) {
        List<UrbanGateAcceptance.ScenarioMetric> out = new ArrayList<>();
        out.add(metric(UrbanValidationTelemetry.Scenario.SIDEWALK, 10, sidewalk));
        out.add(metric(UrbanValidationTelemetry.Scenario.ROAD_EDGE, 10, road));
        out.add(metric(UrbanValidationTelemetry.Scenario.BUILDING_WALL, 10, building));
        out.add(metric(UrbanValidationTelemetry.Scenario.POLE_FENCE, 10, pole));
        out.add(metric(UrbanValidationTelemetry.Scenario.TRAFFIC_CONTROL, 10, traffic));
        out.add(metric(UrbanValidationTelemetry.Scenario.PERSON_VEHICLE, 10, personVehicle));
        out.add(metric(UrbanValidationTelemetry.Scenario.LOW_LIGHT, 10, lowLight));
        return out;
    }

    private static UrbanGateAcceptance.ScenarioMetric metric(
            UrbanValidationTelemetry.Scenario scenario, long frames, float rate) {
        return new UrbanGateAcceptance.ScenarioMetric(
                scenario, frames, Math.round(frames * rate));
    }
}
