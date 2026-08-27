package com.mg.structuralai;

import java.util.Locale;

/** Autonomous alpha path for multi-body assemblies proven safe for bonded ties only. */
public final class AssemblyAutonomousRunner {
    public static final class Result {
        public final TetMeshData mesh; public final StaticFemSolver.Result fem; public final boolean ready; public final String report;
        Result(TetMeshData m,StaticFemSolver.Result f,boolean r,String s){mesh=m;fem=f;ready=r;report=s;}
    }
    private AssemblyAutonomousRunner(){}

    public static Result run(MeshModel source){
        ContactRegressionGate.Result regression=ContactRegressionGate.run();
        if(!regression.pass)throw new IllegalStateException(regression.summary);

        final double scale=0.001; // geometry-only STL/OBJ screening: mm reference, not a certified unit assertion
        AssemblyTetContactBuilder.Result a=AssemblyTetContactBuilder.build(source,10,scale);
        if(!a.safeBondedOnly)throw new IllegalStateException("CONTACT QA BLOCKED: "+a.summary);

        LinearElasticMaterial mat=new LinearElasticMaterial("Reference isotropic screening",210e9,0.30,7850,355e6);
        StaticFemSolver solver=new StaticFemSolver(a.mesh,mat);
        solver.addContactConstraints(a.constraints);

        double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(MeshModel.V3 p:a.mesh.nodes){
            lo[0]=Math.min(lo[0],p.x); lo[1]=Math.min(lo[1],p.y); lo[2]=Math.min(lo[2],p.z);
            hi[0]=Math.max(hi[0],p.x); hi[1]=Math.max(hi[1],p.y); hi[2]=Math.max(hi[2],p.z);
        }
        double[] span={hi[0]-lo[0],hi[1]-lo[1],hi[2]-lo[2]};
        int axis=span[1]>span[0]?1:0; if(span[2]>span[axis])axis=2;
        double tol=Math.max(span[axis]*0.04,1e-9);
        int fixed=0,loaded=0;
        for(int i=0;i<a.mesh.nodes.size();i++){
            MeshModel.V3 p=a.mesh.nodes.get(i); double q=axis==0?p.x:(axis==1?p.y:p.z);
            if(q<=lo[axis]+tol){solver.fixNode(i);fixed++;}
            if(q>=hi[axis]-tol)loaded++;
        }
        if(fixed<3||loaded<1)throw new IllegalStateException("Assembly support/load inference failed: fixed="+fixed+" loaded="+loaded);
        double each=-1.0/loaded;
        for(int i=0;i<a.mesh.nodes.size();i++){
            MeshModel.V3 p=a.mesh.nodes.get(i); double q=axis==0?p.x:(axis==1?p.y:p.z);
            if(q>=hi[axis]-tol){if(axis==2)solver.addNodalForce(i,0,each,0);else solver.addNodalForce(i,0,0,each);}
        }

        StaticFemSolver.Result f=solver.solve();
        boolean numerical=f.linearSolve.converged&&f.forceEquilibriumRelativeError<1e-5&&Double.isFinite(f.maxDisplacementM)&&f.maxDisplacementM>0&&Double.isFinite(f.maxVonMisesPa)&&f.maxVonMisesPa>0;
        String axisName=axis==0?"X":axis==1?"Y":"Z";
        String text=String.format(Locale.US,
            "CONTACT REGRESSION\n%s\n\nAUTONOMOUS BONDED ASSEMBLY\n%s\nDominant axis: %s\nReference unit assumption: 1 model unit = 1 mm (screening only)\nReference material: E=210 GPa, nu=0.30 (normalization only)\nSupport nodes: %d\nLoaded nodes: %d\nInfluence load: 1 N transverse\nBonded tie pairs: %d\nNodes: %d | TET4: %d\n\nSOLVER\nPCG converged: %s | iter=%d\nResidual: %.3e\nForce equilibrium error: %.3e\nUmax per 1N: %.9f mm\nVon Mises raw max per 1N: %.9f MPa\n\nAUTONOMOUS ASSEMBLY NUMERICAL GATE: %s\nDESIGN CAPACITY: BLOCKED — actual units, material, service load and physical contact definition are not proven.\nFrictionless/frictional/no-separation contacts remain blocked in this alpha path.",
            regression.summary,a.summary,axisName,fixed,loaded,a.constraints.bondedCount(),a.mesh.nodes.size(),a.mesh.tets.size(),f.linearSolve.converged,f.linearSolve.iterations,f.linearSolve.relativeResidual,f.forceEquilibriumRelativeError,f.maxDisplacementM*1000,f.maxVonMisesPa/1e6,numerical?"PASS":"BLOCKED");
        return new Result(a.mesh,f,numerical,text);
    }
}
