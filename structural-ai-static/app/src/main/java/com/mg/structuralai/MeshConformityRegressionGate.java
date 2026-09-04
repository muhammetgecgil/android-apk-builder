package com.mg.structuralai;

import java.util.Locale;

/** Locks the bbox-fitted voxel-grid conformity behavior so short axes can never overrun CAD bounds again. */
public final class MeshConformityRegressionGate {
    public static final class Result {
        public final boolean pass;
        public final String summary;
        Result(boolean p,String s){pass=p;summary=s;}
    }
    private static volatile Result cached;
    private MeshConformityRegressionGate(){}

    public static Result run(){
        Result c=cached;if(c!=null)return c;
        try{
            MeshModel box=box(0,100,-10,10,-5,5);
            int[] levels={16,24,32};
            StringBuilder sb=new StringBuilder();
            boolean pass=true;
            for(int cells:levels){
                VoxelTetMesher.Result vr=VoxelTetMesher.generate(box,cells,0.001);
                BoundaryConformityReport bc=vr.conformity;
                boolean axisFit=withinBounds(vr.mesh,box,0.001,1e-12);
                boolean strict=bc.productionReady&&bc.meanRelativeToCell<=0.10&&bc.maxRelativeToCell<=0.35;
                pass&=axisFit&&strict&&vr.quality.pass;
                if(sb.length()>0)sb.append("\n");
                sb.append(String.format(Locale.US,
                    "cells=%d | nx=%d ny=%d nz=%d | axisFit=%s | quality=%s | mean/h=%.4f | max/h=%.4f | conformal=%s",
                    cells,vr.nx,vr.ny,vr.nz,axisFit,vr.quality.pass,bc.meanRelativeToCell,bc.maxRelativeToCell,bc.productionReady));
            }
            return cached=new Result(pass,"MESH CONFORMITY REGRESSION "+(pass?"PASS":"FAIL")+"\n"+sb);
        }catch(Throwable t){return cached=new Result(false,"MESH CONFORMITY REGRESSION ERROR: "+t.getMessage());}
    }

    private static boolean withinBounds(TetMeshData m,MeshModel s,double scale,double eps){
        double minX=s.minX*scale,minY=s.minY*scale,minZ=s.minZ*scale,maxX=s.maxX*scale,maxY=s.maxY*scale,maxZ=s.maxZ*scale;
        for(MeshModel.V3 p:m.nodes){
            if(p.x<minX-eps||p.x>maxX+eps||p.y<minY-eps||p.y>maxY+eps||p.z<minZ-eps||p.z>maxZ+eps)return false;
        }
        return true;
    }

    private static MeshModel box(double x0,double x1,double y0,double y1,double z0,double z1){
        MeshModel m=new MeshModel();double[][] p={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
        for(double[] q:p)m.addVertex(new MeshModel.V3(q[0],q[1],q[2]));
        int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
        for(int[] t:f)m.triangles.add(t);return m;
    }
}
