package com.mg.structuralai;

/** Deterministic Coulomb contact regression: low tangential load must stick; high tangential load must slip. */
public final class FrictionalContactRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private static volatile Result cached;
    private FrictionalContactRegressionGate(){}

    public static Result run(){
        Result c=cached;if(c!=null)return c;
        try{
            StaticFemSolver.Result low=solve(1.0);
            StaticFemSolver.Result high=solve(5000.0);
            boolean lowOk=low.linearSolve.converged&&low.activeFrictionalContacts==1&&low.stickingFrictionalContacts==1&&low.slidingFrictionalContacts==0;
            boolean highOk=high.linearSolve.converged&&high.activeFrictionalContacts==1&&high.slidingFrictionalContacts==1;
            boolean pass=lowOk&&highOk;
            String s="FRICTIONAL CONTACT REGRESSION "+(pass?"PASS":"FAIL")+
                " | low[active="+low.activeFrictionalContacts+",stick="+low.stickingFrictionalContacts+",slip="+low.slidingFrictionalContacts+",it="+low.contactIterations+"]"+
                " | high[active="+high.activeFrictionalContacts+",stick="+high.stickingFrictionalContacts+",slip="+high.slidingFrictionalContacts+",it="+high.contactIterations+"]";
            return cached=new Result(pass,s);
        }catch(Throwable t){return cached=new Result(false,"FRICTIONAL CONTACT REGRESSION ERROR: "+t.getMessage());}
    }

    private static StaticFemSolver.Result solve(double tangentialN){
        TetMeshData m=new TetMeshData();
        m.addNode(0,0,0);m.addNode(1,0,0);m.addNode(0,1,0);m.addNode(0,0,1);m.addTet(0,1,2,3);
        LinearElasticMaterial mat=new LinearElasticMaterial("Friction regression",70e9,0.30,2700,250e6);
        StaticFemSolver s=new StaticFemSolver(m,mat);
        s.fixNode(0);s.fixNode(2);s.fixNode(3);
        s.addFrictionalContact(0,1,new MeshModel.V3(1,0,0),5e3,2e3,0.0,0.30);
        s.addNodalForce(1,-1000.0,tangentialN,0.0);
        return s.solve();
    }
}
