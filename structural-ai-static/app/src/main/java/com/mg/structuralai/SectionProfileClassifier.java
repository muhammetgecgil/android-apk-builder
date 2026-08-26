package com.mg.structuralai;

/** Conservative section-family classifier driven by extracted mid-span section contours. */
public final class SectionProfileClassifier {
    public enum Type { RECTANGULAR_SOLID, BOX, PIPE, I_SECTION, C_SECTION, T_SECTION, L_SECTION, GENERAL_SOLID }
    public static final class Result {
        public final Type type; public final double confidence; public final String reason; public final CrossSectionAnalyzer.Result section;
        Result(Type t,double c,String r,CrossSectionAnalyzer.Result s){type=t;confidence=c;reason=r;section=s;}
    }
    private SectionProfileClassifier(){}

    public static Result classify(MeshModel m){
        GeometryFeatureDetector.FeatureSet f=GeometryFeatureDetector.detect(m);
        CrossSectionAnalyzer.Result cs=CrossSectionAnalyzer.analyze(m);
        double[] d={m.dx(),m.dy(),m.dz()}; int major=0;if(d[1]>d[major])major=1;if(d[2]>d[major])major=2;
        double longest=d[major],second=0;for(int i=0;i<3;i++)if(i!=major)second=Math.max(second,d[i]);
        boolean slender=longest>=3.0*Math.max(second,1e-12);

        if(slender&&cs.closed&&cs.loops.size()==2){
            CrossSectionAnalyzer.Loop outer=cs.loops.get(0),inner=cs.loops.get(1);
            double ratio=inner.area/Math.max(outer.area,1e-30);
            if(outer.circularity>0.88&&inner.circularity>0.88&&ratio>0.05&&ratio<0.95)
                return new Result(Type.PIPE,0.91,"Two closed concentric-like section contours with strong circularity evidence; hollow circular section proven by actual slice loops.",cs);
            if(outer.circularity<0.86&&inner.circularity<0.86&&ratio>0.05&&ratio<0.95)
                return new Result(Type.BOX,0.84,"Two closed non-circular section contours prove a hollow prismatic section; treated as box/tube candidate pending corner rectangularity check.",cs);
        }

        if(slender&&cs.closed&&cs.loops.size()==1&&f.planarMountCandidates.size()==6&&f.circularHoleCandidates.isEmpty()&&f.flangeCandidates.isEmpty())
            return new Result(Type.RECTANGULAR_SOLID,0.94,"Single closed section contour plus six merged planar faces and slender dominant axis prove a rectangular solid beam.",cs);

        return new Result(Type.GENERAL_SOLID,0.30,"Profile family not proven from extracted section topology; retain general 3D solid path. I/C/T/L classification requires stronger contour-corner evidence.",cs);
    }
}
