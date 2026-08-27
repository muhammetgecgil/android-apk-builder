package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fast in-app regression that catches broken FEM mapping, theory drift, contact and production-mesh regressions before user analyses. */
public final class AutonomousRegressionGate {
    public static final class Result {
        public final boolean pass;
        public final String summary;
        Result(boolean p,String s){pass=p;summary=s;}
    }
    private static volatile Result cached;
    private AutonomousRegressionGate(){}

    public static Result run(){
        Result c=cached; if(c!=null)return c;
        try{
            MeshModel s=beamSurface();
            VoxelTetMesher.Result mr=VoxelTetMesher.generate(s,10,0.001);
            if(!mr.quality.pass)return cached=new Result(false,"REGRESSION FAIL: mesh QA "+mr.quality.summary());
            LinearElasticMaterial mat=new LinearElasticMaterial("Regression steel",210e9,0.30,7850,355e6);
            StaticFemSolver solver=new StaticFemSolver(mr.mesh,mat);
            List<MeshModel.V3> sup=new ArrayList<>(),load=new ArrayList<>();
            for(MeshModel.V3 v:s.vertices){if(Math.abs(v.x)<=1e-9)sup.add(v);if(Math.abs(v.x-100.0)<=1e-9)load.add(v);}
            AdvancedFemLoads.Result map=AdvancedFemLoads.apply(solver,mr.mesh,s,sup,load,0.001,0,0,-1.0,0,false,7850);
            StaticFemSolver.Result r=solver.solve();
            boolean mapOk=map.fixedNodes>=3 && map.loadedNodes>0;
            boolean response=Double.isFinite(r.maxDisplacementM)&&Double.isFinite(r.maxVonMisesPa)&&r.maxDisplacementM>1e-15&&r.maxVonMisesPa>1e-6;
            boolean solve=r.linearSolve.converged && r.forceEquilibriumRelativeError<1e-5;
            boolean femPass=mapOk&&response&&solve;
            String femMsg=String.format(Locale.US,
                "Cantilever mapping regression %s | fixed=%d loaded=%d | F=(%.4g,%.4g,%.4g) N | U=%.6g mm | VM=%.6g MPa | eqErr=%.3e | residual=%.3e",
                femPass?"PASS":"FAIL",map.fixedNodes,map.loadedNodes,map.resultantFx,map.resultantFy,map.resultantFz,
                r.maxDisplacementM*1000,r.maxVonMisesPa/1e6,r.forceEquilibriumRelativeError,r.linearSolve.relativeResidual);

            FemBenchmarks.BenchmarkResult tet=FemBenchmarks.unitTetSanity();
            FemBenchmarks.BenchmarkResult theory=FemBenchmarks.cantileverTheory();
            AxialSolidBenchmark.Result axial=AxialSolidBenchmark.run();
            ContactRegressionGate.Result cr=ContactRegressionGate.run();
            ProductionMeshGate.Result pm=ProductionMeshGate.run();
            boolean pass=femPass&&tet.pass&&theory.pass&&axial.pass&&cr.pass&&pm.pass;
            return cached=new Result(pass,femMsg+"\n"+tet.message+"\n"+theory.message+"\n"+axial.message+"\n"+cr.summary+"\n"+pm.summary);
        }catch(Throwable t){return cached=new Result(false,"REGRESSION ERROR: "+t.getMessage());}
    }

    private static MeshModel beamSurface(){
        MeshModel m=new MeshModel();
        double[][] v={{0,-10,-5},{0,10,-5},{0,10,5},{0,-10,5},{100,-10,-5},{100,10,-5},{100,10,5},{100,-10,5}};
        for(double[] p:v)m.addVertex(new MeshModel.V3(p[0],p[1],p[2]));
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
        for(int[] t:f)m.triangles.add(t);
        return m;
    }
}
