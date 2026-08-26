package com.mg.structuralai;

/** Conservative section-family classifier for autonomous idealization. */
public final class SectionProfileClassifier {
    public enum Type { RECTANGULAR_SOLID, BOX, PIPE, I_SECTION, C_SECTION, T_SECTION, L_SECTION, GENERAL_SOLID }
    public static final class Result {
        public final Type type; public final double confidence; public final String reason;
        Result(Type t,double c,String r){type=t;confidence=c;reason=r;}
    }
    private SectionProfileClassifier(){}

    public static Result classify(MeshModel m){
        GeometryFeatureDetector.FeatureSet f=GeometryFeatureDetector.detect(m);
        double[] d={m.dx(),m.dy(),m.dz()}; int major=0;if(d[1]>d[major])major=1;if(d[2]>d[major])major=2;
        double longest=d[major], second=0; for(int i=0;i<3;i++)if(i!=major)second=Math.max(second,d[i]);
        boolean slender=longest>=3.0*Math.max(second,1e-12);
        if(slender && f.planarMountCandidates.size()==6 && f.circularHoleCandidates.isEmpty() && f.flangeCandidates.isEmpty())
            return new Result(Type.RECTANGULAR_SOLID,0.92,"Six merged planar faces with slender dominant axis; no hole/flange evidence.");
        return new Result(Type.GENERAL_SOLID,0.30,"Profile family not proven from current tessellation evidence; retain general 3D solid path.");
    }
}
