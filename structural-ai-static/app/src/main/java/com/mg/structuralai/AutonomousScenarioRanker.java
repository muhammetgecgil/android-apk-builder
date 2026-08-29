package com.mg.structuralai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Generates and ranks multiple plausible geometry-driven unit-load scenarios without claiming real service loads. */
public final class AutonomousScenarioRanker {
    public interface PreviewListener { void onMeshReady(String scenario,int cells,TetMeshData mesh,MeshQualityReport quality); }
    public static final class Scenario {
        public final String name;
        public final List<MeshModel.V3> supports;
        public final List<MeshModel.V3> loads;
        public final double fx,fy,fz;
        public final double priorScore;
        public double numericalScore;
        public double totalScore;
        public MeshConvergenceStudy.Result convergence;
        public String note;
        Scenario(String n,List<MeshModel.V3>s,List<MeshModel.V3>l,double x,double y,double z,double p){name=n;supports=s;loads=l;fx=x;fy=y;fz=z;priorScore=p;}
    }
    private AutonomousScenarioRanker(){}

    public static List<Scenario> generate(MeshModel m, AutonomousAnalysisPlanner.Plan base){
        List<Scenario> out=new ArrayList<>();
        out.add(new Scenario("Dominant-axis transverse",base.supports,base.loads,base.fx,base.fy,base.fz,0.75));
        double[][] dirs={{1,0,0},{0,1,0},{0,0,1}};
        for(double[] d:dirs){
            if(Math.abs(d[0]-Math.abs(base.fx))+Math.abs(d[1]-Math.abs(base.fy))+Math.abs(d[2]-Math.abs(base.fz))<0.5) continue;
            out.add(new Scenario("Alternate influence "+axisName(d),base.supports,base.loads,-d[0],-d[1],-d[2],0.45));
        }
        return out;
    }

    public static Scenario runAndRank(MeshModel surface, AutonomousAnalysisPlanner.Plan plan){return runAndRank(surface,plan,null);}

    public static Scenario runAndRank(MeshModel surface, AutonomousAnalysisPlanner.Plan plan,PreviewListener listener){
        List<Scenario> scenarios=generate(surface,plan);
        for(Scenario s:scenarios){
            try{
                MeshConvergenceStudy.MeshListener ml=listener==null?null:(cells,mesh,quality)->listener.onMeshReady(s.name,cells,mesh,quality);
                java.util.ArrayList<AdvancedFemLoads.SupportPatch> patches=new java.util.ArrayList<>();if(s.supports!=null)for(MeshModel.V3 p:s.supports)patches.add(new AdvancedFemLoads.SupportPatch(p,true,true,true));
                s.convergence=MeshConvergenceStudy.run(surface,plan.unitScaleM,plan.material,patches,s.loads,s.fx,s.fy,s.fz,0,false,plan.material.densityKgM3,ml);
                MeshConvergenceStudy.Level f=s.convergence.fine;
                double conv=s.convergence.converged?1.0:0.0;
                double eq=Math.max(0,1.0-Math.min(1.0,f.fem.forceEquilibriumRelativeError*1e5));
                double resid=f.fem.linearSolve.converged?1.0:0.0;
                s.numericalScore=0.50*conv+0.25*eq+0.25*resid;
                s.totalScore=0.60*s.priorScore+0.40*s.numericalScore;
                s.note="unit-load influence only; real load not inferred";
            }catch(Exception ex){s.numericalScore=0;s.totalScore=0;s.note="blocked: "+ex.getMessage();}
        }
        scenarios.sort(Comparator.comparingDouble((Scenario s)->s.totalScore).reversed());
        return scenarios.get(0);
    }

    private static String axisName(double[] d){return d[0]!=0?"X":d[1]!=0?"Y":"Z";}
}
