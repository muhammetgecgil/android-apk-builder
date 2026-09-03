package com.mg.machineelementspro;

public final class GearboxDesignEngine {
    public static final class GearboxResult {
        public final double ratio;
        public final double outputTorqueNm;
        public final double tangentialForceN;
        public final double radialForceN;
        public final double pinionPitchDiameterMm;
        public final double gearPitchDiameterMm;
        public final double shaft1RequiredMm;
        public final double shaft2RequiredMm;
        public final double shaft1PreferredMm;
        public final double shaft2PreferredMm;
        public final DesignOptimizationEngine.BearingPick bearing1;
        public final DesignOptimizationEngine.BearingPick bearing2;
        public final double gearBendingStressMpa;
        public final double gearSafetyFactor;

        GearboxResult(double ratio,double outputTorqueNm,double ft,double fr,double dp,double dg,
                      double s1,double s2,double p1,double p2,
                      DesignOptimizationEngine.BearingPick b1,DesignOptimizationEngine.BearingPick b2,
                      double sigma,double sf){
            this.ratio=ratio;this.outputTorqueNm=outputTorqueNm;this.tangentialForceN=ft;this.radialForceN=fr;
            this.pinionPitchDiameterMm=dp;this.gearPitchDiameterMm=dg;this.shaft1RequiredMm=s1;this.shaft2RequiredMm=s2;
            this.shaft1PreferredMm=p1;this.shaft2PreferredMm=p2;this.bearing1=b1;this.bearing2=b2;
            this.gearBendingStressMpa=sigma;this.gearSafetyFactor=sf;
        }
    }

    public static GearboxResult sizeSingleStage(double inputTorqueNm,double inputRpm,int pinionTeeth,int gearTeeth,
                                                 double moduleMm,double faceWidthMm,double pressureAngleDeg,
                                                 double efficiency,double shaftSyMpa,double shaftTargetFoS,
                                                 double gearAllowableBendingMpa,double targetLifeHours){
        if(inputTorqueNm<=0||inputRpm<=0||pinionTeeth<12||gearTeeth<=pinionTeeth||moduleMm<=0||faceWidthMm<=0||
           efficiency<=0||efficiency>1||shaftSyMpa<=0||shaftTargetFoS<=0||gearAllowableBendingMpa<=0||targetLifeHours<=0)
            throw new IllegalArgumentException("Invalid gearbox inputs");

        double ratio=(double)gearTeeth/pinionTeeth;
        double outTorque=inputTorqueNm*ratio*efficiency;
        double dp=moduleMm*pinionTeeth;
        double dg=moduleMm*gearTeeth;
        double ft=2.0*inputTorqueNm*1000.0/dp;
        double fr=ft*Math.tan(Math.toRadians(pressureAngleDeg));
        double resultant=Math.hypot(ft,fr);

        // Preliminary simply-supported shaft bending assumption: gear centered between bearings, span = 3 x pitch diameter.
        double span1=Math.max(80.0,3.0*dp);
        double span2=Math.max(100.0,2.0*dg);
        double m1=resultant*span1/4.0/1000.0;
        double m2=resultant*span2/4.0/1000.0;
        double s1=EngineeringLibrary.requiredSolidShaftDiameter(m1,inputTorqueNm,shaftSyMpa,shaftTargetFoS);
        double s2=EngineeringLibrary.requiredSolidShaftDiameter(m2,outTorque,shaftSyMpa,shaftTargetFoS);
        double p1=EngineeringLibrary.selectPreferredShaft(s1);
        double p2=EngineeringLibrary.selectPreferredShaft(s2);

        // Preliminary Lewis form factor for 20-deg full-depth involute.
        double y=Math.max(0.05,0.154-0.912/pinionTeeth);
        double sigma=ft/(faceWidthMm*moduleMm*y);
        double gearSf=gearAllowableBendingMpa/sigma;

        DesignOptimizationEngine.BearingPick b1=DesignOptimizationEngine.selectBearing(resultant/2.0,0,inputRpm,targetLifeHours,1.5);
        double outRpm=inputRpm/ratio;
        DesignOptimizationEngine.BearingPick b2=DesignOptimizationEngine.selectBearing(resultant/2.0,0,outRpm,targetLifeHours,1.5);
        return new GearboxResult(ratio,outTorque,ft,fr,dp,dg,s1,s2,p1,p2,b1,b2,sigma,gearSf);
    }

    private GearboxDesignEngine(){}
}
