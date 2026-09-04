package com.mg.structuralai;

import java.util.ArrayList;
import java.util.List;

/** Maps picked surface points to local volumetric node patches; user-selected BCs replace the v0.7 auto planes. */
public final class UserFemSetup {
    public static final class SetupResult {
        public final int fixedNodes, loadedNodes; public final double radiusM;
        SetupResult(int f,int l,double r){fixedNodes=f;loadedNodes=l;radiusM=r;}
    }
    private UserFemSetup(){}

    public static SetupResult apply(StaticFemSolver solver,TetMeshData mesh,MeshModel.V3 supportModel,MeshModel.V3 loadModel,double unitScale,double fx,double fy,double fz){
        if(supportModel==null||loadModel==null) throw new IllegalArgumentException("Mesnet ve yük noktaları seçilmeli");
        MeshModel.V3 s=new MeshModel.V3(supportModel.x*unitScale,supportModel.y*unitScale,supportModel.z*unitScale);
        MeshModel.V3 l=new MeshModel.V3(loadModel.x*unitScale,loadModel.y*unitScale,loadModel.z*unitScale);
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=-minX,maxY=-minX,maxZ=-minX;
        for(MeshModel.V3 n:mesh.nodes){minX=Math.min(minX,n.x);maxX=Math.max(maxX,n.x);minY=Math.min(minY,n.y);maxY=Math.max(maxY,n.y);minZ=Math.min(minZ,n.z);maxZ=Math.max(maxZ,n.z);}
        double diag=Math.sqrt((maxX-minX)*(maxX-minX)+(maxY-minY)*(maxY-minY)+(maxZ-minZ)*(maxZ-minZ));
        double r=Math.max(diag*0.085,1e-9);
        List<Integer> fixed=near(mesh,s,r), loaded=near(mesh,l,r);
        if(fixed.size()<3) fixed=nearest(mesh,s,4);
        if(loaded.isEmpty()) loaded=nearest(mesh,l,4);
        for(int n:fixed) solver.fixNode(n);
        double inv=1.0/loaded.size();
        for(int n:loaded) solver.addNodalForce(n,fx*inv,fy*inv,fz*inv);
        return new SetupResult(fixed.size(),loaded.size(),r);
    }

    private static List<Integer> near(TetMeshData m,MeshModel.V3 p,double r){List<Integer> out=new ArrayList<>();double r2=r*r;for(int i=0;i<m.nodes.size();i++){MeshModel.V3 n=m.nodes.get(i);double d2=sq(n.x-p.x)+sq(n.y-p.y)+sq(n.z-p.z);if(d2<=r2)out.add(i);}return out;}
    private static List<Integer> nearest(TetMeshData m,MeshModel.V3 p,int count){List<Integer> out=new ArrayList<>();boolean[] used=new boolean[m.nodes.size()];for(int k=0;k<Math.min(count,m.nodes.size());k++){int bi=-1;double bd=Double.POSITIVE_INFINITY;for(int i=0;i<m.nodes.size();i++)if(!used[i]){MeshModel.V3 n=m.nodes.get(i);double d=sq(n.x-p.x)+sq(n.y-p.y)+sq(n.z-p.z);if(d<bd){bd=d;bi=i;}}if(bi>=0){used[bi]=true;out.add(bi);}}return out;}
    private static double sq(double v){return v*v;}
}
