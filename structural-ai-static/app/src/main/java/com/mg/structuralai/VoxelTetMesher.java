package com.mg.structuralai;

import java.util.HashMap;
import java.util.Map;

/**
 * Offline mobile volumetric mesher v1 fallback.
 * A closed triangle surface is sampled on a Cartesian lattice and occupied cells are split into
 * conforming TET4 elements. BoundaryConformityReport explicitly quantifies the voxel-boundary error;
 * this mesher is not promoted to production CAD-conformal status unless that gate passes.
 */
public final class VoxelTetMesher {
    public static final class Result {
        public final TetMeshData mesh;
        public final MeshQualityReport quality;
        public final BoundaryConformityReport conformity;
        public final int nx,ny,nz,insideCells;
        /** Representative maximum cell edge in model units, used for normalized conformity gates. */
        public final double cellSizeModelUnits;
        Result(TetMeshData m,MeshQualityReport q,BoundaryConformityReport bc,int nx,int ny,int nz,int inside,double h){
            this.mesh=m; this.quality=q; this.conformity=bc; this.nx=nx; this.ny=ny; this.nz=nz; this.insideCells=inside; this.cellSizeModelUnits=h;
        }
    }

    private VoxelTetMesher(){}

    public static Result generate(MeshModel s,int targetLongestAxisCells,double unitScaleToMetres){
        if(s==null || s.vertices.size()<4 || s.triangles.size()<4) throw new IllegalArgumentException("Closed triangle surface required");
        if(!(unitScaleToMetres>0) || !Double.isFinite(unitScaleToMetres)) throw new IllegalArgumentException("Unit scale to metres must be resolved");
        int n=Math.max(4,Math.min(64,targetLongestAxisCells));
        double dx=s.dx(),dy=s.dy(),dz=s.dz();
        double longest=Math.max(dx,Math.max(dy,dz));
        if(!(longest>0)||!Double.isFinite(longest))throw new IllegalArgumentException("Model bounding box is degenerate");
        double hTarget=longest/n;
        int nx=Math.max(1,(int)Math.ceil(dx/hTarget));
        int ny=Math.max(1,(int)Math.ceil(dy/hTarget));
        int nz=Math.max(1,(int)Math.ceil(dz/hTarget));
        // Critical conformity rule: each axis is fitted exactly to its source bounding extent.
        // The old single-h grid could overshoot short axes by nearly one full cell, creating a
        // false geometry boundary and making production conformity fail even for an exact box.
        double hx=dx>0?dx/nx:hTarget;
        double hy=dy>0?dy/ny:hTarget;
        double hz=dz>0?dz/nz:hTarget;
        double h=Math.max(hx,Math.max(hy,hz));
        long cells=(long)nx*ny*nz;
        if(cells>65536) throw new IllegalArgumentException("Requested mesh exceeds mobile v1.9 cell budget: "+cells);

        boolean[][][] inside=new boolean[nx][ny][nz];
        int insideCount=0;
        for(int i=0;i<nx;i++) for(int j=0;j<ny;j++) for(int k=0;k<nz;k++){
            double x=s.minX+(i+0.5)*hx;
            double y=s.minY+(j+0.5)*hy;
            double z=s.minZ+(k+0.5)*hz;
            if(pointInside(s,x,y,z)){ inside[i][j][k]=true; insideCount++; }
        }
        if(insideCount==0) throw new IllegalStateException("No closed interior detected. Surface may be open/non-manifold or mesh resolution too coarse.");

        TetMeshData out=new TetMeshData();
        Map<Long,Integer> nodes=new HashMap<>();
        final int[][] pattern={{0,1,3,7},{0,3,2,7},{0,2,6,7},{0,6,4,7},{0,4,5,7},{0,5,1,7}};
        final int[][] corner={{0,0,0},{1,0,0},{0,1,0},{1,1,0},{0,0,1},{1,0,1},{0,1,1},{1,1,1}};
        for(int i=0;i<nx;i++) for(int j=0;j<ny;j++) for(int k=0;k<nz;k++) if(inside[i][j][k]){
            int[] c=new int[8];
            for(int p=0;p<8;p++){
                int gi=i+corner[p][0], gj=j+corner[p][1], gk=k+corner[p][2];
                long key=gridKey(gi,gj,gk); Integer idx=nodes.get(key);
                if(idx==null){
                    double x=(s.minX+gi*hx)*unitScaleToMetres, y=(s.minY+gj*hy)*unitScaleToMetres, z=(s.minZ+gk*hz)*unitScaleToMetres;
                    idx=out.addNode(x,y,z); nodes.put(key,idx);
                }
                c[p]=idx;
            }
            for(int[] p:pattern) addPositiveTet(out,c[p[0]],c[p[1]],c[p[2]],c[p[3]]);
        }
        out.validate();
        MeshQualityReport q=MeshQualityReport.evaluate(out);
        BoundaryConformityReport bc=BoundaryConformityReport.evaluate(out,s,unitScaleToMetres,h);
        return new Result(out,q,bc,nx,ny,nz,insideCount,h);
    }

    private static void addPositiveTet(TetMeshData m,int a,int b,int c,int d){
        MeshModel.V3 A=m.nodes.get(a),B=m.nodes.get(b),C=m.nodes.get(c),D=m.nodes.get(d);
        double v6=(B.x-A.x)*((C.y-A.y)*(D.z-A.z)-(C.z-A.z)*(D.y-A.y))
            -(B.y-A.y)*((C.x-A.x)*(D.z-A.z)-(C.z-A.z)*(D.x-A.x))
            +(B.z-A.z)*((C.x-A.x)*(D.y-A.y)-(C.y-A.y)*(D.x-A.x));
        if(Math.abs(v6)<=1e-18) throw new IllegalStateException("Mesher produced degenerate TET4");
        if(v6>0) m.addTet(a,b,c,d); else m.addTet(a,c,b,d);
    }

    private static long gridKey(int i,int j,int k){return (((long)i)&0x1fffffL)<<42 | ((((long)j)&0x1fffffL)<<21) | (((long)k)&0x1fffffL);}

    private static boolean pointInside(MeshModel s,double x,double y,double z){
        double eps=Math.max(s.diagonal(),1.0)*1e-10; double oy=y+0.371*eps, oz=z+0.613*eps; int hits=0;
        for(int[] t:s.triangles){if(t.length<3) continue;MeshModel.V3 a=s.vertices.get(t[0]),b=s.vertices.get(t[1]),c=s.vertices.get(t[2]);if(rayHitPositiveX(x,oy,oz,a,b,c))hits++;}
        return (hits&1)==1;
    }

    private static boolean rayHitPositiveX(double ox,double oy,double oz,MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){
        double e1x=b.x-a.x,e1y=b.y-a.y,e1z=b.z-a.z,e2x=c.x-a.x,e2y=c.y-a.y,e2z=c.z-a.z;
        double det=e1y*(-e2z)+e1z*e2y;if(Math.abs(det)<1e-14)return false;double inv=1.0/det;
        double tx=ox-a.x,ty=oy-a.y,tz=oz-a.z;double u=(ty*(-e2z)+tz*e2y)*inv;if(u<-1e-10||u>1.0+1e-10)return false;
        double qx=ty*e1z-tz*e1y,qy=tz*e1x-tx*e1z,qz=tx*e1y-ty*e1x;double v=qx*inv;if(v<-1e-10||u+v>1.0+1e-10)return false;
        double distance=(e2x*qx+e2y*qy+e2z*qz)*inv;return distance>1e-10;
    }
}
