package com.mg.structuralai;

import java.util.Locale;

/** End-to-end autonomous static screening pipeline for geometry-only models. */
public final class AutonomousAnalysisRunner {
    public static final class Result {
        public final AutonomousAnalysisPlanner.Plan plan;
        public final AutonomousScenarioRanker.Scenario scenario;
        public final boolean numericallyReady;
        public final double capacityScaleToYield;
        public final String report;
        Result(AutonomousAnalysisPlanner.Plan p,AutonomousScenarioRanker.Scenario s,boolean r,double c,String txt){plan=p;scenario=s;numericallyReady=r;capacityScaleToYield=c;report=txt;}
    }
    private AutonomousAnalysisRunner(){}

    public static Result run(MeshModel model){
        SurfaceTopologyReport topo=SurfaceTopologyReport.evaluate(model);
        if(!topo.closedManifold) throw new IllegalStateException("Geometry QA blocked: "+topo.summary());
        AutonomousAnalysisPlanner.Plan p=AutonomousAnalysisPlanner.infer(model);
        AutonomousScenarioRanker.Scenario s=AutonomousScenarioRanker.runAndRank(model,p);
        if(s.convergence==null) throw new IllegalStateException("No autonomous scenario passed numerical setup");
        MeshConvergenceStudy.Level f=s.convergence.fine;
        boolean ready=s.convergence.converged && f.fem.linearSolve.converged && f.fem.forceEquilibriumRelativeError<1e-5;
        double cap=f.fem.maxVonMisesPa>0?p.material.yieldPa/f.fem.maxVonMisesPa:Double.POSITIVE_INFINITY;
        String txt=String.format(Locale.US,
            "AUTONOMOUS STATIC ANALYSIS\n\nGeometry class: %s\nGeometry QA: %s\nDetected features: %s\nSelected scenario: %s\nScenario score: %.2f/1.00\n\nASSUMPTIONS / CONFIDENCE\nUnit: %.0f %% • %s\nMaterial: %.0f %% • %s\nSupport: %.0f %% • %s\nLoad: %.0f %% • %s\n\nLOAD POLICY\nReal service load was NOT invented. Solver used a 1 N influence load.\nCapacity-to-reference-yield scale: %.6g N-equivalent for this assumed reference material/scenario.\n\nFINE FEM\nUmax per 1 N: %.6g mm\nVon Mises per 1 N: %.6g MPa\nResidual: %.3e\nForce equilibrium error: %.3e\nMesh convergence: %s\nΔU: %.2f %% • Δσ: %.2f %%\n\nAUTONOMOUS NUMERICAL GATE: %s\n\nThis is an autonomous geometry-only engineering screening result. Physical certification still requires evidence for real material, fixture and service load.",
            p.geometryClass,topo.summary(),p.featureSummary,s.name,s.totalScore,
            p.unitConfidence*100,p.unitReason,p.materialConfidence*100,p.materialReason,
            p.supportConfidence*100,p.supportReason,p.loadConfidence*100,p.loadReason,
            cap,f.fem.maxDisplacementM*1000,f.fem.maxVonMisesPa/1e6,
            f.fem.linearSolve.relativeResidual,f.fem.forceEquilibriumRelativeError,
            s.convergence.converged,s.convergence.displacementChange*100,s.convergence.stressChange*100,
            ready?"PASS":"BLOCKED");
        return new Result(p,s,ready,cap,txt);
    }
}
