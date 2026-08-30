package com.mgecgil.seslirehber.core;

/** Connection point for validated on-device object, free-space and depth inference. */
public interface SceneUnderstandingEngine {
    record SceneResult(String summary, float confidence, boolean safeForGuidance) {}
    SceneResult latest();
}
