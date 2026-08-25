package com.mg.structuralai;

/** Explains the most likely physical/numerical reason behind the peak-stress region. */
public final class CriticalRegionExplainer {
    private CriticalRegionExplainer(){}

    public static String explain(AutonomousAnalysisPlanner.Plan p, HotspotSingularityAnalyzer.Result h){
        StringBuilder s=new StringBuilder();
        if(h.type==HotspotSingularityAnalyzer.Type.POSSIBLE_SINGULARITY){
            s.append("Peak stress is likely dominated by a numerical/geometric singularity because it keeps rising with mesh refinement. ");
            s.append("Do not use the raw maximum for FoS/capacity. Local geometry healing, radius/contact representation, or adaptive refinement is required.");
            return s.toString();
        }
        if(h.type==HotspotSingularityAnalyzer.Type.UNRESOLVED){
            s.append("Critical region is unresolved: the peak-stress trend has not stabilized. ");
            s.append("The application should refine locally before assigning a physical failure interpretation.");
            return s.toString();
        }
        s.append("Peak stress behaves as a converged hotspot. ");
        if(p.featureSummary!=null && p.featureSummary.contains("FLANGE")) s.append("A flange/mounting-face feature was detected, so load introduction or restraint transfer near this region is a plausible driver. ");
        if(p.featureSummary!=null && p.featureSummary.contains("HOLE")) s.append("Hole-like features are present; local stress concentration around a hole edge is a plausible contributor. ");
        if(p.geometryClass!=null && p.geometryClass.startsWith("SLENDER")) s.append("The part is beam-like, so bending load path and root restraint are likely dominant. ");
        s.append("Interpretation confidence is limited by the evidence available in geometry-only input.");
        return s.toString();
    }
}
