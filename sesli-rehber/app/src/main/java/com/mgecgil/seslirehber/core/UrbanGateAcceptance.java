package com.mgecgil.seslirehber.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * Conservative device-side acceptance evaluator for the advisory urban segmentation channel.
 * A PASS here is only an Urban Gate engineering result; it is not a declaration that blind
 * navigation is safe or independently validated.
 */
public final class UrbanGateAcceptance {
    public enum Verdict { PASS, REVIEW, FAIL }

    public record ScenarioMetric(
            UrbanValidationTelemetry.Scenario scenario,
            long frames,
            long evidenceFrames) {
        public float evidenceRate() {
            return frames <= 0L ? 0f : evidenceFrames / (float) frames;
        }
    }

    public record Input(
            UrbanValidationTelemetry.Backend backend,
            long successes,
            long failures,
            long p95InferenceMs,
            float maxBatteryC,
            int maxThermalStatus,
            List<ScenarioMetric> scenarioMetrics) {}

    public record Result(Verdict verdict, List<String> reasons) {
        public Result {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }

        public String shortText() {
            String prefix = switch (verdict) {
                case PASS -> "Urban Gate PASS";
                case REVIEW -> "Urban Gate REVIEW";
                case FAIL -> "Urban Gate FAIL";
            };
            if (reasons.isEmpty()) return prefix + ".";
            return prefix + ": " + String.join("; ", reasons) + ".";
        }
    }

    private static final int MIN_FRAMES_MAJOR = 5;
    private static final int MIN_FRAMES_SMALL = 6;
    private static final int MIN_FRAMES_LOW_LIGHT = 4;

    private UrbanGateAcceptance() {}

    public static Result evaluate(Input input) {
        if (input == null) return new Result(Verdict.FAIL, List.of("ölçüm yok"));
        List<String> fail = new ArrayList<>();
        List<String> review = new ArrayList<>();

        UrbanValidationTelemetry.Backend backend = input.backend() == null
                ? UrbanValidationTelemetry.Backend.INITIALIZING : input.backend();
        if (backend == UrbanValidationTelemetry.Backend.UNAVAILABLE
                || backend == UrbanValidationTelemetry.Backend.INITIALIZING) {
            fail.add("PIDNet backend hazır değil");
        } else if (backend == UrbanValidationTelemetry.Backend.CPU) {
            review.add("GPU yerine CPU fallback kullanıldı");
        }

        long total = Math.max(0L, input.successes()) + Math.max(0L, input.failures());
        if (total < 20L) {
            fail.add("toplam inference sayısı yetersiz");
        }
        float failureRate = total <= 0L ? 1f : input.failures() / (float) total;
        if (failureRate > 0.15f) {
            fail.add("inference hata oranı %" + Math.round(failureRate * 100f));
        } else if (failureRate > 0.05f) {
            review.add("inference hata oranı %" + Math.round(failureRate * 100f));
        }

        long p95 = Math.max(0L, input.p95InferenceMs());
        if (backend == UrbanValidationTelemetry.Backend.GPU) {
            if (p95 <= 0L || p95 > 2600L) fail.add("GPU p95 gecikme " + p95 + " ms");
            else if (p95 > 1700L) review.add("GPU p95 gecikme " + p95 + " ms");
        } else if (backend == UrbanValidationTelemetry.Backend.CPU) {
            if (p95 <= 0L || p95 > 5200L) fail.add("CPU p95 gecikme " + p95 + " ms");
            else if (p95 > 3600L) review.add("CPU p95 gecikme " + p95 + " ms");
        }

        if (!Float.isNaN(input.maxBatteryC())) {
            if (input.maxBatteryC() >= 45.0f) {
                fail.add(String.format(Locale.US, "batarya %.1f C", input.maxBatteryC()));
            } else if (input.maxBatteryC() >= 42.0f) {
                review.add(String.format(Locale.US, "batarya %.1f C", input.maxBatteryC()));
            }
        } else {
            review.add("batarya sıcaklığı ölçülemedi");
        }

        if (input.maxThermalStatus() >= 4) {
            fail.add("thermal status " + input.maxThermalStatus());
        } else if (input.maxThermalStatus() >= 3) {
            review.add("thermal status " + input.maxThermalStatus());
        }

        EnumMap<UrbanValidationTelemetry.Scenario, ScenarioMetric> metrics =
                new EnumMap<>(UrbanValidationTelemetry.Scenario.class);
        if (input.scenarioMetrics() != null) {
            for (ScenarioMetric m : input.scenarioMetrics()) {
                if (m != null && m.scenario() != null) metrics.put(m.scenario(), m);
            }
        }

        evaluateScenario(metrics, UrbanValidationTelemetry.Scenario.SIDEWALK,
                MIN_FRAMES_MAJOR, 0.55f, 0.35f, fail, review);
        evaluateScenario(metrics, UrbanValidationTelemetry.Scenario.ROAD_EDGE,
                MIN_FRAMES_MAJOR, 0.55f, 0.35f, fail, review);
        evaluateScenario(metrics, UrbanValidationTelemetry.Scenario.BUILDING_WALL,
                MIN_FRAMES_MAJOR, 0.50f, 0.30f, fail, review);
        evaluateScenario(metrics, UrbanValidationTelemetry.Scenario.POLE_FENCE,
                MIN_FRAMES_SMALL, 0.35f, 0.20f, fail, review);
        evaluateScenario(metrics, UrbanValidationTelemetry.Scenario.TRAFFIC_CONTROL,
                MIN_FRAMES_SMALL, 0.25f, 0.12f, fail, review);
        evaluateScenario(metrics, UrbanValidationTelemetry.Scenario.PERSON_VEHICLE,
                MIN_FRAMES_MAJOR, 0.45f, 0.25f, fail, review);

        ScenarioMetric lowLight = metrics.get(UrbanValidationTelemetry.Scenario.LOW_LIGHT);
        if (lowLight == null || lowLight.frames() < MIN_FRAMES_LOW_LIGHT) {
            fail.add("Düşük ışık senaryosu ölçülmedi");
        } else if (lowLight.evidenceRate() < 0.15f) {
            // Low light is not expected to remain semantically reliable. This only checks that the
            // advisory channel stays alive enough to record behavior; scene-health remains authority.
            review.add("Düşük ışıkta urban kanal çok zayıf; scene-health öncelikli olmalı");
        }

        if (!fail.isEmpty()) {
            List<String> reasons = new ArrayList<>(fail);
            reasons.addAll(review);
            return new Result(Verdict.FAIL, reasons);
        }
        if (!review.isEmpty()) return new Result(Verdict.REVIEW, review);
        return new Result(Verdict.PASS, Collections.singletonList(
                "urban performans ve senaryo tekrar ölçütleri karşılandı"));
    }

    private static void evaluateScenario(
            EnumMap<UrbanValidationTelemetry.Scenario, ScenarioMetric> metrics,
            UrbanValidationTelemetry.Scenario scenario,
            int minFrames,
            float passRate,
            float reviewRate,
            List<String> fail,
            List<String> review) {
        ScenarioMetric m = metrics.get(scenario);
        if (m == null || m.frames() < minFrames) {
            fail.add(scenario.label() + " ölçümü yetersiz");
            return;
        }
        float rate = m.evidenceRate();
        if (rate < reviewRate) {
            fail.add(scenario.label() + " kanıtı %" + Math.round(rate * 100f));
        } else if (rate < passRate) {
            review.add(scenario.label() + " kanıtı %" + Math.round(rate * 100f));
        }
    }
}
