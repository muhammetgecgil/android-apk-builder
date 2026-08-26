package com.mg.structuralai;

import java.util.*;

/**
 * Conforming local TET4 refinement without hanging nodes on existing element faces.
 * A selected tetra is split by inserting its centroid and replacing it with four child tetrahedra.
 * Original triangular faces are preserved, so neighbouring unrefined tetrahedra remain conforming.
 */
public final class LocalTetRefiner {
    public static final class Result {
        public final TetMeshData mesh;
        public final MeshQualityReport quality;
        public final int refinedParents, addedNodes, childTets;
        Result(TetMeshData m,MeshQualityReport q,int p,int n,int c){mesh=m;quality=q;refinedParents=p;addedNodes=n;childTets=c;}
    }
    private LocalTetRefiner(){}

    public static Result refine(TetMeshData src,Set<Integer> selected){
        if(src==null||selected==null||selected.isEmpty())return new Result(copy(src),MeshQualityReport.evaluate(src),0,0,0);
        TetMeshData out=new TetMeshData();
        for(MeshModel.V3 p:src.nodes)out.addNode(p.x,p.y,p.z);
        int refined=0,children=0,added=0;
        for(int e=0;e<src.tets.size();e++){
            int[] t=src.tets.get(e);
            if(!selected.contains(e)){
                addPositive(out,t[0],t[1],t[2],t[3]);
                continue;
            }
            MeshModel.V3 a=src.nodes.get(t[0]),b=src.nodes.get(t[1]),c=src.nodes.get(t[2]),d=src.nodes.get(t[3]);
            int g=out.addNode((a.x+b.x+c.x+d.x)/4.0,(a.y+b.y+c.y+d.y)/4.0,(a.z+b.z+c.z+d.z)/4.0);
            added++;refined++;
            addPositive(out,t[0],t[1],t[2],g);
            addPositive(out,t[0],t[1],g,t[3]);
            addPositive(out,t[0],g,t[2],t[3]);
            addPositive(out,g,t[1],t[2],t[3]);
            children+=4;
        }
        out.validate();
        MeshQualityReport q=MeshQualityReport.evaluate(out);
        if(!q.pass)throw new IllegalStateException("Local refinement failed mesh QA: "+q.summary());
        return new Result(out,q,refined,added,children);
    }

    public static Set<Integer> selectHighStress(TetMeshData mesh,StaticFemSolver.Result fem,double topFraction,int expandRings){
        int n=mesh.tets.size();Set<Integer> s=new HashSet<>();if(n==0||fem==null||fem.elementVonMisesPa.length!=n)return s;
        Integer[] idx=new Integer[n];for(int i=0;i<n;i++)idx[i]=i;
        Arrays.sort(idx,(i,j)->Double.compare(fem.elementVonMisesPa[j],fem.elementVonMisesPa[i]));
        int keep=Math.max(1,(int)Math.ceil(n*Math.max(0.01,Math.min(0.35,topFraction))));
        for(int i=0;i<keep;i++)s.add(idx[i]);
        if(expandRings>0)expandBySharedFace(mesh,s,expandRings);
        return s;
    }

    private static void expandBySharedFace(TetMeshData m,Set<Integer> selected,int rings){
        Map<Face,List<Integer>> owners=new HashMap<>();
        for(int e=0;e<m.tets.size();e++){
            int[] t=m.tets.get(e);add(owners,new Face(t[0],t[1],t[2]),e);add(owners,new Face(t[0],t[1],t[3]),e);add(owners,new Face(t[0],t[2],t[3]),e);add(owners,new Face(t[1],t[2],t[3]),e);
        }
        for(int r=0;r<rings;r++){
            Set<Integer> next=new HashSet<>(selected);
            for(List<Integer> own:owners.values())if(own.size()==2&&(selected.contains(own.get(0))||selected.contains(own.get(1))))next.addAll(own);
            selected.clear();selected.addAll(next);
        }
    }
    private static void add(Map<Face,List<Integer>> m,Face f,int e){m.computeIfAbsent(f,k->new ArrayList<>()).add(e);}
    private static final class Face{final int a,b,c;Face(int x,int y,int z){int[] q={x,y,z};Arrays.sort(q);a=q[0];b=q[1];c=q[2];}public int hashCode(){return(a*73856093)^(b*19349663)^(c*83492791);}public boolean equals(Object o){if(!(o instanceof Face))return false;Face f=(Face)o;return a==f.a&&b==f.b&&c==f.c;}}

    private static void addPositive(TetMeshData m,int a,int b,int c,int d){
        MeshModel.V3 A=m.nodes.get(a),B=m.nodes.get(b),C=m.nodes.get(c),D=m.nodes.get(d);
        double v6=(B.x-A.x)*((C.y-A.y)*(D.z-A.z)-(C.z-A.z)*(D.y-A.y))-(B.y-A.y)*((C.x-A.x)*(D.z-A.z)-(C.z-A.z)*(D.x-A.x))+(B.z-A.z)*((C.x-A.x)*(D.y-A.y)-(C.y-A.y)*(D.x-A.x));
        if(Math.abs(v6)<=1e-18)throw new IllegalStateException("Local refiner produced degenerate TET4");
        if(v6>0)m.addTet(a,b,c,d);else m.addTet(a,c,b,d);
    }
    private static TetMeshData copy(TetMeshData src){TetMeshData d=new TetMeshData();for(MeshModel.V3 p:src.nodes)d.addNode(p.x,p.y,p.z);for(int[] t:src.tets)d.addTet(t[0],t[1],t[2],t[3]);return d;}
}
