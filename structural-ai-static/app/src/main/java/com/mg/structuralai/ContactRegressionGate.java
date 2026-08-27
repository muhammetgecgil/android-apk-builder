package com.mg.structuralai;

/** End-to-end synthetic regression for bonded-contact recognition, meshing, tying and load transfer. */
public final class ContactRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private ContactRegressionGate(){}

    public static Result run(){
        try{
            MeshModel touching=twoBlocks(0.0);
            AssemblyContactEngine.Result a=AssemblyContactEngine.analyze(touching);
            if(a.components.size()!=2)return new Result(false,"CONTACT REGRESSION FAIL: components="+a.components.size());
            boolean bonded=false;for(AssemblyContactEngine.Pair p:a.pairs)if(p.type==AssemblyContactEngine.Type.BONDED_CANDIDATE)bonded=true;
            if(!bonded)return new Result(false,"CONTACT REGRESSION FAIL: no bonded candidate | "+a.summary(0.001));

            AssemblyTetContactBuilder.Result asm=AssemblyTetContactBuilder.build(touching,8,0.001);
            if(asm.constraints.bondedCount()<3)return new Result(false,"CONTACT REGRESSION FAIL: tiedNodePairs="+asm.constraints.bondedCount()+" | "+asm.summary);

            LinearElasticMaterial mat=new LinearElasticMaterial("ContactRegressionAl",70e9,0.33,2700.0,250e6);
            StaticFemSolver s=new StaticFemSolver(asm.mesh,mat);s.addContactConstraints(asm.constraints);
            double xmin=Double.POSITIVE_INFINITY,xmax=Double.NEGATIVE_INFINITY;
            for(MeshModel.V3 p:asm.mesh.nodes){xmin=Math.min(xmin,p.x);xmax=Math.max(xmax,p.x);}double tol=Math.max((xmax-xmin)*0.04,1e-8);
            int fixed=0,loaded=0;
            for(int i=0;i<asm.mesh.nodes.size();i++){MeshModel.V3 p=asm.mesh.nodes.get(i);if(p.x<=xmin+tol){s.fixNode(i);fixed++;}if(p.x>=xmax-tol){s.addNodalForce(i,0,0,-1.0);loaded++;}}
            if(fixed<3||loaded<1)return new Result(false,"CONTACT REGRESSION FAIL: support/load mapping fixed="+fixed+" loaded="+loaded);
            StaticFemSolver.Result f=s.solve();
            boolean numerical=f.linearSolve.converged&&Double.isFinite(f.maxDisplacementM)&&f.maxDisplacementM>0&&f.forceEquilibriumRelativeError<1e-4;

            MeshModel separated=twoBlocks(20.0);AssemblyContactEngine.Result sep=AssemblyContactEngine.analyze(separated);
            boolean separatedOk=sep.pairs.size()==1&&sep.pairs.get(0).type==AssemblyContactEngine.Type.SEPARATED;
            boolean ok=numerical&&separatedOk;
            String txt="CONTACT REGRESSION "+(ok?"PASS":"FAIL")+" | bodies=2 | bondedCandidate="+bonded+" | ties="+asm.constraints.bondedCount()+" | fixed="+fixed+" | loaded="+loaded+" | U="+f.maxDisplacementM+" m | eqErr="+f.forceEquilibriumRelativeError+" | separatedCheck="+separatedOk;
            return new Result(ok,txt);
        }catch(Exception e){return new Result(false,"CONTACT REGRESSION EXCEPTION: "+e.getMessage());}
    }

    private static MeshModel twoBlocks(double gap){MeshModel m=new MeshModel();addBox(m,0,10,0,10,0,10);addBox(m,10+gap,20+gap,0,10,0,10);return m;}
    private static void addBox(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){
        int o=m.vertices.size();double[][] p={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        for(double[] q:p)m.addVertex(new MeshModel.V3(q[0],q[1],q[2]));
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
        for(int[] t:f)m.triangles.add(new int[]{o+t[0],o+t[1],o+t[2]});
    }
}
