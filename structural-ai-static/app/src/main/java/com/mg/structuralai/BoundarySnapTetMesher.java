package com.mg.structuralai;

import java.util.*;

/**
 * v1.7 mesh step 1: quality-safe boundary snapping on top of the conforming voxel TET topology.
 * Boundary nodes are projected toward the nearest source triangle only when the resulting mesh
 * remains non-inverted/non-degenerate and passes the mesh-quality gate. This is intentionally
 * conservative: rejected snaps are rolled back. A true Delaunay/CAD-conformal volume mesher is
 * still the next tier; this class provides a measurable bridge rather than claiming full CAD conformity.
 */
public final class BoundarySnapTetMesher {
    public static final class Result {
        public final TetMeshData mesh;
        public final MeshQualityReport quality;
        public final BoundaryConformityReport before,after;
        public final int boundaryNodes,acceptedSnaps,rejectedSnaps;
        Result(TetMeshData m,MeshQualityReport q,BoundaryConformityReport b,BoundaryConformityReport a,int n,int ok,int bad){
            mesh=m;quality=q;before=b;after=a;boundaryNodes=n;acceptedSnaps=ok;rejectedSnaps=bad;
        }
    }
    private BoundarySnapTetMesher(){}

    public static Result generate(MeshModel surface,int targetLongestAxisCells,double unitScaleToMetres){
        VoxelTetMesher.Result base=VoxelTetMesher.generate(surface,targetLongestAxisCells,unitScaleToMetres);
        TetMeshData mesh=copy(base.mesh);
        double hModel=base.cellSizeModelUnits;
        double hM=hModel*unitScaleToMetres;
        BoundaryConformityReport before=BoundaryConformityReport.evaluate(mesh,surface,unitScaleToMetres,hModel);
        Set<Integer> boundary=boundaryNodes(mesh);
        int accepted=0,rejected=0;

        // Limit snap distance to avoid pulling voxel topology through thin walls or sharp corners.
        double maxSnap=0.80*hM;
        List<Integer> order=new ArrayList<>(boundary);
        Collections.sort(order);
        for(int idx:order){
            MeshModel.V3 old=mesh.nodes.get(idx);
            MeshModel.V3 target=nearestPointOnSurface(old,surface,unitScaleToMetres);
            if(target==null)continue;
            double dist=distance(old,target);
            if(!(dist>1e-14) || dist>maxSnap){rejected++;continue;}

            boolean ok=false;
            for(double alpha:new double[]{1.0,0.75,0.50,0.25}){
                MeshModel.V3 cand=new MeshModel.V3(old.x+alpha*(target.x-old.x),old.y+alpha*(target.y-old.y),old.z+alpha*(target.z-old.z));
                mesh.nodes.set(idx,cand);
                if(localQualitySafe(mesh,idx)){
                    MeshQualityReport q=MeshQualityReport.evaluate(mesh);
                    if(q.pass){ok=true;break;}
                }
                mesh.nodes.set(idx,old);
            }
            if(ok)accepted++; else {mesh.nodes.set(idx,old);rejected++;}
        }

        MeshQualityReport q=MeshQualityReport.evaluate(mesh);
        if(!q.pass)throw new IllegalStateException("Boundary snapping produced unacceptable TET quality: "+q.summary());
        BoundaryConformityReport after=BoundaryConformityReport.evaluate(mesh,surface,unitScaleToMetres,hModel);
        return new Result(mesh,q,before,after,boundary.size(),accepted,rejected);
    }

    private static boolean localQualitySafe(TetMeshData m,int node){
        for(int[] t:m.tets){
            boolean hit=false;for(int x:t)if(x==node){hit=true;break;}if(!hit)continue;
            MeshModel.V3 a=m.nodes.get(t[0]),b=m.nodes.get(t[1]),c=m.nodes.get(t[2]),d=m.nodes.get(t[3]);
            double v6=signed6(a,b,c,d);
            if(!(v6>1e-18))return false;
            double[] e={distance(a,b),distance(a,c),distance(a,d),distance(b,c),distance(b,d),distance(c,d)};
            double min=Double.POSITIVE_INFINITY,max=0,sum2=0;for(double x:e){min=Math.min(min,x);max=Math.max(max,x);sum2+=x*x;}
            double v=v6/6.0;
            double meanRatio=12.0*Math.pow(3.0*v,2.0/3.0)/Math.max(sum2,1e-30);
            if(meanRatio<0.10 || max/Math.max(min,1e-30)>14.0)return false;
        }
        return true;
    }

