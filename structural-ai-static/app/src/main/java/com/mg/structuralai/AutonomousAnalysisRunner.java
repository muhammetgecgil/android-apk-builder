package com.mg.structuralai;

import java.util.Locale;

/** End-to-end autonomous static screening pipeline for geometry-only models. */
public final class AutonomousAnalysisRunner {
    public static final class Result {
        public final AutonomousAnalysisPlanner.Plan plan; public final AutonomousScenarioRanker.Scenario scenario;
        public final MaterialScaleScenarioEngine.Result uncertainty; public final HotspotSingularityAnalyzer.Result hotspot;
        public final AdaptiveRefinementStudy.Result adaptive; public final RegionalHotspotAnalyzer.Result regional;
        public final AutoIdealizationEngine.Result idealization; public final TetMeshData displayMesh; public final StaticFemSolver.Result displayFem;
        public final boolean numericallyReady; public final String report;
        Result(AutonomousAnalysisPlanner.Plan p,AutonomousScenarioRanker.Scenario s,MaterialScaleScenarioEngine.Result u,HotspotSingularityAnalyzer.Result h,AdaptiveRefinementStudy.Result a,RegionalHotspotAnalyzer.Result rh,AutoIdealizationEngine.Result id,TetMeshData dm,StaticFemSolver.Result df,boolean r,String txt){plan=p;scenario=s;uncertainty=u;hotspot=h;adaptive=a;regional=rh;idealization=id;displayMesh=dm;displayFem=df;numericallyReady=r;report=txt;}
    }
    private AutonomousAnalysisRunner(){}

