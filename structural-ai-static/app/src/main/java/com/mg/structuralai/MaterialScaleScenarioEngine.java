package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** Multi-scenario uncertainty engine for geometry-only models. */
public final class MaterialScaleScenarioEngine {
    public static final class Scenario {
        public final String name; public final double scaleM; public final LinearElasticMaterial material;
        public final MeshConvergenceStudy.Result convergence; public final double capacityN; public final boolean pass;
        Scenario(String n,double s,LinearElasticMaterial m,MeshConvergenceStudy.Result c,double cap,boolean p){name=n;scaleM=s;material=m;convergence=c;capacityN=cap;pass=p;}
    }
    public static final class Result {
        public final List<Scenario> scenarios; public final double minCapacityN,maxCapacityN; public final int passed;
        Result(List<Scenario> s,double min,double max,int p){scenarios=s;minCapacityN=min;maxCapacityN=max;passed=p;}
    }
    private MaterialScaleScenarioEngine(){}

    public static Result evaluate(MeshModel model,AutonomousAnalysisPlanner.Plan plan,double fx,double fy,double fz){
        List<LinearElasticMaterial> mats=new ArrayList<>();
        mats.add(new LinearElasticMaterial("Steel reference",210e9,0.30,7850,355e6));
        mats.add(new LinearElasticMaterial("Aluminium reference",70e9,0.33,2700,275e6));
        mats.add(new LinearElasticMaterial("Titanium reference",110e9,0.34,4430,828e6));
        double[] scales=plan.unitConfidence>=0.75?new double[]{plan.unitScaleM}:new double[]{plan.unitScaleM, plan.unitScaleM==0.001?1.0:0.001};
        List<Scenario> out=new ArrayList<>(); double min=Double.POSITIVE_INFINITY,max=0;int passed=0;
        for(double scale:scales){
            for(LinearElasticMaterial mat:mats){
                try{
                    MeshConvergenceStudy.Result cv=MeshConvergenceStudy.run(model,scale,mat,plan.supports,plan.loads,fx,fy,fz,0,false,mat.densityKgM3);
                    MeshConvergenceStudy.Level fine=cv.fine;
                    boolean ok=cv.converged&&fine.fem.linearSolve.converged&&fine.fem.forceEquilibriumRelativeError<1e-5&&fine.fem.maxVonMisesPa>0;
                    double cap=ok?mat.yieldPa/fine.fem.maxVonMisesPa:Double.NaN;
                    if(ok&&Double.isFinite(cap)){min=Math.min(min,cap);max=Math.max(max,cap);passed++;}
                    out.add(new Scenario(mat.name+" @ scale "+scale+" m/unit",scale,mat,cv,cap,ok));
                }catch(Exception ex){out.add(new Scenario(mat.name+" @ scale "+scale+" m/unit",scale,mat,null,Double.NaN,false));}
            }
        }
        if(passed==0){min=Double.NaN;max=Double.NaN;}
        return new Result(out,min,max,passed);
    }
}
