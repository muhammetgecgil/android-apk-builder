package com.mg.structuralai;

/** Explains baseline hotspot evidence without contradicting the authoritative final adaptive state. */
public final class CriticalRegionExplainer {
    private CriticalRegionExplainer(){}

    public static String explain(AutonomousAnalysisPlanner.Plan p, HotspotSingularityAnalyzer.Result h){
        StringBuilder s=new StringBuilder();
        if(h.type==HotspotSingularityAnalyzer.Type.POSSIBLE_SINGULARITY){
            s.append("Baseline hotspot evidence suggested a possible numerical/geometric singularity because the raw peak kept rising with refinement. ");
            s.append("This baseline observation triggered escalation; use the FINAL 3D HOTSPOT / SINGULARITY state above as the authoritative numerical decision. Raw maxima must not be used for FoS/capacity unless that final state validates them.");
            return s.toString();
        }
        if(h.type==HotspotSingularityAnalyzer.Type.UNRESOLVED){
            s.append("Baseline hotspot evidence was unresolved and therefore triggered adaptive/local refinement. ");
            s.append("This is not the final convergence state. Use the FINAL 3D HOTSPOT / SINGULARITY state above as the authoritative decision after escalation.");
            return s.toString();
        }
        s.append("Baseline peak stress behaved as a converged hotspot. ");
        if(p.featureSummary!=null && p.featureSummary.contains("FLANGE")) s.append("A flange/mounting-face feature was detected, so load introduction or restraint transfer near this region is a plausible driver. ");
        if(p.featureSummary!=null && p.featureSummary.contains("HOLE")) s.append("Hole-like features are present; local stress concentration around a hole edge is a plausible contributor. ");
        if(p.geometryClass!=null && p.geometryClass.startsWith("SLENDER")) s.append("The part is beam-like, so bending load path and root restraint are likely dominant. ");
        s.append("Interpretation confidence is limited by the evidence available in geometry-only input.");
        return s.toString();
    }
}
