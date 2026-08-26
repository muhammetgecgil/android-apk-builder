package com.mg.structuralai;

import java.util.*;

/**
 * Conservative topology signature classifier for single-loop prismatic open-section solids.
 * Uses normalized section occupancy and tests rotations/mirrors. It only returns a named profile
 * when score and separation are both strong; otherwise UNKNOWN is retained.
 */
public final class OpenSectionTopologyClassifier {
    public enum Type { I_SECTION, C_SECTION, T_SECTION, L_SECTION, UNKNOWN }
    public static final class Result {
        public final Type type; public final double confidence, score, margin; public final String reason;
        Result(Type t,double c,double s,double m,String r){type=t;confidence=c;score=s;margin=m;reason=r;}
    }
    private static final int N=21;
    private OpenSectionTopologyClassifier(){}

    public static Result classify(CrossSectionAnalyzer.Result cs){
        if(cs==null||!cs.closed||cs.loops.size()!=1)return new Result(Type.UNKNOWN,0,0,0,"Named open-section profile requires one proven closed material contour at mid-span.");
        boolean[][] occ=raster(cs.loops.get(0).p,N);
        Candidate best=null,second=null;
        for(Type t:new Type[]{Type.I_SECTION,Type.C_SECTION,Type.T_SECTION,Type.L_SECTION}){
            double s=bestSymmetryScore(occ,t);
            Candidate c=new Candidate(t,s);
            if(best==null||s>best.score){second=best;best=c;}else if(second==null||s>second.score)second=c;
        }
        double margin=best.score-(second==null?0:second.score);
        boolean accept=best.score>=0.80&&margin>=0.06;
        if(!accept)return new Result(Type.UNKNOWN,Math.max(0,Math.min(.65,best.score*.7)),best.score,margin,
            "Section signature not separated strongly enough for I/C/T/L classification; retain general section path.");
        double conf=Math.min(.96,.72+.20*(best.score-.80)/.20+.04*Math.min(1,margin/.15));
        return new Result(best.type,conf,best.score,margin,"Normalized mid-span occupancy strongly matches "+best.type+" under rotation/mirror-invariant comparison.");
    }
    private static final class Candidate{Type type;double score;Candidate(Type t,double s){type=t;score=s;}}

    private static double bestSymmetryScore(boolean[][] src,Type t){
        double best=0; boolean[][] a=src;
        for(int r=0;r<4;r++){best=Math.max(best,score(a,t));best=Math.max(best,score(mirror(a),t));a=rotate(a);}return best;
    }
    private static double score(boolean[][] a,Type t){
        int hit=0,uni=0;for(int y=0;y<N;y++)for(int x=0;x<N;x++){
            boolean m=mask(x,y,t);if(a[y][x]||m)uni++;if(a[y][x]&&m)hit++;
        }
        if(uni==0)return 0;double iou=(double)hit/uni;
        double fill=fillRatio(a),target=targetFill(t);double fillPenalty=Math.min(1,Math.abs(fill-target)/Math.max(target,.05));
        return Math.max(0,iou*(1-.25*fillPenalty));
    }
    private static boolean mask(int x,int y,Type t){
        double X=(x+.5)/N,Y=(y+.5)/N;double th=.18;
        switch(t){
            case I_SECTION:return Y<th||Y>1-th||Math.abs(X-.5)<th/2;
            case C_SECTION:return X<th||Y<th||Y>1-th;
            case T_SECTION:return Y<th||Math.abs(X-.5)<th/2;
            case L_SECTION:return X<th||Y>1-th;
            default:return false;
        }
    }
    private static double targetFill(Type t){return t==Type.I_SECTION?.40:t==Type.C_SECTION?.45:t==Type.T_SECTION?.29:.33;}
    private static double fillRatio(boolean[][] a){int c=0;for(boolean[] r:a)for(boolean v:r)if(v)c++;return (double)c/(N*N);}
    private static boolean[][] raster(List<double[]> p,int n){
        double x0=1e99,y0=1e99,x1=-1e99,y1=-1e99;for(double[] q:p){x0=Math.min(x0,q[0]);y0=Math.min(y0,q[1]);x1=Math.max(x1,q[0]);y1=Math.max(y1,q[1]);}
        boolean[][] a=new boolean[n][n];double dx=Math.max(x1-x0,1e-30),dy=Math.max(y1-y0,1e-30);
        for(int j=0;j<n;j++)for(int i=0;i<n;i++){double x=x0+(i+.5)*dx/n,y=y0+(j+.5)*dy/n;a[j][i]=inside(p,x,y);}return a;
    }
    private static boolean inside(List<double[]> p,double x,double y){boolean c=false;int n=p.size();for(int i=0,j=n-1;i<n;j=i++){
        double[] pi=p.get(i),pj=p.get(j);boolean cross=((pi[1]>y)!=(pj[1]>y))&&(x<(pj[0]-pi[0])*(y-pi[1])/(pj[1]-pi[1]+1e-300)+pi[0]);if(cross)c=!c;}return c;}
    private static boolean[][] rotate(boolean[][] a){boolean[][] b=new boolean[N][N];for(int y=0;y<N;y++)for(int x=0;x<N;x++)b[x][N-1-y]=a[y][x];return b;}
    private static boolean[][] mirror(boolean[][] a){boolean[][] b=new boolean[N][N];for(int y=0;y<N;y++)for(int x=0;x<N;x++)b[y][N-1-x]=a[y][x];return b;}
}
