package com.mg.structuralai;

import java.util.Locale;

/**
 * Conservative automatic idealization. At this stage only a strong rectangular-prism beam case
 * is accepted. General thin-wall shell and arbitrary beam sections remain candidates, not claims.
 */
public final class AutoIdealizationEngine {
    public enum Type { RECTANGULAR_BEAM, SOLID_3D }
    public static final class Result {
        public final Type type;
        public final boolean applicable;
        public final double confidence;
        public final int majorAxis, loadAxis;
        public final double lengthM,widthM,depthM,areaM2,inertiaM4;
        public final double displacementM,maxBendingStressPa,maxShearStressPa;
        public final String explanation;
        Result(Type t,boolean a,double c,int major,int load,double L,double w,double d,double A,double I,double u,double sb,double ts,String why){
            type=t;applicable=a;confidence=c;majorAxis=major;loadAxis=load;lengthM=L;widthM=w;depthM=d;areaM2=A;inertiaM4=I;
            displacementM=u;maxBendingStressPa=sb;maxShearStressPa=ts;explanation=why;
        }
        public String summary(){
            if(!applicable)return explanation;
            return String.format(Locale.US,
                "type=%s | confidence=%.0f%% | L=%.6g mm | section=%.6g x %.6g mm | A=%.6g mm^2 | I=%.6g mm^4 | U(1N)=%.9g mm | bending sigma(1N)=%.9g MPa | shear tau_max(1N)=%.9g MPa",
                type,confidence*100,lengthM*1000,widthM*1000,depthM*1000,areaM2*1e6,inertiaM4*1e12,
                displacementM*1000,maxBendingStressPa/1e6,maxShearStressPa/1e6);
        }
    }
    private AutoIdealizationEngine(){}

    public static Result analyze(MeshModel m,AutonomousAnalysisPlanner.Plan p,double fx,double fy,double fz){
        GeometryFeatureDetector.FeatureSet fs=GeometryFeatureDetector.detect(m);
        double[] d={m.dx(),m.dy(),m.dz()};
        int major=0;if(d[1]>d[major])major=1;if(d[2]>d[major])major=2;
        double longest=d[major], second=0,small=1e300;
        for(int i=0;i<3;i++)if(i!=major){second=Math.max(second,d[i]);small=Math.min(small,d[i]);}
        boolean slender=longest>=3.0*Math.max(second,1e-12);
        boolean rectangularEvidence=fs.planarPatches.size()==6 && fs.circularHoleCandidates.isEmpty() && fs.flangeCandidates.isEmpty();
        int load=largestAbsAxis(fx,fy,fz);
        boolean transverse=load!=major && (Math.abs(fx)+Math.abs(fy)+Math.abs(fz))>0;
        if(!(slender&&rectangularEvidence&&transverse)){
            return new Result(Type.SOLID_3D,false,0.0,major,load,0,0,0,0,0,0,0,0,
                "3D solid retained: conservative rectangular-beam evidence was not sufficient.");
        }

        int other=3-major-load; // valid because axes are 0,1,2 and major != load
        double scale=p.unitScaleM;
        double L=d[major]*scale;
        double depth=d[load]*scale; // bending depth lies along force direction
        double width=d[other]*scale;
        double A=width*depth;
        double I=width*depth*depth*depth/12.0;
        if(!(L>0&&width>0&&depth>0&&I>0))throw new IllegalStateException("Beam idealization dimensions invalid");
        double F=Math.sqrt(fx*fx+fy*fy+fz*fz);
        double E=p.material.youngPa,nu=p.material.poisson;
        double G=E/(2.0*(1.0+nu));
        double kappa=5.0/6.0; // rectangular section
        double bending=F*L*L*L/(3.0*E*I);
        double shear=F*L/(kappa*G*A);
        double u=bending+shear;
        double M=F*L;
        double sigma=M*(depth/2.0)/I;
        double tau=1.5*F/A;
        double aspect=longest/Math.max(second,1e-12);
        double confidence=Math.min(0.98,0.86+Math.min(0.10,(aspect-3.0)*0.02));
        String why="Strong rectangular-prism beam evidence: six merged planar faces, no hole/flange evidence, dominant-axis slenderness, transverse unit load. Euler-Bernoulli bending plus Timoshenko shear correction used. Bounding-box section is accepted only because the six-face rectangular evidence passed.";
        return new Result(Type.RECTANGULAR_BEAM,true,confidence,major,load,L,width,depth,A,I,u,sigma,tau,why);
    }

    private static int largestAbsAxis(double x,double y,double z){double[] a={Math.abs(x),Math.abs(y),Math.abs(z)};int i=0;if(a[1]>a[i])i=1;if(a[2]>a[i])i=2;return i;}
}
