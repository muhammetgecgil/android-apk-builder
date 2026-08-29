package com.mg.structuralai;

/** Regression-locks preview availability and geometry blocking semantics. */
public final class PreSolvePreviewRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private PreSolvePreviewRegressionGate(){}

    public static Result run(){
        try{
            MeshModel closed=box();
            PreSolveMeshPreviewEngine.Result ok=PreSolveMeshPreviewEngine.build(closed);
            MeshModel open=box();
            if(!open.triangles.isEmpty())open.triangles.remove(open.triangles.size()-1);
            PreSolveMeshPreviewEngine.Result bad=PreSolveMeshPreviewEngine.build(open);
            boolean pass=ok.available&&ok.mesh!=null&&!ok.mesh.tets.isEmpty()&&!bad.available;
            return new Result(pass,"PRE-SOLVE PREVIEW REGRESSION "+(pass?"PASS":"FAIL")+
                    " | closed="+ok.available+" | tets="+(ok.mesh==null?0:ok.mesh.tets.size())+
                    " | openBlocked="+(!bad.available));
        }catch(Throwable t){return new Result(false,"PRE-SOLVE PREVIEW REGRESSION ERROR: "+t.getMessage());}
    }

    private static MeshModel box(){
        MeshModel m=new MeshModel();double[][] v={{0,0,0},{20,0,0},{20,10,0},{0,10,0},{0,0,8},{20,0,8},{20,10,8},{0,10,8}};
        for(double[] a:v)m.addVertex(new MeshModel.V3(a[0],a[1],a[2]));
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{1,2,6},{1,6,5},{2,3,7},{2,7,6},{3,0,4},{3,4,7}};
        for(int[] t:f)m.triangles.add(t);return m;
    }
}
