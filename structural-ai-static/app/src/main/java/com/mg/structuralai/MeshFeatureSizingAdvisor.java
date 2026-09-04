package com.mg.structuralai;

import java.util.*;

/** Geometry-derived mesh sizing hints. Conservative: it may ask for more cells, never fewer. */
public final class MeshFeatureSizingAdvisor {
    public static final class Result {
        public final boolean thinLike;
        public final double minSpan,maxSpan,slenderness;
        public final int sharpEdges;
        public final int recommendedLongestAxisCells;
        public final String summary;
        Result(boolean thin,double min,double max,double sl,int sharp,int rec,String s){thinLike=thin;minSpan=min;maxSpan=max;slenderness=sl;sharpEdges=sharp;recommendedLongestAxisCells=rec;summary=s;}
    }
    private MeshFeatureSizingAdvisor(){}

    public static Result evaluate(MeshModel m,int requested){
        double[] spans={Math.max(m.dx(),1e-12),Math.max(m.dy(),1e-12),Math.max(m.dz(),1e-12)};
        Arrays.sort(spans);double min=spans[0],max=spans[2],sl=max/min;
        boolean thin=sl>=12.0;
        int sharp=countSharpEdges(m,35.0);
        int rec=Math.max(8,requested);
        if(thin)rec=Math.max(rec,(int)Math.ceil(8.0*max/min)); // target >=8 cells through thinnest span
        if(sharp>0)rec=Math.max(rec,Math.min(56,requested+8));
        rec=Math.min(56,rec);
        String s=String.format(Locale.US,"thinLike=%s | minSpan=%.6g | maxSpan=%.6g | slenderness=%.2f | sharpEdges=%d | requested=%d | recommended=%d",thin,min,max,sl,sharp,requested,rec);
        return new Result(thin,min,max,sl,sharp,rec,s);
    }

    /** Counts manifold edges whose adjacent face normals differ by more than thresholdDeg.
     *  For a 90-degree corner dot(n1,n2)=0, so it must be classified sharp for a 35-degree threshold.
     *  The previous implementation compared against cos(180-threshold), which only detected nearly
     *  opposite normals and therefore missed ordinary CAD corners and stepped features. */
    private static int countSharpEdges(MeshModel m,double thresholdDeg){
        Map<Long,List<Integer>> edgeToTri=new HashMap<>();
        for(int ti=0;ti<m.triangles.size();ti++){
            int[] t=m.triangles.get(ti);if(t.length<3)continue;
            add(edgeToTri,t[0],t[1],ti);add(edgeToTri,t[1],t[2],ti);add(edgeToTri,t[2],t[0],ti);
        }
        int n=0;double cosLimit=Math.cos(Math.toRadians(thresholdDeg));
        for(List<Integer> ids:edgeToTri.values())if(ids.size()==2){
            MeshModel.V3 a=normal(m,ids.get(0)),b=normal(m,ids.get(1));
            double d=Math.max(-1.0,Math.min(1.0,a.x*b.x+a.y*b.y+a.z*b.z));
            if(d<cosLimit)n++;
        }
        return n;
    }
    private static void add(Map<Long,List<Integer>> map,int a,int b,int ti){int lo=Math.min(a,b),hi=Math.max(a,b);long k=(((long)lo)<<32)^(hi&0xffffffffL);map.computeIfAbsent(k,x->new ArrayList<>()).add(ti);}
    private static MeshModel.V3 normal(MeshModel m,int ti){int[] t=m.triangles.get(ti);MeshModel.V3 a=m.vertices.get(t[0]),b=m.vertices.get(t[1]),c=m.vertices.get(t[2]);double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,q=Math.sqrt(nx*nx+ny*ny+nz*nz);return q>1e-30?new MeshModel.V3(nx/q,ny/q,nz/q):new MeshModel.V3(0,0,1);}
}
