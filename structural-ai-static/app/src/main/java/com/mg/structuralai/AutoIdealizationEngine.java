package com.mg.structuralai;

import java.util.Locale;

/** Conservative automatic beam/solid idealization driven by proven mid-span section topology. */
public final class AutoIdealizationEngine {
    public enum Type { RECTANGULAR_BEAM, PROFILE_BEAM, SOLID_3D }
    public static final class Result {
        public final Type type; public final boolean applicable; public final double confidence;
        public final int majorAxis,loadAxis; public final double lengthM,widthM,depthM,areaM2,inertiaM4;
        public final double displacementM,maxBendingStressPa,maxShearStressPa; public final String explanation;
        public final SectionProfileClassifier.Type profileType;
        Result(Type t,boolean a,double c,int major,int load,double L,double w,double d,double A,double I,double u,double sb,double ts,SectionProfileClassifier.Type pt,String why){
            type=t;applicable=a;confidence=c;majorAxis=major;loadAxis=load;lengthM=L;widthM=w;depthM=d;areaM2=A;inertiaM4=I;displacementM=u;maxBendingStressPa=sb;maxShearStressPa=ts;profileType=pt;explanation=why;
        }
        public String summary(){
            if(type==Type.SOLID_3D)return explanation;
            String shear=Double.isFinite(maxShearStressPa)?String.format(Locale.US,"%.9g MPa",maxShearStressPa/1e6):"not resolved";
            return String.format(Locale.US,"type=%s | profile=%s | confidence=%.0f%% | L=%.6g mm | bbox section=%.6g x %.6g mm | A=%.6g mm^2 | I(load-axis bending)=%.6g mm^4 | U(1N)=%.9g mm | bending sigma(1N)=%.9g MPa | shear=%s\n%s",
                type,profileType,confidence*100,lengthM*1000,widthM*1000,depthM*1000,areaM2*1e6,inertiaM4*1e12,displacementM*1000,maxBendingStressPa/1e6,shear,explanation);
        }
    }
    private AutoIdealizationEngine(){}

    public static Result analyze(MeshModel m,AutonomousAnalysisPlanner.Plan p,double fx,double fy,double fz){
        double[] d={m.dx(),m.dy(),m.dz()};int major=0;if(d[1]>d[major])major=1;if(d[2]>d[major])major=2;
        double longest=d[major],second=0;for(int i=0;i<3;i++)if(i!=major)second=Math.max(second,d[i]);
        boolean slender=longest>=3.0*Math.max(second,1e-12);
        int load=largestAbsAxis(fx,fy,fz);boolean transverse=load!=major&&(Math.abs(fx)+Math.abs(fy)+Math.abs(fz))>0;
        SectionProfileClassifier.Result profile=SectionProfileClassifier.classify(m);
        SectionProperties.Result sp=SectionProperties.compute(profile.section);
        if(!(slender&&transverse&&profile.type!=SectionProfileClassifier.Type.GENERAL_SOLID&&profile.confidence>=0.80&&sp.valid))
            return solid(major,load,"3D solid retained: a trustworthy slender section-beam idealization was not proven.");

        double scale=p.unitScaleM,L=d[major]*scale;
        int coord=projectedLoadCoordinate(major,load);
        if(coord<0)return solid(major,load,"3D solid retained: load is not transverse to the extracted beam axis.");
        double A=sp.area*scale*scale;
        double Iraw=coord==0?sp.Iyy:sp.Ixx;
        double I=Iraw*Math.pow(scale,4);
        double centroid=coord==0?sp.cx:sp.cy;
        double cRaw=maxFiberDistance(profile.section,coord,centroid);
        double c=cRaw*scale;
        if(!(L>0&&A>0&&I>0&&c>0))return solid(major,load,"3D solid retained: extracted section properties are invalid for bending.");

        double F=Math.sqrt(fx*fx+fy*fy+fz*fz),E=p.material.youngPa;
        double bending=F*L*L*L/(3.0*E*I);
        double M=F*L,sigma=M*c/I;
        double shear=Double.NaN, u=bending;
        Type type=profile.type==SectionProfileClassifier.Type.RECTANGULAR_SOLID?Type.RECTANGULAR_BEAM:Type.PROFILE_BEAM;
        boolean torsionSensitive=profile.type==SectionProfileClassifier.Type.C_SECTION||profile.type==SectionProfileClassifier.Type.T_SECTION||profile.type==SectionProfileClassifier.Type.L_SECTION;
        boolean symmetricAccepted=profile.type==SectionProfileClassifier.Type.RECTANGULAR_SOLID||profile.type==SectionProfileClassifier.Type.BOX||profile.type==SectionProfileClassifier.Type.PIPE||profile.type==SectionProfileClassifier.Type.I_SECTION;
        double conf=Math.min(0.97,profile.confidence*(symmetricAccepted?1.0:0.82));

        String shearNote;
        if(profile.type==SectionProfileClassifier.Type.RECTANGULAR_SOLID){
            double nu=p.material.poisson,G=E/(2.0*(1.0+nu)),kappa=5.0/6.0;
            double us=F*L/(kappa*G*A);u+=us;shear=1.5*F/A;
            shearNote="Rectangular section: Timoshenko shear-deflection correction included.";
        }else{
            shearNote="General profile: primary coefficient uses Euler-Bernoulli bending; detailed shear correction is withheld until section shear-flow properties are resolved.";
        }

        String why="Mid-span section topology proved "+profile.type+". Bending uses extracted polygon properties rather than bounding-box inertia. "+shearNote;
        if(torsionSensitive)why+=" Shear-center is unresolved for this open asymmetric/singly-symmetric profile, so this bending influence result is advisory and cannot be the autonomous primary PASS model yet.";
        else if(profile.type==SectionProfileClassifier.Type.I_SECTION)why+=" I-section signature is strongly symmetric; bending idealization is accepted for the current transverse influence case, while torsion remains separately gated.";
        else why+=" Section symmetry supports centroidal transverse bending for the current influence study; torsion remains separately gated.";

        boolean applicable=symmetricAccepted&&!torsionSensitive;
        return new Result(type,applicable,conf,major,load,L,sectionDim(m,major,0)*scale,sectionDim(m,major,1)*scale,A,I,u,sigma,shear,profile.type,why);
    }

    private static Result solid(int major,int load,String why){return new Result(Type.SOLID_3D,false,0,major,load,0,0,0,0,0,0,0,0,SectionProfileClassifier.Type.GENERAL_SOLID,why);}
    private static int largestAbsAxis(double x,double y,double z){double[] a={Math.abs(x),Math.abs(y),Math.abs(z)};int i=0;if(a[1]>a[i])i=1;if(a[2]>a[i])i=2;return i;}
    private static int projectedLoadCoordinate(int major,int load){
        if(major==0){if(load==1)return 0;if(load==2)return 1;}
        if(major==1){if(load==0)return 0;if(load==2)return 1;}
        if(major==2){if(load==0)return 0;if(load==1)return 1;}
        return -1;
    }
    private static double maxFiberDistance(CrossSectionAnalyzer.Result s,int coord,double centroid){double c=0;for(CrossSectionAnalyzer.Loop l:s.loops)for(double[] q:l.p)c=Math.max(c,Math.abs(q[coord]-centroid));return c;}
    private static double sectionDim(MeshModel m,int major,int which){
        if(major==0)return which==0?m.dy():m.dz();
        if(major==1)return which==0?m.dx():m.dz();
        return which==0?m.dx():m.dy();
    }
}
