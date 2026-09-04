package com.mg.structuralai;

import java.util.*;

/** Extracts a connected, approximately coplanar/curvature-continuous triangle region from a picked point. */
public final class SurfaceRegionExtractor {
    public static final class Region {
        public final Set<Integer> triangles = new LinkedHashSet<>();
        public final Set<Integer> vertices = new LinkedHashSet<>();
        public double areaModel2;
    }
    private SurfaceRegionExtractor(){}

    public static Region fromPick(MeshModel s, MeshModel.V3 pick, double maxNormalAngleDeg){
        if(s==null || s.triangles.isEmpty()) throw new IllegalArgumentException("Yüzey üçgeni yok");
        int seed=nearestTriangle(s,pick);
        double cosMin=Math.cos(Math.toRadians(maxNormalAngleDeg));
        Map<Long,List<Integer>> edgeToTris=new HashMap<>();
        for(int ti=0;ti<s.triangles.size();ti++){
            int[] t=s.triangles.get(ti); if(t.length<3) continue;
            addEdge(edgeToTris,t[0],t[1],ti); addEdge(edgeToTris,t[1],t[2],ti); addEdge(edgeToTris,t[2],t[0],ti);
        }
        Region out=new Region(); boolean[] seen=new boolean[s.triangles.size()]; ArrayDeque<Integer> q=new ArrayDeque<>(); q.add(seed); seen[seed]=true;
        while(!q.isEmpty()){
            int ti=q.removeFirst(); int[] t=s.triangles.get(ti); double[] ni=normal(s,t); if(norm(ni)<=1e-20) continue;
            out.triangles.add(ti); out.vertices.add(t[0]); out.vertices.add(t[1]); out.vertices.add(t[2]); out.areaModel2+=area(s,t);
            int[][] edges={{t[0],t[1]},{t[1],t[2]},{t[2],t[0]}};
            for(int[] e:edges){
                List<Integer> ns=edgeToTris.get(edgeKey(e[0],e[1])); if(ns==null) continue;
                for(int nj:ns) if(!seen[nj]){
                    int[] tt=s.triangles.get(nj); double[] nn=normal(s,tt); double den=norm(ni)*norm(nn); if(den<=1e-20) continue;
                    double c=dot(ni,nn)/den;
                    // abs handles inconsistent local winding while keeping geometric continuity.
                    if(Math.abs(c)>=cosMin){seen[nj]=true;q.add(nj);}
                }
            }
        }
        if(out.triangles.isEmpty()) throw new IllegalStateException("Bağlı yüzey bölgesi çıkarılamadı");
        return out;
    }

    private static int nearestTriangle(MeshModel s,MeshModel.V3 p){int best=-1;double bd=Double.POSITIVE_INFINITY;for(int i=0;i<s.triangles.size();i++){int[] t=s.triangles.get(i);if(t.length<3)continue;MeshModel.V3 a=s.vertices.get(t[0]),b=s.vertices.get(t[1]),c=s.vertices.get(t[2]);double x=(a.x+b.x+c.x)/3,y=(a.y+b.y+c.y)/3,z=(a.z+b.z+c.z)/3,d=sq(x-p.x)+sq(y-p.y)+sq(z-p.z);if(d<bd){bd=d;best=i;}}if(best<0)throw new IllegalStateException("Seçime yakın yüzey bulunamadı");return best;}
    private static void addEdge(Map<Long,List<Integer>> m,int a,int b,int t){m.computeIfAbsent(edgeKey(a,b),k->new ArrayList<>()).add(t);}    
    private static long edgeKey(int a,int b){int lo=Math.min(a,b),hi=Math.max(a,b);return (((long)lo)<<32)|(hi&0xffffffffL);}    
    private static double[] normal(MeshModel s,int[] t){MeshModel.V3 a=s.vertices.get(t[0]),b=s.vertices.get(t[1]),c=s.vertices.get(t[2]);double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;return new double[]{uy*vz-uz*vy,uz*vx-ux*vz,ux*vy-uy*vx};}
    private static double area(MeshModel s,int[] t){return 0.5*norm(normal(s,t));}
    private static double norm(double[] a){return Math.sqrt(dot(a,a));} private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];} private static double sq(double x){return x*x;}
}