    private static Set<Integer> boundaryNodes(TetMeshData m){
        Map<Face,Integer> count=new HashMap<>();
        for(int[] t:m.tets){
            add(count,t[0],t[1],t[2]);add(count,t[0],t[1],t[3]);add(count,t[0],t[2],t[3]);add(count,t[1],t[2],t[3]);
        }
        Set<Integer> out=new HashSet<>();
        for(Map.Entry<Face,Integer> e:count.entrySet())if(e.getValue()==1){out.add(e.getKey().a);out.add(e.getKey().b);out.add(e.getKey().c);}
        return out;
    }
    private static void add(Map<Face,Integer> m,int a,int b,int c){Face f=new Face(a,b,c);m.put(f,m.getOrDefault(f,0)+1);}
    private static final class Face{
        final int a,b,c;Face(int x,int y,int z){int[] q={x,y,z};Arrays.sort(q);a=q[0];b=q[1];c=q[2];}
        public int hashCode(){return (a*73856093)^(b*19349663)^(c*83492791);}public boolean equals(Object o){if(!(o instanceof Face))return false;Face f=(Face)o;return a==f.a&&b==f.b&&c==f.c;}
    }

    private static MeshModel.V3 nearestPointOnSurface(MeshModel.V3 p,MeshModel s,double scale){
        MeshModel.V3 best=null;double bd=Double.POSITIVE_INFINITY;
        for(int[] t:s.triangles){if(t.length<3)continue;
            MeshModel.V3 A=scaled(s.vertices.get(t[0]),scale),B=scaled(s.vertices.get(t[1]),scale),C=scaled(s.vertices.get(t[2]),scale);
            MeshModel.V3 q=closestPointTriangle(p,A,B,C);double d2=dist2(p,q);if(d2<bd){bd=d2;best=q;}
        }
        return best;
    }
    private static MeshModel.V3 scaled(MeshModel.V3 p,double s){return new MeshModel.V3(p.x*s,p.y*s,p.z*s);}

    private static MeshModel.V3 closestPointTriangle(MeshModel.V3 p,MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){
        double[] ab=sub(b,a),ac=sub(c,a),ap=sub(p,a);double d1=dot(ab,ap),d2=dot(ac,ap);if(d1<=0&&d2<=0)return a;
        double[] bp=sub(p,b);double d3=dot(ab,bp),d4=dot(ac,bp);if(d3>=0&&d4<=d3)return b;
        double vc=d1*d4-d3*d2;if(vc<=0&&d1>=0&&d3<=0){double v=d1/(d1-d3);return lerp(a,b,v);}
        double[] cp=sub(p,c);double d5=dot(ab,cp),d6=dot(ac,cp);if(d6>=0&&d5<=d6)return c;
        double vb=d5*d2-d1*d6;if(vb<=0&&d2>=0&&d6<=0){double w=d2/(d2-d6);return lerp(a,c,w);}
        double va=d3*d6-d5*d4;if(va<=0&&(d4-d3)>=0&&(d5-d6)>=0){double w=(d4-d3)/((d4-d3)+(d5-d6));return lerp(b,c,w);}
        double den=1.0/(va+vb+vc),v=vb*den,w=vc*den;return new MeshModel.V3(a.x+ab[0]*v+ac[0]*w,a.y+ab[1]*v+ac[1]*w,a.z+ab[2]*v+ac[2]*w);
    }
    private static double[] sub(MeshModel.V3 a,MeshModel.V3 b){return new double[]{a.x-b.x,a.y-b.y,a.z-b.z};}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static MeshModel.V3 lerp(MeshModel.V3 a,MeshModel.V3 b,double t){return new MeshModel.V3(a.x+t*(b.x-a.x),a.y+t*(b.y-a.y),a.z+t*(b.z-a.z));}
    private static double dist2(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return x*x+y*y+z*z;}
    private static double distance(MeshModel.V3 a,MeshModel.V3 b){return Math.sqrt(dist2(a,b));}
    private static double signed6(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c,MeshModel.V3 d){
        return (b.x-a.x)*((c.y-a.y)*(d.z-a.z)-(c.z-a.z)*(d.y-a.y))-(b.y-a.y)*((c.x-a.x)*(d.z-a.z)-(c.z-a.z)*(d.x-a.x))+(b.z-a.z)*((c.x-a.x)*(d.y-a.y)-(c.y-a.y)*(d.x-a.x));
    }
    private static TetMeshData copy(TetMeshData src){TetMeshData d=new TetMeshData();for(MeshModel.V3 p:src.nodes)d.addNode(p.x,p.y,p.z);for(int[] t:src.tets)d.addTet(t[0],t[1],t[2],t[3]);return d;}
}
