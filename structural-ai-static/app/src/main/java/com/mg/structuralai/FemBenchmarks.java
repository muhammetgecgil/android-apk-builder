package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic solver checks used by QA and later CI regression. */
public final class FemBenchmarks {
    public static final class BenchmarkResult { public final boolean pass; public final String message; public BenchmarkResult(boolean pass,String message){this.pass=pass;this.message=message;} }
    private FemBenchmarks(){}

    public static BenchmarkResult unitTetSanity(){
        TetMeshData m=new TetMeshData();m.addNode(0,0,0);m.addNode(1,0,0);m.addNode(0,1,0);m.addNode(0,0,1);m.addTet(0,1,2,3);
        LinearElasticMaterial steel=new LinearElasticMaterial("Benchmark steel",210e9,0.30,7850,355e6);StaticFemSolver s=new StaticFemSolver(m,steel);s.fixNode(0);s.fixNode(1);s.fixNode(2);s.addNodalForce(3,1000,-500,-2000);StaticFemSolver.Result r=s.solve();
        boolean pass=Double.isFinite(r.maxDisplacementM)&&Double.isFinite(r.maxVonMisesPa)&&r.maxDisplacementM>0&&r.maxVonMisesPa>0&&r.linearSolve.relativeResidual<=1e-8&&r.forceEquilibriumRelativeError<=5e-9;
        return new BenchmarkResult(pass,"unitTetSanity pass="+pass+", uMax="+r.maxDisplacementM+" m, vmMax="+r.maxVonMisesPa+" Pa, residual="+r.linearSolve.relativeResidual+", forceEqErr="+r.forceEquilibriumRelativeError+", iterations="+r.linearSolve.iterations);
    }

    /** 100x20x10 mm cantilever; v1.9.3 increases all three grid directions while retaining raw <=5% acceptance. */
    public static BenchmarkResult cantileverTheory(){
        try{
            final double L=0.100,b=0.020,h=0.010,E=210e9,F=1.0;final int nx=80,ny=16,nz=16;
            MeshModel surface=beamSurfaceMm();TetMeshData mesh=structuredBeamTet(nx,ny,nz,L,b,h);MeshQualityReport quality=MeshQualityReport.evaluate(mesh);if(!quality.pass)return new BenchmarkResult(false,"cantileverTheory FAIL meshQA: "+quality.summary());
            LinearElasticMaterial mat=new LinearElasticMaterial("Benchmark steel",E,0.30,7850,355e6);StaticFemSolver solver=new StaticFemSolver(mesh,mat);List<MeshModel.V3> sup=new ArrayList<>(),load=new ArrayList<>();for(MeshModel.V3 v:surface.vertices){if(Math.abs(v.x)<=1e-9)sup.add(v);if(Math.abs(v.x-100.0)<=1e-9)load.add(v);}AdvancedFemLoads.Result map=AdvancedFemLoads.apply(solver,mesh,surface,sup,load,0.001,0,0,-F,0,false,7850);StaticFemSolver.Result r=solver.solve();
            double I=b*h*h*h/12.0,theory=F*L*L*L/(3.0*E*I),err=Math.abs(r.maxDisplacementM-theory)/theory;boolean mapping=map.fixedNodes>=3&&map.loadedNodes>0;boolean numerical=r.linearSolve.converged&&r.forceEquilibriumRelativeError<1e-5&&Double.isFinite(r.maxDisplacementM)&&r.maxDisplacementM>0;boolean pass=mapping&&numerical&&err<=0.05;
            return new BenchmarkResult(pass,String.format(Locale.US,"cantileverTheory %s | U_FEM=%.6g mm | U_theory=%.6g mm | error=%.2f%% | grid=%dx%dx%d | tets=%d | fixed=%d loaded=%d | eqErr=%.3e | residual=%.3e | RAW gate<=5%%",pass?"PASS":"FAIL",r.maxDisplacementM*1000,theory*1000,err*100,nx,ny,nz,mesh.tets.size(),map.fixedNodes,map.loadedNodes,r.forceEquilibriumRelativeError,r.linearSolve.relativeResidual));
        }catch(Throwable t){return new BenchmarkResult(false,"cantileverTheory ERROR: "+t.getMessage());}
    }

    private static TetMeshData structuredBeamTet(int nx,int ny,int nz,double L,double b,double h){TetMeshData m=new TetMeshData();int[][][] id=new int[nx+1][ny+1][nz+1];for(int i=0;i<=nx;i++)for(int j=0;j<=ny;j++)for(int k=0;k<=nz;k++)id[i][j][k]=m.addNode(L*i/nx,-b/2.0+b*j/ny,-h/2.0+h*k/nz);final int[][] pattern={{0,1,3,7},{0,3,2,7},{0,2,6,7},{0,6,4,7},{0,4,5,7},{0,5,1,7}};for(int i=0;i<nx;i++)for(int j=0;j<ny;j++)for(int k=0;k<nz;k++){int[] c={id[i][j][k],id[i+1][j][k],id[i][j+1][k],id[i+1][j+1][k],id[i][j][k+1],id[i+1][j][k+1],id[i][j+1][k+1],id[i+1][j+1][k+1]};for(int[] p:pattern)addPositiveTet(m,c[p[0]],c[p[1]],c[p[2]],c[p[3]]);}m.validate();return m;}
    private static void addPositiveTet(TetMeshData m,int a,int b,int c,int d){MeshModel.V3 A=m.nodes.get(a),B=m.nodes.get(b),C=m.nodes.get(c),D=m.nodes.get(d);double v6=(B.x-A.x)*((C.y-A.y)*(D.z-A.z)-(C.z-A.z)*(D.y-A.y))-(B.y-A.y)*((C.x-A.x)*(D.z-A.z)-(C.z-A.z)*(D.x-A.x))+(B.z-A.z)*((C.x-A.x)*(D.y-A.y)-(C.y-A.y)*(D.x-A.x));if(Math.abs(v6)<=1e-18)throw new IllegalStateException("Benchmark mesher produced degenerate TET4");if(v6>0)m.addTet(a,b,c,d);else m.addTet(a,c,b,d);}
    private static MeshModel beamSurfaceMm(){MeshModel m=new MeshModel();double[][] v={{0,-10,-5},{0,10,-5},{0,10,5},{0,-10,5},{100,-10,-5},{100,10,-5},{100,10,5},{100,-10,5}};for(double[] p:v)m.addVertex(new MeshModel.V3(p[0],p[1],p[2]));int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};for(int[] t:f)m.triangles.add(t);return m;}
}
