package com.mgecgil.seslirehber.core;

/**
 * Pure-Java timing/state machine for the M1 Urban Gate real-device test.
 * It never changes SafetyGate decisions and never declares navigation safety.
 */
public final class UrbanGateWizard {
    public enum Action { NONE, RETRY_PROMPT, ADVANCE, COMPLETE }

    public record Decision(
            Action action,
            long elapsedMs,
            long frames,
            long evidenceFrames) {}

    static final long MIN_DWELL_MS = 10_000L;
    static final long RETRY_PROMPT_MS = 14_000L;
    static final long MAX_DWELL_MS = 22_000L;

    private long scenarioStartedElapsedMs;
    private boolean retryPrompted;
    private boolean complete;

    public void start(long elapsedMs) {
        complete = false;
        beginScenario(elapsedMs);
    }

    public void beginScenario(long elapsedMs) {
        scenarioStartedElapsedMs = Math.max(0L, elapsedMs);
        retryPrompted = false;
    }

    public boolean isComplete() { return complete; }

    public Decision tick(
            long elapsedMs,
            UrbanValidationTelemetry.Scenario scenario,
            long frames,
            long evidenceFrames) {
        long elapsed = Math.max(0L, elapsedMs - scenarioStartedElapsedMs);
        if (complete || scenario == null) {
            return new Decision(Action.NONE, elapsed, frames, evidenceFrames);
        }

        boolean enough = enoughEvidence(scenario, frames, evidenceFrames);
        if (elapsed >= MIN_DWELL_MS && enough) {
            if (scenario == UrbanValidationTelemetry.Scenario.LOW_LIGHT) {
                complete = true;
                return new Decision(Action.COMPLETE, elapsed, frames, evidenceFrames);
            }
            return new Decision(Action.ADVANCE, elapsed, frames, evidenceFrames);
        }

        if (elapsed >= MAX_DWELL_MS) {
            if (scenario == UrbanValidationTelemetry.Scenario.LOW_LIGHT) {
                complete = true;
                return new Decision(Action.COMPLETE, elapsed, frames, evidenceFrames);
            }
            return new Decision(Action.ADVANCE, elapsed, frames, evidenceFrames);
        }

        if (!retryPrompted && elapsed >= RETRY_PROMPT_MS && !enough) {
            retryPrompted = true;
            return new Decision(Action.RETRY_PROMPT, elapsed, frames, evidenceFrames);
        }
        return new Decision(Action.NONE, elapsed, frames, evidenceFrames);
    }

    private static boolean enoughEvidence(
            UrbanValidationTelemetry.Scenario scenario,
            long frames,
            long evidenceFrames) {
        if (scenario == UrbanValidationTelemetry.Scenario.LOW_LIGHT) {
            return frames >= 6L;
        }
        long minFrames = switch (scenario) {
            case POLE_FENCE, TRAFFIC_CONTROL -> 8L;
            default -> 7L;
        };
        long minEvidence = switch (scenario) {
            case POLE_FENCE, TRAFFIC_CONTROL -> 2L;
            default -> 3L;
        };
        return frames >= minFrames && evidenceFrames >= minEvidence;
    }
}
