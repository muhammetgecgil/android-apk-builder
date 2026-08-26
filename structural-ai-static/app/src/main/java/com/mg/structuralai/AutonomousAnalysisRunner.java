package com.mg.structuralai;

import java.util.Locale;

/** End-to-end autonomous static screening pipeline for geometry-only models. */
public final class AutonomousAnalysisRunner {
    public static final class Result {
        public final AutonomousAnalysisPlanner.Plan plan;
        public final AutonomousScenarioRanker.Scenario scenario;
        public final MaterialScaleScenarioEngine.Result uncertainty;
        public final HotspotSingularityAnalyzer.Result hotspot;
        public final AdaptiveRefinementStudy.Result adaptive;
        public final TetMeshData displayMesh;
        public final StaticFemSolver.Result displayFem;
        public final boolean numericallyReady;
        public final String report;
        Result(AutonomousAnalysisPlanner.Plan p,AutonomousScenarioRanker.Scenario s,MaterialScaleScenarioEngine.Result u,HotspotSingularityAnalyzer.Result h,
               AdaptiveRefinementStudy.Result a,TetMeshData dm,StaticFemSolver.Result df,boolean r,String txt){
            plan=p;scenario=s;uncertainty=u;hotspot=h;adaptive=a;displayMesh=dm;displayFem=df;numericallyReady=r;report=txt;
        }
    }
    private AutonomousAnalysisRunner(){}

