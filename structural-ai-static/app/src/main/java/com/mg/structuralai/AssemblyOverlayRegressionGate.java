package com.mg.structuralai;

/** Locks the UI-neutral assembly overlay model to the engineering contact graph. */
public final class AssemblyOverlayRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private AssemblyOverlayRegressionGate(){}
    public static Result run(){
        try{
            MeshModel m=new MeshModel();
            addBox(m,0,20,0,20,0,20);addBox(m,20.005,40.005,0,20,0,20);addBox(m,40.010,60.010,0,20,0,20);
            AssemblyBodyDecomposer.Result d=AssemblyBodyDecomposer.decompose(m);
            ContactCandidateEngine.Result c=ContactCandidateEngine.analyze(m,d);
            AssemblyContactGraph.Result g=AssemblyContactGraph.evaluate(d,c);
            AssemblyOverlayModel o=AssemblyOverlayModel.build(d,g);
            boolean bodyOk=o.bodies.size()==3&&"B0".equals(o.bodies.get(0).label());
            boolean edgeOk=o.edges.size()>=2;
            for(AssemblyOverlayModel.Edge e:o.edges)if(e.style==AssemblyOverlayModel.EdgeStyle.INTERFERENCE)edgeOk=false;
            boolean readyOk=o.assemblyReady&&o.badgeText.contains("ASSEMBLY READY");

            MeshModel x=new MeshModel();addBox(x,0,20,0,20,0,20);addBox(x,19.8,39.8,0,20,0,20);
            AssemblyBodyDecomposer.Result dx=AssemblyBodyDecomposer.decompose(x);AssemblyContactGraph.Result gx=AssemblyContactGraph.evaluate(dx,ContactCandidateEngine.analyze(x,dx));AssemblyOverlayModel ox=AssemblyOverlayModel.build(dx,gx);
            boolean blockOk=!ox.assemblyReady&&ox.badgeText.contains("BLOCKED");boolean red=false;for(AssemblyOverlayModel.Edge e:ox.edges)if(e.style==AssemblyOverlayModel.EdgeStyle.INTERFERENCE&&e.blocksReadiness)red=true;
            boolean pass=bodyOk&&edgeOk&&readyOk&&blockOk&&red;
            return new Result(pass,"ASSEMBLY OVERLAY REGRESSION "+(pass?"PASS":"FAIL")+" | bodies="+o.bodies.size()+" | edges="+o.edges.size()+" | ready="+o.assemblyReady+" | interferenceBlock="+red);
        }catch(Throwable t){return new Result(false,"ASSEMBLY OVERLAY REGRESSION ERROR: "+t.getMessage());}
    }
    private static void addBox(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){double[][] v={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{1,2,6},{1,6,5},{2,3,7},{2,7,6},{3,0,4},{3,4,7}};for(int[] t:f){int base=m.vertices.size();for(int k=0;k<3;k++){double[] p=v[t[k]];m.addVertex(new MeshModel.V3(p[0],p[1],p[2]));}m.triangles.add(new int[]{base,base+1,base+2});}}
}
