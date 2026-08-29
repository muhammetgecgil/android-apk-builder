package com.mg.structuralai;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Regression lock for the data contract used by the surface-mesh and filled-contour viewport. */
public final class VisualEvidenceRegressionGate {
    public static final class Result {
        public final boolean pass;
        public final String summary;
        Result(boolean p,String s){pass=p;summary=s;}
    }
    private VisualEvidenceRegressionGate(){}

    public static Result run(TetMeshData mesh, StaticFemSolver.Result r){
        try{
            if(mesh==null||r==null)return new Result(false,"VISUAL EVIDENCE REGRESSION FAIL | null mesh/result");
            int n=mesh.nodes.size(), e=mesh.tets.size();
            boolean shape=r.displacement!=null&&r.displacement.length==3*n
                    &&r.reactions!=null&&r.reactions.length==3*n
                    &&r.elementVonMisesPa!=null&&r.elementVonMisesPa.length==e;
            boolean finite=shape;
            double uMin=Double.POSITIVE_INFINITY,uMax=Double.NEGATIVE_INFINITY;
            double vmMin=Double.POSITIVE_INFINITY,vmMax=Double.NEGATIVE_INFINITY;
            if(shape){
                for(int i=0;i<n;i++){
                    double ux=r.displacement[3*i],uy=r.displacement[3*i+1],uz=r.displacement[3*i+2];
                    double u=Math.sqrt(ux*ux+uy*uy+uz*uz);
                    if(!Double.isFinite(u)){finite=false;break;}
                    if(u<uMin)uMin=u;if(u>uMax)uMax=u;
                }
                for(int i=0;i<e&&finite;i++){
                    double vm=r.elementVonMisesPa[i];
                    if(!Double.isFinite(vm)||vm<0){finite=false;break;}
                    if(vm<vmMin)vmMin=vm;if(vm>vmMax)vmMax=vm;
                }
            }
            int boundaryFaces=countBoundaryFaces(mesh);
            boolean surface=boundaryFaces>0&&boundaryFaces<=4*e;
            boolean extrema=Double.isFinite(uMin)&&Double.isFinite(uMax)&&uMax>=uMin&&Double.isFinite(vmMin)&&Double.isFinite(vmMax)&&vmMax>=vmMin;
            boolean contour=normalizationContract(vmMin,vmMax)&&normalizationContract(uMin,uMax);
            boolean tensor=true;
            if(e>0&&r.material!=null){
                int[] t=mesh.tets.get(0);double[] ue=new double[12];
                for(int a=0;a<4;a++)for(int k=0;k<3;k++)ue[3*a+k]=r.displacement[3*t[a]+k];
                double[] st=Tet4Element.stress(mesh.nodes.get(t[0]),mesh.nodes.get(t[1]),mesh.nodes.get(t[2]),mesh.nodes.get(t[3]),r.material,ue);
                double[] ps=Tet4Element.principalValues(st,false);
                tensor=ps!=null&&ps.length==3&&Double.isFinite(ps[0])&&Double.isFinite(ps[1])&&Double.isFinite(ps[2])&&ps[0]>=ps[1]&&ps[1]>=ps[2];
            }
            boolean pass=shape&&finite&&surface&&extrema&&contour&&tensor;
            return new Result(pass,String.format(Locale.US,
                    "VISUAL EVIDENCE REGRESSION %s | nodes=%d TET4=%d boundaryFaces=%d | U=[%.6g,%.6g] mm | VM=[%.6g,%.6g] MPa | shape=%s finite=%s surface=%s contour=%s tensor=%s",
                    pass?"PASS":"FAIL",n,e,boundaryFaces,uMin*1000,uMax*1000,vmMin/1e6,vmMax/1e6,shape,finite,surface,contour,tensor));
        }catch(Throwable t){return new Result(false,"VISUAL EVIDENCE REGRESSION EXCEPTION: "+t.getMessage());}
    }

    private static boolean normalizationContract(double min,double max){
        if(!Double.isFinite(min)||!Double.isFinite(max)||max<min)return false;
        double range=Math.max(max-min,1e-30);
        double q0=Math.max(0,Math.min(1,(min-min)/range));
        double q1=Math.max(0,Math.min(1,(max-min)/range));
        return Double.isFinite(q0)&&Double.isFinite(q1)&&Math.abs(q0)<=1e-12&&q1>=0&&q1<=1.0+1e-12;
    }

    private static int countBoundaryFaces(TetMeshData mesh){
        Map<String,Integer> counts=new HashMap<>();
        for(int[] t:mesh.tets){
            if(t==null||t.length<4)continue;
            add(counts,t[0],t[1],t[2]);add(counts,t[0],t[1],t[3]);add(counts,t[0],t[2],t[3]);add(counts,t[1],t[2],t[3]);
        }
        int n=0;for(int c:counts.values())if(c==1)n++;return n;
    }
    private static void add(Map<String,Integer> m,int a,int b,int c){
        int x=a,y=b,z=c;if(x>y){int q=x;x=y;y=q;}if(y>z){int q=y;y=z;z=q;}if(x>y){int q=x;x=y;y=q;}
        String k=x+":"+y+":"+z;m.put(k,m.containsKey(k)?m.get(k)+1:1);
    }
}