    public static Result run(MeshModel model){
        SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(model);
        if(!topo.closedManifold)throw new IllegalStateException("Geometry QA blocked: "+topo.summary());
        AutonomousAnalysisPlanner.Plan p=AutonomousAnalysisPlanner.infer(model);
        AutonomousScenarioRanker.Scenario s=AutonomousScenarioRanker.runAndRank(model,p);
        if(s.convergence==null)throw new IllegalStateException("No autonomous scenario passed numerical setup");

        MeshConvergenceStudy.Level base=s.convergence.fine;
        HotspotSingularityAnalyzer.Result h=HotspotSingularityAnalyzer.analyze(s.convergence);
        boolean needAdaptive=!s.convergence.converged || h.type!=HotspotSingularityAnalyzer.Type.CONVERGED_HOTSPOT;
        AdaptiveRefinementStudy.Result adaptive=null;
        TetMeshData finalMesh=base.mesh;
        StaticFemSolver.Result finalFem=base.fem;
        AdvancedFemLoads.Result finalSetup=base.setup;
        double finalDu=s.convergence.displacementChange,finalDs=s.convergence.stressChange;
        boolean finalConverged=s.convergence.converged;
        String adaptiveLine="Not required; baseline 8/12/16 study converged.";

        if(needAdaptive){
            adaptive=AdaptiveRefinementStudy.run(model,p.unitScaleM,p.material,p.supports,p.loads,s.fx,s.fy,s.fz,0,false,p.material.densityKgM3);
            AdaptiveRefinementStudy.Step af=adaptive.finalStep;
            finalMesh=af.mesh;finalFem=af.fem;finalSetup=af.setup;finalDu=adaptive.displacementChange;finalDs=adaptive.stressChange;finalConverged=adaptive.converged;
            adaptiveLine=String.format(Locale.US,"levels=%d | final=%d cells/longest-axis | converged=%s | ΔU=%.2f%% | Δσ=%.2f%% | hotspot shift=%.6g mm",
                adaptive.steps.size(),af.cells,adaptive.converged,adaptive.displacementChange*100,adaptive.stressChange*100,adaptive.hotspotShiftM*1000);
        }

        boolean mappingReady=finalSetup!=null&&finalSetup.fixedNodes>=3&&finalSetup.loadedNodes>0;
        boolean nonZeroResponse=Double.isFinite(finalFem.maxDisplacementM)&&Double.isFinite(finalFem.maxVonMisesPa)&&finalFem.maxDisplacementM>1e-15&&finalFem.maxVonMisesPa>1e-6;
        boolean primaryReady=finalConverged&&finalFem.linearSolve.converged&&finalFem.forceEquilibriumRelativeError<1e-5&&nonZeroResponse&&mappingReady;
        boolean hotspotReady=(adaptive!=null&&adaptive.converged)||h.type==HotspotSingularityAnalyzer.Type.CONVERGED_HOTSPOT;

        MaterialScaleScenarioEngine.Result u=MaterialScaleScenarioEngine.evaluate(model,p,s.fx,s.fy,s.fz);
        boolean bandReady=u.passed>=3&&Double.isFinite(u.minCapacityN)&&Double.isFinite(u.maxCapacityN);
        boolean ready=primaryReady&&bandReady&&hotspotReady;
        String capacityLine=hotspotReady&&nonZeroResponse?String.format(Locale.US,"Yield-capacity band: %.6g to %.6g N-equivalent",u.minCapacityN,u.maxCapacityN):"Yield-capacity band: BLOCKED until a non-zero, converged stress response is obtained";
        String responseWatchdog=nonZeroResponse?"PASS":"FAIL — near-zero FEM response detected; load transfer / support mapping must be corrected before accepting any hotspot or capacity result";
        String mapDiag=String.format(Locale.US,"fixedNodes=%d | loadedNodes=%d | supportRegions=%d | loadRegions=%d | mapped resultant=(%.6g, %.6g, %.6g) N | mapping=%s",
            finalSetup.fixedNodes,finalSetup.loadedNodes,finalSetup.supportRegions,finalSetup.loadRegions,finalSetup.resultantFx,finalSetup.resultantFy,finalSetup.resultantFz,mappingReady?"PASS":"FAIL");
        String criticalWhy=CriticalRegionExplainer.explain(p,h);
        String hotspotText=(adaptive!=null&&adaptive.converged)?"CONVERGED_HOTSPOT after adaptive conforming-grid escalation; peak response and hotspot location stabilized.":HotspotSingularityAnalyzer.summary(h);
        String txt=String.format(Locale.US,
            "AUTONOMOUS STATIC ANALYSIS\n\nGeometry class: %s\nGeometry QA: %s\nDetected features: %s\nSelected load scenario: %s\nScenario score: %.2f/1.00\n\nASSUMPTIONS / CONFIDENCE\nUnit: %.0f %% • %s\nMaterial: %.0f %% • %s\nSupport: %.0f %% • %s\nLoad: %.0f %% • %s\n\nLOAD POLICY\nReal service load was NOT invented. Solver used a 1 N influence load.\n\nLOAD / SUPPORT MAPPING\n%s\n\nZERO-RESPONSE WATCHDOG\n%s\n\nADAPTIVE REFINEMENT\n%s\n\nHOTSPOT / SINGULARITY\n%s\n\nWHY CRITICAL?\n%s\n\nUNCERTAINTY ENVELOPE\nMaterial/unit scenarios passed: %d / %d\n%s\nThe band is intentionally reported instead of a single capacity because bare STL/OBJ does not prove material or units.\n\nFINAL FEM\nUmax per 1 N: %.6g mm\nVon Mises per 1 N: %.6g MPa\nResidual: %.3e\nForce equilibrium error: %.3e\nMesh convergence: %s\nΔU: %.2f %% • Δσ: %.2f %%\n\nAUTONOMOUS NUMERICAL GATE: %s\n\nThe current adaptive method preserves a conforming voxel-TET4 grid by escalating global resolution. True local octree/TET refinement is not yet claimed. Physical certification still requires evidence for actual material, fixture and service load.",
            p.geometryClass,topo.summary(),p.featureSummary,s.name,s.totalScore,
            p.unitConfidence*100,p.unitReason,p.materialConfidence*100,p.materialReason,
            p.supportConfidence*100,p.supportReason,p.loadConfidence*100,p.loadReason,
            mapDiag,responseWatchdog,adaptiveLine,hotspotText,criticalWhy,u.passed,u.scenarios.size(),capacityLine,
            finalFem.maxDisplacementM*1000,finalFem.maxVonMisesPa/1e6,
            finalFem.linearSolve.relativeResidual,finalFem.forceEquilibriumRelativeError,
            finalConverged,finalDu*100,finalDs*100,
            ready?"PASS":"BLOCKED");
        return new Result(p,s,u,h,adaptive,finalMesh,finalFem,ready,txt);
    }
}
