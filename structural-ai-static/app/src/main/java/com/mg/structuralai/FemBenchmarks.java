package com.mg.structuralai;

/** Deterministic solver checks used by QA and later CI regression. */
public final class FemBenchmarks {
    public static final class BenchmarkResult {
        public final boolean pass;
        public final String message;
        public BenchmarkResult(boolean pass,String message){ this.pass=pass; this.message=message; }
    }
    private FemBenchmarks(){}

    /** One unit tetra: three nodes clamped, fourth node loaded. Checks solver residual and global force balance. */
    public static BenchmarkResult unitTetSanity(){
        TetMeshData m=new TetMeshData();
        m.addNode(0,0,0); m.addNode(1,0,0); m.addNode(0,1,0); m.addNode(0,0,1);
        m.addTet(0,1,2,3);
        LinearElasticMaterial steel=new LinearElasticMaterial("Benchmark steel",210e9,0.30,7850,355e6);
        StaticFemSolver s=new StaticFemSolver(m,steel);
        s.fixNode(0); s.fixNode(1); s.fixNode(2);
        s.addNodalForce(3,1000,-500,-2000);
        StaticFemSolver.Result r=s.solve();
        boolean finite=Double.isFinite(r.maxDisplacementM)&&Double.isFinite(r.maxVonMisesPa);
        boolean positive=r.maxDisplacementM>0&&r.maxVonMisesPa>0;
        boolean residual=r.linearSolve.relativeResidual<=1e-8;
        boolean equilibrium=r.forceEquilibriumRelativeError<=5e-9;
        boolean pass=finite&&positive&&residual&&equilibrium;
        String msg="unitTetSanity pass="+pass+", uMax="+r.maxDisplacementM+" m, vmMax="+r.maxVonMisesPa+
            " Pa, residual="+r.linearSolve.relativeResidual+", forceEqErr="+r.forceEquilibriumRelativeError+
            ", iterations="+r.linearSolve.iterations;
        return new BenchmarkResult(pass,msg);
    }
}
