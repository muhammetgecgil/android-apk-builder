package com.mg.structuralai;

import java.util.*;

/** Regression for explicit force/moment/acceleration load mechanics. */
public final class PhysicalLoadRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private PhysicalLoadRegressionGate(){}

    public static Result run(){
        try{
            MeshModel surface=box();double scale=0.001;SmartTetMesher.Result mr=SmartTetMesher.generate(surface,8,scale);if(!mr.quality.pass)return new Result(false,"PHYSICAL LOAD REGRESSION FAIL: mesh QA");
            LinearElasticMaterial mat=new LinearElasticMaterial("reg",210e9,0.3,7800,300e6,450e6,"regression",LinearElasticMaterial.EvidenceLevel.STANDARD_TRACEABLE);
            List<AdvancedFemLoads.SupportPatch> supports=Collections.singletonList(new AdvancedFemLoads.SupportPatch(new MeshModel.V3(0,10,5),true,true,true));
            List<MeshModel.V3> loads=Collections.singletonList(new MeshModel.V3(100,10,5));
            StaticFemSolver solverM=new StaticFemSolver(mr.mesh,mat);PhysicalLoadDefinition moment=new PhysicalLoadDefinition(0,0,0,0,0,12.5,0,0,0,0);AdvancedFemLoads.Result rm=AdvancedFemLoads.apply(solverM,mr.mesh,surface,supports,loads,scale,moment,mat.densityKgM3);
            double mErr=Math.abs(rm.resultantMy-12.5)/12.5;boolean momentOk=Math.abs(rm.resultantFx)+Math.abs(rm.resultantFy)+Math.abs(rm.resultantFz)<1e-7&&mErr<1e-8;

            StaticFemSolver solverA=new StaticFemSolver(mr.mesh,mat);PhysicalLoadDefinition accel=new PhysicalLoadDefinition(0,0,0,0,0,0,0,2.0,-3.0,4.0);AdvancedFemLoads.Result ra=AdvancedFemLoads.apply(solverA,mr.mesh,surface,supports,Collections.emptyList(),scale,accel,mat.densityKgM3);double ex=ra.gravityMassKg*2.0,ey=ra.gravityMassKg*-3.0,ez=ra.gravityMassKg*4.0;double den=Math.max(Math.abs(ex)+Math.abs(ey)+Math.abs(ez),1e-30);double aErr=(Math.abs(ra.resultantFx-ex)+Math.abs(ra.resultantFy-ey)+Math.abs(ra.resultantFz-ez))/den;boolean accelOk=ra.gravityMassKg>0&&aErr<1e-12;
            boolean gateBlock=!EngineeringLoadDefinitionGate.evaluate(new PhysicalLoadDefinition(0,0,0,0,0,0,0,0,0,0),1,1,mat).pass;
            boolean pass=momentOk&&accelOk&&gateBlock;return new Result(pass,"PHYSICAL LOAD REGRESSION "+(pass?"PASS":"FAIL")+" | momentRelErr="+mErr+" | accelForceRelErr="+aErr+" | zeroLoadBlocked="+gateBlock);
        }catch(Throwable t){return new Result(false,"PHYSICAL LOAD REGRESSION ERROR: "+t.getMessage());}
    }

    private static MeshModel box(){MeshModel m=new MeshModel();double[][] v={{0,0,0},{0,20,0},{0,20,10},{0,0,10},{100,0,0},{100,20,0},{100,20,10},{100,0,10}};for(double[] p:v)m.addVertex(new MeshModel.V3(p[0],p[1],p[2]));int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};for(int[] t:f)m.triangles.add(t);return m;}
}
