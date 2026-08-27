package com.mg.structuralai;

import java.util.*;

/**
 * Quality-aware conforming local TET4 refinement.
 *
 * The previous fixed centroid 1->4 split preserved conformity but could create sliver children.
 * This version keeps the same face-preserving topology (therefore no hanging nodes) but searches
 * several interior split points, including longest-edge-guided candidates, and accepts only the
 * candidate with the strongest worst-child shape score. If no candidate is safe, refinement is
 * rejected so the autonomous orchestrator can roll back and use the global SmartTetMesher path.
 */
public final class LocalTetRefiner {
    public static final class Result {
        public final TetMeshData mesh;
        public final MeshQualityReport quality;
        public final int refinedParents, addedNodes, childTets;
        Result(TetMeshData m,MeshQualityReport q,int p,int n,int c){mesh=m;quality=q;refinedParents=p;addedNodes=n;childTets=c;}
    }
    private static final double MIN_CHILD_MEAN_RATIO=0.18;
    private static final double MAX_CHILD_EDGE_RATIO=8.0;
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
            MeshModel.V3 split=bestInteriorPoint(src,t);
            if(split==null)throw new IllegalStateException("Local quality-aware refinement rejected: no safe interior split point");
            int g=out.addNode(split.x,split.y,split.z);
            added++;refined++;
            addPositive(out,t[0],t[1],t[2],g);
            addPositive(out,t[0],t[1],g,t[3]);
            addPositive(out,t[0],g,t[2],t[3]);
            addPositive(out,g,t[1],t[2],t[3]);
            children+=4;
        }
        out.validate();
        MeshQualityReport q=MeshQualityReport.evaluate(out);
        if(!q.pass)throw new IllegalStateException("Local quality-aware refinement failed mesh QA: "+q.summary());
        return new Result(out,q,refined,added,children);
    }

    /**
     * Search interior barycentric candidates. The longest edge biases several candidates, while
     * all original triangular faces remain untouched. The objective maximizes the minimum child
     * mean-ratio and then minimizes the maximum child edge-ratio.
     */
    private static MeshModel.V3 bestInteriorPoint(TetMeshData m,int[] t){
        MeshModel.V3[] p={m.nodes.get(t[0]),m.nodes.get(t[1]),m.nodes.get(t[2]),m.nodes.get(t[3])};
        int ea=0,eb=1;double longest=-1;
        for(int i=0;i<4;i++)for(int j=i+1;j<4;j++){
            double d2=dist2(p[i],p[j]);if(d2>longest){longest=d2;ea=i;eb=j;}
        }
        List<double[]> weights=new ArrayList<>();
        weights.add(new double[]{.25,.25,.25,.25});
        // Longest-edge-guided but strictly interior candidates.
        weights.add(edgeBiased(ea,eb,.30,.20));
        weights.add(edgeBiased(ea,eb,.28,.22));
        weights.add(edgeBiased(ea,eb,.27,.23));
        // Mild opposite-edge biases improve flattened/slender parents without approaching a face.
        int[] other=new int[2];int oi=0;for(int i=0;i<4;i++)if(i!=ea&&i!=eb)other[oi++]=i;
        weights.add(edgeBiased(other[0],other[1],.30,.20));
        weights.add(edgeBiased(other[0],other[1],.28,.22));

        MeshModel.V3 best=null;double bestMin=-1,bestMax=Double.POSITIVE_INFINITY;
        for(double[] w:weights){
            MeshModel.V3 q=blend(p,w);
            ShapeScore s=childrenScore(p,q);
            if(!s.valid)continue;
            if(s.minMeanRatio>bestMin+1e-12||(Math.abs(s.minMeanRatio-bestMin)<=1e-12&&s.maxEdgeRatio<bestMax)){
                best=q;bestMin=s.minMeanRatio;bestMax=s.maxEdgeRatio;
            }
        }
        if(best==null||bestMin<MIN_CHILD_MEAN_RATIO||bestMax>MAX_CHILD_EDGE_RATIO)return null;
        return best;
    }

    private static double[] edgeBiased(int a,int b,double edgeWeight,double otherWeight){
        double[] w={otherWeight,otherWeight,otherWeight,otherWeight};w[a]=edgeWeight;w[b]=edgeWeight;return w;
    }
    private static MeshModel.V3 blend(MeshModel.V3[] p,double[] w){
        double x=0,y=0,z=0,s=0;for(int i=0;i<4;i++){x+=p[i].x*w[i];y+=p[i].y*w[i];z+=p[i].z*w[i];s+=w[i];}
        return new MeshModel.V3(x/s,y/s,z/s);
    }
    private static final class ShapeScore{final boolean valid;final double minMeanRatio,maxEdgeRatio;ShapeScore(boolean v,double q,double e){valid=v;minMeanRatio=q;maxEdgeRatio=e;}}
    private static ShapeScore childrenScore(MeshModel.V3[] p,MeshModel.V3 g){
        MeshModel.V3[][] c={{p[0],p[1],p[2],g},{p[0],p[1],g,p[3]},{p[0],g,p[2],p[3]},{g,p[1],p[2],p[3]}};
        double min=Double.POSITIVE_INFINITY,max=0;
        for(MeshModel.V3[] q:c){
            double vol6=Math.abs(volume6(q[0],q[1],q[2],q[3]));if(vol6<=1e-18)return new ShapeScore(false,0,Double.POSITIVE_INFINITY);
            double sumL2=0,minL=Double.POSITIVE_INFINITY,maxL=0;
            for(int i=0;i<4;i++)for(int j=i+1;j<4;j++){double l=Math.sqrt(dist2(q[i],q[j]));sumL2+=l*l;minL=Math.min(minL,l);maxL=Math.max(maxL,l);}
            // Standard tetra mean-ratio quality, 1 for regular tetra, ->0 for slivers.
            double volume=vol6/6.0;
            double mr=12.0*Math.pow(3.0*volume,2.0/3.0)/Math.max(sumL2,1e-30);
            double er=maxL/Math.max(minL,1e-30);
            min=Math.min(min,mr);max=Math.max(max,er);
        }
        return new ShapeScore(Double.isFinite(min)&&Double.isFinite(max),min,max);
    }
    private static double volume6(MeshModel.V3 A,MeshModel.V3 B,MeshModel.V3 C,MeshModel.V3 D){
        return (B.x-A.x)*((C.y-A.y)*(D.z-A.z)-(C.z-A.z)*(D.y-A.y))-(B.y-A.y)*((C.x-A.x)*(D.z-A.z)-(C.z-A.z)*(D.x-A.x))+(B.z-A.z)*((C.x-A.x)*(D.y-A.y)-(C.y-A.y)*(D.x-A.x));
    }
    private static double dist2(MeshModel.V3 a,MeshModel.V3 b){double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return x*x+y*y+z*z;}

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
        double v6=volume6(A,B,C,D);
        if(Math.abs(v6)<=1e-18)throw new IllegalStateException("Local refiner produced degenerate TET4");
        if(v6>0)m.addTet(a,b,c,d);else m.addTet(a,c,b,d);
    }
    private static TetMeshData copy(TetMeshData src){TetMeshData d=new TetMeshData();for(MeshModel.V3 p:src.nodes)d.addNode(p.x,p.y,p.z);for(int[] t:src.tets)d.addTet(t[0],t[1],t[2],t[3]);return d;}
}
