package com.mgecgil.seslirehber.core;

import static com.mgecgil.seslirehber.core.GuidanceModels.*;

public final class SafetyGate {
    private static final long SCENE_HEALTH_MAX_SKEW_MS = 700L;
    private static final long WALKABLE_MAX_SKEW_MS = 500L;
    private final CollisionCorridor collisionCorridor = new CollisionCorridor();

    public GuidanceDecision evaluate(MotionObservation observation, float deviceStability) {
        GuidanceDecision preflight = preflight(observation.timestampMs(), deviceStability);
        if (preflight != null) return preflight;
        if (deviceStability < 0.35f) return unstable(deviceStability);
        float fused = clamp(observation.visionConfidence() * (0.55f + 0.45f * deviceStability));
        if (observation.changedAreaRatio() < 0.025f) return quiet(fused);
        if (fused < 0.58f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    observation.direction(),
                    "Hareket algısı belirsiz. Bastonla doğrula.",
                    fused);
        }
        return new GuidanceDecision(
                Risk.CAUTION,
                observation.direction(),
                side(observation.direction()) + " hareket var.",
                fused);
    }

    public GuidanceDecision evaluateObject(ObjectObservation observation, float deviceStability) {
        GuidanceDecision preflight = preflight(observation.timestampMs(), deviceStability);
        if (preflight != null) return preflight;
        if (deviceStability < 0.35f) return unstable(deviceStability);

        float fused = clamp(observation.visionConfidence() * (0.60f + 0.40f * deviceStability));
        if (observation.areaRatio() < 0.022f || fused < 0.46f) return quiet(fused);

        CorridorAssessment corridor = collisionCorridor.assess(observation, deviceStability);
        boolean centerStop = corridor.inCorridor()
                && fused >= 0.58f
                && (
                        corridor.hazardScore() >= 0.60f
                        || (corridor.approachScore() >= 0.82f && observation.areaRatio() >= 0.075f)
                        || corridor.sizeScore() >= 0.88f);

        if (centerStop) {
            String reason = corridor.approachScore() >= 0.42f
                    ? "Dur. Önünde yaklaşan engel."
                    : "Dur. Önünde büyük engel.";
            return new GuidanceDecision(Risk.STOP, Direction.CENTER, reason, fused);
        }

        if (corridor.crossingIntoCorridor() && fused >= 0.56f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    observation.direction(),
                    side(observation.direction()) + " engel önüne doğru hareket ediyor.",
                    fused);
        }

        if (corridor.inCorridor()
                && (corridor.hazardScore() >= 0.31f
                    || observation.areaRatio() >= 0.09f
                    || observation.isApproaching())) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Önde engel. Yavaşla ve bastonla doğrula.",
                    fused);
        }

        if (observation.isApproaching() && fused >= 0.54f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    observation.direction(),
                    side(observation.direction()) + " engel yaklaşıyor.",
                    fused);
        }

        if (corridor.hazardScore() >= 0.24f && observation.areaRatio() >= 0.07f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    observation.direction(),
                    side(observation.direction()) + " engel.",
                    fused);
        }
        return quiet(fused);
    }

    /** Camera usability is a P0 prerequisite. Persistent unusable imagery fails safe to STOP. */
    public GuidanceDecision evaluateSceneHealth(SceneHealthObservation observation, float deviceStability) {
        float fused = clamp((1f - observation.qualityScore()) * (0.72f + 0.28f * (1f - deviceStability)));
        if (!observation.persistentlyUnusable()) return quiet(1f - fused);

        String reason;
        if (observation.darkRatio() >= 0.78f) {
            reason = "Dur. Kamera görüntüsü çok karanlık veya kapalı. Bastonla doğrula.";
        } else if (observation.brightRatio() >= 0.78f) {
            reason = "Dur. Kamera görüntüsü aşırı parlak. Bastonla doğrula.";
        } else {
            reason = "Dur. Kamera görüntüsü güvenilir değil. Bastonla doğrula.";
        }
        return new GuidanceDecision(Risk.STOP, Direction.UNKNOWN, reason,
                clamp(Math.max(0.68f, observation.unusableScore())));
    }

    /**
     * Relative openness is advisory only. It may say one side appears more open after persistence,
     * but never instructs the user to turn or calls that lane safe.
     */
    public GuidanceDecision evaluateWalkable(
            WalkableCorridorObservation observation,
            float deviceStability) {
        if (deviceStability < 0.35f) return unstable(deviceStability);
        float fused = clamp(observation.confidence() * (0.58f + 0.42f * deviceStability));
        if (fused < 0.52f || observation.persistenceScore() < 0.54f) return quiet(fused);

        if (observation.centerOpenScore() <= 0.24f
                && observation.leftOpenScore() <= 0.30f
                && observation.rightOpenScore() <= 0.30f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Ön geçiş alanı dar görünüyor. Yavaşla ve bastonla doğrula.",
                    fused);
        }

        if (observation.hasPersistentCandidate()) {
            Direction direction = observation.moreOpenDirection();
            float sideScore = observation.score(direction);
            if (observation.centerOpenScore() <= 0.52f
                    && sideScore - observation.centerOpenScore() >= 0.18f) {
                return new GuidanceDecision(
                        Risk.CAUTION,
                        direction,
                        direction == Direction.LEFT
                                ? "Ön koridor daralıyor. Sol taraf daha açık görünüyor; bastonla doğrula."
                                : "Ön koridor daralıyor. Sağ taraf daha açık görünüyor; bastonla doğrula.",
                        fused);
            }
        }
        return quiet(fused);
    }

    public GuidanceDecision evaluateGround(GroundObservation observation, float deviceStability) {
        GuidanceDecision preflight = preflight(observation.timestampMs(), deviceStability);
        if (preflight != null) return preflight;
        if (deviceStability < 0.35f) return unstable(deviceStability);

        float fused = clamp(observation.viewConfidence() * (0.52f + 0.48f * deviceStability));
        if (observation.viewConfidence() < 0.34f) return quiet(fused);

        boolean persistent = observation.persistenceScore() >= 0.58f;
        boolean strong = observation.anomalyScore() >= 0.62f
                && observation.broadBoundaryScore() >= 0.50f;
        if (persistent && strong && fused >= 0.46f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Ön zeminde süreklilik bozuluyor. Yavaşla ve bastonla doğrula.",
                    fused);
        }

        if (observation.persistenceScore() >= 0.82f
                && observation.anomalyScore() >= 0.54f
                && observation.broadBoundaryScore() >= 0.42f
                && fused >= 0.44f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Ön zemin belirsiz. Yavaşla ve bastonla doğrula.",
                    fused);
        }
        return quiet(fused);
    }

    public GuidanceDecision evaluateDepth(DepthObservation depth, float deviceStability) {
        GuidanceDecision preflight = preflight(depth.timestampMs(), deviceStability);
        if (preflight != null) return preflight;
        if (deviceStability < 0.35f) return unstable(deviceStability);
        float fused = clamp(depth.depthConfidence() * (0.58f + 0.42f * deviceStability));
        if (depth.strongDiscontinuity() && fused >= 0.52f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Ön bölgede ani derinlik değişimi algılandı. Yavaşla ve bastonla doğrula.",
                    fused);
        }
        WalkableCorridorObservation walkable =
                PerceptionContext.walkableNear(depth.timestampMs(), WALKABLE_MAX_SKEW_MS);
        if (walkable != null) {
            GuidanceDecision corridor = evaluateWalkable(walkable, deviceStability);
            if (corridor.risk() != Risk.INFO) return corridor;
        }
        return quiet(fused);
    }

    public GuidanceDecision evaluateGroundWithDepth(
            GroundObservation ground,
            DepthObservation depth,
            float deviceStability) {
        long sourceTimestamp = Math.max(ground.timestampMs(), depth.timestampMs());
        GuidanceDecision preflight = preflight(sourceTimestamp, deviceStability);
        if (preflight != null) return preflight;
        if (deviceStability < 0.35f) return unstable(deviceStability);

        float groundConfidence = clamp(ground.viewConfidence() * (0.52f + 0.48f * deviceStability));
        float depthConfidence = clamp(depth.depthConfidence() * (0.58f + 0.42f * deviceStability));
        float fused = clamp((float) Math.sqrt(Math.max(0f, groundConfidence * depthConfidence)));

        boolean groundStrong = ground.persistenceScore() >= 0.64f
                && ground.anomalyScore() >= 0.64f
                && ground.broadBoundaryScore() >= 0.52f;
        boolean depthStrong = depth.strongDiscontinuity();

        if (groundStrong && depthStrong && fused >= 0.56f) {
            return new GuidanceDecision(
                    Risk.STOP,
                    Direction.CENTER,
                    "Dur. Ön zeminde ani derinlik değişimi. Bastonla doğrula.",
                    fused);
        }

        if (depthStrong
                && ground.persistenceScore() >= 0.42f
                && ground.anomalyScore() >= 0.46f
                && fused >= 0.48f) {
            return new GuidanceDecision(
                    Risk.CAUTION,
                    Direction.CENTER,
                    "Ön zemin ve derinlik birlikte değişiyor. Yavaşla ve bastonla doğrula.",
                    fused);
        }

        GuidanceDecision groundOnly = evaluateGround(ground, deviceStability);
        if (groundOnly.risk() != Risk.INFO) return groundOnly;
        GuidanceDecision depthOnly = evaluateDepth(depth, deviceStability);
        if (depthOnly.risk() != Risk.INFO) return depthOnly;

        WalkableCorridorObservation walkable =
                PerceptionContext.walkableNear(sourceTimestamp, WALKABLE_MAX_SKEW_MS);
        return walkable == null ? quiet(fused) : evaluateWalkable(walkable, deviceStability);
    }

    public CorridorAssessment assessCorridor(ObjectObservation observation, float deviceStability) {
        return collisionCorridor.assess(observation, deviceStability);
    }

    private GuidanceDecision preflight(long timestampMs, float deviceStability) {
        SceneHealthObservation scene =
                PerceptionContext.sceneHealthNear(timestampMs, SCENE_HEALTH_MAX_SKEW_MS);
        if (scene == null) return null;
        GuidanceDecision sceneDecision = evaluateSceneHealth(scene, deviceStability);
        return sceneDecision.risk() == Risk.STOP ? sceneDecision : null;
    }

    private static GuidanceDecision unstable(float stability) {
        return new GuidanceDecision(
                Risk.STOP,
                Direction.UNKNOWN,
                "Telefon veya görüntü kararsız. Dur ve bastonla doğrula.",
                clamp(1f - stability));
    }

    private static GuidanceDecision quiet(float confidence) {
        return new GuidanceDecision(Risk.INFO, Direction.UNKNOWN, "", confidence);
    }

    private static String side(Direction direction) {
        return switch (direction) {
            case LEFT -> "Solda";
            case RIGHT -> "Sağda";
            case CENTER -> "Önde";
            default -> "Çevrede";
        };
    }

    private static float clamp(float x) {
        return Math.max(0f, Math.min(1f, x));
    }
}