    public static Result run(MeshModel model){
        SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(model);if(!topo.closedManifold)throw new IllegalStateException("Geometry QA blocked: "+topo.summary());
        AutonomousAnalysisPlanner.Plan p=AutonomousAnalysisPlanner.infer(model);AutonomousScenarioRanker.Scenario s=AutonomousScenarioRanker.runAndRank(model,p);if(s.convergence==null)throw new IllegalStateException("No autonomous scenario passed numerical setup");
        AutoIdealizationEngine.Result ideal=AutoIdealizationEngine.analyze(model,p,s.fx,s.fy,s.fz);

        MeshConvergenceStudy.Level base=s.convergence.fine;HotspotSingularityAnalyzer.Result h=HotspotSingularityAnalyzer.analyze(s.convergence);
        boolean needAdaptive=!s.convergence.converged||h.type!=HotspotSingularityAnalyzer.Type.CONVERGED_HOTSPOT;
        AdaptiveRefinementStudy.Result adaptive=null;RegionalHotspotAnalyzer.Result regional=null;TetMeshData finalMesh=base.mesh;StaticFemSolver.Result finalFem=base.fem;AdvancedFemLoads.Result finalSetup=base.setup;
        double finalDu=s.convergence.displacementChange,finalDs=s.convergence.stressChange;boolean finalConverged=s.convergence.converged;
        String adaptiveLine="Not required; baseline 8/12/16 study converged.",regionalLine="Not evaluated; adaptive study was not required.";
        if(needAdaptive){
            adaptive=AdaptiveRefinementStudy.run(model,p.unitScaleM,p.material,p.supports,p.loads,s.fx,s.fy,s.fz,0,false,p.material.densityKgM3);AdaptiveRefinementStudy.Step af=adaptive.finalStep;
            finalMesh=af.mesh;finalFem=af.fem;finalSetup=af.setup;finalDu=adaptive.displacementChange;finalDs=adaptive.stressChange;finalConverged=adaptive.converged;
            adaptiveLine=String.format(Locale.US,"levels=%d | final=%d cells/longest-axis | raw-max converged=%s | ΔU=%.2f%% | Δσmax=%.2f%% | hotspot shift=%.6g mm",adaptive.steps.size(),af.cells,adaptive.converged,adaptive.displacementChange*100,adaptive.stressChange*100,adaptive.hotspotShiftM*1000);
            regional=RegionalHotspotAnalyzer.analyze(adaptive);if(regional!=null)regionalLine=String.format(Locale.US,"P95=%.6g MPa | P99=%.6g MPa | rawMax=%.6g MPa | ΔP95=%.2f%% | ΔP99=%.2f%% | high-stress centroid shift=%.6g mm | regional=%s | isolated-peak=%s",regional.current.p95Pa/1e6,regional.current.p99Pa/1e6,regional.current.maxPa/1e6,regional.p95Change*100,regional.p99Change*100,regional.centroidShiftM*1000,regional.regionalConverged?"CONVERGED":"UNRESOLVED",regional.isolatedPeakSuspected?"SUSPECTED":"NO");
        }

        boolean mappingReady=finalSetup!=null&&finalSetup.fixedNodes>=3&&finalSetup.loadedNodes>0;
        boolean nonZeroResponse=Double.isFinite(finalFem.maxDisplacementM)&&Double.isFinite(finalFem.maxVonMisesPa)&&finalFem.maxDisplacementM>1e-15&&finalFem.maxVonMisesPa>1e-6;
        boolean regionalReady=regional!=null&&regional.regionalConverged;
        boolean solidPrimaryReady=(finalConverged||regionalReady)&&finalFem.linearSolve.converged&&finalFem.forceEquilibriumRelativeError<1e-5&&nonZeroResponse&&mappingReady;
        boolean solidHotspotReady=(adaptive!=null&&adaptive.converged)||h.type==HotspotSingularityAnalyzer.Type.CONVERGED_HOTSPOT||regionalReady;
        boolean idealReady=ideal.applicable&&ideal.confidence>=0.85&&Double.isFinite(ideal.displacementM)&&Double.isFinite(ideal.maxBendingStressPa)&&ideal.displacementM>0&&ideal.maxBendingStressPa>0;

        double uRatio=idealReady?finalFem.maxDisplacementM/ideal.displacementM:Double.NaN;
        double sRatio=idealReady?finalFem.maxVonMisesPa/ideal.maxBendingStressPa:Double.NaN;
        String idealLine=ideal.summary();
        if(idealReady)idealLine+=String.format(Locale.US,"\n3D cross-check (advisory): U3D/Ubeam=%.4f | sigma3D/sigmaBeam=%.4f | 3D convergence=%s. The beam model is the primary numerical model for this strongly recognized geometry; voxel-TET4 remains a secondary check.",uRatio,sRatio,finalConverged||regionalReady);

        MaterialScaleScenarioEngine.Result u=MaterialScaleScenarioEngine.evaluate(model,p,s.fx,s.fy,s.fz);boolean bandReady=u.passed>=3&&Double.isFinite(u.minCapacityN)&&Double.isFinite(u.maxCapacityN);
        boolean rawPeakCredible=!(regional!=null&&regional.isolatedPeakSuspected)&&solidHotspotReady;
        boolean solidReady=solidPrimaryReady&&bandReady&&rawPeakCredible;
        boolean ready=idealReady||solidReady;
        String capacityLine;
        if(idealReady&&p.materialConfidence<=0.0)capacityLine=String.format(Locale.US,"Actual yield capacity remains BLOCKED because material is unknown. Reference-material beam coefficient only: sigma/1N=%.6g MPa/N; with the normalization material yield %.6g MPa the non-certified reference load would be %.6g N.",ideal.maxBendingStressPa/1e6,p.material.yieldPa/1e6,p.material.yieldPa/ideal.maxBendingStressPa);
        else if(rawPeakCredible&&nonZeroResponse)capacityLine=String.format(Locale.US,"Yield-capacity band: %.6g to %.6g N-equivalent",u.minCapacityN,u.maxCapacityN);
        else capacityLine="Yield-capacity band: BLOCKED until material evidence and a credible stress model are available";

        String responseWatchdog=nonZeroResponse?"PASS":"FAIL — near-zero 3D FEM response detected";
        String mapDiag=String.format(Locale.US,"fixedNodes=%d | loadedNodes=%d | supportRegions=%d | loadRegions=%d | mapped resultant=(%.6g, %.6g, %.6g) N | mapping=%s",finalSetup.fixedNodes,finalSetup.loadedNodes,finalSetup.supportRegions,finalSetup.loadRegions,finalSetup.resultantFx,finalSetup.resultantFy,finalSetup.resultantFz,mappingReady?"PASS":"FAIL");
        String criticalWhy=CriticalRegionExplainer.explain(p,h);String hotspotText=(adaptive!=null&&adaptive.converged)?"CONVERGED_HOTSPOT after adaptive conforming-grid escalation; raw peak and hotspot location stabilized.":HotspotSingularityAnalyzer.summary(h);if(regionalReady&&!finalConverged)hotspotText+=" Regional high-stress field converged even though the single-element raw maximum did not.";
        String gateReason=idealReady?"PASS — strong beam idealization accepted; 3D FEM retained as advisory cross-check":(solidReady?"PASS — 3D solid numerical gates passed":"BLOCKED");
        String txt=String.format(Locale.US,
            "AUTONOMOUS STATIC ANALYSIS\n\nGeometry class: %s\nGeometry QA: %s\nDetected features: %s\nSelected load scenario: %s\nScenario score: %.2f/1.00\n\nAUTO IDEALIZATION\n%s\n\nASSUMPTIONS / CONFIDENCE\nUnit: %.0f %% • %s\nMaterial: %.0f %% • %s\nSupport: %.0f %% • %s\nLoad: %.0f %% • %s\n\nLOAD POLICY\nReal service load was NOT invented. Solver used a 1 N influence load.\n\nLOAD / SUPPORT MAPPING\n%s\n\nZERO-RESPONSE WATCHDOG\n%s\n\nADAPTIVE 3D REFINEMENT\n%s\n\nREGIONAL 3D HOTSPOT STATISTICS\n%s\n\n3D HOTSPOT / SINGULARITY\n%s\n\nWHY CRITICAL?\n%s\n\nUNCERTAINTY / CAPACITY\nMaterial/unit solid scenarios passed: %d / %d\n%s\n\nFINAL 3D FEM CROSS-CHECK\nUmax per 1 N: %.6g mm\nVon Mises raw max per 1 N: %.6g MPa\nResidual: %.3e\nForce equilibrium error: %.3e\n3D mesh convergence: %s\nΔU: %.2f %% • Δσmax: %.2f %%\n\nAUTONOMOUS NUMERICAL GATE: %s\n\nAuto-idealization is accepted only under strict rectangular-beam evidence. It does not invent service loads or actual material. General geometry remains on the 3D solid path until a trustworthy shell/beam idealization is proven.",
            p.geometryClass,topo.summary(),p.featureSummary,s.name,s.totalScore,idealLine,
            p.unitConfidence*100,p.unitReason,p.materialConfidence*100,p.materialReason,p.supportConfidence*100,p.supportReason,p.loadConfidence*100,p.loadReason,
            mapDiag,responseWatchdog,adaptiveLine,regionalLine,hotspotText,criticalWhy,u.passed,u.scenarios.size(),capacityLine,
            finalFem.maxDisplacementM*1000,finalFem.maxVonMisesPa/1e6,finalFem.linearSolve.relativeResidual,finalFem.forceEquilibriumRelativeError,finalConverged||regionalReady,finalDu*100,finalDs*100,gateReason);
        return new Result(p,s,u,h,adaptive,regional,ideal,finalMesh,finalFem,ready,txt);
    }
}
