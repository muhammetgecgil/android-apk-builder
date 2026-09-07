package com.mg.machineelementspro;

import java.util.ArrayList;
import java.util.List;

public final class SystemSelectionEngine {
    public static final class ShaftBearingFitResult {
        public final double shaftRequiredMm;
        public final double shaftPreferredMm;
        public final String bearing;
        public final double bearingLifeHours;
        public final double bearingStaticFoS;
        public final String shaftFit;
        public final String housingFit;
        ShaftBearingFitResult(double sr,double sp,String b,double life,double s0,String sf,String hf){shaftRequiredMm=sr;shaftPreferredMm=sp;bearing=b;bearingLifeHours=life;bearingStaticFoS=s0;shaftFit=sf;housingFit=hf;}
    }

    public static final class TighteningResult {
        public final double nominalPreloadN;
        public final double preloadMinN;
        public final double preloadMaxN;
        public final double torqueMinNm;
        public final double torqueNominalNm;
        public final double torqueMaxNm;
        TighteningResult(double fn,double fmin,double fmax,double tmin,double t,double tmax){nominalPreloadN=fn;preloadMinN=fmin;preloadMaxN=fmax;torqueMinNm=tmin;torqueNominalNm=t;torqueMaxNm=tmax;}
    }

    public static final class ParetoOption {
        public final String material;
        public final double diameterMm;
        public final double massKgPerM;
        public final double safetyFactor;
        public final double normalizedScore;
        ParetoOption(String m,double d,double kg,double sf,double score){material=m;diameterMm=d;massKgPerM=kg;safetyFactor=sf;normalizedScore=score;}
    }

    public static ShaftBearingFitResult solveShaftBearingFit(double bendingNm,double torqueNm,double syMpa,double targetShaftFoS,double radialN,double axialN,double rpm,double targetHours,double requiredStaticFoS){
        double req=EngineeringLibrary.requiredSolidShaftDiameter(bendingNm,torqueNm,syMpa,targetShaftFoS);
        double pref=EngineeringLibrary.selectPreferredShaft(req);
        DesignOptimizationEngine.BearingPick b=DesignOptimizationEngine.selectBearing(radialN,axialN,rpm,targetHours,requiredStaticFoS);
        if("OUT-OF-RANGE".equals(b.designation)) return new ShaftBearingFitResult(req,pref,b.designation,0,0,"-","-");
        String shaftFit="k6";
        String housingFit="H7";
        if(axialN>0.35*radialN) shaftFit="m6";
        if(radialN<1000) shaftFit="h6";
        return new ShaftBearingFitResult(req,pref,b.designation,b.lifeHours,b.staticFoS,shaftFit,housingFit);
    }

    public static TighteningResult tighteningScatter(double nominalDiameterMm,double targetPreloadN,double nutFactorNominal,double preloadScatterPercent,double nutFactorScatterPercent){
        if(nominalDiameterMm<=0||targetPreloadN<=0||nutFactorNominal<=0||preloadScatterPercent<0||nutFactorScatterPercent<0) throw new IllegalArgumentException("Invalid tightening inputs");
        double fp=preloadScatterPercent/100.0, kp=nutFactorScatterPercent/100.0;
        double fmin=targetPreloadN*(1.0-fp), fmax=targetPreloadN*(1.0+fp);
        double kmin=nutFactorNominal*(1.0-kp), kmax=nutFactorNominal*(1.0+kp);
        double d=nominalDiameterMm/1000.0;
        double tNom=nutFactorNominal*targetPreloadN*d;
        double tMin=kmin*fmin*d;
        double tMax=kmax*fmax*d;
        return new TighteningResult(targetPreloadN,fmin,fmax,tMin,tNom,tMax);
    }

    public static List<ParetoOption> paretoShaftOptions(double bendingNm,double torqueNm,double targetFoS){
        String[] names={"AISI 1045","AISI 4140","7075-T6","Ti-6Al-4V"};
        double[] sy={530,655,503,880};
        double[] rho={7850,7850,2810,4430};
        List<ParetoOption> all=new ArrayList<>();
        double minMass=Double.POSITIVE_INFINITY,maxMass=0,minD=Double.POSITIVE_INFINITY,maxD=0;
        for(int i=0;i<names.length;i++){
            double req=EngineeringLibrary.requiredSolidShaftDiameter(bendingNm,torqueNm,sy[i],targetFoS);
            double d=EngineeringLibrary.selectPreferredShaft(req);
            double kg=rho[i]*Math.PI*Math.pow(d/1000.0,2)/4.0;
            double eq=Math.sqrt(Math.pow(32*bendingNm*1000/Math.PI,2)+3*Math.pow(16*torqueNm*1000/Math.PI,2));
            double vm=eq/Math.pow(d,3);
            double sf=sy[i]/vm;
            minMass=Math.min(minMass,kg);maxMass=Math.max(maxMass,kg);minD=Math.min(minD,d);maxD=Math.max(maxD,d);
            all.add(new ParetoOption(names[i],d,kg,sf,0));
        }
        List<ParetoOption> out=new ArrayList<>();
        for(ParetoOption o:all){
            double massNorm=(o.massKgPerM-minMass)/Math.max(1e-9,maxMass-minMass);
            double dNorm=(o.diameterMm-minD)/Math.max(1e-9,maxD-minD);
            out.add(new ParetoOption(o.material,o.diameterMm,o.massKgPerM,o.safetyFactor,0.65*massNorm+0.35*dNorm));
        }
        out.sort((a,b)->Double.compare(a.normalizedScore,b.normalizedScore));
        return out;
    }

    private SystemSelectionEngine(){}
}
