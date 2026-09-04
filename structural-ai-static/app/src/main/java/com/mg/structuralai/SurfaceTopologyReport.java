package com.mg.structuralai;

import java.util.HashMap;
import java.util.Map;

/** Closed/manifold surface gate that also works with STL files whose triangles do not share vertex indices. */
public final class SurfaceTopologyReport {
    public final int triangleCount,boundaryEdges,nonManifoldEdges,degenerateTriangles;
    public final boolean closedManifold;
    private SurfaceTopologyReport(int t,int b,int n,int d){
        triangleCount=t;boundaryEdges=b;nonManifoldEdges=n;degenerateTriangles=d;
        closedManifold=t>3&&b==0&&n==0&&d==0;
    }

    public static SurfaceTopologyReport evaluate(MeshModel m){
        double tol=Math.max(m.diagonal()*1e-8,1e-12);
        Map<String,Integer> edges=new HashMap<>(); int deg=0;
        for(int[] f:m.triangles){
            if(f.length<3){deg++;continue;}
            MeshModel.V3 a=m.vertices.get(f[0]),b=m.vertices.get(f[1]),c=m.vertices.get(f[2]);
            if(area2(a,b,c)<=tol*tol){deg++;continue;}
            String A=key(a,tol),B=key(b,tol),C=key(c,tol);
            add(edges,A,B); add(edges,B,C); add(edges,C,A);
        }
        int boundary=0,non=0;
        for(int count:edges.values()){ if(count==1) boundary++; else if(count!=2) non++; }
        return new SurfaceTopologyReport(m.triangles.size(),boundary,non,deg);
    }

    public String summary(){
        return "triangles="+triangleCount+" | boundaryEdges="+boundaryEdges+" | nonManifoldEdges="+nonManifoldEdges+
            " | degenerateTriangles="+degenerateTriangles+" | closedManifold="+closedManifold;
    }

    private static void add(Map<String,Integer> e,String a,String b){
        String k=a.compareTo(b)<=0?a+"|"+b:b+"|"+a; e.put(k,e.getOrDefault(k,0)+1);
    }
    private static String key(MeshModel.V3 v,double t){
        return Math.round(v.x/t)+","+Math.round(v.y/t)+","+Math.round(v.z/t);
    }
    private static double area2(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){
        double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;
        double x=uy*vz-uz*vy,y=uz*vx-ux*vz,z=ux*vy-uy*vx; return Math.sqrt(x*x+y*y+z*z);
    }
}
