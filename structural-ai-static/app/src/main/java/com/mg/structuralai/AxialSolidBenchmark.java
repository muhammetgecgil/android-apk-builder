package com.mg.structuralai;

import java.util.Locale;

/** Independent 3D solid verification against the closed-form axial bar solution delta=F L/(A E). */
public final class AxialSolidBenchmark {
    public static final class Result {
        public final boolean pass;
        public final String message;
        Result(boolean p,String m){pass=p;message=m;}
    }
    private AxialSolidBenchmark(){}

    public static Result run(){
        try{
            final double L=0.100,W=0.010,H=0.010,E=210e9,F=1000.0;
            final int nx=20,ny=4,nz=4;
            TetMeshData mesh=structuredBox(nx,ny,nz,L,W,H);
            MeshQualityReport q=MeshQualityReport.evaluate(mesh);
            if(!q.pass)return new Result(false,"axialSolidTheory FAIL meshQA: "+q.summary());

            LinearElasticMaterial steel=new LinearElasticMaterial("Axial benchmark steel",E,0.30,7850,355e6);
            StaticFemSolver solver=new StaticFemSolver(mesh,steel);
            int fixed=0,loaded=0;
            double tol=L*1e-10;
            for(int i=0;i<mesh.nodes.size();i++){
                MeshModel.V3 p=mesh.nodes.get(i);
                if(Math.abs(p.x)<=tol){solver.fixNode(i);fixed++;}
                if(Math.abs(p.x-L)<=tol)loaded++;
            }
            if(fixed<3||loaded<1)return new Result(false,"axialSolidTheory FAIL boundary mapping fixed="+fixed+" loaded="+loaded);
            double nodal=F/loaded;
            for(int i=0;i<mesh.nodes.size();i++)if(Math.abs(mesh.nodes.get(i).x-L)<=tol)solver.addNodalForce(i,nodal,0,0);

            StaticFemSolver.Result r=solver.solve();
            double ux=0;int n=0;
            for(int i=0;i<mesh.nodes.size();i++)if(Math.abs(mesh.nodes.get(i).x-L)<=tol){ux+=r.displacement[3*i];n++;}
            ux/=Math.max(1,n);
            double theory=F*L/(W*H*E);
            double err=Math.abs(ux-theory)/theory;
            boolean numerical=r.linearSolve.converged&&r.linearSolve.relativeResidual<=1e-8&&r.forceEquilibriumRelativeError<=1e-6;
            boolean pass=numerical&&Double.isFinite(ux)&&ux>0&&err<=0.03;
            String msg=String.format(Locale.US,
                "axialSolidTheory %s | Ux_FEM=%.6g mm | U_theory=%.6g mm | error=%.2f%% | grid=%dx%dx%d | tets=%d | fixed=%d loaded=%d | eqErr=%.3e | residual=%.3e | gate<=3%%",
                pass?"PASS":"FAIL",ux*1000,theory*1000,err*100,nx,ny,nz,mesh.tets.size(),fixed,loaded,r.forceEquilibriumRelativeError,r.linearSolve.relativeResidual);
            return new Result(pass,msg);
        }catch(Throwable t){return new Result(false,"axialSolidTheory ERROR: "+t.getMessage());}
    }

    private static TetMeshData structuredBox(int nx,int ny,int nz,double L,double W,double H){
        TetMeshData m=new TetMeshData();
        int[][][] id=new int[nx+1][ny+1][nz+1];
        for(int i=0;i<=nx;i++)for(int j=0;j<=ny;j++)for(int k=0;k<=nz;k++){
            double x=L*i/nx,y=-W/2.0+W*j/ny,z=-H/2.0+H*k/nz;
            id[i][j][k]=m.addNode(x,y,z);
        }
        final int[][] pattern={{0,1,3,7},{0,3,2,7},{0,2,6,7},{0,6,4,7},{0,4,5,7},{0,5,1,7}};
        for(int i=0;i<nx;i++)for(int j=0;j<ny;j++)for(int k=0;k<nz;k++){
            int[] c={id[i][j][k],id[i+1][j][k],id[i][j+1][k],id[i+1][j+1][k],id[i][j][k+1],id[i+1][j][k+1],id[i][j+1][k+1],id[i+1][j+1][k+1]};
            for(int[] p:pattern)addPositiveTet(m,c[p[0]],c[p[1]],c[p[2]],c[p[3]]);
        }
        m.validate();return m;
    }

    private static void addPositiveTet(TetMeshData m,int a,int b,int c,int d){
        MeshModel.V3 A=m.nodes.get(a),B=m.nodes.get(b),C=m.nodes.get(c),D=m.nodes.get(d);
        double v6=(B.x-A.x)*((C.y-A.y)*(D.z-A.z)-(C.z-A.z)*(D.y-A.y))
            -(B.y-A.y)*((C.x-A.x)*(D.z-A.z)-(C.z-A.z)*(D.x-A.x))
            +(B.z-A.z)*((C.x-A.x)*(D.y-A.y)-(C.y-A.y)*(D.x-A.x));
        if(Math.abs(v6)<=1e-18)throw new IllegalStateException("Axial benchmark mesher produced degenerate TET4");
        if(v6>0)m.addTet(a,b,c,d);else m.addTet(a,c,b,d);
    }
}
