package com.mg.fixturecockpitsim.sim;

/** Pure-Java gain model for the procedural twin-engine fighter audio engine. */
public final class FighterSoundModel {
    public static final class Mix {
        public double mach, intake, fan, turbine, exhaust, afterburner, wind, tyre, brake, gearHydraulic, surfaceHydraulic, transonic;
    }

    public static double speedOfSoundMps(double altitudeM){
        double h=clamp(altitudeM,0,20000);
        double t=Math.max(216.65,288.15-0.0065*h);
        return Math.sqrt(1.4*287.05*t);
    }

    public Mix evaluate(double throttle,double speedMps,double altitudeM,double gearPosition,double brake01,boolean onGround,double gearMotion01,double surfaceMotion01){
        Mix m=new Mix();
        double th=clamp(throttle,0,1), sp=Math.max(0,speedMps), gear=clamp(gearPosition,0,1), br=clamp(brake01,0,1);
        m.mach=sp/speedOfSoundMps(altitudeM);
        m.intake=.035+.19*smooth(.08,.88,th);
        m.fan=.025+.16*smooth(.10,.86,th);
        m.turbine=.018+.13*smooth(.22,.92,th);
        m.exhaust=.070+.33*smooth(.05,.95,th);
        m.afterburner=.43*smooth(.80,.96,th);
        m.wind=.32*smooth(24,285,sp)*(1+.18*gear);
        m.tyre=onGround?.28*smooth(2,105,sp):0;
        m.brake=onGround?m.tyre*br*.62:0;
        m.gearHydraulic=.20*clamp(gearMotion01,0,1);
        m.surfaceHydraulic=.065*clamp(surfaceMotion01,0,1);
        double d=Math.abs(m.mach-1.0);
        m.transonic=d<.11?.12*(1-d/.11):0;
        return m;
    }

    /** A boom belongs to a stationary/world-fixed observer, not an aircraft-following camera. */
    public static boolean shouldTriggerSonicBoom(double previousMach,double mach,boolean worldFixedObserver){
        return worldFixedObserver&&previousMach<1.0&&mach>=1.0;
    }

    private static double smooth(double a,double b,double x){
        if(b<=a)return x>=b?1:0;
        double t=clamp((x-a)/(b-a),0,1);return t*t*(3-2*t);
    }
    public static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
}
