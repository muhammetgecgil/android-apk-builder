package com.mg.structuralai;

import java.util.*;

/** Robust hotspot statistics that are less sensitive to a single singular peak element. */
public final class RegionalHotspotAnalyzer {
    public static final class Stats {
        public final double p95Pa,p99Pa,maxPa;
        public final MeshModel.V3 highStressCentroid;
        public final int highStressElements;
        Stats(double a,double b,double c,MeshModel.V3 p,int n){p95Pa=a;p99Pa=b;maxPa=c;highStressCentroid=p;highStressElements=n;}
    }
    public static final class Result {
        public final Stats previous,current;
        public final double p95Change,p99Change,centroidShiftM;
        public final boolean regionalConverged;
        public final boolean isolatedPeakSuspected;
        Result(Stats a,Stats b,double c95,double c99,double shift,boolean ok,boolean isolated){previous=a;current=b;p95Change=c95;p99Change=c99;centroidShiftM=shift;regionalConverged=ok;isolatedPeakSuspected=isolated;}
    }
    private RegionalHotspotAnalyzer(){}

    public static Result analyze(AdaptiveRefinementStudy.Result a){
        if(a==null||a.steps.size()<2)return null;
        AdaptiveRefinementStudy.Step p=a.steps.get(a.steps.size()-2),c=a.steps.get(a.steps.size()-1);
        Stats ps=stats(p.mesh,p.fem),cs=stats(c.mesh,c.fem);
        double c95=rel(ps.p95Pa,cs.p95Pa),c99=rel(ps.p99Pa,cs.p99Pa);
        double shift=dist(ps.highStressCentroid,cs.highStressCentroid);
        double diag=diag(c.mesh);
        boolean location=shift<=Math.max(diag*0.06,1e-9);
        boolean ok=c95<=0.08&&c99<=0.12&&location;
        double rawChange=rel(ps.maxPa,cs.maxPa);
        boolean isolated=ok&&rawChange>0.18&&cs.maxPa>cs.p99Pa*1.20;
        return new Result(ps,cs,c95,c99,shift,ok,isolated);
    }

    public static Stats stats(TetMeshData mesh,StaticFemSolver.Result fem){
        double[] v=fem.elementVonMisesPa.clone();
        if(v.length==0)return new Stats(0,0,0,new MeshModel.V3(0,0,0),0);
        Arrays.sort(v);
        double p95=percentile(v,0.95),p99=percentile(v,0.99),max=v[v.length-1];
        double threshold=p95;
        double wx=0,wy=0,wz=0,ws=0;int n=0;
        for(int i=0;i<fem.elementVonMisesPa.length;i++){
            double s=fem.elementVonMisesPa[i];if(s<threshold)continue;
            int[] t=mesh.tets.get(i);double x=0,y=0,z=0;
            for(int ni:t){MeshModel.V3 q=mesh.nodes.get(ni);x+=q.x;y+=q.y;z+=q.z;}
            x/=4;y/=4;z/=4;double w=Math.max(s,1e-30);
            wx+=w*x;wy+=w*y;wz+=w*z;ws+=w;n++;
        }
        MeshModel.V3 cent=ws>0?new MeshModel.V3(wx/ws,wy/ws,wz/ws):new MeshModel.V3(0,0,0);
        return new Stats(p95,p99,max,cent,n);
    }
    private static double percentile(double[] a,double q){if(a.length==1)return a[0];double x=q*(a.length-1);int i=(int)Math.floor(x),j=Math.min(i+1,a.length-1);double f=x-i;return a[i]*(1-f)+a[j]*f;}
    private static double rel(double a,double b){double d=Math.max(Math.max(Math.abs(a),Math.abs(b)),1e-30);return Math.abs(a-b)/d;}
    private static double dist(MeshModel.V3 a,MeshModel.V3 b){return Math.sqrt(sq(a.x-b.x)+sq(a.y-b.y)+sq(a.z-b.z));}
    private static double diag(TetMeshData m){double x0=1e99,y0=1e99,z0=1e99,x1=-1e99,y1=-1e99,z1=-1e99;for(MeshModel.V3 p:m.nodes){x0=Math.min(x0,p.x);y0=Math.min(y0,p.y);z0=Math.min(z0,p.z);x1=Math.max(x1,p.x);y1=Math.max(y1,p.y);z1=Math.max(z1,p.z);}return Math.sqrt(sq(x1-x0)+sq(y1-y0)+sq(z1-z0));}
    private static double sq(double x){return x*x;}
}
