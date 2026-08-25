package com.mg.structuralai;

import java.util.List;

/** Three-level h-refinement study for user-selected static load cases. */
public final class MeshConvergenceStudy {
    public static final class Level {
        public final int cells,nodes,tets; public final StaticFemSolver.Result fem; public final MeshQualityReport quality; public final AdvancedFemLoads.Result setup;
        Level(int c,VoxelTetMesher.Result m,StaticFemSolver.Result f,AdvancedFemLoads.Result s){cells=c;nodes=m.mesh.nodes.size();tets=m.mesh.tets.size();quality=m.quality;fem=f;setup=s;}
    }
    public static final class Result {
        public final Level coarse,medium,fine; public final double displacementChange,stressChange; public final boolean converged;
        Result(Level a,Level b,Level c,double du,double ds,boolean ok){coarse=a;medium=b;fine=c;displacementChange=du;stressChange=ds;converged=ok;}
    }
    private MeshConvergenceStudy(){}

    public static Result run(MeshModel surface,double scale,LinearElasticMaterial mat,List<MeshModel.V3> supports,List<MeshModel.V3> loads,
                             double fx,double fy,double fz,double pressurePa,boolean gravity,double rho){
        Level a=solveLevel(surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,8);
        Level b=solveLevel(surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,12);
        Level c=solveLevel(surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,16);
        double du=rel(c.fem.maxDisplacementM,b.fem.maxDisplacementM);
        double ds=rel(c.fem.maxVonMisesPa,b.fem.maxVonMisesPa);
        boolean ok=c.quality.pass && b.quality.pass && c.fem.linearSolve.converged && b.fem.linearSolve.converged && c.fem.forceEquilibriumRelativeError<1e-5 && b.fem.forceEquilibriumRelativeError<1e-5 && du<=0.05 && ds<=0.10;
        return new Result(a,b,c,du,ds,ok);
    }

    private static Level solveLevel(MeshModel surface,double scale,LinearElasticMaterial mat,List<MeshModel.V3> supports,List<MeshModel.V3> loads,
                                    double fx,double fy,double fz,double pressurePa,boolean gravity,double rho,int cells){
        VoxelTetMesher.Result mr=VoxelTetMesher.generate(surface,cells,scale);
        if(!mr.quality.pass) throw new IllegalStateException("Mesh QA failed at level "+cells+": "+mr.quality.summary());
        StaticFemSolver solver=new StaticFemSolver(mr.mesh,mat);
        AdvancedFemLoads.Result setup=AdvancedFemLoads.apply(solver,mr.mesh,surface,supports,loads,scale,fx,fy,fz,pressurePa,gravity,rho);
        StaticFemSolver.Result fem=solver.solve();
        return new Level(cells,mr,fem,setup);
    }

    private static double rel(double a,double b){double d=Math.max(Math.max(Math.abs(a),Math.abs(b)),1e-30);return Math.abs(a-b)/d;}
}
