package com.mg.structuralai;

import java.util.*;

/** Guards against STL-style duplicated vertex IDs fragmenting one closed body into one-triangle components. */
public final class AssemblyBodySplitRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private static volatile Result cached;
    private AssemblyBodySplitRegressionGate(){}

    public static Result run(){
        Result c=cached;if(c!=null)return c;
        try{
            MeshModel m=new MeshModel();
            appendBoxDuplicated(m,0,20,0,20,0,20);
            appendBoxDuplicated(m,20.05,40.05,0,20,0,20);
            AssemblyBodyDecomposer.Result ui=AssemblyBodyDecomposer.decompose(m);
            AssemblyContactEngine.Result solver=AssemblyContactEngine.analyze(m);
            boolean uiOk=ui.bodies.size()==2&&ui.bodies.get(0).triangleIndices.size()==12&&ui.bodies.get(1).triangleIndices.size()==12;
            boolean solverOk=solver.components.size()==2&&solver.components.get(0).triangles.size()==12&&solver.components.get(1).triangles.size()==12;
            boolean consistent=uiOk&&solverOk;
            String s="ASSEMBLY BODY SPLIT REGRESSION "+(consistent?"PASS":"FAIL")+
                " | sourceTriangles="+m.triangles.size()+
                " | UI bodies="+ui.bodies.size()+" tris="+bodyCounts(ui)+
                " | solver bodies="+solver.components.size()+" tris="+componentCounts(solver)+
                " | duplicatedVertexFixture=true";
            return cached=new Result(consistent,s);
        }catch(Throwable t){return cached=new Result(false,"ASSEMBLY BODY SPLIT REGRESSION ERROR: "+t.getMessage());}
    }

    private static String bodyCounts(AssemblyBodyDecomposer.Result r){StringBuilder s=new StringBuilder("[");for(int i=0;i<r.bodies.size();i++){if(i>0)s.append(',');s.append(r.bodies.get(i).triangleIndices.size());}return s.append(']').toString();}
    private static String componentCounts(AssemblyContactEngine.Result r){StringBuilder s=new StringBuilder("[");for(int i=0;i<r.components.size();i++){if(i>0)s.append(',');s.append(r.components.get(i).triangles.size());}return s.append(']').toString();}

    private static void appendBoxDuplicated(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){
        double[][] p={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
        for(int[] t:f){int[] nt=new int[3];for(int k=0;k<3;k++){double[] q=p[t[k]];nt[k]=m.vertices.size();m.addVertex(new MeshModel.V3(q[0],q[1],q[2]));}m.triangles.add(nt);}
    }
}
