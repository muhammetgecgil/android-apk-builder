package com.mg.structuralai;

import java.util.HashMap;
import java.util.Map;

/**
 * Offline mobile volumetric mesher v1.
 * A closed triangle surface is sampled on a Cartesian lattice and occupied cells are split into
 * conforming TET4 elements. This is a real volume mesh, but remains a voxel-boundary approximation;
 * CAD-conformal Delaunay/TET10 meshing is the next production tier.
 */
public final class VoxelTetMesher {
    public static final class Result {
        public final TetMeshData mesh;
        public final MeshQualityReport quality;
        public final int nx,ny,nz,insideCells;
        public final double cellSizeModelUnits;
        Result(TetMeshData m,MeshQualityReport q,int nx,int ny,int nz,int inside,double h){
            this.mesh=m; this.quality=q; this.nx=nx; this.ny=ny; this.nz=nz; this.insideCells=inside; this.cellSizeModelUnits=h;
        }
    }

    private VoxelTetMesher(){}

    public static Result generate(MeshModel s,int targetLongestAxisCells,double unitScaleToMetres){
        if(s==null || s.vertices.size()<4 || s.triangles.size()<4) throw new IllegalArgumentException("Closed triangle surface required");
        if(!(unitScaleToMetres>0) || !Double.isFinite(unitScaleToMetres)) throw new IllegalArgumentException("Unit scale to metres must be resolved");
        int n=Math.max(4,Math.min(32,targetLongestAxisCells));
        double longest=Math.max(s.dx(),Math.max(s.dy(),s.dz()));
        double h=longest/n;
        int nx=Math.max(1,(int)Math.ceil(s.dx()/h));
        int ny=Math.max(1,(int)Math.ceil(s.dy()/h));
        int nz=Math.max(1,(int)Math.ceil(s.dz()/h));
        // Mobile safety gate: prevents accidental memory explosion.
        long cells=(long)nx*ny*nz;
        if(cells>32768) throw new IllegalArgumentException("Requested mesh exceeds mobile v1 cell budget: "+cells);

        boolean[][][] inside=new boolean[nx][ny][nz];
        int insideCount=0;
        for(int i=0;i<nx;i++) for(int j=0;j<ny;j++) for(int k=0;k<nz;k++){
            double x=s.minX+(i+0.5)*h;
            double y=s.minY+(j+0.5)*h;
            double z=s.minZ+(k+0.5)*h;
            // Do not let padded last cells extend the sampled center beyond the source bounds.
            if(x>s.maxX || y>s.maxY || z>s.maxZ) continue;
            if(pointInside(s,x,y,z)){ inside[i][j][k]=true; insideCount++; }
        }
        if(insideCount==0) throw new IllegalStateException("No closed interior detected. Surface may be open/non-manifold or mesh resolution too coarse.");

        TetMeshData out=new TetMeshData();
        Map<Long,Integer> nodes=new HashMap<>();
        final int[][] pattern={
            {0,1,3,7},{0,3,2,7},{0,2,6,7},
            {0,6,4,7},{0,4,5,7},{0,5,1,7}
        };
        final int[][] corner={
            {0,0,0},{1,0,0},{0,1,0},{1,1,0},
            {0,0,1},{1,0,1},{0,1,1},{1,1,1}
        };
        for(int i=0;i<nx;i++) for(int j=0;j<ny;j++) for(int k=0;k<nz;k++) if(inside[i][j][k]){
            int[] c=new int[8];
            for(int p=0;p<8;p++){
                int gi=i+corner[p][0], gj=j+corner[p][1], gk=k+corner[p][2];
                long key=gridKey(gi,gj,gk);
                Integer idx=nodes.get(key);
                if(idx==null){
                    double x=(s.minX+gi*h)*unitScaleToMetres;
                    double y=(s.minY+gj*h)*unitScaleToMetres;
                    double z=(s.minZ+gk*h)*unitScaleToMetres;
                    idx=out.addNode(x,y,z); nodes.put(key,idx);
                }
                c[p]=idx;
            }
            for(int[] p:pattern) addPositiveTet(out,c[p[0]],c[p[1]],c[p[2]],c[p[3]]);
        }
        out.validate();
        MeshQualityReport q=MeshQualityReport.evaluate(out);
        return new Result(out,q,nx,ny,nz,insideCount,h);
    }

    private static void addPositiveTet(TetMeshData m,int a,int b,int c,int d){
        MeshModel.V3 A=m.nodes.get(a),B=m.nodes.get(b),C=m.nodes.get(c),D=m.nodes.get(d);
        double v6=(B.x-A.x)*((C.y-A.y)*(D.z-A.z)-(C.z-A.z)*(D.y-A.y))
            -(B.y-A.y)*((C.x-A.x)*(D.z-A.z)-(C.z-A.z)*(D.x-A.x))
            +(B.z-A.z)*((C.x-A.x)*(D.y-A.y)-(C.y-A.y)*(D.x-A.x));
        if(Math.abs(v6)<=1e-18) throw new IllegalStateException("Mesher produced degenerate TET4");
        if(v6>0) m.addTet(a,b,c,d); else m.addTet(a,c,b,d);
    }

    private static long gridKey(int i,int j,int k){
        return (((long)i)&0x1fffffL)<<42 | ((((long)j)&0x1fffffL)<<21) | (((long)k)&0x1fffffL);
    }

    /** Odd-even ray test along +X; deterministic perturbation reduces vertex/edge ambiguity. */
    private static boolean pointInside(MeshModel s,double x,double y,double z){
        double eps=Math.max(s.diagonal(),1.0)*1e-10;
        double oy=y+0.371*eps, oz=z+0.613*eps;
        int hits=0;
        for(int[] t:s.triangles){
            if(t.length<3) continue;
            MeshModel.V3 a=s.vertices.get(t[0]), b=s.vertices.get(t[1]), c=s.vertices.get(t[2]);
            if(rayHitPositiveX(x,oy,oz,a,b,c)) hits++;
        }
        return (hits&1)==1;
    }

    private static boolean rayHitPositiveX(double ox,double oy,double oz,MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){
        // Moller-Trumbore with direction (1,0,0).
        double e1x=b.x-a.x,e1y=b.y-a.y,e1z=b.z-a.z;
        double e2x=c.x-a.x,e2y=c.y-a.y,e2z=c.z-a.z;
        // p = dir x e2 = (0,-e2z,e2y)
        double det=e1y*(-e2z)+e1z*e2y;
        if(Math.abs(det)<1e-14) return false;
        double inv=1.0/det;
        double tx=ox-a.x,ty=oy-a.y,tz=oz-a.z;
        double u=(ty*(-e2z)+tz*e2y)*inv;
        if(u<-1e-10 || u>1.0+1e-10) return false;
        // q = t x e1
        double qx=ty*e1z-tz*e1y;
        double qy=tz*e1x-tx*e1z;
        double qz=tx*e1y-ty*e1x;
        double v=qx*inv; // dir dot q = qx
        if(v<-1e-10 || u+v>1.0+1e-10) return false;
        double distance=(e2x*qx+e2y*qy+e2z*qz)*inv;
        return distance>1e-10;
    }
}
