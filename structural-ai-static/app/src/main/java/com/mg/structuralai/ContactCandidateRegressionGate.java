package com.mg.structuralai;

import java.util.Locale;

/** Guards geometry-only contact classification against unsafe overlap mislabeling. */
public final class ContactCandidateRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private ContactCandidateRegressionGate(){}

    public static Result run(){
        try{
            ContactCandidateEngine.Pair touch=classify(20.005);
            ContactCandidateEngine.Pair gap=classify(20.5);
            ContactCandidateEngine.Pair overlap=classify(19.8);
            boolean pass=touch!=null&&gap!=null&&overlap!=null
                    &&touch.state==ContactCandidateEngine.State.TOUCHING_OR_COINCIDENT
                    &&gap.state==ContactCandidateEngine.State.FINITE_GAP
                    &&overlap.state==ContactCandidateEngine.State.INTERFERENCE_SUSPECTED
                    &&overlap.bboxPenetration>0.15;
            return new Result(pass,String.format(Locale.US,
                    "CONTACT CANDIDATE REGRESSION %s | touch=%s | gap=%s | overlap=%s penetration=%.6g",
                    pass?"PASS":"FAIL",
                    touch==null?"null":touch.state,
                    gap==null?"null":gap.state,
                    overlap==null?"null":overlap.state,
                    overlap==null?Double.NaN:overlap.bboxPenetration));
        }catch(Throwable t){return new Result(false,"CONTACT CANDIDATE REGRESSION ERROR: "+t.getMessage());}
    }

    private static ContactCandidateEngine.Pair classify(double x0){
        MeshModel m=new MeshModel();addBox(m,0,0,0,20,20,20);addBox(m,x0,0,0,x0+20,20,20);
        ContactCandidateEngine.Result r=ContactCandidateEngine.analyze(m,AssemblyBodyDecomposer.decompose(m));
        return r.pairs.isEmpty()?null:r.pairs.get(0);
    }

    private static void addBox(MeshModel m,double x0,double y0,double z0,double x1,double y1,double z1){
        double[][] v={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
        // Intentionally duplicate vertex records per triangle like common STL importers do.
        for(int[] t:f){int base=m.vertices.size();for(int k:t){double[] p=v[k];m.addVertex(new MeshModel.V3(p[0],p[1],p[2]));}m.triangles.add(new int[]{base,base+1,base+2});}
    }
}
