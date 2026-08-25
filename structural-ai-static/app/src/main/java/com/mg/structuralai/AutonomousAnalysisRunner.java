package com.mg.structuralai;

import java.util.Locale;

/** End-to-end autonomous static screening pipeline for geometry-only models. */
public final class AutonomousAnalysisRunner {
    public static final class Result {
        public final AutonomousAnalysisPlanner.Plan plan;
        public final AutonomousScenarioRanker.Scenario scenario;
        public final MaterialScaleScenarioEngine.Result uncertainty;
        public final HotspotSingularityAnalyzer.Result hotspot;
        public final boolean numericallyReady;
        public final String report;
        Result(AutonomousAnalysisPlanner.Plan p,AutonomousScenarioRanker.Scenario s,MaterialScaleScenarioEngine.Result u,HotspotSingularityAnalyzer.Result h,boolean r,String txt){plan=p;scenario=s;uncertainty=u;hotspot=h;numericallyReady=r;report=txt;}
    }
    private AutonomousAnalysisRunner(){}

    public static Result run(MeshModel model){
        SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(model);
        if(!topo.closedManifold) throw new IllegalStateException("Geometry QA blocked: "+topo.summary());
        AutonomousAnalysisPlanner.Plan p=AutonomousAnalysisPlanner.infer(model);
        AutonomousScenarioRanker.Scenario s=AutonomousScenarioRanker.runAndRank(model,p);
        if(s.convergence==null) throw new IllegalStateException("No autonomous scenario passed numerical setup");
        MeshConvergenceStudy.Level f=s.convergence.fine;
        boolean primaryReady=s.convergence.converged && f.fem.linearSolve.converged && f.fem.forceEquilibriumRelativeError<1e-5;
        HotspotSingularityAnalyzer.Result h=HotspotSingularityAnalyzer.analyze(s.convergence);
        boolean hotspotReady=h.type==HotspotSingularityAnalyzer.Type.CONVERGED_HOTSPOT;
        MaterialScaleScenarioEngine.Result u=MaterialScaleScenarioEngine.evaluate(model,p,s.fx,s.fy,s.fz);
        boolean bandReady=u.passed>=3 && Double.isFinite(u.minCapacityN) && Double.isFinite(u.maxCapacityN);
        boolean ready=primaryReady && bandReady && hotspotReady;
        String capacityLine=hotspotReady?String.format(Locale.US,"Yield-capacity band: %.6g to %.6g N-equivalent",u.minCapacityN,u.maxCapacityN):"Yield-capacity band: BLOCKED until peak-stress singularity status is resolved";
        String criticalWhy=CriticalRegionExplainer.explain(p,h);
        String txt=String.format(Locale.US,
            "AUTONOMOUS STATIC ANALYSIS\n\nGeometry class: %s\nGeometry QA: %s\nDetected features: %s\nSelected load scenario: %s\nScenario score: %.2f/1.00\n\nASSUMPTIONS / CONFIDENCE\nUnit: %.0f %% • %s\nMaterial: %.0f %% • %s\nSupport: %.0f %% • %s\nLoad: %.0f %% • %s\n\nLOAD POLICY\nReal service load was NOT invented. Solver used a 1 N influence load.\n\nHOTSPOT / SINGULARITY\n%s\n\nWHY CRITICAL?\n%s\n\nUNCERTAINTY ENVELOPE\nMaterial/unit scenarios passed: %d / %d\n%s\nThe band is intentionally reported instead of a single capacity because bare STL/OBJ does not prove material or units.\n\nPRIMARY FINE FEM\nUmax per 1 N: %.6g mm\nVon Mises per 1 N: %.6g MPa\nResidual: %.3e\nForce equilibrium error: %.3e\nMesh convergence: %s\nΔU: %.2f %% • Δσ: %.2f %%\n\nAUTONOMOUS NUMERICAL GATE: %s\n\nIf hotspot status is POSSIBLE_SINGULARITY or UNRESOLVED, the raw peak stress is not accepted for allowable/capacity decisions. This remains an autonomous geometry-only engineering screening result; physical certification still requires evidence for real material, fixture and service load.",
            p.geometryClass,topo.summary(),p.featureSummary,s.name,s.totalScore,
            p.unitConfidence*100,p.unitReason,p.materialConfidence*100,p.materialReason,
            p.supportConfidence*100,p.supportReason,p.loadConfidence*100,p.loadReason,
            HotspotSingularityAnalyzer.summary(h),criticalWhy,u.passed,u.scenarios.size(),capacityLine,
            f.fem.maxDisplacementM*1000,f.fem.maxVonMisesPa/1e6,
            f.fem.linearSolve.relativeResidual,f.fem.forceEquilibriumRelativeError,
            s.convergence.converged,s.convergence.displacementChange*100,s.convergence.stressChange*100,
            ready?"PASS":"BLOCKED");
        return new Result(p,s,u,h,ready,txt);
    }
}
