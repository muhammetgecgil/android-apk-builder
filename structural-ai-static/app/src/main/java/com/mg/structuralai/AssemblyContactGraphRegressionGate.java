package com.mg.structuralai;

import java.util.Locale;

/** Regression-locks assembly connectivity, isolated-body and interference blocking behavior. */
public final class AssemblyContactGraphRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private AssemblyContactGraphRegressionGate(){}

    public static Result run(){
        try{
            Case chain3=chain(3,20,0.005,true,1,0,2);
            Case chain5=chain(5,15,0.010,true,1,0,4);
            Case disconnected=disconnectedFixture();
            Case isolated=isolatedFixture();
            Case interference=interferenceFixture();
            boolean pass=chain3.pass&&chain5.pass&&disconnected.pass&&isolated.pass&&interference.pass;
            return new Result(pass,"ASSEMBLY CONTACT GRAPH REGRESSION "+(pass?"PASS":"FAIL")+
                    " | chain3="+chain3.summary+" | chain5="+chain5.summary+
                    " | disconnected="+disconnected.summary+" | isolated="+isolated.summary+
                    " | interference="+interference.summary);
        }catch(Throwable t){return new Result(false,"ASSEMBLY CONTACT GRAPH REGRESSION ERROR: "+t.getMessage());}
    }

    private static Case chain(int count,double size,double gap,boolean ready,int components,int isolated,int edges){
        MeshModel m=new MeshModel();for(int i=0;i<count;i++)addBox(m,i*(size+gap),i*(size+gap)+size,0,size,0,size);
        AssemblyContactGraph.Result g=graph(m);
        boolean ok=g.bodies==count&&g.transferEdges==edges&&g.connectedComponents==components&&g.isolatedBodies==isolated&&g.assemblyReady==ready;
        return c(ok,g);
    }
    private static Case disconnectedFixture(){
        MeshModel m=new MeshModel();
        addBox(m,0,20,0,20,0,20);addBox(m,20.005,40.005,0,20,0,20);
        addBox(m,100,120,0,20,0,20);addBox(m,120.005,140.005,0,20,0,20);
        AssemblyContactGraph.Result g=graph(m);
        return c(g.bodies==4&&g.transferEdges==2&&g.connectedComponents==2&&g.isolatedBodies==0&&!g.assemblyReady,g);
    }
    private static Case isolatedFixture(){
        MeshModel m=new MeshModel();
        addBox(m,0,20,0,20,0,20);addBox(m,20.005,40.005,0,20,0,20);addBox(m,100,120,0,20,0,20);
        AssemblyContactGraph.Result g=graph(m);
        return c(g.bodies==3&&g.transferEdges==1&&g.connectedComponents==2&&g.isolatedBodies==1&&!g.assemblyReady,g);
    }
    private static Case interferenceFixture(){
        MeshModel m=new MeshModel();
        addBox(m,0,20,0,20,0,20);addBox(m,19.8,39.8,0,20,0,20);addBox(m,39.805,59.805,0,20,0,20);
        AssemblyContactGraph.Result g=graph(m);
        return c(g.bodies==3&&g.hasInterference&&g.interferenceEdges>=1&&!g.assemblyReady,g);
    }
    private static AssemblyContactGraph.Result graph(MeshModel m){AssemblyBodyDecomposer.Result d=AssemblyBodyDecomposer.decompose(m);return AssemblyContactGraph.evaluate(d,ContactCandidateEngine.analyze(m,d));}
    private static Case c(boolean p,AssemblyContactGraph.Result g){return new Case(p,String.format(Locale.US,"b=%d e=%d comp=%d iso=%d int=%d ready=%s",g.bodies,g.transferEdges,g.connectedComponents,g.isolatedBodies,g.interferenceEdges,g.assemblyReady));}
    private static final class Case{final boolean pass;final String summary;Case(boolean p,String s){pass=p;summary=s;}}

    private static void addBox(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){
        double[][] v={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{1,2,6},{1,6,5},{2,3,7},{2,7,6},{3,0,4},{3,4,7}};
        for(int[] t:f){int base=m.vertices.size();for(int k=0;k<3;k++){double[] p=v[t[k]];m.addVertex(new MeshModel.V3(p[0],p[1],p[2]));}m.triangles.add(new int[]{base,base+1,base+2});}
    }
}
