package com.mg.structuralai;

import java.util.*;

/** Stress-driven conforming local refinement study using centroid-split TET4 children. */
public final class LocalAdaptiveRefinementStudy {
    public static final class Step {
        public final int cycle,nodes,tets,refinedParents;
        public final TetMeshData mesh; public final StaticFemSolver.Result fem; public final AdvancedFemLoads.Result setup; public final MeshQualityReport quality; public final MeshModel.V3 hotspot;
        Step(int c,TetMeshData m,StaticFemSolver.Result f,AdvancedFemLoads.Result s,MeshQualityReport q,int rp){cycle=c;mesh=m;fem=f;setup=s;quality=q;nodes=m.nodes.size();tets=m.tets.size();refinedParents=rp;hotspot=hotspot(m,f);}
    }
    public static final class Result {
        public final List<Step> steps; public final boolean converged; public final double displacementChange,stressChange,hotspotShiftM; public final Step finalStep;
        public final boolean meshQaBlocked;
        Result(List<Step> s,boolean ok,double du,double ds,double hs,boolean blocked){steps=s;converged=ok;displacementChange=du;stressChange=ds;hotspotShiftM=hs;finalStep=s.get(s.size()-1);meshQaBlocked=blocked;}
    }
    private LocalAdaptiveRefinementStudy(){}

    public static Result run(MeshModel surface,double scale,LinearElasticMaterial mat,List<MeshModel.V3> supports,List<MeshModel.V3> loads,
                             double fx,double fy,double fz,double pressurePa,boolean gravity,double rho,int baseCells){
        SmartTetMesher.Result base=SmartTetMesher.generate(surface,baseCells,scale);
        List<Step> steps=new ArrayList<>();
        Step cur=solve(0,base.mesh,base.quality,surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,0);
        steps.add(cur);
        boolean ok=false,blocked=false;double du=Double.POSITIVE_INFINITY,ds=Double.POSITIVE_INFINITY,shift=Double.POSITIVE_INFINITY;
        for(int cycle=1;cycle<=3;cycle++){
            Set<Integer> pick=LocalTetRefiner.selectHighStress(cur.mesh,cur.fem,cycle==1?0.12:0.08,1);
            LocalTetRefiner.Result rr;
            try{ rr=LocalTetRefiner.refine(cur.mesh,pick); }
            catch(RuntimeException ex){ blocked=true; break; }
            if(!rr.quality.pass){ blocked=true; break; }
            Step next=solve(cycle,rr.mesh,rr.quality,surface,scale,mat,supports,loads,fx,fy,fz,pressurePa,gravity,rho,rr.refinedParents);
            steps.add(next);
            du=rel(next.fem.maxDisplacementM,cur.fem.maxDisplacementM);
            ds=rel(next.fem.maxVonMisesPa,cur.fem.maxVonMisesPa);
            shift=dist(next.hotspot,cur.hotspot);
            double diag=diag(next.mesh);
            boolean numerical=next.fem.linearSolve.converged&&next.fem.forceEquilibriumRelativeError<1e-5&&next.fem.maxDisplacementM>1e-15&&next.fem.maxVonMisesPa>1e-6;
            boolean stable=du<=0.05&&ds<=0.10&&shift<=Math.max(diag*0.06,1e-9);
            if(next.quality.pass&&numerical&&stable){ok=true;cur=next;break;}
            cur=next;
        }
        return new Result(steps,ok,du,ds,shift,blocked);
    }

    private static Step solve(int cycle,TetMeshData mesh,MeshQualityReport q,MeshModel surface,double scale,LinearElasticMaterial mat,List<MeshModel.V3> supports,List<MeshModel.V3> loads,
                              double fx,double fy,double fz,double pressurePa,boolean gravity,double rho,int refinedParents){
        if(!q.pass)throw new IllegalStateException("Local adaptive mesh QA failed: "+q.summary());
        StaticFemSolver solver=new StaticFemSolver(mesh,mat);
        AdvancedFemLoads.Result setup=AdvancedFemLoads.apply(solver,mesh,surface,supports,loads,scale,fx,fy,fz,pressurePa,gravity,rho);
        StaticFemSolver.Result fem=solver.solve();
        return new Step(cycle,mesh,fem,setup,q,refinedParents);
    }
    private static MeshModel.V3 hotspot(TetMeshData m,StaticFemSolver.Result f){int bi=0;for(int i=1;i<f.elementVonMisesPa.length;i++)if(f.elementVonMisesPa[i]>f.elementVonMisesPa[bi])bi=i;int[] t=m.tets.get(bi);double x=0,y=0,z=0;for(int n:t){MeshModel.V3 p=m.nodes.get(n);x+=p.x;y+=p.y;z+=p.z;}return new MeshModel.V3(x/4,y/4,z/4);}
    private static double diag(TetMeshData m){double x0=1e99,y0=1e99,z0=1e99,x1=-1e99,y1=-1e99,z1=-1e99;for(MeshModel.V3 p:m.nodes){x0=Math.min(x0,p.x);y0=Math.min(y0,p.y);z0=Math.min(z0,p.z);x1=Math.max(x1,p.x);y1=Math.max(y1,p.y);z1=Math.max(z1,p.z);}return Math.sqrt(sq(x1-x0)+sq(y1-y0)+sq(z1-z0));}
    private static double rel(double a,double b){return Math.abs(a-b)/Math.max(Math.max(Math.abs(a),Math.abs(b)),1e-30);}
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){return Math.sqrt(sq(a.x-b.x)+sq(a.y-b.y)+sq(a.z-b.z));}
    private static double sq(double x){return x*x;}
}
