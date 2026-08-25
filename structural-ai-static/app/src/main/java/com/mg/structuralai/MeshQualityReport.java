package com.mg.structuralai;

import java.util.Locale;

/** Requirement trace: SR-MESH quality gate for solver-ready TET4 meshes. */
public final class MeshQualityReport {
    public final int elementCount;
    public final int invertedCount;
    public final int degenerateCount;
    public final double minVolume;
    public final double maxVolume;
    public final double minMeanRatio;
    public final double maxEdgeRatio;
    public final boolean pass;

    private MeshQualityReport(int n,int inv,int deg,double minV,double maxV,double minQ,double maxER,boolean pass){
        this.elementCount=n; this.invertedCount=inv; this.degenerateCount=deg;
        this.minVolume=minV; this.maxVolume=maxV; this.minMeanRatio=minQ; this.maxEdgeRatio=maxER; this.pass=pass;
    }

    public static MeshQualityReport evaluate(TetMeshData mesh){
        if(mesh.tets.isEmpty()) throw new IllegalArgumentException("No TET4 elements");
        int inv=0,deg=0; double minV=Double.POSITIVE_INFINITY,maxV=0,minQ=Double.POSITIVE_INFINITY,maxER=0;
        for(int[] t:mesh.tets){
            MeshModel.V3 a=mesh.nodes.get(t[0]), b=mesh.nodes.get(t[1]), c=mesh.nodes.get(t[2]), d=mesh.nodes.get(t[3]);
            double signed6=triple(sub(b,a),sub(c,a),sub(d,a));
            if(signed6<0) inv++;
            double v=Math.abs(signed6)/6.0;
            minV=Math.min(minV,v); maxV=Math.max(maxV,v);
            if(v<=1e-18){ deg++; minQ=0; continue; }
            double[] e={dist(a,b),dist(a,c),dist(a,d),dist(b,c),dist(b,d),dist(c,d)};
            double minE=Double.POSITIVE_INFINITY,maxE=0,sumE2=0;
            for(double x:e){ minE=Math.min(minE,x); maxE=Math.max(maxE,x); sumE2+=x*x; }
            maxER=Math.max(maxER,maxE/Math.max(minE,1e-30));
            // Tetra mean-ratio quality, normalized so regular tetra -> 1.
            double q=12.0*Math.pow(3.0*v,2.0/3.0)/Math.max(sumE2,1e-30);
            minQ=Math.min(minQ,q);
        }
        boolean pass=deg==0 && inv==0 && minQ>=0.12 && maxER<=12.0;
        return new MeshQualityReport(mesh.tets.size(),inv,deg,minV,maxV,minQ,maxER,pass);
    }

    public String summary(){
        return String.format(Locale.US,
            "TET4=%d | inverted=%d | degenerate=%d | minMeanRatio=%.3f | maxEdgeRatio=%.2f | qualityGate=%s",
            elementCount,invertedCount,degenerateCount,minMeanRatio,maxEdgeRatio,pass?"PASS":"BLOCKED");
    }

    private static MeshModel.V3 sub(MeshModel.V3 a,MeshModel.V3 b){ return new MeshModel.V3(a.x-b.x,a.y-b.y,a.z-b.z); }
    private static double triple(MeshModel.V3 a,MeshModel.V3 b,MeshModel.V3 c){
        return a.x*(b.y*c.z-b.z*c.y)-a.y*(b.x*c.z-b.z*c.x)+a.z*(b.x*c.y-b.y*c.x);
    }
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){ double x=a.x-b.x,y=a.y-b.y,z=a.z-b.z; return Math.sqrt(x*x+y*y+z*z); }
}
