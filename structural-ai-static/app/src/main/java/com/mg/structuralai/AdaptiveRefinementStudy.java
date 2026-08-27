package com.mg.structuralai;

import java.util.*;

/**
 * Autonomous refinement orchestrator.
 * Local refinement is opportunistic only: any local-mesh QA failure is rejected and rolled back.
 * The analysis then continues on the last known-good SmartTetMesher mesh using global escalation.
 */
public final class AdaptiveRefinementStudy {
    public static final class Step {
        public final int cells,nodes,tets;
        public final TetMeshData mesh;
        public final StaticFemSolver.Result fem;
        public final AdvancedFemLoads.Result setup;
        public final MeshQualityReport quality;
        public final MeshModel.V3 hotspot;
        public final String meshMode;
        Step(int c,TetMeshData m,StaticFemSolver.Result f,AdvancedFemLoads.Result s,MeshQualityReport q,MeshModel.V3 h,String mode){
            cells=c;mesh=m;nodes=m.nodes.size();tets=m.tets.size();quality=q;fem=f;setup=s;hotspot=h;meshMode=mode;
        }
    }
    public static final class Result {
        public final List<Step> steps;
        public final boolean converged;
        public final double displacementChange,stressChange,hotspotShiftM;
        public final Step finalStep;
        public final boolean localAttempted,localConverged,globalFallbackUsed;
        public final int localCycles;
        public final String localDecision;
        Result(List<Step> s,boolean ok,double du,double ds,double hs,boolean la,boolean lc,boolean gf,int cycles,String decision){
            steps=s;converged=ok;finalStep=s.get(s.size()-1);displacementChange=du;stressChange=ds;hotspotShiftM=hs;
            localAttempted=la;localConverged=lc;globalFallbackUsed=gf;localCycles=cycles;localDecision=decision;
        }
    }
    private AdaptiveRefinementStudy(){}

    public static Result run(MeshModel surface,double scale,LinearElasticMaterial mat,List<MeshModel.V3> supports,List<MeshModel.V3> loads,
                             double fx,double fy,double fz,double pressurePa,boolean gravity,double rho){
        List<Step> out=new ArrayList<>();
        boolean localAttempted=true, localConverged=false;
        int localCycles=0;
        String localDecision="LOCAL refinement attempted.";
        double du=Double.POSITIVE_INFINITY,ds=Double.POSITIVE_INFINITY,shift=Double.POSITIVE_INFINITY;

        // Local refinement is never allowed to take the whole autonomous run down. Any exception or
        // mesh-QA rejection means rollback to a freshly generated, known-good SmartTetMesher base.
        try{
            LocalAdaptiveRefinementStudy.Result local=LocalAdaptiveRefinementStudy.run(surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,16);
            for(LocalAdaptiveRefinementStudy.Step s:local.steps){
                out.add(new Step(16,s.mesh,s.fem,s.setup,s.quality,s.hotspot,"LOCAL cycle="+s.cycle+" refinedParents="+s.refinedParents));
            }
            localCycles=Math.max(0,local.steps.size()-1);
            du=local.displacementChange;ds=local.stressChange;shift=local.hotspotShiftM;
            if(local.converged){
                localConverged=true;
                localDecision="LOCAL refinement accepted: convergence and mesh-QA gates passed.";
                return new Result(out,true,du,ds,shift,true,true,false,localCycles,localDecision);
            }
            localDecision=local.meshQaBlocked?
                "LOCAL refinement REJECTED by mesh QA; rolled back automatically to global SmartTetMesher fallback.":
                "LOCAL refinement did not converge; rolled back automatically to global SmartTetMesher fallback.";
        }catch(RuntimeException ex){
            localDecision="LOCAL refinement REJECTED safely ("+ex.getMessage()+"); global SmartTetMesher fallback engaged.";
        }

        // If the rejected local path produced no usable step, establish a clean 16-cell SmartTet base.
        if(out.isEmpty()) out.add(solveGlobal(surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,16));

        // Global escalation always starts from a known-good accepted mesh, never from a rejected local mesh.
        Step prev=out.get(out.size()-1);
        if(!prev.quality.pass){
            out.clear();
            prev=solveGlobal(surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,16);
            out.add(prev);
        }

        int[] levels={20,24,28,32};
        boolean ok=false;
        for(int c:levels){
            Step cur=solveGlobal(surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,c);
            out.add(cur);
            du=rel(cur.fem.maxDisplacementM,prev.fem.maxDisplacementM);
            ds=rel(cur.fem.maxVonMisesPa,prev.fem.maxVonMisesPa);
            shift=dist(cur.hotspot,prev.hotspot);
            double diag=meshDiag(cur.mesh);
            boolean response=cur.fem.maxDisplacementM>1e-15&&cur.fem.maxVonMisesPa>1e-6;
            boolean qa=cur.quality.pass&&prev.quality.pass&&cur.fem.linearSolve.converged&&prev.fem.linearSolve.converged&&cur.fem.forceEquilibriumRelativeError<1e-5;
            boolean stableLocation=shift<=Math.max(diag*0.08,1e-9);
            if(response&&qa&&du<=0.05&&ds<=0.10&&stableLocation){ok=true;break;}
            prev=cur;
        }
        return new Result(out,ok,du,ds,shift,localAttempted,localConverged,true,localCycles,localDecision);
    }

