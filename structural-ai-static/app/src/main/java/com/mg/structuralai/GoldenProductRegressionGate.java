package com.mg.structuralai;

import java.util.Locale;

/** Product-level behavioral lock for representative geometries and safety decisions. */
public final class GoldenProductRegressionGate {
    public static final class Result {
        public final boolean pass;
        public final String summary;
        Result(boolean p,String s){pass=p;summary=s;}
    }
    private static volatile Result cached;
    private GoldenProductRegressionGate(){}

    public static Result run(){
        Result c=cached;if(c!=null)return c;
        try{
            CaseResult near=contactCase("nearTouch",0.005,false,ContactCandidateEngine.State.TOUCHING_OR_COINCIDENT);
            CaseResult finite=contactCase("finiteGap",0.50,false,ContactCandidateEngine.State.FINITE_GAP);
            CaseResult overlap=contactCase("overlap",0.20,true,ContactCandidateEngine.State.INTERFERENCE_SUSPECTED);
            CaseResult chain=threeBodyChain();
            CaseResult thin=thinWallCase();
            boolean pass=near.pass&&finite.pass&&overlap.pass&&chain.pass&&thin.pass;
            String summary="GOLDEN PRODUCT REGRESSION "+(pass?"PASS":"FAIL")+" | "+near.summary+" | "+finite.summary+" | "+overlap.summary+" | "+chain.summary+" | "+thin.summary;
            return cached=new Result(pass,summary);
        }catch(Throwable t){return cached=new Result(false,"GOLDEN PRODUCT REGRESSION ERROR: "+t.getMessage());}
    }

    private static CaseResult contactCase(String name,double gapOrOverlap,boolean overlap,ContactCandidateEngine.State expected){
        MeshModel m=new MeshModel();
        addBoxDuplicated(m,0,20,0,20,0,20);
        double x0=overlap?20-gapOrOverlap:20+gapOrOverlap;
        addBoxDuplicated(m,x0,x0+20,0,20,0,20);
        AssemblyBodyDecomposer.Result dec=AssemblyBodyDecomposer.decompose(m);
        ContactCandidateEngine.Result r=ContactCandidateEngine.analyze(m,dec);
        ContactCandidateEngine.State got=r.pairs.size()==1?r.pairs.get(0).state:null;
        boolean ok=dec.bodies.size()==2&&got==expected;
        return new CaseResult(ok,String.format(Locale.US,"%s=%s(expected=%s,bodies=%d)",name,got,expected,dec.bodies.size()));
    }

    private static CaseResult threeBodyChain(){
        MeshModel m=new MeshModel();
        addBoxDuplicated(m,0,15,0,10,0,10);
        addBoxDuplicated(m,15.01,30.01,0,10,0,10);
        addBoxDuplicated(m,30.03,45.03,0,10,0,10);
        AssemblyBodyDecomposer.Result dec=AssemblyBodyDecomposer.decompose(m);
        ContactCandidateEngine.Result r=ContactCandidateEngine.analyze(m,dec);
        boolean ok=dec.bodies.size()==3&&r.activeCandidates()==2&&r.pairs.size()==3;
        return new CaseResult(ok,"threeBodyChain=bodies"+dec.bodies.size()+"/active"+r.activeCandidates()+"/pairs"+r.pairs.size());
    }

    private static CaseResult thinWallCase(){
        MeshModel thin=new MeshModel();appendBox(thin,0,120,-30,30,-0.125,0.125);
        MeshFeatureSizingAdvisor.Result s=MeshFeatureSizingAdvisor.evaluate(thin,16);
        boolean ok=s.thinLike&&s.recommendedLongestAxisCells>16&&s.slenderness>=100;
        return new CaseResult(ok,String.format(Locale.US,"thinWall=thinLike%s/slenderness%.1f/recommended%d",s.thinLike,s.slenderness,s.recommendedLongestAxisCells));
    }

    private static final class CaseResult{final boolean pass;final String summary;CaseResult(boolean p,String s){pass=p;summary=s;}}

    /** STL-like fixture: every triangle owns its own vertex IDs. */
    private static void addBoxDuplicated(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){
        double[][] p={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
        for(int[] t:f){int o=m.vertices.size();m.addVertex(new MeshModel.V3(p[t[0]][0],p[t[0]][1],p[t[0]][2]));m.addVertex(new MeshModel.V3(p[t[1]][0],p[t[1]][1],p[t[1]][2]));m.addVertex(new MeshModel.V3(p[t[2]][0],p[t[2]][1],p[t[2]][2]));m.triangles.add(new int[]{o,o+1,o+2});}
    }
    private static void appendBox(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){
        int o=m.vertices.size();double[][] p={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};for(double[] q:p)m.addVertex(new MeshModel.V3(q[0],q[1],q[2]));int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};for(int[] t:f)m.triangles.add(new int[]{o+t[0],o+t[1],o+t[2]});
    }
}
