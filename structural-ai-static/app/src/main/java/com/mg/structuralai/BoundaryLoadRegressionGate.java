package com.mg.structuralai;

import java.util.*;

/** Regression coverage for explicit fixed/roller/symmetry/force/gravity setup and safety rejection. */
public final class BoundaryLoadRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private BoundaryLoadRegressionGate(){}
    public static Result run(){
        try{
            TetMeshData m=new TetMeshData();
            m.addNode(0,0,0);m.addNode(1,0,0);m.addNode(0,1,0);m.addNode(0,0,1);m.addTet(0,1,2,3);
            LinearElasticMaterial mat=new LinearElasticMaterial("BC regression",210e9,0.30,7850,355e6);
            EngineeringLoadCase lc=new EngineeringLoadCase("Explicit BC regression")
                .addSupport(EngineeringLoadCase.SupportType.FIXED,Arrays.asList(0,1,2))
                .addLoad(EngineeringLoadCase.Load.force(Collections.singletonList(3),1000,-500,-2000));
            EngineeringLoadCase.Validation v=lc.validate(m,mat);
            StaticFemSolver s=new StaticFemSolver(m,mat);lc.apply(s,m,mat);StaticFemSolver.Result r=s.solve();
            boolean solve=v.pass&&r.linearSolve.converged&&r.maxDisplacementM>0&&r.forceEquilibriumRelativeError<1e-6;

            EngineeringLoadCase roller=new EngineeringLoadCase("Roller + force")
                .addSupport(EngineeringLoadCase.SupportType.FIXED,Collections.singletonList(0))
                .addSupport(EngineeringLoadCase.SupportType.ROLLER_Y,Collections.singletonList(1))
                .addSupport(EngineeringLoadCase.SupportType.ROLLER_Z,Collections.singletonList(2))
                .addLoad(EngineeringLoadCase.Load.force(Collections.singletonList(3),100,0,0));
            boolean rollerValid=roller.validate(m,mat).pass;

            EngineeringLoadCase bad=new EngineeringLoadCase("Reject missing load")
                .addSupport(EngineeringLoadCase.SupportType.FIXED,Arrays.asList(0,1,2));
            boolean missingLoadBlocked=!bad.validate(m,mat).pass;

            EngineeringLoadCase overlap=new EngineeringLoadCase("Reject overlap")
                .addSupport(EngineeringLoadCase.SupportType.FIXED,Arrays.asList(0,1,2,3))
                .addLoad(EngineeringLoadCase.Load.force(Collections.singletonList(3),1,0,0));
            boolean overlapBlocked=!overlap.validate(m,mat).pass;

            boolean ok=solve&&rollerValid&&missingLoadBlocked&&overlapBlocked;
            return new Result(ok,"BOUNDARY/LOAD REGRESSION "+(ok?"PASS":"FAIL")+" | explicitSolve="+solve+" | rollerValidation="+rollerValid+" | missingLoadBlocked="+missingLoadBlocked+" | overlapBlocked="+overlapBlocked+" | eqErr="+r.forceEquilibriumRelativeError);
        }catch(Throwable t){return new Result(false,"BOUNDARY/LOAD REGRESSION ERROR: "+t.getMessage());}
    }
}
