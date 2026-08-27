package com.mg.structuralai;

/** Synthetic geometry regression for the first contact-recognition milestone. */
public final class ContactRegressionGate {
    public static final class Result { public final boolean pass; public final String summary; Result(boolean p,String s){pass=p;summary=s;} }
    private ContactRegressionGate(){}

    public static Result run(){
        MeshModel touching=twoBlocks(0.0);
        AssemblyContactEngine.Result a=AssemblyContactEngine.analyze(touching);
        boolean compA=a.components.size()==2;
        boolean pairA=a.pairs.size()==1;
        boolean contact=pairA&&(a.pairs.get(0).type==AssemblyContactEngine.Type.BONDED_CANDIDATE||a.pairs.get(0).type==AssemblyContactEngine.Type.FRICTIONLESS_CANDIDATE||a.pairs.get(0).type==AssemblyContactEngine.Type.NEAR_GAP);

        MeshModel separated=twoBlocks(20.0);
        AssemblyContactEngine.Result b=AssemblyContactEngine.analyze(separated);
        boolean compB=b.components.size()==2;
        boolean sep=b.pairs.size()==1&&b.pairs.get(0).type==AssemblyContactEngine.Type.SEPARATED;
        boolean ok=compA&&contact&&compB&&sep;
        String s="CONTACT REGRESSION "+(ok?"PASS":"FAIL")+
                " | touching components="+a.components.size()+" pair="+(a.pairs.isEmpty()?"NONE":a.pairs.get(0).type)+
                " | separated components="+b.components.size()+" pair="+(b.pairs.isEmpty()?"NONE":b.pairs.get(0).type);
        return new Result(ok,s);
    }

    private static MeshModel twoBlocks(double gap){
        MeshModel m=new MeshModel();
        addBox(m,0,10,0,10,0,10);
        addBox(m,10+gap,20+gap,0,10,0,10);
        return m;
    }
    private static void addBox(MeshModel m,double x0,double x1,double y0,double y1,double z0,double z1){
        int o=m.vertices.size();
        double[][] p={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        for(double[] q:p)m.addVertex(new MeshModel.V3(q[0],q[1],q[2]));
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
        for(int[] t:f)m.triangles.add(new int[]{o+t[0],o+t[1],o+t[2]});
    }
}
