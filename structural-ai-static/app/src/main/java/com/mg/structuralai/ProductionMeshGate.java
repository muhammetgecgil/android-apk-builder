package com.mg.structuralai;

import java.util.*;

/** Product-maturity mesh regression: feature sizing, thin-solid rejection, quality and mesh independence. */
public final class ProductionMeshGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private static volatile Result cached;
    private ProductionMeshGate(){}

    public static Result run(){
        Result c=cached;if(c!=null)return c;
        try{
            MeshModel beam=box(0,100,-10,10,-5,5);
            MeshFeatureSizingAdvisor.Result sizing=MeshFeatureSizingAdvisor.evaluate(beam,16);
            SmartTetMesher.Result coarse=SmartTetMesher.generate(beam,16,0.001);
            SmartTetMesher.Result medium=SmartTetMesher.generate(beam,24,0.001);
            SmartTetMesher.Result fine=SmartTetMesher.generate(beam,32,0.001);
            LinearElasticMaterial steel=new LinearElasticMaterial("MeshGateSteel",210e9,0.30,7850,355e6);
            StaticFemSolver.Result rc=solveBeam(beam,coarse.mesh,steel);
            StaticFemSolver.Result rm=solveBeam(beam,medium.mesh,steel);
            StaticFemSolver.Result rf=solveBeam(beam,fine.mesh,steel);
            double duCM=rel(rm.maxDisplacementM,rc.maxDisplacementM);
            double duMF=rel(rf.maxDisplacementM,rm.maxDisplacementM);
            boolean monotonicImprovement=duMF<duCM;
            boolean independence=duMF<=0.15&&monotonicImprovement;
            boolean quality=coarse.quality.pass&&medium.quality.pass&&fine.quality.pass&&coarse.quality.minMeanRatio>=0.12&&medium.quality.minMeanRatio>=0.12&&fine.quality.minMeanRatio>=0.12;
            boolean conformity=medium.conformity.maxDistanceM<=coarse.conformity.maxDistanceM*1.10&&fine.conformity.maxDistanceM<=medium.conformity.maxDistanceM*1.10;

            MeshModel thin=box(0,100,-25,25,-0.5,0.5);
            MeshFeatureSizingAdvisor.Result ts=MeshFeatureSizingAdvisor.evaluate(thin,16);
            boolean thinDetected=ts.thinLike&&ts.slenderness>=50;
            boolean thinSafelyBlocked=thinDetected&&ts.recommendedLongestAxisCells>=56;

            MeshModel corner=stepped();
            MeshFeatureSizingAdvisor.Result cs=MeshFeatureSizingAdvisor.evaluate(corner,16);
            boolean featureAware=cs.sharpEdges>0&&cs.recommendedLongestAxisCells>16;

            boolean pass=quality&&conformity&&independence&&thinSafelyBlocked&&featureAware;
            String txt=String.format(Locale.US,
                "PRODUCTION MESH GATE %s\nSUBGATES quality=%s conformity=%s independence=%s thinSafeBlock=%s featureAware=%s\nbeamSizing: %s\ncoarse16: %s | conformity max=%.6g mm\nmedium24: %s | conformity max=%.6g mm\nfine32: %s | conformity max=%.6g mm\nmeshIndependence ΔU16→24=%.2f%% | ΔU24→32=%.2f%% (final gate<=15%%, improving=%s)\nthinWallDetection: %s | detected=%s | safeSolidBlock=%s\nsharpFeatureSizing: %s | featureAware=%s",
                pass?"PASS":"FAIL",quality,conformity,independence,thinSafelyBlocked,featureAware,
                sizing.summary,coarse.quality.summary(),coarse.conformity.maxDistanceM*1000,
                medium.quality.summary(),medium.conformity.maxDistanceM*1000,fine.quality.summary(),fine.conformity.maxDistanceM*1000,
                duCM*100,duMF*100,monotonicImprovement,ts.summary,thinDetected,thinSafelyBlocked,cs.summary,featureAware);
            return cached=new Result(pass,txt);
        }catch(Throwable t){return cached=new Result(false,"PRODUCTION MESH GATE ERROR: "+t.getMessage());}
    }

    private static StaticFemSolver.Result solveBeam(MeshModel surface,TetMeshData mesh,LinearElasticMaterial mat){
        StaticFemSolver s=new StaticFemSolver(mesh,mat);List<MeshModel.V3> sup=new ArrayList<>(),load=new ArrayList<>();
        for(MeshModel.V3 v:surface.vertices){if(Math.abs(v.x)<=1e-9)sup.add(v);if(Math.abs(v.x-100)<=1e-9)load.add(v);}
        AdvancedFemLoads.apply(s,mesh,surface,sup,load,0.001,0,0,-1,0,false,7850);return s.solve();
    }
    private static double rel(double a,double b){double d=Math.max(Math.max(Math.abs(a),Math.abs(b)),1e-30);return Math.abs(a-b)/d;}
    private static MeshModel stepped(){MeshModel m=new MeshModel();appendBox(m,0,70,-10,10,-5,5);appendBox(m,70,100,-7,7,-4,4);return m;}
    private static MeshModel box(double x0,double x1,double y0,double y1,double z0,double z1){MeshModel m=new MeshModel();appendBox(m,x0,x1,y0,y1,z0,z1);return m;}
    private static void appendBox(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){int o=m.vertices.size();double[][] p={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};for(double[] q:p)m.addVertex(new MeshModel.V3(q[0],q[1],q[2]));int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};for(int[] t:f)m.triangles.add(new int[]{o+t[0],o+t[1],o+t[2]});}
}