    private static Step solveGlobal(MeshModel surface,double scale,LinearElasticMaterial mat,List<MeshModel.V3> supports,List<MeshModel.V3> loads,
                                    double fx,double fy,double fz,double pressurePa,boolean gravity,double rho,int cells){
        SmartTetMesher.Result mr=SmartTetMesher.generate(surface,cells,scale);
        if(!mr.quality.pass)throw new IllegalStateException("Adaptive mesh QA failed @"+cells+": "+mr.quality.summary());
        StaticFemSolver solver=new StaticFemSolver(mr.mesh,mat);
        AdvancedFemLoads.Result setup=AdvancedFemLoads.apply(solver,mr.mesh,surface,supports,loads,scale,fx,fy,fz,pressurePa,gravity,rho);
        StaticFemSolver.Result fem=solver.solve();
        String mode="GLOBAL_SMART "+(mr.snapped?"BOUNDARY_SNAP":"VOXEL_FALLBACK")+" | "+mr.decision;
        return new Step(cells,mr.mesh,fem,setup,mr.quality,hotspot(mr.mesh,fem),mode);
    }

    private static MeshModel.V3 hotspot(TetMeshData m,StaticFemSolver.Result f){
        if(f.elementVonMisesPa.length==0)return new MeshModel.V3(0,0,0);
        int bi=0;for(int i=1;i<f.elementVonMisesPa.length;i++)if(f.elementVonMisesPa[i]>f.elementVonMisesPa[bi])bi=i;
        int[] t=m.tets.get(bi);double x=0,y=0,z=0;for(int n:t){MeshModel.V3 p=m.nodes.get(n);x+=p.x;y+=p.y;z+=p.z;}return new MeshModel.V3(x/4,y/4,z/4);
    }
    private static double meshDiag(TetMeshData m){double x0=1e99,y0=1e99,z0=1e99,x1=-1e99,y1=-1e99,z1=-1e99;for(MeshModel.V3 p:m.nodes){x0=Math.min(x0,p.x);y0=Math.min(y0,p.y);z0=Math.min(z0,p.z);x1=Math.max(x1,p.x);y1=Math.max(y1,p.y);z1=Math.max(z1,p.z);}return Math.sqrt(sq(x1-x0)+sq(y1-y0)+sq(z1-z0));}
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){return Math.sqrt(sq(a.x-b.x)+sq(a.y-b.y)+sq(a.z-b.z));}
    private static double rel(double a,double b){double d=Math.max(Math.max(Math.abs(a),Math.abs(b)),1e-30);return Math.abs(a-b)/d;}
    private static double sq(double x){return x*x;}
}
