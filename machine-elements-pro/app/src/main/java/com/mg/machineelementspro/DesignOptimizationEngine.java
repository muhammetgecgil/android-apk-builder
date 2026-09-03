package com.mg.machineelementspro;

public final class DesignOptimizationEngine {
    public static final class BearingPick {
        public final String designation; public final double lifeHours; public final double staticFoS;
        BearingPick(String d,double l,double s){designation=d;lifeHours=l;staticFoS=s;}
    }
    public static final class BoltPick {
        public final String size; public final String propertyClass; public final double preloadN; public final double proofFoS;
        BoltPick(String s,String pc,double p,double f){size=s;propertyClass=pc;preloadN=p;proofFoS=f;}
    }
    public static final class MaterialOption {
        public final String material; public final double diameterMm; public final double kgPerM; public final double score;
        MaterialOption(String m,double d,double k,double s){material=m;diameterMm=d;kgPerM=k;score=s;}
    }

    private static final String[] BRG={"6200","6201","6202","6203","6204","6205","6206","6207","6208","6209","6210"};
    private static final double[] C={7.0,7.8,9.6,12.8,13.5,14.8,19.5,25.5,29.1,35.1,35.1}; // kN preliminary catalog
    private static final double[] C0={2.36,3.1,3.75,4.75,6.55,7.8,11.2,15.3,17.8,21.6,23.2};
    private static final double[] BOLT_D={6,8,10,12,14,16,18,20,22,24,27,30,36};
    private static final double[] ATS={20.1,36.6,58.0,84.3,115,157,192,245,303,353,459,561,817};

    public static BearingPick selectBearing(double radialN,double axialN,double rpm,double targetHours,double requiredStaticFoS){
        if(radialN<0||axialN<0||rpm<=0||targetHours<=0||requiredStaticFoS<=0) throw new IllegalArgumentException("Invalid bearing inputs");
        double P=Math.max(1.0,radialN+0.56*axialN); double P0=Math.max(1.0,radialN+0.5*axialN);
        for(int i=0;i<BRG.length;i++){
            double life=1_000_000.0/(60.0*rpm)*Math.pow(C[i]*1000.0/P,3.0);
            double s0=C0[i]*1000.0/P0;
            if(life>=targetHours&&s0>=requiredStaticFoS) return new BearingPick(BRG[i],life,s0);
        }
        return new BearingPick("OUT-OF-RANGE",0,0);
    }

    public static BoltPick selectBolt(double serviceTensionN,double targetPreloadN,double targetFoS){
        if(serviceTensionN<0||targetPreloadN<0||targetFoS<=0) throw new IllegalArgumentException("Invalid bolt inputs");
        String[] pc={"8.8","10.9","12.9"}; double[] proof={580,830,970};
        for(int p=0;p<pc.length;p++) for(int i=0;i<BOLT_D.length;i++){
            double capacity=proof[p]*ATS[i]; double preload=Math.min(0.75*capacity,targetPreloadN);
            double fos=capacity/Math.max(1.0,serviceTensionN+preload);
            if(preload>=targetPreloadN*0.999 && fos>=targetFoS) return new BoltPick("M"+(int)BOLT_D[i],pc[p],preload,fos);
        }
        return new BoltPick("OUT-OF-RANGE","-",0,0);
    }

    public static double[] basicHoleH7ShaftG6(double nominalMm){
        if(nominalMm<=0) throw new IllegalArgumentException("Invalid nominal size");
        double D=Math.max(3.0,nominalMm); double i=0.45*Math.cbrt(D)+0.001*D; // micrometre ISO 286 unit tolerance approximation
        double it6=10*i, it7=16*i; double esG6=-2.5*Math.pow(D,0.34); // um approximation
        double holeMin=nominalMm, holeMax=nominalMm+it7/1000.0;
        double shaftMax=nominalMm+esG6/1000.0, shaftMin=shaftMax-it6/1000.0;
        return new double[]{holeMin,holeMax,shaftMin,shaftMax,holeMin-shaftMax,holeMax-shaftMin};
    }

    public static MaterialOption optimizeShaft(double bendingNm,double torqueNm,double targetFoS){
        String[] mat={"AISI 1045","AISI 4140","7075-T6","Ti-6Al-4V"};
        double[] sy={310,655,503,880}; double[] rho={7850,7850,2810,4430};
        MaterialOption best=null;
        for(int i=0;i<mat.length;i++){
            double eq=Math.sqrt(Math.pow(32*bendingNm*1000/Math.PI,2)+3*Math.pow(16*torqueNm*1000/Math.PI,2));
            double d=Math.cbrt(eq*targetFoS/sy[i]);
            double kgm=rho[i]*Math.PI*Math.pow(d/1000.0,2)/4.0;
            double score=kgm*(1.0+0.08*d); // mass-diameter compromise
            MaterialOption o=new MaterialOption(mat[i],d,kgm,score); if(best==null||o.score<best.score)best=o;
        }
        return best;
    }

    private DesignOptimizationEngine(){}
}
