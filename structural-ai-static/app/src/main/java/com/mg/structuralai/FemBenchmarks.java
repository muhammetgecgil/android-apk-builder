package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic solver checks used by QA and later CI regression. */
public final class FemBenchmarks {
    public static final class BenchmarkResult {
        public final boolean pass;
        public final String message;
        public BenchmarkResult(boolean pass,String message){ this.pass=pass; this.message=message; }
    }
    private FemBenchmarks(){}

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

    /** Rectangular 100x20x10 mm cantilever against Euler-Bernoulli tip deflection. */
    public static BenchmarkResult cantileverTheory(){
        try{
            final double L=0.100,b=0.020,h=0.010,E=210e9,F=1.0;
            final int cells=60; // six cells through the 10 mm bending thickness.
            MeshModel surface=beamSurfaceMm();
            VoxelTetMesher.Result mr=VoxelTetMesher.generate(surface,cells,0.001);
            if(!mr.quality.pass)return new BenchmarkResult(false,"cantileverTheory FAIL meshQA: "+mr.quality.summary());
            LinearElasticMaterial mat=new LinearElasticMaterial("Benchmark steel",E,0.30,7850,355e6);
            StaticFemSolver solver=new StaticFemSolver(mr.mesh,mat);
            List<MeshModel.V3> sup=new ArrayList<>(),load=new ArrayList<>();
            for(MeshModel.V3 v:surface.vertices){if(Math.abs(v.x)<=1e-9)sup.add(v);if(Math.abs(v.x-100.0)<=1e-9)load.add(v);}
            AdvancedFemLoads.Result map=AdvancedFemLoads.apply(solver,mr.mesh,surface,sup,load,0.001,0,0,-F,0,false,7850);
            StaticFemSolver.Result r=solver.solve();
            double I=b*h*h*h/12.0;
            double theory=F*L*L*L/(3.0*E*I);
            double err=Math.abs(r.maxDisplacementM-theory)/theory;
            boolean mapping=map.fixedNodes>=3&&map.loadedNodes>0;
            boolean numerical=r.linearSolve.converged&&r.forceEquilibriumRelativeError<1e-5&&Double.isFinite(r.maxDisplacementM)&&r.maxDisplacementM>0;
            // v1.9.1 Accuracy Gate: raw solid displacement must be within 10% of closed form.
            boolean pass=mapping&&numerical&&err<=0.10;
            String msg=String.format(Locale.US,
                "cantileverTheory %s | U_FEM=%.6g mm | U_theory=%.6g mm | error=%.2f%% | grid=%dx%dx%d | cells=%d | tets=%d | fixed=%d loaded=%d | eqErr=%.3e | residual=%.3e | gate<=10%%",
                pass?"PASS":"FAIL",r.maxDisplacementM*1000,theory*1000,err*100,mr.nx,mr.ny,mr.nz,cells,mr.mesh.tets.size(),map.fixedNodes,map.loadedNodes,r.forceEquilibriumRelativeError,r.linearSolve.relativeResidual);
            return new BenchmarkResult(pass,msg);
        }catch(Throwable t){return new BenchmarkResult(false,"cantileverTheory ERROR: "+t.getMessage());}
    }

    private static MeshModel beamSurfaceMm(){
        MeshModel m=new MeshModel();
        double[][] v={{0,-10,-5},{0,10,-5},{0,10,5},{0,-10,5},{100,-10,-5},{100,10,-5},{100,10,5},{100,-10,5}};
        for(double[] p:v)m.addVertex(new MeshModel.V3(p[0],p[1],p[2]));
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
        for(int[] t:f)m.triangles.add(t);
        return m;
    }
}
