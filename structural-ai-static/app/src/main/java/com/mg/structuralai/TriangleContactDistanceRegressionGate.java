package com.mg.structuralai;

import java.util.Locale;

/**
 * Golden regression for the production contact-distance classifier.
 * Locks the exact failure found on the six-body phone V&V fixture: a small
 * positive separation must never be promoted to INTERFERENCE merely because
 * triangle centroids are far apart. True volumetric overlap remains blocked.
 */
public final class TriangleContactDistanceRegressionGate {
    public static final class Result {
        public final boolean pass;
        public final String summary;
        Result(boolean p,String s){pass=p;summary=s;}
    }
    private TriangleContactDistanceRegressionGate(){}

    public static Result run(){
        try{
            CaseResult six=sixBodyNearGap();
            CaseResult overlap=trueOverlap();
            CaseResult separated=twoBodyNearGap();
            boolean pass=six.pass&&overlap.pass&&separated.pass;
            return new Result(pass,"TRIANGLE CONTACT DISTANCE REGRESSION "+(pass?"PASS":"FAIL")+
                    " | sixBody="+six.summary+" | nearGap="+separated.summary+" | overlap="+overlap.summary);
        }catch(Throwable t){
            return new Result(false,"TRIANGLE CONTACT DISTANCE REGRESSION ERROR: "+t.getMessage());
        }
    }

    private static CaseResult sixBodyNearGap(){
        MeshModel m=new MeshModel();
        addBox(m,0,40,0,40,0,10);
        addBox(m,5,15,5,15,10.005,25.005);
        addBox(m,25,35,5,15,10.005,25.005);
        addBox(m,5,15,25,35,10.005,25.005);
        addBox(m,25,35,25,35,10.005,25.005);
        addBox(m,7,33,7,33,25.010,35.010);
        AssemblyBodyDecomposer.Result d=AssemblyBodyDecomposer.decompose(m);
        ContactCandidateEngine.Result c=ContactCandidateEngine.analyze(m,d);
        int active=0,interference=0,transferLike=0;
        double min=Double.POSITIVE_INFINITY,max=0;
        for(ContactCandidateEngine.Pair p:c.pairs){
            if(p.state==ContactCandidateEngine.State.FAR)continue;
            active++;
            min=Math.min(min,p.sampledGap);max=Math.max(max,p.sampledGap);
            if(p.state==ContactCandidateEngine.State.INTERFERENCE_SUSPECTED)interference++;
            if(p.state==ContactCandidateEngine.State.TOUCHING_OR_COINCIDENT||p.state==ContactCandidateEngine.State.NEAR_CONTACT)transferLike++;
        }
        boolean distanceSane=Double.isFinite(min)&&min>=0&&min<0.02&&max<0.02;
        boolean ok=d.bodies.size()==6&&active==8&&interference==0&&transferLike==8&&distanceSane;
        return new CaseResult(ok,String.format(Locale.US,
                "b=%d active=%d/8 transfer=%d/8 interference=%d gapRange=[%.6g,%.6g]",
                d.bodies.size(),active,transferLike,interference,min,max));
    }

    private static CaseResult twoBodyNearGap(){
        MeshModel m=new MeshModel();
        addBox(m,0,20,0,20,0,20);
        addBox(m,20.005,40.005,0,20,0,20);
        AssemblyBodyDecomposer.Result d=AssemblyBodyDecomposer.decompose(m);
        ContactCandidateEngine.Result c=ContactCandidateEngine.analyze(m,d);
        ContactCandidateEngine.Pair p=c.pairs.get(0);
        boolean stateOk=p.state==ContactCandidateEngine.State.TOUCHING_OR_COINCIDENT||p.state==ContactCandidateEngine.State.NEAR_CONTACT;
        boolean gapOk=Math.abs(p.sampledGap-0.005)<1e-6;
        return new CaseResult(stateOk&&gapOk&&!isInterference(p),String.format(Locale.US,
                "%s gap=%.9f bboxPen=%.6g",p.state,p.sampledGap,p.bboxPenetration));
    }

    private static CaseResult trueOverlap(){
        MeshModel m=new MeshModel();
        addBox(m,0,20,0,20,0,20);
        addBox(m,19.8,39.8,0,20,0,20);
        AssemblyBodyDecomposer.Result d=AssemblyBodyDecomposer.decompose(m);
        ContactCandidateEngine.Result c=ContactCandidateEngine.analyze(m,d);
        int interference=0;double penetration=0;
        for(ContactCandidateEngine.Pair p:c.pairs)if(isInterference(p)){interference++;penetration=Math.max(penetration,p.bboxPenetration);}
        boolean ok=interference==1&&penetration>0.15;
        return new CaseResult(ok,String.format(Locale.US,"interference=%d penetration=%.6g",interference,penetration));
    }

    private static boolean isInterference(ContactCandidateEngine.Pair p){return p.state==ContactCandidateEngine.State.INTERFERENCE_SUSPECTED;}
    private static final class CaseResult{final boolean pass;final String summary;CaseResult(boolean p,String s){pass=p;summary=s;}}

    /** Closed box with duplicated vertices per triangle to emulate common STL exporters. */
    private static void addBox(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){
        double[][] v={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{1,2,6},{1,6,5},{2,3,7},{2,7,6},{3,0,4},{3,4,7}};
        for(int[] t:f){
            int base=m.vertices.size();
            for(int k=0;k<3;k++){double[] q=v[t[k]];m.addVertex(new MeshModel.V3(q[0],q[1],q[2]));}
            m.triangles.add(new int[]{base,base+1,base+2});
        }
    }
}
