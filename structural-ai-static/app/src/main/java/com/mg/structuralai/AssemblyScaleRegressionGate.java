package com.mg.structuralai;

import java.util.Locale;

/**
 * Product V&V for STL-like multi-body assemblies.  Each triangle owns duplicated
 * vertices just like many real STL exporters, so this gate protects geometric
 * body welding/decomposition and contact-graph scaling rather than idealized
 * shared-index meshes.
 */
public final class AssemblyScaleRegressionGate {
    public static final class Result {
        public final boolean pass;
        public final String summary;
        Result(boolean p,String s){pass=p;summary=s;}
    }
    private AssemblyScaleRegressionGate(){}

    public static Result run(){
        try{
            CaseResult c3=chain(3,20.0,0.005);
            CaseResult c5=chain(5,15.0,0.010);
            CaseResult c6=sixBodyFixture();
            CaseResult interference=interferenceFixture();
            boolean pass=c3.ok&&c5.ok&&c6.ok&&interference.ok;
            return new Result(pass,String.format(Locale.US,
                    "ASSEMBLY SCALE REGRESSION %s | 3body=%s | 5body=%s | 6body=%s | interference=%s",
                    pass?"PASS":"FAIL",c3.summary,c5.summary,c6.summary,interference.summary));
        }catch(Throwable t){
            return new Result(false,"ASSEMBLY SCALE REGRESSION ERROR: "+t.getMessage());
        }
    }

    private static CaseResult chain(int bodies,double size,double gap){
        MeshModel m=new MeshModel();
        for(int i=0;i<bodies;i++){
            double x0=i*(size+gap);
            addBoxStlLike(m,x0,x0+size,0,size,0,size);
        }
        AssemblyBodyDecomposer.Result d=AssemblyBodyDecomposer.decompose(m);
        ContactCandidateEngine.Result c=ContactCandidateEngine.analyze(m,d);
        int expectedContacts=bodies-1;
        boolean tris=true;
        for(AssemblyBodyDecomposer.Body b:d.bodies)tris&=b.triangleIndices.size()==12;
        boolean ok=d.bodies.size()==bodies&&c.activeCandidates()==expectedContacts&&tris;
        return new CaseResult(ok,String.format(Locale.US,"bodies=%d/%d contacts=%d/%d tris12=%s",
                d.bodies.size(),bodies,c.activeCandidates(),expectedContacts,tris));
    }

    private static CaseResult sixBodyFixture(){
        MeshModel m=new MeshModel();
        addBoxStlLike(m,0,40,0,40,0,10);
        addBoxStlLike(m,5,15,5,15,10.005,25.005);
        addBoxStlLike(m,25,35,5,15,10.005,25.005);
        addBoxStlLike(m,5,15,25,35,10.005,25.005);
        addBoxStlLike(m,25,35,25,35,10.005,25.005);
        addBoxStlLike(m,7,33,7,33,25.010,35.010);
        AssemblyBodyDecomposer.Result d=AssemblyBodyDecomposer.decompose(m);
        ContactCandidateEngine.Result c=ContactCandidateEngine.analyze(m,d);
        int touching=0,interference=0;
        for(ContactCandidateEngine.Pair p:c.pairs){
            if(p.state==ContactCandidateEngine.State.TOUCHING_OR_COINCIDENT||p.state==ContactCandidateEngine.State.NEAR_CONTACT)touching++;
            if(p.state==ContactCandidateEngine.State.INTERFERENCE_SUSPECTED)interference++;
        }
        boolean ok=d.bodies.size()==6&&c.activeCandidates()==8&&touching==8&&interference==0;
        return new CaseResult(ok,String.format(Locale.US,"bodies=%d/6 contacts=%d/8 near=%d/8 interference=%d",
                d.bodies.size(),c.activeCandidates(),touching,interference));
    }

    private static CaseResult interferenceFixture(){
        MeshModel m=new MeshModel();
        addBoxStlLike(m,0,20,0,20,0,20);
        addBoxStlLike(m,19.8,39.8,0,20,0,20);
        addBoxStlLike(m,40.3,60.3,0,20,0,20);
        AssemblyBodyDecomposer.Result d=AssemblyBodyDecomposer.decompose(m);
        ContactCandidateEngine.Result c=ContactCandidateEngine.analyze(m,d);
        int suspected=0;
        double maxPen=0;
        for(ContactCandidateEngine.Pair p:c.pairs)if(p.state==ContactCandidateEngine.State.INTERFERENCE_SUSPECTED){suspected++;maxPen=Math.max(maxPen,p.bboxPenetration);}
        boolean ok=d.bodies.size()==3&&suspected>=1&&maxPen>0.15;
        return new CaseResult(ok,String.format(Locale.US,"bodies=%d/3 suspected=%d penetration=%.4f",
                d.bodies.size(),suspected,maxPen));
    }

    private static final class CaseResult{
        final boolean ok;final String summary;
        CaseResult(boolean o,String s){ok=o;summary=s;}
    }

    /** Adds a closed box with duplicated vertices per facet, emulating STL topology. */
    private static void addBoxStlLike(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){
        double[][] v={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{1,2,6},{1,6,5},{2,3,7},{2,7,6},{3,0,4},{3,4,7}};
        for(int[] t:f){
            int base=m.vertices.size();
            for(int k=0;k<3;k++){double[] p=v[t[k]];m.addVertex(new MeshModel.V3(p[0],p[1],p[2]));}
            m.triangles.add(new int[]{base,base+1,base+2});
        }
    }
}
