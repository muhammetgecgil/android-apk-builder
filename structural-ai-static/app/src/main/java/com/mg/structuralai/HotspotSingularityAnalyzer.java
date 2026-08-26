package com.mg.structuralai;

import java.util.Locale;

/** Mesh-trend classifier for peak stress credibility. */
public final class HotspotSingularityAnalyzer {
    public enum Type { CONVERGED_HOTSPOT, POSSIBLE_SINGULARITY, UNRESOLVED }
    public static final class Result {
        public final Type type;
        public final double coarsePa,mediumPa,finePa;
        public final double growthCM,growthMF;
        public final String explanation;
        Result(Type t,double a,double b,double c,double g1,double g2,String e){type=t;coarsePa=a;mediumPa=b;finePa=c;growthCM=g1;growthMF=g2;explanation=e;}
    }
    private HotspotSingularityAnalyzer(){}

    public static Result analyze(MeshConvergenceStudy.Result cv){
        double a=cv.coarse.fem.maxVonMisesPa,b=cv.medium.fem.maxVonMisesPa,c=cv.fine.fem.maxVonMisesPa;
        double g1=growth(a,b),g2=growth(b,c);
        Type type; String why;
        double scale=Math.max(Math.max(Math.abs(a),Math.abs(b)),Math.abs(c));
        if(!Double.isFinite(scale) || scale<1e-6){
            type=Type.UNRESOLVED;
            why="Peak stress is effectively zero; this is not evidence of convergence. Load transfer, constraints and solver response must be validated first.";
        }else if(cv.stressChange<=0.10 && Math.abs(g2)<=0.12){
            type=Type.CONVERGED_HOTSPOT;
            why="Peak stress is stabilizing under h-refinement; treat as a credible hotspot candidate.";
        }else if(c>b && b>a && g2>=g1*0.80 && g2>0.15){
            type=Type.POSSIBLE_SINGULARITY;
            why="Peak stress rises persistently as the mesh is refined; the raw maximum may be singularity-driven and must not be used as a converged allowable check.";
        }else{
            type=Type.UNRESOLVED;
            why="Peak stress trend is not sufficiently stable to classify. Additional local/adaptive refinement is required.";
        }
        return new Result(type,a,b,c,g1,g2,why);
    }

    public static String summary(Result r){return String.format(Locale.US,
        "%s | sigma: %.4g -> %.4g -> %.4g MPa | growth %.1f%% / %.1f%% | %s",
        r.type,r.coarsePa/1e6,r.mediumPa/1e6,r.finePa/1e6,r.growthCM*100,r.growthMF*100,r.explanation);}

    private static double growth(double x,double y){return (y-x)/Math.max(Math.abs(x),1e-30);}
}
